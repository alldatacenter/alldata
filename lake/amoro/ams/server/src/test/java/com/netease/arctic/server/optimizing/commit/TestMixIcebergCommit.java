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

package com.netease.arctic.server.optimizing.commit;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.DefaultKeyedFile;
import com.netease.arctic.data.IcebergContentFile;
import com.netease.arctic.data.IcebergDataFile;
import com.netease.arctic.optimizing.RewriteFilesInput;
import com.netease.arctic.optimizing.RewriteFilesOutput;
import com.netease.arctic.scan.ArcticFileScanTask;
import com.netease.arctic.scan.CombinedScanTask;
import com.netease.arctic.scan.KeyedTableScanTask;
import com.netease.arctic.server.exception.OptimizingCommitException;
import com.netease.arctic.server.optimizing.KeyedTableCommit;
import com.netease.arctic.server.optimizing.TaskRuntime;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DataFiles;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.util.StructLikeMap;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RunWith(Parameterized.class)
public class TestMixIcebergCommit extends TestUnKeyedTableCommit {

  public TestMixIcebergCommit(CatalogTestHelper catalogTestHelper, TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Parameterized.Parameters(name = "commit_test")
  public static Object[] parameters() {
    return new Object[][] {{new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
                            new BasicTableTestHelper(true, true)
                           },
                           {
                               new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
                               new BasicTableTestHelper(true, false)
                           }};
  }

  @Before
  public void initTableFile() {
    arcticTable = getArcticTable();
    spec = arcticTable.spec();
    partitionData = GenericRecord.create(spec.schema());
    partitionData.set(0, 1);
    partitionPath = spec.partitionToPath(partitionData);
  }

  protected void addFile(DataFile dataFile) {
    arcticTable.asKeyedTable().changeTable().newAppend()
        .appendFile(dataFile)
        .commit();
  }

  protected Map<String, ContentFile<?>> getAllFiles() {
    Map<String, ContentFile<?>> maps = new HashMap<>();
    CloseableIterable<CombinedScanTask> combinedScanTasks = arcticTable.asKeyedTable().newScan().planTasks();
    for (CombinedScanTask combinedScanTask : combinedScanTasks) {
      for (KeyedTableScanTask keyedTableScanTask : combinedScanTask.tasks()) {
        for (ArcticFileScanTask task : keyedTableScanTask.dataTasks()) {
          maps.put(task.file().path().toString(), task.file());
          for (DeleteFile deleteFile : task.deletes()) {
            maps.put(deleteFile.path().toString(), deleteFile);
          }
        }
        for (ArcticFileScanTask task : keyedTableScanTask.arcticEquityDeletes()) {
          maps.put(task.file().path().toString(), task.file());
        }
      }
    }
    return maps;
  }

  protected DataFile getEqualityDeleteFile() {
    return DataFiles.builder(spec)
        .withPath(String.format("1-ED-0-00000-0-00-%s.parquet", fileSeq++))
        .withFileSizeInBytes(10)
        .withPartitionPath(partitionPath)
        .withRecordCount(1)
        .withFormat(FileFormat.PARQUET)
        .build();
  }

  protected void addDelete(ContentFile<?> contentFile) {
    addFile((DataFile) contentFile);
  }

  protected void execute(
      DataFile[] rewriteData,
      DataFile[] rewritePos,
      ContentFile<?>[] deletes,
      DataFile[] dataOutput,
      DeleteFile[] deleteOutput
  ) throws OptimizingCommitException {
    RewriteFilesInput input = getRewriteInput(
        rewriteData,
        rewritePos,
        deletes
    );
    RewriteFilesOutput output = new RewriteFilesOutput(
        dataOutput,
        deleteOutput,
        null);
    StructLikeMap<Long> fromSequence = getFromSequenceOfPartitions(input);
    StructLikeMap<Long> toSequence = getToSequenceOfPartitions(input);
    TaskRuntime taskRuntime = Mockito.mock(TaskRuntime.class);
    Mockito.when(taskRuntime.getPartition()).thenReturn(partitionPath);
    Mockito.when(taskRuntime.getInput()).thenReturn(input);
    Mockito.when(taskRuntime.getOutput()).thenReturn(output);
    KeyedTableCommit commit = new KeyedTableCommit(
        getArcticTable(),
        Arrays.asList(taskRuntime),
        Optional.ofNullable(arcticTable.asKeyedTable().baseTable().currentSnapshot()).map(Snapshot::snapshotId)
            .orElse(null),
        fromSequence,
        toSequence);
    commit.commit();
  }

  private StructLikeMap<Long> getFromSequenceOfPartitions(RewriteFilesInput input) {
    long minSequence = Long.MAX_VALUE;
    for (IcebergContentFile<?> contentFile : input.allFiles()) {
      if (contentFile.isDeleteFile()) {
        continue;
      }
      DataFileType type = ((DefaultKeyedFile) (contentFile.asDataFile().internalDataFile())).type();
      if (type == DataFileType.INSERT_FILE || type == DataFileType.EQ_DELETE_FILE) {
        minSequence = Math.min(minSequence, contentFile.getSequenceNumber());
      }
    }
    StructLikeMap<Long> structLikeMap = StructLikeMap.create(spec.partitionType());
    if (minSequence != Long.MAX_VALUE) {
      structLikeMap.put(partitionData, minSequence);
    }
    return structLikeMap;
  }

  private StructLikeMap<Long> getToSequenceOfPartitions(RewriteFilesInput input) {
    long minSequence = -1;
    for (IcebergContentFile<?> contentFile : input.allFiles()) {
      if (contentFile.isDeleteFile()) {
        continue;
      }
      DataFileType type = ((DefaultKeyedFile) (contentFile.asDataFile().internalDataFile())).type();
      if (type == DataFileType.INSERT_FILE || type == DataFileType.EQ_DELETE_FILE) {
        minSequence = Math.max(minSequence, contentFile.getSequenceNumber());
      }
    }
    StructLikeMap<Long> structLikeMap = StructLikeMap.create(spec.partitionType());
    if (minSequence != -1) {
      structLikeMap.put(partitionData, minSequence);
    }
    return structLikeMap;
  }

  private RewriteFilesInput getRewriteInput(
      DataFile[] rewriteDataFiles, DataFile[] rePositionDataFiles,
      ContentFile<?>[] deleteFiles) {
    Map<String, ContentFile<?>> allFiles = getAllFiles();

    IcebergDataFile[] rewriteData = null;
    if (rewriteDataFiles != null) {
      rewriteData = Arrays.stream(rewriteDataFiles)
          .map(s -> (DefaultKeyedFile) allFiles.get(s.path().toString()))
          .map(s -> IcebergContentFile.wrap(s, s.transactionId()).asDataFile())
          .toArray(IcebergDataFile[]::new);
    }

    IcebergDataFile[] rewritePos = null;
    if (rePositionDataFiles != null) {
      rewritePos = Arrays.stream(rePositionDataFiles)
          .map(s -> (DefaultKeyedFile) allFiles.get(s.path().toString()))
          .map(s -> IcebergContentFile.wrap(s, s.transactionId()).asDataFile())
          .toArray(IcebergDataFile[]::new);
    }

    IcebergContentFile<?>[] delete = null;
    if (deleteFiles != null) {
      delete = Arrays.stream(deleteFiles)
          .map(s -> allFiles.get(s.path().toString()))
          .map(s -> {
                if (s instanceof DefaultKeyedFile) {
                  return IcebergContentFile.wrap(s, ((DefaultKeyedFile) s).transactionId());
                } else {
                  return IcebergContentFile.wrap(s, 0);
                }
          }
          )
          .toArray(IcebergContentFile[]::new);
    }
    return new RewriteFilesInput(rewriteData, rewritePos, null, delete, arcticTable);
  }
}
