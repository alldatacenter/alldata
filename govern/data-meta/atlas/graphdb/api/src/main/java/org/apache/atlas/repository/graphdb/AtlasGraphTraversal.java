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

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;


public abstract class AtlasGraphTraversal<V extends AtlasVertex, E extends AtlasEdge> extends DefaultGraphTraversal {

    protected AtlasGraph atlasGraph;

    // For anonymous/inner traversal
    public AtlasGraphTraversal() {
    }

    public AtlasGraphTraversal(final AtlasGraph atlasGraph, final Graph graph) {
        super(graph);
        this.atlasGraph = atlasGraph;
    }

    public AtlasGraphTraversal(final AtlasGraph atlasGraph, final GraphTraversalSource traversalSource) {
        super(traversalSource);
        this.atlasGraph = atlasGraph;
    }

    public abstract AtlasGraphTraversal startAnonymousTraversal();

    public abstract List<V> getAtlasVertexList();

    public abstract Set<V> getAtlasVertexSet();

    public abstract Map<String, Collection<V>> getAtlasVertexMap();

    public abstract Set<E> getAtlasEdgeSet();

    public abstract Map<String, E> getAtlasEdgeMap();

    public abstract TextPredicate textPredicate();

    public abstract AtlasGraphTraversal textRegEx(String key, String value);

    public abstract AtlasGraphTraversal textContainsRegEx(String value, String removeRedundantQuotes);

    public interface TextPredicate {

        /**
         * Whether the text contains a given term as a token in the text (case insensitive)
         */
        BiPredicate contains();

        /**
         * Whether the text contains a token that starts with a given term (case insensitive)
         */
        BiPredicate containsPrefix();
        /**
         * Whether the text contains a token that matches a regular expression
         */
        BiPredicate containsRegex();

        /**
         * Whether the text starts with a given prefix (case sensitive)
         */
        BiPredicate prefix();

        /**
         * Whether the text matches a regular expression (case sensitive)
         */
        BiPredicate regex();
    }
}
