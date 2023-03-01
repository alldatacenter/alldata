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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.client.impl.ShuffleReadClientImpl;
import org.apache.uniffle.client.impl.grpc.ShuffleServerGrpcClient;
import org.apache.uniffle.client.request.RssFinishShuffleRequest;
import org.apache.uniffle.client.request.RssRegisterShuffleRequest;
import org.apache.uniffle.client.request.RssSendCommitRequest;
import org.apache.uniffle.client.request.RssSendShuffleDataRequest;
import org.apache.uniffle.client.response.CompressedShuffleBlock;
import org.apache.uniffle.client.util.DefaultIdHelper;
import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.common.ShuffleBlockInfo;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShuffleServerWithHdfsTest extends ShuffleReadWriteBase {

  protected ShuffleServerGrpcClient shuffleServerClient;

  @BeforeAll
  public static void setupServers() throws Exception {
    CoordinatorConf coordinatorConf = getCoordinatorConf();
    createCoordinatorServer(coordinatorConf);
    ShuffleServerConf shuffleServerConf = getShuffleServerConf();
    shuffleServerConf.setString(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.HDFS.name());
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
  public void hdfsWriteReadTest() {
    String appId = "app_hdfs_read_write";
    String dataBasePath = HDFS_URI + "rss/test";
    RssRegisterShuffleRequest rrsr = new RssRegisterShuffleRequest(appId, 0,
        Lists.newArrayList(new PartitionRange(0, 1)), dataBasePath);
    shuffleServerClient.registerShuffle(rrsr);
    rrsr = new RssRegisterShuffleRequest(appId, 0, Lists.newArrayList(new PartitionRange(2, 3)), dataBasePath);
    shuffleServerClient.registerShuffle(rrsr);

    Roaring64NavigableMap[] bitmaps = new Roaring64NavigableMap[4];
    Map<Long, byte[]> expectedData = Maps.newHashMap();
    Map<Integer, List<ShuffleBlockInfo>>  dataBlocks = createTestData(bitmaps, expectedData);
    Map<Integer, List<ShuffleBlockInfo>> partitionToBlocks = Maps.newHashMap();
    partitionToBlocks.put(0, dataBlocks.get(0));
    partitionToBlocks.put(1, dataBlocks.get(1));

    Map<Integer, Map<Integer, List<ShuffleBlockInfo>>> shuffleToBlocks = Maps.newHashMap();
    shuffleToBlocks.put(0, partitionToBlocks);

    RssSendShuffleDataRequest rssdr = new RssSendShuffleDataRequest(appId, 3, 1000, shuffleToBlocks);
    shuffleServerClient.sendShuffleData(rssdr);
    assertEquals(456, shuffleServers.get(0).getShuffleBufferManager().getUsedMemory());
    assertEquals(0, shuffleServers.get(0).getShuffleBufferManager().getPreAllocatedSize());
    RssSendCommitRequest rscr = new RssSendCommitRequest(appId, 0);
    shuffleServerClient.sendCommit(rscr);
    RssFinishShuffleRequest rfsr = new RssFinishShuffleRequest(appId, 0);

    ShuffleServerInfo ssi = new ShuffleServerInfo(LOCALHOST, SHUFFLE_SERVER_PORT);
    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(),
        appId, 0, 0, 100, 2, 10, 1000,
        dataBasePath, bitmaps[0], Roaring64NavigableMap.bitmapOf(0), Lists.newArrayList(ssi),
        new Configuration(), new DefaultIdHelper());
    assertNull(readClient.readShuffleBlockData());
    shuffleServerClient.finishShuffle(rfsr);

    partitionToBlocks.clear();
    partitionToBlocks.put(2, dataBlocks.get(2));
    shuffleToBlocks.clear();
    shuffleToBlocks.put(0, partitionToBlocks);
    rssdr = new RssSendShuffleDataRequest(appId, 3, 1000, shuffleToBlocks);
    shuffleServerClient.sendShuffleData(rssdr);
    assertEquals(0, shuffleServers.get(0).getShuffleBufferManager().getPreAllocatedSize());
    rscr = new RssSendCommitRequest(appId, 0);
    shuffleServerClient.sendCommit(rscr);
    rfsr = new RssFinishShuffleRequest(appId, 0);
    shuffleServerClient.finishShuffle(rfsr);

    partitionToBlocks.clear();
    partitionToBlocks.put(3, dataBlocks.get(3));
    shuffleToBlocks.clear();
    shuffleToBlocks.put(0, partitionToBlocks);
    rssdr = new RssSendShuffleDataRequest(appId, 3, 1000, shuffleToBlocks);
    shuffleServerClient.sendShuffleData(rssdr);
    rscr = new RssSendCommitRequest(appId, 0);
    shuffleServerClient.sendCommit(rscr);
    rfsr = new RssFinishShuffleRequest(appId, 0);
    shuffleServerClient.finishShuffle(rfsr);

    readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(),
        appId, 0, 0, 100, 2, 10, 1000,
        dataBasePath, bitmaps[0], Roaring64NavigableMap.bitmapOf(0), Lists.newArrayList(ssi),
        new Configuration(), new DefaultIdHelper());
    validateResult(readClient, expectedData, bitmaps[0]);

    readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(),
        appId, 0, 1, 100, 2, 10, 1000,
        dataBasePath, bitmaps[1], Roaring64NavigableMap.bitmapOf(1), Lists.newArrayList(ssi),
        new Configuration(), new DefaultIdHelper());
    validateResult(readClient, expectedData, bitmaps[1]);

    readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(),
        appId, 0, 2, 100, 2, 10, 1000,
        dataBasePath, bitmaps[2], Roaring64NavigableMap.bitmapOf(2), Lists.newArrayList(ssi),
        new Configuration(), new DefaultIdHelper());
    validateResult(readClient, expectedData, bitmaps[2]);

    readClient = new ShuffleReadClientImpl(StorageType.HDFS.name(),
        appId, 0, 3, 100, 2, 10, 1000,
        dataBasePath, bitmaps[3], Roaring64NavigableMap.bitmapOf(3), Lists.newArrayList(ssi),
        new Configuration(), new DefaultIdHelper());
    validateResult(readClient, expectedData, bitmaps[3]);
  }

  protected void validateResult(ShuffleReadClientImpl readClient, Map<Long, byte[]> expectedData,
      Roaring64NavigableMap blockIdBitmap) {
    CompressedShuffleBlock csb = readClient.readShuffleBlockData();
    Roaring64NavigableMap matched = Roaring64NavigableMap.bitmapOf();
    while (csb != null && csb.getByteBuffer() != null) {
      for (Entry<Long, byte[]> entry : expectedData.entrySet()) {
        if (compareByte(entry.getValue(), csb.getByteBuffer())) {
          matched.addLong(entry.getKey());
          break;
        }
      }
      csb = readClient.readShuffleBlockData();
    }
    assertTrue(blockIdBitmap.equals(matched));
  }
}
