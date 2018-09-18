/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.thesmartenergy.sparql.generate.jena.iterator;


import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;

import java.util.List;

/**
 * Support for a iterator function of zero arguments.
 */
public abstract class IteratorFunctionBase0 extends IteratorFunctionBase {

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkBuild(ExprList args) {
        if (args.size() != 0) {
            throw new QueryBuildException("Iterator function '"
                    + this.getClass().getName() + "' takes no arguments");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final List<List<NodeValue>> exec(List<NodeValue> args) {
        if (args == null) {
            throw new ARQInternalErrorException("Iterator function '"
                    + this.getClass().getName() + " Null args list");
        }
        if (!args.isEmpty()) {
            throw new ExprEvalException("Iterator function '"
                    + this.getClass().getName() + " Wanted 0, got "
                    + args.size());
        }
        return exec();
    }

    /**
     * {@inheritDoc}
     */
    public abstract List<List<NodeValue>> exec();
}
