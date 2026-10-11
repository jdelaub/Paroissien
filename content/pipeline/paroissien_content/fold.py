"""Pliage de recherche « fold-v1 » (rapport 11, §3.7 ; revue 15, M3).

Appliqué au texte indexé (colonne FTS5 pliée) comme à la requête. Le code
Kotlin doit en être le miroir exact ; le contrat commun est
``content/schema/fold-vectors.json``, testé des deux côtés.

Étapes, dans cet ordre :

0. retirer U+00AD (trait d'union conditionnel), U+200B (espace sans chasse)
   et U+2060 (gluon) ;
1. NFKC (ﬁ→fi, espaces insécables→espace, exposants→lettres ou chiffres,
   …→...) ;
2. minuscules par ``str.lower()``, PAS ``casefold`` (ß reste ß). Côté Kotlin :
   ``String.lowercase(Locale.ROOT)`` sur la chaîne entière, jamais caractère
   par caractère (sigma final) ni avec la locale par défaut (I→ı en turc) ;
3. NFD, puis suppression des caractères de catégorie Mn ;
4. table : œ→oe, æ→ae, ĳ→ij ;
5. j→i, v→u, sur tous les textes ;
6. tout le reste, ponctuation comprise, est conservé.

La sortie n'est pas recomposée en NFC.
"""

from __future__ import annotations

import unicodedata

SPEC = "fold-v1"

_REMOVED = dict.fromkeys(map(ord, "\u00ad\u200b\u2060"))

# ĳ n'arrive jamais ici : NFKC l'a déjà décomposé en « ij » (étape 1).
# L'entrée reste pour suivre la spécification.
_TABLE = str.maketrans({"œ": "oe", "æ": "ae", "ĳ": "ij"})
_LATIN = str.maketrans({"j": "i", "v": "u"})


def fold(s: str) -> str:
    """Plie ``s`` selon fold-v1 et renvoie la chaîne pliée.

    >>> fold("l’Église")
    'l’eglise'
    >>> fold("prǽdica")
    'praedica'
    >>> fold("Jésus")
    'iesus'
    """
    s = s.translate(_REMOVED)
    s = unicodedata.normalize("NFKC", s)
    s = s.lower()
    s = unicodedata.normalize("NFD", s)
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return s.translate(_TABLE).translate(_LATIN)
