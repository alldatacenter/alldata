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

package com.netease.arctic.server.optimizing.scan;

import com.netease.arctic.data.IcebergContentFile;
import com.netease.arctic.data.IcebergDataFile;
import com.netease.arctic.data.IcebergDeleteFile;
import com.netease.arctic.server.ArcticServiceConstants;
import com.netease.arctic.utils.SequenceNumberFetcher;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Table;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

public class IcebergTableFileScanHelper implements TableFileScanHelper {
  private static final Logger LOG = LoggerFactory.getLogger(IcebergTableFileScanHelper.class);
  private final Table table;
  private final SequenceNumberFetcher sequenceNumberFetcher;
  private PartitionFilter partitionFilter;
  private final long snapshotId;

  public IcebergTableFileScanHelper(Table table, long snapshotId) {
    this.table = table;
    this.sequenceNumberFetcher = new SequenceNumberFetcher(table, snapshotId);
    this.snapshotId = snapshotId;
  }

  @Override
  public List<FileScanResult> scan() {
    List<FileScanResult> results = Lists.newArrayList();
    LOG.info("{} start scan files with snapshotId = {}", table.name(), snapshotId);
    if (snapshotId == ArcticServiceConstants.INVALID_SNAPSHOT_ID) {
      return results;
    }
    long startTime = System.currentTimeMillis();
    PartitionSpec partitionSpec = table.spec();
    try (CloseableIterable<FileScanTask> filesIterable =
             table.newScan().useSnapshot(snapshotId).planFiles()) {
      for (FileScanTask task : filesIterable) {
        if (partitionFilter != null) {
          StructLike partition = task.file().partition();
          String partitionPath = partitionSpec.partitionToPath(partition);
          if (!partitionFilter.test(partitionPath)) {
            continue;
          }
        }
        IcebergDataFile dataFile = createDataFile(task.file());
        List<IcebergContentFile<?>> deleteFiles =
            task.deletes().stream().map(this::createDeleteFile).collect(Collectors.toList());
        results.add(new FileScanResult(dataFile, deleteFiles));
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to close table scan of " + table.name(), e);
    }
    long endTime = System.currentTimeMillis();
    LOG.info("{} finish scan files, cost {} ms, get {} files", table.name(), endTime - startTime, results.size());
    return results;
  }

  @Override
  public TableFileScanHelper withPartitionFilter(PartitionFilter partitionFilter) {
    this.partitionFilter = partitionFilter;
    return this;
  }

  private IcebergDataFile createDataFile(DataFile dataFile) {
    return new IcebergDataFile(dataFile, sequenceNumberFetcher.sequenceNumberOf(dataFile.path().toString()));
  }

  private IcebergDeleteFile createDeleteFile(DeleteFile deleteFile) {
    return new IcebergDeleteFile(deleteFile, sequenceNumberFetcher.sequenceNumberOf(deleteFile.path().toString()));
  }
}
