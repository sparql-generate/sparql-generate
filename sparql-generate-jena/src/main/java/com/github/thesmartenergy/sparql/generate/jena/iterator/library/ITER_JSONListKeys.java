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
import com.google.gson.Gson;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase1;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/JSONListKeys">iter:JSONListKeys</a>
 * iterates over the keys of a JSON object.
 *
 * <ul>
 * <li>Param 1: (json): a JSON object.</li>
 * </ul>
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class ITER_JSONListKeys extends IteratorFunctionBase1 {

    static {
        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider
                    = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }
        });
    }

    private static final Logger LOG = LoggerFactory.getLogger(ITER_JSONListKeys.class);

    public static final String URI = SPARQLGenerate.ITER + "JSONListKeys";

    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/application/json";

    @Override
    public List<NodeValue> exec(NodeValue json) {
        if (json.getDatatypeURI() != null
                && !json.getDatatypeURI().equals(datatypeUri)
                && !json.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST have been"
                    + " <" + datatypeUri + "> or"
                    + " <http://www.w3.org/2001/XMLSchema#string>."
                    + " Got <" + json.getDatatypeURI() + ">"
            );
        }
        try {
            Gson gson = new Gson();
            Set<String> keys = gson.fromJson(
                    json.asNode().getLiteralLexicalForm(), Map.class).keySet();
            List<NodeValue> nodeValues = new ArrayList<>(keys.size());
            for (String key : keys) {
                NodeValue nodeValue
                        = NodeValue.makeNode(NodeFactory.createLiteral(key));
                nodeValues.add(nodeValue);
            }
            return nodeValues;
        } catch (Exception ex) {
            LOG.debug("No evaluation for " + json, ex);
            throw new ExprEvalException("No evaluation for " + json, ex);
        }
    }
}
