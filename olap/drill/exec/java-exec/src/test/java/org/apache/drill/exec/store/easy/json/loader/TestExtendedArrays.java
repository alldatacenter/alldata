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
import static org.apache.drill.test.rowSet.RowSetUtilities.longArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.intArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.decArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.doubleArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.binArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.objArray;

import org.apache.drill.categories.JsonTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(JsonTest.class)
public class TestExtendedArrays extends BaseJsonLoaderTest {

  @Test
  public void testInt() {
    String json =
        "{ a: [ { \"$numberInt\": 10 }, 20, { \"$numberInt\": \"30\" } ] }\n" +
        "{ a: null }\n" +
        "{ a: [] }\n" +
        "{ a: [ { \"$numberInt\": 40 }, \"50\", null ] }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.INT)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(intArray(10, 20, 30))
        .addSingleCol(intArray())
        .addSingleCol(intArray())
        .addSingleCol(intArray(40, 50, 0))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testLong() {
    String json =
        "{ a: [ { \"$numberLong\": 10 }, 20, { \"$numberLong\": \"30\" } ] }\n" +
        "{ a: null }\n" +
        "{ a: [] }\n" +
        "{ a: [ { \"$numberLong\": 40 }, \"50\", null ] }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.BIGINT)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(longArray(10L, 20L, 30L))
        .addSingleCol(longArray())
        .addSingleCol(longArray())
        .addSingleCol(longArray(40L, 50L, 0L))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testDecimal() {
    String json =
        "{ a: [ { \"$numberDecimal\": 10 }, null, { \"$numberDecimal\": \"30\" }, " +
        "       { \"$numberDecimal\": 40.2345 } ] }\n" +
        "{ a: null }\n" +
        "{ a: [] }\n" +
        "{ a: [ 60, \"70.890\", 80.765 ] }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.VARDECIMAL, 38, 10)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(decArray(dec("10"), dec("0"), dec("30"), dec("40.2345")))
        .addSingleCol(decArray())
        .addSingleCol(decArray())
        .addSingleCol(decArray(dec("60"), dec("70.89"), dec("80.765")))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testDouble() {
    String json =
        "{ a: [ { \"$numberDouble\": 10 }, null, { \"$numberDouble\": \"30\" } ] }\n" +
        "{ a: null }\n" +
        "{ a: [] }\n" +
        "{ a: [ { \"$numberDouble\": 40.125 }, 60, \"70.125\", 80.375 ] }\n" +
        "{ a: [ { \"$numberDouble\": \"-Infinity\" }, " +
        "       { \"$numberDouble\": \"Infinity\" }," +
        "       { \"$numberDouble\": \"NaN\" } ] }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.FLOAT8)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(doubleArray(10D, 0D, 30D))
        .addSingleCol(doubleArray())
        .addSingleCol(doubleArray())
        .addSingleCol(doubleArray(40.125D, 60D, 70.125D, 80.375D))
        .addSingleCol(doubleArray(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN))
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
        "{ a: [ { \"$date\": \"" + utc + "\" },\n" +
        // V1 "shell mode"
        "       { \"$date\": " + ts + " } ] }\n" +
        "{ a: null }\n" +
        "{ a: [] }\n" +
        // V2 canonical
        "{ a: [ { \"$date\": { \"$numberLong\": " + ts + " } },\n" +
        // Harmless extensions, only valid after the above
        "      " + ts + ",\n" +
        "      \"" + utc + "\" ] }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.TIMESTAMP)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(longArray(localTs, localTs))
        .addSingleCol(longArray())
        .addSingleCol(longArray())
        .addSingleCol(longArray(localTs, localTs, localTs))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testDateNull() {
    LocalDateTime local = LocalDateTime.of(2020, 4, 21, 11, 22, 33);
    Instant instant = local.atZone(ZoneId.systemDefault()).toInstant();
    String utc = DateTimeFormatter.ISO_INSTANT.format(instant);
    String json =
        "{ a: [ { \"$date\": \"" + utc + "\" }, null ] }\n";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    try {
      loader.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("does not allow null values"));
    }
    loader.close();
  }

  @Test
  public void testBinary() {
    String json =
        // V2 format
        "{ a: [ { \"$binary\": { base64: \"ZHJpbGw=\", subType: \"0\" } },\n" +
        "       { \"$binary\": { subType: \"0\", base64: \"ZHJpbGw=\" } },\n" +
        // Harmless extension
        "       { \"$binary\": { base64: \"ZHJpbGw=\" } }, null ] }\n" +
        "{ a: null }\n" +
        "{ a: [] }\n" +
        // V1 format
        "{ a: [ { \"$binary\": \"ZHJpbGw=\", \"$type\": 1 },\n" +
        // Harmless extension
        "       { \"$binary\": \"ZHJpbGw=\" },\n" +
        // Only valid after the above
        "       \"ZHJpbGw=\" ] }\n";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.VARBINARY)
        .build();
    byte[] bytes = "Drill".getBytes();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(binArray(bytes, bytes, bytes, new byte[] { }))
        .addSingleCol(binArray())
        .addSingleCol(binArray())
        .addSingleCol(binArray(bytes, bytes, bytes))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testObjectID() {
    String json =
        "{ a: [ { \"$oid\": \"foo\" },\n" +
        // Harmless extension. A Real OID can't be a "blank"
        // value, but here we just store it as a string.
        "       null ] }\n" +
        "{ a: null }\n" +
        "{ a: [] }\n" +
        // Only valid after the above
        "{ a: [ \"foo\" ] }\n";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.VARCHAR)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(strArray("foo", ""))
        .addSingleCol(strArray())
        .addSingleCol(strArray())
        .addSingleCol(strArray("foo"))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  // A Mongo document is just a regular JSON map.
  @Test
  public void testDocument() {
    String json =
        "{ m: [ { a: { \"$numberLong\": 10 }, b: \"foo\" },\n" +
        "       { a: { \"$numberLong\": \"20\" }, b: null },\n" +
        "       { a: 30 } ] }\n" +
        // Harmless extension
        "{ m: null }\n" +
        "{ m: [] }\n" +
        "{ m: [ null, { a: { \"$numberLong\": 40 }, b: \"bar\" } ] }\n";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.enableExtendedTypes = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addMapArray("m")
          .addNullable("a", MinorType.BIGINT)
          .addNullable("b", MinorType.VARCHAR)
          .resumeSchema()
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(objArray(mapValue(10L, "foo"),
            mapValue(20L, null), mapValue(30L, null)))
        .addSingleCol(objArray())
        .addSingleCol(objArray())
        .addSingleCol(objArray(mapValue(null, null), mapValue(40L, "bar")))
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
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(time)
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
