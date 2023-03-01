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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.client.impl.ShuffleReadClientImpl;
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

import static org.apache.uniffle.common.util.Constants.SHUFFLE_DATA_FILE_SUFFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShuffleServerConcurrentWriteOfHdfsTest extends ShuffleServerWithHdfsTest {
  private static final int MAX_CONCURRENCY = 3;

  @BeforeAll
  public static void setupServers() throws Exception {
    CoordinatorConf coordinatorConf = getCoordinatorConf();
    createCoordinatorServer(coordinatorConf);
    ShuffleServerConf shuffleServerConf = getShuffleServerConf();
    shuffleServerConf.setString(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.HDFS.name());
    shuffleServerConf.setInteger(ShuffleServerConf.SERVER_MAX_CONCURRENCY_OF_ONE_PARTITION, MAX_CONCURRENCY);
    shuffleServerConf.setBoolean(shuffleServerConf.SINGLE_BUFFER_FLUSH_ENABLED, true);
    shuffleServerConf.setLong(shuffleServerConf.SINGLE_BUFFER_FLUSH_THRESHOLD, 1024 * 1024L);
    createShuffleServer(shuffleServerConf);
    startServers();
  }

  @Test
  public void testConcurrentWrite2Hdfs() throws Exception {
    String appId = "testConcurrentWrite2Hdfs";
    String dataBasePath = HDFS_URI + "rss/test";
    RssRegisterShuffleRequest rrsr = new RssRegisterShuffleRequest(
        appId,
        0,
        Lists.newArrayList(new PartitionRange(0, 1)),
        dataBasePath
    );
    shuffleServerClient.registerShuffle(rrsr);

    List<Roaring64NavigableMap> bitmaps = new ArrayList<>();
    Map<Long, byte[]> expectedDataList = new HashMap<>();
    IntStream.range(0, 20).forEach(x -> {
      Roaring64NavigableMap bitmap = Roaring64NavigableMap.bitmapOf();
      bitmaps.add(bitmap);

      Map<Long, byte[]> expectedData = Maps.newHashMap();

      List<ShuffleBlockInfo> blocks = createShuffleBlockList(
          0,
          0,
          0,
          1,
          1024 * 1025,
          bitmap,
          expectedData,
          mockSSI
      );
      expectedDataList.putAll(expectedData);

      Map<Integer, List<ShuffleBlockInfo>> partitionToBlocks = Maps.newHashMap();
      partitionToBlocks.put(0, blocks);
      Map<Integer, Map<Integer, List<ShuffleBlockInfo>>> shuffleToBlocks = Maps.newHashMap();
      shuffleToBlocks.put(0, partitionToBlocks);
      RssSendShuffleDataRequest rssdr = new RssSendShuffleDataRequest(appId, 3, 1000, shuffleToBlocks);
      shuffleServerClient.sendShuffleData(rssdr);
    });

    RssSendCommitRequest rscr = new RssSendCommitRequest(appId, 0);
    shuffleServerClient.sendCommit(rscr);

    RssFinishShuffleRequest rfsr = new RssFinishShuffleRequest(appId, 0);
    shuffleServerClient.finishShuffle(rfsr);

    // Check the concurrent hdfs file creation
    FileStatus[] fileStatuses = fs.listStatus(new Path(dataBasePath + "/" + appId + "/0/0-1"));
    long actual = Arrays
        .stream(fileStatuses)
        .filter(x -> x.getPath().getName().endsWith(SHUFFLE_DATA_FILE_SUFFIX))
        .count();
    assertEquals(MAX_CONCURRENCY, actual);

    ShuffleServerInfo ssi = new ShuffleServerInfo(LOCALHOST, SHUFFLE_SERVER_PORT);
    Roaring64NavigableMap blocksBitmap = Roaring64NavigableMap.bitmapOf();
    bitmaps.stream().forEach(x -> {
      Iterator<Long> iterator = x.iterator();
      while (iterator.hasNext()) {
        blocksBitmap.add(iterator.next());
      }
    });

    ShuffleReadClientImpl readClient = new ShuffleReadClientImpl(
        StorageType.HDFS.name(),
        appId,
        0,
        0,
        100,
        2,
        10,
        1000,
        dataBasePath,
        blocksBitmap,
        Roaring64NavigableMap.bitmapOf(0),
        Lists.newArrayList(ssi),
        new Configuration(),
        new DefaultIdHelper()
    );

    validateResult(readClient, expectedDataList, blocksBitmap);
  }
}

