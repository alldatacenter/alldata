/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.optimizing.flow;

import com.netease.arctic.hive.io.reader.AdaptHiveGenericArcticDataReader;
import com.netease.arctic.hive.io.reader.GenericAdaptHiveIcebergDataReader;
import com.netease.arctic.scan.CombinedScanTask;
import com.netease.arctic.scan.KeyedTableScanTask;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.data.IdentityPartitionConverters;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.CloseableIterator;
import org.apache.iceberg.relocated.com.google.common.collect.Iterables;
import org.apache.iceberg.relocated.com.google.common.collect.Iterators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DataReader {
  private ArcticTable table;

  public DataReader(ArcticTable table) {
    this.table = table;
  }

  public List<Record> allData() throws Exception {
    if (table.isKeyedTable()) {
      return readKeyed(table.asKeyedTable());
    } else {
      return readIceberg(table.asUnkeyedTable());
    }
  }

  private List<Record> readKeyed(KeyedTable table) throws IOException, ExecutionException, InterruptedException {
    CloseableIterable<CombinedScanTask> combinedScanTasks = table.newScan().planTasks();
    AdaptHiveGenericArcticDataReader dataReader = new AdaptHiveGenericArcticDataReader(
        table.io(),
        table.schema(),
        table.schema(),
        table.primaryKeySpec(),
        null,
        false,
        IdentityPartitionConverters::convertConstant,
        null,
        false
    );
    List<CompletableFuture<List<Record>>> completableFutures = new ArrayList<>();
    for (CombinedScanTask combinedScanTask : combinedScanTasks) {
      for (KeyedTableScanTask scanTask : combinedScanTask.tasks()) {
        completableFutures.add(CompletableFuture.supplyAsync(() -> {
          return table.io().doAs(() -> {
            CloseableIterator<Record> closeableIterator = dataReader.readData(scanTask);
            List<Record> list = new ArrayList<>();
            Iterators.addAll(list, closeableIterator);
            try {
              closeableIterator.close();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
            return list;
          });
        }));
      }
    }

    List<Record> list = new ArrayList<>();
    for (CompletableFuture<List<Record>> completableFuture : completableFutures) {
      list.addAll(completableFuture.get());
    }
    return list;
  }

  private List<Record> readIceberg(UnkeyedTable table) throws ExecutionException, InterruptedException {
    GenericAdaptHiveIcebergDataReader dataReader = new GenericAdaptHiveIcebergDataReader(
        table.io(),
        table.schema(),
        table.schema(),
        null,
        false,
        IdentityPartitionConverters::convertConstant,
        false
    );
    CloseableIterable<FileScanTask> fileScanTasks = table.newScan().planFiles();

    List<CompletableFuture<List<Record>>> completableFutures = new ArrayList<>();
    for (FileScanTask fileScanTask : fileScanTasks) {
      completableFutures.add(CompletableFuture.supplyAsync(() -> {
        CloseableIterable<Record> closeableIterable = dataReader.readData(fileScanTask);
        List<Record> list = new ArrayList<>();
        Iterables.addAll(list, closeableIterable);
        try {
          closeableIterable.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        return list;
      }));
    }

    List<Record> list = new ArrayList<>();
    for (CompletableFuture<List<Record>> completableFuture : completableFutures) {
      List<Record> records = completableFuture.get();
      list.addAll(records);
    }
    return list;
  }
}
