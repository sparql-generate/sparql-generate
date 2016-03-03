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
package com.github.thesmartenergy.sparql.generate.jena.selector.library;


import com.google.gson.Gson;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import com.github.thesmartenergy.sparql.generate.jena.selector.SelectorBase2;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDT;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.log4j.Logger;

/**
 *
 * @author maxime.lefrancois
 */
public class SEL_JSONPath extends SelectorBase2 {

    static {
        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

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

    private static final String iri = "urn:iana:mime:application/json";

    public SEL_JSONPath() {
        super(iri);
    }

    @Override
    public List<NodeValue> exec(NodeValue v1, NodeValue v2) {
        if (v1.getDatatypeURI() == null ? iri != null : !v1.getDatatypeURI().equals(iri)) {
            Logger.getLogger(SEL_JSONPath.class).warn("The URI of NodeValue1 MUST be <" + iri + ">. Returning null.");
        }
        Configuration conf = Configuration.builder().options(Option.ALWAYS_RETURN_LIST).build();

        try {
            List<Object> values = JsonPath.using(conf).parse(v1.asNode().getLiteralLexicalForm()).read(v2.getString());
            List<NodeValue> nodeValues = new ArrayList<>(values.size());
            Gson gson = new Gson();
            for (Object value : values) {
//                System.out.println("SEL --> "+value);
                RDFDatatype dt = org.apache.jena.datatypes.TypeMapper.getInstance().getSafeTypeByName(iri);
                String json = gson.toJson(value);
                Node node = org.apache.jena.graph.NodeFactory.createLiteral(json, dt) ; 
                NodeValue nodeValue = new NodeValueNode(node);
                nodeValues.add(nodeValue);
            }
            return nodeValues;
        } catch (Exception e) {
            throw new ExprEvalException("FunctionBase: no evaluation", e);
        }
    }
}
