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
package org.apache.drill.exec.store.easy.json.loader;

import static org.apache.drill.test.rowSet.RowSetUtilities.dec;
import static org.apache.drill.test.rowSet.RowSetUtilities.intArray;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import org.apache.drill.categories.JsonTest;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test extended types with a schema. We see the schema slightly before
 * we see the full extended type syntax, especially if a field is null
 * or "relaxed" in the first row.
 * <p>
 * The provided type must match the extended type: an extended type of
 * {@code numberLong} must have a provided type of {@code BIGINT}, for example.
 */
@Category(JsonTest.class)
public class TestExtendedWithSchema extends BaseJsonLoaderTest {

  @Test
  public void testInt() {
    String json =
        "{ a: { \"$numberInt\": 10 },\n" +
        "  b: null,\n" +
        "  c: { \"$numberInt\": \"30\" },\n" +
        "  d: 40,\n" +
        "  e: \"50\" }\n" +
        "{ a: 110,\n" +
        "  b: 120,\n" +
        "  c: null,\n" +
        "  d: { \"$numberInt\": 140 },\n" +
        "  e: { \"$numberInt\": 150 } }\n" +
        "{ a: { \"$numberInt\": \"210\" },\n" +
        "  b: { \"$numberInt\": 220 },\n" +
        "  c: 230,\n" +
        "  d: null,\n" +
        "  e: { \"$numberInt\": \"250\" } }";
    TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable("b", MinorType.INT)
        .addNullable("c", MinorType.INT)
        .addNullable("d", MinorType.INT)
        .add("e", MinorType.INT)
        .build();
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.builder.providedSchema(providedSchema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    RowSet expected = fixture.rowSetBuilder(providedSchema)
        .addRow( 10, null,   30,   40,  50)
        .addRow(110,  120, null,  140, 150)
        .addRow(210,  220,  230, null, 250)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  /**
   * Test only the int array. All other arrays use the same
   * code path.
   */
  @Test
  public void testIntArray() {
    String json =
        "{ a: [ { \"$numberInt\": 10 }, 20, { \"$numberInt\": \"30\" } ],\n" +
        "  b: null,\n" +
        "  c: [],\n" +
        "  d: [ { \"$numberInt\": 40 }, \"50\", null ] }\n" +
        "{ a: [],\n" +
        "  b: [ { \"$numberInt\": 140 }, \"150\", null ],\n" +
        "  c: [ { \"$numberInt\": 110 }, 120, { \"$numberInt\": \"130\" } ],\n" +
        "  d: null }\n";
    TupleMetadata providedSchema = new SchemaBuilder()
        .addArray("a", MinorType.INT)
        .addArray("b", MinorType.INT)
        .addArray("c", MinorType.INT)
        .addArray("d", MinorType.INT)
        .build();
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.builder.providedSchema(providedSchema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    RowSet expected = fixture.rowSetBuilder(providedSchema)
        .addRow(intArray(10, 20, 30),
                intArray(),
                intArray(),
                intArray(40, 50, 0))
        .addRow(intArray(),
                intArray(140, 150, 0),
                intArray(110, 120, 130),
                intArray())
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testLong() {
    String json =
        "{ a: { \"$numberLong\": 10 },\n" +
        "  b: null,\n" +
        "  c: { \"$numberLong\": \"30\" },\n" +
        "  d: 40,\n" +
        "  e: \"50\" }\n" +
        "{ a: 110,\n" +
        "  b: 120,\n" +
        "  c: null,\n" +
        "  d: { \"$numberLong\": 140 },\n" +
        "  e: { \"$numberLong\": 150 } }\n" +
        "{ a: { \"$numberLong\": \"210\" },\n" +
        "  b: { \"$numberLong\": 220 },\n" +
        "  c: 230,\n" +
        "  d: null,\n" +
        "  e: { \"$numberLong\": \"250\" } }";
    TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.BIGINT)
        .addNullable("b", MinorType.BIGINT)
        .addNullable("c", MinorType.BIGINT)
        .addNullable("d", MinorType.BIGINT)
        .add("e", MinorType.BIGINT)
        .build();
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.builder.providedSchema(providedSchema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    RowSet expected = fixture.rowSetBuilder(providedSchema)
        .addRow( 10L,  null,   30L,   40L,  50L)
        .addRow(110L,  120L,  null,  140L, 150L)
        .addRow(210L,  220L,  230L,  null, 250L)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testDouble() {
    String json =
        "{ a: { \"$numberDouble\": 10 },\n" +
        "  b: null,\n" +
        "  c: { \"$numberDouble\": \"30\" },\n" +
        "  d: 40,\n" +
        "  e: \"50\" }\n" +
        "{ a: 110,\n" +
        "  b: 120,\n" +
        "  c: null,\n" +
        "  d: { \"$numberDouble\": 140 },\n" +
        "  e: { \"$numberDouble\": 150 } }\n" +
        "{ a: { \"$numberDouble\": \"210\" },\n" +
        "  b: { \"$numberDouble\": 220 },\n" +
        "  c: 230,\n" +
        "  d: null,\n" +
        "  e: { \"$numberDouble\": \"250\" } }";
    TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.FLOAT8)
        .addNullable("b", MinorType.FLOAT8)
        .addNullable("c", MinorType.FLOAT8)
        .addNullable("d", MinorType.FLOAT8)
        .add("e", MinorType.FLOAT8)
        .build();
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.builder.providedSchema(providedSchema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    RowSet expected = fixture.rowSetBuilder(providedSchema)
        .addRow( 10D,  null,   30D,   40D,  50D)
        .addRow(110D,  120D,  null,  140D, 150D)
        .addRow(210D,  220D,  230D,  null, 250D)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testDecimal() {
    String json =
        "{ a: { \"$numberDecimal\": 10 },\n" +
        "  b: null,\n" +
        "  c: { \"$numberDecimal\": \"30\" },\n" +
        "  d: 40,\n" +
        "  e: \"50\" }\n" +
        "{ a: 110,\n" +
        "  b: 120,\n" +
        "  c: null,\n" +
        "  d: { \"$numberDecimal\": 140 },\n" +
        "  e: { \"$numberDecimal\": 150 } }\n" +
        "{ a: { \"$numberDecimal\": \"210\" },\n" +
        "  b: { \"$numberDecimal\": 220 },\n" +
        "  c: 230,\n" +
        "  d: null,\n" +
        "  e: { \"$numberDecimal\": \"250\" } }";
    TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.VARDECIMAL, 4, 0)
        .addNullable("b", MinorType.VARDECIMAL, 5, 1)
        .addNullable("c", MinorType.VARDECIMAL, 6, 2)
        .addNullable("d", MinorType.VARDECIMAL, 7, 3)
        .add("e", MinorType.VARDECIMAL, 8, 4)
        .build();
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.builder.providedSchema(providedSchema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    RowSet expected = fixture.rowSetBuilder(providedSchema)
        .addRow(dec("10"),         null,   dec("30"),   dec("40"),  dec("50"))
        .addRow(dec("110"),  dec("120"),        null,  dec("140"), dec("150"))
        .addRow(dec("210"),  dec("220"),  dec("230"),        null, dec("250"))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testDate() {
    LocalDateTime local = LocalDateTime.of(2020, 4, 21, 11, 22, 33);
    Instant instant = local.atZone(ZoneId.systemDefault()).toInstant();
    long ts = instant.toEpochMilli();
    String utc = DateTimeFormatter.ISO_INSTANT.format(instant);
    long localTs = ts + TimeZone.getDefault().getOffset(ts);
    String json =
        "{ a: { \"$date\": \"" + utc + "\" },\n" +
        "  b: null,\n" +
        "  c: { \"$date\": " + ts + " },\n" +
        "  d: " + ts + ",\n" +
        "  e: \"" + utc + "\",\n" +
        "  f: { \"$date\": { \"$numberLong\": " + ts + " } } }\n" +
        "{ a: \"" + utc + "\",\n" +
        "  b: " + ts + ",\n" +
        "  c: null,\n" +
        "  d: { \"$date\": \"" + utc + "\" },\n" +
        "  e: { \"$date\": { \"$numberLong\": " + ts + " } },\n" +
        "  f: " + ts + " }\n";
    TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.TIMESTAMP)
        .addNullable("b", MinorType.TIMESTAMP)
        .addNullable("c", MinorType.TIMESTAMP)
        .addNullable("d", MinorType.TIMESTAMP)
        .add("e", MinorType.TIMESTAMP)
        .add("f", MinorType.TIMESTAMP)
        .build();
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.builder.providedSchema(providedSchema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    RowSet expected = fixture.rowSetBuilder(providedSchema)
        .addRow(localTs,    null, localTs, localTs, localTs, localTs)
        .addRow(localTs, localTs,    null, localTs, localTs, localTs)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  // Drill extension: date only
  @Test
  public void testDateDay() {
    String json =
        "{ a: { \"$dateDay\": \"2020-04-21\" },\n" +
        "  b: null,\n" +
        "  c: \"2020-04-21\" }\n" +
        "{ a: \"2020-04-21\",\n" +
        "  b: { \"$dateDay\": \"2020-04-21\" },\n" +
        "  c: null }\n";
    TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.DATE)
        .addNullable("b", MinorType.DATE)
        .addNullable("c", MinorType.DATE)
        .build();
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.builder.providedSchema(providedSchema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    LocalDate date = LocalDate.of(2020, 04, 21);
    RowSet expected = fixture.rowSetBuilder(providedSchema)
        .addRow(date, null, date)
        .addRow(date, date, null)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  // Drill extension: time only
  @Test
  public void testTime() {
    String json =
        "{ a: { \"$time\": \"11:22:33\" },\n" +
        "  b: null,\n" +
        "  c: \"11:22:33\" }\n" +
        "{ a: \"11:22:33\",\n" +
        "  b: { \"$time\": \"11:22:33\" },\n" +
        "  c: null }\n";
    TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.TIME)
        .addNullable("b", MinorType.TIME)
        .addNullable("c", MinorType.TIME)
        .build();
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.builder.providedSchema(providedSchema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    LocalTime time = LocalTime.of(11, 22, 33);
    RowSet expected = fixture.rowSetBuilder(providedSchema)
        .addRow(time, null, time)
        .addRow(time, time, null)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  // Drill extension: time interval
  @Test
  public void testInterval() {
    String json =
        "{ a: { \"$interval\": \"P1Y2M3DT4H5M6S\"  },\n" +
        "  b: null,\n" +
        "  c: \"P1Y2M3D\" }\n" +
        "{ a: \"P1Y2M3D\",\n" +
        "  b: { \"$interval\": \"P1Y2M3DT4H5M6S\"  },\n" +
        "  c: null }\n";
    TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.INTERVAL)
        .addNullable("b", MinorType.INTERVAL)
        .addNullable("c", MinorType.INTERVAL)
        .build();
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.builder.providedSchema(providedSchema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    org.joda.time.Period full = org.joda.time.Period.years(1).withMonths(2).withDays(3).withHours(4).withMinutes(5).withSeconds(6);
    org.joda.time.Period ymd = org.joda.time.Period.years(1).withMonths(2).withDays(3);
    RowSet expected = fixture.rowSetBuilder(providedSchema)
        .addRow(full, null,  ymd)
        .addRow( ymd, full, null)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testBinary() {
    String json =
        "{ a: { \"$binary\": { base64: \"ZHJpbGw=\", subType: \"0\" } },\n" +
        "  b: { \"$binary\": { subType: \"0\", base64: \"ZHJpbGw=\" } },\n" +
        "  c: { \"$binary\": { base64: \"ZHJpbGw=\" } },\n" +
        "  d: null,\n" +
        "  e: { \"$binary\": \"ZHJpbGw=\", \"$type\": 1 },\n" +
        "  f: { \"$type\": 1, \"$binary\": \"ZHJpbGw=\" },\n" +
        "  g: { \"$binary\": \"ZHJpbGw=\" },\n" +
        "  h: \"ZHJpbGw=\" }\n" +
        "{ a: null,\n" +
        "  b: { \"$binary\": \"ZHJpbGw=\", \"$type\": 1 },\n" +
        "  c: { \"$type\": 1, \"$binary\": \"ZHJpbGw=\" },\n" +
        "  d: { \"$binary\": \"ZHJpbGw=\" },\n" +
        "  e: \"ZHJpbGw=\",\n" +
        "  f: { \"$binary\": { base64: \"ZHJpbGw=\", subType: \"0\" } },\n" +
        "  g: { \"$binary\": { subType: \"0\", base64: \"ZHJpbGw=\" } },\n" +
        "  h: { \"$binary\": { base64: \"ZHJpbGw=\" } } }\n";
    TupleMetadata providedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.VARBINARY)
        .add("b", MinorType.VARBINARY)
        .add("c", MinorType.VARBINARY)
        .addNullable("d", MinorType.VARBINARY)
        .add("e", MinorType.VARBINARY)
        .add("f", MinorType.VARBINARY)
        .add("g", MinorType.VARBINARY)
        .add("h", MinorType.VARBINARY)
        .build();
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.builder.providedSchema(providedSchema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    byte[] bytes = "Drill".getBytes();
    RowSet expected = fixture.rowSetBuilder(providedSchema)
        .addRow(bytes, bytes, bytes,  null, bytes, bytes, bytes, bytes)
        .addRow( null, bytes, bytes, bytes, bytes, bytes, bytes, bytes)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

}
