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
package fr.mines_stetienne.ci.sparql_generate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.query.QueryCancelledException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.util.FmtUtils;

/**
 * The error raised by the evaluation of a {@code RAISE(message, value...)}
 * expression. It is not an expression evaluation error (which leaves a
 * variable unbound): it stops the execution of the query.
 * <p>
 * The message of the exception is the message of the query, followed by the
 * values, if any, in the syntax of N-Triples (numbers and booleans
 * abbreviated), {@code UNDEF} for a value that could not be evaluated:
 * {@code RAISE("Column not found", "IDs", ?row)} gives
 * {@code Column not found: "IDs", 3}.
 * <p>
 * It is a {@link QueryCancelledException}: RAISE cancels the execution, and
 * the query engine of Jena, which turns the other exceptions of a
 * {@code FILTER} into a false condition, lets it through.
 */
public class RaiseException extends QueryCancelledException {

    private static final long serialVersionUID = 1L;

    private final String raisedMessage;

    private final List<Node> values;

    private final String message;

    /**
     * @param raisedMessage the message, never null
     * @param values the values, null for a value that could not be evaluated
     */
    public RaiseException(final String raisedMessage, final List<Node> values) {
        super();
        this.message = format(raisedMessage, values);
        this.raisedMessage = raisedMessage;
        this.values = Collections.unmodifiableList(new ArrayList<>(values));
    }

    /**
     * The exception of the evaluated arguments of a RAISE expression: the
     * message, then the values.
     *
     * @param args the evaluated arguments, null for an argument that could not
     *            be evaluated
     * @return the exception
     */
    public static RaiseException of(final List<NodeValue> args) {
        final String message;
        if (args.isEmpty() || args.get(0) == null) {
            message = "Error raised by the query";
        } else {
            final Node node = args.get(0).asNode();
            message = node.isLiteral() ? node.getLiteralLexicalForm() : FmtUtils.stringForNode(node);
        }
        final List<Node> values = new ArrayList<>();
        for (int i = 1; i < args.size(); i++) {
            values.add(args.get(i) == null ? null : args.get(i).asNode());
        }
        return new RaiseException(message, values);
    }

    /**
     * @return the message given to RAISE, followed by the values
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * @return the message given to RAISE, without the values
     */
    public String getRaisedMessage() {
        return raisedMessage;
    }

    /**
     * @return the values given to RAISE after the message, null for a value
     *         that could not be evaluated (an unbound variable, an error)
     */
    public List<Node> getValues() {
        return values;
    }

    private static String format(final String message, final List<Node> values) {
        if (values.isEmpty()) {
            return message;
        }
        final StringBuilder sb = new StringBuilder(message).append(":");
        String sep = " ";
        for (Node value : values) {
            sb.append(sep).append(value == null ? "UNDEF" : FmtUtils.stringForNode(value));
            sep = ", ";
        }
        return sb.toString();
    }

    /**
     * @param ex an exception
     * @return the RaiseException in the causes of the exception, itself
     *         included, or null.
     */
    public static RaiseException findIn(final Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof RaiseException) {
                return (RaiseException) t;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return null;
    }
}
