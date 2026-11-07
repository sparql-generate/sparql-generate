/*
 * Copyright 2026 École des Mines de Saint-Étienne.
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
package fr.mines_stetienne.ci.sparql_generate.api;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.api.entities.Request;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorStringMap;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;

/**
 * Turns a playground {@link Request} into the inputs of the engine: the query,
 * the input dataset, and the stream manager that maps the URIs of the named
 * queries, named graphs and documents of the request to their content.
 *
 * The WebSocket endpoint {@link TransformStream} and the test that runs every
 * example of the playground both use this class, so that the test exercises the
 * same set-up as the playground.
 */
public final class RequestSetup {

	/**
	 * The base IRI used to parse the graphs of the request and to write the
	 * generated graph.
	 */
	public static final String BASE = "http://example.org/";

	/**
	 * The media type of the named graphs of the request.
	 */
	public static final String TURTLE_MEDIATYPE = "text/turtle";

	static {
		SPARQLExt.init();
		com.jayway.jsonpath.Configuration.setDefaults(new com.jayway.jsonpath.Configuration.Defaults() {

			private final JsonProvider jsonProvider = new JacksonJsonProvider();
			private final MappingProvider mappingProvider = new JacksonMappingProvider();

			@Override
			public JsonProvider jsonProvider() {
				return jsonProvider;
			}

			@Override
			public MappingProvider mappingProvider() {
				return mappingProvider;
			}

			@Override
			public Set<Option> options() {
				return EnumSet.noneOf(Option.class);
			}
		});
	}

	private RequestSetup() {
	}

	/**
	 * Forces the static initialisation of the engine and of the JSONPath
	 * configuration.
	 */
	public static void init() {
	}

	/**
	 * Parses the default query of the request.
	 *
	 * @param request the request
	 * @return the parsed query
	 */
	public static SPARQLExtQuery query(Request request) {
		return (SPARQLExtQuery) QueryFactory.create(request.defaultquery, SPARQLExt.SYNTAX);
	}

	/**
	 * Builds the input dataset of the request: its default graph and its named
	 * graphs, all in Turtle.
	 *
	 * @param request the request
	 * @param onError called with a message and the exception when a named graph
	 *                cannot be parsed; the named graph is then empty
	 * @return the dataset
	 * @throws IOException if the default graph cannot be read
	 */
	public static Dataset dataset(Request request, BiConsumer<String, Exception> onError) throws IOException {
		Dataset dataset = DatasetFactory.create();
		Model g = ModelFactory.createDefaultModel();
		if (request.defaultgraph != null) {
			RDFDataMgr.read(g, IOUtils.toInputStream(request.defaultgraph, "UTF-8"), BASE, Lang.TTL);
		}
		dataset.setDefaultModel(g);
		request.namedgraphs.forEach((ng) -> {
			Model model = ModelFactory.createDefaultModel();
			try {
				RDFDataMgr.read(model, IOUtils.toInputStream(ng.string, "UTF-8"), BASE, Lang.TTL);
			} catch (Exception ex) {
				onError.accept("WARN: unable to parse graph " + ng.uri, ex);
			}
			dataset.addNamedModel(ng.uri, model);
		});
		return dataset;
	}

	/**
	 * Builds the stream manager of the request. Named queries, named graphs and
	 * documents are looked up first; other URIs are looked up on the file system,
	 * on the class path, and on the Web.
	 *
	 * @param request the request
	 * @return the stream manager
	 */
	public static SPARQLExtStreamManager streamManager(Request request) {
		final LocatorStringMap loc = new LocatorStringMap();
		request.namedqueries.forEach((nq) -> {
			loc.put(nq.uri, nq.string, nq.mediatype);
		});
		request.namedgraphs.forEach((ng) -> {
			loc.put(ng.uri, ng.string, TURTLE_MEDIATYPE);
		});
		request.documentset.forEach((doc) -> {
			loc.put(doc.uri, doc.string, doc.mediatype);
		});
		return SPARQLExtStreamManager.makeStreamManager(loc);
	}
}
