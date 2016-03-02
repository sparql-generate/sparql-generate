/*
 * Copyright 2016 The Apache Software Foundation.
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
package org.w3id.sparql.generate.engine;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingUtils;

/**
 *
 * @author maxime.lefrancois
 */
public class GenerationPlanSource extends GenerationPlanBase {

    private final Node node;
    String accept;
    private final Var var;

    public GenerationPlanSource(Node node, String accept, Var var) {
        this.node = node;
        this.accept = accept;
        this.var = var;
    }

    @Override
    public void $exec(Dataset inputDataset, GenerationQuerySolution initialBindings, Model initialModel) {
        org.apache.log4j.Logger.getLogger(GenerationPlanSource.class.getName()).info("Generation Source " + node.toString());
        Binding b = BindingUtils.asBinding(initialBindings);
        Node n = node;
        if (b.contains(var)) {
            throw new IllegalArgumentException("Variable " + var + " is already bound !");
        }

        try {
            HttpClient client = new DefaultHttpClient();
            if (n.isVariable()) {
                n = initialBindings.get(n.getName()).asNode();
                if (n == null) {
                    return;
                }
            } else if (!node.isURI()) {
                throw new IllegalArgumentException("node shoud be a IRI or be bound to a IRI. got " + n);
            }
            String getURL = node.getURI(); // TODO: what if not http ? what if relative URI ?
            HttpGet get = new HttpGet(getURL);

            if (accept != null) {
                get.setHeader("Accept", accept);
            } 
            HttpResponse responseGet = client.execute(get);
            HttpEntity resEntityGet = responseGet.getEntity();
            if (resEntityGet != null) {
                Header[] contentTypes = responseGet.getHeaders("Content-Type");
                if (contentTypes.length > 0) {
                    String mime = contentTypes[0].getValue();
                    String uri = "urn:iana:mime:" + mime;

                    RDFDatatype dt = org.apache.jena.datatypes.TypeMapper.getInstance().getSafeTypeByName(uri);
                    Node literal = org.apache.jena.graph.NodeFactory.createLiteral(EntityUtils.toString(resEntityGet), dt);
                    Logger.getLogger(GenerationPlanSource.class.getName()).log(Level.INFO, "new binding: " + var + literal);
                    initialBindings.put(var.getName(), initialModel.asRDFNode(literal));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(GenerationPlanSource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
