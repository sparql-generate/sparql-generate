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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.util.Context;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * iter:JSONSurfer without a batch size: one batch, unless the output of the
 * query does not depend on the batches.
 */
public class JSONSurferBatchTest {

	static {
		SPARQLExt.init();
	}

	private static final String JSON = "[{\"id\": 1}, {\"id\": 2}, {\"id\": 3}, {\"id\": 4}, {\"id\": 5}]";

	private static List<Integer> batches(Context context) {
		final ExprList args = new ExprList();
		args.add(NodeValue.makeString(JSON));
		args.add(NodeValue.makeString("$[*]"));
		args.add(NodeValue.makeString("$.id"));
		final ITER_JSONSurfer surfer = new ITER_JSONSurfer();
		surfer.build(args);
		final List<Integer> sizes = new ArrayList<>();
		surfer.exec(BindingFactory.empty(), args, new FunctionEnvBase(context), (rows) -> sizes.add(rows.size()));
		return sizes;
	}

	@Test
	public void batchesWhenTheOutputDoesNotDependOnThem() {
		assertEquals(List.of(5), batches(ContextUtils.build().build()));
		assertEquals(List.of(2, 2, 1), batches(ContextUtils.withIteratorBatchSize(ContextUtils.build().build(), 2)));
	}

	private static Model generate(int batchSize) {
		return PlanFactory.create("PREFIX iter: <http://w3id.org/sparql-generate/iter/> BASE <http://ex/> "
				+ "GENERATE { <r/{?id}> <a> ?id . } ITERATOR iter:JSONSurfer('" + JSON.replace("{", "\\{").replace("}", "\\}")
				+ "', \"$[*]\",\"$.id\") AS ?o ?id WHERE { }")
				.execGenerate(ContextUtils.build().setBatchSize(batchSize).build());
	}

	@Test
	public void sameOutputInBatches() {
		final Model oneBatch = generate(0);
		assertEquals(5, oneBatch.size());
		for (int batchSize : new int[] { 1, 2, 5 }) {
			assertTrue(generate(batchSize).isIsomorphicWith(oneBatch));
		}
	}
}
