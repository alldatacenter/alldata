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
 * Represents a graph database.
 *
 * @param <V> vertex class used by the graph database
 * @param <E> edge class used by the graph database
 */
public interface GraphDatabase<V, E> {

    /**
     * Returns whether the graph has been loaded.
     * @return
     */
    boolean isGraphLoaded();

    /**
     * Gets the graph, loading it if it has not been loaded already.
     * @return
     */
    AtlasGraph<V, E> getGraph();

    /*
     * Get graph initialized for bulk loading. This instance is used for high-performance ingest.
     */
    AtlasGraph<V, E> getGraphBulkLoading();

    /**
     * Sets things up so that getGraph() will return a graph that can be used for running
     * tests.
     */
    void initializeTestGraph();

    /**
     * Removes the test graph that was created.
     */
    void cleanup();
}
