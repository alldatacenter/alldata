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
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.data.IcebergContentFile;
import com.netease.arctic.data.IcebergDataFile;
import com.netease.arctic.data.IcebergDeleteFile;
import com.netease.arctic.optimizing.RewriteFilesInput;
import com.netease.arctic.optimizing.RewriteFilesOutput;
import com.netease.arctic.server.exception.OptimizingCommitException;
import com.netease.arctic.server.optimizing.TaskRuntime;
import com.netease.arctic.server.optimizing.UnKeyedTableCommit;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.utils.SequenceNumberFetcher;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DataFiles;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.FileMetadata;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.io.CloseableIterable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RunWith(Parameterized.class)
public class TestUnKeyedTableCommit extends TableTestBase {
  protected int fileSeq;
  protected PartitionSpec spec;
  protected StructLike partitionData;
  protected String partitionPath;

  protected ArcticTable arcticTable;

  public TestUnKeyedTableCommit(CatalogTestHelper catalogTestHelper, TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Parameterized.Parameters(name = "commit_test")
  public static Object[] parameters() {
    return new Object[][] {{
                               new BasicCatalogTestHelper(TableFormat.ICEBERG),
                               new BasicTableTestHelper(false, true)
                           },
                           {
                               new BasicCatalogTestHelper(TableFormat.ICEBERG),
                               new BasicTableTestHelper(false, false)
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

  @Test
  public void test() throws OptimizingCommitException {

    //change:                     change:
    //  changeDataFile1     =>       null
    //base:                       base:
    //   null                        baseDataFile1
    DataFile changeDataFile1 = getChangeDataFile();
    addFile(changeDataFile1);
    DataFile baseDataFile1 = getBaseDataFile();
    execute(
        new DataFile[] {changeDataFile1},
        null,
        null,
        new DataFile[] {baseDataFile1},
        null);
    checkFile(new ContentFile[] {baseDataFile1});

    //change:                     change:
    //  changeEquFile2     =>       null
    //base:                       base:
    //   baseDataFile1               baseDataFile2
    ContentFile<?> changeEquFile2 = getEqualityDeleteFile();
    addDelete(changeEquFile2);
    DataFile baseDataFile2 = getBaseDataFile();
    execute(
        new DataFile[] {baseDataFile1},
        null,
        new ContentFile[] {changeEquFile2},
        new DataFile[] {baseDataFile2},
        null);
    checkFile(new ContentFile[] {baseDataFile2});

    //change:                     change:
    //  changeEquFile3     =>       null
    //  changeDataFile3
    //base:                       base:
    //   baseDataFile2               baseDataFile3
    ContentFile<?> changeEquFile3 = getEqualityDeleteFile();
    DataFile changeDataFile3 = getChangeDataFile();
    addFile(changeDataFile3);
    addDelete(changeEquFile3);
    DataFile baseDataFile3 = getBaseDataFile();
    execute(
        new DataFile[] {changeDataFile3, baseDataFile2},
        null,
        new ContentFile[] {changeEquFile3},
        new DataFile[] {baseDataFile3},
        null);
    checkFile(new ContentFile[] {baseDataFile3});

    //change:                     change:
    //  changeEquFile4     =>       null
    //base:                       base:
    //   baseDataFile3               baseDataFile3
    //                               basePosFile4
    ContentFile<?> changeEquFile4 = getEqualityDeleteFile();
    addDelete(changeEquFile4);
    DeleteFile basePosFile4 = getPositionDeleteFile();
    execute(
        null,
        new DataFile[] {baseDataFile3},
        new ContentFile[] {changeEquFile4},
        null,
        new DeleteFile[] {basePosFile4});
    checkFile(new ContentFile[] {baseDataFile3, basePosFile4});

    //change:                     change:
    //  changeEquFile5     =>       null
    //  changeDataFile5
    //base:                       base:
    //   baseDataFile3               baseDataFile3
    //   basePosFile4                basePosFile5
    //                               baseDataFile5
    ContentFile<?> changeEquFile5 = getEqualityDeleteFile();
    DataFile changeDataFile5 = getChangeDataFile();
    addFile(changeDataFile5);
    addDelete(changeEquFile5);
    DataFile baseDataFile5 = getBaseDataFile();
    DeleteFile basePosFile5 = getPositionDeleteFile();
    execute(
        new DataFile[] {changeDataFile5},
        new DataFile[] {baseDataFile3},
        new ContentFile[] {changeEquFile5, basePosFile4},
        new DataFile[] {baseDataFile5},
        new DeleteFile[] {basePosFile5});
    checkFile(new ContentFile[] {baseDataFile3, basePosFile5, baseDataFile5});

    //change:                     change:
    //  null               =>       null
    //base:                       base:
    //   baseDataFile3              baseDataFile6
    //   basePosFile5
    //   baseDataFile5
    DataFile baseDataFile6 = getBaseDataFile();
    execute(
        new DataFile[] {baseDataFile3, baseDataFile5},
        null,
        new ContentFile[] {basePosFile5},
        new DataFile[] {baseDataFile6},
        null);
    checkFile(new ContentFile[] {baseDataFile6});

    //change:                     change:
    //  changeEquFile6     =>       null
    //base:                       base:
    //   baseDataFile6              null
    ContentFile<?> changeEquFile6 = getEqualityDeleteFile();
    addDelete(changeEquFile6);
    execute(
        new DataFile[] {baseDataFile6},
        null,
        new ContentFile[] {changeEquFile6},
        null,
        null);
    checkFile(new ContentFile[0]);

    //change:                     change:
    //  changeDataFile7     =>       null
    //base:                       base:
    //   null                        baseDataFile7
    DataFile changeDataFile7 = getChangeDataFile();
    addFile(changeDataFile7);
    DataFile baseDataFile7 = getBaseDataFile();
    execute(
        new DataFile[] {changeDataFile7},
        null,
        null,
        new DataFile[] {baseDataFile7},
        null);
    checkFile(new ContentFile[] {baseDataFile7});

    //change:                     change:
    //  changeEquFile8     =>       null
    //base:                       base:
    //   baseDataFile7               baseDataFile7
    //                               basePosFile8
    ContentFile<?> changeEquFile8 = getEqualityDeleteFile();
    addDelete(changeEquFile8);
    DeleteFile basePosFile8 = getPositionDeleteFile();
    execute(
        null,
        new DataFile[] {baseDataFile7},
        new ContentFile[] {changeEquFile8},
        null,
        new DeleteFile[] {basePosFile8});
    checkFile(new ContentFile[] {baseDataFile7, basePosFile8});

    //change:                     change:
    //  null               =>       null
    //base:                       base:
    //   baseDataFile7               baseDataFile9
    //   basePosFile8      =>        basePosFile8
    // DataFile baseDataFile9 = getBaseDataFile();
    // execute(
    //     new DataFile[] {baseDataFile7},
    //     null,
    //     new ContentFile[] {basePosFile8},
    //     new DataFile[] {baseDataFile9},
    //     null);
    // checkFile(new ContentFile[] {baseDataFile9, basePosFile8});
  }

  protected void addFile(DataFile dataFile) {
    arcticTable.asUnkeyedTable().newAppend()
        .appendFile(dataFile)
        .commit();
  }

  protected void addDelete(ContentFile<?> contentFile) {
    arcticTable.asUnkeyedTable().newRowDelta().addDeletes((DeleteFile) contentFile).commit();
  }

  protected Map<String, ContentFile<?>> getAllFiles() {
    if (arcticTable.asUnkeyedTable().currentSnapshot() == null) {
      return Collections.emptyMap();
    }
    Map<String, ContentFile<?>> maps = new HashMap<>();
    CloseableIterable<FileScanTask> fileScanTasks = arcticTable.asUnkeyedTable().newScan().planFiles();
    SequenceNumberFetcher sequenceNumberFetcher = new SequenceNumberFetcher(
        arcticTable.asUnkeyedTable(),
        arcticTable.asUnkeyedTable().currentSnapshot().snapshotId());
    for (FileScanTask fileScanTask : fileScanTasks) {
      maps.put(fileScanTask.file().path().toString(), new IcebergDataFile(
          fileScanTask.file(),
          sequenceNumberFetcher.sequenceNumberOf(fileScanTask.file().path().toString())));
      for (DeleteFile deleteFile : fileScanTask.deletes()) {
        maps.put(deleteFile.path().toString(), new IcebergDeleteFile(
            deleteFile,
            sequenceNumberFetcher.sequenceNumberOf(deleteFile.path().toString())));
      }
    }
    return maps;
  }

  protected ContentFile<?> getEqualityDeleteFile() {
    return FileMetadata.deleteFileBuilder(spec)
        .withPath(String.format("1-ED-0-00000-0-00-%s.parquet", fileSeq++))
        .ofPositionDeletes()
        .withFileSizeInBytes(10)
        .withPartitionPath(partitionPath)
        .withRecordCount(1)
        .withFormat(FileFormat.PARQUET)
        .build();
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

    TaskRuntime taskRuntime = Mockito.mock(TaskRuntime.class);
    Mockito.when(taskRuntime.getPartition()).thenReturn(partitionPath);
    Mockito.when(taskRuntime.getInput()).thenReturn(input);
    Mockito.when(taskRuntime.getOutput()).thenReturn(output);
    UnKeyedTableCommit commit = new UnKeyedTableCommit(
        Optional.ofNullable(arcticTable.asUnkeyedTable().currentSnapshot()).map(Snapshot::snapshotId)
            .orElse(null),
        getArcticTable(),
        Arrays.asList(taskRuntime));
    commit.commit();
  }

  private RewriteFilesInput getRewriteInput(
      DataFile[] rewriteDataFiles, DataFile[] rePositionDataFiles,
      ContentFile<?>[] deleteFiles) {
    Map<String, ContentFile<?>> allFiles = getAllFiles();

    IcebergDataFile[] rewriteData = null;
    if (rewriteDataFiles != null) {
      rewriteData = Arrays.stream(rewriteDataFiles)
          .map(s -> allFiles.get(s.path().toString()))
          .toArray(IcebergDataFile[]::new);
    }

    IcebergDataFile[] rewritePos = null;
    if (rePositionDataFiles != null) {
      rewritePos = Arrays.stream(rePositionDataFiles)
          .map(s -> allFiles.get(s.path().toString()))
          .toArray(IcebergDataFile[]::new);
    }

    IcebergContentFile<?>[] delete = null;
    if (deleteFiles != null) {
      delete = Arrays.stream(deleteFiles)
          .map(s -> allFiles.get(s.path().toString()))
          .toArray(IcebergContentFile[]::new);
    }
    return new RewriteFilesInput(rewriteData, rewritePos, null, delete, arcticTable);
  }

  private void checkFile(ContentFile<?>[] files) {
    arcticTable.refresh();
    Map<String, ContentFile<?>> maps = getAllFiles();
    Assert.assertEquals(files.length, maps.size());
    for (ContentFile file : files) {
      Assert.assertNotNull(maps.get(file.path().toString()));
    }
  }

  private DataFile getChangeDataFile() {
    return DataFiles.builder(spec)
        .withPath(String.format("1-I-0-00000-0-00-%s.parquet", fileSeq++))
        .withFileSizeInBytes(10)
        .withPartitionPath(partitionPath)
        .withRecordCount(1)
        .withFormat(FileFormat.PARQUET)
        .build();
  }

  private DataFile getBaseDataFile() {
    return DataFiles.builder(spec)
        .withPath(String.format("1-B-0-00000-0-00-%s.parquet", fileSeq++))
        .withFileSizeInBytes(10)
        .withPartitionPath(partitionPath)
        .withRecordCount(1)
        .withFormat(FileFormat.PARQUET)
        .build();
  }

  private DeleteFile getPositionDeleteFile() {
    return FileMetadata.deleteFileBuilder(spec)
        .withPath(String.format("1-PD-0-00000-0-00-%s.parquet", fileSeq++))
        .ofPositionDeletes()
        .withFileSizeInBytes(10)
        .withPartitionPath(partitionPath)
        .withRecordCount(1)
        .withFormat(FileFormat.PARQUET)
        .build();
  }
}
