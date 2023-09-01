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

package org.apache.uniffle.server.storage;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.ShufflePartitionedBlock;
import org.apache.uniffle.server.ShuffleDataFlushEvent;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertSame;

public class StorageManagerFallbackStrategyTest {
  private ShuffleServerConf conf;
  
  @BeforeEach
  public void prepare() {
    conf = new ShuffleServerConf();
    conf.setLong(ShuffleServerConf.FLUSH_COLD_STORAGE_THRESHOLD_SIZE, 10000L);
    conf.set(ShuffleServerConf.RSS_STORAGE_BASE_PATH, Arrays.asList("test"));
    conf.setLong(ShuffleServerConf.DISK_CAPACITY, 10000L);
    conf.setString(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.MEMORY_LOCALFILE_HDFS.name());
  }

  @Test
  public void testDefaultFallbackStrategy() {
    RotateStorageManagerFallbackStrategy fallbackStrategy = new RotateStorageManagerFallbackStrategy(conf);
    LocalStorageManager warmStorageManager = new LocalStorageManager(conf);
    HdfsStorageManager coldStorageManager = new HdfsStorageManager(conf);
    StorageManager current = warmStorageManager;
    String remoteStorage = "test";
    String appId = "testDefaultFallbackStrategy_appId";
    coldStorageManager.registerRemoteStorage(appId, new RemoteStorageInfo(remoteStorage));
    List<ShufflePartitionedBlock> blocks = Lists.newArrayList(
        new ShufflePartitionedBlock(100, 1000, 1, 1, 1L, null));
    ShuffleDataFlushEvent event = new ShuffleDataFlushEvent(
        1, appId, 1, 1, 1, 1000, blocks, null, null);
    event.increaseRetryTimes();
    StorageManager storageManager = fallbackStrategy.tryFallback(
        current, event, warmStorageManager, coldStorageManager);
    assertSame(storageManager, coldStorageManager);

    conf.setLong(ShuffleServerConf.FALLBACK_MAX_FAIL_TIMES, 3);
    fallbackStrategy = new RotateStorageManagerFallbackStrategy(conf);
    storageManager = fallbackStrategy.tryFallback(current, event, warmStorageManager, coldStorageManager);
    assertSame(storageManager, warmStorageManager);
    for (int i = 0; i < 2; i++) {
      event.increaseRetryTimes();
    }
    storageManager = fallbackStrategy.tryFallback(current, event, warmStorageManager, coldStorageManager);
    assertSame(storageManager, coldStorageManager);
    event.increaseRetryTimes();
    storageManager = fallbackStrategy.tryFallback(current, event, warmStorageManager, coldStorageManager);
    assertSame(storageManager, warmStorageManager);
    for (int i = 0; i < 2; i++) {
      event.increaseRetryTimes();
    }
    storageManager = fallbackStrategy.tryFallback(coldStorageManager, event, warmStorageManager, coldStorageManager);
    assertSame(storageManager, warmStorageManager);
  }

  @Test
  public void testHdfsFallbackStrategy() {
    HdfsStorageManagerFallbackStrategy fallbackStrategy = new HdfsStorageManagerFallbackStrategy(conf);
    LocalStorageManager warmStorageManager = new LocalStorageManager(conf);
    HdfsStorageManager coldStorageManager = new HdfsStorageManager(conf);
    String remoteStorage = "test";
    String appId = "testHdfsFallbackStrategy_appId";
    coldStorageManager.registerRemoteStorage(appId, new RemoteStorageInfo(remoteStorage));
    List<ShufflePartitionedBlock> blocks = Lists.newArrayList(
        new ShufflePartitionedBlock(100, 1000, 1, 1, 1L, null));
    ShuffleDataFlushEvent event = new ShuffleDataFlushEvent(
        1, appId, 1, 1, 1, 1000, blocks, null, null);
    event.increaseRetryTimes();
    StorageManager storageManager = fallbackStrategy.tryFallback(
        warmStorageManager, event, warmStorageManager, coldStorageManager);
    assertSame(storageManager, warmStorageManager);

    storageManager = fallbackStrategy.tryFallback(coldStorageManager, event, warmStorageManager, coldStorageManager);
    assertSame(storageManager, warmStorageManager);
  }


  @Test
  public void testLocalFallbackStrategy() {
    LocalStorageManagerFallbackStrategy fallbackStrategy = new LocalStorageManagerFallbackStrategy(conf);
    LocalStorageManager warmStorageManager = new LocalStorageManager(conf);
    HdfsStorageManager coldStorageManager = new HdfsStorageManager(conf);
    String remoteStorage = "test";
    String appId = "testLocalFallbackStrategy_appId";
    coldStorageManager.registerRemoteStorage(appId, new RemoteStorageInfo(remoteStorage));
    List<ShufflePartitionedBlock> blocks = Lists.newArrayList(
        new ShufflePartitionedBlock(100, 1000, 1, 1, 1L, null));
    ShuffleDataFlushEvent event = new ShuffleDataFlushEvent(
        1, appId, 1, 1, 1, 1000, blocks, null, null);
    event.increaseRetryTimes();
    StorageManager storageManager = fallbackStrategy.tryFallback(
        warmStorageManager, event, warmStorageManager, coldStorageManager);
    assertSame(storageManager, coldStorageManager);

    storageManager = fallbackStrategy.tryFallback(coldStorageManager, event, warmStorageManager, coldStorageManager);
    assertSame(storageManager, coldStorageManager);
  }
}
