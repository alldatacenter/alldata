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

package com.netease.arctic.hive.utils;

import org.apache.iceberg.Schema;
import org.apache.iceberg.types.Types;
import org.junit.Assert;
import org.junit.Test;

public class TestHiveSchemaUtil {

  @Test
  public void testChangeFieldNameToLowercase() {

    Schema schema = new Schema(
        Types.NestedField.optional(1, "Col1", Types.IntegerType.get()),
        Types.NestedField.optional(2, "COL2", Types.LongType.get()),
        Types.NestedField.optional(3, "COL3", Types.ListType.ofOptional(4, Types.StringType.get())),
        Types.NestedField.optional(5, "COL4", Types.MapType.ofOptional(6, 7, Types.StringType.get(),
            Types.StringType.get())),
        Types.NestedField.optional(8, "COL5", Types.StructType.of(
            Types.NestedField.optional(9, "COL6", Types.StringType.get()),
            Types.NestedField.optional(10, "COL7", Types.TimestampType.withoutZone())))
    );

    Schema changedSchema = new Schema(
        Types.NestedField.optional(1, "col1", Types.IntegerType.get()),
        Types.NestedField.optional(2, "col2", Types.LongType.get()),
        Types.NestedField.optional(3, "col3", Types.ListType.ofOptional(4, Types.StringType.get())),
        Types.NestedField.optional(5, "col4", Types.MapType.ofOptional(6, 7, Types.StringType.get(),
            Types.StringType.get())),
        Types.NestedField.optional(8, "col5", Types.StructType.of(
            Types.NestedField.optional(9, "col6", Types.StringType.get()),
            Types.NestedField.optional(10, "col7", Types.TimestampType.withoutZone())))
    );

    Assert.assertEquals(changedSchema.asStruct(), HiveSchemaUtil.changeFieldNameToLowercase(schema).asStruct());
  }
}
