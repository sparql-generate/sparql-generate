/*
 * Copyright 2017 Ecole des Mines de Saint-Etienne.
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
package com.github.thesmartenergy.sparql.generate.jena.normalizer;

import com.github.thesmartenergy.sparql.generate.jena.graph.Node_X;
import com.github.thesmartenergy.sparql.generate.jena.graph.Node_XExpr;
import com.github.thesmartenergy.sparql.generate.jena.graph.Node_XLiteral;
import com.github.thesmartenergy.sparql.generate.jena.graph.Node_XURI;
import com.github.thesmartenergy.sparql.generate.jena.graph.SPARQLGenerateNodeVisitor;
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
import org.apache.jena.sparql.expr.E_StrLang;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementGroup;

/**
 *
 * @author maxime.lefrancois
 */
public class NodeExprNormalizer implements SPARQLGenerateNodeVisitor {

    private final List<Element> bindings;

    private final Map<Node_X, Var> cache = new HashMap<>();

    private final ExprNormalizer nzer = new ExprNormalizer();

    private Node result;

    public NodeExprNormalizer(final List<Element> bindings) {
        this.bindings = bindings;
    }

    public NodeExprNormalizer() {
        this(new ArrayList<>());
    }

    public boolean hasBindings() {
        return !bindings.isEmpty();
    }

    public ElementGroup getBindingsAsGroup() {
        ElementGroup group = new ElementGroup();
        bindings.forEach((el) -> {
            group.addElement(el);
        });
        return group;
    }

    public Node getResult() {
        return result;
    }

    public List<Element> getBindings() {
        return bindings;
    }

    @Override
    public Object visit(Node_XExpr node) {
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

    @Override
    public Object visit(Node_XLiteral node) {
        if (cache.containsKey(node)) {
            result = cache.get(node);
            return null;
        }
        Var result = Var.alloc(node.getLabel());
        final ExprList args = new ExprList();
        final List<Expr> components = node.getComponents();
        for (Expr e : components) {
            args.add(nzer.normalize(e));
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

    @Override
    public Object visit(Node_XURI node) {
        if (cache.containsKey(node)) {
            result = cache.get(node);
            return null;
        }
        Var result = Var.alloc(node.getLabel());
        ExprList args = new ExprList();
        List<Expr> components = node.getComponents();
        for (Expr e : components) {
            args.add(nzer.normalize(e));
        }
        Expr str = new E_StrConcat(args);
        Expr expr = new E_IRI(str);
        bindings.add(new ElementBind(result, expr));
        cache.put(node, result);
        this.result = result;
        return null;
    }

    @Override
    public Object visitAny(Node_ANY it) {
        result = it;
        return null;
    }

    @Override
    public Object visitBlank(Node_Blank it, BlankNodeId id) {
        result = it;
        return null;
    }

    @Override
    public Object visitLiteral(Node_Literal it, LiteralLabel lit) {
        result = it;
        return null;
    }

    @Override
    public Object visitURI(Node_URI it, String uri) {
        result = it;
        return null;
    }

    @Override
    public Object visitVariable(Node_Variable it, String name) {
        result = it;
        return null;
    }
}
