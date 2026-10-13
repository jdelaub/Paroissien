# Paroissien — Plan directeur

*v0.2 du 11/10/2026. Synthèse des rapports 01 à 14, corrigée selon la revue critique 15. Le détail et les sources sont dans chaque rapport.*

## 0. En une page

**Le produit.** Un livre de prière papier sur Android : hors ligne, sans compte, sans publicité ni pistage. On y tourne de vraies pages, chaque livre a ses rubans de tissu cousus et l'on glisse des images pieuses entre les feuillets. Les textes viennent de sources traditionnelles choisies avec soin.

**La place sur le marché.** Aucune application ne propose aujourd'hui l'*Imitation* en français, les lettres de Clairval, ni une retraite ignatienne en texte français et hors ligne (13). Les applications les mieux notées sont sobres, hors ligne et sans compte. Le créneau « tout-en-un traditionnel » se remplit vite (Iter Fidei, 01/10/2026). La différence doit donc venir de **l'expérience de livre**, du **français** et des **contenus uniques**.

**L'architecture.**
- Une chaîne de contenu en Python produit des **paquets SQLite** : blocs JSON, identifiants stables, index FTS5 précalculé.
- L'application Kotlin/Compose les affiche avec un **moteur de lecture natif**, sans WebView.
- La page tourne avec un **pli cylindrique AGSL** à partir de l'API 33, et par glissement de 29 à 32.
- Le **calendrier liturgique** est en Kotlin pur.
- Deux **saveurs de build** : `domainePublic`, publique, et `personnel`, privée, qui peut embarquer des contenus réservés à votre usage.

**Le premier risque est juridique, pas technique.** Les lettres de **Clairval**, le français du Propre de 1962 (propriété du **Barroux**) et la forme ordinaire (**AELF**) exigent une autorisation écrite pour toute diffusion au-delà de l'usage personnel. Le MVP est donc conçu pour être **complet avec le seul domaine public**. Les contenus protégés ne s'ajoutent que dans la saveur `personnel`, ou après accord.

**Le séquencement.** Le travail est **séquentiel**, car il y a un seul développeur, et passe par deux portes d'usage réel :
- **B1 « livre de lecture »**, en 3 à 5 mois : Imitation, Exercices, prières, Bible, rubans et recherche ;
- **B2 « missel »**, en 4 à 5 mois de plus : calendrier, Aujourd'hui, méditation, images pieuses, missel de 1962 et forme ordinaire.

Le **spike « livre »** dure 4 à 6 semaines sur votre téléphone. Il valide le moteur et ses critères sont chiffrés (porte A).

**Les décisions de fondation (11/10/2026, §5.1).**
- La diffusion se fera en trois temps : d'abord votre usage personnel (scénario A), puis un partage privé avec des proches (B), et peut-être plus tard une diffusion publique (C), qu'on garde en tête sans la viser.
- Le missel couvre **le 1962 et la forme ordinaire**.
- Le code est public, le contenu privé.
- Les quatre demandes d'autorisation sont rédigées : [`docs/demarches/courriers.md`](../demarches/courriers.md).

**Le téléphone de référence** est un Samsung Galaxy Z Flip6. Sa posture « livre », à demi plié et tenu de côté, donne une double page naturelle (§5.1).

**La prochaine étape.** Envoyer les courriers, puis passer à la phase 0.

---

## 1. Ce que la planification a corrigé ou appris

| Sujet | Hypothèse de départ | Constat (rapport) |
|---|---|---|
| Lettres « de Flavigny » | Peut-être une source distincte | Ce sont **les lettres de Clairval**, dont l'abbaye est à Flavigny-sur-Ozerain (01) |
| « Lettre du mois » | La dernière lettre est en ligne | **Aucune lettre de 2026 n'est en ligne.** Le site archive 1996 à 12/2025 : 313 lettres en français, environ 1 840 dans six langues. La carte propose donc la lettre du saint du jour, sinon la plus récente, sinon celle d'il y a N ans (01) |
| Récupération de Clairval | Aspirer le HTML | Une **API REST WordPress publique** suffit : environ 25 à 40 requêtes avec `Crawl-delay: 10`. Le gabarit est stable de 1996 à 2025 (01) |
| Exercices sur « ragionline.com » | Site source | Ce domaine n'existe pas. Il s'agit de **raggionline.com**, dont le PDF français imprime le HTML de **livres-mystiques.com** : Jennesseaux 1913, modernisé en 2005, « libre de tout droit », avec les 370 paragraphes numérotés (02) |
| Missel de 1962 | Texte libre | **Divinum Officium** (MIT depuis le 28/08/2026) est complet en latin. Le **français du Propre appartient au Barroux**, et celui de l'**Ordinaire** est de provenance inconnue : Sancta Missa et introibo, hors ligne (03, 12, 15) |
| Missel de Dom Lefebvre | Source possible | Protégé jusqu'au 31/12/2036 (03, 12) |
| Mode « Ne pas déranger » | On le coupe soi-même | Depuis l'API 35, on ne modifie plus l'état global : **on passe par une règle**, `AutomaticZenRule` *Temps de prière*, que le système restaure (08, 15) |
| Césure du latin | Gérée par Android | Android **ne coupe pas les mots latins accentués** (*Dóminus*). Les motifs liturgiques s'appliquent **à l'exécution** ou se stockent à part, jamais dans le texte canonique (07, 15) |
| Glyphes | Une police à empattements suffit | Literata n'a ni ℣ ni ℟ ni ✠ ni U+202F. EB Garamond a ℣ et ℟ mais pas ✠ ni U+202F. **Junicode 2 a tout** : on en extrait un petit sous-ensemble de secours (07, 08, 15) |
| Rubans vert et rouge | Distincts | Pour un daltonien deutéranope, ils sont **identiques**. Il faut une extrémité ou une initiale par ruban, et des rubriques en rouge **et** en italique (08) |
| Distribution d'APK | Libre | La **vérification des développeurs Android** sera mondiale en 2027. L'installation par adb reste exemptée, et un compte gratuit couvre 20 appareils (10, 12) |
| Artefacts de CI publique | Privés | Sur un dépôt public, **tout compte GitHub peut télécharger les artefacts**. La CI publique ne doit construire que la saveur `domainePublic` (15) |
| VM de développement | 10 Go de RAM | **7,8 Gio et 2 cœurs.** Tous les builds passent en CI ; la VM ne lance que les tests JVM et adb (10) |
| SQLite embarqué | 3.53.4 | `androidx.sqlite:sqlite-bundled` 2.7.1 embarque **SQLite 3.50.1**, avec FTS5 et sans ICU. La chaîne épingle APSW 3.50.1 (15) |
| Recherche | `unicode61` suffit | Ni œ, ni æ, ni j/i, ni U+00AD ne sont pliés : il faut un **pliage commun Python/Kotlin** avec vecteurs de test (04, 11, 15) |

---

## 2. Architecture

```
┌──────────── content/  (Python + uv, hors appareil, relecture par PR) ─────────────┐
│ sources/ → fetch → .cache/raw (jamais commité) → normalize → data/*.json (commité) │
│   → validate (identifiants stables, id_alias, schémas, droits) → package → publish │
│   paquet v1 = 1 base SQLite : publication, divisions, blocs JSON, FTS5,            │
│               asset (WebP), devotion, calendar ; signature ECDSA P-256            │
│   chaque paquet porte un statut de droits : domaine-public | licence-libre |       │
│               autorisation-écrite:<réf> | usage-privé-seulement                    │
└─────────────────────────────────────┬──────────────────────────────────────────────┘
          saveur domainePublic ◄──────┴──────► saveur personnel
          (CI du dépôt public)                 (CI du dépôt privé ou build local seulement)
┌─────────────────────────────────────▼──────── android/ (Kotlin 2.4 / Compose) ─────┐
│ core:model        Book, Division, Block, Anchor, LiturgicalColor… (JVM)            │
│ core:liturgy      calendrier 1962 / 1969-France, Kotlin pur, données en paquet      │
│ core:content      registre des paquets, BundledSQLiteDriver (3.50.1), FTS5          │
│ core:data         Room 3 (user.db) : rubans, images pieuses, notes, positions       │
│ core:designsystem thèmes Clair/Sépia/Sombre/Nuit, polices, jetons liturgiques       │
│ core:reading      moteur « Codex » : paginateur → pages Compose → tourne-page       │
│                   AGSL (API ≥ 33) ou glissement (API 29-32)                         │
│ feature:today     Aujourd'hui (fête, couleur, messe, lecture suggérée)              │
│ feature:reader    bibliothèque, sommaire, lecteur, recherche, rubans, images        │
│ app               navigation (Nav 3), réglages, séance de méditation                │
│ (V1) feature:missal · feature:retreat · feature:devotions · :benchmark             │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

**Les principes structurants**
1. **Le contenu ne vit pas dans le code.** Bible, Imitation, missel, lettres et prières sont des *paquets* lus par un seul lecteur. Seuls les écrans réellement différents deviennent des modules de code (10).
2. **Les identifiants sont stables par construction.**
   - On reprend les clés naturelles quand elles existent : `jhn.3.16` dans la versification **propre à chaque édition**, `ex.230`, `R1962:Sancti/08-15`.
   - Sinon, un identifiant est attribué une fois puis commité.
   - Un identifiant publié ne disparaît jamais sans alias, et la CI le vérifie (11).
3. **Un texte canonique, une ancre sans numéro de page.**
   - Le texte canonique est en NFC, **sans U+00AD**, et c'est sur lui que reposent la `rev`, les ancres, FTS et le TTS.
   - L'ancre se compose du bloc, d'un décalage en points de code, d'une citation de 32 caractères de contexte et de la `rev`.
   - Une ancre orpheline est rangée dans « Annotations à replacer », jamais supprimée (06, 09, 11, 15).
4. **Le moteur de lecture est natif.** Les pages n-1, n et n+1 sont des composables prêts à l'avance, l'encre dessinée sur fond transparent. Le tourne-page dessine le papier, la reliure et la tranche, et fige la page pendant le tour (06, 07).
5. **Accessibilité.** On expose un nœud d'accessibilité par paragraphe et des actions TalkBack en français. Le tourne-page est désactivable, et un mode défilant est proposé (08).
6. **Vie privée.** Pas d'analytique, pas de SDK tiers, aucune permission INTERNET au MVP. Journal et examen restent locaux, chiffrés et hors sauvegarde. Les coquilles se signalent par un partage que l'utilisateur déclenche lui-même, avec bloc, `rev` et version (10, 12, 14, 15).
7. **Les erreurs de contenu ne font jamais planter l'app.**
   - Un SHA-256 invalide met le paquet en quarantaine.
   - Un `formatMajor` inconnu est refusé.
   - Un bloc inconnu s'affiche comme un paragraphe.
   - Une date hors calendrier s'affiche comme « férie » avec un indicateur (15, §5.5).

**Dépôts.**
- `Paroissien` est public, sous GPL-3.0-or-later. Le calendrier est sous Apache-2.0.
- `paroissien-content` est privé. Il contient le texte des sources non libres et la CI de la saveur `personnel`.
- L'arborescence est celle du rapport 10, §3.

---

## 3. Périmètre

### 3.1 Contenus

| Module | Source retenue | Droits | B1 | B2 (MVP) | V1 |
|---|---|---|---|---|---|
| Prières (environ 40) | Paroissiens Mame, Zech et Lecoffre (1860-1901, archive.org), latin | DP ✔. Mentions d'indulgences « en jours » à purger. Litanies en deux états (14) | ✔ profil Traditionnel | | Profil « Liturgie actuelle » si l'AELF accepte |
| Imitation | Lamennais, Gutenberg n° 57824, et latin de The Latin Library | DP ✔ | ✔ | | |
| Exercices | Jennesseaux, HTML de livres-mystiques, PDF comme témoin | DP ✔, retouches de 2005 « libres de tout droit », crédit | Texte intégral et examen | Gabarit de méditation | Retraite (annotations 19 puis 30 jours), journal chiffré, espagnol et latin |
| Bible | Crampon 1923 (USFM CrossWire), Vulgate clémentine | Crampon : **DP probable** (œuvre collective de 1923), risque faible ; repli Glaire 1905 (15) | ✔ notes repliées | | Glaire, Douay-Rheims, parallèles |
| Évangiles | Vue filtrée de la Bible | — | ✔ | Évangile du jour **1962**, tiré du Propre | Lectionnaire 1969 (sans source libre) |
| Calendrier | Moteur Kotlin, données romcal (MIT) et Divinum Officium (MIT) | ✔ attribution | | 1962 et 1969 complets ; France | Diocèses |
| Ordinaire 1962 | Latin de Divinum Officium ; **français tiré d'un paroissien du DP** ajusté à 1955-1962, relu | DP ✔ | | ✔ | |
| Propre 1962 | Divinum Officium | Latin ✔. Français du Barroux : saveur `personnel` seulement, sauf accord | | Latin, plus français selon la saveur ou l'accord | Lacunes traduites (liste produite par la chaîne) |
| Forme ordinaire | AELF | Usage personnel admis par les CGU de l'AELF : **saveur `personnel` seulement**, sauf accord | | Lectures du jour **préchargées par la chaîne** dans le paquet privé (pas de permission INTERNET) ; Ordinaire 2021 ; source des oraisons **à identifier** (l'API AELF ne fournit que les lectures, rapport 03) | Saveur publique si l'AELF accepte |
| Lettres de Clairval | API REST du site | Usage personnel (A) seulement, sauf accord | Saveur `personnel` si scénario A | | Saveur publique si l'abbaye accepte ; six langues |
| Images pieuses | Institutions CC0 (AIC, Met, NGA, Rijksmuseum, Paris Musées) | CC0 ✔ | | ✔ environ 30 cartes (environ 5 Mo) | Paquet de 100, photos personnelles |

### 3.2 Fonctions par porte
- **B1, « livre de lecture »** :
  - lecteur natif avec page qui tourne et double page Auto/1/2 ;
  - quatre thèmes, bibliothèque, sommaire, position de lecture exacte ;
  - **rubans manuels** et recherche FTS5 ;
  - export JSON minimal des données de l'utilisateur ;
  - examen ignatien.
- **B2, « missel » (MVP)** :
  - calendrier et écran **Aujourd'hui** (fête, rang, couleur, commémoraisons, lecture suggérée) ;
  - **séance de méditation** (`AutomaticZenRule`, écran maintenu allumé, minuteur, cloche) ;
  - **images pieuses** ;
  - **missel de 1962** : messe du jour en mode « assemblée » et en mode « livre » ; vis-à-vis latin/français en paysage, alternance en portrait ; **ruban automatique** « messe du jour » au ferret doré ; Évangile du jour ;
  - **forme ordinaire**, dans la saveur `personnel` : lectures du jour, Ordinaire, Évangile du jour 1969, et bascule entre les deux rites.
- **V1** : export `.paroissien` complet, synthèse vocale, rosaire et dévotions (moteur déclaratif du rapport 14), Angélus, retraite, paquets téléchargeables avec index signé, diocèses, multilingue. Partage avec les proches (B) selon les autorisations reçues.
- **Plus tard** : office divin, chant grégorien, vies des saints, import d'EPUB personnels, audio enregistré, rites secondaires.

> **Écart volontaire avec la revue 15.** Celle-ci repoussait les images pieuses en V1. Je les garde dans B2 : la vision en fait un élément central de l'expérience, la curation est déjà faite (09), et leur mécanique (ancre et rendu en saillie) réutilise celle des rubans.

---

## 4. Feuille de route (séquentielle)

Les durées sont des estimations en semaines de travail effectif pour une personne seule [S, 15]. Une **porte** est un arrêt où l'on décide avant de continuer.

| Phase | Contenu | Durée |
|---|---|---|
| **0. Fondations** | Décisions du §5.1 ; **inventaire du téléphone** (API, Hz, GPU) ; ADR 001 à 008 (§4.1) ; envoi des demandes d'autorisation avec date butoir ; squelette Gradle aligné sur CRA-Toolbox (AGP 9.4, Kotlin 2.4, BOM 2026.09, Room 3, Roborazzi) ; **`applicationId` figé** ; **keystore de débogage stable et suffixe `.dev`** ; saveurs `domainePublic` et `personnel` ; CI publique avec contrôle des droits des paquets embarqués ; CI privée ; `gradle.properties` pour 7,8 Gio ; `content/` sous uv (APSW 3.50.1, `fold.py` et ses vecteurs, gabarit du paquet golden) | 1-2 |
| **1a. Feuille et encre** | Pli AGSL en portrait (code Mekuri adapté) ; papier, thèmes, polices et sous-ensemble Junicode ; césure fr et la ; **mini-porte** : pli fluide et aucun glyphe manquant | 2-3 |
| **1b. Livre** | Paginateur Compose, lettrine, rubriques ; livre ouvert en paysage (reliure, tranche, double page) ; TalkBack ; variante de gestes (exclusion ou non) | 2-3 |
| **Porte A** | Critères chiffrés (§4.2). Si le pli échoue : natif avec glissement. Si la pagination ou la typographie échoue après une semaine de corrections : Readium. Sinon : natif confirmé | — |
| **2. Contenu du domaine public** | Paquet v1 (ADR 003) et pliage ; identifiants et alias ; paquets *Imitation*, *Prières* (relues, sans indulgences en jours), *Exercices*, *Bible* ; Clairval en saveur `personnel` si scénario A ; paquet golden | 4-6 |
| **3. Lecteur v1** | Bibliothèque, sommaire, position, rubans manuels, recherche, export JSON minimal | 4-6 |
| **Porte B1** | **2 semaines d'usage réel** sur votre téléphone ; corrections | — |
| **4. Calendrier, Aujourd'hui, méditation, images pieuses** | `core:liturgy` (1962 et 1969, France) ; banc d'oracles (romcal, Divinum Officium, Missalemeum ; JSONL commités ; `known-divergences.md`) ; 70 types d'année et balayage des années réelles ; Aujourd'hui sans messe ; séance de méditation ; images pieuses (catalogue de 30) | 5-8 |
| **5. Missel 1962** | Ingestion de Divinum Officium (héritage, conditionnels de 1960, alignement) ; Ordinaire du DP ; résolution de la messe du jour (contrat 03 ↔ 05) ; modes « assemblée » et « livre » ; vis-à-vis ; ruban automatique ; Évangile du jour 1962 ; français du Barroux dans la saveur `personnel` ; **liste des formulaires sans français du DP** | 5-8 |
| **6. Forme ordinaire (saveur `personnel`)** | Lectures AELF préchargées par la chaîne (plusieurs années, avec limitation de débit) ; Ordinaire 2021 ; recherche d'une source pour les oraisons ; même lecteur et mêmes modes que le 1962 ; bascule entre rites | 3-5 |
| **Porte B2 = MVP** | Usage quotidien ; corrections ; décision sur le passage à B selon les réponses obtenues | — |
| **V1** | Voir §3.2 | — |

**Phase 5, terminée le 13/10/2026 (0.6.6)** :
- Ordinaire de 1962 du domaine public (`content/sources/ordinaire`), d'un seul tenant, avec 14 emplacements de la messe du jour ;
- Propre de 1962 tiré de Divinum Officium (`content/sources/missel`) : 775 unités, dont 62 commémoraisons, latin public et français du Barroux dans le dépôt privé sous le même paquet ; propres de sainte Jeanne d'Arc et de sainte Geneviève en latin (missel de Pustet, propre de Paris) ; stations relues ;
- mode « livre » (missel avec le ruban de la messe du jour au ferret doré) et mode « messe intégrée » (l'Ordinaire où l'app insère le propre du jour, les oraisons des commémoraisons, la préface du jour en entier, les Communicantes et Hanc igitur propres, et omet le Gloria ou le Credo les jours qui ne les ont pas : `DayProper`) ;
- Aujourd'hui : messe du jour, messe entière, début de l'Évangile du jour ;
- latin et français en regard (réglage « la-fr », par défaut) ;
- liste des formulaires sans français du domaine public : `content/sources/missel/francais-domaine-public.md` (le meilleur candidat pour les combler est Mame 1890, environ 60 messes).

Reportés en V1 : le français du domaine public pour le paquet public ; les féries renvoyées au dimanche (un tiers du poids, mais leurs rubriques diffèrent) ; une seconde relecture des propres de Jeanne d'Arc et de Geneviève.

**Phase 6, en cours (0.6.9, 13/10/2026)**, saveur `personnel` seulement, dépôt privé :
- lectionnaire de l’AELF (`sources/aelf`) : les lectures de 2016 à juin 2027 (l’AELF publie environ huit mois à l’avance), rangées sous les clés que l'application calcule avec son calendrier de 1969 (célébration ou férie, année A/B/C ou paire/impaire) : elles valent pour toutes les années (99,8 % des jours jusqu’en 2045). `LiturgicalDay.temporal` donne la férie sous une mémoire ;
- Ordinaire de 2021 (`sources/ordinaire-2021`), d'après iBreviary, collationné avec les livrets d’Ajaccio et de Cambrai, avec les prières eucharistiques I à IV et 91 préfaces ; l'application y insère les lectures du jour ;
- Aujourd'hui en 1969 : lectures du jour, messe entière, début de l'Évangile ;
- oraisons de 2021 : aucune source licite et structurée ; encadrés « dans le missel ».

### 4.1 ADR de la phase 0 (une page chacun, `docs/adr/`)
1. **001 — Horizon, dépôts et saveurs** : scénario A, B ou C, statuts de droits, et quelle CI construit quoi.
2. **002 — Appareils et SDK** : téléphone de référence et second appareil, minSdk, `applicationId`, keystore de débogage.
3. **003 — Paquet v1** : DDL, tables `asset`, `devotion` et `calendar`, ECDSA P-256, champs de version (`formatMajor.formatMinor`, `packageVersion`, `minAppFormat`, `rights`), SQLite 3.50.1.
4. **004 — Texte canonique et ancres** : NFC sans U+00AD, points de code, `rev` = hachage, cascade de ré-ancrage.
5. **005 — Moteur de lecture et porte A** : critères et règle de décision.
6. **006 — Gestes** : par défaut, aucune exclusion du geste retour et un coin saisi à plus de 32 dp du bord ; la variante est testée en 1b ; zones de tap ; touches de volume ; priorités entre rubans, images et tourne-page.
7. **007 — Polices** : EB Garamond, Literata en option, sous-ensemble Junicode, liste des caractères garantis.
8. **008 — Données de l'utilisateur** : Room 3 (schémas exportés, migrations testées, jamais destructives), sauvegarde, export, chiffrement du journal (V1).

### 4.2 Critères de la porte A (détail : rapport 15, §5.2)
Les mesures se font sur le téléphone de référence, avec un APK *release* construit par la CI.
- **Fluidité du pli** : P95 de la durée d'image inférieur ou égal à la période de rafraîchissement, et moins de 1 % d'images manquées sur 100 tours. Mesure : `dumpsys gfxinfo framestats` et JankStats.
- **Repos et mémoire** : aucune recomposition pendant un tour, pic inférieur à 40 Mo, mémoire stable après 200 tours.
- **Pagination** : un chapitre en moins de 50 ms, le livre I de l'Imitation en moins de 1,5 s hors fil principal, une réouverture en moins de 200 ms.
- **Ancre** : identique après 10 rotations et 3 changements de taille de police.
- **Typographie** : aucun glyphe manquant dans le jeu de contrôle, 50 mots latins correctement coupés, lettrine alignée de 100 à 200 %.
- **TalkBack** : ordre de lecture correct et actions de page en français.
- **Gestes** : aucun retour système involontaire sur 50 saisies de coin.

### 4.3 Tests
- **Calendrier** : JVM pur, batterie G0 à G4 du rapport 05, 70 types d'année et années réelles (1970-2100 pour 1969, 2004-2050 pour 1962), cas manuels sourcés.
- **Contenu** : paquet golden dans `content/golden/` (chaque type de bloc, Ps 22/23, Ex. [1]-[20], Ordinaire mal aligné, ancres avant et après correction), build bit à bit identique, vecteurs de pliage partagés avec Kotlin.
- **Interface** : Roborazzi sur la VM ; Macrobenchmark en V1.
- **i18n** : aucune chaîne en dur (lint en CI), `java.time` avec une locale explicite, `LocaleList` par segment.

---

## 5. Décisions

### 5.1 Décisions de fondation (tranchées le 11/10/2026)
1. **Horizon de diffusion : A, puis B, et C en option.** Usage personnel d'abord, partage privé avec des proches ensuite, diffusion publique non prévue mais à garder en tête. Clairval, le français du Barroux et les textes AELF entrent dans la saveur `personnel`. La saveur publique reste strictement réservée au domaine public et aux licences libres, afin que le passage à B ou C ne demande aucune refonte.
2. **Demandes d'autorisation : quatre courriers rédigés** (Clairval, Barroux, AELF, livres-mystiques) dans [`docs/demarches/courriers.md`](../demarches/courriers.md). C'est vous qui les envoyez. Le délai butoir est de 8 semaines pour le Barroux et l'AELF, et ne concerne que le passage à B.
3. **Rites : 1962 et forme ordinaire.** Le 1962 vient d'abord (phase 5), la forme ordinaire ensuite (phase 6), toutes deux dans le MVP. La forme ordinaire reste dans la saveur `personnel` tant que l'AELF n'a pas donné d'autorisation.
4. **Dépôts : code public (GPL-3.0-or-later, calendrier sous Apache-2.0) et contenu privé** (`paroissien-content`).
5. **Téléphone de référence : Samsung Galaxy Z Flip6**, avec la dernière version d'One UI. Il s'agit sans doute d'Android 16 ou 17 : la phase 0 le confirmera par `adb shell getprop`, et l'ADR 002 consignera le résultat. Le spike commence donc bien par le pli AGSL.
   - **Matériel** [caractéristiques publiques, à confirmer] : Snapdragon 8 Gen 3, Adreno 750 et 12 Go de RAM. L'écran principal fait 6,7 pouces, 2640 × 1080 (22:9), à 120 Hz, ce qui fixe la porte A à un P95 ≤ 8,3 ms. L'écran externe fait 3,4 pouces.
   - **Haut de gamme.** Le téléphone suffit pour l'usage personnel (A). Avant le partage avec des proches (B), il faudra mesurer aussi sur un **second appareil de milieu de gamme** (API ≥ 33).
   - **Posture « livre ».** Tenu de côté et à demi plié, le Flip présente une charnière verticale (`FoldingFeature` VERTICAL et HALF_OPENED) : c'est une **double page naturelle, la charnière servant de reliure**. À tester au spike 1b.
   - **Posture « table »** (Flex mode, charnière horizontale). Posé à demi plié, le téléphone peut accueillir la **séance de méditation** : le texte ou l'image en haut, le minuteur et la cloche en bas. À tester en phase 4.
   - **Paysage, écran déplié.** La hauteur n'est que d'environ 380 à 410 dp selon la densité, soit environ 12 lignes par page en double page. Le seuil du mode Auto se règle sur l'appareil (porte A).
   - **Spécificités Samsung à tester** :
     - interaction entre « Modes et routines » et `AutomaticZenRule` (phase 4) ;
     - poignée des Edge panels contre la saisie des coins de page (ADR 006) ;
     - optimisation agressive de la batterie, qui retarde les alarmes (Angélus, V1).
   - **Écran externe (V1, à vérifier).** Un widget « Aujourd'hui » (fête et couleur liturgique) dans la Flex Window, si One UI accepte les widgets tiers.

### 5.2 À trancher avant une phase donnée
- **Avant la phase 2** :
  - nom divin dans le Crampon : « Yahweh » tel qu'imprimé, ou « le Seigneur » comme variante de texte construite par la chaîne avec ses propres ancres (04, 15) ;
  - affichage de la numérotation des psaumes (les identifiants suivent la versification propre à chaque édition).
- **Avant la phase 4** (tranché le 11/10/2026, ADR 009) : Ordo de référence d'un monastère (Le Barroux ou Fontgombault) ramené au romain de 1962 ; solennités extérieures activées par défaut ; droit en vigueur, plus la discipline traditionnelle en option ; 1962 tel qu'imprimé.
- **Après la phase 4** (tranché le 11/10/2026) :
  - **pas de plafond de taille** pour l'APK : le budget de 30 Mo d'IzzyOnDroid ne vaut que pour une diffusion publique, non prévue ; la CI affiche la taille sans l'imposer ;
  - **identité des livres** : chaque livre a sa reliure (cuir, dorure ou fer à froid, motif, nerfs), son papier (teinte, encre, grammage : fin pour un gros livre, épais pour un livret), sa tranche et ses rubans ; la couverture est la page d'avant la première ; un réglage « Livres : tous pareils » uniformise ;
  - **images pieuses** : aucune image fournie par défaut. L'utilisateur choisira lui-même une bibliothèque, et chacun peut numériser ses propres images (bords détectés, perspective, contraste, exposition, compression).
- **Le 12/10/2026** :
  - **images posées sur leur page** : la page vaut φ fois l'image, qui garde ses proportions ; un toucher la met de côté (elle dépasse alors par la tête), un appui long l'ouvre ; un réglage les range toutes en tête ; import possible « telle quelle », sans recadrage ni retouche ;
  - **ruban en travers de la page** (réglage, actif par défaut) : aux ondulations légères, il sort de la tête près du pli et pend dans la bande du bas ; un toucher le couche dans le pli ; rouvrir le livre par sa queue le remet en travers ;
  - **tissu des rubans** (réglage) : mat par défaut, avec un grain fin et de 3 à 7 côtes discrètes en long, propres à chaque ruban ; ou satin brillant ;
  - **réglages** : un écran complet, ouvert depuis l'accueil ou depuis Aa ; la barre du lecteur défile de côté, avec ‹ › quand des boutons sont cachés.
  - **cadres ornementaux** : quatorze cadres relevés sur un paroissien chromolithographié (`tools/cadres`) ; la règle suit le sommaire, un cadre par section, choisi d'abord par le sens (matin, soir, messe, confession, communion, vêpres, complies, mariage), sinon en boucle ; réglage « Cadres » ;
  - **ouverture du livre** : pris sur le rayon, il s'ouvre par sa couverture, puis la doublure (soie moirée dans une roulette dorée), avant la page ;
  - **atelier** (`ATELIER.md`) : moteur v3 (fait : dispositions partielles dont le côté extérieur suit le folio, pièces de bout, filets à distance fixe, débords, gardes à motif), second lot de scans, couvertures en modules, un habit par livre composé par l'utilisateur ;
  - **rangs d'habillage** : les livres nobles (prières, Imitation) ont un cadre par section et une doublure de soie ; la Bible est réglée d'un filet rouge ; les livres simples (exercices) ont une garde de papier à motif et des pages nues ; les lettres n'ont ni cadre ni garde ;
  - **vignettes gravées** : tirées des scans de l'utilisateur, hors du dépôt public (saveur personnelle) ;
  - **import et export** de livres (txt, epub, PDF) et d'images pieuses recto verso ; **sauvegarde complète** de l'app dans un fichier (réglages, signets, habits, éléments importés), sans cloud, **textes privés compris** : une app livrée vide se remplira en l'important, en attendant les autorisations ; saveur `vide` pour l'essayer dès maintenant.
- **Avant la phase 5** : mode de messe par défaut (« assemblée » recommandé) ; versions rubricales (1960 seulement au MVP).
- **À la porte A** : police par défaut (EB Garamond pour les livres liturgiques, Literata en option de lecture) ; seuil de double page sur téléphone.

### 5.3 Arbitrages adoptés par défaut (à contester si besoin)

| Sujet | Choix | Pourquoi |
|---|---|---|
| minSdk / target / compile | **29** / 36 / 37 | Unifie le NPD par `setAutomaticZenRuleState` (API 29) et permet de chaîner les polices de secours (`CustomFallbackBuilder`, API 29). Coût : environ 5 points de parc, 91,1 % contre 96,1 % (15). Sans objet en scénario A |
| Tourne-page | « Papier » par défaut, à partir de l'API 33 ; glissement de 29 à 32 ; options Glissement, Fondu ou Aucun ; aucune animation si les animations système sont coupées | 07, 13 |
| Mekuri | Reprendre le code (MIT, avec notice), sans en faire une dépendance ; traduire les libellés d'accessibilité | 07, 15 |
| Gestes | Zones de tap 25/50/25 ; validation à 35 % ou au-delà de 600 dp/s ; trois signaux haptiques ; pas de son ; exclusion selon l'ADR 006 | 07, 08, 15 |
| Double page | Réglage Auto/1/2 ; en Auto, au moins 300 dp par page, 38 caractères par ligne et 12 lignes | 07, 08, 13 |
| Bilingue | Vis-à-vis en paysage, alternance en portrait | 03, 06, 08 |
| Navigation et visuel | Quatre entrées (Aujourd'hui, Bibliothèque, Signets, Recherche) ; M3 habillé « papier et encre », sans Expressive ni couleur dynamique ; Clair, Sépia, Sombre, Nuit ; couleur liturgique en accent discret | 08 |
| Rubriques | Rouge et italique ; ℣ ℟ ✠ en rouge | 08 |
| Césure du latin | Motifs `la-x-liturgic` (MIT) appliqués à l'exécution (Liang en Kotlin) ou stockés à part ; jamais dans le texte canonique | 07, 15 |
| Rubans | Propres à chaque livre ; 7ᵉ ruban automatique au ferret doré (phase 5) ; extrémité ou initiale par couleur | 09, 08, 15 |
| Images pieuses | Aucune fournie par défaut : bibliothèque choisie par l'utilisateur ; images personnelles numérisées dans l'app (bords, perspective, contraste, exposition, compression ; EXIF supprimés), ou importées telles quelles ; posées sur leur page au nombre d'or, elles dépassent par la tête du livre une fois mises de côté | 09, 12, décisions des 11 et 12/10/2026 |
| Données de l'utilisateur | Room 3 ; sauvegarde cloud désactivée ; transfert d'appareil à appareil ; export proposé dès le premier ruban posé (ADR 008) | 09, 10, 15 |
| Paquets | Une base SQLite par paquet (ADR 003) ; `BundledSQLiteDriver` ; ABI `arm64-v8a` (plus `armeabi-v7a` pour la distribution) ; taille de l'APK affichée en CI, sans plafond (décision du 11/10/2026) | 10, 11, 15 |
| Requêtes de recherche | Construites à partir des jetons ; la saisie brute ne va jamais dans `MATCH` | 15 |
| Chaîne de contenu | Python et uv ; JSON canonique commité ; relecture par PR ; normalisation typographique (’ et U+202F) | 11 |
| Pile Android | Navigation 3, injection de dépendances manuelle, DataStore, WorkManager, kotlinx.serialization | 10 |
| Build | Tout en CI ; sur la VM, tests JVM, Roborazzi et adb ; jamais Gradle et un émulateur en même temps | 10, CLAUDE.md |
| Lectures en 1962 | Français du Propre (Barroux ou DP), sinon Glaire ; le Crampon sert à la lecture libre | 04, 15 |
| Textes de prière | « Profil de textes » qui suit le rite ; Traditionnel tant que l'AELF n'a pas répondu | 14 |
| Examen de conscience | Éphémère ; rien n'est écrit ; exclu des captures | 14 |
| Statistiques | Aucune : ni séries ni compteurs | 08, 13, 14 |
| Synthèse vocale | V1 ; la langue de chaque bloc et de chaque segment est gardée dès la phase 2 | 06, 13, 15 |
| Économie | Gratuit, éventuellement avec un bouton de don ; jamais d'abonnement | 13 |

---

## 6. Risques principaux et parades

| Risque | Parade |
|---|---|
| Refus ou silence de Clairval | Saveur `personnel` (A) ; lecteur qui récupère les lettres sur l'appareil sans redistribution ; liens |
| Refus du Barroux, ou absence de réponse 8 semaines après l'envoi | Latin seul ; français du DP pour les formulaires d'avant 1920 ; liste des lacunes de 1920 à 1962 (Christ-Roi, Assomption 1950, Semaine sainte 1955…) à traduire soi-même sous CC BY-SA, sinon latin seul |
| Français de l'Ordinaire de provenance inconnue | Ordinaire tiré d'un paroissien du DP, ajusté à 1955-1962 et relu |
| Contenus protégés diffusés par erreur via la CI publique | Saveurs séparées ; contrôle des droits des paquets en CI publique ; artefacts privés gardés 1 jour, jamais en Release |
| Moteur natif trop coûteux | Spike en deux temps et porte A chiffrée ; repli Readium |
| Performance du pli, pilotes GPU | Mesure sur le téléphone dès 1a ; second appareil ; liste de pilotes exclus ; glissement |
| Identifiants instables | Clés naturelles, réalignement flou, `id_alias` ; la CI bloque |
| Rubriques de 1962, nouveaux décrets | Oracles multiples, années réelles ; au MVP, correction par nouvel APK ; en V1, par paquet téléchargé |
| Coquilles ou erreurs doctrinales | Relecture par PR ; « Signaler une coquille » déclenché par l'utilisateur ; purge des indulgences en jours |
| Perte de la clé de signature | Sauvegarde hors VM (deux supports) ; signature locale possible avec `apksigner` |
| Mise à jour impossible (clé de débogage qui change) | Keystore de débogage stable et suffixe `.dev` |
| Périmètre et épuisement d'un développeur seul | Deux portes d'usage réel ; tout ce qui n'est pas B1 ou B2 va en V1 |
| Données sensibles (journal, intentions) | Stockage local, chiffrement, hors sauvegarde, aucune télémétrie (RGPD, art. 9) |
| VM limitée (7,8 Gio, 2 cœurs) | CI pour tout build ; `workers.max=2` ; aucun émulateur |
| Concurrence en 2026 | Expérience de livre, français, contenus uniques |

---

## 7. Démarches externes

Les quatre brouillons sont dans [`docs/demarches/courriers.md`](../demarches/courriers.md). Ils visent l'usage personnel (a) et le partage avec des proches (b), et posent sans s'engager la question d'une diffusion publique (c).

| Destinataire | Objet | Nécessaire pour |
|---|---|---|
| Abbaye Saint-Joseph de Clairval | Lettres spirituelles : texte intégral, crédit, synthèse vocale, langues, possibilité de retrait | B et C |
| Abbaye Sainte-Madeleine du Barroux | Traduction française du Propre de 1962 (Divinum Officium) | B et C |
| AELF | Lectures, Ordinaire 2021, prières usuelles actuelles, API | B et C |
| livres-mystiques.com | Courtoisie et formule de crédit (le texte est libre de droits) | — |

*Rien n'est envoyé sans votre validation explicite.*

---

## 8. Index des rapports

01 [Clairval](01-clairval-lettres.md) · 02 [Exercices](02-exercices-ignace.md) · 03 [Missel](03-missel-rites.md) · 04 [Bible et Imitation](04-bible-imitation.md) · 05 [Calendrier](05-calendrier-liturgique.md) · 06 [Moteur de lecture](06-moteur-lecture.md) · 07 [Tourne-page et papier](07-tourne-page-papier.md) · 08 [UX](08-ux-design-system.md) · 09 [Signets et images pieuses](09-signets-images-pieuses.md) · 10 [Architecture Android](10-architecture-android.md) · 11 [Chaîne de contenu](11-pipeline-contenu.md) · 12 [Juridique](12-juridique-licences.md) · 13 [Benchmark](13-benchmark.md) · 14 [Prières et extensions](14-prieres-devotions-extensions.md) · 15 [Revue critique](15-revue-critique.md)

> **Note de méthode.** Le quota de recherche web partagé s'est épuisé pendant la planification. Les points marqués [S] ou [C] dans les rapports restent à revérifier avant d'engager une décision. La revue 15 (§3 et §4) dresse la liste de ce qui a été vérifié et de ce qui ne l'a pas été.
