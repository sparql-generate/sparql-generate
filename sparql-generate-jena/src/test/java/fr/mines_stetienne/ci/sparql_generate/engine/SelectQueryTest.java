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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

public class SelectQueryTest {

	static {
		SPARQLExt.init();
	}

	/**
	 * Without a WHERE clause, the variables bound by the binding clauses are
	 * still projected on the SELECT clause.
	 */
	@Test
	public void projectionWithoutWhereClause() {
		ResultSet rs = PlanFactory.create("SELECT ?y BIND(1 AS ?x) BIND(2 AS ?y) WHERE { }")
				.execSelect(ContextUtils.build().build());
		assertEquals(Arrays.asList("y"), rs.getResultVars());
		Binding binding = rs.nextBinding();
		assertFalse(binding.contains(Var.alloc("x")));
		assertEquals("2", binding.get(Var.alloc("y")).getLiteralLexicalForm());
	}

	@Test
	public void starWithoutWhereClause() {
		ResultSet rs = PlanFactory.create("SELECT * BIND(1 AS ?x) BIND(2 AS ?y) WHERE { }")
				.execSelect(ContextUtils.build().build());
		assertEquals(2, rs.getResultVars().size());
	}
}
