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

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Noorani Bakerally
 */
public class CMDConfigurations {
    public static CommandLine parseArguments(String[] args) throws ParseException {
        
         BasicParser commandLineParser = new BasicParser();
         CommandLine cl = commandLineParser.parse(getCMDOptions(), args);
         
         /*Process Options*/
        
        //print help menu
        if ( cl.hasOption('h') ) {
            CMDConfigurations.displayHelp();
        }
        
        
         
         
        return cl;
    }
    
    public static Options getCMDOptions(){
        Options opt = new Options()
                .addOption("h", false, "Show help")
                .addOption("qf", "queryfile", true, "Local path to the file containing the SPARGL query")
                .addOption("qs", "query string", true, "The SPARGL query string")
                .addOption("f","outputformat",true,"Output RDF format, e.g. -f TTL. Possible serializations are: TTL for Turtle, NTRIPLES for NTRIPLES, RDFXML for RDF/XML, N3 for N3, JSONLD for JSON-LD, TRIG for TRIG")
                .addOption("l", false, "Disable logging, by default logging is enabled")
                .addOption("c", true, "Configuration for mapping remote IRI to local files of the form IRI=/path/to/file1;IRI=/path/to/file2")
                ;
        return opt;
    }
    
    public static void displayHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("RML Processor", getCMDOptions());
        System.exit(1);
    }
}
