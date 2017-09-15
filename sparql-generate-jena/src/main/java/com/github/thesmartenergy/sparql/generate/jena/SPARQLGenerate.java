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
package com.github.thesmartenergy.sparql.generate.jena;

import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_BNode2;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_CBOR;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_CSV;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_CustomCSV;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_DateTime;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_HTMLAttribute;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_HTMLTag;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_HTMLTagElement;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.function.FunctionRegistry;
import org.apache.jena.sparql.lang.SPARQLParser;
import org.apache.jena.sparql.lang.SPARQLParserFactory;
import org.apache.jena.sparql.lang.SPARQLParserRegistry;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.sparql.util.TranslationTable;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_JSONPath;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_Regex;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_SplitAtPostion;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_XPath;
import com.github.thesmartenergy.sparql.generate.jena.lang.ParserSPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionRegistry;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_CBOR;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_CSSPath;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_CSV;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_CSVFirstRow;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_CSVWrapped;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_CSVHeaders;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_CSVStream;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_CustomCSV;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_JSONListElement;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_JSONListKeys;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_JSONPath;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_Regex;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_Split;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_XPath;
import com.github.thesmartenergy.sparql.generate.jena.locator.LocatorClassLoaderAccept;
import com.github.thesmartenergy.sparql.generate.jena.locator.LocatorFileAccept;
import com.github.thesmartenergy.sparql.generate.jena.locator.LocatorURLAccept;
import com.github.thesmartenergy.sparql.generate.jena.serializer.SPARQLGenerateQuerySerializer;
import java.util.Iterator;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.LangBuilder;
import org.apache.jena.riot.RDFLanguages;
import static org.apache.jena.riot.RDFLanguages.strLangRDFXML;
import static org.apache.jena.riot.WebContent.contentTypeRDFXML;
import org.apache.jena.riot.system.stream.JenaIOEnvironment;
import org.apache.jena.riot.system.stream.LocationMapper;
import org.apache.jena.riot.system.stream.Locator;
import org.apache.jena.riot.system.stream.StreamManager;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.serializer.QuerySerializerFactory;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.serializer.SerializerRegistry;
import org.apache.jena.sparql.util.NodeToLabelMapBNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The configuration entry point of SPARQL-Generate. Method {@link #init()} must
 * be called operated prior any further operation.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public final class SPARQLGenerate {

    /**
     * Private constructor. No instance should be created.
     */
    private SPARQLGenerate() {
    }

    /**
     * The SPARQL Generate media type.
     */
    public static final String MEDIA_TYPE = "application/vnd.sparql-generate";

    /**
     * The SPARQL Generate media type URI.
     */
    public static final String MEDIA_TYPE_URI = "http://www.iana.org/assignments/media-types/" + MEDIA_TYPE;

    /**
     * The namespace of SPARQL Generate.
     */
    public static final String NS = "http://w3id.org/sparql-generate/";

    /**
     * The namespace of SPARQL Generate functions.
     */
    public static final String FN = NS + "fn/";

    /**
     * The namespace of SPARQL Generate iterator functions.
     */
    public static final String ITER = NS + "iter/";

    /**
     * The URI of the SPARQL Generate syntax.
     */
    public static final String SYNTAX_URI = NS + "syntax";

    /**
     * The SPARQL-Generate syntax.
     */
    public static final Syntax SYNTAX;
    
    /**
     * Force the initialization of SPARQL-Generate.
     */
    public static void init() {
    }

    static final Logger log = LogManager.getLogger(SPARQLGenerate.class);

    static {
        log.trace("initializing SPARQLGenerate");
        
        SYNTAX = new SPARQLGenerateSyntax(SYNTAX_URI);

        FunctionRegistry fnreg = FunctionRegistry.get();
        fnreg.put(FN_JSONPath.URI, FN_JSONPath.class);
        fnreg.put(FN_XPath.URI, FN_XPath.class);
        fnreg.put(FN_CSV.URI, FN_CSV.class);
        fnreg.put(FN_CustomCSV.URI, FN_CustomCSV.class);
        fnreg.put(FN_SplitAtPostion.URI, FN_SplitAtPostion.class);
        fnreg.put(FN_HTMLTag.URI, FN_HTMLTag.class);
        fnreg.put(FN_HTMLAttribute.URI, FN_HTMLAttribute.class);
        fnreg.put(FN_CBOR.URI, FN_CBOR.class);
        fnreg.put(FN_Regex.URI, FN_Regex.class);
        fnreg.put(FN_BNode2.URI, FN_BNode2.class);
        fnreg.put(FN_HTMLTagElement.URI, FN_HTMLTagElement.class);
        fnreg.put(FN_DateTime.URI, FN_DateTime.class);

        
        IteratorFunctionRegistry itereg = IteratorFunctionRegistry.get();
        itereg.put(ITE_JSONPath.URI, ITE_JSONPath.class);
        itereg.put(ITE_JSONListKeys.URI, ITE_JSONListKeys.class);
        itereg.put(ITE_JSONListElement.URI, ITE_JSONListElement.class);
        itereg.put(ITE_Regex.URI, ITE_Regex.class);
        itereg.put(ITE_XPath.URI, ITE_XPath.class);
        itereg.put(ITE_Split.URI, ITE_Split.class);
        itereg.put(ITE_CSV.URI, ITE_CSV.class);
        itereg.put(ITE_CustomCSV.URI, ITE_CustomCSV.class);
        itereg.put(ITE_CSVFirstRow.URI, ITE_CSVFirstRow.class);
        itereg.put(ITE_CSVWrapped.URI, ITE_CSVWrapped.class);
        itereg.put(ITE_CSSPath.URI, ITE_CSSPath.class);
        itereg.put(ITE_CBOR.URI, ITE_CBOR.class);
        itereg.put(ITE_CSVHeaders.URI, ITE_CSVHeaders.class);
        itereg.put(ITE_CSVStream.URI, ITE_CSVStream.class);

        SPARQLParserRegistry.get()
                .add(SYNTAX, new SPARQLParserFactory() {
                    @Override
                    public boolean accept(final Syntax syntax) {
                        return SYNTAX.equals(syntax);
                    }

                    @Override
                    public SPARQLParser create(final Syntax syntax) {
                        return new ParserSPARQLGenerate();
                    }
                });

        QuerySerializerFactory factory = new QuerySerializerFactory() {
            @Override
            public boolean accept(Syntax syntax) {
                // Since ARQ syntax is a super set of SPARQL 1.1 both SPARQL 1.0
                // and SPARQL 1.1 can be serialized by the same serializer
                return Syntax.syntaxARQ.equals(syntax) || Syntax.syntaxSPARQL_10.equals(syntax)
                        || Syntax.syntaxSPARQL_11.equals(syntax) || SPARQLGenerate.SYNTAX.equals(syntax);
            }

            @Override
            public QueryVisitor create(Syntax syntax, Prologue prologue, IndentedWriter writer) {
                SerializationContext context = new SerializationContext(prologue, new NodeToLabelMapBNode("g", false));
                return new SPARQLGenerateQuerySerializer(writer, context);
            }

            @Override
            public QueryVisitor create(Syntax syntax, SerializationContext context, IndentedWriter writer) {
                return new SPARQLGenerateQuerySerializer(writer, context);
            }
        };

        SerializerRegistry registry = SerializerRegistry.get();
        registry.addQuerySerializer(SPARQLGenerate.SYNTAX, factory);
        
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
        getStreamManager(true);
    }

    /**
     * This class must be used instead of class <code>Syntax</code>.
     *
     * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
     */
    public static class SPARQLGenerateSyntax extends Syntax {

        /**
         *
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

    public static StreamManager getStreamManager() {
        return getStreamManager(false);
    }

    public static StreamManager getStreamManager(boolean fresh) {
        if (fresh) {
            resetStreamManager((Locator) null);
        }
        return StreamManager.get();
    }

    public static void resetStreamManager(Locator locator) {
        StreamManager sm = new StreamManager();
        if(locator != null) {
            sm.addLocator(locator);
        }
        sm.addLocator(new LocatorFileAccept(null));
        sm.addLocator(new LocatorClassLoaderAccept(StreamManager.class.getClassLoader())) ;
        sm.setLocationMapper(JenaIOEnvironment.getLocationMapper()) ;
        sm.addLocator(new LocatorURLAccept());
        StreamManager.setGlobal(sm);
    }

    public static StreamManager resetStreamManager(Model configurationModel) {
        StreamManager sm = getStreamManager(true);
        if (configurationModel == null) {
            return sm;
        }

        org.apache.jena.util.LocationMapper old = new org.apache.jena.util.LocationMapper(configurationModel);
        LocationMapper mapper = new LocationMapper();
        Iterator<String> altEntries = old.listAltEntries();
        while (altEntries.hasNext()) {
            String uri = altEntries.next();
            String entry = old.getAltEntry(uri);
            if(!uri.startsWith("accept:")) {
                uri = "accept:*/*:" + uri;
            }
            if(!entry.startsWith("accept:")) {
                entry = "accept:*/*:" + uri;
            }
            mapper.addAltEntry(uri, entry);
        }

        Iterator<String> altPrefixes = old.listAltPrefixes();
        while (altPrefixes.hasNext()) {
            String uri = altPrefixes.next();
            mapper.addAltPrefix(uri, old.getAltPrefix(uri));
        }
        StreamManager.get().setLocationMapper(mapper);

        return sm;
    }

}
