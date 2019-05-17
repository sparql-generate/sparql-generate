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
package fr.emse.ci.sparqlext.jersey.resources;

/**
 *
 * @author maxime.lefrancois
 */
public class QueryRequest {

    private String query;

    private boolean valid;

    private String log;

    private String result;

    private String graph;

    public QueryRequest() {
    }

    public QueryRequest(String query, boolean valid, String log, String result, String graph) {
        this.query = query;
        this.valid = valid;
        this.log = log;
        this.result = result;
        this.graph = graph;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getLog() {
        return log;
    }

    public boolean getValid() {
        return valid;
    }

    public String getResult() {
        return result;
    }

    public String getGraph() {
        return graph;
    }

    public boolean setValid() {
        return valid;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setGraph(String graph) {
        this.graph = graph;
    }

}
