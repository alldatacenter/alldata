/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.graphdb;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;

public interface AtlasGraphTraversalSource<V extends AtlasVertex, E extends AtlasEdge> {

    // Concrete implementations need to have graph and graphTraversal source
    Graph getGraph();

    GraphTraversalSource getGraphTraversalSource();

    AtlasGraphTraversal<V, E> startAnonymousTraversal();

    AtlasGraphTraversal<V, E> V(final Object... vertexIds);

    AtlasGraphTraversal<V, E> E(final Object... edgesIds);

    Transaction tx();

    void close() throws Exception;
}
