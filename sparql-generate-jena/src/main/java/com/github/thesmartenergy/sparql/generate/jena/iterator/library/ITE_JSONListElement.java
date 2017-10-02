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
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase2;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * A SPARQL Iterator function that extracts a list of sub-JSON documents of a
 * JSON document, according to a JSONPath expression. The Iterator function URI
 * is {@code <http://w3id.org/sparql-generate/iter/JSONElement>}.
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class ITE_JSONListElement extends IteratorFunctionBase2 {

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
    private static final Logger LOG = LoggerFactory.getLogger(ITE_JSONListElement.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITER + "JSONElement";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/application/json";

    /**
     *
     * @param json a RDF Literal with datatype URI
     * {@code <http://www.iana.org/assignments/media-types/application/json>} or {@code xsd:string}
     * @param jsonquery a RDF Literal with datatype {@code xsd:string}
     * @return a list of RDF Literal with datatype URI
     * {@code <http://www.iana.org/assignments/media-types/application/json>}. For this iterator function, the
     * returned RDF literal has a specific JSON Structure. The structure is
     * {@code {"element":elementValue,"position":position,"hasNext":hasNextValue}}
     * where:
     * <ul>
     * <li>elementValue: a JSON element selected according to JSON query</li>
     * <li>position: an integer corresponding to the location of the JSON
     * element is the list of generated elements according to the JSON
     * query</li>
     * <li>hasNext: a boolean indicating there is another element after the
     * current element </li>
     * </ul>
     */
    @Override
    public List<NodeValue> exec(NodeValue json, NodeValue jsonquery) {
        if (json.getDatatypeURI() != null
                && !json.getDatatypeURI().equals(datatypeUri)
                && !json.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST be"
                    + " <" + datatypeUri + "> or"
                    + " <http://www.w3.org/2001/XMLSchema#string>. Got <"
                    + json.getDatatypeURI() + ">. Returning null.");
        }
        Configuration conf = Configuration.builder()
                .options(Option.ALWAYS_RETURN_LIST).build();

        try {
            List<Object> values = JsonPath
                    .using(conf)
                    .parse(json.asNode().getLiteralLexicalForm())
                    .read(jsonquery.getString());
            List<NodeValue> nodeValues = new ArrayList<>(values.size());
            Gson gson = new Gson();

            int position = 0;
            for (Object value : values) {
                RDFDatatype dt = TypeMapper.getInstance().getSafeTypeByName(datatypeUri);
                String jsonstring = gson.toJson(value);
                String structure = "{\"element\":elementValue,\"position\":intPos,\"hasNext\":hasNextValue}";

                structure = structure.replaceAll("intPos", String.valueOf(position));
                if (position < values.size() - 1) {
                    structure = structure.replaceAll("hasNextValue", "true");
                } else {
                    structure = structure.replaceAll("hasNextValue", "false");
                }
                structure = structure.replaceAll("elementValue", jsonstring);
                jsonstring = structure;

                Node node = NodeFactory.createLiteral(jsonstring, dt);
                NodeValue nodeValue = new NodeValueNode(node);
                nodeValues.add(nodeValue);

                position++;
            }
            return nodeValues;
        } catch (Exception ex) {
            LOG.debug("No evaluation for " + json + ", " + jsonquery , ex);
            throw new ExprEvalException("No evaluation for " + json + ", " + jsonquery , ex);
        }
    }
}
