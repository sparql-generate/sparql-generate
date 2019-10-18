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
import fr.emse.ci.sparqlext.serializer.SPARQLExtFmtUtils;
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import fr.emse.ci.sparqlext.utils.ST;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.ExprUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Binding function
 * <a href="http://ns.inria.fr/sparql-template/">st:turtle</a>
 * takes as input a RDF Term, and returns some Turtle representation for this
 * term.
 *
 * <ul>
 * <li>Param 1 (input) a RDF Term</li>
 * </ul>
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class ST_Turtle implements Function {

    private static final Logger LOG = LoggerFactory.getLogger(ST_Turtle.class);

    public static String URI = ST.turtle;

    @Override
    public final void build(String uri, ExprList args) {
        if (args.size() != 1) {
            throw new ExprEvalException("Expecting one argument");
        }
    }

    @Override
    public NodeValue exec(Binding binding, ExprList args, String uri, FunctionEnv env) {
        if (args == null) // The contract on the function interface is that this should not happen.
        {
            throw new ARQInternalErrorException("FunctionBase: Null args list");
        }

        try {
            return exec(args.get(0).eval(binding, env), env);
        } catch (Exception ex) {
            if (LOG.isDebugEnabled()) {
                String errorId = UUID.randomUUID().toString().substring(0, 6);
                if (SPARQLExt.isDebugStConcat(env.getContext())) {
                    String message = String.format("Error id %s executing st:turtle with expression %s and binding %s", errorId, ExprUtils.fmtSPARQL(args), SPARQLExt.compress(binding).toString());
                    LOG.debug(message, ex);
                    return new NodeValueString("[WARN %s]");
                } else {
                    String message = String.format("Error executing st:turtle with expression %s and binding %s", errorId, ExprUtils.fmtSPARQL(args), SPARQLExt.compress(binding).toString());
                    LOG.debug(message, ex);
                }
            } else if (SPARQLExt.isDebugStConcat(env.getContext())) {
                return new NodeValueString("[WARN %s]");
            }
        }
        return new NodeValueString("");
    }

    public NodeValue exec(NodeValue node, FunctionEnv env) {
        IndentedLineBuffer buff = new IndentedLineBuffer();    
        Dataset dataset = env.getContext().get(SPARQLExt.DATASET);
        SerializationContext context = new SerializationContext(dataset.getDefaultModel());
        SPARQLExtFmtUtils.printNode(buff, node.asNode(), context);
        return new NodeValueString(buff.toString());
    }
}
