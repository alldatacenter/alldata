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
import com.bytedance.bitsail.common.column.BytesColumn;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.ListColumn;
import com.bytedance.bitsail.common.column.LongColumn;
import com.bytedance.bitsail.common.column.MapColumn;
import com.bytedance.bitsail.common.column.StringColumn;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.AdapterOptions;
import com.bytedance.bitsail.flink.core.parser.RowBytesParser;
import com.bytedance.bitsail.flink.core.typeinfo.ListColumnTypeInfo;
import com.bytedance.bitsail.flink.core.typeinfo.MapColumnTypeInfo;
import com.bytedance.bitsail.flink.core.typeinfo.PrimitiveColumnTypeInfo;

import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.PrimitiveArrayTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RowBytesParserTest {
  @Test
  public void testParseFlinkRow() {
    List<String> bitSailNameList = Arrays.asList("name", "conf");
    List<TypeInformation> bitSailTypeInfoList = new ArrayList<>();
    bitSailTypeInfoList.add(PrimitiveColumnTypeInfo.STRING_COLUMN_TYPE_INFO);
    bitSailTypeInfoList.add(
        new MapColumnTypeInfo(PrimitiveColumnTypeInfo.STRING_COLUMN_TYPE_INFO, new ListColumnTypeInfo(PrimitiveColumnTypeInfo.LONG_COLUMN_TYPE_INFO)));

    RowTypeInfo bitSailRowTypeInfo = new RowTypeInfo(bitSailTypeInfoList.toArray(new TypeInformation[bitSailTypeInfoList.size()]),
        bitSailNameList.toArray(new String[bitSailNameList.size()]));

    RowBytesParser rowBytesParser = new RowBytesParser();
    Row flinkRow = new Row(2);
    flinkRow.setField(0, "byte");

    Map mapData = new HashMap();
    List listData = Arrays.asList(0L, 1L);
    mapData.put("dance", listData);
    flinkRow.setField(1, mapData);

    Row bitSailRow = new Row(2);
    rowBytesParser.parseFlinkRow(bitSailRow, flinkRow, bitSailRowTypeInfo);

    Column bitSailItem1 = (Column) bitSailRow.getField(0);
    Column bitSailItem2 = (Column) bitSailRow.getField(1);

    assertTrue(bitSailItem1 instanceof StringColumn);
    assertTrue(bitSailItem2 instanceof MapColumn);

    assertEquals("byte", bitSailItem1.asString());
    assertEquals("{\"dance\":[0,1]}", bitSailItem2.asString());
  }

  @Test
  public void testParseBitSailRow() {
    List<String> flinkNameList = Arrays.asList("name", "conf");
    List<TypeInformation> flinkTypeInfoList = new ArrayList<>();
    flinkTypeInfoList.add(BasicTypeInfo.STRING_TYPE_INFO);
    flinkTypeInfoList.add(
        new org.apache.flink.api.java.typeutils.MapTypeInfo(
            BasicTypeInfo.STRING_TYPE_INFO,
            new org.apache.flink.api.java.typeutils.ListTypeInfo(BasicTypeInfo.LONG_TYPE_INFO)
        )
    );

    RowTypeInfo flinkRowTypeInfo = new RowTypeInfo(flinkTypeInfoList.toArray(new TypeInformation[flinkTypeInfoList.size()]),
        flinkNameList.toArray(new String[flinkNameList.size()]));

    RowBytesParser rowBytesParser = new RowBytesParser();
    Row bitSailRow = new Row(2);
    bitSailRow.setField(0, new StringColumn("byte"));

    Map mapData = new MapColumn(String.class, ListColumn.class);
    ListColumn listData = new ListColumn<>(Arrays.asList(new LongColumn(0), new LongColumn(1)), LongColumn.class);
    mapData.put(new StringColumn("dance"), listData);
    bitSailRow.setField(1, mapData);

    Row flinkRow = new Row(2);
    rowBytesParser.parseBitSailRow(flinkRow, bitSailRow, flinkRowTypeInfo, BitSailConfiguration.newDefault());

    Object flinkItem1 = flinkRow.getField(0);
    Object flinkItem2 = flinkRow.getField(1);

    assertTrue(flinkItem1 instanceof String);
    assertTrue(flinkItem2 instanceof Map);

    assertEquals("byte", flinkItem1);
    assertEquals("{dance=[0, 1]}", flinkItem2.toString());
  }

  @Test
  public void testParseBitSailNullRow() {
    List<String> flinkNameList = Arrays.asList("name", "conf", "byte");
    List<TypeInformation> flinkTypeInfoList = new ArrayList<>();
    flinkTypeInfoList.add(BasicTypeInfo.BIG_DEC_TYPE_INFO);
    flinkTypeInfoList.add(BasicTypeInfo.SHORT_TYPE_INFO);
    flinkTypeInfoList.add(PrimitiveArrayTypeInfo.BYTE_PRIMITIVE_ARRAY_TYPE_INFO);

    RowTypeInfo flinkRowTypeInfo = new RowTypeInfo(flinkTypeInfoList.toArray(new TypeInformation[flinkTypeInfoList.size()]),
        flinkNameList.toArray(new String[flinkNameList.size()]));

    RowBytesParser rowBytesParser = new RowBytesParser();
    Row bitSailRow = new Row(3);
    bitSailRow.setField(0, new LongColumn());
    bitSailRow.setField(1, new LongColumn());
    bitSailRow.setField(2, new BytesColumn());

    Row flinkRow = new Row(3);
    rowBytesParser.parseBitSailRow(flinkRow, bitSailRow, flinkRowTypeInfo, BitSailConfiguration.newDefault());

    Object flinkItem1 = flinkRow.getField(0);
    Object flinkItem2 = flinkRow.getField(1);
    Object flinkItem3 = flinkRow.getField(2);

    assertEquals(null, flinkItem1);
    assertEquals(null, flinkItem2);
    assertEquals(null, flinkItem3);
  }

  @Test
  public void testGetShortValueForTinyintType() {
    List<String> flinkNameList = Arrays.asList("name", "conf");
    List<TypeInformation> flinkTypeInfoList = new ArrayList<>();
    flinkTypeInfoList.add(BasicTypeInfo.SHORT_TYPE_INFO);
    flinkTypeInfoList.add(BasicTypeInfo.SHORT_TYPE_INFO);
    RowTypeInfo flinkRowTypeInfo = new RowTypeInfo(flinkTypeInfoList.toArray(new TypeInformation[flinkTypeInfoList.size()]),
        flinkNameList.toArray(new String[flinkNameList.size()]));

    Row bitSailRow = new Row(2);
    bitSailRow.setField(0, new StringColumn("123"));
    bitSailRow.setField(1, new StringColumn("true"));

    RowBytesParser rowBytesParser = new RowBytesParser();
    Row flinkRow = new Row(2);

    BitSailConfiguration adapterConf = BitSailConfiguration.newDefault();
    adapterConf.set(AdapterOptions.CONVERT_BOOLEAN_TO_TINYINT, true);
    rowBytesParser.parseBitSailRow(flinkRow, bitSailRow, flinkRowTypeInfo, adapterConf);

    Object flinkItem1 = flinkRow.getField(0);
    Object flinkItem2 = flinkRow.getField(1);

    assertEquals((short) 123, flinkItem1);
    assertEquals((short) 1, flinkItem2);
  }

  @Test(expected = BitSailException.class)
  public void testGetShortValueForTinyintTypeFalse() {
    List<String> flinkNameList = Arrays.asList("name", "conf");
    List<TypeInformation> flinkTypeInfoList = new ArrayList<>();
    flinkTypeInfoList.add(BasicTypeInfo.SHORT_TYPE_INFO);
    flinkTypeInfoList.add(BasicTypeInfo.SHORT_TYPE_INFO);
    RowTypeInfo flinkRowTypeInfo = new RowTypeInfo(flinkTypeInfoList.toArray(new TypeInformation[flinkTypeInfoList.size()]),
        flinkNameList.toArray(new String[flinkNameList.size()]));

    Row bitSailRow = new Row(2);
    bitSailRow.setField(0, new StringColumn("123"));
    bitSailRow.setField(1, new StringColumn("true"));

    RowBytesParser rowBytesParser = new RowBytesParser();
    Row flinkRow = new Row(2);

    BitSailConfiguration adapterConf = BitSailConfiguration.newDefault();
    rowBytesParser.parseBitSailRow(flinkRow, bitSailRow, flinkRowTypeInfo, adapterConf);

    Object flinkItem1 = flinkRow.getField(0);
    Object flinkItem2 = flinkRow.getField(1);

    assertEquals(123L, flinkItem1);
    assertEquals(1L, flinkItem2);
  }
}