/*
 * Copyright 2017 Ecole des Mines de Saint-Etienne.
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
package com.github.thesmartenergy.sparql.generate.jena.serializer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.serializer.SerializationContext;

/**
 *
 * @author maxime.lefrancois
 */
public class SPARQLGenerateFormatterBase {

    protected final IndentedWriter out;
    protected final SerializationContext context;

    protected SPARQLGenerateFormatterBase(IndentedWriter _out, SerializationContext _context) {
        out = _out;
        context = _context;
    }

    public void finishVisit() {
        out.flush();
    }

    // Utilities
    protected void formatTriples(BasicPattern pattern) {
        SPARQLGenerateFmtUtils.formatPattern(out, pattern, context);
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
            SPARQLGenerateFmtUtils.printNode(w, n, context);
        }
        return new String(os.toByteArray(), Charset.defaultCharset());
        
    }

}
