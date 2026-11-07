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
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

import org.jsfr.json.GsonParser;
import org.jsfr.json.JsonSurfer;
import org.jsfr.json.provider.GsonProvider;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;

/**
 * A JsonSurfer over Gson that keeps the integers of the JSON document as
 * integers. The Gson parser of JsonSurfer reads every number with
 * {@link JsonReader#nextDouble()}: 1 became 1.0, typed xsd:double. Here the
 * reader keeps the text of the number, and the provider makes an integer of
 * a number written without fraction or exponent, and an xsd:decimal of a
 * number with a fraction and no exponent, as iter:JSONPath does.
 */
final class JsonSurferNumbers {

	private static final Pattern INTEGER = Pattern.compile("-?(0|[1-9][0-9]*)");

	/**
	 * The text of the last number read, in this thread: the parser gives it
	 * to the provider right after reading it.
	 */
	private static final ThreadLocal<String> LAST_NUMBER = new ThreadLocal<>();

	static final JsonSurfer SURFER = new JsonSurfer(new GsonParser((Reader reader) -> new JsonReader(reader) {
		@Override
		public double nextDouble() throws IOException {
			final String text = nextString();
			LAST_NUMBER.set(text);
			return Double.parseDouble(text);
		}
	}), new GsonProvider() {
		@Override
		public JsonElement primitive(double value) {
			final String text = LAST_NUMBER.get();
			LAST_NUMBER.remove();
			if (text != null && INTEGER.matcher(text).matches()) {
				return new JsonPrimitive(new BigInteger(text));
			}
			if (text != null && JsonNumbers.DECIMAL.matcher(text).matches()) {
				return new JsonPrimitive(new BigDecimal(text));
			}
			return super.primitive(value);
		}
	});

	private JsonSurferNumbers() {
	}
}
