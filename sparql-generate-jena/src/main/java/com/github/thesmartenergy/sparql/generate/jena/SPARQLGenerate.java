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

import com.github.thesmartenergy.sparql.generate.jena.function.library.*;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionRegistry;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.*;
import com.github.thesmartenergy.sparql.generate.jena.lang.ParserSPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.serializer.SPARQLGenerateQuerySerializer;
import com.github.thesmartenergy.sparql.generate.jena.stream.SPARQLGenerateStreamManager;
import com.github.thesmartenergy.sparql.generate.jena.utils.WktLiteral;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.query.Syntax;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.LangBuilder;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.apache.jena.riot.RDFLanguages.strLangRDFXML;
import static org.apache.jena.riot.WebContent.contentTypeRDFXML;

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
     * The namespace of SPARQL-Generate functions.
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
     * Force the initialization of SPARQL-Generate.
     */
    public static void init() {
    }

    static final Logger log = LoggerFactory.getLogger(SPARQLGenerate.class);

    static {
        log.trace("initializing SPARQLGenerate");

        SYNTAX = new SPARQLGenerateSyntax(SYNTAX_URI);

        FunctionRegistry fnreg = FunctionRegistry.get();
        fnreg.put(FUN_JSONPath.URI, FUN_JSONPath.class);
        fnreg.put(FUN_XPath.URI, FUN_XPath.class);
        fnreg.put(FUN_CSV.URI, FUN_CSV.class);
        fnreg.put(FUN_CustomCSV.URI, FUN_CustomCSV.class);
        fnreg.put(FUN_SplitAtPostion.URI, FUN_SplitAtPostion.class);
        fnreg.put(FUN_HTMLTag.URI, FUN_HTMLTag.class);
        fnreg.put(FUN_HTMLAttribute.URI, FUN_HTMLAttribute.class);
        fnreg.put(FUN_CBOR.URI, FUN_CBOR.class);
        fnreg.put(FUN_regex.URI, FUN_regex.class);
        fnreg.put(FUN_bnode.URI, FUN_bnode.class);
        fnreg.put(FUN_HTMLTagElement.URI, FUN_HTMLTagElement.class);
        fnreg.put(FUN_dateTime.URI, FUN_dateTime.class);
        fnreg.put(FUN_GeoJSONGeometry.URI, FUN_GeoJSONGeometry.class);
        fnreg.put(FUN_GeoCSVasWKT.URI, FUN_GeoCSVasWKT.class);

        IteratorFunctionRegistry itereg = IteratorFunctionRegistry.get();
        itereg.put(ITER_JSONPath.URI, ITER_JSONPath.class);
        itereg.put(ITER_JSONListKeys.URI, ITER_JSONListKeys.class);
        itereg.put(ITER_JSONElement.URI, ITER_JSONElement.class);
        itereg.put(ITER_regex.URI, ITER_regex.class);
        itereg.put(ITER_XPath.URI, ITER_XPath.class);
        itereg.put(ITER_Split.URI, ITER_Split.class);
        itereg.put(ITER_CSV.URI, ITER_CSV.class);
        itereg.put(ITER_CustomCSV.URI, ITER_CustomCSV.class);
        itereg.put(ITER_CSVFirstRow.URI, ITER_CSVFirstRow.class);
        itereg.put(ITER_CSVWrapped.URI, ITER_CSVWrapped.class);
        itereg.put(ITER_CSSPath.URI, ITER_CSSPath.class);
        itereg.put(ITER_CBOR.URI, ITER_CBOR.class);
        itereg.put(ITER_CSVHeaders.URI, ITER_CSVHeaders.class);
        itereg.put(ITER_CSVStream.URI, ITER_CSVStream.class);
        itereg.put(ITER_for.URI, ITER_for.class);
        itereg.put(ITER_CSVMultipleOutput.URI, ITER_CSVMultipleOutput.class);
        itereg.put(ITER_GeoJSONFeatures.URI, ITER_GeoJSONFeatures.class);
        itereg.put(ITER_regexgroups.URI, ITER_regexgroups.class);
        itereg.put(ITER_GeoJSON.URI, ITER_GeoJSON.class);
        itereg.put(ITER_PeriodicHTTP.URI, ITER_PeriodicHTTP.class);
        itereg.put(ITER_PeriodicMQTT.URI, ITER_PeriodicMQTT.class);
        itereg.put(ITER_StreamWebSocket.URI, ITER_StreamWebSocket.class);


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

        TypeMapper.getInstance().registerDatatype(WktLiteral.wktLiteralType);

        StreamManager.setGlobal(SPARQLGenerateStreamManager.makeStreamManager());
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
         * @param url           the url of the syntax
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

    private static final Map<Thread, SPARQLGenerateStreamManager> streamManagers = new HashMap<>();

    public static SPARQLGenerateStreamManager getStreamManager() {
        if (streamManagers.containsKey(Thread.currentThread())) {
            log.trace("Using stream manager for " + Thread.currentThread());
            SPARQLGenerateStreamManager sm = streamManagers.get(Thread.currentThread());
            StreamManager.setGlobal(sm);
            return sm;
        }
        try {
            log.trace("Using default stream manager.");
            return (SPARQLGenerateStreamManager) StreamManager.get();
        } catch (ClassCastException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public static void setStreamManager(SPARQLGenerateStreamManager streamManager) {
        Objects.requireNonNull(streamManager);
        streamManagers.put(Thread.currentThread(), streamManager);
        StreamManager.setGlobal(streamManager);
    }

}
