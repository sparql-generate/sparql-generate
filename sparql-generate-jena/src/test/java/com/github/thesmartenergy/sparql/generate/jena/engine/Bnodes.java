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
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import java.io.File;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.LocationMapper;
import org.apache.jena.util.Locator;
import org.apache.jena.util.LocatorFile;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class Bnodes {

    static Logger LOG;
    public Bnodes() {

    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        LOG = Logger.getLogger(Bnodes.class);
        LOG.debug(Bnodes.class.getName());
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testbnode1() throws Exception {
        test("bnode1"); 
    }

    @Test
    public void testbnode2() throws Exception {
        test("bnode2"); 
    }

    public void test(String value) throws Exception {
        URL examplePath = Bnodes.class.getResource("/" + value);
        File exampleDir = new File(examplePath.toURI());

        // read location-mapping
        URI confUri = exampleDir.toURI().resolve("configuration.ttl");
        Model conf = RDFDataMgr.loadModel(confUri.toString());

        // initialize file manager
        FileManager fileManager = FileManager.makeGlobal();
        Locator loc = new LocatorFile(exampleDir.toURI().getPath());
        LocationMapper mapper = new LocationMapper(conf);
        fileManager.addLocator(loc);
        fileManager.setLocationMapper(mapper);

        String qstring = IOUtils.toString(fileManager.open("query.rqg"), "UTF-8");
        SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(qstring, SPARQLGenerate.SYNTAX);

        // create generation plan
        PlanFactory factory = new PlanFactory(fileManager);
        RootPlan plan = factory.create(q);
        Model output = plan.exec();

        // write output
        output.write(System.out, "TTL");

        Model expectedOutput = fileManager.loadModel("expected_output.ttl");
        StringWriter sw = new StringWriter();
        expectedOutput.write(sw, "TTL");
        LOG.debug("\n"+sw.toString());

        Assert.assertTrue(output.isIsomorphicWith(expectedOutput));
    }

}
