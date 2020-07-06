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
package fr.mines_stetienne.ci.sparql_generate.iterator.library;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionBase3;
import java.math.BigDecimal;
import java.util.ArrayList;

import java.util.Collections;
import java.util.List;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDecimal;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/for">iter:for</a>
 * iterates over numeric values that start at the first argument, increment by
 * the second argument (positive or negatice), and stops whenever it goes beyond
 * the third argument.
 *
 * <p>
 * See
 * <a href="https://w3id.org/sparql-generate/playground.html#ex=example/generate/Example-03-Iterators">Live
 * example</a></p>
 *
 * <ul>
 * <li>Param 1: (a decimal) the first value;</li>
 * <li>Param 2: (a decimal) the increment (positive or negative);</li>
 * <li>Param 3: (a decimal) the threshold that defines the condition for
 * stopping.</li>
 * </ul>
 *
 * @author Antoine Zimmermann <antoine.zimmermann at emse.fr>
 */
public class ITER_for extends IteratorFunctionBase3 {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_for.class);
    public static final String URI = SPARQLExt.ITER + "for";

    public ITER_for() {
    }

    @Override
    public List<List<NodeValue>> exec(NodeValue start, NodeValue incr, NodeValue stop) {
    	if(start == null || incr == null || stop == null) {
    		throw new ExprEvalException("Parameters must not be null.");
    	}
    	boolean isInteger = start.isInteger() && incr.isInteger() && stop.isInteger();
    	

        // the initial value
        BigDecimal value = start.getDecimal();

        // the size of the increment
        BigDecimal incrV = incr.getDecimal();

        // the increment must not be zero
        if (incrV.equals(BigDecimal.ZERO)) {
            LOG.debug("Increment value must be non zero.");
            throw new ExprEvalException("Increment value must be non zero.");
        }

        // the threshold above/under which it stops
        BigDecimal stopV = stop.getDecimal();

        final List<List<NodeValue>> listNodeValues = new ArrayList<>();

        while (stopV.subtract(value).multiply(incrV).compareTo(BigDecimal.ZERO) > 0) {
        	NodeValue nodeValue;
        	if(isInteger) {
        		nodeValue = new NodeValueInteger(value.toBigInteger());
        	} else {
        		nodeValue = new NodeValueDecimal(value);
        	}
            listNodeValues.add(Collections.singletonList(nodeValue));
            value = value.add(incrV);
        }
        return listNodeValues;
    }

}
