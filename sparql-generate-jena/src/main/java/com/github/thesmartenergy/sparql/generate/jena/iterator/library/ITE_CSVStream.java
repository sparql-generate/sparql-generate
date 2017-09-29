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
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorStreamFunctionBase4;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.supercsv.io.ICsvListReader;
import java.util.function.Consumer;
import org.apache.jena.atlas.web.TypedInputStream;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A SPARQL Iterator function that return a row of a CSV document, together with
 * the header. The Iterator function URI is
 * {@code <http://w3id.org/sparql-generate/iter/CSV>}.
 *
 * @see
 * com.github.thesmartenergy.sparql.generate.jena.function.library.FN_CustomCSV
 * for CSV document with different dialects
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class ITE_CSVStream extends IteratorStreamFunctionBase4 {

    public static int line = 0;
    
    /**
     * The logger.
     */
    private static final Logger LOG = LogManager.getLogger(ITE_CSVStream.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITER + "CSVStream";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/text/csv";

    private static final RDFDatatype dt = TypeMapper.getInstance().getSafeTypeByName(datatypeUri);

    /**
     *
     * {@code <http://www.iana.org/assignments/media-types/text/csv>} for each
     * row of the CSV document.
     */
    @Override
    public void exec(final NodeValue csv,
            final NodeValue headerValue,
            final NodeValue maxValue,
            final NodeValue recurrenceValue,
            final Consumer<List<NodeValue>> nodeValuesStream) {

        final String csvString = getCSV(csv);
        final int max = getMax(maxValue);
        final int recurrence = getRecurrence(recurrenceValue);
        LOG.trace("CSV Stream " + csv + " " + max + " " + recurrence);

        if (recurrence > 0) {
            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                exec(csvString, headerValue, max, nodeValuesStream, scheduler);
            }, 0, recurrence, TimeUnit.SECONDS);
        } else {
            exec(csvString, headerValue, max, nodeValuesStream, null);
        }
    }

    private void exec(final String csvString, final NodeValue headerValue, final int max, final Consumer<List<NodeValue>> nodeValuesStream, final ScheduledExecutorService scheduler) {
        try (TypedInputStream tis = SPARQLGenerate.getStreamManager().open(csvString);
                BufferedReader br = new BufferedReader(new InputStreamReader(tis.getInputStream(), "UTF-8"))) {

            String header = null;
            if (!(headerValue == null) && headerValue.getBoolean()) {
                header = br.readLine();
                line++;
                LOG.trace("header " + header);
            }

            ICsvListReader listReader = new CsvListReader(br, CsvPreference.STANDARD_PREFERENCE);
            while (true) {
                List<NodeValue> nodeValues = new ArrayList<>();
                for (int i = 0; i < max; i++) {
                    List<String> line = listReader.read();
                    ITE_CSVStream.line++;
                    if (line == null) {
                        if (scheduler != null) {
                            scheduler.shutdown();
                        }
                        LOG.trace("finished break");
                        break;
                    }
                    StringWriter sw = new StringWriter();
                    try (CsvListWriter listWriter = new CsvListWriter(sw, CsvPreference.TAB_PREFERENCE)) {
                        if (header != null) {
                            listWriter.writeHeader(header);
                        }
                        String row = listReader.getUntokenizedRow();
                        listWriter.write();
                    }
                    Node node = NodeFactory.createLiteral(sw.toString(), dt);
                    nodeValues.add(new NodeValueNode(node));
                }
                if (nodeValues.isEmpty()) {
                    if (scheduler != null) {
                        scheduler.shutdown();
                    }
                    LOG.trace("finished");
                    break;
                }
                LOG.trace("accept values " + nodeValues);
                nodeValuesStream.accept(nodeValues);
            }

        } catch (Exception ex) {
            LOG.debug("No evaluation for " + csvString , ex);
            throw new ExprEvalException("No evaluation for " + csvString , ex);
        }
    }

    private int getMax(NodeValue maxValue) {
        if (maxValue == null) {
            return Integer.MAX_VALUE;
        } else {
            if (!maxValue.isInteger()) {
                String msg = "The maximal number of lines per batch MUST be an integer,"
                        + " Got " + maxValue + ". Returning null.";
                LOG.warn(msg);
                throw new ExprEvalException(msg);
            } else {
                return maxValue.getInteger().intValue();
            }
        }
    }

    private int getRecurrence(NodeValue recurrenceValue) {
        if (!(recurrenceValue == null)) {
            return recurrenceValue.getInteger().intValue();
        }
        return -1;
    }

    private String getCSV(NodeValue csv) {
        if (!csv.isIRI()) {
            String msg = "The URI of NodeValue1 MUST be a URI, Got "
                    + csv + ". Returning null.";
            LOG.warn(msg);
            throw new ExprEvalException(msg);
        }
        return csv.asNode().getURI();
    }
}
