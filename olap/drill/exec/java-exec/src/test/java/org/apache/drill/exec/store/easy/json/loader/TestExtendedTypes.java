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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import static org.apache.drill.test.rowSet.RowSetUtilities.dec;
import static org.apache.drill.test.rowSet.RowSetUtilities.mapValue;

import org.apache.drill.categories.FlakyTest;
import org.apache.drill.categories.JsonTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({JsonTest.class, FlakyTest.class})
public class TestExtendedTypes extends BaseJsonLoaderTest {

  @Test
  public void testInt() {
    String json =
        "{ a: { \"$numberInt\": 10 } }\n" +
        "{ a: null }\n" +
        "{ a: { \"$numberInt\": \"30\" } }\n" +
        "{ a: 40 }\n" +
        "{ a: \"50\" }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.INT)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10)
        .addSingleCol(null)
        .addRow(30)
        .addRow(40)
        .addRow(50)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testLong() {
    String json =
        "{ a: { \"$numberLong\": 10 } }\n" +
        "{ a: null }\n" +
        "{ a: { \"$numberLong\": \"30\" } }\n" +
        "{ a: 40 }\n" +
        "{ a: \"50\" }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10L)
        .addSingleCol(null)
        .addRow(30L)
        .addRow(40L)
        .addRow(50L)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testDecimal() {
    String json =
        "{ a: { \"$numberDecimal\": 10 } }\n" +
        "{ a: null }\n" +
        "{ a: { \"$numberDecimal\": \"30\" } }\n" +
        "{ a: { \"$numberDecimal\": 40.2345 } }\n" +
        "{ a: 60 }\n" +
        "{ a: \"70.890\" }\n" +
        "{ a: 80.765 }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.VARDECIMAL, 38, 10)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(dec("10"))
        .addSingleCol(null)
        .addRow(dec("30"))
        .addRow(dec("40.2345"))
        .addRow(dec("60"))
        .addRow(dec("70.89"))
        .addRow(dec("80.765"))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testDouble() {
    String json =
        "{ a: { \"$numberDouble\": 10 } }\n" +
        "{ a: null }\n" +
        "{ a: { \"$numberDouble\": \"30\" } }\n" +
        "{ a: { \"$numberDouble\": 40.125 } }\n" +
        "{ a: 60 }\n" +
        "{ a: \"70.125\" }\n" +
        "{ a: 80.375 }\n" +
        "{ a: { \"$numberDouble\": \"-Infinity\" } }\n" +
        "{ a: { \"$numberDouble\": \"Infinity\" } }\n" +
        "{ a: { \"$numberDouble\": \"NaN\" } }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.FLOAT8)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10D)
        .addSingleCol(null)
        .addRow(30D)
        .addRow(40.125D)
        .addRow(60D)
        .addRow(70.125D)
        .addRow(80.375D)
        .addRow(Double.NEGATIVE_INFINITY)
        .addRow(Double.POSITIVE_INFINITY)
        .addRow(Double.NaN)
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
        // V1 string, V2 relaxed
        "{ a: { \"$date\": \"" + utc + "\" } }\n" +
        // V1 "shell mode"
        "{ a: { \"$date\": " + ts + " } }\n" +
        "{ a: null }\n" +
        // V2 canonical
        "{ a: { \"$date\": { \"$numberLong\": " + ts + " } } }\n" +
        // Harmless extensions, only valid after the above
        "{ a: " + ts + " }\n" +
        "{ a: \"" + utc + "\" }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.TIMESTAMP)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(localTs)
        .addRow(localTs)
        .addSingleCol(null)
        .addRow(localTs)
        .addRow(localTs)
        .addRow(localTs)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testBinary() {
    String json =
        // V2 format
        "{ a: { \"$binary\": { base64: \"ZHJpbGw=\", subType: \"0\" } } }\n" +
        "{ a: { \"$binary\": { subType: \"0\", base64: \"ZHJpbGw=\" } } }\n" +
        // Harmless extension
        "{ a: { \"$binary\": { base64: \"ZHJpbGw=\" } } }\n" +
        "{ a: null }\n" +
        // V1 format
        "{ a: { \"$binary\": \"ZHJpbGw=\", \"$type\": 1 } }\n" +
        // Drill-supported variation of V1
        "{ a: { \"$type\": 1, \"$binary\": \"ZHJpbGw=\" } }\n" +
        // Harmless extension
        "{ a: { \"$binary\": \"ZHJpbGw=\" } }\n" +
        // Only valid after the above
        "{ a: \"ZHJpbGw=\" }\n";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.VARBINARY)
        .build();
    byte[] bytes = "Drill".getBytes();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(bytes)
        .addRow(bytes)
        .addRow(bytes)
        .addSingleCol(null)
        .addRow(bytes)
        .addRow(bytes)
        .addRow(bytes)
        .addRow(bytes)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testObjectID() {
    String json =
        "{ a: { \"$oid\": \"foo\" } }\n" +
        // Harmless extension
        "{ a: null }\n" +
        // Only valid after the above
        "{ a: \"foo\" }\n";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.VARCHAR)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow("foo")
        .addSingleCol(null)
        .addRow("foo")
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  // A Mongo document is just a regular JSON map.
  @Test
  public void testDocument() {
    String json =
        "{ m: { a: { \"$numberLong\": 10 }, b: \"foo\" } }\n" +
        // Harmless extension
        "{ m: null }\n";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addMap("m")
          .addNullable("a", MinorType.BIGINT)
          .addNullable("b", MinorType.VARCHAR)
          .resumeSchema()
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(mapValue(10L, "foo"))
        .addSingleCol(mapValue(null, null))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  // Drill extension: date only
  @Test
  public void testDateDay() {
    String json =
        "{ a: { \"$dateDay\": \"2020-04-21\" } }\n" +
        "{ a: null }\n" +
        "{ a: \"2020-04-21\" }\n";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.DATE)
        .build();
    LocalDate date = LocalDate.of(2020, 04, 21);
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(date)
        .addSingleCol(null)
        .addRow(date)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  // Drill extension: time only
  @Test
  public void testTime() {
    String json =
        "{ a: { \"$time\": \"11:22:33\" } }\n" +
        "{ a: { \"$time\": \"11:22:33.123\" } }\n" +
        // Drill's assumed format, though not really valid
        "{ a: { \"$time\": \"11:22:33.123Z\" } }\n" +
        "{ a: null }\n" +
        "{ a: \"11:22:33\" }\n";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.TIME)
        .build();
    LocalTime time  = LocalTime.of(11, 22, 33);
    LocalTime time2  = LocalTime.of(11, 22, 33, 123_000_000);
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(time)
        .addRow(time2)
        .addRow(time2)
        .addSingleCol(null)
        .addRow(time)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  // Drill extension: time interval
  @Test
  public void testInterval() {
    String json =
        "{ a: { \"$interval\": \"P1Y2M3DT4H5M6S\" } }\n" +
        "{ a: { \"$interval\": \"P1Y2M3D\" } }\n" +
        "{ a: { \"$interval\": \"PT4H5M6S\" } }\n" +
        "{ a: null }\n" +
        "{ a: \"P1Y2M3DT4H5M6S\" }\n" +
        "{ a: \"P1Y2M3D\" }\n" +
        "{ a: \"PT4H5M6S\" }\n";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.INTERVAL)
        .build();
    org.joda.time.Period full = org.joda.time.Period.years(1).withMonths(2).withDays(3).withHours(4).withMinutes(5).withSeconds(6);
    org.joda.time.Period ymd = org.joda.time.Period.years(1).withMonths(2).withDays(3);
    org.joda.time.Period hms = org.joda.time.Period.hours(4).withMinutes(5).withSeconds(6);
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(full)
        .addRow(ymd)
        .addRow(hms)
        .addSingleCol(null)
        .addRow(full)
        .addRow(ymd)
        .addRow(hms)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testNonExtended() {
    String json =
        "{ a: 10, b: { }, c: { d: 30 } }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
        .addMap("b")
          .resumeSchema()
        .addMap("c")
          .addNullable("d", MinorType.BIGINT)
          .resumeSchema()
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, mapValue(), mapValue(30))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testUnknownType() {
    String json =
        "{ a: { \"$bogus\": 10 } }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addMap("a")
          .addNullable("$bogus", MinorType.BIGINT)
          .resumeSchema()
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(mapValue(10))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  private final String LONG_HINT = "<{\"$numberLong\": scalar}>";
  @Test
  public void testInvalidTypeToken() {
    String json =
        "{ a: { \"$numberLong\": 10 } }\n" +
        "{ a: [ ] }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    try {
      loader.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(LONG_HINT));
    }
    loader.close();
  }

  @Test
  public void testInvalidTypeObject() {
    String json =
        "{ a: { \"$numberLong\": 10 } }\n" +
        "{ a: { } }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    try {
      loader.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(LONG_HINT));
    }
    loader.close();
  }

  @Test
  public void testInvalidTypeName() {
    String json =
        "{ a: { \"$numberLong\": 10 } }\n" +
        "{ a: { \"$bogus\": 20 } }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    try {
      loader.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(LONG_HINT));
    }
    loader.close();
  }

  @Test
  public void testInvalidValueToken() {
    String json =
        "{ a: { \"$numberLong\": 10 } }\n" +
        "{ a: { \"$numberLong\": [ ] } }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    try {
      loader.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(LONG_HINT));
    }
    loader.close();
  }

  @Test
  public void testInvalidValue() {
    String json =
        "{ a: { \"$numberLong\": 10 } }\n" +
        "{ a: { \"$numberLong\": 20.3 } }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    try {
      loader.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("Unexpected JSON value: VALUE_NUMBER_FLOAT"));
    }
    loader.close();
  }

  @Test
  public void testExtraField() {
    String json =
        "{ a: { \"$numberLong\": 10 } }\n" +
        "{ a: { \"$numberLong\": 20, bogus: 30 } }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    try {
      loader.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(LONG_HINT));
    }
    loader.close();
  }
}
