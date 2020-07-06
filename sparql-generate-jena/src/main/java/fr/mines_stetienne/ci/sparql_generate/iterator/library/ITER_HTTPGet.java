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
package fr.mines_stetienne.ci.sparql_generate.iterator.library;

import fr.mines_stetienne.ci.sparql_generate.utils.LogUtils;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorStreamFunctionBase;
import fr.mines_stetienne.ci.sparql_generate.stream.LookUpRequest;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.riot.SysRIOT;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/ITER_HTTPGet">iter:HTTPGet</a>
 * binds the responses of regular GET operations to a HTTP(s) URL.
 *
 * <p>
 * See
 * <a href="https://w3id.org/sparql-generate/playground.html#ex=example/generate/Example-HTTPGet">Live
 * example</a></p>
 * 
 * <ul>
 * <li>Param 1: (a URI or String) the Web URI where regular GET operations are
 * operated;</li>
 * <li>Param 2: (a positive Integer) the number of seconds between successive
 * calls to the Web API;</li>
 * <li>Param 3 (optional): the total number of calls to make (a positive
 * Integer). If not provided, the iterator never ends.</li>
 * </ul>
 * <p>
 * <b>Example: </b><br>
 * <p>
 * The clause</p>
 * <code>ITERATOR iter:HTTPGet(&lt;https://example.org/room1/temperature>,60) AS ?temperature</code>
 * <p>
 * will fetch the temperature of room 1 every 60 seconds, indefinetely.
 *
 * @author El-Mehdi Khalfi <el-mehdi.khalfi at emse.fr>
 * @since 2018-09-27
 */
public class ITER_HTTPGet extends IteratorStreamFunctionBase {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ITER_HTTPGet.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLExt.ITER + "HTTPGet";

    @Override
    public void checkBuild(ExprList args) {
        if (args.size() < 2 || args.size() > 3) {
            throw new ExprEvalException("Expecting two or three arguments, got: " + args.size());
        }
    }

    @Override
    public void exec(
            final List<NodeValue> args, 
            final Consumer<List<List<NodeValue>>> listNodeValues) {
        if (!args.get(0).isString() && !args.get(0).isIRI()) {
            throw new ExprEvalException("First argument must be a string or a URI, got: " + args.get(0));
        }
        final String url_s = args.get(0).isString() ? args.get(0).asString() : args.get(0).asNode().getURI();
        final LookUpRequest req = new LookUpRequest(url_s);

        if (!args.get(1).isInteger() || args.get(1).getInteger().intValue() <= 0) {
            throw new ExprEvalException("Second argument must be an integer, got: " + args.get(1));
        }
        long recurrenceValueNano = (args.get(1).getInteger().longValue() * 1_000_000_000);

        if (args.size() == 3 && (!args.get(2).isInteger() || args.get(2).getInteger().intValue() <= 0)) {
            throw new ExprEvalException("Third argument must be a positive integer, got: " + args.get(2));
        }
        int times = args.size() == 3 ? args.get(2).getInteger().intValue() : Integer.MAX_VALUE;

        final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) getContext().get(SysRIOT.sysStreamManager);
        for (int i = 0; i < times; i++) {
            long start = System.nanoTime();
            LOG.info("Call HTTPGet #" + i + " to " + url_s);
            String message;
            final TypedInputStream tin = sm.open(req);
            if(tin != null) {
                tin.getMediaType().toHeaderString();
                try (InputStream in = tin.getInputStream()) {
                    message = IOUtils.toString(in, StandardCharsets.UTF_8);
                    String datatypeUri = "http://www.iana.org/assignments/media-types/application" + tin.getMediaType().toHeaderString();
                    RDFDatatype dt = TypeMapper.getInstance().getSafeTypeByName(datatypeUri);
                    NodeValue outNode = new NodeValueNode(NodeFactory.createLiteral(message, dt));

                    if (LOG.isDebugEnabled()) {
                        String compressed = LogUtils.compress(message);
                        LOG.debug("Message retrieved: \"\"\"" + compressed + "\"\"\"^^<" + datatypeUri + ">");
                    }

                    listNodeValues.accept(Collections.singletonList(Collections.singletonList(outNode)));

                } catch (IOException ex) {
                    throw new ExprEvalException("An IOException occurred", ex);
                }
            }
            long end = System.nanoTime();
            try {
                LOG.debug("Will sleep  " + recurrenceValueNano + " - " + (end - start )+ " = " + (recurrenceValueNano - (end - start)));
                TimeUnit.NANOSECONDS.sleep(recurrenceValueNano - (end - start));
            } catch (InterruptedException ex) {
                LOG.debug("Call HTTPGET to " + url_s + " Interrupted");
                throw new ExprEvalException("Call HTTPGET to " + url_s + " Interrupted");
            }
        }
    }

}
