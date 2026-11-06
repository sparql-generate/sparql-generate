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

public class XPathTest {

	static {
		SPARQLExt.init();
	}

	private static final String PREFIXES = "PREFIX ite: <http://w3id.org/sparql-generate/iter/> "
			+ "PREFIX fun: <http://w3id.org/sparql-generate/fn/> PREFIX ex: <http://ex/> ";

	private static void assertGenerates(String expectedTurtle, String query) {
		Model expected = ModelFactory.createDefaultModel();
		RDFDataMgr.read(expected, new StringReader("PREFIX ex: <http://ex/> " + expectedTurtle), null, Lang.TTL);
		Model actual = PlanFactory.create(PREFIXES + query).execGenerate(ContextUtils.build().build());
		StringWriter sw = new StringWriter();
		RDFDataMgr.write(sw, actual, Lang.NT);
		assertTrue("got:\n" + sw, actual.isIsomorphicWith(expected));
	}

	private static String literal(String xml) {
		return "\"\"\"" + xml + "\"\"\"";
	}

	/**
	 * Elements that have an HTML name, such as body, are XML elements like
	 * any other.
	 */
	@Test
	public void iteratorKeepsElementsWithHtmlNames() {
		assertGenerates("ex:M17 ex:body \"Don't forget me\" . ex:M18 ex:body \"We leave at six\" .",
				"GENERATE { ?s ex:body ?body } ITERATOR ite:XPath(" + literal(
						"<notes><note id='M17'><heading>Reminder</heading><body>Don't forget me</body></note>"
								+ "<note id='M18'><heading>Update</heading><body>We leave at six</body></note></notes>")
						+ ", \"//note\", \"/note/@id\") AS ?note ?id "
						+ "WHERE { BIND(fun:XPath(?note, \"/note/body/text()\") AS ?body) BIND(IRI(CONCAT(\"http://ex/\", ?id)) AS ?s) }");
	}

	/**
	 * XML is case-sensitive (issue #109).
	 */
	@Test
	public void iteratorIsCaseSensitive() {
		assertGenerates("ex:Process_1 a ex:ProcessRef . ex:Process_2 a ex:ProcessRef .",
				"GENERATE { ?s a ex:ProcessRef } ITERATOR ite:XPath(" + literal(
						"<element id='Element_1'><user id='User_1' processRef='Process_1'/>"
								+ "<processRef id='Process_1' /><processRef id='Process_2' /></element>")
						+ ", \"//processRef\", \"/processRef/@id\") AS ?ref ?id "
						+ "WHERE { BIND(IRI(CONCAT(\"http://ex/\", ?id)) AS ?s) }");
	}

	/**
	 * Input that is not well-formed XML, such as HTML, is still cleaned
	 * before the XPath expression is evaluated.
	 */
	@Test
	public void iteratorStillCleansHtml() {
		assertGenerates("ex:doc ex:item \"one\", \"two\" .",
				"GENERATE { ex:doc ex:item ?item } ITERATOR ite:XPath(\"<ul><li>one<li>two</ul>\", \"//li/text()\") AS ?item");
	}
}
