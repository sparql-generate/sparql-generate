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
package com.github.thesmartenergy.sparql.generate.jena.stream;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.system.stream.LocationMapper;
import org.apache.jena.vocabulary.LocationMappingVocab;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * @author maxime.lefrancois
 */
public class LocationMapperAccept extends LocationMapper {
    
    private static final Property accept = ResourceFactory.getInstance().createProperty("http://jena.hpl.hp.com/2004/08/location-mapping#media");
    
    static Logger log = LoggerFactory.getLogger(LocationMapperAccept.class);
    Map<LookUpRequest, LookUpRequest> altLocations = new HashMap<>();

    /**
     * Create a LocationMapperAccept with no mapping yet
     */
    public LocationMapperAccept() {
    }
    
    public LocationMapperAccept(final Model configurationModel) {
        Query q = QueryFactory.create("PREFIX lm: <http://jena.hpl.hp.com/2004/08/location-mapping#>"
                + "SELECT * WHERE {"
                + "[] lm:mapping ?e ."
                + "?e lm:name ?name ; lm:altName ?alt ."
                + "OPTIONAL { ?e lm:media ?media . } "
                + "}");
        QueryExecutionFactory.create(q, configurationModel).execSelect().forEachRemaining((result) -> {
            try {
                String name = result.getLiteral("name").getString();
                String altName = result.getLiteral("alt").getString();
                String media = (result.getLiteral("media") == null ? null : result.getLiteral("media").getString());
                altLocations.put(new LookUpRequest(name, media), new LookUpRequest(altName, media));
            } catch (Exception ex) {
                log.warn("Error while reading mapping in configuration model " + ex);
            }
        });
    }
 
    /**
     * Deep copy of location and prefix maps
     */
    @Override
    public LocationMapperAccept clone() {
        return clone(this);
    }

    private static LocationMapperAccept clone(LocationMapperAccept other) {
        LocationMapperAccept mapper = new LocationMapperAccept();
        mapper.altLocations.putAll(other.altLocations);
        return mapper;
    }

    @Override
    public void copyFrom(LocationMapper lmap2) {
        lmap2.listAltEntries().forEachRemaining((String entry) -> {
            this.altLocations.put(new LookUpRequest(entry), new LookUpRequest(lmap2.getAltEntry(entry)));
        });
    }

    public void copyFrom(LocationMapperAccept lmap2) {
        this.altLocations.putAll(lmap2.altLocations);
    }

    @Override
    @Deprecated
    public String altMapping(String uri) {
        throw new UnsupportedOperationException("Unsupported, use altRequest instead");
    }

    public LookUpRequest altRequest(String uri) {
        return altRequest(new LookUpRequest(uri));
    }
    
    public LookUpRequest altRequest(LookUpRequest request) {
        return altRequest(request, request);
    }
    
    /**
     * Apply mappings: first try for an exact alternative location, then try to
     * remap by prefix, finally, try the special case of filenames in a specific
     * base directory.
     *
     * @param uri
     * @param otherwise
     * @return The alternative location choosen
     */
    @Override
    @Deprecated
    public String altMapping(String uri, String otherwise) {
        throw new UnsupportedOperationException("Unsupported, use altRequest instead");
    }

    public LookUpRequest altRequest(String uri, String otherwise) {
        return altRequest(new LookUpRequest(uri), new LookUpRequest(otherwise));
    }
    
    public LookUpRequest altRequest(LookUpRequest request, LookUpRequest otherwise) {
        if (altLocations.containsKey(request)) {
            return altLocations.get(request);
        }
        //relax subtype
        for(LookUpRequest poss : altLocations.keySet()) {
            if(poss.getFilenameOrURI().equals(request.getFilenameOrURI()) && poss.getType().equals(request.getType()) && ( poss.getSubType().equals("*") || request.getSubType().equals("*"))) {
                return altLocations.get(poss);
            }
        }
        //relax type and subtype
        for(LookUpRequest poss : altLocations.keySet()) {
            if(poss.getFilenameOrURI().equals(request.getFilenameOrURI())
                    && ( poss.getType().equals("*") || request.getType().equals("*") )
                    && ( poss.getSubType().equals(request.getSubType()) || poss.getSubType().equals("*") || request.getSubType().equals("*")) ) {
                return altLocations.get(poss);
            }
        }
        return otherwise;
    }

    @Deprecated
    public void addAltEntry(String uri, String alt) {
        altLocations.put(new LookUpRequest(uri), new LookUpRequest(alt));
    }

    public void addAltEntry(LookUpRequest request, LookUpRequest alt) {
        altLocations.put(request, alt);
    }

    @Deprecated
    public void addAltPrefix(String uriPrefix, String altPrefix) {
        throw new UnsupportedOperationException("Prefix mapping not supported in this class");
    }

    /**
     * Iterate over all the entries registered
     */
    @Deprecated
    public Iterator<String> listAltEntries() {
        throw new UnsupportedOperationException("Use listAltRequests instead");
    }

    public Iterator<LookUpRequest> listAltRequests() {
        return altLocations.keySet().iterator();
    }
        
    /**
     * Iterate over all the prefixes registered
     */
    @Deprecated
    public Iterator<String> listAltPrefixes() {
        throw new UnsupportedOperationException("Prefix mapping not supported in this class");
    }

    @Deprecated
    public void removeAltEntry(String uri) {
        throw new UnsupportedOperationException("Use removeAltRequest instead");
    }

    public void removeAltRequest(LookUpRequest request) {
        altLocations.remove(request);
    }

    @Deprecated
    public void removeAltPrefix(String uriPrefix) {
        throw new UnsupportedOperationException("Prefix mapping not supported in this class");
    }

    @Deprecated
    public String getAltEntry(String uri) {
        throw new UnsupportedOperationException("Use getAltRequest instead");
    }

    public LookUpRequest getAltEntry(LookUpRequest request) {
        return altLocations.get(request);
    }

    @Deprecated 
    public String getAltPrefix(String uriPrefix) {
        throw new UnsupportedOperationException("Prefix mapping not supported in this class");
    }

    @Override
    public int hashCode() {
        int x = 0;
        x = x ^ altLocations.hashCode();
        return x;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LocationMapperAccept)) {
            return false;
        }
        LocationMapperAccept other = (LocationMapperAccept) obj;
        if (!this.altLocations.equals(other.altLocations)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String s = "";
        for (LookUpRequest k : altLocations.keySet()) {
            LookUpRequest v = altLocations.get(k);
            s = s + "(Loc:" + k + "=>" + v + ") ";
        }
        return s;
    }

    public Model toModel() {
        Model m = ModelFactory.createDefaultModel();
        m.setNsPrefix("lmap", "http://jena.hpl.hp.com/2004/08/location-mapping#");
        toModel(m);
        return m;
    }

    public void toModel(Model model) {
        for (LookUpRequest s1 : altLocations.keySet()) {
            Resource r = model.createResource();
            Resource e = model.createResource();
            model.add(r, LocationMappingVocab.mapping, e);
            model.add(e, LocationMappingVocab.name, s1.getFilenameOrURI());
            model.add(e, LocationMappingVocab.altName, altLocations.get(s1).getFilenameOrURI());
            model.add(e, accept, s1.getAccept());
        }
    }

}
