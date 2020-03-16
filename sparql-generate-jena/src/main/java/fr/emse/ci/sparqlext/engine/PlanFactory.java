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
package fr.emse.ci.sparqlext.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.jena.graph.Node;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_Function;
import org.apache.jena.sparql.expr.E_Str;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprFunction;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.aggregate.AggGroupConcat;
import org.apache.jena.sparql.expr.aggregate.AggGroupConcatDistinct;
import org.apache.jena.sparql.expr.aggregate.Aggregator;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.SPARQLExtException;
import fr.emse.ci.sparqlext.lang.ParserSPARQLExt;
import fr.emse.ci.sparqlext.normalizer.xexpr.TemplateUtils;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.syntax.ElementBox;
import fr.emse.ci.sparqlext.syntax.ElementExpr;
import fr.emse.ci.sparqlext.syntax.ElementFormat;
import fr.emse.ci.sparqlext.syntax.ElementGenerateTriplesBlock;
import fr.emse.ci.sparqlext.syntax.ElementIterator;
import fr.emse.ci.sparqlext.syntax.ElementSource;
import fr.emse.ci.sparqlext.syntax.ElementSubExtQuery;
import fr.emse.ci.sparqlext.syntax.ElementTGroup;
import fr.emse.ci.sparqlext.syntax.SPARQLExtElementVisitorBase;
import fr.emse.ci.sparqlext.utils.ST;

/**
 * A factory that creates a {@link RootPlan} from a query. Then the
 * {@code RootPlan} may be used to execute the query.
 * <p>
 * A {@link SPARQLExtQuery} may be created from a string as follows:
 * <pre>{@code
 * String query;
 * SPARQLExtQuery query;
 *
 * Syntax syntax = SPARQLExt.SYNTAX;
 * query = (SPARQLExtQuery) QueryFactory.create(query, syntax);
 * }</pre>
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class PlanFactory {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PlanFactory.class);

    private PlanFactory() {

    }

    /**
     * A factory that creates a {@link RootPlan} from a query.
     * <p>
     * A {@link SPARQLExtQuery} may be created from a string as follows:
     * <pre>{@code
     * String query;
     * SPARQLExtQuery query;
     *
     * Syntax syntax = SPARQLExt.syntaxSPARQLGenerate;
     * query = (SPARQLExtQuery) QueryFactory.create(query, syntax);
     * }</pre>
     *
     * @param query the Query.
     * @return the RootPlan that may be used to execute the query.
     */
    public static final RootPlan create(final SPARQLExtQuery query) {
        Objects.requireNonNull(query, "Query must not be null");
        return make(query);
    }

    /**
     * A factory that creates a {@link RootPlan} from a SPARQL-Generate or
     * SPARQL-Template query.
     *
     * @param queryStr the string representation of the SPARQL-Generate or
     * SPARQL-Template Query.
     * @return the RootPlan that may be used to execute the SPARQL-Generate or
     * SPARQL-Template Query. query.
     */
    public static final RootPlan create(final String queryStr) {
        return create(queryStr, null);
    }

    /**
     * A factory that creates a {@link RootPlan} from a query.
     *
     * @param queryStr the string representation of the SPARQL-Generate or
     * SPARQL-Template Query.
     * @param base the base URI, if not set explicitly in the query string
     * @return the RootPlan that may be used to execute the query.
     */
    public static final RootPlan create(final String queryStr, String base) {
        Objects.requireNonNull(queryStr, "Parameter string must not be null");
        SPARQLExtQuery query;
        try {
            query = (SPARQLExtQuery) QueryFactory.create(queryStr, base,
                    SPARQLExt.SYNTAX);
            if(!query.explicitlySetBaseURI()) {
            	query.setBaseURI(base);
            }
        } catch (QueryParseException ex) {
            throw new SPARQLExtException(
                    "Error while parsing the query \n" + queryStr, ex);
        }
        LOG.trace("Creating plan for query: \n" + query);
        return create(query);
    }

    /**
     * Makes a {@code RootPlan} for a query.
     *
     * @param query the Query.
     * @return the RootPlan.
     */
    private static RootPlan make(final SPARQLExtQuery query) {
        Objects.requireNonNull(query, "The query must not be null");
        if (query.hasEmbeddedExpressions()) {
            LOG.debug("Query has embedded expressions:\n" + query);
            String qs = query.toString();
            SPARQLExtQuery query2;
            if (query.isSubQuery()) {
                query2 = (SPARQLExtQuery) ParserSPARQLExt.parseSubQuery(query, qs);
                query2.normalizeXExpr();
                query2.normalizeAggregates();
            } else {
                query2 = (SPARQLExtQuery) QueryFactory.create(qs, SPARQLExt.SYNTAX);
                query2.normalizeXExpr();
                query2.normalizeBNode();
                query2.normalizeAggregates();
            }
            return make(query2);
        }
        
        LOG.trace("Making plan for query without embedded expressions\n" + query);
        DatasetDeclarationPlan datasetDeclarationPlan = new DatasetDeclarationPlan(query);

        List<BindingsClausePlan> iteratorAndSourcePlans = new ArrayList<>();
        if (query.hasBindingClauses()) {
            for (Element el : query.getBindingClauses()) {
                BindingsClausePlan iteratorOrSourcePlan;
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

        /*
        * TEMPLATE queries are translated into a SELECT with project 
        * variable ?out.
         */
        final SelectPlan selectPlan = makeSelectPlan(query);

        if (query.isTemplateType()) {
	        final TemplatePlan templatePlan = makeTemplatePlan(query);
            return new RootPlan(
                    query, datasetDeclarationPlan, iteratorAndSourcePlans,
                    selectPlan, templatePlan);
        }
        if (query.isGenerateType()) {
	        final GeneratePlan generatePlan;
	        if (query.hasGenerateClause()) {
	            generatePlan = makeGenerateFormPlan(query);
	        } else {
	            generatePlan = makeGenerateNamedPlan(query);
	        }
	        return new RootPlan(
	                query, datasetDeclarationPlan, iteratorAndSourcePlans,
	                selectPlan, generatePlan);
        }
        return new RootPlan(query, datasetDeclarationPlan, iteratorAndSourcePlans, selectPlan);
    }

    /**
     * Makes the plan for a SPARQL ITERATOR clause.
     *
     * @param elementIterator the SPARQL ITERATOR
     * @return -
     */
    static IteratorPlan makeIteratorPlan(
            final ElementIterator elementIterator)
            throws SPARQLExtException {
        Objects.requireNonNull(elementIterator, "The Iterator must not be null");

        List<Var> vars = elementIterator.getVars();
        Expr expr = elementIterator.getExpr();

        Objects.requireNonNull(vars, "The variables of the Iterator must not be null");
        Objects.requireNonNull(expr, "The Expr in the iterator must not be null");
        checkIsTrue(expr.isFunction(), "Iterator should be a function:"
                + " <iri>(...) AS ?var1 ?var2 ...");

        ExprFunction function = expr.getFunction();
        String iri = function.getFunctionIRI();
        ExprList exprList = new ExprList(function.getArgs());
        return new IteratorPlan(iri, exprList, vars);
    }

    /**
     * Makes the plan for a SPARQL SOURCE clause.
     *
     * @param elementSource the SPARQL SOURCE
     * @return -
     */
    private static BindOrSourcePlan makeSourcePlan(
            final ElementSource elementSource) throws SPARQLExtException {
        Objects.requireNonNull(elementSource, "The Source must not be null");

        Node node = elementSource.getSource();
        Node accept = elementSource.getAccept();
        Var var = elementSource.getVar();

        Objects.requireNonNull(node, "The source must not be null");
        checkIsTrue(node.isURI() || node.isVariable(), "The source must be a"
                + " URI or a variable. Got " + node);
        // accept may be null
        checkIsTrue(accept == null || accept.isVariable() || accept.isURI(),
                "The accept must be null, a variable or a URI. Got " + accept);
        Objects.requireNonNull(var, "The variable must not be null.");

        return new SourcePlan(node, accept, var);
    }

    /**
     * Makes the plan for a SPARQL BIND clause.
     *
     * @param elementBind the SPARQL BIND
     * @return -
     */
    private static BindOrSourcePlan makeBindPlan(
            final ElementBind elementBind) throws SPARQLExtException {
        Objects.requireNonNull(elementBind, "The Bind element must not be null");

        Var var = elementBind.getVar();
        Expr expr = elementBind.getExpr();

        Objects.requireNonNull(var, "The source must not be null");
        Objects.requireNonNull(expr, "The expression must not be null.");

        return new BindPlan(expr, var);
    }

    /**
     * Makes the plan for a SPARQL {@code GENERATE ?source() } clause.
     *
     * @param query the query for which the plan is created.
     * @return -
     */
    private static GenerateNamedPlan makeGenerateNamedPlan(
            final SPARQLExtQuery query) throws SPARQLExtException {
        Objects.requireNonNull(query, "The query must not be null");
        checkIsTrue(query.isSubQuery() && query.getName() != null, "Query was expected to be a named "
                + "sub query");
        return new GenerateNamedPlan(query.getName(), query.getCallParameters());
    }

    /**
     * Makes the plan for a SPARQL {@code GENERATE {}} clause.
     *
     * @param query the query for which the plan is created.
     * @return -
     */
    private static GenerateFormPlan makeGenerateFormPlan(
            final SPARQLExtQuery query) throws SPARQLExtException {
        Objects.requireNonNull(query, "The query must not be null");
        checkIsTrue(query.hasGenerateClause(), "Query was expected to be of"
                + " type GENERATE {...} ...");
        final BasicPattern bgp = new BasicPattern();
        final List<RootPlan> subQueriesPlans = new ArrayList<>();
        for (Element elem : query.getGenerateClause()) {
            if (elem instanceof ElementGenerateTriplesBlock) {
                ElementGenerateTriplesBlock sub
                        = (ElementGenerateTriplesBlock) elem;
                bgp.addAll(sub.getPattern());
            } else if (elem instanceof ElementSubExtQuery) {
                ElementSubExtQuery sub = (ElementSubExtQuery) elem;
                RootPlan rootPlan = make(sub.getQuery());
                subQueriesPlans.add(rootPlan);
            } else {
                throw new SPARQLExtException("should not reach this point");
            }
        }
        return new GenerateFormPlan(bgp, subQueriesPlans);
    }

    /**
     * Create a SPARQL SELECT Query from a SPARQL-Ext Query. Hence one may rely
     * on the existing SPARQL engine to do most of the job.
     *
     * @param query the SPARQL-Generate query
     * @return the SPARQL SELECT Query.
     */
    private static SelectPlan makeSelectPlan(final SPARQLExtQuery query) {
        Objects.requireNonNull(query, "The query must not be null");
        SelectExtractionVisitor selectExtractionVisitor = new SelectExtractionVisitor(query);
        query.visit(selectExtractionVisitor);
        SPARQLExtQuery newQuery = selectExtractionVisitor.getOutput();
        if(newQuery == null) {
            LOG.trace(String.format("No SelectPlan for dummy SELECT queries"));
            return null;
        }
        if (newQuery.hasAggregators()) {
            if (query.hasSignature()) {
                query.getSignature().forEach(newQuery::addGroupBy);
            }
        }
        LOG.trace(String.format("Generated SELECT query\n%s", newQuery.toString()));
        return new SelectPlan(newQuery, query.isSelectType(), query.getSignature());
    }
    
    private static TemplatePlan makeTemplatePlan(final SPARQLExtQuery query) {
        Objects.requireNonNull(query, "The query must not be null");
		Expr before = query.getTemplateClauseBefore();
		Expr separator = query.getTemplateClauseSeparator();
		Expr after = query.getTemplateClauseAfter();
        final TProc tproc = new TProc();
        List<Element> templateElements = query.getTemplateClause();
        if (templateElements.size() == 1) {
            templateElements.get(0).visit(tproc);
            LOG.trace(String.format("Generated Template plan for \n%s", tproc.result));
            return new TemplatePlan(before, tproc.result, separator, after);
        } else {
            List<Expr> texprs = new ArrayList<>();
            for (int i = 0; i < templateElements.size(); i++) {
                templateElements.get(i).visit(tproc);
                Expr result = tproc.result;
                if (result instanceof E_Function && ((E_Function) result).getFunctionIRI().equals(ST.concat)) {
                    texprs.addAll(((E_Function) result).getArgs());
                } else {
                    texprs.add(result);
                }
            }
            Expr concat = new E_Function(ST.concat, new ExprList(texprs));
            LOG.trace(String.format("Generated Template plan for \n%s", concat));
            return new TemplatePlan(before, concat, separator, after);
        }
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
    
    private static class TProc extends SPARQLExtElementVisitorBase {

        Expr result = null;
        

        @Override
        public void visit(ElementExpr el) {
            if (el.getExpr() instanceof NodeValueString) {
                result = el.getExpr();
            } else {
                result = new E_Str(el.getExpr());
            }
        }

        @Override
        public void visit(ElementBox el) {
            final List<Expr> texprs = new ArrayList<>();
            texprs.add(new E_Function(ST.incr, new ExprList()));
            List<Element> elements = el.getTExpressions();
            for (int i = 0; i < elements.size(); i++) {
                elements.get(i).visit(this);
                if (result instanceof E_Function && ((E_Function) result).getFunctionIRI().equals(ST.concat)) {
                    texprs.addAll(((E_Function) result).getArgs());
                } else {
                    texprs.add(result);
                }
            }
            texprs.add(new E_Function(ST.decr, new ExprList()));
            result = new E_Function(ST.concat, new ExprList(texprs));
        }

        @Override
        public void visit(ElementFormat el) {
            final List<Expr> texprs = new ArrayList<>();
            texprs.add(el.getExpr().getExpr());
            el.getTExpressions().forEach((e) -> {
                e.visit(this);
                texprs.add(result);
            });
            result = new E_Function(ST.format, new ExprList(texprs));
        }

        @Override
        public void visit(ElementTGroup el) {
            String sep = el.getSeparator();
            final List<Expr> texprs = new ArrayList<>();
            el.getTExpressions().forEach((e) -> {
                e.visit(this);
                texprs.add(result);
            });
            final Expr concat = new E_Function(ST.concat, new ExprList(texprs));
            final Aggregator groupConcat;
            if (el.isDistinct()) {
                groupConcat = new AggGroupConcatDistinct(concat, sep);
            } else {
                groupConcat = new AggGroupConcat(concat, sep);
            }
            Var var = Var.alloc(UUID.randomUUID().toString().substring(0, 8));
            result = new ExprAggregator(var, groupConcat);
        }

        @Override
        public void visit(ElementSubExtQuery el) {
            SPARQLExtQuery query = el.getQuery();
            result = TemplateUtils.getFunction(query);
        }

    };

}
