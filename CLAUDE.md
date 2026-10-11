# Paroissien

Application Android catholique (Kotlin/Compose) qui imite un livre de prière papier. Le plan est dans `docs/planning/PLAN.md` et les décisions dans `docs/adr/`. Les docs et les commentaires sont en français.

## Builds et livraison

- La VM a 2 cœurs et 7,8 Gio : un seul build Gradle à la fois, jamais d'émulateur. `./gradlew --stop` arrête aussi les daemons des autres sessions qui utilisent Gradle 9.8 : à éviter si une autre session compile.
- JDK : `JAVA_HOME=~/Android/jdk-21` (java n'est pas dans le PATH).
- **Livrer un APK de test** : monter `versionCode` et `versionName` dans `android/app/build.gradle.kts`, puis lancer `tools/deliver-apk.sh`. Le script construit sur la VM le debug et la release de la saveur `personnel`, puis les copie dans `~/VM-Exchange/APK/Paroissien/` sous les noms `Paroissien-<v>-debug.apk` et `Paroissien-<v>.apk`.
- **Signatures** :
  - release : clé `~/.android-keys/paroissien-release.jks`, propriétés `PAROISSIEN_*` dans `~/.gradle/gradle.properties` ;
  - debug : `android/keystore/debug.keystore` (commité, non secret) ;
  - le debug (`app.paroissien.dev`) et la release (`app.paroissien`) cohabitent sur le téléphone.
- **Tests rapides** :
  - `./gradlew :core:model:test :core:liturgy:test`, à lancer dans `android/` ;
  - `python3 -I run_tests.py`, à lancer dans `content/`.
- **Captures** : `:core:designsystem:recordRoborazziDebug` enregistre les images de référence dans `src/test/screenshots` ; la CI les vérifie.

## Règles

- Deux saveurs. `domainePublic` est la seule construite par la CI publique (github.com/jdelaub/Paroissien). `personnel` lit ses paquets privés hors du dépôt, via `-Pparoissien.personnelPacksDir` ou `PACKS=` pour `deliver-apk.sh`. Aucun contenu protégé (Clairval, Barroux, AELF) n'entre dans ce dépôt : il va dans le dépôt privé `jdelaub/paroissien-content`.
- `core:liturgy` est sous Apache-2.0 : il ne doit dépendre d'aucun module GPL du projet.
- Le pliage de recherche (`core:model` `Fold.kt` et `content/.../fold.py`) se modifie d'abord dans `content/schema/fold-vectors.json`.
- Commits : identité `53884675+jdelaub@users.noreply.github.com` (configurée dans le dépôt), jamais l'adresse personnelle.
