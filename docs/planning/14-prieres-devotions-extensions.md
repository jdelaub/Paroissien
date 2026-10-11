# 14 — Prières classiques, dévotions et modules d'extension

> Agent 14 · phase de planification · rapport rédigé le 2026-10-11
> Périmètre : corpus de prières (latin/français), dévotions interactives, modules d'extension et architecture des « paquets de contenu ».
>
> **Légende de fiabilité** : ✔ = vérifié sur une source citée en §6 · ≈ = supposé, connu de mémoire ou déduit, **à vérifier** · ⚖ = point juridique à transmettre à l'agent 12.
>
> Aucun texte protégé n'est reproduit ici. Les prières du domaine public sont nommées par leur titre ou leur incipit, sans être recopiées.

---

## 1. Résumé

- **Un MVP d'environ 40 textes est réaliste et peu coûteux** : prières usuelles, quatre litanies, prières du matin et du soir et antiennes mariales. On les tire **d'anciens paroissiens romains du domaine public** : plusieurs éditions Mame (1869, 1872, 1890), Zech (1901), Barbou (vers 1895) et Lecoffre (1860) sont numérisées sur archive.org (✔ identifiants en §6). Le latin est libre.
- **Les versions françaises *actuelles* des prières usuelles sont des traductions liturgiques AELF**, protégées par le droit d'auteur : Notre Père de 2013, en usage liturgique depuis le 3 décembre 2017 (✔), ainsi que le Credo et le *Je confesse* du Missel de 2021, en vigueur depuis le 28 novembre 2021 (✔). L'AELF ne publie aucune licence. Les applications tierces affichent « avec l'aimable autorisation de l'AELF » (✔). ⚖ **Il faut donc demander une autorisation à l'AELF**, faute de quoi seules les formes traditionnelles du domaine public seront proposées.
- **Le réglage proposé est un « profil de textes »** (*Traditionnel*, *Liturgie actuelle* ou *Personnalisé*) plutôt qu'un simple interrupteur entre « tu » et « vous ». Le tutoiement ne touche en effet que certaines prières : le Notre Père depuis 1966, mais pas l'Ave, qui reste au vouvoiement. Par défaut, ce profil suit le rite choisi dans le module Missel (agent 03).
- **Un moteur de dévotions déclaratif, en code natif, piloté par des données.** Une petite grammaire (séquence, répétition, grain, annonce, choix selon le jour ou le temps liturgique, étape, minuteur, liste privée, plan de jours) suffit à décrire le rosaire, les chapelets, le chemin de croix, les neuvaines, l'Heure sainte, les consécrations et l'examen. Ajouter une dévotion se fait alors **par paquet, sans mettre à jour l'application**. Seul un nouveau *type d'interaction* exige du code.
- **Rosaire guidé (MVP)** : les mystères suivent le jour, avec deux répartitions au choix, celle de *Rosarium Virginis Mariae* (2002, avec les mystères lumineux le jeudi) et la répartition traditionnelle à 15 mystères (✔ RVM §38). Le dizainier est tactile et plein écran. Le retour haptique s'appuie sur `HapticFeedbackType.SegmentTick` ou sur `VibrationEffect.Composition.PRIMITIVE_TICK` (✔). La *clausula* est proposée en option (✔ RVM §33).
- **Rappels de l'Angélus**, facultatifs et désactivés par défaut : ils sont programmés par une alarme *inexacte* autorisée en Doze, sans demander la permission d'alarme exacte, refusée par défaut depuis Android 14 (✔). Le Regina Caeli remplace automatiquement l'Angélus au temps pascal (✔), selon le calendrier de l'agent 05. La cloche peut être remplacée par une vibration discrète au rythme 3‑3‑3‑9.
- **Examen de conscience et confession** : le texte vient d'examens du domaine public, organisés selon les commandements. **Rien n'est enregistré par défaut** : les notes restent éphémères, ces écrans sont exclus des sauvegardes et des captures, et les notifications ne révèlent jamais leur contenu. Les composants « liste » et « minuteur » sont partagés avec l'examen ignatien de l'agent 02.
- **Modules d'extension.** Les gains rapides sont la *Légende dorée* (Wyzewa, texte propre sur Gutenberg ✔), les Complies et le Petit office de la Sainte Vierge, le *Catéchisme du concile de Trente* (traductions du XIXᵉ siècle sur archive.org ✔), les **neuvaines et consécrations**, et des métadonnées d'indulgences reformulées à partir de l'*Enchiridion* de 1999 (✔). Les chantiers lourds sont l'office divin (Divinum Officium : code MIT ✔, mais **traductions françaises de provenance mixte** ✔ ⚖), les *Petits Bollandistes* (17 volumes à corriger après OCR), le chant grégorien et la Liturgie des Heures de l'AELF (⚖ autorisation).
- **À exclure du contenu embarqué** : le *Catéchisme de l'Église catholique* et son *Abrégé* (© LEV et éditeurs, ✔), les *Sources chrétiennes* (Cerf), l'*Enchiridion* dans son texte intégral et les traductions modernes de sainte Faustine (© Marian Fathers ✔). On les remplace par des **renvois** (numéros de paragraphe, liens vers vatican.va).
- **Risques majeurs** : les droits sur les textes liturgiques français modernes ; la provenance des traductions françaises de Divinum Officium ; la mention obsolète de jours d'indulgence dans les paroissiens anciens (★ à purger) ; le retard des alarmes chez certains constructeurs ; l'étendue d'un moteur de bréviaire ; la qualité de l'OCR et la relecture doctrinale.

---

## 2. Constats détaillés

### 2.1 Corpus des prières classiques

#### 2.1.1 Où trouver des textes français du domaine public

| Source | Contenu utile | Statut | Fiabilité |
|---|---|---|---|
| **Paroissiens romains numérisés (archive.org)** : Mame 1869 `paroissienromai00churgoog`, Mame 1872 `paroissienromain00cath_0`, Mame 1890 `paroissienromain00tour`, Zech 1901 `paroissienromain00cath`, Barbou vers 1895 `leparoissienroma00unse` (avec chemin de croix), Lecoffre 1860 `bub_gb_PcH1G0A8jZUC`, Belin 1830 `paroissienromain00unse`, *Paroissien complet* 1842 `paroissiencomple00unse` | Prières usuelles, prières du matin et du soir, vêpres et complies, litanies, chemin de croix, examen et confession | Domaine public : les éditeurs et traducteurs sont des XIXᵉ et début XXᵉ siècles, souvent des ouvrages anonymes publiés il y a plus de 70 ans | ✔ existence des notices · ≈ statut de chaque édition |
| **Canadiana** (Québec) : *Paroissien romain* 1893 (`cihm_95158`), *Paroissien de poche* 1914 (`cihm_80314`), *Nouveau paroissien romain* 1913 (`cihm_990278`), *Paroissien noté* 1873 et 1883 | Prières usuelles « en harmonie avec les décrets pontificaux » de Pie X : la communion fréquente, par exemple | Domaine public probable (anonymes de plus de 70 ans) | ✔ notices · ≈ statut |
| **Gallica (BnF)** | Nombreux paroissiens (Mame, Desclée, Morizot) | Œuvres du domaine public. **La réutilisation non commerciale est libre si la source est mentionnée, la réutilisation commerciale passe par une licence payante** (✔ CGU citées par linuxfr et l'Assemblée nationale). L'API SRU de Gallica a répondu 403 à l'outil : **la recherche n'a pas pu être faite ici** | ✔ régime · ✘ inventaire non réalisé |
| **Catéchismes diocésains anciens** (Canadiana, archive.org) | Actes de foi, d'espérance, de charité et de contrition dans leur forme traditionnelle | Domaine public | ≈ |

**Recommandation de méthode** (pour l'agent 11) : choisir **un paroissien de référence**, par exemple Mame 1890, pour le français traditionnel, et **un second exemplaire pour la collation** (Zech 1901 ou Barbou). On conserve dans les métadonnées de chaque texte la page source, l'URL du scan et l'*imprimatur*, que l'on affichera dans le colophon. Si l'on passe par archive.org plutôt que par Gallica, la question de la réutilisation commerciale imposée par la BnF ne se pose pas (⚖ à confirmer par l'agent 12).

#### 2.1.2 Variantes françaises : ce qui a été vérifié

| Prière | Forme traditionnelle (domaine public) | 1966 → 2017 | Forme actuelle | Statut des droits |
|---|---|---|---|---|
| **Notre Père** | Vouvoiement (« qui êtes aux cieux ») ; 6ᵉ demande « ne nous laissez pas succomber… » (≈ à confirmer sur un paroissien) | Version œcuménique de 1966 (✔ communiqué du 4 janvier 1966), tutoiement, « ne nous soumets pas à la tentation » | « Ne nous laisse pas entrer en tentation », tirée de la Bible liturgique de 2013 (✔), **en vigueur depuis le 3 décembre 2017 en France** (✔) et le 1ᵉʳ avril 2018 en Suisse romande (✔) | 1966 : ≈ commission œcuménique ou AELF · 2013 : ⚖ © AELF probable |
| **Je vous salue Marie** | Vouvoiement | — | **Toujours au vouvoiement** dans l'usage officiel en France (≈ ; seule source : une correction sur un dépôt open source) | Forme traditionnelle libre |
| **Symbole de Nicée-Constantinople** | Traductions des anciens paroissiens | « De même nature que le Père » (1970–2021) | « **Consubstantiel au Père** », Missel de 2021, **en usage depuis le 28 novembre 2021** (✔) | ⚖ © AELF (Missel) |
| **Je confesse à Dieu** | Forme longue traditionnelle | Forme de 1970 | Missel 2021 : « je reconnais devant vous, frères et sœurs », mention de la « bienheureuse Vierge Marie » (✔) | ⚖ © AELF |
| **Symbole des Apôtres, Gloire au Père** | Formes anciennes | — | Différences mineures | ≈ |

**Conclusion** : le vouvoiement et le tutoiement ne forment **pas** un axe unique. En pratique, ce sont des **familles de versions** : avant 1966, de 1966 à 2017 et 2021, et forme actuelle.

#### 2.1.3 Litanies : il existe deux états du texte

- **Lorette** : trois invocations ont été ajoutées le 20 juin 2020, *Mater misericordiae* (après *Mater Ecclesiae*), *Mater spei* et *Solacium migrantium*, chacune à une place fixée (✔). *Mater Ecclesiae* date de 2018 (≈ date).
- **Saint Joseph** : sept invocations ont été ajoutées le 1ᵉʳ mai 2021 (*Custos Redemptoris*, *Serve Christi*, *Minister salutis*, *Fulcimen in difficultatibus*, *Patrone exsulum*, *Patrone afflictorum*, *Patrone pauperum*), ce qui porte la litanie à 31 invocations (✔).
- **Sacré-Cœur** : forme stable depuis 1899 (≈).
- **Litanies des saints** : la forme traditionnelle du Rituel (1962) diffère des formes brèves de la liturgie actuelle (≈).
- Les litanies approuvées pour l'usage public sont au nombre de six : Saint Nom, Sacré-Cœur, Précieux Sang, Lorette, Saint Joseph et Saints (≈). L'*Enchiridion* les mentionne à la concession 22, 2° (✔).

→ Chaque litanie aura deux variantes : **« forme actuelle »**, par défaut si le rite choisi est la forme ordinaire, et **« forme 1962 »**. Les invocations récentes sont marquées dans les données (`since: 2020`), ce qui permet de les filtrer.

#### 2.1.4 Proposition de liste pour le MVP (≈ 40 textes)

| # | Texte (latin / français) | Latin | Source française du domaine public | Variantes | Interface |
|---|---|---|---|---|---|
| 1 | Signe de croix (*In nomine Patris*) | ✔ libre | Paroissien | — | |
| 2 | *Pater noster* / Notre Père | ✔ | Paroissien (vouvoiement) | Traditionnel / 1966 / 2017 ⚖ | 03 |
| 3 | *Ave Maria* / Je vous salue Marie | ✔ | Paroissien | (tutoiement : plus tard) | |
| 4 | *Gloria Patri* / Gloire au Père | ✔ | Paroissien | mineures | |
| 5 | Symbole des Apôtres | ✔ | Paroissien | traditionnel / actuel ⚖ | |
| 6 | Symbole de Nicée-Constantinople | ✔ | Paroissien | traditionnel / 1970 / 2021 ⚖ | 03 |
| 7 | *Confiteor* / Je confesse à Dieu | ✔ | Paroissien (forme longue) | traditionnel / 2021 ⚖ | 03 |
| 8 | *Salve Regina* | ✔ | Paroissien | — | 05 |
| 9–11 | *Alma Redemptoris Mater*, *Ave Regina caelorum*, *Regina caeli* (antiennes mariales du temps) | ✔ | Paroissien | choisies selon le temps liturgique | 05 |
| 12 | *Angelus* | ✔ | Paroissien | — | 05, 10 |
| 13 | *Memorare* / Souvenez-vous | ✔ | Paroissien | — | |
| 14 | *Sub tuum praesidium* | ✔ | Paroissien | — | |
| 15–18 | Actes de foi, d'espérance, de charité et de contrition | ✔ (*Actus fidei*…) | Catéchisme ancien ou paroissien | forme longue / forme courte | 02 |
| 19 | *Angele Dei* / Ange de Dieu | ✔ | Paroissien | — | |
| 20 | Bénédicité et grâces | ✔ | Paroissien | — | |
| 21 | *Requiem aeternam* | ✔ | Paroissien | — | |
| 22 | *De profundis* (Ps 129 dans la Vulgate, 130 dans la numérotation hébraïque) | ✔ Vulgate | Paroissien ou Bible du domaine public (04) | numérotation des psaumes | 04 |
| 23 | *Miserere* (Ps 50 dans la Vulgate, 51 dans la numérotation hébraïque) | ✔ | idem | | 04 |
| 24 | Prière à saint Michel (Léon XIII) | ✔ | Paroissien du XXᵉ siècle (après 1886) | forme courte | |
| 25 | *Anima Christi* | ✔ | Paroissien | — | 02 |
| 26 | *Suscipe* (saint Ignace) | ✔ | Traduction ancienne | — | **02** |
| 27 | *Veni Creator* | ✔ | Paroissien | — | 05 (1ᵉʳ janvier, Pentecôte) |
| 28 | *Te Deum* | ✔ | Paroissien | — | 05 (31 décembre) |
| 29–31 | *Magnificat*, *Benedictus*, *Nunc dimittis* | ✔ Vulgate | Paroissien ou Bible du domaine public | — | 04 |
| 32 | *O mi Iesu* (prière de Fatima) | ✔ | ≈ usage public | option du rosaire | |
| 33 | Prière devant le crucifix (*En ego*) | ✔ | Paroissien | — | 03 |
| 34 | Communion spirituelle (saint Alphonse de Liguori) | — | Traduction du XIXᵉ siècle | — | 03 |
| 35 | Offrande de la journée (Apostolat de la prière) | — | Ancien | — | |
| 36 | Prière à saint Joseph (*Ad te, beate Ioseph*, Léon XIII) | ✔ | Paroissien | — | 05 (octobre) |
| 37–40 | Litanies du Sacré-Cœur, de Lorette, de saint Joseph et des saints | ✔ | Paroissien (forme 1962) et traduction maison des ajouts récents (courts) | actuelle / 1962 | |
| + | **Prières du matin et du soir**, composées à partir du paroissien de référence, en forme courte (2 min) et longue (10 min) | | | | 02 (examen du soir), 08 |

**À ajouter en V1** : litanies du Saint Nom et du Précieux Sang ; hymnes eucharistiques et mariales (*Adoro te*, *Ave verum*, *O salutaris*, *Tantum ergo*, *Pange lingua*, *Stabat Mater*, *Ave maris stella*, *O sacrum convivium*) ; *Veni Sancte Spiritus* ; Salut du Saint-Sacrement.

#### 2.1.5 Modèle de données d'une prière (besoins transmis aux agents 06 et 11)

- Un **identifiant stable** (`prayer:ave`), qui permet la **transclusion** : le rosaire *référence* l'Ave au lieu d'en copier le texte.
- Des **variantes** par langue (`fr-trad`, `fr-1966`, `fr-2017`, `fr-2021`, `la`), choisies selon le profil de l'utilisateur.
- Un **balisage sémantique** : rubrique (rouge), verset et répons (℣/℟), alternance entre le célébrant et le fidèle, répétition (`×3`), latin et français alignés paragraphe par paragraphe, incises facultatives (la *clausula* du rosaire, par exemple).
- Des **métadonnées** : source (ouvrage, page, URL du scan, *imprimatur*), licence, catégorie, renvoi à l'*Enchiridion* (§2.3.7) et crochets calendaires (`season: paschal`).
- Une **ancre stable par paragraphe**, pour les signets et annotations de l'agent 09.

### 2.2 Dévotions interactives

#### 2.2.1 Rosaire guidé

**Répartition des mystères selon le jour** (✔ RVM §38) :

| Jour | Coutume traditionnelle (15 mystères) | *Rosarium Virginis Mariae* (2002, 20 mystères) |
|---|---|---|
| Lundi | Joyeux | Joyeux |
| Mardi | Douloureux | Douloureux |
| Mercredi | Glorieux | Glorieux |
| Jeudi | Joyeux | **Lumineux** |
| Vendredi | Douloureux | Douloureux |
| Samedi | Glorieux | **Joyeux** |
| Dimanche | Glorieux | Glorieux |

- Exceptions saisonnières (≈ coutume) : les dimanches de l'Avent et du temps de Noël prennent les mystères joyeux, ceux du Carême les mystères douloureux. Le calcul relève de l'agent 05 ; la règle s'exprime dans la définition de la dévotion.
- Les cinq **mystères lumineux** sont : le Baptême, Cana, l'annonce du Royaume, la Transfiguration et l'Eucharistie (✔ RVM §21).
- La **clausula**, une incise ajoutée après « Jésus » pour rappeler le mystère, est une « pratique louable » (✔ RVM §33). On la propose en option, avec des incises rédigées par l'équipe ou tirées du domaine public.
- On peut aussi afficher le « fruit du mystère » (≈ usage français traditionnel) et une image pieuse pour chaque mystère (agent 09).

**Structure** (données) : sur le crucifix, le Credo ; sur le gros grain, le Pater ; puis trois Ave (foi, espérance, charité) et le Gloria. Viennent ensuite cinq dizaines (annonce, Pater, dix Ave, Gloria et, en option, *O mi Iesu*). La conclusion comprend le *Salve Regina*, l'oraison et, en option, les litanies de Lorette et la prière à saint Joseph en octobre.

**Dizainier tactile** (proposition UX, à arbitrer avec les agents 08 et 10) :
- Mode plein écran « prière » : l'écran reste allumé, le mode Ne pas déranger est facultatif (cf. brief) et l'affichage suit le thème sépia ou sombre.
- **On touche ou on glisse n'importe où** pour avancer, car la cible doit être grande et l'usage possible les yeux mi-clos. Un appui long recule d'un grain. Le chapelet dessiné avance visuellement, grain par grain.
- **Retour haptique** : `LocalHapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)` à chaque Ave (✔ l'API existe dans Compose). Pour un rendu plus fin, `VibrationEffect.Composition` avec `PRIMITIVE_TICK` pour l'Ave et `PRIMITIVE_CLICK` (ou un double tick) pour le Pater et la fin de dizaine. On teste d'abord `Vibrator.arePrimitivesSupported()` et l'on prévoit un repli (✔ documentation Android). Le rendu varie selon les appareils (≈).
- Trois modes d'affichage : **texte complet**, **compteur seul** (pour les habitués) et, en V1, **audio guidé** (synthèse vocale de l'agent 06 : l'application dit la première moitié et laisse une pause pour la réponse).
- Les touches de volume peuvent servir à avancer (≈ faisable tant que l'activité a le focus).
- Accessibilité : TalkBack annonce « Ave 7 sur 10, 3ᵉ mystère douloureux ».
- La position est enregistrée, pour reprendre après une interruption.
- **Pas de gamification** (séries de jours, statistiques), conformément au principe « sans distraction ». Un journal facultatif reste une décision à prendre (§4).

**Indulgence** (✔ *Enchiridion* 1999, concession 17 §1, 1°) : le rosaire récité en église, en famille ou en communauté. L'application affiche une note brève, rédigée par l'équipe, avec un renvoi.

**Chapelets** réutilisant le même moteur (V1) : chapelet des Sept Douleurs, chapelet de saint Michel, chapelet de la Miséricorde divine (⚖ les prières viennent du *Petit Journal* de sainte Faustine, traduction © Marian Fathers ✔ : vérifier avant de les inclure).

#### 2.2.2 Chemin de croix

- **Forme traditionnelle à 14 stations**, avec une 15ᵉ (la Résurrection) en option. Texte de saint Alphonse de Liguori (mort en 1787) dans une traduction française du XIXᵉ siècle : par exemple les *Prières pour les stations* (Québec, 1851, Canadiana `oocihm.49534`, ✔ notice) ou l'« exercice du chemin de la croix » des paroissiens Barbou et Mame (✔ mentionné dans les titres).
- **Variante biblique** (Jean-Paul II, 1991, ≈) : la liste des stations est un fait non protégé. Les textes viennent du module Bible (agent 04).
- Entre les stations, des strophes du *Stabat Mater* (latin du domaine public, ancienne traduction française).
- Une **image pieuse par station** (agent 09 : gravures du domaine public).
- Présentation en « pas à pas » : une station par page, ce qui s'accorde naturellement avec le tourne-page de l'agent 07.
- Indulgence : concession 13, 2° (✔). Les conditions (stations érigées, ou lecture et méditation en cas d'empêchement) sont résumées par l'équipe (≈ à relire).

#### 2.2.3 Neuvaines

- Ce sont des données pures : neuf jours, un texte par jour et des prières communes. Une **règle de départ** est reliée au calendrier de l'agent 05 : la fête moins neuf jours, ou une fête mobile.
- **Rappel quotidien facultatif.** La progression (« jour 4 sur 9 ») est enregistrée localement.
- Sélection V1 (sources du domaine public, ≈) : Immaculée Conception (29 novembre–7 décembre), Noël (16–24 décembre), **Saint-Esprit** (de l'Ascension à la Pentecôte, la neuvaine originelle), Saint Joseph (10–18 mars), Notre-Dame de Lourdes (2–10 février), neuvaine de la grâce de saint François Xavier (4–12 mars), Sacré-Cœur, sainte Thérèse de l'Enfant-Jésus (morte en 1897, domaine public). La neuvaine à la Miséricorde divine pose un problème de droits ⚖.
- Indulgence : concession 22, 1° (✔).

#### 2.2.4 Rappels de l'Angélus

- **Désactivés par défaut** ; on les active depuis la page de l'Angélus, au moment de l'usage, et non dans l'accueil de l'application.
- **Horaires réglables** : 6 h, 12 h et 18 h (horaire classique ✔), ou **7 h, 12 h et 19 h** (sonnerie courante des cloches en France ✔).
- **Programmation** (à arbitrer avec l'agent 10) : sur les appareils récents, `SCHEDULE_EXACT_ALARM` est **refusée par défaut** depuis Android 14 et `USE_EXACT_ALARM` est réservée aux applications d'alarme et de calendrier (✔). On utilise donc une alarme inexacte autorisée en Doze (`setAndAllowWhileIdle`, ≈) ou `setWindow` avec une fenêtre de 5 à 10 minutes (≈ retard possible en Doze). La précision exacte reste une option, si l'utilisateur accorde lui-même la permission. Il faut reprogrammer après un redémarrage et après un changement d'heure ou de fuseau (≈). Sur Android 13 et plus, la permission `POST_NOTIFICATIONS` est requise (≈ connu).
- **Discrétion** : un canal de notification de faible importance. On choisit entre une **cloche douce** (échantillon CC0 ou enregistrement maison), une **vibration au rythme de l'Angélus (3‑3‑3‑9)** ou une notification silencieuse. Le mode Ne pas déranger est respecté. La notification ouvre directement la prière.
- **Au temps pascal, le Regina Caeli remplace l'Angélus** (✔). La borne de fin dépend du rite : la Pentecôte selon Vatican News, le dimanche de la Trinité selon Aleteia, un désaccord entre sources (✔) ; dans la forme extraordinaire, le samedi après la Pentecôte (≈). C'est l'agent 05 qui fournit la fenêtre.
- On propose aussi en option le silence des cloches pendant le Triduum (≈ coutume, à vérifier).
- Indulgence : concession 17 §2, 2° (✔).

#### 2.2.5 Examen de conscience et préparation à la confession

- **Déroulé** : prière à l'Esprit Saint, puis **examen** (liste à cocher selon les dix commandements, les commandements de l'Église, les péchés capitaux et les devoirs d'état), puis acte de contrition, rappel du déroulement de la confession (formules usuelles), et enfin, après la confession, pénitence et action de grâces.
- **Textes** : les examens des anciens paroissiens sont du domaine public, mais leur ton doit être relu (risque de scrupule) ; une version *adulte* et une version *jeune* sont prévues (≈ V1). On ajoute la mention que l'outil **ne remplace pas** le confesseur.
- **Vie privée, une exigence forte** (transmise à l'agent 10) :
  - par défaut, **rien n'est écrit sur le disque** : l'état ne vit qu'en mémoire et s'efface quand on quitte l'écran ;
  - l'option « garder jusqu'à ma confession » chiffre les données avec une clé du Keystore Android, prévoit un effacement automatique au bout de N jours et un bouton « J'ai reçu l'absolution → effacer » ;
  - les écrans sont exclus des sauvegardes (`dataExtractionRules`) et protégés par `FLAG_SECURE` (ni capture ni vignette dans les applications récentes). Aucune notification n'affiche de contenu. Aucune télémétrie.
  - ≈ Vérifier l'état de `androidx.security:security-crypto`, qu'on dit déprécié. On pourra utiliser le Keystore directement, ou Tink.
- **Lien avec l'agent 02** : l'**examen ignatien quotidien** (en cinq points) et l'examen *de confession* sont deux entrées distinctes, qui partagent les composants « liste » et « minuteur ». Les prières du soir proposent l'examen ignatien.
- Indulgence : concession 9 (examen et acte de contrition, ✔).

#### 2.2.6 Heure sainte, visites au Saint-Sacrement et consécrations

- **Heure sainte** : un **minuteur segmenté** (par exemple 60 minutes : adoration, lecture de Gethsémani tirée du module Bible, réparation, actes, silence), une cloche discrète entre les segments et le mode méditation du brief. Les textes sont du domaine public (tradition de sainte Marguerite-Marie, prières anciennes).
- **Visites au Saint-Sacrement et à la Sainte Vierge** de saint Alphonse de Liguori (domaine public, ≈ traduction du XIXᵉ siècle à localiser) : 31 visites, une par jour du mois. C'est un très bon rapport valeur/effort pour la V1. Adoration eucharistique : concession 7 (✔).
- **Consécrations** :
  - **Saint Louis-Marie Grignion de Montfort** : le *Traité de la vraie dévotion* est du domaine public (l'auteur est mort en 1716 ; une édition de Montréal, vers 1896, est sur Canadiana ✔). Le **plan de préparation de 33 jours** décrit dans le *Traité* (≈ §§ 227 et suivants) devient un « plan de jours » avec rappels. Il faut **éviter** les adaptations modernes, comme les livres à succès récents, qui sont protégées.
  - **Sacré-Cœur** : acte de Léon XIII (1899) et formule de Pie XI (≈). Consécration du genre humain au Christ Roi : concession 2 (✔).
  - **Consécration des familles** : concession 1 (✔). ⚖ La formule d'intronisation du P. Mateo Crawley-Boevey (mort en 1960, ≈) est protégée en France jusqu'en 2030 (≈). Il faut prendre une formule plus ancienne.
  - Le **vœu de Louis XIII** (1638) peut être rappelé le 15 août (domaine public).

### 2.3 Modules d'extension

Échelle d'effort (≈, pour un développeur, en comptant la relecture) : **S** moins d'une semaine · **M** une à trois semaines · **L** un à deux mois · **XL** plus de deux mois. Valeur : ★ à ★★★.

#### 2.3.1 Bréviaire et office divin

| Option | Source | Licence | Effort | Valeur | Constat |
|---|---|---|---|---|---|
| **Divinum Officium** (1570, 1910, *Divino Afflatu*, 1955, **1960**, monastique, cistercien, dominicain) | `github.com/DivinumOfficium/divinum-officium`, actif (dernier push le 2026-10-10 ✔) | **Code et données sous licence MIT** ✔ ; le site déclare les textes liturgiques du domaine public « à sa connaissance » ✔ | **XL** pour porter le moteur de rubriques (Perl) ; **L** pour précalculer les jours dans le pipeline (exécuter DO dans la CI, avec l'image Docker officielle ✔, puis normaliser et produire un paquet par année) | ★★ (★★★ pour le public traditionnel) | Il existe des dossiers `Francais` et **`Latin-gabc`** (✔). ⚖ **La provenance du français est mixte** (✔ page Credits) : les propres appartiennent à l'**Abbaye Sainte-Madeleine du Barroux**, qui les décrit comme leur travail et leur propriété ; le reste vient du **bréviaire Labergerie (1956)**, sans doute encore protégé (≈), d'introibo.fr, de la Bible Crampon (Nouveau Testament, passé au vouvoiement) et de Fillion. → **Le latin est utilisable ; le français demande une autorisation (Barroux et autres) ou une traduction refaite à partir de sources du domaine public.** |
| **Liturgie des Heures de l'AELF** | `api.aelf.org` (API publique, documentation Nelmio ✔) | ⚖ **Aucune licence publiée** (✔ ni la page d'accueil ni les mentions légales n'en indiquent). Les textes restent la propriété de l'AELF, et les clients tiers affichent une mention d'autorisation (✔) | **M** sur le plan technique (JSON), mais **en ligne uniquement**, ce qui contredit le principe « hors ligne d'abord » ; il faudrait une autorisation pour la mise en cache et l'embarquement | ★★★ pour la forme ordinaire (la majorité des fidèles) | En attendant, on peut proposer un **lien profond** vers l'application ou le site AELF. Une demande d'autorisation formelle est à préparer (agent 12). |
| **Complies et Petit office de la Sainte Vierge** | Paroissiens anciens (vêpres et complies du dimanche), *Officium parvum BMV* | Domaine public | **M** | ★★ | **Fidèle à l'esprit du paroissien** et peu coûteux. C'est un bon pas intermédiaire avant un bréviaire complet (≈ sources à localiser, la recherche n'ayant pas abouti). |

#### 2.3.2 Catéchismes

| Catéchisme | Sources | Statut | Effort | Valeur |
|---|---|---|---|---|
| **Concile de Trente** (*Catechismus Romanus*, 1566) | Le latin est du domaine public. Traductions françaises numérisées (✔ archive.org) : **abbé Gagey**, texte latin en regard (Dijon, 1868 ; réimpression Beauchesne 1905, `catchismeducon01cath` et `catchismeducon02cath`) ; abbé Doney (Liège, 1847) ; Dassance (1865) ; Hallez (1862) ; Varet de Fontigny (1694, `lecatechismeduc00churgoog`) | Domaine public probable (traducteurs du XIXᵉ siècle) · ≈ vérifier la date de décès de Gagey | **M** (OCR et relecture d'environ 1 000 pages) | ★★ |
| **Saint Pie X** (*Catechismo della dottrina cristiana*, 1912) | L'italien de 1912 est du domaine public (Pie X est mort en 1914 ✔). Les versions françaises en ligne sont **de provenance inconnue** : les dépôts archive.org de 2024 (`catechisme-de-rome-de-saint-pie-x-1912`) n'indiquent ni éditeur ni traducteur ✔, et une traduction récente diffusée par une fraternité parle d'une traduction « coûteuse », donc protégée (✔) | ⚖ Risque. → Option : **traduire nous-mêmes depuis l'italien de 1912** (433 questions et réponses ✔) | **M** | ★★ (format questions-réponses idéal) |
| **CEC** (1992/1997) et **Abrégé** (2005) | vatican.va | ⚖ **© Libreria Editrice Vaticana** (édition typique latine ✔), édition française Mame/Plon (✔ notice). Aux États-Unis, une autorisation écrite est exigée pour les éditions (✔ USCCB). Le seuil de 1 000 mots évoqué ne vaut que pour les États-Unis (✔). | **S** si l'on se limite à des **renvois** (§ et liens) | ★★ |

→ **On n'embarque pas le CEC.** On fournit un index de renvois (« voir CEC § 2558 ») avec un lien vers la page française de vatican.va, ouvert sur action explicite de l'utilisateur.

#### 2.3.3 Vies des saints

| Œuvre | Sources | Statut | Effort | Valeur |
|---|---|---|---|---|
| **Petits Bollandistes** (Mgr Paul Guérin, 17 volumes) | archive.org : **deux jeux complets ou presque** (✔) : 1880 (`lespetitsbolland0001guer` … `0015guer`) et **1888** (`lespetitsbolland01guuoft`, `03gu` … `16gu`), plus le *Supplément* (Piolin, 1888) | Domaine public (Guérin, mort vers 1908, ≈) | **XL** pour l'intégrale (≈ 10 000 pages, environ 40 Mo de texte brut, ≈) ; **L** pour des notices brèves pour 365 jours | ★★★ |
| **Butler** (anglais, 1756–1759) et **Godescard** (adaptation française, 1763–1788 ; augmentée par Doney en 1844) | Notices BnF (✔). Scans à localiser | Domaine public. Attention : le Butler révisé par Thurston et Attwater (1926–1938, 1956) est protégé (≈) | **L** | ★★ |
| **Légende dorée** (Jacques de Voragine), traduction **Wyzewa** (Perrin, 1902) | **Project Gutenberg n° 69917**, texte propre en HTML et EPUB (✔) | Domaine public aux États-Unis (✔) ; en France, Wyzewa est mort en 1917 (≈), donc domaine public | **S** (gain rapide) | ★★ ; une note éditoriale s'impose, car le texte est légendaire et la traduction jugée « peu fidèle » ✔ |
| **L'Année liturgique** (Dom Guéranger, 1841–1866, poursuivie après 1875) | Traduction anglaise de 1871 numérisée (PTSEM, « No Copyright – United States » ✔) ; scans français à localiser | Domaine public (≈) | **XL** | ★★★ (commentaire au fil des jours, en parfaite synergie avec le calendrier) |

→ L'approche recommandée est un **« saint du jour » progressif** : une notice courte reliée à l'identifiant de fête de l'agent 05, tirée au choix des Petits Bollandistes ou de Godescard, puis enrichie volume après volume.

#### 2.3.4 Pères de l'Église

- **BKV, Bibliothek der Kirchenväter** (Université de Fribourg) : elle contient des traductions françaises du XIXᵉ siècle, par exemple les *Confessions* traduites par Moreau (1864) et la *Cité de Dieu* par Saisset (1869), en lien avec l'**Abbaye Saint-Benoît de Port-Valais** (✔). Selon ses conditions, les textes sont **libres de droits à la connaissance de l'opérateur, qui demande seulement une mention de la source** (✔). Aucune API ni aucun export TEI n'est signalé (✔).
- En anglais : Schaff (*ANF/NPNF*, domaine public, ≈ ccel.org).
- **À exclure** : *Sources chrétiennes* (Cerf, protégé, ≈), ainsi que les lectures patristiques de la Liturgie des Heures (© AELF).
- **Effort M ; valeur ★★.** On propose une **anthologie « lecture patristique du dimanche »** reliée au calendrier, plutôt que des œuvres complètes.

#### 2.3.5 Chant grégorien

| Brique | Constat | Licence |
|---|---|---|
| **GregoBase** (gregobase.selapa.net) | Partitions GABC collaboratives. Selon l'opérateur, les mélodies sont libres, sauf compositions récentes, mais pas les livres (✔). Aucune API ni export en masse n'est documenté (✔). | **Licence par fichier**, champ `gabc-copyright` (✔ documentation Gregorio) ; une partie est en CC0 (✔ chant21) |
| **DO `Latin-gabc`** | GABC des pièces de l'office dans le dépôt DO (✔) | MIT pour le dépôt (✔) · ≈ origine des transcriptions |
| **Liber Usualis 1961** | Scans répandus (CCWatershed, MusicaSacra, IMSLP) (✔) | ⚖ **Statut incertain** : domaine public aux États-Unis faute de renouvellement selon IMSLP (✔) ; Solesmes revendique depuis 1957 les signes rythmiques (✔) ; dans l'UE, une protection de 70 ans après publication est possible selon un forum (✔ opinion) |
| **Exsurge** | Rendu GABC → SVG en JavaScript, **MIT** ✔, mais **non maintenu** (dernier push le 2017-04-07 ✔) | MIT |
| **jgabc** (bbloomf) | Éditeur et rendu GABC en JavaScript, **actif** (push le 2026-10-08 ✔) | **Unlicense** ✔ |
| **Gregorio / GregorioTeX** | Moteur de référence (LuaTeX), lourd | GPLv3 ✔ |

→ **Stratégie** : le rendu se fait **dans le pipeline** (agent 11, Node en CI), au format SVG, et l'application affiche des SVG statiques, sans WebView. **Limite** : une ligne de chant ne se recompose pas à la volée. Il faut donc précalculer pour deux ou trois largeurs (portrait et paysage) ou découper par incise. **Effort L ; valeur ★★**, partagée avec le Missel de l'agent 03 pour le propre et l'ordinaire chantés.

#### 2.3.6 Martyrologe romain

- Le latin traditionnel (Baronius, 1584, puis éditions jusqu'en 1956) est du domaine public pour l'essentiel (≈). DO contient des dossiers `Martyrologium` en latin et en français (✔ arborescence), mais la **source du français n'est pas documentée** ⚖.
- Les traductions françaises anciennes sont du domaine public : Solier (Lyon, 1620, ✔ BnF) et **Willot (Lille, 1658), en PDF sur ULiège DONum** (✔).
- Le *Martyrologium Romanum* actuel (2001/2004) et ses traductions sont protégés (≈ © LEV).
- **Effort M ; valeur ★★** : une lecture quotidienne, traditionnellement à Prime, qui complète le saint du jour.

#### 2.3.7 *Enchiridion indulgentiarum*

- La 4ᵉ édition (1999) est en latin sur vatican.va (✔). La traduction anglaise de l'USCCB (2006) est protégée (✔). **Aucune édition française n'a été trouvée** (✔ recherche négative).
- **Concessions vérifiées** (lecture automatisée du texte latin, ✔, à recontrôler) : 1 consécration des familles · 2 dédicace au Christ Roi · 3 réparation · 7 adoration eucharistique · 8 *Anima Christi* · 9 examen et contrition (*De profundis*) · 13 chemin de croix · 17 §1 rosaire · 17 §2 *Magnificat*, Angélus, *Regina caeli*, *Memorare*, *Sub tuum* · 19 saint Joseph · 22 neuvaines et litanies · 26 *Te Deum* et *Veni Creator* · 28 actes des vertus théologales.
- → **Pas de texte intégral.** On stocke des **métadonnées** (numéro de concession, type plénière ou partielle, conditions **reformulées par l'équipe**) affichées sous chaque prière. **Effort S ; valeur ★★.**
- ★ **Point critique** : les paroissiens anciens indiquent des indulgences en **jours** (« 300 jours »), un système aboli en 1967 (*Indulgentiarum doctrina*, ≈). **Ces mentions doivent être retirées ou annotées** lors de l'import (agent 11).

### 2.4 Architecture d'extension : le « paquet de contenu »

À coordonner avec l'agent 10 (architecture) et l'agent 11 (pipeline). Ce qui suit exprime les besoins du point de vue des prières et des dévotions.

#### 2.4.1 Structure proposée

```
saint-joseph-neuvaine.ppk          (zip signé)
├── manifest.json     id, type, version, schemaVersion, minEngine, langues,
│                     licences[], attributions[], sources[] (scan, page, imprimatur),
│                     sha256 de chaque fichier, renommages d'ancres
├── manifest.sig      signature Ed25519 du manifeste (clé publique embarquée dans l'APK)
├── texts/            documents au format commun (défini par 06/11) : rubriques, ℣/℟,
│                     alignement la/fr, variantes, ancres stables, transclusion `ref:prayer:ave`
├── devotions/        définitions déclaratives (voir 2.4.2)
├── calendar/         liaisons date ou fête (id de l'agent 05) → contenu ; règles de départ des neuvaines
├── chant/            .gabc (source) + .svg précalculés
├── images/           images pieuses .webp (agent 09)
└── audio/            (facultatif, plus tard)
```

**Types de paquets** : `prayers` (recueil), `devotion` (interactif), `book` (lecture linéaire : Légende dorée, Trente), `proper` (indexé sur le calendrier : saint du jour, martyrologe), `office` (heures précalculées), `chant`, `images`.

**Cycle de vie** :
1. **MVP** : le contenu de base est livré sous forme de paquets embarqués dans `assets/`. Le format est ainsi éprouvé dès le départ.
2. **V1** : import d'un fichier (Storage Access Framework), puis catalogue facultatif (un JSON statique sur GitHub Releases ou Pages). Les téléchargements sont **déclenchés par l'utilisateur**, sans compte ni pistage.
3. L'application vérifie la signature et les sommes de contrôle, protège contre le *zip-slip*, impose des limites de taille et valide le schéma, puis décompresse dans le stockage interne. Elle enregistre le paquet dans Room et construit l'index plein texte à l'installation.

#### 2.4.2 Le moteur de dévotions déclaratif

Les primitives sont implémentées une fois pour toutes en natif (Compose). Le paquet ne fait que les **composer** :

| Primitive | Rôle | Vue native |
|---|---|---|
| `sequence` | enchaînement d'éléments | — |
| `prayer(ref, variant=profile)` | transclusion d'une prière | lecteur |
| `repeat(n, body, bead=…)` | répétition avec compteur | **dizainier** |
| `announce(ref)` | annonce d'un mystère ou d'une station (texte, image, fruit) | carte |
| `choose(rule)` | choix selon le jour, le temps liturgique (05) ou l'utilisateur | — |
| `step(title, image, text)` | étape (station, jour de neuvaine) | pas à pas, tourne-page (07) |
| `timer(min, bell)` | segment chronométré | minuteur (Heure sainte, méditation) |
| `checklist(items, private=true)` | examen | liste privée (§2.2.5) |
| `plan(days, startRule, reminder)` | neuvaine, consécration de 33 jours, retraite (02) | plan de jours et rappels |
| `versicle/response` | alternance ℣/℟ (litanies) | lecteur |

Exemple de définition (**données**, pas du code applicatif) :

```json
{ "id": "devotion:rosaire", "engine": ">=1",
  "choose": { "rule": "weekday", "table": "setting:rosary.distribution",
              "overrides": [{ "season": "advent", "weekday": "sun", "use": "joyeux" }] },
  "sequence": [
    { "prayer": "credo-apotres", "bead": "crucifix" },
    { "prayer": "pater", "bead": "large" },
    { "repeat": 3, "prayer": "ave", "bead": "small" },
    { "prayer": "gloria-patri" },
    { "repeat": 5, "body": [
        { "announce": "$mystery[i]" }, { "prayer": "pater", "bead": "large" },
        { "repeat": 10, "prayer": "ave", "bead": "small", "clausula": "$mystery[i].clausula" },
        { "prayer": "gloria-patri" }, { "prayer": "o-mi-iesu", "optional": "setting:rosary.fatima" } ] },
    { "prayer": "salve-regina" } ] }
```

#### 2.4.3 Limites assumées

1. **Pas de code exécutable dans les paquets.** C'est une question de sécurité et de politique Google Play : celle-ci interdit de charger du code exécutable (dex, JAR, .so) depuis une autre source que Play ; le JavaScript exécuté dans une WebView fait exception (✔). Même si l'APK est diffusé hors de Play, on garde cette règle.
2. **Un nouveau type d'interaction exige une mise à jour de l'application** : rosaire audio avec reconnaissance vocale, montre connectée, carte des stations, etc.
3. **Dégradation gracieuse** : toute dévotion possède un **rendu statique de repli** (lecture simple). Un paquet récent reste lisible dans une version plus ancienne de l'application (`minEngine` donne un avertissement, sans bloquer).
4. **Règles calendaires** : un langage d'expressions limité (décalages relatifs à un identifiant de fête, jours de la semaine, temps liturgiques), sans script.
5. **Les polices restent celles de l'application.** Les glyphes du chant sont précalculés en SVG.
6. **Les annotations survivent aux mises à jour** grâce aux ancres stables et à la table de renommage du manifeste (agents 06 et 09).
7. **Taille** : les gros corpus (Bollandistes, office) sont découpés par volume, par mois ou par année, et téléchargés de préférence en Wi-Fi.
8. **Paquets tiers non signés** : désactivés par défaut, accessibles seulement via un « mode développeur » (décision en §4).

#### 2.4.4 Feuille de route priorisée des extensions

| Priorité | Paquet | Effort | Valeur | Dépend de |
|---|---|---|---|---|
| **MVP** | Prières usuelles, quatre litanies, matin et soir, antiennes mariales | M | ★★★ | 06, 11 |
| **MVP** | Rosaire guidé (moteur et dizainier) | M | ★★★ | 05, 08, 10 |
| **MVP** | Chemin de croix traditionnel | S | ★★★ | 07, 09 |
| **MVP** | Examen et confession (texte du domaine public, privé) | S–M | ★★★ | 02, 10 |
| **MVP** | Rappels de l'Angélus et du Regina Caeli | S | ★★ | 05, 10 |
| **V1** | Neuvaines (8 à 10), Heure sainte, Visites au Saint-Sacrement, consécration de Montfort (33 jours), Sacré-Cœur | M | ★★★ | 05 |
| **V1** | Chapelets (Sept Douleurs, saint Michel ; Miséricorde ⚖) et litanies supplémentaires, hymnes eucharistiques | S | ★★ | — |
| **V1** | Format de paquet ouvert : import de fichier et catalogue | M | ★★★ | 10, 11 |
| **V1** | Métadonnées d'indulgences | S | ★★ | — |
| **V1** | *Légende dorée* (Gutenberg) | S | ★★ | 11 |
| **V1** | Complies et Petit office de la Sainte Vierge | M | ★★ | 05 |
| **V1** | Catéchisme du concile de Trente | M | ★★ | 11 |
| **V2** | Saint du jour (notices et martyrologe) | L | ★★★ | 05, 11 |
| **V2** | Anthologie patristique (BKV) | M | ★★ | 05 |
| **V2** | Office divin en latin, DO 1960, précalculé | L | ★★ | 11, 03 |
| **Plus tard** | Chant grégorien (rendu SVG dans le pipeline) | L | ★★ | 03, 11 |
| **Plus tard** | Liturgie des Heures de l'AELF (si l'autorisation est obtenue) | M | ★★★ | 12 |
| **Plus tard** | Catéchisme de saint Pie X (traduction maison) | M | ★★ | — |
| **Plus tard** | *L'Année liturgique* de Guéranger, *Petits Bollandistes* en intégrale | XL | ★★★ | 11 |
| **Plus tard** | Bibliothèque spirituelle (*Introduction à la vie dévote*, Liguori, Scupoli…) | M chacun | ★★ | 04 |
| **Plus tard** | Rosaire audio guidé, Wear OS (dizainier au poignet) | L | ★★ | 06 |

---

## 3. Recommandations

### MVP
1. **Corpus d'environ 40 textes** (§2.1.4), en latin et en français traditionnel du domaine public, tiré d'**un paroissien de référence (Mame 1890)** et collationné avec un second. Les métadonnées de source accompagnent chaque texte.
2. **Réglage « profil de textes »** : *Traditionnel* (par défaut tant que l'AELF n'a pas répondu), *Liturgie actuelle* (activé seulement si l'autorisation est obtenue) et *Personnalisé* (choix prière par prière). Un réglage d'affichage séparé propose latin, français, ou les deux en regard. Ce profil suit par défaut le rite choisi dans le Missel (agent 03).
3. **Litanies à double état** : forme actuelle (ajouts de 2018, 2020 et 2021) et forme 1962.
4. **Moteur de dévotions déclaratif**, avec pour premières instances le rosaire, le chemin de croix et l'examen. Le contenu de base utilise **déjà le format de paquet**, embarqué dans l'APK.
5. **Rosaire** : deux répartitions des mystères (RVM ou traditionnelle), clausula et prière de Fatima en option, dizainier tactile avec retour haptique et repli, reprise de la position, aucune statistique.
6. **Angélus** : désactivé par défaut, alarme inexacte autorisée en Doze, horaires 6‑12‑18 ou 7‑12‑19, cloche, vibration 3‑3‑3‑9 ou silence, bascule automatique vers le Regina Caeli.
7. **Examen et confession** : éphémère par défaut, `FLAG_SECURE`, aucune sauvegarde, avertissement pastoral, lien vers l'examen ignatien (agent 02).
8. **Purge des mentions d'indulgences en jours** à l'import ; renvois sobres à l'*Enchiridion* de 1999.
9. ⚖ **Envoyer sans attendre la demande d'autorisation à l'AELF**, préparée par l'agent 12. Elle doit couvrir les prières usuelles (Notre Père de 2013, Credo, *Je confesse* et *Gloire à Dieu* du Missel de 2021) et, si possible, l'API de la Liturgie des Heures avec mise en cache hors ligne.

### V1
10. Neuvaines (8 à 10) reliées au calendrier, Heure sainte (minuteur segmenté), Visites de Liguori, consécration de Montfort sur 33 jours et consécrations au Sacré-Cœur.
11. Chapelets supplémentaires, litanies du Saint Nom et du Précieux Sang, hymnes eucharistiques et Salut du Saint-Sacrement.
12. **Ouverture du format de paquet** : import de fichier, catalogue statique facultatif, signature Ed25519, licences affichées dans l'application.
13. Gains rapides de contenu : *Légende dorée* (Gutenberg n° 69917), Complies et Petit office de la Sainte Vierge, Catéchisme de Trente (Gagey).
14. Métadonnées d'indulgences sous chaque prière.

### Plus tard
15. Saint du jour, d'abord en notices brèves, avec le martyrologe ; anthologie patristique (BKV, avec attribution).
16. Office divin en latin (DO 1960) **précalculé dans la CI** plutôt que porté ; le français n'est envisagé qu'après autorisation du Barroux, ou à partir de sources du domaine public.
17. Chant grégorien rendu en SVG dans le pipeline (jgabc, sous Unlicense, plutôt qu'Exsurge, qui n'est plus maintenu).
18. Liturgie des Heures de l'AELF selon la réponse obtenue ; catéchisme de saint Pie X retraduit ; *L'Année liturgique* ; *Petits Bollandistes* en intégrale ; rosaire audio ; Wear OS.

---

## 4. Décisions à soumettre à l'utilisateur

| # | Question | Options | Recommandation |
|---|---|---|---|
| D1 | **Quel profil de textes français par défaut ?** | (a) traditionnel au vouvoiement ; (b) liturgie actuelle de 2017 et 2021 ; (c) selon le rite choisi | (c), avec (a) en repli tant que l'AELF n'a pas répondu |
| D2 | **Demander l'autorisation de l'AELF ?** | oui pour les prières usuelles seules / oui pour les prières usuelles et la Liturgie des Heures / non (domaine public uniquement) | Oui pour les deux, demande à envoyer tôt |
| D3 | **Répartition des mystères du rosaire par défaut** | RVM (20 mystères) / traditionnelle (15) / selon le rite | Selon le rite, modifiable |
| D4 | **Horaires et signal de l'Angélus** | 6‑12‑18 ou 7‑12‑19 ; cloche, vibration ou silence ; alarme exacte (permission) ou approximative | 7‑12‑19, vibration, approximative ; tout reste désactivé par défaut |
| D5 | **Examen de conscience : autoriser une note persistante ?** | jamais / chiffrée avec effacement automatique / libre | Éphémère par défaut, option chiffrée avec effacement automatique |
| D6 | **Dévotions récentes sous droits** (Miséricorde divine, intronisation du Sacré-Cœur, formules de Pie XII) | demander une autorisation / omettre / formules anciennes seulement | Formules anciennes au MVP ; demandes ciblées en V1 |
| D7 | **Quelle piste pour l'office divin ?** | DO en latin (public traditionnel) / AELF (forme ordinaire) / Complies et Petit office seulement | Complies et Petit office en V1, puis selon D2 |
| D8 | **Paquets tiers** | interdits / mode développeur / ouverts avec avertissement | Mode développeur uniquement |
| D9 | **L'application est-elle non commerciale ?** Cela conditionne l'usage des scans Gallica | gratuite et non commerciale / payante ou avec dons | Préciser ; en attendant, privilégier archive.org |
| D10 | **Statistiques et journal de prière** | aucun / journal privé facultatif / séries de jours (« streaks ») | Aucun au MVP ; journal facultatif plus tard, jamais de « streaks » |
| D11 | **Catéchisme de saint Pie X** | retraduire depuis l'italien de 1912 / attendre une source claire / renoncer | Retraduire, plus tard |

---

## 5. Risques et inconnues

| Risque ou inconnue | Gravité | Atténuation |
|---|---|---|
| ⚖ **Droits AELF** sur le Notre Père de 2013, le Credo et le *Je confesse* de 2021, et la Liturgie des Heures ; aucune licence publiée n'a été trouvée | **Élevée** | Formes du domaine public par défaut ; demande d'autorisation (agent 12) ; liens profonds |
| ⚖ **Provenance du français de Divinum Officium** (Barroux, Labergerie 1956, introibo.fr) : la licence MIT du dépôt ne couvre pas forcément ces droits | Élevée pour l'office en français | Latin seul ; autorisation du Barroux ; traductions du domaine public (Fillion, Crampon) |
| **Indulgences obsolètes** (en jours) et formulations datées dans les paroissiens | Moyenne (crédibilité) | Règle de purge dans le pipeline ; relecture doctrinale humaine |
| **Qualité de l'OCR** (typographie du XIXᵉ siècle, ligatures, latin) et coût de relecture | Moyenne à élevée | Commencer par de petits corpus ; collation avec deux éditions ; relecteurs bénévoles |
| **Alarmes retardées** (Doze, surcouches constructeurs agressives sur la batterie) | Moyenne | Fenêtre tolérée ; option exacte ; aide « désactiver l'optimisation de batterie » ; tests sur appareil physique |
| **Retour haptique hétérogène** selon les appareils | Faible | `arePrimitivesSupported` et repli sur `HapticFeedbackType` |
| **Fuite de données de l'examen** (sauvegardes, capture, vignettes, notifications) | Élevée en cas d'erreur | Éphémère par défaut, `FLAG_SECURE`, `dataExtractionRules`, revue de sécurité |
| **Explosion du périmètre** (moteur de bréviaire, intégrale des Bollandistes) | Élevée | Précalcul plutôt que portage ; corpus découpés ; feuille de route stricte |
| **Évolution du schéma de paquet** et ancres d'annotations | Moyenne | `schemaVersion`, `minEngine`, table de renommage, rendu statique de repli |
| ⚖ **Gallica** : la BnF revendique la propriété de ses reproductions et exige une licence pour l'usage commercial (contesté ✔) | Moyenne | Préférer archive.org et Canadiana ; mention « Source gallica.bnf.fr / BnF » si Gallica est utilisé |
| **Liber Usualis et Solesmes** (signes rythmiques revendiqués) | Moyenne pour le chant | Fichiers GABC en CC0 ou du domaine public uniquement ; vérifier fichier par fichier |
| **Inconnues non levées** : inventaire Gallica (SRU refusée en 403) ; date de décès de l'abbé Gagey ; texte exact du Notre Père d'avant 1966 (« succomber ») ; bornes du Regina Caeli dans la forme extraordinaire ; silence de l'Angélus pendant le Triduum ; statut des formules de 1966 ; édition française du XIXᵉ siècle des *Visites* de Liguori et du Petit office | — | À lever par l'agent 11 lors de l'import et par l'agent 12. **Le quota de recherche web partagé était épuisé** en fin d'étude : ces points n'ont pas pu être creusés davantage. |

---

## 6. Sources

### Variantes françaises et liturgie
- Notre Père, nouvelle traduction (diocèse de Paris) : https://dioceseparis.fr/notre-pere-nouvelle-traduction.html
- Vatican News, entrée en vigueur le 3 décembre 2017 : https://www.vaticannews.va/fr/eglise/news/2017-11/france---une-nouvelle-version-du--notre-pere--a-partir-du-3-dece.html
- Suisse romande, 1ᵉʳ avril 2018 : https://www.eglisecatholique-ge.ch/wp-content/uploads/CP-MARS-2018.pdf
- Historique de 1966 et de 2013 : https://ecumenism.net/?p=6879 · https://fr.aleteia.org/2013/10/15/une-nouvelle-traduction-du-notre-pere-ne-nous-laisse-pas-entrer-en-tentation/
- Missel de 2021 : https://dioceseparis.fr/nouvelle-traduction-francaise-du.html · https://fr.aleteia.org/2021/11/12/credo-du-de-meme-nature-que-le-pere-au-consubstantiel-au-pere-un-chemin-complexe/
- Vouvoiement de l'Ave (source faible) : https://codeberg.org/jozo/prayer-book/pulls/14
- Litanies de Lorette, 2020 : https://www.thecatholictelegraph.com/pope-francis-adds-three-titles-to-catholic-litany-of-the-blessed-virgin-mary/67252 · https://dioceseparis.fr/litanies-de-lorette.html
- Litanie de saint Joseph, 2021 : https://www.vaticannews.va/fr/vatican/news/2021-05/culte-divin-saint-joseph-eveques-sacrements-litanie-priere-mai.html · https://press.vatican.va/content/salastampa/en/bollettino/pubblico/2021/05/01/210501c.pdf
- *Rosarium Virginis Mariae* (§§ 21, 33, 38) : https://www.vatican.va/content/john-paul-ii/fr/apost_letters/2002/documents/hf_jp-ii_apl_20021016_rosarium-virginis-mariae.html
- Regina Caeli et Angélus : https://www.vaticannews.va/fr/pape-francois/regina-coeli/2021-04/regina-coeli-du-18-avril-2021.html · https://fr.aleteia.org/2023/05/06/quest-ce-que-le-regina-caeli/
- *Enchiridion indulgentiarum* (1999, latin) : https://www.vatican.va/roman_curia/tribunals/apost_penit/documents/rc_trib_appen_doc_20020826_enchiridion-indulgentiarum_lt.html

### Paroissiens et sources du domaine public (archive.org, Canadiana)
- Mame 1890 : https://archive.org/details/paroissienromain00tour
- Zech 1901 : https://archive.org/details/paroissienromain00cath
- Mame 1872 : https://archive.org/details/paroissienromain00cath_0
- Mame 1869 : https://archive.org/details/paroissienromai00churgoog
- Barbou vers 1895 : https://archive.org/details/leparoissienroma00unse
- Lecoffre 1860 : https://archive.org/details/bub_gb_PcH1G0A8jZUC
- Belin 1830 : https://archive.org/details/paroissienromain00unse · *Paroissien complet* 1842 : https://archive.org/details/paroissiencomple00unse
- Canadiana, *Paroissien romain* 1893 : https://archive.org/details/cihm_95158 · *Paroissien de poche* 1914 : https://archive.org/details/cihm_80314 · *Nouveau paroissien romain* 1913 : https://archive.org/details/cihm_990278
- Liguori, stations (1851) : https://www.canadiana.ca/view/oocihm.49534
- Montfort, *Traité* (Montréal, vers 1896) : https://www.canadiana.ca/view/oocihm.06362
- Conditions Gallica : https://linuxfr.org/users/pamputt/journaux/conditions-d-utilisation-des-contenus-de-gallica · https://actualitte.com/article/57529/archives/la-bnf-en-flagrant-delit-de-copyfraud-une-deputee-alerte-aurelie-filippetti

### AELF, CEC et droits
- API de l'AELF : https://api.aelf.org/ · mentions légales : https://www.aelf.org/page/mentions-legales
- Clients tiers de l'AELF : https://codeberg.org/erwanlpfr/harbour-aelf · https://apt.izzysoft.de/fdroid/index/apk/co.epitre.aelf_lectures?repo=main
- Droits sur le CEC : https://www.catholic.com/qa/is-the-catechism-in-the-public-domain · https://www.catholicworldreport.com/2022/01/20/copyright-and-the-catechism-of-the-catholic-church-make-for-some-legal-surprises/ · https://bibliotheques.paris.fr/doc/SYRACUSE/232701
- Faustine (© Marian Fathers) : https://lifeteen.com/wp-content/uploads/2025/03/DivineMercyChaplet.pdf

### Modules
- Divinum Officium : https://divinumofficium.com/www/horas/Help/technical.html · https://divinumofficium.com/www/horas/Help/credits.html · https://github.com/DivinumOfficium/divinum-officium · https://divinumofficium.com/www/horas/Help/download.html
- Catéchisme de Trente (Gagey) : https://archive.org/details/catchismeducon01cath · https://archive.org/details/catchismeducon02cath · Varet 1694 : https://archive.org/details/lecatechismeduc00churgoog · notice Gagey 1868 : https://bibliotecavirtualmadrid.comunidad.madrid/bvmadrid_publicacion/en/consulta/registro.do?id=207234
- Catéchisme de saint Pie X : https://archive.org/details/catechisme-de-rome-de-saint-pie-x-1912 · https://en.wikipedia.org/wiki/Catechism_of_Saint_Pius_X · https://laportelatine.org/wp-content/uploads/2020/07/cate_doctrine_chretienne.pdf
- *Petits Bollandistes* : https://archive.org/details/lespetitsbolland01guuoft · https://archive.org/details/lespetitsbolland0001guer · https://en.wikipedia.org/wiki/Les_Petits_Bollandistes
- Godescard : https://catalogue.bnf.fr/ark:/12148/cb301805268 · https://mameetfils.univ-tours.fr/personnes/godescard-jean-frana%c2%a7ois
- *Légende dorée* (Wyzewa) : https://www.gutenberg.org/ebooks/69917
- Guéranger, traduction anglaise : https://commons.ptsem.edu/id/liturgicalyear07gura · https://en.wikipedia.org/wiki/The_Liturgical_Year
- Pères de l'Église (BKV) : https://bkv.unifr.ch/fr/works/9 · https://bkv.unifr.ch/about/copyrights · https://www.brepols.net/products/IS-9782503614731-1
- Martyrologe (Willot, 1658) : https://donum.uliege.be/handle/2268.1/13634 · Solier (1620) : https://catalogue.bnf.fr/ark:/12148/cb368140493
- Chant : https://gregobase.selapa.net/?p=18 · https://github.com/frmatthew/exsurge · https://github.com/bbloomf/jgabc · https://mirror.ircam.fr/pub/CTAN/support/gregoriotex/doc/Gabc.tex · https://github.com/bacor/chant21 · https://cn.imslp.org/wiki/Liber_Usualis_(Anonymous) · https://ccwatershed.org/2013/03/19/1961-solesmes-liber-usualis-online-free-pdf
- Indulgences (édition anglaise USCCB) : https://www.ewtn.com/catholicism/library/decree-fourth-edition-of-the-enchiridion-indulgentiarum-29067

### Android et politique de diffusion
- Alarmes exactes sous Android 14 : https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- `HapticFeedbackType` : https://developer.android.com/reference/kotlin/androidx/compose/ui/hapticfeedback/HapticFeedbackType
- `VibrationEffect.Composition` : https://developer.android.com/reference/android/os/VibrationEffect.Composition · https://developer.android.com/develop/ui/views/haptics/custom-haptic-effects
- Google Play, politique *Device and Network Abuse* : https://support.google.com/googleplay/android-developer/answer/9888379

### Référence de comparaison (agent 13)
- *Prayer Book* (Codeberg ; prières en TOML et Markdown, Capacitor) : https://codeberg.org/jozo/prayer-book

---

### Points d'interface (récapitulatif)

| Agent | Sujet partagé |
|---|---|
| 02 Ignace | *Suscipe*, *Anima Christi* ; examen ignatien et examen de confession ; composants « liste », « minuteur » et « plan de jours » (retraite) |
| 03 Missel | Source unique pour les prières de l'ordinaire (*Confiteor*, *Gloria*, Credo) ; profil de textes relié au rite ; provenance du français de DO ; GABC du propre |
| 04 Bible | Cantiques et psaumes (*Magnificat*, *De profundis*, *Miserere*) ; numérotation Vulgate ou hébraïque ; textes du chemin de croix biblique ; Heure sainte |
| 05 Calendrier | Mystères selon le jour et le temps ; fenêtre du Regina Caeli selon le rite ; antienne mariale du temps ; départs des neuvaines ; identifiants de fêtes pour le saint du jour ; suggestions du *Te Deum* et du *Veni Creator* |
| 06 Moteur de lecture | Balisage ℣/℟, rubriques, latin et français alignés, transclusion, variantes ; synthèse vocale du rosaire audio ; ancres stables |
| 07 Tourne-page | Chemin de croix et neuvaines en pages ; le rosaire en mode plein écran *hors pagination* |
| 08 UX | Réglages du profil de textes, de l'Angélus et du rosaire ; mode prière ; discrétion |
| 09 Signets et images | Images des mystères et des stations ; signets sur les prières ; ancres |
| 10 Architecture | Format et signature des paquets, Room, AlarmManager, notifications, retour haptique, `FLAG_SECURE`, exclusion des sauvegardes |
| 11 Pipeline | OCR et collation des paroissiens ; purge des indulgences ; précalcul de DO ; rendu GABC en SVG ; signature |
| 12 Juridique | AELF, Barroux, Gallica, LEV/CEC, Faustine, Crawley-Boevey, Pie XII, Liber Usualis |
| 13 Benchmark | *Prayer Book* (Codeberg), application AELF, Divinum Officium |
