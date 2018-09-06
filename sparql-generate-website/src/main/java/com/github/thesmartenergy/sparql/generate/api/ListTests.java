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

import com.github.thesmartenergy.sparql.generate.jena.cli.Request;
import com.google.gson.Gson;
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
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@Path("/tests")
public class ListTests extends HttpServlet {
    
    private static final Logger LOG = LoggerFactory.getLogger(ListTests.class);
    private static final Gson gson = new Gson();

    @GET
    public Response doGet() throws IOException, URISyntaxException {
        File tests = new File(ListTests.class.getClassLoader().getResource("tests").toURI());

        File[] files = tests.listFiles();
        Arrays.sort(files);

        List<String> names = new ArrayList<>();
        for (File test : files) {
            if (test.isDirectory()) {
                names.add(test.getName());
            }
        }

        Response.ResponseBuilder res = Response.ok(gson.toJson(names), "application/json");
        return res.build();

    }

    @GET
    @Path("/{id}")
    public Response doGet(@PathParam("id") String id) throws IOException, URISyntaxException, Exception {
        File dir = new File(ListTests.class.getClassLoader().getResource("tests/" + id).toURI());

        // read sparql-generate-conf.json
        Request request;
        try {
            String conf = IOUtils.toString(new FileInputStream(new File(dir, "sparql-generate-conf.json")), "utf-8");
            request = gson.fromJson(conf, Request.class);
        } catch(Exception ex) {
            LOG.warn("Error while loading the location mapping model for the queryset. No named queries will be used");
            request = Request.DEFAULT;
        }

        // load strings
        request.loadStrings(dir);
        
        Response.ResponseBuilder res = Response.ok(gson.toJson(request), "application/json");
        return res.build();
    }

}
