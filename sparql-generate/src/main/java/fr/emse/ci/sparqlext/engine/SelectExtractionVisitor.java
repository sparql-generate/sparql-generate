/*
 * Copyright 2019 École des Mines de Saint-Étienne.
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
package fr.emse.ci.sparqlext.engine;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.SPARQLExtException;
import fr.emse.ci.sparqlext.function.library.FUN_Select_Call_Template;
import fr.emse.ci.sparqlext.lang.ParserSPARQLExt;
import fr.emse.ci.sparqlext.normalizer.TemplateUtils;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.query.SPARQLExtQueryVisitor;
import fr.emse.ci.sparqlext.syntax.ElementBox;
import fr.emse.ci.sparqlext.syntax.ElementExpr;
import fr.emse.ci.sparqlext.syntax.ElementFormat;
import fr.emse.ci.sparqlext.syntax.ElementIterator;
import fr.emse.ci.sparqlext.syntax.ElementSource;
import fr.emse.ci.sparqlext.syntax.ElementSubExtQuery;
import fr.emse.ci.sparqlext.syntax.ElementTGroup;
import fr.emse.ci.sparqlext.syntax.SPARQLExtElementVisitorBase;
import fr.emse.ci.sparqlext.utils.ST;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.E_Function;
import org.apache.jena.sparql.expr.E_Str;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.aggregate.AggGroupConcat;
import org.apache.jena.sparql.expr.aggregate.AggGroupConcatDistinct;
import org.apache.jena.sparql.expr.aggregate.Aggregator;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
class SelectExtractionVisitor implements SPARQLExtQueryVisitor {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SelectExtractionVisitor.class);

    private final SPARQLExtQuery input;
    private final SPARQLExtQuery output = new SPARQLExtQuery();

    public SelectExtractionVisitor(SPARQLExtQuery input) {
        this.input = input;
    }

    public SPARQLExtQuery getOutput() {
        LOG.trace("raw select query is\n" + output);
        return (SPARQLExtQuery) output.cloneQuery();
    }

    @Override
    public void startVisit(final Query query) {
    }

    @Override
    public void visitResultForm(final Query query) {
        output.setQuerySelectType();
    }

    @Override
    public void visitPrologue(final Prologue prologue) {
        output.setPrefixMapping(input.getPrefixMapping());
        String b = input.getBaseURI();
        output.setBaseURI(b);
    }

    @Override
    public void visitSelectResultForm(final Query q) {
        SPARQLExtQuery query = asSPARQLExtQuery(q);
        output.setName(query.getName());
        output.setSignature(query.getSignature());
        output.setCallParameters(query.getCallParameters());
        output.setDistinct(query.isDistinct());
        output.setReduced(query.isReduced());
        output.setQueryResultStar(query.isQueryResultStar());
        VarExprList project = query.getProject();
        project.forEachVar((v) -> {
            output.addResultVar(v, project.getExpr(v));
        });
    }

    @Override
    public void visitConstructResultForm(final Query query) {
        throw new SPARQLExtException("should not reach this"
                + " point");
    }

    @Override
    public void visitDescribeResultForm(final Query query) {
        throw new SPARQLExtException("should not reach this"
                + " point");
    }

    @Override
    public void visitAskResultForm(final Query query) {
        throw new SPARQLExtException("should not reach this"
                + " point");
    }

    @Override
    public void visitDatasetDecl(final Query q) {
        SPARQLExtQuery query = asSPARQLExtQuery(q);
        output.getFromClauses().addAll(query.getFromClauses());
    }

    @Override
    public void visitQueryPattern(final Query query) {
        if (query.getQueryPattern() != null) {
            if (output.getQueryPattern() != null) {
                ElementGroup group = new ElementGroup();
                group.addElement(query.getQueryPattern());
                group.addElement(output.getQueryPattern());
                output.setQueryPattern(group);
            } else {
                Element el = query.getQueryPattern();
                output.setQueryPattern(el);
            }
        } else {
            output.setQueryPattern(new ElementGroup());
        }
    }

    @Override
    public void visitBindingClauses(SPARQLExtQuery query) {
        if (query.isSelectType()) {
            return;
        }
        if (query.hasAggregators() || query.hasGroupBy()) {
            return;
        }
        for (Element el : query.getBindingClauses()) {
            if (el instanceof ElementIterator) {
                ElementIterator elementIterator = (ElementIterator) el;
                output.addProjectVars(elementIterator.getVars());
            } else if (el instanceof ElementSource) {
                ElementSource elementSource = (ElementSource) el;
                output.addResultVar(elementSource.getVar());
            } else if (el instanceof ElementBind) {
                ElementBind elementBind = (ElementBind) el;
                output.addResultVar(elementBind.getVar());
            } else {
                throw new UnsupportedOperationException("should not reach"
                        + " this point");
            }
        }
    }

    @Override
    public void visitGroupBy(final Query query) {
        if (query.hasGroupBy()) {
            if (!query.getGroupBy().isEmpty()) {
                VarExprList namedExprs = query.getGroupBy();
                for (Var var : namedExprs.getVars()) {
                    Expr expr = namedExprs.getExpr(var);
                    if (expr != null) {
                        output.addGroupBy(var, expr);
                        if (!query.isSelectType()) {
                            output.addResultVar(var);
                        }
                    } else {
                        output.addGroupBy(var.getVarName());
                        if (!query.isSelectType()) {
                            output.addResultVar(var);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void visitHaving(final Query query) {
        if (query.hasHaving()) {
            for (Expr expr : query.getHavingExprs()) {
                output.addHavingCondition(expr);
            }
        }
    }

    @Override
    public void visitOrderBy(final Query query) {
        if (query.hasOrderBy()) {
            for (SortCondition sc : query.getOrderBy()) {
                output.addOrderBy(sc);
            }
        }
    }

    @Override
    public void visitOffset(final Query query) {
        if (query.hasOffset()) {
            output.setOffset(query.getOffset());
        }
    }

    @Override
    public void visitLimit(final Query query) {
        if (query.hasLimit()) {
            output.setLimit(query.getLimit());
        }
    }

    @Override
    public void visitPostSelect(SPARQLExtQuery query) {
        if (query.isSelectType() || query.isTemplateType()) {
            return;
        }
        int qtype = query.getQueryType();
        query.setQuerySelectType();
        if (!query.hasAggregators() && !query.hasGroupBy()) {
            output.setQueryResultStar(true);
            output.setResultVars();
        }
        if (query.hasPostSelect()) {
            query.getPostSelect().forEachExpr((v, e) -> {
                output.addResultVar(v, e);
            });
        }
        output.setQueryResultStar(false);
        switch (qtype) {
            case SPARQLExtQuery.QueryTypeGenerate:
                query.setQueryGenerateType();
                break;
            case SPARQLExtQuery.QueryTypeTemplate:
                query.setQueryTemplateType();
                break;
            case SPARQLExtQuery.QueryTypePerform:
                query.setQueryPerformType();
                break;
            case SPARQLExtQuery.QueryTypeSelect:
                query.setQuerySelectType();
                break;
            default:
                LOG.warn("Did not recognize type. Was " + qtype + " " + query.toString());
        }
    }

    @Override
    public void visitValues(final Query query) {
        if (query.hasValues() && !query.hasAggregators()) {
            List<Var> variables = query.getValuesVariables();
            List<Binding> values = query.getValuesData();
            output.setValuesDataBlock(variables, values);
            output.addProjectVars(variables);
        }
    }

    @Override
    public void visitGenerateClause(SPARQLExtQuery query) {
    }

    @Override
    public void visitFunctionExpression(SPARQLExtQuery query) {
        Var var = Var.alloc("out");
        ElementBind bind = new ElementBind(var, query.getFunctionExpression());
        output.setQueryPattern(bind);
        output.addResultVar(var);
    }

    @Override
    public void visitTemplateClause(SPARQLExtQuery query) {
        output.setDistinct(query.isDistinct());
        output.setReduced(query.isReduced());
        final TProc tproc = new TProc();
        Var var = Var.alloc("out");
        List<Element> templateElements = query.getTemplateClause();
        if (templateElements.size() == 1) {
            templateElements.get(0).visit(tproc);
            output.addResultVar(var, tproc.result);
        } else {
            List<Expr> texprs = new ArrayList<>();
            for (int i = 0; i < templateElements.size(); i++) {
                templateElements.get(i).visit(tproc);
                Expr result = tproc.result;
                if (result instanceof E_Function && ((E_Function) result).getFunctionIRI().equals(ST.concat)) {
                    texprs.addAll(((E_Function) result).getArgs());
                } else {
                    texprs.add(result);
                }
            }
            Expr concat = new E_Function(ST.concat, new ExprList(texprs));
            output.addResultVar(var, concat);
        }
    }

    @Override
    public void visitPerformClause(SPARQLExtQuery query) {
    }

    @Override
    public void finishVisit(final Query query) {
        if (output.getProjectVars().isEmpty()) {
            output.setQueryResultStar(true);
        }
    }

    @Override
    public void visitPragma(SPARQLExtQuery query) {
    }

    private static class TProc extends SPARQLExtElementVisitorBase {

        Expr result = null;

        @Override
        public void visit(ElementExpr el) {
            if (el.getExpr() instanceof NodeValueString) {
                result = el.getExpr();
            } else {
                result = new E_Str(el.getExpr());
            }
        }

        @Override
        public void visit(ElementBox el) {
            final List<Expr> texprs = new ArrayList<>();
            texprs.add(new E_Function(ST.incr, new ExprList()));
            List<Element> elements = el.getTExpressions();
            for (int i = 0; i < elements.size(); i++) {
                elements.get(i).visit(this);
                if (result instanceof E_Function && ((E_Function) result).getFunctionIRI().equals(ST.concat)) {
                    texprs.addAll(((E_Function) result).getArgs());
                } else {
                    texprs.add(result);
                }
            }
            texprs.add(new E_Function(ST.decr, new ExprList()));
            result = new E_Function(ST.concat, new ExprList(texprs));
        }

        @Override
        public void visit(ElementFormat el) {
            final List<Expr> texprs = new ArrayList<>();
            texprs.add(el.getExpr().getExpr());
            el.getTExpressions().forEach((e) -> {
                e.visit(this);
                texprs.add(result);
            });
            result = new E_Function(ST.format, new ExprList(texprs));
        }

        @Override
        public void visit(ElementTGroup el) {
            String sep = el.getSeparator();
            final List<Expr> texprs = new ArrayList<>();
            el.getTExpressions().forEach((e) -> {
                e.visit(this);
                texprs.add(result);
            });
            final Expr concat = new E_Function(ST.concat, new ExprList(texprs));
            final Aggregator groupConcat;
            if (el.isDistinct()) {
                groupConcat = new AggGroupConcatDistinct(concat, sep);
            } else {
                groupConcat = new AggGroupConcat(concat, sep);
            }
            Var var = Var.alloc(UUID.randomUUID().toString().substring(0, 8));
            result = new ExprAggregator(var, groupConcat);
        }

        @Override
        public void visit(ElementSubExtQuery el) {
            SPARQLExtQuery query = el.getQuery();
            result = TemplateUtils.getFunction(query);
        }

    };
}
