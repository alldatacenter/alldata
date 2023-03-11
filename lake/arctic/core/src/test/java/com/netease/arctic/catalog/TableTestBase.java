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

package com.netease.arctic.catalog;

import com.google.common.collect.Maps;
import com.netease.arctic.TableTestHelpers;
import com.netease.arctic.ams.api.properties.TableFormat;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableBuilder;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.junit.After;
import org.junit.Before;

import java.util.Map;

public abstract class TableTestBase extends CatalogTestBase {

  private final Schema tableSchema;
  private final PartitionSpec partitionSpec;
  private final PrimaryKeySpec primaryKeySpec;
  private final Map<String, String> tableProperties;

  private ArcticTable arcticTable;

  public TableTestBase(
      TableFormat testFormat, Schema tableSchema, PrimaryKeySpec primaryKeySpec,
      PartitionSpec partitionSpec, Map<String, String> tableProperties) {
    super(testFormat);
    this.tableSchema = tableSchema;
    this.partitionSpec = partitionSpec;
    this.primaryKeySpec = primaryKeySpec;
    this.tableProperties = tableProperties;
    if (isKeyedTable()) {
      Preconditions.checkArgument(TableFormat.MIXED_HIVE.equals(testFormat) ||
          TableFormat.MIXED_ICEBERG.equals(testFormat), "Only mixed format table support primary key spec");
    }
  }

  public TableTestBase(
      TableFormat testFormat, boolean keyedTable, boolean partitionedTable,
      Map<String, String> tableProperties) {
    this(testFormat, TableTestHelpers.TABLE_SCHEMA,
        keyedTable ? TableTestHelpers.PRIMARY_KEY_SPEC : PrimaryKeySpec.noPrimaryKey(),
        partitionedTable ? TableTestHelpers.SPEC : PartitionSpec.unpartitioned(),
        tableProperties);
  }

  public TableTestBase(TableFormat testFormat, boolean keyedTable, boolean partitionedTable) {
    this(testFormat, keyedTable, partitionedTable, Maps.newHashMap());
  }

  @Before
  public void setupTable() {
    switch (getTestFormat()) {
      case MIXED_HIVE:
      case MIXED_ICEBERG:
        createMixedFormatTable();
        break;
      case ICEBERG:
        createIcebergFormatTable();
        break;
    }
  }

  private void createMixedFormatTable() {
    getCatalog().createDatabase(TableTestHelpers.TEST_DB_NAME);
    TableBuilder tableBuilder = getCatalog().newTableBuilder(TableTestHelpers.TEST_TABLE_ID, tableSchema);
    tableBuilder.withProperties(tableProperties);
    if (isKeyedTable()) {
      tableBuilder.withPrimaryKeySpec(primaryKeySpec);
    }
    if (isPartitionedTable()) {
      tableBuilder.withPartitionSpec(partitionSpec);
    }
    arcticTable = tableBuilder.create();
  }

  private void createIcebergFormatTable() {
    getIcebergCatalog().createTable(
        TableTestHelpers.TEST_TABLE_ICEBERG_ID,
        tableSchema,
        partitionSpec,
        tableProperties);
    arcticTable = getCatalog().loadTable(TableTestHelpers.TEST_TABLE_ID);
  }

  @After
  public void dropTable() {
    getCatalog().dropTable(TableTestHelpers.TEST_TABLE_ID, true);
    getCatalog().dropDatabase(TableTestHelpers.TEST_DB_NAME);
  }

  protected ArcticTable getArcticTable() {
    return arcticTable;
  }

  protected boolean isKeyedTable() {
    return primaryKeySpec != null && primaryKeySpec.primaryKeyExisted();
  }

  protected boolean isPartitionedTable() {
    return partitionSpec != null && partitionSpec.isPartitioned();
  }
}
