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
package fr.mines_stetienne.ci.sparql_generate.webapis;

import java.io.StringWriter;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/RDFXML">fun:RDFXML</a> returns
 * the RDF Dataset default graph serialized as RDF/XML.
 *
 * @author Maxime Lefrançois
 */
public final class FUN_RDFXML implements Function {

	private static final Logger LOG = LoggerFactory.getLogger(FUN_RDFXML.class);

	public static final String URI = SPARQLExt.FUN + "RDFXML";

    private static final String datatypeUri = "https://www.iana.org/assignments/media-types/application/rdf+xml";

    private static RDFDatatype dt = TypeMapper.getInstance().getSafeTypeByName(datatypeUri);

	@Override
	public final void build(String uri, ExprList args) {
		if (args.size() !=0 ) {
			throw new ExprEvalException("Expecting zero arguments");
		}
	}

	/**
	 *
	 * @param binding
	 * @param args
	 * @param uri
	 * @param env
	 * @return
	 */
	@Override
	public NodeValue exec(final Binding binding, final ExprList args, final String uri, final FunctionEnv env) {
		if (args == null) {
			throw new ARQInternalErrorException("FunctionBase: Null args list");
		}
		if (args.size() != 0) {
			throw new ExprEvalException("Expecting zero arguments");
		}
		Dataset dataset = ContextUtils.getDataset(env.getContext());
		StringWriter out = new StringWriter();
		dataset.getDefaultModel().write(out, Lang.RDFXML.getLabel());
		String ttl = out.toString();
        return new NodeValueNode(NodeFactory.createLiteral(ttl, dt));
	}
}
