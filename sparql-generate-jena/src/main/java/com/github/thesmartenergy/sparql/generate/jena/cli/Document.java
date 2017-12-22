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

/**
 *
 * @author maxime.lefrancois
 */
public class Document {
    
    public String uri;
    public String string;
    public String mediatype;
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Document(")
            .append("uri: \"")
            .append(uri)
            .append("\", mediatype: \"")
            .append(mediatype)
            .append("\", string: \"")
            .append(string)
            .append("\")");
        return sb.toString();
    }
}
