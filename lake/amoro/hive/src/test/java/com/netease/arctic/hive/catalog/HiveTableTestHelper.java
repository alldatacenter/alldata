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

package com.netease.arctic.hive.catalog;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.hive.io.HiveDataTestHelpers;
import com.netease.arctic.io.DataTestHelpers;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.types.TypeUtil;
import org.apache.iceberg.types.Types;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class HiveTableTestHelper extends BasicTableTestHelper {

  public static final String COLUMN_NAME_OP_TIME_WITH_ZONE = "op_time_wz";
  public static final String COLUMN_NAME_D = "d$d";
  public static final String COLUMN_NAME_OP_DAY = "op_time_day";

  public static final Schema HIVE_TABLE_SCHEMA = TypeUtil.join(
      BasicTableTestHelper.TABLE_SCHEMA, new Schema(
          Types.NestedField.required(5, COLUMN_NAME_OP_TIME_WITH_ZONE, Types.TimestampType.withZone()),
          Types.NestedField.required(6, COLUMN_NAME_D, Types.DecimalType.of(10, 0)),
          Types.NestedField.required(7, COLUMN_NAME_OP_DAY, Types.StringType.get()))
  );

  public static final PrimaryKeySpec HIVE_PRIMARY_KEY_SPEC =
      PrimaryKeySpec.builderFor(HIVE_TABLE_SCHEMA).addColumn("id").build();

  public static final PartitionSpec HIVE_SPEC = PartitionSpec.builderFor(HIVE_TABLE_SCHEMA)
      .identity(COLUMN_NAME_OP_DAY).build();

  public HiveTableTestHelper(
      Schema tableSchema,
      PrimaryKeySpec primaryKeySpec,
      PartitionSpec partitionSpec,
      Map<String, String> tableProperties) {
    super(tableSchema, primaryKeySpec, partitionSpec, tableProperties);
  }

  public HiveTableTestHelper(
      boolean hasPrimaryKey,
      boolean hasPartition,
      Map<String, String> tableProperties) {
    this(HIVE_TABLE_SCHEMA, hasPrimaryKey ? HIVE_PRIMARY_KEY_SPEC : PrimaryKeySpec.noPrimaryKey(),
        hasPartition ? HIVE_SPEC : PartitionSpec.unpartitioned(), tableProperties);
  }

  public HiveTableTestHelper(boolean hasPrimaryKey, boolean hasPartition) {
    this(hasPrimaryKey, hasPartition, Maps.newHashMap());
  }

  @Override
  public Record generateTestRecord(int id, String name, long ts, String opTime) {
    return DataTestHelpers.createRecord(HIVE_TABLE_SCHEMA, id, name, ts, opTime,
        opTime + "Z", new BigDecimal("0"), opTime.substring(0, 10));
  }

  @Override
  public List<DataFile> writeChangeStore(
      KeyedTable keyedTable, Long txId, ChangeAction action, List<Record> records, boolean orderedWrite) {
    return HiveDataTestHelpers.writeChangeStore(keyedTable, txId, action, records, orderedWrite);
  }

  @Override
  public List<DataFile> writeBaseStore(
      ArcticTable table, long txId, List<Record> records, boolean orderedWrite) {
    return HiveDataTestHelpers.writeBaseStore(table, txId, records, orderedWrite, false);
  }

  @Override
  public List<Record> readKeyedTable(
      KeyedTable keyedTable, Expression expression, Schema projectSchema, boolean useDiskMap, boolean readDeletedData) {
    return HiveDataTestHelpers.readKeyedTable(keyedTable, expression, projectSchema, useDiskMap, readDeletedData);
  }

  @Override
  public List<Record> readChangeStore(
      KeyedTable keyedTable, Expression expression, Schema projectSchema, boolean useDiskMap) {
    return HiveDataTestHelpers.readChangeStore(keyedTable, expression, projectSchema, useDiskMap);
  }

  @Override
  public List<Record> readBaseStore(
      ArcticTable table,
      Expression expression,
      Schema projectSchema,
      boolean useDiskMap) {
    return HiveDataTestHelpers.readBaseStore(table, expression, projectSchema, useDiskMap);
  }
}
