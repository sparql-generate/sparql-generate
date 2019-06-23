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
package fr.emse.ci.sparqlext.generate.engine;

import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import java.io.File;
import java.util.Iterator;
import java.util.UUID;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.LangBuilder;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserRegistry;
import org.apache.jena.riot.ReaderRIOT;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.riot.system.IRIResolver;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.engine.Plan;
import org.apache.jena.sparql.engine.QueryEngineFactory;
import org.apache.jena.sparql.engine.QueryEngineRegistry;
import org.apache.jena.sparql.engine.QueryExecutionBase;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingRoot;
import org.apache.jena.sparql.engine.binding.BindingUtils;
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

    private SPARQLExtQuery query = null;
    private Plan plan = null;
    private QuerySolution initialBinding = null;

    public SPARQLExtSelectExecution(
            final SPARQLExtQuery query,
            final Dataset dataset,
            final Context context) {
        super(query, dataset, context, null);
        this.query = query;
    }

    @Override
    public Plan getPlan() {
        if (plan == null) {
            DatasetGraph dsg = prepareDataset();
            Binding inputBinding = null;
            if (initialBinding != null) {
                inputBinding = BindingUtils.asBinding(initialBinding);
            }
            if (inputBinding == null) {
                inputBinding = BindingRoot.create();
            }
            QueryEngineFactory qeFactory = QueryEngineRegistry.get().find(getQuery(), dsg, getContext());
            plan = qeFactory.create(query, dsg, inputBinding, getContext());
        }
        return plan;
    }

    private DatasetGraph prepareDataset() {
        if (!query.hasDatasetDescription()) {
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

        if (query.getGraphURIs() != null && !query.getGraphURIs().isEmpty()) {
            query.getGraphURIs().forEach((sourceURI) -> addDefaultGraph(dsg, baseURI, sourceURI));
        }
        if (query.getNamedGraphURIs() != null && !query.getNamedGraphURIs().isEmpty()) {
            query.getNamedGraphURIs().forEach((sourceURI) -> addNamedGraph(dsg, baseURI, sourceURI));
        }
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
    public void setInitialBinding(QuerySolution startSolution) {
        initialBinding = startSolution;
    }

    @Override
    public void close() {
        super.close();
        if (plan != null) {
            plan.close();
        }
    }

    private void addDefaultGraph(DatasetGraph dsg, String baseURI, String sourceURI) {
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

    private void addNamedGraph(DatasetGraph dsg, String baseURI, String sourceURI) {
        final String absURI = baseURI(sourceURI, baseURI);
        Node n = NodeFactory.createURI(absURI);
        Graph g = dsg.getGraph(n);
        if(g==null) {
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
}
