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
package fr.mines_stetienne.ci.sparql_generate.normalizer.xexpr;

import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.PathBlock;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.expr.E_Bound;
import org.apache.jena.sparql.expr.E_SameTerm;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprTransformSubstitute;
import org.apache.jena.sparql.expr.ExprTransformer;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementAssign;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementDataset;
import org.apache.jena.sparql.syntax.ElementExists;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementAntiJoin;
import org.apache.jena.sparql.syntax.ElementLateral;
import org.apache.jena.sparql.syntax.ElementSemiJoin;
import org.apache.jena.sparql.syntax.ElementMinus;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementNotExists;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementService;
import org.apache.jena.sparql.syntax.ElementSubQuery;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.ElementUnfold;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.sparql.syntax.ElementVisitor;
import org.apache.jena.sparql.syntax.PatternVars;

/**
 * Class used to visit an element and normalize it (i.e., replace all expression
 * nodes with the associated expression). The node becomes a variable, bound
 * by a BIND clause or constrained by a FILTER in the group of the element.
 *
 * @author Maxime Lefrançois
 */
public class QueryPatternNormalizer implements ElementVisitor {

    /**
     *
     */
    private Element result;

    /**
     * The latest result of an element normalization.
     */
    public Element getResult() {
        return result;
    }

    /**
     * The BIND clauses of the embedded expressions of the element last
     * visited, which {@link #visit(ElementGroup)} places: none for an element
     * that has no embedded expression of its own.
     */
    private List<ElementBind> binds = new ArrayList<>();

    /**
     * True when the next group visited is the group of an OPTIONAL.
     */
    private boolean optionalGroup = false;

    /**
     * Places the BIND clauses of the embedded expressions of the elements of
     * a group, so that each one is evaluated where the variables of its
     * expression are bound (see {@link GroupScheduler}).
     */
    @Override
    public void visit(ElementGroup el) {
        final boolean optional = optionalGroup;
        optionalGroup = false;
        final List<Element> elements = new ArrayList<>();
        final List<List<ElementBind>> elementBinds = new ArrayList<>();
        for (final Element element : el.getElements()) {
            binds = new ArrayList<>();
            element.visit(this);
            elements.add(result);
            elementBinds.add(binds);
        }
        // the variables bound by the elements after each one
        final List<Set<Var>> later = new ArrayList<>();
        final Set<Var> after = new HashSet<>();
        for (int i = elements.size() - 1; i >= 0; i--) {
            later.add(0, new HashSet<>(after));
            after.addAll(PatternVars.vars(elements.get(i)));
        }
        final GroupScheduler scheduler = new GroupScheduler(optional);
        for (int i = 0; i < elements.size(); i++) {
            scheduler.add(elements.get(i), elementBinds.get(i), later.get(i));
        }
        result = scheduler.finish();
        binds = new ArrayList<>();
    }

    @Override
    public void visit(ElementTriplesBlock el) {
        final BasicPattern bgp = el.getPattern();
        final ElementTriplesBlock nzed = new ElementTriplesBlock();
        final NodeExprNormalizer nenzer = new NodeExprNormalizer();
        bgp.forEach((t) -> {
            t.getSubject().visitWith(nenzer);
            Node s = nenzer.getResult();
            t.getPredicate().visitWith(nenzer);
            Node p = nenzer.getResult();
            t.getObject().visitWith(nenzer);
            Node o = nenzer.getResult();
            nzed.addTriple(Triple.create(s, p, o));
        });
        endVisit(nzed, nenzer);
    }

    @Override
    public void visit(ElementPathBlock el) {
        final PathBlock pb = el.getPattern();
        final ElementPathBlock nzed = new ElementPathBlock();
        final NodeExprNormalizer nenzer = new NodeExprNormalizer();
        final PathNormalizer panzer = new PathNormalizer(nenzer);
        pb.forEach((t) -> {
            t.getSubject().visitWith(nenzer);
            Node s = nenzer.getResult();
            t.getObject().visitWith(nenzer);
            Node o = nenzer.getResult();
            if (t.isTriple()) {
                t.getPredicate().visitWith(nenzer);
                Node p = nenzer.getResult();
                nzed.addTriple(Triple.create(s, p, o));
            } else {
                t.getPath().visit(panzer);
                Path p = panzer.getResult();
                nzed.addTriplePath(new TriplePath(s, p, o));
            }
        });
        endVisit(nzed, nenzer);
    }

    @Override
    public void visit(ElementFilter el) {
        final ExprNormalizer enzer = new ExprNormalizer();
        final Expr nzed = enzer.normalize(el.getExpr());
        result = new ElementFilter(nzed);
    }

    @Override
    public void visit(ElementAssign el) {
        final ExprNormalizer enzer = new ExprNormalizer();
        final Var var = el.getVar();
        final Expr nzed = enzer.normalize(el.getExpr());
        result = new ElementAssign(var, nzed);
    }

    @Override
    public void visit(ElementBind el) {
        final ExprNormalizer enzer = new ExprNormalizer();
        final Var var = el.getVar();
        final Expr nzed = enzer.normalize(el.getExpr());
        result = new ElementBind(var, nzed);
    }

    @Override
    public void visit(ElementUnfold el) {
        final ExprNormalizer enzer = new ExprNormalizer();
        final Expr nzed = enzer.normalize(el.getExpr());
        result = new ElementUnfold(nzed, el.getVar1(), el.getVar2());
    }

    @Override
    public void visit(ElementData el) {
        final NodeExprNormalizer nenzer = new NodeExprNormalizer();
        final ElementData nzed = new ElementData();
        final List<Var> vars = el.getVars();
        vars.forEach((v) -> {
            nzed.add(v);
        });
        el.getRows().forEach((b) -> {
            final BindingBuilder bindingBuilder = Binding.builder();
            vars.forEach((v) -> {
                final Node n = b.get(v);
                if (n != null) {
                    n.visitWith(nenzer);
                    bindingBuilder.add(v, nenzer.getResult());
                }
            });
            nzed.add(bindingBuilder.build());
        });
        endVisit(nzed, nenzer);
    }

    @Override
    public void visit(ElementUnion el) {
        final ElementUnion res = new ElementUnion();
        for (final Element element : el.getElements()) {
            element.visit(this);
            res.addElement(result);
        }
        result = res;
    }

    @Override
    public void visit(ElementOptional el) {
        optionalGroup = el.getOptionalElement() instanceof ElementGroup;
        el.getOptionalElement().visit(this);
        result = new ElementOptional(result);
    }

    @Override
    public void visit(ElementLateral el) {
        el.getLateralElement().visit(this);
        result = new ElementLateral(result);
    }

    @Override
    public void visit(ElementSemiJoin el) {
        el.getSubElement().visit(this);
        result = new ElementSemiJoin(result);
    }

    @Override
    public void visit(ElementAntiJoin el) {
        el.getSubElement().visit(this);
        result = new ElementAntiJoin(result);
    }

    @Override
    public void visit(ElementDataset el) {
        throw new NullPointerException("should not reach this point");
    }

    @Override
    public void visit(ElementNamedGraph el) {
        final NodeExprNormalizer nenzer = new NodeExprNormalizer();
        el.getGraphNameNode().visitWith(nenzer);
        Node graph = nenzer.getResult();
        el.getElement().visit(this);
        Element element = result;
        endVisit(new ElementNamedGraph(graph, element), nenzer);
    }

    @Override
    public void visit(ElementExists el) {
        el.getElement().visit(this);
        result = new ElementExists(result);
    }

    @Override
    public void visit(ElementNotExists el) {
        el.getElement().visit(this);
        result = new ElementNotExists(result);
    }

    @Override
    public void visit(ElementMinus el) {
        el.getMinusElement().visit(this);
        result = new ElementMinus(result);
    }

    @Override
    public void visit(ElementService el) {
        final NodeExprNormalizer nenzer = new NodeExprNormalizer();
        el.getServiceNode().visitWith(nenzer);
        Node serviceNode = nenzer.getResult();
        el.getElement().visit(this);
        endVisit(new ElementService(serviceNode, result, el.getSilent()), nenzer);
    }

    @Override
    public void visit(ElementSubQuery el) {
        SPARQLExtQuery query = (SPARQLExtQuery) el.getQuery();
        query.normalizeXExpr();
        result = new ElementSubQuery(query);
    }

    private void endVisit(Element nzed, NodeExprNormalizer nenzer) {
        result = nzed;
        binds = new ArrayList<>();
        for (Element b : nenzer.getBindings()) {
            binds.add((ElementBind) b);
        }
    }

    /**
     * Places, in a group, the BIND clauses that replace the embedded
     * expressions of its elements. A BIND whose variables are bound before
     * the element, or come from outside the group (the VALUES block that the
     * engine places first in the WHERE clause, for the binding clauses and the
     * parent query), is placed before the element, as the element it
     * replaces would be evaluated: the pattern is then evaluated with the
     * node bound. In a basic graph pattern, a BIND that reads a variable of
     * other triples of the pattern is placed after them (the order of the
     * triples of a basic graph pattern does not matter). Otherwise, when the
     * variables are bound by the element itself or by a later element of the
     * group, or come from outside the group of an OPTIONAL (the left side,
     * which a FILTER of the group sees and a BIND does not), the node is
     * constrained by {@code FILTER(sameTerm(?node, expression))} at the end
     * of the group. A node whose expression is in error matches nothing.
     */
    private static final class GroupScheduler {

        private final boolean optional;

        private final ElementGroup group = new ElementGroup();

        /**
         * The variables bound by the elements placed so far.
         */
        private final Set<Var> bound = new HashSet<>();

        /**
         * The embedded expressions constrained at the end of the group, by
         * their variable.
         */
        private final Map<Var, Expr> filtered = new LinkedHashMap<>();

        GroupScheduler(final boolean optional) {
            this.optional = optional;
        }

        void add(final Element element, final List<ElementBind> binds, final Set<Var> later) {
            if (binds.isEmpty()) {
                place(element);
                return;
            }
            final Map<Var, ElementBind> pending = new LinkedHashMap<>();
            for (ElementBind b : binds) {
                pending.put(b.getVar(), b);
            }
            final Set<Var> elementVars = new HashSet<>(PatternVars.vars(element));
            elementVars.removeAll(pending.keySet());
            if (element instanceof ElementPathBlock || element instanceof ElementTriplesBlock) {
                addBlock(element, pending, elementVars, later);
            } else {
                placeReadyBinds(pending, elementVars, later);
                constrain(pending);
                place(element);
            }
        }

        /**
         * Places the triples of a basic graph pattern and the BIND clauses of
         * their nodes: each BIND as soon as its variables are bound, each
         * triple as soon as the BIND clauses of its nodes are placed.
         */
        private void addBlock(final Element element, final Map<Var, ElementBind> pending,
                final Set<Var> blockVars, final Set<Var> later) {
            final boolean paths = element instanceof ElementPathBlock;
            final List<TriplePath> triples = new ArrayList<>();
            if (paths) {
                ((ElementPathBlock) element).getPattern().forEach(triples::add);
            } else {
                ((ElementTriplesBlock) element).getPattern().forEach((t) -> triples.add(new TriplePath(t)));
            }
            List<TriplePath> segment = new ArrayList<>();
            boolean progress = true;
            while (progress) {
                progress = false;
                if (hasReadyBind(pending, blockVars, later)) {
                    placeBlock(segment, paths);
                    segment = new ArrayList<>();
                    placeReadyBinds(pending, blockVars, later);
                }
                for (Iterator<TriplePath> it = triples.iterator(); it.hasNext();) {
                    final TriplePath t = it.next();
                    if (!dependsOn(t, pending.keySet())) {
                        segment.add(t);
                        addVars(t, bound);
                        it.remove();
                        progress = true;
                    }
                }
            }
            segment.addAll(triples);
            for (TriplePath t : triples) {
                addVars(t, bound);
            }
            placeBlock(segment, paths);
            constrain(pending);
        }

        private boolean hasReadyBind(final Map<Var, ElementBind> pending, final Set<Var> elementVars,
                final Set<Var> later) {
            for (ElementBind b : pending.values()) {
                if (isReady(b, pending.keySet(), elementVars, later)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Places the BIND clauses whose variables are bound, or come from
         * outside the group, followed by a FILTER that their node is bound:
         * an expression in error matches nothing.
         */
        private void placeReadyBinds(final Map<Var, ElementBind> pending, final Set<Var> elementVars,
                final Set<Var> later) {
            boolean progress = true;
            while (progress) {
                progress = false;
                for (Iterator<ElementBind> it = pending.values().iterator(); it.hasNext();) {
                    final ElementBind b = it.next();
                    if (isReady(b, pending.keySet(), elementVars, later)) {
                        group.addElement(b);
                        group.addElement(new ElementFilter(new E_Bound(new ExprVar(b.getVar()))));
                        bound.add(b.getVar());
                        it.remove();
                        progress = true;
                    }
                }
            }
        }

        private boolean isReady(final ElementBind b, final Set<Var> pending, final Set<Var> elementVars,
                final Set<Var> later) {
            for (Var v : b.getExpr().getVarsMentioned()) {
                if (bound.contains(v)) {
                    continue;
                }
                if (pending.contains(v) || filtered.containsKey(v) || elementVars.contains(v) || later.contains(v)
                        || optional) {
                    return false;
                }
            }
            return true;
        }

        /**
         * The expressions left are constrained at the end of the group.
         */
        private void constrain(final Map<Var, ElementBind> pending) {
            for (ElementBind b : pending.values()) {
                filtered.put(b.getVar(), b.getExpr());
            }
            pending.clear();
        }

        private void place(final Element element) {
            group.addElement(element);
            bound.addAll(PatternVars.vars(element));
        }

        private void placeBlock(final List<TriplePath> triples, final boolean paths) {
            if (triples.isEmpty()) {
                return;
            }
            if (paths) {
                final ElementPathBlock block = new ElementPathBlock();
                triples.forEach(block::addTriplePath);
                group.addElement(block);
            } else {
                final ElementTriplesBlock block = new ElementTriplesBlock();
                triples.forEach((t) -> block.addTriple(t.asTriple()));
                group.addElement(block);
            }
        }

        ElementGroup finish() {
            // the expressions read the nodes of the other expressions
            // constrained here: they are substituted
            final Map<Var, Expr> substituted = new LinkedHashMap<>();
            for (Map.Entry<Var, Expr> e : filtered.entrySet()) {
                Expr expr = e.getValue();
                for (Map.Entry<Var, Expr> s : substituted.entrySet()) {
                    expr = ExprTransformer.transform(new ExprTransformSubstitute(s.getKey(), s.getValue()), expr);
                }
                substituted.put(e.getKey(), expr);
                group.addElement(new ElementFilter(new E_SameTerm(new ExprVar(e.getKey()), expr)));
            }
            return group;
        }

        private static boolean dependsOn(final TriplePath t, final Set<Var> vars) {
            return vars.contains(t.getSubject()) || vars.contains(t.getObject())
                    || (t.isTriple() && vars.contains(t.getPredicate()));
        }

        private static void addVars(final TriplePath t, final Set<Var> vars) {
            for (Node n : t.isTriple() ? new Node[] { t.getSubject(), t.getPredicate(), t.getObject() }
                    : new Node[] { t.getSubject(), t.getObject() }) {
                if (Var.isVar(n)) {
                    vars.add(Var.alloc(n));
                }
            }
        }
    }

}
