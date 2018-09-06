/*
 * Copyright 2017 Ecole des Mines de Saint-Etienne.
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
package com.github.thesmartenergy.sparql.generate.jena.serializer;

import java.util.List;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.ARQException;
import org.apache.jena.sparql.path.P_Alt;
import org.apache.jena.sparql.path.P_Distinct;
import org.apache.jena.sparql.path.P_FixedLength;
import org.apache.jena.sparql.path.P_Inverse;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_Mod;
import static org.apache.jena.sparql.path.P_Mod.UNSET;
import org.apache.jena.sparql.path.P_Multi;
import org.apache.jena.sparql.path.P_NegPropSet;
import org.apache.jena.sparql.path.P_OneOrMore1;
import org.apache.jena.sparql.path.P_OneOrMoreN;
import org.apache.jena.sparql.path.P_Path0;
import org.apache.jena.sparql.path.P_Path2;
import org.apache.jena.sparql.path.P_ReverseLink;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.P_Shortest;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.P_ZeroOrMoreN;
import org.apache.jena.sparql.path.P_ZeroOrOne;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathVisitor;
import org.apache.jena.sparql.serializer.SerializationContext;

/**
 *
 * @author maxime.lefrancois
 */
public class SPARQLGeneratePathWriter {

    public static void write(IndentedWriter out, Path path, SerializationContext context) {
        PathWriterWorker w = new PathWriterWorker(out, context);
        path.visit(w);
        out.flush();
    }

    static class PathWriterWorker implements PathVisitor {

        private final IndentedWriter out;
        private final SerializationContext context;
        private static boolean alwaysInnerParens = true;
        private boolean needParens = false;

        PathWriterWorker(IndentedWriter indentedWriter, SerializationContext context) {
            this.out = indentedWriter;
            this.context = context;
        }

        private void visitPath(Path path) {
            visitPath(path, true);
        }

        private void visitPath(Path path, boolean needParensThisTime) {
            if (alwaysInnerParens) {
                needParensThisTime = true;
            }
            boolean b = needParens;
            needParens = needParensThisTime;
            path.visit(this);
            needParens = b;
        }

        private void output(Node node) {
            SPARQLGenerateFmtUtils.printNode(out, node, context);
        }

        private void output(P_Path0 path0) {
            if (!path0.isForward()) {
                out.print("^");
            }
            output(path0.getNode());
        }

        @Override
        public void visit(P_Link pathNode) {
            output(pathNode.getNode());
        }

        @Override
        public void visit(P_ReverseLink pathNode) {
            out.println("^");
            output(pathNode.getNode());
        }

        @Override
        public void visit(P_NegPropSet pathNotOneOf) {
            List<P_Path0> props = pathNotOneOf.getNodes();
            if (props.size() == 0) {
                throw new ARQException("Bad path element: NotOneOf found with no elements");
            }
            out.print("!");
            if (props.size() == 1) {
                output(props.get(0));
            } else {
                out.print("(");
                boolean first = true;
                for (P_Path0 p : props) {
                    if (!first) {
                        out.print("|");
                    }
                    first = false;
                    output(p);
                }
                out.print(")");
            }
        }

        @Override
        public void visit(P_Alt pathAlt) {
            visit2(pathAlt, "|", true);
        }

        @Override
        public void visit(P_Seq pathSeq) {
            visit2(pathSeq, "/", false);
        }

        // Should pass around precedence numbers.
        private void visit2(P_Path2 path2, String sep, boolean isSeq) {
            if (needParens) {
                out.print("(");
            }
            visitPath(path2.getLeft());
            out.print(sep);
            // Don't need parens if same as before.
            if (isSeq) {
                // Make / and ^ chains look nice
                if (path2.getRight() instanceof P_Seq) {
                    visitPath(path2.getRight(), needParens);
                } else {
                    visitPath(path2.getRight(), true);
                }
            } else {
                visitPath(path2.getRight(), true);
            }

            if (needParens) {
                out.print(")");
            }
        }

        @Override
        public void visit(P_Mod pathMod) {
            if (needParens) {
                out.print("(");
            }
            if (alwaysInnerParens) {
                out.print("(");
            }
            pathMod.getSubPath().visit(this);
            if (alwaysInnerParens) {
                out.print(")");
            }

            out.print("{");
            if (pathMod.getMin() != UNSET) {
                out.print(Long.toString(pathMod.getMin()));
            }
            out.print(",");
            if (pathMod.getMax() != UNSET) {
                out.print(Long.toString(pathMod.getMax()));
            }
            out.print("}");
            if (needParens) {
                out.print(")");
            }
        }

        @Override
        public void visit(P_FixedLength pFixedLength) {
            if (needParens) {
                out.print("(");
            }
            if (alwaysInnerParens) {
                out.print("(");
            }
            pFixedLength.getSubPath().visit(this);
            if (alwaysInnerParens) {
                out.print(")");
            }

            out.print("{");
            out.print(Long.toString(pFixedLength.getCount()));
            out.print("}");
            if (needParens) {
                out.print(")");
            }
        }

        @Override
        public void visit(P_Distinct pathDistinct) {
            out.print("DISTINCT(");
            pathDistinct.getSubPath().visit(this);
            out.print(")");
        }

        @Override
        public void visit(P_Multi pathMulti) {
            out.print("MULTI(");
            pathMulti.getSubPath().visit(this);
            out.print(")");
        }

        @Override
        public void visit(P_Shortest path) {
            out.print("SHORTEST(");
            path.getSubPath().visit(this);
            out.print(")");
        }

        @Override
        public void visit(P_ZeroOrOne path) {
            printPathMod("?", path.getSubPath());
        }

        @Override
        public void visit(P_ZeroOrMore1 path) {
            printPathMod("*", path.getSubPath());
        }

        @Override
        public void visit(P_ZeroOrMoreN path) {
            printPathMod("{*}", path.getSubPath());
        }

        @Override
        public void visit(P_OneOrMore1 path) {
            printPathMod("+", path.getSubPath());
        }

        @Override
        public void visit(P_OneOrMoreN path) {
            printPathMod("{+}", path.getSubPath());
        }

        private void printPathMod(String mod, Path path) {
            boolean doParens = (needParens || alwaysInnerParens);
            if (doParens) {
                out.print("(");
            }
            path.visit(this);
            if (doParens) {
                out.print(")");
            }
            out.print(mod);
        }

        // Need to consider binary ^
        @Override
        public void visit(P_Inverse inversePath) {
            out.print("^");
            Path p = inversePath.getSubPath();
            boolean parens = true;
            if (p instanceof P_Link) {
                parens = false;
            }
            visitPath(p, parens);
        }
    }
}
