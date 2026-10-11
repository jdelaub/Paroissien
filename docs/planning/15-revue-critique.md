# 15 — Revue critique du plan directeur (red team)

> Agent 15, revue du 11 octobre 2026 portant sur `PLAN.md` v0.1, confronté à `00-vision.md` et aux rapports 01 à 14 (lus de façon ciblée).
> Conventions : **[V]** = vérifié par moi dans cette session (requête, fichier ou binaire inspecté) ; **[S]** = estimation ou raisonnement du relecteur.
> Gravités : **Bloquant** = à corriger avant la phase 0 ; **Majeur** = à corriger avant la phase concernée, sinon reprise coûteuse ; **Mineur** = précision ou cohérence.

---

## 1. Verdict global

1. Le plan est **fidèle** aux rapports sur les faits (corpus Clairval, couverture de Divinum Officium, glyphes, VM, AELF, Barroux) ; je n'ai trouvé aucun chiffre grossièrement faux recopié, mais plusieurs nuances perdues.
2. En revanche, il **additionne les MVP des rapports au lieu de les arbitrer** : le périmètre représente environ 6 à 9 mois de travail effectif pour un développeur seul [S], et le « en parallèle » des phases 1, 2 et 3 est illusoire.
3. **Deux bloquants**, faciles à lever : la CI d'un dépôt public publierait les contenus protégés dans ses artefacts, et le téléphone de test (niveau d'API, fréquence d'écran) n'est pas identifié alors que tout le spike en dépend.
4. Plusieurs **affirmations porteuses sont fausses ou fragiles** : `sqlite-bundled` 2.7.1 embarque SQLite **3.50.1** et non 3.53.4 ; l'argument « exclusion de gestes » ne justifie pas le minSdk 29 ; le pliage de recherche ignore les tirets conditionnels que le plan fait insérer.
5. Avec la scission du spike, des critères de porte chiffrés, huit ADR courts et une coupe du MVP en deux portes (§2.1), **le plan devient exécutable dès demain**.

---

## 2. Corrections à apporter au plan

| # | Gravité | Section de PLAN.md | Problème | Correction proposée |
|---|---|---|---|---|
| B1 | **Bloquant** | §5.1 (4. Dépôt et licence) ; §4, phase 0 (CI) | Le plan combine un dépôt **public** pour le code, des contenus protégés dans un dépôt privé et des APK construits par la CI avec un « APK en artefact ». Or, sur un dépôt public, **tout utilisateur GitHub connecté peut télécharger les artefacts** [V, docs GitHub]. Un APK contenant Clairval ou le français du Barroux, construit par la CI publique, serait donc une rediffusion publique : c'est précisément le risque n° 1 du plan. | Créer deux saveurs Gradle : `domainePublic` (seule construite par la CI publique) et `personnel` (construite **uniquement** dans la CI du dépôt privé `paroissien-content` ou en local, artefacts conservés 1 jour, jamais attachés à une Release). La chaîne refuse tout paquet `usage-privé-seulement` dans une saveur publique (règle déjà écrite au rapport 12, §3, absente du plan). Ajouter en CI publique un contrôle qui liste les paquets embarqués et échoue si l'un d'eux n'est pas `domaine-public` ou `licence-libre`. Noter le coût : CI privée sur 2 vCPU et 2 000 min/mois (10). |
| B2 | **Bloquant** | §4, phases 0 et 1 ; §5.2 (Tourne-page) | Le spike est conçu autour d'AGSL (API ≥ 33) et d'un budget à 120 Hz, mais **aucun rapport ne précise le téléphone de l'utilisateur** : modèle, niveau d'API, fréquence d'écran, GPU. Si l'appareil est en API < 33, l'étape ① est impossible ; s'il est haut de gamme, la mesure « milieu de gamme » ne vaut rien. | En phase 0, inventorier l'appareil (`adb shell getprop ro.build.version.sdk`, `ro.product.model`, `dumpsys display` pour la fréquence, `getprop ro.hardware.egl`) et consigner le résultat dans l'ADR 002. Si l'API est < 33, se procurer un appareil API ≥ 33 de milieu de gamme ou inverser le spike (glissement d'abord). Désigner un **second appareil de référence** pour la performance. |
| M1 | Majeur | §4, phase 1 ; §0 (« 2 à 3 semaines ») | Le spike est sous-dimensionné. Le prototype du rapport 07 compte à lui seul 3 étapes de 4-6, 3-5 et 4-6 jours, soit 11 à 17 jours ; le spike du rapport 06 (paginateur, lettrine, rangées bilingues, TalkBack) en demande 2 à 3 semaines de plus. Le plan fusionne les deux en « 2 à 3 semaines » et y ajoute Macrobenchmark, que le rapport 06 place en V1. | Scinder en **1a** « feuille et encre » (07 ① et ②, 2 à 3 semaines, mini-porte : pli fluide et typographie sans glyphe manquant) et **1b** « livre » (paginateur de 06, 07 ③, TalkBack, 2 à 3 semaines), puis la porte A, soit **4 à 6 semaines**. Mesurer avec `dumpsys gfxinfo <pkg> framestats` et JankStats (07) ; reporter Macrobenchmark en V1, car il exige un module de test et une exécution d'instrumentation depuis l'hôte. |
| M2 | Majeur | §4, porte A | La porte A n'a **aucun critère mesurable**, et les seuils divergent entre les rapports : P95 < 8,3 ms à 120 Hz (07), P90 < 16 ms à 60 Hz (10), « < 8 ms par image » (plan). Rien ne dit quand choisir Readium plutôt qu'un repli « natif sans pli ». | Adopter les critères du §5.2 ci-dessous, avec une **règle de décision par composant** : si le pli échoue, natif et glissement ; si la pagination ou la typographie échoue après une semaine de corrections, Readium. Le seuil est exprimé **en fonction de la fréquence de l'écran** (P95 ≤ période de rafraîchissement). |
| M3 | Majeur | §5.2 (Césure latine) ; §2, principe 3 ; §1 (pliage) | Le plan fait insérer des **U+00AD dans le texte stocké**. Or : (1) la spécification du pliage du rapport 11 (§3.7) **ne retire pas U+00AD**, et `unicode61` le traite comme un séparateur, donc « Dó­mi­nus » est indexé en trois jetons et « dominus » ne le trouve plus [S, d'après les règles de catégories d'unicode61] ; (2) les ancres sont exprimées en points de code du `plainText`, donc tout changement de motifs de césure modifie la `rev` de chaque bloc latin et force un ré-ancrage ; (3) TTS et copier-coller doivent nettoyer. 06 et 07 le signalent, mais rien n'est tranché dans le plan. | Définir un **texte canonique sans U+00AD** (NFC), qui sert aux identifiants, à la `rev`, aux ancres, à FTS et au TTS. Stocker les points de césure à part (tableau de décalages par bloc) ou les calculer **à l'exécution** en Kotlin (Liang, environ 100 lignes, motifs `la-x-liturgic` amont du 04/02/2019, MIT [V]). Ajouter une étape 0 au pliage : retirer U+00AD, U+200B et U+2060, avec le vecteur `Dó­mi­nus → dominus`. |
| M4 | Majeur | §1 (Recherche plein texte) ; §5.2 (Paquets) | Les rapports 10 et 11 attribuent SQLite **3.53.4** à `sqlite-bundled` 2.7.1 en lisant le `build.gradle` de la branche principale. Le binaire **publié** 2.7.1 embarque **SQLite 3.50.1** (source id du 06/06/2025), avec FTS5 et sans ICU [V]. Conséquences : la règle du rapport 11 (« version de la chaîne ≤ version de l'app ») est violée par l'épinglage APSW 3.53.4, et l'argument du rapport 10 sur les correctifs FTS5 de la 3.53.2 ne vaut pas pour la version livrée. | Épingler **APSW 3.50.1.x** dans `content/`. Ajouter un test JVM qui ouvre chaque paquet avec `sqlite-bundled-jvm` 2.7.1 (natifs linux_x64 présents [V]) et vérifie `sqlite_version()` et une requête FTS5. Ne jamais passer la saisie brute de l'utilisateur à `MATCH` : construire la requête (jetons, préfixes, guillemets). Suivre la 2.8.x pour monter en version. |
| M5 | Majeur | §3.2 ; §4 (tailles, « faisable en parallèle ») | Le MVP réunit les éléments les plus ambitieux de chaque rapport. Le rapport 06 met le vis-à-vis bilingue, la sélection et le ré-ancrage en V1 ; le rapport 13 y met la recherche, le mode méditation et les images personnelles ; le rapport 05 y met le lectionnaire 1969. Un développeur seul ne mène pas les phases 1, 2 et 3 en parallèle, mais les entrelace. Mon estimation [S] : phase 0 en 1-2 sem., 1 en 4-6, 2 en 4-6, 3 en 4-7 (05 R10 : 2-4 sem. pour 1962 et 1,5-3 pour 1969, plus les oracles), 4 en 6-10, 5 en 5-8, soit **24 à 39 semaines** avant la porte B. | Adopter la coupe du §2.1 : **deux portes d'usage réel** (B1 « livre de lecture », puis B2 « missel »), et un chemin critique séquentiel avec des estimations en semaines. Retirer la mention « faisable en parallèle de 1 » ou la remplacer par « entrelacée : les tâches de contenu comblent les temps d'attente de la CI ». |
| M6 | Majeur | §3.2 (Aujourd'hui) ; §4, phase 3 | « Messe du jour » et « Évangile du jour » sont livrés en phase 3, alors qu'ils dépendent du **missel (phase 5)** et, pour 1969, d'un **lectionnaire sans source libre identifiée** (04, risque [S] ; 05 le place en V1, avec en plus une table de versification à construire à la main). | MVP « Aujourd'hui » = fête, rang, couleur, commémoraisons et lecture suggérée. L'Évangile du jour 1962 arrive avec la phase 5, depuis le Propre de Divinum Officium. Le lectionnaire 1969 passe en V1, explicitement. |
| M7 | Majeur | §3.2 (lettre suggérée) ; §4, phase 6 | La carte « lettre suggérée » est au MVP, mais Clairval n'arrive qu'en phase 6, **après** la porte B. La vision cite Clairval en premier module, et le rapport 01 le recommande au MVP. | Si le scénario A est retenu, ingérer Clairval (français, environ 25 requêtes par l'API REST [V]) en phase 2, dans le dépôt privé et la saveur `personnel` (B1). Sinon, retirer la carte du MVP. |
| M8 | Majeur | §4, phase 4 (ruban automatique) | Le 7ᵉ ruban « messe du jour » est placé en phase 4, qui ne dépend que de la porte A et de la phase 2. Or il lui faut la couleur du jour (phase 3) et l'emplacement du Propre (phase 5). | Déplacer le ruban automatique en phase 5. La phase 4 ne garde que les rubans manuels. |
| M9 | Majeur | §0 (« Le reste du MVP s'appuie sur le domaine public ») ; §3.1 (Missel) | La **traduction française de l'Ordinaire** dans Divinum Officium vient de Sancta Missa et d'introibo.fr, sous une licence inconnue et avec des sites hors ligne (12, §2.2.3 ; 03). Le plan n'en parle pas : le MVP « Ordinaire » n'a donc aucune source française sûre. | Ajouter ce point aux risques juridiques. En phase 2, prévoir un paquet « Ordinaire 1962 » latin et français tiré d'un paroissien du domaine public (Mame 1890, rapport 14), ajusté aux changements de 1955 à 1962, par exemple la suppression du second Confiteor (1960) et saint Joseph au Canon (1962), avec relecture. |
| M10 | Majeur | §6 (Refus du Barroux) | La parade « dimanches et fêtes traduits depuis des paroissiens du domaine public » laisse des trous. Ces paroissiens datent de 1860 à 1919, avant le Christ-Roi (1925), la nouvelle messe de l'Assomption (1950), le Cœur immaculé (1944), Marie-Reine (1954), saint Joseph artisan (1955), la Semaine sainte de 1955 et les rubriques de 1960. Le rapport 12 (§2.2.5) parle de « lacunes 1955-1962 » à traduire soi-même. | Faire produire par la chaîne la **liste des formulaires 1962 sans source française du domaine public**, chiffrer leur traduction (sous CC BY-SA, propriété de l'utilisateur) et prévoir le repli « latin seul » pour ces formulaires. |
| M11 | Majeur | §4, phase 5 (« réponse du Barroux ») | La phase 5 attend une réponse tierce **sans délai butoir**, alors que le rapport 12 admet l'usage privé du français de DO en scénario A. | En scénario A, utiliser le français de DO dans la saveur `personnel` dès la phase 5. En B ou C, fixer un délai de 8 semaines après l'envoi, puis appliquer M10. |
| M12 | Majeur | §5.2 (minSdk, Gestes) ; §6 (Conflits de gestes) | Il y a une contradiction **07 ↔ 08** que le plan ne tranche pas : le rapport 07 exclut le geste retour aux coins, le rapport 08 recommande explicitement **de ne pas l'exclure** et de démarrer le glissement à plus de 32 dp du bord. Le plan déclare l'exclusion « indispensable » et en tire un argument pour le minSdk 29. Pour mémoire, la limite est de 200 dp verticaux par bord, sauf barre de navigation masquée en mode collant [V]. | Retenir par défaut la position du rapport 08 (aucune exclusion, saisie du coin à l'intérieur de la page). Tester l'exclusion comme variante au spike 1b, avec le critère « 0 retour involontaire sur 50 tours depuis le coin ». Consigner le choix dans l'ADR 006. |
| M13 | Majeur | §2 (« paquet = 1 base SQLite ») ; §5.2 (Paquets) | Le format de paquet n'est pas unifié. Le rapport 11 propose SQLite avec FTS précalculé et Ed25519, le 10 ECDSA P-256 et `index.json`, le 14 un ZIP `.ppk` (JSON, images, dévotions, calendrier, chant, index construit à l'installation, Ed25519). Le plan retient SQLite et ECDSA sans dire où vont les **images pieuses, les définitions de dévotions, les données du calendrier (05) et le chant**, ni comment se nomment les champs de version (`schema`/`minApp` chez 10, `formatVersion`/`minAppFormat` chez 11). | ADR 003 « paquet v1 » : une base SQLite unique avec des tables `asset` (BLOB WebP), `devotion` et `calendar` (JSON validés par schéma), une signature ECDSA P-256 partout (Ed25519 n'existe dans `java.security` qu'à partir de l'API 33), un seul nom de catalogue et des champs de version uniformes (voir §5.7). Les rapports 11 et 14 sont amendés en conséquence. |
| M14 | Majeur | §4, phase 0 (`tools/install-ci-apk.sh`) | Les APK de débogage de la CI sont signés par une clé de débogage **régénérée sur chaque runner**. Le `adb install -r` suivant échoue donc (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`), de même qu'entre un `installDebug` local et un APK de CI. La désinstallation efface `user.db`. | Ajouter au dépôt un keystore de débogage dédié (non secret) ou le passer en secret CI ; `signingConfigs.debug` pointe vers lui. Donner aux builds de débogage un `applicationIdSuffix ".dev"`, pour que la version d'usage quotidien et celle de développement coexistent. |
| m1 | Mineur | §5.2 (minSdk) | La justification est en partie fausse. Il n'existe aucun geste retour par le bord avant l'API 29 : l'API d'exclusion n'y manque donc pas, et l'appel conditionnel suffit (voir aussi M12). Les arguments valides sont `setAutomaticZenRuleState` (API 29 [V]), qui unifie le mode NPD (08), et `Typeface.CustomFallbackBuilder` (API 29, 07). Part de marché : 91,1 % pour l'API ≥ 29 contre 96,1 % pour l'API ≥ 26 (StatCounter, avril 2026 [V]). | Réécrire la ligne : « 29 pour unifier le NPD par `AutomaticZenRule` et chaîner les polices de secours ; coût d'environ 5 points de parc ; sans objet en scénario A ». Garder l'exclusion de gestes hors de l'argumentaire. |
| m2 | Mineur | §5.2 (Typographie) et §5.3 (Police par défaut) | La police par défaut est à la fois « choisie » (EB Garamond, §5.2) et « reportée à la porte A » (§5.3). Le §1 dit qu'aucune police testée n'a ✠, alors que Junicode 2, testée par le rapport 07, l'a. Literata (08) n'a ni ℣ ni ℟ ni ✠ ni U+202F [V]. | Écrire : « EB Garamond par défaut pour les livres liturgiques ; Literata en option de lecture, à confirmer à la porte A ». Pour le secours, utiliser un **sous-ensemble Junicode** de quelques Ko (✠ ✝ ⁂ U+202F, et ℣ ℟ si Literata) obtenu avec `pyftsubset`, au lieu de la police entière (environ 1 Mo en woff2). |
| m3 | Mineur | §3.1 (Bible) | Le Crampon 1923 est noté « à confirmer ». Les pièces vérifiées vont dans le sens de l'œuvre collective (voir §3) : le risque devient faible. | Remplacer par « domaine public probable (œuvre collective ou anonyme publiée en 1923, L113-2 et L123-3) ; risque faible ; repli Glaire 1905 ». |
| m4 | Mineur | §5.3 (Nom divin, Numérotation des psaumes) | Ces deux points sont reportés, mais la phase 2 construit le paquet Bible et **frappe des identifiants permanents**. Proposer « le Seigneur » en option change le texte (« de Yahweh » devient « du Seigneur ») et donc les ancres. | Avant la phase 2 : (a) les identifiants suivent la versification **native** de chaque édition (hébraïque pour Crampon, Vulgate pour la Vulgate), la numérotation affichée relevant d'un réglage ; (b) le nom divin est une variante de texte construite par la chaîne, avec ses propres ancres, ou il est tranché une fois pour toutes. |
| m5 | Mineur | §5.3 (Ordo 1962, solennités extérieures) | Ces décisions sont reportées après le spike, alors que la phase 3 (parallèle au spike) doit saisir le propre de France 1962 « à partir d'un Ordo de référence » (05). | Les avancer en phase 0, ou isoler la saisie France 1962 dans une sous-tâche bloquée par cette décision. |
| m6 | Mineur | §4, phase 3 | Il manque une dépendance : les oracles « golden » (romcal en TypeScript, Divinum Officium en Perl, Missalemeum en Python) exigent un banc de génération dans `content/` (Node, Perl, uv), qui n'est planifié nulle part. | Ajouter la tâche « banc d'oracles » en phase 2 ou 3, avec des sorties JSONL commitées et le fichier `known-divergences.md` (05). |
| m7 | Mineur | §4, phase 3 ; §6 | « Golden sur les 70 types d'année » n'est pas exhaustif. La date de Pâques et l'année bissextile fixent bien le jour de semaine de chaque date, mais les cycles A/B/C et I/II, les champs `validFrom`/`validUntil` (Newman en 2025) et les décisions ponctuelles dépendent de l'année réelle. | Compléter par un balayage d'années réelles (05, G1 et G3 : 1970-2100 pour 1969, 2004-2050 pour 1962) et par les cas G4. |
| m8 | Mineur | §6 (« mises à jour sans nouvelle version de l'app ») | Au MVP, il n'y a ni permission INTERNET ni téléchargement : un nouveau décret exige donc un nouvel APK. | Écrire « au MVP, par nouvel APK ; en V1, par paquet téléchargé ». |
| m9 | Mineur | §5.2 (Données de l'utilisateur) | Le plan adopte le rapport 10 (sauvegarde cloud désactivée) sans mentionner le rapport 09, qui recommande l'Auto Backup chiffré de bout en bout (`disableIfNoEncryptionCapabilities`), sans les intentions. Or la reprise de position est la demande n° 1 (13). | Consigner l'arbitrage dans l'ADR 008. Si la sauvegarde reste désactivée, proposer l'export `.paroissien` au premier ruban posé et rappeler son existence dans « Mes signets ». |
| m10 | Mineur | §5.3 (`applicationId`) | Cette décision est reportée « avant diffusion », mais la porte B prévoit un usage quotidien : changer l'identifiant ensuite ferait perdre vos propres données. | La figer en phase 0 (coût nul). |
| m11 | Mineur | §3, §4 | La **synthèse vocale**, citée par la vision, a disparu du plan sans arbitrage. Le rapport 06 la met au MVP, le rapport 13 « plus tard ». | La placer explicitement, en V1 de préférence, et garder dès la phase 2 la `lang` par bloc et par segment (déjà prévue par 11). |
| m12 | Mineur | §5.2 (Paquets) ; §2, principe 6 | Le budget de l'APK n'est pas chiffré. Les natifs SQLite pèsent 1,1 à 1,3 Mo par ABI, soit environ 4,9 Mo pour 4 ABI [V]. S'y ajoutent Bible et Vulgate (environ 7 à 9 Mo compressés, 04), le missel (plusieurs Mo), les images (5 Mo) et les polices : on approche les **30 Mo** d'IzzyOnDroid, et l'espace installé double (copie des bases depuis `assets/`). | Filtrer les ABI (`arm64-v8a`, plus `armeabi-v7a` en distribution) et ajouter un contrôle de taille en CI. La Vulgate devient optionnelle si le budget est dépassé. |
| m13 | Mineur | §3.1 (Missel) ; §5.3 | Le plan ne dit pas quelle Bible française résout les lectures en 1962. Le Crampon est traduit sur l'hébreu et le grec ; la liturgie de 1962 suit la Vulgate (psaumes, Siracide). Le rapport 04 signale que le Glaire est calé sur la Vulgate. | Préciser : lectures 1962 = français du Propre (Barroux ou domaine public), sinon Glaire, avec le Crampon pour la lecture libre. |
| m14 | Mineur | §1 (Mode NPD) | « Depuis Android 15, c'est impossible » est inexact : pour une application qui cible l'API 35 ou plus, `setInterruptionFilter` active une `AutomaticZenRule` implicite au lieu de modifier l'état global [V]. La conclusion reste valable. | Reformuler : « on ne modifie plus l'état global ; on passe par une règle ». |
| m15 | Mineur | §6 (Risques) | Plusieurs risques manquent : coquilles ou erreurs doctrinales dans les contenus sans canal de signalement (aucune télémétrie), perte de la clé de signature, épuisement d'un développeur seul face au périmètre, purge des mentions d'indulgences en jours (14) et double état des litanies (14). | Ajouter ces lignes, avec les parades des §5.5 et §2.1. |

### 2.1 Coupe proposée du MVP et séquencement

Les durées sont des estimations du relecteur [S], en semaines de travail effectif, et le chemin est **séquentiel**.

| Jalon | Contenu | Durée |
|---|---|---|
| **0. Fondations** | Inventaire du téléphone (B2) ; ADR 001 à 008 (§5.1) ; squelette « vert » (AGP 9.4, KSP, Room 3, Roborazzi) ; keystore de débogage stable et `.dev` (M14) ; CI publique et privée (B1) ; `applicationId` figé | 1-2 |
| **1a. Feuille et encre** | 07 ① et ② ; mini-porte | 2-3 |
| **1b. Livre** → **Porte A** | Paginateur (06), double page (07 ③), TalkBack, variante de gestes (M12) ; critères du §5.2 | 2-3 |
| **2. Contenu du domaine public** | Paquet v1 (M13), pliage (M3), identifiants ; Imitation, Prières (≈ 40, relues), Exercices, Bible ; Ordinaire 1962 du domaine public (M9) ; Clairval en saveur `personnel` si scénario A (M7) | 4-6 |
| **3. Lecteur v1** → **Porte B1** | Bibliothèque, sommaire, position, rubans manuels, recherche FTS5, export JSON minimal ; **2 semaines d'usage réel** | 4-6 |
| **4. Calendrier et Aujourd'hui** | 1962 complet et France, 1969 « fête et couleur » ; Aujourd'hui sans messe (M6) ; séance de méditation (écran allumé, minuteur, cloche, `AutomaticZenRule`) | 4-7 |
| **5. Missel 1962** → **Porte B2 = MVP** | Ingestion de DO (héritage, conditionnels 1960, alignement à 93,7 %), messe du jour, Évangile du jour 1962, ruban automatique (M8), vis-à-vis | 5-8 |
| **V1** | Images pieuses (première priorité de la V1 : la curation est déjà faite au rapport 09), export `.paroissien` complet, lectionnaire 1969, TTS, retraite, dévotions, téléchargements, Clairval public si l'abbaye l'autorise | — |

Cette coupe livre un **objet utilisable au bout d'environ 3 à 5 mois** (B1) au lieu de 6 à 9, valide l'expérience de livre sur du contenu sans risque juridique, et retarde le missel (le chantier le plus lourd, et le plus exposé juridiquement) jusqu'à ce que le moteur soit éprouvé.

---

## 3. Vérifications effectuées

| Affirmation (rapport) | Résultat | Source |
|---|---|---|
| Crampon 1923 : « œuvre collective, domaine public depuis 1994 » (12 [S]) ; « réviseurs jésuites » (04 [S]) | **Partiellement vérifié, et favorable.** La page de titre indique une édition révisée par des Pères de la Compagnie de Jésus avec des professeurs de Saint-Sulpice, **sans aucun nom**, publiée par la Société de Saint-Jean-l'Évangéliste (Desclée, 1923, « Tous droits réservés », imprimatur de Tournai du 1er mars 1923). L'avant-propos, signé collectivement par la Société, dit que les Pères ont repris l'œuvre « à la prière des Éditeurs ». Ce sont de forts indices d'**œuvre collective** (initiative et nom de l'éditeur, contributions fondues : L113-2 al. 3), sinon d'œuvre anonyme. Dans les deux cas, la protection a duré 70 ans après la publication, d'où le domaine public au 1er janvier 1994. Risque résiduel : une requalification en œuvre de collaboration avec des réviseurs identifiés et morts après 1955. Aux États-Unis, l'œuvre est dans le domaine public (publiée en 1923). | fr.wikisource : `Page:La Sainte Bible, trad Crampon, édition 1923.djvu/2`, `/3`, `/5`, `/6` et l'index `Livre:` (BnF ark `bpt6k318720w`). La notice Gallica a renvoyé 403. |
| Mekuri (AniCanon) : MIT, v0.3.0, un mois, zéro étoile, minSdk 33, GitHub Packages (07) | **Vérifié.** Dépôt `AniCanon/mekuri` créé le 09/09/2026, dernier push le 14/09/2026, tags 0.1.0 à 0.3.0, 0 étoile, 0 fork, licence MIT (« Copyright (c) 2026 AniCanon »). Module Android : `minSdk 33`, `compileSdk 35`, **AGP 8.5.2, Kotlin 2.3.20, Compose BOM 2024.09.01** (un portage vers BOM 2026.09 et AGP 9.4 sera nécessaire). Publication sur GitHub Packages. 34 fichiers Kotlin de bibliothèque (environ 95 Ko) et des tests JVM de géométrie. La documentation d'accessibilité confirme des **libellés anglais codés en dur, impossibles à surcharger**. Bibliothèque double SwiftUI et Compose. | API GitHub (`repos`, `tags`, `commits`, `git/trees`) ; `android/mekuri/build.gradle.kts`, `libs.versions.toml`, `docs/accessibility.md` |
| `View.setSystemGestureExclusionRects` : API 29, limite d'environ 200 dp (07 « à vérifier », 08) | **Vérifié.** Ajouté en API 29. Limite de 200 dp sur l'étendue verticale des exclusions, **non appliquée quand la barre de navigation est masquée en mode collant**. | developer.android.com, référence `View` |
| `NotificationManager.setAutomaticZenRuleState` : API 29 (08) | **Vérifié.** API 29. `addAutomaticZenRule` date de l'API 24 et exige l'accès à la politique de notification. `getAutomaticZenRuleState` date de l'API 35. Pour une application ciblant l'API 35 ou plus, `setInterruptionFilter` **active une règle implicite** au lieu de modifier l'état global. | developer.android.com, référence `NotificationManager` |
| Part de marché des API ≥ 29 (≈ 91 %) et ≥ 33 (≈ 69 %) (plan, 07, 10) | **Vérifié** (StatCounter, avril 2026, mis à jour le 28/05/2026) : ≥ 26 : 96,1 % ; ≥ 29 : 91,1 % ; ≥ 31 : 78,8 % ; ≥ 33 : 68,9 % ; ≥ 34 : 54,5 % ; ≥ 35 : 41,0 %. Le chiffre de 98,3 % pour l'API ≥ 26 (10, Android Studio, décembre 2025) vient d'une autre source et n'a pas été revérifié. | apilevels.com |
| Glyphes ✠ ℣ ℟ (07, 08) | **Vérifié** (cmap lue avec fontTools). **EB Garamond** (google/fonts, v1.003, variable) : ℣ ℟ ❦ ❧ ☙ présents ; **✠ ✝ ⁂ U+202F absents**. **Literata** (v3.103) : **℣ ℟ ✠ ✝ U+202F absents**, ⁂ présent. **Junicode** (`JunicodeVF-Roman.woff2` du dépôt, v2.003) : ✠ ℣ ℟ ✝ ⁂ U+202F tous présents. La release v2.226 (20/06/2026) existe, mais son ZIP de 59 Mo n'a pas été téléchargé. | `github.com/google/fonts` (ofl/ebgaramond, ofl/literata) ; `psb1558/Junicode-font` (webfiles, API des releases) |
| Room 3 et `@Fts5` (06, 10, 11) | **Vérifié.** `androidx.room3` 3.0.0 stable le 01/07/2026, 3.0.3 le 09/09/2026, 3.1.0-alpha01 publiée. `@Fts5` depuis la 3.0.0-alpha02 (25/03/2026), avec les tokeniseurs ASCII et trigram et l'option `detail`. **KSP obligatoire**, nouveau paquet, SupportSQLite retiré. | developer.android.com/jetpack/androidx/releases/room3 ; index Maven Google |
| `androidx.sqlite` embarqué avec FTS5 et SQLite 3.53.4 (10, 11) | **FTS5 vérifié, version fausse.** Dans les `.so` publiés de `sqlite-bundled-android` 2.7.1 (arm64-v8a, armeabi-v7a, x86, x86_64), on lit `ENABLE_FTS5` et `ENABLE_FTS4`, sans ICU, mais **SQLite 3.50.1** (source id du 06/06/2025). Le `build.gradle` d'androidx-main contient bien `-DSQLITE_ENABLE_FTS5` et `3.53.4` : c'est la branche de développement, pas la release. | Maven Google : AAR et JAR 2.7.1 (`strings` sur les natifs) ; `androidx/androidx` `sqlite/sqlite-bundled/build.gradle` |
| Tests sur hôte avec `sqlite-bundled` (10, « à vérifier ») | **Vérifié.** `sqlite-bundled-jvm` 2.7.1 contient des natifs `linux_x64`, `linux_arm64`, `osx_arm64` et `windows_x64` (SQLite 3.50.1, FTS5). Les tests JVM peuvent ouvrir les paquets avec le même moteur que l'application. | Maven Google, `sqlite-bundled-jvm-2.7.1.jar` |
| Divinum Officium sous MIT depuis le 28/08/2026 (03, 12) | **Vérifié.** Le commit « Add MIT License file » date du 28/08/2026 ; le dernier push du dépôt date du 11/10/2026. Réserve : une licence ajoutée en 2026 sur des années de contributions bénévoles ne vaut que pour ce que le projet détient (12 l'a bien vu pour le français). | API GitHub `DivinumOfficium/divinum-officium` |
| Césure : alphabet latin d'Android limité aux lettres sans accent (07) | **Vérifié** pour le fichier d'alphabet : `TeX/la/hyph-la.chr.txt` d'AOSP ne contient que a-z (sans w ni y) plus æ et œ, sans aucune voyelle accentuée. Le comportement de minikin n'a pas été relu. | android.googlesource.com, `platform/external/hyphenation-patterns` |
| Motifs `la-x-liturgic` sous MIT (07) | **Vérifié.** MIT, © 2016-2019 Beccari et Roux, **version amont du 04/02/2019**, plus récente que la copie de 2016 présente dans AOSP. | `hyphenation/tex-hyphen`, `hyph-la-x-liturgic.tex` |
| Clairval : API REST publique, 313 lettres françaises, aucune lettre de 2026 (01) | **Vérifié.** `X-WP-Total: 313` ; la lettre la plus récente est datée du 25/12/2025. `robots.txt` : `crawl-delay: 10` et `Disallow` du chemin `assets/lettres`. Deux requêtes seulement. | clairval.com (`/robots.txt`, `/wp-json/wp/v2/posts?lang=fr`) |
| Artefacts de CI accessibles sur un dépôt public (B1) | **Vérifié.** Toute personne connectée à GitHub et ayant un accès en lecture (donc tout le monde sur un dépôt public) peut télécharger les artefacts. | docs.github.com, *Downloading workflow artifacts* |
| Vérification des développeurs : mondiale en 2027, 20 appareils en compte gratuit (10, 12) | **Vérifié** pour le calendrier (30/09/2026 au Brésil, en Indonésie, à Singapour et en Thaïlande ; reste du monde en 2027) et pour les comptes « distribution limitée » (20 appareils, sans pièce d'identité ni frais). **Exemption adb non retrouvée** sur les pages consultées. | developer.android.com/developer-verification (et le guide *limited-distribution*) |
| « 70 types d'année » exhaustifs (05 [S]) | **Raisonnement** [S]. Pâques et l'année bissextile fixent le jour de semaine de toutes les dates : la structure est donc couverte. Mais les cycles de lectures, la validité des fêtes et les décisions ponctuelles dépendent de l'année réelle, qui n'est pas couverte (m7). | — |

---

## 4. Points non vérifiables restants

| Point | Pourquoi il reste ouvert | Comment le fermer, et quand |
|---|---|---|
| Identité et dates de décès des réviseurs du Crampon 1923 ; mention de droits de Gallica | Aucun nom dans l'ouvrage ; Gallica a refusé l'accès (403) | Consultation manuelle de la notice BnF et de Gallica, plus une recherche « Crampon 1923 révision jésuites » (Bibliothèque des jésuites de Vanves). Avant une diffusion B ou C uniquement. |
| Position de Clairval, du Barroux et de l'AELF | Elle dépend de leur réponse | Envoi en phase 0, avec un délai butoir (M11) |
| Provenance de l'Ordinaire français de DO (Sancta Missa, introibo.fr) | Les deux sites sont hors service | Considérer cette traduction comme non libre (M9) |
| Droits sur la modernisation 2005 des Exercices (« Frère Jérôme ») | La mention « libre de tout droit » n'est pas une licence formelle | Message de courtoisie (§7 du plan), conservation de la page source ; repli Jennesseaux 1913 d'archive.org |
| Statut des textes latins de 1955 à 1962 (Semaine sainte, rubriques de 1960) face aux revendications de la LEV et de l'APSA | Il s'agit d'interprétation juridique (12 : risque faible) | Seulement pour le scénario C : avis d'un juriste ou demande à la LEV |
| Performance réelle du pli AGSL (Mekuri n'a été mesuré que sur émulateur) et compatibilité des pilotes GPU | Il faut un appareil physique | Spike 1a sur le téléphone de référence (B2) |
| Compatibilité entre AGP 9.4.1, Kotlin 2.4.20, KSP 2.3.12, Room 3.0.3, Roborazzi 1.76 et Robolectric 4.17 (SDK 37) | Elle se constate au premier build | Squelette « vert » de la phase 0 (10 l'avait prévu) |
| Durcissement audio en arrière-plan d'Android 17 (cloche écran éteint) ; variantes constructeur des « Modes » | Pas de documentation consultée ; dépend des appareils | Test sur le téléphone en V1 (le MVP garde l'écran allumé) |
| Comportement de minikin avec U+00AD et les locales par segment | Code non relu par moi | Test sur l'appareil au spike 1a (50 mots latins de contrôle, 07) |
| Exemption adb de la vérification des développeurs | Absente des pages consultées | Relire la FAQ officielle avant toute diffusion B |
| Lecture canonique des *Responsa ad dubia* de 2021 (pas de lectionnaire vernaculaire de l'ancien rite) | Interprétation (12, [S]) | Seulement pour le scénario C, avec l'Ordinaire du lieu |
| Licences CC BY-SA des transcriptions Wikisource et marque Gutenberg | Interprétation (04, 12) | Crédit systématique dans l'écran « Sources », marque Gutenberg retirée |

---

## 5. Ce qu'il faut ajouter pour démarrer l'implémentation

### 5.1 ADR à rédiger en phase 0 (une page chacun)

1. **ADR 001, Horizon, dépôts et saveurs.** Scénario A, B ou C ; dépôt public et dépôt privé ; saveurs `domainePublic` et `personnel` ; quelle CI construit quoi (B1) ; statut de droits par paquet (`domaine-public`, `licence-libre:<SPDX>`, `autorisation-écrite:<réf>`, `usage-privé-seulement`, selon 12, §3).
2. **ADR 002, Appareils et SDK.** Téléphone de référence (modèle, API, Hz, GPU), second appareil, minSdk, target et compile, justification corrigée (m1), `applicationId` figé (m10), keystore de débogage et suffixe `.dev` (M14).
3. **ADR 003, Paquet v1.** DDL, tables `asset`, `devotion` et `calendar`, signature ECDSA P-256, catalogue, champs de version (§5.7), SQLite 3.50.1 et APSW épinglé (M4).
4. **ADR 004, Texte canonique et ancres.** NFC, absence de U+00AD dans le texte canonique, décalages en points de code, `rev` = hachage du texte canonique, citation de 32 caractères, cascade de ré-ancrage, orphelins jamais supprimés (M3).
5. **ADR 005, Moteur de lecture et porte A.** Critères du §5.2 et règle de décision.
6. **ADR 006, Gestes.** Exclusion ou non (M12), zones de tap, touches de volume (13), priorités entre rubans, images et tourne-page.
7. **ADR 007, Polices.** EB Garamond, Literata, sous-ensemble Junicode, lettrines, liste des caractères garantis (m2).
8. **ADR 008, Données de l'utilisateur.** Room 3, sauvegarde (m9), chiffrement du journal (V1), export.

### 5.2 Critères mesurables du spike (porte A)

Tous les critères se mesurent sur le téléphone de référence, avec l'APK de CI en build release (R8 actif, sans débogueur).

| Domaine | Critère | Mesure |
|---|---|---|
| Fluidité du pli | P95 de la durée d'image ≤ période de rafraîchissement (8,3 ms à 120 Hz, 16,6 ms à 60 Hz) ; moins de 1 % d'images manquées sur 50 tours lents et 50 tours lancés | `adb shell dumpsys gfxinfo <pkg> framestats`, JankStats |
| Coût au repos | Aucune recomposition de page pendant un tour ; pic mémoire inférieur à 40 Mo pendant le tour ; mémoire stable après 200 tours | Compteur de recompositions (Layout Inspector ou journal) ; `dumpsys meminfo` |
| Pagination | Un chapitre de l'Imitation paginé en moins de 50 ms (médiane) ; le livre I complet en moins de 1,5 s, hors fil principal ; réouverture avec cache en moins de 200 ms jusqu'à la première page | Traces (`Trace.beginSection`) |
| Stabilité de l'ancre | Après 10 rotations et 3 changements de taille de police, le premier bloc visible et son décalage sont identiques | Test instrumenté ou journal |
| Typographie | Aucun glyphe manquant sur le jeu de caractères de contrôle (✠ ℣ ℟ ✝ ⁂ U+202F œ æ ǽ, voyelles accentuées) ; 50 mots latins de contrôle coupés comme attendu ; lettrine alignée sur 2 à 3 lignes à 100, 130 et 200 % | Capture et liste de contrôle |
| Double page | Parité respectée ; la feuille ne traverse pas la reliure ; position conservée en passant du portrait au paysage | Capture et vidéo |
| TalkBack | Lecture de la page dans l'ordre ; actions « page suivante » et « page précédente » en français ; mode défilant proposé | Test manuel scénarisé |
| Gestes | Aucun retour système involontaire sur 50 saisies de coin ; zones de tap 25/50/25 | Test manuel compté |
| **Décision** | Pli en échec : natif et glissement. Pagination ou typographie en échec après une semaine de corrections : Readium. Sinon, natif confirmé | ADR 005 |

### 5.3 Stratégie de test du calendrier

- Reprendre telle quelle la batterie G0 à G4 du rapport 05 (§3.1), en ajoutant **le balayage des années réelles** (m7).
- Le **banc d'oracles** (m6) produit des fichiers JSONL commités par rite et par plage d'années. La comparaison se fait champ par champ, et tout écart est soit corrigé, soit justifié dans `known-divergences.md` : jamais d'acceptation silencieuse.
- Une table de **cas manuels** sourcés (rubrique et numéro de paragraphe) sert de spécification exécutable.
- Les tests tournent en JVM pur (quelques secondes, sur la VM comme en CI). Les oracles sont régénérés en CI, sur déclenchement manuel.

### 5.4 Jeu de données « golden »

Il s'agit d'un **paquet golden** versionné dans `content/golden/`, construit par la chaîne et lu par les tests Kotlin (11). Il doit couvrir :
- chaque type de bloc et chaque inline (rubrique, ℣/℟, ✠, appel de note, renvoi, changement de langue, lettrine) ;
- l'Imitation I, 1-3 en latin et en français ;
- le Ps 22 (Vulgate) / 23 (hébreu), pour la double numérotation ;
- Ex. [1] à [20] ;
- un extrait de l'Ordinaire 1962 bilingue avec des sections mal alignées ;
- une lettre de Clairval fictive (le contenu réel est protégé) ;
- les 50 mots latins de césure ;
- les vecteurs de pliage (y compris U+00AD) ;
- des ancres « avant et après correction » pour tester le ré-ancrage.

Le tout s'accompagne d'empreintes SHA-256 et d'un test qui exige un **build bit à bit identique** (11 l'a mesuré).

### 5.5 Gestion des erreurs de contenu

- **À l'installation ou à la première copie** : vérifier le SHA-256 de chaque paquet. Un paquet invalide est mis en quarantaine, avec un message clair et le livre grisé ; **jamais de plantage**.
- **Compatibilité** : si `formatMajor` dépasse ce que l'application gère, le paquet est refusé ; un bloc de type inconnu s'affiche comme un paragraphe (11).
- **Calendrier** : si une date n'est pas couverte, on affiche « férie » avec un indicateur, et une alerte apparaît en build de débogage.
- **Ancres** : une ancre orpheline va dans « Annotations à replacer », jamais à la poubelle.
- **Signalement sans télémétrie** : une action « Signaler une coquille » prépare un courriel ou un partage, à l'initiative de l'utilisateur, avec l'identifiant de bloc, la `rev` et la version du paquet. Côté chaîne, les corrections passent par PR avec un alias si l'identifiant change.

### 5.6 i18n

- Toutes les chaînes de l'interface sont dans `strings.xml` dès le premier écran (français par défaut, sans texte en dur, pluriels gérés). La CI le vérifie avec un lint (`HardcodedText`).
- Les dates passent par `java.time` avec une locale explicite ; les noms liturgiques viennent des paquets, en latin et en français.
- La `LocaleList` est posée par segment pour la césure et le TTS.
- Les libellés d'accessibilité anglais de Mekuri doivent être remplacés (vérifié).
- La langue par application (API 33) et l'anglais arrivent en V1 ; un réglage « langue des contenus » reste distinct de la langue de l'interface.

### 5.7 Versionnement des paquets

| Champ | Rôle |
|---|---|
| `formatMajor.formatMinor` | Format du schéma (ADR 003). L'application lit tout `formatMajor` connu. |
| `packageId` et `packageVersion` | Version entière croissante. |
| `rev` par bloc | Hachage du texte canonique (ADR 004). |
| `minAppFormat` | Format minimal requis côté application. |
| `PRAGMA user_version` et `application_id` | Identification et version du fichier SQLite. |
| `rights` | Statut de droits (ADR 001). |

La CI échoue dans trois cas : un identifiant publié disparaît sans `id_alias`, le format baisse, ou la version SQLite de la chaîne dépasse celle de l'application.

### 5.8 Migration de `user.db` et des ancres

- Room 3 exporte son schéma dans `android/core/data/schemas/`, commité. Chaque changement passe par `AutoMigration` ou une migration écrite, **testée depuis chaque schéma exporté**. `fallbackToDestructiveMigration` est interdit et vérifié par un lint ou un test.
- Au démarrage, après une mise à jour de paquet, un ré-ancrage paresseux compare `blockRev` à la `rev` courante, puis passe par la citation et par `id_alias`. Le résultat est journalisé localement.
- Le format d'export `.paroissien` porte un `formatVersion` dès la première version (09). Un test d'aller-retour export/import le vérifie.

### 5.9 Liste de démarrage (jour 1)

1. Trancher les quatre décisions du §5.1 du plan, et ajouter la cinquième : le téléphone de référence.
2. Rédiger les ADR 001 à 008 (une page chacun).
3. Préparer les brouillons Barroux et AELF, puis les envoyer **après votre validation**, avec date butoir.
4. Construire le squelette Gradle (keystore de débogage, `.dev`, saveurs), puis la CI publique et la CI privée.
5. Mettre en place `content/` avec uv : APSW 3.50.1, `fold.py` avec ses vecteurs (U+00AD compris), gabarit du paquet golden.
6. Lancer le spike 1a.
