/*
 * Copyright 2016 Ecole des Mines de Saint-Etienne.
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
package com.github.thesmartenergy.sparql.generate.jena.function.library;

import com.github.filosganga.geogson.gson.GeometryAdapterFactory;
import com.github.filosganga.geogson.model.Feature;
import com.github.filosganga.geogson.model.FeatureCollection;
import com.github.filosganga.geogson.model.Geometry;
import com.github.filosganga.geogson.model.Point;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.utils.WktLiteral;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.function.FunctionBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/dateTime">fun:dateTime</a>
 * converts a given datetime or a UNIX timestamp in milliseconds to an xsd:dateTime.
 * <ul>
 * <li>args contains the supplied arguments such that
 * <ul>
 * <li>the first argument is either a datetime or a UNIX timestamp (in milliseconds);</li>
 * <li>the second argument is optional, and (if provided) contains the parsing format string in the <a href="https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html#iso8601timezone">ISO 8601</a> format according to universal time;</li>
 * <li>if there is no second argument, the first argument is considered as a UNIX timestamp (in milliseconds), .</li>
 * </ul>
 * </li>
 * <li>Result is a xsd:dateTime.</li>
 * </ul>
 *
 * <b>Examples: </b>
 * <pre>
 * {@code
 * fun:dateTime("1453508109000") => "2016-01-23T01:15:09Z"^^http://www.w3.org/2001/XMLSchema#dateTime
 * fun:dateTime("04/09/2018","dd/MM/yyyy") => "2018-09-04T00:00:00Z"^^http://www.w3.org/2001/XMLSchema#dateTime
 * }
 * </pre>
 *
 * @author El Mehdi Khalfi <el-mehdi.khalfi at emse.fr>
 * @since 2018-09-05
 */
public final class FUN_GeoCSVasWKT extends FunctionBase {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FUN_GeoCSVasWKT.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.FUN + "GeoCSVasWKT";

    /**
     * Registering the GeometryAdapterFactory.
     * Gson TypeAdapterFactory is responsible fpr serializing/de-serializing all the {@link Geometry}, {@link Feature}
     * and {@link FeatureCollection} instances.
     */
    private static Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new GeometryAdapterFactory())
            .create();

    @Override
    public NodeValue exec(List<NodeValue> args) {
        switch (args.size()) {
            case 1:
                //POINT (30 10)
                //LINESTRING (30 10, 10 30, 40 40)
                //POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))
                //POLYGON ((35 10, 10 20, 15 40, 45 45, 35 10),(20 30, 35 35, 30 20, 20 30))
                //MULTIPOINT ((10 40), (40 30), (20 20), (30 10))
                //MULTILINESTRING ((10 10, 20 20, 10 40),(40 40, 30 30, 40 20, 30 10))
                //MULTIPOLYGON (((30 20, 10 40, 45 40, 30 20)),((15 5, 40 10, 10 20, 5 10, 15 5)))
                //MULTIPOLYGON (((40 40, 20 45, 45 30, 40 40)),((20 35, 45 20, 30 5, 10 10, 10 30, 20 35),(30 20, 20 25, 20 15, 30 20)))
                //GEOMETRYCOLLECTION(POLYGON((1 1,2 1,2 2,1 2,1 1)),POINT(2 3),LINESTRING(2 3,3 4))
                NodeValue wktGeometryNode = args.get(0);
                String wktGeometry = wktGeometryNode.asString();
                return new NodeValueNode(NodeFactory.createLiteral(wktGeometry, WktLiteral.wktLiteralType));
            case 2:
                // "7.34245,45.83736" -> "POINT (7.34245 45.83736)"
                Double lat = Double.parseDouble(args.get(0).asString());
                Double lon = Double.parseDouble(args.get(1).asString());

                Point p = Point.from(lat, lon);
                String pString = gson.toJson(p.positions()).
                        replace(",", " ").
                        replace("] [", ", ").
                        replace("[", "(").
                        replace("]", ")");
                pString = p.getClass().getSimpleName().toUpperCase() + " " + pString;
                Node n = NodeFactory.createLiteral(pString, WktLiteral.wktLiteralType);
                return new NodeValueNode(n);
        }

        //Node node = NodeFactory.createLiteral(defaultFormat.format(date), XSDDatatype.XSDdateTime);
        //return new NodeValueNode(node);
        System.out.println("NULL");
        return null;
    }

    @Override
    public void checkBuild(String uri, ExprList args) {
        if (args.size() == 0 || args.size() > 2) {
            throw new QueryBuildException("Function '"
                    + this.getClass().getName() + "' takes up to two argument");
        }
    }
}
