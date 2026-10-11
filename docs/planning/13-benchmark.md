# 13 — Benchmark concurrentiel et inspiration

> Agent 13. Rapport de planification, rédigé le 11 octobre 2026. Aucun code n'a été écrit.
>
> **Méthode.** Les fiches Google Play (vitrine France) ont été lues directement dans un navigateur le 11 octobre 2026 : notes, nombre d'avis, téléchargements, date de mise à jour, section « Sécurité des données » et une quinzaine d'avis par application, triés par pertinence. Les fiches App Store, les sites officiels et F-Droid ont été consultés avec un outil de lecture web. Deux sous-recherches ont été menées en parallèle, l'une sur les liseuses et l'autre sur les applications de prière et de méditation.
>
> **Légende.** **[V]** signifie vérifié sur une source primaire (fiche de store, site officiel, documentation) ; la source est citée. **[A]** signale un avis d'utilisateur, donc une anecdote vérifiée comme *avis* mais pas comme *fait*. **[R]** désigne une donnée rapportée par un tiers (presse, tracker comme AppGoblin) et non confirmée à la source. **[S]** marque une supposition ou une inférence.
>
> **Limites.** Le quota de recherches web de la session, partagé par les quatorze agents, a été épuisé en cours de route. Certains points n'ont donc pas pu être recoupés ; ils sont marqués [S] et repris en section 5. Les notes des stores changent chaque jour : les chiffres ci-dessous sont un instantané du 11/10/2026.

## 1. Résumé

- **Aucun concurrent n'occupe la place de Paroissien** [S, d'après une cinquantaine de fiches lues]. Les applications francophones se répartissent en deux camps :
  - la forme ordinaire seule : AELF, Prions en Église, Magnificat ;
  - le rite de 1962 seul : ORDO (TradDevs).

  Aucune ne réunit les deux rites avec la lecture spirituelle, et aucune n'imite le livre papier.
- **Trois contenus sont orphelins** [V] : aucune application ne propose les lettres de Clairval, aucune ne propose l'*Imitation* en français, et aucune retraite ignatienne en texte n'existe en français ni hors ligne. La seule *Imitation* moderne repérée est en audio, sous abonnement et accompagnée d'une IA (Sanctus, iOS).
- **Les meilleures notes vont aux applications sobres, hors ligne et sans compte** [V] :

  | Application | Note | Modèle |
  |---|---|---|
  | Universalis | 5,0★ | Achat unique de 11,99 € |
  | Missale Romanum | 5,0★ | Achat unique de 11,99 € |
  | ORDO | 4,9★ | Gratuit, tout embarqué |
  | AELF | 4,7★ (12 k avis) | Gratuit, open source, sans traceur |

  Ce modèle de confiance est validé par le marché.
- **Les douleurs récurrentes dans les avis** [A] :
  - publicités sonores qui se déclenchent *pendant la messe* ;
  - comptes et abonnements qui bloquent l'accès ;
  - mises à jour forcées ;
  - refontes qui retirent une fonction quotidienne (le côte-à-côte latin chez iMass) ;
  - dépendance au réseau ;
  - navigation labyrinthique ;
  - *streaks* culpabilisants.
- **La métaphore du ruban est validée.** Missale Romanum présente déjà ses onglets comme « l'équivalent des rubans d'un missel » qui mémorisent leur position [V]. La demande de « reprendre exactement où j'en étais » revient dans de nombreux avis (Introibo, ORDO, Examen).
- **Le marché « tradi tout-en-un hors ligne » se peuple très vite en 2026** [V] : Iter Fidei (sorti le 1er octobre 2026), Pew Pal, Sanctifica, Introibo, Missal Page. Le différenciateur doit donc être **l'expérience livre, le français et les contenus uniques**, pas le seul catalogue.
- **Côté liseuses**, la page qui tourne plaît quand elle est rapide, suit le doigt, peut s'interrompre et se désactiver. Elle déplaît quand elle est lente, saccadée ou imposée. La double page en paysage est un standard des liseuses Android (Moon+ Reader) [V]. Détails en 2.5.
- **Un piège technique est avéré** [A, V] : en août 2026, une mise à jour d'iBreviary a mal géré l'affichage *edge-to-edge* ; ses boutons sont restés inaccessibles pendant des semaines sur Android 14 et 15.
- **Recommandation centrale.** Le MVP doit être :
  - hors ligne strict, sans compte, sans publicité ni SDK tiers ;
  - accessible au jour courant en un geste ;
  - équipé de rubans qui gardent leur position et du latin et du français en regard ;
  - doté d'une page qui tourne sobre et désactivable ;
  - porteur d'au moins un contenu introuvable ailleurs (*Imitation* en français, et Clairval si l'abbaye donne son accord).

---

## 2. Constats détaillés

### 2.1 Le paysage en un coup d'œil

- **Le Play Store est saturé de clones.** Une recherche « missel 1962 latin français » ou « imissal » renvoie surtout des dizaines d'applications « Catholic Missal 2026 », « Catholic Daily Missal 2026 », etc., publiées par Syncsoft, Freedom Works Apps, BibleChants, Techsoft… Elles contiennent souvent des publicités et sont parfois vendues jusqu'à 19,99 € en version « sans pub » [V] ([recherche Play](https://play.google.com/store/search?q=missel%201962%20latin%20fran%C3%A7ais&c=apps&hl=fr)). La qualité perçue de la catégorie est donc faible, ce qui rend l'attention au soin d'autant plus visible.
- **Le marché francophone est dominé par trois acteurs.** AELF (gratuit, bénévole, open source), Prions en Église (Bayard, abonnement) et Magnificat (abonnement ou abonnés papier). Aucun des trois ne propose la forme extraordinaire [V].
- **Le traditionnel connaît une vague d'applications indépendantes en 2026**, surtout sur iOS et en anglais : Introibo, Missal Page, Sancta Missa 1962 [V, App Store]. Sur Android sont apparus Iter Fidei (première version publiée le 1er octobre 2026) et Pew Pal (mise à jour du 4 octobre 2026) [V]. En français, une seule application tradi sérieuse existe : **ORDO (TradDevs)** [V].
- **Aucune application ne propose les lettres de Clairval.** La recherche « clairval » sur Play ne renvoie que des homonymes (une clinique, un jeu…) [V]. Aucune **Imitation de Jésus-Christ en français** n'est disponible non plus : seules quelques applications en anglais, souvent financées par la publicité, existent [V]. Côté **Exercices spirituels**, on ne trouve rien en français sur Play ; il n'existe que des applications espagnoles ou italiennes, de type texte brut ou programme IVE [V].

### 2.2 Missels et offices

#### AELF – Bible et lectures du jour (Epitre & co)
- **[V]** 4,7★ (12,4 k avis), plus d'un million de téléchargements, mise à jour du 12/06/2026. L'application est **gratuite, sans publicité ni traceur**, open source (licence MIT) et également publiée sur F-Droid. Elle déclare « Aucune donnée collectée » ([Play](https://play.google.com/store/apps/details?id=co.epitre.aelf_lectures&hl=fr), [F-Droid](https://f-droid.org/packages/co.epitre.aelf_lectures)).
- **Contenu [V].** Bible liturgique complète avec recherche, messe et lectures du jour, liturgie des heures (des laudes aux complies). Forme ordinaire uniquement, en français.
- **Hors ligne [V].** Les textes sont synchronisés automatiquement jusqu'à un mois à l'avance. F-Droid classe toutefois l'application comme dépendante d'un service réseau (api.aelf.org).
- **UX [V].** Plein écran sobre, sans mise en veille, mode nuit, taille du texte réglable, compatible TalkBack. Le slogan de la fiche résume l'ambition : « elle sait se faire discrète ».
- **Avis [A].** Les éloges portent sur la simplicité et le fonctionnement en mode avion, avec un témoignage : « on n'entend plus les "c'est à quelle page ?" » en récollection. Les demandes portent sur le calendrier liturgique annuel, les heures conseillées pour chaque office, le choix des communs pour les mémoires, une notification facultative à heure choisie et la correction d'un bug d'affichage vide de la Bible.
- **Ce qui manque.** Le rite de 1962, le latin, les dévotions, la lecture spirituelle et toute dimension « livre ».

#### Prions en Église (Bayard) — fiche « Prions : Lecture de la Bible »
- **[V]** 4,8★ (20,9 k avis), plus de 500 k téléchargements, mise à jour du 07/09/2026. Modèle freemium : essai de deux semaines puis **2,99 €/mois ou 34,99 €/an** ([Play](https://play.google.com/store/apps/details?id=com.groupebayard.prionseneglise.iphone&hl=fr)). L'application collecte des informations personnelles, des informations financières et « 3 autres » types de données [V].
- **Contenu [V].** Messe et Évangile du jour, liturgie des heures (gratuite), Bible audio, podcasts, plus de 500 musiques, méditations, « prière personnalisée », routines. Tout est en français et dans la forme ordinaire.
- **Avis [A].** On reproche à l'application d'avoir du mal à aller directement aux textes du jour et de rendre confuse la frontière entre gratuit et payant, au point que des abonnés papier se trouvent bloqués. Les avis signalent aussi des **déconnexions répétées** qui obligent à se réabonner chaque mois et des **mises à jour forcées qui bloquent l'application pendant la messe**. Bayard répond que « certaines mises à jour sont obligatoires ». D'autres utilisateurs demandent une police plus grande et un audio pour la liturgie des heures.
- **Leçon.** Un contenu riche ne compense pas la friction du compte et de l'abonnement, ni le **blocage au moment critique**, c'est-à-dire pendant la messe.

#### Magnificat (édition française)
- **[V]** 4,3★ (531 avis), plus de 100 k téléchargements, mise à jour du 15/08/2026. Le premier mois est gratuit. Ensuite, on achète chaque numéro à l'unité ou on s'abonne au numérique ; l'application est gratuite pour les abonnés papier, qui doivent saisir leurs identifiants ([Play](https://play.google.com/store/apps/details?id=com.magnificat.magnificat.fra&hl=fr)).
- **Contenu [V].** Prière du matin, messe, méditation, saint du jour, prière du soir et complies, articles mensuels et musique. Les textes viennent de l'AELF.
- **Avis [A].** Des abonnés n'accèdent pas au contenu qu'ils ont payé : mois suivant non téléchargeable, abonnement non reconnu, remboursement refusé. D'autres déplorent le manque de choix pour les collectes des mémoires facultatives et pour les préfaces.
- **Leçon.** Le modèle « mensuel papier transposé » fragilise l'accès hors ligne et la confiance.

#### Laudate (Aycka Soft)
- **[V]** 4,5★ (31,4 k avis), plus d'un million de téléchargements, **gratuit**, déclare n'avoir « aucune donnée collectée », mise à jour du 13/09/2026 ([Play](https://play.google.com/store/apps/details?id=com.aycka.apps.MassReadings&hl=en)).
- **Contenu [V].** C'est l'application la plus « fourre-tout » : lectures, ordinaire, liturgie des heures, messe latine, rosaires et chapelets, chemin de croix, catéchisme, confession, Douay-Rheims hors ligne, documents de Vatican II, droit canon, signets et prières personnelles, disponible en 16 langues dont le français.
- **Avis [A].** Les utilisateurs saluent la gratuité, le hors-ligne et l'exhaustivité, mais trouvent l'interface « datée », « klunky » et peu intuitive. Ils signalent aussi des couleurs à faible contraste, une barre de menu qui empiète sur le texte de certains appareils et des **prières personnelles perdues** avec le téléphone, faute d'export ou de synchronisation.
- **Leçon.** L'exhaustivité attire, mais la navigation devient labyrinthique. Paroissien doit regrouper les contenus **sans** reproduire ce bazar : une table des matières de livre, pas un menu de 40 entrées. Il faut aussi prévoir l'export local des données de l'utilisateur.

#### iBreviary (Paolo Padrini / Custodie de Terre Sainte)
- **[V]** 4,2★ (20,9 k avis), plus d'un million de téléchargements, gratuit, mise à jour du 25/08/2026. Bréviaire, missel et lectionnaire en 10 langues, dont le français et le latin, avec le **« Vetus Ordo » latin**. Jusqu'à 14 jours stockables hors ligne, couleur de fond, police et taille réglables ([Play](https://play.google.com/store/apps/details?id=com.netguru.ibreviary&hl=en)).
- **Avis [A], août 2026.** Après une mise à jour, une **barre « iBreviary » recouvrait les boutons du haut** (Prier, Paramètres) sur de nombreux appareils Android 14 et 15, ce qui a rendu l'application inutilisable pendant plusieurs semaines. Le correctif est arrivé début septembre. Un utilisateur signale aussi que l'application **exige une connexion pour s'ouvrir** même quand les jours sont déjà téléchargés.
- **Leçon technique** (à transmettre à l'agent 10) : la gestion *edge-to-edge* et des *insets* sur les Android récents est un piège réel. Le hors-ligne doit être total, démarrage compris.

#### Universalis (Universalis Publishing)
- **[V]** **5,0★** (4,78 k avis), plus de 50 k téléchargements, **achat unique de 11,99 €**, plus une application « Catholic Calendar » d'essai gratuit de 30 jours (4,5★, 3,84 k avis). Le contenu de chaque jour est disponible « pour toujours ». L'application est « complète en elle-même, sans connexion Internet ni téléchargement ». Seul l'audio est vendu en option ([Play](https://play.google.com/store/apps/details?id=com.universalis.android.universalis&hl=en)).
- **Contenu [V].** Messe du jour avec ordinaire, liturgie des heures complète, calendriers locaux (Royaume-Uni, États-Unis, Australie…), pages « À propos du jour ». Anglais, latin possible [A].
- **Avis [A].** « Excellent value as a once-off purchase », « No need for Internet », « solid and bug free », « freeing you to concentrate on the prayers ».
- **Leçon.** C'est **la preuve qu'un achat unique et un hors-ligne intégral produisent la meilleure satisfaction de la catégorie.**

#### iMissal (Cantcha)
- **[V]** Sur iOS, l'application coûte 4,99 $ avec un achat intégré « Saint A Day » à 1,99 $. Elle est notée 4,4★ (2 k notes), en anglais uniquement, couvre le calendrier 1990-2050 et fonctionne hors ligne pour le texte. Elle déclare « Data Not Collected » ([App Store](https://apps.apple.com/us/app/imissal-catholic/id307312434)).
- **Android [S].** Aucune fiche iMissal n'apparaît dans la recherche Play du 11/10/2026, dominée par les clones. D'anciennes sources mentionnaient une version Android ; elle a peut-être été retirée.
- **Avis [A].** Les reproches portent sur des erreurs de contenu (lectures manquantes, Gloria incohérent) et sur des rites de la Semaine sainte incomplets.
- **Leçon.** **La fiabilité liturgique** (rubriques, cas limites comme un dimanche tombant le 24 décembre) **est le premier critère des usagers avertis.**

#### Missale Romanum (Rafael Cereceda)
- **[V]** **5,0★** (1,91 k avis), plus de 10 k téléchargements, **achat unique de 11,99 €**, « Aucune donnée collectée », mise à jour du 23/08/2026 ([Play](https://play.google.com/store/apps/details?id=rcr.missale.romanum&hl=en)). Missel romain (forme ordinaire) en latin et en espagnol, avec de l'anglais, de l'italien et de l'allemand partiels, affichable sur une ou deux colonnes. Il comprend un calendrier perpétuel.
- **Point remarquable [V].** On y accède aux parties de la messe par des onglets, qui *« viennent à être l'équivalent des rubans d'un missel, en mémorisant la position précédente de chacun »*. Les rubans peuvent ainsi « préparer » la messe à l'avance. **C'est la validation directe du concept de signets-rubans de Paroissien.**
- **Avis [A].** On reproche des allers-retours entre lectures, ordinaire et préfaces, ainsi qu'un plein écran forcé qui masque l'heure et la batterie.

#### ORDO : calendrier liturgique (TradDevs) — le concurrent francophone direct
- **[V]** **4,9★** (261 avis), plus de 10 k téléchargements, gratuit, mise à jour du 10/10/2026, code fermé ([Play](https://play.google.com/store/apps/details?id=com.traddevs.ordo&hl=fr)).
- **Contenu [V].** Calendrier perpétuel 1962 jusqu'en 2037, avec commémoraisons, propres de France, de Bretagne, de Paris et de Suisse, jeûne et abstinence, notes sur les fêtes empêchées. On y trouve le propre du jour, la vie des saints, l'ordinaire de la messe (lue, chantée ou solennelle) qui s'adapte au jour (Asperges, Vidi aquam…), les partitions du Kyriale et un chemin de croix les vendredis de Carême. **Tout est embarqué, sans téléchargement.**
- **Avis [A].** L'application est jugée « esthétiquement très aboutie » et « excellent compagnon du voyageur tradi ». Les demandes portent sur l'accès direct aux partitions du propre, le texte latin des épîtres et évangiles, les indications d'inclinaison, les calendriers belges et un saut plus court vers une section de la messe (par exemple le Confiteor à la communion). Un utilisateur a cherché le code source, qui n'est pas ouvert.
- **Ce qui manque.** La forme ordinaire, la Bible, l'Imitation, Clairval, la lecture spirituelle et l'expérience « livre ».
- **Leçon.** Le public tradi francophone existe, il est exigeant sur les rubriques et il **récompense le soin** (4,9★).

#### iMass (Anchoright Productions, lié à la FSSP)
- **[V]** 3,7★ (300 avis), plus de 10 k téléchargements. Messes en direct de la FSSP, carte des messes tridentines, missel, bréviaire et Rituale de 1962 ([Play](https://play.google.com/store/apps/details?id=fssp.livemass.iMass&hl=en)).
- **Avis [A], mars 2026.** La version 7 a supprimé l'**affichage latin/anglais côte à côte** ainsi que les rites dominicain et mozarabe, ce qui a provoqué une vague de 1 étoile. Le développeur a rétabli le côte-à-côte dans la version 7.0.1.
- **Leçon.** **Le texte en regard est non négociable** pour ce public. Une refonte qui retire une fonction d'usage quotidien est punie immédiatement.

#### Divine Office (St Anselm Technologies), Officium (P. Branicio), Pew Pal, Iter Fidei
- **Divine Office [V].** Bréviaire de 1962 (rubriques de 1960) et propres de la messe, textes issus de divinumofficium.com. Huit heures, latin, anglais ou les deux **avec défilement synchronisé**, cinq ans d'office embarqués (jusqu'en 2030), sélecteur de date. L'application est récente, avec plus de 100 téléchargements ([Play](https://play.google.com/store/apps/details?id=net.breviary.traditional&hl=en)).
- **Pew Pal [V].** Forme ordinaire avec 10 prières eucharistiques, missel de 1962 au jour, liturgie des heures, rosaire, et la mention **« no subscription and no account »** pour la partie mariale. Indicateurs de posture (debout, assis, à genoux), « No data collected » ([Play](https://play.google.com/store/apps/details?id=com.pewpal.app&hl=en)).
- **Iter Fidei [V].** Se présente comme « Une application pour toute la vie catholique traditionnelle » : missel de 1962 avec texte en regard, bréviaire, Petit Office, Bible, neuvaines avec rappels, rosaire, saints, recherche de messes, le tout « fully available offline ». L'application propose des achats intégrés et collecte des informations personnelles. Première version publiée le 1er octobre 2026 ([Play](https://play.google.com/store/apps/details?id=com.iterfidei.mobileapp&hl=en)).
- **Leçon.** **Le positionnement « tout-en-un traditionnel hors ligne » est en train de se peupler** (Iter Fidei, Sanctifica, Pew Pal, Introibo). Le différenciateur de Paroissien ne peut donc pas être seulement « tout-en-un tradi ». Il doit tenir à **l'expérience livre**, au **français**, aux **deux formes** et aux **contenus uniques** (Clairval, Imitation, Exercices).

#### Sanctifica (Ave Maria Technologies)
- **[V]** 5,0★ (1,57 k avis), plus de 10 k téléchargements, freemium. Le contenu est traditionnel : missel 1962, office 1962, Douay-Rheims et Vulgate, l'*Année liturgique* de Dom Guéranger, Butler, rosaire avec Mgr Schneider, recherche de messes tridentines. L'application mise aussi sur des **« HABITS – progress tracker »**, des routines, des rappels et des notifications de début de neuvaine. Elle collecte l'activité de l'application et des identifiants ([Play](https://play.google.com/store/apps/details?id=com.sanctifica&hl=en)).
- **Avis [A].** Un utilisateur a connu une **boucle de connexion** en juillet 2026 qui l'empêchait d'entrer dans l'application ; le développeur l'attribue aux cookies bloqués. D'autres jugent les textes et l'audio des saints « bizarres », probablement générés par IA ; le développeur promet des voix professionnelles.
- **Leçon.** **Le compte obligatoire pour une application de prière crée une panne totale** quand l'authentification échoue. **Le contenu généré par IA est perçu comme une faute de goût**, voire de piété.

#### Applications iOS traditionnelles récentes (pour l'inspiration)
- **Introibo [V].** Gratuit, « Ad-free, offline, and without subscription », 4,8★ (60 notes). Rubriques 1962, 1955 et pré-1955, avec trois disciplines de jeûne. L'application **n'a aucune permission Internet**, exporte en PDF et propose des widgets. Elle contient néanmoins un suivi de pratiques avec *streaks*. Un avis demande la **reprise à l'endroit exact** après une interruption ([App Store](https://apps.apple.com/us/app/introibo-tlm-app/id6762865358)).
- **Missal Page [V].** Calendriers 1954 ou 1962, avec **renvois aux pages des missels papier Angelus Press et Lasance**. Financement par **dons « patron »** de 1,99 à 99,99 $, « Data Not Collected » ([App Store](https://apps.apple.com/us/app/missal-page-tlm-companion/id6760142128)).
- **Sancta Missa 1962 [V].** Abonnement de 2,99 $/mois ou 24,99 $/an, traduction Lasance, posture facultative (désactivée par défaut) ([App Store](https://apps.apple.com/us/app/-/id6760446896)).
- **Romanitas Press Ordo [V].** Ordo de 1962 (missel et bréviaire) à 12,99 $ sur iOS et Android, hors ligne, sans les textes ([site](https://www.romanitaspress.com/ordo-mobile-app)).

#### Autres francophones notables
- **Liturgie des heures (Père Philoux) [V].** Plus de 100 k téléchargements. L'application est née pour fonctionner **« sans nécessiter la moindre connexion Internet »** et pour corriger des manques des applications dépendantes d'aelf.org : choix des communs, deux antiennes, psaumes invitatoires au choix ([Play](https://play.google.com/store/apps/details?id=eu.philoux.lh&hl=fr)).
- **Prières Latin-Français (Survival In Extremis) [V].** 3,7★, **contient des annonces** ([Play](https://play.google.com/store/apps/details?id=survivalinextremis.com.Orationes_Latine&hl=fr)).
- **Tradiz [V].** 3,8★, recherche de messes tridentines et chapelet, **contient des annonces** ([Play](https://play.google.com/store/apps/details?id=com.gingersquirrel.prod.messetradi&hl=fr)).

#### Les clones financés par la publicité, ou l'anti-modèle
- **« Catholic Missal 2026 » (BibleChants) [V].** 4,6★ (8,91 k avis), plus de 500 k téléchargements, **contient des annonces** ([Play](https://play.google.com/store/apps/details?id=com.BibleChants.Catholic.Daily.Readings&hl=en)). Les avis [A] décrivent des **publicités vidéo sonores qui se déclenchent pendant la messe** (« THIS APP HAS ADS THAT AUTO PLAY! »), une pub plein écran dans les dix premières secondes et **une nouvelle application à télécharger chaque année**.
- **« Catholic Missal 2026 & Prayers » (Freedom Works) [V].** Publicités et achats intégrés ([Play](https://play.google.com/store/apps/details?id=com.audio.practise&hl=en)). Avis [A] : des réflexions générées par IA laissent parfois des suggestions de l'IA dans le texte, et une refonte a fait disparaître le catéchisme.

### 2.3 Prière et méditation

#### Hallow (Hallow Inc., Chicago), le leader commercial
- **Audience et notes [V].** Note de 4,4★ sur 124 k avis et plus de 10 millions de téléchargements sur Play, vitrine FR au 11/10/2026 ([Play](https://play.google.com/store/apps/details?id=app.hallow.android&hl=en)). Sur l'App Store, 4,9/5 avec 379 k notes aux États-Unis et 4,8/5 avec environ 1 300 notes en France ([App Store US](https://apps.apple.com/us/app/hallow-prayer-meditation/id1405323394), [App Store FR](https://apps.apple.com/fr/app/hallow-pri%C3%A8re-et-m%C3%A9ditation/id1405323394)).
- **Contenu [V].** Plus de 10 000 sessions **audio** : lectio, rosaire, Examen, chant, Bible, histoires pour dormir. L'interface existe en 7 langues dont le français. On y trouve un programme **« 28-Day Spiritual Exercises »** avec le P. Gallagher : 29 sessions de 8 à 21 minutes, débloquées une par jour, et 157 k participants affichés ([collection](https://hallow.com/collections/2416/)). C'est une « micro-retraite » et non une 19ᵉ annotation.
- **Modèle [V].** Freemium : 69,99 $/an aux États-Unis, 59,99 €/an en France ([aide Hallow](https://help.hallow.com/en/articles/2880438-how-much-does-the-subscription-cost)).
- **Engagement [V].** L'application compte les *streaks* (séries de jours), fixe des objectifs hebdomadaires et propose des jeux (« Daily Trivia »). Les séries peuvent être masquées ([aide](https://help.hallow.com/en/articles/5761398-streaks-prayer-activity-faq)). Il existe aussi un onglet communauté avec chat et messages privés [R].
- **Données [V].** Sur Play, l'application **partage avec des tiers** des informations personnelles, financières et « 3 autres » types. Le label App Store mentionne des identifiants « utilisés pour vous suivre » et la publicité. L'outil AppGoblin y détecte les SDK Google Ads, Branch, Firebase et Segment [R] ([AppGoblin](https://appgoblin.info/apps/app.hallow.android)).
- **Avis [A].** Les reproches portent sur l'abonnement annuel imposé (« I cannot afford 70 odd euro in a lump payment »), le *streak* perdu « discouraging » et le message « vous êtes hors ligne » alors que l'appareil est connecté. Un utilisateur raconte que l'audio s'est lancé seul en Bluetooth pendant une réunion et en voiture. En France, on signale que presque tout est derrière le paywall et qu'**il n'y a pas de marque-page dans la Bible**.
- **Controverses [R].** Hallow a levé plus de 157 M$, notamment auprès de Peter Thiel et J.D. Vance. Son marketing par des célébrités (Liam Neeson, Russell Brand, Gwen Stefani) a été critiqué. *UnHerd* juge l'application « bossy » ([UnHerd](https://unherd.com/2025/05/you-wont-find-god-on-your-iphone/), [Wikipédia](https://en.wikipedia.org/wiki/Hallow_(app))).
- **Leçon.** Hallow illustre exactement ce que Paroissien **ne veut pas être** : un abonnement, de l'audio en flux, des *streaks*, de la communauté et des traceurs. Il prouve néanmoins que **les Exercices en programme guidé quotidien** répondent à une forte demande.

#### Pray As You Go et Prie en Chemin (Jésuites de Grande-Bretagne)
- **Pray As You Go [V].** 4,6★ sur 2,23 k avis et plus de 100 k téléchargements sur Play, « No data collected » ([Play](https://play.google.com/store/apps/details?id=pl.foxcode.prayasyougo&hl=en)). Gratuit, financé par les dons, l'application revendique 500 k usagers par an ([jesuit.org.uk](https://www.jesuit.org.uk/news/jesuit-prayer-app-pray-as-you-go-turns-20-serving-a-global-community-of-over-half-a-million)). Des avis iOS rapportent qu'une **refonte en 2022 a effacé les téléchargements** et dégradé le hors-ligne [V, avis].
- **Prie en Chemin [V].** 4,5★ sur 658 avis et plus de 100 k téléchargements sur Play ([Play](https://play.google.com/store/apps/details?id=org.prieenchemin&hl=fr)). La fiche App Store FR annonce « Gratuit. Sans inscription. » ([App Store](https://apps.apple.com/fr/app/prie-en-chemin-pri%C3%A8re-audio/id1202145096)). AppGoblin y détecte les SDK Firebase et Facebook [R].
- **Leçon.** La caution ecclésiale, la gratuité sans inscription et la régularité quotidienne sont saluées. L'audio y est central.

#### Ascension (Ascension Press)
- **[V]** 4,8★ sur 22,6 k avis et plus de 500 k téléchargements sur Play. L'application collecte des informations personnelles et financières ([Play](https://play.google.com/store/apps/details?id=com.ascension.app&hl=en)).
- **Contenu et prix [V].** Bible et Catéchisme gratuits ainsi que le podcast *Bible in a Year*. Les plans d'étude et l'audio coûtent 8,99 $/mois ou 59,99 $/an. Connexion par email, en anglais seulement ([App Store](https://apps.apple.com/us/app/ascension-catholic-bible/id1660909501)).

#### Applications ignatiennes
- **Reimagining the Examen (Loyola Press) [V].** Gratuit, sans collecte de données, propose 34 variantes de l'Examen et obtient 4,3/5. Critiques : **l'application perd sa place pendant le silence**, ce qui demande d'empêcher la mise en veille, et elle n'a pas de mode sombre ([App Store](https://apps.apple.com/us/app/reimagining-the-examen/id1065042173)).
- **3-Minute Retreat, abandonnée depuis 2019, et Jesuit Prayer, abandonnée depuis 2017 [V].** Ce sont des exemples d'applications d'Église laissées à l'abandon ([3MR](https://apps.apple.com/us/app/3-minute-retreat/id323368405), [JP](https://apps.apple.com/us/app/jesuitprayer/id540380545)).
- **Click To Pray (Réseau mondial de prière du Pape) [V].** 3,7/5. Les utilisateurs se plaignent de bugs de connexion et d'inscription et de l'absence de réglage de la taille de police ([App Store](https://apps.apple.com/us/app/click-to-pray/id934935942)).
- **Retraites longues.** Cloisters Ignatian Prayer propose 4 « semaines » en vidéo, avec inscription obligatoire et un abonnement de 60 $/an ([site](https://cloistersignatianprayer.org/)). La retraite en ligne de Creighton dure 34 semaines, en **web seulement** et gratuitement ([Creighton](https://onlineministries.creighton.edu/CollaborativeMinistry/Exer-Ignat.html)). La 19ᵉ annotation classique dure environ 8 mois avec un accompagnateur ([Boston College](https://www.bc.edu/bc-web/offices/missionministry/sites/center-for-ignatian-spirituality/prayer/19th-annotation.html)).
- **Constat [S].** Aucune application trouvée ne propose une retraite ignatienne longue, **en texte**, hors ligne et sans compte, *a fortiori* en français.

#### Hozana, YouPray, Prier dans la ville (France)
- **Hozana [V].** 4,3★ sur 1,85 k avis et plus de 100 k téléchargements sur Play. L'application propose communautés, neuvaines et retraites partagées, avec connexion via Facebook, Google ou Apple. Elle est gratuite et vit de dons ([Play](https://play.google.com/store/apps/details?id=org.hozana.app&hl=fr), [App Store](https://apps.apple.com/fr/app/hozana-christian-prayer/id1477461388)). La fiche Play déclare une collecte de position, d'informations personnelles et de « 4 autres » types de données. Avis [A] : affichage illisible façon « site web », écrans blancs, et **des notifications réclamées**, ce qui montre que les rappels ne sont pas rejetés en soi.
- **YouPray (Media Tech SAS, Rennes) [V].** 4,9★ sur 3,06 k avis et plus de 100 k téléchargements. Application **audio** de prière rythmée par la liturgie, avec achats intégrés. Elle déclare une collecte de position, d'informations personnelles et de « 5 autres » types, ainsi qu'un partage de l'activité ([Play](https://play.google.com/store/apps/details?id=com.mediatech.youpray&hl=fr)).
- **Prier dans la ville (Dominicains) [V].** 4,6★ sur Play. Elle est connue pour « Carême / Avent dans la ville » par email, avec plus de 160 k inscrits ([site](https://www.prierdanslaville.org/)).

#### « Sanctus » et « Credo » : homonymes multiples, souvent dopés à l'IA
- **« Sanctus: Bible, Rosary & Chant » (iOS) [V].** Abonnement de 69,99 $/an. Le contenu comprend une Bible audio, la liturgie des heures, 78 chants, **l'*Imitation de Jésus-Christ* en audio**, de l'audio en français et un **compagnon IA « Brother Thomas »**. La fiche annonce « No account required, no ads » ([App Store](https://apps.apple.com/app/id6792047130)). **C'est le concurrent le plus proche par le contenu**, mais avec un modèle opposé : abonnement, audio et IA.
- **D'autres « Sanctus » (juillet 2026) [R].** Ils misent sur les séries, les badges, les niveaux, des « graines » à gagner ou une IA « Lumen » ([mwm.ai](https://mwm.ai/apps/sanctus-bible-prayers/6788785102)).
- **« Credo: Catholic Prayer App » (avril 2026) [R].** Liturgie des heures, rosaire, checklist quotidienne et séries, avec un abonnement Premium ([mwm.ai](https://mwm.ai/apps/credo-catholic-prayer-app/6759874385)).
- **Tendance 2026 [S].** Prolifération d'applications « IA + gamification » sur des noms liturgiques. **La sobriété devient elle-même un positionnement distinctif.**

### 2.4 Clairval, Imitation, Exercices : des contenus orphelins

| Contenu | Offre existante constatée | Commentaire |
|---|---|---|
| Lettres de Clairval | **Aucune application** (recherche Play « clairval » du 11/10/2026) [V]. Le site propose les lettres gratuitement, un abonnement, des archives et cinq autres langues [V] ([clairval.com](https://www.clairval.com/)). | Monopole de fait pour Paroissien, **sous réserve de l'autorisation de l'abbaye** (voir agents 01 et 12). |
| Imitation de Jésus-Christ (FR) | **Aucune application française** sur Play [V]. En anglais : « Imitation of Christ Audio » (Catholic Zone), **avec publicités**, 500+ téléchargements [V] ([Play](https://play.google.com/store/apps/details?id=com.Catholic.Zone.Imitation.Christ.Book.Audio.offline.Text&hl=en)), et d'autres mono-livres sans soin. | Une Imitation française bien composée (lecture du jour, signet, livre et chapitre) serait inédite [S]. |
| Exercices spirituels | Aucune application française sur Play [V]. On trouve en espagnol « Ejercicios Espirituales – IVE », avec suivi de progression et « no data collected » [V] ([Play](https://play.google.com/store/apps/details?id=com.churomobile.ive.ejercicios&hl=en)), et « Ejercicios Espirituales » (CLICKApps), simple texte non mis à jour depuis 2022 [V]. Les retraites ignatiennes guidées existent surtout en audio et en anglais (voir 2.3). | Une retraite guidée **en texte**, au rythme des semaines ignatiennes, en français et hors ligne, n'a pas d'équivalent constaté [S]. |

### 2.5 Les liseuses, référence pour l'expérience de lecture

#### Fiches

- **Apple Books, la référence historique de la courbure.**
  - **Historique [V, presse].** Depuis 2010, Apple Books tournait la page avec une courbure réaliste qui suivait le doigt. Le pli variait selon le point de saisie, en haut, au milieu ou en bas. Si on relâchait avant le seuil, la page « retombait », avec des ombres subtiles ([The Verge](https://www.theverge.com/2022/11/21/23471306/apple-books-ios-16-page-flip-animation-sucks)).
  - **Retrait puis retour [V, presse].** iOS 16 (2022) a imposé un glissement façon « cartes », ce qui a provoqué un tollé. Quatre mois plus tard, iOS 16.4 a rétabli le choix entre Slide, Curl et None ([Gadget Hacks](https://ios.gadgethacks.com/how-to/get-page-turning-curl-animation-back-apple-books-for-iphone-and-ipad-0385329)).
  - **Options actuelles [V].** Le guide officiel liste aujourd'hui Curl, Fast Fade et Scroll. On avance par la marge droite ou par un swipe, et l'option « Both Margins Advance » fait avancer les deux marges ([Apple](https://support.apple.com/en-us/guide/iphone/iphc1af7c57/ios)).
  - **Signets [S].** L'application place un signet unique par bouton et sauvegarde la position automatiquement. Sous iOS 16.0, de nombreux signets étaient créés par erreur ([Basic Apple Guy](https://basicappleguy.com/basicappleblog/build-a-better-books)).
- **Google Play Livres.** Note de 4,6★ avec 3,25 M d'avis [V].
  - **Mise en page [V].** Le réglage « Mise en page » propose Auto, 1 page ou 2 pages ; en Auto, il dépend de l'orientation et de la taille d'écran ([aide](https://support.google.com/googleplay/answer/9755756)).
  - **Thèmes [V].** Clair, sépia et sombre, plus une « lumière de nuit ».
  - **Tourne-page [V].** On tourne avec les touches de volume, désactivables.
  - **Signets [V].** Un tap en haut à droite pose un signet. Les surlignages sont colorés, et l'export se fait vers Drive ([aide](https://support.google.com/googleplay/answer/3165868)).
  - **Avis [A].** Les avis signalent une réouverture à la mauvaise page et des signets qui renvoient à la mauvaise page.
- **Moon+ Reader.** Note de 3,6★ avec 269 k avis, plus de 10 M de téléchargements, **contient des annonces** [V] ([Play](https://play.google.com/store/apps/details?id=com.flyersoft.moonreader&hl=en)).
  - **Fonctions [V].** La fiche annonce une « Real page turning effect » dont on règle la vitesse, la couleur et la transparence, « 5 page flip animations » et un « **Dual page mode for landscape screen** ». S'y ajoutent la justification, la césure et 24 actions configurables (taps, gestes, touches).
  - **Avis [A].** Un lecteur dit avoir composé un thème « comme un livre », avec texture de page et texte légèrement brun : « every book i read feels a little bit more like an actual book ». D'autres se plaignent de publicités plein écran, de thèmes non sauvegardés sur un Fold et de pertes de position.
- **ReadEra.** Note de **4,9★ avec 1,59 M d'avis**, plus de 50 M de téléchargements, **sans publicité ni achat intégré**, hors ligne [V] ([Play](https://play.google.com/store/apps/details?id=org.readera&hl=en)).
  - **Fonctions [V].** Modes jour, nuit, sépia et console, défilement horizontal ou vertical, césure. Un tap dans le coin supérieur droit pose un signet.
  - **Ce qui manque [V].** **Aucune courbure ni double page documentée.** C'est la preuve que la sobriété et la fiabilité suffisent à une note record.
- **Librera.** Note de 4,1★, contient des annonces [V] ([Play](https://play.google.com/store/apps/details?id=com.foobnix.pdf.reader&hl=en)).
  - **Mise en page [V].** Trois modes : défilement, livre et « musicien ». Une page en portrait, deux en paysage, à choisir manuellement ou via des profils. Un mode « Couverture, deux pages » respecte la parité recto/verso ([FAQ](https://librera.mobi/faq/how-to-set-up-reading-mode)).
  - **Signets [V].** Librera distingue les signets « fixes » des signets « flottants » ancrés au texte, dont on peut modifier le texte et le style ([FAQ](https://librera.mobi/faq/edit-bookmark-page-and-floating-bookmarks)).
  - **Avis [A].** « Trop de façons de tourner les pages » ; un signet qui ne s'affiche pas à l'endroit marqué.
- **KOReader.**
  - **Interface [V].** Interface pensée pour l'e-ink, **sans animation**. Plus de 200 gestes, profils de mise en page (par exemple deux colonnes en paysage) et dictionnaires de césure ([README](https://github.com/koreader/koreader)).
  - **Signets [V].** On pose un signet en touchant le coin supérieur droit, **dessiné comme un coin de page corné** ([guide](https://koreader.rocks/user_guide/)).
- **Kindle.** L'application Android affiche 4,7★ avec 4,83 M d'avis et **contient des annonces** [V].
  - **Application [V].** Fonds blanc, sépia, vert et noir, défilement continu, et touches de volume sur Fire ([aide Amazon](https://digprjsurvey.amazon.com/csad/help/node/TABlJ4ot69emTO8jJG)).
  - **Liseuses [S, blog spécialisé].** Les liseuses proposent une « Page Turn Animation », un balayage discret, sujette à des avis partagés : certains la jugent distrayante ou s'inquiètent pour la batterie ([the-ebook-reader](https://blog.the-ebook-reader.com/2022/02/14/do-you-like-the-page-turn-animation-on-the-new-kindle-paperwhite/)).
  - **Signets [S].** Depuis février 2026, la Colorsoft propose **8 couleurs de signets** et des lecteurs demandent à pouvoir les nommer ([the-ebook-reader](https://blog.the-ebook-reader.com/2026/02/19/kindle-colorsoft-now-has-colored-bookmarks-and-green-highlights/)). Une plainte notable : le carnet de signets et de notes met des minutes à s'ouvrir sur **une Bible très annotée** ([the-ebook-reader](https://blog.the-ebook-reader.com/2025/12/09/change-to-bookmark-feature-on-kindles-after-update/)).

#### Ce qui rend une page qui tourne agréable, ou pénible

| Agréable | Pénible |
|---|---|
| La page **suit le doigt** et se plie selon le point de saisie. Relâchée avant le seuil, elle **retombe** sans tourner : l'animation est interruptible et annulable [V, The Verge]. | Une animation **imposée**, comme la page-carte d'iOS 16, qui a déclenché un tollé et un retour arrière [V]. |
| Un réalisme **mesuré** : ombre de pli, léger aperçu du verso, pas de bruit. | Un **scintillement** ou une saccade sur les appareils modestes [S, Moon+ sous Android 12]. |
| **Rapide sur un tap, proportionnelle sur un drag.** La vitesse est réglable (Moon+) [V]. | Une animation incompatible avec le mode sombre ou qui produit des artefacts sur les surlignages (Kindle) [S]. |
| **Désactivable** : courbure, glissement, fondu ou aucune animation [V, Apple, Kindle, Moon+]. Elle respecte « Supprimer les animations ». La norme WCAG 2.3.3 demande que les animations déclenchées par l'utilisateur puissent être désactivées ([W3C](https://www.w3.org/WAI/WCAG22/Understanding/animation-from-interactions.html)). | **Perdre sa position**, que ce soit à la rotation, au changement de police ou à la sortie d'un menu. C'est la plainte n° 1 contre toutes les liseuses (Play Livres, Moon+, Librera) [A]. |
| Plusieurs **moyens de tourner** : zones de tap aux marges, swipe, touches de volume. | **Trop** d'options de tourne mal expliquées (Librera) [A] ; des pubs plein écran au moment de lire (Moon+) [A]. |

#### La double page en paysage

- **Le standard [V].** Les liseuses proposent un réglage **Auto / 1 page / 2 pages**, Auto choisissant selon l'orientation et la taille (Play Livres, Librera, Moon+).
- **La parité [V, Librera].** Il faut respecter la parité recto/verso : couverture seule, puis planches avec la page paire à gauche et l'impaire à droite. Le folio va en marge extérieure, selon la convention typographique [S].
- **L'ancre [V, changelog Librera].** **L'ancre de lecture doit survivre** à la rotation : passer de 1 à 2 pages ne doit jamais faire sauter le lecteur.
- **Un point de vigilance pour la vision [S].**
  - Un téléphone en paysage n'offre qu'environ 380 dp de hauteur utile. Une double page y donne deux colonnes d'une douzaine de lignes, soit l'équivalent d'un missel de poche.
  - Les recommandations Android distinguent la largeur « Expanded » (840 dp ou plus) et la hauteur « compacte » (moins de 480 dp) ([Android](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes)).
  - Il faut donc tester la double page sur téléphone, et peut-être la conditionner à un réglage (voir la décision 9).
- **La gouttière [S].** Aucune liseuse ne la documente. Il faut prévoir une marge intérieure et une ombre de reliure discrète. Sur un écran pliable, le pli fait une gouttière naturelle.

#### Les signets

- **Le geste standard [V].** Un tap dans le **coin supérieur droit** pose un signet (Play Livres, ReadEra, KOReader, Kindle). KOReader le dessine comme un coin corné.
- **Les attentes qui émergent [V/S].** Des signets **multiples, colorés et nommables** (Kindle Colorsoft), **ancrés au texte** plutôt qu'à un numéro de page (signets flottants de Librera), et listés **à part des notes** (cas de lenteur Kindle).
- **Ce qu'aucune liseuse ne propose [S].** **Aucune liseuse ne propose de ruban** : un signet persistant, toujours visible sur la tranche, qu'on « tire » pour rouvrir le livre. C'est un espace de design libre pour Paroissien. Les images pieuses en sont le prolongement : des signets nommés, illustrés et en nombre illimité.

#### Bibliothèques de courbure repérées (pour les agents 07 et 10, sans évaluation)

- [oleksandrbalan/pagecurl](https://github.com/oleksandrbalan/pagecurl) : Jetpack Compose, Apache-2.0.
- [harism/android_page_curl](https://github.com/harism/android_page_curl) : OpenGL ES, Apache-2.0.
- [eschao/android-PageFlip](https://github.com/eschao/android-PageFlip) : OpenGL ES 2.0, double page automatique en paysage, ancien (testé jusqu'à l'API 26).

### 2.6 Ce que disent les avis : synthèse transversale

| Douleur récurrente | Exemples constatés [A] | Réponse de Paroissien |
|---|---|---|
| **Publicités**, surtout sonores pendant la messe | BibleChants, Freedom Works, Tradiz, Prières Latin-Français | Zéro publicité, zéro SDK publicitaire. |
| **Compte ou abonnement qui bloque l'accès** | Prions (déconnexions, abonnés papier bloqués), Magnificat (mois non téléchargeable), Sanctifica (boucle de connexion), Hallow (abonnement annuel imposé, environ 70 à 80 €) | Aucun compte, contenu embarqué ou en paquets locaux. |
| **Mise à jour forcée ou régression de refonte** | Prions (MAJ obligatoire à la messe), iMass v7 (côte-à-côte supprimé), iBreviary (barre qui masque les boutons), Freedom Works (catéchisme disparu) | Ne jamais forcer de mise à jour. Tests de non-régression sur les fonctions quotidiennes. Ne pas retirer de fonctionnalité sans option. |
| **Dépendance réseau** | iBreviary (connexion requise au démarrage), AELF (API), Hallow (« vous êtes hors ligne » alors que connecté) | Hors ligne strict, y compris au premier lancement après installation des paquets. |
| **Navigation labyrinthique** | Laudate (« klunky »), Missale Romanum (allers-retours), Prions (difficile d'aller aux textes du jour) | Table des matières de livre, rubans et « Aujourd'hui » en un geste. |
| **Gamification qui culpabilise** | Hallow (*streak* perdu, « discouraging ») | Pas de *streaks* ni de badges. |
| **Contenu IA ou de faible qualité** | Sanctifica (audio des saints), Freedom Works (réflexions IA) | Textes sourcés, éditions nommées, aucune génération. |
| **Lisibilité** | Laudate (contraste), Prions (taille de police), Missale Romanum (plein écran forcé) | Thèmes contrôlés, taille réglable, plein écran facultatif. |
| **Perte des données personnelles** | Laudate (prières perdues avec le téléphone) | Export et import locaux des signets, notes et images pieuses (fichier). |
| **Reprendre là où l'on s'est arrêté** | Introibo (Matines qui recommencent), ORDO (sauts dans la messe) | C'est exactement la fonction des **rubans**. |

**Ce que les utilisateurs louent [A] :** la sobriété (« elle sait se faire discrète » chez AELF), le hors-ligne en mode avion, l'absence de recherche (« automatically displaying the right psalms… without any need to go looking » chez Universalis), le texte en regard, l'achat unique, la réactivité des développeurs et l'esthétique soignée (ORDO).

### 2.7 Différenciateurs de Paroissien

1. **Le livre papier comme modèle d'interaction, pas comme décor.** Aucune application catholique étudiée ne propose de page qui tourne ni de double page en paysage [S, d'après les fiches lues]. Missale Romanum emploie la métaphore des rubans, mais sous forme d'onglets [V].
2. **Des rubans de couleur liturgique** qui mémorisent chacun sa position : messe, office, Imitation, lettre en cours… On résout ainsi la douleur « reprendre où j'en étais » relevée dans les avis.
3. **Des images pieuses glissées entre les pages**, un usage absent de tous les concurrents [S]. C'est un ancrage affectif fort, avec des images personnelles et des souvenirs de communion ou d'ordination.
4. **Le regroupement de contenus jamais réunis** : les deux formes du rite romain, en français et en latin, la Bible, l'Imitation, les Exercices et Clairval. Les apps francophones sont soit OF (AELF, Prions, Magnificat), soit tradi (ORDO), jamais les deux ni avec la lecture spirituelle.
5. **Sans distraction par construction** : pas de compte, pas de publicité, pas de traceur, pas de streak, pas de notification par défaut, avec un mode méditation (NPD, minuteur, cloche).
6. **Une confiance mesurable** : déclaration « Aucune donnée collectée », distribution possible sur F-Droid, code ouvert, comme AELF, qui obtient 4,7★ avec 12 k avis.

### 2.8 Erreurs à éviter

- Les **publicités**, quel que soit leur format, et les SDK publicitaires ou analytiques tiers.
- Le **compte obligatoire**, y compris pour « sauvegarder » ; préférer un fichier d'export.
- Les **abonnements opaques** ou l'abonnement annuel seul. Si un modèle payant est retenu, préférer l'achat unique ou le don.
- Les **notifications par défaut**. Un rappel d'office ne doit exister qu'en opt-in, à l'heure choisie par l'utilisateur (une demande d'avis AELF le confirme).
- La **gamification** : *streaks*, badges, compteurs de jours, classements.
- Les **mises à jour forcées** et les refontes qui retirent une fonction quotidienne.
- La **dépendance réseau** au démarrage.
- **L'animation de page imposée** : elle doit pouvoir être désactivée (voir 2.5).
- Le **contenu IA** ou sans source.
- Les **régressions d'*insets*** (edge-to-edge sur Android 15 et suivants).

### 2.9 Tableau comparatif synthétique

Légende : ✔ oui · ~ partiel · ✘ non · ? non vérifié. Les notes sont celles de Google Play (vitrine FR) au 11/10/2026, sauf mention iOS.

| Application | Note (avis) | Rites et contenus | Français | Latin en regard | Hors ligne | Compte | Pubs ou traceurs | Modèle | Expérience « livre » | Lecture spirituelle (Imitation, Clairval, Exercices) |
|---|---|---|---|---|---|---|---|---|---|---|
| **AELF** | 4,7 (12,4 k) | FO : messe, LH, Bible | ✔ | ✘ | ~ (1 mois synchronisé) | ✘ | ✘ / ✘ | Gratuit, bénévole, MIT | ✘ | ✘ |
| **Prions en Église** | 4,8 (20,9 k) | FO : messe, LH, audio, musique | ✔ | ✘ | ~ | ✔ | ✘ / collecte | 2,99 €/mois, 34,99 €/an | ✘ | ~ méditations |
| **Magnificat FR** | 4,3 (531) | FO : messe, offices, méditations | ✔ | ✘ | ~ (mensuel) | ✔ | ✘ / collecte | Numéro ou abonnement | ✘ | ~ méditations |
| **Laudate** | 4,5 (31,4 k) | FO + messe latine, dévotions, catéchisme | ✔ (16 langues) | ~ | ~ | ✘ | ✘ / ✘ | Gratuit | ✘ | ✘ |
| **iBreviary** | 4,2 (20,9 k) | FO en 10 langues + Vetus Ordo latin | ✔ | ~ | ~ (14 jours ; réseau au démarrage [A]) | ✘ | ✘ / diagnostics | Gratuit | ✘ | ✘ |
| **Universalis** | 5,0 (4,78 k) | FO : messe, LH (EN) | ✘ | ~ | ✔ intégral | ✘ | ✘ / IDs | **Achat unique 11,99 €** | ✘ | ✘ |
| **iMissal** (iOS) | 4,4 iOS (2 k) | FO (EN) | ✘ | ✘ | ✔ texte | ✘ | ✘ / ✘ | 4,99 $ + achats intégrés | ✘ | ✘ |
| **Missale Romanum** | 5,0 (1,91 k) | FO latin, ES (EN/IT/DE partiels) | ✘ | ✔ 2 colonnes | ✔ | ✘ | ✘ / ✘ | **Achat unique 11,99 €** | ~ onglets-rubans | ✘ |
| **ORDO** (TradDevs) | 4,9 (261) | 1962 : calendrier, propre, ordinaire, Kyriale | ✔ | ~ (bascule) | ✔ embarqué | ✘ | ✘ / perf. | Gratuit | ✘ | ✘ |
| **iMass** (FSSP) | 3,7 (300) | 1962 : messe, office, rituel, vidéo | ✘ | ✔ (rétabli) | ? | ? | ✘ / ✘ | Payant (ancien) | ✘ | ✘ |
| **Sanctifica** | 5,0 (1,57 k) | 1962, Guéranger, Butler, Vulgate | ✘ | ✔ | ~ | ✔ | ✘ / collecte | Freemium | ✘ | ~ Guéranger |
| **Iter Fidei** | nouveau (oct. 2026) | 1962, bréviaire, Bible, neuvaines | ? (fiche FR) | ✔ | ✔ | ? | ✘ / collecte | Achats intégrés | ✘ | ✘ |
| **Introibo** (iOS) | 4,8 iOS (60) | 1962, 1955, pré-1955, jeûnes | ✘ | ✔ | ✔ (sans Internet) | ✘ | ✘ / ✘ | Gratuit | ✘ | ✘ |
| **Hallow** | 4,4 (124 k) | Audio : prières, Exercices en 28 jours | ✔ (UI) | ✘ | ~ (téléchargements) | ✔ | ✘ / **partage avec des tiers** | 59,99 €/an | ✘ | ~ Exercices (audio) |
| **Pray As You Go / Prie en Chemin** | 4,6 (2,23 k) / 4,5 (658) | Audio ignatien quotidien | ✔ (PeC) | ✘ | ~ | ✘ | ✘ / SDK (PeC) [R] | Gratuit, dons | ✘ | ~ ignatien |
| **Hozana** | 4,3 (1,85 k) | Neuvaines, retraites communautaires | ✔ | ✘ | ✘ | ✔ | ✘ / collecte | Gratuit, dons | ✘ | ~ retraites |
| **Sanctus** (iOS, S. Hirondelle) | 4,7 iOS (11) | LH, Bible audio, chant, Imitation audio, IA | ~ (audio FR) | ? | ? | ✘ | ✘ / ? | 69,99 $/an | ✘ | ~ Imitation audio |
| **Moon+ Reader** (liseuse) | 3,6 (269 k) | — | ✔ | — | ✔ | ✘ | **✔ pubs** | Freemium | ✔ courbure, double page | — |
| **ReadEra** (liseuse) | 4,9 (1,59 M) | — | ✔ | — | ✔ | ✘ | ✘ / ✘ | Gratuit sans pub | ~ | — |
| **Paroissien (cible)** | — | **1962 + FO, Bible, Imitation, Clairval, Exercices, prières** | **✔** | **✔** | **✔ strict** | **✘** | **✘ / ✘** | **À décider (§4)** | **✔ pages, rubans, images pieuses** | **✔** |

FO : forme ordinaire. LH : liturgie des heures. « Collecte » : la fiche Play déclare collecter des données personnelles. « Perf. » : données de performance de l'application uniquement.

---

## 3. Recommandations

### MVP : ce qui fait la différence dès le premier jour

1. **Un hors-ligne strict, démarrage compris.** Le contenu doit être embarqué ou fourni par paquets locaux, sans aucun appel réseau pour ouvrir l'application ou un texte. Cela répond aux plaintes contre iBreviary et Hallow, et c'est ce qui vaut à Universalis et ORDO leurs notes de 5,0 et 4,9.
2. **Zéro compte, zéro publicité, zéro SDK tiers** : ni analytique, ni attribution, ni crash reporter tiers. Viser la mention « Aucune donnée collectée » dans la fiche Play, comme AELF, Laudate et Missale Romanum.
3. **« Aujourd'hui » en un geste.** L'application ouvre le livre à la messe ou à l'office du jour, d'après le calendrier hors ligne. C'est la douleur n° 1 chez Prions (« il faut vraiment chercher ») et c'est l'éloge n° 1 d'Universalis.
4. **Des rubans qui mémorisent chacun leur position**, avec un retour exact au paragraphe près et non au chapitre. Le concept est validé par Missale Romanum (« onglets = rubans ») et répond à la demande de reprise exprimée chez Introibo, ORDO et Examen.
5. **Le latin et le français en regard pour 1962** : en colonnes en paysage et sur double page, en alternance ou par bascule rapide en portrait. La crise iMass v7 l'a montré : on ne retire jamais cette fonction.
6. **Une page qui tourne, sobre et maîtrisable** : elle suit le doigt, reste rapide et interruptible, et se désactive (courbure, glissement ou aucune animation). Elle respecte le réglage système « supprimer les animations » et accepte des zones de tap ainsi que les touches de volume. Détails en 2.5.
7. **Une double page en paysage** réglable en Auto, 1 page ou 2 pages, comme Play Livres. Elle respecte la parité (page paire à gauche, impaire à droite) et prévoit une gouttière. Surtout, **l'ancre de lecture reste stable** à la rotation et au changement de police : la perte de position est la plainte n° 1 contre les liseuses.
8. **Un signet rapide d'un tap dans le coin supérieur droit**, le geste standard des liseuses, distinct des rubans. Ce geste doit être annulable et accompagné d'un retour haptique pour éviter les créations accidentelles (cas d'iOS 16).
9. **Une lisibilité irréprochable** : thèmes clair, sombre et sépia contrastés, taille de police réglable, plein écran *facultatif* qui ne masque pas l'heure de force (avis sur Missale Romanum), et TalkBack (atout d'AELF).
10. **La robustesse *edge-to-edge* et *insets*** sur Android 15 et suivants, testée en portrait et en paysage. La panne d'iBreviary en août 2026 a duré des semaines. Ce point est à transmettre à l'agent 10.
11. **Au moins un contenu introuvable ailleurs dès le MVP** : l'*Imitation* en français bien composée, et/ou les lettres de Clairval si l'abbaye donne son accord. Sinon Paroissien n'est qu'un ORDO de plus.
12. **L'export et l'import locaux** des signets, des notes et des références d'images pieuses dans un fichier, en remplacement du compte (avis Laudate : prières perdues).

### V1

1. **Une retraite ignatienne guidée en texte**, au rythme des « semaines ». Elle comprend la préparation, les points, les colloques et l'examen, un journal local chiffré, et l'écran maintenu allumé pendant l'oraison. Elle propose au choix un parcours court (environ 30 jours) et un parcours long de type 19ᵉ annotation. **Aucune application n'en propose en français** (voir 2.3 et 2.4).
2. **Un mode méditation** combinant « Ne pas déranger », minuteur, cloche discrète et écran allumé. Il répond à la plainte « l'app perd sa place pendant le silence » relevée chez Reimagining the Examen.
3. **Des rappels en opt-in uniquement**, à l'heure choisie par l'utilisateur (angélus, office), et **désactivés par défaut**. Les avis AELF et Hozana montrent que certains les veulent, mais à leurs conditions.
4. **Les images pieuses personnelles** : importer une photo d'image de communion ou d'ordination et la glisser entre deux pages.
5. **Une recherche plein texte** dans tous les livres installés.
6. **La forme ordinaire**, en s'appuyant sur les textes AELF si une licence est obtenue (agent 12) plutôt qu'en concurrençant AELF de front.
7. **Les variantes propres au public tradi** : messe lue, chantée ou solennelle, propres de France et diocésains, indications de posture facultatives et désactivées par défaut, comme dans Sancta Missa.

### Plus tard

- Une synthèse vocale hors ligne pour les lectures et les méditations. Les concurrents sont très audio, mais Paroissien reste « livre d'abord ».
- Les partitions grégoriennes, demandées par les utilisateurs d'ORDO.
- Les autres rubriques (1955, pré-1955), le bréviaire 1962 complet et le Petit Office.
- Des renvois de page vers les missels papier courants, sur l'idée de Missal Page. Ils seraient très utiles pour les fidèles qui utilisent un missel papier à la messe ; les éditions de référence restent à définir.
- Une distribution F-Droid et un code ouvert, si l'option est retenue.
- Une version multilingue : Clairval existe en 5 langues supplémentaires.
- Des widgets « saint et couleur du jour ». Ce sont des fonctions appréciées chez Introibo et Missal Page, mais pas essentielles.

### À ne jamais faire

Les publicités, le compte obligatoire, l'abonnement seul, les *streaks*, badges et classements, les notifications par défaut, les mises à jour forcées, le contenu généré par IA, les compagnons conversationnels IA, la communauté et le chat, et les SDK de suivi.

---

## 4. Décisions à soumettre à l'utilisateur

1. **Le modèle économique.**
   - (a) Gratuit et bénévole, avec éventuellement un bouton de don, sur le modèle d'AELF, de Pray As You Go et de Hozana.
   - (b) Un achat unique, sur le modèle d'Universalis et de Missale Romanum (11,99 €, notes de 5,0).
   - (c) Le gratuit avec des « dons patron » facultatifs sans contrepartie fonctionnelle, comme Missal Page.
   - *Recommandation : (a) ou (c). L'abonnement est à exclure.*
2. **Le code ouvert et F-Droid** (comme AELF) ou un code fermé (comme ORDO). Le code ouvert renforce la confiance du public « vie privée », mais complique certaines licences de contenu (agent 12).
3. **La permission INTERNET.**
   - (a) Aucune, avec des paquets installés par fichier ou via un module séparé, comme Introibo qui fonctionne « sans permission Internet ».
   - (b) Un téléchargement intégré des paquets, explicite et à la demande.
   - *Recommandation : (b) avec un écran clair, ou (a) pour une édition « F-Droid ».*
4. **La place de la forme ordinaire au MVP** : l'intégrer dès le départ (droits AELF), la reporter en V1, ou renvoyer vers AELF. Le marché FO francophone est déjà bien servi gratuitement.
5. **L'animation de page par défaut** : une courbure réaliste « papier » ou un glissement sobre, la courbure restant une option. Ce choix touche à l'identité du produit.
   - *Recommandation : une courbure courte et discrète par défaut, puisque c'est l'identité du produit.* Le glissement, le fondu ou l'absence d'animation restent proposés.
   - L'application bascule automatiquement sur « aucune animation » si les animations système sont coupées ou si l'économiseur de batterie est actif (voir 2.5).
6. **Le positionnement face à ORDO (TradDevs)** : rester concurrents ou contacter l'éditeur pour coopérer sur le calendrier et le propre 1962. Les deux applications sont francophones, gratuites et bien notées.
7. **L'audio** : aucun, une synthèse vocale locale, ou des enregistrements. Les concurrents à forte audience (Hallow, Prions, YouPray, Prie en Chemin) sont audio d'abord.
8. **Le nom « Paroissien »** : aucune application homonyme n'apparaît sur Play (recherche du 11/10/2026) [V]. Une vérification des marques reste à faire (agent 12).
9. **La double page sur téléphone en paysage.** La vision la prévoit systématiquement, mais un téléphone en paysage n'offre qu'environ 380 dp de hauteur, soit une douzaine de lignes par page.
   - (a) Double page systématique, ce qui assume le format « missel de poche ».
   - (b) Double page selon un réglage Auto / 1 / 2, avec un défaut à choisir après un test sur un téléphone réel (agents 07 et 08).
   - *Recommandation : (b).*

---

## 5. Risques et inconnues

- **Une fenêtre concurrentielle qui se referme [V].** Iter Fidei (sortie le 1er octobre 2026, fiche FR « Messe tridentine »), Pew Pal (octobre 2026), Sanctifica, Introibo, Missal Page et Sanctus (avec une *Imitation* audio) occupent tous le créneau « traditionnel tout-en-un » en 2026. Le différenciateur doit rester l'expérience livre, le français et les contenus uniques, pas seulement le catalogue.
- **La fiabilité liturgique.** Les erreurs de rubriques et de textes sont la première cause de critique des usagers avertis (iMissal, coquilles relevées dans ORDO). Il faut un jeu de tests de calendrier (agent 05) et une relecture humaine.
- **Les droits sur les contenus.** Lettres de Clairval, textes AELF, traductions françaises du missel de 1962 : sans autorisation, les contenus uniques du MVP tombent. Voir les agents 01, 11 et 12.
- **Le coût de la page qui tourne.** Une courbure saccadée ou qui scintille sur un téléphone modeste détruirait l'effet « papier » ; on en signale chez Moon+ sous Android 12 [S]. Les bibliothèques repérées sont soit en Compose, soit en OpenGL ancien (2.5). La performance reste à valider par l'agent 07, avec un mode dégradé automatique.
- **Un concept de ruban sans précédent.** Ce point est à la fois une opportunité et un risque d'ergonomie : il faut tester que les utilisateurs comprennent qu'il faut « tirer » le ruban [S].
- **Le piège *edge-to-edge*** sur les versions Android récentes, illustré par l'incident iBreviary [V, avis].
- **La découvrabilité.** Le Play Store est saturé de clones « Catholic Missal 2026 » qui achètent la visibilité. Le bouche-à-oreille, les paroisses, communautés et fraternités, ainsi que F-Droid seront probablement plus efficaces que l'optimisation pour le store [S].
- **Une attente audio.** Les leaders sont audio d'abord. Un public jeune pourrait trouver Paroissien « trop texte » [S].
- **Les inconnues dues au quota de recherche épuisé.** Plusieurs points n'ont pas pu être confirmés :
  - le statut actuel d'iMissal sur Android ;
  - les applications Android de l'éditeur Magnificat hors France ;
  - les éventuelles applications « Imitation » ou « Clairval » sur l'App Store français ;
  - les notes Play exactes de Reimagining the Examen et de Sanctus ;
  - une éventuelle application Android de Breviarium Meum.
- **Des notes instables.** Les chiffres sont des instantanés au 11/10/2026. Les notes affichées par Play varient selon l'appareil et la vitrine ; par exemple, AppGoblin affiche 4,7★ pour Hallow là où la vitrine FR montrait 4,4★.

---

## 6. Sources

Toutes les sources ont été consultées le 11 octobre 2026. Les fiches Play renvoient à la vitrine France ; le paramètre `hl` fixe la langue d'affichage.

### Fiches Google Play (lues dans un navigateur)
- AELF : https://play.google.com/store/apps/details?id=co.epitre.aelf_lectures&hl=fr
- Prions en Église : https://play.google.com/store/apps/details?id=com.groupebayard.prionseneglise.iphone&hl=fr
- Magnificat FR : https://play.google.com/store/apps/details?id=com.magnificat.magnificat.fra&hl=fr
- Laudate : https://play.google.com/store/apps/details?id=com.aycka.apps.MassReadings&hl=en
- iBreviary : https://play.google.com/store/apps/details?id=com.netguru.ibreviary&hl=en
- Universalis : https://play.google.com/store/apps/details?id=com.universalis.android.universalis&hl=en
- Universalis (calendrier et essai) : https://play.google.com/store/apps/details?id=com.universalis.android.calendar&hl=en
- Missale Romanum : https://play.google.com/store/apps/details?id=rcr.missale.romanum&hl=en
- ORDO (TradDevs) : https://play.google.com/store/apps/details?id=com.traddevs.ordo&hl=fr
- iMass : https://play.google.com/store/apps/details?id=fssp.livemass.iMass&hl=en
- Divine Office (St Anselm) : https://play.google.com/store/apps/details?id=net.breviary.traditional&hl=en
- Pew Pal : https://play.google.com/store/apps/details?id=com.pewpal.app&hl=en
- Iter Fidei : https://play.google.com/store/apps/details?id=com.iterfidei.mobileapp&hl=en
- Sanctifica : https://play.google.com/store/apps/details?id=com.sanctifica&hl=en
- Liturgie des heures (Père Philoux) : https://play.google.com/store/apps/details?id=eu.philoux.lh&hl=fr
- Tradiz : https://play.google.com/store/apps/details?id=com.gingersquirrel.prod.messetradi&hl=fr
- Prières Latin-Français : https://play.google.com/store/apps/details?id=survivalinextremis.com.Orationes_Latine&hl=fr
- Catholic Missal 2026 (BibleChants) : https://play.google.com/store/apps/details?id=com.BibleChants.Catholic.Daily.Readings&hl=en
- Catholic Missal 2026 & Prayers (Freedom Works) : https://play.google.com/store/apps/details?id=com.audio.practise&hl=en
- Imitation of Christ Audio : https://play.google.com/store/apps/details?id=com.Catholic.Zone.Imitation.Christ.Book.Audio.offline.Text&hl=en
- Ejercicios Espirituales (IVE) : https://play.google.com/store/apps/details?id=com.churomobile.ive.ejercicios&hl=en
- Ejercicios Espirituales (CLICKApps) : https://play.google.com/store/apps/details?id=org.clickapps.eesil&hl=en
- Hallow : https://play.google.com/store/apps/details?id=app.hallow.android&hl=en
- Pray As You Go : https://play.google.com/store/apps/details?id=pl.foxcode.prayasyougo&hl=en
- Prie en Chemin : https://play.google.com/store/apps/details?id=org.prieenchemin&hl=fr
- Ascension : https://play.google.com/store/apps/details?id=com.ascension.app&hl=en
- Hozana : https://play.google.com/store/apps/details?id=org.hozana.app&hl=fr
- YouPray : https://play.google.com/store/apps/details?id=com.mediatech.youpray&hl=fr
- Moon+ Reader : https://play.google.com/store/apps/details?id=com.flyersoft.moonreader&hl=en
- ReadEra : https://play.google.com/store/apps/details?id=org.readera&hl=en
- Librera : https://play.google.com/store/apps/details?id=com.foobnix.pdf.reader&hl=en
- Google Play Livres : https://play.google.com/store/apps/details?id=com.google.android.apps.books&hl=en
- Kindle : https://play.google.com/store/apps/details?id=com.amazon.kindle&hl=en
- Recherches Play :
  - « missel 1962 latin français » : https://play.google.com/store/search?q=missel%201962%20latin%20fran%C3%A7ais&c=apps&hl=fr
  - « messe tridentine » : https://play.google.com/store/search?q=messe%20tridentine%20missel%20traditionnel&c=apps&hl=fr
  - « clairval » : https://play.google.com/store/search?q=clairval&c=apps&hl=fr
  - « imitation de jésus-christ » : https://play.google.com/store/search?q=imitation%20de%20j%C3%A9sus-christ&c=apps&hl=fr
  - « exercices spirituels » : https://play.google.com/store/search?q=exercices%20spirituels%20saint%20ignace%20retraite&c=apps&hl=fr
  - « paroissien » : https://play.google.com/store/search?q=paroissien&c=apps&hl=fr

### App Store
- iMissal : https://apps.apple.com/us/app/imissal-catholic/id307312434
- Introibo : https://apps.apple.com/us/app/introibo-tlm-app/id6762865358
- Sancta Missa 1962 : https://apps.apple.com/us/app/-/id6760446896
- Missal Page : https://apps.apple.com/us/app/missal-page-tlm-companion/id6760142128
- Hallow US : https://apps.apple.com/us/app/hallow-prayer-meditation/id1405323394
- Hallow FR : https://apps.apple.com/fr/app/hallow-pri%C3%A8re-et-m%C3%A9ditation/id1405323394
- Pray As You Go : https://apps.apple.com/us/app/pray-as-you-go-daily-prayer/id865934048
- Prie en Chemin : https://apps.apple.com/fr/app/prie-en-chemin-pri%C3%A8re-audio/id1202145096
- Ascension : https://apps.apple.com/us/app/ascension-catholic-bible/id1660909501
- Reimagining the Examen : https://apps.apple.com/us/app/reimagining-the-examen/id1065042173
- 3-Minute Retreat : https://apps.apple.com/us/app/3-minute-retreat/id323368405
- Jesuit Prayer : https://apps.apple.com/us/app/jesuitprayer/id540380545
- Click To Pray : https://apps.apple.com/us/app/click-to-pray/id934935942
- Hozana : https://apps.apple.com/fr/app/hozana-christian-prayer/id1477461388
- Liturgie – Textes AELF : https://apps.apple.com/fr/app/liturgie-textes-aelf/id991620025
- Sanctus (S. Hirondelle) : https://apps.apple.com/app/id6792047130

### Sites officiels, aides et documentation
- F-Droid AELF : https://f-droid.org/packages/co.epitre.aelf_lectures
- Clairval : https://www.clairval.com/
- Romanitas Press Ordo : https://www.romanitaspress.com/ordo-mobile-app
- Hallow (aide) :
  - tarifs : https://help.hallow.com/en/articles/2880438-how-much-does-the-subscription-cost
  - streaks : https://help.hallow.com/en/articles/5761398-streaks-prayer-activity-faq
  - téléchargements : https://help.hallow.com/en/articles/3276892-how-to-download-prayers-and-manage-downloaded-sessions
- Hallow, collection « Spiritual Exercises » : https://hallow.com/collections/2416/
- Pray As You Go, 20 ans : https://www.jesuit.org.uk/news/jesuit-prayer-app-pray-as-you-go-turns-20-serving-a-global-community-of-over-half-a-million
- Cloisters Ignatian Prayer : https://cloistersignatianprayer.org/
- Creighton Online Retreat : https://onlineministries.creighton.edu/CollaborativeMinistry/Exer-Ignat.html
- Boston College, 19ᵉ annotation : https://www.bc.edu/bc-web/offices/missionministry/sites/center-for-ignatian-spirituality/prayer/19th-annotation.html
- Hozana : https://hozana.org/
- Prier dans la ville : https://www.prierdanslaville.org/
- Apple Books (guides) :
  - iPhone : https://support.apple.com/en-us/guide/iphone/iphc1af7c57/ios
  - iPad : https://support.apple.com/en-us/guide/ipad/ipadc8494b6b/ipados
- Google Play Livres (aide) :
  - mise en page : https://support.google.com/googleplay/answer/9755756
  - signets et notes : https://support.google.com/googleplay/answer/3165868
  - touches de volume : https://support.google.com/googleplay/answer/7059097
- Librera (FAQ) :
  - mode de lecture : https://librera.mobi/faq/how-to-set-up-reading-mode
  - signets : https://librera.mobi/faq/edit-bookmark-page-and-floating-bookmarks
  - changelog : https://github.com/foobnix/LibreraReader/blob/master/CHANGELOG.md
- KOReader :
  - dépôt : https://github.com/koreader/koreader
  - guide : https://koreader.rocks/user_guide/
- Kindle (aide Amazon) : https://digprjsurvey.amazon.com/csad/help/node/TABlJ4ot69emTO8jJG
- WCAG 2.3.3 : https://www.w3.org/WAI/WCAG22/Understanding/animation-from-interactions.html
- Android, classes de taille de fenêtre : https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes
- Android, `ValueAnimator.areAnimatorsEnabled` : https://developer.android.com/reference/android/animation/ValueAnimator
- Bibliothèques :
  - https://github.com/oleksandrbalan/pagecurl
  - https://github.com/harism/android_page_curl
  - https://github.com/eschao/android-PageFlip

### Presse, blogs et trackers (statut [R] ou [S])
- The Verge, courbure d'Apple Books : https://www.theverge.com/2022/11/21/23471306/apple-books-ios-16-page-flip-animation-sucks
- Gadget Hacks, retour de la courbure : https://ios.gadgethacks.com/how-to/get-page-turning-curl-animation-back-apple-books-for-iphone-and-ipad-0385329
- Basic Apple Guy : https://basicappleguy.com/basicappleblog/build-a-better-books
- the-ebook-reader.com :
  - animation Kindle : https://blog.the-ebook-reader.com/2022/02/14/do-you-like-the-page-turn-animation-on-the-new-kindle-paperwhite/
  - signets Kindle 2025 : https://blog.the-ebook-reader.com/2025/12/09/change-to-bookmark-feature-on-kindles-after-update/
  - signets colorés Colorsoft : https://blog.the-ebook-reader.com/2026/02/19/kindle-colorsoft-now-has-colored-bookmarks-and-green-highlights/
- UnHerd sur Hallow : https://unherd.com/2025/05/you-wont-find-god-on-your-iphone/
- Wikipédia, Hallow : https://en.wikipedia.org/wiki/Hallow_(app)
- AppGoblin :
  - Hallow : https://appgoblin.info/apps/app.hallow.android
  - Prie en Chemin : https://appgoblin.info/apps/org.prieenchemin
  - Ascension : https://appgoblin.info/apps/com.ascension.app
  - Hozana : https://appgoblin.info/apps/org.hozana.app
- Mozilla *Privacy Not Included*, Hallow : https://www.mozillafoundation.org/en/privacynotincluded/hallow/
- mwm.ai :
  - Sanctus (autre éditeur) : https://mwm.ai/apps/sanctus-bible-prayers/6788785102
  - Credo : https://mwm.ai/apps/credo-catholic-prayer-app/6759874385
