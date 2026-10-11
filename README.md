# Paroissien

Application Android catholique romaine qui reproduit l'expérience d'un livre de prière papier : pages qui tournent, rubans de tissu, images pieuses. Elle réunit le missel (1962 et forme ordinaire), la Bible, l'Imitation de Jésus-Christ, les Exercices de saint Ignace, les lettres de Clairval, des prières et le calendrier liturgique.

État : **phase 0 (fondations)**. Le plan est dans [docs/planning/PLAN.md](docs/planning/PLAN.md) et les décisions techniques dans [docs/adr/](docs/adr/README.md).

## Arborescence

| Dossier | Rôle |
|---|---|
| `android/` | Application Kotlin/Compose (AGP 9.4, Kotlin 2.4, Gradle 9.8) |
| `content/` | Chaîne de contenu en Python, qui produit les paquets SQLite |
| `docs/` | Planification, ADR, démarches d'autorisation |
| `tools/` | Scripts : installation sur le téléphone, relevé de l'appareil, contrôle des droits |
| `.github/workflows/` | CI publique, limitée à la saveur `domainePublic` |

## Développement

La VM n'a que 2 cœurs et 7,8 Gio : un seul build Gradle à la fois, jamais d'émulateur. Les builds complets passent par la CI.

```bash
cd android && JAVA_HOME=~/Android/jdk-21 ./gradlew :core:model:test :core:liturgy:test
```

```bash
cd android && JAVA_HOME=~/Android/jdk-21 ./gradlew :app:assembleDomainePublicDebug
```

```bash
cd android && JAVA_HOME=~/Android/jdk-21 ./gradlew :core:designsystem:recordRoborazziDebug
```

```bash
cd content && python3 -I run_tests.py
```

Pour livrer un APK de test, montez d'abord `versionName` et `versionCode`. Le script dépose le debug et la release dans `~/VM-Exchange/APK/Paroissien/` :

```bash
tools/deliver-apk.sh
```

Autre possibilité, installer directement l'APK de la CI par adb :

```bash
tools/install-ci-apk.sh
```

```bash
tools/device-inventory.sh
```

La saveur `personnel` lit ses paquets privés hors du dépôt : `-Pparoissien.personnelPacksDir=/chemin/vers/paquets`.

## Licences

Le code est sous GPL-3.0-or-later ([LICENSE](LICENSE)). Le module `android/core/liturgy` est sous Apache-2.0 ([LICENSE](android/core/liturgy/LICENSE)). Chaque paquet de contenu porte sa propre licence et sa provenance.
