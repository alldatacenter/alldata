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

package com.netease.arctic.server.optimizing;

import com.netease.arctic.TableTestHelper;
import com.netease.arctic.server.table.BasicTableSnapshot;
import com.netease.arctic.server.table.KeyedTableSnapshot;
import com.netease.arctic.server.table.TableSnapshot;
import com.netease.arctic.server.utils.IcebergTableUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.util.StructLikeMap;

import java.util.List;

public class OptimizingTestHelpers {
  public static TableSnapshot getCurrentTableSnapshot(ArcticTable table) {
    if (table.isKeyedTable()) {
      return getCurrentKeyedTableSnapshot(table.asKeyedTable());
    } else {
      long baseSnapshotId = IcebergTableUtil.getSnapshotId(table.asUnkeyedTable(), true);
      return new BasicTableSnapshot(baseSnapshotId);
    }
  }

  public static KeyedTableSnapshot getCurrentKeyedTableSnapshot(KeyedTable keyedTable) {
    long baseSnapshotId = IcebergTableUtil.getSnapshotId(keyedTable.baseTable(), true);
    long changeSnapshotId = IcebergTableUtil.getSnapshotId(keyedTable.changeTable(), true);
    StructLikeMap<Long> partitionOptimizedSequence =
        TablePropertyUtil.getPartitionOptimizedSequence(keyedTable);
    StructLikeMap<Long> legacyPartitionMaxTransactionId =
        TablePropertyUtil.getLegacyPartitionMaxTransactionId(keyedTable);

    return new KeyedTableSnapshot(baseSnapshotId, changeSnapshotId,
        partitionOptimizedSequence, legacyPartitionMaxTransactionId);
  }

  public static List<Record> generateRecord(TableTestHelper tableTestHelper, int from, int to, String opTime) {
    List<Record> newRecords = Lists.newArrayList();
    for (int i = from; i <= to; i++) {
      newRecords.add(tableTestHelper.generateTestRecord(i, i + "", 0, opTime));
    }
    return newRecords;
  }

  public static List<DataFile> appendBase(ArcticTable arcticTable, List<DataFile> dataFiles) {
    AppendFiles appendFiles;
    if (arcticTable.isKeyedTable()) {
      appendFiles = arcticTable.asKeyedTable().baseTable().newAppend();
    } else {
      appendFiles = arcticTable.asUnkeyedTable().newAppend();
    }
    dataFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();
    return dataFiles;
  }

  public static List<DeleteFile> appendBasePosDelete(ArcticTable arcticTable, List<DeleteFile> deleteFiles) {
    RowDelta rowDelta;
    if (arcticTable.isKeyedTable()) {
      rowDelta = arcticTable.asKeyedTable().baseTable().newRowDelta();
    } else {
      rowDelta = arcticTable.asUnkeyedTable().newRowDelta();
    }
    deleteFiles.forEach(rowDelta::addDeletes);
    rowDelta.commit();
    return deleteFiles;
  }

  public static List<DataFile> appendChange(KeyedTable keyedTable, List<DataFile> dataFiles) {
    AppendFiles appendFiles = keyedTable.changeTable().newAppend();
    dataFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();
    return dataFiles;
  }
}
