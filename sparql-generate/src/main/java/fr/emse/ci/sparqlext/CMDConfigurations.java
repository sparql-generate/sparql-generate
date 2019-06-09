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
    public static final String ARG_HELP_MAN = "Show help";
    public static final String ARG_DIRECTORY = "d";
    public static final String ARG_DIRECTORY_LONG = "dir";
    public static final String ARG_DIRECTORY_DEFAULT = ".";
    public static final String ARG_DIRECTORY_MAN = "Location of the directory with the queryset, documentset, dataset, and configuration files as explained in https://w3id.org/sparql-generate/language-cli.html. Default value is . (the current folder)";
    public static final String ARG_BASE = "b";
    public static final String ARG_BASE_LONG = "base";
    public static final String ARG_BASE_MAN = "Base URI of the working directory. If set, each file in the working directory is identified by a URI resolved against the base.";
    public static final String ARG_QUERY = "q";
    public static final String ARG_QUERY_LONG = "query-file";
    public static final String ARG_QUERY_MAN = "Name of the query file in the directory. Default value is ./query.rqg";
    public static final String ARG_OUTPUT = "o";
    public static final String ARG_OUTPUT_LONG = "output";
    public static final String ARG_OUTPUT_MAN = "Location where the output is to be stored. No value means output goes to the console.";
    public static final String ARG_OUTPUT_APPEND = "oa";
    public static final String ARG_OUTPUT_APPEND_LONG = "output-append";
    public static final String ARG_OUTPUT_APPEND_MAN = "Write from the end of the output file, instead of replacing it.";
    public static final String ARG_OUTPUT_FORMAT = "of";
    public static final String ARG_OUTPUT_FORMAT_LONG = "output-format";
    public static final String ARG_OUTPUT_FORMAT_MAN = "Format of the output file, e.g. TTL, NT, etc.";
    public static final String ARG_SOURCE_LONG = "source";
    public static final String ARG_SOURCE_MAN = "Replaces <source> in a SOURCE clause with the given value, e.g. urn:sg:source=source.json.";
    public static final String ARG_STREAM = "s";
    public static final String ARG_STREAM_LONG = "stream";
    public static final String ARG_STREAM_MAN = "Generate output as stream.";
    public static final String ARG_HDT = "hdt";
    public static final String ARG_HDT_LONG = "hdt";
    public static final String ARG_HDT_MAN = "Generate output as HDT.";
    public static final String ARG_LOG_LEVEL = "l";
    public static final String ARG_LOG_LEVEL_LONG = "log-level";
    public static final String ARG_LOG_LEVEL_MAN = "Set log level, acceptable values are TRACE < DEBUG < INFO < WARN < ERROR < OFF. No value or unrecognized value results in level DEBUG";
    public static final String ARG_LOG_FILE = "f";
    public static final String ARG_LOG_FILE_LONG = "log-file";
    public static final String ARG_LOG_FILE_MAN = "Location where the log is to be stored. No value means output goes to the console.";
    public static final String ARG_DEBUG_TEMPLATE = "dt";
    public static final String ARG_DEBUG_TEMPLATE_LONG = "debug-template";
    public static final String ARG_DEBUG_TEMPLATE_MAN = "Debug the template output: insert warning identifiers that refer to the log.";

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
                .desc(ARG_SOURCE_MAN)
                .build();

        return new Options()
                .addOption(ARG_HELP, ARG_HELP_LONG, false, ARG_HELP_MAN)
                .addOption(ARG_DIRECTORY, ARG_DIRECTORY_LONG, true, ARG_DIRECTORY_MAN)
                .addOption(ARG_QUERY, ARG_QUERY_LONG, true, ARG_QUERY_MAN)
                .addOption(ARG_OUTPUT, ARG_OUTPUT_LONG, true, ARG_OUTPUT_MAN)
                .addOption(ARG_OUTPUT_APPEND, ARG_OUTPUT_APPEND_LONG, false, ARG_OUTPUT_APPEND_MAN)
                .addOption(ARG_OUTPUT_FORMAT, ARG_OUTPUT_FORMAT_LONG, true, ARG_OUTPUT_FORMAT_MAN)
                .addOption(ARG_LOG_LEVEL, ARG_LOG_LEVEL_LONG, true, ARG_LOG_LEVEL_MAN)
                .addOption(ARG_LOG_FILE, ARG_LOG_FILE_LONG, true, ARG_LOG_FILE_MAN)
                .addOption(ARG_STREAM, ARG_STREAM_LONG, false, ARG_STREAM_MAN)
                .addOption(ARG_HDT, ARG_HDT_LONG, false, ARG_HDT_MAN)
                .addOption(ARG_BASE, ARG_BASE_LONG, true, ARG_BASE_MAN)
                .addOption(ARG_DEBUG_TEMPLATE, ARG_DEBUG_TEMPLATE_LONG, false, ARG_DEBUG_TEMPLATE_MAN)
                .addOption(sourcesOpt);
    }

    public static void displayHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("SPARQL-Generate processor", getCMDOptions());
        System.exit(1);
    }
}
