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

package com.bytedance.bitsail.component.format.hive;

import com.bytedance.bitsail.common.column.BooleanColumn;
import com.bytedance.bitsail.common.column.BytesColumn;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.ColumnCast;
import com.bytedance.bitsail.common.column.DateColumn;
import com.bytedance.bitsail.common.column.DoubleColumn;
import com.bytedance.bitsail.common.column.ListColumn;
import com.bytedance.bitsail.common.column.LongColumn;
import com.bytedance.bitsail.common.column.MapColumn;
import com.bytedance.bitsail.common.column.StringColumn;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.flink.core.typeinfo.ListColumnTypeInfo;
import com.bytedance.bitsail.flink.core.typeinfo.MapColumnTypeInfo;
import com.bytedance.bitsail.flink.core.typeinfo.PrimitiveColumnTypeInfo;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.StandardListObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.StandardMapObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.JavaConstantBinaryObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.JavaConstantBooleanObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.JavaConstantDoubleObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.JavaConstantLongObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.JavaConstantStringObjectInspector;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Date;
import java.util.Collections;
import java.util.Map;

public class HiveGeneralRowBuilderTest {

  @Test
  public void testBuildBasicType() {
    TypeInformation<?>[] typeInformationList = {
        PrimitiveColumnTypeInfo.STRING_COLUMN_TYPE_INFO,
        PrimitiveColumnTypeInfo.BOOL_COLUMN_TYPE_INFO,
        PrimitiveColumnTypeInfo.BYTES_COLUMN_TYPE_INFO,
        PrimitiveColumnTypeInfo.LONG_COLUMN_TYPE_INFO,
        PrimitiveColumnTypeInfo.DATE_COLUMN_TYPE_INFO,
        PrimitiveColumnTypeInfo.DOUBLE_COLUMN_TYPE_INFO
    };
    Object[] dataList = {
        "string", "true", new byte[] {1, 2, 3}, 1234L, Date.valueOf("2022-01-01"), 3.14D
    };
    ObjectInspector[] inspectors = {
        new JavaConstantStringObjectInspector("string"),
        new JavaConstantBooleanObjectInspector(true),
        new JavaConstantBinaryObjectInspector(new byte[] {1, 2, 3}),
        new JavaConstantLongObjectInspector(1234L),
        new JavaConstantStringObjectInspector("2022-01-01"),
        new JavaConstantDoubleObjectInspector(3.14D)
    };
    Column[] expectedColumnList = {
        new StringColumn("string"),
        new BooleanColumn(true),
        new BytesColumn(new byte[] {1, 2, 3}),
        new LongColumn(1234L),
        new DateColumn(Date.valueOf("2022-01-01").getTime()),
        new DoubleColumn(3.14D)
    };

    ColumnCast.initColumnCast(BitSailConfiguration.newDefault());
    HiveGeneralRowBuilder rowBuilder = new HiveGeneralRowBuilder();
    for (int i = 0; i < 6; ++i) {
      Column actualColumn = rowBuilder.createColumn(typeInformationList[i], inspectors[i], dataList[i]);
      Assert.assertEquals(expectedColumnList[i].asString(), actualColumn.asString());
    }
  }

  @Test
  public void testBuildListType() {
    TypeInformation<?> typeInformation = new ListColumnTypeInfo<>(StringColumn.class);
    ObjectInspector inspector = new TestListObjectInspector(new JavaConstantStringObjectInspector("test"));
    Object[] dataList = new Object[] {"test"};

    HiveGeneralRowBuilder rowBuilder = new HiveGeneralRowBuilder();
    Column column = rowBuilder.createColumn(typeInformation, inspector, dataList);
    Assert.assertTrue(column instanceof ListColumn);
    Assert.assertTrue(((ListColumn<?>) column).getColumnRawData().get(0) instanceof StringColumn);
    Assert.assertEquals("test", (((ListColumn<?>) column).getColumnRawData().get(0)).asString());
  }

  @Test
  public void testBuildMapType() {
    TypeInformation<?> typeInformation = new MapColumnTypeInfo<>(StringColumn.class, StringColumn.class);
    ObjectInspector inspector = new TestMapObjectInspector(
        new JavaConstantStringObjectInspector("test_key"),
        new JavaConstantStringObjectInspector("test_value")
    );
    Object dataMap = Collections.singletonMap("test_key", "test_value");

    HiveGeneralRowBuilder rowBuilder = new HiveGeneralRowBuilder();
    Column column = rowBuilder.createColumn(typeInformation, inspector, dataMap);
    Assert.assertTrue(column instanceof MapColumn);

    Map<?, ?> map = ((MapColumn) column).getColumnRawData();
    Assert.assertEquals(1, map.size());

    for (Map.Entry<?, ?> entry : map.entrySet()) {
      Assert.assertEquals("test_key", ((StringColumn) entry.getKey()).getRawData());
      Assert.assertEquals("test_value", ((StringColumn) entry.getValue()).getRawData());
    }
  }

  static class TestListObjectInspector extends StandardListObjectInspector {
    public TestListObjectInspector(ObjectInspector inspector) {
      super(inspector);
    }
  }

  static class TestMapObjectInspector extends StandardMapObjectInspector {
    public TestMapObjectInspector(ObjectInspector keyInspector, ObjectInspector valueInspector) {
      super(keyInspector, valueInspector);
    }
  }
}
