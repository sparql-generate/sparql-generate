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
package fr.emse.ci.sparqlext;

import static fr.emse.ci.sparqlext.CMDConfigurations.*;
import fr.emse.ci.sparqlext.utils.Request;
import fr.emse.ci.sparqlext.generate.engine.PlanFactory;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.stream.LocatorFileAccept;
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import fr.emse.ci.sparqlext.syntax.ElementSource;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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
import fr.emse.ci.sparqlext.generate.engine.RootPlan;
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
import org.apache.jena.sparql.util.ResultSetUtils;
import org.rdfhdt.hdt.hdt.HDT;

/**
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>, Maxime Lefrançois
 * <maxime.lefrancois at emse.fr>
 */
public class SPARQLExtCli {

    private static final Logger LOG = LoggerFactory.getLogger(SPARQLExtCli.class);
    private static final Gson GSON = new Gson();

    private static final Layout LAYOUT = new PatternLayout("%-5p - %d - %c{1}:%L - %m%n");
    private static final org.apache.log4j.Logger ROOT_LOGGER = org.apache.log4j.Logger.getRootLogger();

    public static void main(String[] args) throws ParseException {
        Instant start = Instant.now();
        CommandLine cl = CMDConfigurations.parseArguments(args);

        if (cl.getOptions().length == 0 || cl.hasOption(ARG_HELP)) {
            CMDConfigurations.displayHelp();
            return;
        }

        SPARQLExt.init();
        String dir = cl.getOptionValue(ARG_DIRECTORY, ARG_DIRECTORY_DEFAULT);
        File dirFile = new File(dir);

        // read sparql-generate-conf.json
        Request request;
        try {
            String conf = IOUtils.toString(
                    new FileInputStream(new File(dirFile, "sparql-generate-conf.json")), StandardCharsets.UTF_8);
            request = GSON.fromJson(conf, Request.class);
        } catch (IOException ex) {
            LOG.warn("IOException while loading the location mapping model for the queryset.");
            request = Request.DEFAULT;
        } catch (JsonSyntaxException ex) {
            LOG.warn("JSON Syntax exceptioun while loading the location mapping model for the queryset.", ex);
            request = Request.DEFAULT;
        }

        SPARQLExtStreamManager sm = prepareStreamManager(dirFile, request, cl);

        setLogging(cl, request.loglevel);

        String queryPath = cl.getOptionValue(ARG_QUERY, request.query);
        String query;
        SPARQLExtQuery q;
        try {
            try {
                query = IOUtils.toString(sm
                        .open(new LookUpRequest(queryPath, SPARQLExt.MEDIA_TYPE)), StandardCharsets.UTF_8);
            } catch (IOException | NullPointerException ex) {
                LOG.error("No file named {} was found in the directory that contains the query to be executed.", queryPath);
                return;
            }

            try {
                q = (SPARQLExtQuery) QueryFactory.create(query, SPARQLExt.SYNTAX);
            } catch (Exception ex) {
                LOG.error("Error while parsing the query to be executed.", ex);
                return;
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        final Context context = SPARQLExt.createContext(q.getPrefixMapping(), sm);

        SPARQLExt.setDebugStConcat(context, cl.hasOption(ARG_DEBUG_TEMPLATE));

        try {
            replaceSourcesIfRequested(cl, q);

            RootPlan plan;
            try {
                plan = PlanFactory.create(q);
            } catch (Exception ex) {
                LOG.error("Error while creating the plan for the query.", ex);
                return;
            }

            final Dataset ds = getDataset(dirFile, request);

            String output = cl.getOptionValue(ARG_OUTPUT);
            boolean outputAppend = cl.hasOption(ARG_OUTPUT_APPEND);
            String outputLang = cl.getOptionValue(ARG_OUTPUT_FORMAT, null);

            boolean stream = cl.hasOption(ARG_STREAM);
            boolean hdt = cl.hasOption(ARG_HDT);

            if (!q.isGenerateType() && hdt) {
                LOG.error("Option HDT is only for queries of type GENERATE");
                return;
            }
            if (q.isTemplateType() && outputLang != null) {
                LOG.error("Option outputLang is only for queries of type GENERATE or SELECT");
                return;
            }

            if (q.isGenerateType() && !stream && !hdt) {
                execGenerate(q, plan, ds, context, outputLang, output, outputAppend);
            } else if (q.isGenerateType() && hdt) {
                execGenerateHDT(q, plan, ds, context, output, outputAppend);
            } else if (q.isGenerateType()) {
                execGenerateStream(q, plan, ds, context, output, outputAppend);
            } else if (q.isSelectType() && !stream) {
                execSelect(q, plan, ds, context, outputLang, output, outputAppend);
            } else if (q.isSelectType()) {
                execSelectStream(q, plan, ds, context, outputLang, output, outputAppend);
            } else if (q.isTemplateType() && !stream) {
                execTemplate(q, plan, ds, context, output, outputAppend);
            } else if (q.isTemplateType()) {
                execTemplateStream(q, plan, ds, context, output, outputAppend);
            } else {
                LOG.error("Error: unsupported query type");
            }

            long millis = Duration.between(start, Instant.now()).toMillis();
            System.out.println("Program finished in " + String.format("%d min, %d sec",
                    TimeUnit.MILLISECONDS.toMinutes(millis),
                    TimeUnit.MILLISECONDS.toSeconds(millis)
                    - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))));

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void execGenerateHDT(SPARQLExtQuery q, RootPlan plan, Dataset ds, Context context, String output, boolean outputAppend) {
        if (output == null) {
            LOG.error("Output needs to be set with the option HDT.");
        }
        HDT hdt = null;
        try {
            HDTStreamRDF hdtStreamRDF = new HDTStreamRDF(q.getBaseURI());
            hdt = hdtStreamRDF.getHDT();
            plan.execGenerate(ds, hdtStreamRDF, context).get();
            try (OutputStream out = new FileOutputStream(output)) {
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

    private static void execGenerateStream(Query q, RootPlan plan, Dataset ds, Context context, String output, boolean outputAppend) {
        final ConsoleStreamRDF futurePrintStreamRDF;
        if (output == null) {
            futurePrintStreamRDF = new ConsoleStreamRDF(System.out, q.getPrefixMapping());
        } else {
            try {
                futurePrintStreamRDF = new ConsoleStreamRDF(
                        new PrintStream(new FileOutputStream(output, outputAppend)), q.getPrefixMapping());
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

    private static void execGenerate(Query q, RootPlan plan, Dataset ds, Context context, String outputLang, String output, boolean outputAppend) {
        if (outputLang == null) {
            outputLang = RDFLanguages.strLangTurtle;
        }
        Lang lang = RDFLanguages.nameToLang(outputLang);
        try {
            Model model = plan.execGenerate(ds, context);
            if (output == null) {
                model.write(System.out, lang.getLabel());
            } else {
                try {
                    model.write(new FileOutputStream(output, outputAppend), lang.getLabel());
                } catch (IOException ex) {
                    LOG.error("Error while opening the output file.", ex);
                    return;
                }
            }
        } catch (Exception ex) {
            LOG.error("Error while executing the plan.", ex);
        }
    }

    private static void execSelect(Query q, RootPlan plan, Dataset ds, Context context, String outputLang, String output, boolean outputAppend) {
        try {
            ResultSet result = plan.execSelect(ds, context);
            OutputStream out;
            try {
                if (output == null) {
                    out = System.out;
                } else {
                    out = new FileOutputStream(output, outputAppend);
                }
                ResultsFormat format = getResultsFormat(outputLang);
                if(format.equals(ResultsFormat.FMT_TEXT)) {
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

    private static void execSelectStream(Query q, RootPlan plan, Dataset ds, Context context, String outputLang, String output, boolean outputAppend) {
        try {
            OutputStream out;
            try {
                if (output == null) {
                    out = System.out;
                } else {
                    out = new FileOutputStream(output, outputAppend);
                }
                ResultsFormat format = getResultsFormat(outputLang);
                plan.execSelect(ds,
                       (result) -> {
                            LOG.info("entering accept");
                            if(format.equals(ResultsFormat.FMT_TEXT)) {
                                ResultSetFormatter.out(out, result);
                            } else{
                                ResultSetFormatter.output(out, result, format);                            
                            }
                            LOG.info("exiting accept");
                        },
                        context).get();
            } catch (IOException ex) {
                LOG.error("Error while opening the output file.", ex);
            }
        } catch (Exception ex) {
            LOG.error("Error while executing the plan.", ex);
        }
    }

    private static void execTemplate(Query q, RootPlan plan, Dataset ds, Context context, String output, boolean outputAppend) {
        try {
            PrintStream ps;
            if (output == null) {
                ps = System.out;
            } else {
                ps = new PrintStream(new FileOutputStream(output, outputAppend));
            }
            String result = plan.execTemplate(ds, context);
            ps.print(result);
            ps.flush();
        } catch (Exception ex) {
            LOG.error("Error while executing the plan.", ex);
        }
    }

    private static void execTemplateStream(Query q, RootPlan plan, Dataset ds, Context context, String output, boolean outputAppend) {
        try {
            PrintStream ps;
            if (output == null) {
                ps = System.out;
            } else {
                ps = new PrintStream(new FileOutputStream(output, outputAppend));
            }
            plan.execTemplate(ds, ps::print, context).get();
        } catch (FileNotFoundException | InterruptedException | ExecutionException ex) {
            LOG.error("Error while executing the plan.", ex);
        }
    }

    private static Dataset getDataset(File dir, Request request) {
        try {
            return SPARQLExt.loadDataset(dir, request);
        } catch (Exception ex) {
            LOG.warn("Error while loading the dataset, no dataset will be used.");
            return DatasetFactory.create();
        }
    }

    private static void setLogging(CommandLine cl, int logLevel) {
        Level[] levels = new Level[]{Level.OFF, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE};
        try {
            Level level = Level.toLevel(cl.getOptionValue("l"), levels[logLevel]);
            ROOT_LOGGER.setLevel(level);
        } catch (Exception ex) {
            ROOT_LOGGER.setLevel(Level.DEBUG);
        }

        String logPath = cl.getOptionValue(ARG_LOG_FILE);
        if (logPath != null) {
            try {
                ROOT_LOGGER.removeAllAppenders();
                ROOT_LOGGER.addAppender(new org.apache.log4j.RollingFileAppender(LAYOUT, logPath, false));
            } catch (IOException ex) {
                System.out.println(ex.getClass() + "occurred while initializing the log file: " + ex.getMessage());
                ex.printStackTrace();
            }
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

    private static SPARQLExtStreamManager prepareStreamManager(File dirFile, Request request, CommandLine cl) {
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
        if (cl.hasOption(ARG_BASE)) {
            String base = cl.getOptionValue(ARG_BASE);
            try {
                Files.walk(dirPath)
                        .filter((p) -> {
                            return p.toFile().isFile();
                        })
                        .forEach((p) -> {
                            String relativePath = dirPath.relativize(p).toString();
                            String url = base + relativePath.replace("\\", "/");
                            mapper.addAltEntry(url, p.toString());
                        });
            } catch (IOException ex) {
                LOG.warn("Error while computing the URIs for the files in the working directory.", ex);
            }
        }
        return sm;
    }

    private static ResultsFormat getResultsFormat(String outputLang) {
        if (outputLang == null) {
            return ResultsFormat.FMT_TEXT;
        } else {
            switch (outputLang) {
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

}
