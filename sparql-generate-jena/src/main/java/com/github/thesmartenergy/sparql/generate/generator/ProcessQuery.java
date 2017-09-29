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
package com.github.thesmartenergy.sparql.generate.generator;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.engine.PlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author bakerally
 */
public class ProcessQuery {

    private static final Logger LOG = LogManager.getLogger(ProcessQuery.class);

    public static String process(String query, String conf, String outputFormat) {
        Model configurationModel = null;
        if (conf.length() > 0) {
            configurationModel = generateConfiguration(conf);
            SPARQLGenerate.getStreamManager().setLocationMapper(configurationModel);
        }

        LOG.trace("Processing Query");
        SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(query, SPARQLGenerate.SYNTAX);

        PlanFactory factory = new PlanFactory();
        RootPlan plan = factory.create(q);
        Model output = ModelFactory.createDefaultModel();

        QuerySolutionMap initialBinding = null;

        // execute plan
        plan.exec(initialBinding, output);
        StringWriter sw = new StringWriter();
        output.write(sw, outputFormat);
        System.out.println("Final RDF Output from SPARGL:");
        return sw.toString();
    }

    static Model generateConfiguration(String configuration) {
        Model configurationModel = null;
        List<String> IRI_mappings = Arrays.asList(configuration.split(";"));
        if (IRI_mappings.size() == 0) {
            System.out.println("Invalid configuration");
            return null;
        }
        String rdfMapping = "@prefix lm: <http://jena.hpl.hp.com/2004/08/location-mapping#> .\n";
        rdfMapping += "[] lm:mapping \n";
        List<String> fileMappings = new ArrayList<String>();

        for (String IRI_mapping : IRI_mappings) {

            String filePaths[] = IRI_mapping.split("=");
            if (filePaths.length == 0) {
                System.out.println(IRI_mapping + " Invalid IRI configuration");
                return null;
            } else {

                File f = new File(filePaths[1]);
                if (f.exists() && !f.isDirectory()) {
                    String currentMapping = "[ lm:name \"" + filePaths[0] + "\" ; lm:altName \"" + filePaths[1] + "\" ]";
                    System.out.println(currentMapping);
                    fileMappings.add(currentMapping);
                    System.out.println(fileMappings.size());
                } else {
                    LOG.error("File " + filePaths[1] + " not found.");

                }
            }
        }
        if (fileMappings.size() > 0) {
            System.out.println(fileMappings.size());
            String lastMapping = fileMappings.remove(fileMappings.size() - 1);
            for (String currentMapping : fileMappings) {
                rdfMapping += currentMapping + ", \n";
            }
            rdfMapping += lastMapping + ".";
        }
        String strConf;
        strConf = rdfMapping;

        configurationModel = ModelFactory.createDefaultModel();
        configurationModel.read(new ByteArrayInputStream(strConf.getBytes()), null, "TTL");
        return configurationModel;
    }
}
