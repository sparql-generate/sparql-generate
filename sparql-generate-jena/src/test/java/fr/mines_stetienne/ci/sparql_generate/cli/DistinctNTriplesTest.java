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
package fr.mines_stetienne.ci.sparql_generate.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * The N-Triples output without --stream: the set of triples, written as they
 * are generated.
 */
public class DistinctNTriplesTest {

	@Test
	public void sameTriplesAsTheGraph() {
		String query = "PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
				+ "GENERATE { <http://ex/s> <http://ex/p> ?j, \"é \\\"{?j}\\\"\"@fr . _:b <http://ex/q> ?j . } "
				+ "ITERATOR iter:for(0, 1, 50) AS ?i WHERE { BIND(?i / 5 AS ?j) }";
		Model graph = PlanFactory.create(query).execGenerate(ContextUtils.build().build());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DistinctNTriplesStreamRDF stream = new DistinctNTriplesStreamRDF(out);
		PlanFactory.create(query).execGenerateStream(ContextUtils.build().setGenerateOutput(stream).build());
		String text = new String(out.toByteArray(), StandardCharsets.UTF_8);
		Model streamed = ModelFactory.createDefaultModel();
		RDFDataMgr.read(streamed, new StringReader(text), null, Lang.NT);
		assertTrue(text, streamed.isIsomorphicWith(graph));
		// each triple once
		assertEquals(graph.size(), text.split("\n").length);
	}

	@Test
	public void digestSet() {
		DistinctNTriplesStreamRDF.DigestSet set = new DistinctNTriplesStreamRDF.DigestSet();
		Random random = new Random(1);
		Set<String> reference = new HashSet<>();
		for (int i = 0; i < 300_000; i++) {
			long h = random.nextInt(1000);
			long l = random.nextInt(1000);
			assertEquals(reference.add(h + " " + l), set.add(h, l));
		}
		assertEquals(reference.size(), set.size());
		assertFalse(set.add(0, 0) && set.add(0, 0));
	}
}
