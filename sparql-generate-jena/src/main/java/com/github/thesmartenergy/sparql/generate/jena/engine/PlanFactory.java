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
package com.github.thesmartenergy.sparql.generate.jena.engine;

import com.github.thesmartenergy.sparql.generate.jena.engine.impl.GenerateTriplesPlanImpl;
import com.github.thesmartenergy.sparql.generate.jena.engine.impl.GenerateTemplatePlanImpl;
import com.github.thesmartenergy.sparql.generate.jena.engine.impl.SelectPlanImpl;
import com.github.thesmartenergy.sparql.generate.jena.engine.impl.RootPlanImpl;
import com.github.thesmartenergy.sparql.generate.jena.engine.impl.IteratorPlanImpl;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprFunction;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.syntax.Element;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateException;
import com.github.thesmartenergy.sparql.generate.jena.engine.impl.BindPlanImpl;
import com.github.thesmartenergy.sparql.generate.jena.engine.impl.GenerateNamedQueryPlanImpl;
import com.github.thesmartenergy.sparql.generate.jena.engine.impl.SourcePlanImpl;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQueryVisitor;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionRegistry;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementIterator;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementSource;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementSubGenerate;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.engine.binding.Binding;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunction;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionFactory;
import com.github.thesmartenergy.sparql.generate.jena.lang.ParserSPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementGenerateTriplesBlock;
import java.util.Objects;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.syntax.ElementBind;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * A factory that creates a {@link RootPlan} from a SPARQL-Generate query. Then
 * the {@code RootPlan} may be used to execute the SPARQL-Generate query.
 * <p>
 * A {@link SPARQLGenerateQuery} may be created from a string as follows:
 * <pre>{@code
 * String query;
 * SPARQLGenerateQuery query;
 *
 * Syntax syntax = SPARQLGenerate.SYNTAX;
 * query = (SPARQLGenerateQuery) QueryFactory.create(query, syntax);
 * }</pre>
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class PlanFactory {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PlanFactory.class);

    /**
     * The registry of {@link IteratorFunction}s.
     */
    private static final IteratorFunctionRegistry sr = IteratorFunctionRegistry.get();

    /**
     * A factory that creates a {@link RootPlan} from a SPARQL-Generate query.
     * <p>
     * A {@link SPARQLGenerateQuery} may be created from a string as follows:
     * <pre>{@code
     * String query;
     * SPARQLGenerateQuery query;
     *
     * Syntax syntax = SPARQLGenerate.syntaxSPARQLGenerate;
     * query = (SPARQLGenerateQuery) QueryFactory.create(query, syntax);
     * }</pre>
     *
     * @param query the SPARQL-Generate query.
     * @return the RootPlan that may be used to execute the SPARQL-Generate
     * query.
     */
    public static final RootPlan create(final SPARQLGenerateQuery query) {
        Objects.requireNonNull(query, "Query must not be null");
        return make(query);
    }

    /**
     * A factory that creates a {@link RootPlan} from a SPARQL-Generate query.
     *
     * @param queryStr the string representation of the SPARQL-Generate query.
     * @return the RootPlan that may be used to execute the SPARQL-Generate
     * query.
     */
    public static final RootPlan create(final String queryStr) {
        Objects.requireNonNull(queryStr, "Parameter string must not be null");
        SPARQLGenerateQuery query;
        try {
            query = (SPARQLGenerateQuery) QueryFactory.create(queryStr,
                    SPARQLGenerate.SYNTAX);
        } catch (QueryParseException ex) {
            throw new SPARQLGenerateException(
                    "Error while parsing the query \n" + queryStr, ex);
        }
        LOG.trace("Creating plan for query: \n" + query);
        return create(query);
    }

    /**
     * Makes a {@code RootPlan} for a SPARQL-Generate query.
     *
     * @param query the SPARQL-Generate Query.
     * @return the RootPlan.
     */
    private static RootPlan make(final SPARQLGenerateQuery query) {
        return make(query, true);
    }

    /**
     * Makes a {@code RootPlan} for a SPARQL-Generate query.
     *
     * @param query the SPARQL-Generate Query.
     * @return the RootPlan.
     */
    private static RootPlan make(final SPARQLGenerateQuery query,
                                 final boolean initial) {
        Objects.requireNonNull(query, "The query must not be null");
        LOG.trace("Making plan for query\n" + query);

        if (query.hasEmbeddedExpressions()) {
            LOG.debug("Query has embedded expressions. Will be normalized");
            String qs = query.toString();
            SPARQLGenerateQuery query2;
            if (!query.isNamedSubQuery()) {
                query2 = (SPARQLGenerateQuery) QueryFactory.create(qs, SPARQLGenerate.SYNTAX) ;
            } else {
                query2 = (SPARQLGenerateQuery) ParserSPARQLGenerate.parseSubQuery(query,qs);
            }
            query2.normalize();
            LOG.trace("normalized query:\n" + query2.toString());
            return make(query2, initial);
        }

        List<IteratorOrSourcePlan> iteratorAndSourcePlans = new ArrayList<>();
        SelectPlan selectPlan = null;
        GeneratePlan generatePlan = null;

        if (query.hasIteratorsAndSources()) {
            for (Element el : query.getIteratorsAndSources()) {
                IteratorOrSourcePlan iteratorOrSourcePlan;
                if (el instanceof ElementIterator) {
                    ElementIterator elementIterator = (ElementIterator) el;
                    iteratorOrSourcePlan = makeIteratorPlan(elementIterator);
                } else if (el instanceof ElementSource) {
                    ElementSource elementSource = (ElementSource) el;
                    iteratorOrSourcePlan = makeSourcePlan(elementSource);
                } else if (el instanceof ElementBind) {
                    ElementBind elementBind = (ElementBind) el;
                    iteratorOrSourcePlan = makeBindPlan(elementBind);
                } else {
                    throw new UnsupportedOperationException("should not reach"
                            + " this point");
                }
                iteratorAndSourcePlans.add(iteratorOrSourcePlan);
            }
        }
        if (query.getQueryPattern() != null) {
            selectPlan = makeSelectPlan(query);
        }
        if (query.isNamedSubQuery()) {
            generatePlan = makeGenerateQueryPlan(query);
        } else if (query.hasGenerateTemplate()) {
            generatePlan = makeGenerateTemplatePlan(query);
        } else {
            LOG.warn("Query with no generate part.", query);
        }
        return new RootPlanImpl(
                query, iteratorAndSourcePlans,
                selectPlan, generatePlan, initial);
    }

    /**
     * Makes the plan for a SPARQL SELECTOR clause.
     *
     * @param elementIterator the SPARQL SELECTOR
     * @return -
     */
    static IteratorPlan makeIteratorPlan(
            final ElementIterator elementIterator)
            throws SPARQLGenerateException {
        Objects.requireNonNull(elementIterator, "The Iterator must not be null");

        List<Var> vars = elementIterator.getVars();
        Expr expr = elementIterator.getExpr();

        Objects.requireNonNull(vars, "The variables of the Iterator must not be null");
        Objects.requireNonNull(expr, "The Expr in the iterator must not be null");
        checkIsTrue(expr.isFunction(), "Iterator should be a function:"
                + " <iri>(...) AS ?var");

        ExprFunction function = expr.getFunction();
        String iri = function.getFunctionIRI();

        IteratorFunctionFactory factory = sr.get(iri);
        if (factory == null) {
            throw new SPARQLGenerateException("Unknown Iterator Function: " + iri);
        }
        IteratorFunction iterator = factory.create(iri);
        ExprList exprList = new ExprList(function.getArgs());
        iterator.build(exprList);
        return new IteratorPlanImpl(iterator, exprList, vars);
    }

    /**
     * Makes the plan for a SPARQL SOURCE clause. If (1) accept is not set and
     * (2) the {@code StreamManager} finds the file locally, then the behaviour
     * is not specified (yet).
     *
     * @param elementSource the SPARQL SOURCE
     * @return -
     */
    private static SourcePlan makeSourcePlan(
            final ElementSource elementSource) throws SPARQLGenerateException {
        Objects.requireNonNull(elementSource, "The Source must not be null");

        Node node = elementSource.getSource();
        Node accept = elementSource.getAccept();
        Var var = elementSource.getVar();

        Objects.requireNonNull(node, "The source must not be null");
        checkIsTrue(node.isURI() || node.isVariable(), "The source must be a"
                + " URI or a variable. Got " + node);
        // accept may be null
        checkIsTrue(accept == null || accept.isVariable() || accept.isURI(),
                "The accept must be null, a variable or a URI. Got" + accept);
        Objects.requireNonNull(var, "The variable must not be null.");

        return new SourcePlanImpl(node, accept, var);
    }

    /**
     * Makes the plan for a SPARQL BIND clause.
     *
     * @param elementBind the SPARQL BIND
     * @return -
     */
    private static SourcePlan makeBindPlan(
            final ElementBind elementBind) throws SPARQLGenerateException {
        Objects.requireNonNull(elementBind, "The Bind element must not be null");

        Var var = elementBind.getVar();
        Expr expr = elementBind.getExpr();

        Objects.requireNonNull(var, "The source must not be null");
        Objects.requireNonNull(expr, "The expression must not be null.");

        return new BindPlanImpl(expr, var);
    }

    /**
     * Makes the plan for the SPARQL SELECT part of the query.
     *
     * @param query the query for which the plan is created.
     * @return -
     */
    private static SelectPlan makeSelectPlan(
            final SPARQLGenerateQuery query) {
        Objects.requireNonNull(query.getQueryPattern(), "The query must not be null");
        Query select = asSelectQuery(query);
        return new SelectPlanImpl(select);
    }

    /**
     * Makes the plan for a SPARQL {@code GENERATE ?source} clause.
     *
     * @param query the query for which the plan is created.
     * @return -
     */
    private static GenerateNamedQueryPlanImpl makeGenerateQueryPlan(
            final SPARQLGenerateQuery query) throws SPARQLGenerateException {
        Objects.requireNonNull(query, "The query must not be null");
        checkIsTrue(query.isNamedSubQuery(), "Query was expected to be a named "
                + "sub query");
        return new GenerateNamedQueryPlanImpl(query.getQueryName(), query.getCallParameters());
    }

    /**
     * Makes the plan for a SPARQL {@code GENERATE {}} clause.
     *
     * @param query the query for which the plan is created.
     * @return -
     */
    private static GeneratePlan makeGenerateTemplatePlan(
            final SPARQLGenerateQuery query) throws SPARQLGenerateException {
        Objects.requireNonNull(query, "The query must not be null");
        checkIsTrue(query.hasGenerateTemplate(), "Query was expected to be of"
                + " type GENERATE {...} ...");
        List<GenerateTemplateElementPlan> subPlans = new ArrayList<>();
        for (Element elem : query.getGenerateTemplate().getElements()) {
            GenerateTemplateElementPlan plan;
            if (elem instanceof ElementGenerateTriplesBlock) {
                ElementGenerateTriplesBlock sub
                        = (ElementGenerateTriplesBlock) elem;
                plan = new GenerateTriplesPlanImpl(sub.getPattern());
            } else if (elem instanceof ElementSubGenerate) {
                ElementSubGenerate sub = (ElementSubGenerate) elem;
                plan = make(sub.getQuery(), false);
            } else {
                throw new SPARQLGenerateException("should not reach this"
                        + " point");
            }
            subPlans.add(plan);
        }
        return new GenerateTemplatePlanImpl(subPlans);
    }

    /**
     * Create a SPARQL SELECT Query from a SPARQL-Generate Query. Hence one may
     * rely on the existing SPARQL engine to do most of the job.
     *
     * @param query the SPARQL-Generate query
     * @return the SPARQL SELECT Query.
     */
    private static Query asSelectQuery(final SPARQLGenerateQuery query) {
        Objects.requireNonNull(query, "The query must not be null");

        final Query output = new Query();
        query.visit(new SPARQLGenerateQueryVisitor() {
            @Override
            public void startVisit(final Query query) {
                output.setQuerySelectType();
                output.setQueryResultStar(true);
            }

            @Override
            public void visitResultForm(final Query query) {
            }

            @Override
            public void visitPrologue(final Prologue prologue) {
                output.setPrefixMapping(query.getPrefixMapping());
                output.setBaseURI(query.getBaseURI());
            }

            @Override
            public void visitSelectResultForm(final Query query) {
                throw new SPARQLGenerateException("should not reach this"
                        + " point");
            }

            @Override
            public void visitConstructResultForm(final Query query) {
                throw new SPARQLGenerateException("should not reach this"
                        + " point");
            }

            @Override
            public void visitDescribeResultForm(final Query query) {
                throw new SPARQLGenerateException("should not reach this"
                        + " point");
            }

            @Override
            public void visitAskResultForm(final Query query) {
                throw new SPARQLGenerateException("should not reach this"
                        + " point");
            }

            @Override
            public void visitGenerateResultForm(SPARQLGenerateQuery query) {
            }

            @Override
            public void visitDatasetDecl(final Query query) {
                if (query.getGraphURIs() != null
                        && !query.getGraphURIs().isEmpty()) {
                    for (String uri : query.getGraphURIs()) {
                        output.addGraphURI(uri);
                    }
                }
                if (query.getNamedGraphURIs() != null
                        && !query.getNamedGraphURIs().isEmpty()) {
                    for (String uri : query.getNamedGraphURIs()) {
                        output.addNamedGraphURI(uri);
                    }
                }
            }

            @Override
            public void visitQueryPattern(final Query query) {
                if (query.getQueryPattern() != null) {
                    Element el = query.getQueryPattern();
                    output.setQueryPattern(el);
                }
            }

            @Override
            public void visitIteratorsAndSources(SPARQLGenerateQuery query) {
            }

            @Override
            public void visitGroupBy(final Query query) {
                if (query.hasGroupBy()) {
                    if (!query.getGroupBy().isEmpty()) {
                        VarExprList namedExprs = query.getGroupBy();
                        for (Var var : namedExprs.getVars()) {
                            Expr expr = namedExprs.getExpr(var);
                            if (expr != null) {
                                output.addGroupBy(var, expr);
                            } else {
                                output.addGroupBy(var.getVarName());
                            }
                        }
                    }
                }
            }

            @Override
            public void visitHaving(final Query query) {
                if (query.hasHaving()) {
                    for (Expr expr : query.getHavingExprs()) {
                        output.addHavingCondition(expr);
                    }
                }
            }

            @Override
            public void visitOrderBy(final Query query) {
                if (query.hasOrderBy()) {
                    for (SortCondition sc : query.getOrderBy()) {
                        output.addOrderBy(sc);
                    }
                }
            }

            @Override
            public void visitOffset(final Query query) {
                if (query.hasOffset()) {
                    output.setOffset(query.getOffset());
                }
            }

            @Override
            public void visitLimit(final Query query) {
                if (query.hasLimit()) {
                    output.setLimit(query.getLimit());
                }
            }

            @Override
            public void visitValues(final Query query) {
                if (query.hasValues()) {
                    List<Var> variables = query.getValuesVariables();
                    List<Binding> values = query.getValuesData();
                    output.setValuesDataBlock(variables, values);
                }
            }

            @Override
            public void finishVisit(final Query query) {
            }

        });
        return output;
    }

    /**
     * Checks that the test is true.
     *
     * @param test boolean to check.
     * @param msg message used to throw the exception if the boolean is false.
     * @throws IllegalArgumentException Thrown if the boolean is false.
     */
    private static void checkIsTrue(final boolean test, final String msg) {
        if (!test) {
            throw new IllegalArgumentException(msg);
        }
    }

}