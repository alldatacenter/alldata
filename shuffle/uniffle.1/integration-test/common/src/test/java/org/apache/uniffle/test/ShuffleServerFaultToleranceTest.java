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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.client.TestUtils;
import org.apache.uniffle.client.api.ShuffleServerClient;
import org.apache.uniffle.client.impl.grpc.ShuffleServerGrpcClient;
import org.apache.uniffle.client.request.RssRegisterShuffleRequest;
import org.apache.uniffle.client.request.RssSendCommitRequest;
import org.apache.uniffle.client.request.RssSendShuffleDataRequest;
import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.common.ShuffleBlockInfo;
import org.apache.uniffle.common.ShuffleDataDistributionType;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.coordinator.CoordinatorServer;
import org.apache.uniffle.server.MockedShuffleServer;
import org.apache.uniffle.server.ShuffleServer;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.server.buffer.ShuffleBuffer;
import org.apache.uniffle.storage.factory.ShuffleHandlerFactory;
import org.apache.uniffle.storage.handler.ClientReadHandlerMetric;
import org.apache.uniffle.storage.handler.impl.AbstractClientReadHandler;
import org.apache.uniffle.storage.request.CreateShuffleReadHandlerRequest;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShuffleServerFaultToleranceTest extends ShuffleReadWriteBase {

  private List<ShuffleServerClient> shuffleServerClients;

  private String remoteStoragePath = HDFS_URI + "rss/test";

  @BeforeEach
  public void setupServers(@TempDir File tmpDir) throws Exception {
    CoordinatorConf coordinatorConf = getCoordinatorConf();
    createCoordinatorServer(coordinatorConf);
    shuffleServers.add(createServer(0, tmpDir));
    shuffleServers.add(createServer(1, tmpDir));
    shuffleServers.add(createServer(2, tmpDir));
    startServers();
    shuffleServerClients = new ArrayList<>();
    for (ShuffleServer shuffleServer : shuffleServers) {
      shuffleServerClients.add(new ShuffleServerGrpcClient(shuffleServer.getIp(), shuffleServer.getPort()));
    }
  }

  @AfterEach
  public void cleanEnv() throws Exception {
    shuffleServerClients.forEach((client) -> {
      client.close();
    });
    cleanCluster();
  }

  @Test
  public void testReadFaultTolerance() throws Exception {
    String testAppId = "ShuffleServerFaultToleranceTest.testReadFaultTolerance";
    int shuffleId = 0;
    int partitionId = 0;
    RssRegisterShuffleRequest rrsr = new RssRegisterShuffleRequest(testAppId, shuffleId,
        Lists.newArrayList(new PartitionRange(0, 0)), remoteStoragePath);
    registerShuffle(rrsr);
    Roaring64NavigableMap expectBlockIds = Roaring64NavigableMap.bitmapOf();
    Map<Long, byte[]> dataMap = Maps.newHashMap();
    Roaring64NavigableMap[] bitmaps = new Roaring64NavigableMap[1];
    bitmaps[0] = Roaring64NavigableMap.bitmapOf();
    List<ShuffleBlockInfo> blocks = createShuffleBlockList(
        shuffleId, partitionId, 0, 3, 25,
        expectBlockIds, dataMap, mockSSI);

    RssSendShuffleDataRequest rssdr = getRssSendShuffleDataRequest(testAppId, shuffleId, partitionId, blocks);
    shuffleServerClients.get(1).sendShuffleData(rssdr);

    List<ShuffleServerInfo> shuffleServerInfoList = new ArrayList<>();
    for (ShuffleServer shuffleServer : shuffleServers) {
      shuffleServerInfoList.add(new ShuffleServerInfo(shuffleServer.getId(),
          shuffleServer.getIp(), shuffleServer.getPort()));
    }

    CreateShuffleReadHandlerRequest request = mockCreateShuffleReadHandlerRequest(
        testAppId, shuffleId, partitionId, shuffleServerInfoList, expectBlockIds, StorageType.MEMORY_LOCALFILE);
    AbstractClientReadHandler clientReadHandler = 
        (AbstractClientReadHandler) ShuffleHandlerFactory.getInstance().createShuffleReadHandler(request);
    Map<Long, byte[]> expectedData = Maps.newHashMap();
    expectedData.clear();
    blocks.forEach((block) -> {
      expectedData.put(block.getBlockId(), block.getData());
    });
    ShuffleDataResult sdr = clientReadHandler.readShuffleData();
    TestUtils.validateResult(expectedData, sdr);
    for (BufferSegment bs : sdr.getBufferSegments()) {
      clientReadHandler.updateConsumedBlockInfo(bs, false);
    }
    ClientReadHandlerMetric exceptMetric = mock(ClientReadHandlerMetric.class);
    when(exceptMetric.getReadBlockNum()).thenReturn(3L);
    when(exceptMetric.getReadLength()).thenReturn(75L);
    when(exceptMetric.getReadUncompressLength()).thenReturn(75L);
    ClientReadHandlerMetric readHandlerMetric = clientReadHandler.getReadHandlerMetric();
    assertTrue(readHandlerMetric.equals(exceptMetric));
    // send data to shuffle server, and wait until flush to localfile
    List<ShuffleBlockInfo> blocks2 = createShuffleBlockList(
        shuffleId, partitionId, 0, 3, 25,
        expectBlockIds, dataMap, mockSSI);

    rssdr = getRssSendShuffleDataRequest(testAppId, shuffleId, partitionId, blocks2);
    shuffleServerClients.get(1).sendShuffleData(rssdr);
    RssSendCommitRequest commitRequest = new RssSendCommitRequest(testAppId, shuffleId);
    shuffleServerClients.get(1).sendCommit(commitRequest);
    waitFlush(testAppId, shuffleId);
    request = mockCreateShuffleReadHandlerRequest(
        testAppId, shuffleId, partitionId, shuffleServerInfoList, expectBlockIds, StorageType.LOCALFILE);
    clientReadHandler = (AbstractClientReadHandler)
        ShuffleHandlerFactory.getInstance().createShuffleReadHandler(request);
    sdr = clientReadHandler.readShuffleData();
    blocks2.forEach((block) -> {
      expectedData.put(block.getBlockId(), block.getData());
    });
    TestUtils.validateResult(expectedData, sdr);
    for (BufferSegment bs : sdr.getBufferSegments()) {
      clientReadHandler.updateConsumedBlockInfo(bs, false);
    }
    readHandlerMetric = clientReadHandler.getReadHandlerMetric();
    exceptMetric = mock(ClientReadHandlerMetric.class);
    when(exceptMetric.getReadBlockNum()).thenReturn(6L);
    when(exceptMetric.getReadLength()).thenReturn(150L);
    when(exceptMetric.getReadUncompressLength()).thenReturn(150L);
    assertTrue(readHandlerMetric.equals(exceptMetric));
    // send data to shuffle server, and wait until flush to hdfs
    List<ShuffleBlockInfo> blocks3 = createShuffleBlockList(
        shuffleId, partitionId, 0, 3, 150,
        expectBlockIds, dataMap, mockSSI);
    expectedData.clear();
    blocks3.forEach((block) -> {
      expectedData.put(block.getBlockId(), block.getData());
    });
    rssdr = getRssSendShuffleDataRequest(testAppId, shuffleId, partitionId, blocks3);
    shuffleServerClients.get(1).sendShuffleData(rssdr);
    shuffleServerClients.get(1).sendCommit(commitRequest);
    waitFlush(testAppId, shuffleId);
    request = mockCreateShuffleReadHandlerRequest(
        testAppId, shuffleId, partitionId, shuffleServerInfoList, expectBlockIds, StorageType.HDFS);
    clientReadHandler = (AbstractClientReadHandler)
        ShuffleHandlerFactory.getInstance().createShuffleReadHandler(request);
    sdr = clientReadHandler.readShuffleData();
    TestUtils.validateResult(expectedData, sdr);
    for (BufferSegment bs : sdr.getBufferSegments()) {
      clientReadHandler.updateConsumedBlockInfo(bs, false);
    }
    readHandlerMetric = clientReadHandler.getReadHandlerMetric();
    exceptMetric = mock(ClientReadHandlerMetric.class);
    when(exceptMetric.getReadBlockNum()).thenReturn(3L);
    when(exceptMetric.getReadLength()).thenReturn(450L);
    when(exceptMetric.getReadUncompressLength()).thenReturn(450L);
    assertTrue(readHandlerMetric.equals(exceptMetric));
  }

  private CreateShuffleReadHandlerRequest mockCreateShuffleReadHandlerRequest(
      String testAppId, int shuffleId, int partitionId, List<ShuffleServerInfo> shuffleServerInfoList,
      Roaring64NavigableMap expectBlockIds, StorageType storageType) {
    CreateShuffleReadHandlerRequest request = new CreateShuffleReadHandlerRequest();
    request.setStorageType(storageType.name());
    request.setAppId(testAppId);
    request.setShuffleId(shuffleId);
    request.setPartitionId(partitionId);
    request.setIndexReadLimit(100);
    request.setPartitionNumPerRange(1);
    request.setPartitionNum(1);
    request.setReadBufferSize(14 * 1024 * 1024);
    request.setStorageBasePath(remoteStoragePath);
    request.setShuffleServerInfoList(shuffleServerInfoList);
    request.setHadoopConf(conf);
    request.setExpectBlockIds(expectBlockIds);
    Roaring64NavigableMap processBlockIds = Roaring64NavigableMap.bitmapOf();
    request.setProcessBlockIds(processBlockIds);
    request.setDistributionType(ShuffleDataDistributionType.NORMAL);
    Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf(0);
    request.setExpectTaskIds(taskIdBitmap);
    return request;
  }

  private RssSendShuffleDataRequest getRssSendShuffleDataRequest(
      String appId, int shuffleId, int partitionId, List<ShuffleBlockInfo> blocks) {
    Map<Integer, List<ShuffleBlockInfo>> partitionToBlocks = Maps.newHashMap();
    partitionToBlocks.put(partitionId, blocks);
    Map<Integer, Map<Integer, List<ShuffleBlockInfo>>> shuffleToBlocks = Maps.newHashMap();
    shuffleToBlocks.put(shuffleId, partitionToBlocks);
    return new RssSendShuffleDataRequest(
        appId, 3, 1000, shuffleToBlocks);
  }

  private void registerShuffle(RssRegisterShuffleRequest rrsr) {
    shuffleServerClients.forEach((client) -> {
      client.registerShuffle(rrsr);
    });
  }

  public static MockedShuffleServer createServer(int id, File tmpDir) throws Exception {
    ShuffleServerConf shuffleServerConf = getShuffleServerConf();
    shuffleServerConf.set(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.LOCALFILE.name());
    shuffleServerConf.set(ShuffleServerConf.SERVER_APP_EXPIRED_WITHOUT_HEARTBEAT, 5000L);
    shuffleServerConf.set(ShuffleServerConf.SERVER_MEMORY_SHUFFLE_LOWWATERMARK_PERCENTAGE, 20.0);
    shuffleServerConf.set(ShuffleServerConf.SERVER_MEMORY_SHUFFLE_HIGHWATERMARK_PERCENTAGE, 40.0);
    shuffleServerConf.set(ShuffleServerConf.SERVER_BUFFER_CAPACITY, 600L);
    shuffleServerConf.set(ShuffleServerConf.SERVER_APP_EXPIRED_WITHOUT_HEARTBEAT, 5000L);
    shuffleServerConf.set(ShuffleServerConf.DISK_CAPACITY, 1000000L);
    shuffleServerConf.setLong("rss.server.heartbeat.interval", 5000);
    File dataDir1 = new File(tmpDir, id + "_1");
    File dataDir2 = new File(tmpDir, id + "_2");
    String basePath = dataDir1.getAbsolutePath() + "," + dataDir2.getAbsolutePath();
    shuffleServerConf.set(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.MEMORY_LOCALFILE_HDFS.name());
    shuffleServerConf.setLong(ShuffleServerConf.FLUSH_COLD_STORAGE_THRESHOLD_SIZE, 450L);
    shuffleServerConf.setInteger("rss.rpc.server.port", SHUFFLE_SERVER_PORT + 20 + id);
    shuffleServerConf.setInteger("rss.jetty.http.port", 19081 + id * 100);
    shuffleServerConf.setString("rss.storage.basePath", basePath);
    return new MockedShuffleServer(shuffleServerConf);
  }

  protected void waitFlush(String appId, int shuffleId) throws InterruptedException {
    int retry = 0;
    while (true) {
      if (retry > 5) {
        fail("Timeout for flush data");
      }
      ShuffleBuffer shuffleBuffer = shuffleServers.get(1).getShuffleBufferManager()
          .getShuffleBuffer(appId, shuffleId, 0);
      if (shuffleBuffer.getBlocks().size() == 0 && shuffleBuffer.getInFlushBlockMap().size() == 0) {
        break;
      }
      Thread.sleep(1000);
      retry++;
    }
  }

  public static void cleanCluster() throws Exception {
    for (CoordinatorServer coordinator : coordinators) {
      coordinator.stopServer();
    }
    for (ShuffleServer shuffleServer : shuffleServers) {
      shuffleServer.stopServer();
    }
    shuffleServers = Lists.newArrayList();
    coordinators = Lists.newArrayList();
  }
}
