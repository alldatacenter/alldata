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

package com.netease.arctic.io;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.relocated.com.google.common.collect.Streams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;

public class TestRecoverableArcticFileIO extends TableTestBase {
  private RecoverableHadoopFileIO recoverableArcticFileIO;
  private ArcticFileIO arcticFileIO;
  TableTrashManager trashManager;
  private String file1;
  private String file2;
  private String file3;

  public TestRecoverableArcticFileIO() {
    super(
        new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
        new BasicTableTestHelper(true, true));
  }

  @Before
  public void before() {
    ArcticTable arcticTable = getArcticTable();
    trashManager = TableTrashManagers.build(arcticTable.id(), arcticTable.location(),
        arcticTable.properties(), (ArcticHadoopFileIO) arcticTable.io());
    recoverableArcticFileIO =
        new RecoverableHadoopFileIO(
            getTableMetaStore(), trashManager, TableProperties.TABLE_TRASH_FILE_PATTERN_DEFAULT);
    arcticFileIO = arcticTable.io();

    file1 = getArcticTable().location() + "/base/test/test1/test1.parquet";
    file2 = getArcticTable().location() + "/base/test/test2/test2.parquet";
    file3 = getArcticTable().location() + "/base/test/test2.parquet";
  }

  @Test
  public void exists() throws IOException {
    createFile(file1);
    Assert.assertTrue(recoverableArcticFileIO.exists(file1));
    Assert.assertFalse(recoverableArcticFileIO.exists(file2));
  }

  @Test
  public void rename() throws IOException {
    String newLocation = getArcticTable().location() + "/base/test/test4.parquet";
    createFile(file1);
    recoverableArcticFileIO.rename(file1, newLocation);
    Assert.assertFalse(arcticFileIO.exists(file1));
    Assert.assertTrue(arcticFileIO.exists(newLocation));
  }

  @Test
  public void deleteDirectoryRecursively() throws IOException {
    createFile(file1);
    createFile(file2);
    createFile(file3);
    String dir = getArcticTable().location() + "/base/test";
    recoverableArcticFileIO.deletePrefix(dir);
    Assert.assertFalse(arcticFileIO.exists(dir));
  }

  @Test
  public void list() throws IOException {
    createFile(file1);
    createFile(file2);
    createFile(file3);
    Iterable<PathInfo> items = recoverableArcticFileIO.listDirectory(getArcticTable().location() + "/base/test");
    Assert.assertEquals(3L, Streams.stream(items).count());
  }

  @Test
  public void isDirectory() throws IOException {
    createFile(file1);
    Assert.assertFalse(recoverableArcticFileIO.isDirectory(file1));
    Assert.assertTrue(recoverableArcticFileIO.isDirectory(getArcticTable().location()));
  }

  @Test
  public void isEmptyDirectory() {
    String dir = getArcticTable().location() + "location";
    arcticFileIO.asFileSystemIO().makeDirectories(dir);
    Assert.assertTrue(recoverableArcticFileIO.isEmptyDirectory(dir));
    Assert.assertFalse(recoverableArcticFileIO.isEmptyDirectory(getArcticTable().location()));
  }

  @Test
  public void deleteFile() throws IOException {
    createFile(file1);
    recoverableArcticFileIO.deleteFile(file1);
    Assert.assertFalse(arcticFileIO.exists(file1));
    Assert.assertTrue(trashManager.fileExistInTrash(file1));
  }

  @Test
  public void deleteInputFile() throws IOException {
    createFile(file1);
    recoverableArcticFileIO.deleteFile(recoverableArcticFileIO.newInputFile(file1));
    Assert.assertFalse(arcticFileIO.exists(file1));
    Assert.assertTrue(trashManager.fileExistInTrash(file1));
  }

  @Test
  public void deleteOutputFile() throws IOException {
    createFile(file1);
    recoverableArcticFileIO.deleteFile(recoverableArcticFileIO.newOutputFile(file1));
    Assert.assertFalse(arcticFileIO.exists(file1));
    Assert.assertTrue(trashManager.fileExistInTrash(file1));
  }

  @Test
  public void trashFilePattern() {
    Assert.assertTrue(recoverableArcticFileIO.matchTrashFilePattern(file1));
    Assert.assertTrue(recoverableArcticFileIO.matchTrashFilePattern(file2));
    Assert.assertTrue(recoverableArcticFileIO.matchTrashFilePattern(file3));
    Assert.assertTrue(recoverableArcticFileIO.matchTrashFilePattern(getArcticTable().location() +
        "/metadata/version-hint.text"));
    Assert.assertTrue(recoverableArcticFileIO.matchTrashFilePattern(getArcticTable().location() +
        "/metadata/v2.metadata.json"));
    Assert.assertTrue(recoverableArcticFileIO.matchTrashFilePattern(getArcticTable().location() +
        "/metadata/snap-1515213806302741636-1-85fc817e-941d-4e9a-ab41-2dbf7687bfcd.avro"));
    Assert.assertTrue(recoverableArcticFileIO.matchTrashFilePattern(getArcticTable().location() +
        "/metadata/3ce7600d-4853-45d0-8533-84c12a611916-m0.avro"));

    Assert.assertFalse(recoverableArcticFileIO.matchTrashFilePattern(getArcticTable().location() +
        "/metadata/3ce7600d-4853-45d0-8533-84c12a611916.avro"));
  }

  private void createFile(String path) throws IOException {
    OutputFile baseOrphanDataFile = arcticFileIO.newOutputFile(path);
    baseOrphanDataFile.createOrOverwrite().close();
  }
}
