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
package fr.emse.ci.sparqlext.engine;

import fr.emse.ci.sparqlext.SPARQLExtException;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.query.SPARQLExtQueryVisitor;
import java.util.List;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Maxime Lefrançois
 */
public class SelectQueryPartialCopyVisitor implements SPARQLExtQueryVisitor {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SelectQueryPartialCopyVisitor.class);

    private final Query output = new Query();
    
    private final Binding binding;
    private final Context context;
    

    public Query getOutput() {
        return output;
    }

    public SelectQueryPartialCopyVisitor(Binding binding, Context context) {
		this.binding = binding;
		this.context = context;
	}
    
    @Override
    public void startVisit(final Query query) {
        asSPARQLExtQuery(query);
        if(!query.isSelectType()) {
            throw new SPARQLExtException("Expecting a SELECT query:" + query);            
        }
    }

    @Override
    public void visitResultForm(final Query query) {
        output.setQuerySelectType();
    }

    @Override
    public void visitPrologue(final Prologue prologue) {
        output.setPrefixMapping(prologue.getPrefixMapping());
        String b = prologue.getBaseURI();
        output.setBaseURI(b);
    }

    @Override
    public void visitSelectResultForm(final Query q) {
        SPARQLExtQuery query = asSPARQLExtQuery(q);
        output.setDistinct(query.isDistinct());
        output.setReduced(query.isReduced());
        output.setQueryResultStar(query.isQueryResultStar());
        VarExprList project = query.getProject();
        project.forEachVar((v) -> {
            output.addResultVar(v, project.getExpr(v));
        });
    }

    @Override
    public void visitConstructResultForm(final Query query) {
        throw new SPARQLExtException("should not reach this"
                + " point");
    }

    @Override
    public void visitDescribeResultForm(final Query query) {
        throw new SPARQLExtException("should not reach this"
                + " point");
    }

    @Override
    public void visitAskResultForm(final Query query) {
        throw new SPARQLExtException("should not reach this"
                + " point");
    }

    @Override
    public void visitDatasetDecl(final Query q) {
        SPARQLExtQuery query = asSPARQLExtQuery(q);
        query.getFromClauses().forEach(fc->{
        	if(fc.isNamed()) {
        		String graphURI = evalSourceURI(binding, context, fc.getName());
        		output.addNamedGraphURI(graphURI);
        	} 
        });
        
    }
    
	private String evalSourceURI(Binding binding, Context context, Expr sourceExpr) {
		if (binding == null) {
			throw new NullPointerException("No binding to evaluate the source expression " + sourceExpr);
		}
		try {
			FunctionEnv env = new FunctionEnvBase(context);
			NodeValue nodeValue = sourceExpr.eval(binding, env);
			if (!nodeValue.isIRI()) {
				throw new IllegalArgumentException("FROM source expression did not eval to a URI " + sourceExpr);
			}
			return nodeValue.asNode().getURI();
		} catch (ExprEvalException ex) {
			throw new IllegalArgumentException("Exception when evaluating the source expression " + sourceExpr, ex);
		}
	}

    @Override
    public void visitQueryPattern(final Query query) {
        ElementGroup group = new ElementGroup();
        if (query.getQueryPattern() != null) {
            Element el = query.getQueryPattern();
            if(!(el instanceof ElementGroup)) {
                throw new SPARQLExtException("should not reach this point");
            }
            ((ElementGroup) el).getElements().forEach(group::addElement);
        }
        output.setQueryPattern(group);
    }

    @Override
    public void visitBindingClauses(SPARQLExtQuery query) {
    }

    @Override
    public void visitGenerateClause(SPARQLExtQuery query) {
    }

    @Override
    public void visitTemplateClause(SPARQLExtQuery query) {
    }

    @Override
    public void visitPerformClause(SPARQLExtQuery query) {
    }

    @Override
    public void visitGroupBy(final Query query) {
        if (query.hasGroupBy()) {
            if (!query.getGroupBy().isEmpty()) {
                VarExprList namedExprs = query.getGroupBy();
                for (Var var : namedExprs.getVars()) {
                    Expr expr = namedExprs.getExpr(var);
                    if (expr != null) {
                        output.addGroupBy(var, expr);
                    } else {
                        output.addGroupBy(var.getVarName());
                    }
                }
            }
        }
    }

    @Override
    public void visitHaving(final Query query) {
        if (query.hasHaving()) {
            for (Expr expr : query.getHavingExprs()) {
                output.addHavingCondition(expr);
            }
        }
    }

    @Override
    public void visitOrderBy(final Query query) {
        if (query.hasOrderBy()) {
            for (SortCondition sc : query.getOrderBy()) {
                output.addOrderBy(sc);
            }
        }
    }

    @Override
    public void visitOffset(final Query query) {
        if (query.hasOffset()) {
            output.setOffset(query.getOffset());
        }
    }

    @Override
    public void visitLimit(final Query query) {
        if (query.hasLimit()) {
            output.setLimit(query.getLimit());
        }
    }

    @Override
    public void visitPostSelect(SPARQLExtQuery query) {
        
    }

    @Override
    public void visitValues(final Query query) {
        if (query.hasValues() && !query.hasAggregators()) {
            List<Var> variables = query.getValuesVariables();
            List<Binding> values = query.getValuesData();
            output.setValuesDataBlock(variables, values);
            output.addProjectVars(variables);
        }
    }

    @Override
    public void visitFunctionExpression(SPARQLExtQuery query) {
        Var var = Var.alloc("out");
        ElementBind bind = new ElementBind(var, query.getFunctionExpression());
        output.setQueryPattern(bind);
        output.addResultVar(var);
    }

    @Override
    public void finishVisit(final Query query) {
        query.getAggregators().forEach((agg)->{
            output.allocAggregate(agg.getAggregator());
        });
        if (output.getProjectVars().isEmpty()) {
            output.setQueryResultStar(true);
        }
    }

    @Override
    public void visitPragma(SPARQLExtQuery query) {
    }

	@Override
	public void visitJsonResultForm(Query query) {
	}

}
