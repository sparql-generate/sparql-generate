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

import com.github.thesmartenergy.sparql.generate.jena.cli.Request;
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
    
    public static Dataset loadDataset(File dir, Request request) {
        Dataset ds = DatasetFactory.create();
        String dgfile = request.graph != null ? request.graph : "dataset/default.ttl";
        try {
            ds.setDefaultModel(RDFDataMgr.loadModel(new File(dir, dgfile).toString(), Lang.TTL));
        } catch (Exception ex) {
            LOG.debug("Cannot load default graph " + dgfile + ": " + ex.getMessage());
        }

        if(request.namedgraphs == null)
            return ds;
        
        request.namedgraphs.forEach((ng)->  {
            try {
                Model model = RDFDataMgr.loadModel(new File(dir, ng.path).toString(), Lang.TTL);
                ds.addNamedModel(ng.uri, model);
            } catch (Exception ex) {  
                LOG.debug("Cannot load named graph " + ng.path + ": " + ex.getMessage());
            }
        });
            
        return ds;
    }
}
