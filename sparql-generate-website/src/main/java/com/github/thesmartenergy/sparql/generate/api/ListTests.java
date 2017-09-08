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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@Path("/list")
public class ListTests extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(ListTests.class.getSimpleName());

    @GET
    public Response doGet() throws IOException, URISyntaxException {
        File tests = new File(ListTests.class.getClassLoader().getResource("tests").toURI());
        
        StringBuilder sb = new StringBuilder();
        File[] files = tests.listFiles();
        Arrays.sort(files);
        for (File test : files) {
            if(test.isDirectory()) {
                sb.append(test.getName()+"\n");
            }
        }
                
        Response.ResponseBuilder res = Response.ok(sb.toString(), "text/plain");
        return res.build();

    }

    @GET
    @Path("/{id}")
    public Response doGet(@PathParam("id") String id) throws IOException, URISyntaxException {
        File test = new File(ListTests.class.getClassLoader().getResource("tests/"+id).toURI());
        
        StringBuilder sb = new StringBuilder();
        
        sb.append(IOUtils.toString(new FileReader(new File(test, "query.rqg"))));
        sb.append("**********");
        
        Model conf = ModelFactory.createDefaultModel();
        conf.read(new FileInputStream(new File(test, "configuration.ttl")), "http://example.org" ,"TTL");
        ResIterator resit = conf.listSubjectsWithProperty(ResourceFactory.createProperty("http://jena.hpl.hp.com/2004/08/location-mapping#name"));
        while(resit.hasNext()) {
            try {
                Resource subject = resit.next();
                String key = subject.getProperty(ResourceFactory.createProperty("http://jena.hpl.hp.com/2004/08/location-mapping#name")).getString();
                System.out.println(key);
                sb.append(key);
                sb.append("%%%%%%%%%%");
                String altName = subject.getProperty(ResourceFactory.createProperty("http://jena.hpl.hp.com/2004/08/location-mapping#altName")).getString();
                String value = IOUtils.toString(new FileInputStream(new File(test, altName)));
                sb.append(value);
                sb.append("**********");
            } catch(Exception ex) {
                System.out.println(ex.toString());
            }
        }
        
        Response.ResponseBuilder res = Response.ok(sb.toString(), "text/plain");
        return res.build();

    }

}
