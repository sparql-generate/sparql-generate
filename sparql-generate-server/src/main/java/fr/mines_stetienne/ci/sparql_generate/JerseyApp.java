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
package fr.mines_stetienne.ci.sparql_generate;

import javax.ws.rs.ApplicationPath;
import org.apache.jena.sparql.expr.NodeValue;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Maxime Lefrançois
 */
@ApplicationPath("api")
public class JerseyApp extends ResourceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(JerseyApp.class);
    
    public static final String MAX_TIME_ENV = "SPARQL_GENERATE_MAX_TIME";
    public static int MAX_TIME = 10;
    		
    public JerseyApp() {
        LOG.info("Starting Jersey app..."); 
        packages("fr.mines_stetienne.ci.sparql_generate.api");
        
        NodeValue.VerboseExceptions = true;
        NodeValue.VerboseWarnings = false;
        
        String maxTime = System.getenv(MAX_TIME_ENV);
        if(maxTime == null) {
        	LOG.warn(String.format("Using default value for the %s variable: %s", MAX_TIME_ENV, MAX_TIME));
        	return;
        }
        try {
        	MAX_TIME = Integer.parseInt(maxTime);
        } catch (Exception ex) {
        	LOG.warn(String.format("Could not parse the %s environment variable", MAX_TIME_ENV));
        }
    }
}
