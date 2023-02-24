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

import java.util.Arrays;
import java.util.List;

/**
 * Groovy expression that represents the ternary operator (expr ? trueValue :
 * falseValue)
 */
public class TernaryOperatorExpression extends AbstractGroovyExpression {

    private GroovyExpression booleanExpr;
    private GroovyExpression trueValue;
    private GroovyExpression falseValue;

    public TernaryOperatorExpression(GroovyExpression booleanExpr, GroovyExpression trueValue,
                                     GroovyExpression falseValue) {

        this.booleanExpr = booleanExpr;
        this.trueValue = trueValue;
        this.falseValue = falseValue;
    }

    @Override
    public void generateGroovy(GroovyGenerationContext context) {

        context.append("((");
        booleanExpr.generateGroovy(context);
        context.append(")?(");
        trueValue.generateGroovy(context);
        context.append("):(");
        falseValue.generateGroovy(context);
        context.append("))");
    }

    public String toString() {
        GroovyGenerationContext context = new GroovyGenerationContext();
        generateGroovy(context);
        return context.getQuery();
    }

    @Override
    public List<GroovyExpression> getChildren() {
        return Arrays.asList(booleanExpr, trueValue, falseValue);
    }

    @Override
    public GroovyExpression copy(List<GroovyExpression> newChildren) {
        assert newChildren.size() == 3;
        return new TernaryOperatorExpression(newChildren.get(0), newChildren.get(1), newChildren.get(2));
    }

    @Override
    public TraversalStepType getType() {
        return trueValue.getType();
    }
}
