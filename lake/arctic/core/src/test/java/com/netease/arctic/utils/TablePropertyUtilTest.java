/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.utils;

import com.google.common.collect.Maps;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.util.StructLikeMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class TablePropertyUtilTest {

  private static final Schema SCHEMA = new Schema(
      Types.NestedField.required(1, "id", Types.IntegerType.get()),
      Types.NestedField.required(2, "name", Types.StringType.get())
  );
  private static final PartitionSpec SPEC = PartitionSpec.builderFor(SCHEMA)
      .identity("name").build();

  private static final String JSON_VALUE = "{\"name=p0\":{\"key1\":\"p0_value1\",\"key0\":\"p0_value0\"}," +
      "\"name=p1\":{\"key0\":\"p1_value0\"}}";

  private static final StructLikeMap<Map<String, String>> PARTITION_PROPERTIES = buildPartitionProperties();

  private static StructLikeMap<Map<String, String>> buildPartitionProperties() {
    StructLikeMap<Map<String, String>> partitionProperties = StructLikeMap.create(SPEC.partitionType());
    GenericRecord partition0 = GenericRecord.create(SPEC.partitionType());
    partition0.set(0, "p0");
    GenericRecord partition1 = GenericRecord.create(SPEC.partitionType());
    partition1.set(0, "p1");
    Map<String, String> partition0Properties = Maps.newHashMap();
    Map<String, String> partition1Properties = Maps.newHashMap();
    partition0Properties.put("key0", "p0_value0");
    partition0Properties.put("key1", "p0_value1");
    partition1Properties.put("key0", "p1_value0");
    partitionProperties.put(partition0, partition0Properties);
    partitionProperties.put(partition1, partition1Properties);
    return partitionProperties;
  }

  @Test
  public void testEncodePartitionProperties() {
    Assert.assertEquals(JSON_VALUE, TablePropertyUtil.encodePartitionProperties(SPEC, PARTITION_PROPERTIES));
  }

  @Test
  public void testDecodePartitionProperties() {
    Assert.assertEquals(PARTITION_PROPERTIES, TablePropertyUtil.decodePartitionProperties(SPEC, JSON_VALUE));
  }
}
