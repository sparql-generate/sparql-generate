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
package fr.mines_stetienne.ci.sparql_generate.function;

import java.util.Iterator;
import java.util.Locale;

/**
 * Suggests the known IRI closest to an unknown one, for the messages about
 * unknown functions and iterators: a misspelled iter:JSONpath is likely
 * iter:JSONPath.
 */
public final class Suggestions {

	private Suggestions() {
	}

	/**
	 * @param unknown the unknown IRI
	 * @param known   the known IRIs
	 * @return " Did you mean &lt;iri&gt;?" for the known IRI of the same
	 *         namespace whose local name is the closest to that of the unknown
	 *         one, ignoring case, if it is close enough; or "".
	 */
	public static String didYouMean(final String unknown, final Iterator<String> known) {
		final int split = Math.max(unknown.lastIndexOf('/'), unknown.lastIndexOf('#')) + 1;
		final String namespace = unknown.substring(0, split);
		final String name = unknown.substring(split).toLowerCase(Locale.ROOT);
		if (name.isEmpty()) {
			return "";
		}
		String best = null;
		int bestDistance = Integer.MAX_VALUE;
		while (known.hasNext()) {
			final String iri = known.next();
			if (!iri.startsWith(namespace) || iri.equals(unknown)) {
				continue;
			}
			final String other = iri.substring(namespace.length());
			if (other.indexOf('/') >= 0 || other.indexOf('#') >= 0) {
				continue;
			}
			final int distance = distance(name, other.toLowerCase(Locale.ROOT));
			if (distance < bestDistance || (distance == bestDistance && iri.compareTo(best) < 0)) {
				best = iri;
				bestDistance = distance;
			}
		}
		// at most two edits, and less than half of the name
		if (best == null || bestDistance > 2 || 2 * bestDistance >= name.length()) {
			return "";
		}
		return " Did you mean <" + best + ">?";
	}

	/**
	 * The Levenshtein distance.
	 */
	static int distance(final String a, final String b) {
		int[] previous = new int[b.length() + 1];
		int[] current = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) {
			previous[j] = j;
		}
		for (int i = 1; i <= a.length(); i++) {
			current[0] = i;
			for (int j = 1; j <= b.length(); j++) {
				final int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
			}
			final int[] t = previous;
			previous = current;
			current = t;
		}
		return previous[b.length()];
	}
}
