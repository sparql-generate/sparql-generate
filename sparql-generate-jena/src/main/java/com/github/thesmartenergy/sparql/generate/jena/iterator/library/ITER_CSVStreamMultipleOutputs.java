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
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
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
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ITER_CSVStreamMultipleOutputs extends IteratorStreamFunctionBase {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_CSVStream.class);

    public static final String URI = SPARQLGenerate.ITER + "CSVStreamMultipleOutputs";

    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/text/csv";


    @Override
    public void checkBuild(ExprList args) {

    }

    @Override
    public void exec(List<NodeValue> args, Consumer<List<List<NodeValue>>> nodeValuesStream) {
        Instant start = Instant.now();
        withoutStreams(args, nodeValuesStream);
        System.out.println("Sent in " + Duration.between(start, Instant.now()).toMillis() + " ms.");
        /*try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public void withoutStreams(List<NodeValue> args, Consumer<List<List<NodeValue>>> nodeValuesStream) {
        String csvPath = args.get(0).asString();
        int chunkSize = args.get(1).getInteger().intValue();

        try {
            BufferedReader br = null;
            br = new BufferedReader(new FileReader(csvPath));
            ICsvListReader listReader = new CsvListReader(br, CsvPreference.STANDARD_PREFERENCE);
            final List<String> header = listReader.read();


            Map<String, Integer> wantedColumnsNames;
            if (args.size() == 3 && args.get(2).asString().equals("*")) {
                wantedColumnsNames = IntStream.range(0, header.size()).boxed().collect(Collectors.toMap(header::get, i -> i));
            } else {
                wantedColumnsNames = args.subList(2, args.size()).stream().map(NodeValue::asString).collect(Collectors.toMap(col -> col, col -> header.indexOf(col)));
            }

            int chunkNumber = 0;
            boolean endOfFileReached = false;
            while (true) {
                List<List<NodeValue>> nodeValues = Stream.generate(ArrayList<NodeValue>::new).limit(wantedColumnsNames.size()).collect(Collectors.toList());
                for (int x = 0; x < chunkSize; x++) {
                    List<String> row = listReader.read();
                    if (row == null) {
                        endOfFileReached = true;
                        break;
                    }
                    for (String col : wantedColumnsNames.keySet()) {
                        int indexOfColInHeader = wantedColumnsNames.get(col);
                        NodeValue n = new NodeValueNode(NodeFactory.createLiteral(row.get(indexOfColInHeader), XSDDatatype.XSDstring));

                        nodeValues.get(wantedColumnsNames.get(col)).add(n);
                    }
                }
                nodeValuesStream.accept(nodeValues);
                //System.out.println("################################## Chunk N° " + (++chunkNumber) + " ##################################");
                if (endOfFileReached)
                    break;
            }
        } catch (Exception ex) {
            LOG.debug("No evaluation for " + csvPath, ex);
            throw new ExprEvalException("No evaluation for " + csvPath, ex);
        }
    }

    public void withStreams(List<NodeValue> args, Consumer<List<List<NodeValue>>> nodeValuesStream) {
        String csvPath = args.get(0).asString();
        int chunkSize = args.get(1).getInteger().intValue();
        List<String> wantedColumnsNames;
        if (args.size() == 3 && args.get(2).asString().equals("*")) {
            wantedColumnsNames = new ArrayList<>();
        } else {
            wantedColumnsNames = args.subList(2, args.size()).stream().map(column -> column.asString()).collect(Collectors.toList());
        }


        Supplier<Stream<List<String>>> csvStreamAsLinesSupplier = () -> {
            try {
                return Files.lines(Paths.get(csvPath)).map(csvLine -> Arrays.asList(csvLine.split(",")));
            } catch (IOException e) {
                return null;
            }
        };
        List<String> headerAsList = csvStreamAsLinesSupplier.get().limit(1).findFirst().get();
        List<Integer> wantedColumnsIndexes = wantedColumnsNames.isEmpty() ? IntStream.range(0, headerAsList.size()).boxed().collect(Collectors.toList()) : wantedColumnsNames.stream().map(headerAsList::indexOf).collect(Collectors.toList());

        if (wantedColumnsIndexes.contains(-1)) {
            System.out.println("XXX Probleme colonne introuvable");
            return;
        }

        List<List<NodeValue>> nodeValues;
        List<List<String>> currentChunk;
        int skip = 1;
        int chunkNumber = 1;
        do {
            currentChunk = csvStreamAsLinesSupplier.get().skip(skip).limit(chunkSize).collect(Collectors.toList());
            if (currentChunk.size() == 0)
                break;
            nodeValues = new ArrayList<>();

            for (List<String> lineInCurrentBatch : currentChunk)
                nodeValues.add(wantedColumnsIndexes.stream().mapToInt(Integer::intValue).mapToObj(lineInCurrentBatch::get).map(cell -> new NodeValueNode(NodeFactory.createLiteral(cell, XSDDatatype.XSDstring))).collect(Collectors.toList()));
            skip += chunkSize;
            List<List<NodeValue>> finalNodeValues = nodeValues;
            List<List<NodeValue>> transposedChunk = IntStream.range(0, nodeValues.get(0).size())
                    .mapToObj(i -> finalNodeValues.stream().map(l -> l.get(i)).collect(Collectors.toList()))
                    .collect(Collectors.toList());

            //Instant startChunk = Instant.now();
            nodeValuesStream.accept(transposedChunk);
            //System.out.println("in " + Duration.between(startChunk, Instant.now()).toMillis() + " ms.");

            //Instant finishChunk = Instant.now();
            //long timeElapsedChunk = Duration.between(startChunk, finishChunk).toMillis();
            //System.out.println("Chunk N° " + (chunkNumber++) + " sent in " + timeElapsedChunk + " ms.");
        } while (true);

    }
}