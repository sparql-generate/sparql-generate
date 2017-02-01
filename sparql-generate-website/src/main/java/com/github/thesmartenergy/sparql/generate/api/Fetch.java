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

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.engine.PlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import org.apache.commons.io.IOUtils;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@Path("/fetch")
public class Fetch extends HttpServlet {
    
    @GET
    public Response doFetch(
            @Context Request r,
            @Context HttpServletRequest request,
            @DefaultValue("http://localhost:8080/sparql-generate/example/example1") @QueryParam("uri") String uri,
            @DefaultValue("*/*") @QueryParam("useaccept") String useaccept,
            @DefaultValue("") @QueryParam("accept") String accept)  throws IOException {

        HttpURLConnection con;
        String message = null;
        URL obj;
        try {
            obj = new URL(uri);
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", useaccept);
            con.setInstanceFollowRedirects(true);
            System.out.println("GET to "+uri+" returned "+con.getResponseCode());
            InputStream in = con.getInputStream();
            message = IOUtils.toString(in);
        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error while trying to access message at URI <" + uri + ">: " + e.getMessage()).build();
        }

        Map<String, List<String>> headers = con.getHeaderFields();
        if(!headers.containsKey("Link")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error while processing message at URI <" + uri + ">: there should be a Link header field").build();
        }
        String var = null;
        URL qobj = null;
        for(String linkstr : headers.get("Link")) {
            Link link = Link.valueOf(linkstr);
            if(link.getRels().contains("https://w3id.org/sparql-generate/voc#spargl-query") || link.getRels().contains("spargl-query")) {
                Map<String, String> params = link.getParams();
                // check the context
                // check syntax for variable ?
                var = params.getOrDefault("var", "message");
                // ok for relative and absolute URIs ?
                try{
                    qobj = obj.toURI().resolve(link.getUri()).toURL();
                    System.out.println("found link " + qobj + " and var " + var);
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            }
            // what if multiple suitable links ? 
        } 
            
        if(var == null || qobj == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error while processing message at URI <" + uri + ">: did not find a suitable link with relation spargl-query, or https://w3id.org/sparql-generate/voc#spargl-query").build();
        }
        
        HttpURLConnection qcon;
        String query;
        try {
            qcon = (HttpURLConnection) qobj.openConnection();
            qcon.setRequestMethod("GET");
            qcon.setRequestProperty("Accept", "application/sparql-generate");
            qcon.setInstanceFollowRedirects(true);
            System.out.println("GET to " + qobj + " returned "+qcon.getResponseCode());
            InputStream in = qcon.getInputStream();
            query = IOUtils.toString(in);
        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error while trying to access query at URI <" + qobj + ">: " + e.getMessage()).build();            
        }
            
        System.out.println("got query " + query);
        
        // parse the SPARQL-Generate query and create plan
        PlanFactory factory = new PlanFactory();

        Syntax syntax = SPARQLGenerate.SYNTAX;
        SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(query, syntax);
        if(q.getBaseURI() == null) {
            q.setBaseURI("http://example.org/");
        }
        RootPlan plan = factory.create(q);

        // create the initial model
        Model model = ModelFactory.createDefaultModel();

        // if content encoding is not text-based, then use base64 encoding
        // use con.getContentEncoding() for this

        String dturi = "http://www.w3.org/2001/XMLSchema#string";
        String ct = con.getContentType();
        if(ct != null) {
            dturi = "http://www.iana.org/assignments/media-types/"+ct;
        }
       
        QuerySolutionMap initialBinding = new QuerySolutionMap();
        TypeMapper typeMapper = TypeMapper.getInstance();
        RDFDatatype dt = typeMapper.getSafeTypeByName(dturi);
        Node arqLiteral = NodeFactory.createLiteral(message, dt);
        RDFNode jenaLiteral = model.asRDFNode(arqLiteral);
        initialBinding.add(var, jenaLiteral) ; 

        // execute the plan
        plan.exec(initialBinding, model);

        System.out.println(accept);
        if(!accept.equals("text/turtle") && !accept.equals("application/rdf+xml")) {
            List<Variant> vs = Variant.mediaTypes(new MediaType("application", "rdf+xml"), new MediaType("text", "turtle")).build();
            Variant v = r.selectVariant(vs);
            accept = v.getMediaType().toString();
        }

        System.out.println(accept);
        StringWriter sw = new StringWriter();
        Response.ResponseBuilder res;
        if (accept.equals("application/rdf+xml")) {
            model.write(sw, "RDF/XML", "http://example.org/"); 
            res = Response.ok(sw.toString(), "application/rdf+xml");
            res.header("Content-Disposition", "filename= message.rdf;");
            return res.build();
        } else {
            model.write(sw, "TTL", "http://example.org/");
            res = Response.ok(sw.toString(), "text/turtle");
            res.header("Content-Disposition", "filename= message.ttl;");
            return res.build();
        }
    }

    
}
