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
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorStreamFunctionBase2;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
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
import org.apache.jena.riot.system.stream.StreamManager;
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
public class ITE_CSVStream extends IteratorStreamFunctionBase2 {

    /**
     * The logger.
     */
    private static final Logger LOG = LogManager.getLogger(ITE_CSVStream.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITER + "CSV";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/text/csv";

    /**
     *
     * @param csv the URI of the source document
     * @param nodeValueStream - where is emited the list of RDF Literals with datatype URI
     * {@code <http://www.iana.org/assignments/media-types/text/csv>} for each row of the CSV document.
     */
    @Override
    public void exec(NodeValue csv, NodeValue maxNumber, Consumer<List<NodeValue>> nodeValuesStream) {
        
        if (!csv.isIRI()) {
            LOG.warn("The URI of NodeValue1 MUST be a URI, Got "
                    + csv + ". Returning null.");
        }
        RDFDatatype dt = TypeMapper.getInstance()
                .getSafeTypeByName(datatypeUri);
        
        int max = Integer.MAX_VALUE;
        if (maxNumber != null) {
            if(maxNumber.isInteger()) {
                LOG.warn("The maximal number of lines per batch MUST be an integer,"
                    + " Got " + csv + ". Returning null.");
            } else {
                max = maxNumber.getInteger().intValue();
            }
        }                
               
        try(TypedInputStream tis = StreamManager.get().open(csv.asNode().getURI());
            BufferedReader br = new BufferedReader(new InputStreamReader(tis.getInputStream(), "UTF-8"))) {
            
            String header = br.readLine();
            ICsvListReader listReader = new CsvListReader(br, CsvPreference.STANDARD_PREFERENCE);

            while (listReader.read() != null){
                StringWriter sw = new StringWriter();
                List<NodeValue> nodeValues = new ArrayList<>();
                try (CsvListWriter listWriter = new CsvListWriter(sw, CsvPreference.TAB_PREFERENCE)) {
                    listWriter.writeHeader(header);
                    int i=0;
                    while (listReader.read() != null && i++ < max) {
                        listWriter.write(listReader.getUntokenizedRow());
                    }
                    Node node = NodeFactory.createLiteral(sw.toString(), dt);
                    nodeValues.add(new NodeValueNode(node));
                }
                nodeValuesStream.accept(nodeValues);
            }

        } catch (Exception e) {
            throw new ExprEvalException("Function " + ITE_CSVStream.class.getSimpleName() + ": no evaluation", e);
        }
    }

}
