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

package com.netease.arctic;

import com.google.common.collect.Maps;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DataFiles;
import org.apache.iceberg.Metrics;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.types.Types;

import java.util.Map;

public class TableTestHelpers {

  public static final String TEST_CATALOG_NAME = "test_catalog";
  public static final String TEST_DB_NAME = "test_db";
  public static final String TEST_TABLE_NAME = "test_table";

  public static final TableIdentifier TEST_TABLE_ID =
      TableIdentifier.of(TEST_CATALOG_NAME, TEST_DB_NAME, TEST_TABLE_NAME);

  public static final org.apache.iceberg.catalog.TableIdentifier TEST_TABLE_ICEBERG_ID =
      org.apache.iceberg.catalog.TableIdentifier.of(TEST_DB_NAME, TEST_TABLE_NAME);

  public static final Schema TABLE_SCHEMA = new Schema(
      Types.NestedField.required(1, "id", Types.IntegerType.get()),
      Types.NestedField.required(2, "name", Types.StringType.get()),
      Types.NestedField.required(3, "ts", Types.LongType.get()),
      Types.NestedField.required(4, "op_time", Types.TimestampType.withoutZone())
  );

  public static final PartitionSpec SPEC = PartitionSpec.builderFor(TABLE_SCHEMA)
      .day("op_time").build();

  public static final PartitionSpec IDENTIFY_SPEC = PartitionSpec.builderFor(TABLE_SCHEMA)
      .identity("op_time").build();

  public static final Record RECORD = GenericRecord.create(TABLE_SCHEMA);

  public static final PrimaryKeySpec PRIMARY_KEY_SPEC = PrimaryKeySpec.builderFor(TABLE_SCHEMA)
      .addColumn("id").build();

  private static final Map<String, DataFile> DATA_FILE_MAP = Maps.newHashMap();

  public static DataFile getFile(
      String basePath, int number, PartitionSpec spec, String partitionPath,
      Metrics metrics, boolean fromCache) {
    String filePath;
    if (partitionPath != null) {
      filePath = String.format("%s/%s/data-%d.parquet", basePath, partitionPath, number);
    } else {
      filePath = String.format("%s/data-%d.parquet", basePath, number);
    }
    if (fromCache) {
      return DATA_FILE_MAP.computeIfAbsent(filePath, path -> buildDataFile(filePath, spec, partitionPath, metrics));
    } else {
      return buildDataFile(filePath, spec, partitionPath, metrics);
    }
  }

  public static DataFile getFile(int number) {
    return getFile("/data", number, PartitionSpec.unpartitioned(), null, null, true);
  }

  public static DataFile getFile(int number, String partitionPath) {
    return getFile("/data", number, SPEC, partitionPath, null, true);
  }

  private static DataFile buildDataFile(
      String filePath, PartitionSpec spec, String partitionPath,
      Metrics metrics) {
    DataFiles.Builder fileBuilder = DataFiles.builder(spec);
    fileBuilder
        .withPath(filePath)
        .withFileSizeInBytes(10)
        .withRecordCount(2);
    if (partitionPath != null) {
      fileBuilder.withPartitionPath(partitionPath);
    }
    if (metrics != null) {
      fileBuilder.withMetrics(metrics);
    }
    return fileBuilder.build();
  }


}
