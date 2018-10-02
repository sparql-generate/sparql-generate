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
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/WebSocket">iter:WebSocket</a>
 * connects to a WebSocket server for a given number of seconds, and iteratively
 * binds the messages that are received.
 * 
 * Optionally, a specific message can be sent to the WebSocket server as a first
 * step (This message is usually a query that specifies what stream of data is
 * to be retrieved).
 *
 * <ul>
 * <li>Param 1: (a String) is the WebSocket server URI to connect to;</li>
 * <li>Param 2: (an Integer) is the number of seconds after which the connection
 * is closed. Use 0 to never close the connection;</li>
 * <li>Param 3: (a String, optional) is the message that will be first sent
 * to the server (e.g., a json query).</li>
 * </ul>
 * <p>
 * <b>Example: </b><br>
 * <p>The clause</p>
 * <code>ITERATOR iter:WebSocket("wss://api.gemini.com/v1/marketdata/BTCUSD",10) AS ?events</code>
 * <p>will connect to the Gemini WebSocket server (a cryptocurrency exchange
 * platform) for 10 seconds and iteratively produce the following bindings:</p>
 * <pre>
 * ?events => {"type":"update","eventId":4568099292,"timestamp":1537974017,"timestampms":1537974017597,"socket_sequence":1,"events":[{"type":"change","side":"ask","price":"6513.20","remaining":"0","delta":"-1.321","reason":"cancel"}]}<br>
 * ?events => {"type":"update","eventId":4568099362,"timestamp":1537974018,"timestampms":1537974018434,"socket_sequence":2,"events":[{"type":"change","side":"bid","price":"1.00","remaining":"5666.001","delta":"0.001","reason":"place"}]}<br>
 * ?events => {"type":"update","eventId":4568099456,"timestamp":1537974019,"timestampms":1537974019735,"socket_sequence":3,"events":[{"type":"change","side":"bid","price":"6504.97","remaining":"0","delta":"-1","reason":"cancel"}]}<br>
 * ?events => {"type":"update","eventId":4568099464,"timestamp":1537974019,"timestampms":1537974019919,"socket_sequence":4,"events":[{"type":"change","side":"ask","price":"6513.20","remaining":"0.104","delta":"0.104","reason":"place"}]}<br>
 * ?events => {"type":"update","eventId":4568099470,"timestamp":1537974020,"timestampms":1537974020197,"socket_sequence":5,"events":[{"type":"trade","tid":4568099470,"price":"6508.45","amount":"0.02441407","makerSide":"ask"},{"type":"change","side":"ask","price":"6508.45","remaining":"0.45117186","delta":"-0.02441407","reason":"trade"}]}<br>
 * ?events => {"type":"update","eventId":4568099488,"timestamp":1537974020,"timestampms":1537974020414,"socket_sequence":6,"events":[{"type":"change","side":"bid","price":"6492.03","remaining":"0","delta":"-0.2","reason":"cancel"}]}<br>
 * ?events => {"type":"update","eventId":4568099494,"timestamp":1537974020,"timestampms":1537974020515,"socket_sequence":7,"events":[{"type":"change","side":"ask","price":"6544.36","remaining":"0","delta":"-0.2","reason":"cancel"}]}<br>
 * ?events => {"type":"update","eventId":4568099498,"timestamp":1537974020,"timestampms":1537974020542,"socket_sequence":8,"events":[{"type":"change","side":"ask","price":"6513.20","remaining":"0","delta":"-0.104","reason":"cancel"}]}<br>
 * ?events => {"type":"update","eventId":4568099513,"timestamp":1537974020,"timestampms":1537974020623,"socket_sequence":9,"events":[{"type":"change","side":"bid","price":"6491.40","remaining":"0.2","delta":"0.2","reason":"place"}]}<br>
 * ?events => {"type":"update","eventId":4568099529,"timestamp":1537974020,"timestampms":1537974020723,"socket_sequence":10,"events":[{"type":"change","side":"ask","price":"6543.79","remaining":"0.2","delta":"0.2","reason":"place"}]}<br>
 * ?events => {"type":"update","eventId":4568099531,"timestamp":1537974020,"timestampms":1537974020726,"socket_sequence":11,"events":[{"type":"change","side":"bid","price":"1.00","remaining":"5666","delta":"-0.001","reason":"cancel"}]}<br>
 * ?events => {"type":"update","eventId":4568099631,"timestamp":1537974021,"timestampms":1537974021785,"socket_sequence":12,"events":[{"type":"change","side":"bid","price":"6504.37","remaining":"1","delta":"1","reason":"place"}]}<br>
 * </pre>
 *
 * @author El-Mehdi Khalfi <el-mehdi.khalfi at emse.fr>
 * @since 2018-09-26
 */

public class ITER_WebSocket extends IteratorStreamFunctionBase {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ITER_CSVStream.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITER + "WebSocket";

    @Override
    public void checkBuild(ExprList args) {
        if (args.size() != 2 && args.size() != 3) {
            throw new QueryBuildException("Function '"
                    + this.getClass().getName() + "' takes two or three parameters: (1) the server URI to connect to, (2) the number of seconds to be taken to retrieve data from the WebSocket, and, if needed, (3) the message to be send to the server (e.g., a json query).");
        }
    }

    @Override
    public void exec(List<NodeValue> args, Consumer<List<List<NodeValue>>> nodeValuesStream) {
        if (!args.get(0).isString()) {
            LOG.debug("First argument must be a string, got: " + args.get(0));
            throw new ExprEvalException("First argument must be a string, got: " + args.get(0));
        }
        if (!args.get(1).isInteger() || args.get(1).getInteger().intValue() < 0) {
            LOG.debug("Second argument must be a positive integer, got: " + args.get(1));
            throw new ExprEvalException("Second argument must be an integer, got: " + args.get(1));
        }
        if (args.size() == 3 && !args.get(2).isString()) {
            LOG.debug("Third argument must be a string, got: " + args.get(2));
            throw new ExprEvalException("Third argument must be a string, got: " + args.get(2));
        }

        String url = args.get(0).asString();
        int duration = args.get(1).getInteger().intValue();

        String query = "";
        if (args.size() == 3) {
            query = args.get(2).asString();
        }

        try {
            WebSocketClient webSocketClient = new WebSocketClient(new URI(url)) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    LOG.debug("Connection to " + url + " successful !");
                    if (duration != 0) {
                        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                        Runnable task = () -> this.close();
                        scheduler.schedule(task, duration, TimeUnit.SECONDS);
                    }
                }

                @Override
                public void onMessage(String s) {
                    Node node = NodeFactory.createLiteral(s);
                    NodeValue nodeValue = new NodeValueNode(node);
                    nodeValuesStream.accept(Collections.singletonList(Collections.singletonList(nodeValue)));
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    LOG.debug(duration + " seconds is elapsed. Connection with " + url + " closed !");
                }

                @Override
                public void onError(Exception e) {
                    LOG.debug("An error occurred ", e);
                }
            };
            webSocketClient.connectBlocking();
            if (!query.isEmpty()) {
                LOG.debug("Sending " + query + " to the server");
                webSocketClient.send(query);
            } else
                LOG.debug("As specified, no message will be send the WebSocket server");
        } catch (URISyntaxException | InterruptedException e) {
            LOG.debug("No evaluation for " + args, e);
            throw new ExprEvalException("No evaluation for " + args, e);
        }
    }
}
