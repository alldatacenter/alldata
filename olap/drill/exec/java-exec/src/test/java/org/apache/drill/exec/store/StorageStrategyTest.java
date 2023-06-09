/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StorageStrategyTest extends BaseTest {
  private static final FsPermission FULL_PERMISSION = FsPermission.getDirDefault();
  private static final StorageStrategy PERSISTENT_STRATEGY = new StorageStrategy("002", false);
  private static final StorageStrategy TEMPORARY_STRATEGY = new StorageStrategy("077", true);
  private static FileSystem fs;

  @BeforeClass
  public static void setup() throws Exception {
    fs = ExecTest.getLocalFileSystem();
  }

  @Test
  public void testPermissionAndDeleteOnExitFalseForFileWithParent() throws Exception {
    Path initialPath = prepareStorageDirectory();
    Path file = addNLevelsAndFile(initialPath, 2, true);
    Path firstCreatedParentPath = addNLevelsAndFile(initialPath, 1, false);

    Path createdParentPath = PERSISTENT_STRATEGY.createFileAndApply(fs, file);

    assertEquals("Path should match", firstCreatedParentPath, createdParentPath);
    checkPathAndPermission(initialPath, file, true, 2, PERSISTENT_STRATEGY);
    checkDeleteOnExit(firstCreatedParentPath, false);
  }

  @Test
  public void testPermissionAndDeleteOnExitTrueForFileWithParent() throws Exception {
    Path initialPath = prepareStorageDirectory();
    Path file = addNLevelsAndFile(initialPath, 2, true);
    Path firstCreatedParentPath = addNLevelsAndFile(initialPath, 1, false);

    Path createdParentPath = TEMPORARY_STRATEGY.createFileAndApply(fs, file);

    assertEquals("Path should match", firstCreatedParentPath, createdParentPath);
    checkPathAndPermission(initialPath, file, true, 2, TEMPORARY_STRATEGY);
    checkDeleteOnExit(firstCreatedParentPath, true);
  }

  @Test
  public void testPermissionAndDeleteOnExitFalseForFileOnly() throws Exception {
    Path initialPath = prepareStorageDirectory();
    Path file = addNLevelsAndFile(initialPath, 0, true);

    Path createdFile = PERSISTENT_STRATEGY.createFileAndApply(fs, file);

    assertEquals("Path should match", file, createdFile);
    checkPathAndPermission(initialPath, file, true, 0, PERSISTENT_STRATEGY);
    checkDeleteOnExit(file, false);
  }

  @Test
  public void testPermissionAndDeleteOnExitTrueForFileOnly() throws Exception {
    Path initialPath = prepareStorageDirectory();
    Path file = addNLevelsAndFile(initialPath, 0, true);

    Path createdFile = TEMPORARY_STRATEGY.createFileAndApply(fs, file);

    assertEquals("Path should match", file, createdFile);
    checkPathAndPermission(initialPath, file, true, 0, TEMPORARY_STRATEGY);
    checkDeleteOnExit(file, true);
  }

  @Test(expected = IOException.class)
  public void testFailureOnExistentFile() throws Exception {
    Path initialPath = prepareStorageDirectory();
    Path file = addNLevelsAndFile(initialPath, 0, true);
    fs.createNewFile(file);
    assertTrue("File should exist", fs.exists(file));
    try {
      PERSISTENT_STRATEGY.createFileAndApply(fs, file);
    } catch (IOException e) {
      assertEquals("Error message should match", String.format("File [%s] already exists on file system [%s].",
          file.toUri().getPath(), fs.getUri()), e.getMessage());
      throw e;
    }
  }

  @Test
  public void testCreatePathAndDeleteOnExitFalse() throws Exception {
    Path initialPath = prepareStorageDirectory();
    Path resultPath = addNLevelsAndFile(initialPath, 2, false);
    Path firstCreatedParentPath = addNLevelsAndFile(initialPath, 1, false);

    Path createdParentPath = PERSISTENT_STRATEGY.createPathAndApply(fs, resultPath);

    assertEquals("Path should match", firstCreatedParentPath, createdParentPath);
    checkPathAndPermission(initialPath, resultPath, false, 2, PERSISTENT_STRATEGY);
    checkDeleteOnExit(firstCreatedParentPath, false);
  }

  @Test
  public void testCreatePathAndDeleteOnExitTrue() throws Exception {
    Path initialPath = prepareStorageDirectory();
    Path resultPath = addNLevelsAndFile(initialPath, 2, false);
    Path firstCreatedParentPath = addNLevelsAndFile(initialPath, 1, false);

    Path createdParentPath = TEMPORARY_STRATEGY.createPathAndApply(fs, resultPath);

    assertEquals("Path should match", firstCreatedParentPath, createdParentPath);
    checkPathAndPermission(initialPath, resultPath, false, 2, TEMPORARY_STRATEGY);
    checkDeleteOnExit(firstCreatedParentPath, true);
  }

  @Test
  public void testCreateNoPath() throws Exception {
    Path path = prepareStorageDirectory();

    Path createdParentPath = TEMPORARY_STRATEGY.createPathAndApply(fs, path);

    assertNull("Path should be null", createdParentPath);
    assertEquals("Permission should match", FULL_PERMISSION, fs.getFileStatus(path).getPermission());
  }

  @Test
  public void testStrategyForExistingFile() throws Exception {
    Path initialPath = prepareStorageDirectory();
    Path file = addNLevelsAndFile(initialPath, 0, true);
    fs.createNewFile(file);
    fs.setPermission(file, FULL_PERMISSION);

    assertTrue("File should exist", fs.exists(file));
    assertEquals("Permission should match", FULL_PERMISSION, fs.getFileStatus(file).getPermission());

    TEMPORARY_STRATEGY.applyToFile(fs, file);

    assertEquals("Permission should match", new FsPermission(TEMPORARY_STRATEGY.getFilePermission()),
        fs.getFileStatus(file).getPermission());
    checkDeleteOnExit(file, true);
  }

  @Test
  public void testInvalidUmask() throws Exception {
    for (String invalid : Lists.newArrayList("ABC", "999", null)) {
      StorageStrategy storageStrategy = new StorageStrategy(invalid, true);
      assertEquals("Umask value should match default", StorageStrategy.DEFAULT.getUmask(), storageStrategy.getUmask());
      assertTrue("deleteOnExit flag should be set to true", storageStrategy.isDeleteOnExit());
    }
  }

  private Path prepareStorageDirectory() throws IOException {
    File storageDirectory = DrillFileUtils.createTempDir();
    storageDirectory.deleteOnExit();
    Path path = new Path(storageDirectory.toURI().getPath());
    fs.setPermission(path, FULL_PERMISSION);
    return path;
  }

  private Path addNLevelsAndFile(Path initialPath, int levels, boolean addFile) {
    Path resultPath = initialPath;
    for (int i = 1; i <= levels; i++) {
      resultPath = new Path(resultPath, "level" + i);
    }
    if (addFile) {
      resultPath = new Path(resultPath, "test_file.txt");
    }
    return resultPath;
  }

  private void checkPathAndPermission(Path initialPath,
                                      Path resultPath,
                                      boolean isFile,
                                      int levels,
                                      StorageStrategy storageStrategy) throws IOException {

    assertEquals("Path type should match", isFile, fs.isFile(resultPath));
    assertEquals("Permission should match", FULL_PERMISSION, fs.getFileStatus(initialPath).getPermission());

    if (isFile) {
      assertEquals("Permission should match", new FsPermission(storageStrategy.getFilePermission()),
          fs.getFileStatus(resultPath).getPermission());
    }
    Path startingPath = initialPath;
    FsPermission folderPermission = new FsPermission(storageStrategy.getFolderPermission());
    for (int i = 1; i <= levels; i++) {
      startingPath = new Path(startingPath, "level" + i);
      assertEquals("Permission should match", folderPermission, fs.getFileStatus(startingPath).getPermission());
    }
  }

  private void checkDeleteOnExit(Path path, boolean isMarkedToBeDeleted) throws IOException {
    assertTrue("Path should be present", fs.exists(path));
    assertEquals("Path delete-on-exit status should match", isMarkedToBeDeleted, fs.cancelDeleteOnExit(path));
  }
}
