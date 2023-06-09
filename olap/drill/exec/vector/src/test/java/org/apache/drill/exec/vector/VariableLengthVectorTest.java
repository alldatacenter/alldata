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

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.memory.RootAllocator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * This test uses {@link VarCharVector} to test the template code in VariableLengthVector.
 */
public class VariableLengthVectorTest extends BaseTest {
  /**
   * If the vector contains 1000 records, setting a value count of 1000 should work.
   */
  @Test
  public void testSettingSameValueCount()
  {
    try (RootAllocator allocator = new RootAllocator(10_000_000)) {
      MaterializedField field = MaterializedField.create("stringCol", Types.required(TypeProtos.MinorType.VARCHAR));
      @SuppressWarnings("resource")
      VarCharVector vector = new VarCharVector(field, allocator);

      vector.allocateNew();

      try {
        int size = 1000;
        VarCharVector.Mutator mutator = vector.getMutator();
        VarCharVector.Accessor accessor = vector.getAccessor();

        setSafeIndexStrings("", 0, size, mutator);

        mutator.setValueCount(size);
        Assert.assertEquals(size, accessor.getValueCount());
        checkIndexStrings("", 0, size, accessor);
      } finally {
        vector.clear();
      }
    }
  }

  /**
   * Test truncating data. If you have 10000 records, reduce the vector to 1000 records.
   */
  @Test
  public void testTrunicateVectorSetValueCount()
  {
    try (RootAllocator allocator = new RootAllocator(10_000_000)) {
      MaterializedField field = MaterializedField.create("stringCol", Types.required(TypeProtos.MinorType.VARCHAR));
      @SuppressWarnings("resource")
      VarCharVector vector = new VarCharVector(field, allocator);

      vector.allocateNew();

      try {
        int size = 1000;
        int fluffSize = 10000;
        VarCharVector.Mutator mutator = vector.getMutator();
        VarCharVector.Accessor accessor = vector.getAccessor();

        setSafeIndexStrings("", 0, size, mutator);
        setSafeIndexStrings("first cut ", size, fluffSize, mutator);

        mutator.setValueCount(fluffSize);
        Assert.assertEquals(fluffSize, accessor.getValueCount());

        checkIndexStrings("", 0, size, accessor);

      } finally {
        vector.clear();
      }
    }
  }

  @Test
  public void testDRILL7341() {
    try (RootAllocator allocator = new RootAllocator(10_000_000)) {
      MaterializedField field = MaterializedField.create("stringCol", Types.optional(TypeProtos.MinorType.VARCHAR));
      NullableVarCharVector sourceVector = new NullableVarCharVector(field, allocator);
      @SuppressWarnings("resource")
      NullableVarCharVector targetVector = new NullableVarCharVector(field, allocator);

      sourceVector.allocateNew();
      targetVector.allocateNew();

      try {
        NullableVarCharVector.Mutator sourceMutator = sourceVector.getMutator();
        sourceMutator.setValueCount(sourceVector.getValueCapacity() * 4);

        targetVector.exchange(sourceVector);
        NullableVarCharVector.Mutator targetMutator = targetVector.getMutator();
        targetMutator.setValueCount(targetVector.getValueCapacity() * 2);
      } finally {
        sourceVector.clear();
        targetVector.clear();
      }
    }
  }

  /**
   * Set 10000 values. Then go back and set new values starting at the 1001 the record.
   */
  @Test
  public void testSetBackTracking()
  {
    try (RootAllocator allocator = new RootAllocator(10_000_000)) {
      MaterializedField field = MaterializedField.create("stringCol", Types.required(TypeProtos.MinorType.VARCHAR));
      @SuppressWarnings("resource")
      VarCharVector vector = new VarCharVector(field, allocator);

      vector.allocateNew();

      try {
        int size = 1000;
        int fluffSize = 10000;
        VarCharVector.Mutator mutator = vector.getMutator();
        VarCharVector.Accessor accessor = vector.getAccessor();

        setSafeIndexStrings("", 0, size, mutator);
        setSafeIndexStrings("first cut ", size, fluffSize, mutator);
        setSafeIndexStrings("redone cut ", size, fluffSize, mutator);

        mutator.setValueCount(fluffSize);
        Assert.assertEquals(fluffSize, accessor.getValueCount());

        checkIndexStrings("", 0, size, accessor);
        checkIndexStrings("redone cut ", size, fluffSize, accessor);

      } finally {
        vector.clear();
      }
    }
  }

  public static void setSafeIndexStrings(String prefix, int offset, int size, VarCharVector.Mutator mutator)
  {
    for (int index = offset; index < size; index++) {
      String indexString = prefix + "String num " + index;
      mutator.setSafe(index, indexString.getBytes());
    }
  }

  public static void checkIndexStrings(String prefix, int offset, int size, VarCharVector.Accessor accessor)
  {
    for (int index = offset; index < size; index++) {
      String indexString = prefix + "String num " + index;
      Assert.assertArrayEquals(indexString.getBytes(), accessor.get(index));
    }
  }
}
