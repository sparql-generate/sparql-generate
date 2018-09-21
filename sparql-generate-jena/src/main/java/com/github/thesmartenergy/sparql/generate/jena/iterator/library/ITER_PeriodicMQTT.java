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
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorStreamFunctionBase2;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.eclipse.paho.client.mqttv3.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class ITER_PeriodicMQTT extends IteratorStreamFunctionBase2 {

    public static final String URI = SPARQLGenerate.ITER + "PeriodicMQTT";

    @Override
    public void exec(NodeValue url, NodeValue period, Consumer<List<List<NodeValue>>> nodeValuesStream) {
        String publisherId = UUID.randomUUID().toString();
        IMqttClient mqttClient = null;
        try {
            mqttClient = new MqttClient("tcp://localhost:1883", publisherId);


            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    List<List<NodeValue>> nvs = new ArrayList<>();
                    String payload = new String(message.getPayload());
                    System.out.println(">> " + payload);
                    for (int i = 0; i < payload.length(); i++) {
                        String s = Character.toString(payload.charAt(i));
                        NodeValue n = new NodeValueNode(NodeFactory.createLiteral(s));
                        List<NodeValue> nv = new ArrayList<>();
                        nv.add(n);
                        nvs.add(nv);
                    }
                    nodeValuesStream.accept(nvs);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });

            mqttClient.connect(options);
            mqttClient.subscribe("testTopic");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
