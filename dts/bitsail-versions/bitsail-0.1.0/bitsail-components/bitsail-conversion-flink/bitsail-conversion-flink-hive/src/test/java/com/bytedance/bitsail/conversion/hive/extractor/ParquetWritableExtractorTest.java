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

package com.bytedance.bitsail.conversion.hive.extractor;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.ColumnCast;
import com.bytedance.bitsail.common.column.DateColumn;
import com.bytedance.bitsail.common.column.MapColumn;
import com.bytedance.bitsail.common.column.StringColumn;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.conversion.hive.ConvertToHiveObjectOptions;

import com.bytedance.bitsail.shaded.hive.client.HiveMetaClientUtil;

import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ParquetWritableExtractorTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void initEnv() {
    BitSailConfiguration bitSailConfiguration = BitSailConfiguration.newDefault();
    bitSailConfiguration.set(CommonOptions.DateFormatOptions.TIME_ZONE,
        ZoneOffset.ofHours(8).getId());
    ColumnCast.initColumnCast(bitSailConfiguration);
  }

  @Test
  public void dateTypeToStringTypeCreateBasicWritableTestParquetExtractor() {
    ParquetWritableExtractor parquetWritableExtractor = new ParquetWritableExtractor();
    ConvertToHiveObjectOptions options = ConvertToHiveObjectOptions.builder()
        .convertErrorColumnAsNull(false)
        .dateTypeToStringAsLong(false)
        .nullStringAsNull(false)
        .useStringText(false)
        .datePrecision(ConvertToHiveObjectOptions.DatePrecision.SECOND)
        .build();
    parquetWritableExtractor.initConvertOptions(options);

    Column column = new DateColumn();
    Writable writable = parquetWritableExtractor.createBasicWritableBatch(SupportHiveDataType.STRING, column);
    assertEquals(writable, new BytesWritable());

    column = new DateColumn(1539492540123L);
    writable = parquetWritableExtractor.createBasicWritableBatch(SupportHiveDataType.STRING, column);
    assertEquals(getStringFromByteWritable((BytesWritable) writable), "2018-10-14 12:49:00");

    column = new DateColumn();
    options.setDateTypeToStringAsLong(true);
    parquetWritableExtractor.initConvertOptions(options);
    writable = parquetWritableExtractor.createBasicWritableBatch(SupportHiveDataType.STRING, column);
    assertEquals(writable, new BytesWritable());

    column = new DateColumn(1539492540123L);
    writable = parquetWritableExtractor.createBasicWritableBatch(SupportHiveDataType.STRING, column);
    assertEquals(getStringFromByteWritable((BytesWritable) writable), "1539492540");

    options.setDatePrecision(ConvertToHiveObjectOptions.DatePrecision.MILLISECOND);
    parquetWritableExtractor.initConvertOptions(options);
    column = new DateColumn();
    writable = parquetWritableExtractor.createBasicWritableBatch(SupportHiveDataType.STRING, column);
    assertEquals(writable, new BytesWritable());

    column = new DateColumn(1539492540123L);
    writable = parquetWritableExtractor.createBasicWritableBatch(SupportHiveDataType.STRING, column);
    assertEquals(getStringFromByteWritable((BytesWritable) writable), "1539492540123");
  }

  @Test
  public void nullStringAsNullMapTestParquetExtractor() {
    String columnNames = "map_field";
    String columnTypes = "map<string,string>";
    ParquetWritableExtractor parquetWritableExtractor = new ParquetWritableExtractor();
    ConvertToHiveObjectOptions options = ConvertToHiveObjectOptions.builder()
        .convertErrorColumnAsNull(false)
        .dateTypeToStringAsLong(false)
        .nullStringAsNull(true)
        .useStringText(false)
        .datePrecision(ConvertToHiveObjectOptions.DatePrecision.SECOND)
        .build();
    parquetWritableExtractor.initConvertOptions(options);
    parquetWritableExtractor.createObjectInspector(columnNames, columnTypes);

    MapColumn<StringColumn, StringColumn> column = new MapColumn<>(StringColumn.class, StringColumn.class);
    StringColumn key = new StringColumn("key");
    StringColumn value = new StringColumn();
    column.put(key, value);
    Writable writable =
        parquetWritableExtractor.createWritableFromObjectInspector(parquetWritableExtractor.getInspector().getAllStructFieldRefs().get(0).getFieldObjectInspector(),
            column);
    ArrayWritable entryWritable = (ArrayWritable) HiveMetaClientUtil.getHiveShim().getComplexValueFromArrayWritable((ArrayWritable) writable).get()[0];
    Writable valueWritable = entryWritable.get()[1];
    assertNull(valueWritable);

    options = ConvertToHiveObjectOptions.builder()
        .convertErrorColumnAsNull(false)
        .dateTypeToStringAsLong(false)
        .nullStringAsNull(false)
        .useStringText(false)
        .datePrecision(ConvertToHiveObjectOptions.DatePrecision.SECOND)
        .build();
    parquetWritableExtractor.initConvertOptions(options);

    writable =
        parquetWritableExtractor.createWritableFromObjectInspector(parquetWritableExtractor.getInspector().getAllStructFieldRefs().get(0).getFieldObjectInspector(),
            column);
    entryWritable = (ArrayWritable) HiveMetaClientUtil.getHiveShim().getComplexValueFromArrayWritable((ArrayWritable) writable).get()[0];
    valueWritable = entryWritable.get()[1];
    assertEquals(valueWritable, new BytesWritable());
  }

  @Test
  public void dateTypeToIntTypeCreateIntOrBigIntWritableTestParquetExtractor() {
    ParquetWritableExtractor parquetWritableExtractor = new ParquetWritableExtractor();
    /* to int case */
    boolean intOrBigint = true; // true:IntWritable; false:LongWritable

    ConvertToHiveObjectOptions options = ConvertToHiveObjectOptions.builder()
        .convertErrorColumnAsNull(false)
        .dateTypeToStringAsLong(false)
        .nullStringAsNull(false)
        .useStringText(false)
        .datePrecision(ConvertToHiveObjectOptions.DatePrecision.SECOND)
        .build();
    parquetWritableExtractor.initConvertOptions(options);
    Column column = new DateColumn();
    Writable writable = parquetWritableExtractor.createIntOrBigIntWritable(column, intOrBigint);
    assertNull(writable);

    column = new DateColumn(1539492540123L);
    writable = parquetWritableExtractor.createIntOrBigIntWritable(column, intOrBigint);
    assertEquals(((IntWritable) writable).get(), 1539492540);

    options.setDatePrecision(ConvertToHiveObjectOptions.DatePrecision.MILLISECOND);
    parquetWritableExtractor.initConvertOptions(options);
    column = new DateColumn();
    writable = parquetWritableExtractor.createIntOrBigIntWritable(column, intOrBigint);
    assertNull(writable);

    column = new DateColumn(1539492540123L);
    thrown.expect(BitSailException.class);
    thrown.expectMessage("[1539492540123], long convert to int overflow.");
    parquetWritableExtractor.createIntOrBigIntWritable(column, intOrBigint);
  }

  @Test
  public void dateTypeToBigIntTypeCreateIntOrBigIntWritableTestParquetExtractor() {
    ParquetWritableExtractor parquetWritableExtractor = new ParquetWritableExtractor();
    /* to BigInt case */
    boolean intOrBigint = false; // true:IntWritable; false:LongWritable

    ConvertToHiveObjectOptions options = ConvertToHiveObjectOptions.builder()
        .convertErrorColumnAsNull(false)
        .dateTypeToStringAsLong(false)
        .nullStringAsNull(false)
        .useStringText(false)
        .datePrecision(ConvertToHiveObjectOptions.DatePrecision.SECOND)
        .build();
    parquetWritableExtractor.initConvertOptions(options);
    Column column = new DateColumn();
    Writable writable = parquetWritableExtractor.createIntOrBigIntWritable(column, intOrBigint);
    assertNull(writable);

    column = new DateColumn(1539492540123L);
    writable = parquetWritableExtractor.createIntOrBigIntWritable(column, intOrBigint);
    assertEquals(((LongWritable) writable).get(), 1539492540L);

    options.setDatePrecision(ConvertToHiveObjectOptions.DatePrecision.MILLISECOND);
    parquetWritableExtractor.initConvertOptions(options);
    column = new DateColumn();
    writable = parquetWritableExtractor.createIntOrBigIntWritable(column, intOrBigint);
    assertNull(writable);

    column = new DateColumn(1539492540123L);
    writable = parquetWritableExtractor.createIntOrBigIntWritable(column, intOrBigint);
    assertEquals(((LongWritable) writable).get(), 1539492540123L);
  }

  private String getStringFromByteWritable(BytesWritable writable) {
    byte[] value = writable.getBytes();
    return new String(value);
  }
}
