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
package com.github.thesmartenergy.sparql.generate.api;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.system.stream.Locator;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class UniqueLocator implements Locator {

    private String name = "transientLocator";

    private String messageuri;
    private String message;

    public UniqueLocator(String messageuri, String message) {
        this.messageuri = messageuri;
        this.message = message;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public TypedInputStream open(String string) {
        if (messageuri.equals(this.messageuri)) {
            try {
                return new TypedInputStream(IOUtils.toInputStream(message, "UTF-8"), (String) null);
            } catch (IOException ex) {
                Logger.getLogger(UniqueLocator.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        return null;
    }

}
