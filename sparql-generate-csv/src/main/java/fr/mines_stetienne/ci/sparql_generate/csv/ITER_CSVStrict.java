/*
 * Copyright 2026 MINES Saint-Étienne
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.AbstractRowProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import fr.mines_stetienne.ci.sparql_generate.RaiseException;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.iterator.DownstreamException;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorStreamFunctionBase;
import fr.mines_stetienne.ci.sparql_generate.stream.DocumentNotFoundException;
import fr.mines_stetienne.ci.sparql_generate.stream.LookUpRequest;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/CSVStrict">iter:CSVStrict</a>
 * iterates over the rows of a CSV document with a header, as
 * <a href="http://w3id.org/sparql-generate/iter/CSV">iter:CSV</a> with named
 * columns, and reads it strictly, as the logical sources of RML-IO:
 *
 * <ul>
 * <li>an empty cell is the empty string (iter:CSV leaves its variable
 * unbound);</li>
 * <li>a row whose number of fields is not the number of columns of the
 * header, and a column that the header does not have, are data errors: they
 * stop the execution, as {@code RAISE} does.</li>
 * </ul>
 *
 * The list of parameters is interpreted as follows:
 *
 * <ul>
 * <li>the URI of the CSV document (a URI), or the CSV document itself (a
 * String);</li>
 * <li>(integer: batch) Optional number of rows per batch (by default, all the
 * CSV document is processed as one batch);</li>
 * <li>(strings) the names of the columns to select (without any, the output
 * is the position of the row only).</li>
 * </ul>
 *
 * The output is the value of each selected column, then the position of the
 * row (an integer, from 0, the header excluded, across batches).
 *
 * <p>
 * <b>Example: </b>
 * <code>ITERATOR iter:CSVStrict(&lt;people.csv>, "id", "name") AS ?id ?name ?row</code>
 * </p>
 *
 * @since 2.6.1
 */
public class ITER_CSVStrict extends IteratorStreamFunctionBase {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_CSVStrict.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLExt.ITER + "CSVStrict";

    @Override
    public void checkBuild(ExprList args) {
    }

    @Override
    public void exec(final List<NodeValue> args, final Consumer<List<List<NodeValue>>> collectionListNodeValue) {
        Objects.requireNonNull(args);
        if (args.isEmpty() || args.get(0) == null) {
            throw new ExprEvalException("No CSV document provided");
        }
        final NodeValue csv = args.remove(0);
        int batch = 0;
        if (!args.isEmpty() && args.get(0) != null && args.get(0).isInteger()) {
            batch = Math.max(0, args.remove(0).getInteger().intValue());
        }
        if (args.stream().anyMatch(col -> col == null || !col.isString())) {
            throw new ExprEvalException("Expecting the names of the columns, got: " + args);
        }
        final String[] columns = args.stream().map(NodeValue::asString).toArray(String[]::new);
        final int rowsInABatch = batch;
        // the task that stops the parser if the execution is closed meanwhile,
        // removed when the parse ends
        final Runnable[] stop = new Runnable[1];
        try (InputStream in = getInputStream(csv)) {
            final CsvParserSettings settings = new CsvParserSettings();
            settings.setHeaderExtractionEnabled(true);
            // an empty cell, quoted or not, is the empty string
            settings.setNullValue("");
            settings.setEmptyValue("");
            settings.setMaxColumns(2500);
            settings.setMaxCharsPerColumn(-1);
            settings.setProcessor(new AbstractRowProcessor() {
                private int[] indexes;
                private int fields;
                private int rowsInThisBatch = 0;
                private int total = 0;
                private int batches = 0;
                private List<List<NodeValue>> rows = new ArrayList<>();

                @Override
                public void processStarted(ParsingContext context) {
                    stop[0] = context::stop;
                    ContextUtils.addTaskOnClose(getContext(), stop[0]);
                }

                private void readHeader(ParsingContext context) {
                    final String[] headers = context.headers();
                    if (headers == null) {
                        throw error("The CSV document has no header", csv);
                    }
                    fields = headers.length;
                    final List<String> names = Arrays.asList(headers);
                    indexes = new int[columns.length];
                    for (int i = 0; i < columns.length; i++) {
                        indexes[i] = names.indexOf(columns[i]);
                        if (indexes[i] < 0) {
                            throw error("The CSV document has no column", csv, columns[i]);
                        }
                    }
                }

                @Override
                public void rowProcessed(String[] row, ParsingContext context) {
                    if (indexes == null) {
                        readHeader(context);
                    }
                    if (row.length != fields) {
                        throw error("A row of the CSV document does not have as many fields as the header", csv,
                                total, row.length, fields);
                    }
                    final List<NodeValue> values = new ArrayList<>(indexes.length + 1);
                    for (int index : indexes) {
                        values.add(new NodeValueString(row[index] == null ? "" : row[index]));
                    }
                    values.add(new NodeValueInteger(total));
                    rows.add(values);
                    rowsInThisBatch++;
                    total++;
                    if (rowsInABatch > 0 && rowsInThisBatch >= rowsInABatch) {
                        send();
                        rowsInThisBatch = 0;
                    }
                }

                @Override
                public void processEnded(ParsingContext context) {
                    if (indexes == null && context.headers() != null) {
                        // no row: the columns are checked anyway
                        readHeader(context);
                    }
                    if (rowsInThisBatch > 0 || batches == 0) {
                        send();
                    }
                }

                private void send() {
                    collectionListNodeValue.accept(rows);
                    rows = new ArrayList<>();
                    batches++;
                }
            });
            new CsvParser(settings).parse(in, StandardCharsets.UTF_8);
        } catch (ExprEvalException | DocumentNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            // a data error, RAISE, or an error of the rest of the query
            DownstreamException.rethrowIfNotOwn(ex);
            LOG.warn("Exception while fetching or parsing CSV document", ex);
            throw new ExprEvalException("No evaluation", ex);
        } finally {
            ContextUtils.removeTaskOnClose(getContext(), stop[0]);
        }
    }

    private static RaiseException error(String message, NodeValue csv, Object... values) {
        final List<Node> nodes = new ArrayList<>();
        nodes.add(csv.isIRI() ? csv.asNode() : NodeFactory.createLiteralString("(inline document)"));
        for (Object v : values) {
            nodes.add(v instanceof Integer ? NodeValue.makeInteger((Integer) v).asNode()
                    : NodeFactory.createLiteralString(v.toString()));
        }
        return new RaiseException(message, nodes);
    }

    private InputStream getInputStream(NodeValue csv) throws IOException {
        if (csv.isString()) {
            return IOUtils.toInputStream(csv.asString(), StandardCharsets.UTF_8);
        } else if (csv.isLiteral()) {
            return IOUtils.toInputStream(csv.asNode().getLiteralLexicalForm(), StandardCharsets.UTF_8);
        } else if (csv.isIRI()) {
            final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) getContext().get(SysRIOT.sysStreamManager);
            Objects.requireNonNull(sm);
            final TypedInputStream tin = sm.open(new LookUpRequest(csv.asNode().getURI(), "text/csv"));
            if (tin == null) {
                final String message = "Could not look up csv document " + csv.asNode().getURI();
                LOG.warn(message);
                throw new ExprEvalException(message);
            }
            return tin.getInputStream();
        }
        throw new ExprEvalException("First argument must be a URI or a String");
    }
}
