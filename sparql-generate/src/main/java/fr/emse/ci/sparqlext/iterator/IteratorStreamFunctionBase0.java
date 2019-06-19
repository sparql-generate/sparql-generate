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
package fr.emse.ci.sparqlext.iterator;

import java.util.List;
import java.util.function.Consumer;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;

/**
 * Support for a iterator function of one argument.
 */
public abstract class IteratorStreamFunctionBase0 extends IteratorStreamFunctionBase {

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkBuild(ExprList args) {
        if (!args.isEmpty()) {
            throw new QueryBuildException("Selector '"
                    + this.getClass().getName() + "' takes zero arguments");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(List<NodeValue> args, Consumer<List<List<NodeValue>>> nodeValuesStream, ExecutionControl control) {
        if (args == null) {
            throw new ARQInternalErrorException("Null args list");
        }
        if (!args.isEmpty()) {
            throw new ExprEvalException("Wrong number of"
                    + " arguments: Wanted 0, got " + args.size());
        }
        exec(nodeValuesStream, control);
    }

    /**
     * {@inheritDoc}
     */
    public abstract void exec(Consumer<List<List<NodeValue>>> nodeValuesStream, ExecutionControl control);
}
