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
package fr.mines_stetienne.ci.sparql_generate.stream;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Locator that can stores the documents as strings in a map.
 *
 * @author Maxime Lefrançois
 */
public class LocatorStringMap extends LocatorAcceptBase {

    private static final Logger LOG = LoggerFactory.getLogger(LocatorStringMap.class);

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
        try {
            if (docs.containsKey(request)) {
                TypedString doc = docs.get(request);
                return new TypedInputStream(IOUtils.toInputStream(doc.message, "UTF-8"), doc.mediaType);
            }
            //relax subtype
            for (LookUpRequest poss : docs.keySet()) {
                if (poss.getFilenameOrURI().equals(request.getFilenameOrURI()) && poss.getType().equals(request.getType()) && (poss.getSubType().equals("*") || request.getSubType().equals("*"))) {
                    TypedString doc = docs.get(poss);
                    return new TypedInputStream(IOUtils.toInputStream(doc.message, "UTF-8"), doc.mediaType);
                }
            }
            //relax type and subtype
            for (LookUpRequest poss : docs.keySet()) {
                if (poss.getFilenameOrURI().equals(request.getFilenameOrURI())
                        && (poss.getType().equals("*") || request.getType().equals("*"))
                        && (poss.getSubType().equals(request.getSubType()) || poss.getSubType().equals("*") || request.getSubType().equals("*"))) {
                    TypedString doc = docs.get(poss);
                    return new TypedInputStream(IOUtils.toInputStream(doc.message, "UTF-8"), doc.mediaType);
                }
            }
        } catch (Exception ex) {
            LOG.error("error", ex);
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
