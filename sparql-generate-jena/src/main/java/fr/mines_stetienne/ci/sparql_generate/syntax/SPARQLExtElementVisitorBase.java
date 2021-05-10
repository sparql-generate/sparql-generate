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
package fr.mines_stetienne.ci.sparql_generate.syntax;

import org.apache.jena.sparql.syntax.ElementAssign;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementDataset;
import org.apache.jena.sparql.syntax.ElementExists;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementFind;
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

/**
 * Extends the ARQ Element Visitor with SPARQL-Generate specificities.
 *
 * @author Maxime Lefrançois
 */
public class SPARQLExtElementVisitorBase implements SPARQLExtElementVisitor {

    @Override
    public void visit(ElementGenerateTriplesBlock el) {
    }

    @Override
    public void visit(ElementSubExtQuery el) {
    }

    @Override
    public void visit(ElementIterator el) {
    }

    @Override
    public void visit(ElementSource el) {
    }

    @Override
    public void visit(ElementExpr el) {
    }

    @Override
    public void visit(ElementBox el) {
    }

    @Override
    public void visit(ElementFormat el) {
    }

    @Override
    public void visit(ElementTGroup el) {
    }

    @Override
    public void visit(ElementPerform el) {
    }

    @Override
    public void visit(ElementTriplesBlock el) {
    }

    @Override
    public void visit(ElementPathBlock el) {
    }

    @Override
    public void visit(ElementFilter el) {
    }

    @Override
    public void visit(ElementAssign el) {
    }

    @Override
    public void visit(ElementBind el) {
    }

    @Override
    public void visit(ElementData el) {
    }

    @Override
    public void visit(ElementUnion el) {
    }

    @Override
    public void visit(ElementOptional el) {
    }

    @Override
    public void visit(ElementGroup el) {
    }

    @Override
    public void visit(ElementDataset el) {
    }

    @Override
    public void visit(ElementNamedGraph el) {
    }

    @Override
    public void visit(ElementExists el) {
    }

    @Override
    public void visit(ElementNotExists el) {
    }

    @Override
    public void visit(ElementMinus el) {
    }

    @Override
    public void visit(ElementService el) {
    }

    @Override
    public void visit(ElementSubQuery el) {
    }

	@Override
	public void visit(ElementFind el) {	
	}

}
