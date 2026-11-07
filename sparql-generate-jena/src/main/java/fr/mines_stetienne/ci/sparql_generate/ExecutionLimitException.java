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
package fr.mines_stetienne.ci.sparql_generate;

/**
 * An execution exceeded a limit set in its context (time, number of
 * triples), or was interrupted: it stops.
 */
public class ExecutionLimitException extends SPARQLExtException {

	private static final long serialVersionUID = 1L;

	public ExecutionLimitException(final String message) {
		super(message, null);
	}

	/**
	 * @param ex an exception
	 * @return the ExecutionLimitException in the causes of the exception,
	 *         itself included, or null.
	 */
	public static ExecutionLimitException findIn(final Throwable ex) {
		for (Throwable t = ex; t != null; t = t.getCause()) {
			if (t instanceof ExecutionLimitException) {
				return (ExecutionLimitException) t;
			}
			if (t.getCause() == t) {
				break;
			}
		}
		return null;
	}
}
