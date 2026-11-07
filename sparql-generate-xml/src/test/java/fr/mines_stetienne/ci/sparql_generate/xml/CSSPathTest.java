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
package fr.mines_stetienne.ci.sparql_generate.xml;

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

public class CSSPathTest {

	static {
		SPARQLExt.init();
	}

	/**
	 * After the results of the selectors, iter:CSSPath gives the position of
	 * the element and whether another element follows, as iter:XPath and
	 * iter:JSONPath do.
	 */
	@Test
	public void positionAndHasNext() {
		Model expected = ModelFactory.createDefaultModel();
		RDFDataMgr.read(expected, new StringReader("<http://ex/a> <http://ex/pos> 0 ; <http://ex/hasNext> true . "
				+ "<http://ex/b> <http://ex/pos> 1 ; <http://ex/hasNext> false ."), null, Lang.TTL);
		Model actual = PlanFactory
				.create("PREFIX ite: <http://w3id.org/sparql-generate/iter/> "
						+ "GENERATE { <http://ex/{?t}> <http://ex/pos> ?i ; <http://ex/hasNext> ?hasNext } "
						+ "ITERATOR ite:CSSPath(\"<ul><li>a</li><li>b</li></ul>\", \"li\", \"li/text()\") "
						+ "AS ?li ?t ?i ?hasNext")
				.execGenerate(ContextUtils.build().build());
		StringWriter sw = new StringWriter();
		RDFDataMgr.write(sw, actual, Lang.NT);
		assertTrue("got:\n" + sw, actual.isIsomorphicWith(expected));
	}
}
