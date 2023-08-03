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

package org.apache.uniffle.server;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.client.api.CoordinatorClient;
import org.apache.uniffle.client.factory.CoordinatorClientFactory;
import org.apache.uniffle.client.request.RssSendHeartBeatRequest;
import org.apache.uniffle.client.response.RssSendHeartBeatResponse;
import org.apache.uniffle.common.rpc.StatusCode;
import org.apache.uniffle.common.storage.StorageInfo;
import org.apache.uniffle.common.util.ThreadUtils;

public class RegisterHeartBeat {

  private static final Logger LOG = LoggerFactory.getLogger(RegisterHeartBeat.class);

  private final long heartBeatInitialDelay;
  private final long heartBeatInterval;
  private final ShuffleServer shuffleServer;
  private final String coordinatorQuorum;
  private final List<CoordinatorClient> coordinatorClients;
  private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(
      ThreadUtils.getThreadFactory("startHeartBeat-%d"));
  private final ExecutorService heartBeatExecutorService;
  private long heartBeatTimeout;

  public RegisterHeartBeat(ShuffleServer shuffleServer) {
    ShuffleServerConf conf = shuffleServer.getShuffleServerConf();
    this.heartBeatInitialDelay = conf.getLong(ShuffleServerConf.SERVER_HEARTBEAT_DELAY);
    this.heartBeatInterval = conf.getLong(ShuffleServerConf.SERVER_HEARTBEAT_INTERVAL);
    this.heartBeatTimeout = conf.getLong(ShuffleServerConf.SERVER_HEARTBEAT_TIMEOUT);
    this.coordinatorQuorum = conf.getString(ShuffleServerConf.RSS_COORDINATOR_QUORUM);
    CoordinatorClientFactory factory =
        new CoordinatorClientFactory(conf.get(ShuffleServerConf.RSS_CLIENT_TYPE));
    this.coordinatorClients = factory.createCoordinatorClient(this.coordinatorQuorum);
    this.shuffleServer = shuffleServer;
    this.heartBeatExecutorService = Executors.newFixedThreadPool(
        conf.getInteger(ShuffleServerConf.SERVER_HEARTBEAT_THREAD_NUM),
        ThreadUtils.getThreadFactory("sendHeartBeat-%d"));
  }

  public void startHeartBeat() {
    LOG.info("Start heartbeat to coordinator {} after {}ms and interval is {}ms",
        coordinatorQuorum, heartBeatInitialDelay, heartBeatInterval);
    Runnable runnable = () -> {
      try {
        sendHeartBeat(
            shuffleServer.getId(),
            shuffleServer.getIp(),
            shuffleServer.getPort(),
            shuffleServer.getUsedMemory(),
            shuffleServer.getPreAllocatedMemory(),
            shuffleServer.getAvailableMemory(),
            shuffleServer.getEventNumInFlush(),
            shuffleServer.getTags(),
            shuffleServer.isHealthy(),
            shuffleServer.getStorageManager().getStorageInfo());
      } catch (Exception e) {
        LOG.warn("Error happened when send heart beat to coordinator");
      }
    };
    service.scheduleAtFixedRate(runnable, heartBeatInitialDelay, heartBeatInterval, TimeUnit.MILLISECONDS);
  }

  @VisibleForTesting
  boolean sendHeartBeat(
      String id,
      String ip,
      int port,
      long usedMemory,
      long preAllocatedMemory,
      long availableMemory,
      int eventNumInFlush,
      Set<String> tags,
      boolean isHealthy,
      Map<String, StorageInfo> localStorageInfo) {
    boolean sendSuccessfully = false;
    RssSendHeartBeatRequest request = new RssSendHeartBeatRequest(
        id,
        ip,
        port,
        usedMemory,
        preAllocatedMemory,
        availableMemory,
        eventNumInFlush,
        heartBeatTimeout,
        tags,
        isHealthy,
        localStorageInfo);
    List<Future<RssSendHeartBeatResponse>> respFutures = coordinatorClients
        .stream()
        .map(client -> heartBeatExecutorService.submit(() -> client.sendHeartBeat(request)))
        .collect(Collectors.toList());

    String msg = "";
    for (Future<RssSendHeartBeatResponse> rf : respFutures) {
      try {
        if (rf.get(request.getTimeout() * 2, TimeUnit.MILLISECONDS).getStatusCode()
            == StatusCode.SUCCESS) {
          sendSuccessfully = true;
        }
      } catch (Exception e) {
        msg = e.getMessage();
      }
    }

    if (!sendSuccessfully) {
      LOG.error(msg);
    }

    return sendSuccessfully;
  }

  public void shutdown() {
    heartBeatExecutorService.shutdownNow();
    service.shutdownNow();
  }
}
