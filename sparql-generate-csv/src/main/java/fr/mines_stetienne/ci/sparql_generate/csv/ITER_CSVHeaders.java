/*
 * Copyright 2020 MINES Saint-Étienne
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
package fr.mines_stetienne.ci.sparql_generate.csv;

import fr.mines_stetienne.ci.sparql_generate.utils.LogUtils;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionBase1;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.util.Collections;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.SysRIOT;
import fr.mines_stetienne.ci.sparql_generate.stream.LookUpRequest;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/CSVHeaders">iter:CSVHeaders</a>
 * iterates over the cells in the header row.
 *
 * <ul>
 * <li>Param 1: (csv) is the CSV document with a header line, or its IRI: only the
 * header line of the document is then read.</li>
 * </ul>
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class ITER_CSVHeaders extends IteratorFunctionBase1 {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_CSVHeaders.class);

    public static final String URI = SPARQLExt.ITER + "CSVHeaders";

    private static final String datatypeUri = "https://www.iana.org/assignments/media-types/text/csv";

    @Override
    public List<List<NodeValue>> exec(NodeValue csv) {
        if(csv == null) {
        	String msg = "No CSV provided";
            LOG.debug(msg);
        	throw new ExprEvalException(msg);
        }
        if (csv.isIRI()) {
            return headersOf(csv.asNode().getURI());
        }
        if (csv.getDatatypeURI() != null
                && !csv.getDatatypeURI().equals(datatypeUri)
                && !csv.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The URI of NodeValue1 MUST be <" + datatypeUri + ">"
                    + "or <http://www.w3.org/2001/XMLSchema#string>."
            );
        }
        try {
            String sourceCSV = String.valueOf(csv.asNode().getLiteralLexicalForm());

            InputStream is = new ByteArrayInputStream(sourceCSV.getBytes("UTF-8"));
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            CsvMapReader mapReader = new CsvMapReader(br, CsvPreference.STANDARD_PREFERENCE);
            String headers_str[] = mapReader.getHeader(true);

            final List<List<NodeValue>> listNodeValues = new ArrayList<>();
            Node node;
            NodeValue nodeValue;
            for (String header : headers_str) {
                node = NodeFactory.createLiteral(header);
                nodeValue = new NodeValueNode(node);
                listNodeValues.add(Collections.singletonList(nodeValue));
            }
            return listNodeValues;
        } catch (Exception ex) {
            if(LOG.isDebugEnabled()) {
                Node compressed = LogUtils.compress(csv.asNode());
                LOG.debug("No evaluation for " + compressed, ex);
            }
            throw new ExprEvalException("No evaluation ", ex);
        }
    }


    /**
     * The header of the document of an IRI: only its first line is read.
     */
    private List<List<NodeValue>> headersOf(final String iri) {
        final LookUpRequest req = new LookUpRequest(iri, "text/csv");
        final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) getContext().get(SysRIOT.sysStreamManager);
        Objects.requireNonNull(sm);
        final TypedInputStream tin = sm.open(req);
        if (tin == null) {
            String message = String.format("Could not look up csv document %s", iri);
            LOG.warn(message);
            throw new ExprEvalException(message);
        }
        try (InputStream in = tin.getInputStream();
                CsvMapReader mapReader = new CsvMapReader(
                        new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)),
                        CsvPreference.STANDARD_PREFERENCE)) {
            final String[] headers = mapReader.getHeader(true);
            final List<List<NodeValue>> listNodeValues = new ArrayList<>();
            if (headers != null) {
                for (String header : headers) {
                    listNodeValues.add(Collections.singletonList(new NodeValueNode(NodeFactory.createLiteralString(header == null ? "" : header))));
                }
            }
            return listNodeValues;
        } catch (IOException ex) {
            LOG.warn("Exception while reading the header of the CSV document " + iri, ex);
            throw new ExprEvalException("No evaluation", ex);
        }
    }
}
