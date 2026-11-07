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
}
