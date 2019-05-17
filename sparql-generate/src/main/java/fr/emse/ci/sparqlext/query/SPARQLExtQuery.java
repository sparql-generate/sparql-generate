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
package fr.emse.ci.sparqlext.query;

import org.apache.jena.sparql.core.Prologue;
import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.normalizer.QueryNormalizer;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.syntax.Element;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Represents a SPARQL-Generate or SPARQL-Template query.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
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
        usePrologueFrom(prologue);
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
     * Returns if the query contains expressions in literals, URIs, or in place
     * of variables.
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

    private boolean isNamedSubQuery;

    /**
     * Returns if the query is a named sub query of another query.
     *
     */
    public boolean isNamedSubQuery() {
        return isNamedSubQuery;
    }

    /**
     * Sets if the query is a named sub query of another query.
     *
     * @param isNamedSubQuery
     */
    public void isNamedSubQuery(boolean isNamedSubQuery) {
        this.isNamedSubQuery = isNamedSubQuery;
    }

    /**
     * the name of the query.
     */
    private Node name = null;

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
    public final Node getName() {
        return name;
    }

    /**
     * Sets the name of the query.
     *
     * @param name the name of the query
     */
    public final void setName(final Node name) {
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
     * @param signature the signature of the query
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
     * @param parameters the call parameters of the query
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
        return generateClause != null && !generateClause.isEmpty();
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
     * @param generateClause the template.
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
        return templateClause != null && !templateClause.isEmpty();
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
     * @param templateClause the template.
     */
    public final void setTemplateClause(final List<Element> templateClause) {
        this.templateClause = templateClause;
    }

    /**
     * the separator in the {@code TEMPLATE} clause of the query.
     */
    private String templateClauseSeparator = null;

    /**
     * Returns if this query has a separator in the {@code TEMPLATE} clause.
     *
     * @return if this query has a separator in the {@code TEMPLATE} clause
     */
    public boolean hasTemplateClauseSeparator() {
        return templateClauseSeparator != null;
    }

    /**
     * Returns the separator in the {@code TEMPLATE} clause.
     *
     * @return the separator in the {@code TEMPLATE} clause
     */
    public String getTemplateClauseSeparator() {
        return templateClauseSeparator;
    }

    /**
     * Set the separator in the {@code TEMPLATE} clause.
     *
     * @param templateClauseSeparator the separator.
     */
    public void setTemplateClauseSeparator(String templateClauseSeparator) {
        this.templateClauseSeparator = templateClauseSeparator;
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
     * @param performClause the template.
     */
    public final void setPerformClause(final List<Element> performClause) {
        this.performClause = performClause;
    }

    /**
     * the deque of {@code SOURCE} and {@code ITERATOR} clauses of a
     * SPARQL-Generate query.
     */
    private List<Element> bindingClauses = null;

    /**
     * Gets if the query has at least one {@code SOURCE} or {@code ITERATOR}
     * clause.
     *
     * @return -
     */
    public final boolean hasBindingClauses() {
        return bindingClauses != null && !bindingClauses.isEmpty();
    }

    /**
     * Sets the list of {@code SOURCE} and {@code ITERATOR} clauses.
     *
     * @param list -
     */
    public final void setBindingClauses(
            final List<Element> list) {
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
    public void normalize() {
        if (!hasEmbeddedExpressions) {
            return;
        }
        QueryNormalizer normalizer = new QueryNormalizer();
        visit(normalizer);
    }

    /**
     * Visits the query by a SPARQLExt Query visitor, or throw an exception.
     *
     * @param visitor must be a SPARQLExt Query visitor.
     * @throws IllegalArgumentException if the query is not a SPARQLExtQuery
     * Query.
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
     * @param visitor the SPARQLExt Query visitor.
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

//    public Query getSelectQueryFromSignature() {
//        final Query select = QueryFactory.create("SELECT * WHERE {}");
//        final ElementTriplesBlock pattern = new ElementTriplesBlock();
//        final Node a = RDF.type.asNode();
//        if(getSignature()!=null) {
//            getSignature().forEach((p) -> {
//                final Node o = NodeFactory.createURI(p.getIri());
//                pattern.addTriple(new Triple(p.getVar(), a, o));
//            });
//            select.setQueryPattern(pattern);
//        }
//        return select;
//    }
}
