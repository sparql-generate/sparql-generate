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
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;

/**
 * Support for a iterator function of two arguments.
 */
public abstract class IteratorStreamFunctionBase2 extends IteratorStreamFunctionBase {

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkBuild(ExprList args) {
        if (args.size() != 2) {
            throw new QueryBuildException("Selector '"
                    + this.getClass().getName() + "' takes two arguments");
        }
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
        if (args.size() != 2) {
            throw new ExprEvalException(this.getClass().getName()
                    + ": Wrong number of arguments: Wanted 2, got "
                    + args.size());
        }
        NodeValue v1 = args.get(0);
        NodeValue v2 = args.get(1);
        exec(v1, v2, nodeValuesStream);
    }

    /**
     * {@inheritDoc}
     */
    public abstract void exec(NodeValue v1, NodeValue v2, Consumer<List<List<NodeValue>>> nodeValuesStream);
}
