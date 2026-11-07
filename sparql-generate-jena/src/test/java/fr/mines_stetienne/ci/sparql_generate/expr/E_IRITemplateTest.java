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
package fr.mines_stetienne.ci.sparql_generate.expr;

import static org.junit.Assert.assertEquals;

import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.expr.E_IRI;
import org.apache.jena.sparql.expr.E_StrConcat;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.junit.Test;

/**
 * E_IRITemplate has the value of E_IRI, with or without its shortcut.
 */
public class E_IRITemplateTest {

	private static final String[] BASES = { "http://ex.org/a/b", "http://ex.org/a/", "http://ex.org", null };

	private static final String[] PREFIXES = { "http://ex.org/stop/", "http://ex.org/stop", "stop/", "stop", "../x/",
			"/root/", "..", "a/.", "http://ex.org", "http://ex.org/q?x=", "http://ex.org/f#", "urn:x:", "_:b",
			"HTTP://EX.ORG/A/", "http://ex.org/a b/" };

	private static final String[] RESTS = { "", "12", "a%20b", "a/b", "..", ".", "./x", "/../x", "x/..", ".hidden",
			"12.5", "%zz", "%2", "a b", "é", "?q", "#f", "//x", "~-_" };

	private static String eval(Expr expr, String base) {
		try {
			// the base of E_IRI is the parser base, given here
			return expr.eval(BindingFactory.binding(), new FunctionEnvBase()).asNode().getURI();
		} catch (ExprEvalException ex) {
			return "error";
		} catch (RuntimeException ex) {
			return "error " + ex.getClass().getSimpleName();
		}
	}

	@Test
	public void sameValueAsIRI() {
		int n = 0;
		for (String base : BASES) {
			for (String prefix : PREFIXES) {
				for (String rest : RESTS) {
					ExprList args = new ExprList();
					args.add(NodeValue.makeString(prefix));
					args.add(NodeValue.makeString(rest));
					String expected = eval(new E_IRI(base, new E_StrConcat(args)), base);
					E_IRITemplate template = new E_IRITemplate(base, new E_StrConcat(args));
					// twice: the resolved prefix is cached
					assertEquals("base " + base + ", " + prefix + " + " + rest, expected, eval(template, base));
					assertEquals("base " + base + ", " + prefix + " + " + rest, expected, eval(template, base));
					n++;
				}
			}
		}
		assertEquals(BASES.length * PREFIXES.length * RESTS.length, n);
	}
}
