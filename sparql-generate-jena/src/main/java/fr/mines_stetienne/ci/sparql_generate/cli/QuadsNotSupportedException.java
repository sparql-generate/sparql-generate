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
package fr.mines_stetienne.ci.sparql_generate.cli;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExtException;

/**
 * The query generates quads in named graphs, and the output format asked for
 * holds triples only.
 */
public class QuadsNotSupportedException extends SPARQLExtException {

    private static final long serialVersionUID = 1L;

    public QuadsNotSupportedException(String format) {
        super("The query generates quads in named graphs, which " + format
                + " cannot hold: use --format-output TRIG or NQUADS.");
    }

    /**
     * @return the exception in the causes of an exception, or null.
     */
    public static QuadsNotSupportedException findIn(final Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof QuadsNotSupportedException) {
                return (QuadsNotSupportedException) t;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return null;
    }
}
