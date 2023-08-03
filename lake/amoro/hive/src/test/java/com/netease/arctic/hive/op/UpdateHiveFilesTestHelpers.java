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

package com.netease.arctic.hive.op;

import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.utils.HivePartitionUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.ArcticTableUtil;
import com.netease.arctic.utils.TableFileUtil;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.io.FileInfo;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Streams;
import org.apache.iceberg.util.StructLikeMap;
import org.apache.thrift.TException;
import org.junit.Assert;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UpdateHiveFilesTestHelpers {

  public static void validateHiveTableValues(
      HiveMetaStoreClient hiveClient, ArcticTable table,
      List<DataFile> exceptFiles) throws TException {
    if (table.spec().isPartitioned()) {
      UpdateHiveFilesTestHelpers.assertHivePartitionValues(hiveClient, table, exceptFiles);
    } else {
      UpdateHiveFilesTestHelpers.assertHiveTableValue(hiveClient, table, exceptFiles);
    }
  }

  public static void assertHivePartitionValues(
      HiveMetaStoreClient hiveClient, ArcticTable table,
      List<DataFile> files) throws TException {
    StructLikeMap<List<DataFile>> filesByPartition = groupFilesByPartition(table.spec(), files);
    StructLikeMap<String> pathByPartition = pathByPartition(table.spec(), filesByPartition);
    TableIdentifier identifier = table.id();
    final String database = identifier.getDatabase();
    final String tableName = identifier.getTableName();

    List<Partition> partitions = hiveClient.listPartitions(
        database,
        tableName,
        Short.MAX_VALUE);

    Assert.assertEquals(filesByPartition.size(), partitions.size());

    UnkeyedTable baseStore = ArcticTableUtil.baseStore(table);
    StructLikeMap<Map<String, String>> partitionProperties = baseStore.partitionProperty();
    for (Partition p : partitions) {
      StructLike partitionData = HivePartitionUtil.buildPartitionData(p.getValues(), table.spec());
      Assert.assertTrue(filesByPartition.containsKey(partitionData));
      String valuePath = p.getSd().getLocation();
      Assert.assertEquals(valuePath, pathByPartition.get(partitionData));

      Map<String, String> properties = partitionProperties.get(partitionData);
      Assert.assertEquals(
          valuePath,
          properties.get(HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION));
      Assert.assertEquals(
          p.getParameters().get("transient_lastDdlTime"),
          properties.get(HiveTableProperties.PARTITION_PROPERTIES_KEY_TRANSIENT_TIME));
      Assert.assertEquals(
          Streams.stream(table.io().asFileSystemIO().listDirectory(valuePath))
              .map(FileInfo::location)
              .collect(Collectors.toSet()),

          filesByPartition.get(partitionData).stream().map(DataFile::path)
              .map(CharSequence::toString).collect(Collectors.toSet()));
    }
  }

  public static void assertHiveTableValue(
      HiveMetaStoreClient hiveClient, ArcticTable table,
      List<DataFile> files) throws TException {
    TableIdentifier identifier = table.id();
    final String database = identifier.getDatabase();
    final String tableName = identifier.getTableName();
    Table hiveTable = hiveClient.getTable(database, tableName);

    String fileDir = dirOfFiles(files);
    if (fileDir != null) {
      Assert.assertEquals(hiveTable.getSd().getLocation(), fileDir);

      UnkeyedTable baseStore = ArcticTableUtil.baseStore(table);
      Map<String, String> properties = baseStore.partitionProperty().get(TablePropertyUtil.EMPTY_STRUCT);
      Assert.assertEquals(
          fileDir,
          properties.get(HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION));
      Assert.assertEquals(
          hiveTable.getParameters().get("transient_lastDdlTime"),
          properties.get(HiveTableProperties.PARTITION_PROPERTIES_KEY_TRANSIENT_TIME));
    }
  }

  private static StructLikeMap<List<DataFile>> groupFilesByPartition(
      PartitionSpec partitionSpec,
      List<DataFile> files) {
    StructLikeMap<List<DataFile>> filesByPartition = StructLikeMap.create(partitionSpec.partitionType());
    files.forEach(file -> {
      if (!filesByPartition.containsKey(file.partition())) {
        filesByPartition.put(file.partition(), Lists.newArrayList());
      }
      filesByPartition.get(file.partition()).add(file);
    });
    return filesByPartition;
  }

  private static StructLikeMap<String> pathByPartition(
      PartitionSpec partitionSpec,
      StructLikeMap<List<DataFile>> filesByPartition) {
    StructLikeMap<String> pathByPartition = StructLikeMap.create(partitionSpec.partitionType());
    filesByPartition.forEach((partition, files) -> {
      pathByPartition.put(partition, dirOfFiles(files));
    });
    return pathByPartition;
  }

  private static String dirOfFiles(List<DataFile> files) {
    String fileDir = null;
    for (DataFile file : files) {
      if (fileDir == null) {
        fileDir = TableFileUtil.getFileDir(file.path().toString());
      } else {
        Assert.assertEquals(fileDir, TableFileUtil.getFileDir(file.path().toString()));
      }
    }
    return fileDir;
  }
}
