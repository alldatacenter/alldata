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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.test.SubOperatorTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(RowSetTests.class)
public class TestResultVectorCache extends SubOperatorTest {

  @Test
  public void testIsPromotable() {

    final MaterializedField required = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.INT)
          .setMode(DataMode.REQUIRED)
          .build());

    // Type is promotable to itself

    assertTrue(required.isPromotableTo(required, true));
    assertTrue(required.isPromotableTo(required, false));

    // Required is promotable to null

    final MaterializedField nullable = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.INT)
          .setMode(DataMode.OPTIONAL)
          .build());

    assertTrue(required.isPromotableTo(nullable, true));
    assertFalse(required.isPromotableTo(nullable, false));

    // Nullable not promotable to required

    assertFalse(nullable.isPromotableTo(required, true));

    // Arrays cannot be promoted to/from other types

    final MaterializedField repeated = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.INT)
          .setMode(DataMode.REPEATED)
          .build());

    assertFalse(required.isPromotableTo(repeated, true));
    assertFalse(nullable.isPromotableTo(repeated, true));
    assertFalse(repeated.isPromotableTo(required, true));
    assertFalse(repeated.isPromotableTo(nullable, true));

    // Narrower precision promotable to wider

    final MaterializedField narrow = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.VARCHAR)
          .setMode(DataMode.REQUIRED)
          .setPrecision(10)
          .build());
    final MaterializedField wide = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.VARCHAR)
          .setMode(DataMode.REQUIRED)
          .setPrecision(20)
          .build());
    final MaterializedField unset = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.VARCHAR)
          .setMode(DataMode.REQUIRED)
          .build());

    assertTrue(narrow.isPromotableTo(wide, false));
    assertTrue(unset.isPromotableTo(narrow, false));
    assertTrue(unset.isPromotableTo(wide, false));
    assertFalse(wide.isPromotableTo(narrow, false));
    assertFalse(narrow.isPromotableTo(unset, false));
  }

  @Test
  public void testBasics() {
    final ResultVectorCache cache = new ResultVectorCacheImpl(fixture.allocator());

    // Create a vector

    final MaterializedField required = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.INT)
          .setMode(DataMode.REQUIRED)
          .build());
    final ValueVector vector1 = cache.vectorFor(required);
    assertTrue(vector1.getField().isEquivalent(required));

    // Request the same schema, should get the same vector.

    final ValueVector vector2 = cache.vectorFor(required);
    assertSame(vector1, vector2);

    // Non-permissive. Change in mode means different vector.

    final MaterializedField optional = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.INT)
          .setMode(DataMode.OPTIONAL)
          .build());
    final ValueVector vector3 = cache.vectorFor(optional);
    assertTrue(vector3.getField().isEquivalent(optional));
    assertNotSame(vector1, vector3);

    // Asking for the required type again produces a new vector.
    // Name is the key, and we can have only one type associated
    // with each name.

    final ValueVector vector4 = cache.vectorFor(required);
    assertTrue(vector4.getField().isEquivalent(required));
    assertNotSame(vector3, vector4);
    assertNotSame(vector1, vector4);

    // Varchar, no precision.

    final MaterializedField varchar1 = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.VARCHAR)
          .setMode(DataMode.REQUIRED)
          .build());

    final ValueVector vector5 = cache.vectorFor(varchar1);
    assertTrue(vector5.getField().isEquivalent(varchar1));

    // Varchar, with precision, no match.

    final MaterializedField varchar2 = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.VARCHAR)
          .setMode(DataMode.REQUIRED)
          .setPrecision(10)
          .build());

    final ValueVector vector6 = cache.vectorFor(varchar2);
    assertTrue(vector6.getField().isEquivalent(varchar2));
    assertNotSame(vector5, vector6);

    // Does match if same precision.

    final ValueVector vector7 = cache.vectorFor(varchar2);
    assertTrue(vector7.getField().isEquivalent(varchar2));
    assertSame(vector6, vector7);

    // Different names have different types

    final MaterializedField varchar3 = MaterializedField.create("b",
        MajorType.newBuilder()
          .setMinorType(MinorType.VARCHAR)
          .setMode(DataMode.REQUIRED)
          .setPrecision(10)
          .build());

    final ValueVector vector8 = cache.vectorFor(varchar3);
    assertTrue(vector8.getField().isEquivalent(varchar3));
    assertSame(vector7, cache.vectorFor(varchar2));
    assertSame(vector8, cache.vectorFor(varchar3));

    ((ResultVectorCacheImpl) cache).close();
  }

  @Test
  public void testPermissive() {
    final ResultVectorCache cache = new ResultVectorCacheImpl(fixture.allocator(), true);

    // Create a nullable vector

    final MaterializedField optional = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.INT)
          .setMode(DataMode.OPTIONAL)
          .build());
    final ValueVector vector1 = cache.vectorFor(optional);

    // Ask for a required version of the same name and type.
    // Should return the nullable version.

    final MaterializedField required = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.INT)
          .setMode(DataMode.REQUIRED)
          .build());
    final ValueVector vector2 = cache.vectorFor(required);
    assertTrue(vector2.getField().isEquivalent(optional));
    assertSame(vector1, vector2);

    // Repeat with Varchar

    final MaterializedField varchar1 = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.VARCHAR)
          .setMode(DataMode.OPTIONAL)
          .build());

    final ValueVector vector3 = cache.vectorFor(varchar1);

    final MaterializedField varchar2 = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.VARCHAR)
          .setMode(DataMode.REQUIRED)
          .build());

    final ValueVector vector4 = cache.vectorFor(varchar2);
    assertSame(vector3, vector4);

    // Larger precision. Needs new vector.

    final MaterializedField varchar3 = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.VARCHAR)
          .setMode(DataMode.OPTIONAL)
          .setPrecision(10)
          .build());

    final ValueVector vector5 = cache.vectorFor(varchar3);
    assertTrue(vector5.getField().isEquivalent(varchar3));
    assertNotSame(vector4, vector5);

    // Smaller precision, reuse vector.

    final ValueVector vector6 = cache.vectorFor(varchar1);
    assertTrue(vector6.getField().isEquivalent(varchar3));
    assertSame(vector5, vector6);

    // Same precision, required: reuse vector.

    final MaterializedField varchar4 = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.VARCHAR)
          .setMode(DataMode.REQUIRED)
          .setPrecision(5)
          .build());

    final ValueVector vector7 = cache.vectorFor(varchar4);
    assertTrue(vector7.getField().isEquivalent(varchar3));
    assertSame(vector5, vector7);

    // TODO: Repeat with decimal precision and scale.

    ((ResultVectorCacheImpl) cache).close();
  }

  @Test
  public void testClose() {
    final ResultVectorCache cache = new ResultVectorCacheImpl(fixture.allocator());

    // Create a vector

    final MaterializedField required = MaterializedField.create("a",
        MajorType.newBuilder()
          .setMinorType(MinorType.INT)
          .setMode(DataMode.REQUIRED)
          .build());
    final IntVector vector1 = (IntVector) cache.vectorFor(required);
    vector1.allocateNew(100);

    // Close the cache. Note: close is on the implementation, not
    // the interface, because only the implementation should decide
    // when to close.

    // Close should release the allocated vector. If not, then
    // this test suite will fail with a memory leak when shutting
    // down the root allocator.

    ((ResultVectorCacheImpl) cache).close();
    assertEquals(0, vector1.getBuffer().capacity());
  }
}
