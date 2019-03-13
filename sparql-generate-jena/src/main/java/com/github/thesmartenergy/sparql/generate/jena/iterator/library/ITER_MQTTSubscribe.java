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
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.jena.sparql.util.Context;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/ITER_MQTTSubscribe">iter:MQTTSubscribe</a>
 * connects to a MQTT server, subscribes to some topics, and issues bindings for
 * the messages and their topic when they are received.
 *
 * <ul>
 * <li>Param 1: (a String) the MQTT server. Two types of connection are
 * supported tcp:// for a TCP connection and ssl:// for a TCP connection secured
 * by SSL/TLS;</li>
 * <li>Param 2 .. <em>n</em> (optional, Strings): topics to subscribe to.</li>
 * </ul>
 *
 * <p>
 * For SSL configuration (programmatic or using JVM arguments), see
 * <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html#MqttClient-java.lang.String-java.lang.String-">the
 * constructor of MqttClient</a>.
 * <p>
 * Additional connection OPTIONS configuration can be programmatically set using
 * the {@link #OPTIONS} object.</p>
 * <p>
 * By default, the MQTT message (byte[]) is simply used as parameter of the
 * <code>String</code>. Other behaviour may be implemented by changing the
 * {@link #PARSER} object.</p>
 *
 * @author El-Mehdi Khalfi <el-mehdi.khalfi at emse.fr>, Maxime Lefrançois
 * <maxime.lefrancois at emse.fr>
 * @since 2018-10-02
 */
public class ITER_MQTTSubscribe extends IteratorStreamFunctionBase {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_MQTTSubscribe.class);

    public static final String URI = SPARQLGenerate.ITER + "MQTTSubscribe";

    public static final MqttConnectOptions OPTIONS = new MqttConnectOptions();

    public static int MAX = Integer.MAX_VALUE;
    
    static {
        OPTIONS.setAutomaticReconnect(true);
        OPTIONS.setCleanSession(true);
        OPTIONS.setConnectionTimeout(10);
    }

    public static Function<byte[], NodeValue> PARSER = (message) -> {
        String messageString = new String(message);
        return new NodeValueNode(NodeFactory.createLiteral(messageString));
    };

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
        if (args.size() > 1) {
            for (int i = 1; i < args.size(); i++) {
                if (!args.get(i).isString()) {
                    LOG.debug("Argument " + i + " must be a string, got: " + args.get(i));
                    throw new ExprEvalException("Argument " + i + " must be a string, got: " + args.get(i));
                }
            }
        }

        final Context context = getContext();
        try {
            IMqttClient mqttClient = new MqttClient(args.get(0).asString(), MqttClient.generateClientId());

            mqttClient.setCallback(new MyMqttCallback(context, nodeValuesStream));

            mqttClient.connect(OPTIONS);
            if (args.size() > 1) {
                for (int i = 1; i < args.size(); i++) {
                    mqttClient.subscribe(args.get(i).asString());
                }
            }
        } catch (MqttException e) {
            LOG.debug("A MqttException occurred", e);
            throw new ExprEvalException("A MqttException occurred", e);
        }

    }

    private class MyMqttCallback implements MqttCallback {

        private final Context context;

        private final Consumer<List<List<NodeValue>>> nodeValuesStream;

        MyMqttCallback(Context context, Consumer<List<List<NodeValue>>> nodeValuesStream) {
            this.context = context;
            this.nodeValuesStream = nodeValuesStream;
            SPARQLGenerate.registerThread(context);
        }

        @Override
        public void connectionLost(Throwable cause) {
            LOG.debug("MQTT Connection is lost", cause);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            SPARQLGenerate.registerThread(context);            
            List<NodeValue> nv = new ArrayList<>();
            nv.add(new NodeValueNode(NodeFactory.createLiteral(topic)));
            nv.add(PARSER.apply(message.getPayload()));
            nodeValuesStream.accept(Collections.singletonList(nv));
            SPARQLGenerate.unregisterThread(context);            
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    }

}
