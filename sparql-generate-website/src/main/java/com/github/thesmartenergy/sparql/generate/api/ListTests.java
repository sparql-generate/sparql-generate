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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@Path("/list")
public class ListTests extends HttpServlet {

    private static final Logger LOG = LogManager.getLogger(ListTests.class);

    @GET
    public Response doGet() throws IOException, URISyntaxException {
        File tests = new File(ListTests.class.getClassLoader().getResource("tests").toURI());

        StringBuilder sb = new StringBuilder();
        File[] files = tests.listFiles();
        Arrays.sort(files);
        for (File test : files) {
            if (test.isDirectory()) {
                sb.append(test.getName() + "\n");
            }
        }

        Response.ResponseBuilder res = Response.ok(sb.toString(), "text/plain");
        return res.build();

    }

    @GET
    @Path("/{id}")
    public Response doGet(@PathParam("id") String id) throws IOException, URISyntaxException, Exception {
        File test = new File(ListTests.class.getClassLoader().getResource("tests/" + id).toURI());

        Request request = new Request();

        try {
            request.defaultquery = IOUtils.toString(new BOMInputStream(new FileInputStream(new File(test, "query.rqg"))), "UTF-8");
        } catch (Exception ex) {
            request.defaultquery = "";
            LOG.warn(ex.getMessage());
        }
        request.namedqueries = getDocuments(test, "queryset");
        try {
            request.defaultgraph = IOUtils.toString(new BOMInputStream(new FileInputStream(new File(test, "default.ttl"))), "UTF-8");
        } catch (Exception ex) {
            request.defaultgraph = "";
            LOG.warn(ex.getMessage());
        }
        request.namedgraphs = getDocuments(test, "dataset");
        request.documentset = getDocuments(test, "documentset");

        Response.ResponseBuilder res = Response.ok(new Gson().toJson(request), "application/json");
        return res.build();
    }

    private List<Document> getDocuments(File test, String dirname) {
        File dir = new File(test, dirname);
        List<Document> documents = new ArrayList<>();

        try {

            Model conf = ModelFactory.createDefaultModel();
            conf.read(new FileInputStream(new File(dir, "configuration.ttl")), "http://example.org", "TTL");

            ResIterator resit = conf.listSubjectsWithProperty(ResourceFactory.createProperty("http://jena.hpl.hp.com/2004/08/location-mapping#name"));
            while (resit.hasNext()) {
                Resource subject = resit.next();
                Document document = new Document();
                document.uri = subject.getProperty(ResourceFactory.createProperty("http://jena.hpl.hp.com/2004/08/location-mapping#name")).getString();
                document.mediatype = subject.getProperty(ResourceFactory.createProperty("http://jena.hpl.hp.com/2004/08/location-mapping#media")).getString();
                String altName = subject.getProperty(ResourceFactory.createProperty("http://jena.hpl.hp.com/2004/08/location-mapping#altName")).getString();
                try {
                    document.string = IOUtils.toString(new BOMInputStream(new FileInputStream(new File(test, altName))), "UTF-8");
                } catch (Exception ex) {
                    LOG.warn(ex.getMessage());
                }
                documents.add(document);
            }
        } catch (Exception ex) {
            LOG.warn(ex.getMessage());
        }
        return documents;
    }

}
