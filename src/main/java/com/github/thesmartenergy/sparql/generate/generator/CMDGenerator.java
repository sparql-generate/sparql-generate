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
package com.github.thesmartenergy.sparql.generate.generator;

/**
 *
 * @author Noorani Bakerally
 */
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateException;
import com.github.thesmartenergy.sparql.generate.jena.engine.PlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.github.thesmartenergy.sparql.generate.jena.lang.ParseException;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.LocationMapper;
import org.apache.jena.util.Locator;
import org.apache.jena.util.LocatorFile;
import org.apache.log4j.Logger;
import org.apache.commons.cli.Options;
import org.apache.jena.atlas.logging.Log;
import org.apache.log4j.Logger;

public class CMDGenerator {
    
    static Logger LOG;
    static FileManager fileManager;
    
    public static void main(String [] args){
        
        
       
        List<String> formats = Arrays.asList("TTL","TURTLE","NTRIPLES","TRIG","RDFXML","JSONLD");
        
        //command line options goes here
        Options opt = new Options()
                .addOption("h", false, "Show help")
                .addOption("qf", "queryfile", true, "Local path to the file containing the SPARGL query")
                .addOption("f","outputformat",true,"Output RDF format, e.g. -f TTL. Possible serializations are: TTL for Turtle, NTRIPLES for NTRIPLES, RDFXML for RDF/XML, N3 for N3, JSONLD for JSON-LD, TRIG for TRIG")
                .addOption("l", false, "Disable logging, by default logging is enabled")
                ;
                
        try {
            //parsing the command line options
            BasicParser parser = new BasicParser();
            CommandLine cl = parser.parse(opt, args);
            
            
            String query = "";
            String outputFormat = "TTL";
            
            //print help menu
            if ( cl.hasOption('h') ) {
                HelpFormatter f = new HelpFormatter();
                f.printHelp("OptionsTip", opt);
                return;
            } 
            
            LOG = Logger.getLogger(CMDGenerator.class);
            fileManager = FileManager.makeGlobal(); 
            if ( cl.hasOption('l') ) {
                LOG.setLevel(org.apache.log4j.Level.OFF);
            }
            
            
            
        
            //get query file path
            if (cl.hasOption("qf")){
                String file_path = cl.getOptionValue("qf");
                File f = new File(file_path);
                if(f.exists() && !f.isDirectory()) { 
                    FileInputStream fisTargetFile = new FileInputStream(f);
                    query = IOUtils.toString(fisTargetFile, "UTF-8");    
                    LOG.debug("\n\nRead SPARGL Query ..\n"+query+"\n\n");
                } else {
                    LOG.error("File "+file_path+" not found.");
                }   
            }
            if (cl.hasOption("f")){
                String format = cl.getOptionValue("f");
                if (formats.contains(format)){
                    outputFormat = format;
                } else {
                    LOG.error("Invalid output format,"+cl.getOptionProperties("f").getProperty("description"));
                    return;
                }
                
            }
         
            LOG.debug("Processing Query");
            SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(query, SPARQLGenerate.SYNTAX);
            
            PlanFactory factory = new PlanFactory(fileManager);
            RootPlan plan = factory.create(q);
            Model output = ModelFactory.createDefaultModel();
            QuerySolutionMap initialBinding = null;

            // execute plan
            plan.exec(initialBinding, output);
            
            StringWriter sw = new StringWriter();
            
            
            output.write(sw,outputFormat);
            
            System.out.println(sw.toString());
            
            
        } catch (org.apache.commons.cli.ParseException ex) {
            LOG.error(ex);
        } catch (FileNotFoundException ex) {
            LOG.error(ex);
        } catch (IOException ex) {
            LOG.error(ex);
        }
        
        
    }
}
