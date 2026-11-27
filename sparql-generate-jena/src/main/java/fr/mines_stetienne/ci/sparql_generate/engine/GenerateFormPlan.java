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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.modify.TemplateLib;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The plan of a {@code GENERATE { ... }} template: its triple and quad
 * patterns, instantiated once per solution, then its sub-queries.
 *
 * <p>
 * A pattern of the default graph is emitted with {@link StreamRDF#triple}, as
 * before quad patterns existed; a pattern of a {@code GRAPH} block with
 * {@link StreamRDF#quad}, if its graph node is bound to an IRI or a blank node.
 * A sub-query of a {@code GRAPH} block emits its default graph in that graph.
 * </p>
 *
 * @author Maxime Lefrançois
 */
public class GenerateFormPlan implements GeneratePlan {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(GenerateFormPlan.class);

    /**
     * The patterns, in the order of the template. The graph of a pattern of
     * the default graph is {@link Quad#defaultGraphNodeGenerated}.
     */
    private final List<Quad> template;

    /**
     * The sub queries.
     */
    private final List<RootPlan> subQueries;

    /**
     * The graph node of the GRAPH block of each sub-query, or null for a
     * sub-query outside GRAPH blocks.
     */
    private final List<Node> subQueryGraphs;

    private static final Node FIRST = RDF.first.asNode();
    private static final Node REST = RDF.rest.asNode();

    /**
     * Constructor.
     *
     * @param basicGraphPattern the basic pattern.
     * @param subQueries the sub queries.
     */
    public GenerateFormPlan(final BasicPattern basicGraphPattern, List<RootPlan> subQueries) {
        this(toQuads(basicGraphPattern), subQueries, Collections.nCopies(subQueries.size(), (Node) null));
    }

    /**
     * Constructor for a template with quad patterns.
     *
     * @param template the patterns; the graph of a pattern of the default
     * graph is {@link Quad#defaultGraphNodeGenerated}, that of a pattern of a
     * GRAPH block is a variable or an IRI.
     * @param subQueries the sub queries.
     * @param subQueryGraphs for each sub-query, the graph node of its GRAPH
     * block, or null.
     */
    public GenerateFormPlan(final List<Quad> template, final List<RootPlan> subQueries, final List<Node> subQueryGraphs) {
        Objects.requireNonNull(template);
        Objects.requireNonNull(subQueries);
        Objects.requireNonNull(subQueryGraphs);
        if (subQueries.size() != subQueryGraphs.size()) {
            throw new IllegalArgumentException("One graph node, or null, per sub-query");
        }
        this.template = new ArrayList<>(template);
        this.subQueries = subQueries;
        this.subQueryGraphs = new ArrayList<>(subQueryGraphs);
    }

    /**
     * @return the plans of the sub-queries.
     */
    List<RootPlan> getSubQueries() {
        return Collections.unmodifiableList(subQueries);
    }

    /**
     * @return true if a pattern of the template has a list ({@code LIST(?x)}
     * without {@code GROUP BY}), whose nodes depend on the position of the
     * solution in its batch.
     */
    boolean hasList() {
        for (Quad q : template) {
            if (q.getSubject() instanceof Node_List || q.getObject() instanceof Node_List) {
                return true;
            }
        }
        return false;
    }

    private static List<Quad> toQuads(final BasicPattern bgp) {
        final List<Quad> quads = new ArrayList<>(bgp.size());
        for (Triple t : bgp.getList()) {
            quads.add(Quad.create(Quad.defaultGraphNodeGenerated, t));
        }
        return quads;
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
            if ((i & 255) == 0) {
                ContextUtils.checkLimits(context);
            }
            final Map<Node, Node> bNodeMap = new HashMap<>();
            final Binding binding = values.get(i);
            for (Quad q : template) {
                // null for the default graph
                final Node graph;
                if (Quad.isDefaultGraph(q.getGraph())) {
                    graph = null;
                } else {
                    graph = graphName(q.getGraph(), binding);
                    if (graph == null) {
                        continue;
                    }
                }
                final Triple t = q.asTriple();
                if (t.getObject() instanceof Node_List) {
                    substAndOutputForList(graph, t.getSubject(), t.getPredicate(), (Node_List) t.getObject(), sb, binding, outputStream, newContext, i, bNodeMap);
                } else {
                    Triple t2 = TemplateLib.subst(t, binding, bNodeMap);
                    if (ListAggregate.isList(t2.getObject())) {
                        outputList(graph, sb, outputStream, newContext, t2);
                    } else {
                        outputIfConcrete(graph, sb, outputStream, context, t2);
                    }
                }
            }

        }
        if (LOG.isTraceEnabled()) {
            LOG.trace(sb.toString());
        }

        for (int i = 0; i < subQueries.size(); i++) {
            final RootPlan subPlan = subQueries.get(i);
            final Node graphNode = subQueryGraphs.get(i);
            if (graphNode == null) {
                subPlan.execSubGenerateStream(variables, values, newContext);
            } else if (graphNode.isVariable()) {
                // the solutions are split by graph; those whose graph node is
                // not bound to an IRI or a blank node give nothing
                final Map<Node, List<Binding>> byGraph = new LinkedHashMap<>();
                for (Binding binding : values) {
                    final Node graph = graphName(graphNode, binding);
                    if (graph != null) {
                        byGraph.computeIfAbsent(graph, (g) -> new ArrayList<>()).add(binding);
                    }
                }
                for (Map.Entry<Node, List<Binding>> entry : byGraph.entrySet()) {
                    subPlan.execSubGenerateStream(variables, entry.getValue(),
                            inGraph(newContext, outputStream, entry.getKey(), entry.getValue().size()));
                }
            } else {
                subPlan.execSubGenerateStream(variables, values, inGraph(newContext, outputStream, graphNode, size));
            }
        }
    }

    /**
     * A context whose GENERATE output puts the default graph in a named graph.
     */
    private static Context inGraph(final Context context, final StreamRDF outputStream, final Node graph, final int size) {
        return ContextUtils.fork(context)
                .setSize(size)
                .setGenerateOutput(new GraphRoutingStreamRDF(outputStream, graph))
                .fork();
    }

    /**
     * The graph name that a graph node denotes for a solution: an IRI, or a
     * blank node, which RDF 1.1 datasets and Jena accept as graph names; null
     * if the variable is unbound or bound to a literal or a triple term, as for
     * a triple whose term is not valid RDF.
     */
    static Node graphName(final Node graphNode, final Binding binding) {
        final Node graph = graphNode.isVariable() ? binding.get(Var.alloc(graphNode)) : graphNode;
        if (graph == null || !(graph.isURI() || graph.isBlank())) {
            return null;
        }
        return graph;
    }

    private synchronized void outputIfConcrete(
            final Node graph,
            final StringBuilder sb,
            final StreamRDF outputStream,
            final Context context,
            final Triple t) {
        // as in CONSTRUCT, a triple that is not valid RDF is not output
        if (t.isConcrete() && isValid(t)) {
            ContextUtils.countTriple(context);
            if (LOG.isTraceEnabled()) {
                Triple t2 = LogUtils.compress(t);
                sb.append("\n  ").append(t2);
                if (graph != null) {
                    sb.append(" in ").append(graph);
                }
            }
            if (graph == null) {
                outputStream.triple(t);
            } else {
                outputStream.quad(Quad.create(graph, t));
            }
        }
    }

    /**
     * Writes the value of a list aggregate as a fresh RDF list.
     */
    private void outputList(final Node graph, final StringBuilder sb, final StreamRDF outputStream, final Context context, final Triple t) {
        final List<Node> nodes = ListAggregate.nodes(t.getObject());
        Node current = nodes.isEmpty() ? RDF.nil.asNode() : NodeFactory.createBlankNode();
        outputIfConcrete(graph, sb, outputStream, context, Triple.create(t.getSubject(), t.getPredicate(), current));
        for (int i = 0; i < nodes.size(); i++) {
            final Node next = i == nodes.size() - 1 ? RDF.nil.asNode() : NodeFactory.createBlankNode();
            outputIfConcrete(graph, sb, outputStream, context, Triple.create(current, FIRST, nodes.get(i)));
            outputIfConcrete(graph, sb, outputStream, context, Triple.create(current, REST, next));
            current = next;
        }
    }

    private static boolean isValid(final Triple t) {
        final Node s = t.getSubject();
        return (s.isURI() || s.isBlank() || s.isTripleTerm()) && t.getPredicate().isURI();
    }

    private void substAndOutputForList(
            final Node graph,
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
        Triple t = Triple.create(s2, p2, first);
        Triple t2 = Substitute.substitute(t, binding);
        outputIfConcrete(graph, sb, outputStream, context, t2);
        // potentially substitute var
        Node var2 = subst(var, bNodeMap);
        Node var2sub = Substitute.substitute(var2, binding);
        Triple tfirst = Triple.create(current, FIRST, var2sub);
        outputIfConcrete(graph, sb, outputStream, context, tfirst);
        // nothing to substitute here
        Triple tRest = Triple.create(current, REST, next);
        outputIfConcrete(graph, sb, outputStream, context, tRest);
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
