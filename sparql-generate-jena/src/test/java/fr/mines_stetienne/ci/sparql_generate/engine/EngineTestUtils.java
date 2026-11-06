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

import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * Helpers to execute a query given as a string and compare its output.
 */
public final class EngineTestUtils {

	static {
		SPARQLExt.init();
	}

	private EngineTestUtils() {
	}

	public static Model generate(String query) {
		RootPlan plan = PlanFactory.create(query);
		return plan.execGenerate(ContextUtils.build().build());
	}

	public static String template(String query) {
		RootPlan plan = PlanFactory.create(query);
		return plan.execTemplate(ContextUtils.build().build());
	}

	public static Model turtle(String turtle) {
		Model model = ModelFactory.createDefaultModel();
		RDFDataMgr.read(model, new StringReader(turtle), null, Lang.TTL);
		return model;
	}

	public static void assertGenerates(String expectedTurtle, String query) {
		Model expected = turtle(expectedTurtle);
		Model actual = generate(query);
		assertTrue("expected:\n" + write(expected) + "\nbut got:\n" + write(actual),
				actual.isIsomorphicWith(expected));
	}

	public static String write(Model model) {
		StringWriter sw = new StringWriter();
		RDFDataMgr.write(sw, model, Lang.NT);
		return sw.toString();
	}
}
