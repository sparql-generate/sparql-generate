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
package com.github.thesmartenergy.sparql.generate.jena;

import com.github.thesmartenergy.sparql.generate.jena.cli.Request;
import com.github.thesmartenergy.sparql.generate.jena.engine.PlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import com.github.thesmartenergy.sparql.generate.jena.stream.LocatorFileAccept;
import com.github.thesmartenergy.sparql.generate.jena.stream.LookUpRequest;
import com.github.thesmartenergy.sparql.generate.jena.stream.SPARQLGenerateStreamManager;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementSource;
import com.github.thesmartenergy.sparql.generate.jena.utils.SPARQLGenerateUtils;
import com.google.gson.Gson;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.log4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>, Maxime Lefrançois
 * <maxime.lefrancois at emse.fr>
 */
public class SPARQLGenerateCli {

    private static final Logger LOG = LoggerFactory.getLogger(SPARQLGenerateCli.class);
    private static final Gson GSON = new Gson();

    private static final Layout LAYOUT = new PatternLayout("%5p [%t] (%F:%L) - %m%n");
    private static final Appender CONSOLE_APPENDER = new ConsoleAppender(LAYOUT);
    private static final org.apache.log4j.Logger ROOT_LOGGER = org.apache.log4j.Logger.getRootLogger();

    private static final String ARG_HELP = "h";
    private static final String ARG_HELP_LONG = "help";
    private static final String ARG_DIRECTORY = "d";
    private static final String ARG_DIRECTORY_LONG = "dir";
    private static final String ARG_DIRECTORY_DEFAULT = ".";
    private static final String ARG_QUERY = "q";
    private static final String ARG_QUERY_LONG = "query-file";
    private static final String ARG_QUERY_DEFAULT = "query.rqg";
    private static final String ARG_OUTPUT = "o";
    private static final String ARG_OUTPUT_LONG = "output";
    private static final String ARG_OUTPUT_APPEND = "oa";
    private static final String ARG_OUTPUT_APPEND_LONG = "output-append";
    private static final String ARG_OUTPUT_FORMAT = "of";
    private static final String ARG_OUTPUT_FORMAT_LONG = "output-format";
    private static final String ARG_SOURCE_LONG = "source";
    private static final String ARG_STREAM = "s";
    private static final String ARG_STREAM_LONG = "stream";
    private static final String ARG_LOG_LEVEL = "l";
    private static final String ARG_LOG_LEVEL_LONG = "log-level";
    private static final String ARG_LOG_FILE = "f";
    private static final String ARG_LOG_FILE_LONG = "log-file";

    public static void main(String[] args) throws ParseException {
        Instant start = Instant.now();
        CommandLine cl = CMDConfigurations.parseArguments(args);

        if (cl.getOptions().length == 0) {
            CMDConfigurations.displayHelp();
            return;
        }

        setLogging(cl);

        String dirPath = cl.getOptionValue(ARG_DIRECTORY, ARG_DIRECTORY_DEFAULT);
        File dir = new File(dirPath);

        SPARQLGenerate.init();

        // read sparql-generate-conf.json
        Request request;
        try {
            String conf = IOUtils.toString(
                    new FileInputStream(new File(dir, "sparql-generate-conf.json")), StandardCharsets.UTF_8);
            request = GSON.fromJson(conf, Request.class);
        } catch (Exception ex) {
            LOG.warn("Error while loading the location mapping model for the queryset. No named queries will be used");
            request = Request.DEFAULT;
        }

        // initialize stream manager
        SPARQLGenerateStreamManager sm = SPARQLGenerateStreamManager
                .makeStreamManager(new LocatorFileAccept(dir.toURI().getPath()));
        sm.setLocationMapper(request.asLocationMapper());
        SPARQLGenerate.setStreamManager(sm);

        String queryPath = cl.getOptionValue(ARG_QUERY, ARG_QUERY_DEFAULT);
        String query;
        try {
            query = IOUtils.toString(SPARQLGenerate.getStreamManager()
                    .open(new LookUpRequest(queryPath, SPARQLGenerate.MEDIA_TYPE)), StandardCharsets.UTF_8);
        } catch (IOException | NullPointerException ex) {
            LOG.error("No file named {} was found in the directory that contains the query to be executed.", queryPath);
            return;
        }

        SPARQLGenerateQuery q;
        try {
            q = (SPARQLGenerateQuery) QueryFactory.create(query, SPARQLGenerate.SYNTAX);
        } catch (Exception ex) {
            LOG.error("Error while parsing the query to be executed.", ex);
            return;
        }

        replaceSourcesIfRequested(cl, q);

        RootPlan plan;
        try {
            plan = PlanFactory.create(q);
        } catch (Exception ex) {
            LOG.error("Error while creating the plan for the query.", ex);
            return;
        }

        Dataset ds;
        try {
            ds = SPARQLGenerateUtils.loadDataset(dir, request);
        } catch (Exception ex) {
            LOG.warn("Error while loading the dataset, no dataset will be used.");
            ds = DatasetFactory.create();
        }

        String output = cl.getOptionValue(ARG_OUTPUT);
        boolean outputAppend = cl.hasOption(ARG_OUTPUT_APPEND);
        Lang outputLang = RDFLanguages.nameToLang(cl.getOptionValue(ARG_OUTPUT_FORMAT, RDFLanguages.strLangTurtle));

        boolean stream = cl.hasOption(ARG_STREAM);
        if (stream) {
            StreamRDF outputRDF;
            if (output == null) {
                outputRDF = new ConsoleStreamRDF(System.out, q.getPrefixMapping());
            } else {
                try {
                    outputRDF = new ConsoleStreamRDF(
                            new PrintStream(new FileOutputStream(output, outputAppend)), q.getPrefixMapping());
                } catch (IOException ex) {
                    LOG.error("Error while opening the output file.", ex);
                    return;
                }
            }
            try {
                plan.exec(ds, outputRDF);
            } catch (Exception ex) {
                LOG.error("Error while executing the plan.", ex);
            }
        } else {
            try {
                Model model = plan.exec(ds);
                if (output == null) {
                    model.write(System.out, outputLang.getLabel());
                } else {
                    try {
                        model.write(new FileOutputStream(output, outputAppend), outputLang.getLabel());
                    } catch (IOException ex) {
                        LOG.error("Error while opening the output file.", ex);
                        return;
                    }
                }
            } catch (Exception ex) {
                LOG.error("Error while executing the plan.", ex);
            }
        }
        long millis = Duration.between(start, Instant.now()).toMillis();
        System.out.println("Program finished in " + String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))));
    }

    private static class ConsoleStreamRDF implements StreamRDF {

        private final PrefixMapping pm;
        private final SerializationContext context;

        private PrintStream out;

        int i = 0;

        public ConsoleStreamRDF(PrintStream out, PrefixMapping pm) {
            this.out = out;
            this.pm = pm;
            context = new SerializationContext(pm);
        }

        @Override
        public void start() {
            pm.getNsPrefixMap().forEach((prefix, uri) -> {
                out.append("@prefix ").append(prefix).append(": <").append(uri).append("> .\n");
            });
        }

        @Override
        public void base(String string) {
            out.append("@base <").append(string).append(">\n");
        }

        @Override
        public void prefix(String prefix, String uri) {
            out.append("@prefix ").append(prefix).append(": <").append(uri).append("> .\n");
        }

        @Override
        public void triple(Triple triple) {
            out.append(FmtUtils.stringForTriple(triple, context)).append(" .\n");
            i++;
            if (i > 1000) {
                i = 0;
                out.flush();
            }
        }

        @Override
        public void quad(Quad quad) {
        }

        @Override
        public void finish() {
            LOG.info("end of transformation");
        }
    }

    private static void setLogging(CommandLine cl) {
        try {
            Level level = Level.toLevel(cl.getOptionValue("l"), Level.DEBUG);
            ROOT_LOGGER.setLevel(level);
        } catch (Exception ex) {
            ROOT_LOGGER.setLevel(Level.DEBUG);
        }

        String logPath = cl.getOptionValue(ARG_LOG_FILE);
        if (logPath == null) {
            ROOT_LOGGER.addAppender(CONSOLE_APPENDER);
        } else {
            try {
                ROOT_LOGGER.addAppender(new org.apache.log4j.RollingFileAppender(LAYOUT, logPath, false));
            } catch (IOException ex) {
                System.out.println(ex.getClass() + "occurred while initializing the log file: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private static void replaceSourcesIfRequested(CommandLine cli, SPARQLGenerateQuery query) {
        final Properties replacementSources = cli.getOptionProperties(ARG_SOURCE_LONG);

        List<Element> updatedSources = query.getIteratorsAndSources().stream()
                .map(element -> {
                    if (element instanceof ElementSource) {
                        ElementSource elementSource = (ElementSource) element;
                        String sourceURI = elementSource.getSource().toString(query.getPrefixMapping(), false);

                        if (replacementSources.containsKey(sourceURI)) {
                            Node replacementSource = NodeFactory.createURI(replacementSources.getProperty(sourceURI));

                            LOG.info("Replaced source <{}> with <{}>.", sourceURI, replacementSource);

                            return new ElementSource(replacementSource,
                                    elementSource.getAccept(),
                                    elementSource.getVar());
                        }
                    }

                    return element;
                })
                .collect(Collectors.toList());

        query.setIteratorsAndSources(updatedSources);
    }

    private static class CMDConfigurations {

        public static CommandLine parseArguments(String[] args) throws ParseException {

            DefaultParser commandLineParser = new DefaultParser();
            CommandLine cl = commandLineParser.parse(getCMDOptions(), args);

            /*Process Options*/
            //print help menu
            if (cl.hasOption(ARG_HELP)) {
                CMDConfigurations.displayHelp();
            }

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
                            "Set log level, acceptable values are TRACE < DEBUG < INFO < WARN < ERROR < FATAL < OFF. No value or unrecognized value results in level DEBUG")
                    .addOption(ARG_LOG_FILE, ARG_LOG_FILE_LONG, true,
                            "Location where the log is to be stored. No value means output goes to the console.")
                    .addOption(ARG_STREAM, ARG_STREAM_LONG, false, "Generate output as stream.")
                    .addOption(sourcesOpt);
        }

        public static void displayHelp() {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("SPARQL-Generate processor", getCMDOptions());
            System.exit(1);
        }
    }

}
