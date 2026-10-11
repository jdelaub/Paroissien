# Divergences connues entre les oracles et notre moteur

Chaque écart entre `core:liturgy` et un oracle (`*.jsonl`) est listé ici, ligne par ligne, avec sa justification. `GoldenTest` refuse tout écart qui n'y figure pas : rien n'est accepté en silence (rapport 05, §3.1).

Format des lignes : `| rite | date | champ | détail |`. Le test lit les trois premières colonnes.

## 1969 : romcal, France (2025-2045)

### Jeudi saint, couleur

Aucune messe violette ce jour-là : messe chrismale et messe en mémoire de la Cène sont en blanc. romcal donne au jour la couleur violette de l'office du matin ; nous gardons le blanc.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1969 | 2025-04-17 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2026-04-02 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2027-03-25 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2028-04-13 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2029-03-29 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2030-04-18 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2031-04-10 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2032-03-25 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2033-04-14 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2034-04-06 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2035-03-17 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2035-03-22 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2036-04-10 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2037-04-02 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2038-04-22 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2039-04-07 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2040-03-29 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2041-04-18 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2042-04-03 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2043-03-26 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2044-04-14 | color | oracle=VIOLET nous=WHITE |
| 1969 | 2045-04-06 | color | oracle=VIOLET nous=WHITE |

### Samedi saint, couleur

Pas de messe ce jour-là ; romcal ne donne aucune couleur. Nous gardons le violet, pour que le ruban du jour ait une couleur.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1969 | 2025-04-19 | color | oracle=null nous=VIOLET |
| 1969 | 2026-04-04 | color | oracle=null nous=VIOLET |
| 1969 | 2027-03-27 | color | oracle=null nous=VIOLET |
| 1969 | 2028-04-15 | color | oracle=null nous=VIOLET |
| 1969 | 2029-03-31 | color | oracle=null nous=VIOLET |
| 1969 | 2030-04-20 | color | oracle=null nous=VIOLET |
| 1969 | 2031-04-12 | color | oracle=null nous=VIOLET |
| 1969 | 2032-03-27 | color | oracle=null nous=VIOLET |
| 1969 | 2033-04-16 | color | oracle=null nous=VIOLET |
| 1969 | 2034-04-08 | color | oracle=null nous=VIOLET |
| 1969 | 2035-03-24 | color | oracle=null nous=VIOLET |
| 1969 | 2036-04-12 | color | oracle=null nous=VIOLET |
| 1969 | 2037-04-04 | color | oracle=null nous=VIOLET |
| 1969 | 2038-04-24 | color | oracle=null nous=VIOLET |
| 1969 | 2039-04-09 | color | oracle=null nous=VIOLET |
| 1969 | 2040-03-31 | color | oracle=null nous=VIOLET |
| 1969 | 2041-04-20 | color | oracle=null nous=VIOLET |
| 1969 | 2042-04-05 | color | oracle=null nous=VIOLET |
| 1969 | 2043-03-28 | color | oracle=null nous=VIOLET |
| 1969 | 2044-04-16 | color | oracle=null nous=VIOLET |
| 1969 | 2045-04-08 | color | oracle=null nous=VIOLET |

### Cœur immaculé de Marie avec une autre mémoire obligatoire

Les deux deviennent facultatives et la férie demeure (notification de la Congrégation pour le culte divin, 1998 ; LitCal fait de même). romcal garde le Cœur immaculé comme mémoire obligatoire.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1969 | 2025-06-28 | id | oracle=immaculate_heart_of_mary nous=ordinary_time_12_saturday |
| 1969 | 2025-06-28 | rank | oracle=MEMORIAL nous=WEEKDAY |
| 1969 | 2025-06-28 | color | oracle=WHITE nous=GREEN |
| 1969 | 2025-06-28 | optional | oracle=[] nous=[immaculate_heart_of_mary, irenaeus_of_lyon_bishop] |
| 1969 | 2026-06-13 | id | oracle=immaculate_heart_of_mary nous=ordinary_time_10_saturday |
| 1969 | 2026-06-13 | rank | oracle=MEMORIAL nous=WEEKDAY |
| 1969 | 2026-06-13 | color | oracle=WHITE nous=GREEN |
| 1969 | 2026-06-13 | optional | oracle=[] nous=[immaculate_heart_of_mary, anthony_of_padua_priest] |
| 1969 | 2027-06-05 | id | oracle=immaculate_heart_of_mary nous=ordinary_time_9_saturday |
| 1969 | 2027-06-05 | rank | oracle=MEMORIAL nous=WEEKDAY |
| 1969 | 2027-06-05 | color | oracle=WHITE nous=GREEN |
| 1969 | 2027-06-05 | optional | oracle=[] nous=[immaculate_heart_of_mary, boniface_of_mainz_bishop] |
| 1969 | 2031-06-21 | id | oracle=immaculate_heart_of_mary nous=ordinary_time_11_saturday |
| 1969 | 2031-06-21 | rank | oracle=MEMORIAL nous=WEEKDAY |
| 1969 | 2031-06-21 | color | oracle=WHITE nous=GREEN |
| 1969 | 2031-06-21 | optional | oracle=[] nous=[immaculate_heart_of_mary, aloysius_gonzaga_religious] |
| 1969 | 2032-06-05 | id | oracle=immaculate_heart_of_mary nous=ordinary_time_9_saturday |
| 1969 | 2032-06-05 | rank | oracle=MEMORIAL nous=WEEKDAY |
| 1969 | 2032-06-05 | color | oracle=WHITE nous=GREEN |
| 1969 | 2032-06-05 | optional | oracle=[] nous=[immaculate_heart_of_mary, boniface_of_mainz_bishop] |
| 1969 | 2036-06-21 | id | oracle=immaculate_heart_of_mary nous=ordinary_time_11_saturday |
| 1969 | 2036-06-21 | rank | oracle=MEMORIAL nous=WEEKDAY |
| 1969 | 2036-06-21 | color | oracle=WHITE nous=GREEN |
| 1969 | 2036-06-21 | optional | oracle=[] nous=[immaculate_heart_of_mary, aloysius_gonzaga_religious] |
| 1969 | 2037-06-13 | id | oracle=immaculate_heart_of_mary nous=ordinary_time_10_saturday |
| 1969 | 2037-06-13 | rank | oracle=MEMORIAL nous=WEEKDAY |
| 1969 | 2037-06-13 | color | oracle=WHITE nous=GREEN |
| 1969 | 2037-06-13 | optional | oracle=[] nous=[immaculate_heart_of_mary, anthony_of_padua_priest] |

### Saint John Henry Newman avant son inscription

Le décret d'inscription date du 9 novembre 2025 : la mémoire facultative n'existe qu'à partir de 2026 (`validFrom`). romcal la donne pour toutes les années.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1969 | 2025-10-09 | optional | oracle=[denis_of_paris_bishop_and_companions_martyrs, john_leonardi_priest, john_henry_newman_priest] nous=[denis_of_paris_bishop_and_companions_martyrs, john_leonardi_priest] |

### Cœur immaculé de Marie et une fête ou une solennité

Défaut de romcal : une mémoire évince une solennité ou une fête. Nous appliquons la préséance des NU (n. 59) : la fête ou la solennité l'emporte.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1969 | 2028-06-24 | id | oracle=immaculate_heart_of_mary nous=nativity_of_john_the_baptist |
| 1969 | 2028-06-24 | rank | oracle=MEMORIAL nous=SOLEMNITY |
| 1969 | 2030-06-29 | id | oracle=immaculate_heart_of_mary nous=peter_and_paul_apostles |
| 1969 | 2030-06-29 | rank | oracle=MEMORIAL nous=SOLEMNITY |
| 1969 | 2030-06-29 | color | oracle=WHITE nous=RED |
| 1969 | 2038-07-03 | id | oracle=immaculate_heart_of_mary nous=thomas_apostle |
| 1969 | 2038-07-03 | rank | oracle=MEMORIAL nous=FEAST |
| 1969 | 2038-07-03 | color | oracle=WHITE nous=RED |
| 1969 | 2041-06-29 | id | oracle=immaculate_heart_of_mary nous=peter_and_paul_apostles |
| 1969 | 2041-06-29 | rank | oracle=MEMORIAL nous=SOLEMNITY |
| 1969 | 2041-06-29 | color | oracle=WHITE nous=RED |

### Nativité de saint Jean-Baptiste le jour du Sacré-Cœur

Défaut de romcal, qui supprime la solennité. Nous la transférons au jour libre le plus proche, la veille (NU 60 ; précédent de 2022, confirmé par l'AELF).

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1969 | 2033-06-23 | id | oracle=ordinary_time_12_thursday nous=nativity_of_john_the_baptist |
| 1969 | 2033-06-23 | rank | oracle=WEEKDAY nous=SOLEMNITY |
| 1969 | 2033-06-23 | color | oracle=GREEN nous=WHITE |
| 1969 | 2044-06-23 | id | oracle=ordinary_time_12_thursday nous=nativity_of_john_the_baptist |
| 1969 | 2044-06-23 | rank | oracle=WEEKDAY nous=SOLEMNITY |
| 1969 | 2044-06-23 | color | oracle=GREEN nous=WHITE |

### Saint Joseph en Semaine sainte

Défaut de romcal, qui omet la solennité en 2035. Nous l'avançons au samedi qui précède les Rameaux (NU 60 ; précédent de 2008).

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1969 | 2035-03-17 | id | oracle=lent_5_saturday nous=joseph_spouse_of_mary |
| 1969 | 2035-03-17 | rank | oracle=WEEKDAY nous=SOLEMNITY |
| 1969 | 2035-03-17 | optional | oracle=[patrick_of_ireland_bishop] nous=[] |

### Marie, Mère de l'Église, avec une autre mémoire

La notification de 2018 lui donne la préséance sur une autre mémoire obligatoire. romcal garde saint Philippe Néri.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1969 | 2042-05-26 | id | oracle=philip_neri_priest nous=mary_mother_of_the_church |

## 1962 : Missalemeum, calendrier général (2020-2045)

Missalemeum ne garde qu'une commémoraison par jour : `GoldenTest` vérifie seulement que la sienne figure parmi les nôtres. Une date `*-MM-JJ` vaut pour toutes les années : l'oracle se trompe toujours sur cette fête.

### Dimanche des Rameaux, ordre des couleurs

Le violet est la couleur de la messe, le rouge celle de la seule bénédiction et procession (n. 126, 128 b). Missalemeum met le rouge en premier.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1962 | 2020-04-05 | color | oracle=RED nous=VIOLET |
| 1962 | 2021-03-28 | color | oracle=RED nous=VIOLET |
| 1962 | 2022-04-10 | color | oracle=RED nous=VIOLET |
| 1962 | 2023-04-02 | color | oracle=RED nous=VIOLET |
| 1962 | 2024-03-24 | color | oracle=RED nous=VIOLET |
| 1962 | 2025-04-13 | color | oracle=RED nous=VIOLET |
| 1962 | 2026-03-29 | color | oracle=RED nous=VIOLET |
| 1962 | 2027-03-21 | color | oracle=RED nous=VIOLET |
| 1962 | 2028-04-09 | color | oracle=RED nous=VIOLET |
| 1962 | 2029-03-25 | color | oracle=RED nous=VIOLET |
| 1962 | 2030-04-14 | color | oracle=RED nous=VIOLET |
| 1962 | 2030-04-14 | commemorations | oracle=[Sancti/04-14] nous=[] |
| 1962 | 2031-04-06 | color | oracle=RED nous=VIOLET |
| 1962 | 2032-03-21 | color | oracle=RED nous=VIOLET |
| 1962 | 2033-04-10 | color | oracle=RED nous=VIOLET |
| 1962 | 2034-04-02 | color | oracle=RED nous=VIOLET |
| 1962 | 2035-03-18 | color | oracle=RED nous=VIOLET |
| 1962 | 2036-04-06 | color | oracle=RED nous=VIOLET |
| 1962 | 2037-03-29 | color | oracle=RED nous=VIOLET |
| 1962 | 2038-04-18 | color | oracle=RED nous=VIOLET |
| 1962 | 2039-04-03 | color | oracle=RED nous=VIOLET |
| 1962 | 2040-03-25 | color | oracle=RED nous=VIOLET |
| 1962 | 2041-04-14 | color | oracle=RED nous=VIOLET |
| 1962 | 2041-04-14 | commemorations | oracle=[Sancti/04-14] nous=[] |
| 1962 | 2042-03-30 | color | oracle=RED nous=VIOLET |
| 1962 | 2043-03-22 | color | oracle=RED nous=VIOLET |
| 1962 | 2044-04-10 | color | oracle=RED nous=VIOLET |
| 1962 | 2045-04-02 | color | oracle=RED nous=VIOLET |

### Féries de la semaine de la Sainte Famille après le 13 janvier

Le temps de Noël finit le 13 janvier ; les féries qui suivent sont vertes (n. 72, 77, 127 a). Missalemeum les garde blanches.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1962 | 2030-01-18 | color | oracle=WHITE nous=GREEN |
| 1962 | 2036-01-18 | color | oracle=WHITE nous=GREEN |
| 1962 | 2041-01-18 | color | oracle=WHITE nous=GREEN |

### Couleur de douze fêtes de IIIᵉ classe

Missalemeum prend la couleur du saint commémoré au lieu de celle du saint du jour. Divinum Officium et le Calendarium de 1961 donnent la couleur indiquée.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1962 | *-06-12 | color | Saint Jean de Saint-Facond (blanc ; commémoraison des saints Basilide et compagnons, martyrs) |
| 1962 | *-06-18 | color | Saint Éphrem (blanc ; commémoraison des saints Marc et Marcellien, martyrs) |
| 1962 | *-07-18 | color | Saint Camille de Lellis (blanc ; commémoraison de sainte Symphorose et ses fils, martyrs) |
| 1962 | *-07-20 | color | Saint Jérôme Émilien (blanc ; commémoraison de sainte Marguerite, martyre) |
| 1962 | *-07-23 | color | Saint Apollinaire, évêque et martyr (rouge) |
| 1962 | *-07-29 | color | Sainte Marthe (blanc ; commémoraison des saints Félix, Simplice et compagnons, martyrs) |
| 1962 | *-08-28 | color | Saint Augustin (blanc ; commémoraison de saint Hermès, martyr) |
| 1962 | *-11-14 | color | Saint Josaphat, évêque et martyr (rouge) |
| 1962 | *-06-19 | color | Sainte Julienne Falconieri (blanc ; commémoraison des saints Gervais et Protais, martyrs) |
| 1962 | *-07-12 | color | Saint Jean Gualbert (blanc ; commémoraison des saints Nabor et Félix, martyrs) |
| 1962 | *-08-02 | color | Saint Alphonse de Liguori (blanc ; commémoraison de saint Étienne, pape et martyr) |
| 1962 | *-08-30 | color | Sainte Rose de Lima (blanc ; commémoraison des saints Félix et Adaucte, martyrs) |

### Saint Ubald (16 mai) et saint Didace (13 novembre)

Fêtes de IIIᵉ classe au Calendarium du Bréviaire de 1961 et dans Divinum Officium ; Missalemeum en fait de simples commémoraisons. Tous les champs de ces deux jours en dépendent.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1962 | *-05-16 | proper | IIIᵉ classe au Calendarium de 1961 |
| 1962 | *-05-16 | rank | IIIᵉ classe au Calendarium de 1961 |
| 1962 | *-05-16 | commemorations | IIIᵉ classe au Calendarium de 1961 |
| 1962 | *-11-13 | proper | IIIᵉ classe au Calendarium de 1961 |
| 1962 | *-11-13 | rank | IIIᵉ classe au Calendarium de 1961 |
| 1962 | *-11-13 | color | IIIᵉ classe au Calendarium de 1961 |
| 1962 | *-11-13 | commemorations | IIIᵉ classe au Calendarium de 1961 |
| 1962 | *-05-16 | color | IIIᵉ classe au Calendarium de 1961 |

### Commémoraison de saint Pierre le 30 juin

Commémoraison intrinsèque de la Commémoraison de saint Paul (Calendarium de 1961 ; propre de Divinum Officium). Missalemeum ne l'a pas.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1962 | *-06-30 | commemorations | saint Pierre |

### Vigile de saint Laurent (9 août)

Vigile de IIIᵉ classe, violette (n. 130). Missalemeum la met en rouge, et la remplace le samedi par la Sainte Vierge au samedi, que le n. 78 réserve aux féries de IVᵉ classe.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1962 | *-08-09 | color | vigile en violet |
| 1962 | 2025-08-09 | proper | oracle=Commune/C10 nous=Sancti/08-09 |
| 1962 | 2025-08-09 | rank | oracle=IV nous=III |
| 1962 | 2025-08-09 | color | oracle=WHITE nous=VIOLET |
| 1962 | 2031-08-09 | proper | oracle=Commune/C10 nous=Sancti/08-09 |
| 1962 | 2031-08-09 | rank | oracle=IV nous=III |
| 1962 | 2031-08-09 | color | oracle=WHITE nous=VIOLET |
| 1962 | 2036-08-09 | proper | oracle=Commune/C10 nous=Sancti/08-09 |
| 1962 | 2036-08-09 | rank | oracle=IV nous=III |
| 1962 | 2036-08-09 | color | oracle=WHITE nous=VIOLET |
| 1962 | 2042-08-09 | proper | oracle=Commune/C10 nous=Sancti/08-09 |
| 1962 | 2042-08-09 | rank | oracle=IV nous=III |
| 1962 | 2042-08-09 | color | oracle=WHITE nous=VIOLET |

### Fête de Iʳᵉ classe un dimanche : commémoraison du dimanche

La commémoraison du dimanche est privilégiée (n. 108 a) et admise un jour de Iʳᵉ classe (n. 111 a), sauf pour une fête du Seigneur, qui tient la place du dimanche (n. 16). Missalemeum ne la fait que pour saint Michel et saints Pierre et Paul.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1962 | 2020-11-01 | commemorations | oracle=[] nous=[Tempora/Pent22-0] |
| 1962 | 2021-08-15 | commemorations | oracle=[] nous=[Tempora/Pent12-0] |
| 1962 | 2022-05-01 | commemorations | oracle=[] nous=[Tempora/Pasc2-0] |
| 1962 | 2026-11-01 | commemorations | oracle=[] nous=[Tempora/Pent23-0] |
| 1962 | 2027-08-15 | commemorations | oracle=[] nous=[Tempora/Pent13-0] |
| 1962 | 2029-06-24 | commemorations | oracle=[] nous=[Tempora/Pent05-0] |
| 1962 | 2032-08-15 | commemorations | oracle=[] nous=[Tempora/Pent13-0] |
| 1962 | 2033-05-01 | commemorations | oracle=[] nous=[Tempora/Pasc2-0] |
| 1962 | 2035-06-24 | commemorations | oracle=[] nous=[Tempora/Pent06-0] |
| 1962 | 2037-11-01 | commemorations | oracle=[] nous=[Tempora/Pent23-0] |
| 1962 | 2038-08-15 | commemorations | oracle=[] nous=[Tempora/Pent09-0] |
| 1962 | 2039-05-01 | commemorations | oracle=[] nous=[Tempora/Pasc3-0] |
| 1962 | 2040-06-24 | commemorations | oracle=[] nous=[Tempora/Pent05-0] |
| 1962 | 2043-11-01 | commemorations | oracle=[] nous=[Tempora/Epi4-0] |
| 1962 | 2044-05-01 | commemorations | oracle=[] nous=[Tempora/Pasc2-0] |

### Fête de Iʳᵉ ou IIᵉ classe en Avent : commémoraison de la férie

La férie de l'Avent est une commémoraison privilégiée (n. 108 c), admise un jour de Iʳᵉ classe (n. 111 a) et de IIᵉ classe (n. 111 b). Missalemeum l'omet à l'Immaculée Conception et à saint André.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1962 | 2020-11-30 | commemorations | oracle=[] nous=[Tempora/Adv1-1] |
| 1962 | 2020-12-08 | commemorations | oracle=[] nous=[Tempora/Adv2-2] |
| 1962 | 2021-11-30 | commemorations | oracle=[] nous=[Tempora/Adv1-2] |
| 1962 | 2021-12-08 | commemorations | oracle=[] nous=[Tempora/Adv2-3] |
| 1962 | 2022-11-30 | commemorations | oracle=[] nous=[Tempora/Adv1-3] |
| 1962 | 2022-12-08 | commemorations | oracle=[] nous=[Tempora/Adv2-4] |
| 1962 | 2023-12-08 | commemorations | oracle=[] nous=[Tempora/Adv1-5] |
| 1962 | 2025-12-08 | commemorations | oracle=[] nous=[Tempora/Adv2-1] |
| 1962 | 2026-11-30 | commemorations | oracle=[] nous=[Tempora/Adv1-1] |
| 1962 | 2026-12-08 | commemorations | oracle=[] nous=[Tempora/Adv2-2] |
| 1962 | 2027-11-30 | commemorations | oracle=[] nous=[Tempora/Adv1-2] |
| 1962 | 2027-12-08 | commemorations | oracle=[] nous=[Tempora/Adv2-3] |
| 1962 | 2028-12-08 | commemorations | oracle=[] nous=[Tempora/Adv1-5] |
| 1962 | 2029-12-08 | commemorations | oracle=[] nous=[Tempora/Adv1-6] |
| 1962 | 2031-12-08 | commemorations | oracle=[] nous=[Tempora/Adv2-1] |
| 1962 | 2032-11-30 | commemorations | oracle=[] nous=[Tempora/Adv1-2] |
| 1962 | 2032-12-08 | commemorations | oracle=[] nous=[Tempora/Adv2-3] |
| 1962 | 2033-11-30 | commemorations | oracle=[] nous=[Tempora/Adv1-3] |
| 1962 | 2033-12-08 | commemorations | oracle=[] nous=[Tempora/Adv2-4] |
| 1962 | 2034-12-08 | commemorations | oracle=[] nous=[Tempora/Adv1-5] |
| 1962 | 2035-12-08 | commemorations | oracle=[] nous=[Tempora/Adv1-6] |
| 1962 | 2036-12-08 | commemorations | oracle=[] nous=[Tempora/Adv2-1] |
| 1962 | 2037-11-30 | commemorations | oracle=[] nous=[Tempora/Adv1-1] |
| 1962 | 2037-12-08 | commemorations | oracle=[] nous=[Tempora/Adv2-2] |
| 1962 | 2038-11-30 | commemorations | oracle=[] nous=[Tempora/Adv1-2] |
| 1962 | 2038-12-08 | commemorations | oracle=[] nous=[Tempora/Adv2-3] |
| 1962 | 2039-11-30 | commemorations | oracle=[] nous=[Tempora/Adv1-3] |
| 1962 | 2039-12-08 | commemorations | oracle=[] nous=[Tempora/Adv2-4] |
| 1962 | 2040-12-08 | commemorations | oracle=[] nous=[Tempora/Adv1-6] |
| 1962 | 2042-12-08 | commemorations | oracle=[] nous=[Tempora/Adv2-1] |
| 1962 | 2043-11-30 | commemorations | oracle=[] nous=[Tempora/Adv1-1] |
| 1962 | 2043-12-08 | commemorations | oracle=[] nous=[Tempora/Adv2-2] |
| 1962 | 2044-11-30 | commemorations | oracle=[] nous=[Tempora/Adv1-3] |
| 1962 | 2044-12-08 | commemorations | oracle=[] nous=[Tempora/Adv2-4] |
| 1962 | 2045-12-08 | commemorations | oracle=[] nous=[Tempora/Adv1-5] |

### Fête de IIᵉ classe un dimanche, avec un saint commémoré

Un jour de IIᵉ classe admet une commémoraison (n. 111 b) : celle du saint du jour (Télesphore, Hygin, Sixte, Théodore) ; le dimanche, occupé par une fête du Seigneur, n'est pas commémoré (n. 16). Missalemeum n'en fait aucune.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1962 | 2020-01-05 | commemorations | oracle=[] nous=[Sancti/01-05] |
| 1962 | 2023-08-06 | commemorations | oracle=[] nous=[Sancti/08-06] |
| 1962 | 2025-01-05 | commemorations | oracle=[] nous=[Sancti/01-05] |
| 1962 | 2025-11-09 | commemorations | oracle=[] nous=[Sancti/11-09] |
| 1962 | 2026-01-11 | commemorations | oracle=[] nous=[Sancti/01-11] |
| 1962 | 2028-08-06 | commemorations | oracle=[] nous=[Sancti/08-06] |
| 1962 | 2031-01-05 | commemorations | oracle=[] nous=[Sancti/01-05] |
| 1962 | 2031-11-09 | commemorations | oracle=[] nous=[Sancti/11-09] |
| 1962 | 2032-01-11 | commemorations | oracle=[] nous=[Sancti/01-11] |
| 1962 | 2034-08-06 | commemorations | oracle=[] nous=[Sancti/08-06] |
| 1962 | 2036-11-09 | commemorations | oracle=[] nous=[Sancti/11-09] |
| 1962 | 2037-01-11 | commemorations | oracle=[] nous=[Sancti/01-11] |
| 1962 | 2042-01-05 | commemorations | oracle=[] nous=[Sancti/01-05] |
| 1962 | 2042-11-09 | commemorations | oracle=[] nous=[Sancti/11-09] |
| 1962 | 2043-01-11 | commemorations | oracle=[] nous=[Sancti/01-11] |
| 1962 | 2045-08-06 | commemorations | oracle=[] nous=[Sancti/08-06] |

### Saint Joseph le vendredi de la Passion

La fête de Iʳᵉ classe l'emporte sur la férie de IIIᵉ classe (table n. 91). La règle des Sept Douleurs de Missalemeum, appliquée avant la préséance, l'éclipse.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1962 | 2027-03-19 | proper | oracle=Tempora/Quad5-5 nous=Sancti/03-19 |
| 1962 | 2027-03-19 | rank | oracle=III nous=I |
| 1962 | 2027-03-19 | color | oracle=VIOLET nous=WHITE |
| 1962 | 2032-03-19 | proper | oracle=Tempora/Quad5-5 nous=Sancti/03-19 |
| 1962 | 2032-03-19 | rank | oracle=III nous=I |
| 1962 | 2032-03-19 | color | oracle=VIOLET nous=WHITE |

### Commémoraison non privilégiée un dimanche de Iʳᵉ classe

Un jour de Iʳᵉ classe n'admet que les commémoraisons privilégiées (n. 111 a) : saint Casimir n'est pas commémoré le dimanche de Lætare.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1962 | 2035-03-04 | commemorations | oracle=[Sancti/03-04] nous=[] |

### Samedi de la Sainte Vierge, année bissextile

Saint Gabriel de la Vierge des Douleurs passe au 28 février : le samedi 27 est une férie de IVᵉ classe, donc Sainte Marie au samedi (n. 78). Missalemeum garde la férie.

| Rite | Date | Champ | Détail |
|---|---|---|---|
| 1962 | 2044-02-27 | proper | oracle=Tempora/Quadp2-6 nous=Commune/C10 |
| 1962 | 2044-02-27 | color | oracle=VIOLET nous=WHITE |
