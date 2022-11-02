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
 * Groovy expression that calls a method on an object.
 */
public class FunctionCallExpression extends AbstractFunctionExpression {

    private String functionName;
    private List<GroovyExpression> arguments = new ArrayList<>();

    public FunctionCallExpression(TraversalStepType type, String functionName, GroovyExpression... arguments) {
        super(type, null);
        this.functionName = functionName;
        this.arguments.addAll(Arrays.asList(arguments));
    }

    public FunctionCallExpression(String functionName, GroovyExpression... arguments) {
        super(null);
        this.functionName = functionName;
        this.arguments.addAll(Arrays.asList(arguments));
    }

    public FunctionCallExpression(TraversalStepType type, String functionName, List<GroovyExpression> arguments) {
        super(type, null);
        this.functionName = functionName;
        this.arguments.addAll(arguments);
    }

    public FunctionCallExpression(GroovyExpression target, String functionName, GroovyExpression... arguments) {
        super(target);
        this.functionName = functionName;
        this.arguments.addAll(Arrays.asList(arguments));
    }

    public FunctionCallExpression(TraversalStepType type, GroovyExpression target, String functionName,
                                  List<? extends GroovyExpression> arguments) {
        super(type, target);
        this.functionName = functionName;
        this.arguments.addAll(arguments);
    }

    public FunctionCallExpression(TraversalStepType type, GroovyExpression target, String functionName,
                                  GroovyExpression... arguments) {
        super(type, target);
        this.functionName = functionName;
        this.arguments.addAll(Arrays.asList(arguments));
    }

    public void addArgument(GroovyExpression expr) {
        arguments.add(expr);
    }

    public List<GroovyExpression> getArguments() {
        return Collections.unmodifiableList(arguments);
    }


    public String getFunctionName() {
        return functionName;
    }

    @Override
    public void generateGroovy(GroovyGenerationContext context) {

        if (getCaller() != null) {
            getCaller().generateGroovy(context);
            context.append(".");
        }
        context.append(functionName);
        context.append("(");
        Iterator<GroovyExpression> it = arguments.iterator();
        while (it.hasNext()) {
            GroovyExpression expr = it.next();
            expr.generateGroovy(context);
            if (it.hasNext()) {
                context.append(",");
            }
        }
        context.append(")");
    }

    @Override
    public List<GroovyExpression> getChildren() {
        List<GroovyExpression> result = new ArrayList<>(arguments.size() + 1);
        if (getCaller() != null) {
            result.add(getCaller());
        }
        result.addAll(arguments);
        return result;
    }

    @Override
    public GroovyExpression copy(List<GroovyExpression> newChildren) {

        if (getCaller() == null) {
            return new FunctionCallExpression(getType(), functionName, newChildren);
        }

        GroovyExpression newTarget = newChildren.get(0);
        List<GroovyExpression> args = null;
        if (newChildren.size() > 1) {
            args = newChildren.subList(1, newChildren.size());
        } else {
            args = Collections.emptyList();
        }
        return new FunctionCallExpression(getType(), newTarget, functionName, args);

    }

    public void setArgument(int index, GroovyExpression value) {
        if (index < 0 || index >= arguments.size()) {
            throw new IllegalArgumentException("Invalid argIndex " + index);
        }
        arguments.set(index, value);
    }
}
