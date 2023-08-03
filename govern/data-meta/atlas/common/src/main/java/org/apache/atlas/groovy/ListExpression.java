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
 * Groovy expression that represents a list literal.
 */
public class ListExpression extends AbstractGroovyExpression {

    private List<GroovyExpression> values = new ArrayList<>();


    public ListExpression(GroovyExpression... values) {
        this.values = Arrays.asList(values);
    }

    public ListExpression(List<? extends GroovyExpression> values) {
        this.values.addAll(values);
    }

    public void addArgument(GroovyExpression expr) {
        values.add(expr);
    }

    public void generateGroovy(GroovyGenerationContext context) {

        context.append("[");
        Iterator<GroovyExpression> it = values.iterator();
        while(it.hasNext()) {
            GroovyExpression expr = it.next();
            expr.generateGroovy(context);
            if (it.hasNext()) {
                context.append(", ");
            }
        }
        context.append("]");
    }

    @Override
    public List<GroovyExpression> getChildren() {
        return Collections.unmodifiableList(values);
    }

    @Override
    public GroovyExpression copy(List<GroovyExpression> newChildren) {
        return new ListExpression(newChildren);
    }


}
