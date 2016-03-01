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
package org.w3id.sparql.generate.query;

import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.sparql.util.TranslationTable;
import org.w3id.sparql.generate.SPARQLGenerate;

/**
 *
 * @author maxime.lefrancois
 */
public class SPARQLGenerateSyntax extends Syntax {

    public SPARQLGenerateSyntax(String syntax) {
        super(syntax);
    }
    /**
     * The syntax for SPARQL-Generate
     */
    public static final Syntax syntaxSPARQLGenerate = new SPARQLGenerateSyntax(SPARQLGenerate.SYNTAX);

    public static TranslationTable<Syntax> generateSyntaxNames = new TranslationTable<>(true);

    static {
        generateSyntaxNames.put("sparqlGenerate", syntaxSPARQLGenerate);
    }

    public static Syntax make(String uri) {
        if (uri == null) {
            return null;
        }
        Symbol sym = Symbol.create(uri);
        if (sym.equals(syntaxSPARQLGenerate)) {
            return syntaxSPARQLGenerate;
        }
        return Syntax.make(uri);
    }

    /**
     * Gues the synatx (query and update) based on filename
     */
    public static Syntax guessFileSyntax(String url, Syntax defaultSyntax) {
        if (url.endsWith(".rqg")) {
            return syntaxSPARQLGenerate;
        }
        return Syntax.guessFileSyntax(url, defaultSyntax);
    }

}
