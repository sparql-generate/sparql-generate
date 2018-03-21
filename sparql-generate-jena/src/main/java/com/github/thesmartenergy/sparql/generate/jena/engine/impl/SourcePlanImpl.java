/*
 * Copyright 2016 The Apache Software Foundation.
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

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateException;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import java.util.Objects;
import com.github.thesmartenergy.sparql.generate.jena.engine.SourcePlan;
import com.github.thesmartenergy.sparql.generate.jena.stream.LookUpRequest;
import org.apache.jena.atlas.web.TypedInputStream;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Executes a <code>{@code SOURCE <node> ACCEPT <mime> AS <var>}</code> clause.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class SourcePlanImpl extends PlanBase implements SourcePlan {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SourcePlanImpl.class);

    /**
     * The source node. A uri or a variable.
     */
    private final Node node;

    /**
     * The accept node. A uri or a variable.
     */
    private final Node accept;

    /**
     * The bound variable.
     */
    private final Var var;

    /**
     * type mapper.
     */
    final static TypeMapper tm = TypeMapper.getInstance();

    /**
     * The generation plan of a <code>{@code SOURCE <node> ACCEPT <mime> AS
     * <var>}</code> clause.
     *
     * @param node0 The IRI or the Variable node where a GET must be operated.
     * Must not be null.
     * @param accept0 The IRI or the Variable node that represent the accepted
     * Internet Media Type. May be null.
     * @param var0 The variable to bound the potentially retrieved document.
     * Must not be null.
     */
    public SourcePlanImpl(
            final Node node0,
            final Node accept0,
            final Var var0) {
        Objects.requireNonNull(node0, "Node must not be null");
        Objects.requireNonNull(var0, "Var must not be null");
        if (!node0.isURI() && !node0.isVariable()) {
            throw new IllegalArgumentException("Source node must be a IRI or a"
                    + " Variable. got " + node0);
        }
        this.node = node0;
        this.accept = accept0;
        this.var = var0;
    }

    /**
     * {@inheritDoc}
     */
    final public void exec(
            final List<Var> variables,
            final List<BindingHashMapOverwrite> values) {
        LOG.debug("Exec SOURCE " + node  + (accept!=null? " ACCEPT " + accept : "" ) + " AS " + var);

        boolean added = variables.add(var);
        if (!added) {
            throw new SPARQLGenerateException("Variable " + var + " is already"
                    + " bound !");
        }
        ensureNotEmpty(variables, values);
        values.replaceAll((BindingHashMapOverwrite value) -> {
            // generate the source URI.
            final String sourceUri = getActualSource(value);
            final String acceptHeader = getAcceptHeader(value);
            LOG.trace("... resolved to SOURCE <" + sourceUri + "> ACCEPT " + acceptHeader + " AS " + var);
            try {
                final LookUpRequest request = new LookUpRequest(sourceUri, acceptHeader);
                final TypedInputStream stream = SPARQLGenerate.getStreamManager().open(request);
                if(stream == null) {
                    LOG.info("Exec SOURCE <" + sourceUri + "> ACCEPT " + acceptHeader + " AS " + var + " returned nothing.");
                    return new BindingHashMapOverwrite(value, var, null);
                }
                final String literal = IOUtils.toString(stream.getInputStream(), "UTF-8");
                final RDFDatatype dt;;
                if (stream.getMediaType() != null && stream.getMediaType().getContentType() != null) {
                    dt = tm.getSafeTypeByName("http://www.iana.org/assignments/media-types/" + stream.getMediaType().getContentType());
                } else {
                    dt = tm.getSafeTypeByName("http://www.w3.org/2001/XMLSchema#string");
                }
                final Node n = NodeFactory.createLiteral(literal, dt);
                LOG.info("Exec SOURCE <" + sourceUri + "> ACCEPT " + acceptHeader + " AS " + var + " returned. Enable DEBUG level for more.");
                if(LOG.isDebugEnabled()) {
                    String out = n.toString();
                    if(out.length()>200) {
                        out = out.substring(0, 120) + "\n ... \n" + out.substring(out.length()-80);
                    }
                    LOG.debug(out);
                }
                return new BindingHashMapOverwrite(value, var, n);
            } catch (Exception ex) {
                LOG.warn("Exception while looking up " + sourceUri + ":", ex);
                return new BindingHashMapOverwrite(value, var, null);
            }
        });
    }

    /**
     *
     * @param binding -
     * @return the actual URI that represents the location of the query to be
     * fetched.
     */
    private String getActualSource(
            final BindingHashMapOverwrite binding) {
        if (node.isVariable()) {
            Node actualSource = binding.get((Var) node);
            if (actualSource == null) {
                return null;
            }
            if (!actualSource.isURI()) {
                throw new SPARQLGenerateException("Variable " + node.getName()
                        + " must be bound to a IRI that represents the location"
                        + " of the query to be fetched.");
            }
            return actualSource.getURI();
        } else {
            if (!node.isURI()) {
                throw new SPARQLGenerateException("The source must be a IRI"
                        + " that represents the location of the query to be"
                        + " fetched. Got " + node.getURI());
            }
            return node.getURI();
        }
    }

    /**
     * returns the accept header computed from ACCEPT clause.
     *
     * @param binding -
     * @return the actual accept header to use.
     */
    private String getAcceptHeader(
            final BindingHashMapOverwrite binding) {
        Node actualAccept = accept;
        if (accept == null) {
            return "*/*";
        }
        if (accept.isVariable()) {
            actualAccept = binding.get((Var) accept);
            if (accept == null) {
                return "*/*";
            }
        }
        if (!actualAccept.isURI()) {
            throw new SPARQLGenerateException("Variable " + node.getName()
                    + " must be bound to a IRI that represents the internet"
                    + " media type of the source to be fetched. For"
                    + " instance, <http://www.iana.org/assignments/media-types/application/xml>.");
        }
        if (!actualAccept.getURI().startsWith("http://www.iana.org/assignments/media-types/")) {
            throw new SPARQLGenerateException("Variable " + node.getName()
                    + " must be bound to a IANA MIME URN (RFC to be"
                    + " written). For instance,"
                    + " <http://www.iana.org/assignments/media-types/application/xml>.");
        }
        return actualAccept.getURI();

    }

}
