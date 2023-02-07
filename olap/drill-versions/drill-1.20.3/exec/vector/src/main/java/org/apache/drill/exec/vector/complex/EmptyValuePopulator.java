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
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Tracks and populate empty values in repeated value vectors.
 */
public class EmptyValuePopulator {
  private final UInt4Vector offsets;

  public EmptyValuePopulator(UInt4Vector offsets) {
    this.offsets = Preconditions.checkNotNull(offsets, "offsets cannot be null");
  }

  /**
   * Marks all values since the last set as empty. The last set value is
   * obtained from underlying offsets vector.
   *
   * @param valueCount
   *          the last index (inclusive) in the offsets vector until which empty
   *          population takes place. Given in record (not offset) units
   * @throws java.lang.IndexOutOfBoundsException
   *           if lastIndex is negative or greater than offsets capacity.
   */
  public void populate(int valueCount) {
    Preconditions.checkElementIndex(valueCount, Integer.MAX_VALUE);
    UInt4Vector.Accessor accessor = offsets.getAccessor();
    // currentCount is offset counts
    int currentCount = accessor.getValueCount();
    UInt4Vector.Mutator mutator = offsets.getMutator();
    if (currentCount == 0) {
      // Fill in the implicit zero position.

      mutator.setSafe(0, 0);
      currentCount = 1;
    }
    int previousEnd = accessor.get(currentCount - 1);
    int offsetCount = valueCount + 1;
    // Indexes are in offset locations, 1 greater than record indexes
    for (int i = currentCount; i < offsetCount; i++) {
      mutator.setSafe(i, previousEnd);
    }
    mutator.setValueCount(offsetCount);
  }
}
