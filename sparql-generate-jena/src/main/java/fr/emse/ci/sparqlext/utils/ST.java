/*
 * Copyright 2018 École des Mines de Saint-Étienne.
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
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 *
 * @author maxime.lefrancois
 */
public class ST {

    public static final String NS = "http://ns.inria.fr/sparql-template/";

    public static final String nl = NS + "nl";

    public static final String turtle = NS + "turtle";
    
    public static final String concat = NS + "concat";

    public static final String number = NS + "number";

    public static final String format = NS + "format";

    public static final String process = NS + "process";

    public static final String applyTemplates = NS + "apply-templates";

    public static final String applyTemplateWith = NS + "apply-template-with";

    public static final String applyTemplateAll = NS + "apply-template-all";

    public static final String applyTemplateWithAll = NS + "apply-template-with-all";

    public static final String applyTemplateWithGraph = NS + "apply-template-with-graph";

    public static final String applyTemplateGraph = NS + "apply-template-graph";

    public static final String callTemplate = NS + "call-template";

    public static final String getFocusNode = NS + "getFocusNode";

    public static final String template = NS + "template";

    public static final String incr = NS + "incr";
    
    public static final String decr = NS + "decr";

    public static final Resource Template = ResourceFactory.createResource(template);

    public static final String priority = NS + "priority";

    public static final Property Priority = ResourceFactory.createProperty(priority);

}
