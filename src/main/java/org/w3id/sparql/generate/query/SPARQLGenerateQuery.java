/*
 * Copyright 2016 ITEA 12004 SEAS Project.
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
package org.w3id.sparql.generate.query;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.w3id.sparql.generate.SPARQLGenerate;

/**
 *
 * @author maxime.lefrancois
 */
public class SPARQLGenerateQuery extends Query {

    static {
        SPARQLGenerate.init();
        /* Ensure everything has started properly */ }

    /**
     * Creates a new empty query
     */
    public SPARQLGenerateQuery() {
        setSyntax(SPARQLGenerateSyntax.syntaxSPARQLGenerate);
    }

    /**
     * Creates a new empty query with the given prologue
     */
    public SPARQLGenerateQuery(Prologue prologue) {
        this();
        usePrologueFrom(prologue);
    }

    public static final int QueryTypeGenerate = 555;

    int queryType = QueryTypeUnknown;

    public void setQueryGenerateType() {
        queryType = QueryTypeGenerate;
    }

    public boolean isGenerateType() {
        return queryType == QueryTypeGenerate;
    }

    // ---- GENERATE
    private String source = null;
    private ElementGroup generateTemplate = null;
    private Expr selector = null;

    public boolean hasSource() {
        return source != null;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean hasGenerateTemplate() {
        return generateTemplate != null && !generateTemplate.isEmpty();
    }

    /**
     * Set the element in the GENERATION clause of a SPARGL GENERATE query
     */
    public void setGenerateTemplate(ElementGroup generateTemplate) {
        this.generateTemplate = generateTemplate;
    }

    /**
     * Get the element in the GENERATION clause of a SPARGL GENERATE query
     */
    public ElementGroup getGenerateTemplate() {
        return generateTemplate;
    }

    public boolean hasSelector() {
        return selector != null;
    }

    public Expr getSelector() {
        return selector;
    }

    public void setSelector(Expr selector) {
        this.selector = selector;
    }
    
    public void visit(QueryVisitor visitor) {
        if(visitor instanceof SPARQLGenerateQueryVisitor) {
            visit((SPARQLGenerateQueryVisitor) visitor);
        } else {
            throw new IllegalArgumentException("A SPARQLGenerateQueryVisitor is needed to visit a SPARQL Generate Query.");
        }
    }

                
    public void visit(SPARQLGenerateQueryVisitor visitor)
    {
        visitor.startVisit(this) ;
        visitor.visitResultForm(this) ;
        visitor.visitPrologue(this) ;
        if ( this.isSelectType() )
            visitor.visitSelectResultForm(this) ;
        if ( this.isConstructType() )
            visitor.visitConstructResultForm(this) ;
        if ( this.isDescribeType() )
            visitor.visitDescribeResultForm(this) ;
        if ( this.isAskType() )
            visitor.visitAskResultForm(this) ;
        if ( this.isGenerateType() ) 
            visitor.visitGenerateResultForm(this) ;
        visitor.visitDatasetDecl(this) ;
        visitor.visitQueryPattern(this) ;
        visitor.visitGroupBy(this) ;
        visitor.visitHaving(this) ;
        visitor.visitOrderBy(this) ;
        visitor.visitOffset(this) ;
        visitor.visitLimit(this) ;
        visitor.visitValues(this) ;
        visitor.visitSelector(this) ;
        visitor.finishVisit(this) ;
    }
    
}
