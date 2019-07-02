/*
 * Copyright 2019 École des Mines de Saint-Étienne.
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
package fr.emse.ci.sparqlext.api.entities;

import fr.emse.ci.sparqlext.cli.FileConfigurations;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public class Request {

    private static final Logger LOG = LoggerFactory.getLogger(Request.class);

    public Request() {
        cancel = false;
        defaultquery = "";
        namedqueries = new ArrayList<>();
        defaultgraph = "";
        namedgraphs = new ArrayList<>();
        documentset = new ArrayList<>();
        stream = false;
        debugTemplate = false;
        loglevel = 5;
    }

    public boolean cancel; // true to cancel transformation
    public String readme;
    public String defaultquery;
    public String defaultgraph;
    public List<NamedQuery> namedqueries;
    public List<NamedGraph> namedgraphs;
    public List<NamedDocument> documentset;
    public boolean stream;
    public boolean debugTemplate;
    public int loglevel; // ERROR=1 WARN=2 INFO=3 DEBUG=4 TRACE=5

    public Request(FileConfigurations config, File dir) {
        this();
        try {
            readme = IOUtils.toString(new FileReader(new File(dir, "readme.html")));
        } catch (IOException ex) {
            readme = "";
            LOG.debug("IOException while loading readme.html: " + ex.getMessage());
        }
        try {
            defaultquery = IOUtils.toString(new FileReader(new File(dir, config.query)));
        } catch (IOException ex) {
            defaultquery = "";
            LOG.debug("IOException while loading default query: " + ex.getMessage());
        }
        try {
            defaultgraph = IOUtils.toString(new FileReader(new File(dir, config.graph)));
        } catch (IOException ex) {
            defaultgraph = "";
            LOG.debug("IOException while loading default graph: " + ex.getMessage());
        }
        config.namedqueries.forEach((nq) -> {
            NamedQuery nq2 = new NamedQuery();
            nq2.mediatype = nq.mediatype;
            nq2.uri = nq.uri;
            try {
                nq2.string = IOUtils.toString(new FileReader(new File(dir, nq.path)));
            } catch (IOException ex) {
                nq2.string = "";
                LOG.debug("IOException while loading named query <" + nq.path + "> : " + ex.getMessage());
            }
            namedqueries.add(nq2);
        });
        config.namedgraphs.forEach((ng) -> {
            NamedGraph ng2 = new NamedGraph();
            ng2.uri = ng.uri;
            try {
                ng2.string = IOUtils.toString(new FileReader(new File(dir, ng.path)));
            } catch (IOException ex) {
                ng2.string = "";
                LOG.debug("IOException while loading named graph <" + ng.path + "> : " + ex.getMessage());
            }
            namedgraphs.add(ng2);
        });
        config.documentset.stream().forEach((doc) -> {
            NamedDocument doc2 = new NamedDocument();
            doc2.uri = doc.uri;
            doc2.mediatype = doc.mediatype;
            try {
                doc2.string = IOUtils.toString(new FileReader(new File(dir, doc.path)));
            } catch (IOException ex) {
                doc2.string = "";
                LOG.debug("IOException while loading document <" + doc.path + "> : " + ex.getMessage());
            }
            documentset.add(doc2);
        });
        loglevel = config.loglevel;
        stream = config.stream;
        debugTemplate = config.debugTemplate;
    }

    public class NamedQuery {

        public String uri;
        public String string;
        public String mediatype;
    }

    public class NamedGraph {

        public String uri;
        public String string;
    }

    public class NamedDocument {

        public String uri;
        public String string;
        public String mediatype;
    }

}
