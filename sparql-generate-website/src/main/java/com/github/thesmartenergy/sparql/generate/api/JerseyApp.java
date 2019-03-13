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

import javax.ws.rs.ApplicationPath;
import org.apache.jena.sparql.expr.NodeValue;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@ApplicationPath("api")
public class JerseyApp extends ResourceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(JerseyApp.class);
    
    public JerseyApp() {
        LOG.info("Starting Jersey app..."); 
        packages("com.github.thesmartenergy.sparql.generate.api");
        NodeValue.VerboseExceptions = true;
        NodeValue.VerboseWarnings = false;
    }
}
