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
package fr.emse.ci.sparqlext.api;


import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import fr.emse.ci.sparqlext.api.entities.Request;
import fr.emse.ci.sparqlext.cli.FileConfigurations;
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
@Path("/example")
public class ListExercises extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ListExercises.class);
    private static final Gson GSON = new Gson();

    private static enum TYPE {
        GENERATE("examples/generate"),
        SELECT("examples/select"),
        TEMPLATE("examples/template");
        private final String dir;

        private TYPE(String dir) {
            this.dir = dir;
        }
    }

    @GET
    @Path("/generate")
    public Response doListGenerate() throws IOException, URISyntaxException {
        return doList(TYPE.GENERATE);
    }

    @GET
    @Path("/select")
    public Response doListSelect() throws IOException, URISyntaxException {
        return doList(TYPE.SELECT);
    }

    @GET
    @Path("/template")
    public Response doListTemplate() throws IOException, URISyntaxException {
        return doList(TYPE.TEMPLATE);
    }

    @GET
    @Path("/generate/{id}")
    public Response doGetGenerate(@PathParam("id") String id) throws IOException, URISyntaxException, Exception {
        return doGet(TYPE.GENERATE, id);
    }

    @GET
    @Path("/select/{id}")
    public Response doGetSelect(@PathParam("id") String id) throws IOException, URISyntaxException, Exception {
        return doGet(TYPE.SELECT, id);
    }

    @GET
    @Path("/template/{id}")
    public Response doGetTemplate(@PathParam("id") String id) throws IOException, URISyntaxException, Exception {
        return doGet(TYPE.TEMPLATE, id);
    }

    private Response doList(TYPE type) throws URISyntaxException {
        File exercises = new File(ListExercises.class.getClassLoader().getResource(type.dir).toURI());

        File[] files = exercises.listFiles();
        Arrays.sort(files);

        List<String> names = new ArrayList<>();
        for (File exercise : files) {
            if (exercise.isDirectory()) {
                names.add(exercise.getName());
            }
        }
        Response.ResponseBuilder res = Response.ok(GSON.toJson(names), "application/json");
        return res.build();

    }

    private Response doGet(TYPE type, String id) throws URISyntaxException {
        LOG.info("Loading: " + id);
        File dir = new File(ListExercises.class.getClassLoader().getResource(type.dir + "/" + id).toURI());

        // read sparql-generate-conf.json
        FileConfigurations config;
        try {
            String conf = IOUtils.toString(new FileInputStream(new File(dir, "sparql-generate-conf.json")), "utf-8");
            config = GSON.fromJson(conf, FileConfigurations.class);
        } catch (IOException | JsonSyntaxException ex) {
            LOG.warn("Error while loading the location mapping model for the queryset. No named queries will be used");
            config = FileConfigurations.DEFAULT;
        }

        // load strings
        Request request = new Request(config, dir);

        Response.ResponseBuilder res = Response.ok(GSON.toJson(request), "application/json");
        return res.build();
    }

}
