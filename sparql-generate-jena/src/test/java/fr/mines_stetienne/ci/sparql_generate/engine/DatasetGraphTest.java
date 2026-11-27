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
package fr.mines_stetienne.ci.sparql_generate.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.junit.Test;

/**
 * The graphs of a FROM dataset hold many keys that differ only in their last
 * characters (the join keys of GTFS-Madrid-Bench at scale 1000) in seconds:
 * Jena's default GraphMem2Fast took more than 200 s for 1.26 M of them.
 */
public class DatasetGraphTest {

	private static String key(int i) {
		final String s = Integer.toString(i, 36);
		return "0000000000000000".substring(0, 20 - s.length() > 16 ? 16 : 20 - s.length()) + s;
	}

	@Test(timeout = 60_000)
	public void manyKeysWithACommonPrefix() {
		final Graph graph = DatasetDeclarationPlan.newGraph();
		final Node p = NodeFactory.createURI("urn:x:key");
		final int n = 1_300_000;
		for (int i = 0; i < n; i++) {
			final String k = key(i);
			graph.add(Triple.create(NodeFactory.createURI("http://transport.linkeddata.es/madrid/metro/stops/" + k), p,
					NodeFactory.createLiteralString(k)));
		}
		assertEquals(n, graph.size());
		assertTrue(graph.contains(Node.ANY, p, NodeFactory.createLiteralString(key(n - 1))));
	}
}
