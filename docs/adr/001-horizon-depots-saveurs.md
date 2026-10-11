# ADR 001 — Horizon de diffusion, dépôts et saveurs

## Statut
Accepté, 2026-10-11.

## Contexte
Le premier risque du projet est juridique : les lettres de Clairval, le français du Propre de 1962 (Barroux, via Divinum Officium) et les textes AELF ne se diffusent pas sans autorisation écrite. Le code est public, et sur un dépôt public tout compte GitHub peut télécharger les artefacts de CI : un APK qui contiendrait ces textes, construit par la CI publique, serait une rediffusion. Il faut donc fixer dès la phase 0 ce qui est construit, où, et avec quels contenus.

## Décision
- **Horizon.** Scénario A (usage personnel, installation par adb) maintenant ; B (proches) ensuite, selon les autorisations reçues ; C (public) gardé en tête, non visé. La saveur publique est dès aujourd'hui « propre » pour C, afin que B ou C ne demandent aucune refonte.
- **Dépôts.**
  - `Paroissien` : public, GPL-3.0-or-later, en-tête SPDX dans chaque fichier source ; le module `core:liturgy` est sous Apache-2.0 (fichier `LICENSE` propre au module). Il contient le code, la chaîne `content/` et les seules données libres.
  - `paroissien-content` : privé. Il contient le texte normalisé des sources non libres, la définition des paquets privés et la CI de la saveur `personnel`.
- **Statut de droits** : une valeur `rights` par paquet (ADR 003), une seule, parmi `domaine-public`, `licence-libre:<SPDX>`, `autorisation-écrite:<réf>`, `usage-privé-seulement`. Chaque paquet a une fiche de provenance : source, édition, auteurs et traducteurs avec leurs dates de décès, qualification de l'œuvre, scénarios autorisés, date de vérification.
- **Affectation initiale** :

  | Contenu | `rights` |
  |---|---|
  | Imitation (Lamennais), Exercices (Jennesseaux), Crampon 1923 (DP probable), Vulgate clémentine, prières des paroissiens de 1860-1901, Ordinaire 1962 français tiré du DP | `domaine-public` |
  | Latin de Divinum Officium, données du calendrier (romcal, Divinum Officium) | `licence-libre:MIT` |
  | Images pieuses des institutions | `licence-libre:CC0-1.0` |
  | Traductions personnelles des lacunes de 1920 à 1962 | `licence-libre:CC-BY-SA-4.0` |
  | Clairval ; français du Propre 1962 (Barroux) ; AELF (lectures, Ordinaire 2021) | `usage-privé-seulement` |

- **Saveurs Gradle**, dimension `distribution`, même `applicationId` (ADR 002) :
  - `domainePublic` n'embarque que `domaine-public` et `licence-libre:<SPDX>` ;
  - `personnel` embarque en plus `autorisation-écrite:<réf>` et `usage-privé-seulement`. La forme ordinaire (textes AELF) y reste tant que l'AELF n'a pas donné d'autorisation.
- **Qui construit quoi** :

  | Variante | Où | Artefacts |
  |---|---|---|
  | `domainePublicDebug`, `domainePublicBenchmark`, tests, lint, Roborazzi | CI publique de `Paroissien` | publics, 7 jours |
  | `personnelDebug` | CI privée de `paroissien-content`, ou build local | 1 jour, jamais attachés à une Release |
  | `*Release` | local uniquement, signé en local (ADR 002) | `personnel` : jamais publié ; `domainePublic` : Release GitHub à partir de B |

- **Contrôles bloquants** :
  1. une tâche Gradle de vérification, dépendance de tout `assemble` d'une variante `domainePublic`, lit le `rights` de chaque paquet embarqué et échoue sur tout statut autre que les deux admis ;
  2. la CI publique refait ce contrôle (`tools/check-pack-rights.py`) sur les paquets effectivement présents dans l'APK produit (`assets/packs/`) ;
  3. la chaîne refuse de placer un paquet `usage-privé-seulement` dans le jeu de paquets publics ;
  4. les paquets privés n'entrent dans un build `personnel` que par un chemin hors dépôt (propriété Gradle `paroissien.personnelPacksDir`) ; aucun paquet ni texte non libre n'est commité dans `Paroissien`.
- **Aucune permission INTERNET** au MVP, dans les deux saveurs : les lectures AELF sont préchargées par la chaîne dans le paquet privé.
- Un APK `personnel` ne s'installe que sur le téléphone de l'utilisateur.

## Conséquences
- (+) Passer à B ou C revient à changer le statut d'un paquet ou la saveur distribuée, sans refonte.
- (+) La CI publique ne peut pas diffuser de contenu protégé, même par erreur de configuration (contrôle double).
- (−) Deux CI à maintenir. La CI privée est limitée (2 vCPU, 2 000 min/mois) : elle ne construit que `personnelDebug`.
- (−) Les deux saveurs partagent l'`applicationId` : on ne garde qu'une release installée à la fois. En échange, passer de l'une à l'autre conserve `user.db` (même clé).

## Points ouverts
- Réponses de Clairval, du Barroux et de l'AELF (délai butoir de 8 semaines après l'envoi pour le Barroux et l'AELF). Un paquet passé en `autorisation-écrite:<réf>` n'entre dans `domainePublic` qu'après un ADR qui le remplace, selon la portée (B ou C) de l'autorisation.
- Mécanisme du build destiné aux proches (B) : à définir à la première autorisation reçue.
- Modernisation 2005 des Exercices (« libre de tout droit », sans licence formelle) : classée `domaine-public` sous réserve ; repli sur Jennesseaux 1913 (archive.org).

## Références
PLAN §0, §2 (Dépôts), §5.1 (1 à 4) ; 15 B1, §5.1 ; 12 §2.8, §3 ; 10 §2.7, §3.1 h, §4 (3).
