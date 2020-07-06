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
package fr.mines_stetienne.ci.sparql_generate.function.library;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import java.util.List;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/log">fun:log</a>
 * logs the input to the console, and outputs the empty string.
 *
 *
 * <ul>
 * <li>Param 1 (input) a string literal</li>
 * </ul>
 *
 * @author Maxime Lefrançois
 */
public final class FUN_Log extends FunctionBase {

    private static final Logger LOG = LoggerFactory.getLogger(FUN_Log.class);

    public static final String URI = SPARQLExt.FUN + "log";
    public static final NodeValue OUTPUT = new NodeValueString("");

    @Override
    public void checkBuild(String uri, ExprList args) {
    }
    
    @Override
    public NodeValue exec(List<NodeValue> nodes) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(NodeValue nv : nodes) {
            if(!first) {
                sb.append(", ");
            } else {
                first = false;
            }
            sb.append(nv.toString());
        }
        LOG.info(sb.toString());
        return OUTPUT;
    }
    
    
}
