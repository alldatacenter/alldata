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

package com.netease.arctic.spark.test;

import com.netease.arctic.spark.test.helper.TableFiles;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.hive.HiveSchemaUtil;
import org.apache.iceberg.relocated.com.google.common.collect.Streams;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Asserts {

  public static void assertType(Type expect, Type actual) {
    Assert.assertEquals(
        "type should be same",
        expect.isPrimitiveType(), actual.isPrimitiveType());
    if (expect.isPrimitiveType()) {
      Assert.assertEquals(expect, actual);
    } else {
      List<Types.NestedField> expectFields = expect.asNestedType().fields();
      List<Types.NestedField> actualFields = actual.asNestedType().fields();
      Assert.assertEquals(expectFields.size(), actualFields.size());

      Streams.zip(expectFields.stream(), actualFields.stream(), Pair::of)
          .forEach(x -> {
            Assert.assertEquals(x.getLeft().name(), x.getRight().name());
            Assert.assertEquals("The fields' nullable constraint are different",
                x.getLeft().isOptional(), x.getRight().isOptional());
            Assert.assertEquals(x.getLeft().doc(), x.getRight().doc());
            assertType(x.getLeft().type(), x.getRight().type());
          });
    }
  }

  public static void assertPartition(PartitionSpec expectSpec, PartitionSpec actualSpec) {
    Schema expectSchema = expectSpec.schema();
    Schema actualSchema = actualSpec.schema();
    Assertions.assertEquals(expectSpec.fields().size(), actualSpec.fields().size());

    Streams.zip(expectSpec.fields().stream(), actualSpec.fields().stream(), Pair::of)
        .forEach(x -> {
          Assertions.assertEquals(x.getLeft().transform(), x.getRight().transform());
          Assertions.assertEquals(
              expectSchema.findField(x.getLeft().sourceId()).name(),
              actualSchema.findField(x.getRight().sourceId()).name());
        });
  }

  public static void assertPrimaryKey(PrimaryKeySpec expect, PrimaryKeySpec actual) {
    Assertions.assertEquals(expect.fields().size(), actual.fields().size());

    Streams.zip(expect.fields().stream(), actual.fields().stream(), Pair::of)
        .forEach(x -> {
          Assertions.assertEquals(x.getLeft().fieldName(), x.getRight().fieldName());
        });
  }

  public static <K, V> void assertHashMapContainExpect(Map<K, V> expect, Map<K, V> actual) {
    for (K key : expect.keySet()) {
      V expectValue = expect.get(key);
      V actualValue = actual.get(key);
      Assertions.assertEquals(expectValue, actualValue);
    }
  }


  public static void assertHiveColumns(Schema expectSchema, PartitionSpec spec, List<FieldSchema> hiveColumns) {
    Schema schema = com.netease.arctic.hive.utils.HiveSchemaUtil.hiveTableSchema(expectSchema, spec);
    Assert.assertEquals(schema.columns().size(), hiveColumns.size());

    Streams.zip(hiveColumns.stream(), schema.columns().stream(), Pair::of)
        .forEach(x -> {
          Assert.assertEquals(x.getLeft().getName(), x.getRight().name());
          String expectTypeInfoString = HiveSchemaUtil.convert(x.getRight().type()).toString();
          Assert.assertEquals(x.getLeft().getType(), expectTypeInfoString);
        });
  }

  public static void assertHivePartition(PartitionSpec expectSpec, List<FieldSchema> hivePartitions) {
    assertEquals(expectSpec.fields().size(), hivePartitions.size());
    Schema expectSpecSchema = expectSpec.schema();

    Streams.zip(expectSpec.fields().stream(), hivePartitions.stream(), Pair::of)
        .forEach(x -> {
          assertTrue(x.getLeft().transform().isIdentity());
          String expectFieldName = expectSpecSchema.findColumnName(x.getLeft().sourceId());
          assertEquals(expectFieldName, x.getRight().getName());
        });
  }

  public static void assertAllFilesInBaseStore(TableFiles files) {
    assertEquals(0, files.changeInsertFiles.size());
    assertEquals(0, files.changeEqDeleteFiles.size());
    assertTrue(files.baseDataFiles.size() > 0);
  }

  public static void assertAllFilesInHiveLocation(TableFiles files, String hiveLocation) {
    files.baseDataFiles.forEach(f -> {
      String path = f.path().toString();
      Assertions.assertTrue(path.contains(hiveLocation), f.path().toString() + " not in hive location.");
    });
    files.baseDeleteFiles.forEach(f -> {
      Assertions.assertFalse(f.path().toString().contains(hiveLocation));
    });
    files.changeInsertFiles.forEach(f -> {
      Assertions.assertFalse(f.path().toString().contains(hiveLocation));
    });
    files.changeEqDeleteFiles.forEach(f -> {
      Assertions.assertFalse(f.path().toString().contains(hiveLocation));
    });

  }

}
