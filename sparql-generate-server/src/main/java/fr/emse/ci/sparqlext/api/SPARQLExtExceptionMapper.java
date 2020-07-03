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

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class SPARQLExtExceptionMapper implements ExceptionMapper<WebApplicationException> {

    public Response toResponse(WebApplicationException exception) {
    	String entity = getEntity(exception);
    	return Response.fromResponse(exception.getResponse())
				.entity(entity)
				.build();
    }
    
    
    String getEntity(WebApplicationException exception) {
    	StatusType status = exception.getResponse().getStatusInfo();
    	String pre = "";
    	if(exception.getCause()!=null) {
        	StringWriter sw = new StringWriter();
        	PrintWriter pw = new PrintWriter(sw);
        	exception.getCause().printStackTrace(pw);
    		pre = String.format("<p>Caused by:</p>\n<pre><code>%s</code></pre>\n", sw.toString().replaceAll("<", "&lt;"));
    	}
    	
    	return String.format("<!doctype html>\n<html lang=\"en\">\n<head><title>Error %s: %s</title></head>\n<body><h1>Error %s: %s</h1>\n<p>%s</p>\n%s</body></html>",
    	    	status.getStatusCode(),
    	    	status.getReasonPhrase(),
    	    	status.getStatusCode(),
    	    	status.getReasonPhrase(),
    	    	exception.getMessage(),
    	    	pre
    	);
    }
}