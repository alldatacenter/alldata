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

package org.apache.atlas.repository.graphdb.janus;

import org.janusgraph.core.EdgeLabel;
import org.apache.atlas.repository.graphdb.AtlasCardinality;
import org.apache.atlas.repository.graphdb.AtlasGraphIndex;
import org.apache.atlas.repository.graphdb.janus.query.AtlasJanusGraphQuery;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import org.janusgraph.core.Cardinality;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.schema.JanusGraphIndex;


/**
 * Factory that serves up instances of graph database abstraction layer classes
 * that correspond to Janus/Tinkerpop3 classes.
 */
public final class GraphDbObjectFactory {

    private GraphDbObjectFactory() {

    }

    /**
     * Creates an AtlasJanusEdge that corresponds to the given Gremlin Edge.
     *
     * @param graph The graph the edge should be created in
     * @param source The gremlin edge
     */
    public static AtlasJanusEdge createEdge(AtlasJanusGraph graph, Edge source) {

        if (source == null) {
            return null;
        }
        return new AtlasJanusEdge(graph, source);
    }

    /**
     * Creates a AtlasJanusGraphQuery that corresponds to the given GraphQuery.
     *
     * @param graph the graph that is being quried
     */
    public static AtlasJanusGraphQuery createQuery(AtlasJanusGraph graph, boolean isChildQuery) {

        return new AtlasJanusGraphQuery(graph, isChildQuery);
    }

    /**
     * Creates an AtlasJanusVertex that corresponds to the given Gremlin Vertex.
     *
     * @param graph The graph that contains the vertex
     * @param source the Gremlin vertex
     */
    public static AtlasJanusVertex createVertex(AtlasJanusGraph graph, Vertex source) {

        if (source == null) {
            return null;
        }
        return new AtlasJanusVertex(graph, source);
    }

    /**
     * @param propertyKey The Gremlin propertyKey.
     *
     */
    public static AtlasJanusPropertyKey createPropertyKey(PropertyKey propertyKey) {
        if (propertyKey == null) {
            return null;
        }
        return new AtlasJanusPropertyKey(propertyKey);
    }

    /**
     * @param label The Gremlin propertyKey.
     *
     */
    public static AtlasJanusEdgeLabel createEdgeLabel(EdgeLabel label) {
        if (label == null) {
            return null;
        }
        return new AtlasJanusEdgeLabel(label);
    }

    /**
     * @param index The gremlin index.
     * @return
     */
    public static AtlasGraphIndex createGraphIndex(JanusGraphIndex index) {
        if (index == null) {
            return null;
        }
        return new AtlasJanusGraphIndex(index);
    }

    /**
     * Converts a Multiplicity to a Cardinality.
     *
     * @param cardinality
     * @return
     */
    public static AtlasCardinality createCardinality(Cardinality cardinality) {

        if (cardinality == Cardinality.SINGLE) {
            return AtlasCardinality.SINGLE;
        } else if (cardinality == Cardinality.LIST) {
            return AtlasCardinality.LIST;
        }
        return AtlasCardinality.SET;
    }

}
