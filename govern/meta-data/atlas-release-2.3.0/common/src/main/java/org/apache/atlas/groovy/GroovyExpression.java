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
 * Represents an expression in the Groovy programming language, which
 * is the language that Gremlin scripts are written and interpreted in.
 */
public interface GroovyExpression  {
    /**
     * Generates a Groovy script from the expression.
     *
     * @param context
     */
    void generateGroovy(GroovyGenerationContext context);

    /**
     * Gets all of the child expressions of this expression.
     * s
     * @return
     */
    List<GroovyExpression> getChildren();

    /**
     * Makes a copy of the expression, keeping everything the
     * same except its child expressions.  These are replaced
     * with the provided children.  The order of the children
     * is important.  It is expected that the children provided
     * here are updated versions of the children returned by
     * getChildren().  The order of the children must be the
     * same as the order in which the children were returned
     * by getChildren()
     *
     * @param newChildren
     * @return
     */
    GroovyExpression copy(List<GroovyExpression> newChildren);

    /**
     * Makes a shallow copy of the GroovyExpression.  This
     * is equivalent to copy(getChildren());
     *
     * @return
     */
    GroovyExpression copy();

    /**
     * Gets the type of traversal step represented by this
     * expression (or TraversalStepType.NONE if it is not part of a graph traversal).
     *
     * @return
     */
    TraversalStepType getType();
}
