/*
 * Copyright 2016 Ecole des Mines de Saint-Etienne.
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class LocatorStringMap extends LocatorAcceptBase {
    
    private static final Logger LOG = LogManager.getLogger(LocatorStringMap.class);

    private final Map<LookUpRequest, TypedString> docs = new HashMap<>();
    
    public void put(String uri, String message, String mediaType) {
        Objects.requireNonNull(uri);
        Objects.requireNonNull(message);
        docs.put(new LookUpRequest(uri, mediaType), new TypedString(mediaType, message));
    }

    @Override
    public String getName() {
        return LocatorStringMap.class.getSimpleName();
    }

    @Override
    public TypedInputStream open(LookUpRequest request) {
        TypedString ts = docs.get(request);
        if(ts!=null) {
            try {            
                return new TypedInputStream(IOUtils.toInputStream(ts.message, "UTF-8"), ts.mediaType);
            } catch (IOException ex) {
                LOG.error("error", ex);
            }
        }
        return null;
    }

    private static class TypedString {
        private String mediaType;
        private String message;

        public TypedString(String mediaType, String message) {
            this.mediaType = mediaType;
            this.message = message;
        }
    }
}
