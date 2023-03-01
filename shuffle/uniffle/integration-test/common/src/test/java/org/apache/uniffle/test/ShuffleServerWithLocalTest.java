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
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.roaringbitmap.longlong.LongIterator;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.client.impl.grpc.ShuffleServerGrpcClient;
import org.apache.uniffle.client.request.RssFinishShuffleRequest;
import org.apache.uniffle.client.request.RssRegisterShuffleRequest;
import org.apache.uniffle.client.request.RssSendCommitRequest;
import org.apache.uniffle.client.request.RssSendShuffleDataRequest;
import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.common.ShuffleBlockInfo;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.util.ChecksumUtils;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShuffleServerWithLocalTest extends ShuffleReadWriteBase {

  private ShuffleServerGrpcClient shuffleServerClient;

  @BeforeAll
  public static void setupServers(@TempDir File tmpDir) throws Exception {
    CoordinatorConf coordinatorConf = getCoordinatorConf();
    createCoordinatorServer(coordinatorConf);
    ShuffleServerConf shuffleServerConf = getShuffleServerConf();
    File dataDir1 = new File(tmpDir, "data1");
    File dataDir2 = new File(tmpDir, "data2");
    String basePath = dataDir1.getAbsolutePath() + "," + dataDir2.getAbsolutePath();
    shuffleServerConf.setString("rss.storage.type", StorageType.LOCALFILE.name());
    shuffleServerConf.setString("rss.storage.basePath", basePath);
    shuffleServerConf.setString("rss.server.app.expired.withoutHeartbeat", "5000");
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
  public void localWriteReadTest() throws Exception {
    String testAppId = "localWriteReadTest";
    RssRegisterShuffleRequest rrsr = new RssRegisterShuffleRequest(testAppId, 0,
        Lists.newArrayList(new PartitionRange(0, 0)), "");
    shuffleServerClient.registerShuffle(rrsr);
    rrsr = new RssRegisterShuffleRequest(testAppId, 0, Lists.newArrayList(new PartitionRange(1, 1)), "");
    shuffleServerClient.registerShuffle(rrsr);
    rrsr = new RssRegisterShuffleRequest(testAppId, 0, Lists.newArrayList(new PartitionRange(2, 2)), "");
    shuffleServerClient.registerShuffle(rrsr);
    rrsr = new RssRegisterShuffleRequest(testAppId, 0, Lists.newArrayList(new PartitionRange(3, 3)), "");
    shuffleServerClient.registerShuffle(rrsr);

    Map<Long, byte[]> expectedData = Maps.newHashMap();

    Roaring64NavigableMap[] bitmaps = new Roaring64NavigableMap[4];
    Map<Integer, List<ShuffleBlockInfo>> partitionToBlocks = createTestData(bitmaps, expectedData);

    Map<Integer, Map<Integer, List<ShuffleBlockInfo>>> shuffleToBlocks = Maps.newHashMap();
    shuffleToBlocks.put(0, partitionToBlocks);

    RssSendShuffleDataRequest rssdr = new RssSendShuffleDataRequest(
        testAppId, 3, 1000, shuffleToBlocks);
    shuffleServerClient.sendShuffleData(rssdr);
    RssSendCommitRequest rscr = new RssSendCommitRequest(testAppId, 0);
    shuffleServerClient.sendCommit(rscr);
    RssFinishShuffleRequest rfsr = new RssFinishShuffleRequest(testAppId, 0);
    shuffleServerClient.finishShuffle(rfsr);

    final Set<Long> expectedBlockIds1 = transBitmapToSet(bitmaps[0]);
    final Set<Long> expectedBlockIds2 = transBitmapToSet(bitmaps[1]);
    final Set<Long> expectedBlockIds3 = transBitmapToSet(bitmaps[2]);
    final Set<Long> expectedBlockIds4 = transBitmapToSet(bitmaps[3]);
    ShuffleDataResult sdr  = readShuffleData(
        shuffleServerClient, testAppId, 0, 0, 1,
        4, 1000, 0);
    validateResult(sdr, expectedBlockIds1, expectedData, 0);
    sdr  = readShuffleData(
        shuffleServerClient, testAppId, 0, 1, 1,
        4, 1000, 0);
    validateResult(sdr, expectedBlockIds2, expectedData, 1);
    sdr  = readShuffleData(
        shuffleServerClient, testAppId, 0, 2, 1,
        4, 1000, 0);
    validateResult(sdr, expectedBlockIds3, expectedData, 2);
    sdr  = readShuffleData(
        shuffleServerClient, testAppId, 0, 3, 1,
        4, 1000, 0);
    validateResult(sdr, expectedBlockIds4, expectedData, 3);

    assertNotNull(shuffleServers.get(0).getShuffleTaskManager()
        .getPartitionsToBlockIds().get(testAppId));
    Thread.sleep(8000);
    assertNull(shuffleServers.get(0).getShuffleTaskManager().getPartitionsToBlockIds().get(testAppId));
  }

  protected void validateResult(ShuffleDataResult sdr, Set<Long> expectedBlockIds,
      Map<Long, byte[]> expectedData, long expectedTaskAttemptId) {
    byte[] buffer = sdr.getData();
    List<BufferSegment> bufferSegments = sdr.getBufferSegments();
    int matched = 0;
    for (BufferSegment bs : bufferSegments) {
      if (expectedBlockIds.contains(bs.getBlockId())) {
        byte[] data = new byte[bs.getLength()];
        System.arraycopy(buffer, bs.getOffset(), data, 0, bs.getLength());
        assertEquals(bs.getCrc(), ChecksumUtils.getCrc32(data));
        assertTrue(Arrays.equals(data, expectedData.get(bs.getBlockId())));
        assertTrue(expectedBlockIds.contains(bs.getBlockId()));
        assertEquals(expectedTaskAttemptId, bs.getTaskAttemptId());
        matched++;
      }
    }
    assertEquals(expectedBlockIds.size(), matched);
  }

  private Set<Long> transBitmapToSet(Roaring64NavigableMap blockIdBitmap) {
    Set<Long> blockIds = Sets.newHashSet();
    LongIterator iter = blockIdBitmap.getLongIterator();
    while (iter.hasNext()) {
      blockIds.add(iter.next());
    }
    return blockIds;
  }
}
