/*
 * Copyright 2016 ITEA 12004 SEAS Project.
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
package com.github.thesmartenergy.sparql.generate.jena.function.library;

import com.jayway.jsonpath.JsonPath;
import com.github.thesmartenergy.sparql.generate.jena.selector.library.SEL_JSONPath;
import java.math.BigDecimal;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDecimal;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDouble;
import org.apache.jena.sparql.expr.nodevalue.NodeValueFloat;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase2;
import org.apache.log4j.Logger;

/**
 *
 * @author maxime.lefrancois
 */
public class FN_JSONPath extends FunctionBase2 {

    private static final String uri = "urn:iana:mime:application/json";

    @Override
    public NodeValue exec(NodeValue v1, NodeValue v2) {
        if (v1.getDatatypeURI() == null ? uri != null : !v1.getDatatypeURI().equals(uri)) {
            Logger.getLogger(SEL_JSONPath.class).warn("The URI of NodeValue1 MUST be <" + uri + ">. Returning null.");
        }

        try {
            Object value = JsonPath.parse(v1.asNode().getLiteralLexicalForm()).limit(1).read(v2.getString());
//            System.out.println("FN --> "+value);
            if (value instanceof String) {
                return new NodeValueString((String) value);
            } else if (value instanceof Float) {
                return new NodeValueFloat((Float) value);
            } else if (value instanceof Boolean) {
                return new NodeValueBoolean((Boolean) value);
            } else if (value instanceof Integer) {
                return new NodeValueInteger((Integer) value);
            } else if (value instanceof Double) {
                return new NodeValueDouble((Double) value);
            } else if (value instanceof BigDecimal) {
                return new NodeValueDecimal((BigDecimal) value);
            }
            throw new ExprEvalException("FunctionBase: not a primitive type . Got" + value.getClass());
        } catch (Exception e) {
            throw new ExprEvalException("FunctionBase: no evaluation", e);
        }
    }
}
