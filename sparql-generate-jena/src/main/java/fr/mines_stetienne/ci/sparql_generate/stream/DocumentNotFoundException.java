/*
 * Copyright 2026 MINES Saint-Étienne
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

import fr.mines_stetienne.ci.sparql_generate.SPARQLExtException;

/**
 * Thrown by a {@link SPARQLExtStreamManager} that fails on missing documents
 * (see {@link SPARQLExtStreamManager#setFailOnMissingDocument(boolean)}) when
 * no locator finds a document. It is not an expression evaluation error: it
 * stops the execution.
 */
public class DocumentNotFoundException extends SPARQLExtException {

    private static final long serialVersionUID = 1L;

    private final String uri;

    public DocumentNotFoundException(final String uri) {
        super("Document not found: " + uri, null);
        this.uri = uri;
    }

    /**
     * @return the IRI or file name of the document that was looked up.
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param ex an exception
     * @return the DocumentNotFoundException in the causes of the exception,
     *         itself included, or null.
     */
    public static DocumentNotFoundException findIn(final Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof DocumentNotFoundException) {
                return (DocumentNotFoundException) t;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return null;
    }
}
