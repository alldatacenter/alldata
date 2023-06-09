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
package org.apache.drill.exec.vector.complex;

import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.ValueVector;

/**
 * Represents repeated (AKA "array") value vectors.
 * <p>
 * A repeated vector contains values that may either be flat or nested. A value
 * consists of zero or more cells(inner values). Current design maintains data
 * and offsets vectors. Each cell is stored in the data vector. Repeated vector
 * uses the offset vector to determine the sequence of cells pertaining to an
 * individual value.
 */

public interface RepeatedValueVector extends ValueVector, ContainerVectorLike {

  int DEFAULT_REPEAT_PER_RECORD = 5;

  /**
   * @return the underlying offset vector or null if none exists.
   */

  UInt4Vector getOffsetVector();

  /**
   * @return the underlying data vector or null if none exists.
   */
  ValueVector getDataVector();

  @Override
  RepeatedAccessor getAccessor();

  @Override
  RepeatedMutator getMutator();

  interface RepeatedAccessor extends ValueVector.Accessor {
    /**
     * @return total number of cells that vector contains.
     * The result includes empty, null valued cells.
     */
    int getInnerValueCount();

    /**
     * @return number of cells that the value at the given index contains.
     */
    int getInnerValueCountAt(int index);

    /**
     * @param index  value index
     * @return true if the value at the given index is empty, false otherwise.
     */
    boolean isEmpty(int index);
  }

  interface RepeatedMutator extends ValueVector.Mutator {
    /**
     * Starts a new value that is a container of cells.
     *
     * @param index  index of new value to start
     */
    void startNewValue(int index);
  }
}
