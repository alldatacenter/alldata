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
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.client.impl.ShuffleWriteClientImpl;
import org.apache.uniffle.common.ClientType;
import org.apache.uniffle.common.ShuffleAssignmentsInfo;
import org.apache.uniffle.common.config.ReconfigurableBase;
import org.apache.uniffle.common.config.RssBaseConf;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.coordinator.SimpleClusterManager;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CoordinatorAssignmentTest extends CoordinatorTestBase {
  private static final Logger LOG = LoggerFactory.getLogger(CoordinatorAssignmentTest.class);
  private static final int SHUFFLE_NODES_MAX = 10;
  private static final int SERVER_NUM = 10;
  private static final HashSet<String> TAGS = Sets.newHashSet("t1");

  private static final String QUORUM =
      LOCALHOST + ":" + COORDINATOR_PORT_1 + "," + LOCALHOST + ":" + COORDINATOR_PORT_2;

  @BeforeAll
  public static void setupServers(@TempDir File tmpDir) throws Exception {
    CoordinatorConf coordinatorConf1 = getCoordinatorConf();
    coordinatorConf1.setLong(CoordinatorConf.COORDINATOR_APP_EXPIRED, 2000);
    coordinatorConf1.setInteger(CoordinatorConf.COORDINATOR_SHUFFLE_NODES_MAX, SHUFFLE_NODES_MAX);
    coordinatorConf1.setString(ReconfigurableBase.RECONFIGURABLE_FILE_NAME,
        new File(tmpDir, "coordinator.conf").getPath());
    coordinatorConf1.setLong(RssBaseConf.RSS_RECONFIGURE_INTERVAL_SEC, 1L);
    createCoordinatorServer(coordinatorConf1);

    CoordinatorConf coordinatorConf2 = getCoordinatorConf();
    coordinatorConf2.setLong(CoordinatorConf.COORDINATOR_APP_EXPIRED, 2000);
    coordinatorConf2.setInteger(CoordinatorConf.COORDINATOR_SHUFFLE_NODES_MAX, SHUFFLE_NODES_MAX);
    coordinatorConf2.setInteger(CoordinatorConf.RPC_SERVER_PORT, COORDINATOR_PORT_2);
    coordinatorConf2.setInteger(CoordinatorConf.JETTY_HTTP_PORT, JETTY_PORT_2);
    coordinatorConf2.setString(ReconfigurableBase.RECONFIGURABLE_FILE_NAME,
        new File(tmpDir, "coordinator.conf").getPath());
    coordinatorConf2.setLong(RssBaseConf.RSS_RECONFIGURE_INTERVAL_SEC, 1L);
    createCoordinatorServer(coordinatorConf2);

    for (int i = 0; i < SERVER_NUM; i++) {
      ShuffleServerConf shuffleServerConf = getShuffleServerConf();
      File dataDir1 = new File(tmpDir, "data1");
      String basePath = dataDir1.getAbsolutePath();
      shuffleServerConf.set(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.MEMORY_LOCALFILE_HDFS.name());
      shuffleServerConf.set(ShuffleServerConf.RSS_STORAGE_BASE_PATH, Arrays.asList(basePath));
      shuffleServerConf.set(RssBaseConf.RPC_METRICS_ENABLED, true);
      shuffleServerConf.set(ShuffleServerConf.SERVER_APP_EXPIRED_WITHOUT_HEARTBEAT, 2000L);
      shuffleServerConf.set(ShuffleServerConf.SERVER_PRE_ALLOCATION_EXPIRED, 5000L);
      shuffleServerConf.setInteger(RssBaseConf.RPC_SERVER_PORT, 18001 + i);
      shuffleServerConf.setInteger(RssBaseConf.JETTY_HTTP_PORT, 19010 + i);
      shuffleServerConf.set(ShuffleServerConf.TAGS, new ArrayList<>(TAGS));
      shuffleServerConf.setString("rss.coordinator.quorum", QUORUM);
      createShuffleServer(shuffleServerConf);
    }
    startServers();

    Thread.sleep(1000 * 5);
  }

  @Test
  public void testSilentPeriod() throws Exception {
    ShuffleWriteClientImpl shuffleWriteClient = new ShuffleWriteClientImpl(ClientType.GRPC.name(), 3, 1000, 1,
        1, 1, 1, true, 1, 1, 10, 10);
    shuffleWriteClient.registerCoordinators(QUORUM);

    // Case1: Disable silent period
    ShuffleAssignmentsInfo info = shuffleWriteClient.getShuffleAssignments("app1", 0, 10, 1, TAGS, -1, -1);
    assertEquals(SHUFFLE_NODES_MAX, info.getServerToPartitionRanges().keySet().size());

    // Case2: Enable silent period mechanism, it should fallback to slave coordinator.
    SimpleClusterManager clusterManager = (SimpleClusterManager) coordinators.get(0).getClusterManager();
    clusterManager.setReadyForServe(false);
    clusterManager.setStartupSilentPeriodEnabled(true);
    clusterManager.setStartTime(System.currentTimeMillis() - 1);

    if (clusterManager.getNodesNum() < 10) {
      info = shuffleWriteClient.getShuffleAssignments("app1", 0, 10, 1, TAGS, -1, -1);
      assertEquals(SHUFFLE_NODES_MAX, info.getServerToPartitionRanges().keySet().size());
    }

    // recover
    clusterManager.setReadyForServe(true);
  }

  @Test
  public void testAssignmentServerNodesNumber() throws Exception {
    ShuffleWriteClientImpl shuffleWriteClient = new ShuffleWriteClientImpl(ClientType.GRPC.name(), 3, 1000, 1,
        1, 1, 1, true, 1, 1, 10, 10);
    shuffleWriteClient.registerCoordinators(COORDINATOR_QUORUM);

    /**
     * case1: user specify the illegal shuffle node num(<0)
     * it will use the default shuffle nodes num when having enough servers.
     */
    ShuffleAssignmentsInfo info = shuffleWriteClient.getShuffleAssignments("app1", 0, 10, 1, TAGS, -1, -1);
    assertEquals(SHUFFLE_NODES_MAX, info.getServerToPartitionRanges().keySet().size());

    /**
     * case2: user specify the illegal shuffle node num(==0)
     * it will use the default shuffle nodes num when having enough servers.
     */
    info = shuffleWriteClient.getShuffleAssignments("app1", 0, 10, 1, TAGS, 0, -1);
    assertEquals(SHUFFLE_NODES_MAX, info.getServerToPartitionRanges().keySet().size());

    /**
     * case3: user specify the illegal shuffle node num(>default max limitation)
     * it will use the default shuffle nodes num when having enough servers
     */
    info = shuffleWriteClient.getShuffleAssignments("app1", 0, 10, 1, TAGS, SERVER_NUM + 10, -1);
    assertEquals(SHUFFLE_NODES_MAX, info.getServerToPartitionRanges().keySet().size());

    /**
     * case4: user specify the legal shuffle node num,
     * it will use the customized shuffle nodes num when having enough servers
     */
    info = shuffleWriteClient.getShuffleAssignments("app1", 0, 10, 1, TAGS, SERVER_NUM - 1, -1);
    assertEquals(SHUFFLE_NODES_MAX - 1, info.getServerToPartitionRanges().keySet().size());
  }

  @Test
  public void testReconfigureNodeMax() throws Exception {
    String fileName = coordinators.get(0).getCoordinatorConf()
        .getString(ReconfigurableBase.RECONFIGURABLE_FILE_NAME,"");
    new File(fileName).createNewFile();
    ShuffleWriteClientImpl shuffleWriteClient = new ShuffleWriteClientImpl(ClientType.GRPC.name(), 3, 1000, 1,
        1, 1, 1, true, 1, 1, 10, 10);
    shuffleWriteClient.registerCoordinators(COORDINATOR_QUORUM);
    ShuffleAssignmentsInfo info = shuffleWriteClient.getShuffleAssignments("app1", 0, 10, 1, TAGS, SERVER_NUM + 10, -1);
    assertEquals(SHUFFLE_NODES_MAX, info.getServerToPartitionRanges().keySet().size());
    Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    try (FileWriter fileWriter = new FileWriter(fileName)) {
      fileWriter.append(CoordinatorConf.COORDINATOR_SHUFFLE_NODES_MAX.key() + " " + 5);
    }
    Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
    info = shuffleWriteClient.getShuffleAssignments("app1", 0, 10, 1, TAGS, SERVER_NUM + 10, -1);
    assertEquals(5, info.getServerToPartitionRanges().keySet().size());
    try (FileWriter fileWriter = new FileWriter(fileName)) {
      fileWriter.append(CoordinatorConf.COORDINATOR_SHUFFLE_NODES_MAX.key() + " " + 10);
    }
    Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
  }

}
