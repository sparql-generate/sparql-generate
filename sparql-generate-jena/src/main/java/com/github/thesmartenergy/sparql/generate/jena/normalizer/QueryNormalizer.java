/*
 * Copyright 2016 Ecole des Mines de Saint-Etienne.
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
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQueryVisitor;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementGenerateTriplesBlock;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementIterator;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementSource;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementSubGenerate;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementGroup;

/**
 * Class used to normalize SPARQL-Generate queries, i.e. output an equivalent 
 * query with no expression nodes.
 * 
 * This class is instantiated by calls to the method 
 * 
 * {@code SPARQLGenerateQuery.normalize()}
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class QueryNormalizer implements SPARQLGenerateQueryVisitor {

    @Override
    public void startVisit(Query query) {
        if (!(query instanceof SPARQLGenerateQuery)) {
            throw new IllegalArgumentException("Expecting a SPARQL-Generate query.");
        }
    }

    @Override
    public void visitIteratorsAndSources(SPARQLGenerateQuery query) {
        if (query.getIteratorsAndSources() == null) {
            return;
        }
        final List<Element> iteratorsAndSources = new ArrayList<>();
        for (final Element iteratorOrSource : query.getIteratorsAndSources()) {
            if (iteratorOrSource instanceof ElementBind) {
                iteratorsAndSources.add(iteratorOrSource);
            } else if (iteratorOrSource instanceof ElementIterator) {
                ElementIterator el = (ElementIterator) iteratorOrSource;
                final ExprNormalizer enzer = new ExprNormalizer();
                final Expr expr = enzer.normalize(el.getExpr());
                iteratorsAndSources.add(new ElementIterator(expr, el.getVar()));
            } else if (iteratorOrSource instanceof ElementSource) {
                ElementSource el = (ElementSource) iteratorOrSource;
                final NodeExprNormalizer nenzer = new NodeExprNormalizer(iteratorsAndSources);
                el.getSource().visitWith(nenzer);
                final Node source = nenzer.getResult();
                final Node accept;
                if(el.getAccept() == null) {
                    accept = null;
                } else {
                    el.getAccept().visitWith(nenzer);
                    accept = nenzer.getResult();
                }
                iteratorsAndSources.add(new ElementSource(source, accept, el.getVar()));
            } else {
                throw new NullPointerException("Should not reach this point");
            }
        }
        query.setIteratorsAndSources(iteratorsAndSources);
    }

    @Override
    public void visitGenerateResultForm(SPARQLGenerateQuery query) {
        if (query.hasGenerateURI() || !query.hasGenerateTemplate()) {
            return;
        }
        final NodeExprNormalizer nenzer = new NodeExprNormalizer();
        final ElementGroup group = new ElementGroup();
        for (final Element element : query.getGenerateTemplate().getElements()) {
            if (element instanceof ElementGenerateTriplesBlock) {
                final ElementGenerateTriplesBlock el = (ElementGenerateTriplesBlock) element;
                final BasicPattern bgp = el.getPattern();

                final ElementGenerateTriplesBlock nzed = new ElementGenerateTriplesBlock();
                bgp.forEach((t) -> {
                    t.getSubject().visitWith(nenzer);
                    Node s = nenzer.getResult();
                    t.getPredicate().visitWith(nenzer);
                    Node p = nenzer.getResult();
                    t.getObject().visitWith(nenzer);
                    Node o = nenzer.getResult();
                    nzed.addTriple(new Triple(s, p, o));
                });
                group.addElement(nzed);
            } else if (element instanceof ElementSubGenerate) {
                ElementSubGenerate el = (ElementSubGenerate) element;
                SPARQLGenerateQuery nzed = el.getQuery().normalize();
                group.addElement(new ElementSubGenerate(nzed));
            } else {
                throw new NullPointerException("Should not reach this point");
            }
        }
        query.setGenerateTemplate(group);
        appendBindings(query, nenzer);
    }

    @Override
    public void visitQueryPattern(Query query) {
        if (query.getQueryPattern() == null) {
            return;
        }
        final ElementNormalizer nzer = new ElementNormalizer();
        query.getQueryPattern().visit(nzer);
        query.setQueryPattern(nzer.getResult());
    }

    @Override
    public void visitSelectResultForm(Query query) {
        if(query.isQueryResultStar()) {
            return;
        }
        final VarExprList project = query.getProject();
        final VarExprList newProject = new VarExprList();
        final ExprNormalizer enzer = new ExprNormalizer();
        project.getVars().forEach((var) -> {
            if(project.hasExpr(var)) {
                final Expr nzed = enzer.normalize(project.getExpr(var));
                newProject.add(var, nzed);
            } else {
                newProject.add(var);
            }
        });
        project.clear();
        project.addAll(newProject);
    }

    @Override
    public void visitConstructResultForm(Query query) {
        throw new NullPointerException("should not reach this point");
    }

    @Override
    public void visitDescribeResultForm(Query query) {
        throw new NullPointerException("should not reach this point");
    }

    @Override
    public void visitAskResultForm(Query query) {
        throw new NullPointerException("should not reach this point");
    }

    @Override
    public void visitPrologue(Prologue prologue) {
    }

    @Override
    public void visitResultForm(Query query) {
    }

    @Override
    public void visitDatasetDecl(Query query) {
    }

    @Override
    public void visitGroupBy(Query query) {
        if (query.hasGroupBy() && !query.getGroupBy().isEmpty()) {
            // Can have an empty GROUP BY list if the grouping is implicit
            // by use of an aggregate in the SELECT clause.
            final VarExprList namedExprs = query.getGroupBy();
            final VarExprList newNamedExprs = new VarExprList();
            final ExprNormalizer enzer = new ExprNormalizer();
            for (Var var : namedExprs.getVars()) {
                if(namedExprs.hasExpr(var)) {
                    final Expr nzed = enzer.normalize(namedExprs.getExpr(var));
                    newNamedExprs.add(var, nzed);
                } else {
                    newNamedExprs.add(var);
                }
            }
            namedExprs.clear();
            namedExprs.addAll(newNamedExprs);
        }
    }

    @Override
    public void visitHaving(Query query) {
        if(query.hasHaving()) {
            final ExprNormalizer enzer = new ExprNormalizer();
            query.getHavingExprs().replaceAll((expr) -> {
                return enzer.normalize(expr);
            });
        }
    }

    @Override
    public void visitOrderBy(Query query) {
        if(query.hasOrderBy()) {
            final ExprNormalizer enzer = new ExprNormalizer();
            query.getOrderBy().replaceAll((sc) -> {
                final Expr expr = enzer.normalize(sc.getExpression());
                return new SortCondition(expr, sc.getDirection());
            });
        }
    }

    @Override
    public void visitLimit(Query query) {
    }

    @Override
    public void visitOffset(Query query) {
    }

    @Override
    public void visitValues(Query query) {
    }

    @Override
    public void finishVisit(Query query) {
        ((SPARQLGenerateQuery) query).hasEmbeddedExpressions(false);
    }

    private void appendBindings(Query query, NodeExprNormalizer nenzer) {
        if(!nenzer.hasBindings()) {
            return;
        }
        Element el = query.getQueryPattern();
        final ElementGroup group;
        if (el == null) {
            group = new ElementGroup();
        } else if (el instanceof ElementGroup) {
            group = (ElementGroup) el;
        } else {
            group = new ElementGroup();
            group.addElement(el);
        }
        for(Element binding : nenzer.getBindings()) {
            group.addElement(binding);
        }
        query.setQueryPattern(group);
    }

}
