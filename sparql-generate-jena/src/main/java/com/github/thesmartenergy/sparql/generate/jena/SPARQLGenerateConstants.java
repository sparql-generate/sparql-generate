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
package com.github.thesmartenergy.sparql.generate.jena;

import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.SystemARQ;
import org.apache.jena.sparql.util.Symbol;

/**
 * Adds a symbol for {@link registrySelectors#registryIterators} 
 * to {@link ARQConstants}.
 * 
 * @author maxime.lefrancois
 */
public class SPARQLGenerateConstants extends ARQConstants {

    /**
     * The selectors library registry key.
     */
    public static final Symbol registryIterators =
        SystemARQ.allocSymbol("registryIterators");
}
