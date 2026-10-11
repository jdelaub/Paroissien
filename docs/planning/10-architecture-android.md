# 10 — Architecture technique Android, build et distribution

> Agent 10, rapport de planification du 11 octobre 2026. Périmètre : structure Gradle, choix techniques, paquets de contenu côté application, distribution, build sur la VM, CI, tests et vie privée.
> Le moteur de lecture (06), le tourne-page (07), l'UX (08), les signets (09), la chaîne de contenu (11) et le juridique (12) sont seulement **cités comme points d'interface**.

---

## 1. Résumé

- **Peu de modules, mais aux bonnes frontières.** Au MVP : `app`, 6 modules `core` et 2 modules `feature`, plus un `build-logic` de conventions. Missel, Bible, Imitation, Lettres et Prières ne sont **pas** des modules de code : ce sont des **paquets de contenu** lus par un même lecteur. Seuls les écrans vraiment différents deviendront des modules (missel, retraite, méditation) en V1.
- **minSdk 26 recommandé (environ 98 % du parc), targetSdk 36 et compileSdk 37.** Passer à minSdk 33 pour l'effet AGSL ferait perdre environ 42 % du parc d'après les chiffres Google de décembre 2025, et encore 30 à 35 % estimés en octobre 2026. Il vaut mieux garder un tourne-page de repli sous l'API 33.
- **Pile retenue : Navigation 3 (1.2.0), DataStore 1.2.1, WorkManager 2.12.0 et kotlinx.serialization.** Pour l'injection de dépendances, on commence par un **graphe écrit à la main**, sans Hilt ni KSP supplémentaire. Metro 1.4.5 reste l'option de repli si le graphe grossit.
- **Base de données : Room 3.0.3 avec le pilote `BundledSQLiteDriver` d'androidx.sqlite 2.7.1.** Ce SQLite 3.53.4 est compilé **avec FTS5 et sans ICU**, et Room 3 ajoute `@Fts5`. Le tokenizer `unicode61 remove_diacritics 2` couvre le français et le latin sans ICU. **Inutile de passer par requery.** Les paquets de contenu arrivent **sous forme de bases SQLite déjà indexées par la chaîne de contenu** et s'ouvrent en lecture seule. Room ne gère que les données de l'utilisateur.
- **Paquets de contenu.** Le noyau du domaine public est intégré à l'APK. Les gros paquets (missel complet, Bible complète, lettres) seront téléchargeables en V1 depuis des **GitHub Releases**, avec un **index signé (ECDSA P-256)** et un SHA-256 par paquet. Budget : **30 Mo au plus par APK**, la limite pratique d'IzzyOnDroid.
- **Distribution.** Au MVP, APK signé publié sur GitHub Releases, puis **IzzyOnDroid**. F-Droid principal et Play Store viendront plus tard, et chacun pose un problème précis (droits du contenu, 12 testeurs pendant 14 jours). L'**enregistrement de développeur Android** est à prévoir **avant 2027** : il sera obligatoire dans le monde entier pour les installations hors adb.
- **Builds lourds en CI.** GitHub Actions construit les APK (debug en artefact, release signée sur tag). La VM se limite aux tests JVM, aux captures Roborazzi, à `gh run download` et à `adb install` en Wi-Fi. Attention : la VM n'expose que **7,75 Gio de RAM** et 2 Gio de swap, pas 10 Go. `gradle.properties` doit être réglé en conséquence.
- **Tests.** Le calendrier (`core:liturgy`) est testé en JVM pur avec des tables de référence. Les captures d'écran passent par **Roborazzi 1.76** sur Robolectric en rendu natif, comme le fait déjà CRA-Toolbox. Paparazzi 2 est encore en alpha. La fluidité du tourne-page se mesure par **Macrobenchmark 1.5.0 et des baseline profiles sur le téléphone physique**, jamais sur l'émulateur sans GPU.
- **Vie privée.** Aucune analytique, aucun SDK Google, **aucune permission INTERNET au MVP** si tout le contenu est intégré. Les permissions sont ajoutées une à une en V1, à la demande et dans leur contexte. Une liste blanche des permissions est vérifiée en CI.

---

## 2. Constats détaillés

### 2.1 Ce que l'on réutilise du projet CRA-Toolbox

Lecture seule de `/home/joseph/Work/Projects/CRA-Toolbox/android` :

| Élément | CRA-Toolbox | À reprendre pour Paroissien |
|---|---|---|
| Disposition | Dépôt racine avec `android/` (projet Gradle), `docs/` et `tools/` (scripts Python) | **Oui**, et `content/` prend le rôle de `tools/` pour la chaîne de contenu |
| Wrapper | Gradle 9.8.0 | Oui |
| Catalogue | `gradle/libs.versions.toml` : AGP 9.4.1, Kotlin 2.4.20, BOM 2026.09.00, activity 1.13.0, lifecycle 2.11.0, core 1.19.1, coroutines 1.11.0, serialization 1.11.0, okhttp 5.5.0 | Oui, compléter avec Nav3, Room3, sqlite, etc. (voir §3.1) |
| Kotlin | **Kotlin intégré d'AGP 9**, sans plugin `kotlin-android`. Plugin `kotlin.compose` et module `core` en `kotlin.jvm` pur | Oui : les modules métier sont en JVM pur, donc testés en quelques secondes |
| SDK | `compileSdk 37`, `targetSdk 36`, `minSdk 26`, Java 17 en bytecode, JDK 21 pour lancer Gradle | Oui |
| Tests d'interface | Robolectric 4.17 en `@GraphicsMode(NATIVE)` et `captureToImage()` vers des PNG dans `build/screens/` | Oui, mais **outillé avec Roborazzi** pour comparer les images aux références |
| `gradle.properties` | `-Xmx2g`, `workers.max=2`, `caching=true`, Kotlin daemon `-Xmx1g` | Base reprise et complétée par le configuration cache et le GC (§3.1) |
| Injection de dépendances | Aucune : une simple classe `Store` sur SharedPreferences | Même philosophie : graphe manuel |
| CI | Aucune (`.github` absent) | À créer |
| Manifeste | Hors ligne, sans permission INTERNET, avec un commentaire qui le justifie | Même principe au MVP |

Mesures faites sur la VM : `nproc = 2` et `MemTotal = 8 128 004 kB` (environ **7,75 Gio**), avec **2 Gio de swap**. Le brief annonce 10 Go et un commentaire de CRA-Toolbox parle de « 3 CPUs » ; les deux sont inexacts. Le SDK installé comprend platforms 36 et 37.0, build-tools 35, 36 et 37, une image système android-36 et `adb` dans `platform-tools`. Le `.gitignore` de Paroissien ignore déjà `/content/raw/`, `/content/cache/`, `*.apk`, `*.aab` et `*.keystore`, ce qui est cohérent avec l'arborescence proposée.

### 2.2 minSdk et part de marché

Google ne publie plus de tableau public. Les chiffres viennent de l'assistant « Help me choose » d'Android Studio, **arrêtés au 1er décembre 2025** et repris par composables.com et heise :

| minSdk | Version | Part cumulée (déc. 2025) |
|---|---|---|
| 26 | Android 8.0 | **98,3 %** |
| 29 | Android 10 | 90,7 % |
| 31 | Android 12 | 69,2 % |
| **33** | Android 13 | **57,8 %** |
| 34 | Android 14 | 43,9 % |
| 35 | Android 15 | 26,7 % |

- **Avec minSdk 33, on perdait 42 % du parc fin 2025.** En prolongeant la tendance (adoption d'Android 16, sortie d'Android 17), on peut estimer cette perte à **30-35 % en octobre 2026**. C'est une extrapolation, pas une mesure. Le public visé (paroissiens, souvent âgés, qui gardent leur téléphone longtemps) est probablement plus en retard que la moyenne mondiale.
- **Ce que chaque seuil apporte** (d'après la documentation des API) :
  - **API 26** : `java.time` natif, donc pas de désucrage pour le calendrier. Justification inter-mots du texte (`JUSTIFICATION_MODE_INTER_WORD`, dont dépend `TextAlign.Justify`, à vérifier sur Compose 1.12). Polices XML.
  - **API 24** : plancher imposé de toute façon par WorkManager 2.12. AndroidX est passé à minSdk 23 par défaut.
  - **API 29** : thème sombre du système.
  - **API 30** : débogage Wi-Fi sans câble.
  - **API 31** : `RenderEffect` (flou).
  - **API 33** : `RuntimeShader` (AGSL), `POST_NOTIFICATIONS`, `Signature("Ed25519")` dans `java.security`, langue par application, profils de référence générés sans root.
- **Conclusion** : minSdk 26 avec un **double rendu du tourne-page** : AGSL sur l'API 33 et plus, et en dessous une rotation 3D par `graphicsLayer` (`rotationY` et ombre portée) ou un glissement de page. Le coût du repli est à chiffrer par l'agent 07.

### 2.3 Navigation

- **Navigation 3** est stable : 1.1.0 le 8 avril 2026, **1.2.0 le 23 septembre 2026** et 1.3.0-alpha02 le 7 octobre. La version 1.1 a apporté les scènes partagées (shared elements), `SceneDecoratorStrategy` et une liste de `SceneStrategy`. La 1.2 a ajouté les **liens profonds** (`DeepLinkRequest`, `UriDeepLinkMatcher`).
- La pile de retour est **une simple liste d'état** qui nous appartient. On peut donc la persister (« rouvrir le livre là où on l'a laissé »), y pousser directement un `Reader(locator)` depuis un signet ou une notification, et la tester sans framework.
- Artefacts compagnons : `lifecycle-viewmodel-navigation3` (un ViewModel par entrée de pile ; la documentation cite **2.12.0-alpha04**, vérifier si la 2.11.0 stable le contient) et `adaptive-navigation3` (1.4.0-alpha03, scènes liste/détail pour tablette).
- La **double page en paysage** se joue à l'intérieur du lecteur (agents 06 et 07), pas dans la navigation. Une scène Nav3 à deux volets servira à la bibliothèque sur tablette (table des matières à gauche, page à droite).

### 2.4 Injection de dépendances

| Option | État en octobre 2026 | Coût sur la VM à 2 cœurs | Risque |
|---|---|---|---|
| **Manuelle** (`AppGraph` et `viewModelFactory { initializer {} }`) | Rien à installer | **Nul** | Erreurs de câblage visibles à la compilation (constructeurs). Le graphe reste petit : une dizaine de singletons, moins de dix ViewModels |
| **Metro** 1.4.5 (24 sept. 2026, 1.0 le 27 avril 2026) | Plugin du compilateur, **sans KSP**, validé à la compilation. Kotlin 2.4.20 pris en charge **depuis Metro 1.2.0**. Intégration Compose par `metrox-viewmodel-compose` (`metroViewModel()`) | Faible | Lié à la version du compilateur Kotlin : chaque montée de Kotlin peut demander une montée de Metro |
| **Hilt** (Dagger 2.59 et plus pour AGP 9, 2.60.x en juin-juillet 2026, androidx.hilt 1.4.0) | Mûr et très documenté | **Une passe KSP de plus dans chaque module** annoté, plus un plugin Gradle | Lourd pour la VM. 2.59 exige AGP 9 et Gradle 9.1 |
| **Koin** 4.2.x | Résolution à l'exécution, sans génération de code. `koin-compose-navigation3` encore **expérimental** | Nul | Erreurs découvertes seulement à l'exécution |

Room 3 impose déjà KSP, mais **uniquement dans `core:data`**. Hilt l'étendrait à toutes les features. Le graphe manuel ne coûte rien et se migre mécaniquement vers Metro, puisque l'injection par constructeur est déjà en place.

### 2.5 Base de données, FTS5 et SQLite

**Constats vérifiés :**

- **Room 3** : 3.0.0 le 1er juillet 2026, **3.0.3 le 9 septembre 2026**, 3.1.0-alpha01 le 9 septembre.
  - Nouveau paquet `androidx.room3`.
  - **Un `SQLiteDriver` est obligatoire.** `SupportSQLite` n'existe plus que dans un adaptateur.
  - Tout passe par des coroutines : DAO `suspend` ou `Flow`.
  - Génération de code **en Kotlin par KSP uniquement**, sans KAPT.
  - **FTS5 natif via `@Fts5`** depuis la 3.0.0-alpha02, avec les constantes `TOKENIZER_ASCII` et `TOKENIZER_TRIGRAM` et l'option `detail`.
- **androidx.sqlite** 2.7.1 (9 septembre 2026), artefact `sqlite-bundled` (`BundledSQLiteDriver`).
  - D'après le `build.gradle` d'androidx, il embarque **SQLite 3.53.4**, compilé avec `SQLITE_ENABLE_FTS3`, `FTS3_PARENTHESIS`, `FTS4`, **`FTS5`**, `JSON1`, `MATH_FUNCTIONS`, `RTREE`, `STAT4`, `SECURE_DELETE`, `TEMP_STORE=3`, `THREADSAFE=2` et `DEFAULT_AUTOVACUUM=1`.
  - **Aucun drapeau ICU.**
  - Depuis la 2.6.0, minSdk 23 et `addExtension()` pour charger des extensions.
- La version 3.53.4 est **postérieure à 3.53.2**, qui corrigeait des CVE dans FTS5. Le SQLite fourni par Android **n'active pas FTS5** de façon fiable, quelle que soit la version : c'est bien la raison d'embarquer le sien.

**Analyse :**

- **FTS5 sans ICU suffit pour le français et le latin.** Le tokenizer `unicode61` (inclus dans FTS5) replie la casse Unicode, et `remove_diacritics 2` assimile *é/è/ê/e* et *à/a*. ICU ne sert qu'à découper les mots des langues sans espaces (CJK, thaï), hors sujet ici.
- **Limites à traiter dans la chaîne de contenu**, ce qui concerne les agents 06 et 11 :
  - `unicode61` ne décompose **pas** les ligatures *œ* et *æ* (fréquentes en latin liturgique et dans « cœur », « œuvre »).
  - Il n'existe **pas de racinisation française**.
  - Solution proposée : indexer une **colonne normalisée** (*œ* devient *oe*, *æ* devient *ae*, apostrophes typographiques unifiées) dans une table FTS5 à contenu externe, et appliquer **la même normalisation à la requête**, dans le code Kotlin partagé.
  - Ajouter des index de préfixes (`prefix='2 3'`) et la complétion par préfixe dans la requête (`prière*`).
  - Une racinisation (Snowball français) reste possible plus tard dans une colonne séparée.
- **requery/sqlite-android est inutile** : le pilote embarqué d'androidx est maintenu par Google, KMP, compatible avec Room 3 et plus récent. La dernière version de requery repérée est 3.50.x, antérieure aux correctifs FTS5.
- **Coût** : environ 1 à 1,5 Mo de bibliothèque native par ABI (estimation). Ces `.so` sont stockés non compressés et alignés sur 16 Ko par AGP 8.5.1 et plus. **Play refusera dès le 1er février 2027** les mises à jour qui ne prennent pas en charge les pages de 16 Ko. À contrôler avec `zipalign -c -P 16`.

**Architecture des données qui en découle :**

| Base | Contenu | Accès | Producteur |
|---|---|---|---|
| `content/<paquet>/content.db` (une par paquet) | Arbre (sommaire), blocs de texte balisés, index FTS5, métadonnées | **Lecture seule**, ouverte avec `file:…?immutable=1` par `BundledSQLiteDriver`, requêtes SQL écrites à la main (peu nombreuses : sommaire, section, recherche) | **Chaîne de contenu (agent 11)**, en Python avec un `sqlite3` disposant de FTS5 |
| `user.db` | Signets, images pieuses placées, annotations, positions de lecture, historique, progression des retraites, registre des paquets installés | **Room 3** et `BundledSQLiteDriver` | Application |
| DataStore Preferences | Thème, taille de police, rite, région liturgique, options | DataStore 1.2.1 | Application |

On n'utilise **pas Room pour les bases de contenu**. Room vérifie un hachage d'identité du schéma (`room_master_table`) que la chaîne Python devrait reproduire exactement : ce couplage serait fragile. La recherche dans toute la bibliothèque se fait par `ATTACH` des bases de paquets sur une connexion (10 bases au plus par défaut) et un `UNION ALL`, ou en interrogeant les bases une à une puis en fusionnant les résultats.

**Ancre stable (« locator »)**, contrat à figer avec les agents 06, 09 et 11 : un signet stocke `paquet:id_bloc_stable + décalage`, jamais un numéro de page, puisque la pagination dépend de la police et de l'orientation. Les identifiants de bloc doivent **survivre aux nouvelles versions d'un paquet**.

### 2.6 Arrière-plan, notifications et sérialisation

- **WorkManager 2.12.0** (23 sept. 2026) a un **minSdk de 24**. On l'utilise pour la vérification périodique de l'index des paquets et de la lettre du mois (contrainte réseau, batterie non faible, intervalle de 15 minutes au moins avec une marge souple), et pour les téléchargements de paquets (`CoroutineWorker`, reprise par en-têtes HTTP `Range`).
  - **À savoir** : le manifeste de WorkManager ajoute de lui-même `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `ACCESS_NETWORK_STATE` et `FOREGROUND_SERVICE`, et sa base interne repose sur **Room 2** (paquet `androidx.room`, distinct de `room3`, donc sans conflit mais avec quelques centaines de Ko en plus, estimation).
  - Les services de premier plan `dataSync` sont limités à **6 h par 24 h** pour un targetSdk 35 et plus. On les évite : un paquet de quelques dizaines de Mo se télécharge dans la fenêtre normale d'un worker.
- **Angélus (6 h, 12 h, 18 h).** Il relève d'**AlarmManager**, pas de WorkManager, qui n'est pas précis à l'heure.
  - Les alarmes exactes demandent `SCHEDULE_EXACT_ALARM`, **non accordée par défaut** depuis Android 14 pour un targetSdk 33 et plus. `USE_EXACT_ALARM` est réservée aux réveils et aux agendas.
  - **`setWindow()` ne demande aucune permission**, mais sa fenêtre minimale est de **10 minutes** pour un targetSdk 31 et plus.
  - Il faut `RECEIVE_BOOT_COMPLETED`, un récepteur de changement d'heure ou de fuseau, et `POST_NOTIFICATIONS` (API 33 et plus) demandée au moment où l'on active le rappel.
- **Mode méditation « Ne pas déranger ».** Pour un targetSdk 35 et plus, une application **ne peut plus modifier l'état global du DND**. Un appel à `setInterruptionFilter()` crée une **`AutomaticZenRule` implicite**. Il faut l'accès à la politique de notifications (`ACCESS_NOTIFICATION_POLICY`), accordé par l'utilisateur dans les réglages. Garder l'écran allumé (`FLAG_KEEP_SCREEN_ON`) ne demande aucune permission. Point d'interface avec l'agent 08.
- **Sérialisation.** kotlinx.serialization 1.11.0 (déjà au catalogue) sert aux clés Nav3, aux manifestes et à l'index des paquets, et à l'export JSON des données utilisateur. Inutile d'ajouter Protobuf.
- **Réseau.** Pour du GET HTTPS sur des fichiers statiques, OkHttp 5.5.0 (déjà au catalogue) suffit. Ktor serait surdimensionné. `HttpsURLConnection` reste possible pour une dépendance de moins.

### 2.7 Paquets de contenu : intégrés ou téléchargés

| Critère | Intégrés à l'APK (assets) | Téléchargeables (hébergement statique ou GitHub Releases) | Play Asset Delivery |
|---|---|---|---|
| Hors ligne dès l'installation | **Oui** | Après un premier téléchargement | Oui (au moment de l'installation) |
| Taille de l'APK | Grossit à chaque paquet | Reste stable | Hors APK |
| F-Droid principal | Le contenu doit être **reconstruit depuis les sources** et sous **licence libre**, sinon anti-fonctionnalité *NonFreeAssets* ou refus | Possible (pas d'exécutable téléchargé). *NonFreeNet* si l'application dépend d'un service non libre | Impossible |
| IzzyOnDroid | **30 Mo au plus par APK** (règle pratique) | Oui | Impossible |
| Mises à jour du contenu sans republier l'application | Non | **Oui** | Via le Play Store |
| Vie privée | Parfaite | L'hébergeur voit l'adresse IP | Google |
| Stockage sur l'appareil | **Double** : l'asset compressé plus la copie extraite (SQLite exige un vrai fichier) | Simple | Simple |

Repères de taille, **estimations** à confirmer par les agents 02, 03, 04 et 11 :

| Élément | Taille estimée |
|---|---|
| Code Compose et AndroidX après R8 | 6 à 8 Mo |
| SQLite embarqué | 1,5 Mo par ABI, 5 Mo pour un APK universel |
| Polices | 2 à 3 Mo |
| Bible complète en français (base et FTS, zippée) | environ 5 Mo |
| Missel 1962 latin-français | 10 à 12 Mo |
| Lettres de Clairval | environ 7 Mo |
| Images pieuses | environ 6 Mo |
| Prières, Imitation et Ignace | moins de 1 Mo chacun |

**Vérification d'intégrité :**

- Le SHA-256 de chaque paquet figure dans un **`index.json` signé**.
- `Signature("Ed25519")` n'existe dans `java.security` qu'à partir de l'**API 33**. Avec minSdk 26, on utilise **ECDSA P-256 (`SHA256withECDSA`)**, disponible partout et sans dépendance. Le pipeline signe avec `openssl dgst -sha256 -sign`.
- La clé publique est intégrée à l'APK. On en prévoit **deux** (l'actuelle et la suivante) pour pouvoir changer de clé.
- On vérifie aussi les paquets intégrés à l'APK : le coût est négligeable et la même voie de code est testée.
- GitHub Releases accepte des fichiers jusqu'à 2 Gio. Play Asset Delivery n'existe que sur le Play Store ; on l'écarte, sauf en cas de passage au Play Store plus tard.

### 2.8 Distribution

| Canal | Conditions (octobre 2026) | Signature et mises à jour | Conséquences |
|---|---|---|---|
| **APK direct** (GitHub Releases) | Aucune aujourd'hui. **Enregistrement de développeur Android** : en vigueur depuis le **30 septembre 2026** au Brésil, en Indonésie, à Singapour et en Thaïlande, **dans le monde entier en 2027**. Une application non enregistrée ne s'installe alors que par **adb** ou par un « parcours avancé » (options développeur, redémarrage, **attente de 24 h**). Compte « distribution limitée » gratuit : **20 appareils**, sans pièce d'identité. Compte complet : environ 25 $ et une pièce d'identité | Notre clé. Mises à jour manuelles ou par Obtainium | Simple et rapide. Il faudra s'enregistrer avant 2027 pour une diffusion au-delà de 20 appareils |
| **IzzyOnDroid** | Logiciel libre (licence OSI ou FSF), APK **signé par le développeur** et attaché à une **GitHub Release** taguée, **30 Mo au plus**, ni pisteur ni composant propriétaire | **Notre clé**, donc compatible avec l'APK direct | Le meilleur rapport effort/visibilité pour une application libre |
| **F-Droid principal** | Licence libre. Recette de build dans fdroiddata, **reconstruit sur leurs serveurs**. Ni Play Services ni Firebase. *Tous les assets* sous licence valide. Anti-fonctionnalités : *NonFreeAssets*, *NonFreeNet* et *Tracking* (la vérification de mise à jour sans consentement compte comme du pistage) | **Clé de F-Droid**, sauf en **build reproductible** (champs `Binaries` et `AllowedAPKSigningKeys`) : F-Droid publie alors l'APK signé par nous | Délais de publication. Le contenu protégé (lettres de Clairval) ne peut pas être intégré : point juridique pour l'agent 12 |
| **Play Store** | Compte à 25 $ avec identité vérifiée. **Compte personnel récent : test fermé avec 12 testeurs inscrits sans interruption pendant 14 jours** avant la production. **targetSdk 36 obligatoire depuis le 31 août 2026** (dérogation possible jusqu'au 1er novembre 2026), targetSdk 37 en août 2027. AAB : 500 Mo pour le module de base | Play App Signing. **Téléverser notre propre clé** (PEPK) pour garder la même signature que l'APK direct | Fiche « Sécurité des données » (« aucune donnée collectée ») et règles de contenu. Visibilité maximale |

Règle commune : **l'`applicationId` et la clé de signature ne changent plus jamais** après la première diffusion. Si les signatures diffèrent d'un canal à l'autre, l'utilisateur doit désinstaller (et donc perdre ses signets) pour changer de canal. D'où l'export et l'import JSON des données (§3).

### 2.9 Build sur la VM et CI

- **Gradle 9.** Le configuration cache est le mode « préféré » mais **pas encore activé par défaut** ; il le sera dans Gradle 10. On l'active explicitement.
- **AGP 9.**
  - Kotlin intégré activé par défaut : on n'applique plus `kotlin-android`.
  - Nouveau DSL et API de variantes `androidComponents`.
  - Valeurs par défaut : R8 en mode strict, `optimizedResourceShrinking` actif, tests unitaires sur la seule variante debug, contrôle `minCompileSdk` pour les bibliothèques.
  - Minimums : Gradle 9.1, JDK 17, build-tools 36.
- **KSP 2.3.12** (9 sept. 2026) exige AGP 8.12 au moins. Ses corrections pour le Kotlin intégré d'AGP 9 datent des versions 2.3.5 à 2.3.10. Seul `core:data` en a besoin, pour Room 3.
- **GitHub Actions.**

  | Dépôt | Runner `ubuntu-24.04` | Coût |
  |---|---|---|
  | Public | 4 vCPU, 16 Go | Gratuit |
  | Privé | 2 vCPU, 8 Go | **2 000 minutes par mois** et **500 Mo d'artefacts** sur le plan Free, puis environ 0,006 $ par minute |

  - Accélération KVM de l'émulateur disponible sur les runners Linux standards (utile pour générer un baseline profile).
  - Versions courantes : `actions/checkout@v7`, `actions/setup-java@v6`, `gradle/actions/setup-gradle@v6` (6.4.0, qui a **retiré** la mise en cache du configuration cache) et `actions/upload-artifact@v7`.
  - La facturation des runners auto-hébergés prévue en mars 2026 a été **reportée**.
- **adb en Wi-Fi (Android 11 et plus).** Associer l'appareil avec `adb pair IP:PORT` et le code affiché, puis le connecter.
  - La reconnexion automatique repose sur **mDNS**, qui ne traverse pas forcément le NAT de la VM. Dans ce cas, `adb connect IP:PORT_CONNEXION`, avec le port affiché sur l'écran « Débogage sans fil » (différent du port d'association).
  - Avec Android 10 et moins : `adb tcpip 5555` en USB, puis `adb connect`.
  - Diagnostic : `adb server-status` doit renvoyer `mdns_enabled: true`, et `adb mdns track-services --proto-text` montre si le réseau laisse passer mDNS.

### 2.10 Tests : l'outillage disponible

| Outil | Version et état | Usage |
|---|---|---|
| JUnit (JVM) | — | `core:liturgy`, `core:model`, et les requêtes de `core:content` |
| Robolectric | 4.17 (déjà utilisé, `@Config(sdk=[35])` et rendu natif) | Interface Compose sur la JVM |
| **Roborazzi** | **1.76.0 (29 sept. 2026)**, publications toutes les deux semaines, API de test Compose v2 depuis la 1.68 | Captures avec `recordRoborazziDebug`, `verifyRoborazziDebug` et `compareRoborazziDebug` |
| Paparazzi | 2.0.0-alpha05.1 (28 sept.), toujours en alpha. Layoutlib, prise en charge d'AGP 9 annoncée | Écarté pour l'instant |
| Compose Preview Screenshot Testing (Google) | `com.android.compose.screenshot`, **expérimental** (alpha) | Écarté |
| **Benchmark / Macrobenchmark** | **1.5.0 (9 sept. 2026)**. Le plugin `androidx.baselineprofile` **n'exige plus `newDsl=false` avec AGP 9**. UiAutomator 2.4 | Démarrage, `FrameTimingMetric` sur le tourne-page, génération des baseline profiles |
| ProfileInstaller | 1.4.1 | Installation des baseline profiles hors Play |

Un baseline profile se génère sur un **appareil rooté, un émulateur ou un appareil physique en API 33 et plus sans root**. Il n'est installé que dans les builds release.

### 2.11 Vie privée

- Aucune analytique ni rapport de crash automatique. **Ni Google Play Services ni Firebase**, ce qui est aussi une condition de F-Droid et d'IzzyOnDroid.
- Au MVP, avec tout le contenu intégré : **aucune permission dangereuse ni réseau**. L'application est alors *incapable* de communiquer, ce qui est la meilleure garantie à présenter.
- En V1, chaque permission correspond à une fonction que l'on peut désactiver :

  | Permission | Fonction |
  |---|---|
  | `INTERNET` | Paquets et lettre du mois |
  | `POST_NOTIFICATIONS` | Rappel de l'Angélus |
  | `RECEIVE_BOOT_COMPLETED` | Reprogrammation des alarmes au démarrage |
  | `ACCESS_NOTIFICATION_POLICY` | Mode méditation |
  | `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Synthèse vocale en arrière-plan, si l'agent 06 la retient |

- Jamais : localisation (le calendrier se règle par une **région choisie**), contacts, `AD_ID`, ni stockage partagé (l'import et l'export passent par le sélecteur de fichiers SAF, sans permission).
- La sauvegarde automatique Android vers Drive est chiffrée de bout en bout avec un verrouillage d'écran, mais transite par Google. Le transfert d'appareil à appareil reste local.

---

## 3. Recommandations

### 3.1 MVP

#### a) Arborescence du dépôt

```
Paroissien/
├── android/                         # projet Gradle (même disposition que CRA-Toolbox)
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   ├── gradle.properties
│   ├── gradle/libs.versions.toml
│   ├── build-logic/convention/      # 4 plugins de convention (voir c)
│   ├── app/                         # Application, MainActivity, pile Nav3, AppGraph, Réglages, Méditation (MVP)
│   ├── core/
│   │   ├── model/                   # JVM : Book, Node, Block, Locator, LiturgicalColor, PackageManifest…
│   │   ├── liturgy/                 # JVM : calendrier hors ligne (agent 05), java.time
│   │   ├── content/                 # Android : registre et installation des paquets, lecture SQLite, recherche FTS5, vérification
│   │   ├── data/                    # Android : Room 3 (user.db), DataStore, dépôts, export et import JSON
│   │   ├── designsystem/            # Android : thèmes clair, sombre et sépia, polices, couleurs liturgiques, signet, lettrine, rubrique
│   │   └── reading/                 # Android : moteur de pagination et de rendu, tourne-page AGSL et repli (agents 06 et 07)
│   ├── feature/
│   │   ├── today/                   # « Aujourd'hui » : jour liturgique, couleur, accès au propre, lettre du mois
│   │   └── reader/                  # bibliothèque, sommaire, lecteur, recherche, signets et images pieuses (agent 09)
│   └── benchmark/                   # (V1) com.android.test : Macrobenchmark et générateur de baseline profile
├── content/                         # chaîne de contenu (agent 11)
│   ├── schema/                      # ★ contrat partagé : schéma SQL des paquets, JSON Schema du manifeste et de l'index, tests de conformité
│   ├── sources/                     # manifestes des sources (URL, licence, empreinte), scripts de collecte
│   ├── pipeline/                    # Python : nettoyage, normalisation, création de content.db, FTS5, empaquetage, signature
│   ├── packages/                    # définitions déclaratives des paquets (YAML)
│   ├── tests/
│   ├── raw/      (ignoré)           # déjà dans .gitignore
│   └── cache/    (ignoré)
├── docs/
│   ├── planning/                    # rapports 00 à 14 et PLAN.md
│   ├── adr/                         # décisions d'architecture courtes (minSdk, injection, format de paquet…)
│   └── privacy.md                   # politique de confidentialité (exigée par Play, utile ailleurs)
├── tools/                           # adb-wifi.sh, install-ci-apk.sh, check-permissions.sh
├── fastlane/metadata/android/fr-FR/ # descriptions, captures et journaux de version (lus par F-Droid et IzzyOnDroid)
├── .github/workflows/               # ci.yml, release.yml, (V1) content.yml, benchmark.yml
├── LICENSE
└── README.md
```

#### b) Modules et dépendances

```
app ─────────► feature:today, feature:reader, core:*        (seul module qui connaît tout le monde)
feature:reader ─► core:reading, core:content, core:data, core:designsystem, core:model
feature:today ──► core:liturgy, core:content, core:data, core:designsystem, core:model
core:reading ───► core:designsystem, core:model
core:content ───► core:model           (androidx.sqlite bundled, serialization, okhttp en V1)
core:data ──────► core:model           (room3 + KSP, datastore)
core:designsystem ► core:model         (LiturgicalColor → couleurs des signets)
core:liturgy ───► core:model           (JVM pur)
```

Règles :

1. **Une feature ne dépend jamais d'une autre.** Elle expose ses écrans avec des rappels (`onOpen(locator)`), et seul `app` possède la pile Nav3 et associe les clés aux écrans.
2. **Un module naît d'un besoin mesuré** : une API stable, une durée de compilation, des tests isolés. Le reste vit dans des *packages* Kotlin.
3. Les « livres » (Bible, Imitation, Lettres, Ignace, Prières) sont des **données**. Le comportement propre à un type de livre (versets, colonnes latin-français, jour de retraite) passe par des **rendus de blocs** déclarés dans `core:reading` et choisis selon le type de bloc du paquet.

#### c) Conventions Gradle (`build-logic`)

Quatre plugins, chacun de 20 à 40 lignes :

| Plugin | Rôle |
|---|---|
| `paroissien.jvm.library` | `kotlin("jvm")`, toolchain JDK 21, bytecode Java 17, JUnit 5 |
| `paroissien.android.library` | `com.android.library`, Kotlin intégré, `compileSdk 37`, `minSdk 26`, configuration des tests |
| `paroissien.android.compose` | Plugin `kotlin.compose`, BOM, Robolectric et Roborazzi |
| `paroissien.android.application` | Signature, versions, R8, ABI |

Paramètres retenus :

- `minSdk = 26`, `targetSdk = 36`, `compileSdk = 37`. Passer à `targetSdk 37` en V1, avant l'échéance Play d'août 2027 : l'adaptation aux grands écrans y devient obligatoire, ce que la double page en paysage impose de toute façon.
- `applicationId` : à trancher **maintenant** (§4). Pour rester cohérent avec `app.cratoolbox`, proposition : `app.paroissien`.

#### d) `gradle.properties` pour la VM (2 vCPU, 7,75 Gio et 2 Gio de swap)

```properties
# VM : 2 vCPU, ~7,75 Gio visibles + 2 Gio de swap. Budget de build ≤ 5 Gio, IDE fermé.
org.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g -XX:+UseParallelGC -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
org.gradle.workers.max=2
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
# Le daemon s'arrête seul après 20 min d'inactivité (en plus de ./gradlew --stop en fin de séance).
org.gradle.daemon.idletimeout=1200000
# Un seul JVM : la compilation Kotlin tourne dans le daemon Gradle (pas de daemon Kotlin séparé de 1 à 2 Go).
kotlin.compiler.execution.strategy=in-process
kotlin.code.style=official
android.nonTransitiveRClass=true
```

Dans le plugin de convention :

```kotlin
tasks.withType<Test>().configureEach {
    maxParallelForks = 1          // Robolectric en rendu natif est gourmand
    maxHeapSize = "1536m"
}
// Lint seulement en CI ; en local : ./gradlew :feature:reader:testDebugUnitTest
android { lint { checkReleaseBuilds = false } }
// Debug local : une seule ABI (le téléphone physique) pour un empaquetage plus rapide.
android { buildTypes { debug { ndk { abiFilters += "arm64-v8a" } } } }
```

En CI, on écrase ces valeurs depuis `~/.gradle/gradle.properties`, qui a priorité sur le fichier du projet : 4 workers et `-Xmx6g` sur un runner public.

#### e) Catalogue de versions (ajouts vérifiés en octobre 2026)

```toml
[versions]
# existants (CRA-Toolbox) : agp 9.4.1, kotlin 2.4.20, composeBom 2026.09.00, activityCompose 1.13.0,
# lifecycle 2.11.0, coreKtx 1.19.1, coroutines 1.11.0, serialization 1.11.0, okhttp 5.5.0, junit 4.13.2
navigation3 = "1.2.0"
lifecycleNav3 = "2.12.0-alpha04"   # à remplacer par la stable dès qu'elle sort (vérifier si 2.11.0 suffit)
room3 = "3.0.3"
sqlite = "2.7.1"
datastore = "1.2.1"
work = "2.12.0"          # V1
ksp = "2.3.12"           # vérifier la compatibilité avec Kotlin 2.4.20 au premier build
benchmark = "1.5.0"      # V1
profileinstaller = "1.4.1"
robolectric = "4.17"
roborazzi = "1.76.0"
junit5 = "5.x"           # dernière 5.x au moment du build

[libraries]
androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "navigation3" }
androidx-lifecycle-viewmodel-navigation3 = { module = "androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycleNav3" }
androidx-room3-runtime = { module = "androidx.room3:room3-runtime", version.ref = "room3" }
androidx-room3-compiler = { module = "androidx.room3:room3-compiler", version.ref = "room3" }
androidx-sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
androidx-work-runtime = { module = "androidx.work:work-runtime", version.ref = "work" }
androidx-profileinstaller = { module = "androidx.profileinstaller:profileinstaller", version.ref = "profileinstaller" }
androidx-benchmark-macro = { module = "androidx.benchmark:benchmark-macro-junit4", version.ref = "benchmark" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
roborazzi = { module = "io.github.takahirom.roborazzi:roborazzi", version.ref = "roborazzi" }
roborazzi-compose = { module = "io.github.takahirom.roborazzi:roborazzi-compose", version.ref = "roborazzi" }
roborazzi-junit-rule = { module = "io.github.takahirom.roborazzi:roborazzi-junit-rule", version.ref = "roborazzi" }

[plugins]
android-library = { id = "com.android.library", version.ref = "agp" }
android-test = { id = "com.android.test", version.ref = "agp" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
room3 = { id = "androidx.room3", version.ref = "room3" }
roborazzi = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }
baselineprofile = { id = "androidx.baselineprofile", version.ref = "benchmark" }
```

#### f) Choix techniques du MVP

| Domaine | Choix | Notes |
|---|---|---|
| Navigation | **Navigation 3** 1.2.0 | Pile de retour persistée dans DataStore (rouvrir le livre au bon endroit). Clés `@Serializable` dans `app` |
| Injection de dépendances | **Graphe manuel** (`AppGraph` créé dans `Application`, fabriques de ViewModel `viewModelFactory { initializer { … } }`) | Environ 100 lignes. Migration vers Metro possible plus tard |
| Préférences | DataStore Preferences 1.2.1 | Thème, police, rite, région, options |
| Données utilisateur | **Room 3.0.3** avec `BundledSQLiteDriver`, schéma exporté (`room3 { schemaDirectory(...) }`) pour les migrations | `setQueryCoroutineContext(Dispatchers.IO)` |
| Contenu | `core:content` ouvre chaque `content.db` en lecture seule (`immutable=1`) avec le même pilote. Requêtes SQL écrites à la main. **Recherche FTS5 avec `unicode61 remove_diacritics 2`**, colonne normalisée (*œ*, *æ*, apostrophes), préfixes | Le schéma vient de `content/schema/`. Un test de conformité tourne des deux côtés : Python et JVM |
| Paquets | **Intégrés** : prières, ordinaire de la messe, Imitation, Exercices et extraits bibliques, selon les droits. Fichiers `.paq` (zip : `manifest.json`, `content.db` et `images/`) dans `assets/packages/` avec `noCompress += "paq"`. Au premier lancement, extraction vers `noBackupFilesDir/packages/<id>/<version>/` **après vérification du SHA-256** | Le format du paquet est figé dès le MVP pour que la V1 n'ait qu'à ajouter le téléchargement |
| Calendrier | `core:liturgy` en JVM pur avec `java.time` (natif à partir de l'API 26) | Interface pour l'agent 05 : `fun day(date: LocalDate, rite: Rite, region: Region): LiturgicalDay` |
| Réseau | **Aucun.** Pas de permission `INTERNET` | Lettre du mois et paquets téléchargeables en V1 |
| Taille | **25 Mo au plus** pour l'APK universel du MVP (contrôle en CI). APK par ABI si l'on dépasse 30 Mo | Contrainte d'IzzyOnDroid |
| R8 | `isMinifyEnabled` et `isShrinkResources` en release, `proguard-android-optimize.txt` (obligatoire avec AGP 9) | Les règles de kotlinx.serialization sont fournies par la bibliothèque |

#### g) Signature et versions

- Clé de publication générée **hors de la VM de build** :

  ```sh
  keytool -genkeypair -keyalg RSA -keysize 4096 -validity 10000 -alias paroissien -keystore paroissien-release.jks
  ```

  Une copie dans le gestionnaire de mots de passe, une copie hors ligne. **Une clé perdue rend toute mise à jour impossible.**
- `versionName` est tiré du tag Git (`v0.1.0`) et `versionCode` est calculé (`major*10000 + minor*100 + patch`, ou un multiple de 10 suivi du numéro d'ABI si l'on découpe par ABI).
- Configuration lue depuis les variables d'environnement, sans rien dans le dépôt :

```kotlin
android {
    signingConfigs {
        create("release") {
            providers.environmentVariable("PAROISSIEN_KEYSTORE").orNull?.let { path ->
                storeFile = file(path)
                storePassword = providers.environmentVariable("PAROISSIEN_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("PAROISSIEN_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("PAROISSIEN_KEY_PASSWORD").get()
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.getByName("release").takeIf { it.storeFile != null }?.let { signingConfig = it }
        }
    }
}
```

#### h) CI GitHub Actions

`.github/workflows/ci.yml` (à chaque push et PR) :

```yaml
name: ci
on:
  push: { branches: [main] }
  pull_request:
  workflow_dispatch:
concurrency: { group: "${{ github.workflow }}-${{ github.ref }}", cancel-in-progress: true }
jobs:
  build:
    runs-on: ubuntu-24.04
    timeout-minutes: 40
    defaults: { run: { working-directory: android } }
    steps:
      - uses: actions/checkout@v7
      - uses: gradle/actions/wrapper-validation@v6
      - uses: actions/setup-java@v6
        with: { distribution: temurin, java-version: "21" }
      - uses: gradle/actions/setup-gradle@v6
        with: { cache-read-only: "${{ github.ref != 'refs/heads/main' }}" }
      - name: CI-sized Gradle
        run: |
          mkdir -p ~/.gradle
          printf 'org.gradle.workers.max=4\norg.gradle.jvmargs=-Xmx6g -XX:+UseParallelGC\n' >> ~/.gradle/gradle.properties
      - run: ./gradlew test testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug
      - name: Permissions allowlist
        run: ../tools/check-permissions.sh app/build/outputs/apk/debug/app-debug.apk ../docs/permissions-allowlist.txt
      - uses: actions/upload-artifact@v7
        with:
          name: paroissien-debug-${{ github.sha }}
          path: android/app/build/outputs/apk/debug/*.apk
          retention-days: 7              # 500 Mo d'artefacts max en dépôt privé
      - if: failure()
        uses: actions/upload-artifact@v7
        with: { name: screenshot-diffs, path: "android/**/build/outputs/roborazzi/**", retention-days: 7 }
```

`.github/workflows/release.yml` (sur tag `v*`) :

```yaml
name: release
on: { push: { tags: ["v*"] } }
permissions: { contents: write }
jobs:
  release:
    runs-on: ubuntu-24.04
    environment: release                 # secrets accessibles uniquement ici
    defaults: { run: { working-directory: android } }
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with: { distribution: temurin, java-version: "21" }
      - uses: gradle/actions/setup-gradle@v6
        with: { cache-disabled: true }   # build de release sans cache, pour la reproductibilité
      - run: echo "$KEYSTORE_B64" | base64 -d > "$RUNNER_TEMP/release.jks"
        env: { KEYSTORE_B64: "${{ secrets.KEYSTORE_B64 }}" }
      - run: ./gradlew assembleRelease
        env:
          PAROISSIEN_KEYSTORE: ${{ runner.temp }}/release.jks
          PAROISSIEN_KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          PAROISSIEN_KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          PAROISSIEN_KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
          PAROISSIEN_VERSION: ${{ github.ref_name }}
      - run: |
          cd app/build/outputs/apk/release
          mv app-release.apk "paroissien-${GITHUB_REF_NAME}.apk"
          sha256sum *.apk > SHA256SUMS
          gh release create "$GITHUB_REF_NAME" *.apk SHA256SUMS --generate-notes
        env: { GH_TOKEN: "${{ github.token }}" }
```

#### i) Boucle quotidienne sans faire souffrir la VM

```sh
# 1) Une seule fois : associer le téléphone (Android 11+, Options développeur > Débogage sans fil)
adb pair 192.168.1.42:37199          # code à 6 chiffres affiché sur le téléphone
adb connect 192.168.1.42:41235       # port de CONNEXION (différent du port d'association)

# 2) Tests rapides en local (JVM pur : quelques secondes)
./gradlew :core:liturgy:test :core:content:testDebugUnitTest
./gradlew :feature:reader:recordRoborazziDebug   # « aperçus » PNG lisibles sans Android Studio

# 3) APK : laisser la CI construire, puis installer l'artefact
gh run download --name "paroissien-debug-$(git rev-parse HEAD)" -D /tmp/apk && adb install -r /tmp/apk/*.apk

# 4) En fin de séance (règle de la VM)
./gradlew --stop
```

Ces commandes vont dans `tools/adb-wifi.sh` et `tools/install-ci-apk.sh`. Un `./gradlew :app:installDebug` local reste possible quand on itère vite, **sans émulateur**.

#### j) Tests au MVP

| Niveau | Contenu | Où |
|---|---|---|
| Calendrier | **Tables de référence** : dates de Pâques (1583-4099), comparées à l'algorithme de Meeus-Butcher et à des sources publiées. Jours liturgiques complets (rang, couleur, préséance) pour 3 à 5 années types, Pâques précoce et tardive comprises, comparés à un Ordo ou à des sources de référence (agent 05). **Invariants** : un seul jour par date, couleur autorisée pour le rite, octaves et vigiles cohérentes | `core:liturgy`, JUnit 5 paramétré, sur la VM et en CI |
| Contenu | Ouverture d'un **paquet de test** produit par `content/pipeline` (fixture versionnée). Recherche FTS5 avec et sans accent, *cœur* trouvé par *coeur*, préfixes, sommaire. Vérification SHA-256 et rejet d'un paquet altéré | `core:content`, de préférence en JVM si `sqlite-bundled` fournit ses binaires natifs pour hôte Linux (à vérifier), sinon Robolectric |
| Données utilisateur | DAO Room 3 en mémoire et migrations testées à partir des schémas exportés | `core:data` |
| Interface | **Roborazzi** sur les écrans clés : Aujourd'hui, bibliothèque, page de lecture, page avec rubriques rouges et lettrine. Trois thèmes, portrait téléphone (`w390dp-h844dp`), paysage en double page (`w844dp-h390dp`), tablette, échelle de police 1,0 et 1,5 | `feature:*`, Robolectric en rendu natif, sdk 35. Références PNG versionnées |
| Navigation | Parcours Compose sous Robolectric : ouvrir un livre, poser un signet, rouvrir au signet | `app` |

Le **shader AGSL** n'est pas fidèlement rendu par Robolectric (risque §5). Les captures portent sur des pages **au repos**. Le tourne-page se vérifie sur le téléphone, puis par Macrobenchmark en V1.

#### k) Vie privée au MVP

- Le manifeste ne demande **aucune permission**. `docs/permissions-allowlist.txt` est vide, et la CI échoue si une dépendance en ajoute une par fusion de manifeste (`aapt2 dump permissions`).
- `android:allowBackup="true"` avec `dataExtractionRules` : **transfert d'appareil à appareil autorisé**, sauvegarde cloud limitée à `user.db` et aux préférences, à trancher (§4). Les paquets (`noBackupFilesDir`) ne sont jamais sauvegardés.
- **Export et import JSON** des signets, annotations, images et positions par le sélecteur de fichiers. C'est indispensable pour changer de canal de distribution.
- Aucune dépendance Google propriétaire. Analyse de l'APK publié par εxodus ou par l'outil d'IzzyOnDroid.

### 3.2 V1

1. **Paquets téléchargeables.**
   - `core:content` ajoute `PackageRepository.refresh()`.
   - On télécharge `index.json` et `index.json.sig`, on vérifie la signature **ECDSA P-256** (deux clés publiques intégrées), on compare les versions et le `minAppVersion`.
   - Les paquets sont téléchargés par un `CoroutineWorker` (contrainte réseau, au choix sur réseau non limité), puis le SHA-256 est vérifié.
   - Installation atomique : extraction dans `…/<id>/<version>.tmp`, `rename`, mise à jour du registre dans `user.db`, suppression de l'ancienne version.
   - **Import d'un `.paq` depuis un fichier** (SAF) : pour les utilisateurs hors ligne et les tests.
   - Hébergement : dépôt `paroissien-content`, tags `content-AAAA.MM`, assets de GitHub Release. `content.yml` construit, signe et publie (agent 11).

   Format de l'index, à figer avec l'agent 11 :

   ```json
   { "format": 1, "generated": "2026-10-11T00:00:00Z",
     "packages": [ { "id": "missel-1962", "version": 3, "schema": 1, "minApp": 10200,
                     "title": "Missel romain 1962 (latin-français)", "lang": ["la","fr"],
                     "license": "domaine public", "size": 11534336,
                     "sha256": "…", "url": "https://github.com/…/releases/download/content-2026.10/missel-1962-3.paq" } ] }
   ```

2. **Lettre du mois** : `PeriodicWorkRequest` hebdomadaire avec une marge souple, selon la décision §4.6, et une notification facultative (canal « Lettre du mois »).
3. **Angélus** : `AlarmManager.setWindow()` à 6 h, 12 h et 18 h (fenêtre de 10 minutes, sans permission). Récepteurs `BOOT_COMPLETED`, `TIME_SET` et `TIMEZONE_CHANGED`. `POST_NOTIFICATIONS` est demandée à l'activation. Canal propre avec le son de la cloche. Une option « heure exacte » demande `SCHEDULE_EXACT_ALARM`, selon la décision §4.9.
4. **Mode méditation** : `ACCESS_NOTIFICATION_POLICY` et une `AutomaticZenRule` (comportement d'Android 15 et plus), avec retour à l'état précédent à la fin du minuteur. Point d'interface avec l'agent 08.
5. **Nouveaux modules** quand les écrans existent :
   - `feature:missal` : ordinaire et propre, sélection par jour à partir de `core:liturgy`, colonnes latin-français ;
   - `feature:retreat` : Exercices de saint Ignace en retraite guidée, progression enregistrée dans `user.db` ;
   - `feature:meditation`, tiré de `app`.
6. **Module `:benchmark`** (`com.android.test`, plugin `androidx.baselineprofile`, `targetProjectPath = ":app"`) :
   - `StartupBenchmark` : démarrage à froid jusqu'à « Aujourd'hui » ;
   - `PageTurnBenchmark` : 50 pages tournées par glissement, `FrameTimingMetric` (`frameDurationCpuMs` P50, P90 et P99, `frameOverrunMs`) en AGSL et en repli ;
   - `BaselineProfileGenerator` : démarrage, ouverture d'un livre, 20 pages tournées, recherche.

   On les lance **sur le téléphone physique en adb Wi-Fi, en API 33 et plus, sans root**, après `./gradlew --stop` des autres builds. Le profil est versionné (`app/src/release/generated/baselineProfiles/`) et `profileinstaller` est ajouté. Le workflow facultatif `benchmark.yml` (déclenché à la main) génère le profil sur un émulateur KVM `aosp` en CI. **Les mesures de fluidité ne comptent que sur le téléphone.**
7. **Distribution** : fiche IzzyOnDroid, métadonnées fastlane en fr-FR et enregistrement de développeur Android (Android Developer Console) **avant l'application mondiale de 2027**. Le compte gratuit de distribution limitée (20 appareils) suffit pour une bêta paroissiale.
8. **targetSdk 37** et vérification de la reproductibilité du build release (deux builds CI, puis `diffoscope`), en préparation de F-Droid.
9. **Synthèse vocale en arrière-plan**, si l'agent 06 la retient : Media3 1.11.1 avec `MediaSessionService` et `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.

### 3.3 Plus tard

- **Metro** (1.4.x) si le graphe manuel dépasse une trentaine de liaisons ou si l'on ajoute des portées (une retraite en cours, par exemple).
- **F-Droid principal**, si les droits du contenu intégré le permettent (agent 12) et si le build est reproductible (APK signé par nous via `Binaries`).
- **Play Store** : AAB, téléversement de **notre** clé de signature, et Play Asset Delivery pour les gros paquets si l'on veut éviter notre hébergement sur ce canal. Prévoir les 14 jours de test fermé avec 12 testeurs, la fiche « Sécurité des données » et la politique de confidentialité.
- **APK par ABI** (`splits.abi`, avec arm64-v8a, armeabi-v7a et x86_64) si l'APK universel dépasse 30 Mo.
- **Vérification des dépendances Gradle** (`gradle/verification-metadata.xml`) pour la chaîne d'approvisionnement.
- **Widget « Aujourd'hui »** (Glance 1.2.0) affichant le jour liturgique et sa couleur.
- **KMP** : `core:model` et `core:liturgy` deviennent multiplateformes (passage de `java.time` à `kotlinx-datetime`), et Room 3 et `sqlite-bundled` sont déjà KMP. Une version iOS resterait alors envisageable sans réécrire le cœur.

### 3.4 Points d'interface avec les autres agents

| Agent | Interface |
|---|---|
| 01 Clairval | Mode de récupération de la lettre du mois : sur l'appareil depuis clairval.com, ou par notre chaîne (§4.6). Taille de l'archive, qui décide si elle va dans l'APK ou en téléchargement |
| 02, 03 et 04 (Ignace, Missel, Bible et Imitation) | Taille et droits de chaque paquet. Types de blocs nécessaires (verset, rubrique, colonnes latin-français, répons) |
| 05 Calendrier | API de `core:liturgy` en JVM pur, sans dépendance Android. Énumération `LiturgicalColor` partagée avec `core:designsystem` et les signets. Tables de référence pour les tests |
| 06 Moteur de lecture | Module `core:reading`. **Ancre stable** (paquet, bloc et décalage). Normalisation de la recherche (*œ*, *æ*, accents) partagée avec le pipeline. Synthèse vocale et service de premier plan |
| 07 Tourne-page | AGSL **sur l'API 33 et plus seulement**, repli obligatoire en dessous si minSdk 26. Robolectric ne rend pas le shader. Critères Macrobenchmark (P90 sous 16 ms à 60 Hz) |
| 08 UX | Navigation 3 (pile, scènes adaptatives), thèmes, `AutomaticZenRule` pour le DND, demandes de permission dans leur contexte |
| 09 Signets et images | Schéma Room de `user.db` (signet : couleur et ancre ; image : identifiant et ancre). Les images pieuses sont livrées dans les paquets (`images/*.webp`) |
| 11 Pipeline | **`content/schema/` est le contrat** : schéma SQL, `PRAGMA user_version`, tokenizer FTS5, format `.paq`, `index.json` signé ECDSA P-256 et hébergement |
| 12 Juridique | Licence du code (logiciel libre nécessaire pour IzzyOnDroid et F-Droid). Droits sur chaque paquet (intégrable ? redistribuable ?). Politique de confidentialité. Enregistrement de développeur |
| 13 Benchmark | Rien de bloquant. Les tailles d'APK des applications comparables seraient utiles |
| 14 Prières et extensions | Les extensions sont des **paquets**, pas des modules de code, sauf si elles ont une interface propre (le chapelet interactif, par exemple, pourrait devenir une feature) |

---

## 4. Décisions à soumettre à l'utilisateur

1. **minSdk** :
   - **26, recommandé** : environ 98 % du parc, avec un tourne-page de repli sous l'API 33 ;
   - 29 : environ 91 %, moins de cas à tester ;
   - 33 : environ 58 % fin 2025, peut-être 65-70 % aujourd'hui. Le code est le plus simple (AGSL partout, Ed25519 natif), mais un paroissien sur trois environ resterait exclu.
2. **`applicationId` définitif**, par exemple `app.paroissien`. Il est **impossible à changer** après la première diffusion, même si le nom de l'application change.
3. **Dépôt public et licence libre ?**
   - Public : CI gratuite (4 vCPU, 16 Go), condition d'IzzyOnDroid et de F-Droid.
   - Privé : 2 000 minutes par mois et 500 Mo d'artefacts.
   - Le choix de la licence (GPL-3.0, Apache-2.0…) relève de l'agent 12.
4. **Canaux de distribution et ordre** :
   - **recommandé** : GitHub Releases, puis IzzyOnDroid, puis (plus tard) F-Droid et/ou Play ;
   - dans tous les cas, s'enregistrer comme développeur Android avant 2027 : compte gratuit limité à 20 appareils, ou compte complet à environ 25 $ avec pièce d'identité.
5. **Contenu au MVP** : tout intégré (APK plus lourd, zéro réseau) ou **noyau intégré et paquets téléchargeables en V1 (recommandé)** ? Hébergement des paquets : GitHub Releases (recommandé, gratuit, mais GitHub voit les adresses IP), Codeberg, ou un hébergement européen à soi (OVH ou Scaleway).
6. **Lettre du mois** :
   - (a) **récupérée sur l'appareil depuis clairval.com** : rien n'est redistribué, mais l'analyseur HTML vit dans l'application et elle dépend d'un site tiers ;
   - (b) récupérée par notre chaîne et republiée dans un paquet : plus robuste, mais il faut **l'accord de Clairval**. À arbitrer avec les agents 01 et 12.
7. **Permission INTERNET au MVP** : **non, recommandé** (preuve forte de vie privée), ou oui pour livrer la lettre du mois dès le MVP.
8. **Signature des versions** : **en CI avec des secrets protégés par l'environnement `release` (recommandé pour le confort)**, ou en local avec `apksigner` sur un APK non signé produit par la CI. La seconde voie est très légère pour la VM, et la clé ne quitte jamais l'utilisateur.
9. **Angélus** : fenêtre de 10 minutes sans permission (recommandé), ou heure exacte sur option avec `SCHEDULE_EXACT_ALARM`, que l'utilisateur doit accorder.
10. **Sauvegarde** : sauvegarde cloud Android **désactivée**, transfert d'appareil à appareil et export manuel (recommandé pour la vie privée) ; ou sauvegarde cloud activée (plus pratique, mais elle passe par Google Drive).
11. **Injection de dépendances** : **manuelle (recommandé)**, Metro, ou Hilt.

---

## 5. Risques et inconnues

| Risque | Gravité | Atténuation |
|---|---|---|
| **Room 3 est jeune** (3.0.0 en juillet 2026). Les arguments du tokenizer de `@Fts5` (`remove_diacritics`) n'ont pas pu être vérifiés dans l'API de référence | Moyenne | Les index FTS de **contenu** sont créés par le pipeline, pas par Room. Côté utilisateur, FTS n'est pas indispensable. Repli : Room 2.8 avec pilote, ou du SQL brut |
| **RAM de la VM : 7,75 Gio visibles et non 10 Go.** Robolectric en rendu natif, R8 et lint sont gourmands | Haute | Kotlin dans le processus, `-Xmx3g`, un seul fork de test, lint et release en CI, `--stop` systématique, IDE fermé pendant les builds |
| **Enregistrement de développeur Android en 2027** : les installations d'APK hors magasin d'un développeur non enregistré imposeront le « parcours avancé » (24 h d'attente). Le comportement exact pour F-Droid n'est pas encore garanti par Google | Haute pour la diffusion | S'enregistrer en 2026-2027 (compte limité gratuit pour la bêta). Suivre le calendrier publié par Google |
| **Droits du contenu et F-Droid** : intégrer des textes protégés (lettres de Clairval, traductions récentes) déclenche *NonFreeAssets* ou un refus, et peut poser un problème juridique | Haute | Seul le domaine public est intégré. Le reste passe en paquets téléchargés, après décision de l'agent 12 |
| **Shader AGSL non testable sous Robolectric** et VM sans GPU : régressions visuelles du tourne-page invisibles en CI | Moyenne | Captures sur des pages au repos. Macrobenchmark et contrôle manuel sur le téléphone. Les paramètres du shader font l'objet de tests unitaires |
| **Repli du tourne-page sous l'API 33** : du travail en plus pour l'agent 07 | Moyenne | Repli simple (`graphicsLayer.rotationY` ou glissement), pas une copie du rendu AGSL |
| **Compatibilité de l'outillage avec AGP 9.4.1 et Kotlin 2.4.20** : Roborazzi ne mentionne pas AGP 9, KSP 2.3.12 ne donne pas de tableau de compatibilité Kotlin, Robolectric 4.17 ne prend pas en charge le SDK 37 | Moyenne | Premier jalon : un « squelette vert » (tous les plugins, un test par module) **en CI**, avant toute fonctionnalité |
| **`lifecycle-viewmodel-navigation3`** cité en 2.12.0-alpha04 par la documentation | Faible | Utiliser la stable si elle existe, sinon des ViewModels au niveau de l'Activity plus un état par écran |
| **Taille de l'APK** : SQLite embarqué (environ 1,5 Mo par ABI), polices, contenu. IzzyOnDroid plafonne à 30 Mo | Moyenne | Contrôle de taille en CI, paquets téléchargeables, APK par ABI si besoin |
| **Pages de 16 Ko** : la bibliothèque native de `sqlite-bundled` doit être alignée (Play l'exigera dès le 1er février 2027) | Faible | `zipalign -c -P 16` dans la CI de release |
| **Stabilité des ancres** entre versions d'un paquet : un signet orphelin après une mise à jour du contenu | Haute (fidélité des signets) | Identifiants de bloc stables imposés par le schéma et une table `moved(old_id, new_id)` dans les paquets (agent 11) |
| **Reproductibilité** pour F-Droid : le `baseline.prof` et les horodatages peuvent rendre le build non déterministe (non vérifié pour AGP 9.4) | Faible à moyenne | Vérifier en V1 avec deux builds CI et `diffoscope` |
| **Clé de signature perdue ou divulguée** | Haute | Deux sauvegardes hors ligne, secrets réservés à l'environnement `release`, ou signature locale |
| **mDNS à travers le NAT de la VM** : la reconnexion automatique d'adb en Wi-Fi peut échouer | Faible | `adb connect IP:PORT` explicite, ou réseau en pont |
| **Données de part de marché** datées de décembre 2025, avec une estimation pour octobre 2026 | Faible | Relire le sélecteur de minSdk d'Android Studio au moment de décider |
| Le **budget de recherche web** était épuisé en fin d'étude : quelques points restent « à vérifier » (binaires JVM de `sqlite-bundled` pour les tests sur hôte, `TextAlign.Justify` et API 26, cycle exact d'IzzyOnDroid) | Faible | À vérifier au premier squelette |

---

## 6. Sources

Consultées le 11 octobre 2026.

**Projet existant**

- Projet CRA-Toolbox lu localement : `/home/joseph/Work/Projects/CRA-Toolbox/android` (`libs.versions.toml`, `gradle.properties`, `app/build.gradle.kts`, `core/build.gradle.kts`, `ScreensTest.kt`, `AndroidManifest.xml`, `README.md`).

**SDK et parts de marché**

- Répartition des versions d'Android (données Google du 1er déc. 2025) : https://composables.com/android-distribution-chart et https://heise.de/-11162154
- Exigence de targetSdk sur Google Play : https://developer.android.com/google/play/requirements/target-sdk
- Android 17, orientation et redimensionnement : https://developer.android.com/blog/posts/prepare-your-app-for-the-resizability-and-orientation-changes-in-android-17
- Changements de comportement d'Android 15 (DND et `AutomaticZenRule`, délai `dataSync`) : https://developer.android.com/about/versions/15/behavior-changes-15
- Pages de 16 Ko : https://developer.android.com/guide/practices/page-sizes

**Bibliothèques Jetpack**

- Navigation 3, notes de version : https://developer.android.com/jetpack/androidx/releases/navigation3
- Navigation 3, prise en main (dépendances) : https://developer.android.com/guide/navigation/navigation-3/get-started
- Canal stable AndroidX : https://developer.android.com/jetpack/androidx/versions/stable-channel
- Room 3 : https://developer.android.com/jetpack/androidx/releases/room3
- androidx.sqlite : https://developer.android.com/jetpack/androidx/releases/sqlite et https://developer.android.com/kotlin/multiplatform/sqlite
- Drapeaux de compilation de `sqlite-bundled` : https://raw.githubusercontent.com/androidx/androidx/androidx-main/sqlite/sqlite-bundled/build.gradle
- DataStore : https://developer.android.com/jetpack/androidx/releases/datastore
- WorkManager : https://developer.android.com/jetpack/androidx/releases/work
- Alarmes exactes et inexactes : https://developer.android.com/develop/background-work/services/alarms/schedule
- Benchmark et baseline profiles : https://developer.android.com/jetpack/androidx/releases/benchmark et https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile

**Build**

- AGP 9.0, notes de version : https://developer.android.com/build/releases/agp-9-0-0-release-notes
- Migration vers le Kotlin intégré : https://developer.android.com/build/migrate-to-built-in-kotlin
- KSP : https://github.com/google/ksp/releases
- Kotlin 2.4.20 : https://blog.jetbrains.com/kotlin/2026/09/kotlin-2-4-20-released/
- Configuration cache de Gradle : https://docs.gradle.org/current/userguide/configuration_cache.html et https://docs.gradle.org/9.0.0/release-notes.html

**Injection de dépendances**

- Metro, compatibilité : https://zacsweers.github.io/metro/latest/compatibility/
- Metro, journal des versions : https://zacsweers.github.io/metro/latest/changelog/
- Metro, ViewModel et Compose : https://zacsweers.github.io/metro/latest/metrox-viewmodel-compose/
- Migration de Square vers Metro : https://engineering.block.xyz/blog/metro-migration-at-square-android
- Dagger et Hilt : https://github.com/google/dagger/releases
- Koin et Navigation 3 : https://insert-koin.io/docs/reference/koin-compose/navigation3

**Tests**

- Roborazzi : https://github.com/takahirom/roborazzi/releases et https://raw.githubusercontent.com/takahirom/roborazzi/main/README.md
- Paparazzi : https://github.com/cashapp/paparazzi/releases
- Compose Preview Screenshot Testing : https://developer.android.com/studio/preview/compose-screenshot-testing

**Distribution et outillage**

- adb en Wi-Fi : https://developer.android.com/tools/adb
- Enregistrement des développeurs Android :
  - https://developer.android.com/developer-verification
  - https://android.gadgethacks.com/news/google-android-sideloading-restrictions-sept-30-explained/
  - https://techjournal.org/android-sideloading-developer-verification
- F-Droid :
  - critères d'inclusion : https://f-droid.org/docs/Inclusion_Policy/
  - anti-fonctionnalités : https://f-droid.org/docs/Anti-Features/
  - builds reproductibles : https://f-droid.org/docs/Reproducible_Builds/
- Critères d'inclusion d'IzzyOnDroid : https://izzyondroid.org/docs/general/AppInclusionPolicy/
- Google Play :
  - test fermé pour les nouveaux comptes personnels : https://support.google.com/googleplay/android-developer/answer/14151465
  - limites de taille : https://support.google.com/googleplay/android-developer/answer/9859372
- GitHub Actions :
  - runners hébergés : https://docs.github.com/en/actions/reference/runners/github-hosted-runners
  - facturation : https://docs.github.com/en/billing/concepts/product-billing/github-actions
  - accélération KVM : https://github.blog/changelog/2024-04-02-github-actions-hardware-accelerated-android-virtualization-now-available/
  - report de la tarification 2026 : https://github.blog/changelog/2025-12-16-coming-soon-simpler-pricing-and-a-better-experience-for-github-actions/
- Versions des actions :
  - https://github.com/gradle/actions/releases
  - https://github.com/actions/checkout/releases
  - https://github.com/actions/setup-java/releases
  - https://github.com/actions/upload-artifact/releases
