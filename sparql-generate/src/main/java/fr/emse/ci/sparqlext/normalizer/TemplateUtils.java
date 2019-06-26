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
package fr.emse.ci.sparqlext.normalizer;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.function.library.FUN_Select_Call_Template;
import fr.emse.ci.sparqlext.lang.ParserSPARQLExt;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.utils.ST;
import org.apache.jena.sparql.expr.E_Function;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.syntax.ElementGroup;

/**
 *
 * @author maxime.lefrancois
 */
public class TemplateUtils {

    public static E_Function getFunction(SPARQLExtQuery query) {
        if (query.isNamedSubQuery()) {
            String qs = query.toString();
            SPARQLExtQuery query2 = (SPARQLExtQuery) ParserSPARQLExt.parseSubQuery(query, qs);
            query2.setQuerySelectType();
            query2.setQueryResultStar(true);
            query2.setName(null);
            query2.setCallParameters(null);
            if (query2.getQueryPattern() == null) {
                query2.setQueryPattern(new ElementGroup());
            }
            NodeValue selectQuery = NodeValue.makeNode(query2.toString(), null, SPARQLExt.MEDIA_TYPE_URI);
            ExprList exprList = new ExprList(selectQuery);
            exprList.add(query.getName());
            exprList.addAll(query.getCallParameters());
            return new E_Function(FUN_Select_Call_Template.URI, exprList);
        } else {
            NodeValue n = NodeValue.makeNode(query.toString(), null, SPARQLExt.MEDIA_TYPE_URI);
            return new E_Function(ST.callTemplate, new ExprList(n));
        }
    }
}
