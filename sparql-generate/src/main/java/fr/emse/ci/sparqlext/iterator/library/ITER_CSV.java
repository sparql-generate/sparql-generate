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
import fr.emse.ci.sparqlext.iterator.IteratorStreamFunctionBase;
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.AbstractRowProcessor;
import com.univocity.parsers.common.processor.core.Processor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import fr.emse.ci.sparqlext.iterator.ExecutionControl;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.SysRIOT;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/CSV">iter:CSV</a>
 * batch-processes CSV documents, potentially having some custom CSV dialect,
 * and iteratively binds the content of a selection of cells to the list of
 * provided variables.
 *
 * <p>
 * See
 * <a href="https://w3id.org/sparql-generate/playground.html#ex=example/generate/Example-CSV">Live
 * example</a></p>
 *
 * The list of parameters is interpreted as follows:
 *
 * <ul>
 * <li>the URI of the CSV document (a URI), or the CSV document itself (a
 * String);</li>
 * <li>Optional group of string parameters for custom CSV dialects.
 * <ul>
 * <li>(boolean: header) true if the CSV has a header row (default is true
 * ).</li>
 * <li>(string: quoteChar) the quote character (default is '"' );</li>
 * <li>(string: delimiterChar) the delimiter character (default is ',' );</li>
 * <li>(string: endOfLineSymbols) the end of line symbol (default is '\n'
 * );</li>
 * </ul>
 * <li>(integer: batch) Optional number of rows per batch (by default, all the
 * CSV document is processed as one batch);</li>
 * <li>(string parameters: names) Names of the columns to select (by default,
 * all the columns are selected).</li>
 * </ul>
 * <p>
 * <b>Examples: </b>
 * <ul>
 * <li><code>ITERATOR ite:CSV(&lt;path/to/file>) AS ?PersonId ?Name</code>
 * fetches the document having URI &lt;path/to/file> (using the provided jena
 * stream manager); assumes that it has a header row, quote character '"',
 * delimiter character ',', end of line symbol '\n'; processes it in one batch,
 * and binds the cells of the two first column to ?PersonId and ?Name (except
 * for the first column which is the header).
 * </li>
 * <li><code>ITERATOR ite:CSV("""A1    B1<br>A2 B2""", false, '"', '\t', '\n') AS
 * ?A ?B</code> uses the provided CSV document; assumes that it has no header
 * now, uses the provided custom configuration; processes it in one batch, and
 * binds the cells of the two first column to ?A and ?B.
 * </li>
 * <Li><code>ITERATOR ite:CSV(&lt;path/to/file>, "PersonId", "Name") AS ?PersonId ?Name</code>
 * fetches the document having URI &lt;path/to/file> (using the provided jena
 * stream manager); uses the default configuration; processes it in one batch,
 * and binds the cells of the rows named "PersonId" and "Name" to the
 * corresponding variables.
 * </li>
 * <Li><code>ITERATOR ite:CSV(&lt;path/to/file>, 1000, "PersonId", "Name") AS ?PersonId ?Name</code>
 * fetches the document having URI &lt;path/to/file> (using the provided jena
 * stream manager); uses the default configuration; processes it in batches of
 * 1000 rows, and binds the cells of the rows named "PersonId" and "Name" to the
 * corresponding variables.
 * </li>
 *
 * @author Maxime Lefrançois &lt;maxime.lefrancois at emse.fr>
 * @since 2019-03-23
 */
public class ITER_CSV extends IteratorStreamFunctionBase {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ITER_CSV.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLExt.ITER + "CSV";

    @Override
    public void exec(
            final List<NodeValue> args,
            final Consumer<List<List<NodeValue>>> collectionListNodeValue,
            final ExecutionControl control) {
        Objects.nonNull(args);
        if (args.isEmpty()) {
            LOG.debug("Must have at leat one argument");
            throw new ExprEvalException("Must have at leat one argument");
        }
        LOG.trace("Executing CSV with variables " + args);
        final NodeValue csv = args.remove(0);
        if (csv == null) {
            String message = "Param 1 is null";
            LOG.warn(message);
            throw new ExprEvalException(message);
        }
        try (InputStream in = getInputStream(csv)) {
            final CsvParserSettings parserSettings = new CsvParserSettings();
            parserSettings.setHeaderExtractionEnabled(true);
            setFormatInformation(args, parserSettings);
            setProcessor(args, parserSettings, collectionListNodeValue, control);
            setSelectedColumns(args, parserSettings);
            CsvParser parser = new CsvParser(parserSettings);
            parser.parse(in, StandardCharsets.UTF_8);
        } catch (ExprEvalException | IOException ex) {
            LOG.warn("Exception while fetching or parsing CSV document", ex);
        } catch (Exception ex) {
            LOG.warn("Exception while fetching or parsing CSV document", ex);
        }
    }

    @Override
    public void checkBuild(ExprList args) {
    }

    private void setProcessor(
            final List<NodeValue> args,
            final CsvParserSettings parserSettings,
            final Consumer<List<List<NodeValue>>> collectionListNodeValue,
            final ExecutionControl control) {
        final int rowsInABatch;
        if (!args.isEmpty() && args.get(0).isInteger()) {
            int batch = args.remove(0).getInteger().intValue();
            if (batch > 0) {
                rowsInABatch = batch;
                LOG.trace("  With batches of " + rowsInABatch + " lines.");
            } else {
                rowsInABatch = 0;
                LOG.trace("  As one batch");
            }
        } else {
            rowsInABatch = 0;
            LOG.trace("  As one batch");
        }

        final Processor processor = new AbstractRowProcessor() {
            private int rowsInThisBatch = 0;
            private int total = 0;
            List<List<NodeValue>> nodeValues = new ArrayList<>();

            @Override
            public void processStarted(ParsingContext context) {
                SPARQLExt.addTaskOnClose(getContext(), context::stop);
            }

            @Override
            public void rowProcessed(String[] row, ParsingContext context) {
                final List<NodeValue> list = new ArrayList<>();
                for (String cell : row) {
                    if (cell == null) {
                        list.add(null);
                    } else {
                        list.add(new NodeValueString(cell));
                    }
                }
                nodeValues.add(list);
                rowsInThisBatch++;
                total++;
                if (rowsInABatch > 0 && rowsInThisBatch >= rowsInABatch) {
                    LOG.trace("New batch of " + rowsInThisBatch + " rows, " + total + " total");
                    send();
                    rowsInThisBatch = 0;
                }
            }

            @Override
            public void processEnded(ParsingContext context) {
                LOG.trace("Last batch of " + rowsInThisBatch + " rows, " + total + " total.");
                send();
                control.complete();
            }

            private void send() {
                collectionListNodeValue.accept(nodeValues);
                nodeValues = new ArrayList<>();
            }

        };
        parserSettings.setProcessor(processor);
    }

    private InputStream getInputStream(NodeValue csv) throws ExprEvalException, IOException {
        if (csv.isString()) {
            return IOUtils.toInputStream(csv.asString(), StandardCharsets.UTF_8);
        } else if (csv.isLiteral() && csv.asNode().getLiteralDatatypeURI().startsWith("http://www.iana.org/assignments/media-types/")) {
            return IOUtils.toInputStream(csv.asNode().getLiteralLexicalForm(), StandardCharsets.UTF_8);
        } else if (csv.isIRI()) {
            String csvPath = csv.asNode().getURI();
            LookUpRequest req = new LookUpRequest(csvPath, "text/csv");
            final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) getContext().get(SysRIOT.sysStreamManager);
            Objects.requireNonNull(sm);
            TypedInputStream tin = sm.open(req);
            if (tin == null) {
                String message = String.format("Could not look up csv document %s", csvPath);
                LOG.warn(message);
                throw new ExprEvalException(message);
            }
            return tin.getInputStream();
        } else {
            String message = String.format("First argument must be a URI or a String");
            LOG.warn(message);
            throw new ExprEvalException(message);
        }
    }

    private void setFormatInformation(List<NodeValue> args, CsvParserSettings parserSettings) {
        if (!args.isEmpty() && args.get(0).isBoolean()) {
            boolean header = args.remove(0).getBoolean();
            if (args.size() < 3
                    || !args.get(0).isString()
                    || !args.get(1).isString()
                    || !args.get(2).isString()) {
                throw new ExprEvalException("Block of CSV configuration parameters does not conform to the specification. Check out the documentation.");
            }
            char quote = args.remove(0).getString().charAt(0);
            char delimiter = args.remove(0).getString().charAt(0);
            String lineSeparator = args.remove(0).getString();
            parserSettings.setHeaderExtractionEnabled(header);
            parserSettings.getFormat().setQuote(quote);
            parserSettings.getFormat().setDelimiter(delimiter);
            parserSettings.getFormat().setLineSeparator(lineSeparator);
            LOG.trace("\tWith custom CSV configuration: header: " + header + ", quote character: '" + quote + "', delimiter character: '" + delimiter + "', line separator: '" + lineSeparator);
        } else {
            LOG.debug("\tWith default CSV parser settings.");
        }
    }

    private void setSelectedColumns(List<NodeValue> args, CsvParserSettings parserSettings) {
        if (!args.isEmpty()) {
            if (args.stream().anyMatch(col -> col == null || !col.isString())) {
                LOG.debug("Columns names must strings, got: " + args);
                throw new ExprEvalException("Columns names must be strings, got: " + args);
            }
            parserSettings.selectFields(args.stream().map(NodeValue::asString).toArray(String[]::new));
        }
    }

}
