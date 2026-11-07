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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.util.Context;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.engine.RootPlan;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionBase;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionRegistry;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorStringMap;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * Executes the code snippets of the Java API page of the documentation
 * ({@code docs/java-api.md}). A change of the API that breaks the page breaks
 * this test.
 */
public class JavaApiDocumentationTest {

	private static final String CSV = "id,name,country\n1,Ada Lovelace,GB\n2,Alan Turing,GB\n3,Grace Hopper,US\n";

	private static final String QUERY = "PREFIX iter: <http://w3id.org/sparql-generate/iter/>\n"
			+ "PREFIX schema: <http://schema.org/>\n"
			+ "GENERATE <http://example.org/q> (?country) { <http://example.org/person/{?id}> schema:name ?name }\n"
			+ "ITERATOR iter:CSV(<http://example.org/people.csv>, \"id\", \"name\", \"country\") AS ?id ?name ?c\n"
			+ "WHERE { FILTER(!BOUND(?country) || ?c = ?country) }";

	private static SPARQLExtStreamManager streamManager() {
		LocatorStringMap strings = new LocatorStringMap();
		strings.put("http://example.org/people.csv", CSV, "text/csv");
		return SPARQLExtStreamManager.makeStreamManager(strings);
	}

	@Test
	public void parsePlanAndExecute() {
		SPARQLExt.init();
		SPARQLExtQuery query = (SPARQLExtQuery) QueryFactory.create(QUERY, SPARQLExt.SYNTAX);
		RootPlan plan = PlanFactory.create(query);
		Context context = ContextUtils.build().setStreamManager(streamManager()).setPrefixMapping(query).build();
		Model graph = plan.execGenerate(context);
		assertEquals(3, graph.size());
	}

	@Test
	public void initialBindings() {
		RootPlan plan = PlanFactory.create(QUERY);
		Context context = ContextUtils.build().setStreamManager(streamManager()).build();
		Binding binding = Binding.builder().add(Var.alloc("country"), NodeFactory.createLiteralString("GB"))
				.build();
		Model graph = plan.execGenerate(List.of(binding), context);
		assertEquals(2, graph.size());
	}

	@Test
	public void streams() {
		RootPlan generate = PlanFactory.create(QUERY);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Context context = ContextUtils.build().setStreamManager(streamManager())
				.setGenerateOutput(StreamRDFWriter.getWriterStream(out, Lang.NTRIPLES)).build();
		generate.execGenerateStream(context);
		assertEquals(3, out.toString().trim().split("\n").length);

		RootPlan select = PlanFactory.create("SELECT ?x WHERE { VALUES ?x { 1 2 } }");
		AtomicInteger rows = new AtomicInteger();
		select.execSelectStream(ContextUtils.build().setSelectOutput(rs -> {
			rows.addAndGet(ResultSetFormatter.consume(rs));
		}).build());
		assertEquals(2, rows.get());

		RootPlan template = PlanFactory.create("TEMPLATE { ?x ; separator = \",\" } WHERE { VALUES ?x { 1 2 } }");
		ByteArrayOutputStream text = new ByteArrayOutputStream();
		try (IndentedWriter writer = new IndentedWriter(text)) {
			template.execTemplateStream(ContextUtils.build().setTemplateOutput(writer).build());
		}
		assertEquals("1,2", text.toString());
	}

	@Test
	public void selectAndTemplate() {
		ResultSet results = PlanFactory.create("SELECT ?x WHERE { VALUES ?x { 1 2 } }")
				.execSelect(ContextUtils.build().build());
		assertEquals(2, ResultSetFormatter.consume(results));
		String text = PlanFactory.create("TEMPLATE { ?x ; separator = \",\" } WHERE { VALUES ?x { 1 2 } }")
				.execTemplate(ContextUtils.build().build());
		assertEquals("1,2", text);
	}

	/**
	 * The iterator of the documentation.
	 */
	public static class ITER_Repeat extends IteratorFunctionBase {
		public static final String URI = "http://example.org/iter/repeat";

		@Override
		public List<List<NodeValue>> exec(List<NodeValue> args) {
			int n = args.get(1).getInteger().intValue();
			List<List<NodeValue>> rows = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				rows.add(List.of(args.get(0)));
			}
			return rows;
		}

		@Override
		public void checkBuild(ExprList args) {
			if (args.size() != 2) {
				throw new QueryBuildException("iter:repeat takes two arguments");
			}
		}
	}

	@Test
	public void customIterator() {
		IteratorFunctionRegistry.get().put(ITER_Repeat.URI, ITER_Repeat.class);
		String text = PlanFactory
				.create("TEMPLATE { ?x ; separator = \",\" } ITERATOR <http://example.org/iter/repeat>(\"a\", 3) AS ?x")
				.execTemplate(ContextUtils.build().build());
		assertTrue(text, text.equals("a,a,a"));
	}
}
