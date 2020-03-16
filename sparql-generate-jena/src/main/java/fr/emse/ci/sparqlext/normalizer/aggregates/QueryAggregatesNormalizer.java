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
package fr.emse.ci.sparqlext.normalizer.aggregates;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.syntax.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.query.SPARQLExtQueryVisitor;
import fr.emse.ci.sparqlext.syntax.FromClause;

/**
 * Class used to normalize SPARQL-Generate queries, i.e. output an equivalent
 * query with no expression nodes.
 *
 * This class is instantiated by calls to the method
 *
 * {@code SPARQLExtQuery.normalizeXExpr()}
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class QueryAggregatesNormalizer implements SPARQLExtQueryVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(QueryAggregatesNormalizer.class);
    
    @Override
    public void startVisit(Query query) {
        asSPARQLExtQuery(query);
    }

    @Override
    public void visitBindingClauses(SPARQLExtQuery query) {
//        if (query.getBindingClauses() == null) {
//            return;
//        }
//        final List<Element> bindingClauses = new ArrayList<>();
//        for (final Element bindingClause : query.getBindingClauses()) {
//            if (bindingClause instanceof ElementBind) {
//                ElementBind el = (ElementBind) bindingClause;
//                final Expr expr = enzer.normalize(el.getExpr());
//                bindingClauses.add(new ElementBind(el.getVar(), expr));
//            } else if (bindingClause instanceof ElementIterator) {
//                ElementIterator el = (ElementIterator) bindingClause;
//                final Expr expr = enzer.normalize(el.getExpr());
//                bindingClauses.add(new ElementIterator(expr, el.getVars()));
//            } else if (bindingClause instanceof ElementSource) {
//                ElementSource el = (ElementSource) bindingClause;
//                bindingClauses.add(bindingClause);
//            } else {
//                throw new NullPointerException("Should not reach this point");
//            }
//        }
//        query.setBindingClauses(bindingClauses);
    }

    @Override
    public void visitGenerateClause(SPARQLExtQuery query) {
    	final ExprNormalizer enzer = new ExprNormalizer(query);
        if(query.isSubQuery()) {
            normalizeSignature(query);
        } else {
            normalizeCallParams(enzer, query);
        }
        if (query.hasGenerateClause()) {
            List<Element> group = normalizeOutput(query.getGenerateClause(), enzer);
            query.setGenerateClause(group);
        }
    }

    @Override
    public void visitTemplateClause(SPARQLExtQuery query) {
    	final ExprNormalizer enzer = new ExprNormalizer(query);
        if(query.isSubQuery()) {
            normalizeSignature(query);
        } else {
            normalizeCallParams(enzer, query);
        }
        if (query.hasTemplateClauseBefore()) {
            Expr expr = enzer.normalize(query.getTemplateClauseBefore());
            query.setTemplateClauseBefore(expr);
        }
        if (query.hasTemplateClause()) {
            List<Element> group = normalizeOutput(query.getTemplateClause(), enzer);
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
    }

    @Override
    public void visitFunctionExpression(SPARQLExtQuery query) {
    	final ExprNormalizer enzer = new ExprNormalizer(query);
        Expr expr = enzer.normalize(query.getFunctionExpression());
        query.setFunctionExpression(expr);
    }

    @Override
    public void visitPerformClause(SPARQLExtQuery query) {
        throw new NullPointerException("not implemented yet");
    }

    @Override
    public void visitQueryPattern(Query query) {
    }

    @Override
    public void visitSelectResultForm(Query query) {
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
    	final ExprNormalizer enzer = new ExprNormalizer(query);
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
    }

    @Override
    public void visitHaving(Query query) {
    }

    @Override
    public void visitOrderBy(Query query) {
    }

    @Override
    public void visitLimit(Query query) {
    }

    @Override
    public void visitOffset(Query query) {
    }

    @Override
    public void visitPostSelect(SPARQLExtQuery query) {
    }

    @Override
    public void visitValues(Query query) {
    }

    @Override
    public void finishVisit(Query query) {
    }

    @Override
    public void visitPragma(SPARQLExtQuery query) {
    }

    private void normalizeSignature(SPARQLExtQuery query) {
    }

    private void normalizeCallParams(ExprNormalizer enzer, SPARQLExtQuery query) {
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

    private List<Element> normalizeOutput(List<Element> elements, ExprNormalizer enzer) {
        final OutputClauseNormalizer nzer = new OutputClauseNormalizer(enzer);
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
