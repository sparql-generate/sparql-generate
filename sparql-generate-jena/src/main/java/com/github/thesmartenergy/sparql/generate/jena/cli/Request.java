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

import com.github.thesmartenergy.sparql.generate.jena.utils.LM;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/** 
 *
 * @author maxime.lefrancois
 */
public class Request {

    public static Request DEFAULT = new Request();

    static {
        DEFAULT.query = "query.rqg";
        DEFAULT.defaultquery = null;
        DEFAULT.namedqueries = new ArrayList<>();
        DEFAULT.graph = "dataset/default.ttl";
        DEFAULT.defaultgraph = null;
        DEFAULT.namedgraphs = new ArrayList<>();
        DEFAULT.documentset = new ArrayList<>();
        DEFAULT.stream = false;
    }

    public String query; // chemin
    public String defaultquery; // contenu 
    public List<Document> namedqueries;
    public String graph; // chemin
    public String defaultgraph; // contenu
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

    private void addMap(Model model, Document doc) {
        Resource bn = model.createResource();
        Resource mapping = model.createResource();
        model.add(bn, LM.mapping, mapping);
        model.add(mapping, LM.name, doc.uri);
        model.add(mapping, LM.altName, doc.path);
        model.add(mapping, LM.media, doc.mediatype);
    }
}
