# 01 — Lettres spirituelles de l'Abbaye Saint-Joseph de Clairval

> Agent 01 · phase de recherche et de planification · relevés du 2026-10-11
> Légende : **[V]** vérifié par observation directe du site (URL en §6) · **[S]** supposé ou estimé · **[?]** inconnu, à vérifier.
> Méthode : 29 requêtes HTTP vers clairval.com, espacées de 10 s ou plus conformément au `Crawl-delay: 10` (sauf les deux premières, robots.txt puis page d'accueil, faites à 4 s d'intervalle avant lecture du délai). Aucun crawl : uniquement les sitemaps, l'API REST WordPress et un échantillon de 25 lettres. Aucun texte de lettre n'est reproduit ici.
> Le budget de recherche web partagé était épuisé avant ma première requête. La question 1b (autre lettre mensuelle à Flavigny) reste donc ouverte, voir §2.1.

---

## 1. Résumé

- **[V] Les « lettres mensuelles de Flavigny » sont bien les lettres de Clairval.** La page d'abonnement parle des « Lettres mensuelles de l'abbaye Saint-Joseph de Clairval ». L'éditeur, la Communauté des Bénédictins de Saint-Joseph de Clairval, a son siège au 6 Grande Rue, 21150 Flavigny-sur-Ozerain. Le PDF porte l'ISSN 1956-3876 (édition française). Je n'ai pas pu vérifier s'il existe une autre lettre mensuelle à Flavigny (§2.1).
- **[V] Le site est un WordPress (WPML, Yoast, WPBakery) qui expose une API REST publique** (`/wp-json/wp/v2/posts?lang=xx`). Elle renvoie le corps complet de chaque lettre, les liens vers ses traductions et ses mots-clés. Environ 25 requêtes suffisent pour tout le corpus, contre environ 2 000 pages HTML (5 h 30 avec le délai de 10 s).
- **[V] Corpus en ligne au 2026-10-11** : 313 lettres en français, du 25/07/1996 au 25/12/2025 (environ 11 par an), 306 en anglais, 300 en espagnol, 300 en italien, environ 309 en allemand et environ 300 en néerlandais. Après dédoublonnage, cela fait environ 1 840 lettres distinctes. Le portugais et le catalan sont diffusés sur papier mais absents du site.
- **[V] Aucune lettre de 2026 n'est en ligne.** L'archive présente les lettres « des années précédentes », et les traductions suivent le français avec 4 à 14 mois de retard. **Il n'existe donc pas de « lettre du mois » en ligne**, et cette fonction doit être redéfinie (§2.6).
- **[V] Le format HTML est vraiment stable.** Les 25 lettres examinées (1996 à 2025, en français, anglais, néerlandais et italien) suivent toutes le même gabarit à classes `lettre-*` : date, h1, salutation, 5 à 9 intertitres h2, paragraphes et italiques. Il n'y a **aucune note de bas de page** en HTML. Les images n'existent que depuis 2016 (une image française commune à toutes les langues) et les PDF depuis 2006. La bibliographie figure seulement dans le PDF.
- **[V] Droits.** Chaque lettre affiche une mention : publier la Lettre dans une revue, un journal ou sur un site **exige une autorisation**, à demander par la page de contact. Les lettres sont aussi vendues en livres (7 tomes) et en CD audio. **Embarquer le corpus dans l'APK sans accord écrit est le risque majeur.** La prise de contact avec l'abbaye bloque toute distribution.
- **[V] robots.txt** impose `Crawl-delay: 10` et interdit le chemin `/wp-content/themes/twentyseventeen-child/assets/lettres`. L'API REST, les sitemaps, `/documents/*.pdf` et les images ne sont pas interdits.
- **[V] Anomalies de données à gérer.** Depuis 2014 environ, des lettres italiennes sont dupliquées sous `/nl/` (environ 95 à 120) et sous `/de/` (environ 13). La typographie est irrégulière (espaces insécables ajoutées en JavaScript côté client, `<sup>` parasites, quelques mots cassés). Des lettres déjà publiées sont parfois corrigées.
- **Volume [S, mesuré sur échantillon].** En français : environ 6,8 Mo de texte brut, soit environ 2,5 Mo compressés, plus 3 à 4 Mo d'index de recherche. Pour les 6 langues : environ 40 Mo, soit environ 14 Mo compressés. Les images sont environ 105 fichiers communs, de 4 à 6 Mo en WebP. Les PDF pèsent environ 100 Mo en français et environ 600 Mo au total : on ne les embarque pas.
- **Recommandation.** Le MVP se limite au français. Le pack de contenu est construit hors de l'appareil par le pipeline (11), avec un lecteur dédié : sommaire par année, intertitres, recherche plein texte, index des saints, signets et carte « Aujourd'hui » (fête du saint du jour, sinon anniversaire de parution). Le multilingue, le thésaurus de thèmes, les images et la bibliographie arrivent en V1.

---

## 2. Constats détaillés

### 2.1 Identification de la lettre et de Flavigny

| Constat | Statut | Preuve |
|---|---|---|
| La page d'abonnement emploie l'expression « Lettres mensuelles de l'abbaye Saint-Joseph de Clairval » | [V] | `/la-lettre-spirituelle/recevoir-la-lettre-gratuitement/` |
| La lettre est publiée chaque mois en 8 langues : français, anglais, allemand, espagnol, italien, néerlandais, portugais et catalan | [V] | `/la-lettre-spirituelle/presentation-de-la-lettre/` |
| Éditeur : « La Communauté des Bénédictins de Saint-Joseph de Clairval », congrégation reconnue (décret du 2 octobre 1992), siège au 6 Grande Rue, 21150 Flavigny-sur-Ozerain, hébergeur Icodia, directeur de la publication : le Père Abbé | [V] | `/mentions-legales/` |
| Le pied du PDF indique l'adresse de Flavigny-sur-Ozerain, l'ISSN 1956-3876 (édition française) et un dépôt légal à la date de parution : c'est un périodique enregistré | [V] | `/documents/FR-2025-12-25.pdf` (métadonnées seulement) |
| L'abbaye a été fondée en Suisse (à Clairval) puis s'est installée à Flavigny dans les années 1970 | [S] | Connaissance générale, page `/abbaye/histoire-abbaye-saint-joseph-de-clairval/` non consultée |
| Autre lettre mensuelle à Flavigny | **[?]** | Non vérifié faute de budget de recherche. Pistes à vérifier : le séminaire de la Fraternité Saint-Pie-X installé à Flavigny publie un bulletin pour ses amis et bienfaiteurs, de périodicité inconnue ; la confiserie des Anis de Flavigny diffuse une lettre commerciale ; l'abbaye elle-même a une page « Nouvelles / chronique de l'abbaye » [V : la page existe], distincte de la Lettre spirituelle. |

**Conclusion.** L'identification est quasi certaine : l'expression de l'utilisateur correspond mot pour mot à la page d'abonnement. Il reste à faire confirmer par l'utilisateur, par exemple avec un exemplaire papier reçu (en-tête « Abbaye Saint-Joseph de Clairval », ISSN 1956-3876).

### 2.2 Cartographie du site

**Pile technique [V]** : Apache, PHP 8.2, WordPress avec WPML (`sitepress`, `wpml-ls-*`), Yoast SEO 28.6, WPBakery (`vc_row`), Shortcodes Ultimate (spoilers), Contact Form 7 avec reCAPTCHA, et le thème enfant `twentyseventeen-child` avec des shortcodes maison `csc_lettre_*`.

| Ressource | URL ou motif | Constat |
|---|---|---|
| robots.txt | `/robots.txt` | [V] `Crawl-delay: 10`. Interdits : `/wp-content/themes/twentyseventeen-child/assets/lettres` et un dossier d'import. **Ne jamais accéder au premier**, qui hébergeait peut-être l'ancien site statique des lettres [S]. |
| Sitemap | `/sitemap.xml` → `/sitemap_index.xml` (Yoast) | [V] Contient `post-sitemap.xml` (1 000 URL), `post-sitemap2.xml` (960) et `page-sitemap.xml` (106). Les URL des 6 langues sont mélangées. `lastmod` donne la date de **modification**, pas de parution. Les annotations `hreflang` ne pointent que vers la page elle-même : aucun lien entre traductions. |
| API REST, lettres | `/wp-json/wp/v2/posts?lang={fr,en,de,es,it,nl}` | [V] En-têtes `X-WP-Total` et `X-WP-TotalPages`. Paramètres `per_page` (100 au plus), `page`, `_fields`, `slug=a,b`, `include=id1,id2` : tous testés et fonctionnels. Sans `lang`, l'API renvoie le français. |
| API REST, catégories | `/wp-json/wp/v2/categories` | [V] `lettre-fr` (313) et `lt-fr-1996` à `lt-fr-2025`. En anglais : `lettre-an` et `lt-an-AAAA`. Il existe aussi des catégories de retraites, sans rapport avec les lettres. |
| Flux RSS | `/feed/`, `/en/feed/`, et vraisemblablement `/{lang}/feed/` | [V] pour `/feed/` (français, 10 éléments, **extraits seulement**, `ETag` et `Last-Modified` présents) ; [V] lien vers `/en/feed/` ; [S] pour les autres langues. |
| Archive HTML (fr) | `/lettres-en-ligne-6/` (2021-2025), `-5` (2016-2020), `-4`, `-3`, `-2`, `-1` (1996-2000) | [V] Une page par tranche de 5 ans, avec titre, date et mots-clés. Le lien « Lettres précédentes » mène à la page suivante. |
| Archive HTML (autres langues) | `/en/archived-newsletters-1…6/`, `/es/cartas-en-linea-1…6/`, `/de/der-geistliche-rundbrief/rundbriefarchiv/`, `/it/la-lettera-spirituale/archivio-delle-lettere/` (et `/lettere-dallabbazia/`) | [V] D'après le sitemap des pages et les liens « Autres langues ». **Le néerlandais n'est pas relié** depuis l'archive française [V]. |
| Lettre (permalien) | FR `/{slug}/`, autres langues `/{lang}/{slug}/` | [V] Slug tiré du titre, avec parfois un suffixe `-2` pour les doublons. |
| Archives par date | `/AAAA/MM/JJ/` | [V] Liens présents dans l'archive. |
| PDF | `/documents/{CODE}-{AAAA-MM-JJ}.pdf` | [V] Codes observés : FR, AN, FL et IT ; [S] AL et ES pour l'allemand et l'espagnol. Disponibles **depuis 2006** dans toutes les langues ; avant, la requête renvoie 404 [V]. Taille de 345 à 570 Kio, 4 pages A4, mise en page InDesign. |
| Image de lettre | `/wp-content/uploads/img-lettres/img_lettre-FR-{AAAA-MM-JJ}.jpg` | [V] Depuis 2016. **Toujours le nom de fichier français**, réutilisé par les traductions. Environ 74 Kio pour l'échantillon. |
| Abonnement | formulaire Contact Form 7 avec reCAPTCHA | [V] Adresse postale **obligatoire** et courriel obligatoire, choix parmi 8 langues, mention de déclaration CNIL. L'envoi est **postal** et gratuit. Aucune liste de diffusion par courriel ni lettre électronique publique détectée [?]. |
| Vidéo | `/video-lettre-fr`, `/wp-content/uploads/2025/10/flavigny-lettre-fr.mp4` | [V] Vidéo de présentation, hors périmètre. |

**Identifiant interne « idlt » [V].** Le site désigne chaque lettre par `{CODE}-{AAAA-MM-JJ}`, par exemple `FR-2025-12-25`. On retrouve ce code dans les shortcodes Yoast `[csc_lettre_titre_seo idlt="FR-2025-12-25"]`, les noms des PDF, les noms des images et les identifiants CSS (`#titre_FR-2025-10-15`). Les codes de langue du site sont `fr`, `an` (anglais), `al` (allemand), `es`, `it` et `fl` (flamand, c'est-à-dire néerlandais), d'après les classes `lettre-spoiler-a-{code}`. **C'est la clé naturelle du schéma canonique.**

**Comptages par langue** (sitemaps du 2026-10-11, recoupés par `X-WP-Total` pour le français et l'anglais) :

| Langue | Posts | 1re date | Dernière date | Posts avec PDF (≥ 2006) | Posts avec image (≥ 2016) | Remarque |
|---|---|---|---|---|---|---|
| fr | 313 [V] | 1996-07-25 [V] | 2025-12-25 [V] | 209 | 105 | Langue originale. Les traductions suivent avec 4 à 14 mois de retard. |
| en | 306 [V] | 1996-11-20 | 2025-12-31 [V] | 209 | 98 | Certaines lettres anciennes ne sont pas traduites (Perboyre 1997, par exemple). |
| de | 322 | 1996-11-25 | ≈ fin 2025 [S] | 222 | 100 | Environ 13 doublons italiens mal classés [S] |
| es | 300 | 1997-07-15 | ≈ 2025-12 [S] | 208 | 94 | |
| it | 300 | 1997-07-15 | ≈ 2025-12 [S] | 208 | 92 | |
| nl | 419 | 1997-02-21 | ≈ fin 2025 [S] | 327 | 182 | Environ 20 posts par an depuis 2014 : **lettres italiennes dupliquées sous `/nl/`** [V sur 2 cas]. Le néerlandais réel compte environ 300 à 325 lettres [S]. |
| **Total** | **1 960** | | | **1 383** | | **Environ 1 840 lettres distinctes** après dédoublonnage [S] |

**Cadence [V].** Entre deux lettres françaises, l'écart va de 22 à 45 jours (médiane 35 jours), soit 10 à 13 lettres par an et environ 11 en moyenne. Toutes les dates sont à minuit, heure de Paris (`date` locale ; `date_gmt` tombe la veille à 22 h ou 23 h UTC).

**Décalage de mise en ligne [V/S].** Au 2026-10-11, la lettre française la plus récente en ligne date du 25/12/2025. L'archive annonce les lettres « éditées les années précédentes ». La page `/lettres-en-ligne-6/` a été modifiée le 2026-02-23 et le PDF de décembre 2025 a été créé le 2025-11-19. **Hypothèse [S] :** les lettres d'une année sont mises en ligne d'un bloc au début de l'année suivante, puis les traductions arrivent au fil de l'eau. Le pipeline doit donc s'attendre à un gros lot annuel (environ 11 lettres françaises) et à un flux de traductions, pas à une lettre par mois.

**Corrections a posteriori [V].** La lettre sur Jean van Ruysbroeck (15/10/2025) a été modifiée le 2026-02-23. Plusieurs posts allemands et anglais ont été modifiés en mai 2026. Il faut donc versionner les lettres et prévoir des ancres de signets tolérantes aux corrections (§2.6).

### 2.3 Structure HTML : un format stable, avec quelques variantes

**Échantillon de 25 lettres examinées [V].** En français, 20 lettres : 1996 (×2), 1997, 1998, 2000, 2002, 2003, 2005, 2008, 2010, 2012, 2014, 2015 (×2), 2017, 2019, 2021, 2023, 2024 et 2025. En anglais : 2012 et 2025. En néerlandais : 2016 et 2020. En italien (classée sous `/nl/`) : 2016. J'ai consulté l'API REST pour toutes ces lettres et la page HTML rendue pour deux d'entre elles (FR 2025, EN 2025).

**Gabarit commun.** Les lettres ont été réimportées lors de la refonte du site en 2023-2024 (le favicon est daté de 2023-12 et le pictogramme PDF de 2024-02). Le texte ci-dessous est remplacé par des « … ».

```html
<div class="wpb-content-wrapper">
  [vc_row …] …shortcodes WPBakery non rendus dans la REST…
  <div class="su-spoiler … lettre-spoiler-versions">              <!-- liens vers les traductions -->
    <div class="su-spoiler-content">
      <a class="lettre-spoiler-a-an" href="/en/…/">…</a>
      <a class="lettre-spoiler-a-al" href="/de/…/">…</a> …          <!-- fr | an | al | es | it | fl -->
  <a class="lettre-lien-telecharger-en-pdf lettre-lien-telecharger-en-pdf-fr"
     href="https://www.clairval.com/documents/FR-AAAA-MM-JJ.pdf">   <!-- ≥ 2006 -->
  <p class="lettre-para-date">25 décembre 2025</p>
  <h1 class="lettre-titre-h1">Bienheureuse …<br>
      <span class="lettre-soustitre">… (1897-1929)</span></h1>  <!-- sous-titre : lettres de 2025 seulement -->
  <p class="lettre-para-intro">Bien chers Amis,</p>               <!-- Dear Friends, / Dierbare Vrienden, / Carissimi Amici, -->
  <div class="lettre-div-contenu">
    <p class="lettre-premier-para">…</p>                          <!-- lettrine en CSS ::first-letter -->
    <p class="lettre-deuxieme-para"><img class="img_lettre" alt="…" src="/wp-content/uploads/img-lettres/img_lettre-FR-….jpg">…</p>
    <p>… <i>…</i> (Mt 5, 48) …</p>
    <h2 class="lettre-sous-titre-h2">…</h2>                         <!-- 5 à 9 par lettre, 6 le plus souvent -->
    <p class="lettre-para-apres-h2">…</p>
    <p>…</p>
    <p></p>                                                       <!-- paragraphe vide final, toujours présent -->
  </div>
  [/vc_column_text]…[vc_widget_sidebar …]…                         <!-- mention de droits : widget absent de la REST -->
  <div class="lettre-row-vignettes …">                            <!-- 4 « autres lettres » : à ignorer -->
    … <style>#titre_FR-…>strong:after{content:"…"}</style> …       <!-- titres injectés en CSS -->
</div>
```

| Élément | Sélecteur | Présence | Remarques |
|---|---|---|---|
| Date | `p.lettre-para-date` | 25/25 | Format propre à chaque langue (« 25 décembre 2025 », « December 31, 2025 », « 29 Juli 2020 », « 9 Marzo 2016 »). Préférer le champ REST `date`. |
| Titre | `h1.lettre-titre-h1` (premier nœud texte) | 25/25 | Contient souvent un espace insécable final. Le statut indiqué (Bienheureux, Vénérable…) est **celui de l'époque de rédaction**. |
| Sous-titre | `span.lettre-soustitre` dans le h1 | Lettres de 2025 seulement (11 FR et EN) | Fonction et dates de vie. L'extrait REST le reprend dans `span.soustitre-extrait-lettre`. |
| Saint ou thème (mots-clés) | `excerpt` REST, liste séparée par des virgules | 313/313 en français | **674 mots-clés distincts**, 3,3 par lettre en moyenne, non normalisés (par exemple « Education » sans accent, singulier et pluriel mêlés). |
| Salutation | `p.lettre-para-intro` | 25/25 | |
| Corps | `div.lettre-div-contenu` | 25/25 | 23 à 33 blocs, de 3 170 à 3 860 mots, de 17 800 à 22 900 caractères. |
| Intertitres | `h2.lettre-sous-titre-h2` | 25/25 | 5 à 9 par lettre. En 1996, intertitres TOUT EN CAPITALES. |
| Citations | Guillemets « » dans le texte, souvent en `<i>`. Références entre parenthèses, par exemple « (Ga 2, 20) » | 25/25 | **Aucun `<blockquote>`.** Les références bibliques peuvent être détectées par expression régulière, ce qui ouvre un lien avec le module Bible (04). |
| Notes de bas de page | — | **0/25** | Aucun appel de note ni ancre. Les `<sup>` servent aux ordinaux et abréviations (`er`, `e`, `gr` pour Mgr, `os` pour nos) ou sont des **artefacts** (guillemet ou espace dans un `<sup>`). |
| Bibliographie | Uniquement dans le PDF (bloc final de « sources ») | Vu sur le PDF de 2025 [V] ; ailleurs [S] | Absente du HTML. On pourrait l'extraire des PDF depuis 2006. |
| Images | `img.img_lettre` à l'intérieur du 2e paragraphe | ≥ 2016 (105 en français) | `alt` contient le titre. Mêmes fichiers pour toutes les langues. Droits sur les photos inconnus [?]. |
| PDF | `a.lettre-lien-telecharger-en-pdf-{lang}` | ≥ 2006 | Le préfixe du nom de fichier donne la **vraie langue** de la lettre. |
| Traductions | `.lettre-spoiler-versions a.lettre-spoiler-a-{code}` | Présent dès qu'une traduction existe | Vide pour la lettre française la plus récente (pas encore traduite). Donne le **graphe des traductions**, utilisable même avant 2006. |
| Mention de droits | `.lettre-mention-droit-usage` | Page HTML seulement (widget) | Voir §2.4. |
| Signature (abbé) | — | Absente du HTML et absente du texte du PDF de 2025 | [?] Peut-être une image dans le PDF. |
| Numéro de parution | — | Absent | Aucun numéro trouvé dans le HTML ni dans le PDF de 2025 : il faudra **calculer un rang**. |

**Le format est-il stable ?** **Oui, de façon remarquable** : même gabarit pour 1996 comme pour 2025, et dans toutes les langues testées. Cette stabilité tient toutefois à **une réimportation unique** : un changement de thème ou de constructeur de pages pourrait tout casser d'un coup (§5). Il faut gérer les variantes suivantes :

1. Le sous-titre n'existe que pour 2025 et après.
2. L'image n'existe qu'à partir de 2016, insérée à l'intérieur d'un paragraphe : il faut la déplacer dans un `<figure>`.
3. Le lien PDF n'existe qu'à partir de 2006 ; avant, il n'y a ni PDF ni préfixe de langue.
4. Le bloc des traductions peut être vide ou incomplet, et pointer vers un doublon mal classé (par exemple `it` → `/nl/beata-…/` ou `/de/santa-gertrude/`).
5. **Doublons WPML** : la même lettre italienne existe sous `/it/`, `/nl/` et parfois `/de/`. On dédoublonne avec la clé (code de langue réel, date). Le code vient du PDF ; pour les lettres d'avant 2006, il vient de la salutation et du bloc des traductions.
6. Les shortcodes `[vc_*]` et `[/vc_*]` ne sont pas rendus dans la REST et leurs guillemets sont typographiés (`»`, `″`) : il faut les retirer avant le parsing.
7. Les espaces insécables françaises sont irrégulières : un script du site (« Correction ajout des espaces insécables FR ») les ajoute **côté client**. Le texte anglais contient aussi des espaces insécables injustifiées (`In\xa01621`).
8. Il y a des coquilles de conversion, par exemple un mot coupé autour d'un « ? » dans une lettre de 2015, ou un `<sup>` contenant « « ». **Ne pas les corriger automatiquement** : on ne peut modifier l'œuvre qu'avec un accord (§4).
9. Le paragraphe vide final est systématique.
10. Les titres sont parfois collectifs (« Saints Louis Versiglia et Calixte Caravario », « Louis et Zélie Martin ») ou thématiques (« Miracles eucharistiques », « Apparition mariale à Knock », « Jubilé an 2000 »).

**Répartition des sujets** (titres des 313 lettres françaises) [V] : 100 « Saint/Sainte », 116 « Bienheureux/Bienheureuse », 18 « Vénérable » et 79 autres. Ces 79 comprennent des laïcs et convertis (Chesterton, Psichari, Jérôme Lejeune…), des serviteurs de Dieu, des personnes devenues saintes depuis (Carlo Acutis, John Henry Newman, Mère Teresa, Pier Giorgio Frassati…), des apparitions, des miracles et des événements. **Le statut canonique actuel ne peut pas venir du titre** : il faut une table de personnes tenue à jour.

### 2.4 Conditions d'utilisation et droits

| Source | Contenu | Statut |
|---|---|---|
| Mention en bas de chaque lettre (`.lettre-mention-droit-usage`), en français et en anglais | « Pour publier la Lettre spirituelle […] une autorisation est nécessaire » ; demande par la page de contact. Le texte vise explicitement les revues, les journaux et **les sites internet**. La version anglaise dit la même chose. | [V] |
| Mentions légales | Identité de l'éditeur, hébergeur et directeur de la publication. **Aucune clause de licence, aucune CGU, aucune mention ©** sur le site. | [V] |
| PDF | ISSN et dépôt légal : c'est un périodique. Pas de mention « © » relevée dans le texte extrait. | [V] (PDF de 2025 seulement) |
| Exploitation commerciale | Recueils « vies de saints et de convertis » en 7 tomes de 20 vies, et CD audio, vendus sur traditions-monastiques.com, la boutique de l'abbaye. | [V] (liens de la page de présentation) |
| Analyse juridique | En droit français, toute reproduction ou représentation sans consentement est illicite (CPI L.122-4). Les exceptions de copie privée et de courte citation (L.122-5) ne couvrent pas la diffusion d'un corpus dans une application. Le droit *sui generis* du producteur de base de données (L.341-1) pourrait aussi s'appliquer à l'archive. Une application qui distribue les lettres équivaut à une « publication sur un site » au sens de la mention, a fortiori. | [S] à confirmer par l'agent 12 |

**Conséquence.** Il faut **demander l'autorisation à l'abbaye avant toute distribution**, que le corpus soit dans l'APK ou dans un pack téléchargeable depuis nos serveurs. Le scénario de repli est le « lecteur » : l'application télécharge elle-même les lettres depuis clairval.com à la demande de l'utilisateur, comme un lecteur RSS, et les garde en cache local. C'est moins exposé, mais pas sans risque, puisqu'on reformate le texte et qu'on le stocke hors ligne. C'est aussi plus fragile techniquement. **La courtoisie et l'esprit du projet plaident pour une demande en bonne et due forme.** La lettre de demande devrait proposer : application gratuite, sans publicité ni pistage ; attribution complète, lien source, liens « S'abonner » et « Soutenir » ; texte non modifié ; retrait sur simple demande ; et éventuellement un accès direct aux données (export, ou portugais et catalan absents du site).

### 2.5 Conception du scraper (pipeline 11, module `sources/clairval`)

**Principes.**
- **La source primaire est l'API REST**, pas le HTML : elle est plus légère pour le serveur et plus propre à parser. Le **repli HTML** passe par les sitemaps et les mêmes sélecteurs `lettre-*` si l'API venait à être fermée (401, 403 ou 404).
- **Politesse** : une seule connexion, au moins 10 s entre deux requêtes (`Crawl-delay`), un User-Agent identifiable avec une adresse de contact, `Retry-After` respecté, backoff exponentiel sur 429 et 5xx, arrêt après N erreurs. Le chemin interdit par robots.txt n'est jamais visité. **L'appareil de l'utilisateur ne scrape rien** dans le scénario autorisé : seul le pipeline de CI le fait.
- **Cache brut immuable** : chaque réponse est conservée telle quelle (`raw/rest/{lang}/{fetched_at}-p{n}.json.gz`, avec son sha256), puis éclatée par post (`raw/posts/{wp_id}/{modified_gmt}.json`). On ne retélécharge jamais un post dont le `modified_gmt` n'a pas changé.
- **Reprise** : un fichier d'état SQLite (curseur par langue, dernier `modified_gmt` vu, pages terminées) rend les opérations idempotentes et les relances sans effet de bord.
- **Mise à jour incrémentale** : `modified_after={dernier_sync}` et `orderby=modified`, langue par langue. [S] Ces paramètres existent depuis WordPress 5.7 ; la version du site est inconnue mais vraisemblablement récente (PHP 8.2, Yoast 28.6). **À vérifier au premier passage.** Pour détecter un changement à moindre coût, on fait un GET conditionnel sur `/feed/` et `/{lang}/feed/` (`If-None-Match` sur l'`ETag`) et on compare le `lastmod` du `sitemap_index`. Cadence proposée : **hebdomadaire en CI**, car la mise en ligne se fait par lots annuels et les traductions arrivent au fil de l'eau.
- **Coût d'un passage complet** : 6 langues × 7 pages de 50 lettres ≈ 40 requêtes, soit environ 7 minutes à 10 s d'intervalle. Les réponses font environ 1,5 Mo chacune. Je recommande `per_page=50` plutôt que 100, pour ménager le serveur. Un passage incrémental coûte de 6 à 12 requêtes.
- Images : un seul téléchargement par fichier `img_lettre-FR-*.jpg`. PDF : pas de téléchargement par défaut ; en V1, uniquement pour extraire la bibliographie.

**Esquisse** (non exécutée, à titre d'illustration de la conception) :

```python
# sources/clairval/fetch.py — esquisse
BASE = "https://www.clairval.com/wp-json/wp/v2/posts"
FIELDS = "id,date,date_gmt,modified_gmt,slug,link,title,excerpt,content,featured_media"
LANGS = ("fr", "en", "de", "es", "it", "nl")

class Polite:                                   # httpx.Client, 1 connexion
    delay = 10.0                                # robots.txt Crawl-delay
    def get(self, url, params):
        sleep_until(self.last + self.delay)
        for attempt in range(5):
            r = self.client.get(url, params=params); self.last = monotonic()
            if r.status_code in (429, 500, 502, 503, 504):
                sleep(int(r.headers.get("Retry-After", 60 * 2 ** attempt))); continue
            r.raise_for_status(); return r
        raise Abort(url)

def sync_lang(http, store, lang):
    since = store.cursor(lang)                  # None => passage complet
    page = store.resume_page(lang) or 1
    while True:
        p = dict(lang=lang, per_page=50, page=page, orderby="modified", order="asc", _fields=FIELDS)
        if since: p["modified_after"] = since
        r = http.get(BASE, p)
        store.save_raw(lang, page, r.content)   # gzip + sha256, immuable
        for post in r.json():
            store.upsert_post(post)             # clé (wp_id, modified_gmt) => idempotent
        store.checkpoint(lang, page, max_modified(r.json()))
        if page >= int(r.headers.get("X-WP-TotalPages", 1)): break
        page += 1
    store.commit_cursor(lang)
```

**Normalisation** (`sources/clairval/normalize.py`, BeautifulSoup avec lxml, ou selectolax ; pas de JavaScript) :
1. Retirer les shortcodes avec `\[/?vc_[^\]]*\]` et supprimer `.lettre-row-vignettes`, les `<style>` et les `<script>`.
2. Extraire les métadonnées : date (REST `date`, recoupée avec `p.lettre-para-date` et des tables de mois par langue) ; titre et sous-titre (h1 découpé sur `<br>`) ; salutation ; URL du PDF et **code de langue réel** ; liens de traduction ; mots-clés (extrait REST, sans le span de sous-titre, découpé sur `,` et `;`).
3. Découper en sections : un intertitre h2 ouvre une nouvelle section ; tout ce qui précède le premier h2 forme la section 0, sans titre.
4. Traiter les blocs inline : liste blanche `i`→`em`, `b`/`strong`, `sup` (seulement si le contenu correspond à `^(er|re|e|ème|os|o|gr|me|mes|th|st|nd|rd)$`, sinon on ne garde que le contenu), `sub`→texte ; suppression des attributs `style` et des paragraphes vides.
5. Sortir `img.img_lettre` du paragraphe et la placer dans un `<figure>` qui précède ce paragraphe.
6. Typographie, **sans toucher aux mots** : NFC ; espace fine insécable U+202F avant `; ! ?` et insécable U+00A0 avant `:` et à l'intérieur des guillemets « » (français uniquement) ; suppression des insécables parasites dans les autres langues. L'opération est idempotente.
7. Détecter les références bibliques (`\((?:cf\. )?[1-3]?\s?[A-Z][a-z]{0,3}\s?\d+,\s?\d+(?:-\d+)?\)`) et les annoter sans modifier le texte (V1, lien avec 04).
8. Attribuer des identifiants stables `s{n}p{m}` à chaque bloc, utiles aux signets, à la synthèse vocale et à la recherche.
9. Dédoublonner par (code de langue réel, date). En cas de doublon, on préfère l'URL dont le préfixe correspond à la langue (`/it/` pour l'italien).
10. Construire le graphe des traductions : `work_id` = date de l'original français (`clairval:AAAA-MM-JJ`), résolue par le lien `fr` des traductions ou par le bloc des traductions de la page française.
11. Valider : un seul h1, au moins 3 intertitres, une salutation, entre 12 000 et 30 000 caractères, aucun reste `[vc_`, une date cohérente. Les échecs vont dans un rapport et ne sont jamais publiés silencieusement.
12. **Tests de non-régression** sur les 25 lettres de l'échantillon, en version figée.

**Schéma canonique (JSON, une entrée par lettre et par langue)** :

```json
{
  "id": "clairval:fr:2025-12-25",
  "idlt": "FR-2025-12-25",
  "work_id": "clairval:2025-12-25",
  "lang": "fr", "site_lang_code": "fr",
  "seq": 313,
  "date": "2025-12-25",
  "title": "Bienheureuse Dina Bélanger",
  "subtitle": "Religieuse … (1897-1929)",
  "subject": { "kind": "person|group|event|theme",
               "person_refs": ["person:dina-belanger"],
               "status_at_writing": "bienheureux" },
  "keywords_raw": ["…", "…"],
  "themes": ["vie-interieure", "musique"],
  "salutation": "Bien chers Amis,",
  "sections": [
    { "id": "s0", "heading": null, "blocks": [ { "id": "s0p0", "type": "p", "role": "incipit", "html": "…" } ] },
    { "id": "s1", "heading": "…", "blocks": [ … ] }
  ],
  "notes": [],
  "sources": [],
  "scripture_refs": [ { "ref": "Mt 5,48", "block": "s4p1" } ],
  "images": [ { "file": "img/FR-2025-12-25.webp", "orig_url": "…/img_lettre-FR-2025-12-25.jpg",
                "alt": "…", "w": 800, "h": 480, "sha256": "…", "rights": "unknown" } ],
  "translations": { "en": null, "de": null, "es": null, "it": null, "nl": null },
  "source": { "url": "https://www.clairval.com/bienheureuse-dina-belanger/", "wp_id": 18402,
              "pdf_url": "https://www.clairval.com/documents/FR-2025-12-25.pdf",
              "fetched_at": "…", "modified_gmt": "2025-12-24T23:00:00" },
  "hash": { "raw_sha256": "…", "content_sha256": "…" },
  "license": { "status": "all-rights-reserved", "permission": "pending",
               "attribution": "Abbaye Saint-Joseph de Clairval, Flavigny-sur-Ozerain", "permission_ref": null },
  "quality": { "issues": ["sup-artefact"], "version": 3 }
}
```

**XHTML sémantique produit**, consommé par le moteur de lecture (06) :

```html
<article xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"
         lang="fr" id="clairval-fr-2025-12-25" class="lettre">
  <header>
    <p class="date"><time datetime="2025-12-25">25 décembre 2025</time></p>
    <h1>…</h1>
    <p class="subtitle">…</p>
  </header>
  <p class="salutation">…</p>
  <section id="s0"><p id="s0p0" class="incipit">…</p>
    <figure id="s0f0"><img src="img/FR-2025-12-25.webp" alt="…"/></figure>
    <p id="s0p1">…</p></section>
  <section id="s1"><h2>…</h2><p id="s1p0">…<em>…</em> <span class="bibref" data-ref="Mt 5,48">(Mt 5, 48)</span></p></section>
  <aside epub:type="endnotes" role="doc-endnotes" hidden="hidden">…</aside>
  <footer class="provenance">Abbaye Saint-Joseph de Clairval · source · droits</footer>
</article>
```

### 2.6 Conception du lecteur dédié

**Écrans** (à détailler avec 08 UX) :
1. **Rayonnage « Lettres de Clairval »** : couverture, puis sommaire **par année**, de 1996 à 2025, en accordéon. Chaque entrée affiche la vignette (≥ 2016), le titre, le sous-titre ou les mots-clés, la date et une pastille de lecture (non lue, en cours, lue).
2. **Lettre** : en-tête avec date, titre et sous-titre ; lettrine sur l'incipit (le site fait déjà une lettrine en CSS, ce qui s'accorde avec le principe papier) ; **sommaire interne** construit à partir des 5 à 9 intertitres ; temps de lecture d'environ 15 min (3 400 mots à 230 mots/min) ; environ 12 à 15 pages en portrait sur un téléphone, environ 7 doubles pages en paysage [S] ; menu « Lire en : EN · DE · ES · IT · NL » si une traduction existe (V1) ; pied de page de provenance avec les liens « S'abonner » et « Soutenir » de l'abbaye.
3. **Notes en pop-up** : le HTML n'en contient pas, mais le mécanisme générique (`doc-noteref` vers une bulle) servira à trois choses : (a) les **références bibliques**, qui affichent le verset du module Bible hors ligne (04) ; (b) la **bibliographie** extraite des PDF (V1) ; (c) un éventuel glossaire.
4. **Index** :
   - **par saint ou personne**, de A à Z, trié sur le nom normalisé, avec un badge de statut **actuel** (saint, bienheureux, vénérable, serviteur de Dieu, laïc) tiré d'une table `person` tenue à jour et non du titre ;
   - **par thème**, à partir d'un thésaurus curé d'environ 60 à 80 entrées qui regroupe les 674 mots-clés bruts (par exemple : martyre, conversion, Eucharistie, souffrance, humilité, éducation, sacerdoce, famille et mariage, Exercices de saint Ignace…) ;
   - **par date liturgique** : vue calendrier mois par mois, où chaque jour porte les lettres dont le sujet est fêté ce jour-là ;
   - **par catégorie** : saints, bienheureux, vénérables, convertis et laïcs, apparitions et miracles ;
   - **par époque ou pays** (V1), à partir des dates de vie et de Wikidata.
5. **Lien avec le calendrier (05)** : chaque personne reçoit `feast_of` (forme ordinaire, Martyrologe romain), `feast_1962` et `dies_natalis`, ainsi qu'un identifiant Wikidata (propriétés P411 pour le statut et P841 pour la fête) récupéré **au moment du build** puis relu à la main : 313 lettres, soit environ une journée de revue. Le jour d'une fête, la carte « Lire la lettre sur … » s'affiche sur « Aujourd'hui » et dans le calendrier. Ordre de grandeur [S] : environ 215 à 240 lettres portent sur une personne qui a une date liturgique, ce qui couvrirait à peu près un jour sur deux si l'on s'appuie sur le Martyrologe, mais bien moins avec le seul calendrier général.
6. **Recherche** : SQLite FTS5, une table par langue, tokenizer `unicode61 remove_diacritics 2`, recherche par préfixe, extraits surlignés, filtres par année, thème et statut. Pas de racinisation du français en MVP [S] ; un index trigramme est envisageable en V1.
7. **Lettre du mois sur l'écran « Aujourd'hui »**. Comme il n'y a pas de lettre courante en ligne, je propose une **règle de priorité** :
   1. le sujet d'une lettre est fêté aujourd'hui (via 05) : « Lettre du jour » ;
   2. sinon, un nouveau lot est arrivé depuis moins de 30 jours : « Nouvelle lettre » ;
   3. sinon, « Il y a N ans ce mois-ci » : une lettre du même mois, tirée de façon déterministe par (année, mois). On compte 25 à 28 lettres par mois calendaire [V], ce qui suffit pour une rotation sur des années.
8. **Signets, annotations, synthèse vocale** (interfaces 06 et 09) : ancre = `(letter_id, block_id, offset)` complétée d'un sélecteur de citation (préfixe, extrait, suffixe, à la manière de W3C Web Annotation), pour survivre aux corrections que l'abbaye apporte à ses lettres. Les identifiants de blocs `s{n}p{m}` servent aussi à la synthèse vocale.

**Modèle de données côté application** (SQLite en lecture seule, livré en pack ; à valider avec 10) :
`letter(id, work_id, lang, seq, date, title, subtitle, subject_kind, xhtml_zstd, words, has_image, pdf_url, source_url, content_sha256, version)` · `letter_fts(title, subtitle, body, keywords)` · `person(id, name, sort_key, status, wikidata, birth, death, feast_om, feast_1962, dies_natalis)` · `letter_person(work_id, person_id)` · `theme(id, label_fr, …)` · `letter_theme(work_id, theme_id)` · `image(id, work_id, file, w, h, sha256)`. Les états utilisateur (lu, signets, notes) vivent dans une **autre** base, en lecture et écriture, indexée par `id` et `work_id`.

### 2.7 Volume estimé

Mesures sur l'échantillon de 14 lettres françaises de 1997 à 2024 [V] : texte brut médian de **21,7 Ko** et corps HTML médian de **22,5 Ko** (maximum 23,5 Ko) par lettre. Les tailles sont comparables en anglais, néerlandais et italien (20,5 à 22,9 k caractères).

| Élément | Français seul | 6 langues (≈ 1 840 lettres) | Commentaire |
|---|---|---|---|
| Texte brut | ≈ 6,8 Mo | ≈ 40 Mo | 313 × 21,7 Ko |
| XHTML normalisé | ≈ 7,5 Mo | ≈ 44 Mo | Avec balisage et identifiants |
| XHTML compressé (zstd ou deflate, ≈ 0,35) | **≈ 2,5 Mo** | **≈ 15 Mo** | [S] taux habituel sur du texte |
| Index FTS5 | ≈ 3 à 4 Mo | ≈ 18 à 22 Mo | [S] |
| Images (≥ 2016, fichiers français communs) | ≈ 105 fichiers, 8 à 15 Mo bruts, **4 à 6 Mo en WebP 800 px** | identique, car partagées | [S] taille moyenne extrapolée à partir d'un seul fichier de 74 Kio |
| PDF (≥ 2006) | 209 × 0,35 à 0,57 Mo ≈ **75 à 120 Mo** | ≈ 1 270 PDF après dédoublonnage ≈ **450 à 700 Mo** | **Ne pas embarquer** : simple lien, téléchargement à la demande éventuel |
| Cache brut côté pipeline | ≈ 10 Mo (JSON) | ≈ 60 Mo (≈ 15 Mo en gzip) | ≈ 31 Ko par post avec `_fields` |
| **Pack MVP (français, texte, index et vignettes)** | **≈ 10 à 12 Mo** | — | |
| Pack complet V1 (6 langues) | — | ≈ 40 Mo | Téléchargement par langue, au choix de l'utilisateur |

### 2.8 Points d'interface avec les autres sujets

| Sujet | Interface |
|---|---|
| **02 Ignace** | 5 lettres françaises portent le mot-clé « Exercices de saint Ignace » (Brochero, Lantéri…). Le site Clairval a aussi une page `/la-vie-de-saint-ignace-de-loyola/`. On peut proposer des liens croisés de lecture, notamment en mode retraite. |
| **04 Bible** | Les références bibliques en ligne, par exemple « (Mt 5, 48) », pourraient s'ouvrir en pop-up sur le verset. Il faut une API `resolveRef("Mt 5,48", lang)` et une table des abréviations françaises. |
| **05 Calendrier** | Il faut une table partagée `person` ou `saint` avec des identifiants stables (QID Wikidata) et les fêtes en forme ordinaire et en 1962. Il faut aussi une API « personnes fêtées le jour J », Martyrologe compris si possible : c'est elle qui détermine la couverture de la carte « Lettre du jour ». |
| **06 Moteur de lecture** | Le moteur consomme le XHTML défini en §2.5 : classes `incipit` (lettrine), `salutation`, `subtitle`, `figure` et `bibref`, identifiants de blocs, mécanisme de notes. Il gère la pagination et la synthèse vocale par bloc. |
| **07 Tourne-page** | Rien de spécifique. Une lettre fait environ 12 à 15 pages en portrait. |
| **08 UX** | Rayonnage, sommaire par année, index, carte « Aujourd'hui », sélecteur de langue, provenance et liens vers l'abbaye. |
| **09 Signets et images pieuses** | Modèle d'ancre robuste (§2.6). Les vignettes des lettres **ne doivent pas** servir d'images pieuses sans droits clarifiés. |
| **10 Architecture** | Pack de contenu SQLite en lecture seule et versionné, séparé des données utilisateur. Téléchargement de packs par langue, en option. Aucun appel réseau en MVP (hors ligne d'abord). |
| **11 Pipeline** | Héberge le module `sources/clairval` : récupération, cache, normalisation, tests de référence, construction des packs en CI hebdomadaire. Le format de manifeste est à partager avec les autres sources. |
| **12 Juridique** | Mention « autorisation nécessaire », vente des recueils, CPI L.122-4 et L.122-5, droit *sui generis* L.341-1, droits sur les photos. Rédaction de la demande d'autorisation et du contrat ou de l'accord écrit. |
| **13 Benchmark** | Voir comment d'autres applications catholiques intègrent des contenus d'abbayes sous licence (attribution, liens de don). |

---

## 3. Recommandations

### MVP
1. **Envoyer la demande d'autorisation à l'abbaye** (formulaire de contact ou courrier postal), avec les garanties listées en §2.4. **Rien n'est distribué avant une réponse écrite.** En attendant, le pipeline peut tourner en local pour le développement, sans publication.
2. Mettre en place le pipeline `sources/clairval` **en français seulement** : API REST, cache brut, reprise, normalisation XHTML, JSON canonique, tests de référence sur les 25 lettres, rapport de qualité.
3. Construire un **pack de contenu français** (SQLite, XHTML zstd et FTS5, environ 10 à 12 Mo avec les vignettes, ou environ 6 Mo sans) intégré à l'APK ou téléchargé une fois, selon la décision D2.
4. Livrer le **lecteur** : rayonnage, sommaire par année, sommaire interne, typographie (lettrine, intertitres), signets et position de lecture, recherche FTS, index A–Z par personne (statut tiré du titre, avertissement « statut à l'époque de la lettre »).
5. Ajouter la **carte « Aujourd'hui »** selon la règle de priorité de §2.6, en commençant par « Il y a N ans ce mois-ci » et « Nouvelle lettre » ; « Lettre du jour » arrive dès que la table `person` minimale est prête avec 05.
6. Afficher sur chaque lettre la **provenance et les liens** vers l'abbaye (s'abonner, soutenir), et ne faire **aucune correction textuelle** du contenu.

### V1
1. Ajouter les **6 langues** en packs optionnels, avec dédoublonnage WPML, graphe des traductions et sélecteur « Lire en… ».
2. Établir la **table `person` curée** (QID Wikidata, statut actuel, fêtes en forme ordinaire, en 1962 et *dies natalis*) et l'intégrer pleinement au calendrier 05, avec un index par date liturgique.
3. Bâtir le **thésaurus de thèmes** (674 mots-clés ramenés à 60 à 80 thèmes) et l'index par thème.
4. Ajouter les **images** (2016 et après) en WebP, si l'abbaye donne son accord.
5. Faire des **références bibliques** cliquables vers le module 04.
6. Extraire la **bibliographie** des PDF (`pdftotext`, à partir de 2006) et l'afficher en notes de fin.
7. Lancer la **mise à jour incrémentale** hebdomadaire en CI (`modified_after`, `ETag` du flux) et publier des packs différentiels, avec gestion des versions de lettres et migration des ancres.

### Plus tard
- Mode bilingue côte à côte (français et autre langue).
- Synthèse vocale de lecture continue, ou audio officiel si l'abbaye partage ses CD.
- Portugais et catalan, si l'abbaye fournit les fichiers.
- Parcours de lecture, par exemple « un saint par semaine » ou un parcours de retraite avec 02.
- Partage d'un court extrait avec attribution et lien, dans les limites de l'accord.
- Index par époque et pays, et carte du monde des saints.

---

## 4. Décisions à soumettre à l'utilisateur

1. **D1 — Autorisation.** Valides-tu une demande formelle à l'abbaye avant toute distribution ? Qui l'envoie, et en quel nom (personne physique ou projet) ? Je peux proposer un brouillon avec l'agent 12.
2. **D2 — Mode de distribution**, selon la réponse :
   - (a) corpus embarqué dans l'APK ou en pack téléchargeable depuis nos serveurs, ce qui suppose un accord ;
   - (b) « lecteur » qui récupère les lettres depuis clairval.com sur l'appareil, ce qui implique du réseau et une fragilité technique ;
   - (c) simples liens vers le site ;
   - (d) abandon du module.
3. **D3 — Langues.** Français seul en MVP, puis les 6 langues en V1 ? Ou d'emblée français et anglais ?
4. **D4 — Images et PDF.** Faut-il inclure les vignettes, sous réserve de leurs droits ? Les PDF restent-ils de simples liens externes ?
5. **D5 — Définition de la « lettre du mois »**, compte tenu du décalage d'environ un an : valides-tu la règle « fête du jour, sinon nouveauté, sinon il y a N ans ce mois-ci » ?
6. **D6 — Calendrier de référence pour les fêtes** : forme ordinaire avec Martyrologe, forme extraordinaire 1962, ou les deux selon le réglage du missel ? À trancher avec 05.
7. **D7 — Fidélité du texte.** On reste strictement verbatim, et les coquilles se signalent à l'abbaye au lieu d'être corrigées. Seule la typographie (espaces insécables) est normalisée. D'accord ?
8. **D8 — Confirmation de l'identification.** Les lettres que tu reçois portent-elles bien l'en-tête « Abbaye Saint-Joseph de Clairval » ? Ou penses-tu à une autre publication de Flavigny ?

---

## 5. Risques et inconnues

| Risque ou inconnue | Gravité | Probabilité | Parade |
|---|---|---|---|
| **Refus ou absence de réponse de l'abbaye** (mention d'autorisation explicite, exploitation commerciale des recueils) | Élevée | Moyenne [S] | Demande soignée, garanties, repli D2 (b) ou (c). Rien n'est distribué sans accord. |
| Fermeture ou limitation de l'API REST, par exemple par un plugin de sécurité | Moyenne | Faible à moyenne | Repli sur le HTML via les sitemaps (mêmes sélecteurs `lettre-*`) ; cache brut conservé. |
| Refonte du thème ou du constructeur de pages, avec perte des classes `lettre-*` | Moyenne | Faible à court terme | Tests de référence ; alerte quand la validation échoue ; pas de publication automatique en cas d'erreur. |
| Doublons et mauvais classement WPML (italien sous `/nl/` et `/de/`) | Moyenne | Certaine [V] | Clé (code de langue réel, date), détection par le PDF et la salutation, rapport d'anomalies. |
| Corrections a posteriori des lettres qui cassent les ancres de signets | Moyenne | Certaine [V] (2026-02, 2026-05) | Versionnage, sélecteur de citation, migration des ancres. |
| Absence de lettre courante en ligne (décalage d'environ un an) | Faible pour la technique, moyenne pour les attentes | Certaine [V] au 2026-10-11 | Règle « Aujourd'hui » de §2.6 ; demander à l'abbaye un accès anticipé (facultatif). |
| `modified_after` non supporté par la version de WordPress du site | Faible | Faible [S] | Repli : liste complète triée par `modified`, arrêt au premier post déjà vu. |
| Droits sur les photos des vignettes (souvent des tiers) | Moyenne | [?] | Les exclure du MVP et les inclure seulement avec l'accord de l'abbaye. |
| Typographie et coquilles hétérogènes | Faible | Certaine [V] | Normalisation typographique idempotente ; signalement plutôt que correction. |
| Statut canonique et fêtes faux si l'on se fie aux titres | Moyenne | Certaine [V] (Acutis, Newman, Frassati…) | Table `person` curée et datée. |
| Autre « lettre mensuelle de Flavigny » non identifiée | Faible | [?] (recherche impossible, budget épuisé) | Question D8 à l'utilisateur ; recherche complémentaire dans une session ultérieure. |
| Diffusion par courriel de la lettre (alternative au papier) | — | [?] | Le formulaire demande un courriel, mais l'envoi semble postal. À demander à l'abbaye. |
| Signature de l'abbé, numéro de parution, bibliographie avant 2025 | Faible | [?] | Absents du HTML ; à vérifier sur d'autres PDF en V1. |
| Charge sur le serveur de l'abbaye | Faible | Faible | Environ 40 requêtes pour un passage complet, 6 à 12 par semaine ensuite, avec un intervalle de 10 s ou plus. |

---

## 6. Sources

Toutes les pages ont été consultées le **2026-10-11** entre 13 h 25 et 13 h 39 UTC (29 requêtes HTTP, journal conservé dans le scratchpad de l'agent).

**Site clairval.com — vérifié**
- https://www.clairval.com/robots.txt : `Crawl-delay` et chemins interdits.
- https://www.clairval.com/ : menu, langues (`/en/ /es/ /it/ /de/ /nl/`), indices WPML.
- https://www.clairval.com/lettres-en-ligne-6/ : archive française 2021-2025, liens vers les archives `-1` à `-5` et vers les autres langues.
- https://www.clairval.com/sitemap.xml (redirige vers `/sitemap_index.xml`), https://www.clairval.com/post-sitemap.xml, https://www.clairval.com/post-sitemap2.xml et https://www.clairval.com/page-sitemap.xml : inventaire et comptages.
- https://www.clairval.com/wp-json/wp/v2/posts?per_page=3 : `X-WP-Total` = 313 pour le français, structure du contenu.
- https://www.clairval.com/wp-json/wp/v2/posts?lang=en&per_page=2 : `X-WP-Total` = 306 pour l'anglais.
- https://www.clairval.com/wp-json/wp/v2/posts?per_page=100&page=1…4&_fields=id,date,modified,slug,title,excerpt,featured_media : métadonnées des 313 lettres françaises (titres, dates, mots-clés).
- https://www.clairval.com/wp-json/wp/v2/posts?slug=… et `?include=…` : échantillon de 19 lettres françaises de 1996 à 2024.
- https://www.clairval.com/wp-json/wp/v2/posts?lang=en&slug=saint-jeanne-de-lestonnac,blessed-chiara-luce-badano : échantillon anglais.
- https://www.clairval.com/wp-json/wp/v2/posts?lang=nl&slug=santa-gertrude,heilige-gertrudis,gelukzalige-ladislas-batthyany-strattmann : échantillon néerlandais et doublon italien.
- https://www.clairval.com/wp-json/wp/v2/categories?per_page=100 : catégories `lettre-fr` et `lt-fr-AAAA`.
- https://www.clairval.com/bienheureuse-dina-belanger/ : page HTML rendue d'une lettre française de 2025 (gabarit, mention de droits, PDF).
- https://www.clairval.com/en/saint-jeanne-de-lestonnac/ : page HTML rendue d'une lettre anglaise de 2025 (mention de droits en anglais, liens de traduction).
- https://www.clairval.com/la-lettre-spirituelle/presentation-de-la-lettre/ : 8 langues, parution mensuelle, adresse de Flavigny, livres et CD.
- https://www.clairval.com/la-lettre-spirituelle/recevoir-la-lettre-gratuitement/ : expression « Lettres mensuelles », formulaire d'abonnement postal.
- https://www.clairval.com/feed/ : flux RSS en français, extraits seulement.
- https://www.clairval.com/mentions-legales/ : éditeur, siège à Flavigny-sur-Ozerain, absence de clause de licence.
- https://www.clairval.com/documents/FR-2025-12-25.pdf (téléchargé pour analyse des métadonnées et de la structure, non redistribué), https://www.clairval.com/documents/FR-2015-10-28.pdf (requête HEAD : 200, 345 Kio) et https://www.clairval.com/documents/FR-2005-08-31.pdf (requête HEAD : 404).
- https://www.clairval.com/wp-content/uploads/img-lettres/img_lettre-FR-2025-12-25.jpg (requête HEAD : 74 Kio).

**Liens relevés mais non consultés**
- https://www.traditions-monastiques.com/fr/ (boutique : livres et CD des vies de saints).
- https://www.clairval.com/abbaye/histoire-abbaye-saint-joseph-de-clairval/ (histoire de l'abbaye).
- https://www.clairval.com/la-vie-de-saint-ignace-de-loyola/ (intéresse l'agent 02).

**Non vérifié**
- La recherche web était indisponible (quota de session partagé épuisé). Restent donc sans preuve : l'existence d'une autre lettre mensuelle à Flavigny (séminaire de la Fraternité Saint-Pie-X, Anis de Flavigny), l'historique de l'abbaye et l'ancien schéma d'URL des lettres d'avant la refonte.
- Références juridiques (CPI L.111-1, L.122-4, L.122-5, L.341-1) : rappel général, à confirmer par l'agent 12.
