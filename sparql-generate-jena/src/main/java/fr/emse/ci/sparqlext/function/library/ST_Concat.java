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

import fr.emse.ci.sparqlext.utils.LogUtils;
import fr.emse.ci.sparqlext.utils.ContextUtils;
import fr.emse.ci.sparqlext.utils.ST;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.ExprUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * https://ns.inria.fr/sparql-template/ sec. 4.3 A named template is called by
 * name with parameter values using the st:call-template function. When several
 * parameters occur, parameter passing is done by position (i.e. not by name).
 *
 * @author maxime.lefrancois
 */
public class ST_Concat implements Function {

    private static final Logger LOG = LoggerFactory.getLogger(ST_Concat.class);

    public static final String URI = ST.concat;

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
    public NodeValue exec(Binding binding, ExprList args, String uri, FunctionEnv env) {
        final boolean isDebugStConcat = ContextUtils.isDebugStConcat(env.getContext());
        if (args == null) {
            throw new ARQInternalErrorException("FunctionBase: Null args list");
        }
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
        		IndentedWriter writer = new IndentedWriter(baos);) {
	        Context newContext = ContextUtils.fork(env.getContext()).setTemplateOutput(writer).fork();
	        FunctionEnv newEnv = new FunctionEnvBase(newContext);
	        for (int i = 0; i < args.size(); i++) {
	            Expr expr = args.get(i);
	            try {
	                NodeValue arg = expr.eval(binding, newEnv);
	                writer.print(arg.asString());
	            } catch (Exception ex) {
	                StringWriter sw = new StringWriter();
	                ex.printStackTrace(new PrintWriter(sw));
	                String message = String.format("Error executing st:concat with expression %s and binding %s: %s", ExprUtils.fmtSPARQL(expr), LogUtils.compress(binding).toString(), sw.toString());
	                if (LOG.isDebugEnabled()) {
	                    LOG.debug(message, ex);
	                }
	                if (isDebugStConcat) {
	                    writer.print(String.format("\n<<<<<<<<<< %s >>>>>>>>>>\n", message));
	                }
	            }
	        }
	        writer.close(); // need to flush. One before the other isn't important. boas.close is the only variant that doesn't work.
	        String result = new String(baos.toByteArray());
	        return new NodeValueString(result);
        } catch (IOException ex) {
        	throw new ExprEvalException(ex);
		}
    }

}
