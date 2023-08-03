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

import java.util.List;

/**
 * Represents a logical (and/or) expression.
 *
 */
public class LogicalExpression extends BinaryExpression {

    /**
     * Allowed logical operators.
     *
     */
    public enum LogicalOperator  {
        AND("&&"),
        OR("||");

        private String groovyValue;
        LogicalOperator(String groovyValue) {
            this.groovyValue = groovyValue;
        }
        public String getGroovyValue() {
            return groovyValue;
        }
    }

    public LogicalExpression(GroovyExpression left, LogicalOperator op, GroovyExpression right) {
        super(left, op.getGroovyValue(), right);
    }

    private LogicalExpression(GroovyExpression left, String op, GroovyExpression right) {
        super(left, op, right);
    }

    @Override
    public GroovyExpression copy(List<GroovyExpression> newChildren) {
        assert newChildren.size() == 2;
        return new LogicalExpression(newChildren.get(0), op, newChildren.get(1));
    }
}
