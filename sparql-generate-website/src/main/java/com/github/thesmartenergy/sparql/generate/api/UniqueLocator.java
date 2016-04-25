/*
 * Copyright 2016 ITEA 12004 SEAS Project.
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

import org.apache.commons.io.IOUtils;
import org.apache.jena.util.Locator;
import org.apache.jena.util.TypedStream;

/**
 *
 * @author maxime.lefrancois
 */
public class UniqueLocator implements Locator {

    private String name = "transientLocator";

    private String messageuri;
    private String message;
    private String datatype;

    public UniqueLocator(String messageuri, String message, String datatype) {
        this.messageuri = messageuri;
        this.message = message;
        this.datatype = datatype;
    }

    @Override 
    public TypedStream open(String messageuri) {
        if (messageuri.equals(this.messageuri)) {
            return new TypedStream(IOUtils.toInputStream(message), datatype);
        } else {
            return null;
        }
    }

    @Override
    public String getName() {
        return name;
    }

}
