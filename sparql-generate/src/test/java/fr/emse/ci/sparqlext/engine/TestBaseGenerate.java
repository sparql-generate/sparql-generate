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
package fr.emse.ci.sparqlext.engine;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.utils.Request;
import fr.emse.ci.sparqlext.stream.LocatorFileAccept;
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import org.apache.jena.sparql.util.Context;
import static org.junit.Assert.assertTrue;

/**
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@RunWith(Parameterized.class)
public class TestBaseGenerate {

    private static final Logger LOG = LoggerFactory.getLogger(TestBaseGenerate.class);
    private static final Gson gson = new Gson();

    private final Logger log;

    private final File exampleDir;
    private final String name;
    private Request request;
    private SPARQLExtStreamManager sm;

    private static final String pattern = "geoj.*";

    public TestBaseGenerate(Logger log, File exampleDir, String name) {
        this.log = log;
        this.exampleDir = exampleDir;
        this.name = name;

        log.info("constructing with " + exampleDir);

        SPARQLExt.init();

        // read sparql-generate-conf.json
        try {
            String conf = IOUtils.toString(new FileInputStream(new File(exampleDir, "sparql-generate-conf.json")), "utf-8");
            request = gson.fromJson(conf, Request.class);
            if (request.query == null) {
                request.query = "query.rqg";
            }
            if (request.graph == null) {
                request.graph = "graph.ttl";
            }
        } catch (Exception ex) {
            LOG.warn("Error while loading the location mapping model for the queryset. No named queries will be used");
            request = Request.DEFAULT;
        }
        // initialize stream manager
        sm = SPARQLExtStreamManager.makeStreamManager(new LocatorFileAccept(exampleDir.toURI().getPath()));
        sm.setLocationMapper(request.asLocationMapper());
    }

    @Test
    public void testPlanExecution() throws Exception {

        String query = IOUtils.toString(sm.open(new LookUpRequest(request.query, SPARQLExt.MEDIA_TYPE)), "UTF-8");

        long start0 = System.currentTimeMillis();
        long start = start0;
        SPARQLExtQuery q = (SPARQLExtQuery) QueryFactory.create(query, SPARQLExt.SYNTAX);
        long now = System.currentTimeMillis();
        log.info("needed " + (now - start) + " to parse query");
        start = now;

        // create generation plan
        RootPlan plan = PlanFactory.create(q);
        Dataset ds = SPARQLExt.loadDataset(exampleDir, request);

        now = System.currentTimeMillis();
        log.info("needed " + (now - start) + " to get ready");
        start = now;

        // execute plan

        ExecutorService executor = new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
            (ForkJoinPool pool) -> {
                final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                worker.setName("test-" + name + "-" + worker.getPoolIndex());
                return worker;
            },
            null, true);

        ScheduledExecutorService guard = Executors.newScheduledThreadPool(1);
        guard.schedule(()->{executor.shutdownNow();}, 15, TimeUnit.SECONDS);
        Model output;
        Context context = SPARQLExt.createContext(q.getPrefixMapping(), sm, executor);
        output = plan.execGenerate(ds, context);
        guard.shutdownNow();

        now = System.currentTimeMillis();
        log.info("executed plan in " + (now - start));
        start = now;
        log.info("total needed " + (now - start0));

        // write output
        String fileName = exampleDir.toString() + "/output.ttl";
        FileWriter out = new FileWriter(fileName);
        try {
            output.write(out, "TTL");
            StringWriter sw = new StringWriter();
            output.write(sw, "TTL");
            LOG.debug("output is \n" + sw.toString());
        } finally {
            try {
                out.close();
            } catch (IOException closeException) {
                log.error("Error while writing to file");
            }
        }

        URI expectedOutputUri = exampleDir.toURI().resolve("expected_output.ttl");
        Model expectedOutput = RDFDataMgr.loadModel(expectedOutputUri.toString(), Lang.TTL);
//        StringWriter sw = new StringWriter();
//        expectedOutput.write(System.out, "TTL");
        System.out.println("Is isomorphic: " + output.isIsomorphicWith(expectedOutput));
        if (!output.isIsomorphicWith(expectedOutput)) {
            output.listStatements().forEachRemaining((s) -> {
                if (!expectedOutput.contains(s)) {
                    LOG.debug("expectedOutput does not contain " + s);
                }
                expectedOutput.remove(s);
            });
            expectedOutput.listStatements().forEachRemaining((s) -> {
                LOG.debug("output does not contain " + s);
            });
        }

        assertTrue("Error with test " + exampleDir.getName(), output.isIsomorphicWith(expectedOutput));
    }

    @Parameters(name = "Test {2}")
    public static Collection<Object[]> data() {

        try {
            Collection<Object[]> data = new ArrayList<>();

            File tests = new File(TestBaseGenerate.class.getClassLoader().getResource("").toURI());
            LOG.trace(tests.getAbsolutePath());

            File[] files = tests.listFiles();
            Arrays.sort(files);
            for (File exampleDir : files) {
                if (!exampleDir.getName().matches(pattern)) {
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
            java.util.logging.Logger.getLogger(TestBaseGenerate.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
