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

import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.system.stream.LocationMapper;
import org.apache.jena.riot.system.stream.Locator;
import org.apache.jena.riot.system.stream.StreamManager;
import org.apache.jena.riot.RiotNotFoundException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * @author maxime.lefrancois
 */
public class SPARQLGenerateStreamManager extends StreamManager {

    private static final Logger LOG = LoggerFactory.getLogger(SPARQLGenerateStreamManager.class);

    public SPARQLGenerateStreamManager() {
    }

    public static SPARQLGenerateStreamManager makeStreamManager() {
        return makeStreamManager(null);
    }

    public static SPARQLGenerateStreamManager makeStreamManager(LocatorAccept locator) {
        SPARQLGenerateStreamManager sm = new SPARQLGenerateStreamManager();
        if (locator != null) {
            sm.addLocator(locator);
        }
        sm.addLocator(new LocatorFileAccept(null));
        sm.addLocator(new LocatorClassLoaderAccept(SPARQLGenerateStreamManager.class.getClassLoader()));
        sm.addLocator(new LocatorURLAccept());
        return sm;
    }

    /**
     * Open a file using the locators of this StreamManager. Returns null if not
     * found.
     */
    @Override
    @Deprecated
    public TypedInputStream open(String filenameOrURI) {
        return open(new LookUpRequest(filenameOrURI, LookUpRequest.ACCEPT_ALL));
    }

    /**
     * Open a file using the locators of this StreamManager. Returns null if not
     * found.
     */
    public TypedInputStream open(LookUpRequest _request) {
        LookUpRequest request = mapRequest(_request);
        return openNoMapOrNull(request);
    }

    public String mapURI(LookUpRequest request) {
        throw new UnsupportedOperationException("Unsupported. Use mapRequest instead");
    }

    public LookUpRequest mapRequest(LookUpRequest _request) {
        if (getLocationMapper() == null) {
            return _request;
        }
        LookUpRequest request = ((LocationMapperAccept) getLocationMapper()).altRequest(_request, null);
        if (request == null) {
            request = _request;
        }
        return request;
    }

    /**
     * Open a file using the locators of this FileManager but without location
     * mapping. Throws RiotNotFoundException if not found.
     */
    @Override
    @Deprecated
    public TypedInputStream openNoMap(String filenameOrURI) {
        return openNoMap(new LookUpRequest(filenameOrURI, LookUpRequest.ACCEPT_ALL));
    }

    public TypedInputStream openNoMap(LookUpRequest request) {
        TypedInputStream in = openNoMapOrNull(request);
        if (in == null) {
            throw new RiotNotFoundException(request.toString());
        }
        return in;
    }

    /**
     * Open a file using the locators of this FileManager without location
     * mapping. Return null if not found
     */
    @Override
    @Deprecated
    public TypedInputStream openNoMapOrNull(String filenameOrURI) {
        return openNoMapOrNull(new LookUpRequest(filenameOrURI, LookUpRequest.ACCEPT_ALL));
    }

    public TypedInputStream openNoMapOrNull(LookUpRequest request) {
        for (Locator loc : locators()) {
            LocatorAccept loca = (LocatorAccept) loc;
            TypedInputStream in = loca.open(request);
            if (in != null) {
                LOG.debug("Locator " + loc.getName() + " found: " + request.getFilenameOrURI() + " with accept: " + request.getAccept());
                return in;
            }
        };
        return null;
    }

    /**
     * Set the location mapping
     */
    @Override
    @Deprecated
    public void setLocationMapper(LocationMapper _mapper) {
        try {
            setLocationMapper((LocationMapperAccept) _mapper);
        } catch (ClassCastException ex) {
            LOG.error("LocationMapper does not have class LocationMapperAccept. Aborting");
        }
    }
    
    public void setLocationMapper(LocationMapperAccept _mapper) {
        super.setLocationMapper(_mapper);
    }

    public void setLocationMapper(Model configurationModel) {
        setLocationMapper(new LocationMapperAccept(configurationModel));
    }

    /**
     * Add a locator to the end of the locators list
     */
    @Override
    @Deprecated
    public void addLocator(Locator loc) {
        try {
            addLocator((LocatorAccept) loc);
        } catch (ClassCastException ex) {
            LOG.error("Locator does not have class LocatorAccept. Aborting");
        }
    }
    
    /**
     * Add a locator to the end of the locators list
     */
    public void addLocator(LocatorAccept loc) {
        super.addLocator(loc);
    }

}
