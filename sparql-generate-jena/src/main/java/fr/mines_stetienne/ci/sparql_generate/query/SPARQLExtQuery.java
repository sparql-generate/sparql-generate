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
package fr.mines_stetienne.ci.sparql_generate.query;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.sparql.core.DatasetDescription;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.syntax.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.normalizer.aggregates.QueryAggregatesNormalizer;
import fr.mines_stetienne.ci.sparql_generate.normalizer.bnodes.QueryBNodeNormalizer;
import fr.mines_stetienne.ci.sparql_generate.normalizer.xexpr.QueryXExprNormalizer;
import fr.mines_stetienne.ci.sparql_generate.syntax.FromClause;

/**
 * Represents a SPARQL-Generate or SPARQL-Template query.
 *
 * @author Maxime Lefrançois
 */
public class SPARQLExtQuery extends Query {

	private static final Logger LOG = LoggerFactory.getLogger(SPARQLExtQuery.class);

	static {
		SPARQLExt.init();
		/* Ensure everything has started properly */ }

	/**
	 * Creates a new empty query.
	 */
	public SPARQLExtQuery() {
		setSyntax(SPARQLExt.SYNTAX);
	}

	/**
	 * Creates a new empty query with the given prologue.
	 */
	public SPARQLExtQuery(Prologue prologue) {
		this();
		Prologue p2 = prologue.copy();
		prefixMap = p2.getPrefixMapping();
		seenBaseURI = false;
		resolver = p2.getResolver();
	}

	/**
	 * The query type of SPARQL-Generate queries.
	 */
	public static final int QueryTypeGenerate = 555;

	/**
	 * The query type of SPARQL-Template queries.
	 */
	public static final int QueryTypeTemplate = 556;

	/**
	 * The query type of SPARQL-Perform queries.
	 */
	public static final int QueryTypePerform = 557;

	/**
	 * The query type of SPARQL-Function queries.
	 */
	public static final int QueryTypeFunction = 558;

	int queryType = QueryTypeUnknown;

	/**
	 * Specifies that the Query is a SPARQL-Generate query.
	 */
	public void setQueryGenerateType() {
		queryType = QueryTypeGenerate;
	}

	/**
	 * Specifies that the Query is a SPARQL-Template query.
	 */
	public void setQueryTemplateType() {
		queryType = QueryTypeTemplate;
	}

	/**
	 * Specifies that the Query is a SPARQL-Perform query.
	 */
	public void setQueryPerformType() {
		queryType = QueryTypePerform;
	}

	/**
	 * Specifies that the Query is a SPARQL-Function query.
	 */
	public void setQueryFunctionType() {
		queryType = QueryTypeFunction;
	}

	public void setQuerySelectType() {
		queryType = QueryTypeSelect;
	}

	public void setQueryConstructType() {
		queryType = QueryTypeConstruct;
		queryResultStar = true;
	}

	public void setQueryDescribeType() {
		queryType = QueryTypeDescribe;
	}

	public void setQueryAskType() {
		queryType = QueryTypeAsk;
	}

	public int getQueryType() {
		return queryType;
	}

	public boolean isSelectType() {
		return queryType == QueryTypeSelect;
	}

	public boolean isConstructType() {
		return queryType == QueryTypeConstruct;
	}

	public boolean isDescribeType() {
		return queryType == QueryTypeDescribe;
	}

	public boolean isAskType() {
		return queryType == QueryTypeAsk;
	}

	public boolean isUnknownType() {
		return queryType == QueryTypeUnknown;
	}

	/**
	 * Gets if the Query is a SPARQL-Generate query.
	 */
	public boolean isGenerateType() {
		return queryType == QueryTypeGenerate;
	}

	/**
	 * Gets if the Query is a SPARQL-Template query.
	 */
	public boolean isTemplateType() {
		return queryType == QueryTypeTemplate;
	}

	/**
	 * Gets if the Query is a SPARQL-Perform query.
	 */
	public boolean isPerformType() {
		return queryType == QueryTypePerform;
	}

	/**
	 * Gets if the Query is a SPARQL-Function query.
	 */
	public boolean isFunctionType() {
		return queryType == QueryTypeFunction;
	}

	private boolean hasEmbeddedExpressions;

	/**
	 * Sets if the query contains expressions in literals, URIs, or in place of
	 * variables.
	 *
	 * @param hasEmbeddedExpressions
	 */
	public void hasEmbeddedExpressions(boolean hasEmbeddedExpressions) {
		this.hasEmbeddedExpressions = hasEmbeddedExpressions;
	}

	/**
	 * Returns if the query contains expressions in literals, URIs, or in place of
	 * variables.
	 *
	 */
	public boolean hasEmbeddedExpressions() {
		return hasEmbeddedExpressions;
	}

	private VarExprList postSelect = new VarExprList();

	public boolean hasPostSelect() {
		return !postSelect.isEmpty();
	}

	public void addPostSelect(Var v, Expr expr) {
		postSelect.add(v, expr);
	}

	public VarExprList getPostSelect() {
		return postSelect;
	}

	private boolean isSubQuery;

	/**
	 * Returns if the query is a sub query of another query.
	 *
	 */
	public boolean isSubQuery() {
		return isSubQuery;
	}

	/**
	 * Sets if the query is a sub query of another query.
	 *
	 * @param isSubQuery
	 */
	public void isSubQuery(boolean isSubQuery) {
		this.isSubQuery = isSubQuery;
	}

	/**
	 * the name of the query.
	 */
	private Expr name = null;

	/**
	 * Gets if the query has a name.
	 *
	 * @return -
	 */
	public final boolean hasName() {
		return name != null;
	}

	/**
	 * Gets the name of the query, or null.
	 *
	 * @return the name node.
	 */
	public final Expr getName() {
		return name;
	}

	/**
	 * Sets the name of the query.
	 *
	 * @param name
	 *            the name of the query
	 */
	public final void setName(final Expr name) {
		this.name = name;
	}

	/**
	 * the signature of the query.
	 */
	private List<Var> signature = null;

	/**
	 * Gets if the query has a signature.
	 *
	 * @return -
	 */
	public final boolean hasSignature() {
		return signature != null;
	}

	/**
	 * Gets the signature of the query, or null.
	 *
	 * @return the signature of the query.
	 */
	public final List<Var> getSignature() {
		return signature;
	}

	/**
	 * Sets the signature of the query.
	 *
	 * @param signature
	 *            the signature of the query
	 */
	public final void setSignature(final List<Var> signature) {
		this.signature = signature;
	}

	/**
	 * the call parameters of the query.
	 */
	private ExprList callParameters = null;

	/**
	 * Gets if the query has call parameters.
	 *
	 * @return -
	 */
	public final boolean hasCallParameters() {
		return callParameters != null;
	}

	/**
	 * Gets the call parameters of the query, or null.
	 *
	 * @return call parameters.
	 */
	public final ExprList getCallParameters() {
		return callParameters;
	}

	/**
	 * Sets the call parameters of the query.
	 *
	 * @param parameters
	 *            the call parameters of the query
	 */
	public final void setCallParameters(final ExprList parameters) {
		this.callParameters = parameters;
	}

	/**
	 * the {@code GENERATE} clause of the query.
	 */
	private List<Element> generateClause = null;

	/**
	 * Gets if the query has a {@code GENERATE} clause
	 *
	 * @return -
	 */
	public final boolean hasGenerateClause() {
		return generateClause != null;
	}

	/**
	 * Gets the {@code GENERATE} form, or null if is is a URI.
	 *
	 * @return -
	 */
	public final List<Element> getGenerateClause() {
		return generateClause;
	}

	/**
	 * Set the elements in the {@code GENERATE} clause.
	 *
	 * @param generateClause
	 *            the template.
	 */
	public final void setGenerateClause(final List<Element> generateClause) {
		this.generateClause = generateClause;
	}

	/**
	 * the {@code TEMPLATE} clause of the query.
	 */
	private List<Element> templateClause = null;

	/**
	 * Gets if the query has a {@code TEMPLATE} clause.
	 *
	 * @return -
	 */
	public final boolean hasTemplateClause() {
		return templateClause != null;
	}

	/**
	 * Gets the elements in the {@code TEMPLATE} clause.
	 *
	 * @return -
	 */
	public final List<Element> getTemplateClause() {
		return templateClause;
	}

	/**
	 * Set the elements in the {@code TEMPLATE} clause.
	 *
	 * @param templateClause
	 *            the template.
	 */
	public final void setTemplateClause(final List<Element> templateClause) {
		this.templateClause = templateClause;
	}

	/**
	 * the separator expression in the {@code TEMPLATE} clause of the query.
	 */
	private Expr templateClauseSeparator = null;

	/**
	 * Returns if this query has a separator expression in the {@code TEMPLATE}
	 * clause.
	 *
	 * @return if this query has a separator expression in the {@code TEMPLATE}
	 *         clause
	 */
	public boolean hasTemplateClauseSeparator() {
		return templateClauseSeparator != null;
	}

	/**
	 * Returns the separator expression in the {@code TEMPLATE} clause.
	 *
	 * @return the separator expression in the {@code TEMPLATE} clause
	 */
	public Expr getTemplateClauseSeparator() {
		return templateClauseSeparator;
	}

	/**
	 * Set the separator expression in the {@code TEMPLATE} clause.
	 *
	 * @param templateClauseSeparator
	 *            the separator.
	 */
	public void setTemplateClauseSeparator(Expr templateClauseSeparator) {
		this.templateClauseSeparator = templateClauseSeparator;
	}

	/**
	 * the before expression in the {@code TEMPLATE} clause of the query.
	 */
	private Expr templateClauseBefore = null;

	/**
	 * Returns if this query has a before expression in the {@code TEMPLATE} clause.
	 *
	 * @return if this query has a before expression in the {@code TEMPLATE} clause
	 */
	public boolean hasTemplateClauseBefore() {
		return templateClauseBefore != null;
	}

	/**
	 * Returns the before expression in the {@code TEMPLATE} clause.
	 *
	 * @return the before expression in the {@code TEMPLATE} clause
	 */
	public Expr getTemplateClauseBefore() {
		return templateClauseBefore;
	}

	/**
	 * Set the before expression in the {@code TEMPLATE} clause.
	 *
	 * @param templateClauseBefore
	 *            the before string.
	 */
	public void setTemplateClauseBefore(Expr templateClauseBefore) {
		this.templateClauseBefore = templateClauseBefore;
	}

	/**
	 * the after expression in the {@code TEMPLATE} clause of the query.
	 */
	private Expr templateClauseAfter = null;

	/**
	 * Returns if this query has a after expression in the {@code TEMPLATE} clause.
	 *
	 * @return if this query has a after expression in the {@code TEMPLATE} clause
	 */
	public boolean hasTemplateClauseAfter() {
		return templateClauseAfter != null;
	}

	/**
	 * Returns the after expression in the {@code TEMPLATE} clause.
	 *
	 * @return the after expression in the {@code TEMPLATE} clause
	 */
	public Expr getTemplateClauseAfter() {
		return templateClauseAfter;
	}

	/**
	 * Set the after expression in the {@code TEMPLATE} clause.
	 *
	 * @param templateClauseAfter
	 *            the after string.
	 */
	public void setTemplateClauseAfter(Expr templateClauseAfter) {
		this.templateClauseAfter = templateClauseAfter;
	}

	/**
	 * the {@code PERFORM} clause of the query.
	 */
	private List<Element> performClause = null;

	/**
	 * Gets if the query has a {@code PERFORM} clause.
	 *
	 * @return -
	 */
	public final boolean hasPerformClause() {
		return performClause != null && !performClause.isEmpty();
	}

	/**
	 * Get the elements in the {@code PERFORM} clause.
	 *
	 * @return the clause.
	 */
	public final List<Element> getPerformClause() {
		return performClause;
	}

	/**
	 * Set the elements in the {@code PERFORM} clause.
	 *
	 * @param performClause
	 *            the template.
	 */
	public final void setPerformClause(final List<Element> performClause) {
		this.performClause = performClause;
	}

	/**
	 * the function expression of the query.
	 */
	private Expr functionExpression = null;

	/**
	 * Gets if the query has a function expression.
	 *
	 * @return -
	 */
	public final boolean hasFunctionExpression() {
		return functionExpression != null;
	}

	/**
	 * Gets the function expression of the query, or null.
	 *
	 * @return the function expression.
	 */
	public final Expr getFunctionExpression() {
		return functionExpression;
	}

	/**
	 * Sets the function expression of the query.
	 *
	 * @param functionExpression
	 *            the function expression
	 */
	public final void setFunctionExpression(final Expr functionExpression) {
		this.functionExpression = functionExpression;
	}

	/**
	 * the list of FROM clauses
	 */
	private List<FromClause> fromClauses = new ArrayList<>();

	/**
	 * Gets the list of FROM clauses
	 *
	 * @return
	 */
	public List<FromClause> getFromClauses() {
		return fromClauses;
	}

	/**
	 * Gets the list of FROM clauses
	 *
	 * @return
	 */
	public void setFromClauses(List<FromClause> fromClauses) {
		this.fromClauses = fromClauses;
	}

	/**
	 * adds a FROM varOrIri clause
	 *
	 * @param expr
	 */
	public void addGraphExpr(Expr expr) {
		fromClauses.add(new FromClause(expr));
	}

	/**
	 * adds a FROM NAMED varOrIri clause
	 *
	 * @param expr
	 */
	public void addNamedGraphExpr(Expr expr) {
		fromClauses.add(new FromClause(true, expr));
	}

	/**
	 * adds a FROM NAMED varOrIri clause
	 *
	 * @param expr
	 */
	public void addNamedGraphQuery(Expr expr, SPARQLExtQuery generate) {
		fromClauses.add(new FromClause(generate, expr));
	}

	/**
	 * adds a FROM GENERATE clause
	 *
	 * @param q
	 */
	public void addGraphQuery(SPARQLExtQuery generate) {
		fromClauses.add(new FromClause(generate));
	}

	@Override
	@Deprecated
	public List<String> getGraphURIs() {
		return null;
	}

	/**
	 * Throws UnsupportedOperationException
	 */
	@Override
	@Deprecated
	public void addGraphURI(String s) {
		throw new UnsupportedOperationException("Not for SPARQLExtQuery");
	}

	/**
	 * Throws UnsupportedOperationException
	 */
	@Override
	@Deprecated
	public void addNamedGraphURI(String s) {
		throw new UnsupportedOperationException("Not for SPARQLExtQuery");
	}

	/**
	 * Throws UnsupportedOperationException
	 */
	@Override
	@Deprecated
	public boolean usesGraphURI(String uri) {
		return false;
	}

	/**
	 * Throws UnsupportedOperationException
	 */
	@Override
	@Deprecated
	public List<String> getNamedGraphURIs() {
		return null;
	}

	/**
	 * Throws UnsupportedOperationException
	 */
	@Override
	@Deprecated
	public boolean usesNamedGraphURI(String uri) {
		return false;
	}

	/**
	 * Throws UnsupportedOperationException
	 */
	@Override
	@Deprecated
	public boolean hasDatasetDescription() {
		return false;
	}

	/**
	 * Throws UnsupportedOperationException
	 */
	@Override
	@Deprecated
	public DatasetDescription getDatasetDescription() {
		return null;
	}

	/**
	 * the deque of {@code SOURCE} and {@code ITERATOR} clauses of a SPARQL-Generate
	 * query.
	 */
	private List<Element> bindingClauses = null;

	/**
	 * Gets if the query has at least one {@code SOURCE} or {@code ITERATOR} clause.
	 *
	 * @return -
	 */
	public final boolean hasBindingClauses() {
		return bindingClauses != null && !bindingClauses.isEmpty();
	}

	/**
	 * Sets the list of {@code SOURCE} and {@code ITERATOR} clauses.
	 *
	 * @param list
	 *            -
	 */
	public final void setBindingClauses(final List<Element> list) {
		this.bindingClauses = list;
	}

	/**
	 * Gets the list of {@code SOURCE} and {@code ITERATOR} clauses.
	 *
	 * @return -
	 */
	public final List<Element> getBindingClauses() {
		return this.bindingClauses;
	}

	/**
	 * Change this query to a semantically equivalent one, but with no embedded
	 * expressions.
	 *
	 */
	public void normalizeXExpr() {
		if (hasEmbeddedExpressions) {
			QueryXExprNormalizer normalizer = new QueryXExprNormalizer();
			visit(normalizer);
            LOG.trace("after normalizeXExpr: " + toString());
		}
	}

	/**
	 * Change this query to a semantically equivalent one, but with no BNode common
	 * to a query and a subquery.
	 *
	 */
	public void normalizeBNode() {
		QueryBNodeNormalizer.normalizeCallParameters(this);
        LOG.trace("after normalizeBNode: " + toString());
	}

	/**
	 * Change this query to a semantically equivalent one, but with aggregates only
	 * in EXPRESSIONS clause
	 *
	 */
	public void normalizeAggregates() {
		QueryAggregatesNormalizer normalizer = new QueryAggregatesNormalizer();
		visit(normalizer);
        LOG.trace("after normalizeAggregates: " + toString());
	}

	/**
	 * Visits the query by a SPARQLExt Query visitor, or throw an exception.
	 *
	 * @param visitor
	 *            must be a SPARQLExt Query visitor.
	 * @throws IllegalArgumentException
	 *             if the query is not a SPARQLExtQuery Query.
	 */
	@Override
	public void visit(final QueryVisitor visitor) {
		if (visitor instanceof SPARQLExtQueryVisitor) {
			visit((SPARQLExtQueryVisitor) visitor);
		} else {
			SPARQLExtQueryVisitor visitor2 = new SPARQLExtQueryVisitorDelegate(visitor);
			visit((SPARQLExtQueryVisitor) visitor2);
		}
	}

	/**
	 * Visits the query by a SPARQLExt Query visitor.
	 *
	 * @param visitor
	 *            the SPARQLExt Query visitor.
	 */
	public void visit(final SPARQLExtQueryVisitor visitor) {
		visitor.startVisit(this);
		visitor.visitResultForm(this);
		visitor.visitPrologue(this);
		if (this.isSelectType()) {
			visitor.visitSelectResultForm(this);
		}
		if (this.isConstructType()) {
			visitor.visitConstructResultForm(this);
		}
		if (this.isDescribeType()) {
			visitor.visitDescribeResultForm(this);
		}
		if (this.isAskType()) {
			visitor.visitAskResultForm(this);
		}
		if (this.isGenerateType()) {
			visitor.visitGenerateClause(this);
		}
		if (this.isTemplateType()) {
			visitor.visitTemplateClause(this);
		}
		if (this.isFunctionType()) {
			visitor.visitFunctionExpression(this);
		}
		if (this.isPerformType()) {
			visitor.visitPerformClause(this);
		}
		visitor.visitDatasetDecl(this);
		visitor.visitBindingClauses(this);
		visitor.visitQueryPattern(this);
		visitor.visitGroupBy(this);
		visitor.visitHaving(this);
		visitor.visitOrderBy(this);
		visitor.visitLimit(this);
		visitor.visitOffset(this);
		visitor.visitPostSelect(this);
		visitor.visitValues(this);
		visitor.visitPragma(this);
		visitor.finishVisit(this);
	}

	/**
	 * {@inheritDoc }
	 */
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof SPARQLExtQuery)) {
			return false;
		}
		if (this == other) {
			return true;
		}
		return SPARQLExtQueryCompare.equals(this, (SPARQLExtQuery) other);
	}

	public SPARQLExtQuery cloneQuery() {

		String qs = this.toString();
		return (SPARQLExtQuery) QueryFactory.create(qs, getSyntax());
	}

	private int hashcode = -1;

	@Override
	public int hashCode() {
		if (hashcode == -1) {
			hashcode = toString().hashCode();
		}
		return hashcode;
	}

	// public Query getSelectQueryFromSignature() {
	// final Query select = QueryFactory.create("SELECT * WHERE {}");
	// final ElementTriplesBlock pattern = new ElementTriplesBlock();
	// final Node a = RDF.type.asNode();
	// if(getSignature()!=null) {
	// getSignature().forEach((p) -> {
	// final Node o = NodeFactory.createURI(p.getIri());
	// pattern.addTriple(new Triple(p.getVar(), a, o));
	// });
	// select.setQueryPattern(pattern);
	// }
	// return select;
	// }
}
