// The state of the playground, and its conversions. No DOM here, so that
// `npm test` can check it with Node.

import LZString from 'lz-string';

export const QUERY_MEDIA_TYPE = 'application/vnd.sparql-generate';

export const EMPTY_QUERY = `PREFIX iter: <http://w3id.org/sparql-generate/iter/>
PREFIX fun: <http://w3id.org/sparql-generate/fn/>
PREFIX schema: <http://schema.org/>
BASE <http://example.org/>

GENERATE {
  <person/{?id}> a schema:Person ;
    schema:name ?name .
}
ITERATOR iter:CSV(<http://example.org/people.csv>, "id", "name") AS ?id ?name
`;

export const EMPTY_DOCUMENT = {
  uri: 'http://example.org/people.csv',
  mediatype: 'text/csv',
  string: 'id,name\n1,Ada Lovelace\n2,Alan Turing\n',
};

/** A fresh state: one query and one document. */
export function emptyState() {
  return {
    readme: '',
    readmeIsHtml: false,
    example: null,
    defaultquery: EMPTY_QUERY,
    namedqueries: [],
    defaultgraph: '',
    namedgraphs: [],
    documentset: [{ ...EMPTY_DOCUMENT }],
    stream: false,
    debugTemplate: false,
    loglevel: 3,
  };
}

function text(value) {
  return typeof value === 'string' ? value : '';
}

function items(list, withMediaType) {
  if (!Array.isArray(list)) return [];
  return list.filter((x) => x && typeof x === 'object').map((x) => {
    const item = { uri: text(x.uri), string: text(x.string) };
    if (withMediaType) item.mediatype = text(x.mediatype);
    return item;
  });
}

/**
 * Normalises a request of the server (an example of `api/example/...`) or a
 * decoded link into a state. `readmeIsHtml` says whether the readme may be
 * rendered as HTML: only the examples of the server are trusted.
 */
export function fromRequest(request, { readmeIsHtml = false, example = null } = {}) {
  const state = emptyState();
  state.readme = text(request.readme);
  state.readmeIsHtml = readmeIsHtml;
  state.example = example;
  state.defaultquery = text(request.defaultquery);
  state.namedqueries = items(request.namedqueries, true).map((q) => ({ ...q, mediatype: q.mediatype || QUERY_MEDIA_TYPE }));
  state.defaultgraph = text(request.defaultgraph);
  state.namedgraphs = items(request.namedgraphs, false);
  state.documentset = items(request.documentset, true);
  if (typeof request.stream === 'boolean') state.stream = request.stream;
  if (typeof request.debugTemplate === 'boolean') state.debugTemplate = request.debugTemplate;
  const level = Number(request.loglevel);
  if (Number.isInteger(level) && level >= 1 && level <= 5) state.loglevel = level;
  return state;
}

/** The message sent to the WebSocket endpoint `transformStream`. */
export function toMessage(state) {
  return {
    defaultquery: state.defaultquery,
    namedqueries: state.namedqueries.map(({ uri, mediatype, string }) => ({ uri, mediatype: mediatype || QUERY_MEDIA_TYPE, string })),
    defaultgraph: state.defaultgraph,
    namedgraphs: state.namedgraphs.map(({ uri, string }) => ({ uri, string })),
    documentset: state.documentset.map(({ uri, mediatype, string }) => ({ uri, mediatype, string })),
    stream: state.stream,
    debugTemplate: state.debugTemplate,
    loglevel: state.loglevel,
  };
}

/** The fragment of a link that restores the state: `#msg=...`, as in the former playground. */
export function toFragment(state) {
  const message = toMessage(state);
  if (state.readme && !state.readmeIsHtml) message.readme = state.readme;
  return 'msg=' + LZString.compressToEncodedURIComponent(JSON.stringify(message));
}

/** Decodes a `#msg=...` fragment, or returns null. */
export function fromFragment(fragment) {
  const value = fragment.replace(/^#/, '');
  if (!value.startsWith('msg=')) return null;
  const json = LZString.decompressFromEncodedURIComponent(value.slice(4));
  if (!json) return null;
  try {
    const request = JSON.parse(json);
    // the readme of a link is shown as text: a link may come from anywhere
    const readme = text(request.readme).replace(/<[^>]*>/g, '');
    return fromRequest({ ...request, readme }, { readmeIsHtml: false });
  } catch {
    return null;
  }
}

/** The error messages of the state: duplicate IRIs, invalid media types, empty IRIs. */
export function validate(state) {
  const errors = [];
  const seen = new Map();
  const check = (collection, index, uri, mediatype) => {
    if (!uri.trim()) {
      errors.push({ collection, index, message: 'The IRI is empty.' });
      return;
    }
    const key = uri + ' ' + mediatype;
    if (seen.has(key)) {
      errors.push({ collection, index, message: `Another ${seen.get(key)} has the same IRI and media type.` });
    } else {
      seen.set(key, collection === 'namedqueries' ? 'query' : collection === 'graphs' ? 'graph' : 'document');
    }
  };
  state.namedqueries.forEach((q, i) => check('namedqueries', i, q.uri, q.mediatype || QUERY_MEDIA_TYPE));
  state.namedgraphs.forEach((g, i) => check('graphs', i, g.uri, 'text/turtle'));
  state.documentset.forEach((d, i) => {
    check('documentset', i, d.uri, d.mediatype);
    if (!/^[\w.+-]+\/[\w.+-]+$/.test(d.mediatype)) {
      errors.push({ collection: 'documentset', index: i, message: 'The media type is invalid.' });
    }
  });
  return errors;
}

/** The media type of a dropped file. */
export function guessMediaType(name, type) {
  if (type) return type;
  const extension = name.toLowerCase().split('.').pop();
  return {
    csv: 'text/csv', tsv: 'text/tab-separated-values', json: 'application/json', geojson: 'application/geo+json',
    xml: 'application/xml', html: 'text/html', htm: 'text/html', ttl: 'text/turtle', md: 'text/markdown',
    txt: 'text/plain', rqg: QUERY_MEDIA_TYPE, cbor: 'application/cbor',
  }[extension] ?? 'text/plain';
}

/** The examples in the order of the tutorial: numbered steps first, GENERATE, SELECT, then TEMPLATE. */
export function orderExamples(lists) {
  const result = [];
  for (const type of ['generate', 'select', 'template']) {
    const names = [...(lists[type] ?? [])].sort((a, b) => {
      const na = /^\d/.test(a), nb = /^\d/.test(b);
      if (na !== nb) return na ? -1 : 1;
      return a.localeCompare(b);
    });
    for (const name of names) result.push({ id: `example/${type}/${name}`, type, name });
  }
  return result;
}

/** A readable label for an example: `03-Iterators` gives `03 Iterators`. */
export function exampleLabel(name) {
  return name.replace(/^Example-/, '').replace(/-/g, ' ').replace(/([a-z])([A-Z])/g, '$1 $2');
}
