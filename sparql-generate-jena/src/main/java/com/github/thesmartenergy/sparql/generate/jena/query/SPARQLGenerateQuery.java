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
package com.github.thesmartenergy.sparql.generate.jena.query;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.syntax.ElementGroup;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.normalizer.QueryNormalizer;
import java.util.List;
import org.apache.jena.sparql.syntax.Element;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class SPARQLGenerateQuery extends Query {

    static {
        SPARQLGenerate.init();
        /* Ensure everything has started properly */ }

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
     * @param hasEmbeddedExpressions 
     */
    public boolean hasEmbeddedExpressions() {
        return hasEmbeddedExpressions;
    }

    /**
     * Creates a new empty query.
     */
    public SPARQLGenerateQuery() {
        setSyntax(SPARQLGenerate.SYNTAX);
    }

    /**
     * Creates a new empty query with the given prologue.
     */
    public SPARQLGenerateQuery(Prologue prologue) {
        this();
        usePrologueFrom(prologue);
    }

    /**
     * The query type of SPARQL Generate queries.
     */
    public static final int QueryTypeGenerate = 555;

    /**
     * The query type of the query.
     */
    int queryType = QueryTypeUnknown;

    /**
     * Specifies that the Query is a SPARQL Generate query.
     */
    public void setQueryGenerateType() {
        queryType = QueryTypeGenerate;
    }

    /**
     * Gets if the Query is a SPARQL Generate query.
     */
    public boolean isGenerateType() {
        return queryType == QueryTypeGenerate;
    }

    /** the {@code GENERATE} clause URI. */
    private String generateURI = null;

    /** the {@code GENERATE} template. */
    private ElementGroup generateTemplate = null;

    /** the deque of {@code SOURCE} and {@code ITERATOR} clauses. */
    private List<Element> iteratorsAndSources = null;

    /**
     * Gets if the {@code GENERATE} clause is a URI.
     * @return -
     */
    public final boolean hasGenerateURI() {
        return generateURI != null;
    }

    /**
     * Gets the {@code GENERATE} URI of the query, or null.
     * @return the URI string.
     */
    public final String getGenerateURI() {
        return generateURI;
    }

    /**
     * Sets the {@code GENERATE} URI of the query.
     * @param generateURI the URI
     */
    public final void setGenerateURI(final String generateURI) {
        this.generateURI = generateURI;
    }

    /**
     * Gets if the query has a {@code GENERATE} template.
     * @return -
     */
    public final boolean hasGenerateTemplate() {
        return generateTemplate != null && !generateTemplate.isEmpty();
    }

    /**
     * Set the element in the {@code GENERATE} clause.
     * @param generateTemplate the template.
     */
    public final void setGenerateTemplate(final ElementGroup generateTemplate) {
        this.generateTemplate = generateTemplate;
    }

    /**
     * Gets the {@code GENERATE} template, or null if is is a URI.
     * @return -
     */
    public final ElementGroup getGenerateTemplate() {
        return generateTemplate;
    }

    /**
     * Gets if the query has at least one {@code SOURCE} or {@code ITERATOR}
     * clause.
     * @return -
     */
    public final boolean hasIteratorsAndSources() {
        return iteratorsAndSources != null && !iteratorsAndSources.isEmpty();
    }

    /**
     * Sets the list of {@code SOURCE} and {@code ITERATOR}
     * clauses.
     * @param list -
     */
    public final void setIteratorsAndSources(
            final List<Element> list) {
        this.iteratorsAndSources = list;
    }

    /**
     * Gets the list of {@code SOURCE} and {@code ITERATOR}
     * clauses.
     * @return -
     */
    public final List<Element> getIteratorsAndSources() {
        return this.iteratorsAndSources;
    }
    
    /**
     * Return a new query semantically equivalent to this one, but with no
     * embedded expressions.
     * 
     * @return 
     */
    public SPARQLGenerateQuery normalize() {
        if(!hasEmbeddedExpressions) {
            return this;
        }
        SPARQLGenerateQuery query = (SPARQLGenerateQuery) cloneQuery();
        QueryNormalizer normalizer = new QueryNormalizer();
        query.visit(normalizer);
        return query;
    }
    

    /**
     * Visits the query by a SPARQL Generate Query visitor, or throw an
     * exception.
     * @param visitor must be a SPARQL Generate Query visitor.
     * @throws IllegalArgumentException if the query is not a SPARQL Generate
     * Query.
     */
    @Override
    public void visit(final QueryVisitor visitor) {
        if (visitor instanceof SPARQLGenerateQueryVisitor) {
            visit((SPARQLGenerateQueryVisitor) visitor);
        } else {
            throw new IllegalArgumentException(
                    "Only instances of SPARQLGenerateQueryVisitor can visit"
                            + " SPARQL Generate Queries.");
        }
    }

    /**
     * Visits the query by a SPARQL Generate Query visitor.
     * @param visitor the SPARQL Generate Query visitor.
     */
    public void visit(final SPARQLGenerateQueryVisitor visitor)
    {
        visitor.startVisit(this);
        visitor.visitResultForm(this);
        visitor.visitPrologue(this);
        if ( this.isSelectType() )
            visitor.visitSelectResultForm(this);
        if ( this.isConstructType() )
            visitor.visitConstructResultForm(this);
        if ( this.isDescribeType() )
            visitor.visitDescribeResultForm(this);
        if ( this.isAskType() )
            visitor.visitAskResultForm(this);
        if ( this.isGenerateType() ) 
            visitor.visitGenerateResultForm(this);
        visitor.visitDatasetDecl(this);
        visitor.visitIteratorsAndSources(this);
        visitor.visitQueryPattern(this);
        visitor.visitGroupBy(this);
        visitor.visitHaving(this);
        visitor.visitOrderBy(this);
        visitor.visitLimit(this);
        visitor.visitOffset(this);
        visitor.visitValues(this);
        visitor.finishVisit(this);
    }

    @Override
    public boolean equals(Object other) {
        if ( ! ( other instanceof SPARQLGenerateQuery ) )
            return false ;
        if ( this == other ) return true ;
        return SPARQLGenerateQueryCompare.equals(this, (SPARQLGenerateQuery)other) ;
    }

}