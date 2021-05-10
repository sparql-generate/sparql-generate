/*
* Copyright 2021 MINES Saint-Étienne
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
package fr.mines_stetienne.ci.sparql_generate.binary;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.favre.lib.bytes.Bytes;

public interface FUN_Bytes {

	public static final Logger LOG = LoggerFactory.getLogger(FUN_BitwiseLeftShift.class);

	public default Bytes getBytes(NodeValue nodeValue) {
		if (!nodeValue.isLiteral()) {
			LOG.debug("Argument must be a Literal");
			throw new ExprEvalException("Argument must be a Literal");
		}
		Node nValue = nodeValue.getNode();
		RDFDatatype datatype = nValue.getLiteralDatatype();
		if(!datatype.equals(XSDDatatype.XSDhexBinary) && !datatype.equals(XSDDatatype.XSDbase64Binary)) {
			LOG.debug("Argument must be a xsd:hexBinary or xsd:base64Binary literal");
			throw new ExprEvalException("Argument must be a xsd:hexBinary or xsd:base64Binary literal");
		}
		byte[] binary = (byte[]) nValue.getLiteralValue();
		return Bytes.wrap(binary);
	}
	
	public default int getInteger(NodeValue nodeValue) {
		if (!nodeValue.isInteger()) {
			LOG.debug("Argument must be an integer");
			throw new ExprEvalException("Argument must be an integer");
		}
		return	 nodeValue.getInteger().intValue();
	}
	
	public default RDFDatatype getDatatype(NodeValue nodeValue) {
		return nodeValue.getNode().getLiteralDatatype();
	}
	
	public default NodeValue getNodeValue(Bytes bytes, RDFDatatype datatype) {
		Node output = NodeFactory.createLiteral(datatype.unparse(bytes.array()), datatype);
		return new NodeValueNode(output);
	}
}
