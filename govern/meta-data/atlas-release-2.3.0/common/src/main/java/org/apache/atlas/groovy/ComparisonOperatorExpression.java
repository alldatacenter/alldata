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
 * Represents an expression that compares two expressions using
 * the Groovy "spaceship" operator.  This is basically the
 * same as calling left.compareTo(right), except that it has
 * built-in null handling and some other nice features.
 *
 */
public class ComparisonOperatorExpression extends BinaryExpression {

    public ComparisonOperatorExpression(GroovyExpression left, GroovyExpression right) {
        super(left, "<=>", right);
    }

    @Override
    public GroovyExpression copy(List<GroovyExpression> newChildren) {
        assert newChildren.size() == 2;
        return new ComparisonOperatorExpression(newChildren.get(0), newChildren.get(1));
    }
}
