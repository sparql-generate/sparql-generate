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
package fr.emse.ci.sparqlext.cli;

import static fr.emse.ci.sparqlext.cli.CMDConfigurations.*;
import fr.emse.ci.sparqlext.engine.PlanFactory;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.stream.LocatorFileAccept;
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import fr.emse.ci.sparqlext.syntax.ElementSource;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import fr.emse.ci.sparqlext.SPARQLExt;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.sparql.syntax.Element;
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
import fr.emse.ci.sparqlext.engine.RootPlan;
import fr.emse.ci.sparqlext.stream.LocationMapperAccept;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.apache.jena.sparql.util.Context;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.rdfhdt.hdt.hdt.HDT;

/**
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>, Maxime Lefrançois
 * <maxime.lefrancois at emse.fr>
 */
public class SPARQLExtCli {

    private static final Logger LOG = LoggerFactory.getLogger(SPARQLExtCli.class);

    private static final Layout LAYOUT = new PatternLayout("%d{mm:ss,SSS} %t %-5p %c:%L - %m%n");
    private static final org.apache.log4j.Logger ROOT_LOGGER = org.apache.log4j.Logger.getRootLogger();
    private static final String CONF_FILE = "sparql-generate-conf.json";
    private static final Level[] LOG_LEVELS = new Level[]{Level.OFF, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE};

    static {
        SPARQLExt.init();
    }

    public static void main(String[] args) throws ParseException {
        Instant start = Instant.now();
        CommandLine cl = CMDConfigurations.parseArguments(args);

        if (cl.getOptions().length == 0 || cl.hasOption(ARG_HELP)) {
            CMDConfigurations.displayHelp();
            return;
        }

        String dirName = cl.getOptionValue(ARG_DIRECTORY, ARG_DIRECTORY_DEFAULT);
        if(dirName.equals("")) {
            dirName = ARG_DIRECTORY_DEFAULT;
        }
        File dirFile = new File(dirName);

        // read sparql-generate-conf.json
        CliRequest r = createRequest(dirFile, cl);
        ROOT_LOGGER.setLevel(r.logLevelObject);
        if (r.logFile != null) {
            try {
                ROOT_LOGGER.getAppender("stdout").addFilter(new Filter() {
                    @Override
                    public int decide(LoggingEvent event) {
                        if(event.getLevel().isGreaterOrEqual(Level.INFO)) {
                            return Filter.ACCEPT;
                        } else {
                            return Filter.DENY;
                        }
                    }
                });
                ROOT_LOGGER.addAppender(new org.apache.log4j.RollingFileAppender(LAYOUT, r.logFile, false));
            } catch (IOException ex) {
                throw new RuntimeException("Exception while initializing the log file", ex);
            }
        }
        SPARQLExtStreamManager sm = prepareStreamManager(dirFile, r);
        SPARQLExtQuery q = getQueryOrDie(r, sm);
        final Context context = SPARQLExt.createContext(q, sm);
        SPARQLExt.setDebugStConcat(context, r.debugTemplate);

        try {
            replaceSourcesIfRequested(cl, q);

            RootPlan plan;
            try {
                plan = PlanFactory.create(q);
            } catch (Exception ex) {
                LOG.error("Error while creating the plan for the query.", ex);
                return;
            }

            final Dataset ds = getDataset(dirFile, r);

            if (!q.isGenerateType() && r.hdt) {
                LOG.error("Option HDT is only for queries of type GENERATE");
                return;
            }
            if (q.isTemplateType() && r.outputFormat != null) {
                LOG.error("Option outputFormat is only for queries of type GENERATE or SELECT");
                return;
            }

            if (q.isGenerateType() && !r.stream && !r.hdt) {
                execGenerate(q, plan, ds, context, r);
            } else if (q.isGenerateType() && r.debugTemplate) {
                execGenerateHDT(q, plan, ds, context, r);
            } else if (q.isGenerateType()) {
                execGenerateStream(q, plan, ds, context, r);
            } else if (q.isSelectType() && !r.stream) {
                execSelect(q, plan, ds, context, r);
            } else if (q.isSelectType()) {
                execSelectStream(q, plan, ds, context, r);
            } else if (q.isTemplateType() && !r.stream) {
                execTemplate(q, plan, ds, context, r);
            } else if (q.isTemplateType()) {
                execTemplateStream(q, plan, ds, context, r);
            } else {
                LOG.error("Error: unsupported query type");
            }

            long millis = Duration.between(start, Instant.now()).toMillis();
            int min = (int)(millis/60000);
            int sec = (int)(millis%60000/1000);
            int milli = (int)(millis%1000);
            LOG.info("Program finished in " + String.format("%d min, %d.%d sec",
                    min,sec,milli));

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void execGenerateHDT(SPARQLExtQuery q, RootPlan plan, Dataset ds, Context context, CliRequest request) {
        if (request.output == null) {
            LOG.error("Output needs to be set with the option HDT.");
        }
        HDT hdt = null;
        try {
            HDTStreamRDF hdtStreamRDF = new HDTStreamRDF(q.getBaseURI());
            hdt = hdtStreamRDF.getHDT();
            plan.execGenerate(ds, hdtStreamRDF, context).get();
            try (OutputStream out = new FileOutputStream(request.output)) {
                // Save generated HDT to a file
                hdt.saveToHDT(out, null);
            } catch (IOException ex) {
                LOG.error("Error while opening the output file.", ex);
            }
        } catch (ExecutionException ex) {
            LOG.error("Error while executing the plan.", ex);
        } catch (InterruptedException ex) {
            LOG.error("Interrupted while executing the plan.", ex);
        } finally {
            if (hdt != null) {
                try {
                    hdt.close();
                } catch (IOException ex) {
                    LOG.error("Error while closing the HDT.", ex);
                }
            }
        }
    }

    private static void execGenerateStream(Query q, RootPlan plan, Dataset ds, Context context, CliRequest request) {
        final ConsoleStreamRDF futurePrintStreamRDF;
        if (request.output == null) {
            futurePrintStreamRDF = new ConsoleStreamRDF(System.out, q.getPrefixMapping());
        } else {
            try {
                futurePrintStreamRDF = new ConsoleStreamRDF(
                        new PrintStream(new FileOutputStream(request.output, request.outputAppend)), q.getPrefixMapping());
            } catch (IOException ex) {
                LOG.error("Error while opening the output file.", ex);
                return;
            }
        }
        try {
            plan.execGenerate(ds, futurePrintStreamRDF, context).get();
        } catch (ExecutionException ex) {
            LOG.error("Error while executing the plan.", ex);
        } catch (InterruptedException ex) {
            LOG.error("Interrupted while executing the plan.", ex);
        }
    }

    private static void execGenerate(Query q, RootPlan plan, Dataset ds, Context context, CliRequest request) {
        if (request.outputFormat == null) {
            request.outputFormat = RDFLanguages.strLangTurtle;
        }
        Lang lang = RDFLanguages.nameToLang(request.outputFormat);
        try {
            Model model = plan.execGenerate(ds, context);
            if (request.output == null) {
                model.write(System.out, lang.getLabel());
            } else {
                try {
                    model.write(new FileOutputStream(request.output, request.outputAppend), lang.getLabel());
                } catch (IOException ex) {
                    LOG.error("Error while opening the output file.", ex);
                }
            }
        } catch (Exception ex) {
            LOG.error("Error while executing the plan.", ex);
        }
    }

    private static void execSelect(Query q, RootPlan plan, Dataset ds, Context context, CliRequest request) {
        try {
            ResultSet result = plan.execSelect(ds, context);
            OutputStream out;
            try {
                if (request.output == null) {
                    out = System.out;
                } else {
                    out = new FileOutputStream(request.output, request.outputAppend);
                }
                ResultsFormat format = getResultsFormat(request);
                if (format.equals(ResultsFormat.FMT_TEXT)) {
                    ResultSetFormatter.out(out, result);
                } else {
                    ResultSetFormatter.output(out, result, format);
                }
            } catch (IOException ex) {
                LOG.error("Error while opening the output file.", ex);
            }
        } catch (Exception ex) {
            LOG.error("Error while executing the plan.", ex);
        }
    }

    private static void execSelectStream(Query q, RootPlan plan, Dataset ds, Context context, CliRequest request) {
        try {
            OutputStream out;
            try {
                if (request.output == null) {
                    out = System.out;
                } else {
                    out = new FileOutputStream(request.output, request.outputAppend);
                }
                ResultsFormat format = getResultsFormat(request);
                plan.execSelect(ds,
                        (result) -> {
                            LOG.info("entering accept");
                            if (format.equals(ResultsFormat.FMT_TEXT)) {
                                ResultSetFormatter.out(out, result);
                            } else {
                                ResultSetFormatter.output(out, result, format);
                            }
                            LOG.info("exiting accept");
                        },
                        context).get();
            } catch (IOException ex) {
                LOG.error("Error while opening the output file.", ex);
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Error while executing the plan.", ex);
        }
    }

    private static void execTemplate(Query q, RootPlan plan, Dataset ds, Context context, CliRequest request) {
        try {
            PrintStream ps;
            if (request.output == null) {
                ps = System.out;
            } else {
                ps = new PrintStream(new FileOutputStream(request.output, request.outputAppend));
            }
            String result = plan.execTemplate(ds, context);
            ps.print(result);
            ps.flush();
        } catch (Exception ex) {
            LOG.error("Error while executing the plan.", ex);
        }
    }

    private static void execTemplateStream(Query q, RootPlan plan, Dataset ds, Context context, CliRequest request) {
        try {
            PrintStream ps;
            if (request.output == null) {
                ps = System.out;
            } else {
                ps = new PrintStream(new FileOutputStream(request.output, request.outputAppend));
            }
            plan.execTemplate(ds, ps::print, context).get();
        } catch (FileNotFoundException | InterruptedException | ExecutionException ex) {
            LOG.error("Error while executing the plan.", ex);
        }
    }

    private static Dataset getDataset(File dir, FileConfigurations request) {
        try {
            return SPARQLExt.loadDataset(dir, request);
        } catch (Exception ex) {
            LOG.warn("Error while loading the dataset, no dataset will be used.");
            return DatasetFactory.create();
        }
    }

    private static void replaceSourcesIfRequested(CommandLine cli, SPARQLExtQuery query) {
        final Properties replacementSources = cli.getOptionProperties(ARG_SOURCE_LONG);

        List<Element> updatedSources = query.getBindingClauses().stream()
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

        query.setBindingClauses(updatedSources);
    }

    private static SPARQLExtStreamManager prepareStreamManager(File dirFile, CliRequest request) {
        Path dirPath = Paths.get(dirFile.toURI());
        // initialize stream manager
        LocatorFileAccept locator = new LocatorFileAccept(dirFile.toURI().getPath());
        LocationMapperAccept mapper = new LocationMapperAccept();
        SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator);
        sm.setLocationMapper(mapper);

        if (request.namedqueries != null) {
            request.namedqueries.forEach((doc) -> {
                LookUpRequest req = new LookUpRequest(doc.uri, doc.mediatype);
                LookUpRequest alt = new LookUpRequest(doc.path);
                mapper.addAltEntry(req, alt);
            });
        }
        if (request.documentset != null) {
            request.documentset.forEach((doc) -> {
                LookUpRequest req = new LookUpRequest(doc.uri, doc.mediatype);
                LookUpRequest alt = new LookUpRequest(doc.path);
                mapper.addAltEntry(req, alt);
            });
        }
        if (request.base != null) {
            try {
                Files.walk(dirPath)
                        .filter((p) -> {
                            return p.toFile().isFile();
                        })
                        .forEach((p) -> {
                            String relativePath = dirPath.relativize(p).toString();
                            String url = request.base + relativePath.replace("\\", "/");
                            mapper.addAltEntry(url, p.toString());
                        });
            } catch (IOException ex) {
                LOG.warn("Error while computing the URIs for the files in the working directory.", ex);
            }
        }
        return sm;
    }

    private static ResultsFormat getResultsFormat(CliRequest request) {
        if (request.outputFormat == null) {
            return ResultsFormat.FMT_TEXT;
        } else {
            switch (request.outputFormat) {
                case "XML":
                    return ResultsFormat.FMT_RS_XML;
                case "CSV":
                    return ResultsFormat.FMT_RS_CSV;
                case "JSON":
                    return ResultsFormat.FMT_RS_JSON;
                case "TSV":
                    return ResultsFormat.FMT_RS_TSV;
                case "TEXT":
                    return ResultsFormat.FMT_TEXT;
                default:
                    LOG.warn("Output lang not valid for SELECT queries. Expecting one of XML, CSV, JSON, TSV, or TEXT.");
                    return ResultsFormat.FMT_TEXT;
            }
        }
    }

    private static CliRequest createRequest(File dirFile, CommandLine cl) {
        CliRequest request;
        try {
            String conf = IOUtils.toString(
                    new FileInputStream(new File(dirFile, CONF_FILE)), StandardCharsets.UTF_8);
            request = (new Gson()).fromJson(conf, CliRequest.class);
        } catch (IOException ex) {
            LOG.warn("IOException while loading the location mapping model for the queryset.");
            request = new CliRequest();
        } catch (JsonSyntaxException ex) {
            LOG.warn("JSON Syntax exceptioun while loading the location mapping model for the queryset.", ex);
            request = new CliRequest();
        }
        request.query = cl.getOptionValue(ARG_QUERY, request.query);
        request.base = cl.getOptionValue(ARG_BASE, request.base);
        request.output = cl.getOptionValue(ARG_OUTPUT, request.output);
        request.outputAppend = cl.hasOption(ARG_OUTPUT_APPEND);
        request.outputFormat = cl.getOptionValue(ARG_OUTPUT_FORMAT, request.outputFormat);
        request.stream = cl.hasOption(ARG_STREAM) || request.stream;
        request.hdt = cl.hasOption(ARG_HDT) || request.hdt;
        request.debugTemplate = cl.hasOption(ARG_DEBUG_TEMPLATE) || request.debugTemplate;
        request.logFile = cl.getOptionValue(ARG_LOG_FILE, request.logFile);
        try {
            request.logLevelObject = Level.toLevel(cl.getOptionValue(ARG_LOG_LEVEL), LOG_LEVELS[request.loglevel]);
        } catch (Exception ex) {
            request.logLevelObject = Level.DEBUG;
        }
        return request;
    }

    private static SPARQLExtQuery getQueryOrDie(CliRequest request, SPARQLExtStreamManager sm) {
        String query;
        try {
            query = IOUtils.toString(sm
                    .open(new LookUpRequest(request.query, SPARQLExt.MEDIA_TYPE)), StandardCharsets.UTF_8);
        } catch (IOException | NullPointerException ex) {
            throw new RuntimeException(String.format("No file named %s was found in the directory that contains the query to be executed.", request.query), ex);
        }

        try {
            SPARQLExtQuery q = (SPARQLExtQuery) QueryFactory.create(query, request.base, SPARQLExt.SYNTAX);
            q.setBaseURI(q.getBaseURI());
            return q;
        } catch (Exception ex) {
            throw new RuntimeException("Error while parsing the query to be executed.", ex);
        }
    }

    private static class CliRequest extends FileConfigurations {
        private Level logLevelObject;
    }

}
