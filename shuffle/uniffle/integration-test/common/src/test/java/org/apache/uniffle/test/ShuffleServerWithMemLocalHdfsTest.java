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

package org.apache.uniffle.test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.client.impl.grpc.ShuffleServerGrpcClient;
import org.apache.uniffle.client.request.RssRegisterShuffleRequest;
import org.apache.uniffle.client.request.RssSendShuffleDataRequest;
import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.common.ShuffleBlockInfo;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.server.buffer.ShuffleBuffer;
import org.apache.uniffle.storage.handler.api.ClientReadHandler;
import org.apache.uniffle.storage.handler.impl.ComposedClientReadHandler;
import org.apache.uniffle.storage.handler.impl.HdfsClientReadHandler;
import org.apache.uniffle.storage.handler.impl.LocalFileClientReadHandler;
import org.apache.uniffle.storage.handler.impl.MemoryClientReadHandler;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ShuffleServerWithMemLocalHdfsTest extends ShuffleReadWriteBase {

  private ShuffleServerGrpcClient shuffleServerClient;
  private static String REMOTE_STORAGE = HDFS_URI + "rss/test";

  @BeforeAll
  public static void setupServers(@TempDir File tmpDir) throws Exception {
    CoordinatorConf coordinatorConf = getCoordinatorConf();
    createCoordinatorServer(coordinatorConf);
    ShuffleServerConf shuffleServerConf = getShuffleServerConf();
    File dataDir = new File(tmpDir, "data");
    String basePath = dataDir.getAbsolutePath();
    shuffleServerConf.set(ShuffleServerConf.RSS_STORAGE_BASE_PATH, Arrays.asList(basePath));
    shuffleServerConf.set(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.MEMORY_LOCALFILE_HDFS.name());
    shuffleServerConf.setLong(ShuffleServerConf.FLUSH_COLD_STORAGE_THRESHOLD_SIZE, 450L);
    shuffleServerConf.set(ShuffleServerConf.SERVER_APP_EXPIRED_WITHOUT_HEARTBEAT, 5000L);
    shuffleServerConf.set(ShuffleServerConf.SERVER_MEMORY_SHUFFLE_LOWWATERMARK_PERCENTAGE, 20.0);
    shuffleServerConf.set(ShuffleServerConf.SERVER_MEMORY_SHUFFLE_HIGHWATERMARK_PERCENTAGE, 40.0);
    shuffleServerConf.set(ShuffleServerConf.SERVER_BUFFER_CAPACITY, 500L);
    shuffleServerConf.set(ShuffleServerConf.SERVER_TRIGGER_FLUSH_CHECK_INTERVAL, 500L);
    createShuffleServer(shuffleServerConf);
    startServers();
  }

  @BeforeEach
  public void createClient() {
    shuffleServerClient = new ShuffleServerGrpcClient(LOCALHOST, SHUFFLE_SERVER_PORT);
  }

  @AfterEach
  public void closeClient() {
    shuffleServerClient.close();
  }

  @Test
  public void memoryLocalFileHDFSReadWithFilterAndSkipTest() throws Exception {
    runTest(true);
  }
  
  @Test
  public void memoryLocalFileHDFSReadWithFilterTest() throws Exception {
    runTest(false);
  }
  
  private void runTest(boolean checkSkippedMetrics) throws Exception {
    String testAppId = "memoryLocalFileHDFSReadWithFilterTest_" + "ship_" + checkSkippedMetrics;
    int shuffleId = 0;
    int partitionId = 0;
    RssRegisterShuffleRequest rrsr = new RssRegisterShuffleRequest(testAppId, 0,
        Lists.newArrayList(new PartitionRange(0, 0)), REMOTE_STORAGE);
    shuffleServerClient.registerShuffle(rrsr);
    Roaring64NavigableMap expectBlockIds = Roaring64NavigableMap.bitmapOf();
    Map<Long, byte[]> dataMap = Maps.newHashMap();
    Roaring64NavigableMap[] bitmaps = new Roaring64NavigableMap[1];
    bitmaps[0] = Roaring64NavigableMap.bitmapOf();
    List<ShuffleBlockInfo> blocks = createShuffleBlockList(
        shuffleId, partitionId, 0, 3, 25,
        expectBlockIds, dataMap, mockSSI);
    Map<Integer, List<ShuffleBlockInfo>> partitionToBlocks = Maps.newHashMap();
    partitionToBlocks.put(partitionId, blocks);
    Map<Integer, Map<Integer, List<ShuffleBlockInfo>>> shuffleToBlocks = Maps.newHashMap();
    shuffleToBlocks.put(shuffleId, partitionToBlocks);

    // send data to shuffle server's memory
    RssSendShuffleDataRequest rssdr = new RssSendShuffleDataRequest(
        testAppId, 3, 1000, shuffleToBlocks);
    shuffleServerClient.sendShuffleData(rssdr);

    Roaring64NavigableMap processBlockIds = Roaring64NavigableMap.bitmapOf();
    Roaring64NavigableMap exceptTaskIds = Roaring64NavigableMap.bitmapOf(0);
    // read the 1-th segment from memory
    MemoryClientReadHandler memoryClientReadHandler = new MemoryClientReadHandler(
        testAppId, shuffleId, partitionId, 150, shuffleServerClient, exceptTaskIds);
    LocalFileClientReadHandler localFileClientReadHandler = new LocalFileClientReadHandler(
        testAppId, shuffleId, partitionId, 0, 1, 3,
        75, expectBlockIds, processBlockIds, shuffleServerClient);
    HdfsClientReadHandler hdfsClientReadHandler = new HdfsClientReadHandler(testAppId, shuffleId, partitionId, 0, 1, 3,
        500, expectBlockIds, processBlockIds, REMOTE_STORAGE, conf);
    ClientReadHandler[] handlers = new ClientReadHandler[3];
    handlers[0] = memoryClientReadHandler;
    handlers[1] = localFileClientReadHandler;
    handlers[2] = hdfsClientReadHandler;
    ShuffleServerInfo ssi = new ShuffleServerInfo(LOCALHOST, SHUFFLE_SERVER_PORT);
    ComposedClientReadHandler composedClientReadHandler = new ComposedClientReadHandler(
        ssi, handlers);
    Map<Long, byte[]> expectedData = Maps.newHashMap();
    expectedData.clear();
    expectedData.put(blocks.get(0).getBlockId(), blocks.get(0).getData());
    expectedData.put(blocks.get(1).getBlockId(), blocks.get(1).getData());
    expectedData.put(blocks.get(2).getBlockId(), blocks.get(1).getData());
    ShuffleDataResult sdr  = composedClientReadHandler.readShuffleData();
    validateResult(expectedData, sdr);
    processBlockIds.addLong(blocks.get(0).getBlockId());
    processBlockIds.addLong(blocks.get(1).getBlockId());
    processBlockIds.addLong(blocks.get(2).getBlockId());
    sdr.getBufferSegments().forEach(bs -> composedClientReadHandler.updateConsumedBlockInfo(bs, checkSkippedMetrics));

    // send data to shuffle server, and wait until flush to LocalFile
    List<ShuffleBlockInfo> blocks2 = createShuffleBlockList(
        shuffleId, partitionId, 0, 3, 50,
        expectBlockIds, dataMap, mockSSI);
    partitionToBlocks = Maps.newHashMap();
    partitionToBlocks.put(partitionId, blocks2);
    shuffleToBlocks = Maps.newHashMap();
    shuffleToBlocks.put(shuffleId, partitionToBlocks);
    rssdr = new RssSendShuffleDataRequest(
        testAppId, 3, 1000, shuffleToBlocks);
    shuffleServerClient.sendShuffleData(rssdr);
    waitFlush(testAppId, shuffleId);

    // read the 2-th segment from localFile
    // notice: the 1-th segment is skipped, because it is processed
    sdr  = composedClientReadHandler.readShuffleData();
    expectedData.clear();
    expectedData.put(blocks2.get(0).getBlockId(), blocks2.get(0).getData());
    expectedData.put(blocks2.get(1).getBlockId(), blocks2.get(1).getData());
    validateResult(expectedData, sdr);
    processBlockIds.addLong(blocks2.get(0).getBlockId());
    processBlockIds.addLong(blocks2.get(1).getBlockId());
    sdr.getBufferSegments().forEach(bs -> composedClientReadHandler.updateConsumedBlockInfo(bs, checkSkippedMetrics));

    // read the 3-th segment from localFile
    sdr  = composedClientReadHandler.readShuffleData();
    expectedData.clear();
    expectedData.put(blocks2.get(2).getBlockId(), blocks2.get(2).getData());
    validateResult(expectedData, sdr);
    processBlockIds.addLong(blocks2.get(2).getBlockId());
    sdr.getBufferSegments().forEach(bs -> composedClientReadHandler.updateConsumedBlockInfo(bs, checkSkippedMetrics));

    // send data to shuffle server, and wait until flush to HDFS
    List<ShuffleBlockInfo> blocks3 = createShuffleBlockList(
        shuffleId, partitionId, 0, 2, 200,
        expectBlockIds, dataMap, mockSSI);
    partitionToBlocks = Maps.newHashMap();
    partitionToBlocks.put(partitionId, blocks3);
    shuffleToBlocks = Maps.newHashMap();
    shuffleToBlocks.put(shuffleId, partitionToBlocks);
    rssdr = new RssSendShuffleDataRequest(
        testAppId, 3, 1000, shuffleToBlocks);
    shuffleServerClient.sendShuffleData(rssdr);
    waitFlush(testAppId, shuffleId);

    // read the 4-th segment from HDFS
    sdr  = composedClientReadHandler.readShuffleData();
    expectedData.clear();
    expectedData.put(blocks3.get(0).getBlockId(), blocks3.get(0).getData());
    expectedData.put(blocks3.get(1).getBlockId(), blocks3.get(1).getData());
    validateResult(expectedData, sdr);
    processBlockIds.addLong(blocks3.get(0).getBlockId());
    processBlockIds.addLong(blocks3.get(1).getBlockId());
    sdr.getBufferSegments().forEach(bs -> composedClientReadHandler.updateConsumedBlockInfo(bs, checkSkippedMetrics));

    // all segments are processed
    sdr  = composedClientReadHandler.readShuffleData();
    assertNull(sdr);

    if (checkSkippedMetrics) {
      String readBlockNumInfo = composedClientReadHandler.getReadBlockNumInfo();
      assertTrue(readBlockNumInfo.contains("Client read 0 blocks from [" + ssi + "]"));
      assertTrue(readBlockNumInfo.contains("Skipped[ hot:3 warm:3 cold:2 frozen:0 ]"));
      assertTrue(readBlockNumInfo.contains("Consumed[ hot:0 warm:0 cold:0 frozen:0 ]"));
      String readLengthInfo = composedClientReadHandler.getReadLengthInfo();
      assertTrue(readLengthInfo.contains("Client read 0 bytes from [" + ssi + "]"));
      assertTrue(readLengthInfo.contains("Skipped[ hot:75 warm:150 cold:400 frozen:0 ]"));
      assertTrue(readBlockNumInfo.contains("Consumed[ hot:0 warm:0 cold:0 frozen:0 ]"));
      String readUncompressLengthInfo = composedClientReadHandler.getReadUncompressLengthInfo();
      assertTrue(readUncompressLengthInfo.contains("Client read 0 uncompressed bytes from [" + ssi + "]"));
      assertTrue(readUncompressLengthInfo.contains("Skipped[ hot:75 warm:150 cold:400 frozen:0 ]"));
      assertTrue(readBlockNumInfo.contains("Consumed[ hot:0 warm:0 cold:0 frozen:0 ]"));
    } else {
      String readBlockNumInfo = composedClientReadHandler.getReadBlockNumInfo();
      assertTrue(readBlockNumInfo.contains("Client read 8 blocks from [" + ssi + "]"));
      assertTrue(readBlockNumInfo.contains("Consumed[ hot:3 warm:3 cold:2 frozen:0 ]"));
      assertTrue(readBlockNumInfo.contains("Skipped[ hot:0 warm:0 cold:0 frozen:0 ]"));
      String readLengthInfo = composedClientReadHandler.getReadLengthInfo();
      assertTrue(readLengthInfo.contains("Client read 625 bytes from [" + ssi + "]"));
      assertTrue(readLengthInfo.contains("Consumed[ hot:75 warm:150 cold:400 frozen:0 ]"));
      assertTrue(readBlockNumInfo.contains("Skipped[ hot:0 warm:0 cold:0 frozen:0 ]"));
      String readUncompressLengthInfo = composedClientReadHandler.getReadUncompressLengthInfo();
      assertTrue(readUncompressLengthInfo.contains("Client read 625 uncompressed bytes from [" + ssi + "]"));
      assertTrue(readUncompressLengthInfo.contains("Consumed[ hot:75 warm:150 cold:400 frozen:0 ]"));
      assertTrue(readBlockNumInfo.contains("Skipped[ hot:0 warm:0 cold:0 frozen:0 ]"));
    }
    
  }

  protected void waitFlush(String appId, int shuffleId) throws InterruptedException {
    int retry = 0;
    while (true) {
      if (retry > 5) {
        fail("Timeout for flush data");
      }
      ShuffleBuffer shuffleBuffer = shuffleServers.get(0).getShuffleBufferManager()
          .getShuffleBuffer(appId, shuffleId, 0);
      if (shuffleBuffer.getBlocks().size() == 0 && shuffleBuffer.getInFlushBlockMap().size() == 0) {
        break;
      }
      Thread.sleep(1000);
      retry++;
    }
  }

  protected void validateResult(
      Map<Long, byte[]> expectedData,
      ShuffleDataResult sdr) {
    byte[] buffer = sdr.getData();
    List<BufferSegment> bufferSegments = sdr.getBufferSegments();
    assertEquals(expectedData.size(), bufferSegments.size());
    for (Map.Entry<Long, byte[]> entry : expectedData.entrySet()) {
      BufferSegment bs = findBufferSegment(entry.getKey(), bufferSegments);
      assertNotNull(bs);
      byte[] data = new byte[bs.getLength()];
      System.arraycopy(buffer, bs.getOffset(), data, 0, bs.getLength());
    }
  }

  private BufferSegment findBufferSegment(long blockId, List<BufferSegment> bufferSegments) {
    for (BufferSegment bs : bufferSegments) {
      if (bs.getBlockId() == blockId) {
        return bs;
      }
    }
    return null;
  }
}
