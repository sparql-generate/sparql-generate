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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * The sub-queries of iter:XPath that are paths of element names ending with
 * an attribute or text() are evaluated without the XPath engine: same output.
 */
public class XPathSimplePathTest {

	static {
		SPARQLExt.init();
	}

	private static final String XML = "<r><p id=\"1\"><n>a &amp; b</n><n>second</n><q><n>deep</n></q></p>"
			+ "<p id=\"2\"><n><![CDATA[c <d>]]></n><x:n xmlns:x=\"urn:x\">prefixed</x:n></p>"
			+ "<p><n/><n>  </n><m><k>1</k></m><m><k>2</k></m></p><p id=\"\"/></r>";

	private static Model generate(boolean simple) {
		boolean before = ITER_XPath.simplePaths;
		ITER_XPath.simplePaths = simple;
		try {
			return PlanFactory.create("PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
					+ "GENERATE { <http://ex/s{?i}> <http://ex/id> ?id ; <http://ex/n> ?n ; <http://ex/k> ?k ; "
					+ "<http://ex/none> ?none ; <http://ex/other> ?other ; <http://ex/elt> ?elt } "
					+ "ITERATOR iter:XPath(\"\"\"" + XML + "\"\"\", \"/r/p\", \"/p/@id\", \"/p/n/text()\", "
					+ "\"/p/m/k/text()\", \"/p/missing/text()\", \"/q/n/text()\", \"/p/n\") "
					+ "AS ?p ?id ?n ?k ?none ?other ?elt ?i")
					.execGenerate(ContextUtils.build().build());
		} finally {
			ITER_XPath.simplePaths = before;
		}
	}

	@Test
	public void sameAsTheXPathEngine() {
		Model engine = generate(false);
		Model simple = generate(true);
		StringWriter a = new StringWriter();
		RDFDataMgr.write(a, engine, Lang.NT);
		StringWriter b = new StringWriter();
		RDFDataMgr.write(b, simple, Lang.NT);
		assertFalse(engine.isEmpty());
		assertTrue("XPath engine:\n" + a + "\nsimple paths:\n" + b, simple.isIsomorphicWith(engine));
	}
}
