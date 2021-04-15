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
package fr.mines_stetienne.ci.sparql_generate;

import java.util.ServiceLoader;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.query.Syntax;
import org.apache.jena.riot.LangBuilder;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.stream.StreamManager;
import org.apache.jena.sparql.SystemARQ;
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

import fr.mines_stetienne.ci.sparql_generate.function.FunctionLoader;
import fr.mines_stetienne.ci.sparql_generate.function.library.FUN_CamelCase;
import fr.mines_stetienne.ci.sparql_generate.function.library.FUN_Log;
import fr.mines_stetienne.ci.sparql_generate.function.library.FUN_MixedCase;
import fr.mines_stetienne.ci.sparql_generate.function.library.FUN_PrefixedIRI;
import fr.mines_stetienne.ci.sparql_generate.function.library.FUN_Property;
import fr.mines_stetienne.ci.sparql_generate.function.library.FUN_Select_Call_Template;
import fr.mines_stetienne.ci.sparql_generate.function.library.FUN_SplitAtPostion;
import fr.mines_stetienne.ci.sparql_generate.function.library.FUN_TitleCase;
import fr.mines_stetienne.ci.sparql_generate.function.library.FUN_dateTime;
import fr.mines_stetienne.ci.sparql_generate.function.library.FUN_regex;
import fr.mines_stetienne.ci.sparql_generate.function.library.ST_Call_Template;
import fr.mines_stetienne.ci.sparql_generate.function.library.ST_Concat;
import fr.mines_stetienne.ci.sparql_generate.function.library.ST_Decr;
import fr.mines_stetienne.ci.sparql_generate.function.library.ST_Format;
import fr.mines_stetienne.ci.sparql_generate.function.library.ST_Incr;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionLoader;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionRegistry;
import fr.mines_stetienne.ci.sparql_generate.iterator.library.ITER_Call_Select;
import fr.mines_stetienne.ci.sparql_generate.iterator.library.ITER_DefaultGraphNamespaces;
import fr.mines_stetienne.ci.sparql_generate.iterator.library.ITER_HTTPGet;
import fr.mines_stetienne.ci.sparql_generate.iterator.library.ITER_Split;
import fr.mines_stetienne.ci.sparql_generate.iterator.library.ITER_for;
import fr.mines_stetienne.ci.sparql_generate.iterator.library.ITER_regex;
import fr.mines_stetienne.ci.sparql_generate.lang.ParserSPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.serializer.SPARQLExtQuerySerializer;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.WktLiteral;

/**
 * The configuration entry point of SPARQL-Generate. Method {@link #init()} must
 * be called before anything else.
 * 
 * @author Maxime Lefrançois
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
	public static final String MEDIA_TYPE_URI = "https://www.iana.org/assignments/media-types/" + MEDIA_TYPE;

	/**
	 * The namespace of SPARQL-Generate, also the root of SPARQL-Generate-defined
	 * parameter names
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

	/**
	 * The iterators library registry key.
	 */
	public static final Symbol REGISTRY_ITERATORS = SystemARQ.allocSymbol(NS, "registryIterators");

	/**
	 * Forces the initialization of SPARQL-Generate.
	 */
	public static void init() {
	}

	static final Logger LOG = LoggerFactory.getLogger(SPARQLExt.class);

	static {
		SYNTAX = new SPARQLGenerateSyntax(SYNTAX_URI);

		FunctionRegistry fnreg = FunctionRegistry.get();
		fnreg.put(FUN_SplitAtPostion.URI, FUN_SplitAtPostion.class);
		fnreg.put(FUN_regex.URI, FUN_regex.class);
		fnreg.put(FUN_dateTime.URI, FUN_dateTime.class);
		fnreg.put(FUN_Property.URI, FUN_Property.class);
		fnreg.put(FUN_CamelCase.URI, FUN_CamelCase.class);
		fnreg.put(FUN_MixedCase.URI, FUN_MixedCase.class);
		fnreg.put(FUN_TitleCase.URI, FUN_TitleCase.class);
		fnreg.put(FUN_PrefixedIRI.URI, FUN_PrefixedIRI.class);
		fnreg.put(FUN_Select_Call_Template.URI, FUN_Select_Call_Template.class);
		fnreg.put(FUN_Log.URI, FUN_Log.class);

		final ServiceLoader<FunctionLoader> functionLoaders = ServiceLoader.load(FunctionLoader.class);
		functionLoaders.forEach((loader) -> {
			loader.load(fnreg);
		});

		fnreg.put(ST_Call_Template.URI, ST_Call_Template.class);
		fnreg.put(ST_Decr.URI, ST_Decr.class);
		fnreg.put(ST_Incr.URI, ST_Incr.class);
		fnreg.put(ST_Concat.URI, ST_Concat.class);
		fnreg.put(ST_Format.URI, ST_Format.class);

		IteratorFunctionRegistry itereg = IteratorFunctionRegistry.get();
		itereg.put(ITER_regex.URI, ITER_regex.class);
		itereg.put(ITER_Split.URI, ITER_Split.class);
		itereg.put(ITER_for.URI, ITER_for.class);
		itereg.put(ITER_HTTPGet.URI, ITER_HTTPGet.class);
		itereg.put(ITER_DefaultGraphNamespaces.URI, ITER_DefaultGraphNamespaces.class);
		itereg.put(ITER_Call_Select.URI, ITER_Call_Select.class);

		final ServiceLoader<IteratorFunctionLoader> iteratorFunctionLoaders = ServiceLoader
				.load(IteratorFunctionLoader.class);
		iteratorFunctionLoaders.forEach((loader) -> {
			loader.load(itereg);
		});

		SPARQLParserRegistry.get().add(SYNTAX, new SPARQLParserFactory() {
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
				SerializationContext context = new SerializationContext(prologue, new NodeToLabelMapBNode("bn", false));
				return new SPARQLExtQuerySerializer(writer, context);
			}

			@Override
			public QueryVisitor create(Syntax syntax, SerializationContext context, IndentedWriter writer) {
				return new SPARQLExtQuerySerializer(writer, context);
			}
		};

		SerializerRegistry registry = SerializerRegistry.get();
		registry.addQuerySerializer(SPARQLExt.SYNTAX, factory);

//        RDFLanguages.unregister(Lang.RDFXML);
//        RDFLanguages.register(LangBuilder.create(strLangRDFXML, contentTypeRDFXML)
//                .addAltNames("RDFXML", "RDF/XML-ABBREV", "RDFXML-ABBREV")
//                .addFileExtensions("rdf", "owl")
//                .build());
		RDFLanguages.register(LangBuilder.create("XML", "application/xml").addFileExtensions("xml").build());
		RDFLanguages.register(LangBuilder.create("JSON", "application/json").addFileExtensions("json").build());
		RDFLanguages.register(LangBuilder.create("HTML", "text/html").addFileExtensions("html", "xhtml").build());
		RDFLanguages.register(LangBuilder.create("XHTML", "application/xhtml+xml").addFileExtensions("xhtml").build());

		TypeMapper.getInstance().registerDatatype(WktLiteral.wktLiteralType);

		StreamManager.setGlobal(SPARQLExtStreamManager.makeStreamManager());
	}

	/**
	 * This class must be used instead of class <code>Syntax</code>.
	 *
	 * @author Maxime Lefrançois
	 */
	public static class SPARQLGenerateSyntax extends Syntax {

		/**
		 * @param the name of the syntax
		 */
		public SPARQLGenerateSyntax(final String syntax) {
			super(syntax);
		}

		/**
		 *
		 */
		private final static TranslationTable<Syntax> generateSyntaxNames = new TranslationTable<>(true);

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
		 * @param url           the url of the syntax
		 * @param defaultSyntax a default syntax
		 * @return an available syntax for the filename
		 */
		public static Syntax guessFileSyntax(final String url, final Syntax defaultSyntax) {
			if (url.endsWith(".rqg")) {
				return SYNTAX;
			}
			return Syntax.guessFileSyntax(url, defaultSyntax);
		}

	}
}
