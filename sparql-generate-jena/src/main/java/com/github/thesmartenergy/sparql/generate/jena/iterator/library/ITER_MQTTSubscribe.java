///*
// * Copyright 2016 Ecole des Mines de Saint-Etienne.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.github.thesmartenergy.sparql.generate.jena.iterator.library;
//
//import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
//import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorStreamFunctionBase;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import org.apache.jena.sparql.expr.ExprEvalException;
//import org.apache.jena.sparql.expr.ExprList;
//import org.apache.jena.sparql.expr.NodeValue;
//import org.eclipse.paho.client.mqttv3.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.function.Consumer;
//import java.util.function.Function;
//import org.apache.commons.io.IOUtils;
//import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
//
///**
// * Iterator function
// * <a href="http://w3id.org/sparql-generate/iter/ITER_MQTTSubscribe">iter:MQTTSubscribe</a>
// * connects to a MQTT server, subscribes to some topics, and issues bindings for
// * the topic (first variable) and the message (second variable) when they are
// * received.
// *
// * <ul>
// * <li>Param 1: (a String or URL) the MQTT server. Two types of connection are
// * supported tcp:// for a TCP connection and ssl:// for a TCP connection secured
// * by SSL/TLS;</li>
// * <li>Param 2 .. <em>n</em> (optional, Strings): topics to subscribe to.</li>
// * </ul>
// *
// * <p>
// * For SSL configuration (programmatic or using JVM arguments), see
// * <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html#MqttClient-java.lang.String-java.lang.String-">the
// * constructor of MqttClient</a>.
// * <p>
// * Additional connection OPTIONS configuration can be programmatically set using
// * the MqttConnectOptions returned by {@link #getOptions}.
// * </p>
// * <p>
// * The MQTT message (byte[]) is assumed to be a UTF-8 string. This behaviour may
// * may changed using {@link #setParser} static method.
// * </p>
// *
// * @author El-Mehdi Khalfi <el-mehdi.khalfi at emse.fr>, Maxime Lefrançois
// * <maxime.lefrancois at emse.fr>
// * @since 2018-10-02
// */
//public class ITER_MQTTSubscribe extends IteratorStreamFunctionBase {
//
//    private static final Logger LOG = LoggerFactory.getLogger(ITER_MQTTSubscribe.class);
//
//    public static final String URI = SPARQLGenerate.ITER + "MQTTSubscribe";
//
//    public static final MqttConnectOptions OPTIONS = new MqttConnectOptions();
//
//    static {
//        OPTIONS.setAutomaticReconnect(true);
//        OPTIONS.setCleanSession(true);
//        OPTIONS.setConnectionTimeout(10);
//    }
//
//    public static Function<byte[], NodeValue> PARSER = (message) -> {
//        try {
//            String messageString = IOUtils.toString(message, StandardCharsets.UTF_8.name());
//            return new NodeValueString(messageString);
//        } catch (IOException ex) {
//            LOG.warn("Exception while parsing message " + new String(message), ex);
//            return null;
//        }
//    };
//
//    public static void setParser(Function<byte[], NodeValue> parser) {
//        PARSER = parser;
//    }
//
//    public static MqttConnectOptions getOptions() {
//        return OPTIONS;
//    }
//    
//    private final CompletableFuture<Void> future = new CompletableFuture<>();
//
//    @Override
//    public void checkBuild(ExprList args) {
//        if (args.size() < 1) {
//            LOG.debug("Expecting at least one argument");
//            throw new ExprEvalException("Expecting at least one argument");
//        }
//    }
//
//    @Override
//    public CompletableFuture<Void> exec(
//            final List<NodeValue> args,
//            final Consumer<Collection<List<NodeValue>>> nodeValuesStream) {
//        if (!args.get(0).isString() && !args.get(0).isIRI()) {
//            LOG.debug("First argument must be a string or a URI, got: " + args.get(0));
//            throw new ExprEvalException("First argument must be a string, got: " + args.get(0));
//        }
//        final String url_s = args.get(0).isString() ? args.get(0).asString() : args.get(0).asNode().getURI();
//
//        if (args.size() > 1) {
//            for (int i = 1; i < args.size(); i++) {
//                if (!args.get(i).isString()) {
//                    LOG.debug("Argument " + i + " must be a string, got: " + args.get(i));
//                    throw new ExprEvalException("Argument " + i + " must be a string, got: " + args.get(i));
//                }
//            }
//        }
//
//        try {
//            IMqttClient mqttClient = new MqttClient(url_s, MqttClient.generateClientId());
//
//            mqttClient.setCallback(new MqttCallback() {
//
//                @Override
//                public void connectionLost(Throwable cause) {
//                    LOG.debug("MQTT Connection is lost", cause);
//                    getContext().getExecutor().execute(() -> {
//                        LOG.debug("MQTT Connection is lost", cause);
//                    });
//                    future.complete(null);
//                }
//
//                @Override
//                public void messageArrived(String topic, MqttMessage message) throws Exception {
//                    LOG.debug("MQTT message arrived");
//                    getContext().getExecutor().execute(() -> {
//                        LOG.debug("MQTT message arrived");
//                        List<NodeValue> nv = new ArrayList<>();
//                        nv.add(new NodeValueString(topic));
//                        nv.add(PARSER.apply(message.getPayload()));
//                        nodeValuesStream.accept(Collections.singleton(nv));
//                    });
//                }
//
//                @Override
//                public void deliveryComplete(IMqttDeliveryToken token) {
//                }
//            });
//
//            mqttClient.connect(OPTIONS);
//            if (args.size() > 1) {
//                for (int i = 1; i < args.size(); i++) {
//                    mqttClient.subscribe(args.get(i).asString());
//                }
//            }
//
//            getContext().addTaskOnClose(() -> {
//                try {
//                    mqttClient.close();
//                } catch (MqttException ex) {
//                    LOG.debug("Error while trying to close mqttClient ", ex);
//                }
//            });
//
//            LOG.debug("Exiting the iterator! Does the MQTT connection still hold ?");
//
//        } catch (MqttException e) {
//            LOG.debug("A MqttException occurred", e);
//            throw new ExprEvalException("A MqttException occurred", e);
//        }
//        return future;
//    }
//}
