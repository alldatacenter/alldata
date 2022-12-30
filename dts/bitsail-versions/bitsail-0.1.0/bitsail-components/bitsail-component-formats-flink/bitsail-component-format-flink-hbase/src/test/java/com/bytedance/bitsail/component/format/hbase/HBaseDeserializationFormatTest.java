/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.component.format.hbase;

import com.bytedance.bitsail.base.format.DeserializationSchema;
import com.bytedance.bitsail.common.column.DoubleColumn;
import com.bytedance.bitsail.common.column.LongColumn;
import com.bytedance.bitsail.common.column.StringColumn;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.BitSailTypeInfoConverter;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfoUtils;

import com.google.common.collect.Lists;
import org.apache.flink.types.Row;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class HBaseDeserializationFormatTest {

  @Test
  public void testBasicTypeConversion() {
    HBaseDeserializationFormat deserializationFormat = new HBaseDeserializationFormat(BitSailConfiguration.newDefault());

    List<ColumnInfo> columnInfos = Lists.newArrayList();
    columnInfos.add(ColumnInfo.builder()
        .name("id")
        .type("long")
        .build());

    columnInfos.add(ColumnInfo.builder()
        .name("name")
        .type("string")
        .build());

    columnInfos.add(ColumnInfo.builder()
        .name("rate")
        .type("double")
        .build());
    TypeInfo<?>[] typeInfos = TypeInfoUtils.getTypeInfos(new BitSailTypeInfoConverter(), columnInfos);

    DeserializationSchema<byte[][], Row> runtimeDeserializationSchema =
        deserializationFormat.createRuntimeDeserializationSchema(typeInfos);

    byte[][] rowCell = new byte[3][];
    rowCell[0] = Bytes.toBytesBinary("100");
    rowCell[1] = Bytes.toBytesBinary("bitsail");
    rowCell[2] = Bytes.toBytesBinary("1.6161");
    Row deserialize = runtimeDeserializationSchema.deserialize(rowCell);

    Assert.assertTrue(deserialize.getField(0) instanceof LongColumn);
    Assert.assertTrue(deserialize.getField(1) instanceof StringColumn);
    Assert.assertTrue(deserialize.getField(2) instanceof DoubleColumn);
  }
}