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
package fr.emse.ci.sparqlext.jersey.resources;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.generate.engine.PlanFactory;
import fr.emse.ci.sparqlext.generate.engine.RootPlanImpl;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class Normalize {

    private static final Logger LOG = LoggerFactory.getLogger(Normalize.class);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/string")
    public QueryRequest toString(QueryRequest greeting) {
        LOG.info("toString " + greeting.getQuery());
        String query = greeting.getQuery();
        if (query == null) {
            return new QueryRequest();
        }
        try {
            SPARQLExtQuery q = (SPARQLExtQuery) QueryFactory.create(query, SPARQLExt.SYNTAX);
            LOG.info(q.toString());
            return new QueryRequest(q.toString(), true, null, null, null);
        } catch (Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            return new QueryRequest(null, false, errors.toString(), null, null);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/normalize")
    public QueryRequest normalize(QueryRequest greeting) {
        LOG.info("normalize " + greeting.getQuery());
        String query = greeting.getQuery();
        if (query == null) {
            return null;
        }
        try {
            SPARQLExtQuery q = (SPARQLExtQuery) QueryFactory.create(query, SPARQLExt.SYNTAX);
            q.normalize();
            return new QueryRequest(q.toString(), true, null, null, null);
        } catch (Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            return new QueryRequest(null, false, errors.toString(), null, null);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/selectQuery")
    public QueryRequest format(QueryRequest greeting) {
        LOG.info("selectQuery " + greeting.getQuery());
        String query = greeting.getQuery();
        if (query == null) {
            return null;
        }
        try {
            SPARQLExtQuery q = (SPARQLExtQuery) QueryFactory.create(query, SPARQLExt.SYNTAX);
            RootPlanImpl plan = (RootPlanImpl) PlanFactory.create(q);
            SPARQLExtQuery sq = (SPARQLExtQuery) QueryFactory.create(plan.selectPlan.select.toString(), SPARQLExt.SYNTAX);
            return new QueryRequest(plan.selectPlan.select.toString(), true, null, null, null);

        } catch (Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            return new QueryRequest(null, false, errors.toString(), null, null);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/execSelect")
    public QueryRequest execSelect(QueryRequest greeting) {
        LOG.info("execSelect " + greeting.getQuery());
        String query = greeting.getQuery();
        if (query == null) {
            return null;
        }
        String graph = greeting.getGraph();
        if (graph == null) {
            return null;
        } 
        try {
            SPARQLExtQuery q = (SPARQLExtQuery) QueryFactory.create(query, SPARQLExt.SYNTAX);

            RootPlanImpl plan = (RootPlanImpl) PlanFactory.create(q);
            Model m = ModelFactory.createDefaultModel();
            RDFDataMgr.read(m, IOUtils.toInputStream(graph, "UTF-8"), "http://example.org/", Lang.TTL);

            System.out.println(plan.selectPlan.select.toString());

            ResultSet out = plan.execSelect(m);
            return new QueryRequest(plan.selectPlan.select.toString(), true, ResultSetFormatter.asText(out), null, null);

        } catch (Exception ex) {

            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            return new QueryRequest(null, false, errors.toString(), null, null);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/execTemplate")
    public QueryRequest execTemplate(QueryRequest greeting) {
        LOG.info("execTemplate " + greeting.getQuery());
        String query = greeting.getQuery();
        if (query == null) {
            return null;
        }
        String graph = greeting.getGraph();
        if (graph == null) {
            return null;
        } 
        try {
            SPARQLExtQuery q = (SPARQLExtQuery) QueryFactory.create(query, SPARQLExt.SYNTAX);

            RootPlanImpl plan = (RootPlanImpl) PlanFactory.create(q);
            Model m = ModelFactory.createDefaultModel();
            RDFDataMgr.read(m, IOUtils.toInputStream(graph, "UTF-8"), "http://example.org/", Lang.TTL);

            System.out.println(plan.selectPlan.select.toString());

            String out = plan.execTemplate(m);
            return new QueryRequest(plan.selectPlan.select.toString(), true, out, null, null);

        } catch (Exception ex) {

            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            return new QueryRequest(null, false, errors.toString(), null, null);
        }
    }
}
