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
package fr.emse.ci.sparqlext.normalizer.xexpr;

import fr.emse.ci.sparqlext.expr.E_URIParam;
import fr.emse.ci.sparqlext.graph.Node_Expr;
import fr.emse.ci.sparqlext.graph.Node_ExtendedLiteral;
import fr.emse.ci.sparqlext.graph.Node_ExtendedURI;
import fr.emse.ci.sparqlext.graph.Node_List;
import fr.emse.ci.sparqlext.graph.Node_Template;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_ANY;
import org.apache.jena.graph.Node_Blank;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_IRI;
import org.apache.jena.sparql.expr.E_StrConcat;
import org.apache.jena.sparql.expr.E_StrDatatype;
import org.apache.jena.sparql.expr.E_StrEncodeForURI;
import org.apache.jena.sparql.expr.E_StrLang;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementGroup;
import fr.emse.ci.sparqlext.graph.SPARQLExtNodeVisitor;
import fr.emse.ci.sparqlext.lang.ParserSPARQLExt;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import org.apache.jena.sparql.expr.E_Function;
import org.apache.jena.sparql.expr.E_Str;

/**
 * Class used to normalize nodes, i.e., take as input a node (potentially an
 * expression node), and output a normalized node and potentially a binding
 * clause.
 *
 * @author Maxime Lefrançois
 */
public class NodeExprNormalizer implements SPARQLExtNodeVisitor {

    /**
     * List of bindings to augment with clauses equivalent to expression nodes
     */
    private final List<Element> bindings;

    /**
     * Already normalized nodes (useful when normalizing Basic Graph Patterns)
     */
    private final Map<Node, Node> cache = new HashMap<>();

    /**
     * Expression normalizer
     */
    private final ExprNormalizer nzer = new ExprNormalizer();

    /**
     * Latest normalized node
     */
    private Node result;

    /**
     * Constructor
     *
     * @param bindings initial bindings
     */
    public NodeExprNormalizer(final List<Element> bindings) {
        this.bindings = bindings;
    }

    /**
     * Constructor, with empty initial bindings.
     */
    public NodeExprNormalizer() {
        this(new ArrayList<>());
    }

    /**
     *
     * @return true if the list of bindings is not empty.
     */
    public boolean hasBindings() {
        return !bindings.isEmpty();
    }

    /**
     *
     * @return an ElementGroup with the list of binding elements.
     */
    public ElementGroup getBindingsAsGroup() {
        ElementGroup group = new ElementGroup();
        bindings.forEach((el) -> {
            group.addElement(el);
        });
        return group;
    }

    /**
     *
     * @return the latest normalized node
     */
    public Node getResult() {
        return result;
    }

    /**
     *
     * @return the list of bindings
     */
    public List<? extends Element> getBindings() {
        return bindings;
    }

    /**
     * {@inheritDoc
     */
    @Override
    public Object visit(Node_List node) {
        if (cache.containsKey(node)) {
            result = cache.get(node);
            return null;
        }
        if (node.getExpr().isVariable()) {
            result = node;
            return null;
        }
        Var var = Var.alloc(node.getLabel());
        final Expr expr = nzer.normalize(node.getExpr());
        bindings.add(new ElementBind(var, expr));
        Node_List nzedList = new Node_List(new ExprVar(var));
        cache.put(node, nzedList);
        this.result = nzedList;
        return null;
    }

    /**
     * {@inheritDoc
     */
    @Override
    public Object visit(Node_Expr node) {
        if (cache.containsKey(node)) {
            result = cache.get(node);
            return null;
        }
        Var result = Var.alloc(node.getLabel());
        final Expr expr = nzer.normalize(node.getExpr());
        bindings.add(new ElementBind(result, expr));
        cache.put(node, result);
        this.result = result;
        return null;
    }

    /**
     * {@inheritDoc
     */
    @Override
    public Object visit(Node_ExtendedLiteral node) {
        if (cache.containsKey(node)) {
            result = cache.get(node);
            return null;
        }
        Var result = Var.alloc(node.getLabel());
        final ExprList args = new ExprList();
        final List<Expr> components = node.getComponents();
        for (Expr e : components) {
            Expr n = new E_Str(nzer.normalize(e));            
            args.add(n);
        }
        final Expr str = new E_StrConcat(args);
        if (node.getLang() != null) {
            Expr expr = new E_StrLang(str, new NodeValueString(node.getLang()));
            bindings.add(new ElementBind(result, expr));
        } else if (node.getDatatype() != null) {
            node.getDatatype().visitWith(this);
            Expr expr = new E_StrDatatype(str, new NodeValueNode(this.result));
            bindings.add(new ElementBind(result, expr));
        } else {
            bindings.add(new ElementBind(result, str));
        }
        cache.put(node, result);
        this.result = result;
        return null;
    }

    /**
     * {@inheritDoc
     */
    @Override
    public Object visit(Node_ExtendedURI node) {
        if (cache.containsKey(node)) {
            result = cache.get(node);
            return null;
        }
        Var result = Var.alloc(node.getLabel());
        ExprList args = new ExprList();
        List<Expr> components = node.getComponents();
        for (Expr e : components) {
            if (e instanceof E_URIParam) {
                Expr subst = new E_StrEncodeForURI(nzer.normalize(((E_URIParam) e).getArg()));
                args.add(nzer.normalize(subst));
            } else {
                args.add(nzer.normalize(e));
            }
        }
        Expr str = new E_StrConcat(args);
        Expr expr = new E_IRI(str);
        bindings.add(new ElementBind(result, expr));
        cache.put(node, result);
        this.result = result;
        return null;
    }

    @Override
    public Object visit(Node_Template node) {
        if (cache.containsKey(node)) {
            result = cache.get(node);
            return null;
        }
        Var result = Var.alloc(node.getLabel());
        SPARQLExtQuery query = (SPARQLExtQuery) node.getQuery();
        query = (SPARQLExtQuery) ParserSPARQLExt.parseSubQuery(query, query.toString());
        E_Function expr = TemplateUtils.getFunction(query);
        bindings.add(new ElementBind(result, expr));
        cache.put(node, result);
        this.result = result;
        return null;
    }
    

    /**
     * {@inheritDoc
     */
    @Override
    public Object visitAny(Node_ANY it) {
        result = it;
        return null;
    }

    /**
     * {@inheritDoc
     */
    @Override
    public Object visitBlank(Node_Blank it, BlankNodeId id) {
        result = it;
        return null;
    }

    /**
     * {@inheritDoc
     */
    @Override
    public Object visitLiteral(Node_Literal it, LiteralLabel lit) {
        result = it;
        return null;
    }

    /**
     * {@inheritDoc
     */
    @Override
    public Object visitURI(Node_URI it, String uri) {
        result = it;
        return null;
    }

    /**
     * {@inheritDoc
     */
    @Override
    public Object visitVariable(Node_Variable it, String name) {
        result = it;
        return null;
    }

//    @Override
//    public Object visit(Node_BGP node) {
//        if (cache.containsKey(node)) {
//            result = cache.get(node);
//            return null;
//        }
//        final BasicPattern bgp = node.getPattern();
//        final BasicPattern bpnzed = new BasicPattern();
//        bgp.forEach((t) -> {
//            t.getSubject().visitWith(this);
//            Node s = result;
//            t.getPredicate().visitWith(this);
//            Node p = result;
//            t.getObject().visitWith(this);
//            Node o = result;
//            bpnzed.add(new Triple(s, p, o));
//        });
//        final Node_BGP result = new Node_BGP(bpnzed);
//        cache.put(node, result);
//        this.result = result;
//        return null;
//    }
}
