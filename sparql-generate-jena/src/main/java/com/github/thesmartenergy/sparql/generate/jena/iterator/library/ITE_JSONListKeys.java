/*
 * Copyright 2016 ITEA 12004 SEAS Project.
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
import org.apache.log4j.Logger;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase1;

/**
 * A SPARQL Iterator function that lists the keys of a JSON object represented by a
 * JSON document. The Iterator function URI is
 * {@code <http://w3id.org/sparql-generate/ite/JSON_Path_jayway>}.
 * It takes one parameter,{@param json} as input which is a RDF Literal with datatype URI
 * {@code <urn:iana:mime:application/json>}</li>
 * </ul>
 * and returns a list of {@code xsd:string} Literals.
 *
 * @author maxime.lefrancois
 */
public class ITE_JSONListKeys extends IteratorFunctionBase1 {

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

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(ITE_JSONListKeys.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITE + "JSONListKeys";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "urn:iana:mime:application/json";

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NodeValue> exec(NodeValue json) {
        if (json.getDatatypeURI() == null
                && datatypeUri == null
                || json.getDatatypeURI() != null
                && !json.getDatatypeURI().equals(datatypeUri)
                && !json.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST have been"
                    + " <" + datatypeUri + "> or"
                    + " <http://www.w3.org/2001/XMLSchema#string>."
                    + " Got <" +json.getDatatypeURI()+">"
                    + " Returning null.");
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
        } catch (Exception e) {
            throw new ExprEvalException("FunctionBase: no evaluation", e);
        }
    }
}
