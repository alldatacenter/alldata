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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.StreamSupport;

import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasEdgeDirection;
import org.apache.atlas.repository.graphdb.AtlasSchemaViolationException;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.graphdb.AtlasVertexQuery;
import org.apache.atlas.repository.graphdb.utils.IteratorToIterableAdapter;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import org.janusgraph.core.SchemaViolationException;
import org.janusgraph.core.JanusGraphVertex;
/**
 * Janus implementation of AtlasVertex.
 */
public class AtlasJanusVertex extends AtlasJanusElement<Vertex> implements AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> {


    public AtlasJanusVertex(AtlasJanusGraph graph, Vertex source) {
        super(graph, source);
    }

    @Override
    public <T> void addProperty(String propertyName, T value) {
        try {
            getWrappedElement().property(VertexProperty.Cardinality.set, propertyName, value);
        } catch(SchemaViolationException e) {
            throw new AtlasSchemaViolationException(e);
        }
    }

    @Override
    public <T> void addListProperty(String propertyName, T value) {
        try {
            getWrappedElement().property(VertexProperty.Cardinality.list, propertyName, value);
        } catch(SchemaViolationException e) {
            throw new AtlasSchemaViolationException(e);
        }
    }


    @Override
    public Iterable<AtlasEdge<AtlasJanusVertex, AtlasJanusEdge>> getEdges(AtlasEdgeDirection dir, String edgeLabel) {

        Direction d = AtlasJanusObjectFactory.createDirection(dir);
        Iterator<Edge> edges = getWrappedElement().edges(d, edgeLabel);
        return graph.wrapEdges(edges);
    }

    @Override
    public Iterable<AtlasEdge<AtlasJanusVertex, AtlasJanusEdge>> getEdges(AtlasEdgeDirection dir, String[] edgeLabels) {
        Direction      direction = AtlasJanusObjectFactory.createDirection(dir);
        Iterator<Edge> edges     = getWrappedElement().edges(direction, edgeLabels);

        return graph.wrapEdges(edges);
    }

    @Override
    public long getEdgesCount(AtlasEdgeDirection dir, String edgeLabel) {
        Direction      direction = AtlasJanusObjectFactory.createDirection(dir);
        Iterator<Edge> it     = getWrappedElement().edges(direction, edgeLabel);
        IteratorToIterableAdapter<Edge> iterable = new IteratorToIterableAdapter<>(it);
        return StreamSupport.stream(iterable.spliterator(), true).count();
    }

    @Override
    public boolean hasEdges(AtlasEdgeDirection dir, String edgeLabel) {
        Direction      direction = AtlasJanusObjectFactory.createDirection(dir);
        Iterator<Edge> edges     = getWrappedElement().edges(direction, edgeLabel);
        return edges.hasNext();
    }

    private JanusGraphVertex getAsJanusVertex() {
        return (JanusGraphVertex)getWrappedElement();
    }

    @Override
    public Iterable<AtlasEdge<AtlasJanusVertex, AtlasJanusEdge>> getEdges(AtlasEdgeDirection in) {
        Direction d = AtlasJanusObjectFactory.createDirection(in);
        Iterator<Edge> edges = getWrappedElement().edges(d);
        return graph.wrapEdges(edges);
    }

    @Override
    public <T> Collection<T> getPropertyValues(String propertyName, Class<T> clazz) {

        Collection<T> result = new ArrayList<T>();
        Iterator<VertexProperty<T>> it = getWrappedElement().properties(propertyName);
        while(it.hasNext()) {
            result.add(it.next().value());
        }

        return result;
    }

    @Override
    public AtlasVertexQuery<AtlasJanusVertex, AtlasJanusEdge> query() {

        return new AtlasJanusVertexQuery(graph, getAsJanusVertex().query());
    }


    @Override
    public AtlasJanusVertex getV() {
        return this;
    }



}
