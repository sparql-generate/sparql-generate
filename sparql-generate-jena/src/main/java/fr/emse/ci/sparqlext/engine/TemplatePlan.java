/*
 * Copyright 2020 Ecole des Mines de Saint-Etienne.
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
package fr.emse.ci.sparqlext.engine;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.emse.ci.sparqlext.utils.ContextUtils;

public class TemplatePlan {

	/**
	 * The logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TemplatePlan.class);

	private final Expr before;
	private final Expr expr;
	private final Expr separator;
	private final Expr after;

	public TemplatePlan(Expr before, Expr expr, Expr separator, Expr after) {
		Objects.requireNonNull(expr, "expr must not be null");
		this.before = before;
		this.expr = expr;
		this.separator = separator;
		this.after = after;
	}

	public void exec(List<Var> variables, List<Binding> values, Context context) {
		final IndentedWriter writer = ContextUtils.getTemplateOutput(context);
		boolean first = true;
		final FunctionEnv env = new FunctionEnvBase(context);
		String result;
		for(Iterator<Binding> it=values.iterator(); it.hasNext();) {
			Binding binding = it.next();
			if (first && before != null) {
				result = getExprEval(before, binding, context, env);
				writer.print(result);
			}
			if (!first && separator != null) {
				result = getExprEval(separator, binding, context, env);
				writer.print(result);
			}
			result = getExprEval(expr, binding, context, env);
			writer.print(result);
			first = false;
			if (!it.hasNext() && after != null) {
				result = getExprEval(after, binding, context, env);
				writer.print(result);
			}
			writer.flush();
		}
	}

	private String getExprEval(Expr expr, Binding binding, Context context, FunctionEnv env) {
		NodeValue nv = null;
		try {
			nv = expr.eval(binding, env);
		} catch (ExprEvalException ex) {
			LOG.debug("Could not evaluate expression " + expr, ex);
		}
		if (nv == null) {
			return "";
		}
		if (!nv.isLiteral()) {
			LOG.debug("Expression did not evaluate as a literal " + expr + ", got :" + nv);
		}
		return nv.asNode().getLiteralLexicalForm();
	}

}
