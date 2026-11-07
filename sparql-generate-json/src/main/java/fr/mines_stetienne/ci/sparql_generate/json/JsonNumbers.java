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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

/**
 * How the JSON functions and iterators read a JSON document, with JSONPath:
 * an integer is a Java integer of its size (Integer, Long, BigInteger:
 * xsd:integer), a number with a decimal point and no exponent a BigDecimal
 * (xsd:decimal), a number with an exponent a Double (xsd:double). The
 * typing does not depend on the JSON library that JSONPath uses by default.
 */
final class JsonNumbers {

	/**
	 * A JSON number without exponent.
	 */
	static final Pattern DECIMAL = Pattern.compile("-?(0|[1-9][0-9]*)\\.[0-9]+");

	private static final JsonFactory FACTORY = new JsonFactory();

	private static final JacksonJsonProvider PROVIDER = new JacksonJsonProvider() {
		@Override
		public Object parse(String json) throws InvalidJsonException {
			return read(new StringReader(json));
		}

		@Override
		public Object parse(InputStream in, String charset) throws InvalidJsonException {
			return read(new InputStreamReader(in, Charset.forName(charset)));
		}
	};

	/**
	 * The configuration of JSONPath for the JSON functions and iterators.
	 */
	static final Configuration CONFIGURATION = Configuration.builder().jsonProvider(PROVIDER)
			.mappingProvider(new JacksonMappingProvider()).options(EnumSet.noneOf(Option.class)).build();

	/**
	 * Same, where every path gives a list.
	 */
	static final Configuration LIST_CONFIGURATION = CONFIGURATION.addOptions(Option.ALWAYS_RETURN_LIST);

	private JsonNumbers() {
	}

	private static Object read(Reader reader) {
		try (JsonParser parser = FACTORY.createParser(reader)) {
			final JsonToken token = parser.nextToken();
			if (token == null) {
				throw new InvalidJsonException("Empty JSON document");
			}
			return value(parser, token);
		} catch (IOException ex) {
			throw new InvalidJsonException(ex);
		}
	}

	private static Object value(JsonParser parser, JsonToken token) throws IOException {
		switch (token) {
		case START_OBJECT: {
			final Map<String, Object> map = new LinkedHashMap<>();
			JsonToken t;
			while ((t = parser.nextToken()) == JsonToken.FIELD_NAME) {
				final String name = parser.currentName();
				map.put(name, value(parser, parser.nextToken()));
			}
			return map;
		}
		case START_ARRAY: {
			final List<Object> list = new ArrayList<>();
			JsonToken t;
			while ((t = parser.nextToken()) != JsonToken.END_ARRAY) {
				if (t == null) {
					throw new InvalidJsonException("Unexpected end of the JSON document");
				}
				list.add(value(parser, t));
			}
			return list;
		}
		case VALUE_STRING:
			return parser.getText();
		case VALUE_NUMBER_INT:
			// Integer, Long or BigInteger, as the value needs
			return parser.getNumberValue();
		case VALUE_NUMBER_FLOAT: {
			final String text = parser.getText();
			if (DECIMAL.matcher(text).matches()) {
				return new BigDecimal(text);
			}
			return parser.getDoubleValue();
		}
		case VALUE_TRUE:
			return Boolean.TRUE;
		case VALUE_FALSE:
			return Boolean.FALSE;
		case VALUE_NULL:
			return null;
		default:
			throw new InvalidJsonException("Unexpected JSON token " + token);
		}
	}
}
