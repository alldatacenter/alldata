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
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.client.impl.ShuffleReadClientImpl;
import org.apache.uniffle.client.impl.grpc.ShuffleServerGrpcClient;
import org.apache.uniffle.client.request.RssFinishShuffleRequest;
import org.apache.uniffle.client.request.RssRegisterShuffleRequest;
import org.apache.uniffle.client.request.RssSendCommitRequest;
import org.apache.uniffle.client.request.RssSendShuffleDataRequest;
import org.apache.uniffle.client.util.DefaultIdHelper;
import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.common.ShuffleBlockInfo;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DiskErrorToleranceTest extends ShuffleReadWriteBase {
  private ShuffleServerGrpcClient shuffleServerClient;

  @TempDir private static File serverTmpDir;
  private static File data1 = new File(serverTmpDir, "data1");
  private static File data2 = new File(serverTmpDir, "data2");
  private List<ShuffleServerInfo> shuffleServerInfo =
      Lists.newArrayList(new ShuffleServerInfo("127.0.0.1-20001", LOCALHOST, SHUFFLE_SERVER_PORT));

  @BeforeAll
  public static void setupServers() throws Exception {
    CoordinatorConf coordinatorConf = getCoordinatorConf();
    createCoordinatorServer(coordinatorConf);
    ShuffleServerConf shuffleServerConf = getShuffleServerConf();
    shuffleServerConf.setString(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.LOCALFILE.name());
    shuffleServerConf.set(
        ShuffleServerConf.RSS_STORAGE_BASE_PATH,
        Arrays.asList(data1.getAbsolutePath(), data2.getAbsolutePath())
    );
    shuffleServerConf.setBoolean(ShuffleServerConf.HEALTH_CHECK_ENABLE, true);
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
  public void diskErrorTest() throws Exception {

    String appId = "ap_disk_error_data";
    Map<Long, byte[]> expectedData = Maps.newHashMap();
    Set<Long> expectedBlock1 = Sets.newHashSet();
    Roaring64NavigableMap blockIdBitmap1 = Roaring64NavigableMap.bitmapOf();
    List<ShuffleBlockInfo> blocks1 = createShuffleBlockList(
        0, 0, 1,3, 25, blockIdBitmap1, expectedData);
    RssRegisterShuffleRequest rr1 =  new RssRegisterShuffleRequest(appId, 0,
        Lists.newArrayList(new PartitionRange(0, 0)), "");
    shuffleServerClient.registerShuffle(rr1);
    blocks1.forEach(b -> expectedBlock1.add(b.getBlockId()));
    Map<Integer, List<ShuffleBlockInfo>> partitionToBlocks = Maps.newHashMap();
    partitionToBlocks.put(0, blocks1);
    Map<Integer, Map<Integer, List<ShuffleBlockInfo>>> shuffleToBlocks = Maps.newHashMap();
    shuffleToBlocks.put(0, partitionToBlocks);
    RssSendShuffleDataRequest rs1 = new RssSendShuffleDataRequest(appId, 3, 1000, shuffleToBlocks);
    shuffleServerClient.sendShuffleData(rs1);
    RssSendCommitRequest rc1 = new RssSendCommitRequest(appId, 0);
    shuffleServerClient.sendCommit(rc1);
    RssFinishShuffleRequest rf1 = new RssFinishShuffleRequest(appId, 0);
    shuffleServerClient.finishShuffle(rf1);
    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(StorageType.LOCALFILE.name(),
        appId, 0, 0, 100, 1, 10, 1000, null,
        blockIdBitmap1, Roaring64NavigableMap.bitmapOf(1), shuffleServerInfo, conf, new DefaultIdHelper());
    validateResult(readClient, expectedData);

    File shuffleData = new File(data2, appId);
    assertTrue(shuffleData.exists());
    FileUtils.deleteDirectory(data2);
    assertFalse(data2.exists());
    boolean suc = data2.createNewFile();
    assertTrue(suc);
    Uninterruptibles.sleepUninterruptibly(10, TimeUnit.SECONDS);

    expectedData.clear();
    partitionToBlocks.clear();
    shuffleToBlocks.clear();
    Roaring64NavigableMap blockIdBitmap2 = Roaring64NavigableMap.bitmapOf();
    Set<Long> expectedBlock2 = Sets.newHashSet();
    List<ShuffleBlockInfo> blocks2 = createShuffleBlockList(
        0, 0, 2, 5, 30, blockIdBitmap2, expectedData);
    blocks2.forEach(b -> expectedBlock2.add(b.getBlockId()));
    partitionToBlocks.put(0, blocks2);
    shuffleToBlocks.put(0, partitionToBlocks);
    rs1 = new RssSendShuffleDataRequest(appId, 3, 1000, shuffleToBlocks);
    shuffleServerClient.sendShuffleData(rs1);
    shuffleServerClient.sendCommit(rc1);
    shuffleServerClient.finishShuffle(rf1);

    readClient = new ShuffleReadClientImpl(StorageType.LOCALFILE.name(),
        appId, 0, 0, 100, 1, 10, 1000, null,
        blockIdBitmap2, Roaring64NavigableMap.bitmapOf(2), shuffleServerInfo, conf, new DefaultIdHelper());
    validateResult(readClient, expectedData);
    shuffleData = new File(data1, appId);
    assertTrue(shuffleData.exists());
  }
}
