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
package com.github.thesmartenergy.sparql.generate.api;

import com.github.thesmartenergy.sparql.generate.jena.engine.PlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.system.stream.StreamManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@Path("/transform")
public class Transform extends HttpServlet {

    private static final Logger LOG = LogManager.getLogger(Transform.class);

    private TransformUtils utils;

    @PostConstruct
    private void postConstruct() {
        utils = new TransformUtils(UUID.randomUUID().toString(), LOG);
    }
    
    @GET
    @Produces("application/json")
    public Response doGet(
            @Context Request r,
            @Context HttpServletRequest request,
            @DefaultValue("") @QueryParam("query") String query,
            @DefaultValue("") @QueryParam("queryurl") String queryurl,
            @DefaultValue("") @QueryParam("defaultGraph") String defaultGraph,
            @DefaultValue("") @QueryParam("documentset") String documentset,
            @DefaultValue("warn") @QueryParam("logLevel") String logLevel) throws IOException {
        return doTransform(r, request, query, queryurl, defaultGraph, documentset, logLevel);
    }

    @POST
    @Produces("application/json")
    public Response doPost(
            @Context Request r,
            @Context HttpServletRequest request,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("queryurl") String queryurl,
            @DefaultValue("") @FormParam("defaultGraph") String defaultGraph,
            @DefaultValue("") @FormParam("documentset") String documentset,
            @DefaultValue("warn") @FormParam("logLevel") String logLevel) throws IOException {
        return doTransform(r, request, query, queryurl, defaultGraph, documentset, logLevel);
    }

    private Response doTransform(Request r, HttpServletRequest request, String query, String queryurl, String defaultGraph, String documentset, String logLevel) {
        if (query.equals("") && queryurl.equals("")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("At least one of query or queryurl query parameters must be set.").build();
        }
        if (!queryurl.equals("") && !query.equals("")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("At most one of query or queryurl query parameters must be set.").build();
        }
               

        final StringWriter swLog = utils.setLogger(logLevel);

        try {
            utils.setStreamManager(documentset);
        } catch (Exception ex) {
            LOG.error("Error while processing the documentset", ex);
            return createResponse(Response.Status.BAD_REQUEST, null, swLog);
        }

        final StringWriter swOut = new StringWriter();
        try {
            if (!queryurl.equals("")) {
                query = IOUtils.toString(StreamManager.get().open(queryurl), "UTF-8");
            }
        } catch (IOException | NullPointerException ex) {
            LOG.error("Error while loading the query", ex);
            return createResponse(Response.Status.INTERNAL_SERVER_ERROR, null, swLog);
        }

        final Model inputModel = utils.setInputModel(defaultGraph);

        final RootPlan plan;
        try {
            plan = PlanFactory.create(query);
        } catch (Exception e) {
            LOG.error("Exception while processing the request", e);
            return createResponse(Response.Status.BAD_REQUEST, swOut, swLog);
        }

        return execFull(plan, inputModel, swOut, swLog);
    }


    private Response createResponse(Response.Status status, StringWriter swOut, StringWriter swLog) {
        utils.removeAppender();
        final Map<String, String> response = new HashMap<>();
        if (swOut != null) {
            response.put("output", swOut.toString());
        }
        response.put("log", swLog.toString());
        return Response.status(status)
                .entity(new Gson().toJson(response))
                .type("application/json")
                .build();
    }

    private Response execFull(RootPlan plan, Model inputModel, StringWriter swOut, StringWriter swLog) {
        try {
            Model model = plan.exec(inputModel);
            model.write(swOut, "TTL", "http://example.org/");
            return createResponse(Response.Status.OK, swOut, swLog);
        } catch (Exception ex) {
            LOG.error("Exception while executing the plan", ex);
            return createResponse(Response.Status.INTERNAL_SERVER_ERROR, swOut, swLog);
        }
    }
}
