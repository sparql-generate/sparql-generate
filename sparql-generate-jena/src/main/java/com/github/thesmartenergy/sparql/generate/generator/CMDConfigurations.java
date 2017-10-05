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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class CMDConfigurations {
    public static CommandLine parseArguments(String[] args) throws ParseException {
        
         DefaultParser commandLineParser = new DefaultParser();
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
                .addOption("h", "help", false, "Show help")
                .addOption("d", "dir", true, "Location of the directory with the queries, documentset, dataset, and configuration files as explained in https://w3id.org/sparql-generate/language-cli.html. Default value is .")
                .addOption("q", "query-file", true, "Name of the query file in the directory. Default value is query.rqg")
                .addOption("o" , "output", true, "Location where the output is to be stored. No value means output goes to the console.")
                .addOption("l", "log-level", true, "Set log level, acceptable values are TRACE < DEBUG < INFO < WARN < ERROR < FATAL < OFF. No value or unrecognized value results in level DEBUG")
                .addOption("f" , "log-file", true, "Location where the log is to be stored. No value means output goes to the console.")
                ;
        return opt;
    }
    
    public static void displayHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("SPARQL-Generate processor", getCMDOptions());
        System.exit(1);
    }
}
