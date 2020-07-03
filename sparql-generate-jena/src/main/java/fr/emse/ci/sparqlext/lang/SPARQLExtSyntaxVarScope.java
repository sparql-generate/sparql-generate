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
package fr.emse.ci.sparqlext.lang;

import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.syntax.FromClause;
import java.util.ArrayList;
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
 * @author Maxime Lefrançois
 */
public class SPARQLExtSyntaxVarScope {

    public static void check(SPARQLExtQuery query) {
        checkTemplateClauseExpressions(query);
    }

    private static void checkTemplateClauseExpressions(SPARQLExtQuery query) {
        final List<Var> signature;
        if (query.getSignature() != null) {
            signature = query.getSignature();
        } else {
            signature = new ArrayList<>();
        }
        if (query.isTemplateType() && !query.isSubQuery()) {
        	
            checkVarsInExpr(query.getTemplateClauseBefore(), signature, "Before expression should only have vars from the signature");
            checkVarsInExpr(query.getTemplateClauseSeparator(), signature, "Separator expression should only have vars from the signature");
            checkVarsInExpr(query.getTemplateClauseAfter(), signature, "After expression should only have vars from the signature");
        	// << except! if the query has no signature and is a sub-query. Then the vars should be in-scope of the super-query. >>
        }
        if(query.isSelectType() && !query.hasName()) {
            return;
        }
    	if(!query.isSubQuery()) {
	        for (FromClause fromClause : query.getFromClauses()) {
	            if (fromClause.getName() != null) {
	                checkVarsInExpr(fromClause.getName(), signature, "FROM expression should only have vars from the signature");
	            }
	            if (fromClause.getGenerate() != null) {
	                SPARQLExtQuery generate = fromClause.getGenerate();
	                if (generate.hasSignature()) {
	                    checkVarsInList(generate.getSignature(), signature, "Signature of FROM GENERATE should be a subset of the signature");
	                }
	                if (generate.hasName()) {
	                    checkVarsInExpr(generate.getName(), signature, "Call parameters of FROM GENERATE should only have vars from the signature");
	                }
	                if (generate.hasCallParameters()) {
	                    generate.getCallParameters().forEach((callParameter) -> {
	                        checkVarsInExpr(callParameter, signature, "Call parameters of FROM GENERATE should only have vars from the signature");
	                    });
	                }
	            }
	        }
	    }
    }

    private static void checkVarsInList(List<Var> vars, List<Var> signature, String message) {
        if (!signature.containsAll(vars)) {
            throw new QueryParseException(message, -1, -1);
        }
    }

    private static void checkVarsInExpr(Expr expr, List<Var> signature, String message) {
        Set<Var> vars = ExprVars.getVarsMentioned(expr);
        vars.removeAll(signature);
        if (!vars.isEmpty()) {
            throw new QueryParseException(message, -1, -1);
        }
    }

}
