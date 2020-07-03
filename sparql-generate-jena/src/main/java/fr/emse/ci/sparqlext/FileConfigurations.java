/*
 * Copyright 2020 MINES Saint-Étienne
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
 * Contains the configuration of a SPARQL-Generate execution in a directory or on the website.
 * 
 * @author Maxime Lefrançois
 */
public class FileConfigurations {

    private static final Logger LOG = LoggerFactory.getLogger(FileConfigurations.class);
    public static FileConfigurations DEFAULT = new FileConfigurations();

    /**
     * Default constructor with default values: loglevel=5, query=query.rqg, graph=dataset/default.ttl, stream=false, debugTemplate=false, hdt=false, outputAppend=false.
     */
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
        hdt = false;
        outputAppend = false;
    }

    /**
     * base URL of the directory. Every file in the directory may be used as a source with a URL that is resolved against this base.
     */
    public String base; 
    
    /**
     * Location of the file where the output is to be stored
     */
    public String output;
    
    /**
     * Format of the output file, e.g. TTL, NT, RDF/XML, etc for GENERATE, or TEXT, XML, CSV, etc for SELECT.
     */
    public String outputFormat;

    /**
     * when true and a GENERATE query, the output is HDT
     * 
     * @see http://www.rdfhdt.org/
     */
    public boolean hdt; // Generate output as HDT
    
    /**
     * when true, the output is appended to the file
     */
    public boolean outputAppend;

    public String logFile;
    
    /**
     * log level for the query execution, ERROR=1 WARN=2 INFO=3 DEBUG=4 TRACE=5
     */
    public int loglevel; // 
    
    /**
     * description of the query execution, can contain html markup
     */
    public String readme; // html 

    /**
     * the path to the query
     */
    public String query;
    
    /**
     * the content of the query
     */
    public String defaultquery;

    /**
     * List of named queries
     */
    public List<NamedQuery> namedqueries;

    /**
     * the path to the graph
     */
    public String graph;
    
    /**
     * the content of the graph
     */
    public String defaultgraph;
    
    /**
     * List of named graphs
     */
    public List<NamedGraph> namedgraphs;

    /**
     * List of named documents
     */
    public List<NamedDocument> documentset;

    /**
     * If true, the output will be a stream (stream of triples, stream of SELECT results, stream of text)
     */
    public boolean stream;
    
    /**
     * when true, TEMPLATE sub-query failures will be included in the output
     */
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
