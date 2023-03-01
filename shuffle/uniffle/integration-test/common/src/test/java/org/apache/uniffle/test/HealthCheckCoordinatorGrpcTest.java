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
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.apache.uniffle.client.request.RssGetShuffleAssignmentsRequest;
import org.apache.uniffle.client.response.RssGetShuffleAssignmentsResponse;
import org.apache.uniffle.common.rpc.StatusCode;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.coordinator.ServerNode;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class HealthCheckCoordinatorGrpcTest extends CoordinatorTestBase  {

  @TempDir private static File serverTmpDir;
  private static File tempDataFile = new File(serverTmpDir, "data");
  private static int writeDataSize;

  @BeforeAll
  public static void setupServers() throws Exception {
    File data1 = new File(serverTmpDir, "data1");
    data1.mkdirs();
    File data2 = new File(serverTmpDir, "data2");
    data2.mkdirs();
    long freeSize = serverTmpDir.getUsableSpace();
    double maxUsage;
    double healthUsage;
    if (freeSize > 400 * 1024 * 1024) {
      writeDataSize = 200 * 1024 * 1024;
    } else {
      writeDataSize = (int) freeSize / 2;
    }
    long totalSize = serverTmpDir.getTotalSpace();
    long usedSize = serverTmpDir.getTotalSpace() - serverTmpDir.getUsableSpace();
    maxUsage = (writeDataSize * 0.75 + usedSize) * 100.0 / totalSize;
    healthUsage = (writeDataSize * 0.5 + usedSize) * 100.0 / totalSize;
    CoordinatorConf coordinatorConf = getCoordinatorConf();
    coordinatorConf.setLong(CoordinatorConf.COORDINATOR_APP_EXPIRED, 2000);
    coordinatorConf.setLong(CoordinatorConf.COORDINATOR_HEARTBEAT_TIMEOUT, 3000);
    createCoordinatorServer(coordinatorConf);
    ShuffleServerConf shuffleServerConf = getShuffleServerConf();
    shuffleServerConf.setBoolean(ShuffleServerConf.HEALTH_CHECK_ENABLE, true);
    shuffleServerConf.setString(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.LOCALFILE.name());
    shuffleServerConf.set(ShuffleServerConf.RSS_STORAGE_BASE_PATH, Arrays.asList(data1.getAbsolutePath()));
    shuffleServerConf.setDouble(ShuffleServerConf.HEALTH_STORAGE_RECOVERY_USAGE_PERCENTAGE, healthUsage);
    shuffleServerConf.setDouble(ShuffleServerConf.HEALTH_STORAGE_MAX_USAGE_PERCENTAGE, maxUsage);
    shuffleServerConf.setLong(ShuffleServerConf.HEALTH_CHECK_INTERVAL, 1000L);
    createShuffleServer(shuffleServerConf);
    shuffleServerConf.setInteger(ShuffleServerConf.RPC_SERVER_PORT, SHUFFLE_SERVER_PORT + 1);
    shuffleServerConf.setInteger(ShuffleServerConf.JETTY_HTTP_PORT, 18081);
    shuffleServerConf.setString(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.LOCALFILE.name());
    shuffleServerConf.set(ShuffleServerConf.RSS_STORAGE_BASE_PATH, Arrays.asList(data2.getAbsolutePath()));
    shuffleServerConf.setDouble(ShuffleServerConf.HEALTH_STORAGE_RECOVERY_USAGE_PERCENTAGE, healthUsage);
    shuffleServerConf.setDouble(ShuffleServerConf.HEALTH_STORAGE_MAX_USAGE_PERCENTAGE, maxUsage);
    shuffleServerConf.setLong(ShuffleServerConf.HEALTH_CHECK_INTERVAL, 1000L);
    shuffleServerConf.setBoolean(ShuffleServerConf.HEALTH_CHECK_ENABLE, true);
    createShuffleServer(shuffleServerConf);
    startServers();
  }

  @Test
  public void healthCheckTest() throws Exception {
    Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
    assertEquals(2, coordinatorClient.getShuffleServerList().getServersCount());
    List<ServerNode> nodes  = coordinators.get(0).getClusterManager()
        .getServerList(Sets.newHashSet(Constants.SHUFFLE_SERVER_VERSION));
    assertEquals(2, coordinatorClient.getShuffleServerList().getServersCount());
    assertEquals(2, nodes.size());

    RssGetShuffleAssignmentsRequest request =
        new RssGetShuffleAssignmentsRequest(
          "1",
          1,
          1,
          1,
          1,
          Sets.newHashSet(Constants.SHUFFLE_SERVER_VERSION));
    RssGetShuffleAssignmentsResponse response =
        coordinatorClient.getShuffleAssignments(request);
    assertFalse(response.getPartitionToServers().isEmpty());
    for (ServerNode node : nodes) {
      assertTrue(node.isHealthy());
    }
    byte[] bytes = new byte[writeDataSize];
    new Random().nextBytes(bytes);
    try (FileOutputStream out = new FileOutputStream(tempDataFile)) {
      out.write(bytes);
    }
    Uninterruptibles.sleepUninterruptibly(10, TimeUnit.SECONDS);

    nodes  = coordinators.get(0).getClusterManager().list();
    assertEquals(2, nodes.size());
    for (ServerNode node : nodes) {
      assertFalse(node.isHealthy());
    }
    nodes = coordinators.get(0).getClusterManager().getServerList(Sets.newHashSet(Constants.SHUFFLE_SERVER_VERSION));
    assertEquals(0, nodes.size());
    response = coordinatorClient.getShuffleAssignments(request);
    assertEquals(StatusCode.INTERNAL_ERROR, response.getStatusCode());

    tempDataFile.delete();
    int i = 0;
    do {
      Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
      nodes = coordinators.get(0).getClusterManager()
          .getServerList(Sets.newHashSet(Constants.SHUFFLE_SERVER_VERSION));
      i++;
      if (i == 10) {
        fail();
      }
    } while (nodes.size() != 2);
    for (ServerNode node : nodes) {
      assertTrue(node.isHealthy());
    }
    assertEquals(2, nodes.size());
    response =
        coordinatorClient.getShuffleAssignments(request);
    assertFalse(response.getPartitionToServers().isEmpty());
  }
}
