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
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase1;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/CSV">iter:CSV</a>
 * returns a row of a CSV document, together with the header.
 *
 * <ul>
 * <li>Param 1: (csv) is the CSV document with a header line.</li>
 * </ul>
 *
 * <p>
 * For very large CSV files (typically above 100.000 lines), prefer <a href="http://w3id.org/sparql-generate/iter/CSVStream">CSVStream</a>.
 * </p>
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 * @see com.github.thesmartenergy.sparql.generate.jena.function.library.FN_CustomCSV
 * for CSV document with different dialects
 */
public class ITER_CSV extends IteratorFunctionBase1 {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_CSV.class);

    public static final String URI = SPARQLGenerate.ITER + "CSV";

    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/text/csv";

    @Override
    public List<List<NodeValue>> exec(NodeValue csv) {
        Instant start = Instant.now();

        if (csv.getDatatypeURI() != null
                && !csv.getDatatypeURI().equals(datatypeUri)
                && !csv.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The URI of NodeValue1 MUST be"
                    + " <" + datatypeUri + "> or"
                    + " <http://www.w3.org/2001/XMLSchema#string>. Got <"
                    + csv.getDatatypeURI() + ">. Returning null.");
        }
        RDFDatatype dt = TypeMapper.getInstance()
                .getSafeTypeByName(datatypeUri);
        try {

            String sourceCSV = csv.asNode().getLiteralLexicalForm();

            ICsvListReader listReader = null;
            InputStream is = new ByteArrayInputStream(sourceCSV.getBytes("UTF-8"));
            InputStreamReader reader = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(reader);

            listReader = new CsvListReader(br, CsvPreference.STANDARD_PREFERENCE);

            List<String> header = listReader.read();
            LOG.trace("header: " + header);
            List<NodeValue> nodeValues = new ArrayList<>();

            while (true) {
                List<String> row = listReader.read();
                if (row == null) {
                    break;
                }

                StringWriter sw = new StringWriter();

                CsvListWriter listWriter = new CsvListWriter(sw, CsvPreference.STANDARD_PREFERENCE);
                listWriter.write(header);
                listWriter.write(row);
                listWriter.close();

                String lexicalForm = sw.toString();
                LOG.trace(lexicalForm);
                Node node = NodeFactory.createLiteral(lexicalForm, dt);
                NodeValueNode nodeValue = new NodeValueNode(node);
                nodeValues.add(nodeValue);
            }

            long millis = Duration.between(start, Instant.now()).toMillis();
            System.out.println("ITER_CSV sent in " + String.format("%dmin, %d sec", TimeUnit.MILLISECONDS.toMinutes(millis), TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))));
            return new ArrayList<>(Collections.singletonList(nodeValues));
        } catch (Exception ex) {
            LOG.debug("No evaluation for " + csv, ex);
            throw new ExprEvalException("No evaluation for " + csv, ex);
        }
    }

}
