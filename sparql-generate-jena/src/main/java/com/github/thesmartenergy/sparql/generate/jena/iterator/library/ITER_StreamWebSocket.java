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
 * <a href="http://w3id.org/sparql-generate/iter/StreamWebSocket">iter:StreamWebSocket</a>
 * connects to a WebSocket server for a given amount of seconds, and streams data coming from it to the SPARQL-Generate engine.
 * Receiving data can be initiated by just connecting to the server of by sending a specific message (containing a query to specify which stream of data to retrieve).
 *
 * <ul>
 * <li>the first parameter is the WebSocket server URI to connect to;</li>
 * <li>the second parameter is the number of seconds to be taken to retrieve data from the server;</li>
 * <li>the third parameter (optional) is the message to be send to the server (e.g., a json query) to initiate data receiving;</li>
 * </ul>
 * <p>
 * <b>Example: </b>
 * Using <tt>ITERATOR iter:StreamWebSocket("wss://api.gemini.com/v1/marketdata/BTCUSD",10,"") AS ?events</tt> will return (in each iteration) the messages received from the WebSocket server (Gemini, a cryptocurrency exchange platform):<br>
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
 * after 10 seconds, connection with the server will be closed.
 *
 * @author El-Mehdi Khalfi <el-mehdi.khalfi at emse.fr>
 * @since 2018-09-26
 */

public class ITER_StreamWebSocket extends IteratorStreamFunctionBase {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ITER_CSVStream.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITER + "StreamWebSocket";

    @Override
    public void checkBuild(ExprList args) {
        if (args.size() != 2 && args.size() != 3) {
            throw new QueryBuildException("Function '"
                    + this.getClass().getName() + "' takes two or three parameters: (1) the server URI to connect to, (2) the number of seconds to be taken to retrieve data from the WebSocket, and, if needed, (3) the message to be send to the server (e.g., a json query).");
        }
    }

    @Override
    public void exec(List<NodeValue> args, Consumer<List<List<NodeValue>>> nodeValuesStream) {
        String url = args.get(0).asString();
        int duration = args.get(1).getInteger().intValue();
        String query = args.size() == 3 ? args.get(2).asString() : "";

        try {
            WebSocketClient webSocketClient = new WebSocketClient(new URI(url)) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    LOG.info("Connection to " + url + " successfull !");
                    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

                    Runnable task = () -> {
                        this.close();
                    };
                    scheduler.schedule(task, duration, TimeUnit.SECONDS);
                }

                @Override
                public void onMessage(String s) {
                    Node node = NodeFactory.createLiteral(s);
                    NodeValue nodeValue = new NodeValueNode(node);
                    nodeValuesStream.accept(Collections.singletonList(Collections.singletonList(nodeValue)));
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    LOG.warn(duration + " seconds is elapsed. Connection with " + url + " closed !");
                }

                @Override
                public void onError(Exception e) {

                }
            };
            webSocketClient.connectBlocking();
            if (!query.isEmpty()) {
                LOG.info("Sending " + query + " to the server");
                webSocketClient.send(query);
            } else
                LOG.warn("As specified, no message will be send the WebSocket server");
        } catch (URISyntaxException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
