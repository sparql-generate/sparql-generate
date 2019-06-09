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
package fr.emse.ci.sparqlext.lang;


import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.lang.SPARQLParser;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import java.io.Reader;
import java.io.StringReader;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.lang.SyntaxVarScope;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Class to parse a SPARQL-Generate query. Use preferably ARQ methods to parse
 * queries, but then cast the returned query to a {@link SPARQLExtQuery}. For
 * instance:
 * <pre>
 * {@code
 * SPARQLExtQuery q = (SPARQLExtQuery) QueryFactory.create(query, SPARQLGenerate.SYNTAX);
 * }</pre>
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class ParserSPARQLExt extends SPARQLParser {

    /**
     * The logger.
     */
    private static final Logger LOG
            = LoggerFactory.getLogger(ParserSPARQLExt.class);

    /**
     * Private interface.
     */
    private interface Action {

        void exec(SPARQLExtParser parser) throws Exception;
    }

    /**
     * Parses the query.
     *
     * @param query
     * @param queryString
     * @return
     */
    @Override
    protected Query parse$(final Query query, String queryString) {

        SPARQLExtQuery q = new SPARQLExtQuery();
        q.setSyntax(query.getSyntax());
        q.setResolver(query.getResolver());

        Action action = new Action() {
            @Override
            public void exec(SPARQLExtParser parser) throws Exception {
                parser.SPARQLExtUnit();
            }
        };

        perform(q, queryString, action);
        validateParsedQuery(q);
        return q;
    }

    /**
     * Parses the query.
     *
     * @param query
     * @param queryString
     * @return
     */
    public static SPARQLExtQuery parseSubQuery(final Query query, String queryString) {

        SPARQLExtQuery q = new SPARQLExtQuery();
        q.setSyntax(query.getSyntax());
        q.setResolver(query.getResolver());

        Action action = new Action() {
            @Override
            public void exec(SPARQLExtParser parser) throws Exception {
                parser.SubQueryUnit();
            }
        };

        perform(q, queryString, action);
        return q;
    }
    
    /**
     * Parses the query.
     *
     * @param queryString
     * @return
     */
    public static SPARQLExtQuery parseSubQuery(String queryString) {

        SPARQLExtQuery q = new SPARQLExtQuery();

        Action action = new Action() {
            @Override
            public void exec(SPARQLExtParser parser) throws Exception {
                parser.SubQueryUnit();
            }
        };

        perform(q, queryString, action);
        return q;
    }

    /**
     * Performs the parsing.
     *
     * @param query -
     * @param string -
     * @param action -
     */
    private static void perform(Query query, String string, Action action) {
        Reader in = new StringReader(string);
        SPARQLExtParser parser = new SPARQLExtParser(in);

        try {
            query.setStrict(true);
            parser.setQuery(query);
            action.exec(parser);
        } catch (ParseException ex) {
            QueryParseException e = new QueryParseException(ex.getMessage(),
                    ex.currentToken.beginLine,
                    ex.currentToken.beginColumn);
            LOG.error("", e);
            throw e;
        } catch (TokenMgrError tErr) {
            // Last valid token : not the same as token error message - but this should not happen
            int col = parser.token.endColumn;
            int line = parser.token.endLine;
            QueryParseException e = new QueryParseException(tErr.getMessage(), line, col);
            LOG.error("", e);
            throw e;
        } catch (QueryException ex) {
            LOG.error("", ex);
            throw ex;
        } catch (JenaException ex) {
            QueryException e = new QueryException(ex.getMessage(), ex);
            LOG.error("", e);
            throw e;
        } catch (Error err) {
            // The token stream can throw errors.
            QueryParseException e = new QueryParseException(err.getMessage(), err, -1, -1);
            LOG.error("", e);
            throw e;
        } catch (Throwable th) {
            LOG.error("Unexpected throwable: ", th);
            throw new QueryException(th.getMessage(), th);
        }
    }

    // Do any testing of queries after the construction of the parse tree.
    @Override
    protected void validateParsedQuery(Query query) {
        SyntaxVarScope.check(query);
        if(query instanceof SPARQLExtQuery) {
            SPARQLExtSyntaxVarScope.check((SPARQLExtQuery) query);
        }
    }
}
