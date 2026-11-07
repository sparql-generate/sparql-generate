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

import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * The numbers of a JSON document, for iter:JSONPath, iter:JSONSurfer and
 * fun:JSONPath: an integer is an xsd:integer, of any size; a number with a
 * decimal point and no exponent an xsd:decimal; a number with an exponent an
 * xsd:double.
 */
public class JSONNumbersTest {

	static {
		SPARQLExt.init();
	}

	private static final String JSON = "[\\{\\\"n\\\": 1, \\\"x\\\": 1.5, \\\"b\\\": 9223372036854775808, \\\"e\\\": 1.5e3\\}]";

	private static void assertGenerates(String expected, String query) {
		Model expectedModel = ModelFactory.createDefaultModel();
		RDFDataMgr.read(expectedModel, new StringReader(expected), null, Lang.TTL);
		Model actual = PlanFactory.create("PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
				+ "PREFIX fun: <http://w3id.org/sparql-generate/fn/> " + query).execGenerate(ContextUtils.build().build());
		StringWriter sw = new StringWriter();
		RDFDataMgr.write(sw, actual, Lang.NT);
		assertTrue("got:\n" + sw, actual.isIsomorphicWith(expectedModel));
	}

	private static final String EXPECTED = "<http://ex/s> <http://ex/n> 1 ; <http://ex/x> 1.5 ; "
			+ "<http://ex/b> 9223372036854775808 ; <http://ex/e> \"1500.0e0\"^^<http://www.w3.org/2001/XMLSchema#double> .";

	@Test
	public void jsonSurfer() {
		assertGenerates(EXPECTED, "GENERATE { <http://ex/s> <http://ex/n> ?n ; <http://ex/x> ?x ; <http://ex/b> ?b ; <http://ex/e> ?f } "
				+ "ITERATOR iter:JSONSurfer(\"" + JSON + "\", \"$[*]\", \"$.n\", \"$.x\", \"$.b\", \"$.e\") AS ?e ?n ?x ?b ?f");
	}

	@Test
	public void jsonPath() {
		assertGenerates(EXPECTED, "GENERATE { <http://ex/s> <http://ex/n> ?n ; <http://ex/x> ?x ; <http://ex/b> ?b ; <http://ex/e> ?f } "
				+ "ITERATOR iter:JSONPath(\"" + JSON + "\", \"$[*]\", \"$.n\", \"$.x\", \"$.b\", \"$.e\") AS ?e ?n ?x ?b ?f");
	}

	@Test
	public void jsonPathFunction() {
		assertGenerates(EXPECTED, "GENERATE { <http://ex/s> <http://ex/n> ?n ; <http://ex/x> ?x ; <http://ex/b> ?b ; <http://ex/e> ?f } "
				+ "WHERE { BIND(fun:JSONPath(\"" + JSON + "\", \"$[0].n\") AS ?n) BIND(fun:JSONPath(\"" + JSON
				+ "\", \"$[0].x\") AS ?x) BIND(fun:JSONPath(\"" + JSON + "\", \"$[0].b\") AS ?b) "
				+ "BIND(fun:JSONPath(\"" + JSON + "\", \"$[0].e\") AS ?f) }");
	}
}
