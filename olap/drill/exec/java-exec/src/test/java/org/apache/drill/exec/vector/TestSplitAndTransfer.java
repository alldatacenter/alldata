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

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.vector.NullableVarCharVector.Accessor;
import org.apache.drill.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;


public class TestSplitAndTransfer extends BaseTest {
  @Test
  public void test() throws Exception {
    final DrillConfig drillConfig = DrillConfig.create();
    final BufferAllocator allocator = RootAllocatorFactory.newRoot(drillConfig);
    final MaterializedField field = MaterializedField.create("field", Types.optional(MinorType.VARCHAR));
    final NullableVarCharVector varCharVector = new NullableVarCharVector(field, allocator);
    varCharVector.allocateNew(10000, 1000);

    final int valueCount = 500;
    final String[] compareArray = new String[valueCount];

    final NullableVarCharVector.Mutator mutator = varCharVector.getMutator();
    for (int i = 0; i < valueCount; i += 3) {
      final String s = String.format("%010d", i);
      mutator.set(i, s.getBytes());
      compareArray[i] = s;
    }
    mutator.setValueCount(valueCount);

    final TransferPair tp = varCharVector.getTransferPair(allocator);
    final NullableVarCharVector newVarCharVector = (NullableVarCharVector) tp.getTo();
    final Accessor accessor = newVarCharVector.getAccessor();
    final int[][] startLengths = {{0, 201}, {201, 200}, {401, 99}};

    for (final int[] startLength : startLengths) {
      final int start = startLength[0];
      final int length = startLength[1];
      tp.splitAndTransfer(start, length);
      newVarCharVector.getMutator().setValueCount(length);
      for (int i = 0; i < length; i++) {
        final boolean expectedSet = ((start + i) % 3) == 0;
        if (expectedSet) {
          final byte[] expectedValue = compareArray[start + i].getBytes();
          assertFalse(accessor.isNull(i));
          assertArrayEquals(expectedValue, accessor.get(i));
        } else {
          assertTrue(accessor.isNull(i));
        }
      }
      newVarCharVector.clear();
    }

    varCharVector.close();
    allocator.close();
  }

  /**
   *  BitVector tests
   */

  enum TestBitPattern {
    ZERO,
    ONE,
    ALTERNATING,
    RANDOM
  }

  @Test
  public void testBitVectorUnalignedStart() throws Exception {

    testBitVectorImpl(16, new int[][] {{2, 4}}, TestBitPattern.RANDOM);
    testBitVectorImpl(16, new int[][] {{2, 4}}, TestBitPattern.ONE);
    testBitVectorImpl(16, new int[][] {{2, 4}}, TestBitPattern.ZERO);
    testBitVectorImpl(16, new int[][] {{2, 4}}, TestBitPattern.ALTERNATING);

    testBitVectorImpl(4096, new int[][] {{4092, 4}}, TestBitPattern.ONE);
    testBitVectorImpl(4096, new int[][] {{4092, 4}}, TestBitPattern.ZERO);
    testBitVectorImpl(4096, new int[][] {{4092, 4}}, TestBitPattern.ALTERNATING);
    testBitVectorImpl(4096, new int[][] {{4092, 4}}, TestBitPattern.RANDOM);

    testBitVectorImpl(4096, new int[][] {{1020, 8}}, TestBitPattern.ONE);
    testBitVectorImpl(4096, new int[][] {{1020, 8}}, TestBitPattern.ZERO);
    testBitVectorImpl(4096, new int[][] {{1020, 8}}, TestBitPattern.ALTERNATING);
    testBitVectorImpl(4096, new int[][] {{1020, 8}}, TestBitPattern.RANDOM);

    testBitVectorImpl(24, new int[][] {{5, 17}}, TestBitPattern.ONE);
    testBitVectorImpl(24, new int[][] {{5, 17}}, TestBitPattern.ZERO);
    testBitVectorImpl(24, new int[][] {{5, 17}}, TestBitPattern.ALTERNATING);
    testBitVectorImpl(24, new int[][] {{5, 17}}, TestBitPattern.RANDOM);

    testBitVectorImpl(3443, new int[][] {{0, 2047}, {2047, 1396}}, TestBitPattern.ZERO);
    testBitVectorImpl(3443, new int[][] {{0, 2047}, {2047, 1396}}, TestBitPattern.ONE);
    testBitVectorImpl(3443, new int[][] {{0, 2047}, {2047, 1396}}, TestBitPattern.ALTERNATING);
    testBitVectorImpl(3443, new int[][] {{0, 2047}, {2047, 1396}}, TestBitPattern.RANDOM);

    testBitVectorImpl(3447, new int[][] {{0, 2047}, {2047, 1400}}, TestBitPattern.ZERO);
    testBitVectorImpl(3447, new int[][] {{0, 2047}, {2047, 1400}}, TestBitPattern.ONE);
    testBitVectorImpl(3447, new int[][] {{0, 2047}, {2047, 1400}}, TestBitPattern.ALTERNATING);
    testBitVectorImpl(3447, new int[][] {{0, 2047}, {2047, 1400}}, TestBitPattern.RANDOM);
  }

  @Test
  public void testBitVectorAlignedStart() throws Exception {

    testBitVectorImpl(32, new int[][] {{0, 4}}, TestBitPattern.RANDOM);
    testBitVectorImpl(32, new int[][] {{0, 4}}, TestBitPattern.ONE);
    testBitVectorImpl(32, new int[][] {{0, 4}}, TestBitPattern.ZERO);
    testBitVectorImpl(32, new int[][] {{0, 4}}, TestBitPattern.ALTERNATING);


    testBitVectorImpl(32, new int[][] {{0, 8}}, TestBitPattern.ONE);
    testBitVectorImpl(32, new int[][] {{0, 8}}, TestBitPattern.ZERO);
    testBitVectorImpl(32, new int[][] {{0, 8}}, TestBitPattern.ALTERNATING);
    testBitVectorImpl(32, new int[][] {{0, 8}}, TestBitPattern.RANDOM);

    testBitVectorImpl(24, new int[][] {{0, 17}}, TestBitPattern.ONE);
    testBitVectorImpl(24, new int[][] {{0, 17}}, TestBitPattern.ZERO);
    testBitVectorImpl(24, new int[][] {{0, 17}}, TestBitPattern.ALTERNATING);
    testBitVectorImpl(24, new int[][] {{0, 17}}, TestBitPattern.RANDOM);

    testBitVectorImpl(3444, new int[][] {{0, 2048}, {2048, 1396}}, TestBitPattern.ZERO);
    testBitVectorImpl(3444, new int[][] {{0, 2048}, {2048, 1396}}, TestBitPattern.ONE);
    testBitVectorImpl(3444, new int[][] {{0, 2048}, {2048, 1396}}, TestBitPattern.ALTERNATING);
    testBitVectorImpl(3444, new int[][] {{0, 2048}, {2048, 1396}}, TestBitPattern.RANDOM);

    testBitVectorImpl(3448, new int[][] {{0, 2048}, {2048, 1400}}, TestBitPattern.ZERO);
    testBitVectorImpl(3448, new int[][] {{0, 2048}, {2048, 1400}}, TestBitPattern.ONE);
    testBitVectorImpl(3448, new int[][] {{0, 2048}, {2048, 1400}}, TestBitPattern.ALTERNATING);
    testBitVectorImpl(3448, new int[][] {{0, 2048}, {2048, 1400}}, TestBitPattern.RANDOM);
  }

  int getBit(TestBitPattern pattern, int index) {
    if (pattern == TestBitPattern.RANDOM) {
      return (int) (Math.random() * 2);
    }
    return (pattern == TestBitPattern.ALTERNATING) ? (index % 2) : ((pattern == TestBitPattern.ONE) ? 1 : 0);
  }

  public void testBitVectorImpl(int valueCount, final int[][] startLengths, TestBitPattern pattern) throws Exception {
    final DrillConfig drillConfig = DrillConfig.create();
    final BufferAllocator allocator = RootAllocatorFactory.newRoot(drillConfig);
    final MaterializedField field = MaterializedField.create("field", Types.optional(MinorType.BIT));
    final BitVector bitVector = new BitVector(field, allocator);
    bitVector.allocateNew(valueCount  + 8); // extra byte at the end that gets filled with junk
    final int[] compareArray = new int[valueCount];

    int testBitValue = 0;
    final BitVector.Mutator mutator = bitVector.getMutator();
    for (int i = 0; i < valueCount; i++) {
      testBitValue = getBit(pattern, i);
      mutator.set(i, testBitValue);
      compareArray[i] = testBitValue;
    }

    // write some junk value at the end to catch
    // off-by-one out-of-bound reads
    for (int j = valueCount; j < valueCount + 8; j++) {
      mutator.set(j, ~testBitValue); // fill with compliment of testBit
    }
    mutator.setValueCount(valueCount);

    final TransferPair tp = bitVector.getTransferPair(allocator);
    final BitVector newBitVector = (BitVector) tp.getTo();
    final BitVector.Accessor accessor = newBitVector.getAccessor();

    for (final int[] startLength : startLengths) {
      final int start = startLength[0];
      final int length = startLength[1];
      tp.splitAndTransfer(start, length);
      assertEquals(newBitVector.getAccessor().getValueCount(), length);
      for (int i = 0; i < length; i++) {
        final int expectedValue = compareArray[start + i];
        assertEquals(expectedValue, accessor.get(i));
      }
      newBitVector.clear();
    }
    bitVector.close();
    allocator.close();
  }
}
