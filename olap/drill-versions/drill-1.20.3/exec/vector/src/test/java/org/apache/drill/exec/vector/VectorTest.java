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

import static org.junit.Assert.assertEquals;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.memory.RootAllocator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.vector.complex.EmptyValuePopulator;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.exec.vector.complex.RepeatedMapVector;
import org.apache.drill.exec.vector.complex.impl.NullableVarCharWriterImpl;
import org.apache.drill.exec.vector.complex.impl.SingleMapWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ListWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter;
import org.apache.drill.exec.vector.complex.writer.FieldWriter;
import org.apache.drill.exec.vector.complex.writer.IntWriter;
import org.apache.drill.test.BaseTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.buffer.DrillBuf;

public class VectorTest extends BaseTest {

  private static RootAllocator allocator;

  @BeforeClass
  public static void setup() {
    allocator = new RootAllocator(10_000_000);
  }

  @AfterClass
  public static void tearDown() {
    allocator.close();
  }

  @Test
  public void testVarChar() {
    MaterializedField field = MaterializedField.create("stringCol", Types.required(TypeProtos.MinorType.VARCHAR));
    try (VarCharVector v = new VarCharVector(field, allocator)) {
      v.allocateNew(1000, 128);
      VarCharVector.Accessor va = v.getAccessor();
      UInt4Vector ov = v.getOffsetVector();
      UInt4Vector.Accessor ova = ov.getAccessor();

      assertEquals(1024, v.getBuffer().capacity());
      // Note: offset vector size is (128 + 1) rounded up
      assertEquals(256 * 4, ov.getBuffer().capacity());

      // Zero fill so the following is deterministic

      v.getBuffer().setZero(0, 1024);
      ov.getBuffer().setZero(0, 256 * 4);

      // O-size is special

      v.getMutator().setValueCount(0);
      assertEquals(0, va.getValueCount());
      assertEquals(0, ova.getValueCount());

      // Fill-empties at 0 is also special.

      v.getMutator().fillEmpties(-1, 0);
      assertEquals(0, va.getValueCount());
      assertEquals(0, ova.getValueCount());

      // Write one value

      v.getMutator().setSafe(0, "foo".getBytes());
      v.getMutator().setValueCount(1);
      assertEquals(1, va.getValueCount());
      assertEquals(2, ova.getValueCount());
      assertEquals(0, ova.get(0));
      assertEquals(3, ova.get(1));

      // Pretend to skip a value

      v.getMutator().fillEmpties(0, 2);
      // fillEmpties does not change the value count
      assertEquals(0, ova.get(0));
      assertEquals(3, ova.get(1)); // First value
      assertEquals(3, ova.get(2)); // Filled
      assertEquals(0, ova.get(3)); // Should not be set

      // Write one more value

      v.getMutator().setSafe(2, "mumble".getBytes());
      v.getMutator().setValueCount(2);
      assertEquals(2, va.getValueCount());
      assertEquals(3, ova.getValueCount());
      assertEquals(3, ova.get(1));
      assertEquals(3, ova.get(2));
      assertEquals(9, ova.get(3));
    }
  }

  /**
   * Verify the delicate, fragile logic for setting the value count and
   * filling empty values. Some operators and readers first write values,
   * then set the value count at the end of the batch. The "complex writers"
   * set the value count after each value. The "lastSet" count tracks the
   * last value actually written, but is set in fillEmpties(), which is
   * also called when setting the value count.
   */

  @Test
  public void testNullableVarChar() {
    MaterializedField field = MaterializedField.create("stringCol", Types.optional(TypeProtos.MinorType.VARCHAR));
    try (NullableVarCharVector v = new NullableVarCharVector(field, allocator)) {
      int targetDataLength = 1000;
      int targetValueCount = 128;
      v.allocateNew(targetDataLength, targetValueCount);
      NullableVarCharVector.Accessor va = v.getAccessor();
      NullableVarCharVector.Mutator vm = v.getMutator();
      VarCharVector dv = v.getValuesVector();
      VarCharVector.Accessor da = dv.getAccessor();
      UInt4Vector ov = dv.getOffsetVector();
      UInt4Vector.Accessor ova = ov.getAccessor();
      UInt1Vector bv = v.getBitsVector();

      // Right at edge, so target offset vector is larger
      int actualDataLength = 1024;
      int targetOffsetCount = 2 * targetValueCount;
      assertEquals(actualDataLength, dv.getBuffer().capacity());
      assertEquals(targetOffsetCount * 4, ov.getBuffer().capacity());
      assertEquals(targetValueCount, bv.getBuffer().capacity());

      // Zero fill so the following is deterministic
      // Bits are already zero

      v.getBuffer().setZero(0, 1024);
      ov.getBuffer().setZero(0, 256 * 4);

      // Initial setup. Valid only because of zero-fill and
      // how intial values happen to be set up.

      assertEquals(0, va.getValueCount());
      assertEquals(-1, vm.getLastSet());
      assertEquals(0, bv.getAccessor().getValueCount());
      assertEquals(0, da.getValueCount());
      assertEquals(0, ova.getValueCount());
      assertEquals(0, ova.get(0));
      assertEquals(0, ova.get(1));

      // O-size is special

      v.getMutator().setValueCount(0);
      assertEquals(-1, vm.getLastSet());
      assertEquals(0, va.getValueCount());
      assertEquals(0, bv.getAccessor().getValueCount());
      assertEquals(0, da.getValueCount());
      assertEquals(0, ova.getValueCount());
      assertEquals(0, ova.get(0));
      assertEquals(0, ova.get(1));

      // Fill-empties at 0 is also special.

      v.getMutator().fillEmpties(0);
      assertEquals(0, vm.getLastSet());
      assertEquals(0, va.getValueCount());
      assertEquals(0, ova.getValueCount());
      assertEquals(0, ova.get(0));
      assertEquals(0, ova.get(1));

      // Write one value

      byte[] bytes = "foo".getBytes();
      vm.setSafe(0, bytes, 0, bytes.length);
      assertEquals(0, vm.getLastSet());
      vm.setValueCount(1);
      assertEquals(0, vm.getLastSet());
      assertEquals(1, va.getValueCount());
      assertEquals(2, ova.getValueCount());
      assertEquals(0, ova.get(0));
      assertEquals(bytes.length, ova.get(1));

      // Pretend to skip a value

      v.getMutator().fillEmpties(2);
      // Optimistically pre-set for value we're about to write
      assertEquals(2, vm.getLastSet());
      // fillEmpties does not change the value count
      assertEquals(0, ova.get(0));
      assertEquals(3, ova.get(1)); // First value
      assertEquals(3, ova.get(2)); // Filled
      assertEquals(0, ova.get(3)); // Should not be set

      // Calling fillEmpties() twice is idempotent

      v.getMutator().fillEmpties(2);
      assertEquals(2, v.getMutator().getLastSet());
      assertEquals(0, ova.get(0));
      assertEquals(3, ova.get(1)); // First value
      assertEquals(3, ova.get(2)); // Filled
      assertEquals(0, ova.get(3)); // Should not be set

      // Write one more value

      byte[] second = "mumble".getBytes();
      vm.setSafe(2, second, 0, second.length);
      assertEquals(2, v.getMutator().getLastSet());
      vm.setValueCount(3);
      assertEquals(2, v.getMutator().getLastSet());
      assertEquals(3, va.getValueCount());
      assertEquals(4, ova.getValueCount());
      assertEquals(3, ova.get(1));
      assertEquals(3, ova.get(2));
      assertEquals(9, ova.get(3));

      // Skip two values

      v.getMutator().setValueCount(5);
      assertEquals(4, v.getMutator().getLastSet());
      assertEquals(5, va.getValueCount());
      assertEquals(6, ova.getValueCount());
      assertEquals(9, ova.get(3));
      assertEquals(9, ova.get(4));
      assertEquals(9, ova.get(5));

      // Skip a large number of values. Finish the vector
      // right where the offset vector would have to increase
      // in length

      v.getMutator().setValueCount(targetOffsetCount);
      assertEquals(targetOffsetCount - 1, v.getMutator().getLastSet());
      assertEquals(targetOffsetCount, va.getValueCount());
      assertEquals(targetOffsetCount + 1, ova.getValueCount());
      assertEquals(9, ova.get(targetOffsetCount-1));
      assertEquals(9, ova.get(targetOffsetCount));
      assertEquals(0, ova.get(targetOffsetCount + 1));
    }
  }

  @Test
  public void testNullableVarCharWriter() throws Exception {
    MaterializedField field = MaterializedField.create("stringCol", Types.optional(TypeProtos.MinorType.VARCHAR));
    try (NullableVarCharVector v = new NullableVarCharVector(field, allocator);
         DrillBuf buf = allocator.buffer(100)) {
      v.allocateNew(1000, 128);
      @SuppressWarnings("resource")
      FieldWriter w = new NullableVarCharWriterImpl(v, null);

      // Write in locations 1 and 3.

      w.setPosition(0);
      buf.setBytes(0, "foo".getBytes());
      w.writeVarChar(0, 3, buf);

      w.setPosition(2);
      buf.setBytes(0, "mumble".getBytes());
      w.writeVarChar(0, 6, buf);

      // Don't close the writer; it clears the vector
      // w.close();

      VarCharVector dv = v.getValuesVector();
      UInt4Vector ov = dv.getOffsetVector();
      UInt4Vector.Accessor ova = ov.getAccessor();

      v.getMutator().setValueCount(2);
      assertEquals(2, v.getAccessor().getValueCount());
      assertEquals(3, ova.getValueCount());
      assertEquals(3, ova.get(1));
      assertEquals(3, ova.get(2));
      assertEquals(9, ova.get(3));
    }
  }

  @Test
  public void testEmptyValuePopulator() throws Exception {
    MaterializedField field = MaterializedField.create("offsets", Types.required(TypeProtos.MinorType.UINT4));
    try (UInt4Vector v = new UInt4Vector(field, allocator)) {
      EmptyValuePopulator pop = new EmptyValuePopulator(v);
      UInt4Vector.Accessor va = v.getAccessor();
      UInt4Vector.Mutator vm = v.getMutator();

      v.allocateNew(128);

      // Zero case; for zero-length batches

      vm.setValueCount(0);
      assertEquals(0, va.getValueCount());
      assertEquals(0, va.get(0));

      // Start a record 0 value.

      pop.populate(0);
      assertEquals(1, va.getValueCount());
      assertEquals(0, va.get(0));

      // Pretend batch count is 1. Offset vector is
      // currently in special 0-size state.

      pop.populate(1);
      vm.setValueCount(1);
      assertEquals(1, va.getValueCount());
      assertEquals(0, va.get(0));
      assertEquals(0, va.get(1));

      // Pretend values are [xx] and [xxx]

      vm.set(1, 2);
      vm.set(2, 5);
      vm.set(3, 0);
      vm.setValueCount(3);
      assertEquals(3, va.getValueCount());
      assertEquals(5, va.get(2));
      assertEquals(0, va.get(3));

      // Pretend that the record count is 2 for the two
      // values above.

      pop.populate(2);
      assertEquals(3, va.getValueCount());
      assertEquals(5, va.get(2));
      assertEquals(0, va.get(3));

      // Pretend, instead we skipped records 2, 3 and 4

      pop.populate(5);
      assertEquals(6, va.getValueCount());
      assertEquals(5, va.get(2));
      assertEquals(5, va.get(3));
      assertEquals(5, va.get(4));
      assertEquals(5, va.get(5));
      assertEquals(0, va.get(6));
    }
  }

  @Test
  public void testRepeatedMapCount() throws Exception {
    try (RepeatedMapVector v = buildRepeatedMap()) {

      IntVector iv = getInner(v);
      RepeatedMapVector.Accessor va = v.getAccessor();
      RepeatedMapVector.Mutator vm = v.getMutator();
      IntVector.Accessor ia = iv.getAccessor();
      IntVector.Mutator im = iv.getMutator();
      UInt4Vector ov = v.getOffsetVector();
      UInt4Vector.Accessor oa = ov.getAccessor();
      UInt4Vector.Mutator om = ov.getMutator();

      // Zero fill so the following is deterministic

      ov.getBuffer().setZero(0, 6 * 4);

      // Initial state

      assertEquals(0, va.getValueCount());
      assertEquals(0, ia.getValueCount());
      assertEquals(0, oa.getValueCount());
      assertEquals(0, oa.get(0));

      // Record size = 0

      vm.setValueCount(0);
      assertEquals(0, va.getValueCount());
      assertEquals(0, ia.getValueCount());
      assertEquals(0, oa.getValueCount());
      assertEquals(0, oa.get(0));

      // Record size = 1, so, implicit record 1 (1-based) of []

      vm.setValueCount(1);
      assertEquals(1, va.getValueCount());
      assertEquals(0, ia.getValueCount());
      assertEquals(2, oa.getValueCount());
      assertEquals(0, oa.get(0));
      assertEquals(0, oa.get(1));

      // Record 2 (1-based) is [10, 20]

      im.set(0, 10);
      im.set(1, 20);
      im.setValueCount(2);
      om.set(2, 2);
      om.setValueCount(3);
      vm.setValueCount(2);
      assertEquals(2, va.getValueCount());
      assertEquals(2, ia.getValueCount());
      assertEquals(3, oa.getValueCount());
      assertEquals(0, oa.get(0));
      assertEquals(0, oa.get(1));
      assertEquals(2, oa.get(2));
      assertEquals(0, oa.get(3));

      // Batch size = 4, so implicit record 2, 4 of []

      vm.setValueCount(4);
      assertEquals(4, va.getValueCount());
      assertEquals(2, ia.getValueCount());
      assertEquals(5, oa.getValueCount());
      assertEquals(0, oa.get(0));
      assertEquals(0, oa.get(1));
      assertEquals(2, oa.get(2));
      assertEquals(2, oa.get(3));
      assertEquals(2, oa.get(4));
      assertEquals(0, oa.get(5));
    }
  }

  @Test
  public void testRepeatedCopySafe() throws Exception {
    try (RepeatedMapVector v = buildRepeatedMap();
        RepeatedMapVector f = buildFromMap()) {

      RepeatedMapVector.Mutator vm = v.getMutator();
      UInt4Vector ov = v.getOffsetVector();
      UInt4Vector.Accessor oa = ov.getAccessor();
      UInt4Vector.Mutator om = ov.getMutator();

      TransferPair tp = f.makeTransferPair(v);

      tp.copyValueSafe(0, 0);

      // CopyValue does not change the value count
      //assertEquals(1, va.getValueCount());
      //assertEquals(2, oa.getValueCount());
      //assertEquals(2, ia.getValueCount());
      assertEquals(0, oa.get(0));
      assertEquals(2, oa.get(1));

      tp.copyValueSafe(1, 1);
      assertEquals(0, oa.get(0));
      assertEquals(2, oa.get(1));
      assertEquals(5, oa.get(2));

      tp.copyValueSafe(2, 2);
      assertEquals(2, oa.get(1));
      assertEquals(5, oa.get(2));
      assertEquals(5, oa.get(3));

     vm.setValueCount(3);

      // v should now be the same as f
      validateFrom(v);
    }
  }

  private class SpecialMapVector extends MapVector {

    private final RepeatedMapVector v;

    public SpecialMapVector(RepeatedMapVector v) {
      super("", null, null);
      this.v = v;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ValueVector> T addOrGet(String name, MajorType type, Class<T> clazz) {
      assert clazz == RepeatedMapVector.class;
      return (T) v;
    }
  }

  @Test
  public void testRepeatedMapWriter() throws Exception {

    // The writers will create a nullable int inner vector.

    MaterializedField field = MaterializedField.create("repeated_map", Types.repeated(TypeProtos.MinorType.MAP));
    try (RepeatedMapVector v = new RepeatedMapVector(field, allocator, null)) {

      SpecialMapVector mapVector = new SpecialMapVector(v);
      @SuppressWarnings("resource")
      SingleMapWriter mapRoot = new SingleMapWriter(mapVector, null, false);
      ListWriter lw = mapRoot.list("repeated_map");
      MapWriter mw = lw.map();

      // Record 1: [10, 20]

      lw.setPosition(0);
      lw.startList();
      mw.start();
      IntWriter iw = mw.integer("inner");
      iw.writeInt(10);
      mw.end();
      mw.start();
      iw.writeInt(20);
      mw.end();
      lw.endList();

      // Record 2: [30, 40, 50]

      lw.setPosition(1);
      lw.startList();
      mw.start();
      iw.writeInt(30);
      mw.end();
      mw.start();
      iw.writeInt(40);
      mw.end();
      mw.start();
      iw.writeInt(50);
      mw.end();
      lw.endList();

      // Record 3: []

      lw.setPosition(2);
      lw.startList();
      lw.endList();

      v.getMutator().setValueCount(3);

      assertEquals(3, v.getAccessor().getValueCount());
      UInt4Vector.Accessor oa = v.getOffsetVector().getAccessor();
      assertEquals(4, oa.getValueCount());
      assertEquals(0, oa.get(0));
      assertEquals(2, oa.get(1));
      assertEquals(5, oa.get(2));
      assertEquals(5, oa.get(3));
      assertEquals(0, oa.get(4)); // Past end

      NullableIntVector inner =  v.addOrGet("inner", Types.optional(TypeProtos.MinorType.INT), NullableIntVector.class);
      UInt1Vector.Accessor ba = inner.getBitsVector().getAccessor();
      assertEquals(1, ba.get(0));
      assertEquals(1, ba.get(1));
      assertEquals(1, ba.get(2));
      assertEquals(1, ba.get(3));
      assertEquals(1, ba.get(4));
      assertEquals(0, ba.get(5)); // Past end

      NullableIntVector.Accessor ia = inner.getAccessor();
      assertEquals(10, ia.get(0));
      assertEquals(20, ia.get(1));
      assertEquals(30, ia.get(2));
      assertEquals(40, ia.get(3));
      assertEquals(50, ia.get(4));
    }
  }

  private RepeatedMapVector buildRepeatedMap() {
    MaterializedField field = MaterializedField.create("repeated_map", Types.repeated(TypeProtos.MinorType.MAP));
    RepeatedMapVector v = new RepeatedMapVector(field, allocator, null);
    getInner(v);
    v.allocateNew(5, 10);
    return v;
  }

  private IntVector getInner(RepeatedMapVector v) {
    return v.addOrGet("inner", Types.required(TypeProtos.MinorType.INT), IntVector.class);
  }

  private RepeatedMapVector buildFromMap() {
    RepeatedMapVector v = buildRepeatedMap();

    // Can't figure out how to get the indexes to step for
    // inner and outer values.
    // If we get the int writer from the repeated map writer,
    // the column will be converted to nullable int.

    IntVector iv = getInner(v);
    IntVector.Mutator im = iv.getMutator();
    UInt4Vector ov = v.getOffsetVector();
    UInt4Vector.Mutator om = ov.getMutator();

    om.set(0, 0);

    // Record 1: [10, 20]

    im.set(0, 10);
    im.set(1, 20);
    om.set(1, 2);

    // Record 2: [30, 40, 50]

    im.set(2, 30);
    im.set(3, 40);
    im.set(4, 50);
    om.set(2, 5);

    // Record 3: []

    om.set(3, 5);

    om.setValueCount(4);
    v.getMutator().setValueCount(3);

    // Sanity check

    validateFrom(v);
    return v;
  }

  private void validateFrom(RepeatedMapVector v) {
    UInt4Vector.Accessor oa = v.getOffsetVector().getAccessor();
    assertEquals(3, v.getAccessor().getValueCount());
    assertEquals(4, oa.getValueCount());
    assertEquals(0, oa.get(0));
    assertEquals(2, oa.get(1));
    assertEquals(5, oa.get(2));
    assertEquals(5, oa.get(3));

    IntVector.Accessor ia = getInner(v).getAccessor();
    assertEquals(10, ia.get(0));
    assertEquals(20, ia.get(1));
    assertEquals(30, ia.get(2));
    assertEquals(40, ia.get(3));
    assertEquals(50, ia.get(4));
  }
}
