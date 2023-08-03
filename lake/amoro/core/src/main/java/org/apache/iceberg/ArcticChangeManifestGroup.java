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

package org.apache.iceberg;

import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.metrics.ScanMetrics;

import java.util.List;
import java.util.Map;

public class ArcticChangeManifestGroup extends ManifestGroup {

  ArcticChangeManifestGroup(FileIO io, Iterable<ManifestFile> dataManifests, Iterable<ManifestFile> deleteManifests) {
    super(io, dataManifests, deleteManifests);
  }

  @Override
  ArcticChangeManifestGroup caseSensitive(boolean newCaseSensitive) {
    super.caseSensitive(newCaseSensitive);
    return this;
  }

  @Override
  ArcticChangeManifestGroup select(List<String> newColumns) {
    super.select(newColumns);
    return this;
  }

  @Override
  ArcticChangeManifestGroup filterData(Expression newDataFilter) {
    super.filterData(newDataFilter);
    return this;
  }

  @Override
  ArcticChangeManifestGroup specsById(Map<Integer, PartitionSpec> newSpecsById) {
    super.specsById(newSpecsById);
    return this;
  }

  @Override
  ArcticChangeManifestGroup scanMetrics(ScanMetrics metrics) {
    super.scanMetrics(metrics);
    return this;
  }

  @Override
  ArcticChangeManifestGroup ignoreDeleted() {
    super.ignoreDeleted();
    return this;
  }


  public CloseableIterable<ChangeFileScanTask> planFilesWithSequence() {
    return plan(ArcticChangeManifestGroup::createContentFileWithSequence);
  }

  private static CloseableIterable<ChangeFileScanTask> createContentFileWithSequence(
      CloseableIterable<ManifestEntry<DataFile>> entries, TaskContext ctx) {
    return CloseableIterable.transform(
        entries,
        entry -> {
          DataFile dataFile = entry.file().copy(ctx.shouldKeepStats());
          return new ChangeFileScanTask(dataFile, entry.dataSequenceNumber());
        });
  }


  public static class ChangeFileScanTask implements ScanTask {

    private final DataFile file;
    private final long dataSequenceNumber;

    public ChangeFileScanTask(DataFile file, long seq) {
      this.file = file;
      this.dataSequenceNumber = seq;
    }

    public DataFile file() {
      return this.file;
    }

    public long getDataSequenceNumber() {
      return dataSequenceNumber;
    }
  }
}
