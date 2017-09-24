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
package com.github.thesmartenergy.sparql.generate.jena;

import org.apache.jena.riot.system.stream.StreamManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author maxime.lefrancois
 */
public class SPARQLGenerateStreamManager extends StreamManager {

    private static final Logger LOG = LogManager.getLogger(SPARQLGenerateStreamManager.class);

    private static StreamManager clone(StreamManager other) {
        SPARQLGenerateStreamManager sm = new SPARQLGenerateStreamManager();
        other.locators().forEach((loc) -> {
            sm.addLocator(loc);
        });
        sm.setLocationMapper(other.getLocationMapper() == null ? null : other.getLocationMapper().clone());
        return sm;
    }

    /**
     * Apply the mapping of a filename or URI
     */
    @Override
    public String mapURI(String filenameOrURI) {
        if (getLocationMapper() == null) {
            return filenameOrURI;
        }
        String uri;
        if (filenameOrURI.startsWith("accept:*/*")) {
            uri = getLocationMapper().altMapping(filenameOrURI,
                    getLocationMapper().altMapping(filenameOrURI.substring(11),
                    null));
        } else {
            uri = getLocationMapper().altMapping(filenameOrURI,
                    getLocationMapper().altMapping("accept:*/*" + filenameOrURI,
                    null));
        }
        if (uri == null) {
            LOG.debug("Not mapped: " + filenameOrURI);
            uri = filenameOrURI;
        } else {
            LOG.debug("Mapped: " + filenameOrURI + " => " + uri);
        }
        return uri;
    }

}
