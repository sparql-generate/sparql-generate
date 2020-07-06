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
package fr.mines_stetienne.ci.sparql_generate.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.mines_stetienne.ci.sparql_generate.JerseyApp;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.engine.RootPlan;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.stream.LookUpRequest;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 *
 * @author Maxime Lefrançois
 */
@Path("/transform")
public class Transform extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(Transform.class);

    String query;
    
    @GET
    public Response doGet(
            final @QueryParam("query") String query,
            final @QueryParam("queryurl") String queryurl,
            final @QueryParam("param") List<String> params) throws ServerErrorException {
        return doTransform(query, queryurl, params);
    }

    @POST
    public Response doPost(
            final @FormParam("query") String query,
            final @FormParam("queryurl") String queryurl,
            final @FormParam("param") List<String> params) throws WebApplicationException {
        return doTransform(query, queryurl, params);
    }

    
    private Response doTransform(
            String query, String queryurl, List<String> params) {
    	if(query == null && queryurl == null) {
    		throw new BadRequestException("One of parameters query or queryurl must be set.");
    	}
    	if(query != null && queryurl != null) {
    		throw new BadRequestException("Only one of parameters query or queryurl must be set.");
    	}
    	if(queryurl != null) {
    		LookUpRequest request = new LookUpRequest(queryurl, SPARQLExt.MEDIA_TYPE);
    		SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager();
    		try (TypedInputStream tin = sm.open(request); InputStream in = tin.getInputStream()){
    			query = IOUtils.toString(in, StandardCharsets.UTF_8);
    		} catch(IOException ex) {
    			throw new BadRequestException(String.format("Could not lookup request %s", queryurl), ex);
    		}
    	}
    	
		SPARQLExtQuery q;
    	RootPlan plan;
		try {
			q = (SPARQLExtQuery) QueryFactory.create(query, SPARQLExt.SYNTAX);
    		plan = PlanFactory.create(q);
		} catch (Exception ex) {
        	throw new BadRequestException("Exception while building the plan for the query " + query, ex);
		}    	
		
		List<Var> signature = q.getSignature();
		BindingHashMap binding = new BindingHashMap();
		if(signature == null && params != null) {
        	throw new BadRequestException(String.format("The query %s has no signature, so no request parameter param may be set", query));
		}
		if(signature != null && params == null) {
        	throw new BadRequestException(String.format("The query %s has a signature, so there shall be request parameters named param", query));
		}
		if(signature != null && params != null) {
			if(signature.size() != params.size()) {
				throw new BadRequestException(String.format("The query %s has a signature whose size does not match the number of request parameters", query));
			}
			for(int i=0;i<signature.size();i++) {
				binding.add(signature.get(i), NodeFactory.createLiteral(params.get(i)));
			}
		}
		List<Binding> values = new ArrayList();
		values.add(binding);
		Context context = ContextUtils.createSimple();
		
        final ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            final Future<Response> f = service.submit(() -> {
        		if(q.isGenerateType()) {
                    Model model = plan.execGenerate(values, context);
                    StringWriter sw = new StringWriter();
                    model.write(sw, "TTL");
                    return Response.ok(sw.toString(), "text/turtle")
                    		.header("Content-Disposition", "filename= message.ttl;")
                			.build();
        		} else if(q.isSelectType()) {
        			ResultSet resultSet = plan.execSelect(values, context);
                    StringWriter sw = new StringWriter();
                    ByteArrayOutputStream  baos = new ByteArrayOutputStream();
                    ResultSetFormatter.outputAsJSON(baos, resultSet);
        			return Response.ok(baos.toString(), "application/sparql-results+json")
                    		.header("Content-Disposition", "filename= message.ttl;")
                			.build();
        		} else if(q.isTemplateType()) {
        			String result = plan.execTemplate(values, context);
        			return Response.ok(result, "text/plain")
                    		.header("Content-Disposition", "filename= message.ttl;")
                			.build();
        		} else {
        			throw new BadRequestException("The request should be a SELECT, GENERATE, or TEMPLATE query");
        		}
            });
            return f.get(JerseyApp.MAX_TIME, TimeUnit.SECONDS);
        } catch (final TimeoutException ex) {
             return Response.status(Response.Status.REQUEST_TIMEOUT)
                    .entity(String.format("In this API, request timeout is set at %s s. Please use the executable jar instead.", JerseyApp.MAX_TIME))
                    .build();
        } catch (final Exception ex) {
        	throw new InternalServerErrorException(ex);
        } finally {
            service.shutdown();
        }
    }

}
