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
import com.github.thesmartenergy.sparql.generate.jena.lang.ParseException;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
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
import org.apache.log4j.Logger;

public class CMDGenerator {
    
    static Logger LOG;
     
    public static void main(String [] args){
        
        LOG = Logger.getLogger(CMDGenerator.class);
        //command line options goes here
        Options opt = new Options()
                .addOption("qf", "queryfile", true, "Path to the file containing the SPARGL query")
                .addOption("h", false, "Print help")
                
                ;
                
        try {
            //parsing the command line options
            BasicParser parser = new BasicParser();
            CommandLine cl = parser.parse(opt, args);
            
            //print help menu
            if ( cl.hasOption('h') ) {
                HelpFormatter f = new HelpFormatter();
                f.printHelp("OptionsTip", opt);
            } 
            
            //get query file path
            if (cl.hasOption("qf")){
                System.out.println(cl.getOptionValue("qf"));
            }
       
        } catch (org.apache.commons.cli.ParseException ex) {
            LOG.error(ex);
        }
        
        
    }
}
