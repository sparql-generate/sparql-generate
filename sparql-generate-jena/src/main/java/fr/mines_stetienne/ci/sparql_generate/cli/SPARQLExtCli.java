/*
 * Copyright 2020 MINES Saint-Étienne
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
package fr.mines_stetienne.ci.sparql_generate.cli;

import static fr.mines_stetienne.ci.sparql_generate.cli.CMDConfigurations.ARG_BASE;
import static fr.mines_stetienne.ci.sparql_generate.cli.CMDConfigurations.ARG_DEBUG_TEMPLATE;
import static fr.mines_stetienne.ci.sparql_generate.cli.CMDConfigurations.ARG_DIRECTORY;
import static fr.mines_stetienne.ci.sparql_generate.cli.CMDConfigurations.ARG_DIRECTORY_DEFAULT;
import static fr.mines_stetienne.ci.sparql_generate.cli.CMDConfigurations.ARG_HDT;
import static fr.mines_stetienne.ci.sparql_generate.cli.CMDConfigurations.ARG_HELP;
import static fr.mines_stetienne.ci.sparql_generate.cli.CMDConfigurations.ARG_LOG_FILE;
import static fr.mines_stetienne.ci.sparql_generate.cli.CMDConfigurations.ARG_LOG_LEVEL;
import static fr.mines_stetienne.ci.sparql_generate.cli.CMDConfigurations.ARG_OUTPUT;
import static fr.mines_stetienne.ci.sparql_generate.cli.CMDConfigurations.ARG_OUTPUT_APPEND;
import static fr.mines_stetienne.ci.sparql_generate.cli.CMDConfigurations.ARG_OUTPUT_FORMAT;
import static fr.mines_stetienne.ci.sparql_generate.cli.CMDConfigurations.ARG_QUERY;
import static fr.mines_stetienne.ci.sparql_generate.cli.CMDConfigurations.ARG_SOURCE_LONG;
import static fr.mines_stetienne.ci.sparql_generate.cli.CMDConfigurations.ARG_STREAM;
import static fr.mines_stetienne.ci.sparql_generate.cli.CMDConfigurations.ARG_SYNTAX;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.lang.extra.javacc.TurtleJavacc;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.util.Context;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.rdfhdt.hdt.hdt.HDT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import fr.mines_stetienne.ci.sparql_generate.FileConfigurations;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExtException;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.engine.RootPlan;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.stream.LocationMapperAccept;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorFileAccept;
import fr.mines_stetienne.ci.sparql_generate.stream.LookUpRequest;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.syntax.ElementSource;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * Main class for the CLI interface to SPARQL-Generate; see {@link HDTStreamRDF}
 * for the options.
 * 
 * @author Maxime Lefrançois
 */
public class SPARQLExtCli {

	private static final Logger LOG = LoggerFactory.getLogger(SPARQLExtCli.class);

	private static final Layout LAYOUT = new PatternLayout("%d{mm:ss,SSS} %t %-5p %c:%L - %m%n");
	private static final org.apache.log4j.Logger ROOT_LOGGER = org.apache.log4j.Logger.getRootLogger();
	private static final String CONF_FILE = "sparql-generate-conf.json";
	private static final Level[] LOG_LEVELS = new Level[] { Level.OFF, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG,
			Level.TRACE };

	static {
		SPARQLExt.init();
	}

	/**
	 * see {@link HDTStreamRDF} for the options.
	 * 
	 * @param args
	 * @throws ParseException
	 */
	public static void main(String[] args) throws ParseException {
		Instant start = Instant.now();
		CommandLine cl = CMDConfigurations.parseArguments(args);

		if (cl.getOptions().length == 0 || cl.hasOption(ARG_HELP)) {
			CMDConfigurations.displayHelp();
			return;
		}

		String dirName = cl.getOptionValue(ARG_DIRECTORY, ARG_DIRECTORY_DEFAULT);
		if (dirName.equals("")) {
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
						if (event.getLevel().isGreaterOrEqual(Level.INFO)) {
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
		try {

//			exec(dirFile, r);
			exec(dirFile, r, cl);

			long millis = Duration.between(start, Instant.now()).toMillis();
			int min = (int) (millis / 60000);
			int sec = (int) (millis % 60000 / 1000);
			int milli = (int) (millis % 1000);
			LOG.info("Program finished in " + String.format("%d min, %d.%d sec", min, sec, milli));

		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Executes SPARQL-Generate in a working directory, with {@link CliRequest}
	 * parameters, and an optional CommandLink that contains URL-URL binding for the
	 * sources in the main query
	 * 
	 * @param workingDir
	 * @param rq
	 * @param cl
	 * @throws SPARQLExtException
	 */
	public static void exec(File workingDir, CliRequest rq, CommandLine cl) throws SPARQLExtException {
		Objects.nonNull(workingDir);
		Objects.nonNull(rq);

		final SPARQLExtStreamManager sm = prepareStreamManager(workingDir, rq);
		final SPARQLExtQuery q = getQueryOrDie(rq, cl, sm);

		if(cl != null && cl.hasOption(CMDConfigurations.ARG_SYNTAX_LONG)) {
			return;
		}
		
		if (cl != null) {
			replaceSourcesIfRequested(cl, q);
		}

		RootPlan plan;
		try {
			plan = PlanFactory.create(q);
		} catch (Exception ex) {
			LOG.error("Error while creating the plan for the query.", ex);
			return;
		}

		final Dataset ds = getDataset(workingDir, rq);

		if (!q.isGenerateType() && rq.hdt) {
			LOG.error("Option HDT is only for queries of type GENERATE");
			return;
		}
		if (q.isTemplateType() && rq.outputFormat != null) {
			LOG.error("Option outputFormat is only for queries of type GENERATE or SELECT");
			return;
		}

		// prepare context
		final ContextUtils.Builder contextBuilder = ContextUtils.build().setBase(rq.base).setPrefixMapping(q)
				.setInputDataset(ds).setStreamManager(sm).setDebugTemplate(rq.debugTemplate);
		
		// prepare initial binding
		List<Binding> bindings = getBinding(cl);

		if (q.isTemplateType()) {
			if (rq.output == null) {
				try (IndentedWriter output = IndentedWriter.stdout) {
					Context context = contextBuilder.setTemplateOutput(output).build();
					plan.execTemplateStream(bindings, context);
					output.flush();
				} catch (Exception ex) {
					LOG.error("Error while executing the plan.", ex);
				}
			} else {
				try (IndentedWriter output = new IndentedWriter(new FileOutputStream(rq.output, rq.outputAppend));) {
					Context context = contextBuilder.setTemplateOutput(output).build();
					plan.execTemplateStream(bindings, context);
					output.flush();
				} catch (Exception ex) {
					LOG.error("Error while executing the plan.", ex);
				}
			}
		} else if (q.isGenerateType() && !rq.stream && !rq.hdt) {
			Context context = contextBuilder.build();
			execGenerate(bindings, plan, context, rq);
		} else if (q.isGenerateType() && rq.hdt) {
			execGenerateHDT(bindings, plan, contextBuilder, rq);
		} else if (q.isGenerateType()) {
			execGenerateStream(bindings, plan, contextBuilder, rq);
		} else if (q.isSelectType() && !rq.stream) {
			Context context = contextBuilder.build();
			execSelect(bindings, plan, context, rq);
		} else if (q.isSelectType()) {
			execSelectStream(bindings, plan, contextBuilder, rq);
		} else {
			LOG.error("Error: unsupported query type");
		}
	}

	private static List<Binding> getBinding(CommandLine cl) {
		final BindingBuilder bindingBuilder = Binding.builder();
		if(!cl.hasOption(CMDConfigurations.ARG_BIND_LONG)) {
			ArrayList<Binding> bindings = new ArrayList<Binding>();
			bindings.add(bindingBuilder.build());
			return bindings;
		}
		Properties properties = cl.getOptionProperties(CMDConfigurations.ARG_BIND_LONG);
		if(properties.isEmpty()) {
			ArrayList<Binding> bindings = new ArrayList<Binding>();
			bindings.add(bindingBuilder.build());
			return bindings;
		}
		for(Object param : properties.keySet()) {
			Var var = Var.alloc((String) param);
			String value = properties.getProperty((String) param);
			StringReader valueReader = new StringReader(value);
			TurtleJavacc valueParser = new TurtleJavacc(valueReader);
			if(LOG.isTraceEnabled()) {
				LOG.trace(String.format("With binding %s -> %s", param, value));
			}
			try {
				Node node = valueParser.Literal();
				bindingBuilder.add(var, node);
			} catch (org.apache.jena.riot.lang.extra.javacc.ParseException ex) {
				LOG.error(String.format("Error while parsing value for parameter %s", param), ex);
				throw new SPARQLExtException(ex);
			}
		}
		return Lists.newArrayList(bindingBuilder.build());
	}

	/**
	 * Executes SPARQL-Generate in a working directory, with {@link CliRequest}
	 * parameters
	 * 
	 * @param workingDir
	 * @param rq
	 * @throws SPARQLExtException
	 */
	public static void exec(File workingDir, CliRequest rq) throws SPARQLExtException {
		exec(workingDir, rq, null);
	}

	private static void execGenerateHDT(List<Binding> bindings, RootPlan plan, ContextUtils.Builder builder, CliRequest request) {
		if (request.output == null) {
			LOG.error("Output needs to be set with the option HDT.");
		}
		final String baseURI = plan.getQuery().getBaseURI();
		HDTStreamRDF hdtStreamRDF = new HDTStreamRDF(baseURI);
		HDT hdt = hdtStreamRDF.getHDT();
		Context context = builder.setGenerateOutput(hdtStreamRDF).build();
		plan.execGenerateStream(bindings, context);
		try (OutputStream out = new FileOutputStream(request.output)) {
			// Save generated HDT to a file
			hdt.saveToHDT(out, null);
		} catch (IOException ex) {
			LOG.error("Error while opening the output file.", ex);
		}
		try {
			hdt.close();
		} catch (IOException ex) {
			LOG.error("Error while closing the HDT.", ex);
		}
	}

	private static void execGenerateStream(List<Binding> bindings, RootPlan plan, ContextUtils.Builder builder, CliRequest request) {
		final PrefixMapping pm = plan.getQuery().getPrefixMapping();
		final ConsoleStreamRDF consoleStreamRDF;
		if (request.output == null) {
			consoleStreamRDF = new ConsoleStreamRDF(System.out, pm);
		} else {
			try {
				consoleStreamRDF = new ConsoleStreamRDF(
						new PrintStream(new FileOutputStream(request.output, request.outputAppend)), pm);
			} catch (IOException ex) {
				LOG.error("Error while opening the output file.", ex);
				return;
			}
		}
		Context context = builder.setGenerateOutput(consoleStreamRDF).build();
		plan.execGenerateStream(bindings, context);
	}

	private static void execGenerate(List<Binding> bindings, RootPlan plan, Context context, CliRequest request) {
		if (request.outputFormat == null) {
			request.outputFormat = RDFLanguages.strLangTurtle;
		}
		Lang lang = RDFLanguages.nameToLang(request.outputFormat);
		try {
			Model model = plan.execGenerate(bindings, context);
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

	private static void execSelect(List<Binding> bindings, RootPlan plan, Context context, CliRequest request) {
		try {
			ResultSet result = plan.execSelect(bindings, context);
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

	private static void execSelectStream(List<Binding> bindings, RootPlan plan, ContextUtils.Builder builder, CliRequest request) {
		OutputStream out;
		try {
			if (request.output == null) {
				out = System.out;
			} else {
				out = new FileOutputStream(request.output, request.outputAppend);
			}
			ResultsFormat format = getResultsFormat(request);
			Context context = builder.setSelectOutput((result) -> {
				LOG.info("entering accept");
				if (format.equals(ResultsFormat.FMT_TEXT)) {
					ResultSetFormatter.out(out, result);
				} else {
					ResultSetFormatter.output(out, result, format);
				}
				LOG.info("exiting accept");
			}).build();
			plan.execSelectStream(bindings, context);
		} catch (IOException ex) {
			LOG.error("Error while opening the output file.", ex);
		}
	}

	private static Dataset getDataset(File dir, FileConfigurations request) {
		try {
			return request.loadDataset(dir);
		} catch (Exception ex) {
			LOG.warn("Error while loading the dataset, no dataset will be used.");
			return DatasetFactory.create();
		}
	}

	private static void replaceSourcesIfRequested(CommandLine cli, SPARQLExtQuery query) {
		final Properties replacementSources = cli.getOptionProperties(ARG_SOURCE_LONG);

		List<Element> updatedSources = query.getBindingClauses().stream().map(element -> {
			if (element instanceof ElementSource) {
				ElementSource elementSource = (ElementSource) element;
				String sourceURI = elementSource.getSource().toString(query.getPrefixMapping(), false);

				if (replacementSources.containsKey(sourceURI)) {
					Node replacementSource = NodeFactory.createURI(replacementSources.getProperty(sourceURI));

					LOG.info("Replaced source <{}> with <{}>.", sourceURI, replacementSource);

					return new ElementSource(replacementSource, elementSource.getAccept(), elementSource.getVar());
				}
			}

			return element;
		}).collect(Collectors.toList());

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
		if (request.namedgraphs != null) {
			request.namedgraphs.forEach((doc) -> {
				LookUpRequest req = new LookUpRequest(doc.uri, Lang.TTL.getContentType().getContentTypeStr());
				LookUpRequest alt = new LookUpRequest(doc.path);
				mapper.addAltEntry(req, alt);
			});
		}
		if (request.base != null) {
			try {
				Files.walk(dirPath).filter((p) -> {
					return p.toFile().isFile();
				}).forEach((p) -> {
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
			String conf = IOUtils.toString(new FileInputStream(new File(dirFile, CONF_FILE)), StandardCharsets.UTF_8);
			request = (new Gson()).fromJson(conf, CliRequest.class);
		} catch (IOException ex) {
			request = new CliRequest();
		} catch (JsonSyntaxException ex) {
			LOG.warn("JSON Syntax exception while loading the location mapping model for the queryset.", ex);
			request = new CliRequest();
		}
		request.query = new File(dirFile, cl.getOptionValue(ARG_QUERY, request.query)).getAbsolutePath();
		request.base = cl.getOptionValue(ARG_BASE, request.base);
		request.output = cl.getOptionValue(ARG_OUTPUT, request.output);
		if (request.output != null) {
			request.output = new File(dirFile, cl.getOptionValue(ARG_OUTPUT, request.output)).getAbsolutePath();
		}
		request.outputAppend = cl.hasOption(ARG_OUTPUT_APPEND);
		request.outputFormat = cl.getOptionValue(ARG_OUTPUT_FORMAT, request.outputFormat);
		request.stream = cl.hasOption(ARG_STREAM) || request.stream;
		request.hdt = cl.hasOption(ARG_HDT) || request.hdt;
		request.debugTemplate = cl.hasOption(ARG_DEBUG_TEMPLATE) || request.debugTemplate;
		if (request.logFile != null) {
			request.logFile = new File(dirFile, cl.getOptionValue(ARG_LOG_FILE, request.logFile)).getAbsolutePath();
		}
		try {
			request.logLevelObject = Level.toLevel(cl.getOptionValue(ARG_LOG_LEVEL), LOG_LEVELS[request.loglevel]);
		} catch (Exception ex) {
			request.logLevelObject = Level.DEBUG;
		}
		return request;
	}

	private static SPARQLExtQuery getQueryOrDie(CliRequest request, CommandLine cl, SPARQLExtStreamManager sm) {
		String query;
		try {
			query = IOUtils.toString(sm.open(new LookUpRequest(request.query, SPARQLExt.MEDIA_TYPE)),
					StandardCharsets.UTF_8);
		} catch (IOException | NullPointerException ex) {
			throw new RuntimeException(
					String.format("No file named %s was found in the directory that contains the query to be executed.",
							request.query),
					ex);
		}

		try {
			SPARQLExtQuery q = (SPARQLExtQuery) QueryFactory.create(query, request.base, SPARQLExt.SYNTAX);
			if (!q.explicitlySetBaseURI()) {
				q.setBaseURI(request.base);
			}
			return q;
		} catch (Exception ex) {
			if(cl != null && cl.hasOption(CMDConfigurations.ARG_SYNTAX_LONG)) {
				System.out.println(ex.getMessage());
				System.exit(0);
				return null;
			} else {
				throw new RuntimeException("Error while parsing the query to be executed.", ex);
			}
		}
	}

}
