/*
 * Copyright 2016 ITEA 12004 SEAS Project.
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
package org.w3id.sparql.generate;

import java.util.HashMap;
import java.util.Map;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.function.FunctionRegistry;
import org.apache.jena.sparql.lang.SPARQLParser;
import org.apache.jena.sparql.lang.SPARQLParserFactory;
import org.apache.jena.sparql.lang.SPARQLParserRegistry;
import org.w3id.sparql.generate.function.library.FN_JSONPath;
import org.w3id.sparql.generate.function.library.FN_XPath;
import org.w3id.sparql.generate.lang.ParserSPARQLGenerate;
import org.w3id.sparql.generate.query.SPARQLGenerateSyntax;
import org.w3id.sparql.generate.selector.SelectorRegistry;
import org.w3id.sparql.generate.selector.library.SEL_JSONPath;
import org.w3id.sparql.generate.selector.library.SEL_XPath;
import org.w3id.sparql.generate.serializer.SPARQLGenerateQuerySerializer;

/**
 *
 * @author maxime.lefrancois
 */
public class SPARQLGenerate {

    public static final String NS = "http://w3id.org/sparql-generate/";
    
    public static final String FN = NS + "fn/";
    public static final String SEL = NS + "sel/";
    public static final String SYNTAX = NS + "syntax";
    
    public static final Map<String, String> MAP_LOCAL = new HashMap<>();
    
    private static boolean init = false;

    public static void init() {
        if(init) {
           return; 
        }
        
        FunctionRegistry.get().put(FN + "JSONPath_jayway_string", FN_JSONPath.class);
        FunctionRegistry.get().put(FN + "XPath_string", FN_XPath.class);
        
        SelectorRegistry.get().put(SEL + "JSONPath_jayway", SEL_JSONPath.class);
        SelectorRegistry.get().put(SEL + "XPath", SEL_XPath.class);

        SPARQLParserRegistry.get().add(SPARQLGenerateSyntax.syntaxSPARQLGenerate, new SPARQLParserFactory() {
            @Override
            public boolean accept(Syntax syntax) {
                return SPARQLGenerateSyntax.syntaxSPARQLGenerate.equals(syntax);
            }

            @Override
            public SPARQLParser create(Syntax syntax) {
                return new ParserSPARQLGenerate();
            }
        });

        // Register standard serializers
        SPARQLGenerateQuerySerializer.init();
        
        init = true;    
    }

}
