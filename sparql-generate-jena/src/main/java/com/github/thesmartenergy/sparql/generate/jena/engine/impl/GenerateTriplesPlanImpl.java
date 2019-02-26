/*
 * Copyright 2016 The Apache Software Foundation.
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
package com.github.thesmartenergy.sparql.generate.jena.engine.impl;

import com.github.thesmartenergy.sparql.generate.jena.engine.GenerateTemplateElementPlan;
import com.github.thesmartenergy.sparql.generate.jena.engine.StreamRDFBlock;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.modify.TemplateLib;
import org.apache.jena.sparql.util.FmtUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Generates a triples block in the {@code GENERATE} clause.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class GenerateTriplesPlanImpl
        extends PlanBase implements GenerateTemplateElementPlan {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(GenerateTriplesPlanImpl.class);

    /**
     * The basic pattern.
     */
    private final BasicPattern bgp;

    /**
     * Constructor.
     *
     * @param basicGraphPattern - the basic pattern.
     */
    public GenerateTriplesPlanImpl(final BasicPattern basicGraphPattern) {
        this.bgp = basicGraphPattern;
    }

    /**
     * {@inheritDoc}
     */
    final void exec(
            final Dataset inputDataset,
            final StreamRDF outputStream,
            final BindingHashMapOverwrite binding,
            final BNodeMap bNodeMap) {
        final StringBuilder sb = new StringBuilder("Output triples");
        if(outputStream instanceof StreamRDFBlock) {
            StreamRDFBlock stream = (StreamRDFBlock) outputStream;
            stream.startBlock();
            bgp.getList().stream()
                .map((t) -> TemplateLib.subst(t, binding, bNodeMap.asMap()))
                .filter((t2) -> (t2.isConcrete()))
                .forEach((t2) -> {
                    if (LOG.isTraceEnabled()) {
                        sb.append("\n\t").append(FmtUtils.stringForTriple(t2));
                    }
                    outputStream.triple(t2);
                });
            stream.endBlock();
        } else {
            bgp.getList().stream()
                .map((t) -> TemplateLib.subst(t, binding, bNodeMap.asMap()))
                .filter((t2) -> (t2.isConcrete()))
                .forEach((t2) -> {
                    if (LOG.isTraceEnabled()) {
                        sb.append("\n\t").append(FmtUtils.stringForTriple(t2));
                    }
                    outputStream.triple(t2);
                });
        }
        LOG.trace(sb.toString());
    }
}
