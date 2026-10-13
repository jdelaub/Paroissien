# ADR 009 — Calendrier liturgique

## Statut
Accepté, 2026-10-11 (phase 4).

## Contexte
Il faut le calendrier de 1962 et celui de 1969, avec le propre de France, hors ligne. Aucune bibliothèque ne couvre les deux : romcal (MIT) ne fait que 1969, Missalemeum et Divinum Officium (MIT) ne font que 1962 et ignorent la France (rapport 05, §2.2). Le 1962 français n'existe nulle part sous une forme lisible par machine.

## Décision
- **Moteur** : Kotlin pur dans `core:liturgy` (Apache-2.0, aucune dépendance GPL). Le temporal est calculé par le code ; le sanctoral vient de données déclaratives (`content/schema/calendar-v1.md`). Une année entière est calculée à la fois, parce que les transferts regardent en avant.
- **Sources** : romcal pour 1969 (général et France, précepte corrigé) ; Divinum Officium et Missalemeum pour 1962. Les autres moteurs servent d'**oracles de test**, jamais de dépendances.
- **Ordo de référence pour 1962 en France** : celui d'un **monastère** (Le Barroux ou Fontgombault), ramené au calendrier romain de 1962. Il tranche les cas litigieux du propre de France.
- **Version de 1962** : le calendrier **tel qu'imprimé en 1962**. Les saints canonisés depuis (décret *Cum sanctissima*, 2020) pourront venir plus tard, en option.
- **Solennités extérieures (1962, France)** : **activées par défaut**, désactivables dans les réglages.
- **Jeûne et abstinence** : le **droit en vigueur** (CIC 1983 et CEF) dans les deux rites, avec en option la **discipline traditionnelle** (Quatre-Temps, vigiles jeûnées), étiquetée comme facultative.
- **Périmètre** : France sans diocèses (V1) ; plage garantie par les tests 1970-2100 pour 1969 et 2004-2050 pour 1962, au-delà calculée sans garantie.

## Conséquences
- (+) Un seul modèle (`LiturgicalDay`) pour les deux rites ; les identifiants sont communs quand la fête est la même.
- (+) Un nouveau décret ne demande qu'une mise à jour des données.
- (−) La casuistique de 1962 est à écrire et à tester nous-mêmes (tests G0 à G4, rapport 05 §3.1).
- (−) Le propre de France de 1962 est saisi à la main : il doit être relu.

## Références
PLAN §4 (phase 4), §5.2 ; rapport 05 ; `content/schema/calendar-v1.md`.
