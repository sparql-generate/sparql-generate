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
package fr.emse.ci.sparqlext.utils;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Vocabulary for graphs used to instantiate the LocationMapper class
 *
 * @author Maxime Lefrançois
 */
public class LM {

    public static final String NS = "http://jena.hpl.hp.com/2004/08/location-mapping#";

    public static final Property mapping = ResourceFactory.createProperty(NS + "mapping");
    public static final Property name = ResourceFactory.createProperty(NS + "name");
    public static final Property altName = ResourceFactory.createProperty(NS + "altName");
    public static final Property media = ResourceFactory.createProperty(NS + "media");

}
