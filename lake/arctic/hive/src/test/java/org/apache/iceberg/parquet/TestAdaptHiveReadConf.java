/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.iceberg.parquet;

import org.apache.iceberg.Schema;
import org.apache.iceberg.mapping.MappingUtil;
import org.apache.iceberg.mapping.NameMapping;
import org.apache.iceberg.types.TypeUtil;
import org.apache.iceberg.types.Types;
import org.apache.parquet.schema.MessageType;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.iceberg.types.Types.NestedField.optional;
import static org.apache.iceberg.types.Types.NestedField.required;

public class TestAdaptHiveReadConf {

  private static final Types.StructType SUPPORTED_PRIMITIVES =
      Types.StructType.of(
          required(100, "id", Types.LongType.get()),
          optional(101, "data", Types.StringType.get()),
          required(102, "b", Types.BooleanType.get()),
          optional(103, "i", Types.IntegerType.get()),
          required(104, "l", Types.LongType.get()),
          optional(105, "f", Types.FloatType.get()),
          required(106, "d", Types.DoubleType.get()),
          optional(107, "date", Types.DateType.get()),
          required(108, "ts", Types.TimestampType.withZone()),
          required(110, "s", Types.StringType.get()),
          required(112, "fixed", Types.FixedType.ofLength(7)),
          optional(113, "bytes", Types.BinaryType.get()),
          required(114, "dec_9_0", Types.DecimalType.of(9, 0)),
          required(115, "dec_11_2", Types.DecimalType.of(11, 2)),
          required(116, "dec_38_10", Types.DecimalType.of(38, 10)) // spark's maximum precision
      );

  private final String col0 = "id";
  private final String col1 = "list_of_maps";
  private final String col5 = "map_of_lists";
  private final String col9 = "list_of_lists";
  private final String col12 = "map_of_maps";
  private final String col17 = "list_of_struct_of_nested_types";
  private final String col20 = "m1";
  private final String col23 = "l1";
  private final String col25 = "l2";
  private final String col27 = "m2";

  @Test
  public void testAssignIdsByNameMapping() {
    //hive struct field names are all uppercase
    Types.StructType hiveStructType = Types.StructType.of(
        required(0, col0.toUpperCase(), Types.LongType.get()),
        optional(1, col1.toUpperCase(),
            Types.ListType.ofOptional(2, Types.MapType.ofOptional(3, 4, Types.StringType.get(), SUPPORTED_PRIMITIVES))),
        optional(
            5,
            col5.toUpperCase(),
            Types.MapType.ofOptional(6, 7, Types.StringType.get(), Types.ListType.ofOptional(8, SUPPORTED_PRIMITIVES))),
        required(9, col9.toUpperCase(),
            Types.ListType.ofOptional(10, Types.ListType.ofOptional(11, SUPPORTED_PRIMITIVES))),
        required(12, col12.toUpperCase(),
            Types.MapType.ofOptional(
                13,
                14,
                Types.StringType.get(),
                Types.MapType.ofOptional(15, 16, Types.StringType.get(), SUPPORTED_PRIMITIVES))),
        required(17, col17.toUpperCase(), Types.ListType.ofOptional(19, Types.StructType.of(
            Types.NestedField.required(
                20,
                col20.toUpperCase(),
                Types.MapType.ofOptional(21, 22, Types.StringType.get(), SUPPORTED_PRIMITIVES)),
            Types.NestedField.optional(23, col23.toUpperCase(), Types.ListType.ofRequired(24, SUPPORTED_PRIMITIVES)),
            Types.NestedField.required(25, col25.toUpperCase(), Types.ListType.ofRequired(26, SUPPORTED_PRIMITIVES)),
            Types.NestedField.optional(
                27,
                col27.toUpperCase(),
                Types.MapType.ofOptional(28, 29, Types.StringType.get(), SUPPORTED_PRIMITIVES))
        )))
    );

    Types.StructType structType = Types.StructType.of(
        required(0, col0, Types.LongType.get()),
        optional(1, col1,
            Types.ListType.ofOptional(2, Types.MapType.ofOptional(3, 4, Types.StringType.get(), SUPPORTED_PRIMITIVES))),
        optional(
            5,
            col5,
            Types.MapType.ofOptional(6, 7, Types.StringType.get(), Types.ListType.ofOptional(8, SUPPORTED_PRIMITIVES))),
        required(
            9,
            col9,
            Types.ListType.ofOptional(10, Types.ListType.ofOptional(11, SUPPORTED_PRIMITIVES))),
        required(12, col12,
            Types.MapType.ofOptional(
                13,
                14,
                Types.StringType.get(),
                Types.MapType.ofOptional(15, 16, Types.StringType.get(), SUPPORTED_PRIMITIVES))),
        required(17, col17, Types.ListType.ofOptional(19, Types.StructType.of(
            Types.NestedField.required(
                20,
                col20,
                Types.MapType.ofOptional(21, 22, Types.StringType.get(), SUPPORTED_PRIMITIVES)),
            Types.NestedField.optional(23, col23, Types.ListType.ofRequired(24, SUPPORTED_PRIMITIVES)),
            Types.NestedField.required(25, col25, Types.ListType.ofRequired(26, SUPPORTED_PRIMITIVES)),
            Types.NestedField.optional(27, col27,
                Types.MapType.ofOptional(28, 29, Types.StringType.get(), SUPPORTED_PRIMITIVES))
        )))
    );

    Schema schema = new Schema(TypeUtil.assignFreshIds(structType, new AtomicInteger(0)::incrementAndGet)
        .asStructType().fields());
    Schema hiveSchema = new Schema(TypeUtil.assignFreshIds(hiveStructType, new AtomicInteger(0)::incrementAndGet)
        .asStructType().fields());
    NameMapping nameMapping = MappingUtil.create(schema);
    MessageType messageTypeWithIds = ParquetSchemaUtil.convert(hiveSchema, "parquet_type");
    MessageType messageTypeWithIdsFromNameMapping = (MessageType) ParquetTypeVisitor.visit(
        RemoveIds.removeIds(messageTypeWithIds),
        new AdaptHiveApplyNameMapping(nameMapping));

    Assert.assertEquals(messageTypeWithIds, messageTypeWithIdsFromNameMapping);
  }
}
