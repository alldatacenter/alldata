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

package org.apache.celeborn.service.deploy.master.clustermeta;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.haclient.RssHARetryClient;
import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.common.meta.DiskInfo;
import org.apache.celeborn.common.meta.WorkerInfo;
import org.apache.celeborn.common.quota.ResourceConsumption;
import org.apache.celeborn.common.rpc.RpcEndpointAddress;
import org.apache.celeborn.common.rpc.RpcEndpointRef;
import org.apache.celeborn.common.rpc.RpcEnv;
import org.apache.celeborn.common.rpc.netty.NettyRpcEndpointRef;

public class DefaultMetaSystemSuiteJ {

  private RpcEnv mockRpcEnv = mock(RpcEnv.class);
  private CelebornConf conf = new CelebornConf();
  private AbstractMetaManager statusSystem;
  private RpcEndpointRef dummyRef =
      new NettyRpcEndpointRef(
          new CelebornConf(), RpcEndpointAddress.apply("localhost", 111, "dummy"), null);
  private AtomicLong callerId = new AtomicLong();

  private static String HOSTNAME1 = "host1";
  private static int RPCPORT1 = 1111;
  private static int PUSHPORT1 = 1112;
  private static int FETCHPORT1 = 1113;
  private static int REPLICATEPORT1 = 1114;
  private static Map<String, DiskInfo> disks1 = new HashMap<>();
  private static Map<UserIdentifier, ResourceConsumption> userResourceConsumption1 =
      new HashMap<>();

  private static String HOSTNAME2 = "host2";
  private static int RPCPORT2 = 2111;
  private static int PUSHPORT2 = 2112;
  private static int FETCHPORT2 = 2113;
  private static int REPLICATEPORT2 = 2114;
  private static Map<String, DiskInfo> disks2 = new HashMap<>();
  private static Map<UserIdentifier, ResourceConsumption> userResourceConsumption2 =
      new HashMap<>();

  private static String HOSTNAME3 = "host3";
  private static int RPCPORT3 = 3111;
  private static int PUSHPORT3 = 3112;
  private static int FETCHPORT3 = 3113;
  private static int REPLICATEPORT3 = 3114;
  private static Map<String, DiskInfo> disks3 = new HashMap<>();
  private static Map<UserIdentifier, ResourceConsumption> userResourceConsumption3 =
      new HashMap<>();

  @Before
  public void setUp() throws Exception {
    when(mockRpcEnv.setupEndpointRef(any(), any())).thenReturn(dummyRef);
    statusSystem = new SingleMasterMetaManager(mockRpcEnv, conf);

    disks1.clear();
    disks1.put("disk1", new DiskInfo("disk1", 64 * 1024 * 1024 * 1024L, 100, 0));
    disks1.put("disk2", new DiskInfo("disk2", 64 * 1024 * 1024 * 1024L, 100, 0));
    disks1.put("disk3", new DiskInfo("disk3", 64 * 1024 * 1024 * 1024L, 100, 0));
    disks1.put("disk4", new DiskInfo("disk4", 64 * 1024 * 1024 * 1024L, 100, 0));

    disks2.clear();
    disks2.put("disk1", new DiskInfo("disk1", 64 * 1024 * 1024 * 1024L, 100, 0));
    disks2.put("disk2", new DiskInfo("disk2", 64 * 1024 * 1024 * 1024L, 100, 0));
    disks2.put("disk3", new DiskInfo("disk3", 64 * 1024 * 1024 * 1024L, 100, 0));
    disks2.put("disk4", new DiskInfo("disk4", 64 * 1024 * 1024 * 1024L, 100, 0));

    disks3.clear();
    disks3.put("disk1", new DiskInfo("disk1", 64 * 1024 * 1024 * 1024L, 100, 0));
    disks3.put("disk2", new DiskInfo("disk2", 64 * 1024 * 1024 * 1024L, 100, 0));
    disks3.put("disk3", new DiskInfo("disk3", 64 * 1024 * 1024 * 1024L, 100, 0));
    disks3.put("disk4", new DiskInfo("disk4", 64 * 1024 * 1024 * 1024L, 100, 0));
  }

  @After
  public void tearDown() throws Exception {}

  private String getNewReqeustId() {
    return RssHARetryClient.encodeRequestId(
        UUID.randomUUID().toString(), callerId.incrementAndGet());
  }

  @Test
  public void testHandleRegisterWorker() {

    statusSystem.handleRegisterWorker(
        HOSTNAME1,
        RPCPORT1,
        PUSHPORT1,
        FETCHPORT1,
        REPLICATEPORT1,
        disks1,
        userResourceConsumption1,
        getNewReqeustId());
    statusSystem.handleRegisterWorker(
        HOSTNAME2,
        RPCPORT2,
        PUSHPORT2,
        FETCHPORT2,
        REPLICATEPORT2,
        disks2,
        userResourceConsumption2,
        getNewReqeustId());
    statusSystem.handleRegisterWorker(
        HOSTNAME3,
        RPCPORT3,
        PUSHPORT3,
        FETCHPORT3,
        REPLICATEPORT3,
        disks3,
        userResourceConsumption3,
        getNewReqeustId());

    assert (statusSystem.workers.size() == 3);
  }

  @Test
  public void testHandleWorkerLost() {
    statusSystem.handleRegisterWorker(
        HOSTNAME1,
        RPCPORT1,
        PUSHPORT1,
        FETCHPORT1,
        REPLICATEPORT1,
        disks1,
        userResourceConsumption1,
        getNewReqeustId());
    statusSystem.handleRegisterWorker(
        HOSTNAME2,
        RPCPORT2,
        PUSHPORT2,
        FETCHPORT2,
        REPLICATEPORT2,
        disks2,
        userResourceConsumption2,
        getNewReqeustId());
    statusSystem.handleRegisterWorker(
        HOSTNAME3,
        RPCPORT3,
        PUSHPORT3,
        FETCHPORT3,
        REPLICATEPORT3,
        disks3,
        userResourceConsumption3,
        getNewReqeustId());

    statusSystem.handleWorkerLost(
        HOSTNAME1, RPCPORT1, PUSHPORT1, FETCHPORT1, REPLICATEPORT1, getNewReqeustId());
    assert (statusSystem.workers.size() == 2);
  }

  private static String APPID1 = "appId1";
  private static int SHUFFLEID1 = 1;
  private static String SHUFFLEKEY1 = APPID1 + "-" + SHUFFLEID1;

  @Test
  public void testHandleRequestSlots() {
    statusSystem.handleRegisterWorker(
        HOSTNAME1,
        RPCPORT1,
        PUSHPORT1,
        FETCHPORT1,
        REPLICATEPORT1,
        disks1,
        userResourceConsumption1,
        getNewReqeustId());
    statusSystem.handleRegisterWorker(
        HOSTNAME2,
        RPCPORT2,
        PUSHPORT2,
        FETCHPORT2,
        REPLICATEPORT2,
        disks2,
        userResourceConsumption2,
        getNewReqeustId());
    statusSystem.handleRegisterWorker(
        HOSTNAME3,
        RPCPORT3,
        PUSHPORT3,
        FETCHPORT3,
        REPLICATEPORT3,
        disks3,
        userResourceConsumption3,
        getNewReqeustId());

    WorkerInfo workerInfo1 =
        new WorkerInfo(
            HOSTNAME1,
            RPCPORT1,
            PUSHPORT1,
            FETCHPORT1,
            REPLICATEPORT1,
            disks1,
            userResourceConsumption1,
            dummyRef);
    WorkerInfo workerInfo2 =
        new WorkerInfo(
            HOSTNAME2,
            RPCPORT2,
            PUSHPORT2,
            FETCHPORT2,
            REPLICATEPORT2,
            disks2,
            userResourceConsumption2,
            dummyRef);
    WorkerInfo workerInfo3 =
        new WorkerInfo(
            HOSTNAME3,
            RPCPORT3,
            PUSHPORT3,
            FETCHPORT3,
            REPLICATEPORT3,
            disks3,
            userResourceConsumption3,
            dummyRef);

    Map<String, Map<String, Integer>> workersToAllocate = new HashMap<>();
    Map<String, Integer> allocation = new HashMap<>();
    allocation.put("disk1", 5);
    workersToAllocate.put(workerInfo1.toUniqueId(), allocation);
    workersToAllocate.put(workerInfo2.toUniqueId(), allocation);

    statusSystem.handleRequestSlots(SHUFFLEKEY1, HOSTNAME1, workersToAllocate, getNewReqeustId());

    assert (workerInfo1.usedSlots() == 5);
    assert (workerInfo2.usedSlots() == 5);
    assert (workerInfo3.usedSlots() == 0);
  }

  @Test
  public void testHandleReleaseSlots() {
    statusSystem.handleRegisterWorker(
        HOSTNAME1,
        RPCPORT1,
        PUSHPORT1,
        FETCHPORT1,
        REPLICATEPORT1,
        disks1,
        userResourceConsumption1,
        getNewReqeustId());
    statusSystem.handleRegisterWorker(
        HOSTNAME2,
        RPCPORT2,
        PUSHPORT2,
        FETCHPORT2,
        REPLICATEPORT2,
        disks2,
        userResourceConsumption2,
        getNewReqeustId());
    statusSystem.handleRegisterWorker(
        HOSTNAME3,
        RPCPORT3,
        PUSHPORT3,
        FETCHPORT3,
        REPLICATEPORT3,
        disks3,
        userResourceConsumption3,
        getNewReqeustId());

    assert 3 == statusSystem.workers.size();

    Map<String, Map<String, Integer>> workersToAllocate = new HashMap<>();
    Map<String, Integer> allocation = new HashMap<>();
    allocation.put("disk1", 5);
    workersToAllocate.put(
        statusSystem.workers.stream()
            .filter(w -> w.host().equals(HOSTNAME1))
            .findFirst()
            .get()
            .toUniqueId(),
        allocation);
    workersToAllocate.put(
        statusSystem.workers.stream()
            .filter(w -> w.host().equals(HOSTNAME2))
            .findFirst()
            .get()
            .toUniqueId(),
        allocation);

    statusSystem.handleRequestSlots(SHUFFLEKEY1, HOSTNAME1, workersToAllocate, getNewReqeustId());

    List<String> workerIds = new ArrayList<>();
    workerIds.add(
        HOSTNAME1 + ":" + RPCPORT1 + ":" + PUSHPORT1 + ":" + FETCHPORT1 + ":" + REPLICATEPORT1);

    List<Map<String, Integer>> workerSlots = new ArrayList<>();
    workerSlots.add(
        new HashMap() {
          {
            put("disk1", 3);
          }
        });

    statusSystem.handleReleaseSlots(SHUFFLEKEY1, workerIds, workerSlots, getNewReqeustId());

    Assert.assertEquals(
        2,
        statusSystem.workers.stream()
            .filter(w -> w.host().equals(HOSTNAME1))
            .findFirst()
            .get()
            .usedSlots());
  }

  @Test
  public void testHandleAppLost() {
    statusSystem.handleRegisterWorker(
        HOSTNAME1,
        RPCPORT1,
        PUSHPORT1,
        FETCHPORT1,
        REPLICATEPORT1,
        disks1,
        userResourceConsumption1,
        getNewReqeustId());
    statusSystem.handleRegisterWorker(
        HOSTNAME2,
        RPCPORT2,
        PUSHPORT2,
        FETCHPORT2,
        REPLICATEPORT2,
        disks2,
        userResourceConsumption2,
        getNewReqeustId());
    statusSystem.handleRegisterWorker(
        HOSTNAME3,
        RPCPORT3,
        PUSHPORT3,
        FETCHPORT3,
        REPLICATEPORT3,
        disks3,
        userResourceConsumption3,
        getNewReqeustId());

    WorkerInfo workerInfo1 =
        new WorkerInfo(
            HOSTNAME1,
            RPCPORT1,
            PUSHPORT1,
            FETCHPORT1,
            REPLICATEPORT1,
            disks1,
            userResourceConsumption1,
            dummyRef);
    WorkerInfo workerInfo2 =
        new WorkerInfo(
            HOSTNAME2,
            RPCPORT2,
            PUSHPORT2,
            FETCHPORT2,
            REPLICATEPORT2,
            disks2,
            userResourceConsumption2,
            dummyRef);

    Map<String, Map<String, Integer>> workersToAllocate = new HashMap<>();
    Map<String, Integer> allocation = new HashMap<>();
    allocation.put("disk1", 5);
    workersToAllocate.put(workerInfo1.toUniqueId(), allocation);
    workersToAllocate.put(workerInfo2.toUniqueId(), allocation);

    statusSystem.handleRequestSlots(SHUFFLEKEY1, HOSTNAME1, workersToAllocate, getNewReqeustId());

    assert statusSystem.registeredShuffle.size() == 1;

    statusSystem.handleAppLost(APPID1, getNewReqeustId());

    assert statusSystem.registeredShuffle.isEmpty();
  }

  @Test
  public void testHandleUnRegisterShuffle() {
    statusSystem.handleRegisterWorker(
        HOSTNAME1,
        RPCPORT1,
        PUSHPORT1,
        FETCHPORT1,
        REPLICATEPORT1,
        disks1,
        userResourceConsumption1,
        getNewReqeustId());
    statusSystem.handleRegisterWorker(
        HOSTNAME2,
        RPCPORT2,
        PUSHPORT2,
        FETCHPORT2,
        REPLICATEPORT2,
        disks2,
        userResourceConsumption2,
        getNewReqeustId());
    statusSystem.handleRegisterWorker(
        HOSTNAME3,
        RPCPORT3,
        PUSHPORT3,
        FETCHPORT3,
        REPLICATEPORT3,
        disks3,
        userResourceConsumption3,
        getNewReqeustId());

    WorkerInfo workerInfo1 =
        new WorkerInfo(
            HOSTNAME1,
            RPCPORT1,
            PUSHPORT1,
            FETCHPORT1,
            REPLICATEPORT1,
            disks1,
            userResourceConsumption1,
            dummyRef);
    WorkerInfo workerInfo2 =
        new WorkerInfo(
            HOSTNAME2,
            RPCPORT2,
            PUSHPORT2,
            FETCHPORT2,
            REPLICATEPORT2,
            disks2,
            userResourceConsumption2,
            dummyRef);

    Map<String, Map<String, Integer>> workersToAllocate = new HashMap<>();
    Map<String, Integer> allocation = new HashMap<>();
    allocation.put("disk1", 5);
    workersToAllocate.put(workerInfo1.toUniqueId(), allocation);
    workersToAllocate.put(workerInfo2.toUniqueId(), allocation);

    statusSystem.handleRequestSlots(SHUFFLEKEY1, HOSTNAME1, workersToAllocate, getNewReqeustId());

    assert statusSystem.registeredShuffle.size() == 1;

    statusSystem.handleUnRegisterShuffle(SHUFFLEKEY1, getNewReqeustId());

    assert statusSystem.registeredShuffle.isEmpty();
  }

  @Test
  public void testHandleAppHeartbeat() {
    long dummy = 1235L;
    statusSystem.handleAppHeartbeat(APPID1, 1, 1, dummy, getNewReqeustId());
    assert statusSystem.appHeartbeatTime.get(APPID1) == dummy;

    String appId2 = "app02";
    statusSystem.handleAppHeartbeat(appId2, 1, 1, dummy, getNewReqeustId());
    assert statusSystem.appHeartbeatTime.get(appId2) == dummy;

    assert statusSystem.appHeartbeatTime.size() == 2;
  }

  @Test
  public void testHandleWorkerHeartbeat() {
    statusSystem.handleRegisterWorker(
        HOSTNAME1,
        RPCPORT1,
        PUSHPORT1,
        FETCHPORT1,
        REPLICATEPORT1,
        new HashMap<>(),
        userResourceConsumption1,
        getNewReqeustId());
    statusSystem.handleRegisterWorker(
        HOSTNAME2,
        RPCPORT2,
        PUSHPORT2,
        FETCHPORT2,
        REPLICATEPORT2,
        new HashMap<>(),
        userResourceConsumption2,
        getNewReqeustId());
    statusSystem.handleRegisterWorker(
        HOSTNAME3,
        RPCPORT3,
        PUSHPORT3,
        FETCHPORT3,
        REPLICATEPORT3,
        new HashMap<>(),
        userResourceConsumption3,
        getNewReqeustId());

    statusSystem.handleWorkerHeartbeat(
        HOSTNAME1,
        RPCPORT1,
        PUSHPORT1,
        FETCHPORT1,
        REPLICATEPORT1,
        new HashMap<>(),
        userResourceConsumption1,
        new HashMap<>(),
        1,
        getNewReqeustId());

    Assert.assertEquals(statusSystem.blacklist.size(), 1);

    statusSystem.handleWorkerHeartbeat(
        HOSTNAME2,
        RPCPORT2,
        PUSHPORT2,
        FETCHPORT2,
        REPLICATEPORT2,
        new HashMap<>(),
        userResourceConsumption2,
        new HashMap<>(),
        1,
        getNewReqeustId());

    Assert.assertEquals(statusSystem.blacklist.size(), 2);

    statusSystem.handleWorkerHeartbeat(
        HOSTNAME1,
        RPCPORT1,
        PUSHPORT1,
        FETCHPORT1,
        REPLICATEPORT3,
        disks1,
        userResourceConsumption1,
        new HashMap<>(),
        1,
        getNewReqeustId());

    Assert.assertEquals(statusSystem.blacklist.size(), 2);
  }

  @Test
  public void testHandleReportWorkerFailure() {
    statusSystem.handleRegisterWorker(
        HOSTNAME1,
        RPCPORT1,
        PUSHPORT1,
        FETCHPORT1,
        REPLICATEPORT1,
        disks1,
        userResourceConsumption1,
        getNewReqeustId());
    statusSystem.handleRegisterWorker(
        HOSTNAME2,
        RPCPORT2,
        PUSHPORT2,
        FETCHPORT2,
        REPLICATEPORT2,
        disks2,
        userResourceConsumption2,
        getNewReqeustId());
    statusSystem.handleRegisterWorker(
        HOSTNAME3,
        RPCPORT3,
        PUSHPORT3,
        FETCHPORT3,
        REPLICATEPORT3,
        disks3,
        userResourceConsumption3,
        getNewReqeustId());

    WorkerInfo workerInfo1 =
        new WorkerInfo(
            HOSTNAME1,
            RPCPORT1,
            PUSHPORT1,
            FETCHPORT1,
            REPLICATEPORT1,
            disks1,
            userResourceConsumption1,
            dummyRef);
    WorkerInfo workerInfo2 =
        new WorkerInfo(
            HOSTNAME2,
            RPCPORT2,
            PUSHPORT2,
            FETCHPORT2,
            REPLICATEPORT2,
            disks2,
            userResourceConsumption2,
            dummyRef);

    List<WorkerInfo> failedWorkers = new ArrayList<>();
    failedWorkers.add(workerInfo1);

    statusSystem.handleReportWorkerUnavailable(failedWorkers, getNewReqeustId());
    assert 1 == statusSystem.blacklist.size();
  }

  @Test
  public void testHandleUpdatePartitionSize() {
    statusSystem.handleUpdatePartitionSize();
  }
}
