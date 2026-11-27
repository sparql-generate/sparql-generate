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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Test;

/**
 * A graph of FROM GENERATE shares the equal literals of its triples (the keys
 * of a join, one per row): one object instead of one per triple.
 */
public class StreamRDFModelTest {

	private static List<Node> objects(boolean share) {
		final Model model = ModelFactory.createDefaultModel();
		final StreamRDFModel stream = new StreamRDFModel(model, share);
		final Node p = NodeFactory.createURI("http://ex/p");
		for (int i = 0; i < 3; i++) {
			stream.triple(Triple.create(NodeFactory.createURI("http://ex/s" + i), p,
					NodeFactory.createLiteralString(new String("key".toCharArray()))));
		}
		final List<Node> objects = new ArrayList<>();
		model.getGraph().find().forEachRemaining((t) -> objects.add(t.getObject()));
		return objects;
	}

	@Test
	public void equalLiteralsAreShared() {
		final List<Node> shared = objects(true);
		assertEquals(3, shared.size());
		assertSame(shared.get(0), shared.get(1));
		assertSame(shared.get(0), shared.get(2));
		final List<Node> notShared = objects(false);
		assertNotSame(notShared.get(0), notShared.get(1));
	}

	/** A join on keys generated in the dataset gives the same output. */
	@Test
	public void joinOnSharedKeys() {
		EngineTestUtils.assertGenerates(
				"<http://ex/c/1> <http://ex/parent> <http://ex/p/a>, <http://ex/p/b> . <http://ex/c/2> <http://ex/parent> <http://ex/p/c> .",
				"BASE <http://ex/> PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
						+ "GENERATE { <c/{?k}> <parent> ?p . } "
						+ "FROM GENERATE { GENERATE { <p/{?v}> <key> ?k . } "
						+ "  WHERE { VALUES (?v ?k) { (\"a\" \"1\") (\"b\" \"1\") (\"c\" \"2\") (\"d\" \"3\") } } . } . "
						+ "WHERE { VALUES ?k { \"1\" \"2\" } ?p <key> ?k }");
	}
}
