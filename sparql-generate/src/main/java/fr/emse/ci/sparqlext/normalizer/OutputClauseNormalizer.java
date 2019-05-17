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
package fr.emse.ci.sparqlext.normalizer;

import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.syntax.ElementBox;
import fr.emse.ci.sparqlext.syntax.ElementExpr;
import fr.emse.ci.sparqlext.syntax.ElementFormat;
import fr.emse.ci.sparqlext.syntax.ElementGenerateTriplesBlock;
import fr.emse.ci.sparqlext.syntax.ElementIterator;
import fr.emse.ci.sparqlext.syntax.ElementPerform;
import fr.emse.ci.sparqlext.syntax.ElementSource;
import fr.emse.ci.sparqlext.syntax.ElementSubExtQuery;
import fr.emse.ci.sparqlext.syntax.ElementTGroup;
import fr.emse.ci.sparqlext.syntax.SPARQLExtElementVisitor;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementAssign;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementDataset;
import org.apache.jena.sparql.syntax.ElementExists;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementMinus;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementNotExists;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementService;
import org.apache.jena.sparql.syntax.ElementSubQuery;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to normalize output elements of SPARQL-Generate and
 * SPARQL-Template queries
 *
 * @author maxime.lefrancois
 */
public class OutputClauseNormalizer implements SPARQLExtElementVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(OutputClauseNormalizer.class);

    private final ExprNormalizer enzer = new ExprNormalizer();

    private final NodeExprNormalizer nenzer;

    /**
     *
     */
    private Element result;

    /**
     * The latest result of an element normalization.
     */
    public Element getResult() {
        return result;
    }

    public OutputClauseNormalizer(NodeExprNormalizer nenzer) {
        this.nenzer = nenzer;
    }

    @Override
    public void visit(ElementGenerateTriplesBlock el) {
        final BasicPattern bgp = el.getPattern();
        final ElementGenerateTriplesBlock nzed = new ElementGenerateTriplesBlock();
        bgp.forEach((t) -> {
            t.getSubject().visitWith(nenzer);
            Node s = nenzer.getResult();
            t.getPredicate().visitWith(nenzer);
            Node p = nenzer.getResult();
            t.getObject().visitWith(nenzer);
            Node o = nenzer.getResult();
            nzed.addTriple(new Triple(s, p, o));
        });
        result = nzed;
    }

    @Override
    public void visit(ElementExpr el) {
        final Expr expr = enzer.normalize(el.getExpr());
        result = new ElementExpr(expr);
    }

    @Override
    public void visit(ElementBox el) {
        List<Element> nzed = new ArrayList<>();
        for (Element e : el.getTExpressions()) {
            e.visit(this);
            nzed.add(result);
        }
        result = new ElementBox(nzed);
    }

    @Override
    public void visit(ElementFormat el) {
        visit(el.getExpr());
        final ElementExpr expr = (ElementExpr) result;
        List<Element> nzed = new ArrayList<>();
        for (Element e : el.getTExpressions()) {
            e.visit(this);
            nzed.add(result);
        }
        result = new ElementFormat(expr, nzed);
    }

    @Override
    public void visit(ElementTGroup el) {
        List<Element> nzed = new ArrayList<>();
        for (Element e : el.getTExpressions()) {
            e.visit(this);
            nzed.add(result);
        }
        result = new ElementTGroup(nzed, el.isDistinct(), el.getSeparator());
    }

    @Override
    public void visit(ElementPerform el) {
        el.getName().visitWith(nenzer);
        Node newNode = nenzer.getResult();
        if (el.getParams() == null) {
            result = new ElementPerform(newNode, null);
        } else {
            List<Expr> args = new ArrayList<>();
            for (Expr e : el.getParams().getList()) {
                args.add(enzer.normalize(e));
            }
            result = new ElementPerform(newNode, new ExprList(args));
        }
    }

    @Override
    public void visit(ElementSubExtQuery el) {
        SPARQLExtQuery nzed = el.getQuery();
        nzed.normalize();
        result = new ElementSubExtQuery(nzed);
    }

    @Override
    public void visit(ElementIterator el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementSource el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementTriplesBlock el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementPathBlock el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementFilter el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementAssign el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementBind el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementData el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementUnion el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementOptional el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementGroup el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementDataset el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementNamedGraph el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementExists el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementNotExists el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementMinus el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementService el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementSubQuery el) {
        LOG.warn("Should not reach this point");
    }

}
