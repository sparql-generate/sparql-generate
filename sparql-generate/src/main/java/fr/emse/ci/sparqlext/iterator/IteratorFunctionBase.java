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
package fr.emse.ci.sparqlext.iterator;

import java.util.List;
import java.util.function.Consumer;
import org.apache.jena.sparql.expr.NodeValue;

/**
 * The base implementation of the {@link IteratorFunction} interface.
 */
public abstract class IteratorFunctionBase extends IteratorStreamFunctionBase {

    @Override
    public final void exec(List<NodeValue> args, Consumer<List<List<NodeValue>>> nodeValuesStream) {
        nodeValuesStream.accept(exec(args));
        complete();
    }

    /**
     * IteratorFunction call to a list of evaluated argument values.
     *
     * @param args -
     * @return -
     */
    public abstract List<List<NodeValue>> exec(List<NodeValue> args);

}
