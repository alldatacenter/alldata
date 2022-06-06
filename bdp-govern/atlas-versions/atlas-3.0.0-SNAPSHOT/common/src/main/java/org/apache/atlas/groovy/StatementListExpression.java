/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.groovy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a semi-colon delimited list of Groovy expressions.
 */
public class StatementListExpression extends AbstractGroovyExpression {

    private List<GroovyExpression> stmts = new ArrayList<>();

    public StatementListExpression() {

    }

    /**
     * @param newChildren
     */
    public StatementListExpression(List<GroovyExpression> newChildren) {
        stmts.addAll(newChildren);
    }

    public void addStatement(GroovyExpression expr) {
        if (expr instanceof StatementListExpression) {
            stmts.addAll(((StatementListExpression)expr).getStatements());
        } else {
            stmts.add(expr);
        }
    }

    public void addStatements(List<GroovyExpression> exprs) {
        for(GroovyExpression expr : exprs) {
            addStatement(expr);
        }
    }

    @Override
    public void generateGroovy(GroovyGenerationContext context) {

        Iterator<GroovyExpression> stmtIt = stmts.iterator();
        while(stmtIt.hasNext()) {
            GroovyExpression stmt = stmtIt.next();
            stmt.generateGroovy(context);
            if (stmtIt.hasNext()) {
                context.append(";");
            }
        }
    }


    public List<GroovyExpression> getStatements() {
        return stmts;
    }

    @Override
    public List<GroovyExpression> getChildren() {
        return Collections.unmodifiableList(stmts);
    }

    @Override
    public GroovyExpression copy(List<GroovyExpression> newChildren) {
        return new StatementListExpression(newChildren);
    }

    @Override
    public TraversalStepType getType() {
        return TraversalStepType.NONE;
    }

    /**
     * @param oldExpr
     * @param newExpr
     */
    public void replaceStatement(int index, GroovyExpression newExpr) {
        stmts.set(index, newExpr);
    }
}
