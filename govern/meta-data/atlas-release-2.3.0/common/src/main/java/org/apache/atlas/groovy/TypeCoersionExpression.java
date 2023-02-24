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

import java.util.Collections;
import java.util.List;

/**
 * Groovy expression that represents a type coersion (e.g obj as Set).
 */
public class TypeCoersionExpression extends AbstractGroovyExpression {

    private GroovyExpression expr;
    private String className;

    public TypeCoersionExpression(GroovyExpression expr, String className) {
        this.expr = expr;
        this.className  = className;
    }

    @Override
    public void generateGroovy(GroovyGenerationContext context) {

        context.append("((");
        expr.generateGroovy(context);
        context.append(")");
        context.append(" as ");
        context.append(className);
        context.append(")");
    }

    @Override
    public List<GroovyExpression> getChildren() {
        return Collections.singletonList(expr);
    }

    @Override
    public GroovyExpression copy(List<GroovyExpression> newChildren) {
        assert newChildren.size() == 1;
        return new TypeCoersionExpression(newChildren.get(0), className);
    }

}
