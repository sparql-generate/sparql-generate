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
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;

import at.favre.lib.bytes.Bytes;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;

/**
 * Binding function <a href=
 * "http://w3id.org/sparql-generate/fn/bitwiseRightShift">fun:bitwiseRightShift</a>
 * operates a bitwise right shift over the binary literal passed as an argument.
 *
 * <ul>
 * <li>Param 1: a xsd:hexBinary or xsd:base64Binary literal;</li>
 * </ul>
 *
 * 
 * @author Maxime Lefrançois
 * 
 * @organization Ecole des Mines de Saint Etienne
 */
public class FUN_BitwiseRightShift extends FunctionBase2 implements FUN_Bytes {

	public static final String URI = SPARQLExt.FUN + "bitwiseRightShift";

	@Override
	public NodeValue exec(NodeValue argument, NodeValue shiftCount) {
		Bytes b = getBytes(argument);
		int shiftCountInt = getInteger(shiftCount);
		RDFDatatype datatype = getDatatype(argument);
		return getNodeValue(b.rightShift(shiftCountInt), datatype);
	}

}
