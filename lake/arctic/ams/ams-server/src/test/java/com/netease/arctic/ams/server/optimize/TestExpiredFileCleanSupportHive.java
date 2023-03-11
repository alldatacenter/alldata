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

package com.netease.arctic.ams.server.optimize;

import com.netease.arctic.ams.server.service.impl.TableExpireService;
import com.netease.arctic.hive.io.writer.AdaptHiveGenericTaskWriterBuilder;
import com.netease.arctic.hive.table.HiveLocationKind;
import com.netease.arctic.hive.utils.HiveTableUtil;
import com.netease.arctic.hive.utils.TableTypeUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.TableFileUtils;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFiles;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.relocated.com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class TestExpiredFileCleanSupportHive extends TestSupportHiveBase {
  @Test
  public void testExpireTableFiles() throws Exception {
    List<DataFile> hiveFiles = insertHiveDataFiles(testUnPartitionKeyedHiveTable, 1);
    List<DataFile> s2Files = insertTableBaseDataFiles(testUnPartitionKeyedHiveTable).second();

    DeleteFiles deleteHiveFiles = testUnPartitionKeyedHiveTable.baseTable().newDelete();
    for (DataFile hiveFile : hiveFiles) {
      Assert.assertTrue(testUnPartitionKeyedHiveTable.io().exists(hiveFile.path().toString()));
      deleteHiveFiles.deleteFile(hiveFile);
    }
    deleteHiveFiles.commit();

    DeleteFiles deleteIcebergFiles = testUnPartitionKeyedHiveTable.baseTable().newDelete();
    for (DataFile s2File : s2Files) {
      Assert.assertTrue(testUnPartitionKeyedHiveTable.io().exists(s2File.path().toString()));
      deleteIcebergFiles.deleteFile(s2File);
    }
    deleteIcebergFiles.commit();

    List<DataFile> s3Files = insertTableBaseDataFiles(testUnPartitionKeyedHiveTable).second();
    for (DataFile s3File : s3Files) {
      Assert.assertTrue(testUnPartitionKeyedHiveTable.io().exists(s3File.path().toString()));
    }

    Set<String> hiveLocation = new HashSet<>();
    if (TableTypeUtil.isHive(testUnPartitionKeyedHiveTable)) {
      hiveLocation.add(TableFileUtils.getUriPath(TableFileUtils.getFileDir(hiveFiles.get(0).path().toString())));
    }
    TableExpireService.expireSnapshots(testUnPartitionKeyedHiveTable.baseTable(), System.currentTimeMillis(), hiveLocation);
    Assert.assertEquals(1, Iterables.size(testUnPartitionKeyedHiveTable.baseTable().snapshots()));

    for (DataFile hiveFile : hiveFiles) {
      Assert.assertTrue(testUnPartitionKeyedHiveTable.io().exists(hiveFile.path().toString()));
    }
    for (DataFile s2File : s2Files) {
      Assert.assertFalse(testUnPartitionKeyedHiveTable.io().exists(s2File.path().toString()));
    }
    for (DataFile s3File : s3Files) {
      Assert.assertTrue(testUnPartitionKeyedHiveTable.io().exists(s3File.path().toString()));
    }
  }

  private List<DataFile> insertHiveDataFiles(ArcticTable arcticTable, long transactionId) throws Exception {

    String hiveSubDir = HiveTableUtil.newHiveSubdirectory(transactionId);
    AtomicInteger taskId = new AtomicInteger();

    Supplier<TaskWriter<Record>> taskWriterSupplier = () -> AdaptHiveGenericTaskWriterBuilder.builderFor(arcticTable)
        .withTransactionId(transactionId)
        .withTaskId(taskId.incrementAndGet())
        .withCustomHiveSubdirectory(hiveSubDir)
        .buildWriter(HiveLocationKind.INSTANT);
    List<DataFile> dataFiles = insertBaseDataFiles(taskWriterSupplier, arcticTable.schema());
    UnkeyedTable baseTable = arcticTable.isKeyedTable() ?
        arcticTable.asKeyedTable().baseTable() : arcticTable.asUnkeyedTable();
    AppendFiles baseAppend = baseTable.newAppend();
    dataFiles.forEach(baseAppend::appendFile);
    baseAppend.commit();

    return dataFiles;
  }
}
