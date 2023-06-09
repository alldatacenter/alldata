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
package org.apache.drill.vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.exec.vector.NullableIntVector;
import org.apache.drill.exec.vector.NullableVarCharVector;
import org.apache.drill.exec.vector.VarCharVector;
import org.apache.drill.test.SubOperatorTest;
import org.bouncycastle.util.Arrays;
import org.junit.Test;

public class TestToNullable extends SubOperatorTest {

  @Test
  public void testFixedWidth() {
    MaterializedField intSchema =
        SchemaBuilder.columnSchema("a", MinorType.INT, DataMode.REQUIRED);
    @SuppressWarnings("resource")
    IntVector intVector = new IntVector(intSchema, fixture.allocator());
    IntVector.Mutator intMutator = intVector.getMutator();
    intVector.allocateNew(100);
    for (int i = 0; i < 100; i++) {
      intMutator.set(i, i * 10);
    }
    intMutator.setValueCount(100);

    MaterializedField nullableIntSchema =
        SchemaBuilder.columnSchema("a", MinorType.INT, DataMode.OPTIONAL);
    NullableIntVector nullableIntVector = new NullableIntVector(nullableIntSchema, fixture.allocator());

    intVector.toNullable(nullableIntVector);

    assertEquals(0, intVector.getAccessor().getValueCount());
    NullableIntVector.Accessor niAccessor = nullableIntVector.getAccessor();
    assertEquals(100, niAccessor.getValueCount());
    for (int i = 0; i < 100; i++) {
      assertFalse(niAccessor.isNull(i));
      assertEquals(i * 10, niAccessor.get(i));
    }

    nullableIntVector.clear();

    // Don't clear the intVector, it should be empty.
    // If it is not, the test will fail with a memory leak error.
  }

  @Test
  public void testNullable() {
    MaterializedField nullableIntSchema =
        SchemaBuilder.columnSchema("a", MinorType.INT, DataMode.OPTIONAL);
    @SuppressWarnings("resource")
    NullableIntVector sourceVector = new NullableIntVector(nullableIntSchema, fixture.allocator());
    NullableIntVector.Mutator sourceMutator = sourceVector.getMutator();
    sourceVector.allocateNew(100);
    for (int i = 0; i < 100; i++) {
      sourceMutator.set(i, i * 10);
    }
    sourceMutator.setValueCount(100);

    NullableIntVector destVector = new NullableIntVector(nullableIntSchema, fixture.allocator());

    sourceVector.toNullable(destVector);

    assertEquals(0, sourceVector.getAccessor().getValueCount());
    NullableIntVector.Accessor destAccessor = destVector.getAccessor();
    assertEquals(100, destAccessor.getValueCount());
    for (int i = 0; i < 100; i++) {
      assertFalse(destAccessor.isNull(i));
      assertEquals(i * 10, destAccessor.get(i));
    }

    destVector.clear();

    // Don't clear the intVector, it should be empty.
    // If it is not, the test will fail with a memory leak error.
  }

  @Test
  public void testVariableWidth() {
    MaterializedField nonNullableSchema =
        SchemaBuilder.columnSchema("a", MinorType.VARCHAR, DataMode.REQUIRED);
    @SuppressWarnings("resource")
    VarCharVector nonNullableVector = new VarCharVector(nonNullableSchema, fixture.allocator());
    VarCharVector.Mutator mutator = nonNullableVector.getMutator();
    nonNullableVector.allocateNew(100, 20);
    byte value[] = new byte[20];
    for (int i = 0; i < 100; i++) {
      Arrays.fill(value, (byte)('A' + i % 26));
      mutator.setSafe(i, value);
    }
    mutator.setValueCount(100);

    MaterializedField nullableVarCharSchema =
        SchemaBuilder.columnSchema("a", MinorType.VARCHAR, DataMode.OPTIONAL);
    NullableVarCharVector nullableVector = new NullableVarCharVector(nullableVarCharSchema, fixture.allocator());

    nonNullableVector.toNullable(nullableVector);

    assertEquals(0, nonNullableVector.getAccessor().getValueCount());
    NullableVarCharVector.Accessor nullableAccessor = nullableVector.getAccessor();
    assertEquals(100, nullableAccessor.getValueCount());
    for (int i = 0; i < 100; i++) {
      assertFalse(nullableAccessor.isNull(i));
      Arrays.fill(value, (byte)('A' + i % 26));
      assertTrue(Arrays.areEqual(value, nullableAccessor.get(i)));
    }

    nullableVector.clear();

    // Don't clear the nonNullableVector, it should be empty.
    // If it is not, the test will fail with a memory leak error.
  }
}
