/*
 * Copyright 2019 École des Mines de Saint-Étienne.
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
package fr.emse.ci.sparqlext.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.serializer.FormatterElement;
import org.apache.jena.sparql.syntax.ElementData;

/**
 *
 * @author maxime.lefrancois
 */
public class LogUtils {

    private static final Var VAR = Var.alloc("truncated");

    public static String log(List<Var> variables, List<Binding> input) {
        return FormatterElement.asString(compress(variables, input));
    }

    public static String log(List<Binding> input) {
        return FormatterElement.asString(compress(input));
    }

    private static ElementData compress(List<Binding> input) {
        ElementData el = new ElementData();

        if (input.size() < 10) {
            input.forEach((b) -> {
                addCompressedToElementData(el, b);
            });
            return el;
        }
        for (int i = 0; i < 5; i++) {
            addCompressedToElementData(el, input.get(i));
        }
        BindingMap binding = BindingFactory.create();
        Node n = NodeFactory.createLiteral("[ " + (input.size() - 10) + " more ]");
        el.getVars().forEach((v) -> binding.add(v, n));
        el.add(binding);
        for (int i = input.size() - 5; i < input.size(); i++) {
            addCompressedToElementData(el, input.get(i));
        }
        return el;
    }

    private static void addCompressedToElementData(ElementData el, Binding b) {
        final Binding compressed = compress(b);
        final Iterator<Var> varsIterator = compressed.vars();
        while (varsIterator.hasNext()) {
            el.add(varsIterator.next());
        }
        el.add(compressed);
    }

    private static ElementData compress(List<Var> variables, List<Binding> input) {
        ElementData el = new ElementData();
        variables.forEach(el::add);
        if (input.size() < 10) {
            input.forEach((b) -> el.add(compress(variables, b)));
            return el;
        }
        for (int i = 0; i < 5; i++) {
            el.add(compress(variables, input.get(i)));
        }
        BindingMap binding = BindingFactory.create();
        Node n = NodeFactory.createLiteral("[ " + (input.size() - 10) + " more ]");
        variables.forEach((v) -> binding.add(v, n));
        el.add(binding);
        for (int i = input.size() - 5; i < input.size(); i++) {
            el.add(compress(variables, input.get(i)));
        }
        return el;
    }

    public static Binding compress(Binding input) {
        final List<Var> vars = new ArrayList<>();
        final Iterator<Var> varsIterator = input.vars();
        while (varsIterator.hasNext()) {
            vars.add(varsIterator.next());
        }
        return compress(vars, input);
    }

    public static Binding compress(List<Var> variables, Binding input) {
        final BindingMap binding = BindingFactory.create();
        for (Var v : variables) {
            Node n = input.get(v);
            if (n != null) {
                binding.add(v, compress(input.get(v)));
            }
        }
        return binding;
    }

    public static Triple compress(Triple t) {
        Node o = compress(t.getObject());
        return new Triple(t.getSubject(), t.getPredicate(), o);
    }

    public static Node compress(Node n) {
        if (n.isLiteral()) {
            n = NodeFactory.createLiteral(compress(n.getLiteralLexicalForm()), n.getLiteralLanguage(), n.getLiteralDatatype());
        }
        return n;
    }

    public static String compress(String s) {
        if (s.length() > 60) {
            s = s.substring(0, 40) + "\n"
                    + " ... " + s.substring(s.length() - 15);
        }
        return s;
    }

}
