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
package com.github.thesmartenergy.sparql.generate.jena.function.library;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.log4j.Logger;
import org.apache.jena.sparql.function.FunctionBase1;

/**
 * A SPARQL Function that extracts a string from a XML document, according to a
 * XPath expression. The Function URI is
 * {@code <http://w3id.org/sparql-generate/fn/XPath>}.
 *
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class FN_Unzip extends FunctionBase1 {

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(FN_Unzip.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.FN + "Unzip";

    @Override
    public NodeValue exec(NodeValue zip) {
        System.out.println("content of the zip: " + zip.getString());
        try {
            ZipInputStream zipIn = new ZipInputStream(IOUtils.toInputStream(zip.getString()));
            ZipEntry entry = zipIn.getNextEntry();
            if(entry.isDirectory()) {
                throw new Exception("is a directory");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            
            StringWriter sw = new StringWriter();
            IOUtils.copy(zipIn,sw);

            System.out.println(sw);
            
            return new NodeValueString(sw.toString());

        } catch (Exception e) {
            throw new ExprEvalException("Error while Unzipping: " + e.getMessage(), e);
        }
    }

}
