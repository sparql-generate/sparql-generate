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
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static javax.management.Query.value;
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
import static org.apache.jena.vocabulary.VCARD4.key;
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

        Gson gson = new GsonBuilder().create();

        JsonObject output = new JsonObject();
        output.add("queryset", getQueryset(test));
        output.add("dataset", getDataset(test));
        output.add("documentset", getEltset(test, "documentset", "doc"));

        Response.ResponseBuilder res = Response.ok(gson.toJson(output), "application/json");
        return res.build();
    }

    private JsonElement getQueryset(File test) throws Exception {
        JsonArray eltset = new JsonArray();
        try {
            eltset.add(new JsonPrimitive(IOUtils.toString(new BOMInputStream(new FileInputStream(new File(test, "query.rqg"))), "UTF-8")));
        } catch (Exception ex) {
            eltset.add(new JsonPrimitive(""));
            LOG.warn(ex.getMessage());
        }
        eltset.add(getEltset(test, "queryset", "query"));
        return eltset;
    }

    private JsonElement getDataset(File test) {
        JsonArray eltset = new JsonArray();
        try {
            eltset.add(new JsonPrimitive(IOUtils.toString(new BOMInputStream(new FileInputStream(new File(test, "default.ttl"))), "UTF-8")));
        } catch (Exception ex) {
            eltset.add(new JsonPrimitive(""));
            LOG.warn(ex.getMessage());
        }
        eltset.add(getEltset(test, "dataset", "graph"));
        return eltset;
    }

    private JsonElement getEltset(File test, String dirname, String key) {
        File dir = new File(test, dirname);
        JsonArray eltset = new JsonArray();

        try {

            Model conf = ModelFactory.createDefaultModel();
            conf.read(new FileInputStream(new File(dir, "configuration.ttl")), "http://example.org", "TTL");

            ResIterator resit = conf.listSubjectsWithProperty(ResourceFactory.createProperty("http://jena.hpl.hp.com/2004/08/location-mapping#name"));
            while (resit.hasNext()) {
                Resource subject = resit.next();
                JsonObject elt = new JsonObject();
                elt.addProperty("uri", subject.getProperty(ResourceFactory.createProperty("http://jena.hpl.hp.com/2004/08/location-mapping#name")).getString());
                elt.addProperty("mediatype", subject.getProperty(ResourceFactory.createProperty("http://jena.hpl.hp.com/2004/08/location-mapping#media")).getString());
                String altName = subject.getProperty(ResourceFactory.createProperty("http://jena.hpl.hp.com/2004/08/location-mapping#altName")).getString();
                try {
                    String value = IOUtils.toString(new BOMInputStream(new FileInputStream(new File(test, altName))), "UTF-8");
                    elt.addProperty(key, value);
                } catch (Exception ex) {
                    LOG.warn(ex.getMessage());
                }
            }
        } catch (Exception ex) {
            LOG.warn(ex.getMessage());
        }
        return eltset;
    }

}
