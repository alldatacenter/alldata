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
 * Represents any kind of binary expression.  This could
 * be an arithmetic expression, such as a + 3, a boolean
 * expression such as a < 5, or even a comparison function
 * such as a <=> b.
 *
 */
public abstract class BinaryExpression extends AbstractGroovyExpression {


    private GroovyExpression left;
    private GroovyExpression right;
    protected String op;

    public BinaryExpression(GroovyExpression left, String op, GroovyExpression right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }

    @Override
    public void generateGroovy(GroovyGenerationContext context) {

        left.generateGroovy(context);
        context.append(" ");
        context.append(op);
        context.append(" ");
        right.generateGroovy(context);
    }

    @Override
    public List<GroovyExpression> getChildren() {
        return Arrays.asList(left, right);
    }
}
