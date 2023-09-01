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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.prometheus.client.Gauge;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShufflePartitionedBlock;
import org.apache.uniffle.common.config.RssBaseConf;
import org.apache.uniffle.common.util.ChecksumUtils;
import org.apache.uniffle.server.buffer.ShuffleBufferManager;
import org.apache.uniffle.server.event.AppPurgeEvent;
import org.apache.uniffle.server.storage.HdfsStorageManager;
import org.apache.uniffle.server.storage.LocalStorageManager;
import org.apache.uniffle.server.storage.LocalStorageManagerFallbackStrategy;
import org.apache.uniffle.server.storage.MultiStorageManager;
import org.apache.uniffle.server.storage.StorageManager;
import org.apache.uniffle.server.storage.StorageManagerFactory;
import org.apache.uniffle.storage.HdfsTestBase;
import org.apache.uniffle.storage.common.AbstractStorage;
import org.apache.uniffle.storage.common.HdfsStorage;
import org.apache.uniffle.storage.common.LocalStorage;
import org.apache.uniffle.storage.handler.impl.HdfsClientReadHandler;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShuffleFlushManagerTest extends HdfsTestBase {

  private static AtomicInteger ATOMIC_INT = new AtomicInteger(0);
  private static AtomicLong ATOMIC_LONG = new AtomicLong(0);

  private ShuffleServerConf shuffleServerConf = new ShuffleServerConf();
  private RemoteStorageInfo remoteStorage = new RemoteStorageInfo(HDFS_URI + "rss/test", Maps.newHashMap());

  private static ShuffleServer mockShuffleServer = mock(ShuffleServer.class);

  @BeforeAll
  public static void beforeAll() throws Exception {
    ShuffleTaskManager shuffleTaskManager = mock(ShuffleTaskManager.class);
    ShuffleBufferManager shuffleBufferManager = mock(ShuffleBufferManager.class);

    when(mockShuffleServer.getShuffleTaskManager()).thenReturn(shuffleTaskManager);
    when(mockShuffleServer.getShuffleBufferManager()).thenReturn(shuffleBufferManager);
  }

  @BeforeEach
  public void prepare() {
    ShuffleServerMetrics.register();
    shuffleServerConf.set(ShuffleServerConf.RSS_STORAGE_BASE_PATH, Collections.emptyList());
    shuffleServerConf.setString(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.HDFS.name());
    LogManager.getRootLogger().setLevel(Level.INFO);
  }

  @AfterEach
  public void clear() {
    ShuffleServerMetrics.clear();
  }

  @Test
  public void hadoopConfTest() {
    shuffleServerConf.setString("rss.server.hadoop.dfs.replication", "2");
    shuffleServerConf.setString("rss.server.hadoop.a.b", "value");
    StorageManager storageManager =
        StorageManagerFactory.getInstance().createStorageManager(shuffleServerConf);
    ShuffleFlushManager manager =
        new ShuffleFlushManager(shuffleServerConf, "shuffleServerId", mockShuffleServer, storageManager);
    assertEquals("2", manager.getHadoopConf().get("dfs.replication"));
    assertEquals("value", manager.getHadoopConf().get("a.b"));
  }

  @Test
  public void concurrentWrite2HdfsWriteOfSinglePartition() throws Exception {
    ShuffleServerConf shuffleServerConf = new ShuffleServerConf();
    shuffleServerConf.set(ShuffleServerConf.RSS_STORAGE_BASE_PATH, Collections.emptyList());
    shuffleServerConf.setString(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.HDFS.name());
    int maxConcurrency = 3;
    shuffleServerConf.setInteger(ShuffleServerConf.SERVER_MAX_CONCURRENCY_OF_ONE_PARTITION, maxConcurrency);

    String appId = "concurrentWrite2HdfsWriteOfSinglePartition_appId";
    StorageManager storageManager =
        StorageManagerFactory.getInstance().createStorageManager(shuffleServerConf);
    storageManager.registerRemoteStorage(appId, remoteStorage);
    ShuffleFlushManager manager =
        new ShuffleFlushManager(shuffleServerConf, "shuffleServerId", mockShuffleServer, storageManager);

    IntStream.range(0, 20).forEach(x -> {
      ShuffleDataFlushEvent event = createShuffleDataFlushEvent(appId, 1, 1, 1, null);
      manager.addToFlushQueue(event);
    });
    waitForFlush(manager, appId, 1, 10 * 5);

    FileStatus[] fileStatuses = fs.listStatus(new Path(HDFS_URI + "/rss/test/" + appId + "/1/1-1"));
    long actual = Arrays.stream(fileStatuses).filter(x -> x.getPath().getName().endsWith("data")).count();

    assertEquals(maxConcurrency, actual);
    actual = Arrays.stream(fileStatuses).filter(x -> x.getPath().getName().endsWith("index")).count();
    assertEquals(maxConcurrency, actual);
  }

  @Test
  public void writeTest() throws Exception {
    String appId = "writeTest_appId";
    StorageManager storageManager =
        StorageManagerFactory.getInstance().createStorageManager(shuffleServerConf);
    storageManager.registerRemoteStorage(appId, remoteStorage);
    String storageHost = "localhost";
    assertEquals(0.0, ShuffleServerMetrics.counterRemoteStorageTotalWrite.get(storageHost).get(), 0.5);
    assertEquals(0.0, ShuffleServerMetrics.counterRemoteStorageRetryWrite.get(storageHost).get(), 0.5);
    assertEquals(0.0, ShuffleServerMetrics.counterRemoteStorageFailedWrite.get(storageHost).get(), 0.5);
    assertEquals(0.0, ShuffleServerMetrics.counterRemoteStorageSuccessWrite.get(storageHost).get(), 0.5);
    ShuffleFlushManager manager =
        new ShuffleFlushManager(shuffleServerConf, "shuffleServerId", mockShuffleServer, storageManager);
    ShuffleDataFlushEvent event1 =
        createShuffleDataFlushEvent(appId, 1, 1, 1, null);
    final List<ShufflePartitionedBlock> blocks1 = event1.getShuffleBlocks();
    manager.addToFlushQueue(event1);
    ShuffleDataFlushEvent event21 =
        createShuffleDataFlushEvent(appId, 2, 2, 2, null);
    final List<ShufflePartitionedBlock> blocks21 = event21.getShuffleBlocks();
    manager.addToFlushQueue(event21);
    ShuffleDataFlushEvent event22 =
        createShuffleDataFlushEvent(appId, 2, 2, 2, null);
    final List<ShufflePartitionedBlock> blocks22 = event22.getShuffleBlocks();
    manager.addToFlushQueue(event22);
    // wait for write data
    waitForFlush(manager, appId, 1, 5);
    waitForFlush(manager, appId, 2, 10);
    validate(appId, 1, 1, blocks1, 1, remoteStorage.getPath());
    assertEquals(blocks1.size(), manager.getCommittedBlockIds(appId, 1).getLongCardinality());

    blocks21.addAll(blocks22);
    validate(appId, 2, 2, blocks21, 1, remoteStorage.getPath());
    assertEquals(blocks21.size(), manager.getCommittedBlockIds(appId, 2).getLongCardinality());

    assertEquals(3.0, ShuffleServerMetrics.counterRemoteStorageTotalWrite.get(storageHost).get(), 0.5);
    assertEquals(3.0, ShuffleServerMetrics.counterRemoteStorageSuccessWrite.get(storageHost).get(), 0.5);

    // test case for process event whose related app was cleared already
    assertEquals(0, ShuffleServerMetrics.gaugeWriteHandler.get(), 0.5);
    ShuffleDataFlushEvent fakeEvent =
        createShuffleDataFlushEvent("fakeAppId", 1, 1, 1, null);
    manager.addToFlushQueue(fakeEvent);
    waitForQueueClear(manager);
    waitForMetrics(ShuffleServerMetrics.gaugeWriteHandler, 0, 0.5);
  }
  
  @Test
  public void localMetricsTest(@TempDir File tempDir) throws Exception {
    shuffleServerConf.set(ShuffleServerConf.RSS_STORAGE_BASE_PATH, Arrays.asList(tempDir.getAbsolutePath()));
    shuffleServerConf.set(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.MEMORY_LOCALFILE.name());

    String appId = "localMetricsTest_appId";
    StorageManager storageManager =
            StorageManagerFactory.getInstance().createStorageManager(shuffleServerConf);
    ShuffleFlushManager manager =
            new ShuffleFlushManager(shuffleServerConf, "shuffleServerId", mockShuffleServer, storageManager);
    ShuffleDataFlushEvent event1 =
            createShuffleDataFlushEvent(appId, 1, 1, 1, null);
    manager.addToFlushQueue(event1);
    // wait for write data
    waitForFlush(manager, appId, 1, 5);

    validateLocalMetadata(storageManager, 160L);

    ShuffleDataFlushEvent event12 =
            createShuffleDataFlushEvent(appId, 1, 1, 1, null);
    manager.addToFlushQueue(event12);

    // wait for write data
    waitForFlush(manager, appId, 1, 10);

    validateLocalMetadata(storageManager, 320L);
  }

  @Test
  public void complexWriteTest() throws Exception {
    shuffleServerConf.setString("rss.server.flush.handler.expired", "3");
    StorageManager storageManager =
        StorageManagerFactory.getInstance().createStorageManager(shuffleServerConf);
    String appId = "complexWriteTest_appId";
    storageManager.registerRemoteStorage(appId, remoteStorage);
    List<ShufflePartitionedBlock> expectedBlocks = Lists.newArrayList();
    List<ShuffleDataFlushEvent> flushEvents1 = Lists.newArrayList();
    List<ShuffleDataFlushEvent> flushEvents2 = Lists.newArrayList();
    ShuffleFlushManager manager = new ShuffleFlushManager(shuffleServerConf, "shuffleServerId",
        mockShuffleServer, storageManager);
    for (int i = 0; i < 30; i++) {
      ShuffleDataFlushEvent flushEvent1 = createShuffleDataFlushEvent(appId, 1, 1, 1, null);
      ShuffleDataFlushEvent flushEvent2 = createShuffleDataFlushEvent(appId, 1, 1, 1, null);
      expectedBlocks.addAll(flushEvent1.getShuffleBlocks());
      expectedBlocks.addAll(flushEvent2.getShuffleBlocks());
      flushEvents1.add(flushEvent1);
      flushEvents2.add(flushEvent2);
    }
    Thread flushThread1 = new Thread(() -> {
      for (ShuffleDataFlushEvent event : flushEvents1) {
        manager.addToFlushQueue(event);
      }
    });

    Thread flushThread2 = new Thread(() -> {
      for (ShuffleDataFlushEvent event : flushEvents2) {
        manager.addToFlushQueue(event);
      }
    });
    flushThread1.start();
    flushThread2.start();
    flushThread1.join();
    flushThread2.join();

    waitForFlush(manager, appId, 1, 300);
    validate(appId, 1, 1, expectedBlocks, 1, remoteStorage.getPath());
  }

  @Test
  public void clearTest() throws Exception {
    StorageManager storageManager =
        StorageManagerFactory.getInstance().createStorageManager(shuffleServerConf);
    String appId1 = "complexWriteTest_appId1";
    String appId2 = "complexWriteTest_appId2";
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
    final AbstractStorage storage = (AbstractStorage) storageManager.selectStorage(event1);
    assertEquals(5, manager.getCommittedBlockIds(appId1, 1).getLongCardinality());
    assertEquals(5, manager.getCommittedBlockIds(appId2, 1).getLongCardinality());
    assertEquals(storageManager.selectStorage(event1), storageManager.selectStorage(event2));
    int size = storage.getHandlerSize();
    assertEquals(2, size);
    FileStatus[] fileStatus = fs.listStatus(new Path(remoteStorage.getPath() + "/" + appId1 + "/"));
    assertTrue(fileStatus.length > 0);
    manager.removeResources(appId1);

    assertTrue(((HdfsStorageManager)storageManager).getAppIdToStorages().containsKey(appId1));
    storageManager.removeResources(
        new AppPurgeEvent(appId1, StringUtils.EMPTY, Arrays.asList(1))
    );
    assertFalse(((HdfsStorageManager)storageManager).getAppIdToStorages().containsKey(appId1));
    try {
      fs.listStatus(new Path(remoteStorage.getPath() + "/" + appId1 + "/"));
      fail("Exception should be thrown");
    } catch (FileNotFoundException fnfe) {
      // expected exception
    }

    assertEquals(0, manager.getCommittedBlockIds(appId1, 1).getLongCardinality());
    assertEquals(5, manager.getCommittedBlockIds(appId2, 1).getLongCardinality());
    size = storage.getHandlerSize();
    assertEquals(1, size);
    manager.removeResources(appId2);
    assertTrue(((HdfsStorageManager)storageManager).getAppIdToStorages().containsKey(appId2));
    storageManager.removeResources(
        new AppPurgeEvent(appId2, StringUtils.EMPTY, Arrays.asList(1))
    );
    assertFalse(((HdfsStorageManager)storageManager).getAppIdToStorages().containsKey(appId2));
    assertEquals(0, manager.getCommittedBlockIds(appId2, 1).getLongCardinality());
    size = storage.getHandlerSize();
    assertEquals(0, size);
    // fs create a remoteStorage for appId2 before remove resources,
    // but the cache from appIdToStorages has been removed, so we need to delete this path in hdfs
    Path path = new Path(remoteStorage.getPath() + "/" + appId2 + "/");
    assertTrue(fs.mkdirs(path));
    storageManager.removeResources(
        new AppPurgeEvent(appId2, StringUtils.EMPTY, Lists.newArrayList(1))
    );
    assertFalse(fs.exists(path));
    HdfsStorage storageByAppId = ((HdfsStorageManager) storageManager).getStorageByAppId(appId2);
    assertNull(storageByAppId);
  }

  @Test
  public void clearLocalTest(@TempDir File tempDir) throws Exception {
    final String appId1 = "clearLocalTest_appId1";
    final String appId2 = "clearLocalTest_appId2";
    ShuffleServerConf serverConf = new ShuffleServerConf();
    serverConf.set(ShuffleServerConf.RSS_STORAGE_BASE_PATH, Arrays.asList(tempDir.getAbsolutePath()));
    serverConf.setString(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.LOCALFILE.name());
    serverConf.setLong(ShuffleServerConf.DISK_CAPACITY, 1024L * 1024L * 1024L);
    StorageManager storageManager =
        StorageManagerFactory.getInstance().createStorageManager(serverConf);
    ShuffleFlushManager manager =
        new ShuffleFlushManager(serverConf, "shuffleServerId", mockShuffleServer, storageManager);
    ShuffleDataFlushEvent event1 =
        createShuffleDataFlushEvent(appId1, 1, 0, 1, null);
    manager.addToFlushQueue(event1);
    ShuffleDataFlushEvent event2 =
        createShuffleDataFlushEvent(appId2, 1, 0, 1, null);
    manager.addToFlushQueue(event2);
    assertEquals(storageManager.selectStorage(event1), storageManager.selectStorage(event2));
    final AbstractStorage storage = (AbstractStorage) storageManager.selectStorage(event1);
    waitForFlush(manager, appId1, 1, 5);
    waitForFlush(manager, appId2, 1, 5);
    assertEquals(5, manager.getCommittedBlockIds(appId1, 1).getLongCardinality());
    assertEquals(5, manager.getCommittedBlockIds(appId2, 1).getLongCardinality());
    assertEquals(2, storage.getHandlerSize());
    File file = new File(tempDir, appId1);
    assertTrue(file.exists());
    storageManager.removeResources(
        new AppPurgeEvent(appId1, StringUtils.EMPTY, Lists.newArrayList(1))
    );
    manager.removeResources(appId1);
    assertFalse(file.exists());
    ShuffleDataFlushEvent event3 =
        createShuffleDataFlushEvent(appId1, 1, 0, 1, () -> false);
    manager.addToFlushQueue(event3);
    Thread.sleep(1000);
    assertEquals(0, manager.getCommittedBlockIds(appId1, 1).getLongCardinality());
    assertEquals(5, manager.getCommittedBlockIds(appId2, 1).getLongCardinality());
    assertEquals(1, storage.getHandlerSize());
    manager.removeResources(appId2);
    storageManager.removeResources(
        new AppPurgeEvent(appId2, StringUtils.EMPTY, Lists.newArrayList(1))
    );
    assertEquals(0, manager.getCommittedBlockIds(appId2, 1).getLongCardinality());
    assertEquals(0, storage.getHandlerSize());
  }

  private void waitForMetrics(Gauge gauge, double expected, double delta) throws Exception {
    int retry = 0;
    boolean match = false;
    do {
      Thread.sleep(500);
      if (retry > 10) {
        fail("Unexpected flush process when waitForMetrics");
      }
      retry++;
      try {
        assertEquals(0, gauge.get(), delta);
        match = true;
      } catch (Exception e) {
        // ignore
      }
    } while (!match);
  }

  private void waitForQueueClear(ShuffleFlushManager manager) throws Exception {
    int retry = 0;
    int size = 0;
    do {
      Thread.sleep(500);
      if (retry > 10) {
        fail("Unexpected flush process when waitForQueueClear");
      }
      retry++;
      size = manager.getEventNumInFlush();
    } while (size > 0);
  }

  public static void waitForFlush(ShuffleFlushManager manager,
      String appId, int shuffleId, int expectedBlockNum) throws Exception {
    int retry = 0;
    int size = 0;
    do {
      Thread.sleep(500);
      if (retry > 100) {
        fail("Unexpected flush process");
      }
      retry++;
      size = manager.getCommittedBlockIds(appId, shuffleId).getIntCardinality();
    } while (size < expectedBlockNum);
  }

  public static ShuffleDataFlushEvent createShuffleDataFlushEvent(
      String appId, int shuffleId, int startPartition, int endPartition, Supplier<Boolean> isValid) {
    return createShuffleDataFlushEvent(
      appId,
      shuffleId,
        startPartition,
        endPartition,
        isValid,
        1
    );
  }

  public static ShuffleDataFlushEvent createShuffleDataFlushEvent(
      String appId,
      int shuffleId,
      int startPartition,
      int endPartition,
      Supplier<Boolean> isValid,
      int size) {
    List<ShufflePartitionedBlock> spbs = createBlock(5, 32);
    return new ShuffleDataFlushEvent(ATOMIC_LONG.getAndIncrement(),
        appId, shuffleId, startPartition, endPartition, size, spbs, isValid, null);
  }

  public static List<ShufflePartitionedBlock> createBlock(int num, int length) {
    List<ShufflePartitionedBlock> blocks = Lists.newArrayList();
    for (int i = 0; i < num; i++) {
      byte[] buf = new byte[length];
      new Random().nextBytes(buf);
      blocks.add(new ShufflePartitionedBlock(
          length, length, ChecksumUtils.getCrc32(buf),
          ATOMIC_INT.incrementAndGet(), 0, buf));
    }
    return blocks;
  }

  private void validate(String appId, int shuffleId, int partitionId, List<ShufflePartitionedBlock> blocks,
      int partitionNumPerRange, String basePath) {
    Roaring64NavigableMap expectBlockIds = Roaring64NavigableMap.bitmapOf();
    Roaring64NavigableMap processBlockIds = Roaring64NavigableMap.bitmapOf();
    Set<Long> remainIds = Sets.newHashSet();
    for (ShufflePartitionedBlock spb : blocks) {
      expectBlockIds.addLong(spb.getBlockId());
      remainIds.add(spb.getBlockId());
    }
    HdfsClientReadHandler handler = new HdfsClientReadHandler(
        appId,
        shuffleId,
        partitionId,
        100,
        partitionNumPerRange,
        10,
        blocks.size() * 32,
        expectBlockIds,
        processBlockIds,
        basePath,
        new Configuration());
    ShuffleDataResult sdr = null;
    int matchNum = 0;
    sdr = handler.readShuffleData();
    List<BufferSegment> bufferSegments = sdr.getBufferSegments();
    for (ShufflePartitionedBlock block : blocks) {
      for (BufferSegment bs : bufferSegments) {
        if (bs.getBlockId() == block.getBlockId()) {
          matchNum++;
          break;
        }
      }
    }
    for (BufferSegment bs : bufferSegments) {
      remainIds.remove(bs.getBlockId());
    }
    assertEquals(blocks.size(), matchNum);
  }

  @Test
  public void fallbackWrittenWhenMultiStorageManagerEnableTest(@TempDir File tempDir) throws InterruptedException {
    shuffleServerConf.setLong(ShuffleServerConf.FLUSH_COLD_STORAGE_THRESHOLD_SIZE, 10000L);
    shuffleServerConf.set(RssBaseConf.RSS_STORAGE_TYPE, StorageType.LOCALFILE_HDFS.toString());
    shuffleServerConf.set(RssBaseConf.RSS_STORAGE_BASE_PATH, Arrays.asList(tempDir.getAbsolutePath()));
    shuffleServerConf.set(ShuffleServerConf.DISK_CAPACITY, 100L);
    shuffleServerConf.setString(ShuffleServerConf.MULTISTORAGE_FALLBACK_STRATEGY_CLASS,
        LocalStorageManagerFallbackStrategy.class.getCanonicalName());

    StorageManager storageManager = StorageManagerFactory.getInstance().createStorageManager(shuffleServerConf);
    String remoteStorage = "test";
    String appId = "fallbackWrittenWhenMultiStorageManagerEnableTest";
    storageManager.registerRemoteStorage(appId, new RemoteStorageInfo(remoteStorage));

    ShuffleFlushManager flushManager = new ShuffleFlushManager(
        shuffleServerConf,
        "shuffle-server-id",
        mockShuffleServer,
        storageManager
    );

    // case1: normally written to local storage
    ShuffleDataFlushEvent event = createShuffleDataFlushEvent(appId, 1, 1, 1, null, 100);
    flushManager.addToFlushQueue(event);
    Thread.sleep(1000);
    assertTrue(event.getUnderStorage() instanceof LocalStorage);

    // case2: huge event is written to cold storage directly
    event = createShuffleDataFlushEvent(appId, 1, 1, 1, null, 100000);
    flushManager.addToFlushQueue(event);
    Thread.sleep(1000);
    assertTrue(event.getUnderStorage() instanceof HdfsStorage);
    assertEquals(0, event.getRetryTimes());

    // case3: local disk is full or corrupted, fallback to HDFS
    List<ShufflePartitionedBlock> blocks = Lists.newArrayList(
        new ShufflePartitionedBlock(100000, 1000, 1, 1, 1L, null)
    );
    ShuffleDataFlushEvent bigEvent = new ShuffleDataFlushEvent(1, "1", 1, 1, 1, 100, blocks, null, null);
    bigEvent.setUnderStorage(((MultiStorageManager)storageManager).getWarmStorageManager().selectStorage(event));
    ((MultiStorageManager)storageManager).getWarmStorageManager().updateWriteMetrics(bigEvent, 0);

    event = createShuffleDataFlushEvent(appId, 1, 1, 1, null, 100);
    flushManager.addToFlushQueue(event);
    Thread.sleep(1000);
    assertTrue(event.getUnderStorage() instanceof HdfsStorage);
    assertEquals(1, event.getRetryTimes());
  }

  @Test
  public void processPendingEventsTest(@TempDir File tempDir) {
    try {
      shuffleServerConf.set(RssBaseConf.RSS_STORAGE_TYPE, StorageType.LOCALFILE.toString());
      shuffleServerConf.set(RssBaseConf.RSS_STORAGE_BASE_PATH, Arrays.asList(tempDir.getAbsolutePath()));
      shuffleServerConf.set(ShuffleServerConf.DISK_CAPACITY, 100L);
      shuffleServerConf.set(ShuffleServerConf.PENDING_EVENT_TIMEOUT_SEC, 5L);
      StorageManager storageManager =
          StorageManagerFactory.getInstance().createStorageManager(shuffleServerConf);
      ShuffleFlushManager manager =
          new ShuffleFlushManager(shuffleServerConf, "shuffleServerId", mockShuffleServer, storageManager);
      ShuffleDataFlushEvent event = new ShuffleDataFlushEvent(1, "1", 1, 1, 1, 100, null, null, null);
      assertEquals(0, manager.getPendingEventsSize());
      manager.addPendingEvents(event);
      Thread.sleep(1000);
      assertEquals(0, manager.getPendingEventsSize());
      do {
        Thread.sleep(1 * 1000);
      } while (manager.getEventNumInFlush() != 0);

      List<ShufflePartitionedBlock> blocks = Lists.newArrayList(new ShufflePartitionedBlock(100, 1000, 1, 1, 1L, null));
      ShuffleDataFlushEvent bigEvent = new ShuffleDataFlushEvent(1, "1", 1, 1, 1, 100, blocks, null, null);
      bigEvent.setUnderStorage(storageManager.selectStorage(event));
      storageManager.updateWriteMetrics(bigEvent, 0);

      manager.addPendingEvents(event);
      manager.addPendingEvents(event);
      manager.addPendingEvents(event);
      Thread.sleep(1000);
      assertTrue(2 <= manager.getPendingEventsSize());
      int eventNum = (int) ShuffleServerMetrics.counterTotalDroppedEventNum.get();
      Thread.sleep(6 * 1000);
      assertEquals(eventNum + 3, (int) ShuffleServerMetrics.counterTotalDroppedEventNum.get());
      assertEquals(0, manager.getPendingEventsSize());
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }
  
  private void validateLocalMetadata(StorageManager storageManager, Long size) {
    assertInstanceOf(LocalStorageManager.class, storageManager);
    LocalStorage localStorage = ((LocalStorageManager) storageManager).getStorages().get(0);
    assertEquals(size, localStorage.getMetaData().getDiskSize().longValue());
  }
}
