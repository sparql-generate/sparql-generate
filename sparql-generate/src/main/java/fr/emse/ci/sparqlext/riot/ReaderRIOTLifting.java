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
//package fr.emse.ci.sparqlext.riot;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import org.apache.jena.riot.Lang;
//import org.apache.jena.riot.RDFParserRegistry;
//import org.apache.jena.riot.ReaderRIOT;
//import org.apache.jena.riot.ReaderRIOTFactory;
//import org.apache.jena.riot.RiotException;
//import org.apache.jena.riot.lang.RiotParsers;
//import org.apache.jena.riot.system.ParserProfile;
//import org.apache.jena.sparql.core.Var;
//
///**
// *
// * @author maxime.lefrancois
// */
//public class ReaderRIOTLifting {
//
//    private static Map<Lang, LiftingRule> registeredLiftingRules = new HashMap<>();
//
//    public static void register(Lang lang, String name, List<Var> vars) {
//        RiotParsers.createParser(input, lang, dest, profile)
//    }
//
//    public static class Factory implements ReaderRIOTFactory {
//        @Override
//        public ReaderRIOT create(Lang lang, ParserProfile parserProfile) {
//            if (!registeredLiftingRules.containsKey(lang)) {
//                throw new RiotException("lang " + lang.getName() + " not registered");
//            }
//            return new ReaderRIOTLifting(registeredLiftingRules.get(lang), parserProfile);
//        }
//    }
//
//    private ReaderRIOTLifting(LiftingRule get, ParserProfile parserProfile) {
//        
//    }
//            
////            Lang lang = LangBuilder.create()
////                    .langName(UUID.randomUUID().toString().substring(0,6))
////                    .contentType("*/*")
////                    .build();
////            RDFLanguages.register(lang);
////            RDFParserRegistry.registerLangTriples(lang, factory);
//            
//    
//    private class LiftingRule {
//
//        private final String name;
//        private final List<Var> vars;
//
//        public LiftingRule(String name, List<Var> vars) {
//            this.name = name;
//            this.vars = vars;
//        }
//
//    }
//}
