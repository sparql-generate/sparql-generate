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
import org.apache.log4j.Logger;

/**
 * A SPARQL Iterator function that extracts a list of sub-JSON documents of a
 * JSON document, according to a JSONPath expression. The Iterator function URI is
 * {@code <http://w3id.org/sparql-generate/ite/JSON_Path_jayway>}.
 * It takes two parameters as input:
 * <ul>
 * <li>a RDF Literal with datatype URI
 * {@code <urn:iana:mime:application/json>}</li>
 * <li>a RDF Literal with datatype {@code xsd:string}</li>
 * </ul>
 * and returns a list of RDF Literal with datatype URI
 * {@code <urn:iana:mime:application/json>}.
 *
 * @author maxime.lefrancois
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
    private static final Logger LOG = Logger.getLogger(ITE_JSONListElement.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITE + "JSONElement";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "urn:iana:mime:application/json";

    /**
     * {@inheritDoc }
     */
    @Override
    public List<NodeValue> exec(NodeValue json, NodeValue jsonquery) {
        if (json.getDatatypeURI() == null
                && datatypeUri == null
                || json.getDatatypeURI() != null
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
            
            int position=0;
            for (Object value : values) {
                RDFDatatype dt = TypeMapper.getInstance().getSafeTypeByName(datatypeUri);
                String jsonstring = gson.toJson(value);
                String structure = "{\"element\":elementValue,\"position\":intPos,\"hasNext\":\"hasNextValue\"}";
                
                
                structure = structure.replaceAll("intPos",String.valueOf(position));
                if (position < values.size()-1){
                    structure = structure.replaceAll("hasNextValue","true");
                } else {
                    structure = structure.replaceAll("hasNextValue","false");
                }
                structure = structure.replaceAll("elementValue",jsonstring);
                jsonstring = structure;
                
                Node node = NodeFactory.createLiteral(jsonstring, dt);
                NodeValue nodeValue = new NodeValueNode(node);
                nodeValues.add(nodeValue);
                
                position++;
            }
            return nodeValues;
        } catch (Exception e) {
            throw new ExprEvalException("FunctionBase: no evaluation", e);
        }
    }
}
