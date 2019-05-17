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
package fr.emse.ci.sparqlext.normalizer;

import org.apache.jena.sparql.path.P_Alt;
import org.apache.jena.sparql.path.P_Distinct;
import org.apache.jena.sparql.path.P_FixedLength;
import org.apache.jena.sparql.path.P_Inverse;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_Mod;
import org.apache.jena.sparql.path.P_Multi;
import org.apache.jena.sparql.path.P_NegPropSet;
import org.apache.jena.sparql.path.P_OneOrMore1;
import org.apache.jena.sparql.path.P_OneOrMoreN;
import org.apache.jena.sparql.path.P_Path0;
import org.apache.jena.sparql.path.P_ReverseLink;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.P_Shortest;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.P_ZeroOrMoreN;
import org.apache.jena.sparql.path.P_ZeroOrOne;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathVisitor;

/**
 * Class used to normalize path expressions. 
 * 
 * @author maxime.lefrancois
 */
public class PathNormalizer implements PathVisitor {
    
    /**
     * node expression normalizer.
     */
    private final NodeExprNormalizer nzer;
    
    /**
     * latest normalized path
     */
    private Path result;    

    /**
     * Constructor
     * @param nzer 
     */
    public PathNormalizer(NodeExprNormalizer nzer) {
        this.nzer = nzer;
    }

    /**
     * @return latest normalized path
     */
    public Path getResult() {
        return result;
    }
    

    @Override
    public void visit(P_Link pathNode) {
        pathNode.getNode().visitWith(nzer);
        result = new P_Link(nzer.getResult());
    }

    @Override
    public void visit(P_ReverseLink pathNode) {
        pathNode.getNode().visitWith(nzer);
        result = new P_ReverseLink(nzer.getResult());
    }

    @Override
    public void visit(P_NegPropSet pathNotOneOf) {
        P_NegPropSet res = new P_NegPropSet();
        pathNotOneOf.getNodes().forEach((p) -> {
            p.visit(this);
            res.add((P_Path0) result);
        });
        result = res;
    }

    @Override
    public void visit(P_Inverse inversePath) {
        inversePath.getSubPath().visit(this);
        result = new P_Inverse(result);
    }

    @Override
    public void visit(P_Mod pathMod) {
        pathMod.getSubPath().visit(this);
        result = new P_Mod(result, pathMod.getMin(), pathMod.getMax());
    }

    @Override
    public void visit(P_FixedLength pFixedLength) {
        pFixedLength.getSubPath().visit(this);
        result = new P_FixedLength(result, pFixedLength.getCount());
    }

    @Override
    public void visit(P_Distinct pathDistinct) {
        pathDistinct.getSubPath().visit(this);
        result = new P_Distinct(result);
    }

    @Override
    public void visit(P_Multi pathMulti) {
        pathMulti.getSubPath().visit(this);
        result = new P_Multi(result);
    }

    @Override
    public void visit(P_Shortest pathShortest) {
        pathShortest.getSubPath().visit(this);
        result = new P_Shortest(result);
    }

    @Override
    public void visit(P_ZeroOrOne path) {
        path.getSubPath().visit(this);
        result = new P_ZeroOrOne(result);
    }

    @Override
    public void visit(P_ZeroOrMore1 path) {
        path.getSubPath().visit(this);
        result = new P_ZeroOrMore1(result);
    }

    @Override
    public void visit(P_ZeroOrMoreN path) {
        path.getSubPath().visit(this);
        result = new P_ZeroOrMoreN(result);
    }

    @Override
    public void visit(P_OneOrMore1 path) {
        path.getSubPath().visit(this);
        result = new P_OneOrMore1(result);
    }

    @Override
    public void visit(P_OneOrMoreN path) {
        path.getSubPath().visit(this);
        result = new P_OneOrMoreN(result);
    }

    @Override
    public void visit(P_Alt pathAlt) {
        pathAlt.getLeft().visit(this);
        Path left = result;
        pathAlt.getRight().visit(this);
        Path right = result;
        result = new P_Alt(left, right);
    }

    @Override
    public void visit(P_Seq pathSeq) {
        pathSeq.getLeft().visit(this);
        Path left = result;
        pathSeq.getRight().visit(this);
        Path right = result;
        result = new P_Seq(left, right);
    }
    
    
}
