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
package fr.mines_stetienne.ci.sparql_generate.query;

import java.util.Objects;
import org.apache.jena.atlas.lib.Lib;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.ComparisonException;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.core.QueryCompare;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Used to compare queries (syntactically).
 *
 * @author Maxime Lefrançois
 */
public class SPARQLExtQueryCompare implements SPARQLExtQueryVisitor {

    private SPARQLExtQuery query2;
    private boolean result = true;
    static public boolean PrintMessages = false;
    private QueryCompare qc;
    static private final Logger LOG = LoggerFactory.getLogger(SPARQLExtQueryCompare.class);

    public static boolean equals(SPARQLExtQuery query1, SPARQLExtQuery query2) {
        if (query1 == query2) {
            return true;
        }

        query1.setResultVars();
        query2.setResultVars();
        SPARQLExtQueryCompare visitor = new SPARQLExtQueryCompare(query1);
        try {
            query2.visit(visitor);
        } catch (ComparisonException ex) {
            LOG.debug("", ex);
            return false;
        }
        return visitor.isTheSame();
    }

    public SPARQLExtQueryCompare(SPARQLExtQuery query2) {
        this.qc = new QueryCompare(query2);
        this.query2 = query2;
    }

    @Override
    public void startVisit(Query query1) {
    }

    @Override
    public void visitResultForm(Query query1) {
        qc.visitResultForm(query1);
    }

    @Override
    public void visitPrologue(Prologue query1) {
        qc.visitPrologue(query1);
    }

    @Override
    public void visitSelectResultForm(Query query1) {
        qc.visitSelectResultForm(query1);
    }

    @Override
    public void visitConstructResultForm(Query query1) {
        qc.visitConstructResultForm(query1);
    }

    @Override
    public void visitDescribeResultForm(Query query1) {
        qc.visitDescribeResultForm(query1);
    }

    @Override
    public void visitAskResultForm(Query query1) {
        qc.visitAskResultForm(query1);
    }

    @Override
    public void visitDatasetDecl(Query q1) {
        SPARQLExtQuery query1 = asSPARQLExtQuery(q1);
        boolean b1 = Lib.equalsListAsSet(query1.getFromClauses(), query2.getFromClauses());
        try {
            check("From clauses", b1);
        } catch (Exception e) {
            LOG.debug("", e);
        }
    }

    @Override
    public void visitGenerateClause(SPARQLExtQuery query) {
        if (query.hasName()) {
            boolean b1 = query.getName().equals(query2.getName());
            try {
                check("Generate name", b1);
            } catch (Exception e) {
                LOG.debug("", e);
            }
        } else if (query.hasGenerateClause()) {
            boolean b2 = query.getGenerateClause().equals(query2.getGenerateClause());
            try {
                check("Generate clause", b2);
            } catch (Exception e) {
                LOG.debug("", e);
            }
        }
    }

    @Override
    public void visitFunctionExpression(SPARQLExtQuery query) {
        if (query.hasFunctionExpression()) {
            boolean b1 = query.getFunctionExpression().equals(query2.getFunctionExpression());
            try {
                check("Function Expression", b1);
            } catch (Exception e) {
                LOG.debug("", e);
            }
        }
    }

    @Override
    public void visitTemplateClause(SPARQLExtQuery query) {
        if (query.hasName()) {
            boolean b1 = query.getName().equals(query2.getName());
            try {
                check("Template name", b1);
            } catch (Exception e) {
                LOG.debug("", e);
            }
        } else if (query.hasTemplateClause()) {
            boolean b2 = query.getTemplateClause().equals(query2.getTemplateClause());
            try {
                check("Template clause", b2);
            } catch (Exception e) {
                LOG.debug("", e);
            }
        }
    }

    @Override
    public void visitPerformClause(SPARQLExtQuery query) {
        if (query.hasName()) {
            boolean b1 = query.getName().equals(query2.getName());
            try {
                check("Perform name", b1);
            } catch (Exception e) {
                LOG.debug("", e);
            }
        } else if (query.hasPerformClause()) {
            boolean b2 = query.getPerformClause().equals(query2.getPerformClause());
            try {
                check("PErform clause", b2);
            } catch (Exception e) {
                LOG.debug("", e);
            }
        }
    }

    @Override
    public void visitBindingClauses(SPARQLExtQuery query) {
        boolean b1 = Lib.equalsListAsSet(query.getBindingClauses(), query2.getBindingClauses());
        try {
            check("Iterators and sources", b1);
        } catch (Exception e) {
            LOG.debug("", e);
        }
    }

    @Override
    public void visitQueryPattern(Query query1) {
        qc.visitQueryPattern(query1);
    }

    @Override
    public void visitGroupBy(Query query1) {
        qc.visitGroupBy(query1);
    }

    @Override
    public void visitHaving(Query query1) {
        qc.visitHaving(query1);
    }

    @Override
    public void visitLimit(Query query1) {
        qc.visitLimit(query1);
    }

    @Override
    public void visitOrderBy(Query query1) {
        qc.visitOrderBy(query1);
    }

    @Override
    public void visitOffset(Query query1) {
        qc.visitOffset(query1);
    }

    @Override
    public void visitPostSelect(SPARQLExtQuery query) {
        try {
            check("Aggregate variables", query.getPostSelect(), query2.getPostSelect());
        } catch (Exception e) {
            LOG.debug("", e);
        }
    }

    @Override
    public void visitValues(Query query1) {
        qc.visitValues(query1);
    }

    @Override
    public void finishVisit(Query query1) {
    }

    private void check(String msg, Object obj1, Object obj2) throws Exception {
        check(msg, Objects.equals(obj1, obj2));
    }

    private void check(String msg, boolean b) throws Exception {
        if (!b) {
            if (PrintMessages && msg != null) {
                System.out.println("Different: " + msg);
            }
            result = false;
            throw new Exception(msg);
        }
    }

    public boolean isTheSame() {
        return result;
    }

    @Override
    public void visitPragma(SPARQLExtQuery query) {
        LOG.warn("visiting pragma. doing nothing");
    }

	@Override
	public void visitJsonResultForm(Query query) {
	}

}
