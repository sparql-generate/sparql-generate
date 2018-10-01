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
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorStreamFunctionBase;
import com.github.thesmartenergy.sparql.generate.jena.stream.LookUpRequest;
import com.univocity.parsers.common.processor.BatchedColumnProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/CSVStream">iter:CSVStream</a>
 * batch-processes CSV documents and iteratively binds the content of one or multiple cells to variables.
 *
 * <ul>
 * <li>Param 1: the URI of the CSV document (a String);</li>
 * <li>Param 2: the number of rows per batch (an Integer);</li>
 * <li>Param 3 .. <em>n</em>: the names of the columns to select (use "*" to select all the columns).</li>
 * </ul>
 * <p>
 * CSVStream works like a <a href="http://w3id.org/sparql-generate/iter/CSVMultipleOutput">iter:CSVMultipleOutput</a>
 * iterator but for one chunk of the CSV document at a time.
 * </p>
 * <p>
 * CSVStream yields better results for very large CSV files, typically above 100.000 lines.
 * </p>
 * <p>
 * For instance, binding variables to columns of a 14-column/1.500.000-rows CSV document takes a couple of minutes with CSVStream while <a href="http://w3id.org/sparql-generate/iter/CSVMultipleOutput">iter:CSVMultipleOutput</a> reaches <code>java.lang.OutOfMemoryError: GC overhead limit exceeded</code>.
 * <p>
 * <b>Example: </b>
 * <p>
 * <code>ITERATOR ite:CSVStream("path/to/file", 3, "PersonId", "Name") AS ?PersonId ?Name</code> iterates over the following CSV document in batches of three rows:
 * <pre>
 * PersonId,Name,Phone,Email,Birthdate,Height,Weight,Company <br>
 * 1,Jin Lott,374-5365,nonummy@nonsollicitudina.net,1990-10-23T09:39:36+01:00,166.58961852476,72.523064012179,4 <br>
 * 2,Ulric Obrien,1-772-516-9633,non.arcu@velit.co.uk,1961-11-18T02:18:23+01:00,164.38438947455,68.907470544061,9 <br>
 * 3,Travis Wilkerson,240-1629,felis@Duisac.co.uk,1956-03-05T15:57:29+01:00,163.47434097479,64.217840002146,89 <br>
 * 4,Emerson Bass,1-782-875-0201,aliquet.metus@hymenaeosMauris.edu,1996-06-23T01:02:19+02:00,164.29879257543,71.974297306501,16 <br>
 * 5,Nathaniel Mendoza,849-7651,turpis.nec@Praesentluctus.co.uk,1939-02-08T18:30:06+00:00,169.42790140759,68.742803975349,2 <br>
 * 6,Gil Chang,628-2603,sit.amet.orci@justo.co.uk,1974-01-07T04:34:44+01:00,165.25115131786,66.292234202668,12 <br>
 * 7,Gregory Pearson,504-1301,convallis.convallis@penatibusetmagnis.com,1969-08-08T13:59:17+01:00,169.23327712391,69.218201975682,93 <br>
 * 8,Slade Davis,769-6793,ultrices.Duis@nuncest.edu,1996-02-02T01:42:38+01:00,167.6070585433,67.448295111418,76 <br>
 * 9,Hunter Howell,237-7855,augue.id.ante@tinciduntnequevitae.edu,1965-04-29T19:15:38+01:00,163.27719591459,69.74419350177,99 <br>
 * 10,Lionel Melendez,292-7586,posuere.cubilia.Curae@augue.edu,1938-06-18T17:19:01+01:00,170.29595560716,67.133894657783,76 <br>
 * </pre>
 * batch 1
 * <pre>
 *  ?PersonId => "1", ?Name => "Jin Lott"<br>
 *  ?PersonId => "2", ?Name => "Ulric Obrien"<br>
 *  ?PersonId => "3", ?Name => "Travis Wilkerson"<br>
 * </pre>
 * batch 2
 * <pre>
 *  ?PersonId => "4", ?Name => "Emerson Bass"<br>
 *  ?PersonId => "5", ?Name => "Nathaniel Mendoza"<br>
 *  ?PersonId => "6", ?Name => "Gil Chang"<br>
 * </pre>
 * batch 3
 * <pre>
 *  ?PersonId => "7", ?Name => "Gregory Pearson"<br>
 *  ?PersonId => "8", ?Name => "Slade Davis"<br>
 *  ?PersonId => "9", ?Name => "Hunter Howell"<br>
 * </pre>
 * batch 4
 * <pre>
 *  ?PersonId => "10", ?Name => "Lionel Melendez"<br>
 * </pre>
 *
 * @author El-Mehdi Khalfi <el-mehdi.khalfi at emse.fr>
 * @since 2018-09-26
 */

public class ITER_CSVStream extends IteratorStreamFunctionBase {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ITER_CSVStream.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITER + "CSVStream";

    @Override
    public void exec(List<NodeValue> args, Consumer<List<List<NodeValue>>> nodeValuesStream) {
        if (!args.get(0).isString()) {
            LOG.debug("First argument must be a string, got: " + args.get(0));
            throw new ExprEvalException("First argument must be a string, got: " + args.get(0));
        }
        if (!args.get(1).isInteger() || args.get(1).getInteger().intValue() < 0) {
            LOG.debug("Second argument must be a positive integer, got: " + args.get(1));
            throw new ExprEvalException("Second argument must be a positive integer, got: " + args.get(1));
        }
        if (args.subList(2, args.size()).stream().anyMatch(col -> !col.isString())) {
            LOG.debug("Columns names must be a string, got: " + args.subList(2, args.size()));
            throw new ExprEvalException("Columns names must be a string, got: " + args.subList(2, args.size()));
        }

        String csvPath = args.get(0).asString();
        int chunkSize = args.get(1).getInteger().intValue();

        LookUpRequest req = new LookUpRequest(csvPath, "text/csv");
        TypedInputStream in = SPARQLGenerate.getStreamManager().open(req);

        CsvParserSettings parserSettings = new CsvParserSettings();
        parserSettings.getFormat().setLineSeparator("\n");
        parserSettings.setHeaderExtractionEnabled(true);

        if (args.size() == 3 && args.get(2).asString().equals("*")) {
            // nothing to be done, by default the CSV library retrieves all columns
        } else {
            String[] wantedColumns = args.subList(2, args.size()).stream().map(NodeValue::asString).toArray(String[]::new);
            parserSettings.selectFields(wantedColumns);
        }

        parserSettings.setProcessor(
                new BatchedColumnProcessor(chunkSize) {
                    @Override
                    public void batchProcessed(int rowsInThisBatch) {
                        List<List<NodeValue>> nodeValues = getColumnValuesAsMapOfIndexes().values().stream().
                                map(column -> column.stream().
                                        //convert each cell from string to a NodeValue to be fed to nodeValuesStream.accept
                                                map(cell -> (NodeValue) new NodeValueString(cell)).
                                                collect(Collectors.toList())).
                                collect(Collectors.toList());
                        nodeValuesStream.accept(nodeValues);
                    }
                }

        );

        CsvParser parser = new CsvParser(parserSettings);
        parser.parse(in.getInputStream());
    }


    @Override
    public void checkBuild(ExprList args) {
        if (args.size() < 3) {
            throw new QueryBuildException("Function '"
                    + this.getClass().getName() + "' takes at least three arguments: (1) the CSV file path, (2) the number of rows to process in each batch, and (3) \"*\" to iterate over all columns or at least one column name.");
        }
    }
}