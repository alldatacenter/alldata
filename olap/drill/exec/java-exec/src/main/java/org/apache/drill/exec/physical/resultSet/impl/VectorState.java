/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.physical.resultSet.impl;

import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.impl.HierarchicalFormatter;

/**
 * Handles batch and overflow operation for a (possibly compound) vector.
 * <p>
 * The data model is the following:
 * <ul>
 * <li>Column model<ul>
 *   <li>Value vector itself</li>
 *   <li>Column writer</li>
 *   <li>Column schema</li>
 *   <li>Column coordinator (this class)</li>
 * </ul></li></ul>
 * The vector state coordinates events between the result set loader
 * on the one side and the vectors, writers and schema on the other.
 * For example:
 * <pre><code>
 * Result Set       Vector
 *   Loader   <-->  State   <-->    Vectors
 * </code></pre>
 * Events from the row set loader deal with allocation, roll-over,
 * harvesting completed batches and so on. Events from the writer,
 * via the tuple model deal with adding columns and column
 * overflow.
 */

public interface VectorState {

  /**
   * Allocate a new vector with the number of elements given. If the vector
   * is an array, then the cardinality given is the number of arrays.
   * @param cardinality number of elements desired in the allocated
   * vector
   *
   * @return the number of bytes allocated
   */

  int allocate(int cardinality);

  /**
   * A vector has overflowed. Create a new look-ahead vector of the given
   * cardinality, then copy the overflow values from the main vector to the
   * look-ahead vector.
   *
   * @param cardinality the number of elements in the new vector. If this
   * vector is an array, then this is the number of arrays
   * @return the new next write position for the vector index associated
   * with the writer for this vector
   */

  void rollover(int cardinality);

  /**
   * A batch is being harvested after an overflow. Put the full batch
   * back into the main vector so it can be harvested.
   */

  void harvestWithLookAhead();

  /**
   * A new batch is starting while an look-ahead vector exists. Move
   * the look-ahead buffers into the main vector to prepare for writing
   * the rest of the batch.
   */

  void startBatchWithLookAhead();

  /**
   * Clear the vector(s) associated with this state.
   */

  void close();

  /**
   * Underlying vector: the one presented to the consumer of the
   * result set loader.
   */

  <T extends ValueVector> T vector();

  /**
   * Report whether this column is projected (has materialized vectors),
   * or is unprojected (has no materialized backing.)
   *
   * @return true if the column is projected to the output, false if
   * not
   */

  boolean isProjected();

  void dump(HierarchicalFormatter format);
}
