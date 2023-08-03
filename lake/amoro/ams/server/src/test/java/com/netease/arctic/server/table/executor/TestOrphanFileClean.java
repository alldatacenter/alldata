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

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.TableFileUtil;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.io.OutputFile;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.netease.arctic.server.table.executor.OrphanFilesCleaningExecutor.DATA_FOLDER_NAME;
import static com.netease.arctic.server.table.executor.OrphanFilesCleaningExecutor.FLINK_JOB_ID;

@RunWith(Parameterized.class)
public class TestOrphanFileClean extends ExecutorTestBase {

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[] parameters() {
    return new Object[][]{
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(true, true)},
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(true, false)},
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(false, true)},
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(false, false)}};
  }

  public TestOrphanFileClean(CatalogTestHelper catalogTestHelper, TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Test
  public void orphanDataFileClean() throws IOException {
    if (isKeyedTable()) {
      writeAndCommitBaseAndChange(getArcticTable());
    } else {
      writeAndCommitBaseStore(getArcticTable());
    }

    UnkeyedTable baseTable = isKeyedTable() ?
        getArcticTable().asKeyedTable().baseTable() : getArcticTable().asUnkeyedTable();
    String baseOrphanFileDir = baseTable.location() +
        File.separator + DATA_FOLDER_NAME + File.separator + "testLocation";
    String baseOrphanFilePath = baseOrphanFileDir + File.separator + "orphan.parquet";
    OutputFile baseOrphanDataFile = getArcticTable().io().newOutputFile(baseOrphanFilePath);
    baseOrphanDataFile.createOrOverwrite().close();
    Assert.assertTrue(getArcticTable().io().exists(baseOrphanFileDir));
    Assert.assertTrue(getArcticTable().io().exists(baseOrphanFilePath));

    String changeOrphanFilePath = isKeyedTable() ? getArcticTable().asKeyedTable().changeTable().location() +
        File.separator + DATA_FOLDER_NAME + File.separator + "orphan.parquet" : "";
    if (isKeyedTable()) {
      OutputFile changeOrphanDataFile = getArcticTable().io().newOutputFile(changeOrphanFilePath);
      changeOrphanDataFile.createOrOverwrite().close();
      Assert.assertTrue(getArcticTable().io().exists(changeOrphanFilePath));
    }

    OrphanFilesCleaningExecutor.cleanContentFiles(
        getArcticTable(),
        System.currentTimeMillis() - TableProperties.MIN_ORPHAN_FILE_EXISTING_TIME_DEFAULT * 60 * 1000);
    OrphanFilesCleaningExecutor.cleanMetadata(
        getArcticTable(),
        System.currentTimeMillis() - TableProperties.MIN_ORPHAN_FILE_EXISTING_TIME_DEFAULT * 60 * 1000);
    Assert.assertTrue(getArcticTable().io().exists(baseOrphanFileDir));
    Assert.assertTrue(getArcticTable().io().exists(baseOrphanFilePath));

    if (isKeyedTable()) {
      Assert.assertTrue(getArcticTable().io().exists(changeOrphanFilePath));
    }

    OrphanFilesCleaningExecutor.cleanContentFiles(getArcticTable(), System.currentTimeMillis());
    OrphanFilesCleaningExecutor.cleanMetadata(getArcticTable(), System.currentTimeMillis());

    Assert.assertFalse(getArcticTable().io().exists(baseOrphanFileDir));
    Assert.assertFalse(getArcticTable().io().exists(baseOrphanFilePath));
    baseTable.newScan().planFiles().forEach(
        task -> Assert.assertTrue(getArcticTable().io().exists(task.file().path().toString())));
    if (isKeyedTable()) {
      Assert.assertFalse(getArcticTable().io().exists(changeOrphanFilePath));
      getArcticTable().asKeyedTable().changeTable().newScan().planFiles().forEach(
          task -> Assert.assertTrue(getArcticTable().io().exists(task.file().path().toString())));
    }
  }

  @Test
  public void orphanMetadataFileClean() throws IOException {
    if (isKeyedTable()) {
      writeAndCommitBaseAndChange(getArcticTable());
    } else {
      writeAndCommitBaseStore(getArcticTable());
    }

    UnkeyedTable baseTable = isKeyedTable() ?
        getArcticTable().asKeyedTable().baseTable() : getArcticTable().asUnkeyedTable();
    String baseOrphanFilePath = baseTable.location() + File.separator + "metadata" +
        File.separator + "orphan.avro";

    String changeOrphanFilePath = isKeyedTable() ? getArcticTable().asKeyedTable().changeTable().location() +
        File.separator + "metadata" + File.separator + "orphan.avro" : "";

    OutputFile baseOrphanDataFile = getArcticTable().io().newOutputFile(baseOrphanFilePath);
    baseOrphanDataFile.createOrOverwrite().close();
    Assert.assertTrue(getArcticTable().io().exists(baseOrphanFilePath));

    String changeInvalidMetadataJson = isKeyedTable() ? getArcticTable().asKeyedTable().changeTable().location() +
        File.separator + "metadata" + File.separator + "v0.metadata.json" : "";
    if (isKeyedTable()) {
      OutputFile changeOrphanDataFile = getArcticTable().io().newOutputFile(changeOrphanFilePath);
      changeOrphanDataFile.createOrOverwrite().close();
      getArcticTable().io().newOutputFile(changeInvalidMetadataJson).createOrOverwrite().close();
      Assert.assertTrue(getArcticTable().io().exists(changeOrphanFilePath));
      Assert.assertTrue(getArcticTable().io().exists(changeInvalidMetadataJson));
    }

    OrphanFilesCleaningExecutor.cleanMetadata(getArcticTable(), System.currentTimeMillis());

    Assert.assertFalse(getArcticTable().io().exists(baseOrphanFilePath));
    if (isKeyedTable()) {
      Assert.assertFalse(getArcticTable().io().exists(changeOrphanFilePath));
      Assert.assertFalse(getArcticTable().io().exists(changeInvalidMetadataJson));
    }
    ExecutorTestBase.assertMetadataExists(getArcticTable());
  }

  @Test
  public void orphanChangeDataFileInBaseClean() {
    Assume.assumeTrue(isKeyedTable());
    KeyedTable testKeyedTable = getArcticTable().asKeyedTable();
    List<DataFile> dataFiles = tableTestHelper().writeChangeStore(
        testKeyedTable, 1L, ChangeAction.INSERT, createRecords(1, 100), false);
    Set<String> pathAll = new HashSet<>();
    Set<String> fileInBaseStore = new HashSet<>();
    Set<String> fileOnlyInChangeLocation = new HashSet<>();

    AppendFiles appendFiles = testKeyedTable.asKeyedTable().baseTable().newAppend();

    for (int i = 0; i < dataFiles.size(); i++) {
      DataFile dataFile = dataFiles.get(i);
      pathAll.add(TableFileUtil.getUriPath(dataFile.path().toString()));
      if (i == 0) {
        appendFiles.appendFile(dataFile).commit();
        fileInBaseStore.add(TableFileUtil.getUriPath(dataFile.path().toString()));
      } else {
        fileOnlyInChangeLocation.add(TableFileUtil.getUriPath(dataFile.path().toString()));
      }
    }
    pathAll.forEach(path -> Assert.assertTrue(testKeyedTable.io().exists(path)));

    OrphanFilesCleaningExecutor.cleanContentFiles(testKeyedTable, System.currentTimeMillis());
    fileInBaseStore.forEach(path -> Assert.assertTrue(testKeyedTable.io().exists(path)));
    fileOnlyInChangeLocation.forEach(path -> Assert.assertFalse(testKeyedTable.io().exists(path)));
  }

  @Test
  public void notDeleteFlinkTemporaryFile() throws IOException {
    if (isKeyedTable()) {
      writeAndCommitBaseAndChange(getArcticTable());
    } else {
      writeAndCommitBaseStore(getArcticTable());
    }
    String flinkJobId = "flinkJobTest";
    String fakeFlinkJobId = "fakeFlinkJobTest";

    UnkeyedTable baseTable = isKeyedTable() ?
        getArcticTable().asKeyedTable().baseTable() : getArcticTable().asUnkeyedTable();
    String baseOrphanFilePath = baseTable.location() + File.separator + "metadata" +
        File.separator + flinkJobId + "orphan.avro";

    String changeOrphanFilePath = isKeyedTable() ? getArcticTable().asKeyedTable().changeTable().location() +
        File.separator + "metadata" + File.separator + flinkJobId + "orphan.avro" : "";
    String fakeChangeOrphanFilePath = isKeyedTable() ? getArcticTable().asKeyedTable().changeTable().location() +
        File.separator + "metadata" + File.separator + fakeFlinkJobId + "orphan.avro" : "";
    String changeInvalidMetadataJson = isKeyedTable() ? getArcticTable().asKeyedTable().changeTable().location() +
        File.separator + "metadata" + File.separator + "v0.metadata.json" : "";

    OutputFile baseOrphanDataFile = getArcticTable().io().newOutputFile(baseOrphanFilePath);
    baseOrphanDataFile.createOrOverwrite().close();
    Assert.assertTrue(getArcticTable().io().exists(baseOrphanFilePath));

    if (isKeyedTable()) {
      OutputFile changeOrphanDataFile = getArcticTable().io().newOutputFile(changeOrphanFilePath);
      changeOrphanDataFile.createOrOverwrite().close();
      OutputFile fakeChangeOrphanDataFile = getArcticTable().io().newOutputFile(fakeChangeOrphanFilePath);
      fakeChangeOrphanDataFile.createOrOverwrite().close();

      getArcticTable().io().newOutputFile(changeInvalidMetadataJson).createOrOverwrite().close();
      AppendFiles appendFiles = getArcticTable().asKeyedTable().changeTable().newAppend();
      appendFiles.set(FLINK_JOB_ID, fakeFlinkJobId);
      appendFiles.commit();
      // set flink.job-id to change table
      AppendFiles appendFiles2 = getArcticTable().asKeyedTable().changeTable().newAppend();
      appendFiles2.set(FLINK_JOB_ID, flinkJobId);
      appendFiles2.commit();

      Assert.assertTrue(getArcticTable().io().exists(changeOrphanFilePath));
      Assert.assertTrue(getArcticTable().io().exists(fakeChangeOrphanFilePath));
      Assert.assertTrue(getArcticTable().io().exists(changeInvalidMetadataJson));
    }

    OrphanFilesCleaningExecutor.cleanMetadata(getArcticTable(), System.currentTimeMillis());
    Assert.assertFalse(getArcticTable().io().exists(baseOrphanFilePath));
    if (isKeyedTable()) {
      // files whose file name starts with flink.job-id should not be deleted
      Assert.assertTrue(getArcticTable().io().exists(changeOrphanFilePath));
      Assert.assertFalse(getArcticTable().io().exists(fakeChangeOrphanFilePath));
      Assert.assertFalse(getArcticTable().io().exists(changeInvalidMetadataJson));
    }

    ExecutorTestBase.assertMetadataExists(getArcticTable());
  }

}
