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
package com.github.thesmartenergy.sparql.generate.jena.function.library;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import org.apache.jena.sparql.expr.NodeValue;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.function.FunctionBase1;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/dateTime">fun:dateTime</a>
 * converts a UNIX timestamp to an xsd:dateTime.
 * <ul>
 * <li>Param 1 (timeStampValue) is a UNIX timestamp;</li>
 * <li>Result is a xsd:dateTime.</li>
 * </ul>
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public final class FUN_dateTime extends FunctionBase1 {

    static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static final Logger LOG = LoggerFactory.getLogger(FUN_dateTime.class);

    public static final String URI = SPARQLGenerate.FUN + "dateTime";

    @Override
    public NodeValue exec(NodeValue timeStampValue) {

        BigInteger timeStamp;
        if (timeStampValue == null || !timeStampValue.isInteger()) {
            throw new ExprEvalException("The NodeValue " + timeStampValue + " MUST be an integer."
            );
        } else {
            timeStamp = timeStampValue.getInteger();
        }

        if (timeStamp.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) != -1) {
            throw new ExprEvalException("The NodeValue " + timeStamp + " MUST be less than the biggest long value."
            );
        } else if (timeStamp.signum() != 1) {
            throw new ExprEvalException("The NodeValue " + timeStamp + " MUST be positive."
            );
        }

        Timestamp stamp = new Timestamp(timeStamp.longValue());
        Date date = new Date(stamp.getTime());

        Node node = NodeFactory.createLiteral(df.format(date), XSDDatatype.XSDdateTime);
        return new NodeValueNode(node);
    }
}
