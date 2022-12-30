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

package com.bytedance.bitsail.connector.legacy.jdbc.source;

import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.filemapping.JdbcTypeInfoConverter;
import com.bytedance.bitsail.common.typeinfo.BasicArrayTypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfos;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created 2022/10/18
 */
public class OracleSourceEngineConnectorTest {

  @Test
  public void getTypeInfoMapTest() {
    List<ColumnInfo> columnInfoList = Lists.newArrayList();
    columnInfoList.add(new ColumnInfo("COL1", "varchar"));
    columnInfoList.add(new ColumnInfo("COL2", "number"));
    columnInfoList.add(new ColumnInfo("COL3", "integer"));
    columnInfoList.add(new ColumnInfo("COL4", "int"));
    columnInfoList.add(new ColumnInfo("COL5", "smallint"));
    columnInfoList.add(new ColumnInfo("COL6", "float"));
    columnInfoList.add(new ColumnInfo("COL7", "double"));
    columnInfoList.add(new ColumnInfo("COL8", "decimal"));
    columnInfoList.add(new ColumnInfo("COL9", "bool"));
    columnInfoList.add(new ColumnInfo("COL10", "date"));
    columnInfoList.add(new ColumnInfo("COL11", "timestamp"));
    columnInfoList.add(new ColumnInfo("COL12", "blob"));
    JdbcTypeInfoConverter typeInfoConverter = new JdbcTypeInfoConverter("oracle");

    Map<String, TypeInfo<?>> actualTypeInfoMap = Maps.newLinkedHashMap();
    for (ColumnInfo columnInfo : columnInfoList) {
      actualTypeInfoMap.put(columnInfo.getName(), typeInfoConverter.fromTypeString(columnInfo.getType()));
    }

    LinkedHashMap<String, TypeInfo<?>> expectTypeInfoMap = new LinkedHashMap<>();
    expectTypeInfoMap.put("COL1", TypeInfos.STRING_TYPE_INFO);
    expectTypeInfoMap.put("COL2", TypeInfos.LONG_TYPE_INFO);
    expectTypeInfoMap.put("COL3", TypeInfos.INT_TYPE_INFO);
    expectTypeInfoMap.put("COL4", TypeInfos.INT_TYPE_INFO);
    expectTypeInfoMap.put("COL5", TypeInfos.SHORT_TYPE_INFO);
    expectTypeInfoMap.put("COL6", TypeInfos.DOUBLE_TYPE_INFO);
    expectTypeInfoMap.put("COL7", TypeInfos.DOUBLE_TYPE_INFO);
    expectTypeInfoMap.put("COL8", TypeInfos.BIG_DECIMAL_TYPE_INFO);
    expectTypeInfoMap.put("COL9", TypeInfos.BOOLEAN_TYPE_INFO);
    expectTypeInfoMap.put("COL10", TypeInfos.SQL_DATE_TYPE_INFO);
    expectTypeInfoMap.put("COL11", TypeInfos.SQL_TIMESTAMP_TYPE_INFO);
    expectTypeInfoMap.put("COL12", BasicArrayTypeInfo.BINARY_TYPE_INFO);
    assertEquals(actualTypeInfoMap.toString(), expectTypeInfoMap.toString());
  }
}
