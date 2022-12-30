/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.batch.file.parser;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.BooleanColumn;
import com.bytedance.bitsail.common.column.BytesColumn;
import com.bytedance.bitsail.common.column.ColumnCast;
import com.bytedance.bitsail.common.column.DateColumn;
import com.bytedance.bitsail.common.column.DoubleColumn;
import com.bytedance.bitsail.common.column.ListColumn;
import com.bytedance.bitsail.common.column.LongColumn;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.flink.core.parser.BytesParser;
import com.bytedance.bitsail.flink.core.typeinfo.ListColumnTypeInfo;
import com.bytedance.bitsail.flink.core.typeinfo.PrimitiveColumnTypeInfo;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class BytesParserTest {

  private String timeZone;

  @Before
  public void init() {
    timeZone = ZoneOffset.ofHours(0).getId();
    BitSailConfiguration bitSailConfiguration = BitSailConfiguration.newDefault();
    bitSailConfiguration.set(CommonOptions.DateFormatOptions.TIME_ZONE, timeZone);
    ColumnCast.refresh();
    ColumnCast.initColumnCast(bitSailConfiguration);
  }

  @Test
  public void testCreateLongColumn() {
    final TestBytesParser parser = new TestBytesParser();
    final LongColumn column = (LongColumn) parser.createBasicColumn(PrimitiveColumnTypeInfo.LONG_COLUMN_TYPE_INFO, "-1.0");
    Assert.assertEquals(-1L, (long) column.asLong());
  }

  @Test
  public void testCreateArrayColumn() {
    final TestBytesParser parser = new TestBytesParser();
    boolean[] arr = new boolean[] {true, false};
    final ListColumn column = (ListColumn) parser.createColumn(new ListColumnTypeInfo(PrimitiveColumnTypeInfo.BOOL_COLUMN_TYPE_INFO), arr);
    Assert.assertEquals(true, (boolean) column.get(0).asBoolean());
    Assert.assertEquals(false, (boolean) column.get(1).asBoolean());
  }

  @Test
  public void testCreateDoubleColumn() {
    final TestBytesParser parser = new TestBytesParser();
    final DoubleColumn column = (DoubleColumn) parser.createBasicColumn(PrimitiveColumnTypeInfo.DOUBLE_COLUMN_TYPE_INFO, "-1.0");
    Assert.assertEquals(-1.0, column.asDouble(), 0.000001);
  }

  @Test
  public void testGetBooleanColumnValue() {
    final TestBytesParser parser = new TestBytesParser();
    TypeInformation typeInformation = PrimitiveColumnTypeInfo.BOOL_COLUMN_TYPE_INFO;

    Boolean booleanValue = true;
    BooleanColumn booleanColumn = (BooleanColumn) (parser.createBasicColumn(typeInformation, booleanValue));
    Assert.assertEquals(true, booleanColumn.asBoolean());

    booleanValue = null;
    booleanColumn = (BooleanColumn) (parser.createBasicColumn(typeInformation, booleanValue));
    Assert.assertEquals(false, booleanColumn.asBoolean());

    String strBoolean = "true";
    booleanColumn = (BooleanColumn) (parser.createBasicColumn(typeInformation, strBoolean));
    Assert.assertEquals(true, booleanColumn.asBoolean());
  }

  @Test(expected = BitSailException.class)
  public void testGetStrBooleanColumnValue() {
    final TestBytesParser parser = new TestBytesParser();
    TypeInformation typeInformation = PrimitiveColumnTypeInfo.BOOL_COLUMN_TYPE_INFO;

    String strBoolean = "aaa";
    BooleanColumn booleanColumn = (BooleanColumn) (parser.createBasicColumn(typeInformation, strBoolean));
    Assert.assertEquals(null, booleanColumn.asBoolean());
  }

  @Test
  public void testGetDoubleColumnValue() {
    final TestBytesParser parser = new TestBytesParser();

    String doubleValue = "32435234.3242";
    BigDecimal bigDecimal = new BigDecimal(doubleValue);
    TypeInformation typeInformation = PrimitiveColumnTypeInfo.DOUBLE_COLUMN_TYPE_INFO;
    DoubleColumn doubleColumn = (DoubleColumn) (parser.createBasicColumn(typeInformation, doubleValue));
    DoubleColumn decimalColumn = (DoubleColumn) (parser.createBasicColumn(typeInformation, bigDecimal));

    Assert.assertEquals(Double.parseDouble(doubleValue), doubleColumn.asDouble(), 0.000001);
    Assert.assertEquals(Double.parseDouble(doubleValue), decimalColumn.asDouble(), 0.000001);
  }

  @Test
  public void testGetBytesColumnValue() {
    final TestBytesParser parser = new TestBytesParser();

    String str = "bitsail";
    byte[] bytes = str.getBytes();

    Long value = 120L;

    TypeInformation typeInformation = PrimitiveColumnTypeInfo.BYTES_COLUMN_TYPE_INFO;
    BytesColumn longColumn = (BytesColumn) (parser.createBasicColumn(typeInformation, value));
    BytesColumn bytesColumn = (BytesColumn) (parser.createBasicColumn(typeInformation, bytes));

    Assert.assertArrayEquals(value.toString().getBytes(), longColumn.asBytes());
    Assert.assertArrayEquals(bytes, bytesColumn.asBytes());
  }

  @Test
  public void testGetDateColumnValue() {
    final TestBytesParser parser = new TestBytesParser();

    String str = "2018-10-28 02:02:10";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    ZonedDateTime zonedDateTime = ZonedDateTime.parse(str, formatter.withZone(ZoneOffset.of(timeZone)));
    Date date = Date.from(zonedDateTime.toInstant());

    TypeInformation typeInformation = PrimitiveColumnTypeInfo.DATE_COLUMN_TYPE_INFO;
    DateColumn strColumn = (DateColumn) (parser.createBasicColumn(typeInformation, str));
    DateColumn dateColumn = (DateColumn) (parser.createBasicColumn(typeInformation, date));

    Assert.assertEquals(str, strColumn.asString());
    Assert.assertEquals(str, dateColumn.asString());
  }

  @Test
  public void createBasicColumnError() {
    final TestBytesParser parser = new TestBytesParser();
    TypeInformation typeInformation = PrimitiveColumnTypeInfo.LONG_COLUMN_TYPE_INFO;
    try {
      LongColumn longColumn = (LongColumn) (parser.createBasicColumn(typeInformation, "test"));
    } catch (BitSailException e) {
      Assert.assertTrue(e.getMessage().endsWith("String[test] can't convert to Long ."));
    }
  }

  @Test
  public void createBasicColumnNull() {
    final TestBytesParser parser = new TestBytesParser();
    parser.setConvertErrorColumnAsNull(true);

    TypeInformation typeInformation = PrimitiveColumnTypeInfo.LONG_COLUMN_TYPE_INFO;
    LongColumn longColumn = (LongColumn) (parser.createBasicColumn(typeInformation, "test"));
    Assert.assertNull(longColumn.asLong());
  }

  private class TestBytesParser extends BytesParser {
    @Override
    public Row parse(Row row, byte[] bytes, int offset, int numBytes, String charsetName, RowTypeInfo rowTypeInfo) throws Exception {
      return null;
    }

    @Override
    public Row parse(Row row, Object line, RowTypeInfo rowTypeInfo) throws Exception {
      return null;
    }

    public void setConvertErrorColumnAsNull(boolean convertErrorColumnAsNull) {
      this.convertErrorColumnAsNull = convertErrorColumnAsNull;
    }
  }
}