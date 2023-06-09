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
package org.apache.drill.exec.util;


import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.util.Arrays;

/**
 * Simple Map type data structure for storing entries of (int -> int) mappings where the max key value is below 2^16
 * to avoid hashing keys and use direct array index reference for retrieving the values. Not thread-safe. Keys and
 * values are expected to be >=0.
 */
public class ArrayWrappedIntIntMap {
  private static final int MAX_KEY_VALUE = 1 << 16 - 1;
  private static final int INITIAL_MAX_KEY_VALUE = 1 << 8 - 1;
  private int[] values;

  public ArrayWrappedIntIntMap() {
    values = new int[INITIAL_MAX_KEY_VALUE + 1];
    Arrays.fill(values, Integer.MIN_VALUE);
  }

  public void put(final int key, final int value) {
    Preconditions.checkArgument(key >= 0 && key <= MAX_KEY_VALUE,
        String.format("Index should be in range [0, %d], given [%d].", MAX_KEY_VALUE, key));
    Preconditions.checkArgument(value >= 0, String.format("Value must be non-negative, given [%d]", value));

    // resize the values array if the index falls beyond the current size of the array
    if (values.length < key + 1) {
      // Make the new size the next power of 2 number after the given index number
      int newValuesLength = Integer.highestOneBit(key) * 2;
      int[] newValues = Arrays.copyOf(values, newValuesLength);
      Arrays.fill(newValues, values.length, newValues.length - 1, Integer.MIN_VALUE);
      values = newValues;
    }

    values[key] = value;
  }

  /**
   * Returns the value pointed by the given index.
   * If the value is not set through put() it either returns Integer.MIN_VALUE or throws ArrayIndexOutOfBounds
   * exception. Error checking is not done for faster retrieval.
   */
  public int get(int key) {
    return values[key];
  }
}
