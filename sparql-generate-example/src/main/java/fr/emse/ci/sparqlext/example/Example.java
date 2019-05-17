/*
 * Copyright 2019 École des Mines de Saint-Étienne.
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
package fr.emse.ci.sparqlext.example;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.SPARQLExtCli;
import fr.emse.ci.sparqlext.generate.engine.PlanFactory;
import fr.emse.ci.sparqlext.generate.engine.RootPlan;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.stream.LocationMapperAccept;
import fr.emse.ci.sparqlext.stream.LocatorFileAccept;
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public class Example {

    private static final Logger LOG = LoggerFactory.getLogger(Example.class);

    public static void main(String[] args) throws IOException {
        // Parse a query as follows:
        String queryString = IOUtils.toString(new FileInputStream("resources/query.rqg"), StandardCharsets.UTF_8);
        SPARQLExtQuery query = (SPARQLExtQuery) QueryFactory.create(queryString, SPARQLExt.SYNTAX);
        LOG.info("query is\n" + query.toString());

        // create the plan
        RootPlan plan = PlanFactory.create(query);

        // read the default graph
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model,
                new FileInputStream("resources/default-graph.ttl"), Lang.TURTLE);

        // output the default graph
        StringWriter sb = new StringWriter();
        RDFDataMgr.write(sb, model, Lang.TURTLE);
        LOG.info("default graph is\n" + sb.toString());

        // set up the stream manager
        LocatorFileAccept locator = new LocatorFileAccept(new File("resources").toURI().getPath());
        LocationMapperAccept mapper = new LocationMapperAccept();
        for (int i = 0; i < 11; i++) {
            mapper.addAltEntry(
                    "https://example.com/station/" + i, "documentset/station" + i + ".txt");

        }
        SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator, mapper);

        // output a message 
        TypedInputStream tin = sm.open(new LookUpRequest("https://example.com/station/0"));
        LOG.info("message is\n" + IOUtils.toString(tin, StandardCharsets.UTF_8));

        // create context
        Context context = SPARQLExt.createContext(sm);

        long start = System.currentTimeMillis();
        // execute the plan
        Model output = plan.execGenerate(model, context);
        long end = System.currentTimeMillis();

        // output model
        StringWriter sboutput = new StringWriter();
        RDFDataMgr.write(sboutput, output, Lang.TURTLE);
        LOG.info("query is\n" + sboutput.toString());
        LOG.info("transformation time (ms): " + (end - start));
    }

}
