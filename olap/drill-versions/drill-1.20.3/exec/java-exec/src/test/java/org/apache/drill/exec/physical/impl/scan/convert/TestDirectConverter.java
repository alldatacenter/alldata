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
package org.apache.drill.exec.physical.impl.scan.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.impl.scan.convert.StandardConversions.ConversionDefn;
import org.apache.drill.exec.physical.impl.scan.convert.StandardConversions.ConversionType;
import org.apache.drill.exec.physical.impl.scan.v3.FixedReceiver;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.physical.rowSet.RowSetWriter;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleNameSpace;
import org.apache.drill.exec.vector.accessor.InvalidConversionError;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.ValueWriter;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.joda.time.Period;
import org.junit.Test;

public class TestDirectConverter extends SubOperatorTest {

  /**
   * Mock column conversion factory that takes an input schema, matches it against
   * the given writer, and inserts a standard type conversion shim.
   */
  private static class ConversionTestFixture {

    private final RowSetWriter rowWriter;
    private StandardConversions conversions;
    private final TupleNameSpace<ValueWriter> rowFormat = new TupleNameSpace<>();

    public ConversionTestFixture(BufferAllocator allocator, TupleMetadata outputSchema) {
      rowWriter = RowSetTestUtils.makeWriter(allocator, outputSchema);
    }

    public ConversionTestFixture withProperties(Map<String,String> props) {
      conversions = StandardConversions.builder().withProperties(props).build();
      return this;
    }

    private StandardConversions conversions() {
      if (conversions == null) {
        conversions = StandardConversions.builder().build();
      }
      return conversions;
    }

    public void createConvertersFor(TupleMetadata inputSchema) {
      for (ColumnMetadata inputCol : inputSchema) {
        addColumn(inputCol);
      }
    }

    public void addColumn(ColumnMetadata source) {
      ScalarWriter colWriter = rowWriter.scalar(source.name());

      // Test uses simple row writer; no support for adding columns.
      assertNotNull(colWriter);
      ValueWriter converter = conversions().converterFor(colWriter, source);
      assertNotNull(converter);
      rowFormat.add(source.name(), converter);
    }

    public ConversionTestFixture addRow(Object...cols) {
      assertTrue(cols.length <= rowFormat.count());
      for (int i = 0; i < rowFormat.count(); i++) {
        rowFormat.get(i).setValue(cols[i]);
      }
      rowWriter.save();
      return this;
    }

    public ConversionTestFixture addSingleCol(Object col) {
      rowFormat.get(0).setValue(col);
      rowWriter.save();
      return this;
    }

    public RowSet build() {
      return rowWriter.done();
    }
  }

  @Test
  public void testSchemaMerge() {
    TupleMetadata providedSchema = new SchemaBuilder()
      .add("a", MinorType.INT)
      .addNullable("b", MinorType.INT)
      .add("c", MinorType.INT)
      .build();
    providedSchema.setBooleanProperty("foo", true);
    TupleMetadata readerSchema = new SchemaBuilder()
      .add("a", MinorType.VARCHAR)
      .add("b", MinorType.VARCHAR)
      .add("d", MinorType.VARCHAR)
      .build();
    TupleMetadata expected = new SchemaBuilder()
      .add("a", MinorType.INT)
      .addNullable("b", MinorType.INT)
      .add("d", MinorType.VARCHAR)
      .build();

    TupleMetadata mergedSchema = FixedReceiver.Builder.mergeSchemas(providedSchema, readerSchema);
    assertTrue(expected.isEquivalent(mergedSchema));
    assertTrue(mergedSchema.booleanProperty("foo"));
  }

  /**
   * Test the standard string-to-type conversion using an ad-hoc conversion
   * from the input type (the type used by the row set builder) to the output
   * (vector) type.
   */
  @Test
  public void testStringToNumberConversion() {

    // Create the input and output schemas
    TupleMetadata outputSchema = new SchemaBuilder()
        .add("ti", MinorType.TINYINT)
        .add("si", MinorType.SMALLINT)
        .add("int", MinorType.INT)
        .add("bi", MinorType.BIGINT)
        .add("fl", MinorType.FLOAT4)
        .add("db", MinorType.FLOAT8)
        .buildSchema();
    TupleMetadata inputSchema = new SchemaBuilder()
        .add("ti", MinorType.VARCHAR)
        .add("si", MinorType.VARCHAR)
        .add("int", MinorType.VARCHAR)
        .add("bi", MinorType.VARCHAR)
        .add("fl", MinorType.VARCHAR)
        .add("db", MinorType.VARCHAR)
        .buildSchema();

    // Load test data using converters
    ConversionTestFixture testFixture = new ConversionTestFixture(fixture.allocator(), outputSchema);
    testFixture.createConvertersFor(inputSchema);
    RowSet actual = testFixture
        .addRow("11", "12", "13", "14", "15.5", "16.25")
        .addRow("127", "32757", Integer.toString(Integer.MAX_VALUE),
            Long.toString(Long.MAX_VALUE), "10E6", "10E200")
        .build();

    // Build the expected vector without a type converter.
    final SingleRowSet expected = fixture.rowSetBuilder(outputSchema)
        .addRow(11, 12, 13, 14L, 15.5F, 16.25D)
        .addRow(127, 32757, Integer.MAX_VALUE, Long.MAX_VALUE, 10E6F, 10E200D)
        .build();

    // Compare
    RowSetUtilities.verify(expected, actual);
  }

  /**
   * Test the standard string-to-type conversion using an ad-hoc conversion
   * from the input type (the type used by the row set builder) to the output
   * (vector) type.
   */
  @Test
  public void testNumberToStringConversion() {

    // Create the schema
    TupleMetadata outputSchema = new SchemaBuilder()
        .add("ti", MinorType.VARCHAR)
        .add("si", MinorType.VARCHAR)
        .add("int", MinorType.VARCHAR)
        .add("bi", MinorType.VARCHAR)
        .add("fl", MinorType.VARCHAR)
        .add("db", MinorType.VARCHAR)
        .buildSchema();
    TupleMetadata inputSchema = new SchemaBuilder()
        .add("ti", MinorType.TINYINT)
        .add("si", MinorType.SMALLINT)
        .add("int", MinorType.INT)
        .add("bi", MinorType.BIGINT)
        .add("fl", MinorType.FLOAT4)
        .add("db", MinorType.FLOAT8)
        .buildSchema();

    // The setObject() method won't do the Float to Double conversion,
    // so values before are provided as doubles in the float case.
    ConversionTestFixture testFixture = new ConversionTestFixture(fixture.allocator(), outputSchema);
    testFixture.createConvertersFor(inputSchema);
    RowSet actual = testFixture
        .addRow(11, 12, 13, 14L, 15.5D, 16.25D)
        .addRow(127, 32757, Integer.MAX_VALUE, Long.MAX_VALUE, 10E6D, 10E200D)
        .build();

    // Build the expected vector without a type converter.
    final SingleRowSet expected = fixture.rowSetBuilder(outputSchema)
        .addRow("11", "12", "13", "14", "15.5", "16.25")
        .addRow("127", "32757", Integer.toString(Integer.MAX_VALUE),
            Long.toString(Long.MAX_VALUE), "1.0E7", "1.0E201")
        .build();

    // Compare
    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testStringToNumberConversionError() {

    TupleMetadata outputSchema = new SchemaBuilder()
       .add("int", MinorType.INT)
       .buildSchema();
    TupleMetadata inputSchema = new SchemaBuilder()
       .add("int", MinorType.VARCHAR)
       .buildSchema();

    ConversionTestFixture testFixture = new ConversionTestFixture(fixture.allocator(), outputSchema);
    testFixture.createConvertersFor(inputSchema);
    try {
      testFixture.addRow("foo");
      fail();
    } catch (InvalidConversionError e) {
      // Expected
    } finally {
      testFixture.build().clear();
    }
  }

  public static BigDecimal dec(int value) {
    return new BigDecimal(value);
  }

  /**
   * Tests the implicit conversions provided by the column writer itself.
   * No conversion mechanism is needed in this case.
   */
  @Test
  public void testImplicitConversion() {

    TupleMetadata schema = new SchemaBuilder()
        .add("ti", MinorType.TINYINT)
        .add("si", MinorType.SMALLINT)
        .add("int", MinorType.INT)
        .add("bi", MinorType.BIGINT)
        .add("fl", MinorType.FLOAT4)
        .add("db", MinorType.FLOAT8)
        .add("dec", MinorType.VARDECIMAL, 10, 0)
        .buildSchema();

    // Test allowed implicit conversions.
    RowSet actual = new RowSetBuilder(fixture.allocator(), schema)
        .addRow(11,  12,  13,  14,  15,  16, 17)  // int
        .addRow(21L, 22L, 23L, 24L, 25L, 26L, 27L) // long
        .addRow(31F, 32F, 33F, 34F, 35F, 36F, 37F) // float
        .addRow(41D, 42D, 43D, 44D, 45D, 46D, 47D) // double
        .addRow(dec(51), dec(52), dec(53), dec(54), dec(55), dec(56), dec(57)) // decimal
        .build();

    final SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(11, 12, 13, 14L, 15F, 16D, dec(17))
        .addRow(21, 22, 23, 24L, 25F, 26D, dec(27))
        .addRow(31, 32, 33, 34L, 35F, 36D, dec(37))
        .addRow(41, 42, 43, 44L, 45F, 46D, dec(47))
        .addRow(51, 52, 53, 54L, 55L, 56D, dec(57))
        .build();
              RowSetUtilities.verify(expected, actual);
  }

  /**
   * The column accessors provide only int setters. For performance, the int value is
   * assumed to be of the correct range for the target column. If not, truncation of
   * the highest bytes occurs.
   * <p>
   * The assumption is, if the reader or other code expects that overflow might
   * occur, that code should be implemented in the client (or in a type conversion
   * shim), leaving the normal code path to optimize for the 99% of the cases where
   * the value is in the proper range.
   */
  @Test
  public void testImplicitConversionIntTruncation() {

    TupleMetadata schema = new SchemaBuilder()
        .add("ti", MinorType.TINYINT)
        .add("si", MinorType.SMALLINT)
        .buildSchema();

    // Test allowed implicit conversions.
    RowSet actual = new RowSetBuilder(fixture.allocator(), schema)
        .addRow(Byte.MAX_VALUE + 1, Short.MAX_VALUE + 1)
        .addRow(Byte.MAX_VALUE + 2, Short.MAX_VALUE + 2)
        .build();

    // Build the expected vector without a type converter.

    final SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(Byte.MIN_VALUE, Short.MIN_VALUE)
        .addRow(Byte.MIN_VALUE + 1, Short.MIN_VALUE + 1)
        .build();

    RowSetUtilities.verify(expected, actual);
  }

  /**
   * Overflow from double-to-int is detected.
   */
  @Test
  public void testImplicitConversionIntOverflow() {

    TupleMetadata schema = new SchemaBuilder()
        .add("int", MinorType.INT)
        .buildSchema();

    {
      RowSetBuilder builder = new RowSetBuilder(fixture.allocator(), schema);
      try {
        builder.addRow((long) Integer.MAX_VALUE + 1);
        fail();
      } catch (InvalidConversionError e) {
        // Expected
      } finally {
        builder.build().clear();
      }
    }
    {
      RowSetBuilder builder = new RowSetBuilder(fixture.allocator(), schema);
      try {
        builder.addRow((double) Integer.MAX_VALUE + 1);
        fail();
      } catch (InvalidConversionError e) {
        // Expected
      } finally {
        builder.build().clear();
      }
    }
  }

  /**
   * Implicit conversion from double (or float) follows the Java Math.round
   * rules: round to the closest long value. Readers that want other behavior
   * should insert a type-conversion shim to implement the preferred rules.
   */
  @Test
  public void testImplicitConversionDoubleClamp() {

    TupleMetadata schema = new SchemaBuilder()
        .add("bi", MinorType.BIGINT)
        .buildSchema();

    RowSet actual = new RowSetBuilder(fixture.allocator(), schema)
        .addRow(Long.MAX_VALUE * 10D)
        .addRow(Double.NaN)
        .addRow(Double.MAX_VALUE)
        .addRow(Double.MIN_VALUE)
        .addRow(Double.POSITIVE_INFINITY)
        .addRow(Double.NEGATIVE_INFINITY)
        .build();

    final SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(Long.MAX_VALUE)
        .addRow(0)
        .addRow(Long.MAX_VALUE)
        .addRow(0)
        .addRow(Long.MAX_VALUE)
        .addRow(Long.MIN_VALUE)
        .build();

    RowSetUtilities.verify(expected, actual);
  }

  /**
   * Implicit conversion from String to period using default ISO
   * format.
   */
  @Test
  public void testStringToInterval() {

    TupleMetadata outputSchema = new SchemaBuilder()
        .add("id", MinorType.INTERVALDAY)
        .add("iy", MinorType.INTERVALYEAR)
        .add("int", MinorType.INTERVAL)
        .buildSchema();

    TupleMetadata inputSchema = new SchemaBuilder()
        .add("id", MinorType.VARCHAR)
        .add("iy", MinorType.VARCHAR)
        .add("int", MinorType.VARCHAR)
        .buildSchema();

    ConversionTestFixture testFixture = new ConversionTestFixture(fixture.allocator(), outputSchema);
    testFixture.createConvertersFor(inputSchema);
    RowSet actual = testFixture
        .addRow("P2DT3H4M5S", "P9Y8M", "P9Y8M2DT3H4M5S")
        .build();

    Period p1 = Period.days(2).plusHours(3).plusMinutes(4).plusSeconds(5);
    Period p2 = Period.years(9).plusMonths(8);
    Period p3 = p1.plus(p2);
    final SingleRowSet expected = fixture.rowSetBuilder(outputSchema)
        .addRow(p1, p2, p3)
        .build();

    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testIntervalToString() {

    TupleMetadata outputSchema = new SchemaBuilder()
        .add("id", MinorType.VARCHAR)
        .add("iy", MinorType.VARCHAR)
        .add("int", MinorType.VARCHAR)
        .buildSchema();

    TupleMetadata inputSchema = new SchemaBuilder()
        .add("id", MinorType.INTERVALDAY)
        .add("iy", MinorType.INTERVALYEAR)
        .add("int", MinorType.INTERVAL)
        .buildSchema();

    Period p1 = Period.days(2).plusHours(3).plusMinutes(4).plusSeconds(5);
    Period p2 = Period.years(9).plusMonths(8);
    Period p3 = p1.plus(p2);
    ConversionTestFixture testFixture = new ConversionTestFixture(fixture.allocator(), outputSchema);
    testFixture.createConvertersFor(inputSchema);
    RowSet actual = testFixture
        .addRow(p1, p2, p3)
        .build();

    final SingleRowSet expected = fixture.rowSetBuilder(outputSchema)
        .addRow("P2DT3H4M5S", "P9Y8M", "P9Y8M2DT3H4M5S")
        .build();

    RowSetUtilities.verify(expected, actual);
  }

  /**
   * Test VARCHAR to DATE, TIME and TIMESTAMP conversion
   * using default ISO formats.
   */
  @Test
  public void testStringToDateTimeDefault() {

    TupleMetadata outputSchema = new SchemaBuilder()
        .add("date", MinorType.DATE)
        .add("time", MinorType.TIME)
        .add("ts", MinorType.TIMESTAMP)
        .buildSchema();

    TupleMetadata inputSchema = new SchemaBuilder()
        .add("date", MinorType.VARCHAR)
        .add("time", MinorType.VARCHAR)
        .add("ts", MinorType.VARCHAR)
        .buildSchema();

    ConversionTestFixture testFixture = new ConversionTestFixture(fixture.allocator(), outputSchema);
    testFixture.createConvertersFor(inputSchema);
    RowSet actual = testFixture
        .addRow("2019-03-28", "12:34:56", "2019-03-28T12:34:56")
        .build();

    LocalTime lt = LocalTime.of(12, 34, 56);
    LocalDate ld = LocalDate.of(2019, 3, 28);
    Instant ts = LocalDateTime.of(ld, lt).toInstant(ZoneOffset.UTC);
    final SingleRowSet expected = fixture.rowSetBuilder(outputSchema)
        .addRow(ld, lt, ts)
        .build();

    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testStringToDateTimeCustom() {

    TupleMetadata outputSchema = new SchemaBuilder()
        .add("date", MinorType.DATE)
        .add("time", MinorType.TIME)
        .add("ts", MinorType.TIMESTAMP)
        .buildSchema();

    outputSchema.metadata("date").setFormat("M/d/yyyy");
    outputSchema.metadata("time").setFormat("hh:mm:ss a");
    outputSchema.metadata("ts").setFormat("M/d/yyyy hh:mm:ss a VV");

    TupleMetadata inputSchema = new SchemaBuilder()
        .add("date", MinorType.VARCHAR)
        .add("time", MinorType.VARCHAR)
        .add("ts", MinorType.VARCHAR)
        .buildSchema();

    ConversionTestFixture testFixture = new ConversionTestFixture(fixture.allocator(), outputSchema);
    testFixture.createConvertersFor(inputSchema);
    RowSet actual = testFixture
        .addRow("3/28/2019", "12:34:56 PM", "3/28/2019 12:34:56 PM Z")
        .build();

    LocalTime lt = LocalTime.of(12, 34, 56);
    LocalDate ld = LocalDate.of(2019, 3, 28);
    Instant ts = LocalDateTime.of(ld, lt).toInstant(ZoneOffset.UTC);
    final SingleRowSet expected = fixture.rowSetBuilder(outputSchema)
        .addRow(ld, lt, ts)
        .build();

    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testDateTimeToString() {

    TupleMetadata outputSchema = new SchemaBuilder()
        .add("date", MinorType.VARCHAR)
        .add("time", MinorType.VARCHAR)
        .add("ts", MinorType.VARCHAR)
        .buildSchema();

    TupleMetadata inputSchema = new SchemaBuilder()
        .add("date", MinorType.DATE)
        .add("time", MinorType.TIME)
        .add("ts", MinorType.TIMESTAMP)
        .buildSchema();

    LocalTime lt = LocalTime.of(12, 34, 56);
    LocalDate ld = LocalDate.of(2019, 3, 28);
    Instant ts = LocalDateTime.of(ld, lt).atZone(ZoneId.systemDefault()).toInstant();
    ConversionTestFixture testFixture = new ConversionTestFixture(fixture.allocator(), outputSchema);
    testFixture.createConvertersFor(inputSchema);
    RowSet actual = testFixture
        .addRow(ld, lt, ts)
        .build();

    final SingleRowSet expected = fixture.rowSetBuilder(outputSchema)
        .addRow("2019-03-28", "12:34:56", "2019-03-28T12:34:56")
        .build();

    RowSetUtilities.verify(expected, actual);
  }

  /**
   * Test conversion to/from Java-style Booleans.
   */
  @Test
  public void testBooleanToFromString() {

    TupleMetadata outputSchema = new SchemaBuilder()
        .add("bool", MinorType.BIT)
        .add("str", MinorType.VARCHAR)
        .buildSchema();

    TupleMetadata inputSchema = new SchemaBuilder()
        .add("bool", MinorType.VARCHAR)
        .add("str", MinorType.BIT)
        .buildSchema();

    ConversionTestFixture testFixture = new ConversionTestFixture(fixture.allocator(), outputSchema);
    testFixture.createConvertersFor(inputSchema);
    RowSet actual = testFixture
        .addRow("true", false)
        .addRow("false", true)
        .addRow("TRUE", false)
        .addRow("FALSE", true)
        .build();

    final SingleRowSet expected = fixture.rowSetBuilder(outputSchema)
        .addRow(true, "false")
        .addRow(false, "true")
        .addRow(true, "false")
        .addRow(false, "true")
        .build();

    RowSetUtilities.verify(expected, actual);
  }

  private static BigDecimal dec(String value) {
    return new BigDecimal(value);
  }

  @Test
  public void testDecimalFromString() {

    TupleMetadata outputSchema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .add("dec", MinorType.VARDECIMAL, 4, 2)
        .buildSchema();

    TupleMetadata inputSchema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .add("dec", MinorType.VARCHAR)
        .buildSchema();

    ConversionTestFixture testFixture = new ConversionTestFixture(fixture.allocator(), outputSchema);
    testFixture.createConvertersFor(inputSchema);
    RowSet actual = testFixture
        .addRow(1, "0")
        .addRow(2, "-0")
        .addRow(3, "0.12")
        .addRow(4, "1.23")
        .addRow(5, "12.34")
        // Rounding occurs for VARDECIMAL
        .addRow(6, "23.456")
        .addRow(7, "-99.99")
        .build();

    final SingleRowSet expected = fixture.rowSetBuilder(outputSchema)
        .addRow(1, dec("0"))
        .addRow(2, dec("-0"))
        .addRow(3, dec("0.12"))
        .addRow(4, dec("1.23"))
        .addRow(5, dec("12.34"))
        .addRow(6, dec("23.46"))
        .addRow(7, dec("-99.99"))
        .build();

    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testDecimalOverflow() {

    TupleMetadata outputSchema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .add("dec", MinorType.VARDECIMAL, 4, 2)
        .buildSchema();

    TupleMetadata inputSchema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .add("dec", MinorType.VARCHAR)
        .buildSchema();

    ConversionTestFixture testFixture = new ConversionTestFixture(fixture.allocator(), outputSchema);
    testFixture.createConvertersFor(inputSchema);
    try {
      testFixture.addRow(1, "1234567.89");
      fail();
    } catch (UserException e) {
      // Expected
    }
    testFixture.build().clear();
  }

  private static void expect(ConversionType type, ConversionDefn defn) {
    assertEquals(type, defn.type);
  }

  /**
   * Test the conversion type for a subset of type pairs.
   */
  @Test
  public void testBasicConversionType() {
    StandardConversions conversions = StandardConversions.builder().build();
    TupleMetadata schema = new SchemaBuilder()
        .add("ti", MinorType.TINYINT)
        .add("si", MinorType.SMALLINT)
        .add("int", MinorType.INT)
        .add("bi", MinorType.BIGINT)
        .add("fl", MinorType.FLOAT4)
        .add("db", MinorType.FLOAT8)
        .add("dec", MinorType.VARDECIMAL, 10, 0)
        .add("str", MinorType.VARCHAR)
        .buildSchema();
    ColumnMetadata tinyIntCol = schema.metadata("ti");
    ColumnMetadata smallIntCol = schema.metadata("si");
    ColumnMetadata intCol = schema.metadata("int");
    ColumnMetadata bigIntCol = schema.metadata("bi");
    ColumnMetadata float4Col = schema.metadata("fl");
    ColumnMetadata float8Col = schema.metadata("db");
    ColumnMetadata decimalCol = schema.metadata("dec");
    ColumnMetadata stringCol = schema.metadata("str");

    // TinyInt --> x
    expect(ConversionType.NONE, conversions.analyze(tinyIntCol, tinyIntCol));
    expect(ConversionType.IMPLICIT, conversions.analyze(tinyIntCol, smallIntCol));
    expect(ConversionType.IMPLICIT, conversions.analyze(tinyIntCol, intCol));
    expect(ConversionType.IMPLICIT, conversions.analyze(tinyIntCol, bigIntCol));
    expect(ConversionType.IMPLICIT, conversions.analyze(tinyIntCol, float4Col));
    expect(ConversionType.IMPLICIT, conversions.analyze(tinyIntCol, float8Col));
    expect(ConversionType.EXPLICIT, conversions.analyze(tinyIntCol, decimalCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(tinyIntCol, stringCol));

    // SmallInt --> x
    expect(ConversionType.IMPLICIT_UNSAFE, conversions.analyze(smallIntCol, tinyIntCol));
    expect(ConversionType.NONE, conversions.analyze(smallIntCol, smallIntCol));
    expect(ConversionType.IMPLICIT, conversions.analyze(smallIntCol, intCol));
    expect(ConversionType.IMPLICIT, conversions.analyze(smallIntCol, bigIntCol));
    expect(ConversionType.IMPLICIT, conversions.analyze(smallIntCol, float4Col));
    expect(ConversionType.IMPLICIT, conversions.analyze(smallIntCol, float8Col));
    expect(ConversionType.EXPLICIT, conversions.analyze(smallIntCol, decimalCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(smallIntCol, stringCol));

    // Int --> x
    expect(ConversionType.IMPLICIT_UNSAFE, conversions.analyze(intCol, tinyIntCol));
    expect(ConversionType.IMPLICIT_UNSAFE, conversions.analyze(intCol, smallIntCol));
    expect(ConversionType.NONE, conversions.analyze(intCol, intCol));
    expect(ConversionType.IMPLICIT, conversions.analyze(intCol, bigIntCol));
    expect(ConversionType.IMPLICIT, conversions.analyze(intCol, float4Col));
    expect(ConversionType.IMPLICIT, conversions.analyze(intCol, float8Col));
    expect(ConversionType.EXPLICIT, conversions.analyze(intCol, decimalCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(intCol, stringCol));

    // BigInt --> x
    expect(ConversionType.IMPLICIT_UNSAFE, conversions.analyze(bigIntCol, tinyIntCol));
    expect(ConversionType.IMPLICIT_UNSAFE, conversions.analyze(bigIntCol, smallIntCol));
    expect(ConversionType.IMPLICIT_UNSAFE, conversions.analyze(bigIntCol, intCol));
    expect(ConversionType.NONE, conversions.analyze(bigIntCol, bigIntCol));
    expect(ConversionType.IMPLICIT, conversions.analyze(bigIntCol, float4Col));
    expect(ConversionType.IMPLICIT, conversions.analyze(bigIntCol, float8Col));
    expect(ConversionType.EXPLICIT, conversions.analyze(bigIntCol, decimalCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(bigIntCol, stringCol));

    // Float4 --> x
    expect(ConversionType.IMPLICIT_UNSAFE, conversions.analyze(float4Col, tinyIntCol));
    expect(ConversionType.IMPLICIT_UNSAFE, conversions.analyze(float4Col, smallIntCol));
    expect(ConversionType.IMPLICIT_UNSAFE, conversions.analyze(float4Col, intCol));
    expect(ConversionType.IMPLICIT_UNSAFE, conversions.analyze(float4Col, bigIntCol));
    expect(ConversionType.NONE, conversions.analyze(float4Col, float4Col));
    expect(ConversionType.IMPLICIT, conversions.analyze(float4Col, float8Col));
    expect(ConversionType.EXPLICIT, conversions.analyze(float4Col, decimalCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(float4Col, stringCol));

    // Float8 --> x
    expect(ConversionType.IMPLICIT_UNSAFE, conversions.analyze(float8Col, tinyIntCol));
    expect(ConversionType.IMPLICIT_UNSAFE, conversions.analyze(float8Col, smallIntCol));
    expect(ConversionType.IMPLICIT_UNSAFE, conversions.analyze(float8Col, intCol));
    expect(ConversionType.IMPLICIT_UNSAFE, conversions.analyze(float8Col, bigIntCol));
    expect(ConversionType.IMPLICIT_UNSAFE, conversions.analyze(float8Col, float4Col));
    expect(ConversionType.NONE, conversions.analyze(float8Col, float8Col));
    expect(ConversionType.EXPLICIT, conversions.analyze(float8Col, decimalCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(float8Col, stringCol));

    // Decimal --> x
    expect(ConversionType.EXPLICIT, conversions.analyze(decimalCol, tinyIntCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(decimalCol, smallIntCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(decimalCol, intCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(decimalCol, bigIntCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(decimalCol, float4Col));
    expect(ConversionType.EXPLICIT, conversions.analyze(decimalCol, float8Col));
    expect(ConversionType.NONE, conversions.analyze(decimalCol, decimalCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(decimalCol, stringCol));

    // VarChar --> x
    expect(ConversionType.EXPLICIT, conversions.analyze(stringCol, tinyIntCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(stringCol, smallIntCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(stringCol, intCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(stringCol, bigIntCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(stringCol, float4Col));
    expect(ConversionType.EXPLICIT, conversions.analyze(stringCol, float8Col));
    expect(ConversionType.EXPLICIT, conversions.analyze(stringCol, decimalCol));
    expect(ConversionType.NONE, conversions.analyze(stringCol, stringCol));
  }

  /**
   * Test the specialized types: conversation to/from string.
   */
  @Test
  public void testSpecialConversionType() {
    StandardConversions conversions = StandardConversions.builder().build();
    TupleMetadata schema = new SchemaBuilder()
        .add("time", MinorType.TIME)
        .add("date", MinorType.DATE)
        .add("ts", MinorType.TIMESTAMP)
        .add("interval", MinorType.INTERVAL)
        .add("year", MinorType.INTERVALYEAR)
        .add("day", MinorType.INTERVALDAY)
        .add("int", MinorType.INT)
        .add("bi", MinorType.BIGINT)
        .add("str", MinorType.VARCHAR)
        .buildSchema();
    ColumnMetadata timeCol = schema.metadata("time");
    ColumnMetadata dateCol = schema.metadata("date");
    ColumnMetadata tsCol = schema.metadata("ts");
    ColumnMetadata intervalCol = schema.metadata("interval");
    ColumnMetadata yearCol = schema.metadata("year");
    ColumnMetadata dayCol = schema.metadata("day");
    ColumnMetadata intCol = schema.metadata("int");
    ColumnMetadata bigIntCol = schema.metadata("bi");
    ColumnMetadata stringCol = schema.metadata("str");

    // TIME
    expect(ConversionType.NONE, conversions.analyze(timeCol, timeCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(timeCol, stringCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(stringCol, timeCol));
    expect(ConversionType.IMPLICIT, conversions.analyze(intCol, timeCol));
    expect(ConversionType.IMPLICIT, conversions.analyze(timeCol, intCol));

    // DATE
    expect(ConversionType.NONE, conversions.analyze(dateCol, dateCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(dateCol, stringCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(stringCol, dateCol));
    expect(ConversionType.IMPLICIT, conversions.analyze(bigIntCol, dateCol));
    expect(ConversionType.IMPLICIT, conversions.analyze(dateCol, bigIntCol));

    // TIMESTAMP
    expect(ConversionType.NONE, conversions.analyze(tsCol, tsCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(tsCol, stringCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(stringCol, tsCol));
    expect(ConversionType.IMPLICIT, conversions.analyze(bigIntCol, tsCol));
    expect(ConversionType.IMPLICIT, conversions.analyze(tsCol, bigIntCol));

    // INTERVAL
    expect(ConversionType.NONE, conversions.analyze(intervalCol, intervalCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(intervalCol, stringCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(stringCol, intervalCol));

    // INTERVALYEAR
    expect(ConversionType.NONE, conversions.analyze(yearCol, yearCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(yearCol, stringCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(stringCol, yearCol));

    // INTERVALDAY
    expect(ConversionType.NONE, conversions.analyze(dayCol, dayCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(dayCol, stringCol));
    expect(ConversionType.EXPLICIT, conversions.analyze(stringCol, dayCol));
  }

  /**
   * Test the properties for how to handle blanks on string-to-number
   * conversions.
   */
  @Test
  public void testBlankOptions() {

    // Nullable
    try {
      doTestBlanks(DataMode.OPTIONAL, null, null, null);
    } catch (InvalidConversionError e) {
      // Expected
    }
    doTestBlanks(DataMode.OPTIONAL, ColumnMetadata.BLANK_AS_NULL,
        null, null);
    doTestBlanks(DataMode.OPTIONAL, ColumnMetadata.BLANK_AS_ZERO,
        null, 0);

    // Non-nullable
    doTestBlanks(DataMode.REQUIRED, null, null, 20);
    doTestBlanks(DataMode.REQUIRED, ColumnMetadata.BLANK_AS_NULL,
        null, 20);
    doTestBlanks(DataMode.REQUIRED, ColumnMetadata.BLANK_AS_ZERO,
        null, 0);

    // Property on column
    doTestBlanks(DataMode.REQUIRED, null,
        ColumnMetadata.BLANK_AS_NULL, 20);
    doTestBlanks(DataMode.REQUIRED, null,
        ColumnMetadata.BLANK_AS_ZERO, 0);

    // Properties on both: column takes precedence
    doTestBlanks(DataMode.REQUIRED, ColumnMetadata.BLANK_AS_ZERO,
        ColumnMetadata.BLANK_AS_NULL, 20);
    doTestBlanks(DataMode.REQUIRED, ColumnMetadata.BLANK_AS_NULL,
        ColumnMetadata.BLANK_AS_ZERO, 0);
  }

  private void doTestBlanks(DataMode mode, String frameworkOption, String colOption, Integer value) {
    TupleMetadata outputSchema = new SchemaBuilder()
        .add("col", MinorType.INT, mode)
        .buildSchema();
    ColumnMetadata colSchema = outputSchema.metadata("col");
    colSchema.setProperty(ColumnMetadata.DEFAULT_VALUE_PROP, "20");

    TupleMetadata inputSchema = new SchemaBuilder()
        .addNullable("col", MinorType.VARCHAR)
        .buildSchema();
    if (colOption != null) {
      colSchema = inputSchema.metadata("col");
      colSchema.setProperty(ColumnMetadata.BLANK_AS_PROP, colOption);
    }

    Map<String, String> props = null;
    if (frameworkOption != null) {
      props = new HashMap<>();
      props.put(ColumnMetadata.BLANK_AS_PROP, frameworkOption);
    }
    ConversionTestFixture testFixture = new ConversionTestFixture(fixture.allocator(), outputSchema);
    testFixture.withProperties(props);
    testFixture.createConvertersFor(inputSchema);
    try {
      testFixture
        .addSingleCol("")
        .addSingleCol("  ")
        .addSingleCol("10")
        .addSingleCol(" 11  ");
    }
    catch (Exception e) {
      testFixture.build().clear();
      throw e;
    }
    RowSet actual = testFixture.build();

    final SingleRowSet expected = fixture.rowSetBuilder(outputSchema)
        .addSingleCol(value)
        .addSingleCol(value)
        .addSingleCol(10)
        .addSingleCol(11)
        .build();

    RowSetUtilities.verify(expected, actual);
  }
}
