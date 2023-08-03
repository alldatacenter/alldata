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

package org.apache.celeborn.client.write;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.client.DummyShuffleClient;
import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.protocol.PartitionLocation;
import org.apache.celeborn.common.util.JavaUtils;
import org.apache.celeborn.common.util.Utils;
import org.apache.celeborn.common.write.PushState;

public class DataPushQueueSuitJ {
  private static final Logger LOG = LoggerFactory.getLogger(DataPushQueueSuitJ.class);
  private static File tempDir = null;

  private final int shuffleId = 0;
  private final int numPartitions = 10;

  @BeforeClass
  public static void beforeAll() {
    tempDir = Utils.createTempDir(System.getProperty("java.io.tmpdir"), "celeborn_test");
  }

  @AfterClass
  public static void afterAll() {
    try {
      JavaUtils.deleteRecursively(tempDir);
    } catch (IOException e) {
      LOG.error("Failed to delete temporary directory.", e);
    }
  }

  @Test
  public void testDataPushQueue() throws Exception {
    final int numWorker = 3;
    List<List<Integer>> workerData = new ArrayList<>();
    for (int i = 0; i < numWorker; i++) {
      workerData.add(new ArrayList<>());
    }
    for (int i = 0; i < numPartitions; i++) {
      workerData.get(i % numWorker).add(i);
    }
    List<List<Integer>> tarWorkerData = new ArrayList<>();
    for (int i = 0; i < numWorker; i++) {
      tarWorkerData.add(new ArrayList<>());
    }

    Map<Integer, Integer> partitionBatchIdMap = new HashMap<>();

    CelebornConf conf = new CelebornConf();
    conf.set(CelebornConf.CLIENT_PUSH_MAX_REQS_IN_FLIGHT_PERWORKER().key(), "2");

    int shuffleId = 0;
    int mapId = 0;
    int attemptId = 0;
    int numMappers = 10;
    int numPartitions = 10;
    final File tempFile = new File(tempDir, UUID.randomUUID().toString());
    DummyShuffleClient client = new DummyShuffleClient(conf, tempFile);
    client.initReducePartitionMap(shuffleId, numPartitions, numWorker);

    LongAdder[] mapStatusLengths = new LongAdder[numPartitions];
    for (int i = 0; i < numPartitions; i++) {
      mapStatusLengths[i] = new LongAdder();
    }
    DataPusher dataPusher =
        new DataPusher(
            shuffleId,
            mapId,
            attemptId,
            0,
            numMappers,
            numPartitions,
            conf,
            client,
            null,
            integer -> {},
            mapStatusLengths);

    final String mapKey = Utils.makeMapKey(shuffleId, mapId, attemptId);
    DataPushQueue dataPushQueue = dataPusher.getDataPushQueue();
    PushState pushState = client.getPushState(mapKey);
    Map<Integer, PartitionLocation> reducePartitionMap =
        client.getReducePartitionMap().get(shuffleId);

    for (int i = 0; i < numPartitions; i++) {
      byte[] b = intToBytes(workerData.get(i % numWorker).get(i / numWorker));
      dataPusher.addTask(i, b, b.length);
      int batchId = pushState.nextBatchId();
      pushState.addBatch(batchId, reducePartitionMap.get(i).hostAndPushPort());
      partitionBatchIdMap.put(i, batchId);
    }

    AtomicBoolean running = new AtomicBoolean(true);
    new Thread(
            () -> {
              while (running.get()) {
                try {
                  ArrayList<PushTask> tasks = dataPushQueue.takePushTasks();
                  for (int i = 0; i < tasks.size(); i++) {
                    PushTask task = tasks.get(i);
                    byte[] buffer = task.getBuffer();
                    int partitionId = task.getPartitionId();
                    tarWorkerData.get(partitionId % numWorker).add(bytesToInt(buffer));
                    pushState.removeBatch(
                        partitionBatchIdMap.get(partitionId),
                        reducePartitionMap.get(partitionId).hostAndPushPort());
                  }
                } catch (IOException | InterruptedException e) {
                  throw new RuntimeException(e);
                }
              }
            })
        .start();

    Thread.sleep(15 * 1000);
    running.set(false);

    for (int i = 0; i < numWorker; i++) {
      Assert.assertArrayEquals(workerData.get(i).toArray(), tarWorkerData.get(i).toArray());
    }

    client.shutdown();
  }

  public static byte[] intToBytes(int value) {
    byte[] src = new byte[4];
    src[0] = (byte) (value & 0xFF);
    src[1] = (byte) ((value >> 8) & 0xFF);
    src[2] = (byte) ((value >> 16) & 0xFF);
    src[3] = (byte) ((value >> 24) & 0xFF);
    return src;
  }

  public static int bytesToInt(byte[] src) {
    int value;
    value =
        (int)
            ((src[0] & 0xFF)
                | ((src[1] & 0xFF) << 8)
                | ((src[2] & 0xFF) << 16)
                | ((src[3] & 0xFF) << 24));
    return value;
  }
}
