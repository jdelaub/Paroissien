#!/usr/bin/env bash
# Construit sur la VM l'APK debug et l'APK release, puis les dépose dans le partage du NAS comme pour les
# autres projets : ~/VM-Exchange/APK/Paroissien/Paroissien-<version>-debug.apk et Paroissien-<version>.apk.
# Monter versionCode et versionName dans android/app/build.gradle.kts avant chaque livraison.
#
# Saveur « personnel » par défaut (le téléphone de l'utilisateur, scénario A). Variables facultatives :
#   PACKS=/chemin/assets    dossier contenant packs-private/ (paquets privés de la saveur personnelle, ADR 001)
#   FLAVOR=domainePublic    autre saveur (suffixe -domainePublic dans le nom)
#   FLAVOR=vide             l'app sans aucun livre, à remplir par une sauvegarde (Paroissien-<v>-vide.apk)
#   DEST=/autre/dossier     autre destination
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
dest="${DEST:-$HOME/VM-Exchange/APK/Paroissien}"
flavor="${FLAVOR:-personnel}"
export JAVA_HOME="${JAVA_HOME:-$HOME/Android/jdk-21}"
export PATH="$JAVA_HOME/bin:$PATH" # apksigner cherche java dans le PATH
apksigner="$(ls -d "$HOME"/Android/Sdk/build-tools/*/ | sort -V | tail -1)apksigner"

# Un seul travail lourd à la fois sur la VM (2 cœurs, 7,8 Gio, hébergée par le NAS) : elle a gelé deux fois le
# 12/10/2026, un build release tournant en même temps qu'un agent. On refuse de construire sans marge.
if pgrep -f "qemu-system|emulator64" >/dev/null; then
    echo "Un émulateur tourne : arrêtez-le avant de construire." >&2
    exit 1
fi
if pgrep -f "GradleDaemon|GradleWorkerMain|chrome.*--headless" >/dev/null; then
    echo "Un autre build Gradle ou un Chrome sans fenêtre tourne : attendez qu'il finisse." >&2
    exit 1
fi
available=$(awk '/MemAvailable/ {print int($2 / 1024)}' /proc/meminfo)
if [ "$available" -lt 3000 ]; then
    echo "Seulement $available Mo de mémoire libre (3000 au moins) : fermez ce qui tourne avant de construire." >&2
    exit 1
fi

version=$(sed -nE 's/^[[:space:]]*versionName = "([^"]+)".*/\1/p' "$root/android/app/build.gradle.kts")
tag=""
[ "$flavor" = personnel ] || tag="-$flavor"
debug_name="Paroissien-$version$tag-debug.apk"
release_name="Paroissien-$version$tag.apk"
for f in "$debug_name" "$release_name"; do
    if [ -e "$dest/$f" ]; then
        echo "$dest/$f existe déjà : montez versionCode et versionName." >&2
        exit 1
    fi
done

# Saveur personnelle : paquets privés construits depuis le dépôt voisin paroissien-content, s'il est là.
private="$root/../paroissien-content"
if [ -z "${PACKS:-}" ] && [ "$flavor" = personnel ] && [ -d "$private/packages" ]; then
    PACKS="$private/dist/assets"
    rm -rf "$PACKS/packs-private"
    python3 -I "$root/content/pqt.py" --data "$private/data" --packages "$private/packages" --registry "$private/registry" \
        build --sign-key "$root/content/schema/keys/test-p256.pem" --out "$PACKS/packs-private"
    # Vignettes gravées tirées des scans personnels (tools/cadres/vignettes.py) : assets vignettes/.
    rm -rf "$PACKS/vignettes"
    if [ -f "$private/vignettes/catalog.json" ]; then
        mkdir -p "$PACKS/vignettes"
        cp "$private/vignettes/catalog.json" "$private/vignettes/"*.webp "$PACKS/vignettes/"
    fi
    # Images pieuses de l'utilisateur, photographiées et décrites (tools/images/cartes.py) : assets images/.
    rm -rf "$PACKS/images"
    if [ -f "$private/images-pieuses/catalog.json" ]; then
        mkdir -p "$PACKS/images/thumbs"
        cp "$private/images-pieuses/catalog.json" "$private/images-pieuses/"*.webp "$PACKS/images/"
        cp "$private/images-pieuses/thumbs/"*.webp "$PACKS/images/thumbs/"
    fi
fi

packs_arg=()
[ -z "${PACKS:-}" ] || packs_arg=("-Pparoissien.personnelPacksDir=$PACKS")
# Priorité basse (processeur et disque) ; sans daemon, la mémoire du build est rendue dès la fin.
(cd "$root/android" && nice -n 10 ionice -c2 -n7 ./gradlew --no-daemon --console=plain "${packs_arg[@]}" ":app:assemble${flavor^}Debug" ":app:assemble${flavor^}Release")

out="$root/android/app/build/outputs/apk/$flavor"
debug_apk=$(ls "$out"/debug/*.apk)
release_apk=$(ls "$out"/release/*.apk)
case "$release_apk" in
    *unsigned*)
        echo "APK release non signé : propriétés PAROISSIEN_* absentes de ~/.gradle/gradle.properties." >&2
        exit 1
        ;;
esac
"$apksigner" verify "$release_apk"

mkdir -p "$dest"
# Copie vers le partage du NAS bridée à 20 Mo/s : le NAS héberge aussi la VM.
rsync --bwlimit=20000 "$debug_apk" "$dest/$debug_name"
rsync --bwlimit=20000 "$release_apk" "$dest/$release_name"
ls -l "$dest/$debug_name" "$dest/$release_name"
