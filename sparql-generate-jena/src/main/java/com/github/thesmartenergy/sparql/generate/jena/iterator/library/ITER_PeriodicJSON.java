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
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorStreamFunctionBase4;
import com.google.gson.Gson;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ITER_PeriodicJSON extends IteratorStreamFunctionBase4 {

    public static final String URI = SPARQLGenerate.ITER + "PeriodicJSON";

    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/application/json";

    @Override
    public void exec(NodeValue url, NodeValue period, NodeValue times, NodeValue jsonquery, Consumer<List<List<NodeValue>>> nodeValuesStream) {
        /*String url_s = url.asString();
        Client client = ClientBuilder.newClient();
        System.out.println("1: " + Thread.currentThread().getName());
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger x = new AtomicInteger();
        Runnable task = () -> {
            System.out.println("Hi " + Thread.currentThread().getName() + " !");
            String json = client.target(url_s).request(MediaType.APPLICATION_JSON).get(String.class);
            Configuration conf = Configuration.builder().options(Option.ALWAYS_RETURN_LIST).build();
            List<Object> values = JsonPath
                    .using(conf)
                    .parse(json)
                    .read(jsonquery.asString());
            List<NodeValue> nodeValues = new ArrayList<>(values.size());
            Gson gson = new Gson();
            for (Object value : values) {
                RDFDatatype dt = TypeMapper.getInstance().getSafeTypeByName(datatypeUri);
                String jsonstring = gson.toJson(value);
                Node node = NodeFactory.createLiteral(jsonstring, dt);
                NodeValue nodeValue = new NodeValueNode(node);
                nodeValues.add(nodeValue);
            }
            nodeValuesStream.accept(Collections.singletonList(nodeValues));

            if (x.getAndIncrement() == times.getInteger().intValue()) {
                System.out.println("X : " + x.get());
                Thread.currentThread().interrupt();
            }
        };
        scheduler.scheduleAtFixedRate(task, 0, period.getInteger().intValue(), TimeUnit.SECONDS);*/

        String url_s = url.asString();
        Client client = ClientBuilder.newClient();

        int x = 0;
        while ((x++) != times.getInteger().intValue()) {
            System.out.println("Iteration " + x);
            String json = client.target(url_s).request(MediaType.APPLICATION_JSON).get(String.class);
            Configuration conf = Configuration.builder().options(Option.ALWAYS_RETURN_LIST).build();
            List<Object> values = JsonPath
                    .using(conf)
                    .parse(json)
                    .read(jsonquery.asString());
            List<NodeValue> nodeValues = new ArrayList<>(values.size());
            Gson gson = new Gson();
            for (Object value : values) {
                RDFDatatype dt = TypeMapper.getInstance().getSafeTypeByName(datatypeUri);
                String jsonstring = gson.toJson(value);
                Node node = NodeFactory.createLiteral(jsonstring, dt);
                NodeValue nodeValue = new NodeValueNode(node);
                nodeValues.add(nodeValue);
            }
            nodeValuesStream.accept(Collections.singletonList(nodeValues));
            try {
                Thread.sleep(period.getInteger().intValue()*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
