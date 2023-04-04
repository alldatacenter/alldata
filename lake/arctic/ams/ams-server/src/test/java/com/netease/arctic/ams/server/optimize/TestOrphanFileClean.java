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

import com.netease.arctic.ams.api.DataFileInfo;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.service.impl.FileInfoCacheService;
import com.netease.arctic.ams.server.service.impl.OrphanFilesCleanService;
import com.netease.arctic.ams.server.utils.JDBCSqlSessionFactoryProvider;
import com.netease.arctic.catalog.BasicArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.io.writer.GenericChangeTaskWriter;
import com.netease.arctic.io.writer.GenericTaskWriters;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.TableFileUtils;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.ManifestFile;
import org.apache.iceberg.ReachableFileUtil;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.Table;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.io.WriteResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.netease.arctic.ams.server.service.impl.OrphanFilesCleanService.DATA_FOLDER_NAME;
import static com.netease.arctic.ams.server.service.impl.OrphanFilesCleanService.FLINK_JOB_ID;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({
    CatalogLoader.class,
    ServiceContainer.class,
    JDBCSqlSessionFactoryProvider.class
})
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "org.apache.http.conn.ssl.*",
    "com.amazonaws.http.conn.ssl.*",
    "javax.net.ssl.*", "org.apache.hadoop.*", "javax.*", "com.sun.org.apache.*", "org.apache.xerces.*"})
public class TestOrphanFileClean extends TestBaseOptimizeBase {

  @Before
  public void mock() {
    mockStatic(JDBCSqlSessionFactoryProvider.class);
    mockStatic(ServiceContainer.class);
    mockStatic(CatalogLoader.class);
    when(JDBCSqlSessionFactoryProvider.get()).thenReturn(null);
    FakeFileInfoCacheService fakeFileInfoCacheService = new FakeFileInfoCacheService();
    when(ServiceContainer.getFileInfoCacheService()).thenReturn(fakeFileInfoCacheService);
    when(ServiceContainer.getOrphanFilesCleanService()).thenReturn(new OrphanFilesCleanService());
    FakeArcticCatalog fakeArcticCatalog = new FakeArcticCatalog();
    fakeArcticCatalog.addTable(testKeyedTable);
    when(CatalogLoader.load(null, testKeyedTable.id().getCatalog())).thenReturn(fakeArcticCatalog);
  }

  @Test
  public void orphanDataFileClean() throws IOException {
    OrphanFilesCleanService.TableOrphanFileClean tableOrphanFileClean =
        new OrphanFilesCleanService.TableOrphanFileClean(testKeyedTable.id());
    insertTableBaseDataFiles(testKeyedTable);

    String baseOrphanFileDir = testKeyedTable.baseTable().location() +
        File.separator + DATA_FOLDER_NAME + File.separator + "testLocation";
    String baseOrphanFilePath = baseOrphanFileDir + File.separator + "orphan.parquet";
    String changeOrphanFilePath = testKeyedTable.changeTable().location() +
        File.separator + DATA_FOLDER_NAME + File.separator + "orphan.parquet";
    OutputFile baseOrphanDataFile = testKeyedTable.io().newOutputFile(baseOrphanFilePath);
    baseOrphanDataFile.createOrOverwrite().close();
    OutputFile changeOrphanDataFile = testKeyedTable.io().newOutputFile(changeOrphanFilePath);
    changeOrphanDataFile.createOrOverwrite().close();
    Assert.assertTrue(testKeyedTable.io().exists(baseOrphanFileDir));
    Assert.assertTrue(testKeyedTable.io().exists(baseOrphanFilePath));
    Assert.assertTrue(testKeyedTable.io().exists(changeOrphanFilePath));

    tableOrphanFileClean.run();
    Assert.assertTrue(testKeyedTable.io().exists(baseOrphanFileDir));
    Assert.assertTrue(testKeyedTable.io().exists(baseOrphanFilePath));
    Assert.assertTrue(testKeyedTable.io().exists(changeOrphanFilePath));

    testKeyedTable.updateProperties()
        .set(TableProperties.MIN_ORPHAN_FILE_EXISTING_TIME, "0")
        .set(TableProperties.ENABLE_ORPHAN_CLEAN, "true")
        .commit();
    tableOrphanFileClean.run();

    Assert.assertFalse(testKeyedTable.io().exists(baseOrphanFileDir));
    Assert.assertFalse(testKeyedTable.io().exists(baseOrphanFilePath));
    Assert.assertFalse(testKeyedTable.io().exists(changeOrphanFilePath));
    for (FileScanTask task : testKeyedTable.baseTable().newScan().planFiles()) {
      Assert.assertTrue(testKeyedTable.io().exists(task.file().path().toString()));
    }
    for (FileScanTask task : testKeyedTable.changeTable().newScan().planFiles()) {
      Assert.assertTrue(testKeyedTable.io().exists(task.file().path().toString()));
    }
  }

  @Test
  public void orphanChangeDataFileInBaseClean() throws IOException {
    GenericChangeTaskWriter writer = GenericTaskWriters.builderFor(testKeyedTable)
        .withTransactionId(1L).buildChangeWriter();
    for (Record record : baseRecords(1, 10, testKeyedTable.schema())) {
      writer.write(record);
    }
    Set<String> pathAll = new HashSet<>();
    Set<String> fileInBaseStore = new HashSet<>();
    Set<String> fileOnlyInChangeLocation = new HashSet<>();
    WriteResult result = writer.complete();
    AppendFiles appendFiles = testKeyedTable.asKeyedTable().baseTable().newAppend();

    for (int i = 0; i < result.dataFiles().length; i++) {
      DataFile dataFile = result.dataFiles()[i];
      pathAll.add(TableFileUtils.getUriPath(dataFile.path().toString()));
      if (i == 0) {
        appendFiles.appendFile(dataFile).commit();
        fileInBaseStore.add(TableFileUtils.getUriPath(dataFile.path().toString()));
      } else {
        fileOnlyInChangeLocation.add(TableFileUtils.getUriPath(dataFile.path().toString()));
      }
    }
    for (String s : pathAll) {
      Assert.assertTrue(testKeyedTable.io().exists(s));
    }

    OrphanFilesCleanService.cleanContentFiles(testKeyedTable, System.currentTimeMillis());
    for (String s : fileInBaseStore) {
      Assert.assertTrue(testKeyedTable.io().exists(s));
    }
    for (String s : fileOnlyInChangeLocation) {
      Assert.assertFalse(testKeyedTable.io().exists(s));
    }
  }

  @Test
  public void orphanMetadataFileClean() throws IOException {
    insertTableBaseDataFiles(testKeyedTable);

    String baseOrphanFilePath = testKeyedTable.baseTable().location() + File.separator + "metadata" +
        File.separator + "orphan.avro";
    String changeOrphanFilePath = testKeyedTable.changeTable().location() + File.separator + "metadata" +
        File.separator + "orphan.avro";
    OutputFile baseOrphanDataFile = testKeyedTable.io().newOutputFile(baseOrphanFilePath);
    baseOrphanDataFile.createOrOverwrite().close();
    OutputFile changeOrphanDataFile = testKeyedTable.io().newOutputFile(changeOrphanFilePath);
    changeOrphanDataFile.createOrOverwrite().close();

    String changeInvalidMetadataJson = testKeyedTable.changeTable().location() + File.separator + "metadata" +
        File.separator + "v0.metadata.json";
    testKeyedTable.io().newOutputFile(changeInvalidMetadataJson).createOrOverwrite().close();
    Assert.assertTrue(testKeyedTable.io().exists(baseOrphanFilePath));
    Assert.assertTrue(testKeyedTable.io().exists(changeOrphanFilePath));
    Assert.assertTrue(testKeyedTable.io().exists(changeInvalidMetadataJson));
    
    OrphanFilesCleanService.cleanMetadata(testKeyedTable, System.currentTimeMillis());
    Assert.assertFalse(testKeyedTable.io().exists(baseOrphanFilePath));
    Assert.assertFalse(testKeyedTable.io().exists(changeOrphanFilePath));
    Assert.assertFalse(testKeyedTable.io().exists(changeInvalidMetadataJson));
    
    assertMetadataExists(testKeyedTable.changeTable());
    assertMetadataExists(testKeyedTable.baseTable());
  }

  @Test
  public void notDeleteFlinkTemporaryFile() throws IOException {
    insertTableBaseDataFiles(testKeyedTable);
    String flinkJobId = "flinkJobTest";
    String fakeFlinkJobId = "fakeFlinkJobTest";

    String baseOrphanFilePath = testKeyedTable.baseTable().location() + File.separator + "metadata" +
        File.separator + flinkJobId + "orphan.avro";
    String changeOrphanFilePath = testKeyedTable.changeTable().location() + File.separator + "metadata" +
        File.separator + flinkJobId + "orphan.avro";
    String fakeChangeOrphanFilePath = testKeyedTable.changeTable().location() + File.separator + "metadata" +
        File.separator + fakeFlinkJobId + "orphan.avro";
    OutputFile baseOrphanDataFile = testKeyedTable.io().newOutputFile(baseOrphanFilePath);
    baseOrphanDataFile.createOrOverwrite().close();
    OutputFile changeOrphanDataFile = testKeyedTable.io().newOutputFile(changeOrphanFilePath);
    changeOrphanDataFile.createOrOverwrite().close();
    OutputFile fakeChangeOrphanDataFile = testKeyedTable.io().newOutputFile(fakeChangeOrphanFilePath);
    fakeChangeOrphanDataFile.createOrOverwrite().close();

    String changeInvalidMetadataJson = testKeyedTable.changeTable().location() + File.separator + "metadata" +
        File.separator + "v0.metadata.json";
    testKeyedTable.io().newOutputFile(changeInvalidMetadataJson).createOrOverwrite().close();

    AppendFiles appendFiles = testKeyedTable.changeTable().newAppend();
    appendFiles.set(FLINK_JOB_ID, fakeFlinkJobId);
    appendFiles.commit();

    // set flink.job-id to change table
    AppendFiles appendFiles2 = testKeyedTable.changeTable().newAppend();
    appendFiles2.set(FLINK_JOB_ID, flinkJobId);
    appendFiles2.commit();

    Assert.assertTrue(testKeyedTable.io().exists(baseOrphanFilePath));
    Assert.assertTrue(testKeyedTable.io().exists(changeOrphanFilePath));
    Assert.assertTrue(testKeyedTable.io().exists(fakeChangeOrphanFilePath));
    Assert.assertTrue(testKeyedTable.io().exists(changeInvalidMetadataJson));

    OrphanFilesCleanService.cleanMetadata(testKeyedTable, System.currentTimeMillis());
    Assert.assertFalse(testKeyedTable.io().exists(baseOrphanFilePath));
    // files whose file name starts with flink.job-id should not be deleted
    Assert.assertTrue(testKeyedTable.io().exists(changeOrphanFilePath));
    Assert.assertFalse(testKeyedTable.io().exists(fakeChangeOrphanFilePath));
    Assert.assertFalse(testKeyedTable.io().exists(changeInvalidMetadataJson));

    assertMetadataExists(testKeyedTable.changeTable());
    assertMetadataExists(testKeyedTable.baseTable());
  }

  private void assertMetadataExists(Table table) {
    for (Snapshot snapshot : table.snapshots()) {
      Assert.assertTrue(testKeyedTable.io().exists(snapshot.manifestListLocation()));
      for (ManifestFile allManifest : snapshot.allManifests()) {
        Assert.assertTrue(testKeyedTable.io().exists(allManifest.path()));
      }
    }
    for (String metadataFile : ReachableFileUtil.metadataFileLocations(table, false)) {
      Assert.assertTrue(testKeyedTable.io().exists(metadataFile));
    }
    Assert.assertTrue(testKeyedTable.io().exists(ReachableFileUtil.versionHintLocation(table)));
  }

  private static class FakeFileInfoCacheService extends FileInfoCacheService {

    public FakeFileInfoCacheService() {
      super();
    }

    @Override
    public List<DataFileInfo> getOptimizeDatafiles(TableIdentifier tableIdentifier, String tableType) {
      return Collections.emptyList();
    }
  }

  private static class FakeArcticCatalog extends BasicArcticCatalog {
    private final Map<com.netease.arctic.table.TableIdentifier, ArcticTable> tables = new HashMap<>();

    public void addTable(ArcticTable table) {
      tables.put(table.id(), table);
    }

    @Override
    public ArcticTable loadTable(com.netease.arctic.table.TableIdentifier identifier) {
      return tables.get(identifier);
    }
  }
}
