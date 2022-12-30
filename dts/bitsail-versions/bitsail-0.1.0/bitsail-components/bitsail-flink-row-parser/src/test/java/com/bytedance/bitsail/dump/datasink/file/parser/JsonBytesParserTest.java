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

package com.bytedance.bitsail.dump.datasink.file.parser;

import com.bytedance.bitsail.common.column.ArrayMapColumn;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.ColumnCast;
import com.bytedance.bitsail.common.column.MapColumn;
import com.bytedance.bitsail.common.column.StringColumn;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.flink.core.typeinfo.MapColumnTypeInfo;
import com.bytedance.bitsail.flink.core.typeinfo.PrimitiveColumnTypeInfo;
import com.bytedance.bitsail.parser.option.RowParserOptions;

import com.google.common.collect.Lists;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class JsonBytesParserTest {

  private static List<ColumnInfo> getColumnInfo(String columnName) {
    List<ColumnInfo> columnInfos = Lists.newArrayListWithExpectedSize(1);
    columnInfos.add(new ColumnInfo(columnName, null));
    return columnInfos;
  }

  @Test
  public void testArrayMap() {
    BitSailConfiguration jobConf = BitSailConfiguration.newDefault();
    jobConf.set(RowParserOptions.USE_ARRAY_MAP_COLUMN, true);
    JsonBytesParser parser = new JsonBytesParser(jobConf, getColumnInfo("a"));
    Row row = new Row(1);
    RowTypeInfo rowTypeInfo = new RowTypeInfo(new TypeInformation[] {
        new MapColumnTypeInfo(StringColumn.class, StringColumn.class)}, new String[] {"a"});
    Tuple2<Row, Object> rowAndTimestamp = parser.parse(row, "{\"a\": {\"b\": 1577808000}}".getBytes(), rowTypeInfo, Collections.emptyList());
    row = rowAndTimestamp.f0;
    Assert.assertTrue(row.getField(0) instanceof ArrayMapColumn);
  }

  @Test
  public void testMap() {
    BitSailConfiguration jobConf = BitSailConfiguration.newDefault();
    jobConf.set(RowParserOptions.USE_ARRAY_MAP_COLUMN, false);
    JsonBytesParser parser = new JsonBytesParser(jobConf, getColumnInfo("a"));
    Row row = new Row(1);
    RowTypeInfo rowTypeInfo = new RowTypeInfo(new TypeInformation[] {
        new MapColumnTypeInfo<StringColumn, StringColumn>(StringColumn.class, StringColumn.class)}, new String[] {"a"});
    Tuple2<Row, Object> ret = parser.parse(row, "{\"a\": {\"b\": 1577808000}}".getBytes(), rowTypeInfo, Collections.emptyList());
    row = ret.f0;
    Assert.assertTrue(row.getField(0) instanceof MapColumn);
  }

  @Test
  public void testRowColumn() {
    BitSailConfiguration jobConf = BitSailConfiguration.newDefault();
    ColumnCast.initColumnCast(jobConf);
    jobConf.set(RowParserOptions.RAW_COLUMN, "row_column");
    List<ColumnInfo> columnInfos = Lists.newArrayListWithExpectedSize(2);
    columnInfos.add(new ColumnInfo("a", null));
    columnInfos.add(new ColumnInfo("b", null));
    columnInfos.add(new ColumnInfo("row_column", null));

    Row row = new Row(3);
    JsonBytesParser parser = new JsonBytesParser(jobConf, columnInfos);

    RowTypeInfo rowTypeInfo = new RowTypeInfo(
        new TypeInformation[] {PrimitiveColumnTypeInfo.STRING_COLUMN_TYPE_INFO, PrimitiveColumnTypeInfo.STRING_COLUMN_TYPE_INFO,
            PrimitiveColumnTypeInfo.BYTES_COLUMN_TYPE_INFO},
        new String[] {"a", "b", "row_column"});

    String data = "{\n" +
        "    \"a\":\"100\",\n" +
        "    \"b\":\"200\"\n" +
        "}";
    Tuple2<Row, Object> ignore = parser.parse(row, data.getBytes(), rowTypeInfo, Collections.emptyList());
    Assert.assertEquals(((Column) (row.getField(2))).asString(), data);
  }

  @Test
  public void testWriteMapNullSerializeFeature() throws Exception {
    BitSailConfiguration jobConf = BitSailConfiguration.newDefault();
    List<ColumnInfo> columnInfos = getColumnInfo("a");
    jobConf.set(RowParserOptions.JSON_SERIALIZER_FEATURES, "WriteMapNullValue");
    JsonBytesParser parser = new JsonBytesParser(jobConf, columnInfos);
    Row row = new Row(1);
    RowTypeInfo rowTypeInfo = new RowTypeInfo(new TypeInformation[] {PrimitiveColumnTypeInfo.STRING_COLUMN_TYPE_INFO}, new String[] {"a"});

    Row result = parser.parse(row, "{\"a\": { \"b\" : null, \"c\" : 1 }}".getBytes(),
        rowTypeInfo);

    Assert.assertEquals("{\"b\":null,\"c\":1}", ((Column) result.getField(0)).asString());
  }

  @Test
  public void testWithoutWriteMapNullSerializeFeature() throws Exception {
    BitSailConfiguration jobConf = BitSailConfiguration.newDefault();
    List<ColumnInfo> columnInfos = getColumnInfo("a");
    JsonBytesParser parser = new JsonBytesParser(jobConf, columnInfos);
    Row row = new Row(1);
    RowTypeInfo rowTypeInfo = new RowTypeInfo(new TypeInformation[] {PrimitiveColumnTypeInfo.STRING_COLUMN_TYPE_INFO}, new String[] {"a"});

    Row result = parser.parse(row, "{\"a\": { \"b\" : null, \"c\" : 1 }}".getBytes(),
        rowTypeInfo);

    Assert.assertEquals("{\"c\":1}", ((Column) result.getField(0)).asString());
  }

}
