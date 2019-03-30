/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.thesmartenergy.sparql.generate.jena.function.library;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase1;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/bnode">fun:bnode</a>
 * generates a blank node, which is the same accross the different query
 * solutions.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class FUN_bnode extends FunctionBase1 {

    public static Map<String, NodeValue> map = new HashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(FUN_bnode.class);

    public static final String URI = SPARQLGenerate.FUN + "bnode";

    @Override
    public synchronized NodeValue exec(NodeValue node) {
        String input = (String) node.asNode().getLiteralValue();
        if (!map.containsKey(input)) {
            String hash = new BigInteger(120, new Random()).toString(32);
//            LOG.trace("Create bnode for " + node + " : " + hash);
            Node n = NodeFactory.createBlankNode(hash);
            NodeValue nv = NodeValue.makeNode(n);
            map.put(input, nv);
            return nv;
        }
        return map.get(input);
    }

}
