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
 * Groovy statement that assigns a value to a variable.
 */
public class VariableAssignmentExpression extends AbstractGroovyExpression {

    private String type = null;
    private String name;
    private GroovyExpression value;

    /**
     * @param string
     * @param v
     */
    public VariableAssignmentExpression(String type, String name, GroovyExpression v) {
        this.type = type;
        this.name = name;
        this.value = v;
    }

    public VariableAssignmentExpression(String name, GroovyExpression v) {
        this(null, name, v);
    }

    @Override
    public void generateGroovy(GroovyGenerationContext context) {
        if (type == null) {
            context.append("def ");
        } else {
            context.append(type);
            context.append(" ");
        }
        context.append(name);
        context.append("=");
        value.generateGroovy(context);

    }

    @Override
    public List<GroovyExpression> getChildren() {
        return Collections.singletonList(value);
    }

    @Override
    public GroovyExpression copy(List<GroovyExpression> newChildren) {
        assert newChildren.size() == 1;
        return new VariableAssignmentExpression(name, newChildren.get(0));
    }

}
