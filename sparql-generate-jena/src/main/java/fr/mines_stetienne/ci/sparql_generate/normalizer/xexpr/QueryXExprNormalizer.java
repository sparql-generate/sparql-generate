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

import fr.mines_stetienne.ci.sparql_generate.SPARQLExtException;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.syntax.ElementIterator;
import fr.mines_stetienne.ci.sparql_generate.syntax.ElementSource;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementBind;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQueryVisitor;
import fr.mines_stetienne.ci.sparql_generate.syntax.FromClause;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.expr.ExprList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to normalize SPARQL-Generate queries, i.e. output an equivalent
 * query with no expression nodes.
 *
 * This class is instantiated by calls to the method
 *
 * {@code SPARQLExtQuery.normalizeXExpr()}
 *
 * @author Maxime Lefrançois
 */
public class QueryXExprNormalizer implements SPARQLExtQueryVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(QueryXExprNormalizer.class);

    private static final ExprNormalizer enzer = new ExprNormalizer();

    @Override
    public void startVisit(Query query) {
        asSPARQLExtQuery(query);
    }

    @Override
    public void visitBindingClauses(SPARQLExtQuery query) {
        if (query.getBindingClauses() == null) {
            return;
        }
        final List<Element> bindingClauses = new ArrayList<>();
        for (final Element bindingClause : query.getBindingClauses()) {
            if (bindingClause instanceof ElementBind) {
                ElementBind el = (ElementBind) bindingClause;
                final Expr expr = enzer.normalize(el.getExpr());
                bindingClauses.add(new ElementBind(el.getVar(), expr));
            } else if (bindingClause instanceof ElementIterator) {
                ElementIterator el = (ElementIterator) bindingClause;
                final Expr expr = enzer.normalize(el.getExpr());
                bindingClauses.add(new ElementIterator(expr, el.getVars()));
            } else if (bindingClause instanceof ElementSource) {
                ElementSource el = (ElementSource) bindingClause;
                final NodeExprNormalizer nenzer = new NodeExprNormalizer(bindingClauses);
                el.getSource().visitWith(nenzer);
                final Node source = nenzer.getResult();
                final Node accept;
                if (el.getAccept() == null) {
                    accept = null;
                } else {
                    el.getAccept().visitWith(nenzer);
                    accept = nenzer.getResult();
                }
                bindingClauses.add(new ElementSource(source, accept, el.getVar()));
            } else {
                throw new NullPointerException("Should not reach this point");
            }
        }
        query.setBindingClauses(bindingClauses);
    }

    @Override
    public void visitGenerateClause(SPARQLExtQuery query) {
        final NodeExprNormalizer nenzer = new NodeExprNormalizer();
        if(query.isSubQuery()) {
            normalizeSignature(query);
        } else {
            normalizeCallParams(query);
        }
        if (query.hasGenerateClause()) {
            List<Element> group = normalizeOutput(query.getGenerateClause(), nenzer);
            query.setGenerateClause(group);
        }
        appendPostSelect(query, nenzer);
    }

    @Override
    public void visitTemplateClause(SPARQLExtQuery query) {
        final NodeExprNormalizer nenzer = new NodeExprNormalizer();
        normalizeSignature(query);
        normalizeCallParams(query);
        if (query.hasTemplateClauseBefore()) {
            Expr expr = enzer.normalize(query.getTemplateClauseBefore());
            query.setTemplateClauseBefore(expr);
        }
        if (query.hasTemplateClause()) {
            List<Element> group = normalizeOutput(query.getTemplateClause(), nenzer);
            query.setTemplateClause(group);
        }
        if (query.hasTemplateClauseSeparator()) {
            Expr expr = enzer.normalize(query.getTemplateClauseSeparator());
            query.setTemplateClauseSeparator(expr);
        }
        if (query.hasTemplateClauseAfter()) {
            Expr expr = enzer.normalize(query.getTemplateClauseAfter());
            query.setTemplateClauseAfter(expr);
        }
        if (nenzer.hasBindings()) {
            throw new SPARQLExtException("Should not expect bindings here.");
        }
    }

    @Override
    public void visitFunctionExpression(SPARQLExtQuery query) {
        Expr expr = enzer.normalize(query.getFunctionExpression());
        query.setFunctionExpression(expr);
    }

    @Override
    public void visitPerformClause(SPARQLExtQuery query) {
        final NodeExprNormalizer nenzer = new NodeExprNormalizer();
        normalizeCallParams(query);
        if (query.hasPerformClause()) {
            List<Element> group = normalizeOutput(query.getPerformClause(), nenzer);
            query.setPerformClause(group);
        }
        appendPostSelect(query, nenzer);
    }

    @Override
    public void visitQueryPattern(Query query) {
        if (query.getQueryPattern() == null) {
            return;
        }
        final QueryPatternNormalizer nzer = new QueryPatternNormalizer();
        query.getQueryPattern().visit(nzer);
        query.setQueryPattern(nzer.getResult());
    }

    @Override
    public void visitSelectResultForm(Query query) {
        final VarExprList project = query.getProject();
        final VarExprList newProject = new VarExprList();
        project.getVars().forEach((var) -> {
            if (project.hasExpr(var)) {
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
        throw new NullPointerException("not implemented yet");
    }

    @Override
    public void visitDescribeResultForm(Query query) {
        throw new NullPointerException("not implemented yet");
    }

    @Override
    public void visitAskResultForm(Query query) {
        throw new NullPointerException("not implemented yet");
    }

    @Override
    public void visitPrologue(Prologue prologue) {
    }

    @Override
    public void visitResultForm(Query query) {
    }

    @Override
    public void visitDatasetDecl(Query q) {
        SPARQLExtQuery query = asSPARQLExtQuery(q);
        query.getFromClauses().replaceAll((fromClause) -> {
            if (fromClause.getGenerate() == null) {
                Expr nzed = enzer.normalize(fromClause.getName());
                return new FromClause(fromClause.isNamed(), nzed);
            } else {
                SPARQLExtQuery gnzed = fromClause.getGenerate();
                gnzed.normalizeXExpr();
                if (!fromClause.isNamed()) {
                    return new FromClause(gnzed);
                }
                Expr nnzed = enzer.normalize(fromClause.getName());
                return new FromClause(gnzed, nnzed);
            }
        });
    }

    @Override
    public void visitGroupBy(Query query) {
        if (query.hasGroupBy() && !query.getGroupBy().isEmpty()) {
            // Can have an empty GROUP BY list if the grouping is implicit
            // by use of an aggregate in the SELECT clause.
            final VarExprList namedExprs = query.getGroupBy();
            final VarExprList newNamedExprs = new VarExprList();
            for (Var var : namedExprs.getVars()) {
                if (namedExprs.hasExpr(var)) {
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
        if (query.hasHaving()) {
            query.getHavingExprs().replaceAll((expr) -> {
                return enzer.normalize(expr);
            });
        }
    }

    @Override
    public void visitOrderBy(Query query) {
        if (query.hasOrderBy()) {
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
    public void visitPostSelect(SPARQLExtQuery query) {
        final VarExprList postSelect = query.getPostSelect();
        final VarExprList newProject = new VarExprList();
        postSelect.getVars().forEach((var) -> {
            if (postSelect.hasExpr(var)) {
                final Expr nzed = enzer.normalize(postSelect.getExpr(var));
                newProject.add(var, nzed);
            } else {
                newProject.add(var);
            }
        });
        postSelect.clear();
        postSelect.addAll(newProject);
    }

    @Override
    public void visitValues(Query query) {
    }

    @Override
    public void finishVisit(Query query) {
        ((SPARQLExtQuery) query).hasEmbeddedExpressions(false);
    }

    private void appendPostSelect(SPARQLExtQuery query, NodeExprNormalizer nenzer) {
        if (!nenzer.hasBindings()) {
            return;
        }
        for (Element element : nenzer.getBindings()) {
            if (element instanceof ElementBind) {
                ElementBind b = (ElementBind) element;
                query.addPostSelect(b.getVar(), b.getExpr());
            }
        }
    }

    @Override
    public void visitPragma(SPARQLExtQuery query) {
    }

    private void normalizeSignature(SPARQLExtQuery query) {
    }

    private void normalizeCallParams(SPARQLExtQuery query) {
        if (query.hasName()) {
            query.setName(enzer.normalize(query.getName()));
        }
        if (query.hasCallParameters()) {
            final List<Expr> parameters = query.getCallParameters().getList();
            final List<Expr> nzed = new ArrayList<>();
            parameters.forEach((p) -> {
                nzed.add(enzer.normalize(p));
            });
            query.setCallParameters(new ExprList(nzed));
        }
    }

    private List<Element> normalizeOutput(List<Element> elements, NodeExprNormalizer nenzer) {
        final OutputClauseNormalizer nzer = new OutputClauseNormalizer(nenzer);
        final List<Element> group = new ArrayList<>();
        for (Element el : elements) {
            el.visit(nzer);
            group.add(nzer.getResult());
        }
        return group;
    }
    
	@Override
	public void visitJsonResultForm(Query query) {
	}

}
