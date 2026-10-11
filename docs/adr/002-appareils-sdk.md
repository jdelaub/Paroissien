# ADR 002 — Appareils, SDK et signature

## Statut
Accepté, 2026-10-11. Version d'Android du téléphone : à confirmer par adb.

## Contexte
Tout le spike (pli AGSL, budget par image) dépend du téléphone réel : niveau d'API, fréquence d'écran, GPU. L'`applicationId` et les clés de signature ne peuvent plus changer sans perte de données dès que l'usage quotidien commence (porte B1). Enfin, un APK de débogage signé par une clé régénérée sur chaque runner de CI ne s'installe pas par-dessus un build local (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`), et la désinstallation efface `user.db`.

## Décision
- **Téléphone de référence : Samsung Galaxy Z Flip6**, dernière version d'One UI.

  | Élément | Valeur | Source |
  |---|---|---|
  | SoC, GPU, RAM | Snapdragon 8 Gen 3, Adreno 750, 12 Go | fiche publique, à confirmer |
  | Écran principal | 6,7", 2640 × 1080 (22:9), jusqu'à 120 Hz | fiche publique, à confirmer |
  | Écran externe | 3,4" | fiche publique |
  | Version d'Android, API, One UI | **à confirmer** (lancé sous Android 14 : API ≥ 34, donc AGSL, est acquise) | `tools/device-inventory.sh` |
  | Densité, taille en dp, modes de rafraîchissement, pilote EGL | **à confirmer** | `tools/device-inventory.sh` |

  Le script relève au moins `ro.product.model`, `ro.build.version.release`, `ro.build.version.sdk`, `ro.hardware.egl`, `wm size`, `wm density` et les modes de `dumpsys display`. Le résultat est reporté dans ce tableau.
- **Postures du Flip6** :
  - « livre » : tenu de côté, à demi plié, `FoldingFeature` VERTICAL et HALF_OPENED → double page, la charnière servant de reliure (spike 1b) ;
  - « table » (Flex mode, charnière horizontale) → séance de méditation : texte ou image en haut, minuteur et cloche en bas (phase 4) ;
  - paysage déplié : environ 380 à 410 dp de haut, soit environ 12 lignes par page en double page ; le seuil du mode Auto se règle à la porte A.
- **Spécificités Samsung à tester** : poignée des Edge panels contre la saisie des coins (ADR 006) ; « Modes et routines » contre `AutomaticZenRule` (phase 4) ; optimisation de la batterie contre les alarmes (Angélus, V1).
- **Second appareil** : un téléphone de milieu de gamme, API ≥ 33, **obligatoire avant B**. Il passe les critères de la porte A à sa propre fréquence d'écran (ADR 005).
- **SDK** : `minSdk 29`, `targetSdk 36`, `compileSdk 37`.
  - Justification : `NotificationManager.setAutomaticZenRuleState` (API 29) unifie la règle « Temps de prière », et `Typeface.CustomFallbackBuilder` (API 29) chaîne les polices de secours (ADR 007). L'exclusion des gestes n'est **pas** un argument (ADR 006).
  - Coût : 91,1 % du parc contre 96,1 % pour l'API 26 (StatCounter, avril 2026) ; sans objet en scénario A.
  - `targetSdk 37` en V1.
- **ABI** : `arm64-v8a` en debug ; `arm64-v8a` et `armeabi-v7a` en release.
- **`applicationId` figé : `app.paroissien`**, identique pour `domainePublic` et `personnel`. Le type de build `debug` ajoute le suffixe `.dev` : la version d'usage quotidien et celle de développement coexistent.
- **Signature debug** : keystore dédié, commité et non secret, `android/keystore/debug.keystore` (mots de passe standard `android`, alias `androiddebugkey`). `signingConfigs.debug` pointe vers lui en CI comme en local : `adb install -r` fonctionne entre un APK de CI et un build local.
- **Signature release** : en local uniquement. Clé `~/.android-keys/paroissien-release.jks` ; propriétés `PAROISSIEN_KEYSTORE`, `PAROISSIEN_KEYSTORE_PASSWORD`, `PAROISSIEN_KEY_ALIAS` et `PAROISSIEN_KEY_PASSWORD` dans `~/.gradle/gradle.properties`, lues par `providers.gradleProperty`. Rien dans le dépôt ni dans les secrets de CI ; sans ces propriétés, l'APK release n'est pas signé. La clé est sauvegardée sur deux supports hors ligne.
- **Type de build `benchmark`** (ajout de cet ADR) : `initWith(release)`, R8 actif, non débogable, profilable, signé avec le keystore de débogage, suffixe `.bench`. La CI publique le construit pour les mesures de la porte A (ADR 005), sans jamais détenir la clé de release.

## Conséquences
- (+) Le spike peut commencer par le pli AGSL.
- (+) Aucune désinstallation forcée entre builds : `user.db` survit aux mises à jour.
- (+) Trois installations distinctes : `app.paroissien`, `app.paroissien.dev`, `app.paroissien.bench`.
- (−) Le Flip6 est haut de gamme : ses mesures ne valent pas pour B sans le second appareil.
- (−) Le keystore de débogage étant public, n'importe qui peut signer un APK `.dev` : l'enjeu se limite aux données de développement.

## Points ouverts
- Version d'Android, densité et modes de rafraîchissement : `tools/device-inventory.sh`, puis mise à jour du tableau.
- Modèle du second appareil.
- Widget « Aujourd'hui » sur l'écran externe (Flex Window) en V1, si One UI accepte les widgets tiers.

## Références
PLAN §4 (phase 0), §5.1 (5), §5.3 (minSdk) ; 15 B2, M14, m1, m10, m12, §3 ; 10 §2.8, §3.1 c et g ; 08 §2.11.
