# Données du calendrier, format v1

Lues par `core:liturgy` (Kotlin pur, Apache-2.0). Le **temporal** (dimanches, féries, temps, fêtes mobiles du Seigneur) est calculé dans le code ; les fichiers ne décrivent que le **sanctoral** à date fixe et ses surcharges par pays. Rapport 05, §3.1 et §3.4.

## Fichiers

```
content/data/calendar/
  1969/general.json   calendrier romain général (romcal, MIT)
  1969/fr.json        propre de France (romcal France, précepte corrigé)
  1962/general.json   calendrier de 1960-1962 (Divinum Officium et Missalemeum, MIT)
  1962/fr.json        propre de France (Ordo de référence, saisi à la main)
  SOURCES.md          provenance, licences, version figée de chaque source
```

## Structure

```json
{
  "format": 1,
  "rite": "1969",
  "calendar": "general",
  "sources": ["romcal@<commit>"],
  "entries": [
    {
      "id": "joseph_spouse_of_mary",
      "date": "03-19",
      "rank": "SOLEMNITY",
      "colors": ["WHITE"],
      "title": { "fr": "Saint Joseph, époux de la Vierge Marie", "la": "S. Ioseph, Sponsi B. M. V." },
      "lord": false,
      "validFrom": null,
      "validUntil": null,
      "proper": null,
      "commemorations": [],
      "saints": ["joseph"]
    }
  ]
}
```

| Champ | Règle |
|---|---|
| `id` | Slug stable `[a-z0-9_]+`. Les slugs de romcal pour 1969 ; **le même slug en 1962** quand la célébration est la même (saint Joseph, Assomption…), sinon un nouveau slug du même style. Jamais réutilisé pour autre chose. |
| `date` | `MM-DD`, toujours. Une fête mobile du sanctoral (Sacré-Cœur, Christ-Roi, Sainte Famille, Marie Mère de l'Église, Cœur immaculé…) **n'est pas** dans ces fichiers : le code la place. Les lister dans `SOURCES.md` § « Fêtes mobiles laissées au code ». |
| `rank` | 1969 : `SOLEMNITY`, `FEAST`, `MEMORIAL`, `OPTIONAL_MEMORIAL`. 1962 : `I`, `II`, `III`, `IV` (classes), `COMMEMORATION` (simple commémoraison), `VIGIL_I`, `VIGIL_II`, `VIGIL_III`. |
| `colors` | Une ou plusieurs parmi `WHITE`, `RED`, `GREEN`, `VIOLET`, `ROSE`, `BLACK`, `GOLD` ; la première est la couleur principale. |
| `title` | `fr` obligatoire, `la` si la source le donne. Français usuel des calendriers liturgiques francophones (« Sainte Agnès, vierge et martyre »). |
| `lord` | `true` pour une fête du Seigneur (1969 : degré 5 des NU, qui l'emporte sur un dimanche du temps ordinaire ; 1962 : fête du Seigneur de IIᵉ classe, qui l'emporte sur un dimanche de IIᵉ classe). |
| `validFrom`, `validUntil` | Année civile incluse, ou `null`. Ex. Newman : `validFrom: 2026`. |
| `proper` | 1962 seulement : clé du propre de messe dans Divinum Officium (`Sancti/03-19`), utilisée par le missel de la phase 5. `null` en 1969. |
| `commemorations` | 1962 seulement : commémoraisons attachées à cette date, dans l'ordre des oraisons, chacune `{ "id", "title": {fr, la}, "proper" }`. |
| `saints` | Identifiants de saints (`SaintId`), facultatifs. |
| `remove` | Dans un fichier de pays seulement : `true` supprime l'entrée générale de même `id` (avec seulement `id` et `date`). Une entrée de pays de même `id` **remplace** l'entrée générale. |
| `obligation` | Dans un fichier de pays seulement : `true` si c'est une fête d'obligation dans ce pays. Pour la France : Noël, Ascension, Assomption, Toussaint (rapport 05, §2.1.1). |
| `patron` | Dans un fichier de pays : `PRINCIPAL` ou `SECONDARY` (degrés 4 et 8 ou 11 des NU). |

Tri : par `date`, puis par `id`. JSON canonique de la chaîne (UTF-8, NFC, indentation de 2 espaces, clés dans l'ordre ci-dessus, champs `null`, `false` ou vides omis).

## Oracles (`content/golden/calendar/`)

Fichiers JSONL produits par d'autres moteurs, une ligne par jour, triés par date, qui servent aux tests G3 :

```json
{"date":"2026-10-11","id":"ordinary_time_28_sunday","rank":"SUNDAY","colors":["GREEN"],"optional":[],"sundayCycle":"A","weekdayCycle":"II","psalterWeek":4}
{"date":"2026-10-11","proper":"Tempora/Pent19-0","rank":"II","colors":["GREEN"],"commemorations":[]}
```

- 1969 : `id` (slug romcal), `rank` (`SOLEMNITY`, `FEAST`, `MEMORIAL`, `OPTIONAL_MEMORIAL`, `SUNDAY`, `WEEKDAY`), `colors`, `optional` (identifiants des mémoires facultatives), cycles.
- 1962 : `proper` (clé de propre de Divinum Officium : `Tempora/…` ou `Sancti/…`), `rank` (`I` à `IV`), `colors`, `commemorations` (clés de propre).
- Chaque fichier a un en-tête `<nom>.meta.json` : moteur, commit ou version, commande, date, plage d'années.
- Une divergence connue entre un oracle et notre moteur va dans `content/golden/calendar/known-divergences.md`, justifiée, jamais acceptée en silence.
