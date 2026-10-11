# Prières classiques : provenance

Publication `prieres.classiques`, paquet `prieres` (`domaine-public`, version 2026101101). Latin et français traditionnel (vouvoiement), tirés de paroissiens romains anonymes publiés entre 1860 et 1914.

| | |
|---|---|
| Prières | 34 (34 en français, 24 en latin et en français) |
| Unités | 8 : `usuelles`, `matin-soir`, `mariales`, `esprit-saint`, `louange`, `penitence`, `devotion`, `litanies` |
| Blocs | 624, dont 4 marqués `x-review` |
| Indulgences « en jours » retirées | 3 |
| Collation avec l'OCR | 10 832 mots, 95,7 % confirmés mot pour mot ; le reste est expliqué dans `collation.tsv` (voir « Relecture ») |

## Chaîne

```bash
python3 -I content/sources/prieres/fetch.py                       # OCR et index de pages des éditions (cache)
python3 -I content/sources/prieres/fetch.py page ID N… [--scale 2]  # fac-similés des pages, pour la relecture
python3 -I content/sources/prieres/ocr.py find ID 'regex'          # où une prière se trouve dans une édition
python3 -I content/sources/prieres/ocr.py page ID N                # texte OCR brut d'une page
python3 -I content/sources/prieres/normalize.py -v                 # texte/*.txt → content/data/prieres.classiques
python3 -I content/sources/prieres/check.py --tsv content/sources/prieres/collation.tsv
python3 -I content/pqt.py validate prieres.classiques
```

- Téléchargements dans `content/.cache/raw/prieres/<identifiant>/`, User-Agent `Paroissien-pipeline/0.1`, une pause d'une seconde entre deux requêtes, rien n'est retéléchargé. `fetch-log.json` y consigne l'URL, la date, la taille et le sha256 de chaque fichier. Les fichiers téléchargés (texte, JSON, JPEG) ne sont jamais exécutés.
- Le texte OCR utilisé est `<id>_hocr_searchtext.txt.gz`, découpé page par page par `<id>_hocr_pageindex.json.gz` (même contenu que le `_djvu.txt`, avec les limites de page). `ocr.py` donne l'index hOCR ; l'image correspondante est `https://archive.org/download/<id>/page/n<index − décalage>.jpg`, le décalage (`LEAF_OFFSET`) ayant été relevé sur le fac-similé : 1 pour Mame 1890, 3 pour les deux paroissiens de Québec, 0 pour les autres.
- **Les transcriptions sont dans `texte/<unité>.txt`**, une par unité de lecture, lisibles sans outil : c'est là qu'on relit et qu'on corrige. Format : `== clé` ouvre une prière, `title.fr:`, `src: identifiant | langues | pages imprimées | images`, `removed:` (mention retirée), puis des blocs (`para`, `rubric`, `versicle V|R`, `stanza hymn|psalm`, `heading 2`) suivis de leurs lignes `la:` et `fr:`, et éventuellement `review:`. Dans le texte, ` / ` est un saut de vers, ` * ` l'astérisque, `_…_` l'italique ; ℣ et ℟ deviennent les symboles du format.
- `normalize.py` applique seul la typographie (apostrophe ’, espace fine insécable avant ; : ! ? et dans « », NFC) et attribue les identifiants : `heading` `<clé>` puis `<clé>.1`, `<clé>.2`… dans l'ordre des blocs. Chaque `heading` porte `x-source` (identifiant, édition, langues, pages imprimées, images, lien vers la page).

## Éditions

| Identifiant archive.org | Édition | Rôle | sha256 de l'OCR (16 premiers) |
|---|---|---|---|
| [`paroissienromain00tour`](https://archive.org/details/paroissienromain00tour) | *Paroissien romain*, Tours, A. Mame, 1890, approuvé par Mgr Meignan | référence : prières du matin et du soir, prières usuelles, actes, Angélus, Te Deum, Magnificat, Nunc dimittis, De profundis, prière à saint Michel | `310fe3e4493189c0` |
| [`paroissienromai00churgoog`](https://archive.org/details/paroissienromai00churgoog) | *Paroissien romain… en latin et français, traduction nouvelle*, Tours, Alfred Mame et fils, 1869 (Google Books) | textes **en regard** absents de Mame 1890 sous cette forme : antiennes mariales, Sub tuum, Veni Creator, Veni Sancte Spiritus, Miserere, litanies du saint Nom, de la sainte Vierge et des Saints | `faf16f3c27944380` |
| [`bub_gb_PcH1G0A8jZUC`](https://archive.org/details/bub_gb_PcH1G0A8jZUC) | *Grand paroissien romain très-complet*, Lecoffre, 1860 (Google Books) | Souvenez-vous (latin et français), Benedictus, Anima Christi, acte d'offrande | `7273ab4fe915b6dc` |
| [`cihm_80314`](https://archive.org/details/cihm_80314) | *Paroissien de poche*, Québec, I.-P. Déry, 1914 (imprimatur Malines, 10 mai 1914 ; Canadiana) | litanies du Sacré-Cœur (1899) et de saint Joseph (1909), postérieures aux éditions Mame | `152d686702dbbc88` |
| [`cihm_990278`](https://archive.org/details/cihm_990278) | *Nouveau paroissien romain*, Saint-Hyacinthe, É. Solis, 1913 (imprimatur Malines, 31 décembre 1913 ; Canadiana) | acte de contrition | `4b4c7b916c6611d2` |

Consultées sans être retenues : Zech 1901 (`paroissienromain00cath` ; sa prière à saint Michel est une autre traduction que celle de Mame 1890), Mame 1872 (`paroissienromain00cath_0`), Barbou vers 1895 (`leparoissienroma00unse`, OCR trop dégradé), Québec 1893 (`cihm_95158`). Elles servent de témoins si un passage doit être départagé.

Mame 1890 imprime le Pater, l'Ave, le Credo et le Confiteor **en latin seulement** dans la prière du matin et **en français seulement** dans la prière du soir, et ses litanies en français seulement : d'où l'appel à Mame 1869 pour tout ce qui doit être en regard.

## Prières

Pages imprimées, puis images archive.org (`/page/nN`).

| Unité | Clé | Titre imprimé | Langues | Source | Pages | Images |
|---|---|---|---|---|---|---|
| usuelles | `pater` | L'Oraison dominicale | la, fr | Mame 1890 | la 9 ; fr 20 | 22 ; 33 |
| | `ave` | La Salutation angélique | la, fr | Mame 1890 | la 9 ; fr 20-21 | 22 ; 33-34 |
| | `gloria-patri` | (ouverture des Vêpres) | la, fr | Mame 1890 | 111 | 124 |
| | `credo-apostres` | Le Symbole des Apôtres | la, fr | Mame 1890 | la 9-10 ; fr 21 | 22-23 ; 34 |
| | `confiteor` | La Confession des péchés | la, fr | Mame 1890 | la 10 ; fr 21-22 | 23 ; 34-35 |
| | `acte-de-foi`, `acte-d-esperance`, `acte-de-charite` | Acte de foi, d'espérance, de charité | fr | Mame 1890 | 11-12 | 24-25 |
| | `acte-de-contrition` | Acte de contrition | fr | Solis 1913 | 25 | 28 |
| matin-soir | `priere-du-matin` | Prière du matin | la, fr | Mame 1890 | 7-11 | 20-24 |
| | `priere-du-soir` | Prière du soir | fr | Mame 1890 | 18-23 | 31-36 |
| mariales | `alma-redemptoris` | (antienne) | la, fr | Mame 1869 | 158-159 | 168-169 |
| | `ave-regina-caelorum` | (antienne) | la, fr | Mame 1869 | 159-160 | 169-170 |
| | `regina-caeli` | (antienne) | la, fr | Mame 1869 | 160 | 170 |
| | `salve-regina` | (antienne) | la, fr | Mame 1869 | 160-161 | 170-171 |
| | `angelus` | Angélus (table du livre) | la, fr | Mame 1890 | 16-17 | 29-30 |
| | `souvenez-vous` | Prière de saint Bernard | la, fr | Lecoffre 1860 | 15 | 34 |
| | `sub-tuum` | Ant. Sub tuum præsidium | la, fr | Mame 1869 | 773 | 783 |
| esprit-saint | `veni-creator` | Hymne au Saint-Esprit | la, fr | Mame 1869 | 75-77 | 85-87 |
| | `veni-sancte-spiritus` | Prose (Pentecôte) | la, fr | Mame 1869 | 394-395 | 404-405 |
| louange | `te-deum` | Cantique d'action de grâces | la, fr | Mame 1890 | 106-110 | 119-123 |
| | `magnificat` | Cantique de la sainte Vierge | la, fr | Mame 1890 | 124-126 | 137-139 |
| | `benedictus` | Cantique de Zacharie | la, fr | Lecoffre 1860 | 89-90 | 108-109 |
| | `nunc-dimittis` | Cantique de Siméon | la, fr | Mame 1890 | 135 | 148 |
| penitence | `miserere` | Psaume 50 | la, fr | Mame 1869 | 51-54 | 61-64 |
| | `de-profundis` | Prière pour les défunts, Psaume 129 | la, fr | Mame 1890 | 26-27 | 39-40 |
| devotion | `anima-christi` | Prière à Jésus-Christ | fr | Lecoffre 1860 | 45 | 64 |
| | `acte-d-offrande` | Acte d'Offrande | fr | Lecoffre 1860 | 45 | 64 |
| | `saint-michel` | Prières après la sainte Messe | fr | Mame 1890 | 83 | 96 |
| litanies | `litanies-saint-nom` | Litanies du saint Nom de Jésus | la, fr | Mame 1869 | 8-13 | 18-23 |
| | `litanies-lorette` | Litanies de la sainte Vierge | la, fr | Mame 1869 | 19-22 | 29-32 |
| | `litanies-saints` | Litanies des Saints | la, fr | Mame 1869 | 61-69 | 71-79 |
| | `litanies-sacre-coeur` | Litanies du Sacré Cœur de Jésus | fr | Déry 1914 | 224-228 | 231-235 |
| | `litanies-saint-joseph` | Litanies de saint Joseph | fr | Déry 1914 | 231-233 | 238-240 |

Les titres latins (`title.la`) des antiennes, hymnes et cantiques sont leur incipit imprimé ; aucun autre titre n'a été composé.

### Absentes ou incomplètes (la source ne permet pas de les établir)

- **Anima Christi en latin, Suscipe de saint Ignace, prière à saint Michel en latin, Angele Dei** : introuvables dans les neuf éditions consultées (recherche `ocr.py find` sur les incipits latins et français). On donne à la place, en français seulement, la « Prière à Jésus-Christ » de Lecoffre (traduction de l'Anima Christi) et son « Acte d'Offrande », dont la seconde moitié reprend l'offrande du Suscipe sans en être le texte propre. La prière du matin contient une invocation à l'ange gardien (« Ange du ciel, mon fidèle et charitable guide… »), qui n'est pas l'Angele Dei.
- **Saint Michel** : la prière de Léon XIII n'existe ici qu'au sein des « Prières après la sainte Messe » (Mame 1890, p. 83), reprises en entier : rubrique (Ave ×3, Salve Regina), oraison « O Dieu, notre refuge et notre force », puis la prière à saint Michel (`saint-michel.4`).
- **Actes de foi, d'espérance, de charité, de contrition** : en français seulement ; ce sont des formules françaises, sans latin dans les paroissiens. L'acte de contrition vient de Solis 1913, car Mame 1890 n'a que des actes longs (préparation à la communion) ; Déry 1914 (p. 25-26) a le même texte, mais la fin y est couverte d'une tache.
- **Litanies du Sacré-Cœur et de saint Joseph** : en français seulement (Déry 1914 ne donne pas le latin).
- **Litanies des Saints** : arrêtées au ℣/℟ « Domine, exaudi orationem meam » ; les oraisons des p. 69-71 renvoient à d'autres pages du livre et ne sont pas reprises.

## Litanies : état ancien

Les litanies sont celles du paroissien, **sans aucun ajout récent** :

- **Lorette** (Mame 1869) : 1869, donc sans *Regina sacratissimi Rosarii* ni les invocations plus tardives, et sans les trois ajouts du 20 juin 2020 (*Mater misericordiæ*, *Mater spei*, *Solacium migrantium*) ni *Mater Ecclesiæ*. ≈ Les dates des ajouts intermédiaires (Rosaire 1883, *Mater boni Consilii* 1903, *Regina pacis* 1917, *Regina in cælum assumpta* 1950, *Regina familiæ* 1995) sont données de mémoire, à vérifier.
- **Saint Joseph** (Déry 1914) : forme de 1909, 25 invocations de « Saint Joseph » à « Protecteur de la Sainte Église », sans les sept ajouts du 1ᵉʳ mai 2021 (*Custos Redemptoris*, *Serve Christi*, *Minister salutis*, *Fulcimen in difficultatibus*, *Patrone exsulum*, *Patrone afflictorum*, *Patrone pauperum*).
- **Sacré-Cœur** (Déry 1914) : forme de 1899, 33 invocations « Cœur de Jésus… ».
- **Saint Nom** et **Saints** (Mame 1869) : forme du Rituel d'alors. La litanie des Saints garde l'invocation pour l'Empereur (`litanies-saints.103`, « Ut Imperatorem nostrum custodire digneris » / « Daignez conserver notre Empereur »), propre au Second Empire : **décision éditoriale à prendre** (garder comme témoin, ou prendre la litanie de Mame 1890, p. 61-67, qui est en français seulement).

Les réponses abrégées sont celles du livre (« ayez. », « mis. », « ora. », « priez. », « dél. ») ; les litanies de 1914 utilisent un astérisque qui renvoie à la réponse imprimée en bas de page, reprise en rubrique au début (« * ayez pitié de nous. », « * priez pour nous. »).

## Mentions retirées

Indulgences « en jours » (système aboli en 1967, rapport 14 §2.3.7), retirées et consignées dans la ligne `removed:` de la transcription :

| Prière | Mention retirée |
|---|---|
| `saint-michel` | « (Décret du 6 janvier 1884, les enrichissant de 300 jours d'indulgence.) » (Mame 1890, p. 83) |
| `litanies-sacre-coeur` | « (300 jours d'indulgences.) » (Déry 1914, p. 224) |
| `litanies-saint-joseph` | « (300 jours d'indulg., une fois le jour. » (Déry 1914, p. 231 ; parenthèse non fermée dans le livre) |

Autres retraits, qui ne valent que pour le livre imprimé :

- **Renvois de page** : « p. 18 » (oraison de l'Angélus, `alma-redemptoris.5`), « Alma Redemptoris Mater, p. 14[?] » et « p. 14[?] » (`alma-redemptoris.6` et `.9`), « Salve, Regina, p. 139 » (`saint-michel.1`, réduit à « Salve, Regina. »).
- **Note (1) du Veni Creator** (Mame 1869, p. 75) : variantes de l'hymne « dans les diocèses qui ne suivent pas l'Office réformé ».
- **Hors du texte de la prière** : l'« Alleluia » qui suit le Gloria Patri aux Vêpres ; l'antienne *Salva nos* après le Nunc dimittis ; le titre courant des pages.

## Corrections du texte imprimé

Les fautes d'OCR ont été corrigées sur le fac-similé ; elles ne sont pas listées une à une (voir `collation.tsv`). Les seules **coquilles du livre** corrigées sont :

| Bloc | Livre | Corrigé | Page |
|---|---|---|---|
| `de-profundis.9` (la) | « ea omnibus iniquitatibus » | « ex omnibus » | Mame 1890, p. 27 |
| `benedictus.2` (la) | « Et errexit cornu » | « erexit » | Lecoffre, p. 89 |
| `benedictus.2` (fr) | « un puissant Sanveur » | « Sauveur » | Lecoffre, p. 89 |
| `benedictus.8` (fr) | « en sa présnce » | « présence » | Lecoffre, p. 90 |
| `benedictus.10` (la) | « remissionem peccatorem eorum » | « peccatorum » | Lecoffre, p. 90 |
| `benedictus.11` (fr) | « les entrailles da la miséricorde » | « de la » | Lecoffre, p. 90 |
| `veni-sancte-spiritus.5` (fr) | « notre sonlagement » (lettre retournée) | « soulagement » | Mame 1869, p. 395 |
| `miserere.9` (fr) | « consoıation » (lettre cassée) | « consolation » | Mame 1869, p. 52 |
| `miserere.15` (fr) | « O Dieu, Diêu sauveur… i'ai versé » (lettres cassées) | « Dieu », « j'ai » | Mame 1869, p. 53 |
| `litanies-saints.78` (la) | « libera nos, Domine » (point final effacé) | point restitué | Mame 1869, p. 64 |
| `litanies-saints.113` (fr) | « ils de Dieu » (initiale tombée) | « Fils de Dieu » | Mame 1869, p. 68 |
| `litanies-sacre-coeur.12` (fr) | « dans le sein de / de la Vierge Marie » | « de » non redoublé | Déry 1914, p. 224 |
| `litanies-sacre-coeur.15` (fr) | « temple saint de D'eu » (lettre cassée) | « Dieu » | Déry 1914, p. 225 |
| `litanies-saint-joseph.10` (fr) | « qui êtez un seul Dieu » | « êtes » | Déry 1914, p. 231 |
| `litanies-saint-joseph.4` (fr) | « Seigneur,ayez pitié denous. » | espaces rétablies | Déry 1914, p. 231 |
| `te-deum.23`, `.29` (la) | « usque .in », « spe-.ravi » (points parasites) | retirés | Mame 1890, p. 109 |

**Normalisations** (sans changement de texte) : ℣ et ℟ pour « ỳ. », « f. », « V). », « R). » ; « * » de psalmodie en symbole ; lettrines et petites capitales en casse normale ; « ¶ » du Veni Creator conservé ; « œ » et « æ » tels qu'imprimés ; accents latins de la source conservés (« à sæculo », « sinè », « justitiâ » chez Lecoffre) ; « Eglise », « Epoux », « très-pure » laissés tels quels.

## Points à vérifier (`x-review`)

| Bloc | Raison |
|---|---|
| `alma-redemptoris.6` | Rubrique de saison peu lisible (Mame 1869, p. 159) : on lit « De l'Avent à la Purification inclusivement », alors que le verset qui suit est celui du temps de Noël (Mame 1890, p. 137 : « Depuis la Nativité de N.-S. jusqu'à la Purification inclusivement ») ; le renvoi qui suit est retiré. |
| `salve-regina.2` | Ponctuation après « entrailles » (tache, Mame 1869, p. 161). |
| `miserere.17` | Tache d'encre sur « , je » (« des sacrifices, je vous en aurais offert », Mame 1869, p. 53). |
| `litanies-saint-joseph.43` | Dernier mot de l'oraison abrégé et cassé (« Qui… » probable, Déry 1914, p. 233). |

Décisions éditoriales à prendre (non marquées dans les données) : l'invocation pour l'Empereur (`litanies-saints.103`) ; la reprise intégrale des usuelles dans la prière du matin (latin seul) et du soir (français seul), comme dans le livre, plutôt que des renvois vers `usuelles` ; le titre de l'Angélus, pris dans la table du livre (la page n'a que la rubrique « Prière qu'on récite trois fois par jour… »).

## Relecture

1. Ouvrir `texte/<unité>.txt` et, pour chaque prière, la page indiquée par `src` (`https://archive.org/details/<id>/page/n<image>`), ou en local : `fetch.py page <id> <image> --scale 2`.
2. `collation.tsv` liste, bloc par bloc, les mots de la transcription que l'OCR ne confirme pas (forme sans accents ni ponctuation). Presque tous sont des fautes d'OCR (lettrines, ligatures æ/œ, mots coupés en fin de ligne ou de page, « Yierge » pour « Vierge ») ; les autres sont les corrections ci-dessus. Un mot de cette liste qui n'entre dans aucune de ces catégories est une erreur de transcription à signaler. Les taux les plus bas (Alma, Souvenez-vous, Benedictus, Veni Creator, Miserere : 87 à 90 %) viennent d'OCR médiocres (Google Books), pas d'un texte moins sûr : chaque page a été relue sur l'image.
3. Après correction : `normalize.py`, `check.py --tsv …`, `pqt.py validate`, puis `pqt.py registry update prieres.classiques` si des blocs ont été ajoutés. Un identifiant publié ne disparaît jamais : un bloc retiré passe par `content/data/prieres.classiques/aliases.json`.
