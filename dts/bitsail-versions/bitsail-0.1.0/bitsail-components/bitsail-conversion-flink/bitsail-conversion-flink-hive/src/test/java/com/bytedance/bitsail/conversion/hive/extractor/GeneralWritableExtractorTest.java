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
import com.bytedance.bitsail.conversion.hive.BitSailColumnConversion;
import com.bytedance.bitsail.conversion.hive.ConvertToHiveObjectOptions;
import com.bytedance.bitsail.conversion.hive.HiveInspectors;
import com.bytedance.bitsail.conversion.hive.HiveObjectConversion;

import com.bytedance.bitsail.shaded.hive.client.HiveMetaClientUtil;

import org.apache.flink.types.Row;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.BytesWritable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GeneralWritableExtractorTest {
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
  public void nullStringAsNullMapTestGeneralExtractor() {
    String columnNames = "map_field";
    String columnTypes = "map<string,string>";
    GeneralWritableExtractor generalWritableExtractor = new GeneralWritableExtractor();
    ConvertToHiveObjectOptions options = ConvertToHiveObjectOptions.builder()
        .convertErrorColumnAsNull(false)
        .dateTypeToStringAsLong(false)
        .nullStringAsNull(true)
        .useStringText(false)
        .datePrecision(ConvertToHiveObjectOptions.DatePrecision.SECOND)
        .build();
    generalWritableExtractor.initConvertOptions(options);
    generalWritableExtractor.createObjectInspector(columnNames, columnTypes);

    List<TypeInfo> columnTypeList = HiveWritableExtractor.getHiveTypeInfos(columnTypes);
    MapColumn<StringColumn, StringColumn> column = new MapColumn<>(StringColumn.class, StringColumn.class);
    StringColumn key = new StringColumn("key");
    StringColumn value = new StringColumn();
    column.put(key, value);

    HiveObjectConversion objectConversion =
        HiveInspectors.getConversion(HiveInspectors.getObjectInspector(columnTypeList.get(0)), columnTypeList.get(0), HiveMetaClientUtil.getHiveShim());
    Object hiveObject = objectConversion.toHiveObject(column);
    Map<String, String> map = (Map<String, String>) hiveObject;
    Object val = map.get("key");
    assertNull(val);

    options = ConvertToHiveObjectOptions.builder()
        .convertErrorColumnAsNull(false)
        .dateTypeToStringAsLong(false)
        .nullStringAsNull(false)
        .useStringText(false)
        .datePrecision(ConvertToHiveObjectOptions.DatePrecision.SECOND)
        .build();
    generalWritableExtractor.initConvertOptions(options);
    objectConversion = HiveInspectors.getConversion(HiveInspectors.getObjectInspector(columnTypeList.get(0)), columnTypeList.get(0), HiveMetaClientUtil.getHiveShim());

    hiveObject = objectConversion.toHiveObject(column);
    map = (Map<String, String>) hiveObject;
    val = map.get("key");
    assertEquals(val, "");
  }

  @Test
  public void dateTypeToStringTypeCreateBasicWritableTestGeneralExtractor() {
    GeneralWritableExtractor generalWritableExtractor = new GeneralWritableExtractor();
    ConvertToHiveObjectOptions options = ConvertToHiveObjectOptions.builder()
        .convertErrorColumnAsNull(false)
        .dateTypeToStringAsLong(false)
        .nullStringAsNull(true)
        .useStringText(false)
        .datePrecision(ConvertToHiveObjectOptions.DatePrecision.SECOND)
        .build();
    generalWritableExtractor.initConvertOptions(options);

    Column column = new DateColumn();
    Object object = BitSailColumnConversion.toHivePrimitiveObject(column, PrimitiveObjectInspector.PrimitiveCategory.STRING);
    assertNull(object);

    column = new DateColumn(1539492540123L);
    object = BitSailColumnConversion.toHivePrimitiveObject(column, PrimitiveObjectInspector.PrimitiveCategory.STRING);
    assertEquals(object, "2018-10-14 12:49:00");

    column = new DateColumn();
    options.setDateTypeToStringAsLong(true);
    generalWritableExtractor.initConvertOptions(options);
    object = BitSailColumnConversion.toHivePrimitiveObject(column, PrimitiveObjectInspector.PrimitiveCategory.STRING);
    assertNull(object);

    column = new DateColumn(1539492540123L);
    object = BitSailColumnConversion.toHivePrimitiveObject(column, PrimitiveObjectInspector.PrimitiveCategory.STRING);
    assertEquals(object, "1539492540");
    options.setDatePrecision(ConvertToHiveObjectOptions.DatePrecision.MILLISECOND);
    generalWritableExtractor.initConvertOptions(options);
    column = new DateColumn();
    object = BitSailColumnConversion.toHivePrimitiveObject(column, PrimitiveObjectInspector.PrimitiveCategory.STRING);
    assertNull(object);

    column = new DateColumn(1539492540123L);
    object = BitSailColumnConversion.toHivePrimitiveObject(column, PrimitiveObjectInspector.PrimitiveCategory.STRING);
    assertEquals(object, "1539492540123");
  }

  @Test
  public void dateTypeToIntTypeCreateIntOrBigIntWritableTestGeneralExtractor() {
    GeneralWritableExtractor generalWritableExtractor = new GeneralWritableExtractor();
    /* to int case */
    boolean intOrBigint = true; // true:IntWritable; false:LongWritable

    ConvertToHiveObjectOptions options = ConvertToHiveObjectOptions.builder()
        .convertErrorColumnAsNull(false)
        .dateTypeToStringAsLong(false)
        .nullStringAsNull(false)
        .useStringText(false)
        .datePrecision(ConvertToHiveObjectOptions.DatePrecision.SECOND)
        .build();
    generalWritableExtractor.initConvertOptions(options);
    Column column = new DateColumn();
    Object object = BitSailColumnConversion.toIntOrBigintHiveObject(column, intOrBigint);
    assertNull(object);

    column = new DateColumn(1539492540123L);
    object = BitSailColumnConversion.toIntOrBigintHiveObject(column, intOrBigint);
    assertEquals(object, 1539492540);

    options.setDatePrecision(ConvertToHiveObjectOptions.DatePrecision.MILLISECOND);
    generalWritableExtractor.initConvertOptions(options);
    column = new DateColumn();
    object = BitSailColumnConversion.toIntOrBigintHiveObject(column, intOrBigint);
    assertNull(object);

    column = new DateColumn(1539492540123L);
    thrown.expect(BitSailException.class);
    thrown.expectMessage("[1539492540123], long convert to int overflow.");
    BitSailColumnConversion.toIntOrBigintHiveObject(column, intOrBigint);
  }

  @Test
  public void testDateColumn() {
    GeneralWritableExtractor generalWritableExtractor = new GeneralWritableExtractor();
    /* to BigInt case */
    boolean intOrBigint = false; // true:IntWritable; false:LongWritable

    ConvertToHiveObjectOptions options = ConvertToHiveObjectOptions.builder()
        .convertErrorColumnAsNull(false)
        .dateTypeToStringAsLong(false)
        .nullStringAsNull(false)
        .useStringText(false)
        .datePrecision(ConvertToHiveObjectOptions.DatePrecision.SECOND)
        .build();
    generalWritableExtractor.initConvertOptions(options);
    Column column = new DateColumn();
    Object object = BitSailColumnConversion.toIntOrBigintHiveObject(column, intOrBigint);
    assertNull(object);

    column = new DateColumn(1539492540123L);
    object = BitSailColumnConversion.toIntOrBigintHiveObject(column, intOrBigint);
    assertEquals(object, 1539492540L);

    options.setDatePrecision(ConvertToHiveObjectOptions.DatePrecision.MILLISECOND);
    generalWritableExtractor.initConvertOptions(options);
    column = new DateColumn();
    object = BitSailColumnConversion.toIntOrBigintHiveObject(column, intOrBigint);
    assertNull(object);

    column = new DateColumn(1539492540123L);
    object = BitSailColumnConversion.toIntOrBigintHiveObject(column, intOrBigint);
    assertEquals(object, 1539492540123L);
  }

  @Test
  public void testCreateRowObjectGeneral() {
    GeneralWritableExtractor generalWritableExtractor = new GeneralWritableExtractor();
    Object hiveRow = createRowObject(generalWritableExtractor);

    Map<String, String> data = (Map<String, String>) ((List<?>) hiveRow).get(0);
    Assert.assertEquals(1, data.size());
    for (Map.Entry<String, String> entry : data.entrySet()) {
      Assert.assertEquals("test_key", entry.getKey());
      Assert.assertEquals("test_value", entry.getValue());
    }
  }

  @Test
  public void testCreateRowObjectParquet() {
    ParquetWritableExtractor parquetWritableExtractor = new ParquetWritableExtractor();
    Object hiveRow = createRowObject(parquetWritableExtractor);

    ArrayWritable writable = parquetWritableExtractor.getHiveShim().getComplexValueFromArrayWritable((ArrayWritable) hiveRow);
    writable = (ArrayWritable) (writable.get()[0]);
    writable = (ArrayWritable) (writable.get()[0]);

    BytesWritable keyWritable = (BytesWritable) writable.get()[0];
    BytesWritable valueWritable = (BytesWritable) writable.get()[1];
    String keyStr = new String(keyWritable.getBytes());
    String valueStr = new String(valueWritable.getBytes());

    Assert.assertEquals("test_key", keyStr);
    Assert.assertEquals("test_value", valueStr);
  }

  private Object createRowObject(HiveWritableExtractor writableExtractor) {
    String columnNames = "map_field";
    String columnTypes = "map<string,string>";

    ConvertToHiveObjectOptions options = ConvertToHiveObjectOptions.builder()
        .convertErrorColumnAsNull(false)
        .dateTypeToStringAsLong(false)
        .nullStringAsNull(false)
        .useStringText(false)
        .datePrecision(ConvertToHiveObjectOptions.DatePrecision.SECOND)
        .build();
    writableExtractor.initConvertOptions(options);
    writableExtractor.createObjectInspector(columnNames, columnTypes);
    writableExtractor.setFieldNames(new String[] {columnNames});
    writableExtractor.setColumnMapping(Collections.singletonMap(columnNames.toUpperCase(), 0));

    Row record = new Row(1);
    StringColumn keyColumn = new StringColumn("test_key");
    StringColumn valueColumn = new StringColumn("test_value");
    MapColumn mapColumn = new MapColumn(Collections.singletonMap(keyColumn, valueColumn), StringColumn.class, StringColumn.class);

    record.setField(0, mapColumn);
    return writableExtractor.createRowObject(record);
  }
}
