#!/usr/bin/env bash
# Construit sur la VM l'APK debug et l'APK release, puis les dépose dans le partage du NAS comme pour les
# autres projets : ~/VM-Exchange/APK/Paroissien/Paroissien-<version>-debug.apk et Paroissien-<version>.apk.
# Monter versionCode et versionName dans android/app/build.gradle.kts avant chaque livraison.
#
# Saveur « personnel » par défaut (le téléphone de l'utilisateur, scénario A). Variables facultatives :
#   PACKS=/chemin/paquets   paquets privés de la saveur personnelle (ADR 001)
#   FLAVOR=domainePublic    autre saveur (suffixe -domainePublic dans le nom)
#   DEST=/autre/dossier     autre destination
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
dest="${DEST:-$HOME/VM-Exchange/APK/Paroissien}"
flavor="${FLAVOR:-personnel}"
export JAVA_HOME="${JAVA_HOME:-$HOME/Android/jdk-21}"
export PATH="$JAVA_HOME/bin:$PATH" # apksigner cherche java dans le PATH
apksigner="$(ls -d "$HOME"/Android/Sdk/build-tools/*/ | sort -V | tail -1)apksigner"

# Un seul travail lourd à la fois sur la VM (2 cœurs, 7,8 Gio).
if pgrep -f "qemu-system|emulator64" >/dev/null; then
    echo "Un émulateur tourne : arrêtez-le avant de construire." >&2
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

packs_arg=()
[ -z "${PACKS:-}" ] || packs_arg=("-Pparoissien.personnelPacksDir=$PACKS")
(cd "$root/android" && ./gradlew --console=plain "${packs_arg[@]}" ":app:assemble${flavor^}Debug" ":app:assemble${flavor^}Release")

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
cp "$debug_apk" "$dest/$debug_name"
cp "$release_apk" "$dest/$release_name"
ls -l "$dest/$debug_name" "$dest/$release_name"
