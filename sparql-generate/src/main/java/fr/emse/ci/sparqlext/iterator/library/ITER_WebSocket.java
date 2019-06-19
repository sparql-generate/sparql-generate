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
import fr.emse.ci.sparqlext.iterator.IteratorStreamFunctionBase;
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
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QueryBuildException;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/WebSocket">iter:WebSocket</a>
 * connects to a WebSocket server, and iteratively binds the messages that are
 * received.
 *
 * <p>
 * See
 * <a href="https://w3id.org/sparql-generate/playground.html#ex=example/generate/Example-WebSocket">Live
 * example</a></p>
 *
 * Optionally, a specific message can be sent to the WebSocket server as a first
 * step (This message is usually a query that specifies what stream of data is
 * to be retrieved).
 *
 * <ul>
 * <li>Param 1: (a String or URI) is the WebSocket server URI to connect
 * to;</li>
 * <li>Param 3: (a String, optional) is the message that will be first sent to
 * the server (e.g., a json query).</li>
 * </ul>
 * <p>
 * <b>Example: </b><br>
 * <p>
 * The clause</p>
 * <code>ITERATOR iter:WebSocket("wss://api.gemini.com/v1/marketdata/BTCUSD") AS ?events</code>
 * <p>
 * will connect to the Gemini WebSocket server (a cryptocurrency exchange
 * platform) forever and iteratively produce the following bindings:</p>
 * <pre>
 * ?events => {"type":"update","eventId":4568099292,"timestamp":1537974017,"timestampms":1537974017597,"socket_sequence":1,"events":[{"type":"change","side":"ask","price":"6513.20","remaining":"0","delta":"-1.321","reason":"cancel"}]}
 * ?events => {"type":"update","eventId":4568099362,"timestamp":1537974018,"timestampms":1537974018434,"socket_sequence":2,"events":[{"type":"change","side":"bid","price":"1.00","remaining":"5666.001","delta":"0.001","reason":"place"}]}
 * ?events => {"type":"update","eventId":4568099456,"timestamp":1537974019,"timestampms":1537974019735,"socket_sequence":3,"events":[{"type":"change","side":"bid","price":"6504.97","remaining":"0","delta":"-1","reason":"cancel"}]}
 * ?events => {"type":"update","eventId":4568099464,"timestamp":1537974019,"timestampms":1537974019919,"socket_sequence":4,"events":[{"type":"change","side":"ask","price":"6513.20","remaining":"0.104","delta":"0.104","reason":"place"}]}
 * ?events => {"type":"update","eventId":4568099470,"timestamp":1537974020,"timestampms":1537974020197,"socket_sequence":5,"events":[{"type":"trade","tid":4568099470,"price":"6508.45","amount":"0.02441407","makerSide":"ask"},{"type":"change","side":"ask","price":"6508.45","remaining":"0.45117186","delta":"-0.02441407","reason":"trade"}]}
 * ?events => {"type":"update","eventId":4568099488,"timestamp":1537974020,"timestampms":1537974020414,"socket_sequence":6,"events":[{"type":"change","side":"bid","price":"6492.03","remaining":"0","delta":"-0.2","reason":"cancel"}]}
 * ?events => {"type":"update","eventId":4568099494,"timestamp":1537974020,"timestampms":1537974020515,"socket_sequence":7,"events":[{"type":"change","side":"ask","price":"6544.36","remaining":"0","delta":"-0.2","reason":"cancel"}]}
 * ?events => {"type":"update","eventId":4568099498,"timestamp":1537974020,"timestampms":1537974020542,"socket_sequence":8,"events":[{"type":"change","side":"ask","price":"6513.20","remaining":"0","delta":"-0.104","reason":"cancel"}]}
 * ?events => {"type":"update","eventId":4568099513,"timestamp":1537974020,"timestampms":1537974020623,"socket_sequence":9,"events":[{"type":"change","side":"bid","price":"6491.40","remaining":"0.2","delta":"0.2","reason":"place"}]}
 * ?events => {"type":"update","eventId":4568099529,"timestamp":1537974020,"timestampms":1537974020723,"socket_sequence":10,"events":[{"type":"change","side":"ask","price":"6543.79","remaining":"0.2","delta":"0.2","reason":"place"}]}
 * ?events => {"type":"update","eventId":4568099531,"timestamp":1537974020,"timestampms":1537974020726,"socket_sequence":11,"events":[{"type":"change","side":"bid","price":"1.00","remaining":"5666","delta":"-0.001","reason":"cancel"}]}
 * ?events => {"type":"update","eventId":4568099631,"timestamp":1537974021,"timestampms":1537974021785,"socket_sequence":12,"events":[{"type":"change","side":"bid","price":"6504.37","remaining":"1","delta":"1","reason":"place"}]}
 * </pre>
 *
 * @author El-Mehdi Khalfi <el-mehdi.khalfi at emse.fr>
 * @since 2018-09-26
 */
public class ITER_WebSocket extends IteratorStreamFunctionBase {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ITER_WebSocket.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLExt.ITER + "WebSocket";

    @Override
    public void checkBuild(ExprList args) {
        if (args.size() != 1 && args.size() != 2) {
            throw new QueryBuildException("Function '"
                    + this.getClass().getName() + "' takes two arguments: (1) "
                    + "the server URI to connect to, (2) the message to be sent to"
                    + " the server (e.g., a json query).");
        }
    }

    @Override
    public void exec(
            final List<NodeValue> args,
            final Consumer<List<List<NodeValue>>> listListNodeValue,
            final ExecutionControl control) {
        if (!args.get(0).isString() && !args.get(0).isIRI()) {
            LOG.debug("First argument must be a string or a URI, got: " + args.get(0));
            throw new ExprEvalException("First argument must be a string or a URI, got: " + args.get(0));
        }
        final String url_s = args.get(0).isString() ? args.get(0).asString() : args.get(0).asNode().getURI();

        if (args.size() == 2 && !args.get(1).isString()) {
            LOG.debug("Second argument must be a string, got: " + args.get(1));
            throw new ExprEvalException("Second argument must be a string, got: " + args.get(1));
        }
        String query = args.size() == 2 ? args.get(1).asString() : "";

        final Executor executor = (Executor) getContext().get(SPARQLExt.EXECUTOR);
        try {
            WebSocketClient webSocketClient = new WebSocketClient(new URI(url_s)) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    LOG.debug("Connection to " + url_s + " successful !");
                }

                @Override
                public void onMessage(String s) {
                    executor.execute(() -> {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Message arrived " + SPARQLExt.compress(s));
                        }
                        Node node = NodeFactory.createLiteral(s);
                        NodeValue nodeValue = new NodeValueNode(node);
                        listListNodeValue.accept(Collections.singletonList(Collections.singletonList(nodeValue)));
                    });
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    LOG.debug("Websocket connection closed, stopping iterator.");
                    control.complete();
                }

                @Override
                public void onError(Exception e) {
                    if (e instanceof RejectedExecutionException) {
                        LOG.debug("Websocket interrupted");
                        close();
                    } else {
                        LOG.debug("An error occurred ", e);
                    }
                }
            };
            webSocketClient.connectBlocking();
            if (!query.isEmpty()) {
                LOG.debug("Sending " + query + " to the server");
                webSocketClient.send(query);
            }

            SPARQLExt.addTaskOnClose(getContext(), () -> webSocketClient.close());

        } catch (URISyntaxException e) {
            LOG.debug("URISyntaxException " + args, e);
            throw new ExprEvalException("URISyntaxException " + args, e);
        } catch (InterruptedException e) {
            LOG.debug("WebSocket interrupted " + args);
            throw new ExprEvalException("InterruptedException " + args, e);
        }
    }

}
