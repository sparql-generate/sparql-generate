import { test } from 'node:test';
import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { readdir, readFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');

test('the built pages keep no template placeholder', async () => {
  execFileSync(process.execPath, ['build.mjs'], { cwd: ROOT, stdio: 'pipe' });
  const out = join(ROOT, 'public');
  const pages = (await readdir(out)).filter((name) => name.endsWith('.html'));
  assert.ok(pages.includes('index.html'));
  for (const name of pages) {
    const html = await readFile(join(out, name), 'utf8');
    assert.doesNotMatch(html, /\{\{[A-Za-z]+\}\}/, name);
  }
  const index = await readFile(join(out, 'index.html'), 'utf8');
  assert.match(index, /href="https:\/\/sparql-generate\.readthedocs\.io\/en\/latest\/"[^>]*>Read the documentation/);
  assert.match(index, /href="https:\/\/sparql-generate\.readthedocs\.io\/en\/latest\/tutorial\/"/);
});
