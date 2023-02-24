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

import com.google.common.base.Preconditions;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasEdgeDirection;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.graphdb.AtlasVertexQuery;

import org.janusgraph.core.JanusGraphVertexQuery;

/**
 * Janus implementation of AtlasVertexQuery.
 */
public class AtlasJanusVertexQuery implements AtlasVertexQuery<AtlasJanusVertex, AtlasJanusEdge> {

    private AtlasJanusGraph graph;
    private JanusGraphVertexQuery<?> query;

    public AtlasJanusVertexQuery(AtlasJanusGraph graph, JanusGraphVertexQuery<?> query) {
        this.query = query;
        this.graph = graph;
    }

    @Override
    public AtlasVertexQuery<AtlasJanusVertex, AtlasJanusEdge> direction(AtlasEdgeDirection queryDirection) {
        query.direction(AtlasJanusObjectFactory.createDirection(queryDirection));
        return this;

    }

    @Override
    public Iterable<AtlasVertex<AtlasJanusVertex, AtlasJanusEdge>> vertices() {
        Iterable vertices = query.vertices();
        return graph.wrapVertices(vertices);
    }

    @Override
    public Iterable<AtlasVertex<AtlasJanusVertex, AtlasJanusEdge>> vertices(int limit) {
        Preconditions.checkArgument(limit >=0, "Limit should be greater than or equals to 0");
        Iterable vertices = query.limit(limit).vertices();
        return graph.wrapVertices(vertices);
    }

    @Override
    public Iterable<AtlasEdge<AtlasJanusVertex, AtlasJanusEdge>> edges() {
        Iterable edges = query.edges();
        return graph.wrapEdges(edges);
    }

    @Override
    public Iterable<AtlasEdge<AtlasJanusVertex, AtlasJanusEdge>> edges(int limit) {
        Preconditions.checkArgument(limit >=0, "Limit should be greater than or equals to 0");
        Iterable edges = query.limit(limit).edges();
        return graph.wrapEdges(edges);
    }

    @Override
    public long count() {
        return query.count();
    }

    @Override
    public AtlasVertexQuery<AtlasJanusVertex, AtlasJanusEdge> label(String label) {
        query.labels(label);
        return this;
    }

    @Override
    public AtlasVertexQuery<AtlasJanusVertex, AtlasJanusEdge> has(String key, Object value) {
        query.has(key, value);
        return this;
    }
}
