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
import com.github.thesmartenergy.sparql.generate.jena.engine.impl.SourcePlanImpl;
import com.github.thesmartenergy.sparql.generate.jena.engine.impl.IteratorPlanImpl;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprFunction;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.util.FileManager;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateException;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQueryVisitor;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionRegistry;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementGenerateTriplesBlock;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementIterator;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementIteratorOrSource;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementSource;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementSubGenerate;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.log4j.Logger;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunction;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionFactory;

/**
 * A factory that creates a {@link RootPlan} from a SPARQL Generate query.
 * Then the {@code RootPlan} may be used to execute the SPARQL Generate
 * query.
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
 * @author maxime.lefrancois
 */
public class PlanFactory {

    /**
     * The logger.
     */
    static final Logger LOG = Logger.getLogger(PlanFactory.class);

    /**
     * The registry of {@link IteratorFunction}s.
     */
    private final IteratorFunctionRegistry sr;

    /**
     * Enables to use local files instead of attempling an HTTP call.
     */
    private final FileManager fm;

    /**
     * A factory that creates a {@link RootPlan} from a SPARQL Generate
     * query. Use the default {@link IteratorFunctionRegistry}, and the default
     * {@link FileManager}.
     */
    public PlanFactory() {
        this(IteratorFunctionRegistry.get(), FileManager.get());
    }

    /**
     * A factory that creates a {@link RootPlan} from a SPARQL Generate
     * query. Use the default {@link FileManager}.
     * <p>
     * See static methods of class {@link IteratorFunctionRegistry#get} to use an
 existing IteratorFunction registry, or instantiate your own.
     *
     * @param iteratorRegistry the iterator registry to use.
     */
    public PlanFactory(final IteratorFunctionRegistry iteratorRegistry) {
        this(iteratorRegistry, FileManager.get());
    }

    /**
     * A factory that creates a {@link RootPlan} from a SPARQL Generate
     * query.
     * <p>
     * The {@link FileManager} uses a configuration file to load files from disk
     * instead of attempting HTTP GET calls. See
     * <a href="https://jena.apache.org/documentation/notes/file-manager.html">
     * The Jena FileManager and LocationMapper</a>. The following code
     * illustrates the instantiation of a {@code FileManager}:
     * <pre>{@code
     *
     * Model conf = RDFDataMgr.loadModel("file:/path/to/configuration.ttl");
     * LocationMapper mapper = new LocationMapper(conf);
     * FileManager fm = new FileManager();
     *
     * fm.setLocationMapper(mapper);
     *
     * Locator loc = new LocatorFile("file:/path/to/directory1");
     * Locator loc = new LocatorFile("file:/path/to/directory2");
     * fileManager.addLocator(loc);
     * }</pre>
     *
     * <p>
     * See static methods of class {@link IteratorFunctionRegistry#get} to use an
 existing IteratorFunction registry, or instantiate your own.
     *
     * @param iteratorRegistry the iterator registry to use.
     * @param fileManager the file manager to use.
     */
    public PlanFactory(
            final IteratorFunctionRegistry iteratorRegistry,
            final FileManager fileManager) {
        this.sr = iteratorRegistry;
        this.fm = fileManager;
    }

    /**
     * A factory that creates a {@link RootPlan} from a SPARQL Generate
     * query.
     * <p>
     * The {@link FileManager} uses a configuration file to load files from disk
     * instead of attempting HTTP GET calls. See
     * <a href="https://jena.apache.org/documentation/notes/file-manager.html">
     * The Jena FileManager and LocationMapper</a>. The following code
     * illustrates the instantiation of a {@code FileManager}:
     * <pre>{@code
     *
     * Model conf = RDFDataMgr.loadModel("file:/path/to/configuration.ttl");
     * LocationMapper mapper = new LocationMapper(conf);
     * FileManager fm = new FileManager();
     *
     * fm.setLocationMapper(mapper);
     *
     * Locator loc = new LocatorFile("file:/path/to/directory1");
     * Locator loc = new LocatorFile("file:/path/to/directory2");
     * fileManager.addLocator(loc);
     * }</pre>
     *
     *
     * @param fileManager the file manager to use.
     */
    public PlanFactory(final FileManager fileManager) {
        this(IteratorFunctionRegistry.get(), fileManager);
    }

    /**
     * A factory that creates a {@link RootPlan} from a SPARQL Generate
     * query.
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
     * @param query the SPARQL Generate query.
     * @return the RootPlan that may be used to execute the SPARQL
 Generate query.
     */
    public final RootPlan create(final SPARQLGenerateQuery query) {
        checkNotNull(query, "Query must not be null");
        return make(query);
    }

    /**
     * A factory that creates a {@link RootPlan} from a SPARQL Generate
     * query.
     *
     * @param queryStr the string representation of the SPARQL Generate query.
     * @return the RootPlan that may be used to execute the SPARQL
 Generate query.
     */
    public final RootPlan create(final String queryStr) {
        checkNotNull(queryStr, "Parameter string must not be null");
        SPARQLGenerateQuery query;
        try {
            query = (SPARQLGenerateQuery) QueryFactory.create(queryStr,
                    SPARQLGenerate.SYNTAX);
        } catch (QueryParseException ex) {
            throw new SPARQLGenerateException(
                    "Error while parsing the query ", ex);
        }
        return create(query);
    }

    /**
     * Makes a {@code RootPlan} for a SPARQL Generate query.
     *
     * @param query the SPARQL Generate Query.
     * @return the RootPlan.
     */
    private RootPlan make(final SPARQLGenerateQuery query) {
        return make(query, false);
    }

    /**
     * Makes a {@code RootPlan} for a SPARQL Generate query.
     *
     * @param query the SPARQL Generate Query.
     * @param distant whether this query was obtained from a GENERATE URI.
     * @return the RootPlan.
     */
    private RootPlan make(final SPARQLGenerateQuery query, boolean distant) {
        checkNotNull(query, "The query must not be null");
        
        List<IteratorOrSourcePlan> iteratorAndSourcePlans = new ArrayList<>();
        SelectPlan selectPlan = null;
        GeneratePlan generatePlan = null;

        if (query.hasIteratorsAndSources()) {
            for(ElementIteratorOrSource el : query.getIteratorsAndSources()) {
                IteratorOrSourcePlan iteratorOrSourcePlan;
                if (el instanceof ElementIterator) {
                    ElementIterator elementIterator = (ElementIterator) el;
                    iteratorOrSourcePlan = makeIteratorPlan(elementIterator);
                } else if (el instanceof ElementSource) {
                    ElementSource elementSource = (ElementSource) el;
                    iteratorOrSourcePlan = makeSourcePlan(elementSource);
                } else {
                    throw new UnsupportedOperationException("should not reach"
                            + " this point");
                }
                iteratorAndSourcePlans.add(iteratorOrSourcePlan);
            }
        }
        if (query.getQueryPattern()!=null) {
            selectPlan = makeSelectPlan(query);
        }
        if (query.hasGenerateURI()) {
            generatePlan = makeGenerateQueryPlan(query);
        } else if (query.hasGenerateTemplate()) {
            generatePlan = makeGenerateTemplatePlan(query);
        } else {
            throw new UnsupportedOperationException("should not reach this"
                    + " point");
        }
        return new RootPlanImpl(
                iteratorAndSourcePlans, selectPlan,
                generatePlan, query.getPrefixMapping(), distant);
    }

    /**
     * Makes the plan for a SPARQL SELECTOR clause.
     *
     * @param elementIterator the SPARQL SELECTOR
     * @return -
     */
    IteratorOrSourcePlan makeIteratorPlan(
            final ElementIterator elementIterator) {
        checkNotNull(elementIterator, "The Iterator must not be null");

        Var var = elementIterator.getVar();
        Expr expr = elementIterator.getExpr();

        checkNotNull(var, "The variable of the Iterator must not be null");
        checkNotNull(expr, "The Expr in the iterator must not be null");
        checkIsTrue(expr.isFunction(), "Iterator should be a function:"
                + " <iri>(...) AS ?var");

        ExprFunction function = expr.getFunction();
        String iri = function.getFunctionIRI();

        IteratorFunctionFactory factory = sr.get(iri);
        IteratorFunction iterator = factory.create(iri);
        ExprList exprList = new ExprList(function.getArgs());
        iterator.build(exprList);
        return new IteratorPlanImpl(iterator, exprList, var);
    }

    /**
     * Makes the plan for a SPARQL SOURCE clause. If (1) accept
     * is not set and (2) the {@code FileManager} finds the file
     * locally, then the behaviour is not specified (yet).
     *
     * @param elementSource the SPARQL SOURCE
     * @return -
     */
    private IteratorOrSourcePlan makeSourcePlan(
            final ElementSource elementSource) {
        checkNotNull(elementSource, "The Source must not be null");

        Node node = elementSource.getSource();
        Node accept = elementSource.getAccept();
        Var var = elementSource.getVar();

        checkNotNull(node, "The source must not be null");
        checkIsTrue(node.isURI() || node.isVariable(), "The source must be a"
                + " URI or a variable. Got " + node);
        // accept may be null
        checkIsTrue(accept == null || accept.isVariable() || accept.isURI(),
                "The accept must be null, a variable or a URI. Got" + accept);
        checkNotNull(var, "The variable must not be null.");

        return new SourcePlanImpl(node, accept, var, fm);
    }

    /**
     * Makes the plan for the SPARQL SELECT part of the query.
     *
     * @param query the query for which the plan is created.
     * @return -
     */
    private SelectPlan makeSelectPlan(
            final SPARQLGenerateQuery query) {
        checkNotNull(query.getQueryPattern(), "The query must not be null");
        Query select = asSelectQuery(query);
        return new SelectPlanImpl(select);
    }

    /**
     * Makes the plan for a SPARQL {@code GENERATE ?source} clause.
     *
     * @param query the query for which the plan is created.
     * @return -
     * @throws IOException Thrown if the source query cannot be found, or if a
     * parse error occurs
     */
    private RootPlan makeGenerateQueryPlan(
            final SPARQLGenerateQuery query) {
        checkNotNull(query, "The query must not be null");
        checkIsTrue(query.hasGenerateURI(), "Query was expected to be of type"
                + " GENERATE ?source...");
        try {
            InputStream in = fm.open(query.getGenerateURI());
            String qString = IOUtils.toString(in);
            SPARQLGenerateQuery q
                    = (SPARQLGenerateQuery) QueryFactory.create(qString,
                            SPARQLGenerate.SYNTAX);
            return make(q, true);
        } catch (IOException ex) {
            throw new SPARQLGenerateException("Error while loading the query"
                    + " file " + query.getGenerateURI(), ex);
        } catch (QueryParseException ex) {
            throw new SPARQLGenerateException("Error while parsing the query "
                    + query.getGenerateURI(), ex);
        }
    }

    /**
     * Makes the plan for a SPARQL {@code GENERATE {}} clause.
     *
     * @param query the query for which the plan is created.
     * @return -
     */
    private GeneratePlan makeGenerateTemplatePlan(
            final SPARQLGenerateQuery query) {
        checkNotNull(query, "The query must not be null");
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
                plan = make(sub.getQuery());
            } else {
                throw new SPARQLGenerateException("should not reach this"
                        + " point");
            }
            subPlans.add(plan);
        }
        return new GenerateTemplatePlanImpl(subPlans);
    }

    /**
     * Create a SPARQL SELECT Query from a SPARQL Generate Query. Hence one may
     * rely on the existing SPARQL engine to do most of the job.
     *
     * @param query the SPARQL Generate query
     * @return the SPARQL SELECT Query.
     */
    private Query asSelectQuery(final SPARQLGenerateQuery query) {
        checkNotNull(query, "The query must not be null");

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
        LOG.debug("Generated query select query: " + output.serialize());
        return output;
    }

    /**
     * Checks that the object is not null.
     *
     * @param obj object to check.
     * @param msg message used to throw the exception if the object is null.
     * @throws IllegalArgumentException Thrown if the object is null.
     */
    private void checkNotNull(final Object obj, final String msg) {
        checkIsTrue(obj != null, msg);
    }

    /**
     * Checks that the test is true.
     *
     * @param test boolean to check.
     * @param msg message used to throw the exception if the boolean is false.
     * @throws IllegalArgumentException Thrown if the boolean is false.
     */
    private void checkIsTrue(final boolean test, final String msg) {
        if (!test) {
            throw new IllegalArgumentException(msg);
        }
    }

}
