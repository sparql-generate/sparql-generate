/*
 * Copyright 2019 École des Mines de Saint-Étienne.
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
package fr.emse.ci.sparqlext;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author maxime.lefrancois
 */
public class CMDConfigurations {

    public static final String ARG_HELP = "h";
    public static final String ARG_HELP_LONG = "help";
    public static final String ARG_DIRECTORY = "d";
    public static final String ARG_DIRECTORY_LONG = "dir";
    public static final String ARG_DIRECTORY_DEFAULT = ".";
    public static final String ARG_QUERY = "q";
    public static final String ARG_QUERY_LONG = "query-file";
    public static final String ARG_OUTPUT = "o";
    public static final String ARG_OUTPUT_LONG = "output";
    public static final String ARG_OUTPUT_APPEND = "oa";
    public static final String ARG_OUTPUT_APPEND_LONG = "output-append";
    public static final String ARG_OUTPUT_FORMAT = "of";
    public static final String ARG_OUTPUT_FORMAT_LONG = "output-format";
    public static final String ARG_SOURCE_LONG = "source";
    public static final String ARG_STREAM = "s";
    public static final String ARG_STREAM_LONG = "stream";
    public static final String ARG_HDT = "hdt";
    public static final String ARG_HDT_LONG = "hdt";
    public static final String ARG_LOG_LEVEL = "l";
    public static final String ARG_LOG_LEVEL_LONG = "log-level";
    public static final String ARG_LOG_FILE = "f";
    public static final String ARG_LOG_FILE_LONG = "log-file";
    
    public static CommandLine parseArguments(String[] args) throws ParseException {
        DefaultParser commandLineParser = new DefaultParser();
        CommandLine cl = commandLineParser.parse(getCMDOptions(), args);
        return cl;
    }

    public static Options getCMDOptions() {
        Option sourcesOpt = Option.builder()
                .numberOfArgs(2)
                .valueSeparator()
                .argName("uri=uri")
                .longOpt(ARG_SOURCE_LONG)
                .desc("Replaces <source> in a SOURCE clause with the given value, e.g. urn:sg:source=source.json.")
                .build();

        return new Options()
                .addOption(ARG_HELP, ARG_HELP_LONG, false, "Show help")
                .addOption(ARG_DIRECTORY, ARG_DIRECTORY_LONG, true,
                        "Location of the directory with the queryset, documentset, dataset, and configuration files as explained in https://w3id.org/sparql-generate/language-cli.html. Default value is . (the current folder)")
                .addOption(ARG_QUERY, ARG_QUERY_LONG, true,
                        "Name of the query file in the directory. Default value is ./query.rqg")
                .addOption(ARG_OUTPUT, ARG_OUTPUT_LONG, true,
                        "Location where the output is to be stored. No value means output goes to the console.")
                .addOption(ARG_OUTPUT_APPEND, ARG_OUTPUT_APPEND_LONG, false,
                        "Write from the end of the output file, instead of replacing it.")
                .addOption(ARG_OUTPUT_FORMAT, ARG_OUTPUT_FORMAT_LONG, true,
                        "Format of the output file, e.g. TTL, NT, etc.")
                .addOption(ARG_LOG_LEVEL, ARG_LOG_LEVEL_LONG, true,
                        "Set log level, acceptable values are TRACE < DEBUG < INFO < WARN < ERROR < OFF. No value or unrecognized value results in level DEBUG")
                .addOption(ARG_LOG_FILE, ARG_LOG_FILE_LONG, true,
                        "Location where the log is to be stored. No value means output goes to the console.")
                .addOption(ARG_STREAM, ARG_STREAM_LONG, false, "Generate output as stream.")
                .addOption(ARG_HDT, ARG_HDT_LONG, false, "Generate output as HDT.")
                .addOption(sourcesOpt);
    }

    public static void displayHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("SPARQL-Generate processor", getCMDOptions());
        System.exit(1);
    }
}
