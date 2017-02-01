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
package com.github.thesmartenergy.sparql.generate.jena;

import org.apache.jena.sparql.ARQException;

/**
 * The SPARQL Generate exception class.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class SPARQLGenerateException extends ARQException {

    /**
     * Constructs a new SPARQL Generate Exception with the specified detail
     * message and cause.
     * <p>
     * Note that the detail message associated with cause is not automatically
     * incorporated in this exception's detail message.
     *
     * @param message - the message.
     * @param cause - the cause.
     */
    public SPARQLGenerateException(
            final String message,
            final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new SPARQL Generate Exception.
     */
    public SPARQLGenerateException() {
        super();
    }

    /**
     * Constructs a new SPARQL Generate Exception with the specified cause.
     *
     * @param cause - the cause.
     */
    public SPARQLGenerateException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new SPARQL Generate Exception with the specified detail
     * message.
     * <p>
     * Note that the detail message associated with cause is not automatically
     * incorporated in this exception's detail message.
     *
     * @param message - the message.
     */
    public SPARQLGenerateException(final String message) {
        super(message);
    }

}
