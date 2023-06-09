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
package org.apache.drill.exec.physical.rowSet;

import static org.apache.drill.test.rowSet.RowSetUtilities.dec;
import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.BasicTypeHelper;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.BaseDataValueVector;
import org.apache.drill.exec.vector.BitVector;
import org.apache.drill.exec.vector.DateUtilities;
import org.apache.drill.exec.vector.NullableBitVector;
import org.apache.drill.exec.vector.NullableVarCharVector;
import org.apache.drill.exec.vector.NullableVector;
import org.apache.drill.exec.vector.RepeatedVarCharVector;
import org.apache.drill.exec.vector.UInt1Vector;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VarCharVector;
import org.apache.drill.exec.vector.accessor.ArrayReader;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.ScalarReader;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.ValueType;
import org.apache.drill.exec.vector.complex.RepeatedValueVector;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.joda.time.Period;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Verify that simple scalar (non-repeated) column readers
 * and writers work as expected. The focus is on the generated
 * and type-specific functions for each type.
 */

// The following types are not fully supported in Drill
// TODO: Var16Char
// TODO: Bit

@Category(RowSetTests.class)
public class TestScalarAccessors extends SubOperatorTest {

  @Test
  public void testUInt1RW() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.UINT1)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(0)
        .addRow(0x7F)
        .addRow(0xFF)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.INTEGER, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(0, colReader.getInt());

    assertTrue(reader.next());
    assertEquals(0x7F, colReader.getInt());
    assertEquals(0x7F, colReader.getObject());
    assertEquals(Integer.toString(0x7F), colReader.getAsString());

    assertTrue(reader.next());
    assertEquals(0xFF, colReader.getInt());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testUInt2RW() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.UINT2)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(0)
        .addRow(0x7FFF)
        .addRow(0xFFFF)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.INTEGER, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(0, colReader.getInt());

    assertTrue(reader.next());
    assertEquals(0x7FFF, colReader.getInt());
    assertEquals(0x7FFF, colReader.getObject());
    assertEquals(Integer.toString(0x7FFF), colReader.getAsString());

    assertTrue(reader.next());
    assertEquals(0xFFFF, colReader.getInt());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testTinyIntRW() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.TINYINT)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(0)
        .addRow(Byte.MAX_VALUE)
        .addRow(Byte.MIN_VALUE)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.INTEGER, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(0, colReader.getInt());

    assertTrue(reader.next());
    assertEquals(Byte.MAX_VALUE, colReader.getInt());
    assertEquals((int) Byte.MAX_VALUE, colReader.getObject());
    assertEquals(Byte.toString(Byte.MAX_VALUE), colReader.getAsString());

    assertTrue(reader.next());
    assertEquals(Byte.MIN_VALUE, colReader.getInt());

    assertFalse(reader.next());
    rs.clear();
  }

  private void nullableIntTester(MinorType type) {
    TupleMetadata schema = new SchemaBuilder()
        .addNullable("col", type)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(10)
        .addSingleCol(null)
        .addRow(30)
        .build();
    assertEquals(3, rs.rowCount());

    // Verify vector state

    VectorContainer container = rs.container();
    assertEquals(1, container.getNumberOfColumns());
    ValueVector v = container.getValueVector(0).getValueVector();
    assertTrue(v instanceof NullableVector);
    NullableVector nv = (NullableVector) v;
    assertEquals(3, nv.getAccessor().getValueCount());
    assertEquals(3 * BasicTypeHelper.getSize(Types.required(type)),
        ((BaseDataValueVector) v).getBuffer().writerIndex());

    // Verify bits vector. (Assumes UInt1 implementation.)

    UInt1Vector bv = (UInt1Vector) nv.getBitsVector();
    assertEquals(3, bv.getAccessor().getValueCount());
    assertEquals(3, bv.getBuffer().writerIndex());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(10, colReader.getInt());

    assertTrue(reader.next());
    assertTrue(colReader.isNull());
    assertNull(colReader.getObject());
    assertEquals("null", colReader.getAsString());
    // Data value is undefined, may be garbage

    assertTrue(reader.next());
    assertEquals(30, colReader.getInt());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testNullableTinyInt() {
    nullableIntTester(MinorType.TINYINT);
  }

  private void intArrayTester(MinorType type) {
    TupleMetadata schema = new SchemaBuilder()
        .addArray("col", type)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addSingleCol(new int[] {})
        .addSingleCol(new int[] {0, 20, 30})
        .build();
    assertEquals(2, rs.rowCount());

    // Verify vector state

    VectorContainer container = rs.container();
    assertEquals(1, container.getNumberOfColumns());
    ValueVector v = container.getValueVector(0).getValueVector();
    assertTrue(v instanceof RepeatedValueVector);
    RepeatedValueVector rv = (RepeatedValueVector) v;
    assertEquals(2, rv.getAccessor().getValueCount());

    // Data vector: 3 values written above.

    ValueVector vv = rv.getDataVector();
    assertEquals(3, vv.getAccessor().getValueCount());
    assertEquals(3 * BasicTypeHelper.getSize(Types.required(type)),
        ((BaseDataValueVector) vv).getBuffer().writerIndex());

    // Offsets vector: one more than row count

    UInt4Vector ov = rv.getOffsetVector();
    assertEquals(3, ov.getAccessor().getValueCount());
    assertEquals(3 * 4, ov.getBuffer().writerIndex());

    RowSetReader reader = rs.reader();
    ArrayReader arrayReader = reader.array(0);
    ScalarReader colReader = arrayReader.scalar();
    assertEquals(ValueType.INTEGER, colReader.valueType());

    assertTrue(reader.next());
    assertEquals(0, arrayReader.size());

    assertTrue(reader.next());
    assertEquals(3, arrayReader.size());
    assertTrue(arrayReader.next());
    assertFalse(colReader.isNull());
    assertEquals(0, colReader.getInt());
    assertEquals(0, colReader.getObject());
    assertEquals("0", colReader.getAsString());

    assertTrue(arrayReader.next());
    assertFalse(colReader.isNull());
    assertEquals(20, colReader.getInt());
    assertEquals(20, colReader.getObject());
    assertEquals("20", colReader.getAsString());

    assertTrue(arrayReader.next());
    assertFalse(colReader.isNull());
    assertEquals(30, colReader.getInt());
    assertEquals(30, colReader.getObject());
    assertEquals("30", colReader.getAsString());

    assertFalse(arrayReader.next());

    assertEquals("[0, 20, 30]", arrayReader.getAsString());
    assertEquals(Arrays.asList(0, 20, 30), arrayReader.getObject());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testTinyIntArray() {
    intArrayTester(MinorType.TINYINT);
  }

  @Test
  public void testSmallIntRW() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.SMALLINT)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(0)
        .addRow(Short.MAX_VALUE)
        .addRow(Short.MIN_VALUE)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.INTEGER, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(0, colReader.getInt());

    assertTrue(reader.next());
    assertEquals(Short.MAX_VALUE, colReader.getInt());
    assertEquals((int) Short.MAX_VALUE, colReader.getObject());
    assertEquals(Short.toString(Short.MAX_VALUE), colReader.getAsString());

    assertTrue(reader.next());
    assertEquals(Short.MIN_VALUE, colReader.getInt());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testNullableSmallInt() {
    nullableIntTester(MinorType.SMALLINT);
  }

  @Test
  public void testSmallArray() {
    intArrayTester(MinorType.SMALLINT);
  }

  @Test
  public void testIntRW() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.INT)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(0)
        .addRow(Integer.MAX_VALUE)
        .addRow(Integer.MIN_VALUE)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.INTEGER, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(0, reader.scalar(0).getInt());

    assertTrue(reader.next());
    assertEquals(Integer.MAX_VALUE, colReader.getInt());
    assertEquals(Integer.MAX_VALUE, colReader.getObject());
    assertEquals(Integer.toString(Integer.MAX_VALUE), colReader.getAsString());

    assertTrue(reader.next());
    assertEquals(Integer.MIN_VALUE, colReader.getInt());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testNullableInt() {
    nullableIntTester(MinorType.INT);
  }

  @Test
  public void testIntArray() {
    intArrayTester(MinorType.INT);
  }

  private void longRWTester(MinorType type) {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", type)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(0L)
        .addRow(Long.MAX_VALUE)
        .addRow(Long.MIN_VALUE)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.LONG, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(0, colReader.getLong());

    assertTrue(reader.next());
    assertEquals(Long.MAX_VALUE, colReader.getLong());
    if (colReader.extendedType() == ValueType.LONG) {
      assertEquals(Long.MAX_VALUE, colReader.getObject());
      assertEquals(Long.toString(Long.MAX_VALUE), colReader.getAsString());
    }

    assertTrue(reader.next());
    assertEquals(Long.MIN_VALUE, colReader.getLong());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testLongRW() {
    longRWTester(MinorType.BIGINT);
  }

  private void nullableLongTester(MinorType type) {
    TupleMetadata schema = new SchemaBuilder()
        .addNullable("col", type)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(10L)
        .addSingleCol(null)
        .addRow(30L)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(10, colReader.getLong());

    assertTrue(reader.next());
    assertTrue(colReader.isNull());
    assertNull(colReader.getObject());
    assertEquals("null", colReader.getAsString());
    // Data value is undefined, may be garbage

    assertTrue(reader.next());
    assertEquals(30, colReader.getLong());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testNullableLong() {
    nullableLongTester(MinorType.BIGINT);
  }

  private void longArrayTester(MinorType type) {
    TupleMetadata schema = new SchemaBuilder()
        .addArray("col", type)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addSingleCol(new long[] {})
        .addSingleCol(new long[] {0, 20, 30})
        .build();
    assertEquals(2, rs.rowCount());

    RowSetReader reader = rs.reader();
    ArrayReader arrayReader = reader.array(0);
    ScalarReader colReader = arrayReader.scalar();
    assertEquals(ValueType.LONG, colReader.valueType());

    assertTrue(reader.next());
    assertEquals(0, arrayReader.size());

    assertTrue(reader.next());
    assertEquals(3, arrayReader.size());

    assertTrue(arrayReader.next());
    assertEquals(0, colReader.getLong());
    if (colReader.extendedType() == ValueType.LONG) {
      assertEquals(0L, colReader.getObject());
      assertEquals("0", colReader.getAsString());
    }

    assertTrue(arrayReader.next());
    assertEquals(20, colReader.getLong());
    if (colReader.extendedType() == ValueType.LONG) {
      assertEquals(20L, colReader.getObject());
      assertEquals("20", colReader.getAsString());
    }

    assertTrue(arrayReader.next());
    assertEquals(30, colReader.getLong());
    if (colReader.extendedType() == ValueType.LONG) {
      assertEquals(30L, colReader.getObject());
      assertEquals("30", colReader.getAsString());
    }

    assertFalse(arrayReader.next());

    if (colReader.extendedType() == ValueType.LONG) {
      assertEquals("[0, 20, 30]", arrayReader.getAsString());
      assertEquals(Arrays.asList(0L, 20L, 30L), arrayReader.getObject());
    }

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testLongArray() {
    longArrayTester(MinorType.BIGINT);
  }

  @Test
  public void testFloatRW() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.FLOAT4)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(0F)
        .addRow(Float.MAX_VALUE)
        .addRow(Float.MIN_VALUE)
        .addRow(100F)
        .build();
    assertEquals(4, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.FLOAT, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(0, colReader.getFloat(), 0.000001);

    assertTrue(reader.next());
    assertEquals(Float.MAX_VALUE, colReader.getFloat(), 0.000001);
    assertEquals(Float.MAX_VALUE, (float) colReader.getObject(), 0.000001);

    assertTrue(reader.next());
    assertEquals(Float.MIN_VALUE, colReader.getFloat(), 0.000001);

    assertTrue(reader.next());
    assertEquals(100, colReader.getFloat(), 0.000001);
    assertEquals("100.0", colReader.getAsString());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testNullableFloat() {
    TupleMetadata schema = new SchemaBuilder()
        .addNullable("col", MinorType.FLOAT4)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(10F)
        .addSingleCol(null)
        .addRow(30F)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(10, colReader.getFloat(), 0.000001);
    assertEquals(10, colReader.getDouble(), 0.000001);

    assertTrue(reader.next());
    assertTrue(colReader.isNull());
    assertNull(colReader.getObject());
    assertEquals("null", colReader.getAsString());
    // Data value is undefined, may be garbage

    assertTrue(reader.next());
    assertEquals(30, colReader.getFloat(), 0.000001);
    assertEquals(30, colReader.getDouble(), 0.000001);

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testFloatArray() {
    TupleMetadata schema = new SchemaBuilder()
        .addArray("col", MinorType.FLOAT4)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addSingleCol(new double[] {})
        .addSingleCol(new double[] {0, 20.5, 30.0})
        .build();
    assertEquals(2, rs.rowCount());

    RowSetReader reader = rs.reader();
    ArrayReader arrayReader = reader.array(0);
    ScalarReader colReader = arrayReader.scalar();
    assertEquals(ValueType.FLOAT, colReader.valueType());

    assertTrue(reader.next());
    assertEquals(0, arrayReader.size());

    assertTrue(reader.next());
    assertEquals(3, arrayReader.size());

    assertTrue(arrayReader.next());
    assertEquals(0, colReader.getDouble(), 0.00001);
    assertEquals(0, (float) colReader.getObject(), 0.00001);
    assertEquals("0.0", colReader.getAsString());

    assertTrue(arrayReader.next());
    assertEquals(20.5, colReader.getDouble(), 0.00001);
    assertEquals(20.5, (float) colReader.getObject(), 0.00001);
    assertEquals("20.5", colReader.getAsString());

    assertTrue(arrayReader.next());
    assertEquals(30.0, colReader.getDouble(), 0.00001);
    assertEquals(30.0, (float) colReader.getObject(), 0.00001);
    assertEquals("30.0", colReader.getAsString());
    assertFalse(arrayReader.next());

    assertEquals("[0.0, 20.5, 30.0]", arrayReader.getAsString());
    assertEquals(Arrays.asList(0.0F, 20.5F, 30F), arrayReader.getObject());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testDoubleRW() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.FLOAT8)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(0D)
        .addRow(Double.MAX_VALUE)
        .addRow(Double.MIN_VALUE)
        .addRow(100D)
        .build();
    assertEquals(4, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.DOUBLE, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(0, colReader.getDouble(), 0.000001);

    assertTrue(reader.next());
    assertEquals(Double.MAX_VALUE, colReader.getDouble(), 0.000001);
    assertEquals(Double.MAX_VALUE, (double) colReader.getObject(), 0.000001);

    assertTrue(reader.next());
    assertEquals(Double.MIN_VALUE, colReader.getDouble(), 0.000001);

    assertTrue(reader.next());
    assertEquals(100, colReader.getDouble(), 0.000001);
    assertEquals("100.0", colReader.getAsString());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testNullableDouble() {
    TupleMetadata schema = new SchemaBuilder()
        .addNullable("col", MinorType.FLOAT8)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(10D)
        .addSingleCol(null)
        .addRow(30D)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(10, colReader.getDouble(), 0.000001);

    assertTrue(reader.next());
    assertTrue(colReader.isNull());
    assertNull(colReader.getObject());
    assertEquals("null", colReader.getAsString());
    // Data value is undefined, may be garbage

    assertTrue(reader.next());
    assertEquals(30, colReader.getDouble(), 0.000001);

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testVarcharRW() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.VARCHAR)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow("")
        .addRow("fred")
        .addRow("barney")
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.STRING, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals("", colReader.getString());

    assertTrue(reader.next());
    assertEquals("fred", colReader.getString());
    assertEquals("fred", colReader.getObject());
    assertEquals("\"fred\"", colReader.getAsString());

    assertTrue(reader.next());
    assertEquals("barney", colReader.getString());
    assertEquals("barney", colReader.getObject());
    assertEquals("\"barney\"", colReader.getAsString());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testNullableVarchar() {
    TupleMetadata schema = new SchemaBuilder()
        .addNullable("col", MinorType.VARCHAR)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow("")
        .addSingleCol(null)
        .addRow("abcd")
        .build();
    assertEquals(3, rs.rowCount());

    // Verify vector state

    VectorContainer container = rs.container();
    assertEquals(1, container.getNumberOfColumns());
    ValueVector v = container.getValueVector(0).getValueVector();
    assertTrue(v instanceof NullableVarCharVector);
    NullableVarCharVector nvcv = (NullableVarCharVector) v;
    assertEquals(3, nvcv.getAccessor().getValueCount());
    assertEquals(2, nvcv.getMutator().getLastSet());

    // Data vector: 3 values written above.

    VarCharVector vv = nvcv.getValuesVector();
    assertEquals(3, vv.getAccessor().getValueCount());

    // Offsets vector: one more than row count

    UInt4Vector ov = vv.getOffsetVector();
    assertEquals(4, ov.getAccessor().getValueCount());
    assertEquals(4 * 4, ov.getBuffer().writerIndex());

    // Last offset and bytes buf length must agree

    int lastIndex = ov.getAccessor().get(3);
    assertEquals(lastIndex, vv.getBuffer().writerIndex());

    // Verify using the reader

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals("", colReader.getString());

    assertTrue(reader.next());
    assertTrue(colReader.isNull());
    assertNull(colReader.getObject());
    assertEquals("null", colReader.getAsString());

    assertTrue(reader.next());
    assertEquals("abcd", colReader.getString());
    assertEquals("abcd", colReader.getObject());
    assertEquals("\"abcd\"", colReader.getAsString());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testVarcharArray() {
    TupleMetadata schema = new SchemaBuilder()
        .addArray("col", MinorType.VARCHAR)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addSingleCol(strArray())
        .addSingleCol(strArray("fred", "", "wilma"))
        .build();
    assertEquals(2, rs.rowCount());

    RowSetReader reader = rs.reader();
    ArrayReader arrayReader = reader.array(0);
    ScalarReader colReader = arrayReader.scalar();
    assertEquals(ValueType.STRING, colReader.valueType());

    assertTrue(reader.next());
    assertEquals(0, arrayReader.size());

    assertTrue(reader.next());
    assertEquals(3, arrayReader.size());

    assertTrue(arrayReader.next());
    assertEquals("fred", colReader.getString());
    assertEquals("fred", colReader.getObject());
    assertEquals("\"fred\"", colReader.getAsString());

    assertTrue(arrayReader.next());
    assertEquals("", colReader.getString());
    assertEquals("", colReader.getObject());
    assertEquals("\"\"", colReader.getAsString());

    assertTrue(arrayReader.next());
    assertEquals("wilma", colReader.getString());
    assertEquals("wilma", colReader.getObject());
    assertEquals("\"wilma\"", colReader.getAsString());

    assertFalse(arrayReader.next());

    assertEquals("[\"fred\", \"\", \"wilma\"]", arrayReader.getAsString());
    assertEquals(Arrays.asList("fred", "", "wilma"), arrayReader.getObject());

    assertFalse(reader.next());
    rs.clear();
  }

  /**
   * Test for the special case for the "inner" offset vector
   * as explained in the Javadoc for
   * @{link org.apache.drill.exec.vector.accessor.writer.OffsetVectorWriterImpl}
   */
  @Test
  public void testEmptyVarcharArray() {
    TupleMetadata schema = new SchemaBuilder()
        .addArray("col", MinorType.VARCHAR)
        .add("b", MinorType.VARCHAR)
        .buildSchema();
    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(strArray(), "first")
        .addRow(strArray(), "second")
        .addRow(strArray(), "third")
        .build();
    assertEquals(3, rs.rowCount());

    // Verify vector state

    VectorContainer container = rs.container();
    assertEquals(2, container.getNumberOfColumns());
    ValueVector v = container.getValueVector(0).getValueVector();
    assertTrue(v instanceof RepeatedVarCharVector);
    RepeatedVarCharVector rvc = (RepeatedVarCharVector) v;
    assertEquals(3, rvc.getAccessor().getValueCount());

    // Verify outer offsets vector

    UInt4Vector oov = rvc.getOffsetVector();
    assertEquals(4, oov.getAccessor().getValueCount());
    assertEquals(4 * 4, oov.getBuffer().writerIndex());

    // Inner vector

    VarCharVector iv = rvc.getDataVector();
    assertEquals(0, iv.getAccessor().getValueCount());
    assertEquals(0, iv.getBuffer().writerIndex());

    // Inner offset vector. Has 0 entries, not 1 as would be
    // expected according to the general rule:
    // offset vector length = value length + 1

    UInt4Vector iov = iv.getOffsetVector();
    assertEquals(0, iov.getAccessor().getValueCount());
    assertEquals(0, iov.getBuffer().writerIndex());

    rs.clear();
  }

  /**
   * Test the low-level interval-year utilities used by the column accessors.
   */
  @Test
  public void testIntervalYearUtils() {
    {
      Period expected = Period.months(0);
      Period actual = DateUtilities.fromIntervalYear(0);
      assertEquals(expected, actual.normalizedStandard());
      String fmt = DateUtilities.intervalYearStringBuilder(expected).toString();
      assertEquals("0 years 0 months", fmt);
    }

    {
      Period expected = Period.years(1).plusMonths(2);
      Period actual = DateUtilities.fromIntervalYear(DateUtilities.periodToMonths(expected));
      assertEquals(expected, actual.normalizedStandard());
      String fmt = DateUtilities.intervalYearStringBuilder(expected).toString();
      assertEquals("1 year 2 months", fmt);
    }

    {
      Period expected = Period.years(6).plusMonths(1);
      Period actual = DateUtilities.fromIntervalYear(DateUtilities.periodToMonths(expected));
      assertEquals(expected, actual.normalizedStandard());
      String fmt = DateUtilities.intervalYearStringBuilder(expected).toString();
      assertEquals("6 years 1 month", fmt);
    }
  }

  @Test
  public void testIntervalYearRW() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.INTERVALYEAR)
        .buildSchema();

    Period p1 = Period.years(0);
    Period p2 = Period.years(2).plusMonths(3);
    Period p3 = Period.years(1234).plusMonths(11);

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(p1)
        .addRow(p2)
        .addRow(p3)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.PERIOD, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(p1, colReader.getPeriod());

    assertTrue(reader.next());
    assertEquals(p2, colReader.getPeriod());
    assertEquals(p2, colReader.getObject());
    assertEquals(p2.toString(), colReader.getAsString());

    assertTrue(reader.next());
    assertEquals(p3, colReader.getPeriod());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testNullableIntervalYear() {
    TupleMetadata schema = new SchemaBuilder()
        .addNullable("col", MinorType.INTERVALYEAR)
        .buildSchema();

    Period p1 = Period.years(0);
    Period p2 = Period.years(2).plusMonths(3);

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(p1)
        .addSingleCol(null)
        .addRow(p2)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.PERIOD, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(p1, colReader.getPeriod());

    assertTrue(reader.next());
    assertTrue(colReader.isNull());
    assertNull(colReader.getObject());
    assertEquals("null", colReader.getAsString());

    assertTrue(reader.next());
    assertEquals(p2, colReader.getPeriod());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testIntervalYearArray() {
    TupleMetadata schema = new SchemaBuilder()
        .addArray("col", MinorType.INTERVALYEAR)
        .buildSchema();

    Period p1 = Period.years(0);
    Period p2 = Period.years(2).plusMonths(3);
    Period p3 = Period.years(1234).plusMonths(11);

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addSingleCol(new Period[] {})
        .addSingleCol(new Period[] {p1, p2, p3})
        .build();
    assertEquals(2, rs.rowCount());

    RowSetReader reader = rs.reader();
    ArrayReader arrayReader = reader.array(0);
    ScalarReader colReader = arrayReader.scalar();
    assertEquals(ValueType.PERIOD, colReader.valueType());

    assertTrue(reader.next());
    assertEquals(0, arrayReader.size());

    assertTrue(reader.next());
    assertEquals(3, arrayReader.size());

    assertTrue(arrayReader.next());
    assertEquals(p1, colReader.getPeriod());

    assertTrue(arrayReader.next());
    assertEquals(p2, colReader.getPeriod());
    assertEquals(p2, colReader.getObject());
    assertEquals(p2.toString(), colReader.getAsString());

    assertTrue(arrayReader.next());
    assertEquals(p3, colReader.getPeriod());

    assertFalse(arrayReader.next());

    assertFalse(reader.next());
    rs.clear();
  }

  /**
   * Test the low-level interval-day utilities used by the column accessors.
   */
  @Test
  public void testIntervalDayUtils() {
    {
      Period expected = Period.days(0);
      Period actual = DateUtilities.fromIntervalDay(0, 0);
      assertEquals(expected, actual.normalizedStandard());
      String fmt = DateUtilities.intervalDayStringBuilder(expected).toString();
      assertEquals("0 days 0:00:00", fmt);
    }

    {
      Period expected = Period.days(1).plusHours(5).plusMinutes(6).plusSeconds(7);
      Period actual = DateUtilities.fromIntervalDay(1, DateUtilities.timeToMillis(5, 6, 7, 0));
      assertEquals(expected, actual.normalizedStandard());
      String fmt = DateUtilities.intervalDayStringBuilder(expected).toString();
      assertEquals("1 day 5:06:07", fmt);
    }

    {
      Period expected = Period.days(2).plusHours(12).plusMinutes(23).plusSeconds(34).plusMillis(567);
      Period actual = DateUtilities.fromIntervalDay(2, DateUtilities.timeToMillis(12, 23, 34, 567));
      assertEquals(expected, actual.normalizedStandard());
      String fmt = DateUtilities.intervalDayStringBuilder(expected).toString();
      assertEquals("2 days 12:23:34.567", fmt);
    }
  }

  @Test
  public void testIntervalDayRW() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.INTERVALDAY)
        .buildSchema();

    Period p1 = Period.days(0);
    Period p2 = Period.days(3).plusHours(4).plusMinutes(5).plusSeconds(23);
    Period p3 = Period.days(999).plusHours(23).plusMinutes(59).plusSeconds(59);

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(p1)
        .addRow(p2)
        .addRow(p3)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.PERIOD, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    // The normalizedStandard() call is a hack. See DRILL-5689.
    assertEquals(p1, colReader.getPeriod().normalizedStandard());

    assertTrue(reader.next());
    assertEquals(p2, colReader.getPeriod().normalizedStandard());
    assertEquals(p2, ((Period) colReader.getObject()).normalizedStandard());
    assertEquals(p2.toString(), colReader.getAsString());

    assertTrue(reader.next());
    assertEquals(p3.normalizedStandard(), colReader.getPeriod().normalizedStandard());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testNullableIntervalDay() {
    TupleMetadata schema = new SchemaBuilder()
        .addNullable("col", MinorType.INTERVALDAY)
        .buildSchema();

    Period p1 = Period.years(0);
    Period p2 = Period.days(3).plusHours(4).plusMinutes(5).plusSeconds(23);

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(p1)
        .addSingleCol(null)
        .addRow(p2)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.PERIOD, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(p1, colReader.getPeriod().normalizedStandard());

    assertTrue(reader.next());
    assertTrue(colReader.isNull());
    assertNull(colReader.getObject());
    assertEquals("null", colReader.getAsString());

    assertTrue(reader.next());
    assertEquals(p2, colReader.getPeriod().normalizedStandard());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testIntervalDayArray() {
    TupleMetadata schema = new SchemaBuilder()
        .addArray("col", MinorType.INTERVALDAY)
        .buildSchema();

    Period p1 = Period.days(0);
    Period p2 = Period.days(3).plusHours(4).plusMinutes(5).plusSeconds(23);
    Period p3 = Period.days(999).plusHours(23).plusMinutes(59).plusSeconds(59);

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addSingleCol(new Period[] {})
        .addSingleCol(new Period[] {p1, p2, p3})
        .build();
    assertEquals(2, rs.rowCount());

    RowSetReader reader = rs.reader();
    ArrayReader arrayReader = reader.array(0);
    ScalarReader colReader = arrayReader.scalar();
    assertEquals(ValueType.PERIOD, colReader.valueType());

    assertTrue(reader.next());
    assertEquals(0, arrayReader.size());

    assertTrue(reader.next());
    assertEquals(3, arrayReader.size());

    assertTrue(arrayReader.next());
    assertEquals(p1, colReader.getPeriod().normalizedStandard());

    assertTrue(arrayReader.next());
    assertEquals(p2, colReader.getPeriod().normalizedStandard());
    assertEquals(p2, ((Period) colReader.getObject()).normalizedStandard());
    assertEquals(p2.toString(), colReader.getAsString());

    assertTrue(arrayReader.next());
    assertEquals(p3.normalizedStandard(), colReader.getPeriod().normalizedStandard());

    assertFalse(arrayReader.next());

    assertFalse(reader.next());
    rs.clear();
  }

  /**
   * Test the low-level interval utilities used by the column accessors.
   */

  @Test
  public void testIntervalUtils() {
    {
      Period expected = Period.months(0);
      Period actual = DateUtilities.fromInterval(0, 0, 0);
      assertEquals(expected, actual.normalizedStandard());
      String fmt = DateUtilities.intervalStringBuilder(expected).toString();
      assertEquals("0 years 0 months 0 days 0:00:00", fmt);
    }

    {
      Period expected = Period.years(1).plusMonths(2).plusDays(3)
          .plusHours(5).plusMinutes(6).plusSeconds(7);
      Period actual = DateUtilities.fromInterval(DateUtilities.periodToMonths(expected), 3,
          DateUtilities.periodToMillis(expected));
      assertEquals(expected, actual.normalizedStandard());
      String fmt = DateUtilities.intervalStringBuilder(expected).toString();
      assertEquals("1 year 2 months 3 days 5:06:07", fmt);
    }

    {
      Period expected = Period.years(2).plusMonths(1).plusDays(3)
          .plusHours(12).plusMinutes(23).plusSeconds(34)
          .plusMillis(456);
      Period actual = DateUtilities.fromInterval(DateUtilities.periodToMonths(expected), 3,
          DateUtilities.periodToMillis(expected));
      assertEquals(expected, actual.normalizedStandard());
      String fmt = DateUtilities.intervalStringBuilder(expected).toString();
      assertEquals("2 years 1 month 3 days 12:23:34.456", fmt);
    }

    {
      Period expected = Period.years(2).plusMonths(3).plusDays(1)
          .plusHours(12).plusMinutes(23).plusSeconds(34);
      Period actual = DateUtilities.fromInterval(DateUtilities.periodToMonths(expected), 1,
          DateUtilities.periodToMillis(expected));
      assertEquals(expected, actual.normalizedStandard());
      String fmt = DateUtilities.intervalStringBuilder(expected).toString();
      assertEquals("2 years 3 months 1 day 12:23:34", fmt);
    }
  }

  @Test
  public void testIntervalRW() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.INTERVAL)
        .buildSchema();

    Period p1 = Period.days(0);
    Period p2 = Period.years(7).plusMonths(8)
                      .plusDays(3).plusHours(4)
                      .plusMinutes(5).plusSeconds(23);
    Period p3 = Period.years(9999).plusMonths(11)
                      .plusDays(365).plusHours(23)
                      .plusMinutes(59).plusSeconds(59);

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(p1)
        .addRow(p2)
        .addRow(p3)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.PERIOD, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    // The normalizedStandard() call is a hack. See DRILL-5689.
    assertEquals(p1, colReader.getPeriod().normalizedStandard());

    assertTrue(reader.next());
    assertEquals(p2, colReader.getPeriod().normalizedStandard());
    assertEquals(p2, ((Period) colReader.getObject()).normalizedStandard());
    assertEquals(p2.toString(), colReader.getAsString());

    assertTrue(reader.next());
    assertEquals(p3.normalizedStandard(), colReader.getPeriod().normalizedStandard());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testNullableInterval() {
    TupleMetadata schema = new SchemaBuilder()
        .addNullable("col", MinorType.INTERVAL)
        .buildSchema();

    Period p1 = Period.years(0);
    Period p2 = Period.years(7).plusMonths(8)
                      .plusDays(3).plusHours(4)
                      .plusMinutes(5).plusSeconds(23);

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(p1)
        .addSingleCol(null)
        .addRow(p2)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.PERIOD, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(p1, colReader.getPeriod().normalizedStandard());

    assertTrue(reader.next());
    assertTrue(colReader.isNull());
    assertNull(colReader.getObject());
    assertEquals("null", colReader.getAsString());

    assertTrue(reader.next());
    assertEquals(p2, colReader.getPeriod().normalizedStandard());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testIntervalArray() {
    TupleMetadata schema = new SchemaBuilder()
        .addArray("col", MinorType.INTERVAL)
        .buildSchema();

    Period p1 = Period.days(0);
    Period p2 = Period.years(7).plusMonths(8)
                      .plusDays(3).plusHours(4)
                      .plusMinutes(5).plusSeconds(23);
    Period p3 = Period.years(9999).plusMonths(11)
                      .plusDays(365).plusHours(23)
                      .plusMinutes(59).plusSeconds(59);

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addSingleCol(new Period[] {})
        .addSingleCol(new Period[] {p1, p2, p3})
        .build();
    assertEquals(2, rs.rowCount());

    RowSetReader reader = rs.reader();
    ArrayReader arrayReader = reader.array(0);
    ScalarReader colReader = arrayReader.scalar();
    assertEquals(ValueType.PERIOD, colReader.valueType());

    assertTrue(reader.next());
    assertEquals(0, arrayReader.size());

    assertTrue(reader.next());
    assertEquals(3, arrayReader.size());

    assertTrue(arrayReader.next());
    assertEquals(p1, colReader.getPeriod().normalizedStandard());

    assertTrue(arrayReader.next());
    assertEquals(p2, colReader.getPeriod().normalizedStandard());
    assertEquals(p2, ((Period) colReader.getObject()).normalizedStandard());
    assertEquals(p2.toString(), colReader.getAsString());

    assertTrue(arrayReader.next());
    assertEquals(p3.normalizedStandard(), colReader.getPeriod().normalizedStandard());

    assertFalse(arrayReader.next());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testDecimal9RW() {
    MajorType type = MajorType.newBuilder()
        .setMinorType(MinorType.DECIMAL9)
        .setScale(3)
        .setPrecision(9)
        .setMode(DataMode.REQUIRED)
        .build();
    TupleMetadata schema = new SchemaBuilder()
        .add("col", type)
        .buildSchema();

    BigDecimal v1 = BigDecimal.ZERO;
    BigDecimal v2 = BigDecimal.valueOf(123_456_789, 3);
    BigDecimal v3 = BigDecimal.valueOf(999_999_999, 3);

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(v1)
        .addRow(v2)
        .addRow(v3)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.DECIMAL, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(0, v1.compareTo(colReader.getDecimal()));

    assertTrue(reader.next());
    assertEquals(0, v2.compareTo(colReader.getDecimal()));
    assertEquals(0, v2.compareTo((BigDecimal) colReader.getObject()));
    assertEquals(v2.toString(), colReader.getAsString());

    assertTrue(reader.next());
    assertEquals(0, v3.compareTo(colReader.getDecimal()));

    assertFalse(reader.next());
    rs.clear();
  }

  private void nullableDecimalTester(MinorType type, int precision) {
    MajorType majorType = MajorType.newBuilder()
        .setMinorType(type)
        .setScale(3)
        .setPrecision(precision)
        .setMode(DataMode.OPTIONAL)
        .build();
    TupleMetadata schema = new SchemaBuilder()
        .add("col", majorType)
        .buildSchema();

    BigDecimal v1 = BigDecimal.ZERO;
    BigDecimal v2 = BigDecimal.valueOf(123_456_789, 3);

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(v1)
        .addSingleCol(null)
        .addRow(v2)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.DECIMAL, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(0, v1.compareTo(colReader.getDecimal()));

    assertTrue(reader.next());
    assertTrue(colReader.isNull());
    assertNull(colReader.getObject());
    assertEquals("null", colReader.getAsString());

    assertTrue(reader.next());
    assertEquals(0, v2.compareTo(colReader.getDecimal()));

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testNullableDecimal9() {
    nullableDecimalTester(MinorType.DECIMAL9, 9);
  }

  private void decimalArrayTester(MinorType type, int precision) {
    MajorType majorType = MajorType.newBuilder()
        .setMinorType(type)
        .setScale(3)
        .setPrecision(precision)
        .setMode(DataMode.REPEATED)
        .build();
    TupleMetadata schema = new SchemaBuilder()
        .add("col", majorType)
        .buildSchema();

    BigDecimal v1 = BigDecimal.ZERO;
    BigDecimal v2 = BigDecimal.valueOf(123_456_789, 3);
    BigDecimal v3 = BigDecimal.TEN;

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addSingleCol(new BigDecimal[] {})
        .addSingleCol(new BigDecimal[] {v1, v2, v3})
        .build();
    assertEquals(2, rs.rowCount());

    RowSetReader reader = rs.reader();
    ArrayReader arrayReader = reader.array(0);
    ScalarReader colReader = arrayReader.scalar();
    assertEquals(ValueType.DECIMAL, colReader.valueType());

    assertTrue(reader.next());
    assertEquals(0, arrayReader.size());

    assertTrue(reader.next());
    assertEquals(3, arrayReader.size());

    assertTrue(arrayReader.next());
    assertEquals(0, v1.compareTo(colReader.getDecimal()));

    assertTrue(arrayReader.next());
    assertEquals(0, v2.compareTo(colReader.getDecimal()));
    assertEquals(0, v2.compareTo((BigDecimal) colReader.getObject()));
    assertEquals(v2.toString(), colReader.getAsString());

    assertTrue(arrayReader.next());
    assertEquals(0, v3.compareTo(colReader.getDecimal()));

    assertFalse(arrayReader.next());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testDecimal9Array() {
    decimalArrayTester(MinorType.DECIMAL9, 9);
  }

  @Test
  public void testDecimal18RW() {
    MajorType type = MajorType.newBuilder()
        .setMinorType(MinorType.DECIMAL18)
        .setScale(3)
        .setPrecision(9)
        .setMode(DataMode.REQUIRED)
        .build();
    TupleMetadata schema = new SchemaBuilder()
        .add("col", type)
        .buildSchema();

    BigDecimal v1 = BigDecimal.ZERO;
    BigDecimal v2 = BigDecimal.valueOf(123_456_789_123_456_789L, 3);
    BigDecimal v3 = BigDecimal.valueOf(999_999_999_999_999_999L, 3);

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(v1)
        .addRow(v2)
        .addRow(v3)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.DECIMAL, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(0, v1.compareTo(colReader.getDecimal()));

    assertTrue(reader.next());
    assertEquals(0, v2.compareTo(colReader.getDecimal()));
    assertEquals(0, v2.compareTo((BigDecimal) colReader.getObject()));
    assertEquals(v2.toString(), colReader.getAsString());

    assertTrue(reader.next());
    assertEquals(0, v3.compareTo(colReader.getDecimal()));

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testNullableDecimal18() {
    nullableDecimalTester(MinorType.DECIMAL18, 9);
  }

  @Test
  public void testDecimal18Array() {
    decimalArrayTester(MinorType.DECIMAL18, 9);
  }

  // From the perspective of the vector, a date vector is just a long.

  @Test
  public void testDateRW() {
    longRWTester(MinorType.DATE);
  }

  @Test
  public void testNullableDate() {
    nullableLongTester(MinorType.DATE);
  }

  @Test
  public void testDateArray() {
    longArrayTester(MinorType.DATE);
  }

  // From the perspective of the vector, a timestamp vector is just a long.

  @Test
  public void testTimestampRW() {
    longRWTester(MinorType.TIMESTAMP);
  }

  @Test
  public void testNullableTimestamp() {
    nullableLongTester(MinorType.TIMESTAMP);
  }

  @Test
  public void testTimestampArray() {
    longArrayTester(MinorType.TIMESTAMP);
  }

  @Test
  public void testVarBinaryRW() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.VARBINARY)
        .buildSchema();

    byte v1[] = new byte[] {};
    byte v2[] = new byte[] { (byte) 0x00, (byte) 0x7f, (byte) 0x80, (byte) 0xFF};

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(v1)
        .addRow(v2)
        .build();
    assertEquals(2, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.BYTES, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertTrue(Arrays.equals(v1, colReader.getBytes()));

    assertTrue(reader.next());
    assertTrue(Arrays.equals(v2, colReader.getBytes()));
    assertTrue(Arrays.equals(v2, (byte[]) colReader.getObject()));
    assertEquals("[00, 7f, 80, ff]", colReader.getAsString());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testNullableVarBinary() {
    TupleMetadata schema = new SchemaBuilder()
        .addNullable("col", MinorType.VARBINARY)
        .buildSchema();

    byte v1[] = new byte[] {};
    byte v2[] = new byte[] { (byte) 0x00, (byte) 0x7f, (byte) 0x80, (byte) 0xFF};

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addRow(v1)
        .addSingleCol(null)
        .addRow(v2)
        .build();
    assertEquals(3, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.BYTES, colReader.valueType());

    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertTrue(Arrays.equals(v1, colReader.getBytes()));

    assertTrue(reader.next());
    assertTrue(colReader.isNull());
    assertNull(colReader.getObject());
    assertEquals("null", colReader.getAsString());

    assertTrue(reader.next());
    assertTrue(Arrays.equals(v2, colReader.getBytes()));

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testVarBinaryArray() {
    TupleMetadata schema = new SchemaBuilder()
        .addArray("col", MinorType.VARBINARY)
        .buildSchema();

    byte v1[] = new byte[] {};
    byte v2[] = new byte[] { (byte) 0x00, (byte) 0x7f, (byte) 0x80, (byte) 0xFF};
    byte v3[] = new byte[] { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xAF};

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addSingleCol(new byte[][] {})
        .addSingleCol(new byte[][] {v1, v2, v3})
        .build();
    assertEquals(2, rs.rowCount());

    RowSetReader reader = rs.reader();
    ArrayReader arrayReader = reader.array(0);
    ScalarReader colReader = arrayReader.scalar();
    assertEquals(ValueType.BYTES, colReader.valueType());

    assertTrue(reader.next());
    assertEquals(0, arrayReader.size());

    assertTrue(reader.next());
    assertEquals(3, arrayReader.size());

    assertTrue(arrayReader.next());
    assertTrue(Arrays.equals(v1, colReader.getBytes()));

    assertTrue(arrayReader.next());
    assertTrue(Arrays.equals(v2, colReader.getBytes()));
    assertTrue(Arrays.equals(v2, (byte[]) colReader.getObject()));
    assertEquals("[00, 7f, 80, ff]", colReader.getAsString());

    assertTrue(arrayReader.next());
    assertTrue(Arrays.equals(v3, colReader.getBytes()));

    assertFalse(arrayReader.next());

    assertFalse(reader.next());
    rs.clear();
  }

  // Test the convenience objects for DATE, TIME and TIMESTAMP

  @Test
  public void testDateObjectRW() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.DATE)
        .buildSchema();

    LocalDate v1 = LocalDate.of(2019, 3, 24);

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addSingleCol(v1)
        .build();
    assertEquals(1, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.DATE, colReader.extendedType());

    assertTrue(reader.next());
    assertEquals(v1, colReader.getDate());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testTimeObjectRW() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.TIME)
        .buildSchema();

    LocalTime v1 = LocalTime.of(12, 13, 14);

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addSingleCol(v1)
        .build();
    assertEquals(1, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.TIME, colReader.extendedType());

    assertTrue(reader.next());
    assertEquals(v1, colReader.getTime());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testTimeStampObjectRW() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.TIMESTAMP)
        .buildSchema();

    LocalDate dt = LocalDate.of(2019, 3, 24);
    LocalTime lt = LocalTime.of(12, 13, 14);
    Instant v1 = LocalDateTime.of(dt, lt).toInstant(ZoneOffset.UTC);

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addSingleCol(v1)
        .build();
    assertEquals(1, rs.rowCount());

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);
    assertEquals(ValueType.TIMESTAMP, colReader.extendedType());

    assertTrue(reader.next());
    assertEquals(v1, colReader.getTimestamp());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testBitRW() {

    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.BIT)
        .buildSchema();

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addSingleCol(true)
        .addSingleCol(false)
        .addSingleCol(0)
        .addSingleCol(1)
        .addSingleCol(2)
        .addSingleCol(3)
        .build();
    assertEquals(6, rs.rowCount());

    // Verify vector state

    VectorContainer container = rs.container();
    assertEquals(1, container.getNumberOfColumns());
    ValueVector v = container.getValueVector(0).getValueVector();
    assertTrue(v instanceof BitVector);
    BitVector bv = (BitVector) v;
    assertEquals(6, bv.getAccessor().getValueCount());
    assertEquals(1,
        ((BaseDataValueVector) v).getBuffer().writerIndex());

    // Verify using a reader

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);

    assertTrue(reader.next());
    assertEquals(true, colReader.getBoolean());
    assertEquals(1, colReader.getInt());
    assertTrue(reader.next());
    assertEquals(false, colReader.getBoolean());
    assertEquals(0, colReader.getInt());
    assertTrue(reader.next());
    assertEquals(false, colReader.getBoolean());
    assertEquals(0, colReader.getInt());
    assertTrue(reader.next());
    assertEquals(true, colReader.getBoolean());
    assertEquals(1, colReader.getInt());
    assertTrue(reader.next());
    assertEquals(true, colReader.getBoolean());
    assertEquals(1, colReader.getInt());
    assertTrue(reader.next());
    assertEquals(true, colReader.getBoolean());
    assertEquals(1, colReader.getInt());

    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testNullableBitRW() {

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("col", MinorType.BIT)
        .buildSchema();

    SingleRowSet rs = fixture.rowSetBuilder(schema)
        .addSingleCol(true)
        .addSingleCol(false)
        .addSingleCol(null)
        .addSingleCol(1)
        .addSingleCol(2)
        .addSingleCol(null)
        .build();
    assertEquals(6, rs.rowCount());

    // Verify vector state

    VectorContainer container = rs.container();
    assertEquals(1, container.getNumberOfColumns());
    ValueVector v = container.getValueVector(0).getValueVector();
    assertTrue(v instanceof NullableBitVector);
    NullableBitVector nv = (NullableBitVector) v;
    assertEquals(6, nv.getAccessor().getValueCount());

    BitVector dv = nv.getValuesVector();
    assertEquals(6, dv.getAccessor().getValueCount());
    assertEquals(1, dv.getBuffer().writerIndex());

    // Verify bits vector. (Assumes UInt1 implementation.)

    UInt1Vector bv = nv.getBitsVector();
    assertEquals(6, bv.getAccessor().getValueCount());
    assertEquals(6, bv.getBuffer().writerIndex());

    // Verify using a reader

    RowSetReader reader = rs.reader();
    ScalarReader colReader = reader.scalar(0);

    assertTrue(reader.next());
    assertEquals(true, colReader.getBoolean());
    assertEquals(1, colReader.getInt());
    assertFalse(colReader.isNull());
    assertTrue(reader.next());
    assertEquals(false, colReader.getBoolean());
    assertEquals(0, colReader.getInt());
    assertFalse(colReader.isNull());
    assertTrue(reader.next());
    assertTrue(colReader.isNull());
    assertTrue(reader.next());
    assertEquals(true, colReader.getBoolean());
    assertEquals(1, colReader.getInt());
    assertTrue(reader.next());
    assertFalse(colReader.isNull());
    assertEquals(true, colReader.getBoolean());
    assertEquals(1, colReader.getInt());
    assertFalse(colReader.isNull());
    assertTrue(reader.next());
    assertTrue(colReader.isNull());

    assertFalse(reader.next());
    rs.clear();
  }

  /**
   * The bit reader/writer are special and use the BitVector directly.
   * Ensure that resize works in this special case.
   */

  @Test
  public void testBitResize() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.BIT)
        .buildSchema();

    RowSetBuilder rsb = new RowSetBuilder(fixture.allocator(), schema, 100);
    ScalarWriter bitWriter = rsb.writer().scalar(0);
    for (int i = 0; i < ValueVector.MAX_ROW_COUNT; i++) {
      bitWriter.setBoolean((i % 5) == 0);
      rsb.writer().save();
    }

    SingleRowSet rs = rsb.build();
    RowSetReader reader = rs.reader();
    ScalarReader bitReader = reader.scalar(0);
    for (int i = 0; i < ValueVector.MAX_ROW_COUNT; i++) {
      reader.next();
      assertEquals((i % 5) == 0, bitReader.getBoolean());
    }
    rs.clear();
  }

  private static String repeat(String str, int n) {
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < n; i++) {
      buf.append(str);
    }
    return str.toString();
  }

  @Test
  public void testVarDecimalRange() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.VARDECIMAL, 38, 4)
        .buildSchema();

    String big = repeat("9", 34) + ".9999";
    SingleRowSet rs = new RowSetBuilder(fixture.allocator(), schema)
        .addSingleCol(dec("0"))
        .addSingleCol(dec("-0.0000"))
        .addSingleCol(dec("0.0001"))
        .addSingleCol(dec("-0.0001"))
        .addSingleCol(dec(big))
        .addSingleCol(dec("-" + big))
        .addSingleCol(dec("1234.56789"))
        .build();

    RowSetReader reader = rs.reader();
    ScalarReader decReader = reader.scalar(0);
    assertTrue(reader.next());
    assertEquals(dec("0.0000"), decReader.getDecimal());
    assertTrue(reader.next());
    assertEquals(dec("0.0000"), decReader.getDecimal());
    assertTrue(reader.next());
    assertEquals(dec("0.0001"), decReader.getDecimal());
    assertTrue(reader.next());
    assertEquals(dec("-0.0001"), decReader.getDecimal());
    assertTrue(reader.next());
    assertEquals(dec(big), decReader.getDecimal());
    assertTrue(reader.next());
    assertEquals(dec("-" + big), decReader.getDecimal());
    assertTrue(reader.next());
    assertEquals(dec("1234.5679"), decReader.getDecimal());
    assertFalse(reader.next());
    rs.clear();
  }

  @Test
  public void testVarDecimalOverflow() {
    TupleMetadata schema = new SchemaBuilder()
        .add("col", MinorType.VARDECIMAL, 8, 4)
        .buildSchema();

    RowSetBuilder rsb = new RowSetBuilder(fixture.allocator(), schema, 100);
    try {

      // With rounding due to scale, value exceeds allowed precision.

      rsb.addSingleCol(dec("9999.99999"));
      fail();
    } catch (UserException e) {
      // Expected
    }
    rsb.build().clear();
  }

  /**
   * Test the ability to append bytes to a VarChar column. Should work for
   * Var16Char, but that type is not yet supported in Drill.
   */

  @Test
  public void testAppend() {
    doTestAppend(new SchemaBuilder()
        .add("col", MinorType.VARCHAR)
        .buildSchema());
    doTestAppend(new SchemaBuilder()
        .addNullable("col", MinorType.VARCHAR)
        .buildSchema());
  }

  private void doTestAppend(TupleMetadata schema) {
    DirectRowSet rs = DirectRowSet.fromSchema(fixture.allocator(), schema);
    RowSetWriter writer = rs.writer(100);
    ScalarWriter colWriter = writer.scalar("col");

    byte first[] = "abc".getBytes();
    byte second[] = "12345".getBytes();
    colWriter.setBytes(first, first.length);
    colWriter.appendBytes(second, second.length);
    writer.save();
    colWriter.setBytes(second, second.length);
    colWriter.appendBytes(first, first.length);
    writer.save();
    colWriter.setBytes(first, first.length);
    colWriter.appendBytes(second, second.length);
    writer.save();
    RowSet actual = writer.done();

    RowSet expected = new RowSetBuilder(fixture.allocator(), schema)
        .addSingleCol("abc12345")
        .addSingleCol("12345abc")
        .addSingleCol("abc12345")
        .build();

    RowSetUtilities.verify(expected, actual);
  }

  /**
   * Test the ability to append bytes to a VarChar column. Should work for
   * Var16Char, but that type is not yet supported in Drill.
   */

  @Test
  public void testAppendWithArray() {
    TupleMetadata schema = new SchemaBuilder()
        .addArray("col", MinorType.VARCHAR)
        .buildSchema();

    DirectRowSet rs = DirectRowSet.fromSchema(fixture.allocator(), schema);
    RowSetWriter writer = rs.writer(100);
    ArrayWriter arrayWriter = writer.array("col");
    ScalarWriter colWriter = arrayWriter.scalar();

    byte first[] = "abc".getBytes();
    byte second[] = "12345".getBytes();
    for (int i = 0; i < 3; i++) {
      colWriter.setBytes(first, first.length);
      colWriter.appendBytes(second, second.length);
      arrayWriter.save();
      colWriter.setBytes(second, second.length);
      colWriter.appendBytes(first, first.length);
      arrayWriter.save();
      colWriter.setBytes(first, first.length);
      colWriter.appendBytes(second, second.length);
      arrayWriter.save();
      writer.save();
    }
    RowSet actual = writer.done();

    RowSet expected = new RowSetBuilder(fixture.allocator(), schema)
        .addSingleCol(strArray("abc12345", "12345abc", "abc12345"))
        .addSingleCol(strArray("abc12345", "12345abc", "abc12345"))
        .addSingleCol(strArray("abc12345", "12345abc", "abc12345"))
        .build();

    RowSetUtilities.verify(expected, actual);
  }
}
