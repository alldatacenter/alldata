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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Groovy expression that represents a Groovy closure.
 */
public class ClosureExpression extends AbstractGroovyExpression {

    /**
     * Variable declaration in a closure.
     */
    public static class VariableDeclaration {
        private String type;
        private String varName;

        public VariableDeclaration(String type, String varName) {
            super();
            this.type = type;
            this.varName = varName;
        }

        public VariableDeclaration(String varName) {
            this.varName = varName;
        }

        public void append(GroovyGenerationContext context) {
            if (type != null) {
                context.append(type);
                context.append(" ");
            }
            context.append(varName);
        }
    }
    private List<VariableDeclaration> vars = new ArrayList<>();
    private StatementListExpression body = new StatementListExpression();

    public ClosureExpression(String... varNames) {
        this(null, varNames);
    }

    public ClosureExpression(GroovyExpression initialStmt, String... varNames) {
        this(Arrays.asList(varNames), initialStmt);
    }

    public ClosureExpression(List<String> varNames, GroovyExpression initialStmt) {
        if (initialStmt != null) {
            this.body.addStatement(initialStmt);
        }
        for (String varName : varNames) {
            vars.add(new VariableDeclaration(varName));
        }
    }

    public ClosureExpression(GroovyExpression initialStmt, List<VariableDeclaration> varNames) {
        if (initialStmt != null) {
            this.body.addStatement(initialStmt);
        }
        vars.addAll(varNames);
    }

    public void addStatement(GroovyExpression expr) {
        body.addStatement(expr);
    }

    public void addStatements(List<GroovyExpression> exprs) {
        body.addStatements(exprs);
    }

    public void replaceStatement(int index, GroovyExpression newExpr) {
        body.replaceStatement(index, newExpr);
    }

    @Override
    public void generateGroovy(GroovyGenerationContext context) {

        context.append("{");
        if (!vars.isEmpty()) {
            Iterator<VariableDeclaration> varIt = vars.iterator();
            while(varIt.hasNext()) {
                VariableDeclaration var = varIt.next();
                var.append(context);
                if (varIt.hasNext()) {
                    context.append(", ");
                }
            }
            context.append("->");
        }
        body.generateGroovy(context);
        context.append("}");
    }

    @Override
    public List<GroovyExpression> getChildren() {
        return Collections.<GroovyExpression>singletonList(body);
    }

    public List<GroovyExpression> getStatements() {
        return body.getStatements();
    }

    @Override
    public GroovyExpression copy(List<GroovyExpression> newChildren) {
        assert newChildren.size() == 1;
        return new ClosureExpression(newChildren.get(0), vars);
    }
}
