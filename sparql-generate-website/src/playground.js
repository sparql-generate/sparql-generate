// The SPARQL-Generate playground: CodeMirror 6 editors, the examples and the
// tutorial of sparql-generate-server, and its WebSocket endpoint transformStream.
//
// Server API (sparql-generate-server, unchanged since 2.0):
//   GET api/example/{generate|select|template}       -> ["00-Source", ...]
//   GET api/example/{generate|select|template}/{id}  -> a request: readme, defaultquery,
//       namedqueries [{uri, mediatype, string}], defaultgraph, namedgraphs [{uri, string}],
//       documentset [{uri, mediatype, string}], stream, debugTemplate, loglevel
//   WebSocket transformStream: send a request (or {cancel: true}); receive arrays of
//       {clear: true} | {log: "..."} | {type: 0 GENERATE | 1 SELECT | 2 TEMPLATE, result: "..."}
//
// Links: #ex=example/generate/00-Source loads an example of the server;
// #msg=... (lz-string) restores a query with its documents and graphs.

import { basicSetup } from 'codemirror';
import { EditorView, keymap } from '@codemirror/view';
import { Compartment, EditorState, Prec } from '@codemirror/state';
import { StreamLanguage } from '@codemirror/language';
import { oneDark } from '@codemirror/theme-one-dark';
import { turtle } from '@codemirror/legacy-modes/mode/turtle';
import { json } from '@codemirror/legacy-modes/mode/javascript';
import { xml, html } from '@codemirror/legacy-modes/mode/xml';
import { sparqlGenerate } from './vendor/codemirror-sparql-generate.mjs';
import {
  emptyState, exampleLabel, fromFragment, fromRequest, guessMediaType, orderExamples,
  QUERY_MEDIA_TYPE, toFragment, toMessage, validate,
} from './state.js';

const STORAGE_KEY = 'sparql-generate-playground';
const LEVELS = ['FATAL', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'];
const AUTO_RUN_DELAY = 700;

const languages = {
  query: StreamLanguage.define(sparqlGenerate),
  turtle: StreamLanguage.define(turtle),
  json: StreamLanguage.define(json),
  xml: StreamLanguage.define(xml),
  html: StreamLanguage.define(html),
};

function languageOf(mediatype) {
  const type = (mediatype || '').toLowerCase();
  if (type === QUERY_MEDIA_TYPE) return languages.query;
  if (type.includes('turtle') || type.includes('n-triples')) return languages.turtle;
  if (type.includes('json')) return languages.json;
  if (type.includes('html')) return languages.html;
  if (type.includes('xml')) return languages.xml;
  return [];
}

const dark = window.matchMedia?.('(prefers-color-scheme: dark)').matches;
const theme = dark ? [oneDark] : [];

const $ = (id) => document.getElementById(id);

let state = emptyState();
let examples = [];
let currentCollection = 'documentset';
let socket = null;
let socketOpen = false;
let pending = null;
let autoTimer = null;
let logLines = [];

// ---------------------------------------------------------------- storage

function save() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch {
    // private mode or full storage: the state is not kept, nothing else breaks
  }
}

function restore() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
    if (saved && typeof saved === 'object') {
      return fromRequest(saved, { readmeIsHtml: saved.readmeIsHtml === true, example: saved.example ?? null });
    }
  } catch {
    // ignore a broken or missing saved state
  }
  return null;
}

// ---------------------------------------------------------------- editors

function runKeymap() {
  return Prec.highest(keymap.of([{ key: 'Mod-Enter', run: () => { run(); return true; } }]));
}

function makeEditor(parent, doc, language, onChange, { readOnly = false } = {}) {
  const languageCompartment = new Compartment();
  const view = new EditorView({
    parent,
    state: EditorState.create({
      doc,
      extensions: [
        basicSetup,
        runKeymap(),
        languageCompartment.of(language),
        theme,
        EditorView.lineWrapping,
        EditorState.readOnly.of(readOnly),
        EditorView.updateListener.of((update) => {
          if (update.docChanged && onChange) onChange(update.state.doc.toString());
        }),
      ],
    }),
  });
  view.setLanguage = (lang) => view.dispatch({ effects: languageCompartment.reconfigure(lang) });
  return view;
}

function setDoc(view, text) {
  view.dispatch({ changes: { from: 0, to: view.state.doc.length, insert: text } });
}

const queryEditor = makeEditor($('query-editor'), '', languages.query, (text) => {
  state.defaultquery = text;
  changed();
});

const resultEditor = makeEditor($('result-editor'), '', languages.turtle, null, { readOnly: true });

// ---------------------------------------------------------------- collections

function collection(name) {
  if (name === 'namedqueries') return state.namedqueries;
  if (name === 'graphs') return state.namedgraphs;
  return state.documentset;
}

function addItem(name, item) {
  if (name === 'namedqueries') {
    state.namedqueries.push({ uri: item?.uri ?? `http://example.org/query/${state.namedqueries.length + 1}`, mediatype: QUERY_MEDIA_TYPE, string: item?.string ?? 'PREFIX iter: <http://w3id.org/sparql-generate/iter/>\n\nGENERATE <http://example.org/query/1> (?x) {\n}\nWHERE {\n}\n' });
  } else if (name === 'graphs') {
    state.namedgraphs.push({ uri: item?.uri ?? `http://example.org/graph/${state.namedgraphs.length + 1}`, string: item?.string ?? '' });
  } else {
    state.documentset.push({ uri: item?.uri ?? `http://example.org/document/${state.documentset.length + 1}`, mediatype: item?.mediatype ?? 'text/plain', string: item?.string ?? '' });
  }
}

function renderTabs() {
  for (const tab of document.querySelectorAll('.tabs [role="tab"]')) {
    tab.setAttribute('aria-selected', String(tab.dataset.collection === currentCollection));
  }
  $('drop-zone').textContent = {
    documentset: 'Click to add a document, or drop files here',
    namedqueries: 'Click to add a named query, or drop .rqg files here',
    graphs: 'Click to add a named graph, or drop Turtle files here',
  }[currentCollection];
  for (const name of ['documentset', 'namedqueries', 'graphs']) {
    const n = collection(name).length + (name === 'graphs' && state.defaultgraph.trim() ? 1 : 0);
    document.querySelector(`[data-count="${name}"]`).textContent = n ? `(${n})` : '';
  }
}

function itemElement(name, item, index) {
  const element = document.createElement('div');
  element.className = 'item';
  const header = document.createElement('div');
  header.className = 'item-header';

  const uri = document.createElement('input');
  uri.type = 'text';
  uri.className = 'uri';
  uri.value = item.uri;
  uri.setAttribute('aria-label', name === 'graphs' ? 'Graph name' : 'IRI');
  uri.addEventListener('input', () => { item.uri = uri.value; changed(); });
  header.append(uri);

  if (name === 'documentset') {
    const media = document.createElement('input');
    media.type = 'text';
    media.className = 'media';
    media.value = item.mediatype;
    media.setAttribute('aria-label', 'Media type');
    media.addEventListener('input', () => {
      item.mediatype = media.value;
      editor.setLanguage(languageOf(item.mediatype));
      changed();
    });
    header.append(media);
  }

  const toggle = document.createElement('button');
  toggle.type = 'button';
  toggle.className = 'small';
  toggle.textContent = 'Hide';
  toggle.addEventListener('click', () => {
    element.classList.toggle('collapsed');
    toggle.textContent = element.classList.contains('collapsed') ? 'Show' : 'Hide';
  });
  const remove = document.createElement('button');
  remove.type = 'button';
  remove.className = 'small';
  remove.textContent = 'Remove';
  remove.addEventListener('click', () => {
    collection(name).splice(index, 1);
    renderItems();
    changed();
  });
  header.append(toggle, remove);

  const message = document.createElement('div');
  message.className = 'item-message';
  message.dataset.index = String(index);

  const editorParent = document.createElement('div');
  editorParent.className = 'editor';
  const language = name === 'namedqueries' ? languages.query : name === 'graphs' ? languages.turtle : languageOf(item.mediatype);
  const editor = makeEditor(editorParent, item.string, language, (text) => { item.string = text; changed(); });

  element.append(header, message, editorParent);
  return element;
}

function defaultGraphElement() {
  const element = document.createElement('div');
  element.className = 'item';
  const header = document.createElement('div');
  header.className = 'item-header';
  const label = document.createElement('strong');
  label.textContent = 'Default graph (Turtle)';
  header.append(label);
  const editorParent = document.createElement('div');
  editorParent.className = 'editor';
  makeEditor(editorParent, state.defaultgraph, languages.turtle, (text) => {
    state.defaultgraph = text;
    changed();
  });
  element.append(header, editorParent);
  return element;
}

function renderItems() {
  const container = $('items');
  container.replaceChildren();
  if (currentCollection === 'graphs') container.append(defaultGraphElement());
  collection(currentCollection).forEach((item, index) => container.append(itemElement(currentCollection, item, index)));
  renderTabs();
  showValidation();
}

function showValidation() {
  const errors = validate(state).filter((e) => e.collection === currentCollection);
  for (const element of document.querySelectorAll('#items .item-message')) {
    const index = Number(element.dataset.index);
    element.textContent = errors.filter((e) => e.index === index).map((e) => e.message).join(' ');
  }
  return validate(state).length === 0;
}

// ---------------------------------------------------------------- readme

function renderReadme() {
  const readme = $('readme');
  const content = $('readme-content');
  if (!state.readme.trim()) {
    readme.hidden = true;
    return;
  }
  readme.hidden = false;
  $('readme-title').textContent = state.example ? exampleLabel(state.example.split('/').pop()) : 'About this query';
  if (state.readmeIsHtml) {
    // the readme of an example of the server, written by the authors of the examples
    content.innerHTML = state.readme;
  } else {
    const p = document.createElement('p');
    p.textContent = state.readme;
    content.replaceChildren(p);
  }
}

// ---------------------------------------------------------------- state

function load(newState) {
  state = newState;
  setDoc(queryEditor, state.defaultquery);
  $('stream').checked = state.stream;
  $('debugTemplate').checked = state.debugTemplate;
  $('loglevel').value = String(state.loglevel);
  $('examples').value = state.example ?? '';
  updateNavigation();
  renderReadme();
  renderItems();
  clearOutput();
  save();
}

function changed() {
  save();
  renderTabs();
  const valid = showValidation();
  $('run').disabled = !valid;
  if ($('auto').checked && valid) {
    clearTimeout(autoTimer);
    autoTimer = setTimeout(run, AUTO_RUN_DELAY);
  }
}

// ---------------------------------------------------------------- examples and tutorial

async function loadExampleList() {
  const lists = {};
  await Promise.all(['generate', 'select', 'template'].map(async (type) => {
    try {
      const response = await fetch(`api/example/${type}`);
      lists[type] = response.ok ? await response.json() : [];
    } catch {
      lists[type] = [];
    }
  }));
  examples = orderExamples(lists);
  const select = $('examples');
  const groups = { generate: 'GENERATE', select: 'SELECT', template: 'TEMPLATE' };
  for (const type of Object.keys(groups)) {
    const group = document.createElement('optgroup');
    group.label = groups[type];
    for (const example of examples.filter((e) => e.type === type)) {
      const option = document.createElement('option');
      option.value = example.id;
      option.textContent = exampleLabel(example.name);
      group.append(option);
    }
    if (group.children.length) select.append(group);
  }
  select.value = state.example ?? '';
  updateNavigation();
}

async function loadExample(id) {
  setStatus('Loading the example…', 'running');
  try {
    const response = await fetch(`api/${id}`);
    if (!response.ok) throw new Error(`${response.status} ${response.statusText}`);
    const request = await response.json();
    cancel();
    load(fromRequest(request, { readmeIsHtml: true, example: id }));
    setStatus('');
    if (location.hash !== `#ex=${id}`) history.replaceState(null, '', `#ex=${id}`);
  } catch (error) {
    setStatus(`Could not load ${id}: ${error.message}`, 'error');
  }
}

function updateNavigation() {
  const index = examples.findIndex((e) => e.id === state.example);
  $('previous').disabled = index <= 0;
  $('next').disabled = index < 0 ? examples.length === 0 : index >= examples.length - 1;
}

function step(delta) {
  const index = examples.findIndex((e) => e.id === state.example);
  const next = examples[index < 0 ? 0 : index + delta];
  if (next) location.hash = `ex=${next.id}`;
}

// ---------------------------------------------------------------- execution

function socketUrl() {
  const server = new URLSearchParams(location.search).get('server');
  if (server) return server.replace(/\/$/, '') + '/transformStream';
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
  const base = location.pathname.replace(/[^/]*$/, '');
  return `${protocol}//${location.host}${base}transformStream`;
}

function connect() {
  return new Promise((resolve, reject) => {
    if (socket && socketOpen) {
      resolve(socket);
      return;
    }
    socket = new WebSocket(socketUrl());
    socket.addEventListener('open', () => { socketOpen = true; resolve(socket); });
    socket.addEventListener('message', (event) => receive(JSON.parse(event.data)));
    socket.addEventListener('close', () => {
      socketOpen = false;
      if (pending) setStatus('Connection closed', 'error');
      pending = null;
    });
    socket.addEventListener('error', () => {
      socketOpen = false;
      reject(new Error('Could not connect to the server'));
    });
  });
}

async function run() {
  clearTimeout(autoTimer);
  if (!showValidation()) {
    setStatus('Fix the errors of the documents, queries or graphs first', 'error');
    return;
  }
  try {
    const ws = await connect();
    pending = Date.now();
    setStatus('Running…', 'running');
    ws.send(JSON.stringify(toMessage(state)));
  } catch (error) {
    setStatus(error.message, 'error');
  }
}

function cancel() {
  if (socket && socketOpen) socket.send(JSON.stringify({ cancel: true }));
  pending = null;
}

function clearOutput() {
  setDoc(resultEditor, '');
  logLines = [];
  renderLog();
  setStatus('');
}

function appendResult(type, text) {
  if (resultEditor.state.doc.length === 0) {
    resultEditor.setLanguage(type === 0 ? languages.turtle : []);
  }
  const prefix = resultEditor.state.doc.length === 0 ? '' : '\n';
  resultEditor.dispatch({ changes: { from: resultEditor.state.doc.length, insert: prefix + text } });
}

function receive(responses) {
  for (const response of responses) {
    if (response.clear) {
      setDoc(resultEditor, '');
      logLines = [];
    }
    if (typeof response.result === 'string' && response.result !== '') {
      appendResult(response.type, response.result);
    }
    if (typeof response.log === 'string' && response.log !== '') {
      logLines.push(response.log);
    }
  }
  renderLog();
  if (pending) {
    const failed = logLines.some((line) => /^(ERROR|FATAL)\b/.test(line) || /^In this web interface/.test(line));
    const seconds = ((Date.now() - pending) / 1000).toFixed(1);
    setStatus(failed ? `Errors after ${seconds} s, see the log` : `Output after ${seconds} s`, failed ? 'error' : '');
  }
}

function renderLog() {
  const log = $('log');
  log.replaceChildren(...logLines.map((line) => {
    const span = document.createElement('span');
    const level = LEVELS.find((l) => line.startsWith(l));
    if (level) span.className = level;
    span.textContent = line.endsWith('\n') ? line : line + '\n';
    return span;
  }));
  log.scrollTop = log.scrollHeight;
}

function setStatus(text, kind = '') {
  const status = $('status');
  status.textContent = text;
  status.className = 'status' + (kind ? ' ' + kind : '');
}

// ---------------------------------------------------------------- share

function toast(text) {
  const element = document.createElement('div');
  element.className = 'toast';
  element.setAttribute('role', 'status');
  element.textContent = text;
  document.body.append(element);
  setTimeout(() => element.remove(), 3500);
}

async function share() {
  const url = `${location.origin}${location.pathname}#${toFragment(state)}`;
  try {
    await navigator.clipboard.writeText(url);
    toast(url.length > 30000 ? 'Link copied. It is long: some browsers or servers may cut it.' : 'Link copied to the clipboard.');
  } catch {
    window.prompt('Copy this link:', url);
  }
}

// ---------------------------------------------------------------- files

function readFiles(files) {
  for (const file of files) {
    const reader = new FileReader();
    reader.onload = () => {
      const string = String(reader.result);
      if (currentCollection === 'namedqueries') {
        const name = string.match(/(?:GENERATE|TEMPLATE|SELECT|FUNCTION|PERFORM)\s*<([^>]+)>/i);
        addItem('namedqueries', { uri: name ? name[1] : file.name, string });
      } else if (currentCollection === 'graphs') {
        addItem('graphs', { uri: file.name, string });
      } else {
        addItem('documentset', { uri: file.name, mediatype: guessMediaType(file.name, file.type), string });
      }
      renderItems();
      changed();
    };
    reader.readAsText(file);
  }
}

// ---------------------------------------------------------------- wiring

function fromHash() {
  const hash = location.hash;
  if (hash.startsWith('#ex=')) {
    loadExample(decodeURIComponent(hash.slice(4)));
    return true;
  }
  if (hash.startsWith('#msg=')) {
    const decoded = fromFragment(hash);
    if (decoded) {
      cancel();
      load(decoded);
      return true;
    }
    setStatus('The link could not be decoded', 'error');
  }
  return false;
}

$('run').addEventListener('click', run);
$('share').addEventListener('click', share);
$('reset').addEventListener('click', () => {
  history.replaceState(null, '', location.pathname);
  load(emptyState());
});
$('previous').addEventListener('click', () => step(-1));
$('next').addEventListener('click', () => step(1));
$('examples').addEventListener('change', (event) => {
  if (event.target.value) location.hash = `ex=${event.target.value}`;
});
$('stream').addEventListener('change', (event) => { state.stream = event.target.checked; changed(); });
$('debugTemplate').addEventListener('change', (event) => { state.debugTemplate = event.target.checked; changed(); });
$('loglevel').addEventListener('change', (event) => { state.loglevel = Number(event.target.value); changed(); });
$('auto').addEventListener('change', (event) => { if (event.target.checked) changed(); });
for (const tab of document.querySelectorAll('.tabs [role="tab"]')) {
  tab.addEventListener('click', () => { currentCollection = tab.dataset.collection; renderItems(); });
}
const dropZone = $('drop-zone');
dropZone.addEventListener('click', () => { addItem(currentCollection); renderItems(); changed(); });
dropZone.addEventListener('keydown', (event) => {
  if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); dropZone.click(); }
});
dropZone.addEventListener('dragover', (event) => { event.preventDefault(); dropZone.classList.add('over'); });
dropZone.addEventListener('dragleave', () => dropZone.classList.remove('over'));
dropZone.addEventListener('drop', (event) => {
  event.preventDefault();
  dropZone.classList.remove('over');
  readFiles(event.dataTransfer.files);
});
window.addEventListener('hashchange', fromHash);

if (!fromHash()) {
  load(restore() ?? emptyState());
}
loadExampleList();
