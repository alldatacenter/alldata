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

package com.netease.arctic.ams.server.service.impl;

import com.netease.arctic.ams.server.model.TableMetadata;
import com.netease.arctic.ams.server.service.ISupportHiveSyncService;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.utils.ScheduledTasks;
import com.netease.arctic.ams.server.utils.ThreadPool;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.hive.utils.CompatibleHivePropertyUtil;
import com.netease.arctic.hive.utils.HivePartitionUtil;
import com.netease.arctic.hive.utils.TableTypeUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.hive.metastore.Warehouse;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.util.StructLikeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SupportHiveSyncService implements ISupportHiveSyncService {
  private static final Logger LOG = LoggerFactory.getLogger(SupportHiveSyncService.class);
  private static final long SYNC_INTERVAL = 3600_000; // 1 hour

  private ScheduledTasks<TableIdentifier, SupportHiveSyncService.SupportHiveSyncTask> syncTasks;

  @Override
  public void checkHiveSyncTasks() {
    LOG.info("Schedule Support Hive Sync");
    if (syncTasks == null) {
      syncTasks = new ScheduledTasks<>(ThreadPool.Type.HIVE_SYNC);
    }
    List<TableMetadata> tables = ServiceContainer.getMetaService().listTables();
    Set<TableIdentifier> ids =
        tables.stream().map(TableMetadata::getTableIdentifier).collect(Collectors.toSet());
    syncTasks.checkRunningTask(ids,
        () -> 0L,
        () -> SYNC_INTERVAL,
        SupportHiveSyncService.SupportHiveSyncTask::new,
        false);
    LOG.info("Schedule Support Hive Sync finished with {} valid ids", ids.size());
  }

  public static class SupportHiveSyncTask implements ScheduledTasks.Task {
    private final TableIdentifier tableIdentifier;

    SupportHiveSyncTask(TableIdentifier tableIdentifier) {
      this.tableIdentifier = tableIdentifier;
    }

    @Override
    public void run() {
      long startTime = System.currentTimeMillis();
      try {
        LOG.info("{} start hive sync", tableIdentifier);
        ArcticCatalog catalog =
            CatalogLoader.load(ServiceContainer.getTableMetastoreHandler(), tableIdentifier.getCatalog());
        ArcticTable arcticTable = catalog.loadTable(tableIdentifier);
        if (!TableTypeUtil.isHive(arcticTable)) {
          LOG.debug("{} is not a support hive table", tableIdentifier);
          return;
        }

        syncIcebergToHive(arcticTable);
      } catch (Exception e) {
        LOG.error("{} hive sync failed", tableIdentifier, e);
      } finally {
        LOG.info("{} hive sync finished, cost {}ms", tableIdentifier,
            System.currentTimeMillis() - startTime);
      }
    }

    public static void syncIcebergToHive(ArcticTable arcticTable) throws Exception {
      UnkeyedTable baseTable = arcticTable.isKeyedTable() ?
          arcticTable.asKeyedTable().baseTable() : arcticTable.asUnkeyedTable();
      StructLikeMap<Map<String, String>> partitionProperty = baseTable.partitionProperty();

      if (arcticTable.spec().isUnpartitioned()) {
        syncNoPartitionTable(arcticTable, partitionProperty);
      } else {
        syncPartitionTable(arcticTable, partitionProperty);
      }
    }

    /**
     * once get location from iceberg property, should update hive table location,
     * because only arctic update hive table location for unPartitioned table.
     */
    private static void syncNoPartitionTable(ArcticTable arcticTable,
                                             StructLikeMap<Map<String, String>> partitionProperty) {
      Map<String, String> property = partitionProperty.get(TablePropertyUtil.EMPTY_STRUCT);
      if (property == null || property.get(HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION) == null) {
        LOG.debug("{} has no hive location in partition property", arcticTable.id());
        return;
      }

      String currentLocation = property.get(HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION);
      String hiveLocation;
      try {
        hiveLocation = ((SupportHive) arcticTable).getHMSClient().run(client -> {
          Table hiveTable = client.getTable(arcticTable.id().getDatabase(), arcticTable.id().getTableName());
          return hiveTable.getSd().getLocation();
        });
      } catch (Exception e) {
        LOG.error("{} get hive location failed", arcticTable.id(), e);
        return;
      }

      if (!Objects.equals(currentLocation, hiveLocation)) {
        try {
          ((SupportHive) arcticTable).getHMSClient().run(client -> {
            Table hiveTable = client.getTable(arcticTable.id().getDatabase(), arcticTable.id().getTableName());
            hiveTable.getSd().setLocation(currentLocation);
            client.alterTable(arcticTable.id().getDatabase(), arcticTable.id().getTableName(), hiveTable);
            return null;
          });
        } catch (Exception e) {
          LOG.error("{} alter hive location failed", arcticTable.id(), e);
        }
      }
    }

    private static void syncPartitionTable(ArcticTable arcticTable,
                                           StructLikeMap<Map<String, String>> partitionProperty) throws Exception {
      Map<String, StructLike> icebergPartitionMap = new HashMap<>();
      for (StructLike structLike : partitionProperty.keySet()) {
        icebergPartitionMap.put(arcticTable.spec().partitionToPath(structLike), structLike);
      }
      List<String> icebergPartitions = new ArrayList<>(icebergPartitionMap.keySet());
      List<Partition> hivePartitions = ((SupportHive) arcticTable).getHMSClient().run(client ->
          client.listPartitions(arcticTable.id().getDatabase(), arcticTable.id().getTableName(), Short.MAX_VALUE));
      List<String> hivePartitionNames = ((SupportHive) arcticTable).getHMSClient().run(client ->
          client.listPartitionNames(arcticTable.id().getDatabase(), arcticTable.id().getTableName(), Short.MAX_VALUE));
      List<FieldSchema> partitionKeys = ((SupportHive) arcticTable).getHMSClient().run(client -> {
        Table hiveTable = client.getTable(arcticTable.id().getDatabase(), arcticTable.id().getTableName());
        return hiveTable.getPartitionKeys();
      });
      Map<String, Partition> hivePartitionMap = new HashMap<>();
      for (Partition hivePartition : hivePartitions) {
        hivePartitionMap.put(Warehouse.makePartName(partitionKeys, hivePartition.getValues()), hivePartition);
      }

      Set<String> inIcebergNotInHive = icebergPartitions.stream()
          .filter(partition -> !hivePartitionNames.contains(partition))
          .collect(Collectors.toSet());
      Set<String> inHiveNotInIceberg = hivePartitionNames.stream()
          .filter(partition -> !icebergPartitions.contains(partition))
          .collect(Collectors.toSet());
      Set<String> inBoth = icebergPartitions.stream()
          .filter(hivePartitionNames::contains)
          .collect(Collectors.toSet());

      if (CollectionUtils.isNotEmpty(inIcebergNotInHive)) {
        handleInIcebergPartitions(arcticTable, inIcebergNotInHive, icebergPartitionMap, partitionProperty);
      }

      if (CollectionUtils.isNotEmpty(inHiveNotInIceberg)) {
        handleInHivePartitions(arcticTable, inHiveNotInIceberg, hivePartitionMap);
      }

      if (CollectionUtils.isNotEmpty(inBoth)) {
        handleInBothPartitions(arcticTable, inBoth, hivePartitionMap, icebergPartitionMap, partitionProperty);
      }
    }

    /**
     * if iceberg partition location is existed, should update hive table location.
     */
    private static void handleInIcebergPartitions(ArcticTable arcticTable,
                                                  Set<String> inIcebergNotInHive,
                                                  Map<String, StructLike> icebergPartitionMap,
                                                  StructLikeMap<Map<String, String>> partitionProperty) {
      inIcebergNotInHive.forEach(partition -> {
        Map<String, String> property = partitionProperty.get(icebergPartitionMap.get(partition));
        if (property == null || property.get(HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION) == null) {
          return;
        }
        String currentLocation = property.get(HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION);

        if (arcticTable.io().exists(currentLocation)) {
          int transientTime = Integer.parseInt(property
              .getOrDefault(HiveTableProperties.PARTITION_PROPERTIES_KEY_TRANSIENT_TIME, "0"));
          List<DataFile> dataFiles = getIcebergPartitionFiles(arcticTable, icebergPartitionMap.get(partition));
          HivePartitionUtil.createPartitionIfAbsent(((SupportHive) arcticTable).getHMSClient(),
              arcticTable,
              HivePartitionUtil.partitionValuesAsList(icebergPartitionMap.get(partition),
                  arcticTable.spec().partitionType()),
              currentLocation, dataFiles, transientTime);
        }
      });
    }

    private static void handleInHivePartitions(ArcticTable arcticTable,
                                               Set<String> inHiveNotInIceberg,
                                               Map<String, Partition> hivePartitionMap) {
      inHiveNotInIceberg.forEach(partition -> {
        Partition hivePartition = hivePartitionMap.get(partition);
        boolean isArctic = CompatibleHivePropertyUtil.propertyAsBoolean(hivePartition.getParameters(),
            HiveTableProperties.ARCTIC_TABLE_FLAG, false);
        if (isArctic) {
          HivePartitionUtil.dropPartition(((SupportHive) arcticTable).getHMSClient(), arcticTable, hivePartition);
        }
      });
    }

    private static void handleInBothPartitions(ArcticTable arcticTable,
                                               Set<String> inBoth,
                                               Map<String, Partition> hivePartitionMap,
                                               Map<String, StructLike> icebergPartitionMap,
                                               StructLikeMap<Map<String, String>> partitionProperty) {
      Set<String> inHiveNotInIceberg = new HashSet<>();
      inBoth.forEach(partition -> {
        Map<String, String> property = partitionProperty.get(icebergPartitionMap.get(partition));
        if (property == null || property.get(HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION) == null) {
          inHiveNotInIceberg.add(partition);
          return;
        }

        String currentLocation = property.get(HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION);
        Partition hivePartition = hivePartitionMap.get(partition);

        if (!Objects.equals(currentLocation, hivePartition.getSd().getLocation())) {
          int transientTime = Integer.parseInt(property
              .getOrDefault(HiveTableProperties.PARTITION_PROPERTIES_KEY_TRANSIENT_TIME, "0"));
          List<DataFile> dataFiles = getIcebergPartitionFiles(arcticTable, icebergPartitionMap.get(partition));
          HivePartitionUtil.updatePartitionLocation(((SupportHive) arcticTable).getHMSClient(),
              arcticTable, hivePartition, currentLocation, dataFiles, transientTime);
        }
      });

      handleInHivePartitions(arcticTable, inHiveNotInIceberg, hivePartitionMap);
    }

    private static List<DataFile> getIcebergPartitionFiles(ArcticTable arcticTable,
                                                           StructLike partition) {
      UnkeyedTable baseStore;
      baseStore = arcticTable.isKeyedTable() ? arcticTable.asKeyedTable().baseTable() : arcticTable.asUnkeyedTable();

      List<DataFile> partitionFiles = new ArrayList<>();
      arcticTable.io().doAs(() -> {
        try (CloseableIterable<FileScanTask> fileScanTasks = baseStore.newScan().planFiles()) {
          for (FileScanTask fileScanTask : fileScanTasks) {
            if (fileScanTask.file().partition().equals(partition)) {
              partitionFiles.add(fileScanTask.file());
            }
          }
        }

        return null;
      });

      return partitionFiles;
    }
  }
}
