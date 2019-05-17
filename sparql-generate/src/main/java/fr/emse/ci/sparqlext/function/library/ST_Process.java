///*
// * Copyright 2019 École des Mines de Saint-Étienne.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package fr.emse.ci.sparqlext.function.library;
//
//import fr.emse.ci.sparqlext.SPARQLExt;
//import fr.emse.ci.sparqlext.utils.ST;
//import org.apache.jena.shared.PrefixMapping;
//import org.apache.jena.sparql.ARQInternalErrorException;
//import org.apache.jena.sparql.engine.binding.Binding;
//import org.apache.jena.sparql.expr.ExprEvalException;
//import org.apache.jena.sparql.expr.ExprList;
//import org.apache.jena.sparql.expr.NodeValue;
//import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
//import org.apache.jena.sparql.function.Function;
//import org.apache.jena.sparql.function.FunctionEnv;
//import org.apache.jena.sparql.util.FmtUtils;
//
///**
// *
// * @author maxime.lefrancois
// */
//public class ST_Process implements Function {
//
//    public static String URI = ST.process;
//
//    @Override
//    public final void build(String uri, ExprList args) {
//        if (args.size() != 1) {
//            throw new ExprEvalException("Expecting one argument");
//        }
//    }
//
//    @Override
//    public NodeValue exec(Binding binding, ExprList args, String uri, FunctionEnv env) {
//        if (args == null) {
//            throw new ARQInternalErrorException("FunctionBase: Null args list");
//        }
//        if (args.size() != 1) {
//            throw new ExprEvalException("Expecting one argument");
//        }
//        NodeValue arg = args.get(0).eval(binding, env);
//
//        PrefixMapping pm = env.getContext().get(SPARQLExt.PREFIX_MANAGER);
//        if (pm != null) {
//            return new NodeValueString(FmtUtils.stringForNode(arg.asNode(), pm));
//        } else {
//            return new NodeValueString(FmtUtils.stringForNode(arg.asNode()));
//        }
//    }
//}
