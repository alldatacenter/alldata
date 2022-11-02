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

import java.util.Set;

/**
 * Represents a graph index on the database.
 */
public interface AtlasGraphIndex {

    /**
     * Indicates if the index is a mixed index.
     * @return
     */
    boolean isMixedIndex();


    /**
     * Indicates if the index is a composite index.
     * @return
     */
    boolean isCompositeIndex();

    /**
     * Indicates if the index applies to edges.
     *
     * @return
     */
    boolean isEdgeIndex();

    /**
     * Indicates if the index applies to vertices.
     *
     * @return
     */
    boolean isVertexIndex();



    boolean isUnique();

    Set<AtlasPropertyKey> getFieldKeys();

}
