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
import fr.emse.ci.sparqlext.generate.engine.BNodeMap;
import fr.emse.ci.sparqlext.generate.engine.PlanFactory;
import fr.emse.ci.sparqlext.generate.engine.RootPlan;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import fr.emse.ci.sparqlext.utils.ST;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.ExprUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * https://ns.inria.fr/sparql-template/ sec. 4.3 A named template is called by
 * name with parameter values using the st:call-template function. When several
 * parameters occur, parameter passing is done by position (i.e. not by name).
 *
 * @author maxime.lefrancois
 */
public class ST_Concat implements Function {

    private static final Logger LOG = LoggerFactory.getLogger(ST_Concat.class);

    public static final String URI = ST.concat;

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
    public NodeValue exec(Binding binding, ExprList args, String uri, FunctionEnv env) {
        if (args == null) {
            throw new ARQInternalErrorException("FunctionBase: Null args list");
        }
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            Expr expr = args.get(i);
            try {
                NodeValue arg = expr.eval(binding, env);
                res.append(arg.asString());
            } catch (Exception ex) {
                if (LOG.isDebugEnabled()) {
                    String errorId = UUID.randomUUID().toString().substring(0, 6);
                    if (SPARQLExt.DEBUG_ST_CONCAT) {
                        String message = String.format("Error id %s executing st:concat with expression %s and binding %s", errorId, ExprUtils.fmtSPARQL(expr), SPARQLExt.compress(binding).toString());
                        LOG.debug(message, ex);
                        res.append(String.format("[WARN %s]", errorId));
                    } else {
                        String message = String.format("Error executing st:concat with expression %s and binding %s", errorId, ExprUtils.fmtSPARQL(expr), SPARQLExt.compress(binding).toString());
                        LOG.debug(message, ex);
                    }
                } else if (SPARQLExt.DEBUG_ST_CONCAT) {
                    res.append(String.format("[NULL]"));
                }
            }
        }
        return new NodeValueString(res.toString());
    }

}
