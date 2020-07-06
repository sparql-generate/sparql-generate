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
package fr.mines_stetienne.ci.sparql_generate.serializer;

import fr.mines_stetienne.ci.sparql_generate.expr.E_URIParam;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_Exists;
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.expr.E_NotOneOf;
import org.apache.jena.sparql.expr.E_OneOf;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprFunction;
import org.apache.jena.sparql.expr.ExprFunction0;
import org.apache.jena.sparql.expr.ExprFunction1;
import org.apache.jena.sparql.expr.ExprFunction2;
import org.apache.jena.sparql.expr.ExprFunction3;
import org.apache.jena.sparql.expr.ExprFunctionN;
import org.apache.jena.sparql.expr.ExprFunctionOp;
import org.apache.jena.sparql.expr.ExprNone;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.ExprVisitor;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.aggregate.AggCount;
import org.apache.jena.sparql.expr.aggregate.AggCountDistinct;
import org.apache.jena.sparql.expr.aggregate.AggGroupConcat;
import org.apache.jena.sparql.expr.aggregate.AggGroupConcatDistinct;
import org.apache.jena.sparql.expr.aggregate.Aggregator;
import org.apache.jena.sparql.serializer.FmtExprSPARQL;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.syntax.Element;

/**
 * Serializes Expressions
 *
 * @author Maxime Lefrançois
 */
public class SPARQLExtFmtExprSPARQL extends FmtExprSPARQL {

	static final int INDENT = 2;

	private final SPARQLGenerateFmtExprARQVisitor visitor;

	public SPARQLExtFmtExprSPARQL(IndentedWriter writer, SerializationContext cxt) {
		super(writer, cxt);
		visitor = new SPARQLGenerateFmtExprARQVisitor(writer, cxt);
	}

	// Top level writing of an expression.
	public void format(Expr expr) {
		expr.visit(visitor);
	}

	private static class SPARQLGenerateFmtExprARQVisitor implements ExprVisitor {

		private final IndentedWriter out;
		private final SerializationContext context;

		public SPARQLGenerateFmtExprARQVisitor(IndentedWriter writer, SerializationContext cxt) {
			out = writer;
			if (cxt == null) {
				context = new SerializationContext();
			} else {
				context = cxt;
			}
		}

		@Override
		public void visit(ExprFunction0 expr) {
			if (expr.getOpName() == null) {
				printInFunctionForm(expr);
				return;
			}
			out.print("( ");
			out.print(expr.getOpName());
			out.print(" ");
		}

		@Override
		public void visit(ExprFunction1 expr) {
			if (expr instanceof E_URIParam) {
				expr.getArg().visit(this);
				return;
			}
			if (expr.getOpName() == null) {
				printInFunctionForm(expr);
				return;
			}
			out.print("( ");
			out.print(expr.getOpName());
			out.print(" ");
			expr.getArg().visit(this);
			out.print(" )");
		}

		@Override
		public void visit(ExprFunction2 expr) {
			if (expr.getOpName() == null) {
				printInFunctionForm(expr);
				return;
			}
			out.print("( ");
			expr.getArg1().visit(this);
			out.print(" ");
			out.print(expr.getOpName());
			out.print(" ");
			expr.getArg2().visit(this);
			out.print(" )");
		}

		@Override
		public void visit(ExprFunction3 expr) {
			printInFunctionForm(expr);
		}

		@Override
		public void visit(ExprFunctionN func) {
			if (func instanceof E_OneOf) {
				E_OneOf oneOf = (E_OneOf) func;
				out.print("( ");
				oneOf.getLHS().visit(this);
				out.print(" IN ");
				printExprList(oneOf.getRHS());
				out.print(" )");
				return;
			}

			if (func instanceof E_NotOneOf) {
				E_NotOneOf oneOf = (E_NotOneOf) func;
				out.print("( ");
				oneOf.getLHS().visit(this);
				out.print(" NOT IN ");
				printExprList(oneOf.getRHS());
				out.print(" )");
				return;
			}
			printInFunctionForm(func);
		}

		private void printInFunctionForm(ExprFunction func) {
			out.print(func.getFunctionPrintName(context));
			printExprList(func.getArgs());
		}

		private void printExprList(Iterable<Expr> exprs) {
			out.print("(");
			boolean first = true;
			for (Expr expr : exprs) {
				if (expr == null) {
					break;
				}
				if (!first) {
					out.print(", ");
				}
				first = false;
				expr.visit(this);
			}
			out.print(")");
		}

		@Override
		public void visit(ExprFunctionOp funcOp) {
			String fn = funcOp.getFunctionPrintName(context);
			if (funcOp instanceof E_NotExists) {
				fn = "NOT EXISTS";
			} else if (funcOp instanceof E_Exists) {
				fn = "EXISTS";
			} else {
				throw new ARQInternalErrorException("Unrecognized ExprFunctionOp: " + fn);
			}

			SPARQLExtFormatterElement fmtElt = new SPARQLExtFormatterElement(out, context);
			out.print(fn);
			out.print(" ");
			int indent = out.getAbsoluteIndent();
			int currentCol = out.getCol();
			try {
				out.setAbsoluteIndent(currentCol);
				Element el = funcOp.getElement();
				if (el == null) {
					el = OpAsQuery.asQuery(funcOp.getGraphPattern()).getQueryPattern();
				}
				el.visit(fmtElt);
			} finally {
				out.setAbsoluteIndent(indent);
			}
		}

		@Override
		public void visit(NodeValue nv) {
			SPARQLExtFmtUtils.printNode(out, nv.asNode(), context);
		}

		@Override
		public void visit(ExprVar nv) {
			String s = nv.getVarName();
			if (Var.isBlankNodeVarName(s)) {
				// Return to a bNode via the bNode mapping of a variable.
				Var v = Var.alloc(s);
				out.print(context.getBNodeMap().asString(v));
			} else {
				// Print in variable form or as an aggregator expression
				out.print(nv.asSparqlExpr());
			}
		}

		@Override
		public void visit(ExprAggregator eAgg) {
			final Aggregator agg = eAgg.getAggregator();
			if (agg instanceof AggCount) {
				out.print("COUNT(*)");
				return;
			}
			if (agg instanceof AggCountDistinct) {
				out.print("COUNT(DISTINCT *)");
				return;
			}
			out.print(agg.getName());
			out.print("(");
			String clazz = agg.getClass().getSimpleName();
			if (clazz.endsWith("Distinct")) {
				out.print("DISTINCT ");
			}
			if (agg.getExprList() != null) {
				SPARQLExtFmtUtils.fmtSPARQL(out, agg.getExprList(), context);
			}
			String separator = null;
			if (agg instanceof AggGroupConcat) {
				separator = ((AggGroupConcat) agg).getSeparator();
			}
			if (agg instanceof AggGroupConcatDistinct) {
				separator = ((AggGroupConcatDistinct) agg).getSeparator();
			}
			if (separator != null) {
				out.print(" ; SEPARATOR=");
				String y = StrUtils.escapeString(separator);
				out.print("'");
				out.print(y);
				out.print("'");
			}
			out.print(")");
		}

		@Override
		public void visit(ExprNone exprNone) {
			throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods,
																			// choose Tools | Templates.
		}
	}
}
