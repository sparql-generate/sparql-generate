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
package fr.mines_stetienne.ci.sparql_generate.expr;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprFunctionN;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.serializer.SerializationContext;

import fr.mines_stetienne.ci.sparql_generate.RaiseException;

/**
 * {@code RAISE(message, value...)}: an expression whose evaluation stops the
 * execution with a {@link RaiseException} that carries the message and the
 * values. It is evaluated where any expression is, for each solution that
 * reaches it: in {@code IF(BOUND(?x), ?x, RAISE("no x"))}, only when
 * {@code ?x} is unbound; in {@code COALESCE(IRI(?v), RAISE("not an IRI", ?v))},
 * only when {@code IRI(?v)} is an error.
 * <p>
 * Its arguments are evaluated leniently: one that cannot be evaluated (an
 * unbound variable, an error) is an undefined value, and the error is raised
 * anyway. The exception is not an expression evaluation error: {@code BIND},
 * {@code FILTER}, {@code COALESCE} and the iterators do not catch it.
 */
public class E_Raise extends ExprFunctionN {

    /**
     * The keyword of the expression, as it is written in queries.
     */
    public static final String KEYWORD = "RAISE";

    private static final String SYMBOL = "raise";

    public E_Raise(final ExprList args) {
        super(SYMBOL, args);
    }

    @Override
    public String getFunctionPrintName(final SerializationContext cxt) {
        return KEYWORD;
    }

    @Override
    protected NodeValue evalSpecial(final Binding binding, final FunctionEnv env) {
        final List<NodeValue> values = new ArrayList<>(numArgs());
        for (Expr arg : getArgs()) {
            NodeValue value;
            try {
                value = arg.eval(binding, env);
            } catch (ExprEvalException ex) {
                value = null;
            }
            values.add(value);
        }
        throw RaiseException.of(values);
    }

    /**
     * Not called on evaluation ({@link #evalSpecial}); constant folding calls
     * it, and keeps the expression when it throws.
     */
    @Override
    public NodeValue eval(final List<NodeValue> args) {
        throw RaiseException.of(args);
    }

    @Override
    public Expr copy(final ExprList newArgs) {
        return new E_Raise(newArgs);
    }
}
