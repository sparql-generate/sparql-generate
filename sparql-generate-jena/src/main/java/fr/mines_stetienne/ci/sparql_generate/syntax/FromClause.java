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
package fr.mines_stetienne.ci.sparql_generate.syntax;

import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import java.util.Objects;
import org.apache.jena.sparql.expr.Expr;

/**
 *
 * @author Maxime Lefrançois
 */
public class FromClause {

    private final boolean isNamed;
    private final SPARQLExtQuery generate;
    private final Expr name;

    public FromClause(Expr name) {
        this.isNamed = false;
        this.name = name;
        this.generate = null;
    }

    public FromClause(boolean isNamed, Expr name) {
        this.isNamed = isNamed;
        this.name = name;
        this.generate = null;
    }

    public FromClause(SPARQLExtQuery generate) {
        this.isNamed = false;
        this.name = null;
        this.generate = generate;
    }

    public FromClause(SPARQLExtQuery generate, Expr name) {
        this.isNamed = true;
        this.name = name;
        this.generate = generate;
    }

    public boolean isNamed() {
        return isNamed;
    }

    public SPARQLExtQuery getGenerate() {
        return generate;
    }

    public Expr getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof FromClause)) {
            return false;
        }
        FromClause other = (FromClause) obj;
        return Objects.equals(this.isNamed, other.isNamed)
                && Objects.equals(this.name, other.name)
                && Objects.equals(this.generate, other.generate);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.isNamed ? 1 : 0);
        hash = 37 * hash + Objects.hashCode(this.generate);
        hash = 37 * hash + Objects.hashCode(this.name);
        return hash;
    }

}
