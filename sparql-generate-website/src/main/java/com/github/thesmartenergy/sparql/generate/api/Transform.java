/*
 * Copyright 2017 École des Mines de Saint-Étienne.
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
import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@Path("/transform")
public class Transform extends HttpServlet {

    String query;
    
    @GET
    public Response doGet(
            final @DefaultValue("") @QueryParam("queryurl") String queryurl) throws IOException {
        return doTransform(queryurl);
    }

    @POST
    public Response doPost(
            final @DefaultValue("") @FormParam("queryurl") String queryurl) throws IOException {
        return doTransform(queryurl);
    }

    private Response doTransform(
            final @DefaultValue("") @QueryParam("queryurl") String queryurl) throws IOException {
        final ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            final Future<Response> f = service.submit(() -> {
                URL url = new URL(queryurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                query = IOUtils.toString(conn.getInputStream());
                RootPlan plan = PlanFactory.create(query);
                Model model = plan.exec();

                StringWriter sw = new StringWriter();
                Response.ResponseBuilder res;
                model.write(sw, "TTL", "http://example.org/");
                res = Response.ok(sw.toString(), "text/turtle");
                res.header("Content-Disposition", "filename= message.ttl;");
                return res.build();
            });
            return f.get(10, TimeUnit.SECONDS);
        } catch (final TimeoutException ex) {
            return Response.status(Response.Status.REQUEST_TIMEOUT)
                    .entity("In this api, request timeout is set at 10 s. Please"
                            + " use the executable jar instead. Query URL was: \n" + queryurl + "\n which resolves to \n" + query)
                    .build();
        } catch (final Exception ex) {
             return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An exception occurred:" + ex.getMessage())
                    .build();
        } finally {
            service.shutdown();
        }
    }

}
