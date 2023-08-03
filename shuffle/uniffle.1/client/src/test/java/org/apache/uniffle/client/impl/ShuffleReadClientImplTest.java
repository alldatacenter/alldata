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

package org.apache.uniffle.client.impl;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.roaringbitmap.longlong.LongIterator;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.client.TestUtils;
import org.apache.uniffle.client.response.CompressedShuffleBlock;
import org.apache.uniffle.client.util.DefaultIdHelper;
import org.apache.uniffle.common.ShufflePartitionedBlock;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.util.ChecksumUtils;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.storage.HdfsTestBase;
import org.apache.uniffle.storage.handler.impl.HdfsShuffleWriteHandler;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;

public class ShuffleReadClientImplTest extends HdfsTestBase {

  private static final String EXPECTED_EXCEPTION_MESSAGE = "Exception should be thrown";
  private static AtomicLong ATOMIC_LONG = new AtomicLong(0);

  private ShuffleServerInfo ssi1 = new ShuffleServerInfo("host1-0", "host1", 0);
  private ShuffleServerInfo ssi2 = new ShuffleServerInfo("host2-0", "host2", 0);

  @Test
  public void readTest1() throws Exception {
    String basePath = HDFS_URI + "clientReadTest1";
    HdfsShuffleWriteHandler writeHandler =
        new HdfsShuffleWriteHandler("appId", 0, 1, 1, basePath, ssi1.getId(), conf);

    Map<Long, byte[]> expectedData = Maps.newHashMap();
    Roaring64NavigableMap blockIdBitmap = Roaring64NavigableMap.bitmapOf();
    Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf(0);
    writeTestData(writeHandler, 2, 30, 0, expectedData,
        blockIdBitmap);

    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(), "appId", 0, 1, 100, 1,
        10, 1000, basePath, blockIdBitmap, taskIdBitmap,
        Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());

    TestUtils.validateResult(readClient, expectedData);
    readClient.checkProcessedBlockIds();
    readClient.close();

    blockIdBitmap.addLong(Constants.MAX_TASK_ATTEMPT_ID - 1);
    taskIdBitmap.addLong(Constants.MAX_TASK_ATTEMPT_ID - 1);
    readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(), "appId", 0, 1, 100, 1,
        10, 1000, basePath, blockIdBitmap, taskIdBitmap,
        Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());
    TestUtils.validateResult(readClient, expectedData);
    try {
      // can't find all expected block id, data loss
      readClient.checkProcessedBlockIds();
      fail(EXPECTED_EXCEPTION_MESSAGE);
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Blocks read inconsistent:"));
    } finally {
      readClient.close();
    }
  }

  @Test
  public void readTest2() throws Exception {
    String basePath = HDFS_URI + "clientReadTest2";
    HdfsShuffleWriteHandler writeHandler1 =
        new HdfsShuffleWriteHandler("appId", 0, 0, 1, basePath, ssi1.getId(), conf);
    HdfsShuffleWriteHandler writeHandler2 =
        new HdfsShuffleWriteHandler("appId", 0, 0, 1, basePath, ssi2.getId(), conf);

    Map<Long, byte[]> expectedData = Maps.newHashMap();
    Roaring64NavigableMap blockIdBitmap = Roaring64NavigableMap.bitmapOf();
    Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf(0);
    writeTestData(writeHandler1, 2, 30, 0, expectedData, blockIdBitmap);
    writeTestData(writeHandler2, 2, 30, 0, expectedData, blockIdBitmap);

    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(),
        "appId", 0, 1, 100, 2, 10, 1000,
        basePath, blockIdBitmap, taskIdBitmap, Lists.newArrayList(ssi1, ssi2),
        new Configuration(), new DefaultIdHelper());

    TestUtils.validateResult(readClient, expectedData);
    readClient.checkProcessedBlockIds();
    readClient.close();
  }

  @Test
  public void readTest3() throws Exception {
    String basePath = HDFS_URI + "clientReadTest3";
    HdfsShuffleWriteHandler writeHandler1 =
        new HdfsShuffleWriteHandler("appId", 0, 0, 1, basePath, ssi1.getId(), conf);
    HdfsShuffleWriteHandler writeHandler2 =
        new HdfsShuffleWriteHandler("appId", 0, 0, 1, basePath, ssi2.getId(), conf);

    Map<Long, byte[]> expectedData = Maps.newHashMap();
    final Roaring64NavigableMap blockIdBitmap = Roaring64NavigableMap.bitmapOf();
    final Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf(0);
    writeTestData(writeHandler1, 2, 30, 0, expectedData, blockIdBitmap);
    writeTestData(writeHandler2, 2, 30, 0, expectedData, blockIdBitmap);

    // duplicate file created, it should be used in product environment
    String shuffleFolder = basePath + "/appId/0/0-1";
    FileUtil.copy(fs, new Path(shuffleFolder + "/" + ssi1.getId() + "_0.data"), fs,
        new Path(basePath + "/" + ssi1.getId() + ".cp.data"), false, conf);
    FileUtil.copy(fs, new Path(shuffleFolder + "/" + ssi1.getId() + "_0.index"), fs,
        new Path(basePath + "/" + ssi1.getId() + ".cp.index"), false, conf);
    FileUtil.copy(fs, new Path(shuffleFolder + "/" + ssi2.getId() + "_0.data"), fs,
        new Path(basePath + "/" + ssi2.getId() + ".cp.data"), false, conf);
    FileUtil.copy(fs, new Path(shuffleFolder + "/" + ssi2.getId() + "_0.index"), fs,
        new Path(basePath + "/" + ssi2.getId() + ".cp.index"), false, conf);

    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(),
        "appId", 0, 1, 100, 2, 10, 1000,
        basePath, blockIdBitmap, taskIdBitmap, Lists.newArrayList(ssi1, ssi2),
        new Configuration(), new DefaultIdHelper());

    TestUtils.validateResult(readClient, expectedData);
    readClient.checkProcessedBlockIds();
    readClient.close();
  }

  @Test
  public void readTest4() throws Exception {
    String basePath = HDFS_URI + "clientReadTest4";
    HdfsShuffleWriteHandler writeHandler =
        new HdfsShuffleWriteHandler("appId", 0, 0, 1, basePath, ssi1.getId(), conf);

    Map<Long, byte[]> expectedData = Maps.newHashMap();
    Roaring64NavigableMap blockIdBitmap = Roaring64NavigableMap.bitmapOf();
    Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf(0);
    writeTestData(writeHandler, 2, 30, 0, expectedData, blockIdBitmap);

    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(),
        "appId", 0, 1, 100, 2, 10, 1000,
        basePath, blockIdBitmap, taskIdBitmap, Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());
    Path dataFile = new Path(basePath + "/appId/0/0-1/" + ssi1.getId() + "_0.data");
    // data file is deleted after readClient checkExpectedBlockIds
    fs.delete(dataFile, true);

    assertNull(readClient.readShuffleBlockData());
    try {
      fs.listStatus(dataFile);
      fail("Index file should be deleted");
    } catch (Exception e) {
      // ignore
    }

    try {
      readClient.checkProcessedBlockIds();
      fail(EXPECTED_EXCEPTION_MESSAGE);
    } catch (Exception e) {
      assertTrue(e.getMessage().startsWith("Blocks read inconsistent: expected"));
    }
    readClient.close();
  }

  @Test
  public void readTest5() throws Exception {
    String basePath = HDFS_URI + "clientReadTest5";
    HdfsShuffleWriteHandler writeHandler =
        new HdfsShuffleWriteHandler("appId", 0, 0, 1, basePath, ssi1.getId(), conf);

    Map<Long, byte[]> expectedData = Maps.newHashMap();
    Roaring64NavigableMap blockIdBitmap = Roaring64NavigableMap.bitmapOf();
    Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf(0);
    writeTestData(writeHandler, 2, 30, 0, expectedData, blockIdBitmap);

    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(),
        "appId", 0, 1, 100, 2, 10, 1000,
        basePath, blockIdBitmap, taskIdBitmap, Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());
    // index file is deleted after iterator initialization, it should be ok, all index infos are read already
    Path indexFile = new Path(basePath + "/appId/0/0-1/" + ssi1.getId() + "_0.index");
    fs.delete(indexFile, true);
    readClient.close();

    assertNull(readClient.readShuffleBlockData());
  }

  @Test
  public void readTest7() throws Exception {
    String basePath = HDFS_URI + "clientReadTest7";
    HdfsShuffleWriteHandler writeHandler =
        new HdfsShuffleWriteHandler("appId", 0, 0, 1, basePath, ssi1.getId(), conf);

    Map<Long, byte[]> expectedData1 = Maps.newHashMap();
    Map<Long, byte[]> expectedData2 = Maps.newHashMap();
    final Roaring64NavigableMap blockIdBitmap1 = Roaring64NavigableMap.bitmapOf();
    final Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf(0);
    writeTestData(writeHandler, 10, 30, 0, expectedData1, blockIdBitmap1);

    Roaring64NavigableMap blockIdBitmap2 = Roaring64NavigableMap.bitmapOf();
    writeTestData(writeHandler, 10, 30, 0, expectedData2, blockIdBitmap2);

    writeTestData(writeHandler, 10, 30, 0, expectedData1, blockIdBitmap1);

    ShuffleReadClientImpl readClient1 = new ShuffleReadClientImpl(StorageType.HDFS.name(),
        "appId", 0, 0, 100, 2, 10, 100,
        basePath, blockIdBitmap1, taskIdBitmap, Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());
    final ShuffleReadClientImpl readClient2 = new ShuffleReadClientImpl(StorageType.HDFS.name(),
        "appId", 0, 1, 100, 2, 10, 100,
        basePath, blockIdBitmap2, taskIdBitmap, Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());
    TestUtils.validateResult(readClient1, expectedData1);
    readClient1.checkProcessedBlockIds();
    readClient1.close();

    TestUtils.validateResult(readClient2, expectedData2);
    readClient2.checkProcessedBlockIds();
    readClient2.close();
  }

  @Test
  public void readTest8() throws Exception {
    String basePath = HDFS_URI + "clientReadTest8";
    HdfsShuffleWriteHandler writeHandler =
        new HdfsShuffleWriteHandler("appId", 0, 0, 1, basePath, ssi1.getId(), conf);

    Map<Long, byte[]> expectedData = Maps.newHashMap();
    Roaring64NavigableMap blockIdBitmap = Roaring64NavigableMap.bitmapOf();
    Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf(0);
    writeTestData(writeHandler, 2, 30, 0, expectedData, blockIdBitmap);

    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(),
        "appId", 0, 1, 100, 2, 10, 1000,
        basePath, blockIdBitmap, taskIdBitmap, Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());
    ShuffleReadClientImpl readClient2 = new ShuffleReadClientImpl(StorageType.HDFS.name(),
        "appId", 0, 1, 100, 2, 10, 1000,
        basePath, blockIdBitmap, taskIdBitmap, Lists.newArrayList(ssi1, ssi2),
        new Configuration(), new DefaultIdHelper());
    // crc32 is incorrect
    try (MockedStatic<ChecksumUtils> checksumUtilsMock = Mockito.mockStatic(ChecksumUtils.class)) {
      checksumUtilsMock.when(() -> ChecksumUtils.getCrc32((ByteBuffer) any())).thenReturn(-1L);
      try {
        ByteBuffer bb = readClient.readShuffleBlockData().getByteBuffer();
        while (bb != null) {
          bb = readClient.readShuffleBlockData().getByteBuffer();
        }
        fail(EXPECTED_EXCEPTION_MESSAGE);
      } catch (Exception e) {
        assertTrue(e.getMessage().startsWith("Unexpected crc value"));
      }

      CompressedShuffleBlock block = readClient2.readShuffleBlockData();
      assertNull(block);
    }
    readClient.close();
    readClient2.close();
  }

  @Test
  public void readTest9() {
    // empty data
    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(),
        "appId", 0, 1, 100, 2, 10, 1000,
        "basePath", Roaring64NavigableMap.bitmapOf(), Roaring64NavigableMap.bitmapOf(),
        Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());
    assertNull(readClient.readShuffleBlockData());
    readClient.checkProcessedBlockIds();
  }

  @Test
  public void readTest10() throws Exception {
    String basePath = HDFS_URI + "clientReadTest10";
    HdfsShuffleWriteHandler writeHandler =
        new HdfsShuffleWriteHandler("appId", 0, 0, 1, basePath, ssi1.getId(), conf);

    Map<Long, byte[]> expectedData = Maps.newHashMap();
    Roaring64NavigableMap blockIdBitmap = Roaring64NavigableMap.bitmapOf();
    Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf(0);
    writeTestData(writeHandler, 5, 30, 0, expectedData, blockIdBitmap);
    Roaring64NavigableMap wrongBlockIdBitmap = Roaring64NavigableMap.bitmapOf();
    LongIterator iter = blockIdBitmap.getLongIterator();
    while (iter.hasNext()) {
      wrongBlockIdBitmap.addLong(iter.next() + (1 << Constants.TASK_ATTEMPT_ID_MAX_LENGTH));
    }

    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(),
        "appId", 0, 0, 100, 2, 10, 100,
        basePath, wrongBlockIdBitmap, taskIdBitmap, Lists.newArrayList(ssi1),
        new Configuration(), new DefaultIdHelper());
    assertNull(readClient.readShuffleBlockData());
    try {
      readClient.checkProcessedBlockIds();
      fail(EXPECTED_EXCEPTION_MESSAGE);
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Blocks read inconsistent:"));
    }
  }

  @Test
  public void readTest11() throws Exception {
    String basePath = HDFS_URI + "clientReadTest11";
    HdfsShuffleWriteHandler writeHandler =
        new HdfsShuffleWriteHandler("appId", 0, 1, 1, basePath, ssi1.getId(), conf);

    Map<Long, byte[]> expectedData = Maps.newHashMap();
    Roaring64NavigableMap blockIdBitmap = Roaring64NavigableMap.bitmapOf();
    Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf(0);
    writeTestData(writeHandler, 10, 30, 0, expectedData, blockIdBitmap);

    // test with different indexReadLimit to validate result
    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(), "appId", 0, 1, 1, 1,
        10, 1000, basePath, blockIdBitmap, taskIdBitmap,
        Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());

    TestUtils.validateResult(readClient, expectedData);
    readClient.checkProcessedBlockIds();
    readClient.close();

    readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(), "appId", 0, 1, 2, 1,
        10, 1000, basePath, blockIdBitmap, taskIdBitmap,
        Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());

    TestUtils.validateResult(readClient, expectedData);
    readClient.checkProcessedBlockIds();
    readClient.close();

    readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(), "appId", 0, 1, 3, 1,
        10, 1000, basePath, blockIdBitmap, taskIdBitmap,
        Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());

    TestUtils.validateResult(readClient, expectedData);
    readClient.checkProcessedBlockIds();
    readClient.close();

    readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(), "appId", 0, 1, 10, 1,
        10, 1000, basePath, blockIdBitmap, taskIdBitmap,
        Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());

    TestUtils.validateResult(readClient, expectedData);
    readClient.checkProcessedBlockIds();
    readClient.close();

    readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(), "appId", 0, 1, 11, 1,
        10, 1000, basePath, blockIdBitmap, taskIdBitmap,
        Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());

    TestUtils.validateResult(readClient, expectedData);
    readClient.checkProcessedBlockIds();
    readClient.close();
  }

  @Test
  public void readTest12() throws Exception {
    String basePath = HDFS_URI + "clientReadTest12";
    HdfsShuffleWriteHandler writeHandler =
        new HdfsShuffleWriteHandler("appId", 0, 1, 1, basePath, ssi1.getId(), conf);

    Map<Long, byte[]> expectedData = Maps.newHashMap();
    final Roaring64NavigableMap blockIdBitmap = Roaring64NavigableMap.bitmapOf();
    final Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf(0, 1);
    writeTestData(writeHandler, 5, 30, 0, expectedData, blockIdBitmap);
    writeTestData(writeHandler, 5, 30, 2, Maps.newHashMap(), blockIdBitmap);
    writeTestData(writeHandler, 5, 30, 1, expectedData, blockIdBitmap);

    // unexpected taskAttemptId should be filtered
    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(), "appId", 0, 1, 100, 1,
        10, 1000, basePath, blockIdBitmap, taskIdBitmap,
        Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());

    TestUtils.validateResult(readClient, expectedData);
    assertEquals(15, readClient.getProcessedBlockIds().getLongCardinality());
    readClient.checkProcessedBlockIds();
    readClient.close();
  }

  @Test
  public void readTest13() throws Exception {
    String basePath = HDFS_URI + "clientReadTest13";
    HdfsShuffleWriteHandler writeHandler =
        new HdfsShuffleWriteHandler("appId", 0, 1, 1, basePath, ssi1.getId(), conf);

    Map<Long, byte[]> expectedData = Maps.newHashMap();
    final Roaring64NavigableMap blockIdBitmap = Roaring64NavigableMap.bitmapOf();
    final Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf(0, 3);
    writeTestData(writeHandler, 5, 30, 0, expectedData, blockIdBitmap);
    // test case: data generated by speculation task without report result
    writeTestData(writeHandler, 5, 30, 1, Maps.newHashMap(), Roaring64NavigableMap.bitmapOf());
    // test case: data generated by speculation task with report result
    writeTestData(writeHandler, 5, 30, 2, Maps.newHashMap(), blockIdBitmap);
    writeTestData(writeHandler, 5, 30, 3, expectedData, blockIdBitmap);

    // unexpected taskAttemptId should be filtered
    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(), "appId", 0, 1, 100, 1,
        10, 1000, basePath, blockIdBitmap, taskIdBitmap,
        Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());

    TestUtils.validateResult(readClient, expectedData);
    assertEquals(20, readClient.getProcessedBlockIds().getLongCardinality());
    readClient.checkProcessedBlockIds();
    readClient.close();
  }

  @Test
  public void readTest14() throws Exception {
    String basePath = HDFS_URI + "clientReadTest14";
    HdfsShuffleWriteHandler writeHandler =
        new HdfsShuffleWriteHandler("appId", 0, 1, 1, basePath, ssi1.getId(), conf);

    Map<Long, byte[]> expectedData = Maps.newHashMap();
    final Roaring64NavigableMap blockIdBitmap = Roaring64NavigableMap.bitmapOf();
    final Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf(0, 2);
    writeDuplicatedData(writeHandler, 5, 30, 0, expectedData, blockIdBitmap);
    writeTestData(writeHandler, 5, 30, 1, Maps.newHashMap(), Roaring64NavigableMap.bitmapOf());
    writeTestData(writeHandler, 5, 30, 2, expectedData, blockIdBitmap);

    // unexpected taskAttemptId should be filtered
    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(), "appId", 0, 1, 100, 1,
        10, 1000, basePath, blockIdBitmap, taskIdBitmap,
        Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());

    TestUtils.validateResult(readClient, expectedData);
    assertEquals(15, readClient.getProcessedBlockIds().getLongCardinality());
    readClient.checkProcessedBlockIds();
    readClient.close();
  }

  @Test
  public void readTest15() throws Exception {
    String basePath = HDFS_URI + "clientReadTest15";
    HdfsShuffleWriteHandler writeHandler =
        new HdfsShuffleWriteHandler("appId", 0, 1, 1, basePath, ssi1.getId(), conf);

    Map<Long, byte[]> expectedData = Maps.newHashMap();
    final Roaring64NavigableMap blockIdBitmap = Roaring64NavigableMap.bitmapOf();
    final Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf(0);
    writeTestData(writeHandler, 5, 30, 0, expectedData, blockIdBitmap);
    writeTestData(writeHandler, 5, 30, 0, expectedData, blockIdBitmap);
    writeTestData(writeHandler, 5, 30, 0, Maps.newHashMap(), Roaring64NavigableMap.bitmapOf());
    writeTestData(writeHandler, 5, 30, 0, Maps.newHashMap(), Roaring64NavigableMap.bitmapOf());
    writeTestData(writeHandler, 5, 30, 0, expectedData, blockIdBitmap);
    writeTestData(writeHandler, 5, 30, 0, Maps.newHashMap(), Roaring64NavigableMap.bitmapOf());
    // unexpected taskAttemptId should be filtered
    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(), "appId", 0, 1, 100, 1,
        10, 1000, basePath, blockIdBitmap, taskIdBitmap,
        Lists.newArrayList(ssi1), new Configuration(), new DefaultIdHelper());

    TestUtils.validateResult(readClient, expectedData);
    assertEquals(25, readClient.getProcessedBlockIds().getLongCardinality());
    readClient.checkProcessedBlockIds();
    readClient.close();
  }

  private void writeTestData(
      HdfsShuffleWriteHandler writeHandler,
      int num, int length, long taskAttemptId,
      Map<Long, byte[]> expectedData,
      Roaring64NavigableMap blockIdBitmap) throws Exception {
    List<ShufflePartitionedBlock> blocks = Lists.newArrayList();
    for (int i = 0; i < num; i++) {
      byte[] buf = new byte[length];
      new Random().nextBytes(buf);
      long blockId = (ATOMIC_LONG.getAndIncrement()
          << (Constants.PARTITION_ID_MAX_LENGTH + Constants.TASK_ATTEMPT_ID_MAX_LENGTH)) + taskAttemptId;
      blocks.add(new ShufflePartitionedBlock(
          length, length, ChecksumUtils.getCrc32(buf), blockId, taskAttemptId, buf));
      expectedData.put(blockId, buf);
      blockIdBitmap.addLong(blockId);
    }
    writeHandler.write(blocks);
  }

  private void writeDuplicatedData(
      HdfsShuffleWriteHandler writeHandler,
      int num, int length, long taskAttemptId,
      Map<Long, byte[]> expectedData,
      Roaring64NavigableMap blockIdBitmap) throws Exception {
    List<ShufflePartitionedBlock> blocks = Lists.newArrayList();
    for (int i = 0; i < num; i++) {
      byte[] buf = new byte[length];
      new Random().nextBytes(buf);
      long blockId = (ATOMIC_LONG.incrementAndGet()
          << (Constants.PARTITION_ID_MAX_LENGTH + Constants.TASK_ATTEMPT_ID_MAX_LENGTH)) + taskAttemptId;
      ShufflePartitionedBlock spb = new ShufflePartitionedBlock(
          length, length, ChecksumUtils.getCrc32(buf), blockId, taskAttemptId, buf);
      blocks.add(spb);
      blocks.add(spb);
      expectedData.put(blockId, buf);
      blockIdBitmap.addLong(blockId);
    }
    writeHandler.write(blocks);
  }
}
