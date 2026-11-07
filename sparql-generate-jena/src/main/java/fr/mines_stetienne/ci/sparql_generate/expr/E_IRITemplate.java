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

import java.util.List;
import java.util.regex.Pattern;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.E_IRI;
import org.apache.jena.sparql.expr.E_StrConcat;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprFunction1;
import org.apache.jena.sparql.expr.ExprTransform;
import org.apache.jena.sparql.expr.ExprTransformCopy;
import org.apache.jena.sparql.expr.ExprTransformer;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeFunctions;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementGroup;

/**
 * The IRI of an IRI template, such as {@code <stop/{?id}>}:
 * {@code IRI(CONCAT(c0, ENCODE_FOR_URI(STR(?id)), ...))}, with the same value
 * as {@link E_IRI}. Jena parses and resolves the whole IRI for each solution;
 * this expression resolves the constant first part c0 once, and appends the
 * rest when it can only extend the path of that IRI: characters of a path
 * segment (unreserved, percent-encoded) and slashes, and no dot segment;
 * or, after a first part that ends in the query or the fragment, the same
 * characters.
 * Otherwise it evaluates as {@link E_IRI}. It is written as {@code IRI(...)}.
 */
public class E_IRITemplate extends E_IRI {

    /**
     * An absolute IRI with an authority and a path, without query or fragment.
     */
    private static final Pattern WITH_PATH = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*://[^/?#]*/[^?#]*$");

    /**
     * An absolute IRI whose query or fragment has begun: what follows is in it,
     * where there is no dot segment.
     */
    private static final Pattern IN_QUERY_OR_FRAGMENT = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:[^?#]*[?#].*$");

    /**
     * The constant first part of the template, or null.
     */
    private final String prefix;

    /**
     * The base for which {@link #resolvedPrefix} was computed.
     */
    private String cachedBase;

    /**
     * The first part resolved against {@link #cachedBase}, or null if the
     * shortcut does not apply.
     */
    private String resolvedPrefix;

    private boolean cached = false;

    public E_IRITemplate(final E_StrConcat concat) {
        this(null, concat);
    }

    public E_IRITemplate(final String parserBase, final E_StrConcat concat) {
        super(parserBase, concat);
        this.prefix = firstConstant(concat);
    }

    /**
     * IRI(expr) for any expression: only the check of plain absolute IRIs
     * applies (see {@link #isPlainAbsolute(String)}).
     */
    public E_IRITemplate(final String parserBase, final Expr expr) {
        super(parserBase, expr);
        this.prefix = expr instanceof E_StrConcat ? firstConstant((E_StrConcat) expr) : null;
    }

    /**
     * Replaces IRI(CONCAT(...)) by an {@link E_IRITemplate} in the BIND
     * clauses of a group (and of the groups it contains) and in the projected
     * expressions of a query: they are lost when the query is copied by
     * serializing it. The other elements are left as they are.
     *
     * @param query the query, changed in place.
     */
    public static void optimize(final Query query) {
        if (query.getQueryPattern() instanceof ElementGroup) {
            optimize((ElementGroup) query.getQueryPattern());
        }
        final VarExprList project = query.getProject();
        for (Var var : project.getVars()) {
            final Expr expr = project.getExpr(var);
            if (expr != null) {
                project.update(var, ExprTransformer.transform(TRANSFORM, expr));
            }
        }
    }

    /**
     * Replaces IRI(...) by an {@link E_IRITemplate} in an expression.
     */
    public static Expr optimize(final Expr expr) {
        return ExprTransformer.transform(TRANSFORM, expr);
    }

    private static void optimize(final ElementGroup group) {
        final List<Element> elements = group.getElements();
        for (int i = 0; i < elements.size(); i++) {
            final Element el = elements.get(i);
            if (el instanceof ElementBind) {
                final ElementBind bind = (ElementBind) el;
                final Expr expr = ExprTransformer.transform(TRANSFORM, bind.getExpr());
                if (expr != bind.getExpr()) {
                    elements.set(i, new ElementBind(bind.getVar(), expr));
                }
            } else if (el instanceof ElementGroup) {
                optimize((ElementGroup) el);
            }
        }
    }

    private static final ExprTransform TRANSFORM = new ExprTransformCopy(false) {
        @Override
        public Expr transform(final ExprFunction1 func, final Expr arg) {
            if (func.getClass() == E_IRI.class) {
                return new E_IRITemplate(((E_IRI) func).getParserBase(), arg);
            }
            return super.transform(func, arg);
        }
    };

    private static String firstConstant(final E_StrConcat concat) {
        if (concat.getArgs().isEmpty()) {
            return null;
        }
        final Expr first = concat.getArgs().get(0);
        if (!first.isConstant() || !first.getConstant().isString()) {
            return null;
        }
        final String s = first.getConstant().getString();
        // a relative part that ends with a dot may be a dot segment, as in "..",
        // and a query or a fragment ends the path
        if (s.isEmpty() || s.endsWith(".") || s.startsWith("_:")) {
            return null;
        }
        return s;
    }

    @Override
    protected NodeValue evalSpecial(final Binding binding, final FunctionEnv env) {
        final NodeValue v = getRelExpr().eval(binding, env);
        return eval(v, env);
    }

    @Override
    public NodeValue eval(final NodeValue v, final FunctionEnv env) {
        String iri = shortcut(v, env);
        if (iri == null && v.isString() && isPlainAbsolute(v.getString())) {
            iri = v.getString();
        }
        if (iri != null) {
            return NodeValue.makeNode(NodeFactory.createURI(iri));
        }
        return super.eval(v, env);
    }

    @Override
    public Expr copy(final Expr expr) {
        if (expr instanceof E_StrConcat) {
            return new E_IRITemplate(getParserBase(), (E_StrConcat) expr);
        }
        return new E_IRITemplate(getParserBase(), expr);
    }

    private static final Pattern PLAIN_ABSOLUTE = Pattern.compile("^https?://"
            + "[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)*(?::[0-9]{1,5})?"
            + "(?:/[A-Za-z0-9._~!$&'()*+,;=:@%-]*)*"
            + "(?:\\?[A-Za-z0-9._~!$&'()*+,;=:@%/?-]*)?"
            + "(?:#[A-Za-z0-9._~!$&'()*+,;=:@%/?-]*)?$");

    /**
     * True for an absolute http or https IRI that IRI() gives as it is: a
     * lower-case host, then characters of a path, a query and a fragment, with
     * valid percent-encodings and no dot segment. Jena parses and checks every
     * IRI with jena-iri; these need not be.
     */
    static boolean isPlainAbsolute(final String s) {
        if (s.length() > 2000 || !PLAIN_ABSOLUTE.matcher(s).matches()) {
            return false;
        }
        for (int i = s.indexOf('%'); i >= 0; i = s.indexOf('%', i + 1)) {
            if (i + 2 >= s.length() || !isHex(s.charAt(i + 1)) || !isHex(s.charAt(i + 2))) {
                return false;
            }
        }
        // dot segments, in the path only
        int end = s.length();
        final int q = s.indexOf('?');
        final int f = s.indexOf('#');
        if (q >= 0) {
            end = q;
        }
        if (f >= 0 && f < end) {
            end = f;
        }
        final String path = s.substring(0, end);
        return !(path.contains("/./") || path.contains("/../") || path.endsWith("/.") || path.endsWith("/.."));
    }

    /**
     * @return the IRI, or null if the shortcut does not apply.
     */
    private String shortcut(final NodeValue v, final FunctionEnv env) {
        if (prefix == null || !v.isString()) {
            return null;
        }
        final String s = v.getString();
        if (!s.startsWith(prefix)) {
            return null;
        }
        final String resolved = resolvedPrefix(base(env));
        if (resolved == null) {
            return null;
        }
        final String rest = s.substring(prefix.length());
        // in the query or the fragment, there is no dot segment
        final boolean inPath = resolved.indexOf('?') < 0 && resolved.indexOf('#') < 0;
        char previous = prefix.charAt(prefix.length() - 1);
        for (int i = 0; i < rest.length(); i++) {
            final char c = rest.charAt(i);
            if (inPath && c == '.' && previous == '/') {
                // may start a dot segment
                return null;
            }
            if (c == '%') {
                if (i + 2 >= rest.length() || !isHex(rest.charAt(i + 1)) || !isHex(rest.charAt(i + 2))) {
                    return null;
                }
            } else if (!isUnreserved(c) && c != '/') {
                return null;
            }
            previous = c;
        }
        return resolved + rest;
    }

    private String base(final FunctionEnv env) {
        String base = getParserBase();
        if (base == null && env != null && env.getContext() != null) {
            final Query query = (Query) env.getContext().get(ARQConstants.sysCurrentQuery);
            if (query != null) {
                base = query.getBaseURI();
            }
        }
        return base;
    }

    private synchronized String resolvedPrefix(final String base) {
        if (cached && (base == null ? cachedBase == null : base.equals(cachedBase))) {
            return resolvedPrefix;
        }
        String resolved;
        try {
            // as E_IRI does, with the base of the query
            resolved = NodeFunctions.iri(NodeValue.makeString(prefix), base).asNode().getURI();
            final boolean prefixInPath = prefix.indexOf('?') < 0 && prefix.indexOf('#') < 0;
            if (resolved == null || !(prefixInPath ? WITH_PATH : IN_QUERY_OR_FRAGMENT).matcher(resolved).matches()) {
                resolved = null;
            }
        } catch (RuntimeException ex) {
            // ExprEvalException, IRIException: the shortcut does not apply
            resolved = null;
        }
        cachedBase = base;
        resolvedPrefix = resolved;
        cached = true;
        return resolved;
    }

    private static boolean isUnreserved(final char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '.'
                || c == '_' || c == '~';
    }

    private static boolean isHex(final char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
