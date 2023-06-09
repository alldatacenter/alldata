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
package org.apache.drill.exec.vector;

import org.apache.drill.exec.vector.complex.RepeatedFixedWidthVectorLike;
import org.apache.drill.exec.vector.complex.RepeatedVariableWidthVectorLike;

public class AllocationHelper {

  public static final int STD_REPETITION_FACTOR = 5;

  public static void allocate(ValueVector vector, int valueCount, int bytesPerValue) {
    allocate(vector, valueCount, bytesPerValue, STD_REPETITION_FACTOR);
  }

  public static void allocatePrecomputedChildCount(ValueVector vector, int valueCount, int bytesPerValue, int childValCount) {

    // Don't fail if given a 0-sized array. Assume at least one value.
    childValCount = Math.max(1, childValCount);
    valueCount = Math.max(1, valueCount);
    bytesPerValue = Math.max(1, bytesPerValue);

    if (vector instanceof FixedWidthVector) {
      ((FixedWidthVector) vector).allocateNew(valueCount);
    } else if (vector instanceof VariableWidthVector) {
      ((VariableWidthVector) vector).allocateNew(valueCount * bytesPerValue, valueCount);
    } else if (vector instanceof RepeatedFixedWidthVectorLike) {
      ((RepeatedFixedWidthVectorLike) vector).allocateNew(valueCount, childValCount);
    } else if (vector instanceof RepeatedVariableWidthVectorLike) {
      // Assertion thrown if byte count is zero in the full allocateNew,
      // so use default version instead.
      ((RepeatedVariableWidthVectorLike) vector).allocateNew(childValCount * bytesPerValue, valueCount, childValCount);
    } else {
      vector.allocateNew();
    }
  }

  public static void allocate(ValueVector vector, int valueCount, int bytesPerValue, int repeatedPerTop){
    allocatePrecomputedChildCount(vector, valueCount, bytesPerValue, repeatedPerTop * valueCount);
  }

  /**
   * Allocate an array, but with a fractional value for the number of elements
   * per array. This form is useful when the number comes from observations and
   * represents an average.
   *
   * @param vector the vector to allocate
   * @param valueCount the number of top-level values
   * @param bytesPerValue the width of each value
   * @param repeatedPerTop the number of array elements per value.
   */
  public static void allocate(ValueVector vector, int valueCount, int bytesPerValue, float repeatedPerTop){
    allocatePrecomputedChildCount(vector, valueCount, bytesPerValue,
        (int) Math.ceil(repeatedPerTop * valueCount));
  }

  /**
   * Allocates the exact amount if v is fixed width, otherwise falls back to dynamic allocation
   * @param vector value vector we are trying to allocate
   * @param valueCount  size we are trying to allocate
   * @throws org.apache.drill.exec.memory.OutOfMemoryException if it can't allocate the memory
   */
  public static void allocateNew(ValueVector vector, int valueCount) {
    if (vector instanceof  FixedWidthVector) {
      ((FixedWidthVector) vector).allocateNew(valueCount);
    } else {
      vector.allocateNew();
    }
  }
}
