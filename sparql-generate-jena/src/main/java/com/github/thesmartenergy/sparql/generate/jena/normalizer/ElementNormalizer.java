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
package com.github.thesmartenergy.sparql.generate.jena.normalizer;

import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.PathBlock;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementAssign;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementDataset;
import org.apache.jena.sparql.syntax.ElementExists;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementMinus;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementNotExists;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementService;
import org.apache.jena.sparql.syntax.ElementSubQuery;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.sparql.syntax.ElementVisitor;

/**
 * Class used to visit an element and normalize it (i.e., replace all expression
 * nodes with the associated expression)
 * 
 * @author maxime.lefrancois
 */
public class ElementNormalizer implements ElementVisitor {

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

    @Override
    public void visit(ElementGroup el) {
        final ElementGroup res = new ElementGroup();
        for (final Element element : el.getElements()) {
            element.visit(this);
            res.addElement(result);
        }
        result = res;
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
            nzed.addTriple(new Triple(s, p, o));
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
                nzed.addTriple(new Triple(s, p, o));
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
    public void visit(ElementData el) {
        final NodeExprNormalizer nenzer = new NodeExprNormalizer();
        final ElementData nzed = new ElementData();
        final List<Var> vars = el.getVars();
        vars.forEach((v) -> {
            nzed.add(v);
        });
        el.getRows().forEach((b) -> {
            final BindingHashMap binding = new BindingHashMap();
            vars.forEach((v) -> {
                final Node n = b.get(v);
                if (n != null) {
                    n.visitWith(nenzer);
                    binding.add(v, nenzer.getResult());
                }
                nzed.add(binding);
            });
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
        el.getOptionalElement().visit(this);
        result = new ElementOptional(result);
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
        SPARQLGenerateQuery query = (SPARQLGenerateQuery) el.getQuery();
        result = new ElementSubQuery(query.normalize());
    }

    private void endVisit(Element nzed, NodeExprNormalizer nenzer) {
        if (!nenzer.hasBindings()) {
            result = nzed;
        } else {
            ElementGroup group = nenzer.getBindingsAsGroup();
            group.addElement(nzed);
            result = group;
        }
    }

}
