/*
 * Copyright 2020 MINES Saint-Étienne
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
package fr.mines_stetienne.ci.sparql_generate.engine;

import static fr.mines_stetienne.ci.sparql_generate.engine.EngineTestUtils.template;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorStringMap;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * TEMPLATE queries.
 */
public class TemplateTest {

	@Test
	public void oneLinePerSolution() {
		assertEquals("<1><2><3>", template("TEMPLATE { \"<\" ?x \">\" } WHERE { VALUES ?x { 1 2 3 } }"));
	}

	@Test
	public void separatorBeforeAfter() {
		assertEquals("[1,2,3]", template(
				"TEMPLATE { BEFORE = \"[\" ; ?x ; SEPARATOR = \",\" ; AFTER = \"]\" } WHERE { VALUES ?x { 1 2 3 } }"));
	}

	/**
	 * GROUP aggregates the solutions, as GROUP_CONCAT.
	 */
	@Test
	public void group() {
		assertEquals("<1,2,3>",
				template("TEMPLATE { \"<\" GROUP { ?x ; SEPARATOR = \",\" } \">\" } WHERE { VALUES ?x { 1 2 3 } }"));
	}

	@Test
	public void groupDistinct() {
		assertEquals("<1,2>", template(
				"TEMPLATE { \"<\" GROUP DISTINCT { ?x ; SEPARATOR = \",\" } \">\" } WHERE { VALUES ?x { 1 2 1 } }"));
	}

	/**
	 * As in the example template/04-Group of the website: GROUP with GROUP BY
	 * and other aggregates.
	 */
	@Test
	public void groupWithGroupByAndAggregates() {
		assertEquals("a (2): a1;a2.\nb (1): b1", template(
				"TEMPLATE { ?k \" (\" count(?v) \"): \" GROUP { ?v ; separator = \";\" } ; separator = \".\\n\" } "
						+ "WHERE { VALUES (?k ?v) { (\"a\" \"a1\") (\"a\" \"a2\") (\"b\" \"b1\") } } GROUP BY ?k ORDER BY ?k"));
	}

	@Test
	public void aggregate() {
		assertEquals("avg=1.5", template("TEMPLATE { \"avg=\" str(avg(?x)) } WHERE { VALUES ?x { 1 2 } }"));
	}

	/**
	 * An aggregate inside BOX, as in the example template/05-Box of the website.
	 */
	@Test
	public void aggregateInBox() {
		assertEquals("avg=1.5", template("TEMPLATE { \"avg=\" BOX { str(avg(?x)) } } WHERE { VALUES ?x { 1 2 } }"));
	}

	@Test
	public void aggregateInFormat() {
		assertEquals("avg=1.5", template(
				"TEMPLATE { FORMAT { \"avg=%s\" str(avg(?x)) } } WHERE { VALUES ?x { 1 2 } }"));
	}

	private static String templateWith(String query, String namedIri, String named) {
		LocatorStringMap locator = new LocatorStringMap();
		locator.put(namedIri, named, SPARQLExt.MEDIA_TYPE);
		SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator);
		return PlanFactory.create(query).execTemplate(ContextUtils.build().setStreamManager(sm).build());
	}

	@Test
	public void callNamedTemplate() {
		assertEquals("[1][2]", templateWith("TEMPLATE { TEMPLATE <http://ex.org/t>(?x) . } WHERE { VALUES ?x { 1 2 } }",
				"http://ex.org/t", "TEMPLATE <http://ex.org/t>(?y) { \"[\" ?y \"]\" }"));
	}

	/**
	 * A call to a named template with a WHERE clause at the call site, from a
	 * root TEMPLATE query.
	 */
	@Test
	public void callNamedTemplateWithWhereClause() {
		assertEquals("[1]", templateWith("TEMPLATE { TEMPLATE <http://ex.org/t>(?x) WHERE { BIND(1 AS ?x) } . } WHERE { }",
				"http://ex.org/t", "TEMPLATE <http://ex.org/t>(?y) { \"[\" ?y \"]\" }"));
	}
}
