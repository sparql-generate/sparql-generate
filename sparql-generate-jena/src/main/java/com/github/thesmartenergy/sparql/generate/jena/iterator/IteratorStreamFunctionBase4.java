/*
 * Copyright 2016 Ecole des Mines de Saint-Etienne.
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
package com.github.thesmartenergy.sparql.generate.jena.iterator;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;

/**
 * Support for a iterator function of two arguments.
 */
public abstract class IteratorStreamFunctionBase4 extends IteratorStreamFunctionBase {

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkBuild(ExprList args) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> exec(List<NodeValue> args, Function<Collection<List<NodeValue>>, CompletableFuture<Void>> nodeValuesStream) {
        if (args == null) {
            throw new ARQInternalErrorException(this.getClass().getName()
                    + ": Null args list");
        }
        int size = args.size();
        NodeValue v1 = args.size() >= 1 ? args.get(0) : null;
        NodeValue v2 = args.size() >= 2 ? args.get(1) : null;
        NodeValue v3 = args.size() >= 3 ? args.get(2) : null;
        NodeValue v4 = args.size() >= 4 ? args.get(3) : null;
        return exec(v1, v2, v3, v4, nodeValuesStream);
    }

    /**
     * {@inheritDoc}
     */
    public abstract CompletableFuture<Void> exec(NodeValue v1, NodeValue v2, NodeValue v3, NodeValue v4, Function<Collection<List<NodeValue>>, CompletableFuture<Void>> nodeValuesStream);
}
