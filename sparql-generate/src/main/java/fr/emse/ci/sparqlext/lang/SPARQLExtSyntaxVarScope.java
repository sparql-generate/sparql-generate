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
package fr.emse.ci.sparqlext.lang;

import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import java.util.List;
import java.util.Set;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVars;

/**
 * To be completed. Should extend the SPARQL 1.1 variable scope checking to the
 * specificities of SPARQL Generate.
 *
 * Currently, only checking in Template clause that the before, separator, and
 * after expressions use only variables that are in the signature of the query.
 *
 * @author maxime.lefrancois
 */
public class SPARQLExtSyntaxVarScope {

    public static void check(SPARQLExtQuery query) {
        checkTemplateClauseExpressions(query);
    }

    private static void checkTemplateClauseExpressions(SPARQLExtQuery query) {
        if (!query.isTemplateType() || !query.hasSignature()) {
            return;
        }
        checkVarsInExpr(query.getTemplateClauseBefore(), query.getSignature(), "Before expression should only have vars from the signature");
        checkVarsInExpr(query.getTemplateClauseSeparator(), query.getSignature(), "Separator expression should only have vars from the signature");
        checkVarsInExpr(query.getTemplateClauseAfter(), query.getSignature(), "After expression should only have vars from the signature");
    }

    private static void checkVarsInExpr(Expr expr, List<Var> signature, String message) {
        Set<Var> vars = ExprVars.getVarsMentioned(expr);
        vars.removeAll(signature);
        if (!vars.isEmpty()) {
            throw new QueryParseException(message, -1, -1);
        }
    }
}
