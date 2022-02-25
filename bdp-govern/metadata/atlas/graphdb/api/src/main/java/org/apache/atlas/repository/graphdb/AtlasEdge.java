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

package org.apache.atlas.repository.graphdb;

/**
 * Represent an edge in the graph.
 *
 * @param <V> vertex class used by the graph
 * @param <E> edge class used by the graph
 */
public interface AtlasEdge<V, E> extends AtlasElement {

    /**
     * Gets the incoming vertex for this edge.
     * @param in
     * @return
     */
    AtlasVertex<V, E> getInVertex();

    /**
     * Gets the outgoing vertex for this edge.
     *
     * @param in
     * @return
     */
    AtlasVertex<V, E> getOutVertex();

    /**
     * Gets the label associated with this edge.
     *
     * @return
     */
    String getLabel();

    /**
     * Converts the edge to an instance of the underlying implementation class.  This
     * is syntactic sugar that allows the graph database implementation code to be strongly typed.  This
     * should not be called in other places.
     *
     * @return
     */
    E getE();

}
