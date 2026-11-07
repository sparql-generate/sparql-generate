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
package fr.mines_stetienne.ci.sparql_generate.json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.jena.sparql.expr.ExprEvalException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

/**
 * Decodes the base64 encoding of a CBOR document (RFC 8949) into JSON text,
 * for fun:CBOR and iter:CBOR.
 */
final class CBORDocuments {

	private static final ObjectMapper CBOR = new ObjectMapper(new CBORFactory());
	private static final ObjectMapper JSON = new ObjectMapper();

	private CBORDocuments() {
	}

	/**
	 * @param base64 the base64 encoding of a CBOR document. The encoding of
	 *               JSON text, whose first character is { or [, which no CBOR
	 *               map or array starts with, is also accepted, as before.
	 * @return the document as JSON text.
	 */
	static String toJson(final String base64) {
		final byte[] bytes;
		try {
			bytes = Base64.getMimeDecoder().decode(base64);
		} catch (IllegalArgumentException ex) {
			throw new ExprEvalException("Not a base64 encoding of a CBOR document", ex);
		}
		if (isJsonText(bytes)) {
			return new String(bytes, StandardCharsets.UTF_8);
		}
		try {
			final JsonNode node = CBOR.readTree(bytes);
			if (node == null) {
				throw new ExprEvalException("Empty CBOR document");
			}
			return JSON.writeValueAsString(node);
		} catch (IOException ex) {
			throw new ExprEvalException("Not a CBOR document: " + ex.getMessage(), ex);
		}
	}

	private static boolean isJsonText(final byte[] bytes) {
		for (byte b : bytes) {
			if (b == ' ' || b == '\t' || b == '\r' || b == '\n') {
				continue;
			}
			// the UTF-8 byte order mark
			if (b == (byte) 0xEF) {
				return false;
			}
			return b == '{' || b == '[';
		}
		return false;
	}
}
