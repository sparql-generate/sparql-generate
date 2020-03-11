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
package fr.emse.ci.sparqlext.normalizer.aggregates;

import org.apache.jena.sparql.expr.Expr;
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

/**
 * Class used to normalize output elements of SPARQL-Generate and
 * SPARQL-Template queries
 *
 * @author maxime.lefrancois
 */
public class OutputClauseNormalizer implements SPARQLExtElementVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(OutputClauseNormalizer.class);

	    private ExprNormalizer enzer;

    private Element result;

    public OutputClauseNormalizer(ExprNormalizer enzer) {
        this.enzer = enzer;
    }
    
    public Element getResult() {
    	return result;
    }

    @Override
    public void visit(ElementGenerateTriplesBlock el) {
    }

    @Override
    public void visit(ElementExpr el) {
        Expr expr = enzer.normalize(el.getExpr());
        result = new ElementExpr(expr);
    }

    @Override
    public void visit(ElementBox el) {
        for (Element e : el.getTExpressions()) {
            e.visit(this);
        }
        result = el;
    }

    @Override
    public void visit(ElementFormat el) {
        visit(el.getExpr());
        for (Element e : el.getTExpressions()) {
            e.visit(this);
        }
        result = el;
    }

    @Override
    public void visit(ElementTGroup el) {
        for (Element e : el.getTExpressions()) {
            e.visit(this);
        }
        result = el;
    }

    @Override
    public void visit(ElementPerform el) {
        LOG.warn("Should not reach this point");
    }

    @Override
    public void visit(ElementSubExtQuery el) {
        el.getQuery().normalizeAggregates();
        result = el;
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
