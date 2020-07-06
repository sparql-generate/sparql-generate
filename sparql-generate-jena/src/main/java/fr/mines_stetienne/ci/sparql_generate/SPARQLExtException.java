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
package fr.mines_stetienne.ci.sparql_generate;

import org.apache.jena.sparql.ARQException;

/**
 * The SPARQL-Generate exception class.
 *
 * @author Maxime Lefrançois
 */
public class SPARQLExtException extends ARQException {

    /**
     * Constructs a new SPARQL-Generate Exception with the specified detail
     * message and cause.
     * <p>
     * Note that the detail message associated with cause is not automatically
     * incorporated in this exception's detail message.
     *
     * @param message - the message.
     * @param cause - the cause.
     */
    public SPARQLExtException(
            final String message,
            final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new SPARQL-Generate Exception.
     */
    public SPARQLExtException() {
        super();
    }

    /**
     * Constructs a new SPARQL-Generate Exception with the specified cause.
     *
     * @param cause - the cause.
     */
    public SPARQLExtException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new SPARQL-Generate Exception with the specified detail
     * message.
     * <p>
     * Note that the detail message associated with cause is not automatically
     * incorporated in this exception's detail message.
     *
     * @param message - the message.
     */
    public SPARQLExtException(final String message) {
        super(message);
    }

}
