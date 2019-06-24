/*
 * Copyright 2019 École des Mines de Saint-Étienne.
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

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.riot.system.IRIResolver;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.Plan;
import org.apache.jena.sparql.engine.QueryEngineFactory;
import org.apache.jena.sparql.engine.QueryEngineRegistry;
import org.apache.jena.sparql.engine.QueryExecutionBase;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingRoot;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public class SPARQLExtSelectExecution extends QueryExecutionBase {

    private static final Logger LOG = LoggerFactory.getLogger(SPARQLExtSelectExecution.class);

    private final SPARQLExtQuery query;
    private final Binding signatureBinding;
    private final FunctionEnv env;
    private Plan plan = null;

    public SPARQLExtSelectExecution(
            final SPARQLExtQuery query,
            final Dataset dataset,
            final Binding signatureBinding,
            final Context context) {
        super(query, dataset, context, null);
        this.query = query;
        this.signatureBinding = signatureBinding;
        this.env = new FunctionEnvBase(context);
    }

    @Override
    public Plan getPlan() {
        if (plan == null) {
            DatasetGraph dsg = prepareDataset();
            Binding inputBinding = BindingRoot.create();
            QueryEngineFactory qeFactory = QueryEngineRegistry.get().find(getQuery(), dsg, getContext());
            plan = qeFactory.create(query, dsg, inputBinding, getContext());
        }
        return plan;
    }

    private DatasetGraph prepareDataset() {
        if (query.getFromClauses().isEmpty()) {
            if (getDataset() != null) {
                return getDataset().asDatasetGraph();
            } else {
                throw new QueryExecException("No dataset description for query");
            }
        }
        return createDatasetGraph();
    }

    private DatasetGraph createDatasetGraph() {
        final String baseURI;
        if (query.getBaseURI() != null) {
            baseURI = IRIResolver.resolveString(query.getBaseURI());
        } else {
            baseURI = IRIResolver.chooseBaseURI().toString();
        }
        final DatasetGraph dsg = DatasetGraphFactory.createGeneral();
        if (query.getFromClauses().isEmpty()) {
            return dsg;
        }
        query.getFromClauses().forEach((fromClause) -> {
            if (fromClause.getGenerate() == null) {
                if (!fromClause.isNamed()) {
                    addDefaultGraph(dsg, baseURI, fromClause.getName());
                } else {
                    addNamedGraph(dsg, baseURI, fromClause.getName());
                }
            } else {
                SPARQLExtQuery generate = fromClause.getGenerate();
                if (!fromClause.isNamed()) {
                    addDefaultGraph(dsg, baseURI, generate);
                } else {
                    addNamedGraph(dsg, baseURI, generate, fromClause.getName());
                }
            }
        });
        return dsg;
    }

    private static String baseURI(String sourceURI, String absBaseURI) {
        if (absBaseURI == null) {
            return IRIResolver.resolveString(sourceURI);
        } else {
            return IRIResolver.resolveString(sourceURI, absBaseURI);
        }
    }

    @Override
    public void close() {
        super.close();
        if (plan != null) {
            plan.close();
        }
    }

    private void addDefaultGraph(DatasetGraph dsg, String baseURI, Expr sourceExpr) {
        String sourceURI = evalSourceURI(sourceExpr);
        final String absURI = baseURI(sourceURI, baseURI);
        // default: check the dataset
        if (getDataset().containsNamedModel(absURI)) {
            Node n = NodeFactory.createURI(absURI);
            Graph g = getDataset().getNamedModel(absURI).getGraph();
            GraphUtil.addInto(dsg.getDefaultGraph(), g);
            return;
        }
        // fallback: load as RDF graph
        StreamRDF dest = StreamRDFLib.graph(dsg.getDefaultGraph());
        loadGraph(sourceURI, absURI, dest);
    }

    private void addNamedGraph(DatasetGraph dsg, String baseURI, Expr sourceExpr) {
        String sourceURI = evalSourceURI(sourceExpr);
        final String absURI = baseURI(sourceURI, baseURI);
        Node n = NodeFactory.createURI(absURI);
        Graph g = dsg.getGraph(n);
        if (g == null) {
            g = GraphFactory.createJenaDefaultGraph();
            dsg.addGraph(n, g);
        }
        // default: check the dataset
        if (getDataset().containsNamedModel(absURI)) {
            Graph dg = getDataset().getNamedModel(absURI).getGraph();
            GraphUtil.addInto(g, dg);
            return;
        }
        // fallback: load as RDF graph
        StreamRDF dest = StreamRDFLib.graph(g);
        loadGraph(sourceURI, absURI, dest);
    }

    private void addDefaultGraph(DatasetGraph dsg, String baseURI, SPARQLExtQuery generate) {
        loadGraph(generate, dsg.getDefaultGraph());
    }

    private void addNamedGraph(DatasetGraph dsg, String baseURI, SPARQLExtQuery generate, Expr name) {
        String sourceURI = evalSourceURI(name);
        final String absURI = baseURI(sourceURI, baseURI);
        Node n = NodeFactory.createURI(absURI);
        Graph g = dsg.getGraph(n);
        if (g == null) {
            g = GraphFactory.createJenaDefaultGraph();
            dsg.addGraph(n, g);
        }
        loadGraph(generate, g);
    }

    private void loadGraph(String sourceURI, String absURI, StreamRDF dest) {
        final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) getContext().get(SysRIOT.sysStreamManager);
        try {
            sourceURI = sm.mapURI(sourceURI);
            if ((new File(sourceURI)).exists()) {
                sourceURI = (new File(sourceURI)).toURI().normalize().toString();
            }
            RDFParser.create()
                    .source(sourceURI)
                    .base(absURI)
                    .context(getContext())
                    .forceLang(Lang.N3)
                    .parse(dest);
        } catch (RiotException ex) {
            LOG.warn("Error while loading graph " + sourceURI, ex);
        }
    }

    private String evalSourceURI(Expr sourceExpr) {
        try {
            NodeValue nodeValue = sourceExpr.eval(signatureBinding, env);
            if (!nodeValue.isIRI()) {
                throw new IllegalArgumentException("FROM source expression did not eval to a URI " + sourceExpr);
            }
            return nodeValue.asNode().getURI();
        } catch (ExprEvalException ex) {
            throw new IllegalArgumentException("Exception when evaluating the source expression" + sourceExpr, ex);
        }
    }

    /**
     * Throws UnsupportedOperationException
     *
     * @param startSolution
     * @deprecated
     */
    @Override
    @Deprecated
    public void setInitialBinding(QuerySolution startSolution) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException
     *
     * @param table
     * @deprecated
     */
    @Override
    @Deprecated
    public void setInitialBindings(ResultSet table) {
        throw new UnsupportedOperationException();
    }

    private void loadGraph(SPARQLExtQuery generate, Graph graph) {
        // for now, create plan everytime. optimize later.
        RootPlan generatePlan = PlanFactory.create(generate);
        StreamRDF dest = StreamRDFLib.graph(graph);
        List<Var> vars = new ArrayList<>();
        for (Iterator<Var> vs = signatureBinding.vars(); vs.hasNext();) {
            vars.add(vs.next());
        }
        final List<Binding> values = new ArrayList<>();
        values.add(signatureBinding);
        BNodeMap bNodeMap = new BNodeMap();
        Context newContext = SPARQLExt.createContext(getContext());
        try {
            generatePlan.exec(getDataset(), vars, values, bNodeMap, newContext, dest, null, null).get();
        } catch (InterruptedException ex) {
            LOG.warn("Interrupted Exception while generating graph for FROM clause");
        } catch (ExecutionException ex) {
            LOG.warn("Exception while generating graph for FROM clause ", ex.getCause());
        }
    }

}
