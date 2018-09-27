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
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/ITER_PeriodicJSON">iter:ITER_PeriodicJSON</a>
 * connects to a Web API <tt><i>n</i></tt> given number of times separated by a specified period (in seconds), and streams data coming from the server to the SPARQL-Generate engine.
 *
 * <ul>
 * <li>the first parameter (url) is the Web server URI to connect to;</li>
 * <li>the second parameter (recurrenceValue) is the number of seconds between successive calls to the Web API;</li>
 * <li>the third parameter (times) is the number of periodic calls to the Web API;</li>
 * <li>the fourth parameter (jsonPathExpression) is the <a href="https://github.com/json-path/JsonPath">JsonPath</a> expression to query some parts in the returned json document from the Web API;</li>
 * </ul>
 * <p>
 * <b>Example: </b><br>
 * Using <tt>ITERATOR iter:PeriodicJSON("https://api.jcdecaux.com/vls/v1/stations?contract=Lyon&apiKey=0844b898118d179a48a3d95969e7444d0de26a66",60,3,"$.*") AS ?bikeStation</tt> will, for every minute, send (to the SPARQL-Generate engine) the result of applying this JsonPath expression <tt>"$.*"</tt> on the returned json document from the Web API.<br>
 * This processs is repeated three times.<br>
 * Stream 1<br>
 * <pre>
 * ?bikeStation => { "number":2010, "contract_name":"Lyon", "name":"#2010 - CONFLUENCE DARSE", "bike_stands":22, "available_bike_stands":7, "available_bikes":15, "status":"OPEN", "last_update":1538039920000, "address":"ANGLE ALEE ANDRE MURE ET QUAI ANTOINE RIBOUD", "position":{ "lat":45.743317, "lng":4.815747 }, "banking":true, "bonus":false }
 * ?bikeStation => { "number":5015, "contract_name":"Lyon", "name":"#5015 - FULCHIRON", "bike_stands":19, "available_bike_stands":10, "available_bikes":9, "status":"OPEN", "last_update":1538039849000, "address":"Devant le n°41 rue de la Quarantaine", "position":{ "lat":45.75197, "lng":4.821662 }, "banking":true, "bonus":false }
 * ?bikeStation =>  ...
 * </pre>
 *
 * Stream 2<br>
 * <pre>
 * ?bikeStation => { "number":2010, "contract_name":"Lyon", "name":"#2010 - CONFLUENCE DARSE", "bike_stands":22, "available_bike_stands":1, "available_bikes":21, "status":"OPEN", "last_update":1538039950000, "address":"ANGLE ALEE ANDRE MURE ET QUAI ANTOINE RIBOUD", "position":{ "lat":45.743317, "lng":4.815747 }, "banking":true, "bonus":false }
 * ?bikeStation => { "number":5015, "contract_name":"Lyon", "name":"#5015 - FULCHIRON", "bike_stands":19, "available_bike_stands":11, "available_bikes":8, "status":"OPEN", "last_update":1538039879000, "address":"Devant le n°41 rue de la Quarantaine", "position":{ "lat":45.75197, "lng":4.821662 }, "banking":true, "bonus":false }
 * ?bikeStation =>  ...
 * </pre>
 *
 * Stream 3<br>
 * <pre>
 * ?bikeStation => { "number":2010, "contract_name":"Lyon", "name":"#2010 - CONFLUENCE DARSE", "bike_stands":22, "available_bike_stands":2, "available_bikes":20, "status":"OPEN", "last_update":1538039980000, "address":"ANGLE ALEE ANDRE MURE ET QUAI ANTOINE RIBOUD", "position":{ "lat":45.743317, "lng":4.815747 }, "banking":true, "bonus":false }
 * ?bikeStation => { "number":5015, "contract_name":"Lyon", "name":"#5015 - FULCHIRON", "bike_stands":19, "available_bike_stands":9, "available_bikes":10, "status":"OPEN", "last_update":1538039909000, "address":"Devant le n°41 rue de la Quarantaine", "position":{ "lat":45.75197, "lng":4.821662 }, "banking":true, "bonus":false }
 * ?bikeStation =>  ...
 * </pre>
 *
 * @author El-Mehdi Khalfi <el-mehdi.khalfi at emse.fr>
 * @since 2018-09-27
 */
public class ITER_PeriodicJSON extends IteratorStreamFunctionBase4 {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ITER_CSVMultipleOutput.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITER + "PeriodicJSON";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/application/json";

    @Override
    public void exec(NodeValue url, NodeValue recurrenceValue, NodeValue times, NodeValue jsonquery, Consumer<List<List<NodeValue>>> nodeValuesStream) {
        String url_s = url.asString();
        Client client = ClientBuilder.newClient();
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger i_call = new AtomicInteger();

        Runnable task = () -> {
            String json = "";
            try {
                json = client.target(url_s).request(MediaType.APPLICATION_JSON).get(String.class);
                LOG.info("Data retrieved from " + url_s);
            } catch (ProcessingException ex) {
                LOG.error(ex.getMessage());
                scheduler.shutdown();
                ex.printStackTrace();
            }
            Configuration conf = Configuration.builder().options(Option.ALWAYS_RETURN_LIST).build();
            List<Object> values = JsonPath
                    .using(conf)
                    .parse(json)
                    .read(jsonquery.asString());

            Gson gson = new Gson();
            RDFDatatype dt = TypeMapper.getInstance().getSafeTypeByName(datatypeUri);

            List<NodeValue> nodeValues = values.stream().map(value -> new NodeValueNode(NodeFactory.createLiteral(gson.toJson(value), dt))).collect(Collectors.toList());

            nodeValuesStream.accept(Collections.singletonList(nodeValues));
            if (i_call.getAndIncrement() == times.getInteger().intValue()) {
                LOG.info("Process finished after " + i_call.getAndDecrement() + "calls.");
                scheduler.shutdown();
            }
        };
        scheduler.scheduleAtFixedRate(task, 0, recurrenceValue.getInteger().intValue(), TimeUnit.SECONDS);
    }
}
