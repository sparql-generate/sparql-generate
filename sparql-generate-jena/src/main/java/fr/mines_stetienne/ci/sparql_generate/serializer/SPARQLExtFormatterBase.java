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
package fr.mines_stetienne.ci.sparql_generate.serializer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.serializer.SerializationContext;

/**
 * Base class for formatters.
 *
 * @author Maxime Lefrançois
 */
public class SPARQLExtFormatterBase {

    protected final IndentedWriter out;
    protected final SerializationContext context;

    protected SPARQLExtFormatterBase(IndentedWriter _out, SerializationContext _context) {
        out = _out;
        context = _context;
    }

    public void finishVisit() {
        out.flush();
    }

    // Utilities
    protected void formatTriples(BasicPattern pattern) {
        SPARQLExtFmtUtils.formatPattern(out, pattern, context);
    }

    protected void formatTriple(Triple tp) {
        out.print(slotToString(tp.getSubject()));
        out.print(" ");
        out.print(slotToString(tp.getPredicate()));
        out.print(" ");
        out.print(slotToString(tp.getObject()));
    }

    protected String slotToString(Node n) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (IndentedWriter w = new IndentedWriter(os)) {
            SPARQLExtFmtUtils.printNode(w, n, context);
        }
        return new String(os.toByteArray(), Charset.defaultCharset());

    }

}
