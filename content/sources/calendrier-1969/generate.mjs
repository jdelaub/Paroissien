// Extrait de romcal (MIT) les définitions du calendrier romain général et du
// propre de France, puis calcule le calendrier de France jour par jour.
//
//   node content/sources/calendrier-1969/generate.mjs <dépôt romcal construit> <dossier de sortie> [première année] [dernière année]
//
// Le dépôt romcal doit avoir été construit (`npm ci && npm run build`) : on lit
// `rites/roman1969/dist/`. Sorties, toutes brutes (identifiants, rangs et couleurs
// de romcal, sans conversion) :
//   romcal-defs.json         définitions (général, France, temporal) et config
//   romcal-france-days.jsonl une ligne par jour civil, toutes les célébrations
//   romcal-datefns.json      dates rendues par les fonctions `dateFn` du sanctoral
// `convert.py` les transforme ensuite au format de content/schema/calendar-v1.md.

import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const [repoArg, outArg, fromArg = '2025', toArg = '2045'] = process.argv.slice(2);
if (!repoArg || !outArg) {
  console.error('usage : generate.mjs <dépôt romcal> <dossier de sortie> [première année] [dernière année]');
  process.exit(2);
}
const repo = resolve(repoArg);
const out = resolve(outArg);
const firstYear = Number(fromArg);
const lastYear = Number(toArg);
const dist = join(repo, 'rites/roman1969/dist');
const load = (p) => import(pathToFileURL(join(dist, p)).href);

const { Romcal } = await load('esm/romcal.js');
const general = await load('bundles/general-roman/esm/index.js');
const france = await load('bundles/france/esm/index.js');

const git = (...args) => execFileSync('git', ['-C', repo, ...args], { encoding: 'utf8' }).trim();
const source = {
  version: Romcal.getVersion(),
  commit: git('rev-parse', 'HEAD'),
  commitDate: git('show', '-s', '--format=%cI', 'HEAD'),
  branch: git('rev-parse', '--abbrev-ref', 'HEAD'),
  node: process.version,
};

// Une instance par calendrier et par langue ; les noms viennent de la locale du bundle.
const instance = (bundle) => new Romcal({ localizedCalendar: bundle, scope: 'gregorian' });

async function definitions(bundles) {
  const byLocale = {};
  for (const [locale, bundle] of Object.entries(bundles)) {
    byLocale[locale] = await instance(bundle).getAllDefinitions();
  }
  const ref = byLocale.fr;
  return Object.values(ref).map((d) => ({
    id: d.id,
    fromCalendarId: d.fromCalendarId,
    layers: d.input.map((i) => i.fromCalendarId),
    dateDef: d.dateDef,
    dateExceptions: d.dateExceptions,
    alternativeTransferDateDefs: d.alternativeTransferDateDefs,
    precedence: d.precedence,
    rank: d.rank,
    colors: d.colors,
    isOptional: d.isOptional,
    isHolyDayOfObligation: d.isHolyDayOfObligation,
    allowSimilarRankItems: d.allowSimilarRankItems,
    properCycle: d.cycles.properCycle,
    i18nDef: d.i18nDef,
    titles: d.titles,
    martyrology: d.martyrology.map((m) => m.id),
    calendarMetadata: d.calendarMetadata,
    seasons: d.seasons,
    // Le bundle complète une locale incomplète par l'anglais : `en` sert à le détecter.
    name: Object.fromEntries(Object.entries(byLocale).map(([l, defs]) => [l, defs[d.id]?.name ?? null])),
  }));
}

const generalDefs = await definitions({
  fr: general.GeneralRoman_Fr,
  la: general.GeneralRoman_La,
  en: general.GeneralRoman_En,
});
const franceDefs = await definitions({
  fr: france.France_Fr,
  la: france.France_La,
  en: france.France_En,
});

const rcFrance = instance(france.France_Fr);
const rcGeneral = instance(general.GeneralRoman_Fr);

mkdirSync(out, { recursive: true });
writeFileSync(
  join(out, 'romcal-defs.json'),
  JSON.stringify(
    {
      source,
      config: { general: rcGeneral.config, france: rcFrance.config },
      general: generalDefs,
      france: franceDefs,
    },
    null,
    1
  ) + '\n'
);

// Dates rendues par les fonctions nommées du sanctoral (Présentation, Assomption…),
// pour vérifier la date nominale reprise dans general.json.
const dateFns = {};
for (const d of [...generalDefs, ...franceDefs]) {
  const fn = d.dateDef?.dateFn;
  if (!fn || d.fromCalendarId === 'ProperOfTime' || dateFns[fn]) continue;
  dateFns[fn] = {};
  for (let y = firstYear; y <= lastYear; y += 1) {
    const date = rcFrance.dates(y)[fn](...(d.dateDef.dateArgs ?? []));
    dateFns[fn][y] = date ? date.toISOString().slice(0, 10) : null;
  }
}
writeFileSync(join(out, 'romcal-datefns.json'), JSON.stringify(dateFns, null, 1) + '\n');

// Calendrier de France, années civiles (scope « gregorian »).
const lines = [];
for (let y = firstYear; y <= lastYear; y += 1) {
  const cal = await rcFrance.generateCalendar(y);
  for (const [date, items] of Object.entries(cal).sort(([a], [b]) => a.localeCompare(b))) {
    lines.push(
      JSON.stringify({
        date,
        items: items.map((x) => ({
          id: x.id,
          rank: x.rank,
          precedence: x.precedence,
          colors: x.colors,
          isOptional: x.isOptional,
          isHolyDayOfObligation: x.isHolyDayOfObligation,
          fromCalendarId: x.fromCalendarId,
          cycles: x.cycles,
          name: x.name,
        })),
      })
    );
  }
}
writeFileSync(join(out, 'romcal-france-days.jsonl'), lines.join('\n') + '\n');

console.log(
  `romcal ${source.version} (${source.commit.slice(0, 12)}) : ${generalDefs.length} définitions générales, ` +
    `${franceDefs.length} pour la France, ${lines.length} jours de ${firstYear} à ${lastYear} → ${out}`
);
