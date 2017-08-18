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
package com.github.thesmartenergy.sparql.generate.jena.lang;

import org.apache.jena.sparql.lang.SPARQLParserBase;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import org.apache.jena.query.Query;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Prologue;

/**
 * Class that extends the ARQ SPARQL Parser class with the operations for SPARQL
 * Generate.
 *
 */
public class SPARQLGenerateParserBase extends SPARQLParserBase {

    public static String unescapeStr(String s, int line, int column) {
        s = s.replace("\\{", "{");
        s = s.replace("\\}", "}");
        return unescape(s, '\\', false, line, column);
    }

    /**
     * Constructor.
     */
    @Override
    protected final void startQuery() {
        ((SPARQLGenerateQuery) query).hasEmbeddedExpressions(false);
    }
    
    /**
     * Starts parsing a sub GENERATE query.
     */
    protected final void startSubGenerate() {
        String base = getQuery().getBaseURI();
        PrefixMapping pm = getQuery().getPrefixMapping();
        pushQuery();
        query = new SPARQLGenerateQuery();
        query.setBaseURI(base);
        query.setPrefixMapping(pm);
    }

    /**
     * Starts parsing a sub SELECT query.
     */
    @Override
    protected void startSubSelect(int line, int col) {
        pushQuery();
        query = newSubQuery(getPrologue());
    }

    @Override
    protected Query newSubQuery(Prologue progloue) {
        return new SPARQLGenerateQuery(getPrologue());
    }

    /**
     * Finishes the parsing of a sub GENERATE query.
     *
     * @param line -
     * @param column -
     * @return the sub-generate query.
     */
    protected final SPARQLGenerateQuery endSubGenerate(
            final int line,
            final int column) {
        SPARQLGenerateQuery subQuery = (SPARQLGenerateQuery) query;
        if (!subQuery.isGenerateType()) {
            throwParseException("Subquery not a GENERATE query", line, column);
        }
        popQuery();
        return subQuery;
    }

    /**
     * If possible, cast the query to a SPARQL Generate Query. Else, returns
     * null.
     *
     * @return -
     */
    public final SPARQLGenerateQuery asSPARQLGenerateQuery() {
        if (query instanceof SPARQLGenerateQuery) {
            return (SPARQLGenerateQuery) query;
        }
        return null;
    }

    /**
     * Remove first i and last j characters (e.g. ''' or "{") from a string
     *
     * @param string
     * @return
     */
    public final String stripQuotes(String string, int i, int j) {
        return string.substring(i, string.length() - j);
    }

}
