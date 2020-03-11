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
package fr.emse.ci.sparqlext.utils;

import fr.emse.ci.sparqlext.SPARQLExt;
import static fr.emse.ci.sparqlext.SPARQLExt.NS;
import fr.emse.ci.sparqlext.SPARQLExtException;
import fr.emse.ci.sparqlext.engine.QueryExecutor;
import fr.emse.ci.sparqlext.function.SPARQLExtFunctionRegistry;
import fr.emse.ci.sparqlext.graph.Node_List;
import fr.emse.ci.sparqlext.iterator.IteratorFunctionRegistry;
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.riot.system.IRIResolver;
import org.apache.jena.riot.system.StreamOps;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.SystemARQ;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.function.FunctionRegistry;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public class ContextUtils {

	static final Logger LOG = LoggerFactory.getLogger(ContextUtils.class);

	private static final Symbol COMMONS = SystemARQ.allocSymbol(NS, "commons");

	private static final Symbol PARENT_CONTEXT = SystemARQ.allocSymbol(NS, "parent_context");

	private static final Symbol DATASET = SystemARQ.allocSymbol(NS, "dataset");

	private static final Symbol OUTPUT_TEMPLATE = SystemARQ.allocSymbol(NS, "output_template");

	private static final Symbol OUTPUT_GENERATE = SystemARQ.allocSymbol(NS, "output_generate");

	private static final Symbol OUTPUT_SELECT = SystemARQ.allocSymbol(NS, "output_select");

	private static final Symbol BASE = SystemARQ.allocSymbol(NS, "base");

	private static final Symbol PREFIX_MANAGER = SystemARQ.allocSymbol(NS, "prefixManager");

	private static final Symbol SIZE = SystemARQ.allocSymbol(NS, "size");

	private static final Symbol LIST_NODES = SystemARQ.allocSymbol(NS, "list_nodes");

	private static final Node[] NIL = new Node[] { RDF.nil.asNode() };

	/**
	 * get the node at position i in the LIST( expr ). Or rdf:nil if the position is
	 * equal to the number of bindings
	 *
	 * @param list
	 * @param context
	 * @param position
	 * @return
	 */
	public static synchronized Node getNode(final Context context, final Node_List list, int position) {
		return getInfo(context, list)[position];
	}

	public static synchronized Node[] getInfo(final Context context, final Node_List list) {
		final int size = (Integer) context.get(SIZE);
		if (size == 0) {
			return NIL;
		}
		if (context.isUndef(LIST_NODES)) {
			context.set(LIST_NODES, new HashMap<>());
		}
		final Map<Node_List, Node[]> listNodes = context.get(LIST_NODES);
		if (!listNodes.containsKey(list)) {
			createListNodes(listNodes, list, size);
		}
		return listNodes.get(list);
	}

	private static void createListNodes(final Map<Node_List, Node[]> listNodes, final Node_List list, final int size) {
		Node[] nodes = new Node[size + 1];
		for (int i = 0; i < size; i++) {
			nodes[i] = NodeFactory.createBlankNode();
		}
		nodes[size] = RDF.nil.asNode();
		listNodes.put(list, nodes);
	}

	public static Dataset getDataset(Context context) {
		return context.get(DATASET);
	}

	public static ExecutorService getExecutor(Context context) {
		Commons commons = context.get(COMMONS);
		return commons.executor;
	}

	public static QueryExecutor getQueryExecutor(Context context) {
		Commons commons = context.get(COMMONS);
		return commons.queryExecutor;
	}

	public static boolean isRootContext(Context context) {
		return context.get(PARENT_CONTEXT) == null;
	}

	public static boolean isDebugStConcat(Context context) {
		Commons commons = context.get(COMMONS);
		return commons.debugTemplate;
	}

	public static String getBase(Context context) {
		String base = context.get(BASE);
		return base;
	}

	public static PrefixMapping getPrefixMapping(Context context) {
		PrefixMapping pm = context.get(PREFIX_MANAGER);
		Objects.nonNull(pm);
		return pm;
	}

	public static void addTaskOnClose(Context context, final Runnable task) {
		Commons commons = context.get(COMMONS);
		commons.closingTasks.add(task);
	}

	public static void close(Context context) {
		Commons commons = context.get(COMMONS);
		try {
			LOG.trace("Closing context");
			commons.closingTasks.forEach(Runnable::run);
		} catch (Exception ex) {
			LOG.warn("Exception while closing context:", ex);
		}
	}
	
	public static IndentedWriter getTemplateOutput(Context context) {
		IndentedWriter writer = context.get(OUTPUT_TEMPLATE);
		if(writer != null) {
			return writer;
		}
		if(!isRootContext(context)) {
			Context parentContext = (Context) context.get(PARENT_CONTEXT);
			return getTemplateOutput(parentContext);
		}
		return null;
	}
	
	public static Consumer<ResultSet> getSelectOutput(Context context) {
		Consumer<ResultSet> output = context.get(OUTPUT_SELECT);
		if(output != null) {
			return output;
		}
		if(!isRootContext(context)) {
			Context parentContext = (Context) context.get(PARENT_CONTEXT);
			return getSelectOutput(parentContext);
		}
		return null;
	}
	
	public static StreamRDF getGenerateOutput(Context context) {
		StreamRDF output = context.get(OUTPUT_GENERATE);
		if(output != null) {
			return output;
		}
		if(!isRootContext(context)) {
			Context parentContext = (Context) context.get(PARENT_CONTEXT);
			return getGenerateOutput(parentContext);
		}
		return null;
	}

	public static void loadGraph(Context context, String sourceURI, String baseURI, StreamRDF dest) {
		if(getDataset(context).containsNamedModel(sourceURI)) {
			final Model model = getDataset(context).getNamedModel(sourceURI);
			StreamOps.sendGraphToStream(model.getGraph(), dest);
			return;
		}
		if(!isRootContext(context)) {
			Context parentContext = (Context) context.get(PARENT_CONTEXT);
			loadGraph(parentContext, sourceURI, baseURI, dest);
			return;
		}
		final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) context.get(SysRIOT.sysStreamManager);
		final String acceptHeader = "text/turtle;q=1.0,application/rdf+xml;q=0.9,*/*;q=0.1";
		final LookUpRequest request = new LookUpRequest(sourceURI, acceptHeader);
		try (TypedInputStream tin = sm.open(request);) {
			if(tin == null) {
				LOG.warn("Could not locate graph " + request);
				return;
			}
			Lang lang = RDFLanguages.contentTypeToLang(tin.getMediaType());
			RDFParser.create().source(tin).base(baseURI).context(context).lang(lang).parse(dest);
		} catch (RiotException ex) {
			LOG.warn("Error while loading graph " + sourceURI, ex);
		}
	}


	public static TypedInputStream openStream(Context context, String sourceUri, String acceptHeader) {
		final LookUpRequest request = new LookUpRequest(sourceUri, acceptHeader);
		final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) context.get(SysRIOT.sysStreamManager);
		Objects.requireNonNull(sm);
		return sm.open(request);
	}

	public static Forker fork(Context context) {
		return new Forker(context, false);
	}

	public static Forker fork(Context context, boolean isRoot) {
		return new Forker(context, isRoot);
	}

	/**
	 * Forker class is used to create a new context from an existing one.
	 */
	public static class Forker {

		private final Context context;

		/**
		 * 
		 * @param ctx
		 *            the context to fork
		 */
		private Forker(Context ctx, boolean isRoot) {
			context = new Context(ctx);
			context.set(SysRIOT.sysStreamManager,
					context.get(SysRIOT.sysStreamManager, SPARQLExtStreamManager.makeStreamManager()));
			context.set(BASE, context.get(BASE));
			context.set(PREFIX_MANAGER, context.get(PREFIX_MANAGER, PrefixMapping.Standard));
			context.set(SIZE, 0);
			if(!isRoot) {
				context.set(PARENT_CONTEXT, ctx);
			}
		}

		public Forker setDataset(Dataset dataset) {
			context.set(DATASET, dataset);
			return this;
		}

		public Forker setSize(int size) {
			context.set(SIZE, size);
			return this;
		}

		public Forker setTemplateOutput(IndentedWriter output) {
			context.set(OUTPUT_TEMPLATE, output);
			return this;
		}

		public Forker setGenerateOutput(StreamRDF output) {
			context.set(OUTPUT_GENERATE, output);
			return this;
		}

		public Forker setSelectOutput(Consumer<ResultSet> output) {
			context.set(OUTPUT_SELECT, output);
			return this;
		}

		public Context fork() {
			return context;
		}

	}

	public static Context createSimple() {
		return new Builder().build();
	}


	public static Builder build() {
		return new Builder();
	}

	public static Builder build(IndentedWriter output) {
		return new Builder(output);
	}

	public static Builder build(StreamRDF output) {
		return new Builder(output);
	}

	public static Builder build(Consumer<ResultSet> output) {
		return new Builder(output);
	}

	public static class Builder {

		private final Context context;
		private final Commons commons;

		private Builder() {
			this.context = new Context(ARQ.getContext());
			this.commons = new Commons();

			// update functionregistry
			FunctionRegistry registry = (FunctionRegistry) context.get(ARQConstants.registryFunctions);
			SPARQLExtFunctionRegistry newRegistry = new SPARQLExtFunctionRegistry(registry, context);
			context.set(ARQConstants.registryFunctions, newRegistry);

			// update iteratorregistry
			IteratorFunctionRegistry iteratorRegistry = (IteratorFunctionRegistry) context
					.get(SPARQLExt.REGISTRY_ITERATORS);
			IteratorFunctionRegistry newIteratorRegistry = new IteratorFunctionRegistry(iteratorRegistry, context);
			context.set(SPARQLExt.REGISTRY_ITERATORS, newIteratorRegistry);

			// default streammanager
			context.set(SysRIOT.sysStreamManager, SPARQLExtStreamManager.makeStreamManager());

			// set variable parts
			context.set(DATASET, DatasetFactory.create());

			// default prefix manager
			context.set(PREFIX_MANAGER, PrefixMapping.Standard);

			// default number of results and blank nodes
			context.set(SIZE, 0);
			// context.set(LIST_NODES, new HashMap<>());

			context.set(COMMONS, commons);
		}

		private Builder(IndentedWriter output) {
			this();
			context.set(OUTPUT_TEMPLATE, output);
		}

		private Builder(StreamRDF output) {
			this();
			context.set(OUTPUT_GENERATE, output);
		}

		private Builder(Consumer<ResultSet> output) {
			this();
			context.set(OUTPUT_SELECT, output);
		}
		
		public Builder setBase(String base) {
			context.set(BASE, base);
			return this;
		}

		public Builder setPrefixMapping(PrefixMapping pm) {
			context.set(PREFIX_MANAGER, pm);
			return this;
		}

		public Builder setPrefixMapping(Query q) {
			context.set(PREFIX_MANAGER, q.getPrefixMapping());
			return this;
		}

		public Builder setInputModel(Model inputModel) {
			Dataset inputDataset = DatasetFactory.create(inputModel);
			context.set(DATASET, inputDataset);
			return this;
		}

		public Builder setInputDataset(Dataset inputDataset) {
			context.set(DATASET, inputDataset);
			return this;
		}
		
		public Builder setTemplateOutput(IndentedWriter output) {
			context.set(OUTPUT_TEMPLATE, output);
			return this;
		}

		public Builder setGenerateOutput(StreamRDF output) {
			context.set(OUTPUT_GENERATE, output);
			return this;
		}

		public Builder setSelectOutput(Consumer<ResultSet> output) {
			context.set(OUTPUT_SELECT, output);
			return this;
		}

		public Builder setStreamManager(SPARQLExtStreamManager sm) {
			context.set(SysRIOT.sysStreamManager, sm);
			return this;
		}

		public Builder setExecutor(ExecutorService executor) {
			commons.executor = executor;
			return this;
		}

		public Builder setDebugTemplate(boolean debugTemplate) {
			commons.debugTemplate = debugTemplate;
			return this;
		}

		public Builder setQueryExecutor(QueryExecutor queryExecutor) {
			commons.queryExecutor = queryExecutor;
			return this;
		}
		
		public Context build() {
			return context;
		}

	}

	private static class Commons {

		private boolean debugTemplate = false;
		private ExecutorService executor = Executors.newSingleThreadExecutor();
		private QueryExecutor queryExecutor = new QueryExecutor();
		private final Set<Runnable> closingTasks = new HashSet<>();

		private Commons() {
		}

	}
}
