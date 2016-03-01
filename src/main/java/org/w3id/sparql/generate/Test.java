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
package org.w3id.sparql.generate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.w3id.sparql.generate.engine.GenerationPlan;
import org.w3id.sparql.generate.engine.GenerationPlanFactory;
import org.w3id.sparql.generate.query.SPARQLGenerateQuery;
import org.w3id.sparql.generate.query.SPARQLGenerateSyntax;

/**
 *
 * @author maxime.lefrancois
 */
public class Test {
    

    public static void main(String[] args) throws URISyntaxException, IOException {
        SPARQLGenerate.init();
        
        String dir = "example/simple/";

        URL queryPath = Test.class.getResource("/"+dir+"query.rqg");
        String query = new String(Files.readAllBytes(Paths.get(queryPath.toURI())));
        SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(query, SPARQLGenerateSyntax.syntaxSPARQLGenerate);
        System.out.println(q);

        GenerationPlan plan = GenerationPlanFactory.create(q);

        URL inputPath = Test.class.getResource("/"+dir+"input.json");
        String input = new String(Files.readAllBytes(Paths.get(inputPath.toURI())));
        String iri = "urn:iana:mime:application/json";
        System.out.println(input);

        Model m = plan.exec(input, iri);

        URL outPath = Test.class.getResource("/");
        File f = new File(outPath.toURI().resolve(dir+"output.ttl"));
        f.createNewFile();
        OutputStream out = new FileOutputStream(f);
        m.write(out, "TTL");
        out.close();

        m.write(System.out, "TTL");
        String uri = outPath.toURI().resolve(dir+"expected_output.ttl").toString();
        System.out.println(uri);
        Model m2 = RDFDataMgr.loadModel(uri);
        m2.write(System.out, "TTL");

        System.out.println(m.containsAll(m2) && m2.containsAll(m));

    }
}
