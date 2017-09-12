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
package com.github.thesmartenergy.sparql.generate.jena.engine;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.stream.LocatorFile;
import org.apache.jena.riot.system.stream.StreamManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author maxime.lefrancois
 */
public class TestLiteral extends TestBase {
    
    static Logger LOG =LogManager.getLogger(TestLiteral.class);
    static URL examplePath;
    static File exampleDir;

    @BeforeClass
    static public void setUpClass() throws Exception {
        LOG.debug("set up");
        String dir = "testLiteral";
        dir = Character.toLowerCase(dir.charAt(0))
                + (dir.length() > 1 ? dir.substring(1) : "");
        examplePath = TestLiteral.class.getResource("/" + dir);

        exampleDir = new File(examplePath.toURI());

        // read location-mapping
        URI confUri = exampleDir.toURI().resolve("configuration.ttl");
        Model conf = RDFDataMgr.loadModel(confUri.toString());

        // initialize stream manager
        StreamManager sm = SPARQLGenerate.resetStreamManager(conf);
        sm.addLocator(new LocatorFile(exampleDir.toURI().getPath()));
    }

    @Test
    public void testQuerySerialization() throws Exception {
        String qstring = IOUtils.toString(StreamManager.get().open("query.rqg"), "UTF-8");
        SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(qstring, SPARQLGenerate.SYNTAX);

        // serialize query 
        LOG.debug("serialized query " + q.toString());

        SPARQLGenerateQuery q2 = (SPARQLGenerateQuery) QueryFactory.create(q.toString(), SPARQLGenerate.SYNTAX);
        LOG.debug("loaded again " + q2);

        SPARQLGenerateQuery q3 = q.normalize();
                
        LOG.debug("normalizeed " + q3);
    }

}
