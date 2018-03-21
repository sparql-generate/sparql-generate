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
package com.github.thesmartenergy.sparql.generate.jena.utils;

import java.io.File;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public class SPARQLGenerateUtils {
    
    private static final Logger LOG = LoggerFactory.getLogger(SPARQLGenerateUtils.class);
    
    public static Dataset loadDataset(File dir) {
        Dataset ds = DatasetFactory.create();
        try {
            ds.setDefaultModel(RDFDataMgr.loadModel(new File(dir, "default.ttl").toString(), Lang.TTL));
        } catch (Exception ex) {
            LOG.debug("error while loading the default graph default.ttl: " + ex.getMessage());
        }

        String query = "PREFIX lm: <http://jena.hpl.hp.com/2004/08/location-mapping#>\n"
                + "SELECT * WHERE { [] lm:name ?name ; lm:altName ?alt .}" ;
        try {
            Model conf = RDFDataMgr.loadModel(new File(dir, "dataset/configuration.ttl").toString(), Lang.TTL);
            QueryExecutionFactory.create(query, conf).execSelect().forEachRemaining((sol)-> {
                try {
                    String name = sol.getLiteral("name").getString();
                    String alt = sol.getLiteral("alt").getString();
                    Model ng = RDFDataMgr.loadModel(new File(dir, alt).toString(), Lang.TTL);
                    ds.addNamedModel(name, ng);
                } catch (Exception ex) {  
                    LOG.debug("error while loading the default graph default.ttl: " + ex.getMessage());
                }
            });
        } catch (Exception ex) {
            LOG.debug("error while loading the dataset configuration file dataset/configuration.ttl: "  + ex.getMessage());
        }
        return ds;
    }
}
