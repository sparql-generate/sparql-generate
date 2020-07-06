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

import fr.mines_stetienne.ci.sparql_generate.utils.LogUtils;
import fr.mines_stetienne.ci.sparql_generate.graph.Node_List;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.modify.TemplateLib;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Maxime Lefrançois
 */
public class GenerateFormPlan implements GeneratePlan {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(GenerateFormPlan.class);

    /**
     * The basic pattern.
     */
    private final BasicPattern bgp;

    /**
     * The sub queries.
     */
    private final List<RootPlan> subQueries;

    private static final Node FIRST = RDF.first.asNode();
    private static final Node REST = RDF.rest.asNode();

    /**
     * Constructor.
     *
     * @param basicGraphPattern the basic pattern.
     * @param subQueries the sub queries.
     */
    public GenerateFormPlan(final BasicPattern basicGraphPattern, List<RootPlan> subQueries) {
        this.bgp = basicGraphPattern;
        this.subQueries = subQueries;
    }

    @Override
    public void exec(
            final List<Var> variables,
            final List<Binding> values,
            final Context context) {
    	final StreamRDF outputStream = ContextUtils.getGenerateOutput(context);
        final StringBuilder sb = new StringBuilder("Output triples");
        final int size = values.size();
        final Context newContext = ContextUtils.fork(context)
                .setSize(size)
                .fork();

        for (int i = 0; i < size; i++) {
            final Map<Node, Node> bNodeMap = new HashMap<>();
            final Binding binding = values.get(i);
            for (Triple t : bgp.getList()) {
                if (t.getObject() instanceof Node_List) {
                    substAndOutputForList(t.getSubject(), t.getPredicate(), (Node_List) t.getObject(), sb, binding, outputStream, newContext, i, bNodeMap);
                } else {
                    Triple t2 = TemplateLib.subst(t, binding, bNodeMap);
                    outputIfConcrete(sb, outputStream, t2);
                }
            }

        }
        if (LOG.isTraceEnabled()) {
            LOG.trace(sb.toString());
        }

        for (int i = 0; i < subQueries.size(); i++) {
            RootPlan subPlan = subQueries.get(i);
            subPlan.execGenerateStream(values, newContext);
        }
    }

    private synchronized void outputIfConcrete(
            final StringBuilder sb,
            final StreamRDF outputStream,
            final Triple t) {
        if (t.isConcrete()) {
            if (LOG.isTraceEnabled()) {
                Triple t2 = LogUtils.compress(t);
                sb.append("\n  ").append(t2);
            }
            outputStream.triple(t);
        }
    }

    private void substAndOutputForList(
            final Node subject,
            final Node predicate,
            final Node_List list,
            final StringBuilder sb,
            final Binding binding,
            final StreamRDF outputStream,
            final Context context,
            final int position,
            final Map<Node, Node> bNodeMap) {

        final Node first = ContextUtils.getNode(context, list, 0);
        final Node current = ContextUtils.getNode(context, list, position);
        final Node next = ContextUtils.getNode(context, list, position + 1);
        final Node var = list.getExpr().asVar();
        // potentially substitute subject and predicate
        Node s2 = subst(subject, bNodeMap);
        Node p2 = subst(predicate, bNodeMap);
        Triple t = new Triple(s2, p2, first);
        Triple t2 = Substitute.substitute(t, binding);
        outputIfConcrete(sb, outputStream, t2);
        // potentially substitute var
        Node var2 = subst(var, bNodeMap);
        Node var2sub = Substitute.substitute(var2, binding);
        Triple tfirst = new Triple(current, FIRST, var2sub);
        outputIfConcrete(sb, outputStream, tfirst);
        // nothing to substitute here
        Triple tRest = new Triple(current, REST, next);
        outputIfConcrete(sb, outputStream, tRest);
    }

    private Node subst(Node n, Map<Node, Node> bNodeMap) {
        if (n.isBlank() || Var.isBlankNodeVar(n)) {
            if (!bNodeMap.containsKey(n)) {
                bNodeMap.put(n, NodeFactory.createBlankNode());
            }
            return bNodeMap.get(n);
        }
        return n;
    }

}
