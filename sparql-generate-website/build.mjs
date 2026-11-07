// Builds the website of SPARQL-Generate into public/.
//
//   npm ci && npm run build
//
// - pages/*.html are fragments: a first line `<!-- {"title": ..., "active": ...} -->`
//   gives their metadata, and layout.html wraps them;
// - resources/ is copied as is (images, BibTeX files, the 2017 evaluation...);
// - src/playground.js is bundled with CodeMirror 6 by esbuild into js/playground.js;
// - the pages of the former site that moved to the documentation become redirects.
//
// The playground needs the API of sparql-generate-server, deployed at the same
// origin under /sparql-generate/ (api/ and transformStream).

import { build } from 'esbuild';
import { cp, mkdir, readdir, readFile, rm, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = dirname(fileURLToPath(import.meta.url));
const OUT = join(ROOT, 'public');
const DOCS = 'https://sparql-generate.readthedocs.io/en/latest/';

// former pages, now in the documentation
const REDIRECTS = {
  'get-started.html': 'java-api/',
  'get-started-1-x.html': 'archive/get-started-1-x/',
  'language-cli.html': 'cli/',
  'language-api.html': 'web-api/',
  'sublime.html': 'archive/sublime/',
  'iana.html': 'reference/media-type/',
  'evaluation.html': 'archive/evaluation-2017/',
};

const NAV = [
  ['index', 'index.html', 'Overview'],
  ['playground', 'playground.html', 'Playground'],
  ['docs', DOCS, 'Documentation'],
  ['github', 'https://github.com/sparql-generate/sparql-generate', 'GitHub'],
];

function escape(text) {
  return String(text).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]);
}

function nav(active) {
  return NAV.map(([id, href, label]) =>
    `<a href="${escape(href)}"${id === active ? ' aria-current="page"' : ''}>${escape(label)}</a>`).join('\n        ');
}

async function pages(layout, date) {
  const dir = join(ROOT, 'pages');
  for (const name of await readdir(dir)) {
    if (!name.endsWith('.html')) continue;
    const source = await readFile(join(dir, name), 'utf8');
    const match = source.match(/^<!--\s*(\{.*?\})\s*-->\r?\n/s);
    if (!match) throw new Error(`${name}: missing the metadata comment`);
    const meta = JSON.parse(match[1]);
    const head = (meta.styles ?? []).map((s) => `<link rel="stylesheet" href="${escape(s)}">`)
      .concat((meta.scripts ?? []).map((s) => `<script type="module" src="${escape(s)}"></script>`))
      .join('\n    ');
    const html = layout
      .replaceAll('{{title}}', escape(meta.title))
      .replaceAll('{{description}}', escape(meta.description ?? ''))
      .replaceAll('{{bodyClass}}', escape(meta.bodyClass ?? ''))
      .replaceAll('{{docs}}', DOCS)
      .replaceAll('{{date}}', date)
      .replace('{{head}}', head)
      .replace('{{nav}}', nav(meta.active))
      .replace('{{body}}', () => source.slice(match[0].length));
    await writeFile(join(OUT, name), html);
  }
}

async function redirects() {
  for (const [page, target] of Object.entries(REDIRECTS)) {
    const url = DOCS + target;
    await writeFile(join(OUT, page), `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>SPARQL-Generate: moved to the documentation</title>
<link rel="canonical" href="${url}">
<meta http-equiv="refresh" content="0; url=${url}">
</head>
<body><p>This page moved to <a href="${url}">${url}</a>.</p></body>
</html>
`);
  }
}

async function main() {
  const date = new Date().toISOString().slice(0, 10);
  await rm(OUT, { recursive: true, force: true });
  await mkdir(join(OUT, 'js'), { recursive: true });
  await cp(join(ROOT, 'resources'), OUT, { recursive: true });
  await cp(join(ROOT, 'grammar-diagram'), join(OUT, 'grammar-diagram'), { recursive: true });
  await pages(await readFile(join(ROOT, 'layout.html'), 'utf8'), date);
  await redirects();
  await build({
    entryPoints: [join(ROOT, 'src', 'playground.js')],
    bundle: true,
    format: 'esm',
    minify: true,
    sourcemap: true,
    target: ['es2020'],
    outfile: join(OUT, 'js', 'playground.js'),
    logLevel: 'warning',
  });
  console.log(`built ${OUT}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
