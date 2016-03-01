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
package org.w3id.sparql.generate.lang;

import org.apache.jena.sparql.lang.SPARQLParserBase;
import org.w3id.sparql.generate.query.SPARQLGenerateQuery; 

/**
 * Class that has all the parse event operations and other query/update specific
 * things
 */
public class SPARQLGenerateParserBase extends SPARQLParserBase {

    @Override
    protected void startQuery() { }
        
    protected void startSubGenerate() {
        pushQuery();
        query = new SPARQLGenerateQuery(getPrologue());
    }

    protected SPARQLGenerateQuery endSubGenerate(int line, int column) {
        SPARQLGenerateQuery subQuery = (SPARQLGenerateQuery) query;
        if (!subQuery.isGenerateType()) {
            throwParseException("Subquery not a GENERATE query", line, column);
        }
        popQuery();
        return subQuery;
    }
    
    public SPARQLGenerateQuery getSPARQLGenerateQuery() {
        if(query instanceof SPARQLGenerateQuery) {
            return (SPARQLGenerateQuery) query;
        }
        return null;
    }
    
}
