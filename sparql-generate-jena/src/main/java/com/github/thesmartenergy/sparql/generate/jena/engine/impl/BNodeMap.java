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
package com.github.thesmartenergy.sparql.generate.jena.engine.impl;

import java.util.HashMap;
import java.util.Map;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Class to store overridable blank node mappings efficiently.
 * 
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class BNodeMap {

    static final Logger log = LoggerFactory.getLogger(BNodeMap.class);
    
    /** The parent BNodeMap. */
    private final BNodeMap parent;
    
    /** The bNode map. */
    private final Map<Node, Node> bNodeMap = new HashMap<>();
   
    private Map<Node, Node> asMap = null;
            
    /**
     * Constructs a new blank node mapping.
     *
     * @param parent -
     */
    public BNodeMap(final BNodeMap parent) {
        this.parent = parent;
    }
 
    /**
     * Constructs a new blank node mapping.
     *
     */
    public BNodeMap() {
        this(null);
    }
    
    /**
     * Constructs a new binding from a query solution.
     *
     * @param parent -
     * @param binding -
     */
    public BNodeMap(
            final BNodeMap parent,
            final BindingHashMapOverwrite binding) {
        this(parent);
        if (binding == null) {
            return;
        }
        for (Var v : binding.varsList()) {
            Node n = binding.get(v);
            if (n != null && n.isBlank() && !parent.contains(n)) {
                Node bn = NodeFactory.createBlankNode();
                bNodeMap.put(n, bn);
            }
        }
    }

    public final boolean contains(final Node node) {
        if (node == null) {
            return false;
        }
        if (bNodeMap.containsKey(node)) {
            return true;
        }
        if (parent != null) {
            return parent.contains(node);
        }
        return false;
    }

    public final Node get(final Node node) {
        if (node == null) {
            return null;
        }
        if (bNodeMap.containsKey(node)) {
            return bNodeMap.get(node);
        }
        if(parent != null) {
            return parent.get(node);
        }
        return null;
    }
    
    public final Map<Node, Node> asMap() {
        if(asMap == null) {
            if(parent != null) {
                asMap = Maps.newHashMap(parent.asMap());
            } else {
                asMap = Maps.newHashMap();
            }
            asMap.putAll(bNodeMap);
        }
        return asMap;
    }

//    /**
//     * {@inheritDoc }
//     */
//    @Override
//    public final int size() {
//        return varsList().size();
//    }
//
//    /**
//     * {@inheritDoc }
//     */
//    @Override
//    public final boolean isEmpty() {
//        return varsList().isEmpty();
//    }
//    
    
    //todo write equal, hash, and tostring methods.
}
