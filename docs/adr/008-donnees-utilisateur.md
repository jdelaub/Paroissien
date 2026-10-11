# ADR 008 — Données de l'utilisateur

## Statut
Accepté, 2026-10-11.

## Contexte
Rubans, images pieuses placées et positions de lecture sont la mémoire du livre ; reprendre exactement où l'on en était est la demande la plus fréquente des utilisateurs (13). Les perdre à une mise à jour ou à un changement de téléphone serait rédhibitoire. Le journal et les intentions révèlent des convictions religieuses (RGPD, art. 9) et ne doivent jamais quitter l'appareil sans un geste explicite. Il n'y a ni compte ni serveur.

## Décision
- **Stockage** :
  - `user.db` avec **Room 3** (`androidx.room3` 3.0.3), KSP **dans `core:data` seulement**, `BundledSQLiteDriver` (le même SQLite 3.50.1 que les paquets), DAO `suspend` ou `Flow`, requêtes sur `Dispatchers.IO` ;
  - réglages dans DataStore Preferences ; paquets dans `noBackupFilesDir/packs/` (ADR 003) ; cache de pagination dans `cacheDir`.
- **Modèle** au MVP : `Ribbon`, `CardPlacement`, `ReadingPosition`, `PackVersionSeen` (dernière version de chaque paquet connue des ancres) et `ReanchorLog`. Identifiants UUID, `updatedAt`, `deletedAt` (corbeille de 30 jours, pierres tombales pour la fusion à l'import). Ancres au format de l'ADR 004, avec leur état : OK, APPROX ou ORPHELIN. En V1 : `DogEar`, `Highlight`, `Note`, cartes personnelles.
- **Migrations** :
  - schémas exportés dans `android/core/data/schemas/` et **commités** ; la CI échoue si un build modifie un schéma sans qu'il soit commité ;
  - chaque nouvelle version passe par `AutoMigration` ou par une migration écrite, **testée depuis chaque schéma exporté** jusqu'au schéma courant ;
  - `fallbackToDestructiveMigration`, sous toutes ses formes, est **interdit** : la CI échoue si le nom apparaît dans les sources.
- **Sauvegarde** : `allowBackup="true"` avec `dataExtractionRules` (API 31 et plus) :
  - `<cloud-backup>` : tout est exclu, la **sauvegarde cloud est désactivée** ;
  - `<device-transfer>` : `user.db` et DataStore inclus ; paquets, caches, journal et intentions exclus. Le **transfert d'appareil à appareil est autorisé** ;
  - API 29 et 30 : `fullBackupContent` équivalent, dont les inclusions portent `requireFlags="deviceToDeviceTransfer"`.
- **Export** `Paroissien-AAAA-MM-JJ.paroissien` : un ZIP écrit et lu par le sélecteur de fichiers (`CreateDocument`, `OpenDocument`), sans permission.
  - B1 : `manifest.json` (`formatVersion: 1`, version de l'app, date, comptes, SHA-256) et `annotations.json` (rubans, positions, placements de cartes ; ancres doublées de sélecteurs W3C `TextQuoteSelector` et `TextPositionSelector`) ;
  - V1 : cartes personnelles, surlignages, notes, coins cornés ; intentions **seulement si l'utilisateur coche la case**, décochée par défaut ; chiffrement facultatif par mot de passe (AES-GCM, clé dérivée par PBKDF2) ;
  - import : « Fusionner » (par UUID, le `updatedAt` le plus récent l'emporte, pierres tombales respectées) ou « Tout remplacer » (précédé d'une copie locale) ; validation du schéma, protection contre le *zip-slip*, limites de taille. Un test d'aller-retour export puis import est obligatoire.
- **Export proposé dès le premier ruban posé**, une seule fois, puis rappelé dans « Mes signets » avec la date du dernier export.
- **Journal chiffré en V1** : base séparée `journal.db`, champs de texte chiffrés en AES-256-GCM avec une clé non exportable du Keystore Android (pas de SQLCipher, qui embarquerait un second SQLite) ; exclu de la sauvegarde et du transfert ; écran en `FLAG_SECURE`, clavier sans apprentissage, aucun contenu dans les notifications. Les intentions (V1) suivent le même régime. L'examen de conscience reste éphémère : rien n'est écrit.
- Aucune télémétrie, aucun rapport de plantage automatique.

## Conséquences
- (+) Aucune donnée de l'utilisateur ne passe par Google Drive ni par un serveur.
- (+) Une mise à jour de l'app ou d'un paquet ne détruit rien : migrations testées, ré-ancrage sans suppression.
- (−) Sans export, un téléphone perdu emporte rubans et positions ; d'où l'export proposé tôt.
- (−) Une clé du Keystore ne quitte pas l'appareil : le journal ne migre que par un export chiffré.
- (−) Room 3 est jeune (3.0.0 en juillet 2026) ; le squelette de la phase 0 doit prouver qu'il fonctionne avec AGP 9.4.1 et KSP.

## Points ouverts
- Smart Switch (Samsung) passe-t-il par le transfert `device-transfer` pour `user.db` ? À vérifier sur le Flip6 avant de compter dessus.
- Nom de l'artefact de test des migrations de Room 3 : à confirmer au squelette.

## Références
PLAN §2 (principe 6), §3.2, §5.3 (Données de l'utilisateur, Examen de conscience) ; 15 m9, §5.8 ; 09 §2.9, §3.5 ; 10 §2.5, §3.1 k ; 12 §2.7 ; 13 §1.
