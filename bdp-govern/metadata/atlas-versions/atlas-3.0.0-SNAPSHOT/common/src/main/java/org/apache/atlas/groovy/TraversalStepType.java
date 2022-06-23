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

/**
 * Types of graph traversal steps.  These are based on the traversal steps
 * described in the TinkerPop documentation at
 * http://tinkerpop.apache.org/docs/current/reference/#graph-traversal-steps.
 */
public enum TraversalStepType {
    /**
     * Indicates that the expression is not part of a graph traversal.
     */
    NONE,

    /**
     * Indicates that the expression is a
     * {@link org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource}.
     * This is not technically a graph traversal step.  This is the expression the traversal is started from ("g").
     */
    SOURCE,

    /**
     * A Start step adds vertices or edges to the traversal.  These include "V", "E", and "inject".
     */
    START,

    /**
     * An End step causes the traversal to be executed.  This includes steps such as "toList", "toSet", and "fill"
     */
    END,

    /**
     * Map steps map the current traverser value to exactly one new value.  These
     * steps include "map" and "select".  Here, we make a further distinction
     * based on the type of expression that things are being mapped to.
     * <p>
     * MAP_TO_ELEMENT indicates that the traverser value is being mapped
     * to either a Vertex or an Edge.
     */
    MAP_TO_ELEMENT,
    /**
     * Map steps map the current traverser value to exactly one new value.  These
     * steps include "map" and "select".  Here, we make a further distinction
     * based on the type of expression that things are being mapped to.
     * <p>
     * MAP_TO_VALUE indicates that the traverser value is being mapped
     * to something that is not a Vertex or an Edge.
     */
    MAP_TO_VALUE,

    /**
     * FlatMap steps map the current value of the traverser to an iterator of objects that
     * are streamed to the next step.  These are steps like "in, "out", "inE", and
     * so forth which map the current value of the traverser from some vertex or edge
     * to some other set of vertices or edges that is derived from the original set based
     * on the structure of the graph.  This also includes "values", which maps a vertex or
     * edge to the set of values for a given property. Here, we make a further distinction
     * based on the type of expression that things are being mapped to.
     * <p>
     *  FLAT_MAP_TO_ELEMENTS indicates that the traverser value is being mapped
     * to something that is a Vertex or an Edge (in, out, outE fall in this category).
     */
    FLAT_MAP_TO_ELEMENTS,

    /**
     * FlatMap steps map the current value of the traverser to an iterator of objects that
     * are streamed to the next step.  These are steps like "in, "out", "inE", and
     * so forth which map the current value of the traverser from some vertex or edge
     * to some other set of vertices or edges that is derived from the original set based
     * on the structure of the graph.  This also includes "values", which maps a vertex or
     * edge to the set of values for a given property. Here, we make a further distinction
     * based on the type of expression that things are being mapped to.
     * <p>
     *  FLAT_MAP_TO_VALUES indicates that the traverser value is being mapped
     * to something that not is a Vertex or an Edge (values falls in this category).
     */
    FLAT_MAP_TO_VALUES,

    /**
     * Filter steps filter things out of the traversal.  These include "has", "where",
     * "and", "or", and "filter".
     */
    FILTER,

    /**
     * Side effect steps do not affect the traverser value, but do something
     * that affects the state of the traverser.  These include things such as
     * "enablePath()", "as", and "by".
     */
    SIDE_EFFECT,

    /**
     * Branch steps split the traverser, for example, "repeat", "branch", "choose", and "union".
     */
    BRANCH,

    /**
     * Barrier steps in Gremlin force everything before them to be executed
     * before moving on to the steps after them.  We also use this to indicate
     * steps that need to do some aggregation or processing that requires the
     * full query result to be present in order for the step to work correctly.
     * This includes "range", "group", and "order", and "cap"
     */
    BARRIER,
}
