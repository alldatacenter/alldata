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

import com.google.common.collect.Maps;
import com.netease.arctic.TableTestHelpers;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.properties.TableFormat;
import com.netease.arctic.catalog.CatalogTestHelpers;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.hive.TestHMS;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableIdentifier;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.types.Types;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.ClassRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class HiveTableTestBase extends TableTestBase {

  public static final String COLUMN_NAME_ID = "id";
  public static final String COLUMN_NAME_OP_TIME = "op_time";
  public static final String COLUMN_NAME_OP_TIME_WITH_ZONE = "op_time_with_zone";
  public static final String COLUMN_NAME_D = "d$d";
  public static final String COLUMN_NAME_NAME = "name";

  public static final Schema HIVE_TABLE_SCHEMA = new Schema(
      Types.NestedField.required(1, COLUMN_NAME_ID, Types.IntegerType.get()),
      Types.NestedField.required(2, COLUMN_NAME_OP_TIME, Types.TimestampType.withoutZone()),
      Types.NestedField.required(3, COLUMN_NAME_OP_TIME_WITH_ZONE, Types.TimestampType.withZone()),
      Types.NestedField.required(4, COLUMN_NAME_D, Types.DecimalType.of(10, 0)),
      Types.NestedField.required(5, COLUMN_NAME_NAME, Types.StringType.get())
  );

  @ClassRule
  public static TestHMS TEST_HMS = new TestHMS();

  public HiveTableTestBase(Schema tableSchema, PrimaryKeySpec primaryKeySpec,
                           PartitionSpec partitionSpec,
                           Map<String, String> tableProperties) {
    super(TableFormat.MIXED_HIVE, tableSchema, primaryKeySpec, partitionSpec, tableProperties);
  }

  public HiveTableTestBase(boolean keyedTable, boolean partitionedTable,
                           Map<String, String> tableProperties) {
    this(HIVE_TABLE_SCHEMA,
        keyedTable ? TableTestHelpers.PRIMARY_KEY_SPEC : PrimaryKeySpec.noPrimaryKey(),
        partitionedTable ? PartitionSpec.builderFor(HIVE_TABLE_SCHEMA)
            .identity(COLUMN_NAME_NAME).build() : PartitionSpec.unpartitioned(),
        tableProperties);
  }

  public HiveTableTestBase(boolean keyedTable, boolean partitionedTable) {
    this(keyedTable, partitionedTable, Maps.newHashMap());
  }

  @Override
  protected CatalogMeta buildCatalogMeta() {
    Map<String, String> properties = Maps.newHashMap();
    return CatalogTestHelpers.buildHiveCatalogMeta(TEST_CATALOG_NAME,
        properties, TEST_HMS.getHiveConf());
  }

  public void asserFilesName(List<String> exceptedFiles, ArcticTable table) throws TException {
    List<String> fileNameList = new ArrayList<>();
    if (isPartitionedTable()) {
      TableIdentifier identifier = table.id();
      final String database = identifier.getDatabase();
      final String tableName = identifier.getTableName();

      List<Partition> partitions = TEST_HMS.getHiveClient().listPartitions(
          database,
          tableName,
          (short) -1);
      for (Partition p : partitions) {
        fileNameList.addAll(table.io().list(p.getSd().
            getLocation()).stream().map(f -> f.getPath().getName()).sorted().collect(Collectors.toList()));
      }
    } else {
      Table hiveTable = TEST_HMS.getHiveClient().getTable(table.id().getDatabase(), table.name());
      fileNameList.addAll(table.io().list(hiveTable.getSd().
          getLocation()).stream().map(f -> f.getPath().getName()).sorted().collect(Collectors.toList()));
    }
    Assert.assertEquals(exceptedFiles, fileNameList);
  }

}
