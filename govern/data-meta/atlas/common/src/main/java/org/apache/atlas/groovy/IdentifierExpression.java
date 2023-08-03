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
 * Groovy expression that references the variable with the given name.
 *
 */
public class IdentifierExpression extends AbstractGroovyExpression {

    private TraversalStepType type = TraversalStepType.NONE;
    private String varName;

    public IdentifierExpression(String varName) {
        this.varName = varName;
    }

    public IdentifierExpression(TraversalStepType type, String varName) {
        this.varName = varName;
        this.type = type;
    }


    public String getVariableName() {
        return varName;
    }

    @Override
    public void generateGroovy(GroovyGenerationContext context) {
        context.append(varName);
    }

    @Override
    public List<GroovyExpression> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public GroovyExpression copy(List<GroovyExpression> newChildren) {
        assert newChildren.isEmpty();
        IdentifierExpression result =  new IdentifierExpression(varName);
        result.setType(type);
        return result;
    }

    public void setType(TraversalStepType type) {
        this.type = type;
    }

    @Override
    public TraversalStepType getType() {
        return type;
    }
}
