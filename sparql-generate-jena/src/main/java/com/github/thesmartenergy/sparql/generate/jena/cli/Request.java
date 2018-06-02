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
package com.github.thesmartenergy.sparql.generate.jena.cli;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateCli;
import com.github.thesmartenergy.sparql.generate.jena.utils.LM;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public class Request {

    private static final Logger LOG = LoggerFactory.getLogger(Request.class);
    public static Request DEFAULT = new Request();

    public Request() {
        readme = "";
        query = "query.rqg";
        defaultquery = "";
        namedqueries = new ArrayList<>();
        graph = "dataset/default.ttl";
        defaultgraph = "";
        namedgraphs = new ArrayList<>();
        documentset = new ArrayList<>();
        stream = false;
    }

    public String readme; // html 
    public String query; // path
    public String defaultquery; // content
    public List<Document> namedqueries;
    public String graph; // path
    public String defaultgraph; // content
    public List<Document> namedgraphs;
    public List<Document> documentset;
    public boolean stream;

    public Model asLocationMapper() {
        Model model = ModelFactory.createDefaultModel();
        if (namedqueries != null) {
            namedqueries.forEach((doc) -> {
                addMap(model, doc);
            });
        }
        if (documentset != null) {
            documentset.forEach((doc) -> {
                addMap(model, doc);
            });
        }
        return model;
    }

    public void loadStrings(File dir) {
        try {
            readme = IOUtils.toString(new FileReader(new File(dir, "readme.html")));
        } catch (IOException ex) {
            readme = "";
            LOG.debug("no readme", ex);
        }
        try {
            defaultquery = IOUtils.toString(new FileReader(new File(dir, query)));
        } catch (IOException ex) {
            defaultquery = "";
            LOG.debug("no readme", ex);
        }
        namedqueries.stream().forEach((nq) -> {
            try {
                nq.string = IOUtils.toString(new FileReader(new File(dir, nq.path)));
            } catch (IOException ex) {
                nq.string = "";
                LOG.debug("no namedquery", ex);
            }
        });
        try {
            defaultgraph = IOUtils.toString(new FileReader(new File(dir, graph)));
        } catch (IOException ex) {
            defaultgraph = "";
            LOG.debug("no readme", ex);
        }
        namedgraphs.stream().forEach((ng) -> {
            try {
                ng.string = IOUtils.toString(new FileReader(new File(dir, ng.path)));
            } catch (IOException ex) {
                ng.string = "";
                LOG.debug("no namedgraph", ex);
            }
        });
        documentset.stream().forEach((doc) -> {
            try {
                doc.string = IOUtils.toString(new FileReader(new File(dir, doc.path)));
            } catch (IOException ex) {
                doc.string = "";
                LOG.debug("no document", ex);
            }
        });
    }

    private void addMap(Model model, Document doc) {
        Resource bn = model.createResource();
        Resource mapping = model.createResource();
        model.add(bn, LM.mapping, mapping);
        model.add(mapping, LM.name, doc.uri);
        model.add(mapping, LM.altName, doc.path);
        model.add(mapping, LM.media, doc.mediatype);
    }
}
