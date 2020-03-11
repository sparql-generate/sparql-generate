package fr.emse.ci.sparqlext.json;

///*
// * Copyright 2016 Ecole des Mines de Saint-Etienne.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package fr.emse.ci.sparqlext.iterator.library;
//
//import fr.emse.ci.sparqlext.SPARQLExt;
//import fr.emse.ci.sparqlext.function.library.FUN_JSONPath;
//import fr.emse.ci.sparqlext.stream.LookUpRequest;
//import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
//import com.jayway.jsonpath.DocumentContext;
//import fr.emse.ci.sparqlext.utils.LogUtils;
//import fr.emse.ci.sparqlext.iterator.IteratorStreamFunctionBase;
//import fr.emse.ci.sparqlext.utils.ContextUtils;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.charset.StandardCharsets;
//
//import java.util.*;
//import java.util.function.Consumer;
//import org.apache.commons.io.IOUtils;
//import org.apache.jena.atlas.web.TypedInputStream;
//import org.apache.jena.graph.Node;
//import org.apache.jena.riot.SysRIOT;
//
//import org.apache.jena.sparql.expr.ExprEvalException;
//import org.apache.jena.sparql.expr.ExprList;
//import org.apache.jena.sparql.expr.NodeValue;
//import org.jsfr.json.JsonPathListener;
//import org.jsfr.json.JsonSurfer;
//import org.jsfr.json.JsonSurferGson;
//import org.jsfr.json.ParsingContext;
//import org.jsfr.json.compiler.JsonPathCompiler;
//import org.jsfr.json.path.JsonPath;
//import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;
//
///**
// * Iterator function
// * <a href="http://w3id.org/sparql-generate/iter/JSONSurfer">iter:JSONSurfer</a>
// * extracts a list of sub-JSON documents of a JSON document, according to a
// * forward-only JSONPath expression. See https://github.com/jsurfer/JsonSurfer
// * for the JSONPath syntax specification.
// *
// * <p>
// * See
// * <a href="https://w3id.org/sparql-generate/playground.html#ex=example/generate/Example-JSONSurfer">Live
// * example</a></p>
// *
// * <ul>
// * <li>Param 1: (json): the URI of the JSON document (a URI), or the JSON object
// * itself (a String);</li>
// * <li>Param 2: (jsonPath) the JSONPath query;</li>
// * <li>Param 3 .. N : (auxJsonPath ... ) other JSONPath queries, which will be
// * executed over the results of the execution of jsonPath, and provide one
// * result each.</li>
// * </ul>
// *
// * The following variables may be bound:
// *
// * <ul>
// * <li>Output 1: (string) sub-JSON document, encoded in a string literal;</li>
// * <li>Output 2 .. N-1: result of the execution of the auxiliary JsonPath
// * queries on Output 1, encoded as a boolean, float, double, integer, string, as
// * it best fits;</li>
// * <li>Output N: (integer) the position of the result in the list;</li>
// * <li>Output N+1: (boolean) true if this result has a next result in the
// * list.</li>
// * </ul>
// *
// * Output 2 and 3 can be used to generate RDF lists from the input, but the use
// * of keyword <code>LIST( ?var )</code> as the object of a triple pattern covers
// * most cases more elegantly.
// *
// * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
// */
//public class ITER_JSONSurfer extends IteratorStreamFunctionBase {
//
//    private static final Logger LOG = LoggerFactory.getLogger(ITER_JSONSurfer.class);
//
//    public static final String URI = SPARQLExt.ITER + "JSONSurfer";
//
//    private static final FUN_JSONPath function = new FUN_JSONPath();
//
//    private static final JsonSurfer surfer = JsonSurferGson.INSTANCE;
//
//    @Override
//    public void checkBuild(ExprList args) {
//        Objects.nonNull(args);
//    }
//
//    @Override
//    public void exec(
//            final List<NodeValue> args,
//            final Consumer<List<List<NodeValue>>> collectionListNodeValue) {
//        Objects.nonNull(args);
//        if (args.size() < 2) {
//            LOG.debug("Expecting at least two arguments.");
//            throw new ExprEvalException("Expecting at least two arguments.");
//        }
//
//        final NodeValue json = args.remove(0);
//        final NodeValue jsonquery = args.remove(0);
//        try (InputStream jsonInput = getInputStream(json)) {
//
//            final JsonPath compiledPath = getCompiledPath(jsonquery);
//            final int rowsInABatch = getRowsInABatch(args);
//            final com.jayway.jsonpath.JsonPath[] subqueries = getSubQueries(args);
//
//            final Listener listener = new Listener(collectionListNodeValue, rowsInABatch, subqueries);
//            surfer.configBuilder()
//                    .bind(compiledPath, listener)
//                    .buildAndSurf(jsonInput);
//            LOG.debug("built and finished surfing");
//            listener.stop();
//        } catch (ExprEvalException | IOException ex) {
//            if (LOG.isDebugEnabled()) {
//                Node compressed = LogUtils.compress(json.asNode());
//                LOG.debug("No evaluation for " + compressed + ", " + jsonquery);
//            }
//            throw new ExprEvalException("No evaluation for " + jsonquery);
//        } catch (Exception ex) {
//            if (LOG.isDebugEnabled()) {
//                Node compressed = LogUtils.compress(json.asNode());
//                LOG.debug("No evaluation for " + compressed + ", " + jsonquery);
//            }
//            throw new ExprEvalException("No evaluation for " + jsonquery);
//        }
//    }
//
//    private InputStream getInputStream(NodeValue json) throws ExprEvalException, IOException {
//        if (json.isString()) {
//            return IOUtils.toInputStream(json.asString(), StandardCharsets.UTF_8);
//        } else if (json.isLiteral() && json.asNode().getLiteralDatatypeURI().startsWith("http://www.iana.org/assignments/media-types/")) {
//            return IOUtils.toInputStream(json.asNode().getLiteralLexicalForm(), StandardCharsets.UTF_8);
//        } else if (json.isIRI()) {
//            String csvPath = json.asNode().getURI();
//            LookUpRequest req = new LookUpRequest(csvPath, "application/json");
//            final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) getContext().get(SysRIOT.sysStreamManager);
//            Objects.requireNonNull(sm);
//            TypedInputStream tin = sm.open(req);
//            if (tin == null) {
//                String message = String.format("Could not look up json document %s", csvPath);
//                LOG.warn(message);
//                throw new ExprEvalException(message);
//            }
//            return tin.getInputStream();
//        } else {
//            String message = String.format("First argument must be a URI or a String");
//            LOG.warn(message);
//            throw new ExprEvalException(message);
//        }
//    }
//
//    private JsonPath getCompiledPath(NodeValue jsonquery) {
//        if (!jsonquery.isString()) {
//            LOG.debug("Second argument must be a String.");
//            throw new ExprEvalException("Second argument must be a String.");
//        }
//        return JsonPathCompiler.compile(jsonquery.getString());
//    }
//
//    private int getRowsInABatch(List<NodeValue> args) {
//        int rowsInABatch;
//        if (!args.isEmpty() && args.get(0).isInteger()) {
//            int batch = args.remove(0).getInteger().intValue();
//            if (batch > 0) {
//                rowsInABatch = batch;
//                LOG.trace("  With batches of " + rowsInABatch + " results.");
//            } else {
//                rowsInABatch = 0;
//                LOG.trace("  As one batch");
//            }
//        } else {
//            rowsInABatch = 0;
//            LOG.trace("  As one batch");
//        }
//        return rowsInABatch;
//    }
//
//    private com.jayway.jsonpath.JsonPath[] getSubQueries(List<NodeValue> args) {
//        com.jayway.jsonpath.JsonPath[] subqueries = new com.jayway.jsonpath.JsonPath[args.size()];
//        for (int i = 0; i < args.size(); i++) {
//            final NodeValue subquery = args.get(i);
//            if (!subquery.isString()) {
//                LOG.debug("Sub-JSONPath query " + i + " must be a String.");
//                throw new ExprEvalException("Sub-JSONPath query " + i + " must be a String.");
//            }
//            subqueries[i] = com.jayway.jsonpath.JsonPath.compile(subquery.getString());
//        }
//        return subqueries;
//    }
//
//    private class Listener implements JsonPathListener {
//
//        private final Consumer<List<List<NodeValue>>> collectionListNodeValue;
//        private final int rowsInABatch;
//        private final com.jayway.jsonpath.JsonPath[] subqueries;
//
//        public Listener(
//                final Consumer<List<List<NodeValue>>> collectionListNodeValue,
//                int rowsInABatch,
//                final com.jayway.jsonpath.JsonPath[] subqueries) {
//            this.collectionListNodeValue = collectionListNodeValue;
//            this.rowsInABatch = rowsInABatch;
//            this.subqueries = subqueries;
//        }
//
//        private boolean initialized = false;
//        private int rowsInThisBatch = 0;
//        private int total = 0;
//        List<List<NodeValue>> nodeValues = new ArrayList<>();
//
//        @Override
//        public void onValue(Object value, ParsingContext context) {
//            LOG.info("value: " + function.nodeForObject(value));
//            if (!initialized) {
//                initialized = true;
//                ContextUtils.addTaskOnClose(getContext(), context::stop);
//            }
//            List<NodeValue> list = new ArrayList<>(subqueries.length);
//            list.add(function.nodeForObject(value));
//            DocumentContext doc = com.jayway.jsonpath.JsonPath.parse(value);
//            for (com.jayway.jsonpath.JsonPath subquery : subqueries) {
//                try {
//                    Object subvalue = doc.limit(1).read(subquery);
//                    list.add(function.nodeForObject(subvalue));
//                } catch (Exception ex) {
//                    LOG.debug("No evaluation for " + value + ", " + subquery);
//                    list.add(null);
//                }
//            }
//            nodeValues.add(list);
//            rowsInThisBatch++;
//            total++;
//            if (rowsInABatch > 0 && rowsInThisBatch >= rowsInABatch) {
//                LOG.trace("New batch of " + rowsInThisBatch + " rows, " + total + " total");
//                send();
//                rowsInThisBatch = 0;
//            }
//        }
//
//        private void send() {
//            collectionListNodeValue.accept(nodeValues);
//            nodeValues = new ArrayList<>();
//        }
//
//        private void stop() {
//            if (rowsInThisBatch > 0) {
//                send();
//            }
//        }
//
//    }
//}
