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
package fr.emse.ci.sparqlext.stream;

/**
 * Represents a Look Up for a document: contains the URI of the document and a
 * Accept header field.
 *
 * @author Maxime Lefrançois
 */
public class LookUpRequest {

    static final String ACCEPT_ALL = "*/*";

    private final String filenameOrURI;
    private final String accept;

    public LookUpRequest(String filenameOrURI) {
        this(filenameOrURI, ACCEPT_ALL);
    }

    public LookUpRequest(String filenameOrURI, String accept) {
        if (filenameOrURI == null) {
            throw new NullPointerException();
        }
        this.filenameOrURI = filenameOrURI;
        if (accept == null) {
            this.accept = ACCEPT_ALL;
        } else {
            this.accept = accept;
        }
    }

    public String getAccept() {
        return accept;
    }

    public String getFilenameOrURI() {
        return filenameOrURI;
    }

    public String getType() {
        return accept.split("/")[0];
    }

    public String getSubType() {
        return accept.split("/")[1];
    }

    @Override
    public int hashCode() {
        return 3 * filenameOrURI.hashCode() + 7 * accept.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof LookUpRequest)) {
            return false;
        }
        LookUpRequest other = (LookUpRequest) obj;
        return filenameOrURI.equals(other.filenameOrURI) && accept.equals(other.accept);
    }

    @Override
    public String toString() {
        return "LookUp " + filenameOrURI + " with Accept: " + accept;
    }

}
