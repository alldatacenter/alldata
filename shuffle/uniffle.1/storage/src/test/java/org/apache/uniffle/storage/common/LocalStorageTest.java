/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.storage.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.roaringbitmap.RoaringBitmap;

import org.apache.uniffle.common.storage.StorageMedia;
import org.apache.uniffle.common.util.RssUtils;
import org.apache.uniffle.storage.request.CreateShuffleWriteHandlerRequest;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class LocalStorageTest {

  private static File testBaseDir;
  private static File testBaseDirWithoutPermission;
  private static String mountPoint;

  @BeforeAll
  public static void setUp(@TempDir File tempDir) throws IOException  {
    testBaseDir = new File(tempDir, "test");
    testBaseDirWithoutPermission = new File(tempDir, "test-no-permission");
    testBaseDir.mkdir();
    testBaseDirWithoutPermission.mkdirs();
    try {
      mountPoint = Files.getFileStore(testBaseDir.toPath()).name();
    } catch (IOException ioe) {
      // pass
    }
  }

  @AfterAll
  public static void tearDown() {
    testBaseDir.delete();
  }

  private LocalStorage createTestStorage(File baseDir) {
    return LocalStorage.newBuilder().basePath(baseDir.getAbsolutePath())
        .cleanupThreshold(50)
        .highWaterMarkOfWrite(95)
        .lowWaterMarkOfWrite(80)
        .capacity(100)
        .cleanIntervalMs(5000)
        .build();
  }

  @Test
  public void canWriteTest() {
    LocalStorage item = createTestStorage(testBaseDir);

    item.getMetaData().updateDiskSize(20);
    assertTrue(item.canWrite());
    item.getMetaData().updateDiskSize(65);
    assertTrue(item.canWrite());
    item.getMetaData().updateDiskSize(10);
    assertFalse(item.canWrite());
    item.getMetaData().updateDiskSize(-10);
    assertFalse(item.canWrite());
    item.getMetaData().updateDiskSize(-10);
    assertTrue(item.canWrite());
  }

  @Test
  public void baseDirectoryInitTest() throws IOException {
    // empty and writable base dir
    File newBaseDir = new File(testBaseDirWithoutPermission, "test-new");
    assertTrue(newBaseDir.mkdirs());
    // then remove write permission to parent dir.
    Set<PosixFilePermission> perms = Sets.newHashSet();
    perms.add(PosixFilePermission.OWNER_READ);
    perms.add(PosixFilePermission.OWNER_EXECUTE);
    Files.setPosixFilePermissions(testBaseDirWithoutPermission.toPath(), perms);
    createTestStorage(newBaseDir);
    // non-empty and writable base dir
    File childDir = new File(newBaseDir, "placeholder");
    assertTrue(childDir.mkdirs());
    createTestStorage(newBaseDir);
    // base dir is configured to a wrong file instead of dir
    File emptyFile = new File(newBaseDir, "empty_file");
    assertTrue(emptyFile.createNewFile());
    assertThrows(RuntimeException.class, () -> {
      createTestStorage(emptyFile);
    });

    // not existed base dir should work
    File notExisted = new File(newBaseDir, "not_existed");
    createTestStorage(notExisted);
  }

  @Test
  public void removeResourcesTest() throws Exception {
    LocalStorage item = prepareDiskItem();
    final String key1 = RssUtils.generateShuffleKey("1", 1);
    final String key2 = RssUtils.generateShuffleKey("1", 2);
    item.removeResources(key1);
    assertEquals(50L, item.getMetaData().getDiskSize().get());
    assertEquals(0L, item.getMetaData().getShuffleSize(key1));
    assertEquals(50L, item.getMetaData().getShuffleSize(key2));
    assertTrue(item.getMetaData().getNotUploadedPartitions(key1).isEmpty());
  }

  private LocalStorage prepareDiskItem() {
    final LocalStorage item = createTestStorage(testBaseDir);
    RoaringBitmap partitionBitMap = RoaringBitmap.bitmapOf();
    partitionBitMap.add(1);
    partitionBitMap.add(2);
    partitionBitMap.add(1);
    List<Integer> partitionList = Lists.newArrayList(1, 2);
    item.createMetadataIfNotExist("1/1");
    item.createMetadataIfNotExist("1/2");
    item.updateWrite("1/1", 100, partitionList);
    item.updateWrite("1/2", 50, Lists.newArrayList());
    assertEquals(150L, item.getMetaData().getDiskSize().get());
    assertEquals(2, item.getMetaData().getNotUploadedPartitions("1/1").getCardinality());
    assertTrue(partitionBitMap.contains(item.getMetaData().getNotUploadedPartitions("1/1")));
    return item;
  }

  @Test
  public void concurrentRemoveResourcesTest() throws Exception {
    LocalStorage item = prepareDiskItem();
    Runnable runnable = () -> item.removeResources("1/1");
    List<Thread> testThreads = Lists.newArrayList(new Thread(runnable), new Thread(runnable), new Thread(runnable));
    testThreads.forEach(Thread::start);
    testThreads.forEach(t -> {
      try {
        t.join();
      } catch (InterruptedException e) {
        // ignore
      }
    });

    assertEquals(50L, item.getMetaData().getDiskSize().get());
    assertEquals(0L, item.getMetaData().getShuffleSize("1/1"));
    assertEquals(50L, item.getMetaData().getShuffleSize("1/2"));
    assertTrue(item.getMetaData().getNotUploadedPartitions("1/1").isEmpty());
  }

  @Test
  public void diskMetaTest() {
    LocalStorage item = createTestStorage(testBaseDir);
    List<Integer> partitionList1 = Lists.newArrayList(1, 2, 3, 4, 5);
    List<Integer> partitionList2 = Lists.newArrayList(6, 7, 8, 9, 10);
    List<Integer> partitionList3 = Lists.newArrayList(1, 2, 3);
    item.createMetadataIfNotExist("key1");
    item.createMetadataIfNotExist("key2");
    item.updateWrite("key1", 10, partitionList1);
    item.updateWrite("key2", 30, partitionList2);
    item.updateUploadedShuffle("key1", 5, partitionList3);

    assertTrue(item.getNotUploadedPartitions("notKey").isEmpty());
    assertEquals(2, item.getNotUploadedPartitions("key1").getCardinality());
    assertEquals(5, item.getNotUploadedPartitions("key2").getCardinality());
    assertEquals(0, item.getNotUploadedSize("notKey"));
    assertEquals(5, item.getNotUploadedSize("key1"));
    assertEquals(30, item.getNotUploadedSize("key2"));

    assertTrue(item.getSortedShuffleKeys(true, 1).isEmpty());
    assertTrue(item.getSortedShuffleKeys(true, 2).isEmpty());
    item.prepareStartRead("key1");
    assertEquals(1, item.getSortedShuffleKeys(true, 3).size());
    assertEquals(1, item.getSortedShuffleKeys(false, 1).size());
    assertEquals("key2", item.getSortedShuffleKeys(false, 1).get(0));
    assertEquals(2, item.getSortedShuffleKeys(false, 2).size());
    assertEquals(2, item.getSortedShuffleKeys(false, 3).size());
  }

  @Test
  public void diskStorageInfoTest() {
    LocalStorage item = LocalStorage.newBuilder()
        .basePath(testBaseDir.getAbsolutePath())
        .cleanupThreshold(50)
        .highWaterMarkOfWrite(95)
        .lowWaterMarkOfWrite(80)
        .capacity(100)
        .cleanIntervalMs(5000)
        .build();
    assertEquals(mountPoint, item.getMountPoint());
    assertNull(item.getStorageMedia());

    LocalStorage itemWithStorageType = LocalStorage.newBuilder()
        .basePath(testBaseDir.getAbsolutePath())
        .cleanupThreshold(50)
        .highWaterMarkOfWrite(95)
        .lowWaterMarkOfWrite(80)
        .capacity(100)
        .cleanIntervalMs(5000)
        .localStorageMedia(StorageMedia.SSD)
        .build();
    assertEquals(StorageMedia.SSD, itemWithStorageType.getStorageMedia());
  }

  @Test
  public void writeHandlerTest() {
    LocalStorage item = LocalStorage.newBuilder().basePath(testBaseDir.getAbsolutePath()).build();
    String appId = "writeHandlerTest";
    assertFalse(item.containsWriteHandler(appId, 0, 1));
    String[] storageBasePaths = {testBaseDir.getAbsolutePath()};
    CreateShuffleWriteHandlerRequest request = new CreateShuffleWriteHandlerRequest(
        StorageType.LOCALFILE.name(), appId, 0, 1, 1, storageBasePaths,
        "ss1", null, 1, null);
    item.getOrCreateWriteHandler(request);
    assertTrue(item.containsWriteHandler(appId, 0, 1));
  }
}
