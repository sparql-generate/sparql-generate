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
package com.github.thesmartenergy.sparql.generate.api;

import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_BNode2;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_CBOR;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_CSV;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_CustomCSV;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_DateTime;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_HTMLAttribute;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_HTMLTag;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_HTMLTagElement;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_JSONPath;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_Regex;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_SplitAtPostion;
import com.github.thesmartenergy.sparql.generate.jena.function.library.FN_XPath;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionRegistry;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_CBOR;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_CSSPath;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_CSV;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_CSVFirstRow;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_CSVHeaders;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_CSVStream;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_CSVWrapped;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_CustomCSV;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_JSONListElement;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_JSONListKeys;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_JSONPath;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_Regex;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_Split;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITE_XPath;
import javax.ws.rs.ApplicationPath;
import org.apache.jena.sparql.function.FunctionRegistry;
import org.glassfish.jersey.server.ResourceConfig;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@ApplicationPath("api")
public class JerseyApp extends ResourceConfig {
    
    static {
        FunctionRegistry fnreg = FunctionRegistry.get();
        fnreg.put(FN_JSONPath.URI, FN_JSONPath.class);
        fnreg.put(FN_XPath.URI, FN_XPath.class);
        fnreg.put(FN_CSV.URI, FN_CSV.class);
        fnreg.put(FN_CustomCSV.URI, FN_CustomCSV.class);
        fnreg.put(FN_SplitAtPostion.URI, FN_SplitAtPostion.class);
        fnreg.put(FN_HTMLTag.URI, FN_HTMLTag.class);
        fnreg.put(FN_HTMLAttribute.URI, FN_HTMLAttribute.class);
        fnreg.put(FN_CBOR.URI, FN_CBOR.class);
        fnreg.put(FN_Regex.URI, FN_Regex.class);
        fnreg.put(FN_BNode2.URI, FN_BNode2.class);
        fnreg.put(FN_HTMLTagElement.URI, FN_HTMLTagElement.class);
        fnreg.put(FN_DateTime.URI, FN_DateTime.class);

        
        IteratorFunctionRegistry itereg = IteratorFunctionRegistry.get();
        itereg.put(ITE_JSONPath.URI, ITE_JSONPath.class);
        itereg.put(ITE_JSONListKeys.URI, ITE_JSONListKeys.class);
        itereg.put(ITE_JSONListElement.URI, ITE_JSONListElement.class);
        itereg.put(ITE_Regex.URI, ITE_Regex.class);
        itereg.put(ITE_XPath.URI, ITE_XPath.class);
        itereg.put(ITE_Split.URI, ITE_Split.class);
        itereg.put(ITE_CSV.URI, ITE_CSV.class);
        itereg.put(ITE_CustomCSV.URI, ITE_CustomCSV.class);
        itereg.put(ITE_CSVFirstRow.URI, ITE_CSVFirstRow.class);
        itereg.put(ITE_CSVWrapped.URI, ITE_CSVWrapped.class);
        itereg.put(ITE_CSSPath.URI, ITE_CSSPath.class);
        itereg.put(ITE_CBOR.URI, ITE_CBOR.class);
        itereg.put(ITE_CSVHeaders.URI, ITE_CSVHeaders.class);
        itereg.put(ITE_CSVStream.URI, ITE_CSVStream.class);
    }

    public JerseyApp() {
        packages("com.github.thesmartenergy.sparql.generate.api");
    }
}
