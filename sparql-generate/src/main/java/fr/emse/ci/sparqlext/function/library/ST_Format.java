/*
 * Copyright 2019 École des Mines de Saint-Étienne.
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
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import fr.emse.ci.sparqlext.utils.ST;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public class ST_Format implements Function {

    private static final Logger LOG = LoggerFactory.getLogger(ST_Format.class);

    public static String URI = ST.format;

    @Override
    public final void build(String uri, ExprList args) {
        if (args.size() < 1) {
            throw new ExprEvalException("Expecting at least one argument");
        }
    }

    @Override
    public NodeValue exec(Binding binding, ExprList args, String uri, FunctionEnv env) {
        if (args == null) // The contract on the function interface is that this should not happen.
        {
            throw new ARQInternalErrorException("FunctionBase: Null args list");
        }

        List<NodeValue> evalArgs = new ArrayList<>();
        for (Expr e : args) {
            NodeValue x = e.eval(binding, env);
            evalArgs.add(x);
        }

        NodeValue nv = exec(evalArgs, env);
        return nv;
    }

    public NodeValue exec(List<NodeValue> args, FunctionEnv env) {
        if (args.size() < 1) {
            LOG.debug("Expecting at least one arguments.");
            throw new ExprEvalException("Expecting at least one arguments.");
        }
        final NodeValue format = args.get(0);
        if (!format.isIRI() && !format.isString() && !format.asNode().isLiteral()) {
            LOG.debug("First argument must be a URI or a String.");
            throw new ExprEvalException("First argument must be a URI or a String.");
        }

        Object[] params = new String[args.size() - 1];
        for (int i = 0; i < args.size() - 1; i++) {
            params[i] = args.get(i + 1).asUnquotedString();
        }
        try {
            String output = String.format(getString(format, env), params);
            return new NodeValueString(output);
        } catch (IllegalFormatException ex) {
            throw new ExprEvalException("Exception while executing st:format(" + args + ")");
        }
    }

    private String getString(NodeValue template, FunctionEnv env) throws ExprEvalException {
        if (template.isString()) {
            return template.getString();
        } else if (template.isLiteral()) {
            return template.asNode().getLiteralLexicalForm();
        } else if (!template.isIRI()) {
            String message = String.format("First argument must be a URI or a String");
            LOG.warn(message);
            throw new ExprEvalException(message);
        }
        String tPath = template.asNode().getURI();
        String acceptHeader = "*/*";
        LookUpRequest req = new LookUpRequest(tPath, acceptHeader);
        final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) env.getContext().get(SPARQLExt.STREAM_MANAGER);
        Objects.requireNonNull(sm);
        TypedInputStream tin = sm.open(req);
        if (tin == null) {
            String message = String.format("Could not look up document %s", tPath);
            LOG.warn(message);
            throw new ExprEvalException(message);
        }

        try {
            String output = IOUtils.toString(tin.getInputStream(), StandardCharsets.UTF_8);
            if (LOG.isTraceEnabled()) {
                String log = output;
                LOG.debug("Loaded <" + tPath + "> ACCEPT "
                        + acceptHeader + ". Enable TRACE level for more.");
                if (log.length() > 200) {
                    log = log.substring(0, 120) + "\n"
                            + " ... \n" + log.substring(log.length() - 80);
                }
                LOG.trace("Loaded <" + tPath + "> ACCEPT "
                        + acceptHeader + ". returned\n" + log);
            }
            return output;
        } catch (IOException ex) {
            throw new ExprEvalException("IOException while looking up document " + tPath, ex);
        }
    }
}
