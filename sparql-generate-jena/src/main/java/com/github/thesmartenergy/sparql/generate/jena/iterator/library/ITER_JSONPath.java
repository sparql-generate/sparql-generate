/*
 * Copyright 2016 Ecole des Mines de Saint-Etienne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.thesmartenergy.sparql.generate.jena.iterator.library;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FUN_JSONPath;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.github.thesmartenergy.sparql.generate.jena.stream.LookUpRequest;
import com.github.thesmartenergy.sparql.generate.jena.stream.SPARQLGenerateStreamManager;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.util.*;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.TypedInputStream;

import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/JSONPath">iter:JSONPath</a>
 * extracts a list of sub-JSON documents of a JSON document, according to a
 * JSONPath expression. See https://github.com/json-path/JsonPath for the
 * JSONPath syntax specification.
 *
 * <ul>
 * <li>Param 1: (json): the URI of the JSON document (a URI), or the JSON object
 * itself (a String);</li>
 * <li>Param 2: (jsonPath) the JSONPath query;</li>
 * <li>Param 3 .. N : (auxJsonPath ... ) other JSONPath queries, which will be
 * executed over the results of the execution of jsonPath, and provide one
 * result each.</li>
 * </ul>
 *
 * The following variables may be bound:
 *
 * <ul>
 * <li>Output 1: (string) sub-JSON document, encoded in a string literal;</li>
 * <li>Output 2 .. N-1: result of the execution of the auxiliary JsonPath
 * queries on Output 1, encoded as a boolean, float, double, integer, string, as
 * it best fits;</li>
 * <li>Output N: (integer) the position of the result in the list;</li>
 * <li>Output N+1: (boolean) true if this result has a next result in the
 * list.</li>
 * </ul>
 *
 * Output 2 and 3 can be used to generate RDF lists from the input, but the use
 * of keyword LIST( ?var ) as the object of a triple pattern covers most cases
 * more elegantly.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class ITER_JSONPath extends IteratorFunctionBase {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_JSONPath.class);

    public static final String URI = SPARQLGenerate.ITER + "JSONPath";

    private static final String JSON_URI = "http://www.iana.org/assignments/media-types/application/json";

    private final static NodeValue TRUE = new NodeValueBoolean(true);

    private final static NodeValue FALSE = new NodeValueBoolean(false);

    private final FUN_JSONPath function = new FUN_JSONPath();

    private static Gson GSON = new Gson();

    @Override
    public Collection<List<NodeValue>> exec(List<NodeValue> args) {
        if (args.size() < 2) {
            LOG.debug("Expecting at least two arguments.");
            throw new ExprEvalException("Expecting at least two arguments.");
        }
        final NodeValue json = args.get(0);
        if (!json.isIRI() && !json.isString() && !json.asNode().isLiteral()) {
            LOG.debug("First argument must be a URI or a String.");
            throw new ExprEvalException("First argument must be a URI or a String.");
        }
        if (!json.isIRI() && json.getDatatypeURI() != null
                && !json.getDatatypeURI().equals(JSON_URI)
                && !json.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The datatype of the first argument should be"
                    + " <" + JSON_URI + "> or"
                    + " <http://www.w3.org/2001/XMLSchema#string>. Got "
                    + json.getDatatypeURI());
        }
        Configuration conf = Configuration.builder()
                .options(Option.ALWAYS_RETURN_LIST).build();

        String jsonString = getString(json);

        final NodeValue jsonquery = args.get(1);
        if (!jsonquery.isString()) {
            LOG.debug("Second argument must be a String.");
            throw new ExprEvalException("Second argument must be a String.");
        }

        String[] subqueries = new String[args.size() - 2];
        if (args.size() > 2) {
            for (int i = 2; i < args.size(); i++) {
                final NodeValue subquery = args.get(i);
                if (!subquery.isString()) {
                    LOG.debug("Argument " + i + " must be a String.");
                    throw new ExprEvalException("Argument " + i + " must be a String.");
                }
                subqueries[i - 2] = subquery.getString();
            }
        }

        try {
            List<Object> values = JsonPath
                    .using(conf)
                    .parse(jsonString)
                    .read(jsonquery.getString());
            int size = values.size();
            Collection<List<NodeValue>> collectionNodeValues = new HashSet<>(size);
            for (int i = 0; i < size; i++) {
                Object value = values.get(i);
                List<NodeValue> nodeValues = new ArrayList<>(args.size() + 1);
                nodeValues.add(function.nodeForObject(value));
                for (String subquery : subqueries) {
                    try {
                        Object subvalue = JsonPath.parse(value)
                                .limit(1).read(subquery);
                        nodeValues.add(function.nodeForObject(subvalue));
                    } catch (Exception ex) {
                        LOG.debug("No evaluation for " + value + ", " + subquery, ex);
                        nodeValues.add(null);
                    }
                }
                nodeValues.add(new NodeValueInteger(i));
                nodeValues.add((i == size - 1) ? FALSE : TRUE);
                collectionNodeValues.add(nodeValues);
            }
            return collectionNodeValues;
        } catch (Exception ex) {
            LOG.debug("No evaluation for " + json + ", " + jsonquery, ex);
            throw new ExprEvalException("No evaluation for " + json + ", " + jsonquery);
        }
    }

    private String getString(NodeValue json) throws ExprEvalException {
        if (json.isString()) {
            return json.getString();
        } else if (json.isLiteral() && json.asNode().getLiteralDatatypeURI().equals(JSON_URI)) {
            return json.asNode().getLiteralLexicalForm();
        } else if (!json.isIRI()) {
            String message = String.format("First argument must be a URI or a String");
            LOG.warn(message);
            throw new ExprEvalException(message);
        }
        String jsonPath = json.asNode().getURI();
        LookUpRequest req = new LookUpRequest(jsonPath, "application/json");
        SPARQLGenerateStreamManager sm = getContext().getStreamManager();
        Objects.requireNonNull(sm);
        TypedInputStream tin = sm.open(req);
        if (tin == null) {
            String message = String.format("Could not look up json document %s", jsonPath);
            LOG.warn(message);
            throw new ExprEvalException(message);
        }
        try {
            return IOUtils.toString(tin.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ExprEvalException("IOException while looking up json document " + jsonPath, ex);
        }
    }

    @Override
    public void checkBuild(ExprList args) {
        Objects.nonNull(args);
    }
}
