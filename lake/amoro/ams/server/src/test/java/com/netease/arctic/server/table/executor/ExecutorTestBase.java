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

package com.netease.arctic.server.table.executor;

import com.netease.arctic.TableTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.hive.io.HiveDataTestHelpers;
import com.netease.arctic.hive.utils.HiveTableUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.ManifestFile;
import org.apache.iceberg.ReachableFileUtil;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.junit.Assert;

import java.time.LocalDateTime;
import java.util.List;

public class ExecutorTestBase extends TableTestBase {

  public ExecutorTestBase(CatalogTestHelper catalogTestHelper, TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  public List<Record> createRecords(int start, int length) {
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    for (int i = start; i < start + length; i++) {
      builder.add(tableTestHelper().generateTestRecord(
          i, "name" + i, 0L,
          LocalDateTime.of(2022, 1, i % 2 + 1, 12, 0, 0).toString()));
    }
    return builder.build();
  }

  public List<DataFile> writeAndCommitBaseStore(ArcticTable table) {
    UnkeyedTable baseTable = table.isKeyedTable() ? table.asKeyedTable().baseTable() : table.asUnkeyedTable();
    // write 4 file,100 records to 2 partitions(2022-1-1\2022-1-2)
    List<DataFile> dataFiles =
        tableTestHelper().writeBaseStore(baseTable, 0, createRecords(1, 100), false);
    AppendFiles appendFiles = baseTable.newAppend();
    dataFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();
    return dataFiles;
  }

  public List<DataFile> writeAndCommitChangeStore(
      KeyedTable table, long txId, ChangeAction action, List<Record> records) {
    List<DataFile> writeFiles = tableTestHelper().writeChangeStore(table, txId, action, records, false);
    AppendFiles appendFiles = table.changeTable().newAppend();
    writeFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();
    return writeFiles;
  }


  public List<DataFile> writeAndCommitBaseAndHive(
      ArcticTable table, long txId, boolean writeHive) {
    String hiveSubDir = HiveTableUtil.newHiveSubdirectory(txId);
    List<DataFile> dataFiles = HiveDataTestHelpers.writeBaseStore(
        table, txId, createRecords(1, 100), false, writeHive, hiveSubDir);
    UnkeyedTable baseTable = table.isKeyedTable() ?
        table.asKeyedTable().baseTable() : table.asUnkeyedTable();
    AppendFiles baseAppend = baseTable.newAppend();
    dataFiles.forEach(baseAppend::appendFile);
    baseAppend.commit();
    return dataFiles;
  }

  public void writeAndCommitBaseAndChange(ArcticTable table) {
    writeAndCommitBaseStore(table);
    writeAndCommitChangeStore(
        table.asKeyedTable(), 2, ChangeAction.INSERT, createRecords(101, 100));
  }

  public static void assertMetadataExists(ArcticTable table) {
    if (table.isKeyedTable()) {
      checkMetadataExistence(table.asKeyedTable().changeTable());
    }
    UnkeyedTable baseTable = table.isKeyedTable() ? table.asKeyedTable().baseTable() : table.asUnkeyedTable();
    checkMetadataExistence(baseTable);
  }

  public static void checkMetadataExistence(UnkeyedTable table) {
    for (Snapshot snapshot : table.snapshots()) {
      Assert.assertTrue(table.io().exists(snapshot.manifestListLocation()));
      for (ManifestFile allManifest : snapshot.allManifests(table.io())) {
        Assert.assertTrue(table.io().exists(allManifest.path()));
      }
    }
    for (String metadataFile : ReachableFileUtil.metadataFileLocations(table, false)) {
      Assert.assertTrue(table.io().exists(metadataFile));
    }
    Assert.assertTrue(table.io().exists(ReachableFileUtil.versionHintLocation(table)));
  }

}
