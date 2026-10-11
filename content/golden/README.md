# golden/ — paquet de référence (gabarit, phase 2)

Paquet construit par la chaîne, versionné ici et lu par les tests Kotlin : c'est le contrat entre `content/` et l'application (revue 15, §5.4 ; rapport 11, §3.6). **Vide en phase 0.** Aucun contenu protégé : uniquement du domaine public ou du texte fictif.

## Contenu attendu

| Élément | Ce qu'il teste |
|---|---|
| Chaque type de bloc et chaque inline : rubrique, ℣/℟, ✠, appel de note, renvoi, changement de langue, lettrine | Rendu ; bloc inconnu affiché comme paragraphe |
| *Imitation* I, 1-3, latin et français | Bilingue, texte en regard |
| Ps 22 (Vulgate) / 23 (hébreu) | Double numérotation, versification native par édition |
| *Exercices*, [1] à [20] | Clés naturelles `ex.<n>` |
| Extrait de l'Ordinaire 1962 bilingue, sections mal alignées | Alignement latin/français |
| Une lettre de Clairval **fictive** | Structure d'une lettre, sans le texte réel (protégé) |
| Les 50 mots latins de césure | Césure calculée à l'exécution (aucun U+00AD stocké) |
| Vecteurs de pliage, y compris U+00AD | Repris de `../schema/fold-vectors.json` |
| Ancres « avant » et « après correction » | Ré-ancrage, `rev`, `id_alias`, annotations à replacer |

## Fichiers prévus

```
golden/
├── README.md
├── data/            # JSON canonique source du paquet
├── golden.sqlite    # paquet construit (format 1.x, SQLite 3.50.1)
├── golden.manifest.json   # rights: domaine-public
└── SHA256SUMS       # empreintes attendues
```

Un test reconstruit le paquet et exige un résultat **bit à bit identique** aux empreintes de `SHA256SUMS`.
