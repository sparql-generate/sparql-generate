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

/**
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.log4j.Logger;

public class CMDGenerator {
    
    static Logger LOG;
    
    public static void main(String [] args){
        
        List<String> formats = Arrays.asList("TTL","TURTLE","NTRIPLES","TRIG","RDFXML","JSONLD");
       
        try {
            CommandLine cl = CMDConfigurations.parseArguments(args);
            String query = "";
            String outputFormat = "TTL";
            
            if (cl.getOptions().length == 0){
                CMDConfigurations.displayHelp();
                return;
            }
            
            
            if ( cl.hasOption('l') ) {
               Enumeration<String> loggers = LogManager.getLogManager().getLoggerNames();
                while (loggers.hasMoreElements()) {
                   java.util.logging.Logger element = LogManager.getLogManager().getLogger(loggers.nextElement());
                   element.setLevel(Level.OFF);
                }
                Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
            }
            LOG = Logger.getLogger(CMDGenerator.class);
            
            
        
            //get query file path
            //check if the file exists
            if (cl.hasOption("qf")){
                String file_path = cl.getOptionValue("qf");
                File f = new File(file_path);
                if(f.exists() && !f.isDirectory()) { 
                    FileInputStream fisTargetFile = new FileInputStream(f);
                    query = IOUtils.toString(new BOMInputStream(fisTargetFile), "UTF-8");    
                    System.out.println("\n\nRead SPARQL-Generate Query ..\n"+query+"\n\n");
                    LOG.debug("\n\nRead SPARQL-Generate Query ..\n"+query+"\n\n");
                } else {
                    System.out.println("File "+file_path+" not found.");
                    LOG.error("File "+file_path+" not found.");
                }   
            }
            
            //get query string
            if (cl.hasOption("qs")){
                query = cl.getOptionValue("qs");
            }
            System.out.println("Query:"+query);
            
            //get and validate the output format
            if (cl.hasOption("f")){
                String format = cl.getOptionValue("f");
                if (formats.contains(format)){
                    outputFormat = format;
                } else {
                    System.out.println("Invalid output format,"+cl.getOptionProperties("f").getProperty("description"));
                    LOG.error("Invalid output format,"+cl.getOptionProperties("f").getProperty("description"));
                    return;
                }
            }
            
            Model configurationModel = null;
            String conf = "";
            if (cl.hasOption("c")){
                conf =  cl.getOptionValue("c");
                configurationModel = ProcessQuery.generateConfiguration(conf);
            }
            
            String output = ProcessQuery.process(query, conf, outputFormat);
            System.out.println(output);
         
            
            
            
        } catch (org.apache.commons.cli.ParseException ex) {
            System.out.println(ex);
            LOG.error(ex);
        } catch (FileNotFoundException ex) {
            System.out.println(ex);
            LOG.error(ex);
        } catch (IOException ex) {
            System.out.println(ex);
            LOG.error(ex);
        }
        
        
    }
}
