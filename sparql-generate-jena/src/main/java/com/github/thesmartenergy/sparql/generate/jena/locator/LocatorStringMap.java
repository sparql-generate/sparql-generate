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
package com.github.thesmartenergy.sparql.generate.jena.locator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.system.stream.Locator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class LocatorStringMap implements Locator {
    
    static Logger log = LogManager.getLogger(LocatorStringMap.class);

    private final Map<String, String> docs = new HashMap<>();

    
    public void put(String messageuri, String message) {
        log.trace(messageuri, message);
        String bomSafeMessage = null;
        try {
            bomSafeMessage = IOUtils.toString(new BOMInputStream(IOUtils.toInputStream(message, "UTF-8")), "UTF-8");
        } catch (IOException ex) {
            log.debug("failed bomSafeMessage " + message, ex);
            
        }
        if (!messageuri.substring(0, 7).equals("accept:")) {
            messageuri = "accept:*/*:" + messageuri;
        }
        docs.put(messageuri, bomSafeMessage);
    }


    @Override
    public String getName() {
        return LocatorStringMap.class.getSimpleName();
    }

    @Override
    public TypedInputStream open(String uri) {
        if (!uri.substring(0, 7).equals("accept:")) {
            uri = "accept:*/*:" + uri;
        }
        String message = docs.get(uri);
        if(message == null) {
            log.debug("not found " + uri);
            return null;
        }
        try {
            log.info("found " + uri);
            return new TypedInputStream(IOUtils.toInputStream(message, "UTF-8"), (String) null);
        } catch (IOException ex) {
            log.debug("ioexception " + uri , ex);
            return null;
        }
    }

}
