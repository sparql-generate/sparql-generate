/*
 * Copyright 2020 MINES Saint-Étienne
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
package fr.emse.ci.sparqlext.function.library;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.utils.ContextUtils;
import java.util.Map;
import java.util.Set;
import org.apache.jena.query.Dataset;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionEnv;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/prefixedIRI">fun:prefixedIRI</a>
 * takes as input a IRI, and returns the shortened IRI according to the prefixes
 * in the query's prologue.
 *
 * <ul>
 * <li>Param 1 (input) a IRI</li>
 * </ul>
 *
 * @author Maxime Lefrançois
 */
public final class FUN_PrefixedIRI implements Function {

    private static final Logger LOG = LoggerFactory.getLogger(FUN_PrefixedIRI.class);

    public static final String URI = SPARQLExt.FUN + "prefixedIRI";

    @Override
    public final void build(String uri, ExprList args) {
        if (args.size() < 1) {
            throw new ExprEvalException("Expecting at least one argument");
        }
    }

    /**
     *
     * @param binding
     * @param args
     * @param uri
     * @param env
     * @return
     */
    @Override
    public NodeValue exec(
            final Binding binding,
            final ExprList args,
            final String uri,
            final FunctionEnv env) {
        if (args == null) {
            throw new ARQInternalErrorException("FunctionBase: Null args list");
        }
        if (args.size() != 1) {
            throw new ExprEvalException("Expecting one argument");
        }
        NodeValue node = args.get(0).eval(binding, env);
        if (node == null) {
            throw new ExprEvalException("The input did not resolve to a IRI: " + args.get(0));
        }
        if (!node.isIRI()) {
            LOG.debug("The input should be a IRI. Got " + node);
            throw new ExprEvalException("The input should be a IRI. Got " + node);
        }

        String longIRI = node.asNode().getURI();
        Dataset dataset = ContextUtils.getDataset(env.getContext());
        
        final Map<String, String> prefixMap = dataset.getDefaultModel().getNsPrefixMap();
        final Set<String> prefixes = prefixMap.keySet();
        String prefix = null;
        String localName = uri;
        for (String p : prefixes) {
            String ns = prefixMap.get(p);
            if (longIRI.startsWith(ns)) {
                String ln = longIRI.substring(ns.length());
                if (ln.length() <= localName.length()) {
                    prefix = p;
                    localName = ln;
                }
            }
        }
        if(prefix != null) {
            return new NodeValueString(prefix + ":" + localName);
        } else {
            return new NodeValueString(longIRI);
        }
    }
}
