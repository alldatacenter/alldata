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

import com.netease.arctic.hive.HMSClientPool;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.catalog.ArcticHiveCatalog;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableIdentifier;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpgradeHiveTableUtil {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeHiveTableUtil.class);

  private static final long DEFAULT_TXID = 0L;

  /**
   * Upgrade a hive table to an Arctic table.
   *
   * @param arcticHiveCatalog A arctic catalog adapt hive
   * @param tableIdentifier A table identifier
   * @param pkList The name of the columns that needs to be set as the primary key
   * @param properties Properties to be added to the target table
   */
  public static void upgradeHiveTable(ArcticHiveCatalog arcticHiveCatalog, TableIdentifier tableIdentifier,
                                      List<String> pkList, Map<String, String> properties) throws Exception {
    if (!formatCheck(arcticHiveCatalog.getHMSClient(), tableIdentifier)) {
      throw new IllegalArgumentException("Only support storage format is parquet");
    }
    boolean upgradeHive = false;
    try {
      Table hiveTable = HiveTableUtil.loadHmsTable(arcticHiveCatalog.getHMSClient(), tableIdentifier);

      Schema schema = HiveSchemaUtil.convertHiveSchemaToIcebergSchema(hiveTable, pkList);

      List<FieldSchema> partitionKeys = hiveTable.getPartitionKeys();

      PartitionSpec.Builder partitionBuilder = PartitionSpec.builderFor(schema);
      partitionKeys.stream().forEach(p -> partitionBuilder.identity(p.getName()));

      PrimaryKeySpec.Builder primaryKeyBuilder = PrimaryKeySpec.builderFor(schema);
      pkList.stream().forEach(p -> primaryKeyBuilder.addColumn(p));

      ArcticTable arcticTable = arcticHiveCatalog.newTableBuilder(tableIdentifier, schema)
          .withProperties(properties)
          .withPartitionSpec(partitionBuilder.build())
          .withPrimaryKeySpec(primaryKeyBuilder.build())
          .withProperty(HiveTableProperties.ALLOW_HIVE_TABLE_EXISTED, "true")
          .create();
      upgradeHive = true;
      UpgradeHiveTableUtil.hiveDataMigration(arcticTable, arcticHiveCatalog, tableIdentifier);
    } catch (Throwable t) {
      if (upgradeHive) {
        arcticHiveCatalog.dropTableButNotDropHiveTable(tableIdentifier);
      }
      throw t;
    }
  }

  private static void hiveDataMigration(ArcticTable arcticTable, ArcticHiveCatalog arcticHiveCatalog,
                                        TableIdentifier tableIdentifier)
      throws Exception {
    Table hiveTable = HiveTableUtil.loadHmsTable(arcticHiveCatalog.getHMSClient(), tableIdentifier);
    String hiveDataLocation = HiveTableUtil.hiveRootLocation(hiveTable.getSd().getLocation());
    arcticTable.io().mkdirs(hiveDataLocation);
    String newPath;
    if (hiveTable.getPartitionKeys().isEmpty()) {
      newPath = hiveDataLocation + "/" + System.currentTimeMillis() + "_" + UUID.randomUUID();
      arcticTable.io().mkdirs(newPath);
      for (FileStatus fileStatus : arcticTable.io().list(hiveTable.getSd().getLocation())) {
        if (!fileStatus.isDirectory()) {
          arcticTable.io().rename(fileStatus.getPath().toString(), newPath);
        }
      }

      try {
        HiveTableUtil.alterTableLocation(arcticHiveCatalog.getHMSClient(), arcticTable.id(), newPath);
        LOG.info("table{" + arcticTable.name() + "} alter hive table location " + hiveDataLocation + " success");
      } catch (IOException e) {
        LOG.warn("table{" + arcticTable.name() + "} alter hive table location failed", e);
        throw new RuntimeException(e);
      }
    } else {
      List<String> partitions =
          HivePartitionUtil.getHivePartitionNames(arcticHiveCatalog.getHMSClient(), tableIdentifier);
      List<String> partitionLocations =
          HivePartitionUtil.getHivePartitionLocations(arcticHiveCatalog.getHMSClient(), tableIdentifier);
      for (int i = 0; i < partitionLocations.size(); i++) {
        String partition = partitions.get(i);
        String oldLocation = partitionLocations.get(i);
        String newLocation = hiveDataLocation + "/" + partition + "/" + HiveTableUtil.newHiveSubdirectory(DEFAULT_TXID);
        arcticTable.io().mkdirs(newLocation);
        for (FileStatus fileStatus : arcticTable.io().list(oldLocation)) {
          if (!fileStatus.isDirectory()) {
            arcticTable.io().rename(fileStatus.getPath().toString(), newLocation);
          }
        }
        HivePartitionUtil.alterPartition(arcticHiveCatalog.getHMSClient(), tableIdentifier, partition, newLocation);
      }
    }
    HiveMetaSynchronizer.syncHiveDataToArctic(arcticTable, arcticHiveCatalog.getHMSClient());
  }

  /**
   * Check whether Arctic supports the hive table storage formats.
   *
   * @param hiveClient Hive client from ArcticHiveCatalog
   * @param tableIdentifier A table identifier
   * @return Support or not
   */
  private static boolean formatCheck(HMSClientPool hiveClient, TableIdentifier tableIdentifier) throws IOException {
    AtomicBoolean isSupport = new AtomicBoolean(false);
    try {
      hiveClient.run(client -> {
        Table hiveTable = HiveTableUtil.loadHmsTable(hiveClient, tableIdentifier);
        StorageDescriptor storageDescriptor = hiveTable.getSd();
        SerDeInfo serDeInfo = storageDescriptor.getSerdeInfo();
        switch (storageDescriptor.getInputFormat()) {
          case HiveTableProperties.PARQUET_INPUT_FORMAT:
            if (storageDescriptor.getOutputFormat().equals(HiveTableProperties.PARQUET_OUTPUT_FORMAT) &&
                serDeInfo.getSerializationLib().equals(HiveTableProperties.PARQUET_ROW_FORMAT_SERDE)) {
              isSupport.set(true);
            } else {
              throw new IllegalStateException("Please check your hive table storage format is right");
            }
            break;
          default:
            isSupport.set(false);
            break;
        }
        return null;
      });
    } catch (Exception e) {
      throw new IOException(e);
    }
    return isSupport.get();
  }
}
