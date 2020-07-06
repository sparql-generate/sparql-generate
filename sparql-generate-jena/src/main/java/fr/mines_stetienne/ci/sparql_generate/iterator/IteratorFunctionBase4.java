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
package fr.mines_stetienne.ci.sparql_generate.iterator;

import java.util.List;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;

/**
 * Support for a iterator function of four arguments.
 */
public abstract class IteratorFunctionBase4 extends IteratorFunctionBase {

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkBuild(ExprList args) {
        if (args.size() != 4) {
            throw new QueryBuildException("Selector '"
                    + this.getClass().getName() + "' takes 4 arguments");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final List<List<NodeValue>> exec(List<NodeValue> args) {
        if (args == null) {
            throw new ARQInternalErrorException(this.getClass().getName()
                    + ": Null args list");
        }
        if (args.size() != 4) {
            throw new ExprEvalException(this.getClass().getName()
                    + ": Wrong number of arguments: Wanted 4, got "
                    + args.size());
        }
        NodeValue v1 = args.get(0);
        NodeValue v2 = args.get(1);
        NodeValue v3 = args.get(2);
        NodeValue v4 = args.get(3);
        return exec(v1, v2, v3, v4);
    }

    /**
     * {@inheritDoc}
     */
    public abstract List<List<NodeValue>> exec(NodeValue v1, NodeValue v2, NodeValue v3, NodeValue v4);
}
