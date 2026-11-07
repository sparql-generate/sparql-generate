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
package fr.mines_stetienne.ci.sparql_generate.webapis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.sparql.expr.ExprEvalException;

/**
 * The text of an HTTP response, as fun:HTTPGet and the other HTTP functions
 * give it: the status line, the headers, an empty line and the body.
 */
final class HttpResponseText {

	/**
	 * The start of the status line, for any version of HTTP: HTTP/1.0,
	 * HTTP/1.1, HTTP/2...
	 */
	private static final Pattern STATUS_LINE = Pattern.compile("^HTTP/[0-9](?:\\.[0-9])? ");

	private HttpResponseText() {
	}

	/**
	 * @return the index of the status code in the response.
	 * @throws ExprEvalException if the response does not start with a status
	 *                           line.
	 */
	static int codeIndex(final String response) {
		final Matcher matcher = STATUS_LINE.matcher(response);
		if (!matcher.find()) {
			throw new ExprEvalException("Argument must start with a status line, such as HTTP/1.1 200 OK");
		}
		return matcher.end();
	}
}
