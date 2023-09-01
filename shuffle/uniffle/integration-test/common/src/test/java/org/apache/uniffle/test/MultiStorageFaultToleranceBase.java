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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.client.factory.ShuffleServerClientFactory;
import org.apache.uniffle.client.impl.ShuffleReadClientImpl;
import org.apache.uniffle.client.impl.grpc.ShuffleServerGrpcClient;
import org.apache.uniffle.client.request.RssFinishShuffleRequest;
import org.apache.uniffle.client.request.RssRegisterShuffleRequest;
import org.apache.uniffle.client.request.RssReportShuffleResultRequest;
import org.apache.uniffle.client.request.RssSendCommitRequest;
import org.apache.uniffle.client.request.RssSendShuffleDataRequest;
import org.apache.uniffle.client.response.CompressedShuffleBlock;
import org.apache.uniffle.client.util.DefaultIdHelper;
import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.common.ShuffleBlockInfo;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class MultiStorageFaultToleranceBase extends ShuffleReadWriteBase {
  private ShuffleServerGrpcClient shuffleServerClient;
  private static String REMOTE_STORAGE = HDFS_URI + "rss/multi_storage_fault";

  @BeforeEach
  public void createClient() {
    ShuffleServerClientFactory.getInstance().cleanupCache();
    shuffleServerClient = new ShuffleServerGrpcClient(LOCALHOST, SHUFFLE_SERVER_PORT);
  }

  @AfterEach
  public void closeClient() {
    shuffleServerClient.close();
  }

  abstract void makeChaos();

  @Test
  public void fallbackTest() throws Exception {
    String appId = "fallback_test_" + this.getClass().getSimpleName();
    Map<Long, byte[]> expectedData = Maps.newHashMap();
    Map<Integer, List<Integer>> map = Maps.newHashMap();
    map.put(0, Lists.newArrayList(0));
    registerShuffle(appId, map);
    Roaring64NavigableMap blockBitmap = Roaring64NavigableMap.bitmapOf();
    final List<ShuffleBlockInfo> blocks = createShuffleBlockList(
        0, 0, 0, 40, 2 * 1024 * 1024, blockBitmap, expectedData);
    makeChaos();
    sendSinglePartitionToShuffleServer(appId, 0, 0, 0, blocks);
    validateResult(appId, 0, 0, blockBitmap, Roaring64NavigableMap.bitmapOf(0), expectedData);
  }

  private void registerShuffle(String appId, Map<Integer, List<Integer>> registerMap) {
    for (Map.Entry<Integer, List<Integer>> entry : registerMap.entrySet()) {
      for (int partition : entry.getValue()) {
        RssRegisterShuffleRequest rr = new RssRegisterShuffleRequest(appId, entry.getKey(),
            Lists.newArrayList(new PartitionRange(partition, partition)), REMOTE_STORAGE);
        shuffleServerClient.registerShuffle(rr);
      }
    }
  }

  private void sendSinglePartitionToShuffleServer(String appId, int shuffle, int partition,
                                                  long taskAttemptId, List<ShuffleBlockInfo> blocks) {
    Map<Integer, List<ShuffleBlockInfo>> partitionToBlocks = Maps.newHashMap();
    Map<Integer, Map<Integer, List<ShuffleBlockInfo>>> shuffleToBlocks = Maps.newHashMap();
    partitionToBlocks.put(partition, blocks);
    shuffleToBlocks.put(shuffle, partitionToBlocks);
    RssSendShuffleDataRequest rs = new RssSendShuffleDataRequest(appId, 3, 1000, shuffleToBlocks);
    shuffleServerClient.sendShuffleData(rs);
    RssSendCommitRequest rc = new RssSendCommitRequest(appId, shuffle);
    shuffleServerClient.sendCommit(rc);
    RssFinishShuffleRequest rf = new RssFinishShuffleRequest(appId, shuffle);
    shuffleServerClient.finishShuffle(rf);

    Map<Integer, List<Long>> partitionToBlockIds = Maps.newHashMap();
    Set<Long> expectBlockIds = getExpectBlockIds(blocks);
    partitionToBlockIds.put(shuffle, new ArrayList<>(expectBlockIds));
    RssReportShuffleResultRequest rrp = new RssReportShuffleResultRequest(
        appId, shuffle, taskAttemptId, partitionToBlockIds, 1);
    shuffleServerClient.reportShuffleResult(rrp);
  }

  protected void validateResult(String appId, int shuffleId, int partitionId, Roaring64NavigableMap blockBitmap,
                                Roaring64NavigableMap taskBitmap, Map<Long, byte[]> expectedData) {
    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(StorageType.LOCALFILE_HDFS.name(),
        appId, shuffleId, partitionId, 100, 1, 10, 1000, REMOTE_STORAGE, blockBitmap, taskBitmap,
        Lists.newArrayList(new ShuffleServerInfo(LOCALHOST, SHUFFLE_SERVER_PORT)), conf, new DefaultIdHelper());
    CompressedShuffleBlock csb = readClient.readShuffleBlockData();
    Roaring64NavigableMap matched = Roaring64NavigableMap.bitmapOf();
    while (csb != null && csb.getByteBuffer() != null) {
      for (Map.Entry<Long, byte[]> entry : expectedData.entrySet()) {
        if (compareByte(entry.getValue(), csb.getByteBuffer())) {
          matched.addLong(entry.getKey());
          break;
        }
      }
      csb = readClient.readShuffleBlockData();
    }
    assertTrue(blockBitmap.equals(matched));
  }

  private Set<Long> getExpectBlockIds(List<ShuffleBlockInfo> blocks) {
    List<Long> expectBlockIds = Lists.newArrayList();
    blocks.forEach(b -> expectBlockIds.add(b.getBlockId()));
    return Sets.newHashSet(expectBlockIds);
  }
}
