# Propre de France du calendrier romain de 1962 : recherche

> Recherche du 2026-10-11 pour la phase 4. Elle aboutit à `content/data/calendar/1962/fr.json`, saisi à la main, et aux règles des solennités extérieures que le code devra appliquer (dernière section).
>
> **Légende.** **[V]** : vérifié dans la source citée pendant cette recherche. **[S]** : supposé, ou tiré de mémoire, à confirmer. **[?]** : inconnu.

## 1. Cadre

- **Décision de l'utilisateur.** L'Ordo de référence est celui d'un monastère, Le Barroux ou Fontgombault, **ramené au calendrier romain de 1962** : on reprend les usages de France que le monastère applique, et non son calendrier monastique. Version de 1962 pure, sans les ajouts de 2020. Solennités extérieures activées par défaut, que l'utilisateur pourra couper.
- **Ce que publient les monastères.**
  - **Le Barroux** met en ligne chaque année son *Ordo divini officii sacrique peragendi iuxta ritum romano-monasticum*, en latin. Cet Ordo sert à ses trois maisons : Sainte-Madeleine (SM), Notre-Dame de l'Annonciation, les moniales (AN), et Sainte-Marie de la Garde (MC). Le calendrier est **monastique**. Ce qui relève de la France y est marqué de trois façons :
    - « (in Prop.) » : *Proprium provinciæ Gallicæ*, le propre de la province de France de la congrégation ;
    - « Patronæ … Galliæ » : les patronnes de la France ;
    - « Ms. lectæ pro fidelibus » : les messes basses dites pour les fidèles, c'est-à-dire les solennités extérieures. **[V]**
  - **Fontgombault** ne publie pas d'Ordo sur son site (`abbaye-fontgombault.fr`) : on n'y trouve que les horaires et quelques documents. **[V]**
  - Aucun des deux monastères ne publie d'Ordo romain de 1962 pour la France.
- **Méthode suivie pour « ramener au romain ».** On part du calendrier romain général de 1962 (`1962/general.json`, tiré de Divinum Officium). On y applique :
  - les fêtes que l'Ordo du Barroux rattache à la France ;
  - les classes des patrons nationaux, fixées par les rubriques de 1960 (*Rubricae generales*, n. 42 : patron principal d'une nation en Iʳᵉ classe, patron secondaire en IIᵉ classe) ;
  - les messes pour les fidèles que l'Ordo du Barroux signale.

  Quand le Barroux se tait parce que son calendrier est monastique, on suit les Ordos romains de 1962 publiés en France, s'ils concordent. Quand les sources se contredisent, la source monastique l'emporte.

## 2. Sources

| Source | Nature | Accès | Ce qu'on en tire |
|---|---|---|---|
| **Abbaye Sainte-Madeleine du Barroux, Ordo 2026** | Monastique, latin | https://www.barroux.org/wp-content/uploads/2025/11/ordo-2026.pdf, lien depuis https://www.barroux.org/ordo-liturgique/ (sha256 `a04e7515b4209efe5964928514d84d3bbe379ae215d51ca3e2328cc6e0313a40`) | Source de référence |
| **Le Barroux, Ordo 2025** | Idem | https://www.barroux.org/wp-content/uploads/2024/11/ordo-2025.pdf (sha256 `06d65998f02dd2edcaf819843ca9f7afae6984794229a38d6ca06f9bf4408080`) | Recoupement d'une année sur l'autre |
| **Le Barroux, Ordo 2023** | Idem | https://www.barroux.org/wp-content/uploads/2023/02/ordo-2023.pdf (sha256 `3025c25c…1cc5ce`) | L'extraction du texte ne va que jusqu'à février : seule l'Épiphanie a servi |
| **Ordo de la FSSPX**, source `gallia` | Romain de 1962, « avec le propre de France » | API publique `https://ordo.fsspx.org/api/v1/calendar/{année}/{mois}?congregation=fsspx&source=gallia&mode=civil&lang=fr&thirdClass=1`, embarquée par https://laportelatine.org/ordo ; interrogée le 2026-10-11 pour 2025 à 2027 en entier, plus quelques mois de 2028, 2029, 2030, 2035 et 2057 | Classes et couleurs, champ `source: "gallia"` qui marque le propre de France, champ `solemnity` qui donne les solennités extérieures, champ `ofPrecept` qui donne le précepte. Recoupement des cas limites |
| **Francefidele/ordo-liturgique** | Ordo traditionnel 2026 | GitHub, commit `b44a05cf5ac0788cbe5e5f608d49d5d08fae0595`. **Sans licence : consulté pour recoupement seulement, rien n'est copié** | Classes, solennités et notes (précepte) de l'année 2026 |
| **introibo.fr** | Missel de 1962, propres diocésains et propre de France | Site hors service ; lu dans les captures de 2021 de la Wayback Machine : `Sainte-Genevieve-vierge-propre-de`, `Saint-Remi-eveque-et-confesseur`, `Sainte-Jeanne-d-Arc-vierge`, `Saint-Louis-roi`, `St-Jean-Marie-Vianney-confesseur`, `19-07-St-Vincent-de-Paul,369`, `Sainte-Marie-Bernard-Soubirous`, `Sainte-Clotide-Reine-et-Veuve`, `09-10-Sts-Denis-eveque-Rustique-et`, `-Propres-diocesains-` | Les pages qui portent la mention « propre de France » et la classe de chaque messe |
| **Divinum Officium** | MIT | commit `b9c100d79ae481c67b2801fdd3622b00dd86bd73` | Le calendrier général de 1960. **Il n'y a aucun calendrier gallican** (`web/www/Tabulae/Kalendaria/`). `M1963B.txt` est le calendrier monastique du Barroux. On a aussi lu les rubriques générales de 1960 (n. 42) et les rubriques du Missel (n. 341-343, 356-361) dans leur traduction anglaise (`horas/Help/Rubrics/`) |
| **missel.free.fr** | Vies de saints | — | Calendrier de 1969 seulement : inutile ici |
| **Réponse ministérielle**, question écrite n° 7919 (16ᵉ législature) | Droit civil | https://questions.assemblee-nationale.fr/dyn/16/questions/QANR5L16QE7919.pdf | La loi du 10 juillet 1920 institue la fête nationale de Jeanne d'Arc, célébrée le 2ᵉ dimanche de mai |

On n'a recopié aucun texte de messe ni aucune page : seulement des dates, des classes, des couleurs et des titres courts.

## 3. Entrées retenues (`1962/fr.json`)

| Date | Entrée | Classe | Couleur | Effet sur le calendrier général | Sources |
|---|---|---|---|---|---|
| 01-03 | **Sainte Geneviève, vierge** (`genevieve_of_paris_virgin`) | III | blanc | Nouvelle fête. Ce jour-là, le calendrier général n'a qu'une férie du temps de Noël (IVᵉ classe) | FSSPX : 2025-01-03, 2026-01-03, 2028-01-03 et 2030-01-03, `gallia`, classe 3 ; Francefidele 2026, « 3e cl. » ; introibo, « Sainte Geneviève, vierge (propre de France) », *III classis* (la messe du diocèse de Paris est différente). **Absente du Barroux** (voir § 5) **[V]** |
| 05-30 | **Sainte Jeanne d'Arc, vierge, patronne secondaire de la France** (`joan_of_arc_virgin`, `patron: SECONDARY`) | II | blanc | Nouvelle fête. Saint Félix Iᵉʳ, qui n'est que commémoré dans le calendrier général, devient sa commémoraison : on retire l'entrée générale `felix_i_pope` et on la rattache à l'entrée de Jeanne d'Arc | Barroux 2025 : « S. IOANNÆ ARCENSIS V. Patronæ secundæ Galliæ 2cl. » ; Barroux 2026 : « De S. Ioanna Arcensi V. hoc anno nihil fit » (le 30 mai tombe un samedi des Quatre-Temps de Pentecôte) ; FSSPX 2025 et 2029, `gallia`, classe 2 ; introibo, *II classis*, « patronne secondaire de la France » ; *Rubricae generales*, n. 42 b **[V]** |
| 08-15 | **Assomption de la bienheureuse Vierge Marie, patronne principale de la France** (`assumption_of_the_blessed_virgin_mary`, `patron: PRINCIPAL`, `obligation`) | I | blanc | Même fête. On ajoute le patronage, le titre et le précepte | Barroux 2025 et 2026 : « ASSUMPTIO B.M.V. PATRONÆ PRINCIPALIS GALLIÆ 1cl. », puis après les vêpres « processio iuxta votum Ludovici XIII Regis » ; FSSPX, `gallia`, `ofPrecept` ; Francefidele, « Fête d'obligation » **[V]** |
| 10-01 | **Saint Remi, évêque et confesseur** (`remigius_of_reims_bishop`) | III | blanc | La commémoraison du calendrier général devient une fête de IIIᵉ classe | Barroux 2025 et 2026 : « S. Remigii Ep. Rhemensis C. ③ cl. », « Off. in Prop., Ms. prop. » ; FSSPX 2025 à 2027, `gallia`, classe 3 ; Francefidele, « 3e cl. » ; introibo, « Proprium pro Gallia », *III classis* (fête double majeure avant 1960) **[V]** |
| 10-03 | **Sainte Thérèse de l'Enfant-Jésus, vierge, patronne secondaire de la France** (`therese_of_the_child_jesus_and_the_holy_face_of_lisieux_virgin`, `patron: SECONDARY`) | II | blanc | La fête passe de la IIIᵉ à la IIᵉ classe, avec un nouveau titre | Barroux 2025 et 2026 : « S. TERESIÆ A IESU INFANTE V. D. Patronæ secundæ Galliæ 2cl. », « Off. in Prop. » ; FSSPX, `gallia`, classe 2 ; Francefidele, « 2e cl. » ; *Rubricae generales*, n. 42 b **[V]**. introibo donne la page du Missel romain (*III classis*) avec un « Office propre à la France » |
| 11-01 | **Toussaint** (`all_saints`, `obligation`) | I | blanc | On ajoute seulement le précepte | FSSPX, `ofPrecept` ; Francefidele, « Fête d'obligation » ; droit en vigueur **[V]** |
| 12-25 | **Nativité de Notre-Seigneur Jésus-Christ** (`nativity_of_the_lord`, `obligation`) | I | blanc | On ajoute seulement le précepte. Noël appartient au temporal de 1962 (`general.py`, `TEMPORAL_KEYS`) : comme en 1969, cette entrée ne fait que porter le précepte, que le moteur applique à la célébration du temporal de même identifiant | Idem **[V]** |

L'**Ascension**, mobile, est aussi d'obligation : le code s'en charge, comme en 1969.

## 4. Fêtes françaises qui ne changent pas le calendrier général

Ces fêtes sont déjà au calendrier général de 1962 avec la même date et la même classe. Elles ne figurent donc pas dans `fr.json` :
- **saint Hilaire** (01-14, III) ;
- **Notre-Dame de Lourdes** (02-11, III ; « in Prop. » au Barroux, parce que le calendrier monastique général ne l'a pas) ;
- **saint Louis** (08-25, III ; « in Prop. » au Barroux) ;
- **saint Martin** (11-11, III ; IIᵉ classe dans le calendrier monastique).

**Formulaires de messe propres à la France, sans effet sur le calendrier** : ils comptent pour le missel de la phase 5, pas pour le calendrier.
- **Saint Vincent de Paul**, 19 juillet. introibo : messe *Pauperes Sion*, « propre de France », ajoutée en 1962 au supplément *Pro aliquibus locis*.
- **Saint Jean-Marie Vianney**, 8 août. introibo : messe propre « à beaucoup de diocèses de France », ajoutée elle aussi en 1962 au supplément *Pro aliquibus locis*.
- **Saint Louis**, 25 août. introibo la classe au « propre de France », mais un lecteur y objecte, missels Feder de 1958 et Brepols de 1961 à l'appui, qu'elle n'est propre qu'« en certains diocèses ».
- **Saint Remi**, 1ᵉʳ octobre. Messe du propre de France. Divinum Officium n'a que le texte de la commémoraison du calendrier général (`Sancti/10-01`).
- **Sainte Jeanne d'Arc** et **sainte Geneviève** n'ont **aucun propre de messe romain dans Divinum Officium**. Seul existe un fichier d'office monastique, `horas/Latin/SanctiM/05-30b.txt`. La phase 5 devra trouver ces textes ailleurs.

## 5. Désaccords entre sources, et choix retenu

| Point | Barroux (monastique) | FSSPX (`gallia`) | Francefidele 2026 | introibo | Retenu |
|---|---|---|---|---|---|
| Sainte Geneviève, 01-03 | Absente : férie le 3 janvier 2025 et 2026 | III | III | III, « propre de France » | **III, retenue**. Le Barroux se tait parce que son calendrier est monastique ; il ne contredit pas les sources romaines, qui concordent toutes |
| Saint Remi, 10-01 | III | III | III | III | III |
| Sainte Jeanne d'Arc, 05-30 | II | II | Empêchée en 2026 | II | II |
| Sainte Thérèse, 10-03 | II | II | II | III (page du Missel romain) | II, conforme au n. 42 b des rubriques |
| Sainte Clotilde, 06-03 | Absente | III | Absente (férie) | III, rangée dans les « propres diocésains » sans mention « propre de France » | **Écartée** |
| Sainte Jeanne de Valois, 02-04 | Commémoraison chez les moniales (AN) seulement | III | Absente | — | **Écartée** |
| Sainte Radegonde, 08-13 | Commémoraison chez les moniales (AN) seulement | III | Absente | Propre diocésain | **Écartée** |
| Présentation de la Vierge, 11-21 | III | **II** | III | — | **III** : le calendrier général ne change pas |
| Sainte Bernadette, 02-18 | Commémoraison en 2025, avec antienne et messe propres | Absente | Le 18 février 2026 tombe le mercredi des Cendres | III, propre diocésain (le 16 avril dans certains diocèses) | **Écartée** (§ 7) |
| Saint Louis-Marie Grignion de Montfort, 04-28 | Commémoraison « in PAL » | Absent | Mentionné en note | — | Écarté (*Pro aliquibus locis*) |
| Saint Denis, 10-09 | Rien | Commémoraison, comme au calendrier général | Commémoraison | Fête « dans beaucoup de diocèses » | Calendrier général inchangé |

## 6. Solennités extérieures, à coder

Ces solennités sont mobiles : elles ne figurent pas dans `fr.json`. Règles des rubriques de 1960 (*Rubricae generales Missalis*) **[V]** :
- **Définition (n. 356).** On célèbre la fête « sans office », pour le bien des fidèles, un autre jour que le sien, en général un dimanche.
- **Messes permises (n. 360).** Une messe chantée et une messe basse, ou deux messes basses, toutes **votives de IIᵉ classe**. L'office reste celui du jour.
- **Jours permis (n. 341).** Une messe votive de IIᵉ classe est permise les jours de IIᵉ, IIIᵉ et IVᵉ classe. Elle est donc **interdite un jour de Iʳᵉ classe**.
- **Solennités tenues d'un indult (n. 361).** Celles qui avaient été accordées par indult avant 1960 restent en vigueur, interdites les jours de Iʳᵉ classe et jamais avec plus de deux messes.
- **Solennités de droit (n. 358).** Le Sacré-Cœur (3ᵉ dimanche après la Pentecôte), le Rosaire (1ᵉʳ dimanche d'octobre) et la fête du patron principal.
- **Date (n. 359).** Une solennité de droit sans jour fixé se tient le jour empêché, ou le dimanche qui précède ou qui suit. Une solennité accordée par indult a un jour fixé.

**Constat commun aux sources** : une solennité empêchée par un jour de Iʳᵉ classe est **omise** cette année-là, **pas déplacée**. La FSSPX le montre dans tous les cas limites interrogés (voir le tableau). Le Barroux n'a pas d'exemple contraire.

**Forme du résultat** : rien ne change à la célébration du jour, ni à sa classe ni à sa couleur liturgique. La journée porte en plus une « solennité » : identifiant de la fête, messe votive de IIᵉ classe, couleur de la fête (blanc, ou rouge pour saints Pierre et Paul). L'utilisateur peut les couper, toutes ensemble ou une à une.

| Solennité | Règle de date | Cas limites | Sources | Statut |
|---|---|---|---|---|
| **Épiphanie** | **Le dimanche du 2 au 8 janvier**, sauf si le 6 janvier est un dimanche : l'Épiphanie (Iʳᵉ classe) occupe alors le dimanche et il n'y a pas de solennité à part. Ce dimanche est celui du Saint Nom de Jésus (2 au 5 janvier) ou de la Sainte Famille (7 ou 8 janvier), tous deux de IIᵉ classe : la messe votive y est permise. Il ne tombe jamais un jour de Iʳᵉ classe | — | **Barroux** : « Ms. lectæ pro fidelibus de Epiphania Domini » le dimanche 8 janvier 2023, le dimanche 5 janvier 2025 (Saint Nom de Jésus) et le dimanche 4 janvier 2026 (idem). **Désaccord** : la FSSPX (12 janvier 2025, 11 janvier 2026, 10 janvier 2027, 9 janvier 2028) et Francefidele (11 janvier 2026) la mettent le dimanche *qui suit* le 6 janvier, c'est-à-dire le dimanche de la Sainte Famille, du 7 au 13 janvier | **Règle du Barroux retenue.** C'est aussi la date de l'Épiphanie en France dans le calendrier de 1969 (`1969/TEMPORAL.md`) |
| **Saint-Sacrement** (Fête-Dieu) | Le dimanche qui suit la Fête-Dieu, soit Pâques + 63 jours (2ᵉ dimanche après la Pentecôte), avec la procession | Ce dimanche tombe le 24 juin, Nativité de saint Jean-Baptiste (Iʳᵉ classe), quand Pâques est le 22 avril (2057) : la solennité est omise (FSSPX 2057) | **Barroux** 2025-06-22 et 2026-06-07 : « Ms. cv. (necnon Ms. lectæ pro fidelibus) de Sol. Ss. Corporis Xti. Dein fit processio » ; FSSPX et Francefidele concordent | **Attestée par le monastère** |
| **Saints Pierre et Paul** | Le dimanche qui suit le 29 juin (30 juin au 5 juillet). Pas de solennité si le 29 juin est un dimanche | Le 1ᵉʳ juillet est un dimanche quand le 29 juin tombe un vendredi (2029) : la fête du Précieux Sang (Iʳᵉ classe) l'empêche et la solennité est omise (FSSPX 2029). Si ce dimanche est aussi le 3ᵉ après la Pentecôte (2030, 2038, 2041, 2052), la FSSPX garde le Sacré-Cœur et **omet** saints Pierre et Paul | FSSPX 2026-07-05 et 2027-07-04 ; Francefidele 2026-07-05 (« obligatoire en France »). Le Barroux ne prévoit **pas** de messe pour les fidèles (2026) | Ordos séculiers seulement |
| **Sainte Jeanne d'Arc** | Le **2ᵉ dimanche de mai** (8 au 14 mai) | Ce dimanche est la Pentecôte (Iʳᵉ classe) quand Pâques tombe du 22 au 26 mars : 2035 et 2046 (la FSSPX l'omet en 2035). Les autres dimanches possibles (après Pâques, après l'Ascension) sont de IIᵉ classe | FSSPX 2025-05-11, 2026-05-10, 2027-05-09, 2029-05-13 ; Francefidele 2026-05-10 ; Sacra Musica (musique-liturgique.com) : 9 mai 2027, 14 mai 2028, 13 mai 2029, 12 mai 2030 ; fête nationale civile du même jour (loi du 10 juillet 1920). **Désaccord** : le Barroux n'a pas de messe pour les fidèles ce dimanche-là ; il dit une messe conventuelle votive de Jeanne d'Arc le 8 mai (2025, 2026), anniversaire de la délivrance d'Orléans | Ordos séculiers seulement |
| **Sainte Thérèse de l'Enfant-Jésus** | Le **dernier dimanche de septembre** (24 au 30 septembre) | Le 29 septembre, saint Michel (Iʳᵉ classe), est un dimanche en 2030, 2041, 2047, 2052 et 2058 : la solennité serait alors omise, d'après la règle générale. Aucune source ne traite ce cas | Francefidele 2026-09-27, « Solennité de Ste Thérèse de l'Enfant-Jésus », IIᵉ classe ; rapport 05, § 2.1.2. **Ni la FSSPX ni le Barroux ne la célèbrent** | **La plus faible** (§ 7) |
| **Rosaire** | Le 1ᵉʳ dimanche d'octobre (1ᵉʳ au 7 octobre). C'est une solennité **de droit universel** (n. 358 b), pas un usage français | Le 3 octobre est un dimanche en 2027, 2032, 2038… : sainte Thérèse (IIᵉ classe en France) cède au dimanche et est commémorée, et la solennité du Rosaire se tient (FSSPX 2027-10-03). Si le 7 octobre est un dimanche, la fête cède au dimanche, et la solennité se tient ce jour-là | FSSPX 2025-10-05, 2026-10-04, 2027-10-03, 2030-10-06 ; Francefidele 2026-10-04 (« Solennité obligatoire »). Le Barroux ne prévoit **pas** de messe pour les fidèles | À coder dans les règles générales de 1962, pas dans celles de la France |
| **Sacré-Cœur** | Le 3ᵉ dimanche après la Pentecôte : solennité **de droit universel** (n. 358 a) | Omise quand ce dimanche est un jour de Iʳᵉ classe : le 29 juin en 2025 (la FSSPX n'en donne pas cette année-là), le 24 juin, le 1ᵉʳ juillet (2057) | FSSPX 2026-06-14, 2027-06-06, 2030-06-30 ; Francefidele 2026-06-14 (« facultative ») | Règles générales de 1962 |

**Usages relevés, mais non retenus :**
- **Solennité de la Purification**, le dimanche qui suit le 2 février (FSSPX 2026-02-08, 2027-02-07) : seule la FSSPX la célèbre, et rien ne la rattache à la France.
- **Saint Pie X**, solennité et Iʳᵉ classe : propre à la FSSPX.
- **Messe votive de Jeanne d'Arc le 8 mai** : usage conventuel du Barroux.
- **Procession du vœu de Louis XIII**, après les vêpres du 15 août (Barroux, Francefidele) : c'est un usage, pas une entrée de calendrier. On pourra l'afficher sur la page de la fête.

**Origine des solennités françaises** **[S]** : l'indult du cardinal Caprara du 9 avril 1802 a réduit les fêtes d'obligation à quatre (Noël, Ascension, Assomption, Toussaint) et renvoyé au dimanche la solennité de l'Épiphanie, du Saint-Sacrement, des saints Pierre et Paul et des patrons. Ces solennités relèvent donc du n. 361. Ce point vient de mémoire et n'a pas été vérifié dans le texte de l'indult.

## 7. Fêtes d'obligation

Les quatre fêtes du droit en vigueur s'appliquent au rite de 1962 : **Noël, Ascension, Assomption, Toussaint**. L'Ordo du Barroux ne donne pas de liste de fêtes d'obligation : il ne dit donc rien d'autre. La FSSPX (`ofPrecept` sur ces quatre jours seulement, en 2026 et 2027) et Francefidele (« Fête d'obligation » sur ces quatre jours) concordent. Ni le 1ᵉʳ janvier, ni l'Épiphanie, ni le Saint-Sacrement, ni saints Pierre et Paul, ni le 8 décembre ne sont d'obligation en France. **[V]**

## 8. Points à trancher par l'utilisateur

1. **Épiphanie** : faut-il suivre le Barroux (dimanche du 2 au 8 janvier, retenu) ou les Ordos séculiers (dimanche qui suit le 6 janvier) ? On pourrait proposer les deux en option.
2. **Solennités absentes de l'Ordo monastique** : saints Pierre et Paul, Jeanne d'Arc, Thérèse, Rosaire et Sacré-Cœur. Elles sont activées par défaut comme demandé, mais seuls l'Épiphanie et le Saint-Sacrement sont attestés par le monastère. **La solennité de sainte Thérèse** n'a qu'une source, Francefidele : faut-il la garder ?
3. **Collision entre saints Pierre et Paul et le Sacré-Cœur** le même dimanche : on suit la FSSPX (le Sacré-Cœur l'emporte, saints Pierre et Paul est omis), faute d'autre source.
4. **Sainte Geneviève** : on la retient en IIIᵉ classe d'après les trois sources romaines, alors que le Barroux ne l'a pas. À confirmer.
5. **Propre de la FSSPX non retenu** : sainte Clotilde, sainte Jeanne de Valois et sainte Radegonde en IIIᵉ classe, la Présentation en IIᵉ classe. À confirmer.
6. **Sainte Bernadette** (18 février) : faut-il l'ajouter en simple commémoraison, comme le fait le Barroux ?
7. **Textes des messes propres** de Jeanne d'Arc, Geneviève et Remi (phase 5) : Divinum Officium ne les a pas. Il faudra les tirer d'un missel du domaine public ou du dépôt privé.

## 9. Identifiants

Les identifiants viennent de `content/sources/calendrier-1962/sanctoral.tsv`, celui que l'agent du calendrier général utilisait ce jour-là. Pour les fêtes absentes du calendrier général, ce sont ceux de `1969/fr.json`. Tous sont des slugs romcal :
- `genevieve_of_paris_virgin` et `joan_of_arc_virgin` : repris de `1969/fr.json` ;
- `felix_i_pope`, `assumption_of_the_blessed_virgin_mary`, `remigius_of_reims_bishop`, `therese_of_the_child_jesus_and_the_holy_face_of_lisieux_virgin` et `all_saints` : repris de `sanctoral.tsv` ;
- `nativity_of_the_lord` : **à vérifier** quand le temporal de 1962 (`1962/TEMPORAL.md`) sera écrit. Il vient de 1969.

**À revérifier** quand `1962/general.json` sera publié :
- les titres latins des entrées qui en remplacent une : on les a pris dans `[Officium]` ou `[Rank]` de `horas/Latin/Sancti/` de Divinum Officium, comme le fait `general.py` ;
- l'absence de commémoraison générale les 3 janvier, 1ᵉʳ octobre, 3 octobre, 15 août et 1ᵉʳ novembre.
