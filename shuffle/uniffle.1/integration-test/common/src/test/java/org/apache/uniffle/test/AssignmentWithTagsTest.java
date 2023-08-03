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
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.client.impl.ShuffleWriteClientImpl;
import org.apache.uniffle.common.ClientType;
import org.apache.uniffle.common.ShuffleAssignmentsInfo;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.coordinator.CoordinatorServer;
import org.apache.uniffle.server.ShuffleServer;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * This class is to test the conf of {@code org.apache.uniffle.server.ShuffleServerConf.Tags}
 * and {@code RssClientConfig.RSS_CLIENT_ASSIGNMENT_TAGS}
 */
public class AssignmentWithTagsTest extends CoordinatorTestBase {
  private static final Logger LOG = LoggerFactory.getLogger(AssignmentWithTagsTest.class);

  // KV: tag -> shuffle server id
  private static Map<String, List<Integer>> tagOfShufflePorts = new HashMap<>();

  private static List<Integer> findAvailablePorts(int num) throws IOException {
    List<ServerSocket> sockets = new ArrayList<>();
    List<Integer> ports = new ArrayList<>();

    for (int i = 0; i < num; i++) {
      ServerSocket socket = new ServerSocket(0);
      ports.add(socket.getLocalPort());
      sockets.add(socket);
    }

    for (ServerSocket socket : sockets) {
      socket.close();
    }

    return ports;
  }

  private static void createAndStartShuffleServerWithTags(Set<String> tags, File tmpDir) throws Exception {
    ShuffleServerConf shuffleServerConf = getShuffleServerConf();
    shuffleServerConf.setLong("rss.server.app.expired.withoutHeartbeat", 4000);

    File dataDir1 = new File(tmpDir, "data1");
    File dataDir2 = new File(tmpDir, "data2");
    String basePath = dataDir1.getAbsolutePath() + "," + dataDir2.getAbsolutePath();

    shuffleServerConf.setString("rss.storage.type", StorageType.LOCALFILE.name());
    shuffleServerConf.setString("rss.storage.basePath", basePath);
    shuffleServerConf.setString("rss.server.tags", StringUtils.join(tags, ","));

    List<Integer> ports = findAvailablePorts(2);
    shuffleServerConf.setInteger("rss.rpc.server.port", ports.get(0));
    shuffleServerConf.setInteger("rss.jetty.http.port", ports.get(1));

    for (String tag : tags) {
      tagOfShufflePorts.putIfAbsent(tag, new ArrayList<>());
      tagOfShufflePorts.get(tag).add(ports.get(0));
    }
    tagOfShufflePorts.putIfAbsent(Constants.SHUFFLE_SERVER_VERSION, new ArrayList<>());
    tagOfShufflePorts.get(Constants.SHUFFLE_SERVER_VERSION).add(ports.get(0));

    LOG.info("Shuffle server data dir: {}, rpc port: {}, http port: {}", dataDir1 + "," + dataDir2,
        ports.get(0), ports.get(1));

    ShuffleServer server = new ShuffleServer(shuffleServerConf);
    shuffleServers.add(server);
    server.start();
  }

  @BeforeAll
  public static void setupServers(@TempDir File tmpDir) throws Exception {
    CoordinatorConf coordinatorConf = getCoordinatorConf();
    createCoordinatorServer(coordinatorConf);

    for (CoordinatorServer coordinator : coordinators) {
      coordinator.start();
    }

    File dir1 = new File(tmpDir, "server1");
    for (int i = 0; i < 2; i++) {
      createAndStartShuffleServerWithTags(Sets.newHashSet(), dir1);
    }

    File dir2 = new File(tmpDir, "server2");
    for (int i = 0; i < 2; i++) {
      createAndStartShuffleServerWithTags(Sets.newHashSet("fixed"), dir2);
    }

    File dir3 = new File(tmpDir, "server3");
    for (int i = 0; i < 2; i++) {
      createAndStartShuffleServerWithTags(Sets.newHashSet("elastic"), dir3);
    }

    // Wait all shuffle servers registering to coordinator
    long startTimeMS = System.currentTimeMillis();
    while (true) {
      int nodeSum = coordinators.get(0).getClusterManager().getNodesNum();
      if (nodeSum == 6) {
        break;
      }
      if (System.currentTimeMillis() - startTimeMS > 1000 * 5) {
        throw new Exception("Timeout of waiting shuffle servers registry, timeout: 5s.");
      }
    }
  }

  @Test
  public void testTags() throws Exception {
    ShuffleWriteClientImpl shuffleWriteClient = new ShuffleWriteClientImpl(ClientType.GRPC.name(), 3, 1000, 1,
        1, 1, 1, true, 1, 1, 10, 10);
    shuffleWriteClient.registerCoordinators(COORDINATOR_QUORUM);

    // Case1 : only set the single default shuffle version tag
    ShuffleAssignmentsInfo assignmentsInfo =
        shuffleWriteClient.getShuffleAssignments("app-1",
            1, 1, 1, Sets.newHashSet(Constants.SHUFFLE_SERVER_VERSION), 1, -1);

    List<Integer> assignedServerPorts = assignmentsInfo
        .getPartitionToServers()
        .values()
        .stream()
        .flatMap(x -> x.stream())
        .map(x -> x.getPort())
        .collect(Collectors.toList());
    assertEquals(1, assignedServerPorts.size());
    assertTrue(tagOfShufflePorts.get(Constants.SHUFFLE_SERVER_VERSION).contains(assignedServerPorts.get(0)));

    // Case2: Set the single non-exist shuffle server tag
    try {
      assignmentsInfo = shuffleWriteClient.getShuffleAssignments("app-2",
          1, 1, 1, Sets.newHashSet("non-exist"), 1, -1);
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().startsWith("Error happened when getShuffleAssignments with"));
    }

    // Case3: Set the single fixed tag
    assignmentsInfo = shuffleWriteClient.getShuffleAssignments("app-3",
        1, 1, 1, Sets.newHashSet("fixed"), 1, -1);
    assignedServerPorts = assignmentsInfo
        .getPartitionToServers()
        .values()
        .stream()
        .flatMap(x -> x.stream())
        .map(x -> x.getPort())
        .collect(Collectors.toList());
    assertEquals(1, assignedServerPorts.size());
    assertTrue(tagOfShufflePorts.get("fixed").contains(assignedServerPorts.get(0)));

    // case4: Set the multiple tags if exists
    assignmentsInfo = shuffleWriteClient.getShuffleAssignments("app-4",
        1, 1, 1, Sets.newHashSet("fixed", Constants.SHUFFLE_SERVER_VERSION), 1, -1);
    assignedServerPorts = assignmentsInfo
        .getPartitionToServers()
        .values()
        .stream()
        .flatMap(x -> x.stream())
        .map(x -> x.getPort())
        .collect(Collectors.toList());
    assertEquals(1, assignedServerPorts.size());
    assertTrue(tagOfShufflePorts.get("fixed").contains(assignedServerPorts.get(0)));

    // case5: Set the multiple tags if non-exist
    try {
      assignmentsInfo = shuffleWriteClient.getShuffleAssignments("app-5",
          1, 1, 1, Sets.newHashSet("fixed", "elastic", Constants.SHUFFLE_SERVER_VERSION), 1, -1);
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().startsWith("Error happened when getShuffleAssignments with"));
    }
  }
}
