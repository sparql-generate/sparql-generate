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

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.lang.SPARQLParser;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import java.io.Reader;
import java.io.StringReader;
import org.apache.log4j.Logger;

/**
 * Class to parse a SPARQL-Generate query. Use preferably ARQ methods to parse
 * queries, but then cast the returned query to a {@link SPARQLGenerateQuery}.
 * For instance:
 * <pre>
 * {@code
 * SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(query, SPARQLGenerate.SYNTAX);
 * }</pre>
 * 
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class ParserSPARQLGenerate extends SPARQLParser {

    /**
     * The logger.
     */
    private static final Logger LOG
            = Logger.getLogger(ParserSPARQLGenerate.class);

    /**
     * Private interface.
     */
    private interface Action {
        void exec(SPARQLGenerateParser parser) throws Exception;
    }

    /**
     * Parses the query.
     * @param query
     * @param queryString
     * @return 
     */
    @Override
    protected Query parse$(final Query query, String queryString) {

        SPARQLGenerateQuery q = new SPARQLGenerateQuery();
        q.setSyntax(query.getSyntax());
        q.setResolver(query.getResolver());

        Action action = new Action() {
            @Override
            public void exec(SPARQLGenerateParser parser) throws Exception {
                parser.GenerateUnit();
            }
        };

        perform(q, queryString, action);
        validateParsedQuery(q);
        return q;
    }

    /**
     * Performs the parsing.
     * @param query -
     * @param string -
     * @param action -
     */
    private static void perform(Query query, String string, Action action) {
        Reader in = new StringReader(string);
        SPARQLGenerateParser parser = new SPARQLGenerateParser(in);

        try {
            query.setStrict(true);
            parser.setQuery(query);
            action.exec(parser);
        } catch (ParseException ex) {
            throw new QueryParseException(ex.getMessage(),
                    ex.currentToken.beginLine,
                    ex.currentToken.beginColumn
            );
        } catch (TokenMgrError tErr) {
            // Last valid token : not the same as token error message - but this should not happen
            int col = parser.token.endColumn;
            int line = parser.token.endLine;
            throw new QueryParseException(tErr.getMessage(), line, col);
        } catch (QueryException ex) {
            throw ex;
        } catch (JenaException ex) {
            throw new QueryException(ex.getMessage(), ex);
        } catch (Error err) {
            // The token stream can throw errors.
            throw new QueryParseException(err.getMessage(), err, -1, -1);
        } catch (Throwable th) {
            LOG.warn("Unexpected throwable: ", th);
            throw new QueryException(th.getMessage(), th);
        }
    }
}
