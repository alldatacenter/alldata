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

package com.netease.arctic.hive.utils;

import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.CatalogTestBase;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.TestHMS;
import com.netease.arctic.hive.catalog.ArcticHiveCatalog;
import com.netease.arctic.hive.catalog.HiveCatalogTestHelper;
import com.netease.arctic.hive.catalog.HiveTableTestHelper;
import com.netease.arctic.hive.table.UnkeyedHiveTable;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.DataFiles;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RunWith(Parameterized.class)
public class TestUpgradeHiveTableUtil extends CatalogTestBase {

  @ClassRule
  public static TestHMS TEST_HMS = new TestHMS();

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private Table hiveTable;
  private TableIdentifier identifier;
  private String db = "testUpgradeHiveDb";
  private String table = "testUpgradeHiveTable";
  private boolean isPartitioned;
  private String[] partitionNames = {"name", HiveTableTestHelper.COLUMN_NAME_OP_DAY};
  private String[] partitionValues = {"Bob", "2020-01-01"};

  public TestUpgradeHiveTableUtil(
      CatalogTestHelper catalogTestHelper, boolean isPartitioned) throws IOException {
    super(catalogTestHelper);
    folder.create();
    this.isPartitioned = isPartitioned;
  }

  @Before
  public void setUp() throws TException, IOException {
    Database database = new Database();
    database.setName(db);
    TEST_HMS.getHiveClient().createDatabase(database);
    PartitionSpec spec = isPartitioned ? PartitionSpec.builderFor(HiveTableTestHelper.HIVE_TABLE_SCHEMA)
        .identity("name").identity(HiveTableTestHelper.COLUMN_NAME_OP_DAY).build() : PartitionSpec.unpartitioned();
    hiveTable = createHiveTable(db, table, HiveTableTestHelper.HIVE_TABLE_SCHEMA, spec);
    if (isPartitioned) {
      createPartition();
    }
    identifier = TableIdentifier.of(getCatalog().name(), db, table);
  }

  @After
  public void dropTable() throws TException {
    getCatalog().dropTable(identifier, true);
    TEST_HMS.getHiveClient().dropDatabase(db);
  }

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[] parameters() {
    return new Object[][] {{new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()), true},
                           {new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()), false}};
  }

  @Test
  public void upgradeHiveTable() throws Exception {
    UpgradeHiveTableUtil.upgradeHiveTable(
        (ArcticHiveCatalog) getCatalog(),
        identifier,
        new ArrayList<>(),
        new HashMap<>());
    ArcticTable table = getCatalog().loadTable(identifier);
    UnkeyedHiveTable baseTable = table.isKeyedTable() ?
        (UnkeyedHiveTable) table.asKeyedTable().baseTable() :
        (UnkeyedHiveTable) table.asUnkeyedTable();
    if (table.spec().isPartitioned()) {
      List<Partition> partitions =
          HivePartitionUtil.getHiveAllPartitions(((ArcticHiveCatalog) getCatalog()).getHMSClient(), table.id());
      for (Partition partition : partitions) {
        StructLike partitionData = DataFiles.data(table.spec(), String.join("/", partition.getValues()));
        Map<String, String> partitionProperties = baseTable.partitionProperty().get(partitionData);
        Assert.assertTrue(partitionProperties.containsKey(HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION));
        Assert.assertTrue(partitionProperties.containsKey(HiveTableProperties.PARTITION_PROPERTIES_KEY_TRANSIENT_TIME));
        Assert.assertFalse(HiveMetaSynchronizer.partitionHasModified(baseTable, partition, partitionData));
      }
    } else {
      Map<String, String> partitionProperties = baseTable.partitionProperty().get(TablePropertyUtil.EMPTY_STRUCT);
      Assert.assertTrue(partitionProperties.containsKey(HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION));
      Assert.assertTrue(partitionProperties.containsKey(HiveTableProperties.PARTITION_PROPERTIES_KEY_TRANSIENT_TIME));
      Assert.assertFalse(HiveMetaSynchronizer.tableHasModified(baseTable, hiveTable));
    }
  }

  private Table createHiveTable(String db, String table, Schema schema, PartitionSpec spec) throws TException,
      IOException {
    Table hiveTable = newHiveTable(db, table, schema, spec);
    hiveTable.setSd(HiveTableUtil.storageDescriptor(
        schema,
        spec,
        folder.newFolder(db).getAbsolutePath(),
        FileFormat.valueOf(TableProperties.DEFAULT_FILE_FORMAT_DEFAULT.toUpperCase(Locale.ENGLISH))));
    TEST_HMS.getHiveClient().createTable(hiveTable);
    return TEST_HMS.getHiveClient().getTable(db, table);
  }

  private void createPartition() throws TException {
    List<String> partitions = Lists.newArrayList();
    for (int i = 0; i < partitionNames.length; i++) {
      partitions.add(String.join("=", partitionNames[i], partitionValues[i]));
    }
    Partition newPartition =
        HivePartitionUtil.newPartition(hiveTable, partitions,
            hiveTable.getSd().getLocation() + "/" + String.join("/", partitions), new ArrayList<>(),
            (int) (System.currentTimeMillis() / 1000));
    TEST_HMS.getHiveClient().add_partition(newPartition);
  }

  private org.apache.hadoop.hive.metastore.api.Table newHiveTable(
      String db, String table, Schema schema, PartitionSpec partitionSpec) {
    final long currentTimeMillis = System.currentTimeMillis();
    org.apache.hadoop.hive.metastore.api.Table newTable = new org.apache.hadoop.hive.metastore.api.Table(
        table,
        db,
        System.getProperty("user.name"),
        (int) currentTimeMillis / 1000,
        (int) currentTimeMillis / 1000,
        Integer.MAX_VALUE,
        null,
        HiveSchemaUtil.hivePartitionFields(schema, partitionSpec),
        new HashMap<>(),
        null,
        null,
        TableType.EXTERNAL_TABLE.toString());

    newTable.getParameters().put("EXTERNAL", "TRUE");
    return newTable;
  }
}
