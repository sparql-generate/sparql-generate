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
package fr.mines_stetienne.ci.sparql_generate.function;

import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;
import java.util.Arrays;
import java.util.List;
import org.apache.jena.sparql.ARQException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.Unstable;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Maxime Lefrançois
 */
public class SPARQLExtFunction implements Function {


    static final Logger LOG = LoggerFactory.getLogger(SPARQLExtFunction.class);

    private final SPARQLExtQuery function;

    /**
     * Whether the results may be memoised: false if the expression uses
     * BNODE, RAND, UUID or STRUUID.
     */
    private final boolean stable;

    public SPARQLExtFunction(SPARQLExtQuery function) {
        if (!function.isFunctionType()) {
            throw new ARQException("creating function for a query that is not a function");
        }
        this.function = function;
        function.normalizeXExpr();
        this.stable = isStable(function.getFunctionExpression());
    }

    private static boolean isStable(Expr expr) {
        if (expr instanceof Unstable) {
            return false;
        }
        if (expr.isFunction()) {
            for (Expr arg : expr.getFunction().getArgs()) {
                if (!isStable(arg)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void build(String uri, ExprList args, Context context) {
    }

    @Override
    public NodeValue exec(Binding binding, ExprList args, String uri, FunctionEnv env) {
        List<Var> signature = function.getSignature();
        if (args.size() != signature.size()) {
            String message = "the number of parameters (" + args.size() + ") is different from the number of arguments in the function " + uri + " (" + signature.size() + ")";
            LOG.debug(message);
            throw new ExprEvalException(message);
        }
        final BindingBuilder bindingBuilder = Binding.builder();
        for (int i = 0; i < signature.size(); i++) {
            Var v = signature.get(i);
            Expr e = args.get(i);
            try {
                NodeValue nv = e.eval(binding, env);
                bindingBuilder.add(v, nv.asNode());
            } catch (ExprException ex) {
                LOG.trace("could not evaluate expression " + e + " with binding");
            }
        }
        final Expr functionExpression = function.getFunctionExpression();
        final Binding callBinding = bindingBuilder.build();
        if (!stable) {
            return functionExpression.eval(callBinding, env);
        }
        // memoised for the duration of the execution only
        return ContextUtils.memoise(env.getContext(), Arrays.asList(functionExpression, callBinding),
                () -> functionExpression.eval(callBinding, env));
    }

}
