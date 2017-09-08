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
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.modify.TemplateLib;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private static final Logger LOG = LogManager.getLogger(GenerateTriplesPlanImpl.class);


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
        for (Triple t : bgp.getList()) {
            Triple t2 = TemplateLib.subst(t, binding, bNodeMap.asMap());
            if (t2.isConcrete()) {
                outputStream.triple(t2);
            }
        }
    }
}
