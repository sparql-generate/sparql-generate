/*
 * Copyright 2016 ITEA 12004 SEAS Project.
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
package com.github.thesmartenergy.sparql.generate.jena;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.LocationMapper;
import org.apache.jena.util.Locator;
import org.apache.jena.util.LocatorFile;
import org.apache.log4j.Logger;
import com.github.thesmartenergy.sparql.generate.jena.engine.GenerationPlan;
import com.github.thesmartenergy.sparql.generate.jena.engine.GenerationPlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.GenerationQuerySolution;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;

/**
 *
 * @author maxime.lefrancois
 */
public class Test {

    public static void main(String[] args) throws URISyntaxException, IOException {
        SPARQLGenerate.init();

        URL examplesPath = Test.class.getResource("/example");
        File file = new File(examplesPath.toURI());

        for (File exampleDir : file.listFiles()) {
            System.out.println(exampleDir.toURI());

            // read location-mapping
            URI confUri = exampleDir.toURI().resolve("configuration.ttl");
            Model conf = RDFDataMgr.loadModel(confUri.toString());

            // initialize file manager
            FileManager fileManager = FileManager.makeGlobal();
            Locator loc = new LocatorFile(exampleDir.toURI().getPath());
            LocationMapper mapper = new LocationMapper(conf);
            fileManager.addLocator(loc);
            fileManager.setLocationMapper(mapper);

            // parse query 
            System.out.println("Processing example \"" + exampleDir.getName() + "\"");
            String query = IOUtils.toString(fileManager.open("query.rqg"), "UTF-8");
            SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(query, SPARQLGenerate.syntaxSPARQLGenerate);

            // serialize query 
            URI queryOutputUri = exampleDir.toURI().resolve("query_serialized.rqg");
            File queryOutputFile = new File(queryOutputUri);
            OutputStream queryOutputStream = new FileOutputStream(queryOutputFile);
            queryOutputStream.write(q.toString().getBytes());
            queryOutputStream.close();

            // create generation plan
            GenerationPlan plan = GenerationPlanFactory.create(q);
            Model output = ModelFactory.createDefaultModel();
                    GenerationQuerySolution initialBinding = new GenerationQuerySolution();

            // read inputs
            NodeIterator nodes = conf.listObjectsOfProperty(conf.getProperty("http://w3id.org/sparql-generate/example#input"));
            while (nodes.hasNext()) {
                Resource r = nodes.next().asResource();
                try {
                    String varName = conf.listObjectsOfProperty(r, conf.getProperty("http://w3id.org/sparql-generate/example#var")).next().asLiteral().getLexicalForm();
                    Var var = Var.alloc(varName);
                    Resource contentType = conf.listObjectsOfProperty(r, conf.getProperty("http://w3id.org/sparql-generate/example#contentType")).next().asResource();
                    String encoding = conf.listObjectsOfProperty(r, conf.getProperty("http://w3id.org/sparql-generate/example#encoding")).next().asLiteral().getLexicalForm();
                    Resource location = conf.listObjectsOfProperty(r, conf.getProperty("http://w3id.org/sparql-generate/example#location")).next().asResource();

                    String inputString = IOUtils.toString(fileManager.open(location.getURI()), encoding);

                    Literal input = output.createTypedLiteral(inputString, contentType.getURI());
                    initialBinding.put(varName, input);

                } catch (Exception e) {
                    Logger.getLogger(Test.class).info("unable to execute plan for input " + r);
                }
            }

            // execute plan
            plan.exec(initialBinding, output);

            // write output
            URI outputUri = exampleDir.toURI().resolve("output.ttl");
            File outputFile = new File(outputUri);
            OutputStream outputStream = new FileOutputStream(outputFile);
            output.write(outputStream, "TTL");
            outputStream.close( );

            URI expectedOutputUri = exampleDir.toURI().resolve("expected_output.ttl");
            Model expectedOutput = RDFDataMgr.loadModel(expectedOutputUri.toString());
            expectedOutput.write(System.out, "TTL");
            System.out.println("is equal: " + (output.containsAll(expectedOutput) && expectedOutput.containsAll(output)));
        }
    }
}
