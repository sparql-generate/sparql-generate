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
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/CSVMultipleOutput">iter:CSVMultipleOutput</a>
 * iterates over the rows of a CSV document.
 * In each iteration, it corresponds each designated variable to the given cell identified by the column name contained in the arguments
 *
 * <ul>
 * <li>Param 1 (args) is a list of arguments such that
 * <ul>
 * <li>the first argument of the list corresponds to the CSV document with a header line;</li>
 * <li>the second argument of the list corresponds to the delimiter character separating the values in each row;</li>
 * <li>the remaining arguments correspond the specified columns names as in the CSV header</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <b>Examples: </b>
 * <p>
 * Iterating over this CSV document<br>
 * <pre>
 * id,stop,latitude,longitude,date<br>
 * 6523,25,50.901389,4.484444,01/01/01<br>
 * 7000,40,56.901389,4.584444,02/02/02<br>
 * 7001,41,57.901389,5.584444,03/03/03<br>
 * 7002,42,58.901389,6.584444,23/12/80<br>
 * </pre>
 * with <tt>ITERATOR ite:CSVMultipleOutput(?source, ",", "id", "stop") AS ?id ?stop</tt> returns (in each iteration):<br>
 * <pre>
 *  ?id => "6523"^^xsd#string, ?stop => "25"^^xsd#string<br>
 *  ?id => "7000"^^xsd#string, ?stop => "40"^^xsd#string<br>
 *  ?id => "7001"^^xsd#string, ?stop => "41"^^xsd#string<br>
 *  ?id => "7002"^^xsd#string, ?stop => "42"^^xsd#string<br>
 * </pre>
 *
 * @author El Mehdi Khalfi <el-mehdi.khalfi at emse.fr>
 * @since 2018-09-04
 */
public class ITER_CSVMultipleOutput extends IteratorFunctionBase {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ITER_CSVMultipleOutput.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITER + "CSVMultipleOutput";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/text/csv";

    @Override
    public List<List<NodeValue>> exec(List<NodeValue> args) {
        NodeValue csv = args.get(0);
        String separator = args.get(1).asString();

        List<NodeValue> columns = args.subList(2, args.size());
        if (csv.getDatatypeURI() != null
                && !csv.getDatatypeURI().equals(datatypeUri)
                && !csv.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The URI of NodeValue1 MUST be <" + datatypeUri + ">"
                    + "or <http://www.w3.org/2001/XMLSchema#string>."
            );
        }
        List<String> cols = columns.stream().map(c -> c.getString()).collect(Collectors.toList());
        List<List<NodeValue>> nodeValues = new ArrayList<>();
        for (String col : cols) {
            nodeValues.add(new ArrayList<>());
        }

        try {
            String sourceCSV = csv.asNode().getLiteralLexicalForm();
            BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(sourceCSV.getBytes("UTF-8")), "UTF-8"));
            CsvPreference preference = new CsvPreference.Builder('"', separator.charAt(0), "\n").build();
            ICsvListReader listReader = new CsvListReader(br, preference);
            List<String> header = listReader.read();

            for (String col : cols) {
                int indexOfColInHeader = header.indexOf(col);
            }

            while (true) {
                List<String> row = listReader.read();
                if (row == null) {
                    break;
                }
                int i = 0;
                for (String col : cols) {
                    int indexOfColInHeader = header.indexOf(col);
                    //utiliser map
                    NodeValue n = new NodeValueNode(NodeFactory.createLiteral(row.get(indexOfColInHeader), XSDDatatype.XSDstring));
                    nodeValues.get(i++).add(n);
                }
            }
            return nodeValues;
        } catch (Exception ex) {
            LOG.debug("No evaluation for " + csv, ex);
            throw new ExprEvalException("No evaluation for " + csv, ex);
        }
    }

    @Override
    public void checkBuild(ExprList args) {
        if (args.size() < 3) {
            throw new QueryBuildException("Function '"
                    + this.getClass().getName() + "' takes at least three arguments: (1) the CSV document, (2) the separator character, and (3) at least one column to iterate over.");
        }
    }
}