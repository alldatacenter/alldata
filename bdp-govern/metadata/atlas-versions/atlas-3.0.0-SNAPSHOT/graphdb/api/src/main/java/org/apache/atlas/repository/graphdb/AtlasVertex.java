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
 * Represents a Vertex.
 *
 * @param <V> vertex class used by the graph
 * @param <E> edge class used by the graph
 */
public interface AtlasVertex<V, E> extends AtlasElement {

    /**
     * Gets the edges incident to this vertex going the
     * specified direction that have the specified edgeLabel.  If
     * the edgeLabel is null, it is ignored.
     *
     * @param in
     * @return
     */
    Iterable<AtlasEdge<V, E>> getEdges(AtlasEdgeDirection out, String edgeLabel);

    /**
     * Gets the edges associated with this vertex going the
     * specified direction that have the specified edgeLabels.
     *
     * @param direction
     * @param edgeLabels
     * @return
     */
    Iterable<AtlasEdge<V, E>> getEdges(AtlasEdgeDirection direction, String[] edgeLabels);

    long getEdgesCount(AtlasEdgeDirection direction, String edgeLabel);

    /**
     * Does vertex have edges specified by the direction and label
     * @param dir
     * @param edgeLabel
     * @return
     */
    boolean hasEdges(AtlasEdgeDirection dir, String edgeLabel);

    /**
     * Gets the edges associated with this vertex going the
     * specified direction.
     *
     * @param in
     * @return
     */
    Iterable<AtlasEdge<V, E>> getEdges(AtlasEdgeDirection in);

    /**
     * Adds a value to a multiplicity many property.  Follows Java set
     * semantics.  If the property is already present, it is not added again,
     * and no exception is thrown.
     *
     *
     * @param propertyName
     * @param value
     */
    <T> void addProperty(String propertyName, T value);

    /**
     * Adds a value to a multiplicity-many property.
     * If the property is already present, the value is added to it; if not, the propery is set with the given value
     *
     * @param propertyName
     * @param value
     */
    <T> void addListProperty(String propertyName, T value);

    /**
     * Creates a vertex query.
     * @return
     */
    AtlasVertexQuery<V, E> query();

    /**
     * Syntactic sugar to get the vertex as an instance of its
     * implementation type.  This allows the graph database implementation
     * code to be strongly typed.
     *
     * @return
     */
    V getV();
}
