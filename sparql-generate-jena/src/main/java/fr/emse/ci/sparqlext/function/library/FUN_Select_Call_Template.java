/*
 * Copyright 2019 École des Mines de Saint-Étienne.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.query.ResultSet;
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

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.engine.QueryExecutor;
import fr.emse.ci.sparqlext.engine.RootPlan;
import fr.emse.ci.sparqlext.utils.ContextUtils;
import fr.emse.ci.sparqlext.utils.EvalUtils;

/**
 * Extension of st:call-template where the first parameter is a select * query
 * (a string) the second parameter is the name of a template, the rest are the
 * call parameters
 *
 * @author maxime.lefrancois
 */
public class FUN_Select_Call_Template implements Function {

    private static final Logger LOG = LoggerFactory.getLogger(FUN_Select_Call_Template.class);

    public static String URI = SPARQLExt.FUN + "select-call-template";

    @Override
    public final void build(String uri, ExprList args) {
        if (args.size() < 2) {
            throw new ExprEvalException("Expecting at least two arguments");
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
        if (args.size() < 2) {
            throw new ExprEvalException("Expecting at least two arguments");
        }
        NodeValue selectQueryNode = null;
        if(args.get(0) != null) {
        	selectQueryNode = args.get(0).eval(binding, env);
            if (!(selectQueryNode.isLiteral() && SPARQLExt.MEDIA_TYPE_URI.equals(selectQueryNode.getDatatypeURI()))) {
                throw new ExprEvalException("First argument must be a literal with datatype " + SPARQLExt.MEDIA_TYPE_URI + ". Got: " + selectQueryNode);
            }
        }
        NodeValue templateQueryNode = args.get(1).eval(binding, env);
        if (!(templateQueryNode.isIRI())) {
            throw new ExprEvalException("Second argument must be a URI. Got: " + templateQueryNode);
        }
        ExprList callArgs = args.subList(2, args.size());

        final Context context = env.getContext();
        final QueryExecutor queryExecutor = ContextUtils.getQueryExecutor(context);

        final List<List<Node>> callParameters = new ArrayList<>();
        if(selectQueryNode != null) {
	        RootPlan selectPlan = queryExecutor.getPlanFromString(selectQueryNode.asString(), null);
	        List<Binding> bindings = new ArrayList<>();
	        bindings.add(binding);
	        ResultSet resultSet = selectPlan.execSelect(bindings, context);
	        
	        for(;resultSet.hasNext();) {
	            Binding newBinding = resultSet.nextBinding();
	            callParameters.add(EvalUtils.eval(callArgs, newBinding, env));
	        }
        } else {
            callParameters.add(EvalUtils.eval(callArgs, binding, env));
        }
        String templateQueryName = templateQueryNode.asNode().getURI();
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
        		IndentedWriter writer = new IndentedWriter(baos);) {
	        Context newContext = ContextUtils.fork(context).setTemplateOutput(writer).fork();
	        queryExecutor.execTemplateFromName(templateQueryName, callParameters, newContext);
	        String result = new String(baos.toByteArray());
	        writer.close(); // need to flush. One before the other isn't important. boas.close is the only variant that doesn't work.
	        return new NodeValueString(result);
        } catch (IOException ex) {
        	throw new ExprEvalException(ex);
		}

    }
}
