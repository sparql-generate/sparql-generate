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

import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.util.Context;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * A JSON document read by iter:JSONSurfer registers a task that stops its
 * parser if the execution is closed, and removes it when the document is
 * read: the task kept the parser and the rest of the query in memory until
 * the end of the execution, so that the memory grew with the number of
 * documents read.
 */
public class JSONSurferClosingTasksTest {

	static {
		SPARQLExt.init();
	}

	private static void read(Context context, String json) {
		final ExprList args = new ExprList();
		args.add(NodeValue.makeString(json));
		args.add(NodeValue.makeString("$[*]"));
		final ITER_JSONSurfer surfer = new ITER_JSONSurfer();
		surfer.build(args);
		surfer.exec(BindingFactory.empty(), args, new FunctionEnvBase(context), (rows) -> {
		});
	}

	@Test
	public void documentsReadLeaveNoClosingTask() {
		final Context context = ContextUtils.build().build();
		for (int i = 0; i < 100; i++) {
			read(context, "[" + i + ", " + (i + 1) + "]");
		}
		assertEquals(0, ContextUtils.countTasksOnClose(context));
	}

	@Test
	public void documentInErrorLeavesNoClosingTask() {
		final Context context = ContextUtils.build().build();
		for (int i = 0; i < 10; i++) {
			try {
				read(context, "[" + i + ", ");
			} catch (RuntimeException ex) {
				// a document that is not JSON
			}
		}
		assertEquals(0, ContextUtils.countTasksOnClose(context));
	}
}
