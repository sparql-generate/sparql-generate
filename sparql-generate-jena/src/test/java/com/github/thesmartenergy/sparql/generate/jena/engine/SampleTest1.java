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
package com.github.thesmartenergy.sparql.generate.jena.engine;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.ResultSetRewindable;
/**
 *
 * @author bakerally
 */
public class SampleTest1 {
    public static void main(String [] args){
        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
        "SELECT *\n" +
        "WHERE {\n" +
        "         BIND(STRLANG(\"Belgium\",\"en\") as ?countryName)\n" +
        "         SERVICE <http://dbpedia.org/sparql> {\n" +
        "                SELECT ?resource_uri ?countryName ?fr_label WHERE {\n" +
        "                    ?resource_uri rdf:type <http://dbpedia.org/ontology/Country> ;\n" +
        "                              rdfs:label ?countryName,?fr_label .\n" +
        "                              FILTER(lang(?fr_label) = \"fr\")\n" +
        "                 \n" +
        "               } LIMIT 1\n" +
        "         }\n" +
        "}";
        Dataset dataset = TDBFactory.createDataset( "path" );
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query);
        try {
           
            ResultSetRewindable results = ResultSetFactory.makeRewindable(qexec.execSelect());
            ResultSetFormatter.out(System.out, results);
            
        } finally {
            qexec.close();
        }
    }
}
