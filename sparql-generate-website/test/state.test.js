// Unit tests of the state of the playground: node --test test/
import assert from 'node:assert/strict';
import { test } from 'node:test';
import LZString from 'lz-string';
import {
  emptyState, exampleLabel, fromFragment, fromRequest, guessMediaType, orderExamples, toFragment, toMessage, validate,
} from '../src/state.js';

test('a link restores the query, its documents, queries and graphs', () => {
  const state = emptyState();
  state.namedqueries.push({ uri: 'http://example.org/q', mediatype: 'application/vnd.sparql-generate', string: 'GENERATE {} WHERE {}' });
  state.namedgraphs.push({ uri: 'http://example.org/g', string: '<a> <b> <c> .' });
  state.defaultgraph = '<s> <p> <o> .';
  state.stream = true;
  const restored = fromFragment('#' + toFragment(state));
  assert.deepEqual(toMessage(restored), toMessage(state));
});

test('the links of the former playground and of the documentation are read', () => {
  const message = {
    readme: '<p>Example <code>x</code></p>', defaultquery: 'GENERATE {} WHERE {}', namedqueries: [],
    defaultgraph: '', namedgraphs: [], documentset: [{ uri: 'http://example.org/d', mediatype: 'text/csv', string: 'a,b' }],
    stream: false, loglevel: 3,
  };
  const state = fromFragment('#msg=' + LZString.compressToEncodedURIComponent(JSON.stringify(message)));
  assert.equal(state.defaultquery, message.defaultquery);
  assert.equal(state.documentset[0].mediatype, 'text/csv');
  assert.equal(state.readme, 'Example x');
  assert.equal(state.readmeIsHtml, false);
});

test('a broken link gives null', () => {
  assert.equal(fromFragment('#msg=not-lz'), null);
  assert.equal(fromFragment('#ex=example/generate/00-Source'), null);
});

test('the message has the fields of the server request', () => {
  const message = toMessage(fromRequest({ defaultquery: 'q', namedqueries: [{ uri: 'u', string: 's' }] }));
  assert.deepEqual(Object.keys(message).sort(),
    ['debugTemplate', 'defaultgraph', 'defaultquery', 'documentset', 'loglevel', 'namedgraphs', 'namedqueries', 'stream']);
  assert.equal(message.namedqueries[0].mediatype, 'application/vnd.sparql-generate');
});

test('duplicate IRIs and invalid media types are errors', () => {
  const state = emptyState();
  state.documentset.push({ ...state.documentset[0] });
  state.documentset.push({ uri: 'http://example.org/x', mediatype: 'csv', string: '' });
  const messages = validate(state).map((e) => e.message);
  assert.ok(messages.some((m) => m.includes('same IRI')));
  assert.ok(messages.some((m) => m.includes('media type is invalid')));
  assert.deepEqual(validate(emptyState()), []);
});

test('the tutorial steps come first, by query form', () => {
  const order = orderExamples({ generate: ['Example-CSV', '01-Functions', '00-Source'], select: ['00-Select'], template: ['00-Template'] })
    .map((e) => e.id);
  assert.deepEqual(order, ['example/generate/00-Source', 'example/generate/01-Functions', 'example/generate/Example-CSV',
    'example/select/00-Select', 'example/template/00-Template']);
  assert.equal(exampleLabel('05-NestedGenerate'), '05 Nested Generate');
  assert.equal(guessMediaType('a.json', ''), 'application/json');
});
