/*
 * Copyright 2019 Ecole des Mines de Saint-Etienne.
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
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import fr.emse.ci.sparqlext.utils.ContextUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.util.Context;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/Property">fun:property</a>
 * extracts a property from a
 * <a href="https://en.wikipedia.org/wiki/.properties">Properties document</a>
 *
 * <ul>
 * <li>Param 1: (file): the URI of the properties document (a URI), or the
 * properties document itself (a String);</li>
 * <li>Param 2: (property) the name of the property;</li>
 * </ul>
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public final class FUN_Property implements Function {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FUN_Property.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLExt.FUN + "property";

    @Override
    public final void build(String uri, ExprList args) {
        if (args.size() != 2) {
            throw new ExprEvalException("Expecting two argument");
        }
    }

    @Override
    public NodeValue exec(Binding binding, ExprList args, String uri, FunctionEnv env) {
        if (args == null) {
            throw new ARQInternalErrorException("FunctionBase: Null args list");
        }
        if (args.size() != 2) {
            throw new ExprEvalException("Expecting two argument");
        }
        NodeValue file = args.get(0).eval(binding, env);
        NodeValue propertyNode = args.get(1).eval(binding, env);
        if (!propertyNode.isString()) {
            throw new ExprEvalException("Second argument must be a string. Got " + propertyNode);
        }
        Properties properties;
        try {
            properties = getProperties(file, env);
        } catch (IOException ex) {
            throw new ExprEvalException("IOException while extracting properties document " + file, ex);
        }
        String prop = properties.getProperty(propertyNode.getString());
        if (prop == null) {
            throw new ExprEvalException("Property " + prop + " not found in properties document " + file);
        }
        return new NodeValueString(prop);
    }

    private Properties getProperties(NodeValue file, FunctionEnv env) throws IOException {
        Properties properties = new Properties();
        Context context = env.getContext();
        if (file.isString()) {
            properties.load(new StringReader(file.getString()));
        } else if (file.isLiteral()) {
            properties.load(new StringReader(file.asNode().getLiteralLexicalForm()));
        } else if (!file.isIRI()) {
            String message = String.format("First argument must be a URI or a String");
            LOG.warn(message);
            throw new ExprEvalException(message);
        }
        String filePath = file.asNode().getURI();
        try (TypedInputStream tin = ContextUtils.openStream(context, filePath, null)) {
            if (tin == null) {
                String message = String.format("Could not look up Properties document %s", filePath);
                LOG.warn(message);
                throw new ExprEvalException(message);
            }
            properties.load(new InputStreamReader(tin.getInputStream(), StandardCharsets.UTF_8));
            return properties;
        }
    }

}
