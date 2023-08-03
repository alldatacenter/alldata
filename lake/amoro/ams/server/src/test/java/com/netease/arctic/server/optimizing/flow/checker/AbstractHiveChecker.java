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

package com.netease.arctic.server.optimizing.flow.checker;

import com.netease.arctic.hive.HMSClientPool;
import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.server.optimizing.UnKeyedTableCommit;
import com.netease.arctic.server.optimizing.flow.view.MatchResult;
import com.netease.arctic.server.optimizing.flow.view.TableDataView;
import com.netease.arctic.server.optimizing.plan.OptimizingPlanner;
import com.netease.arctic.server.optimizing.plan.TaskDescriptor;
import com.netease.arctic.table.ArcticTable;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.data.parquet.AdaptHiveGenericParquetReaders;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.parquet.AdaptHiveParquet;
import org.apache.iceberg.relocated.com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public abstract class AbstractHiveChecker extends OptimizingCountChecker {

  private int count;

  private TableDataView view;

  public AbstractHiveChecker(TableDataView view) {
    super(1);
    this.view = view;
  }

  @Override
  public void check(
      ArcticTable table,
      @Nullable List<TaskDescriptor> latestTaskDescriptors,
      OptimizingPlanner latestPlanner,
      @Nullable UnKeyedTableCommit latestCommit
  ) throws Exception {

    List<String> locations = dataLocations(table);
    List<Record> allRecordsInHive = readAllRecordsInHive(table, locations);

    MatchResult match = view.match(allRecordsInHive);
    if (!match.isOk()) {
      throw new RuntimeException("Hive data is error: " + match);
    }
  }

  private List<String> dataLocations(ArcticTable table) throws Exception {
    SupportHive supportHive = (SupportHive) table;
    HMSClientPool hmsClient = supportHive.getHMSClient();
    if (table.spec().isUnpartitioned()) {
      String location = hmsClient.run(client -> client.getTable(
          table.id().getDatabase(),
          table.id().getTableName()).getSd().getLocation());
      return Collections.singletonList(location);
    } else {
      List<Partition> list =
          hmsClient.run(client -> client.listPartitions(
              table.id().getDatabase(),
              table.id().getTableName(),
              (short) 100));
      return list.stream().map(Partition::getSd).map(StorageDescriptor::getLocation).collect(Collectors.toList());
    }
  }

  private List<Record> readAllRecordsInHive(ArcticTable table, List<String> locations)
      throws InterruptedException, ExecutionException {
    List<Path> allFiles = locations.stream()
        .flatMap(st -> {
          try {
            return Files.list(Paths.get(st.replace("file:", "")));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }).filter(path -> !Files.isDirectory(path))
        .filter(path -> !path.toString().endsWith("crc")).collect(Collectors.toList());
    List<CompletableFuture<List<Record>>> completableFutures = new ArrayList<>();
    for (Path path : allFiles) {
      completableFutures.add(
          CompletableFuture.supplyAsync(
              () -> {
                List<Record> recordsInHive = new ArrayList<>();
                try (CloseableIterable<Record> closeableIterable = newParquetIterable(path.toString(), table)) {
                  Iterables.addAll(recordsInHive, closeableIterable);
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
                return recordsInHive;
              }
          )
      );
    }

    List<Record> allRecordsInHive = new ArrayList<>();
    for (CompletableFuture<List<Record>> completableFuture : completableFutures) {
      allRecordsInHive.addAll(completableFuture.get());
    }
    return allRecordsInHive;
  }

  private CloseableIterable<Record> newParquetIterable(String path, ArcticTable table) {
    AdaptHiveParquet.ReadBuilder builder = AdaptHiveParquet.read(table.io().newInputFile(path))
        .project(table.schema())
        .createReaderFunc(fileSchema -> AdaptHiveGenericParquetReaders.buildReader(table.schema(),
            fileSchema, new HashMap<>()))
        .caseSensitive(false);
    return builder.build();
  }
}
