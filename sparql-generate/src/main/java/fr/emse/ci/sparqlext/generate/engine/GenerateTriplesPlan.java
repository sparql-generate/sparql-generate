/*
 * Copyright 2016 The Apache Software Foundation.
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
package fr.emse.ci.sparqlext.generate.engine;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.graph.Node_List;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.modify.TemplateLib;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Generates a triples block in the {@code GENERATE} clause.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class GenerateTriplesPlan implements ExecutionPlan {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(GenerateTriplesPlan.class);

    /**
     * The basic pattern.
     */
    private final BasicPattern bgp;

    /**
     * Constructor.
     *
     * @param basicGraphPattern - the basic pattern.
     */
    public GenerateTriplesPlan(final BasicPattern basicGraphPattern) {
        this.bgp = basicGraphPattern;
    }
    
    private static final Node FIRST = RDF.first.asNode();
    private static final Node REST = RDF.rest.asNode();

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Void> exec(
            final Dataset inputDataset,
            final List<Var> variables,
            final List<Binding> values,
            final BNodeMap bNodeMap,
            final Context context,
            final StreamRDF outputGenerate,
            final Consumer<ResultSet> outputSelect,
            final Consumer<String> outputTemplate) {
        final Executor executor = (Executor) context.get(SPARQLExt.EXECUTOR);
        final PrefixMapping pm = (PrefixMapping) context.get(SPARQLExt.PREFIX_MANAGER);
        return CompletableFuture.runAsync(() -> {
            final StringBuilder sb = new StringBuilder("Output triples");
            for (int i = 0; i < values.size(); i++) {
                sb.append("\nposition ").append(i).append(":");
                final Binding binding = values.get(i);
                for (Triple t : bgp.getList()) {
                    if (t.getObject() instanceof Node_List) {
                        substAndOutputForList(t.getSubject(), t.getPredicate(), (Node_List) t.getObject(), sb, binding, outputGenerate, bNodeMap, context, i);
                    } else {
                        Triple t2 = TemplateLib.subst(t, binding, bNodeMap.asMap());
                        outputIfConcrete(sb, outputGenerate, t2, pm);
                    }
                }
            }
            LOG.trace(sb.toString());
        }, executor);
    }
    
    private synchronized void outputIfConcrete(
            final StringBuilder sb,
            final StreamRDF outputStream,
            final Triple t,
            final PrefixMapping pm) {
        if (t.isConcrete()) {
            if (LOG.isTraceEnabled()) {
                Triple t2 = SPARQLExt.compress(t);
                sb.append("\n  ").append(FmtUtils.stringForTriple(t2, pm));
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
            final BNodeMap bNodeMap,
            final Context context,
            final int position) {
        final PrefixMapping pm = (PrefixMapping) context.get(SPARQLExt.PREFIX_MANAGER);
        final Node current = SPARQLExt.getNode(context, list, position);
        final Node next = SPARQLExt.getNode(context, list, position + 1);
        final Node var = list.getExpr().asVar();
        if (position == 0) {
            // potentially substitute subject and predicate
            Node s2 = subst(subject, bNodeMap.asMap());
            Node p2 = subst(predicate, bNodeMap.asMap());
            Triple t = new Triple(s2, p2, current);
            Triple t2 = Substitute.substitute(t, binding);
            outputIfConcrete(sb, outputStream, t2, pm);
        }
        // potentially substitute var
        Node var2 = subst(var, bNodeMap.asMap());
        Node var2sub = Substitute.substitute(var2, binding);
        Triple tfirst = new Triple(current, FIRST, var2sub);
        outputIfConcrete(sb, outputStream, tfirst, pm);
        // nothing to substitute here
        Triple tRest = new Triple(current, REST, next);
        outputIfConcrete(sb, outputStream, tRest, pm);
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
