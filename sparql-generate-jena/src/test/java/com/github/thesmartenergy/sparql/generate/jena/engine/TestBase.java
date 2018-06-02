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
package com.github.thesmartenergy.sparql.generate.jena.engine;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.cli.Request;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import com.github.thesmartenergy.sparql.generate.jena.stream.LocatorFileAccept;
import com.github.thesmartenergy.sparql.generate.jena.stream.LookUpRequest;
import com.github.thesmartenergy.sparql.generate.jena.stream.SPARQLGenerateStreamManager;
import com.github.thesmartenergy.sparql.generate.jena.utils.SPARQLGenerateUtils;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import static org.junit.Assert.assertTrue;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@RunWith(Parameterized.class)
public class TestBase {

    private static final Logger LOG = LoggerFactory.getLogger(TestBase.class);
    private static final Gson gson = new Gson();

    private final Logger log;

    private final File exampleDir;
    private Request request;
    
    private static final String pattern = "";
//    private static final String pattern = ".*";

    
    public TestBase(Logger log, File exampleDir, String name) {
        this.log = log;
        this.exampleDir = exampleDir;

        log.info("constructing with " + exampleDir);

        SPARQLGenerate.init();
                
        // read sparql-generate-conf.json
        
        try {
            String conf = IOUtils.toString(new FileInputStream(new File(exampleDir, "sparql-generate-conf.json")), "utf-8");
            request = gson.fromJson(conf, Request.class);
            if(request.query == null) 
                request.query = "query.rqg";
            if(request.graph == null) 
                request.graph = "graph.ttl";
        } catch(Exception ex) {
            LOG.warn("Error while loading the location mapping model for the queryset. No named queries will be used");
            request = Request.DEFAULT;
        }

        // initialize stream manager
        SPARQLGenerateStreamManager sm = SPARQLGenerateStreamManager.makeStreamManager(new LocatorFileAccept(exampleDir.toURI().getPath()));
        sm.setLocationMapper(request.asLocationMapper());
        SPARQLGenerate.setStreamManager(sm);
    }

    @Test
    public void testPlanExecution() throws Exception {
        
        String query = IOUtils.toString(SPARQLGenerate.getStreamManager().open(new LookUpRequest(request.query, SPARQLGenerate.MEDIA_TYPE)), "UTF-8");
        
        long start0 = System.currentTimeMillis();
        long start = start0;
        SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(query, SPARQLGenerate.SYNTAX);
        long now = System.currentTimeMillis();
        log.info("needed " + (now - start) + " to parse query");
        start = now;

        // create generation plan
        PlanFactory factory = new PlanFactory();
        RootPlan plan = factory.create(q);        
        Dataset ds = SPARQLGenerateUtils.loadDataset(exampleDir, request);
        
        now = System.currentTimeMillis();
        log.info("needed " + (now - start) + " to get ready");
        start = now;

        // execute plan
        Model output = plan.exec(ds);

        now = System.currentTimeMillis();
        log.info("executed plan in " + (now - start));
        start = now;
        log.info("total needed " + (now - start0));

        // write output
        String fileName = exampleDir.toString() + "/output.ttl";
        FileWriter out = new FileWriter(fileName);
        try {
            output.write(out, "TTL");
//            StringWriter sw = new StringWriter();
//            output.write(sw, "TTL");
//            log.debug("output is " + sw.toString());
        } finally {
            try {
                out.close();
            } catch (IOException closeException) {
                log.error("Error while writing to file");
            }
        }

        URI expectedOutputUri = exampleDir.toURI().resolve("expected_output.ttl");
        Model expectedOutput = RDFDataMgr.loadModel(expectedOutputUri.toString(), Lang.TTL);
        StringWriter sw = new StringWriter();
        expectedOutput.write(sw, "TTL");
        assertTrue("Error with test " + exampleDir, output.isIsomorphicWith(expectedOutput));
    }

    void testQuerySerialization() throws Exception {
//        String qstring = IOUtils.toString(StreamManager.get().open("query.rqg"), "UTF-8");
//        SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(qstring, SPARQLGenerate.SYNTAX);
//        LOG.debug(qstring);
//
//        // serialize query 
//        URI queryOutputUri = exampleDir.toURI().resolve("query_serialized.rqg");
//        File queryOutputFile = new File(queryOutputUri);
//        try (OutputStream queryOutputStream = new FileOutputStream(queryOutputFile)) {
//            queryOutputStream.write(q.toString().getBytes("UTF-8"));
//        }
//        LOG.debug(q);
//
//        SPARQLGenerateQuery q2 = (SPARQLGenerateQuery) QueryFactory.create(q.toString(), SPARQLGenerate.SYNTAX);
//        LOG.debug(q2);
//        assertTrue(q.equals(q2));
    }

    void testQueryNormalization() throws Exception {
//        String qstring = IOUtils.toString(StreamManager.get().open("query.rqg"), "UTF-8");
//        SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(qstring, SPARQLGenerate.SYNTAX);
//        LOG.debug(qstring);
//
//        // normalize query 
//        URI queryOutputUri = exampleDir.toURI().resolve("query_normalized.rqg");
//        File queryOutputFile = new File(queryOutputUri);
//        
//        SPARQLGenerateQuery q2 = q.normalize();
//                
//        try (OutputStream queryOutputStream = new FileOutputStream(queryOutputFile)) {
//            queryOutputStream.write(q2.toString().getBytes("UTF-8"));
//        }
//        LOG.debug(q2);
    }

    @Parameters(name = "Test {2}")
    public static Collection<Object[]> data() {
        
        try {
            Collection<Object[]> data = new ArrayList<Object[]>();

            File tests = new File(TestBase.class.getClassLoader().getResource("").toURI());
            LOG.trace(tests.getAbsolutePath());

            File[] files = tests.listFiles();
            Arrays.sort(files);
            for (File exampleDir : files) {
                if(!exampleDir.getName().matches(pattern)){
                    continue;
                }
                if (exampleDir.isDirectory() && !exampleDir.getName().equals("com")) {
                    Logger log = LoggerFactory.getLogger(exampleDir.getName());
                    Object[] fileArg1 = new Object[]{log, exampleDir, exampleDir.getName()};
                    data.add(fileArg1);
                }
            }
            return data;
        } catch (URISyntaxException ex) {
            java.util.logging.Logger.getLogger(TestBase.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
