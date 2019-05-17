/*
 * Copyright 2017 École des Mines de Saint-Étienne.
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
package fr.emse.ci.sparqlext.utils;

/**
 *
 * @author maxime.lefrancois
 */
public class Response {
    static public enum TYPE {
        GENERATE(0), SELECT(1), TEMPLATE(2);

        private final int value;
        
        private TYPE(int value) {
            this.value = value;
        }
        
    }
    public String log;
    public int type; // 0=GENERATE, 1=SELECT, 2=TEMPLATE
    public String result;
    public boolean clear;

    public static Response clear() {
        Response r = new Response();
        r.clear = true;
        return r;
    }

    public static Response log(String log) {
        Response r = new Response();
        r.log = log;
        return r;
    }

    public static Response result(String result, TYPE type) {
        Response r = new Response();
        r.result = result;
        r.type = type.value;
        return r;
    }
    
}
