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
package fr.emse.ci.sparqlext.iterator.library;

import fr.emse.ci.sparqlext.SPARQLExt;
import com.google.gson.Gson;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import fr.emse.ci.sparqlext.iterator.IteratorFunctionBase2;

import java.util.*;

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
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/CBOR">iter:CBOR</a>
 * takes as input a CBOR document, decodes it, and extracts a list of sub-JSON
 * documents according to a JSONPath expression.
 *
 * <ul>
 * <li>Param 1: (cbor) is a base64 encoding of the CBOR document in a RDF
 * Literal with datatype URI
 * {@code <http://www.iana.org/assignments/media-types/application/cbor>} or
 * {@code xsd:string}</li>
 * <li>Param 2: (jsonpath) is the JSONPath expression in a RDF Literal with
 * datatype {@code xsd:string}</li>
 * </ul>
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class ITER_CBOR extends IteratorFunctionBase2 {

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

    private static final Logger LOG = LoggerFactory.getLogger(ITER_CBOR.class);

    public static final String URI = SPARQLExt.ITER + "CBOR";

    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/application/cbor";

    @Override
    public List<List<NodeValue>> exec(NodeValue cbor, NodeValue jsonpath) {
        if (cbor.getDatatypeURI() != null
                && !cbor.getDatatypeURI().equals(datatypeUri)
                && !cbor.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The URI of NodeValue1 MUST be"
                    + " <" + datatypeUri + "> or"
                    + " <http://www.w3.org/2001/XMLSchema#string>. Got <"
                    + cbor.getDatatypeURI() + ">. Returning null.");
        }

        Configuration conf = Configuration.builder()
                .options(Option.ALWAYS_RETURN_LIST).build();

        String json = new String(Base64.getDecoder().decode(cbor.asNode().getLiteralLexicalForm().getBytes()));

        Gson gson = new Gson();
        RDFDatatype dt = TypeMapper.getInstance().getSafeTypeByName(datatypeUri);
        
        try {
            List<Object> values = JsonPath
                    .using(conf)
                    .parse(json)
                    .read(jsonpath.getString());

            final List<List<NodeValue>> nodeValues = new ArrayList<>();

            Node node;
            NodeValue nodeValue;
            for (Object value : values) {
                String jsonstring = gson.toJson(value);
                node = NodeFactory.createLiteral(jsonstring, dt);
                nodeValue = new NodeValueNode(node);
                nodeValues.add(Collections.singletonList(nodeValue));
            }
            return nodeValues;
        } catch (Exception ex) {
            if(LOG.isDebugEnabled()) {
                Node compressed = SPARQLExt.compress(cbor.asNode());
                LOG.debug("No evaluation of " + compressed + ", " + jsonpath, ex);
            }
            throw new ExprEvalException("No evaluation of " + jsonpath, ex);
        }
    }
}
