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

import org.apache.atlas.AtlasException;

import java.util.List;

/**
 * Represents an arithmetic expression such as a+b.
 */
public class ArithmeticExpression extends BinaryExpression {

    /**
     * Allowed arithmetic operators.
     */
    public enum ArithmeticOperator  {
        PLUS("+"),
        MINUS("-"),
        TIMES("*"),
        DIVIDE("/"),
        MODULUS("%");

        private String groovyValue;
        ArithmeticOperator(String groovyValue) {
            this.groovyValue = groovyValue;
        }
        public String getGroovyValue() {
            return groovyValue;
        }

        public static ArithmeticOperator lookup(String groovyValue) throws AtlasException {
            for(ArithmeticOperator op : ArithmeticOperator.values()) {
                if (op.getGroovyValue().equals(groovyValue)) {
                    return op;
                }
            }
            throw new AtlasException("Unknown Operator:" + groovyValue);
        }

    }

    public ArithmeticExpression(GroovyExpression left, ArithmeticOperator op, GroovyExpression right) {
        super(left, op.getGroovyValue(), right);
    }

    private ArithmeticExpression(GroovyExpression left, String op, GroovyExpression right) {
        super(left, op, right);
    }

    @Override
    public GroovyExpression copy(List<GroovyExpression> newChildren) {
        assert newChildren.size() == 2;
        return new ArithmeticExpression(newChildren.get(0), op, newChildren.get(1));
    }
}
