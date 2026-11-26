/*
 * Copyright 2026 MINES Saint-Étienne
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
package fr.mines_stetienne.ci.sparql_generate.iterator.library;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionBase;
import fr.mines_stetienne.ci.sparql_generate.stream.LookUpRequest;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/SPARQL">iter:SPARQL</a>
 * iterates over the solutions of a SPARQL 1.1 SELECT query evaluated on an
 * RDF document: one row per solution.
 *
 * <ul>
 * <li>Param 1: the URI of the RDF document (Turtle, N-Triples, RDF/XML,
 * JSON-LD..., by its media type or its extension), or the document itself in
 * Turtle (a String);</li>
 * <li>Param 2: the SELECT query (a String), with its own prologue;</li>
 * <li>Param 3 .. N: (strings) the names of the variables to output, in order
 * (by default, the projected variables of the query).</li>
 * </ul>
 *
 * The output is the value of each variable (unbound if the solution does not
 * bind it), then the position of the solution (an integer, from 0).
 *
 * <p>
 * <b>Example: </b>
 * <code>ITERATOR iter:SPARQL(&lt;friends.nt>, "PREFIX foaf: &lt;http://xmlns.com/foaf/0.1/> SELECT ?person ?name WHERE { ?person foaf:name ?name }", "person", "name") AS ?person ?name ?row</code>
 * </p>
 *
 * @since 2.6.1
 */
public class ITER_SPARQL extends IteratorFunctionBase {

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLExt.ITER + "SPARQL";

    @Override
    public void checkBuild(ExprList args) {
        Objects.requireNonNull(args);
    }

    @Override
    public List<List<NodeValue>> exec(List<NodeValue> args) {
        if (args.size() < 2 || args.get(0) == null || args.get(1) == null || !args.get(1).isString()) {
            throw new ExprEvalException("Expecting an RDF document and a SELECT query");
        }
        final Query query;
        try {
            query = QueryFactory.create(args.get(1).getString(), Syntax.syntaxSPARQL_11);
        } catch (QueryParseException ex) {
            throw new ExprEvalException("Not a SPARQL query: " + ex.getMessage(), ex);
        }
        if (!query.isSelectType()) {
            throw new ExprEvalException("Not a SELECT query: " + args.get(1).getString());
        }
        final List<Var> vars = new ArrayList<>();
        for (int i = 2; i < args.size(); i++) {
            if (args.get(i) == null || !args.get(i).isString()) {
                throw new ExprEvalException("The names of the variables must be strings, got: " + args.get(i));
            }
            vars.add(Var.alloc(args.get(i).getString()));
        }
        if (vars.isEmpty()) {
            vars.addAll(query.getProjectVars());
        }
        final Model model = ModelFactory.createModelForGraph(load(args.get(0)));
        final List<List<NodeValue>> rows = new ArrayList<>();
        try (QueryExecution exec = QueryExecution.create(query, model)) {
            final ResultSet results = exec.execSelect();
            int position = 0;
            while (results.hasNext()) {
                final Binding binding = results.nextBinding();
                final List<NodeValue> row = new ArrayList<>(vars.size() + 1);
                for (Var var : vars) {
                    final Node node = binding.get(var);
                    row.add(node == null ? null : NodeValue.makeNode(node));
                }
                row.add(new NodeValueInteger(position++));
                rows.add(row);
            }
        }
        return rows;
    }

    private Graph load(final NodeValue document) {
        final Model model = ModelFactory.createDefaultModel();
        try {
            if (document.isIRI()) {
                final String uri = document.asNode().getURI();
                final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) getContext().get(SysRIOT.sysStreamManager);
                Objects.requireNonNull(sm);
                // any media type, as SOURCE: the format is that of the document
                try (TypedInputStream tin = sm.open(new LookUpRequest(uri))) {
                    if (tin == null) {
                        throw new ExprEvalException("Could not look up RDF document " + uri);
                    }
                    Lang lang = tin.getMediaType() == null ? null : RDFLanguages.contentTypeToLang(tin.getMediaType());
                    if (lang == null || lang == RDFLanguages.TEXT) {
                        lang = RDFLanguages.filenameToLang(uri, Lang.TURTLE);
                    }
                    RDFParser.create().source(tin.getInputStream()).base(uri).lang(lang).parse(model);
                }
            } else if (document.isLiteral()) {
                RDFParser.create().fromString(document.asNode().getLiteralLexicalForm()).lang(Lang.TURTLE).parse(model);
            } else {
                throw new ExprEvalException("Expecting the IRI of an RDF document, or a document");
            }
        } catch (RiotException ex) {
            throw new ExprEvalException("Not an RDF document: " + ex.getMessage(), ex);
        }
        return model.getGraph();
    }
}
