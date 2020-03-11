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
package fr.emse.ci.sparqlext.function;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.apache.jena.sparql.ARQException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public class SPARQLExtFunction implements Function {

    public static final int CACHE_SIZE = 1000;
    static final Cache<Key, NodeValue> CACHE = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build();

    static final Logger LOG = LoggerFactory.getLogger(SPARQLExtFunction.class);

    private final SPARQLExtQuery function;

    public SPARQLExtFunction(SPARQLExtQuery function) {
        if (!function.isFunctionType()) {
            throw new ARQException("creating function for a query that is not a function");
        }
        this.function = function;
        function.normalizeXExpr();
    }

    @Override
    public void build(String uri, ExprList args) {
    }

    @Override
    public NodeValue exec(Binding binding, ExprList args, String uri, FunctionEnv env) {
        List<Var> signature = function.getSignature();
        if (args.size() != signature.size()) {
            String message = "the number of parameters (" + args.size() + ") is different from the number of arguments in the function " + uri + " (" + signature.size() + ")";
            LOG.debug(message);
            throw new ExprEvalException(message);
        }
        final BindingMap newBinding = BindingFactory.create();
        for (int i = 0; i < signature.size(); i++) {
            Var v = signature.get(i);
            Expr e = args.get(i);
            try {
                NodeValue nv = e.eval(binding, env);
                newBinding.add(v, nv.asNode());
            } catch (ExprException ex) {
                LOG.trace("could not evaluate expression " + e + " with binding");
            }
        }
        final Expr functionExpression = function.getFunctionExpression();
        Key key = new Key(functionExpression, newBinding, env);
        try {
            return CACHE.get(key, () -> functionExpression.eval(newBinding, env));
        } catch(ExecutionException ex) {
            LOG.trace("could not evaluate expression " + functionExpression + " with binding");
            throw new ExprEvalException("could not evaluate expression " + functionExpression + " with binding");
        }
    }

    static private class Key {

        Expr functionExpression;
        BindingMap binding;
        FunctionEnv env;

        public Key(Expr functionExpression, BindingMap binding, FunctionEnv env) {
            this.functionExpression = functionExpression;
            this.binding = binding;
            this.env = env;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof Key)) {
                return false;
            }
            Key other = (Key) obj;
            return functionExpression.equals(other.functionExpression)
                    && binding.equals(other.binding)
                    && env.equals(other.env);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 79 * hash + Objects.hashCode(this.functionExpression);
            hash = 79 * hash + Objects.hashCode(this.binding);
            hash = 79 * hash + Objects.hashCode(this.env);
            return hash;
        }

    }

}
