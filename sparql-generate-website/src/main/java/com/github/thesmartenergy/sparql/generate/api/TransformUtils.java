/*
 * Copyright 2017 École des Mines de Saint-Étienne.
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
package com.github.thesmartenergy.sparql.generate.api;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.locator.LocatorStringMap;
import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 *
 * @author maxime.lefrancois
 */
public class TransformUtils {
    
    private Appender appender;
    private final String appenderName;
    
    private final Logger log;

    public TransformUtils(String appenderName, Logger log) {
        this.appenderName = appenderName;
        this.log = log;
    }

    static {
        com.jayway.jsonpath.Configuration.setDefaults(new com.jayway.jsonpath.Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider
                    = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }
        });
    }
    
    void setStreamManager(String documentset) throws Exception {
        LocatorStringMap locator = new LocatorStringMap();
        List<Object> documents = (new Gson()).fromJson(documentset, List.class);
        if (documents != null) {
            for (int i = 0; i < documents.size(); i++) {
                StringMap document = (StringMap) documents.get(i);
                String uri = (String) document.get("uri");
                String doc = (String) document.get("document");
                locator.put(uri, doc);
                log.trace("with document: " + uri + " = " + doc);
            }
        }
        SPARQLGenerate.resetStreamManager(locator);
    }
    
    StringWriter setLogger(String logLevel) {
        final LoggerContext context = LoggerContext.getContext(true);
        final org.apache.logging.log4j.core.config.Configuration config = context.getConfiguration();
        final PatternLayout layout = PatternLayout.newBuilder().withPattern("%d{HH:mm:ss,SSS} %-5p %t:%C:%L%n%m%n").build();

        StringWriter sw = new StringWriter();
        appender = WriterAppender.createAppender(layout, null, sw, appenderName, false, true);
        appender.start();
        config.addAppender(appender);
        addAppender(logLevel);

        return sw;
    }

    void addAppender(String logLevel) {
        Level level = Level.getLevel(logLevel);
        if (level == null) {
            level = Level.TRACE;
        }
        final LoggerContext context = LoggerContext.getContext(true);
        final org.apache.logging.log4j.core.config.Configuration config = context.getConfiguration();
        for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
            loggerConfig.addAppender(appender, level, null);
        }
    }

    void removeAppender() {
        final LoggerContext context = LoggerContext.getContext(true);
        final org.apache.logging.log4j.core.config.Configuration config = context.getConfiguration();
        for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
            loggerConfig.removeAppender(appenderName);
        }
    }

    Model setInputModel(String defaultGraph) {
        Model model = ModelFactory.createDefaultModel();
        try {
            InputStream in = IOUtils.toInputStream(defaultGraph, "UTF-8");
            model.read(in, "TTL");
        } catch (Exception ex) {
            log.warn("Error while reading the default graph. Will work with an empty one", ex);
        }
        return model;
    }
}
