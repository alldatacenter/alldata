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

package com.netease.arctic.ams.server.service;

import com.netease.arctic.CatalogMetaTestUtil;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.MockArcticMetastoreServer;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.ams.server.model.UpgradeHiveMeta;
import com.netease.arctic.ams.server.model.UpgradeRunningInfo;
import com.netease.arctic.ams.server.model.UpgradeStatus;
import com.netease.arctic.ams.server.service.impl.AdaptHiveService;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.hive.HMSMockServer;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.catalog.ArcticHiveCatalog;
import com.netease.arctic.table.TableIdentifier;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_HIVE;

public class TestAdaptHiveService {

  protected static final String HIVE_DB_NAME = "hivedb";
  protected static final String HIVE_CATALOG_NAME = "hive_catalog";
  protected static final AtomicInteger testCount = new AtomicInteger(0);

  protected static final TemporaryFolder tempFolder = new TemporaryFolder();

  protected static HMSMockServer hms;
  protected static final MockArcticMetastoreServer AMS = MockArcticMetastoreServer.getInstance();

  protected static final TableIdentifier HIVE_TABLE_ID =
      TableIdentifier.of(HIVE_CATALOG_NAME, HIVE_DB_NAME, "test_hive_table");
  protected static final TableIdentifier HIVE_PK_TABLE_ID =
      TableIdentifier.of(HIVE_CATALOG_NAME, HIVE_DB_NAME, "test_pk_hive_table");

  protected static final TableIdentifier UN_PARTITION_HIVE_TABLE_ID =
      TableIdentifier.of(HIVE_CATALOG_NAME, HIVE_DB_NAME, "un_partition_test_hive_table");
  protected static final TableIdentifier UN_PARTITION_HIVE_PK_TABLE_ID =
      TableIdentifier.of(HIVE_CATALOG_NAME, HIVE_DB_NAME, "un_partition_test_pk_hive_table");

  protected static final TableIdentifier UNSUPPORTED_TABLE =
      TableIdentifier.of(HIVE_CATALOG_NAME, HIVE_DB_NAME, "unsupported_table");


  protected static Table PARTITION_TABLE = new Table();
  protected static Table PARTITION_TABLE_2 = new Table();
  protected static Table NO_PARTITION_TABLE = new Table();
  protected static Table NO_PARTITION_TABLE_2 = new Table();
  protected static Table UNSUPPORTED_ORC_TABLE = new Table();

  protected static UpgradeHiveMeta withPk = new UpgradeHiveMeta();
  protected static UpgradeHiveMeta withoutPk = new UpgradeHiveMeta();

  protected static AdaptHiveService adaptHiveService = new AdaptHiveService();
  protected static ArcticHiveCatalog hiveCatalog;

  @BeforeClass
  public static void startMetastore() throws Exception {
    int ref = testCount.incrementAndGet();
    if (ref == 1){
      tempFolder.create();
      hms = new HMSMockServer(tempFolder.newFolder("hive"));
      hms.start();

      String dbPath = hms.getDatabasePath(HIVE_DB_NAME);
      Database db = new Database(HIVE_DB_NAME, "description", dbPath, new HashMap<>());
      hms.getClient().createDatabase(db);

      Map<String, String> storageConfig = new HashMap<>();
      storageConfig.put(
          CatalogMetaProperties.STORAGE_CONFIGS_KEY_TYPE,
          CatalogMetaProperties.STORAGE_CONFIGS_VALUE_TYPE_HDFS);
      storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_CORE_SITE, MockArcticMetastoreServer.getHadoopSite());
      storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_HDFS_SITE, MockArcticMetastoreServer.getHadoopSite());
      storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_HIVE_SITE, CatalogMetaTestUtil.encodingSite(hms.hiveConf()));


      Map<String, String> authConfig = new HashMap<>();
      authConfig.put(CatalogMetaProperties.AUTH_CONFIGS_KEY_TYPE,
          CatalogMetaProperties.AUTH_CONFIGS_VALUE_TYPE_SIMPLE);
      authConfig.put(CatalogMetaProperties.AUTH_CONFIGS_KEY_HADOOP_USERNAME,
          System.getProperty("user.name"));

      Map<String, String> catalogProperties = new HashMap<>();

      CatalogMeta catalogMeta = new CatalogMeta(HIVE_CATALOG_NAME, CATALOG_TYPE_HIVE,
          storageConfig, authConfig, catalogProperties);
      AMS.createCatalogIfAbsent(catalogMeta);
      setupTables();
    }
  }

  @AfterClass
  public static void stopMetastore() {
    int ref = testCount.decrementAndGet();
    if (ref == 0){
      hms.stop();
      hms = null;
      tempFolder.delete();
    }
  }

  private static void setupTables() throws Exception {
    List<FieldSchema> schema = new ArrayList<>();
    schema.add(new FieldSchema("id", "int", "test comment"));
    schema.add(new FieldSchema("name", "string", null));
    schema.add(new FieldSchema("age", "int", null));
    StorageDescriptor storageDescriptor = new StorageDescriptor();
    storageDescriptor.setInputFormat(HiveTableProperties.PARQUET_INPUT_FORMAT);
    storageDescriptor.setOutputFormat(HiveTableProperties.PARQUET_OUTPUT_FORMAT);
    storageDescriptor.setCols(schema);
    SerDeInfo serDeInfo = new SerDeInfo();
    serDeInfo.setSerializationLib(HiveTableProperties.PARQUET_ROW_FORMAT_SERDE);
    storageDescriptor.setSerdeInfo(serDeInfo);
    List<FieldSchema> partitions = new ArrayList<>();
    partitions.add(new FieldSchema("dt", "string", null));
    partitions.add(new FieldSchema("ts", "string", null));

    PARTITION_TABLE.setDbName(HIVE_DB_NAME);
    PARTITION_TABLE.setTableName(HIVE_TABLE_ID.getTableName());
    PARTITION_TABLE.setSd(storageDescriptor);
    PARTITION_TABLE.setPartitionKeys(partitions);
    hms.getClient().createTable(PARTITION_TABLE);

    PARTITION_TABLE_2.setDbName(HIVE_DB_NAME);
    PARTITION_TABLE_2.setTableName(HIVE_PK_TABLE_ID.getTableName());
    PARTITION_TABLE_2.setSd(storageDescriptor);
    PARTITION_TABLE_2.setPartitionKeys(partitions);
    hms.getClient().createTable(PARTITION_TABLE_2);

    NO_PARTITION_TABLE.setDbName(HIVE_DB_NAME);
    NO_PARTITION_TABLE.setTableName(UN_PARTITION_HIVE_TABLE_ID.getTableName());
    NO_PARTITION_TABLE.setSd(storageDescriptor);
    hms.getClient().createTable(NO_PARTITION_TABLE);

    NO_PARTITION_TABLE_2.setDbName(HIVE_DB_NAME);
    NO_PARTITION_TABLE_2.setTableName(UN_PARTITION_HIVE_PK_TABLE_ID.getTableName());
    NO_PARTITION_TABLE_2.setSd(storageDescriptor);
    hms.getClient().createTable(NO_PARTITION_TABLE_2);

    StorageDescriptor wrongSd = new StorageDescriptor();
    wrongSd.setInputFormat("org.apache.hadoop.hive.ql.io.orc.OrcInputFormat");
    wrongSd.setOutputFormat("org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat");
    wrongSd.setCols(schema);
    SerDeInfo wrongSerDeInfo = new SerDeInfo();
    wrongSerDeInfo.setSerializationLib("org.apache.hadoop.hive.ql.io.orc.OrcSerde");
    wrongSd.setSerdeInfo(wrongSerDeInfo);
    UNSUPPORTED_ORC_TABLE.setDbName(HIVE_DB_NAME);
    UNSUPPORTED_ORC_TABLE.setTableName(UNSUPPORTED_TABLE.getTableName());
    UNSUPPORTED_ORC_TABLE.setSd(wrongSd);
    UNSUPPORTED_ORC_TABLE.setPartitionKeys(partitions);
    hms.getClient().createTable(UNSUPPORTED_ORC_TABLE);

    List<UpgradeHiveMeta.PrimaryKeyField> pkList = new ArrayList<>();
    Map<String, String> properties = new HashMap<>();
    pkList.add(new UpgradeHiveMeta.PrimaryKeyField("id"));
    withPk.setPkList(pkList);
    withPk.setProperties(properties);
    withoutPk.setPkList(new ArrayList<>());
    withoutPk.setProperties(properties);
  }

  @Test
  public void testUpgradeHiveTable() throws InterruptedException {
    hiveCatalog = (ArcticHiveCatalog) CatalogLoader.load(AMS.getUrl(HIVE_CATALOG_NAME));
    adaptHiveService.upgradeHiveTable(hiveCatalog, UN_PARTITION_HIVE_TABLE_ID, withoutPk);
    UpgradeRunningInfo upgradeRunningInfo = adaptHiveService.getUpgradeRunningInfo(UN_PARTITION_HIVE_TABLE_ID);
    while (upgradeRunningInfo.getStatus().equals(UpgradeStatus.NONE.toString())
        || upgradeRunningInfo.getStatus().equals(UpgradeStatus.UPGRADING.toString())) {
      Thread.sleep(3000);
      upgradeRunningInfo = adaptHiveService.getUpgradeRunningInfo(UN_PARTITION_HIVE_TABLE_ID);
    }
    Assert.assertEquals(upgradeRunningInfo.getStatus(), UpgradeStatus.SUCCESS.toString());

    adaptHiveService.upgradeHiveTable(hiveCatalog, HIVE_TABLE_ID, withoutPk);
    upgradeRunningInfo = adaptHiveService.getUpgradeRunningInfo(HIVE_TABLE_ID);
    while (upgradeRunningInfo.getStatus().equals(UpgradeStatus.NONE.toString())
        || upgradeRunningInfo.getStatus().equals(UpgradeStatus.UPGRADING.toString())) {
      Thread.sleep(3000);
      upgradeRunningInfo = adaptHiveService.getUpgradeRunningInfo(HIVE_TABLE_ID);
    }
    Assert.assertEquals(upgradeRunningInfo.getStatus(), UpgradeStatus.SUCCESS.toString());

    adaptHiveService.upgradeHiveTable(hiveCatalog, UN_PARTITION_HIVE_PK_TABLE_ID, withPk);
    upgradeRunningInfo = adaptHiveService.getUpgradeRunningInfo(UN_PARTITION_HIVE_PK_TABLE_ID);
    while (upgradeRunningInfo.getStatus().equals(UpgradeStatus.NONE.toString())
        || upgradeRunningInfo.getStatus().equals(UpgradeStatus.UPGRADING.toString())) {
      Thread.sleep(3000);
      upgradeRunningInfo = adaptHiveService.getUpgradeRunningInfo(UN_PARTITION_HIVE_PK_TABLE_ID);
    }
    Assert.assertEquals(upgradeRunningInfo.getStatus(), UpgradeStatus.SUCCESS.toString());

    adaptHiveService.upgradeHiveTable(hiveCatalog, HIVE_PK_TABLE_ID, withPk);
    upgradeRunningInfo = adaptHiveService.getUpgradeRunningInfo(HIVE_PK_TABLE_ID);
    while (upgradeRunningInfo.getStatus().equals(UpgradeStatus.NONE.toString())
        || upgradeRunningInfo.getStatus().equals(UpgradeStatus.UPGRADING.toString())) {
      Thread.sleep(3000);
      upgradeRunningInfo = adaptHiveService.getUpgradeRunningInfo(HIVE_PK_TABLE_ID);
    }
    Assert.assertEquals(upgradeRunningInfo.getStatus(), UpgradeStatus.SUCCESS.toString());

    adaptHiveService.upgradeHiveTable(hiveCatalog, UNSUPPORTED_TABLE, withPk);
    upgradeRunningInfo = adaptHiveService.getUpgradeRunningInfo(UNSUPPORTED_TABLE);
    while (upgradeRunningInfo.getStatus().equals(UpgradeStatus.NONE.toString())
        || upgradeRunningInfo.getStatus().equals(UpgradeStatus.UPGRADING.toString())) {
      Thread.sleep(3000);
      upgradeRunningInfo = adaptHiveService.getUpgradeRunningInfo(UNSUPPORTED_TABLE);
    }
    Assert.assertEquals(upgradeRunningInfo.getStatus(), UpgradeStatus.FAILED.toString());
  }
}
