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
package fr.emse.ci.sparqlext.iterator.library;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.iterator.ExecutionControl;
import fr.emse.ci.sparqlext.iterator.IteratorStreamFunctionBase0;
import java.util.ArrayList;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/dummy">iter:dummy</a>
 * outputs 1 to 3 numbers every 1 to 3 seconds, until 10 is reached. It takes no
 * parameters.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class ITER_dummy extends IteratorStreamFunctionBase0 {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_dummy.class);
    public static final String URI = SPARQLExt.ITER + "dummy";
    

    public ITER_dummy() {
    }

    private static int MAX = 5;

    @Override
    public void exec(Consumer<List<List<NodeValue>>> nodeValuesStream, ExecutionControl control) {

        ExecutorService executor = (ExecutorService) getContext().get(SPARQLExt.EXECUTOR);
        executor.execute(() -> {
            Random r = new Random();

            int i = 0;
            while (i < MAX) {
                int add = r.nextInt(2) + 2;
                int next = Math.min(MAX, i + add);
                final List<List<NodeValue>> listNodeValues = new ArrayList<>();
                for (; i < next; i++) {
                    NodeValueInteger n = new NodeValueInteger(i);
                    listNodeValues.add(Collections.singletonList(n));
                }
                long waitingTime = (long) (r.nextGaussian() * 100 + 200);
                waitingTime = Math.min(waitingTime, 400);
                LOG.info("Will sleep " + waitingTime);
                try {
                    TimeUnit.MILLISECONDS.sleep(waitingTime);
                } catch (InterruptedException ex) {
                    LOG.debug("interrupted");
                }
                nodeValuesStream.accept(listNodeValues);

            }
            control.complete();
        });
    }

}
