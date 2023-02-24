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
 * Represents an "exclusive" range expression, e.g. [0..&lt;10].
 */
public class RangeExpression extends AbstractFunctionExpression {

    private TraversalStepType stepType;
    private int startIndex;
    private int endIndex;

    public RangeExpression(TraversalStepType stepType, GroovyExpression parent, int offset, int count) {
        super(parent);
        this.startIndex = offset;
        this.endIndex = count;
        this.stepType = stepType;
    }

    @Override
    public void generateGroovy(GroovyGenerationContext context) {
        getCaller().generateGroovy(context);
        context.append(" [");
        new LiteralExpression(startIndex).generateGroovy(context);
        context.append("..<");
        new LiteralExpression(endIndex).generateGroovy(context);
        context.append("]");
    }

    @Override
    public List<GroovyExpression> getChildren() {
        return Collections.singletonList(getCaller());
    }

    @Override
    public GroovyExpression copy(List<GroovyExpression> newChildren) {
        assert newChildren.size() == 1;
        return new RangeExpression(stepType, newChildren.get(0), startIndex, endIndex);
    }

    @Override
    public TraversalStepType getType() {
        return stepType;
    }

    public int getStartIndex() {

        return startIndex;
    }

    public void setStartIndex(int startIndex) {

        this.startIndex = startIndex;
    }

    public int getEndIndex() {

        return endIndex;
    }

    public void setEndIndex(int endIndex) {

        this.endIndex = endIndex;
    }
}
