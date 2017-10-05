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

/**
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.engine.PlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import com.github.thesmartenergy.sparql.generate.jena.stream.LocatorFileAccept;
import com.github.thesmartenergy.sparql.generate.jena.stream.LookUpRequest;
import com.github.thesmartenergy.sparql.generate.jena.stream.SPARQLGenerateStreamManager;
import com.github.thesmartenergy.sparql.generate.jena.utils.SPARQLGenerateUtils;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;

public class CMDGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(CMDGenerator.class);

    private static final Layout layout = new PatternLayout("%5p [%t] (%F:%L) - %m%n");
    private static final Appender consoleAppender = new ConsoleAppender(layout);
    private static final org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();

    public static void main(String[] args) throws ParseException {

        CommandLine cl = CMDConfigurations.parseArguments(args);

        if (cl.getOptions().length == 0) {
            CMDConfigurations.displayHelp();
            return;
        }

        setLogging(cl);

        String dirPath = cl.getOptionValue("d", ".");
        File dir = new File(dirPath);

        SPARQLGenerate.init();

        // read location-mapping
        Model conf = ModelFactory.createDefaultModel();
        try {
            conf.add(RDFDataMgr.loadModel(new File(dir, "queryset/configuration.ttl").toString(), Lang.TTL));
        } catch (Exception ex) {
            LOG.error("Error while loading the location mapping model for the queryset.", ex);
            return;
        }

        try {
            conf.add(RDFDataMgr.loadModel(new File(dir, "documentset/configuration.ttl").toString(), Lang.TTL));
        } catch (Exception ex) {
            LOG.error("Error while loading the location mapping model for the documentset.", ex);
            return;
        }

        // initialize stream manager
        SPARQLGenerateStreamManager sm = SPARQLGenerateStreamManager.makeStreamManager(new LocatorFileAccept(dir.toURI().getPath()));
        sm.setLocationMapper(conf);
        SPARQLGenerate.setStreamManager(sm);

        String queryPath = cl.getOptionValue("q", "query.rqg");
        String query;
        try {
            query = IOUtils.toString(SPARQLGenerate.getStreamManager().open(new LookUpRequest(queryPath, SPARQLGenerate.MEDIA_TYPE)), "UTF-8");
        } catch (IOException ex) {
            LOG.error("There should be a file named query.rqg in the directory that contains the query to be executed.", ex);
            return;
        }

        SPARQLGenerateQuery q;
        try {
            q = (SPARQLGenerateQuery) QueryFactory.create(query, SPARQLGenerate.SYNTAX);
        } catch (Exception ex) {
            LOG.error("Error while parsing the query to be executed.", ex);
            return;
        }

        RootPlan plan;
        try {
            plan = PlanFactory.create(q);
        } catch (Exception ex) {
            LOG.error("Error while creating the plan for the query.", ex);
            return;
        }

        Dataset ds;
        try {
            ds = SPARQLGenerateUtils.loadDataset(dir);
        } catch (Exception ex) {
            LOG.error("Error while loading the dataset.", ex);
            return;
        }
        
        String output = cl.getOptionValue("o");
        StreamRDF outputRDF;
        if(output == null) {
            outputRDF = new ConsoleStreamRDF(System.out, q.getPrefixMapping());
        } else {
            try {
                outputRDF = new ConsoleStreamRDF(new PrintStream(new File(output)), q.getPrefixMapping());
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
    }
    
    private static class ConsoleStreamRDF implements StreamRDF {

        private final PrefixMapping pm;
        private final SerializationContext context;

        private PrintStream out;
        
        public ConsoleStreamRDF(PrintStream out, PrefixMapping pm) {
            this.out = out;
            this.pm = pm;
            context = new SerializationContext(pm);
        }

        @Override
        public void start() {
            pm.getNsPrefixMap().forEach((prefix,uri)->{
                out.append("@Prefix ").append(prefix).append(": <").append(uri).append("> .\n");
            });
        }

        @Override
        public void base(String string) {
            out.append("@base <").append(string).append(">\n");
        }

        @Override
        public void prefix(String prefix, String uri) {
            out.append("@Prefix ").append(prefix).append(": <").append(uri).append("> .\n");
        }

        @Override
        public void triple(Triple triple) {
            out.append(FmtUtils.stringForTriple(triple, context)).append(" .\n");
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
        Level level = Level.toLevel(cl.getOptionValue("l"), Level.TRACE);
        rootLogger.setLevel(level);

        String logPath = cl.getOptionValue("f");
        if (logPath == null) {
            rootLogger.addAppender(consoleAppender);
        } else {
            try {
                rootLogger.addAppender(new org.apache.log4j.RollingFileAppender(layout, logPath));
            } catch (IOException ex) {
                System.out.println(ex.getClass() + "occurred while initializing the log file: " + ex.getMessage());
                ex.printStackTrace();
                return;
            }
        }
    }
}
