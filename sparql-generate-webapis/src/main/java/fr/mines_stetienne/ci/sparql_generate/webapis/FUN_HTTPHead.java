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

import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/HTTPHead">fun:HTTPHead</a> 
 * operates a HTTP HEAD operation.
 *
 * <ul>
 * <li>Param 1: (a URL) the IRI of the resource to get;</li>
 * <li>Param 2: (string): additional HTTP headers for the request.</li>
 * </ul>
 *
 * Returns the HTTP header as a literal
 *
 * @author Maxime Lefrançois
 */
public final class FUN_HTTPHead extends FUN_HTTPBase {

	public static final String URI = SPARQLExt.FUN + "HTTPHead";
	
	@Override
	protected HttpRequestBase createRequest(String fileURI) {
		return new HttpHead(fileURI);
	}

}