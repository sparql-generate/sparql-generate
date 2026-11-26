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
package fr.mines_stetienne.ci.sparql_generate.iterator;

import fr.mines_stetienne.ci.sparql_generate.RaiseException;

/**
 * An exception of the rest of the query, raised in the callback that an
 * iterator calls with its rows: a {@code RAISE}, a limit of execution, an
 * error of a called query. It is not an error of the iterator: an iterator
 * that catches every exception to report its own errors lets it through,
 * neither logged nor wrapped, by calling {@link #rethrowIfNotOwn(Throwable)}
 * first.
 */
public final class DownstreamException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DownstreamException(final RuntimeException cause) {
        super(cause);
    }

    /**
     * @return the exception of the rest of the query.
     */
    public RuntimeException unwrap() {
        return (RuntimeException) getCause();
    }

    /**
     * @param ex an exception
     * @return the DownstreamException in the causes of the exception, itself
     *         included, or null.
     */
    public static DownstreamException findIn(final Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof DownstreamException) {
                return (DownstreamException) t;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return null;
    }

    /**
     * Rethrows, as it is, an exception that is not an error of the iterator:
     * a {@link DownstreamException} or a {@link RaiseException} in the causes
     * of the exception. Does nothing otherwise.
     *
     * @param ex the exception that an iterator caught
     */
    public static void rethrowIfNotOwn(final Throwable ex) {
        final DownstreamException downstream = findIn(ex);
        if (downstream != null) {
            throw downstream;
        }
        final RaiseException raised = RaiseException.findIn(ex);
        if (raised != null) {
            throw raised;
        }
    }
}
