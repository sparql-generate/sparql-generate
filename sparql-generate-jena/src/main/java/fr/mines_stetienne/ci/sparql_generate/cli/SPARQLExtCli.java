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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.jena.atlas.io.IndentedWriter;
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
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.sparql.util.NodeFactoryExtra;
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

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import fr.mines_stetienne.ci.sparql_generate.FileConfigurations;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExtException;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.engine.RootPlan;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.stream.DocumentNotFoundException;
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

		} catch (OutOfMemoryError ex) {
			LOG.error("Out of memory: give the JVM more memory (-Xmx), or write the output as it is generated (--stream) and read large documents in batches.");
			System.exit(1);
		} catch (Exception ex) {
			// a non-zero exit status, for scripts and continuous integration
			final DocumentNotFoundException notFound = DocumentNotFoundException.findIn(ex);
			if (notFound != null) {
				LOG.error(notFound.getMessage());
			} else {
				LOG.error(ex.getMessage(), ex);
			}
			System.exit(1);
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

		if (q.isPerformType()) {
			throw new SPARQLExtException("PERFORM queries are not supported: the engine parses them but cannot execute them.");
		}

		if (cl != null) {
			replaceSourcesIfRequested(cl, q);
		}

		RootPlan plan;
		try {
			plan = PlanFactory.create(q);
		} catch (Exception ex) {
			throw new SPARQLExtException("Error while creating the plan for the query.", ex);
		}

		final Dataset ds = getDataset(workingDir, rq);

		if (!q.isGenerateType() && rq.hdt) {
			throw new SPARQLExtException("Option HDT is only for queries of type GENERATE");
		}
		if (q.isTemplateType() && rq.outputFormat != null) {
			throw new SPARQLExtException("Option outputFormat is only for queries of type GENERATE or SELECT");
		}

		// prepare context
		final ContextUtils.Builder contextBuilder = ContextUtils.build().setBase(rq.base).setPrefixMapping(q)
				.setInputDataset(ds).setStreamManager(sm).setDebugTemplate(rq.debugTemplate);

		// prepare initial binding
		List<Binding> bindings = getBinding(cl, q.getPrefixMapping());

		if (q.isTemplateType()) {
			if (rq.output == null) {
				try (IndentedWriter output = IndentedWriter.stdout) {
					Context context = contextBuilder.setTemplateOutput(output).build();
					plan.execTemplateStream(bindings, context);
					output.flush();
				} catch (Exception ex) {
					throw new SPARQLExtException("Error while executing the plan.", ex);
				}
			} else {
				try (IndentedWriter output = new IndentedWriter(new FileOutputStream(rq.output, rq.outputAppend));) {
					Context context = contextBuilder.setTemplateOutput(output).build();
					plan.execTemplateStream(bindings, context);
					output.flush();
				} catch (Exception ex) {
					throw new SPARQLExtException("Error while executing the plan.", ex);
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
			throw new SPARQLExtException("Error: unsupported query type");
		}
	}

	private static List<Binding> getBinding(CommandLine cl, PrefixMapping pm) {
		final BindingBuilder bindingBuilder = Binding.builder();
		if(cl == null || !cl.hasOption(CMDConfigurations.ARG_BIND_LONG)) {
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
			if(LOG.isTraceEnabled()) {
				LOG.trace(String.format("With binding %s -> %s", param, value));
			}
			try {
				// an RDF term: IRI, prefixed name of the query, literal, number
				Node node = NodeFactoryExtra.parseNode(value, PrefixMapFactory.create(pm));
				bindingBuilder.add(var, node);
			} catch (RiotException ex) {
				throw new SPARQLExtException(String.format("Invalid RDF term for parameter %s: %s", param, value), ex);
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
			throw new SPARQLExtException("Output needs to be set with the option HDT.");
		}
		final String baseURI = plan.getQuery().getBaseURI();
		HDTStreamRDF hdtStreamRDF;
		try {
			hdtStreamRDF = new HDTStreamRDF(baseURI);
		} catch (IOException ex) {
			throw new SPARQLExtException("Error while opening the HDT stream.", ex);
		}
		HDT hdt = hdtStreamRDF.getHDT();
		Context context = builder.setGenerateOutput(hdtStreamRDF).build();
		plan.execGenerateStream(bindings, context);
		try (OutputStream out = new FileOutputStream(request.output)) {
			// Save generated HDT to a file
			hdt.saveToHDT(out, null);
		} catch (IOException ex) {
			throw new SPARQLExtException("Error while opening the output file.", ex);
		}
		try {
			hdt.close();
		} catch (IOException ex) {
			throw new SPARQLExtException("Error while closing the HDT.", ex);
		}
	}

	/**
	 * The output file, or the standard output, which is not closed.
	 */
	private static OutputStream openOutput(CliRequest request) {
		if (request.output == null) {
			return CloseShieldOutputStream.wrap(System.out);
		}
		try {
			return new BufferedOutputStream(new FileOutputStream(request.output, request.outputAppend), 1 << 16);
		} catch (IOException ex) {
			throw new SPARQLExtException("Error while opening the output file.", ex);
		}
	}

	/**
	 * With --stream, a GENERATE query writes N-Triples if asked to, and
	 * Turtle, one triple per line, otherwise.
	 */
	private static boolean isNTriples(CliRequest request) {
		if (request.outputFormat == null) {
			return false;
		}
		Lang lang = RDFLanguages.nameToLang(request.outputFormat);
		if (Lang.NTRIPLES.equals(lang) || Lang.NQUADS.equals(lang)) {
			return true;
		}
		if (!Lang.TURTLE.equals(lang) && !Lang.N3.equals(lang)) {
			LOG.warn("With --stream, the output of a GENERATE query is Turtle or N-Triples, not " + request.outputFormat);
		}
		return false;
	}

	private static void execGenerateStream(List<Binding> bindings, RootPlan plan, ContextUtils.Builder builder, CliRequest request) {
		final PrefixMapping pm = plan.getQuery().getPrefixMapping();
		try (OutputStream out = openOutput(request)) {
			final StreamRDF stream = isNTriples(request) ? StreamRDFWriter.getWriterStream(out, RDFFormat.NTRIPLES)
					: new ConsoleStreamRDF(out, pm);
			// the plan calls start() and finish(), which flushes the stream
			Context context = builder.setGenerateOutput(stream).build();
			plan.execGenerateStream(bindings, context);
		} catch (IOException ex) {
			throw new SPARQLExtException("Error while writing the output file.", ex);
		}
	}

	private static void execGenerate(List<Binding> bindings, RootPlan plan, Context context, CliRequest request) {
		if (request.outputFormat == null) {
			request.outputFormat = RDFLanguages.strLangTurtle;
		}
		Lang lang = RDFLanguages.nameToLang(request.outputFormat);
		final Model model;
		try {
			model = plan.execGenerate(bindings, context);
		} catch (Exception ex) {
			throw new SPARQLExtException("Error while executing the plan.", ex);
		}
		try (OutputStream out = openOutput(request)) {
			model.write(out, lang.getLabel());
		} catch (IOException ex) {
			throw new SPARQLExtException("Error while writing the output file.", ex);
		}
	}

	private static void writeResultSet(OutputStream out, ResultSet result, ResultsFormat format) {
		if (format.equals(ResultsFormat.FMT_TEXT)) {
			ResultSetFormatter.out(out, result);
		} else {
			ResultSetFormatter.output(out, result, format);
		}
	}

	private static void execSelect(List<Binding> bindings, RootPlan plan, Context context, CliRequest request) {
		final ResultSet result;
		try {
			result = plan.execSelect(bindings, context);
		} catch (Exception ex) {
			throw new SPARQLExtException("Error while executing the plan.", ex);
		}
		final ResultsFormat format = getResultsFormat(request);
		try (OutputStream out = openOutput(request)) {
			writeResultSet(out, result, format);
		} catch (IOException ex) {
			throw new SPARQLExtException("Error while writing the output file.", ex);
		}
	}

	private static void execSelectStream(List<Binding> bindings, RootPlan plan, ContextUtils.Builder builder, CliRequest request) {
		final ResultsFormat format = getResultsFormat(request);
		try (OutputStream out = openOutput(request)) {
			Context context = builder.setSelectOutput((result) -> {
				writeResultSet(out, result, format);
			}).build();
			plan.execSelectStream(bindings, context);
		} catch (IOException ex) {
			throw new SPARQLExtException("Error while writing the output file.", ex);
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
				String sourceURI = elementSource.getSource().toString(query.getPrefixMapping());

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
		sm.setFailOnMissingDocument(request.failOnMissing);

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
		// -o and -f are relative to the current directory; output and logFile of
		// sparql-generate-conf.json are relative to the directory of the queryset
		request.output = resolve(dirFile, cl.getOptionValue(ARG_OUTPUT), request.output);
		request.outputAppend = cl.hasOption(ARG_OUTPUT_APPEND);
		request.outputFormat = cl.getOptionValue(ARG_OUTPUT_FORMAT, request.outputFormat);
		request.stream = cl.hasOption(ARG_STREAM) || request.stream;
		request.hdt = cl.hasOption(ARG_HDT) || request.hdt;
		request.debugTemplate = cl.hasOption(ARG_DEBUG_TEMPLATE) || request.debugTemplate;
		request.failOnMissing = cl.hasOption(CMDConfigurations.ARG_FAIL_ON_MISSING_LONG) || request.failOnMissing;
		request.logFile = resolve(dirFile, cl.getOptionValue(ARG_LOG_FILE), request.logFile);
		try {
			request.logLevelObject = Level.toLevel(cl.getOptionValue(ARG_LOG_LEVEL), LOG_LEVELS[request.getLogLevel()]);
		} catch (Exception ex) {
			request.logLevelObject = Level.DEBUG;
		}
		return request;
	}

	/**
	 * Resolves a path given on the command line against the current directory,
	 * or else a path of the configuration file against the directory.
	 */
	private static String resolve(File dirFile, String commandLinePath, String configurationPath) {
		if (commandLinePath != null) {
			return new File(commandLinePath).getAbsolutePath();
		}
		if (configurationPath == null) {
			return null;
		}
		File file = new File(configurationPath);
		return (file.isAbsolute() ? file : new File(dirFile, configurationPath)).getAbsolutePath();
	}

	private static SPARQLExtQuery getQueryOrDie(CliRequest request, CommandLine cl, SPARQLExtStreamManager sm) {
		String query;
		try {
			query = IOUtils.toString(sm.open(new LookUpRequest(request.query, SPARQLExt.MEDIA_TYPE)),
					StandardCharsets.UTF_8);
		} catch (IOException | NullPointerException | DocumentNotFoundException ex) {
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
				System.exit(1);
				return null;
			} else {
				throw new RuntimeException("Error while parsing the query to be executed.", ex);
			}
		}
	}

}
