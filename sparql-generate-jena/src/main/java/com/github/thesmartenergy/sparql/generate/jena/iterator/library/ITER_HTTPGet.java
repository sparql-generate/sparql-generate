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

package com.github.thesmartenergy.sparql.generate.jena.iterator.library;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorStreamFunctionBase;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.jena.sparql.util.Context;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/ITER_HTTPGet">iter:HTTPGet</a>
 * binds the responses of regular GET operations to a HTTP(s) URL.
 *
 * <ul>
 * <li>Param 1: (a String) the Web URI where regular GET operations are operated;</li>
 * <li>Param 2: (a positive Integer) the number of seconds between successive
 * calls to the Web API;</li>
 * <li>Param 3 (optional): the total number of calls to make (a positive
 * Integer). If not provided, the iterator never ends.</li>
 * </ul>
 * <p>
 * <b>Example: </b><br>
 * <p>The clause</p>
 * <code>ITERATOR iter:HTTPGet("https://example.org/room1/temperature",60) AS ?temperature</code>
 * <p>will fetch the temperature of room 1 every 60 seconds, indefinetely.
 *
 * @author El-Mehdi Khalfi <el-mehdi.khalfi at emse.fr>
 * @since 2018-09-27
 */
public class ITER_HTTPGet extends IteratorStreamFunctionBase {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ITER_HTTPGet.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITER + "HTTPGet";
    
    public static int MAX = Integer.MAX_VALUE;

    @Override
    public void checkBuild(ExprList args) {

    }

    @Override
    public void exec(List<NodeValue> args, Consumer<List<List<NodeValue>>> nodeValuesStream) {
        if (!args.get(0).isString()) {
            LOG.debug("First argument must be a string, got: " + args.get(0));
            throw new ExprEvalException("First argument must be a string, got: " + args.get(0));
        } else if (args.get(0).asString().isEmpty()) {
            LOG.debug("First argument is an empty string");
            throw new ExprEvalException("First argument is an empty string");
        }
        if (!args.get(1).isInteger() || args.get(1).getInteger().intValue() < 0) {
            LOG.debug("Second argument must be a positive integer, got: " + args.get(1));
            throw new ExprEvalException("Second argument must be an integer, got: " + args.get(1));
        }
        if (args.size() == 3 && (!args.get(2).isInteger() || args.get(2).getInteger().intValue() < 0)) {
            LOG.debug("Third argument must be a positive integer, got: " + args.get(2));
            throw new ExprEvalException("Third argument must be an integer, got: " + args.get(2));
        }

        String url_s = args.get(0).asString();
        int recurrenceValue = args.get(1).getInteger().intValue();
        int times = args.size() == 3 ? args.get(2).getInteger().intValue() : 0;

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger i_call = new AtomicInteger();

        Runnable task = () -> {
            Context context = getContext();
            SPARQLGenerate.registerThread(context);            
            String message;
            try {
                URL url = new URL(url_s);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                int status = con.getResponseCode();

                if (status != 200) {
                    LOG.debug("HTTP error " + status + " accessing " + url_s + " : " + con.getResponseMessage());
                    con.disconnect();
                    throw new RuntimeException("HTTP error " + status + " accessing " + url_s + " : " + con.getResponseMessage());
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = con.getInputStream().read(buffer)) != -1) {
                    baos.write(buffer, 0, length);
                }
                message = baos.toString();

                String out = message;
                if (out.length() > 200) {
                    out = out.substring(0, 120) + "\n ... \n" + out.substring(out.length() - 80);
                }
                LOG.debug("message retrieved: " + out);
            } catch (MalformedURLException e) {
                scheduler.shutdown();
                LOG.debug("An MalformedURLException occurred", e);
                throw new ExprEvalException("A MalformedURLException occurred", e);
            } catch (IOException e) {
                scheduler.shutdown();
                LOG.debug("An IOException occurred : ", e);
                throw new ExprEvalException("An IOException occurred", e);
            }
            NodeValue outnode = new NodeValueNode(NodeFactory.createLiteral(message));

            nodeValuesStream.accept(Collections.singletonList(Collections.singletonList(outnode)));
            if (times != 0 && i_call.getAndIncrement() >= Math.min(MAX, times)) {
                if(i_call.get() >= MAX) {
                    LOG.warn("The maximum number of operations has been reached. Change ITER_HTTPGet.MAX to overcome this limtation.");
                }
                LOG.info("Process finished after " + i_call.getAndDecrement() + "calls.");
                scheduler.shutdown();
            }
            SPARQLGenerate.unregisterThread(context);            
        };
        scheduler.scheduleAtFixedRate(task, 0, recurrenceValue, TimeUnit.SECONDS);
    }
}
