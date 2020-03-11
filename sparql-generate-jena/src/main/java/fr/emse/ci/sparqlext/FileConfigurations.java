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
package fr.emse.ci.sparqlext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public class FileConfigurations {

    private static final Logger LOG = LoggerFactory.getLogger(FileConfigurations.class);
    public static FileConfigurations DEFAULT = new FileConfigurations();

    public FileConfigurations() {
        loglevel = 5;
        readme = "";
        query = "query.rqg";
        defaultquery = "";
        namedqueries = new ArrayList<>();
        graph = "dataset/default.ttl";
        defaultgraph = "";
        namedgraphs = new ArrayList<>();
        documentset = new ArrayList<>();
        stream = false;
        debugTemplate = false;
    }

    public String base; 
    public String output; // Location where the output is to be stored. 
    public String outputFormat; // Format of the output file, e.g. TTL, NT, etc. for GENERATE, or TEXT, XML, CSV, etc. for SELECT.
    public boolean hdt = false; // Generate output as HDT
    public boolean outputAppend = false;

    public String logFile;
    public int loglevel; // ERROR=1 WARN=2 INFO=3 DEBUG=4 TRACE=5
    public String readme; // html 
    /**
     * the path to the query
     */
    public String query; // path
    /**
     * the content of the query
     */
    public String defaultquery; // content
    public List<NamedQuery> namedqueries;
    /**
     * the path to the graph
     */
    public String graph;
    /**
     * the content of the graph
     */
    public String defaultgraph; // content
    public List<NamedGraph> namedgraphs;
    public List<NamedDocument> documentset;
    public boolean stream;
    public boolean debugTemplate;

    
    public class NamedQuery {
        public String uri;
        public String path;
        public String mediatype;
    }

    public class NamedGraph {
        public String uri;
        public String path;
    }

    public class NamedDocument {
        public String uri;
        public String path;
        public String mediatype;
    }

    public Dataset loadDataset(File dir) {
        Dataset ds = DatasetFactory.create();
        String dgfile = graph != null ? graph : "dataset/default.ttl";
        try {
            ds.setDefaultModel(RDFDataMgr.loadModel(new File(dir, dgfile).toString(), Lang.TTL));
        } catch (Exception ex) {
            LOG.debug("No default graph provided: " + ex.getMessage());
        }

        if (namedgraphs == null) {
            return ds;
        }

        namedgraphs.forEach((ng) -> {
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
