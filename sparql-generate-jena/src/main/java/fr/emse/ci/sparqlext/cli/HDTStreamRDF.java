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
package fr.emse.ci.sparqlext.cli;

import java.io.IOException;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.FmtUtils;
import org.rdfhdt.hdt.dictionary.TempDictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.hdt.TempHDT;
import org.rdfhdt.hdt.hdt.impl.HDTImpl;
import org.rdfhdt.hdt.hdt.impl.ModeOfLoading;
import org.rdfhdt.hdt.hdt.impl.TempHDTImpl;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.TempTriples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public class HDTStreamRDF implements StreamRDF {

    private static final Logger LOG = LoggerFactory.getLogger(HDTStreamRDF.class);

    private final HDTImpl hdt;
    private final TempHDT modHdt;
    private final ProgressTimeLog listener;
    private final TempDictionary dictionary;
    private final TempTriples triples;
    long num = 0;
    long size = 0;

    public HDTStreamRDF(String base) {
        if(base==null) {
            base = "http://example.org/";
        }
        HDTOptions specs = new HDTSpecification();
        hdt = new HDTImpl(specs);
        modHdt = new TempHDTImpl(specs, base, ModeOfLoading.ONE_PASS);
        dictionary = modHdt.getDictionary();
        triples = modHdt.getTriples();
        dictionary.startProcessing();
        listener = new ProgressTimeLog();
    }

    @Override
    public void start() {
        listener.reset();
    }

    @Override
    public void triple(Triple t) {
        CharSequence subject = FmtUtils.stringForNode(t.getSubject());
        CharSequence predicate = FmtUtils.stringForNode(t.getPredicate());
        CharSequence object = FmtUtils.stringForNode(t.getObject());
        triples.insert(
                dictionary.insert(subject, TripleComponentRole.SUBJECT),
                dictionary.insert(predicate, TripleComponentRole.PREDICATE),
                dictionary.insert(object, TripleComponentRole.OBJECT)
        );
        num++;
        size += subject.length() + predicate.length() + object.length() + 4;
        listener.notifyProgressCond(t);
    }

    @Override
    public void quad(Quad quad) {
    }

    @Override
    public void base(String base) {
    }

    @Override
    public void prefix(String prefix, String iri) {
    }

    @Override
    public void finish() {
        dictionary.endProcessing();
        // Reorganize both the dictionary and the triples
        modHdt.reorganizeDictionary(listener);
        modHdt.reorganizeTriples(listener);

        hdt.loadFromModifiableHDT(modHdt, listener);
        hdt.populateHeaderStructure(modHdt.getBaseURI());

        hdt.getHeader().insert("_:statistics", HDTVocabulary.ORIGINAL_SIZE, size);
        try {
            modHdt.close();
        } catch (IOException ex) {
            LOG.debug("IOException while closing modHdt", ex);
        }
    }

    public HDT getHDT() {
        return hdt;
    }

    private class ProgressTimeLog implements ProgressListener {

        long last;

        public ProgressTimeLog() {
            reset();
        }

        public void notifyProgressCond(Triple t) {
            long now = System.currentTimeMillis();
            if (now - last > 5_000) {
                notifyProgress(num, FmtUtils.stringForTriple(t));
                reset(now);
            }
        }

        @Override
        public void notifyProgress(float level, String triple) {
            long now = System.currentTimeMillis();
            if (now - last > 5_000) {
                LOG.info("Loaded " + level + " triples. Last triple processed: " + triple);
                reset(now);
            }
        }

        private void reset() {
            reset(System.currentTimeMillis());
        }

        private void reset(long now) {
            last = now;
        }
    }
}
