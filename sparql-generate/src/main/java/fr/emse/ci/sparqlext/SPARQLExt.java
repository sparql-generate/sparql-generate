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
package fr.emse.ci.sparqlext;

import fr.emse.ci.sparqlext.function.SPARQLExtFunctionRegistry;
import fr.emse.ci.sparqlext.function.library.FUN_CSSPath;
import fr.emse.ci.sparqlext.function.library.FUN_SplitAtPostion;
import fr.emse.ci.sparqlext.function.library.FUN_GeoJSONGeometry;
import fr.emse.ci.sparqlext.function.library.FUN_bnode;
import fr.emse.ci.sparqlext.function.library.FUN_regex;
import fr.emse.ci.sparqlext.function.library.FUN_dateTime;
import fr.emse.ci.sparqlext.function.library.FUN_XPath;
import fr.emse.ci.sparqlext.function.library.FUN_JSONPath;
import fr.emse.ci.sparqlext.function.library.FUN_CBOR;
import fr.emse.ci.sparqlext.function.library.FUN_CamelCase;
import fr.emse.ci.sparqlext.function.library.FUN_MixedCase;
import fr.emse.ci.sparqlext.function.library.FUN_PrefixedIRI;
import fr.emse.ci.sparqlext.function.library.FUN_Property;
import fr.emse.ci.sparqlext.function.library.FUN_TitleCase;
import fr.emse.ci.sparqlext.function.library.ST_Call_Template;
import fr.emse.ci.sparqlext.function.library.ST_Concat;
import fr.emse.ci.sparqlext.function.library.ST_Decr;
import fr.emse.ci.sparqlext.function.library.ST_Format;
import fr.emse.ci.sparqlext.function.library.ST_Incr;
import fr.emse.ci.sparqlext.graph.Node_List;
import fr.emse.ci.sparqlext.iterator.library.ITER_GeoJSON;
import fr.emse.ci.sparqlext.iterator.library.ITER_CSVHeaders;
import fr.emse.ci.sparqlext.iterator.library.ITER_CSV;
import fr.emse.ci.sparqlext.iterator.library.ITER_Split;
import fr.emse.ci.sparqlext.iterator.library.ITER_JSONListKeys;
import fr.emse.ci.sparqlext.iterator.library.ITER_CSSPath;
import fr.emse.ci.sparqlext.iterator.library.ITER_XPath;
import fr.emse.ci.sparqlext.iterator.library.ITER_regex;
import fr.emse.ci.sparqlext.iterator.library.ITER_for;
import fr.emse.ci.sparqlext.iterator.library.ITER_CBOR;
import fr.emse.ci.sparqlext.iterator.library.ITER_JSONPath;
import fr.emse.ci.sparqlext.iterator.IteratorFunctionRegistry;
import fr.emse.ci.sparqlext.iterator.library.ITER_Call_Select;
import fr.emse.ci.sparqlext.iterator.library.ITER_DefaultGraphNamespaces;
import fr.emse.ci.sparqlext.iterator.library.ITER_HTTPGet;
import fr.emse.ci.sparqlext.iterator.library.ITER_MQTTSubscribe;
import fr.emse.ci.sparqlext.iterator.library.ITER_WebSocket;
import fr.emse.ci.sparqlext.iterator.library.ITER_dummy;
import fr.emse.ci.sparqlext.lang.ParserSPARQLExt;
import fr.emse.ci.sparqlext.serializer.SPARQLExtQuerySerializer;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import fr.emse.ci.sparqlext.utils.Request;
import fr.emse.ci.sparqlext.utils.WktLiteral;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.LangBuilder;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.stream.StreamManager;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.function.FunctionRegistry;
import org.apache.jena.sparql.lang.SPARQLParser;
import org.apache.jena.sparql.lang.SPARQLParserFactory;
import org.apache.jena.sparql.lang.SPARQLParserRegistry;
import org.apache.jena.sparql.serializer.QuerySerializerFactory;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.serializer.SerializerRegistry;
import org.apache.jena.sparql.util.NodeToLabelMapBNode;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.sparql.util.TranslationTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jena.riot.RDFLanguages.strLangRDFXML;
import static org.apache.jena.riot.WebContent.contentTypeRDFXML;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.SystemARQ;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.serializer.FormatterElement;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.vocabulary.RDF;

/**
 * The configuration entry point of SPARQL-Generate. Method {@link #init()} must
 * be called before anything else.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public final class SPARQLExt {

    /**
     * Private constructor. No instance should be created.
     */
    private SPARQLExt() {
    }

    /**
     * The SPARQL-Generate media type.
     */
    public static final String MEDIA_TYPE = "application/vnd.sparql-generate";

    /**
     * The SPARQL-Generate media type URI.
     */
    public static final String MEDIA_TYPE_URI = "http://www.iana.org/assignments/media-types/" + MEDIA_TYPE;

    /**
     * The namespace of SPARQL-Generate.
     */
    public static final String NS = "http://w3id.org/sparql-generate/";

    /**
     * The namespace of SPARQL-Generate binding functions.
     */
    public static final String FUN = NS + "fn/";

    /**
     * The namespace of SPARQL-Generate iterator functions.
     */
    public static final String ITER = NS + "iter/";

    /**
     * The URI of the SPARQL-Generate syntax.
     */
    public static final String SYNTAX_URI = NS + "syntax";

    /**
     * The File Extension for SPARQL-Generate documents.
     */
    public static final String EXT = ".rqg";

    /**
     * The SPARQL-Generate syntax.
     */
    public static final Syntax SYNTAX;

    public static final Symbol PREFIX_MANAGER = SystemARQ.allocSymbol("prefixManager");

    public static final Symbol INDENT = SystemARQ.allocSymbol("indent");

    public static final Symbol INDENT_CONTROL = SystemARQ.allocSymbol("indent_control");

    public static final Symbol LOADED_QUERIES = SystemARQ.allocSymbol("loaded_queries");

    public static final Symbol LOADED_PLANS = SystemARQ.allocSymbol("loaded_plans");

    public static final Symbol EXECUTION_CALLS = SystemARQ.allocSymbol("execution_calls");

    public static final Symbol STREAM_MANAGER = SystemARQ.allocSymbol("stream_manager");

    public static final Symbol VARS = SystemARQ.allocSymbol("vars");

    public static final Symbol DATASET = SystemARQ.allocSymbol("dataset");

    public static final Symbol EXECUTOR = SystemARQ.allocSymbol("executor");

    public static final Symbol CLOSE_TASKS = SystemARQ.allocSymbol("close_tasks");

    public static final Symbol SIZE = SystemARQ.allocSymbol("size");

    public static final Symbol LIST_NODES = SystemARQ.allocSymbol("list_nodes");

    public static final Symbol DEBUG_ST_CONCAT = SystemARQ.allocSymbol("debug_st_concat");    

    /**
     * Forces the initialization of SPARQL-Generate.
     */
    public static void init() {
    }

    static final Logger LOG = LoggerFactory.getLogger(SPARQLExt.class);

    static {
        LOG.trace("initializing SPARQLGenerate");

        SYNTAX = new SPARQLGenerateSyntax(SYNTAX_URI);

        FunctionRegistry fnreg = FunctionRegistry.get();
        fnreg.put(FUN_JSONPath.URI, FUN_JSONPath.class);
        fnreg.put(FUN_XPath.URI, FUN_XPath.class);
        fnreg.put(FUN_SplitAtPostion.URI, FUN_SplitAtPostion.class);
        fnreg.put(FUN_CSSPath.URI, FUN_CSSPath.class);
        fnreg.put(FUN_CBOR.URI, FUN_CBOR.class);
        fnreg.put(FUN_regex.URI, FUN_regex.class);
        fnreg.put(FUN_bnode.URI, FUN_bnode.class);
        fnreg.put(FUN_dateTime.URI, FUN_dateTime.class);
        fnreg.put(FUN_GeoJSONGeometry.URI, FUN_GeoJSONGeometry.class);
        fnreg.put(FUN_Property.URI, FUN_Property.class);
        fnreg.put(FUN_CamelCase.URI, FUN_CamelCase.class);
        fnreg.put(FUN_MixedCase.URI, FUN_MixedCase.class);
        fnreg.put(FUN_TitleCase.URI, FUN_TitleCase.class);
        fnreg.put(FUN_PrefixedIRI.URI, FUN_PrefixedIRI.class);

        fnreg.put(ST_Call_Template.URI, ST_Call_Template.class);
        fnreg.put(ST_Decr.URI, ST_Decr.class);
        fnreg.put(ST_Incr.URI, ST_Incr.class);
        fnreg.put(ST_Concat.URI, ST_Concat.class);
        fnreg.put(ST_Format.URI, ST_Format.class);

        IteratorFunctionRegistry itereg = IteratorFunctionRegistry.get();
        itereg.put(ITER_JSONPath.URI, ITER_JSONPath.class);
        itereg.put(ITER_JSONListKeys.URI, ITER_JSONListKeys.class);
        itereg.put(ITER_regex.URI, ITER_regex.class);
        itereg.put(ITER_XPath.URI, ITER_XPath.class);
        itereg.put(ITER_Split.URI, ITER_Split.class);
        itereg.put(ITER_CSV.URI, ITER_CSV.class);
        itereg.put(ITER_CSSPath.URI, ITER_CSSPath.class);
        itereg.put(ITER_CBOR.URI, ITER_CBOR.class);
        itereg.put(ITER_CSVHeaders.URI, ITER_CSVHeaders.class);
        itereg.put(ITER_for.URI, ITER_for.class);
        itereg.put(ITER_GeoJSON.URI, ITER_GeoJSON.class);
        itereg.put(ITER_HTTPGet.URI, ITER_HTTPGet.class);
        itereg.put(ITER_MQTTSubscribe.URI, ITER_MQTTSubscribe.class);
        itereg.put(ITER_WebSocket.URI, ITER_WebSocket.class);
        itereg.put(ITER_DefaultGraphNamespaces.URI, ITER_DefaultGraphNamespaces.class);
        itereg.put(ITER_Call_Select.URI, ITER_Call_Select.class);
        itereg.put(ITER_dummy.URI, ITER_dummy.class);

        SPARQLParserRegistry.get()
                .add(SYNTAX, new SPARQLParserFactory() {
                    @Override
                    public boolean accept(final Syntax syntax) {
                        return SYNTAX.equals(syntax);
                    }

                    @Override
                    public SPARQLParser create(final Syntax syntax) {
                        return new ParserSPARQLExt();
                    }
                });

        QuerySerializerFactory factory = new QuerySerializerFactory() {
            @Override
            public boolean accept(Syntax syntax) {
                // Since ARQ syntax is a super set of SPARQL 1.1 both SPARQL 1.0
                // and SPARQL 1.1 can be serialized by the same serializer
                return Syntax.syntaxARQ.equals(syntax) || Syntax.syntaxSPARQL_10.equals(syntax)
                        || Syntax.syntaxSPARQL_11.equals(syntax) || SPARQLExt.SYNTAX.equals(syntax);
            }

            @Override
            public QueryVisitor create(Syntax syntax, Prologue prologue, IndentedWriter writer) {
                SerializationContext context = new SerializationContext(prologue, new NodeToLabelMapBNode("g", false));
                return new SPARQLExtQuerySerializer(writer, context);
            }

            @Override
            public QueryVisitor create(Syntax syntax, SerializationContext context, IndentedWriter writer) {
                return new SPARQLExtQuerySerializer(writer, context);
            }
        };

        SerializerRegistry registry = SerializerRegistry.get();
        registry.addQuerySerializer(SPARQLExt.SYNTAX, factory);

        RDFLanguages.unregister(Lang.RDFXML);
        RDFLanguages.register(LangBuilder.create(strLangRDFXML, contentTypeRDFXML)
                .addAltNames("RDFXML", "RDF/XML-ABBREV", "RDFXML-ABBREV")
                .addFileExtensions("rdf", "owl")
                .build());
        RDFLanguages.register(LangBuilder.create("XML", "application/xml")
                .addFileExtensions("xml")
                .build());
        RDFLanguages.register(LangBuilder.create("JSON", "application/json")
                .addFileExtensions("json")
                .build());
        RDFLanguages.register(LangBuilder.create("HTML", "text/html")
                .addFileExtensions("html", "xhtml")
                .build());
        RDFLanguages.register(LangBuilder.create("XHTML", "application/xhtml+xml")
                .addFileExtensions("xhtml")
                .build());

        TypeMapper.getInstance().registerDatatype(WktLiteral.wktLiteralType);

        StreamManager.setGlobal(SPARQLExtStreamManager.makeStreamManager());
    }

    /**
     * This class must be used instead of class <code>Syntax</code>.
     *
     * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
     */
    public static class SPARQLGenerateSyntax extends Syntax {

        /**
         * @param syntax the name of the syntax
         */
        public SPARQLGenerateSyntax(final String syntax) {
            super(syntax);
        }

        /**
         *
         */
        private final static TranslationTable<Syntax> generateSyntaxNames
                = new TranslationTable<>(true);

        static {
            generateSyntaxNames.put("sparqlGenerate", SYNTAX);
        }

        /**
         * Creates and registers a new syntax with this symbol.
         *
         * @param uri the name of the syntax
         * @return the syntax
         */
        public static Syntax make(final String uri) {
            if (uri == null) {
                return null;
            }
            Symbol sym = Symbol.create(uri);
            if (sym.equals(SYNTAX)) {
                return SYNTAX;
            }
            return Syntax.make(uri);
        }

        /**
         * Guess the syntax (query and update) based on filename.
         *
         * @param url the url of the syntax
         * @param defaultSyntax a default syntax
         * @return an available syntax for the filename
         */
        public static Syntax guessFileSyntax(
                final String url,
                final Syntax defaultSyntax) {
            if (url.endsWith(".rqg")) {
                return SYNTAX;
            }
            return Syntax.guessFileSyntax(url, defaultSyntax);
        }

    }

    /**
     * Adds a symbol for {@link registrySelectors#registryIterators} to
     * {@link ARQConstants}.
     *
     * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
     */
    public static class Constants extends ARQConstants {

        /**
         * The selectors library registry key.
         */
        public static final Symbol registryIterators
                = SystemARQ.allocSymbol("registryIterators");
    }

    public static synchronized Context createContext(PrefixMapping pm) {
        return createContext(pm, SPARQLExtStreamManager.makeStreamManager(), Executors.newWorkStealingPool());
    }

    public static synchronized Context createContext(PrefixMapping pm, SPARQLExtStreamManager sm) {
        return createContext(pm, sm, Executors.newWorkStealingPool());
    }

    public static synchronized Context createContext(SPARQLExtStreamManager sm) {
        return createContext(null, sm, Executors.newWorkStealingPool());
    }

    public static synchronized Context createContext(SPARQLExtStreamManager sm, Executor executor) {
        return createContext(null, sm, executor);
    }

    public static synchronized Context createContext(PrefixMapping pm, SPARQLExtStreamManager sm, Executor executor) {
        Context context = new Context(ARQ.getContext());
        context.set(SPARQLExt.PREFIX_MANAGER, pm);
        context.set(SPARQLExt.LOADED_QUERIES, new HashMap<>());
        context.set(SPARQLExt.LOADED_PLANS, new HashMap<>());
        context.set(SPARQLExt.EXECUTION_CALLS, new HashMap<>());
        context.set(SPARQLExt.STREAM_MANAGER, sm);
        context.set(SPARQLExt.VARS, new HashMap<>());
        context.set(SPARQLExt.EXECUTOR, executor);
        context.set(SPARQLExt.CLOSE_TASKS, new HashSet<>());
        context.set(SPARQLExt.SIZE, 0);
        context.set(SPARQLExt.LIST_NODES, new HashMap<>());
        context.set(SPARQLExt.DEBUG_ST_CONCAT, false);
        String rand = UUID.randomUUID().toString().substring(0, 4);
        String indentControl = "\f" + rand;
        context.set(SPARQLExt.INDENT_CONTROL, indentControl);
        FunctionRegistry registry = (FunctionRegistry) context.get(ARQConstants.registryFunctions);
        SPARQLExtFunctionRegistry newRegistry = new SPARQLExtFunctionRegistry(registry, context);
        context.set(ARQConstants.registryFunctions, newRegistry);
        return context;
    }

    public static synchronized Context createContext(Context context, PrefixMapping pm) {
        Context newContext = new Context(context);
        newContext.set(SPARQLExt.PREFIX_MANAGER, pm);
        newContext.set(SPARQLExt.LOADED_QUERIES, newContext.get(SPARQLExt.LOADED_QUERIES, new HashMap<>()));
        newContext.set(SPARQLExt.LOADED_PLANS, newContext.get(SPARQLExt.LOADED_PLANS, new HashMap<>()));
        newContext.set(SPARQLExt.EXECUTION_CALLS, newContext.get(SPARQLExt.EXECUTION_CALLS, new HashMap<>()));
        newContext.set(SPARQLExt.STREAM_MANAGER, newContext.get(SPARQLExt.STREAM_MANAGER, SPARQLExtStreamManager.makeStreamManager()));
        newContext.set(SPARQLExt.VARS, newContext.get(SPARQLExt.VARS, new HashMap<>()));
        newContext.set(SPARQLExt.EXECUTOR, newContext.get(SPARQLExt.EXECUTOR, Executors.newWorkStealingPool()));
        newContext.set(SPARQLExt.CLOSE_TASKS, newContext.get(SPARQLExt.CLOSE_TASKS, new HashSet<>()));
        newContext.set(SPARQLExt.SIZE, newContext.get(SPARQLExt.SIZE, 0));
        newContext.set(SPARQLExt.LIST_NODES, newContext.get(SPARQLExt.LIST_NODES, new HashMap<>()));
        newContext.set(SPARQLExt.DEBUG_ST_CONCAT, newContext.get(SPARQLExt.DEBUG_ST_CONCAT, false));
        return newContext;
    }

    public static synchronized Context createContext(Context context) {
        PrefixMapping pm = (PrefixMapping) context.get(SPARQLExt.PREFIX_MANAGER, PrefixMapping.Standard);
        return createContext(context, pm);
    }

    /**
     * Create a new context from an existing one, for a given number of
     * solutions.
     *
     * @param context
     * @param size
     * @return
     */
    public static synchronized Context forkContext(Context context, int size) {
        context = new Context(context);
        context.set(SPARQLExt.SIZE, size);
        context.set(SPARQLExt.LIST_NODES, new HashMap<>());
        return context;
    }

    public static synchronized boolean alreadyExecuted(
            final Context context,
            final String queryName,
            final List<Binding> currentCall) {
        final Map<String, Set<List<Binding>>> calls
                = (Map<String, Set<List<Binding>>>) context.get(SPARQLExt.EXECUTION_CALLS);
        if (!calls.containsKey(queryName)) {
            return false;
        }
        for (List<Binding> previousCall : calls.get(queryName)) {
            if (previousCall.containsAll(currentCall) && currentCall.containsAll(previousCall)) {
                return true;
            }
        }
        return false;
    }

    public static synchronized void registerExecution(
            final Context context,
            final String queryName,
            final List<Binding> values) {
        final Map<String, Set<List<Binding>>> calls
                = (Map<String, Set<List<Binding>>>) context.get(SPARQLExt.EXECUTION_CALLS);
        if (!calls.containsKey(queryName)) {
            calls.put(queryName, new HashSet<>());
        }
        calls.get(queryName).add(values);
    }

    public static synchronized void addTaskOnClose(
            final Context context,
            final Runnable task) {
        final Set<Runnable> closeTasks
                = (Set<Runnable>) context.get(SPARQLExt.CLOSE_TASKS);
        closeTasks.add(task);
    }

    public static void close(Context context) {
        try {
            LOG.info("Closing context");
            final Set<Runnable> closeTasks
                    = (Set<Runnable>) context.get(SPARQLExt.CLOSE_TASKS);
            closeTasks.forEach(Runnable::run);
        } catch (Exception ex) {
            LOG.warn("Exception while closing context:", ex);
        }
    }

    /**
     * get the node at position i in the LIST( expr ). Or rdf:nil if the
     * position is equal to the number of bindings
     *
     * @param list
     * @param context
     * @param position
     * @return
     */
    public static synchronized Node getNode(
            final Context context,
            final Node_List list,
            int position) {
        return getInfo(context, list)[position];
    }

    public static synchronized Node[] getInfo(
            final Context context,
            final Node_List list) {
        final int size = (Integer) context.get(SPARQLExt.SIZE);
        final Map<Node_List, Node[]> listNodes
                = (Map<Node_List, Node[]>) context.get(SPARQLExt.LIST_NODES);
        if (!listNodes.containsKey(list)) {
            Node[] nodes = new Node[size + 1];
            for (int i = 0; i < size; i++) {
                nodes[i] = NodeFactory.createBlankNode();
            }
            nodes[size] = RDF.nil.asNode();
            listNodes.put(list, nodes);
        }
        return listNodes.get(list);
    }

    public static synchronized Var allocVar(
            String label,
            final Context context) {
        final Map<String, Var> vars
                = (Map<String, Var>) context.get(SPARQLExt.VARS);
        if (!vars.containsKey(label)) {
            vars.put(label, Var.alloc(label));
        }
        return vars.get(label);
    }

    public static List<Var> getVariables(
            final QuerySolution sol,
            final Context context) {
        final List<Var> variables = new ArrayList<>();
        for (Iterator<String> it = sol.varNames(); it.hasNext();) {
            variables.add(allocVar(it.next(), context));
        }
        return variables;
    }

    public static List<Var> getVariables(
            final List<String> varNames,
            final Context context) {
        return varNames.stream().map(v -> allocVar(v, context)).collect(Collectors.toList());
    }

    public static Binding getBinding(
            final QuerySolution sol,
            final Context context) {
        final BindingMap binding = BindingFactory.create();
        for (Iterator<String> it = sol.varNames(); it.hasNext();) {
            String varName = it.next();
            binding.add(allocVar(varName, context), sol.get(varName).asNode());
        }
        return binding;
    }

    public static String getIndentControl(Context context) {
        return (String) context.get(SPARQLExt.INDENT_CONTROL);
    }

    public static int getIndent(Context context) {
        if (!context.isDefined(SPARQLExt.INDENT)) {
            context.set(SPARQLExt.INDENT, 0);
            return 0;
        } else {
            return (Integer) context.get(SPARQLExt.INDENT);
        }
    }

    public static void updateIndent(Context context, String incrString) {
        try {
            int indent = getIndent(context);
            indent += Integer.parseInt(incrString);
            context.set(SPARQLExt.INDENT, indent);
        } catch (Exception ex) {
            throw new SPARQLExtException("The two characters that follow the indentation control sequence " + getIndentControl(context) + " is not an integer. Got " + incrString);
        }
    }
    
    public static void setDebugStConcat(Context context, boolean debugStConcat) {
        context.set(SPARQLExt.DEBUG_ST_CONCAT, debugStConcat);
    }

    public static boolean isDebugStConcat(Context context) {
        return (Boolean) context.get(SPARQLExt.DEBUG_ST_CONCAT, false);
    }

    private static final Var VAR = Var.alloc("truncated");

    public static Dataset loadDataset(File dir, Request request) {
        Dataset ds = DatasetFactory.create();
        String dgfile = request.graph != null ? request.graph : "dataset/default.ttl";
        try {
            ds.setDefaultModel(RDFDataMgr.loadModel(new File(dir, dgfile).toString(), Lang.TTL));
        } catch (Exception ex) {
            LOG.debug("No default graph provided: " + ex.getMessage());
        }

        if (request.namedgraphs == null) {
            return ds;
        }

        request.namedgraphs.forEach((ng) -> {
            try {
                Model model = RDFDataMgr.loadModel(new File(dir, ng.path).toString(), Lang.TTL);
                ds.addNamedModel(ng.uri, model);
            } catch (Exception ex) {
                LOG.debug("Cannot load named graph " + ng.path + ": " + ex.getMessage());
            }
        });

        return ds;
    }

    public static String log(List<Var> variables, List<Binding> input) {
        return FormatterElement.asString(compress(variables, input));
    }

    public static String log(List<Binding> input) {
        return FormatterElement.asString(compress(input));
    }

    private static ElementData compress(List<Binding> input) {
        ElementData el = new ElementData();
        
        if (input.size() < 10) {
            input.forEach((b) -> {
                addCompressedToElementData(el, b);
            });
            return el;
        }
        for (int i = 0; i < 5; i++) {
            addCompressedToElementData(el, input.get(i));
        }
        BindingMap binding = BindingFactory.create();
        Node n = NodeFactory.createLiteral("[ " + (input.size() - 10) + " more ]");
        el.getVars().forEach((v) -> binding.add(v, n));
        el.add(binding);
        for (int i = input.size() - 5; i < input.size(); i++) {
            addCompressedToElementData(el, input.get(i));
        }
        return el;
    }


    private static void addCompressedToElementData(ElementData el, Binding b) {
        final Binding compressed = compress(b);
        final Iterator<Var> varsIterator = compressed.vars();
        while (varsIterator.hasNext()) {
            el.add(varsIterator.next());
        }
        el.add(compressed);
    }

    private static ElementData compress(List<Var> variables, List<Binding> input) {
        ElementData el = new ElementData();
        variables.forEach(el::add);
        if (input.size() < 10) {
            input.forEach((b) -> el.add(SPARQLExt.compress(variables, b)));
            return el;
        }
        for (int i = 0; i < 5; i++) {
            el.add(compress(variables, input.get(i)));
        }
        BindingMap binding = BindingFactory.create();
        Node n = NodeFactory.createLiteral("[ " + (input.size() - 10) + " more ]");
        variables.forEach((v) -> binding.add(v, n));
        el.add(binding);
        for (int i = input.size() - 5; i < input.size(); i++) {
            el.add(compress(variables, input.get(i)));
        }
        return el;
    }

    public static Binding compress(Binding input) {
        final List<Var> vars = new ArrayList<>();
        final Iterator<Var> varsIterator = input.vars();
        while (varsIterator.hasNext()) {
            vars.add(varsIterator.next());
        }
        return compress(vars, input);
    }

    public static Binding compress(List<Var> variables, Binding input) {
        final BindingMap binding = BindingFactory.create();
        for (Var v : variables) {
            Node n = input.get(v);
            if (n != null) {
                binding.add(v, compress(input.get(v)));
            }
        }
        return binding;
    }

    public static Triple compress(Triple t) {
        Node o = compress(t.getObject());
        return new Triple(t.getSubject(), t.getPredicate(), o);
    }

    public static Node compress(Node n) {
        if (n.isLiteral()) {
            n = NodeFactory.createLiteral(compress(n.getLiteralLexicalForm()), n.getLiteralLanguage(), n.getLiteralDatatype());
        }
        return n;
    }

    public static String compress(String s) {
        if (s.length() > 60) {
            s = s.substring(0, 40) + "\n"
                    + " ... " + s.substring(s.length() - 15);
        }
        return s;
    }

}
