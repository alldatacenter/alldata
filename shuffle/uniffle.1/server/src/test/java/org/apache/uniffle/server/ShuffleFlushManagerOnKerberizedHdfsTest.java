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

package org.apache.uniffle.server;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.KerberizedHdfsBase;
import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.server.buffer.ShuffleBufferManager;
import org.apache.uniffle.server.event.AppPurgeEvent;
import org.apache.uniffle.server.storage.HdfsStorageManager;
import org.apache.uniffle.server.storage.StorageManager;
import org.apache.uniffle.server.storage.StorageManagerFactory;
import org.apache.uniffle.storage.common.AbstractStorage;
import org.apache.uniffle.storage.util.StorageType;

import static org.apache.uniffle.server.ShuffleFlushManagerTest.createShuffleDataFlushEvent;
import static org.apache.uniffle.server.ShuffleFlushManagerTest.waitForFlush;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShuffleFlushManagerOnKerberizedHdfsTest extends KerberizedHdfsBase {
  private static final Logger LOGGER = LoggerFactory.getLogger(ShuffleFlushManagerOnKerberizedHdfsTest.class);

  private ShuffleServerConf shuffleServerConf = new ShuffleServerConf();

  private static RemoteStorageInfo remoteStorage;
  private static ShuffleServer mockShuffleServer = mock(ShuffleServer.class);

  @BeforeEach
  public void prepare() throws Exception {
    ShuffleServerMetrics.register();
    shuffleServerConf.set(ShuffleServerConf.RSS_STORAGE_BASE_PATH, Collections.emptyList());
    shuffleServerConf.setString(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.HDFS.name());
    shuffleServerConf.setBoolean(ShuffleServerConf.RSS_TEST_MODE_ENABLE, true);
    LogManager.getRootLogger().setLevel(Level.INFO);

    initHadoopSecurityContext();
  }

  @AfterEach
  public void afterEach() {
    ShuffleServerMetrics.clear();
  }

  @BeforeAll
  public static void beforeAll() throws Exception {
    testRunner = ShuffleFlushManagerOnKerberizedHdfsTest.class;
    KerberizedHdfsBase.init();

    ShuffleTaskManager shuffleTaskManager = mock(ShuffleTaskManager.class);
    ShuffleBufferManager shuffleBufferManager = mock(ShuffleBufferManager.class);

    when(mockShuffleServer.getShuffleTaskManager()).thenReturn(shuffleTaskManager);
    when(mockShuffleServer.getShuffleBufferManager()).thenReturn(shuffleBufferManager);

    String storedPath = kerberizedHdfs.getSchemeAndAuthorityPrefix() + "/alex/rss-data/";
    Map<String, String> confMap = new HashMap<>();
    for (Map.Entry<String, String> entry : kerberizedHdfs.getConf()) {
      confMap.put(entry.getKey(), entry.getValue());
    }
    remoteStorage = new RemoteStorageInfo(
        storedPath, confMap
    );
  }

  @Test
  public void clearTest() throws Exception {
    String appId1 = "complexWriteTest_appId1";
    String appId2 = "complexWriteTest_appId2";

    when(mockShuffleServer.getShuffleTaskManager().getUserByAppId(appId1)).thenReturn("alex");
    when(mockShuffleServer.getShuffleTaskManager().getUserByAppId(appId2)).thenReturn("alex");

    StorageManager storageManager =
        StorageManagerFactory.getInstance().createStorageManager(shuffleServerConf);
    storageManager.registerRemoteStorage(appId1, remoteStorage);
    storageManager.registerRemoteStorage(appId2, remoteStorage);
    ShuffleFlushManager manager =
        new ShuffleFlushManager(shuffleServerConf, "shuffleServerId", mockShuffleServer, storageManager);
    ShuffleDataFlushEvent event1 =
        createShuffleDataFlushEvent(appId1, 1, 0, 1, null);
    manager.addToFlushQueue(event1);
    ShuffleDataFlushEvent event2 =
        createShuffleDataFlushEvent(appId2, 1, 0, 1, null);
    manager.addToFlushQueue(event2);
    waitForFlush(manager, appId1, 1, 5);
    waitForFlush(manager, appId2, 1, 5);
    assertEquals(5, manager.getCommittedBlockIds(appId1, 1).getLongCardinality());
    assertEquals(5, manager.getCommittedBlockIds(appId2, 1).getLongCardinality());
    assertEquals(storageManager.selectStorage(event1), storageManager.selectStorage(event2));
    AbstractStorage storage = (AbstractStorage) storageManager.selectStorage(event1);
    int size = storage.getHandlerSize();
    assertEquals(2, size);

    FileStatus[] fileStatus = kerberizedHdfs.getFileSystem()
        .listStatus(new Path(remoteStorage.getPath() + "/" + appId1 + "/"));
    for (FileStatus fileState : fileStatus) {
      assertEquals("alex", fileState.getOwner());
    }
    assertTrue(fileStatus.length > 0);
    manager.removeResources(appId1);

    assertTrue(((HdfsStorageManager)storageManager).getAppIdToStorages().containsKey(appId1));
    storageManager.removeResources(
        new AppPurgeEvent(appId1, "alex", Arrays.asList(1))
    );
    assertFalse(((HdfsStorageManager)storageManager).getAppIdToStorages().containsKey(appId1));
    try {
      kerberizedHdfs.getFileSystem().listStatus(new Path(remoteStorage.getPath() + "/" + appId1 + "/"));
      fail("Exception should be thrown");
    } catch (FileNotFoundException fnfe) {
      // expected exception
    }

    assertTrue(kerberizedHdfs.getFileSystem().exists(new Path(remoteStorage.getPath())));

    assertEquals(0, manager.getCommittedBlockIds(appId1, 1).getLongCardinality());
    assertEquals(5, manager.getCommittedBlockIds(appId2, 1).getLongCardinality());
    size = storage.getHandlerSize();
    assertEquals(1, size);
    manager.removeResources(appId2);
    assertTrue(((HdfsStorageManager)storageManager).getAppIdToStorages().containsKey(appId2));
    storageManager.removeResources(
        new AppPurgeEvent(appId2, "alex", Arrays.asList(1))
    );
    assertFalse(((HdfsStorageManager)storageManager).getAppIdToStorages().containsKey(appId2));
    assertEquals(0, manager.getCommittedBlockIds(appId2, 1).getLongCardinality());
    size = storage.getHandlerSize();
    assertEquals(0, size);
  }
}
