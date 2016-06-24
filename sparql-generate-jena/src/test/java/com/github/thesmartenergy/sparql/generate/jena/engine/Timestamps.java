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
package com.github.thesmartenergy.sparql.generate.jena.engine;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import static com.github.thesmartenergy.sparql.generate.jena.engine.TestBase.fileManager;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class Timestamps extends TestBase {

    public Timestamps() {

    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        TestBase.setUpClass(Timestamps.class);
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
    public void testPlanExecution() throws Exception {
        String query = IOUtils.toString(fileManager.open("query.rqg"), "UTF-8");
        SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(query, SPARQLGenerate.SYNTAX);

        // create generation plan
        PlanFactory factory = new PlanFactory(fileManager);
        RootPlan plan = factory.create(q);
        Model output = ModelFactory.createDefaultModel();
        QuerySolutionMap initialBinding = null;

        // execute plan
        plan.exec(initialBinding, output);

        // write output
        output.write( System.out, "TTL" );

    }    
    
}
