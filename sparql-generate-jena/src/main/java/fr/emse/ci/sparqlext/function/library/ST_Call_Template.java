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
package fr.emse.ci.sparqlext.function.library;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.engine.QueryExecutor;
import fr.emse.ci.sparqlext.utils.ContextUtils;
import fr.emse.ci.sparqlext.utils.EvalUtils;
import fr.emse.ci.sparqlext.utils.ST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * https://ns.inria.fr/sparql-template/ sec. 4.3 A named template is called by
 * name with parameter values using the st:call-template function. When several
 * parameters occur, parameter passing is done by position (i.e. not by name).
 *
 * @author Maxime Lefrançois
 */
public class ST_Call_Template implements Function {

    private static final Logger LOG = LoggerFactory.getLogger(ST_Call_Template.class);

    public static String URI = ST.callTemplate;

    @Override
    public final void build(String uri, ExprList args) {
        if (args.size() < 1) {
            throw new ExprEvalException("Expecting at least one argument");
        }
    }

    /**
     *
     * @param binding
     * @param args
     * @param uri
     * @param env
     * @return
     */
    @Override
    public NodeValue exec(
            final Binding binding,
            final ExprList args,
            final String uri,
            final FunctionEnv env) {
        if (args == null) {
            throw new ARQInternalErrorException("FunctionBase: Null args list");
        }
        if (args.size() < 1) {
            throw new ExprEvalException("Expecting at least one argument");
        }
        NodeValue queryNode = args.get(0).eval(binding, env);
        if (!(queryNode.isIRI() || queryNode.isLiteral() && SPARQLExt.MEDIA_TYPE_URI.equals(queryNode.getDatatypeURI()))) {
            throw new ExprEvalException("Name of sub query "
                    + "should be a URI or a literal with datatype " + SPARQLExt.MEDIA_TYPE_URI + ". Got: " + queryNode);
        }
        if (queryNode.isLiteral() && args.size() > 1) {
            throw new ExprEvalException("Expecting at most one argument when first argument is a literal.");
        }

        final Context context = env.getContext();
        final QueryExecutor queryExecutor = ContextUtils.getQueryExecutor(context);
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
        		IndentedWriter writer = new IndentedWriter(baos);) {
        	Context newContext = ContextUtils.fork(context).setTemplateOutput(writer).fork();
            if (queryNode.isIRI()) {
                String queryName = queryNode.asNode().getURI();
                List<List<Node>> callParameters = new ArrayList<>();
                callParameters.add(EvalUtils.eval(args.subList(1, args.size()), binding, env));
                queryExecutor.execTemplateFromName(queryName, callParameters, newContext);
                return new NodeValueString(new String(baos.toByteArray()));
            }
            String queryString = queryNode.asNode().getLiteralLexicalForm();
        	queryExecutor.execTemplateFromString(queryString, binding, newContext);
            String result = new String(baos.toByteArray());
	        writer.close(); // need to flush. One before the other isn't important. boas.close is the only variant that doesn't work.
            return new NodeValueString(result);
        } catch (IOException ex) {
        	throw new ExprEvalException(ex);
		}
    }

}
