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
package fr.emse.ci.sparqlext.iterator;

import java.util.List;
import java.util.function.Consumer;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;

/**
 * Support for a iterator function of four arguments.
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
    public final void exec(List<NodeValue> args, Consumer<List<List<NodeValue>>> nodeValuesStream) {
        if (args == null) {
            throw new ARQInternalErrorException(this.getClass().getName()
                    + ": Null args list");
        }
        NodeValue v1 = args.size() >= 1 ? args.get(0) : null;
        NodeValue v2 = args.size() >= 2 ? args.get(1) : null;
        NodeValue v3 = args.size() >= 3 ? args.get(2) : null;
        NodeValue v4 = args.size() >= 4 ? args.get(3) : null;
        exec(v1, v2, v3, v4, nodeValuesStream);
    }

    /**
     * {@inheritDoc}
     */
    public abstract void exec(NodeValue v1, NodeValue v2, NodeValue v3, NodeValue v4, Consumer<List<List<NodeValue>>> nodeValuesStream);
}
