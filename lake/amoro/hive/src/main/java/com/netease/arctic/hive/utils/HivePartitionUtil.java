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

import com.netease.arctic.hive.HMSClient;
import com.netease.arctic.hive.HMSClientPool;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import org.apache.hadoop.hive.metastore.PartitionDropOptions;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.PrincipalPrivilegeSet;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.ClientPool;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DataFiles;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.exceptions.NoSuchTableException;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

public class HivePartitionUtil {

  private static final Logger LOG = LoggerFactory.getLogger(HivePartitionUtil.class);

  public static List<String> partitionValuesAsList(StructLike partitionData, Types.StructType partitionSchema) {
    List<Types.NestedField> fields = partitionSchema.fields();
    List<String> values = Lists.newArrayList();
    for (int i = 0; i < fields.size(); i++) {
      Type type = fields.get(i).type();
      Object value = partitionData.get(i, type.typeId().javaClass());
      values.add(String.valueOf(value));
    }
    return values;
  }

  public static StructLike buildPartitionData(List<String> partitionValues, PartitionSpec spec) {
    StringBuilder pathBuilder = new StringBuilder();
    for (int i = 0; i < spec.partitionType().fields().size(); i++) {
      Types.NestedField field = spec.partitionType().fields().get(i);
      pathBuilder.append(field.name()).append("=").append(partitionValues.get(i));
      if (i < spec.partitionType().fields().size() - 1) {
        pathBuilder.append("/");
      }
    }
    return DataFiles.data(spec, pathBuilder.toString());
  }

  public static Partition newPartition(
      Table hiveTable,
      List<String> values,
      String location,
      List<DataFile> dataFiles,
      int createTimeInSeconds) {
    StorageDescriptor tableSd = hiveTable.getSd();
    PrincipalPrivilegeSet privilegeSet = hiveTable.getPrivileges();
    Partition p = new Partition();
    p.setValues(values);
    p.setDbName(hiveTable.getDbName());
    p.setTableName(hiveTable.getTableName());
    p.setCreateTime(createTimeInSeconds);
    p.setLastAccessTime(createTimeInSeconds);
    StorageDescriptor sd = tableSd.deepCopy();
    sd.setLocation(location);
    p.setSd(sd);

    HiveTableUtil.generateTableProperties(createTimeInSeconds, dataFiles)
        .forEach(p::putToParameters);

    if (privilegeSet != null) {
      p.setPrivileges(privilegeSet.deepCopy());
    }
    return p;
  }

  public static Partition getPartition(
      HMSClientPool hmsClient,
      ArcticTable arcticTable,
      List<String> partitionValues) {
    String db = arcticTable.id().getDatabase();
    String tableName = arcticTable.id().getTableName();

    try {
      return hmsClient.run(client -> {
        Partition partition;
        partition = client.getPartition(db, tableName, partitionValues);
        return partition;
      });
    } catch (NoSuchObjectException e) {
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void rewriteHivePartitions(
      Partition partition, String location, List<DataFile> dataFiles,
      int accessTimestamp) {
    partition.getSd().setLocation(location);
    partition.setLastAccessTime(accessTimestamp);
    HiveTableUtil.generateTableProperties(accessTimestamp, dataFiles)
        .forEach(partition::putToParameters);
  }

  /**
   * Gets all partitions object of the Hive table.
   *
   * @param hiveClient      Hive client from ArcticHiveCatalog
   * @param tableIdentifier A table identifier
   * @return A List of Hive partition objects
   */
  public static List<Partition> getHiveAllPartitions(HMSClientPool hiveClient, TableIdentifier tableIdentifier) {
    try {
      return hiveClient.run(client ->
          client.listPartitions(tableIdentifier.getDatabase(), tableIdentifier.getTableName(), Short.MAX_VALUE));
    } catch (NoSuchObjectException e) {
      throw new NoSuchTableException(e, "Hive table does not exist: %s", tableIdentifier.getTableName());
    } catch (TException e) {
      throw new RuntimeException("Failed to get partitions " + tableIdentifier.getTableName(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted in call to listPartitions", e);
    }
  }

  /**
   * Gets all partition names of the Hive table.
   *
   * @param hiveClient      Hive client from ArcticHiveCatalog
   * @param tableIdentifier A table identifier
   * @return A List of Hive partition names
   */
  public static List<String> getHivePartitionNames(HMSClientPool hiveClient, TableIdentifier tableIdentifier) {
    try {
      return hiveClient.run(client -> client.listPartitionNames(
          tableIdentifier.getDatabase(),
          tableIdentifier.getTableName(),
          Short.MAX_VALUE)).stream().collect(Collectors.toList());
    } catch (NoSuchObjectException e) {
      throw new NoSuchTableException(e, "Hive table does not exist: %s", tableIdentifier.getTableName());
    } catch (TException e) {
      throw new RuntimeException("Failed to get partitions " + tableIdentifier.getTableName(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted in call to listPartitions", e);
    }
  }

  /**
   * Gets all partitions location of the Hive table.
   *
   * @param hiveClient      Hive client from ArcticHiveCatalog
   * @param tableIdentifier A table identifier
   * @return A List of Hive partition locations
   */
  public static List<String> getHivePartitionLocations(HMSClientPool hiveClient, TableIdentifier tableIdentifier) {
    try {
      return hiveClient.run(client -> client.listPartitions(
              tableIdentifier.getDatabase(),
              tableIdentifier.getTableName(),
              Short.MAX_VALUE))
          .stream()
          .map(partition -> partition.getSd().getLocation())
          .collect(Collectors.toList());
    } catch (NoSuchObjectException e) {
      throw new NoSuchTableException(e, "Hive table does not exist: %s", tableIdentifier.getTableName());
    } catch (TException e) {
      throw new RuntimeException("Failed to get partitions " + tableIdentifier.getTableName(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted in call to listPartitions", e);
    }
  }

  /**
   * Change the Hive partition location.
   *
   * @param hiveClient      Hive client from ArcticHiveCatalog
   * @param tableIdentifier A table identifier
   * @param partition       A Hive partition name
   * @param newPath         Target partition location
   */
  public static void alterPartition(
      HMSClientPool hiveClient, TableIdentifier tableIdentifier,
      String partition, String newPath) throws IOException {
    try {
      LOG.info("alter table {} hive partition {} to new location {}",
          tableIdentifier, partition, newPath);
      Partition oldPartition = hiveClient.run(
          client -> client.getPartition(
              tableIdentifier.getDatabase(),
              tableIdentifier.getTableName(),
              partition));
      Partition newPartition = new Partition(oldPartition);
      newPartition.getSd().setLocation(newPath);
      hiveClient.run((ClientPool.Action<Void, HMSClient, TException>) client -> {
        try {
          client.alterPartition(tableIdentifier.getDatabase(),
              tableIdentifier.getTableName(),
              newPartition, null);
        } catch (ClassNotFoundException | NoSuchMethodException |
                 InvocationTargetException | IllegalAccessException e) {
          throw new RuntimeException(e);
        }
        return null;
      });
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  public static void createPartitionIfAbsent(
      HMSClientPool hmsClient,
      ArcticTable arcticTable,
      List<String> partitionValues,
      String partitionLocation,
      List<DataFile> dataFiles,
      int accessTimestamp) {
    String db = arcticTable.id().getDatabase();
    String tableName = arcticTable.id().getTableName();

    try {
      hmsClient.run(client -> {
        Partition partition;
        try {
          partition = client.getPartition(db, tableName, partitionValues);
          return partition;
        } catch (NoSuchObjectException noSuchObjectException) {
          Table hiveTable = client.getTable(db, tableName);
          partition = newPartition(hiveTable, partitionValues, partitionLocation,
              dataFiles, accessTimestamp);
          client.addPartition(partition);
          return partition;
        }
      });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void dropPartition(
      HMSClientPool hmsClient,
      ArcticTable arcticTable,
      Partition hivePartition) {
    try {
      hmsClient.run(client -> {
        PartitionDropOptions options = PartitionDropOptions.instance()
            .deleteData(false)
            .ifExists(true)
            .purgeData(false)
            .returnResults(false);
        return client.dropPartition(arcticTable.id().getDatabase(),
            arcticTable.id().getTableName(), hivePartition.getValues(), options);
      });
    } catch (TException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static void updatePartitionLocation(
      HMSClientPool hmsClient,
      ArcticTable arcticTable,
      Partition hivePartition,
      String newLocation,
      List<DataFile> dataFiles,
      int accessTimestamp) {
    dropPartition(hmsClient, arcticTable, hivePartition);
    createPartitionIfAbsent(hmsClient, arcticTable, hivePartition.getValues(), newLocation, dataFiles, accessTimestamp);
  }
}
