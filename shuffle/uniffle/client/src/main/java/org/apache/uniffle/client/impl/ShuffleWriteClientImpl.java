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

package org.apache.uniffle.client.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.roaringbitmap.longlong.Roaring64NavigableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.client.api.CoordinatorClient;
import org.apache.uniffle.client.api.ShuffleServerClient;
import org.apache.uniffle.client.api.ShuffleWriteClient;
import org.apache.uniffle.client.factory.CoordinatorClientFactory;
import org.apache.uniffle.client.factory.ShuffleServerClientFactory;
import org.apache.uniffle.client.request.RssAppHeartBeatRequest;
import org.apache.uniffle.client.request.RssApplicationInfoRequest;
import org.apache.uniffle.client.request.RssFetchClientConfRequest;
import org.apache.uniffle.client.request.RssFetchRemoteStorageRequest;
import org.apache.uniffle.client.request.RssFinishShuffleRequest;
import org.apache.uniffle.client.request.RssGetShuffleAssignmentsRequest;
import org.apache.uniffle.client.request.RssGetShuffleResultForMultiPartRequest;
import org.apache.uniffle.client.request.RssGetShuffleResultRequest;
import org.apache.uniffle.client.request.RssRegisterShuffleRequest;
import org.apache.uniffle.client.request.RssReportShuffleResultRequest;
import org.apache.uniffle.client.request.RssSendCommitRequest;
import org.apache.uniffle.client.request.RssSendShuffleDataRequest;
import org.apache.uniffle.client.request.RssUnregisterShuffleRequest;
import org.apache.uniffle.client.response.ClientResponse;
import org.apache.uniffle.client.response.RssAppHeartBeatResponse;
import org.apache.uniffle.client.response.RssApplicationInfoResponse;
import org.apache.uniffle.client.response.RssFetchClientConfResponse;
import org.apache.uniffle.client.response.RssFetchRemoteStorageResponse;
import org.apache.uniffle.client.response.RssFinishShuffleResponse;
import org.apache.uniffle.client.response.RssGetShuffleAssignmentsResponse;
import org.apache.uniffle.client.response.RssGetShuffleResultResponse;
import org.apache.uniffle.client.response.RssRegisterShuffleResponse;
import org.apache.uniffle.client.response.RssReportShuffleResultResponse;
import org.apache.uniffle.client.response.RssSendCommitResponse;
import org.apache.uniffle.client.response.RssSendShuffleDataResponse;
import org.apache.uniffle.client.response.RssUnregisterShuffleResponse;
import org.apache.uniffle.client.response.SendShuffleDataResult;
import org.apache.uniffle.client.util.ClientUtils;
import org.apache.uniffle.common.ClientType;
import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.ShuffleAssignmentsInfo;
import org.apache.uniffle.common.ShuffleBlockInfo;
import org.apache.uniffle.common.ShuffleDataDistributionType;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.common.rpc.StatusCode;
import org.apache.uniffle.common.util.ThreadUtils;

public class ShuffleWriteClientImpl implements ShuffleWriteClient {

  private static final Logger LOG = LoggerFactory.getLogger(ShuffleWriteClientImpl.class);

  private String clientType;
  private int retryMax;
  private long retryIntervalMax;
  private List<CoordinatorClient> coordinatorClients = Lists.newLinkedList();
  //appId -> shuffleId -> servers
  private Map<String, Map<Integer, Set<ShuffleServerInfo>>> shuffleServerInfoMap = Maps.newConcurrentMap();
  private CoordinatorClientFactory coordinatorClientFactory;
  private ExecutorService heartBeatExecutorService;
  private int replica;
  private int replicaWrite;
  private int replicaRead;
  private boolean replicaSkipEnabled;
  private int dataCommitPoolSize = -1;
  private final ExecutorService dataTransferPool;
  private final int unregisterThreadPoolSize;
  private final int unregisterRequestTimeSec;
  private Set<ShuffleServerInfo> defectiveServers;

  public ShuffleWriteClientImpl(
      String clientType,
      int retryMax,
      long retryIntervalMax,
      int heartBeatThreadNum,
      int replica,
      int replicaWrite,
      int replicaRead,
      boolean replicaSkipEnabled,
      int dataTransferPoolSize,
      int dataCommitPoolSize,
      int unregisterThreadPoolSize,
      int unregisterRequestTimeSec) {
    this.clientType = clientType;
    this.retryMax = retryMax;
    this.retryIntervalMax = retryIntervalMax;
    this.coordinatorClientFactory = new CoordinatorClientFactory(ClientType.valueOf(clientType));
    this.heartBeatExecutorService = Executors.newFixedThreadPool(heartBeatThreadNum,
        ThreadUtils.getThreadFactory("client-heartbeat-%d"));
    this.replica = replica;
    this.replicaWrite = replicaWrite;
    this.replicaRead = replicaRead;
    this.replicaSkipEnabled = replicaSkipEnabled;
    this.dataTransferPool = Executors.newFixedThreadPool(dataTransferPoolSize);
    this.dataCommitPoolSize = dataCommitPoolSize;
    this.unregisterThreadPoolSize = unregisterThreadPoolSize;
    this.unregisterRequestTimeSec = unregisterRequestTimeSec;
    if (replica > 1) {
      defectiveServers = Sets.newConcurrentHashSet();
    }
  }

  private boolean sendShuffleDataAsync(
      String appId,
      Map<ShuffleServerInfo, Map<Integer, Map<Integer, List<ShuffleBlockInfo>>>> serverToBlocks,
      Map<ShuffleServerInfo, List<Long>> serverToBlockIds,
      Map<Long, AtomicInteger> blockIdsTracker, boolean allowFastFail,
      Supplier<Boolean> needCancelRequest) {

    if (serverToBlockIds == null) {
      return true;
    }

    // If one or more servers is failed, the sending is not totally successful.
    List<CompletableFuture<Boolean>> futures = new ArrayList<>();
    for (Map.Entry<ShuffleServerInfo, Map<Integer, Map<Integer, List<ShuffleBlockInfo>>>> entry :
        serverToBlocks.entrySet()) {
      CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
        if (needCancelRequest.get()) {
          LOG.info("The upstream task has been failed. Abort this data send.");
          return true;
        }
        ShuffleServerInfo ssi = entry.getKey();
        try {
          Map<Integer, Map<Integer, List<ShuffleBlockInfo>>> shuffleIdToBlocks = entry.getValue();
          // todo: compact unnecessary blocks that reach replicaWrite
          RssSendShuffleDataRequest request = new RssSendShuffleDataRequest(
              appId, retryMax, retryIntervalMax, shuffleIdToBlocks);
          long s = System.currentTimeMillis();
          RssSendShuffleDataResponse response = getShuffleServerClient(ssi).sendShuffleData(request);

          String logMsg = String.format("ShuffleWriteClientImpl sendShuffleData with %s blocks to %s cost: %s(ms)",
              serverToBlockIds.get(ssi).size(), ssi.getId(), System.currentTimeMillis() - s);

          if (response.getStatusCode() == StatusCode.SUCCESS) {
            // mark a replica of block that has been sent
            serverToBlockIds.get(ssi).forEach(block -> blockIdsTracker.get(block).incrementAndGet());
            if (defectiveServers != null) {
              defectiveServers.remove(ssi);
            }
            LOG.info("{} successfully.", logMsg);
          } else {
            if (defectiveServers != null) {
              defectiveServers.add(ssi);
            }
            LOG.warn("{}, it failed wth statusCode[{}]", logMsg, response.getStatusCode());
            return false;
          }
        } catch (Exception e) {
          if (defectiveServers != null) {
            defectiveServers.add(ssi);
          }
          LOG.warn("Send: " + serverToBlockIds.get(ssi).size() + " blocks to [" + ssi.getId() + "] failed.", e);
          return false;
        }
        return true;
      }, dataTransferPool);
      futures.add(future);
    }

    boolean result = ClientUtils.waitUntilDoneOrFail(futures, allowFastFail);
    if (!result) {
      LOG.error("Some shuffle data can't be sent to shuffle-server, is fast fail: {}, cancelled task size: {}",
          allowFastFail, futures.size());
    }
    return result;
  }

  void genServerToBlocks(
      ShuffleBlockInfo sbi,
      List<ShuffleServerInfo> serverList,
      int replicaNum,
      Collection<ShuffleServerInfo> excludeServers,
      Map<ShuffleServerInfo, Map<Integer, Map<Integer, List<ShuffleBlockInfo>>>> serverToBlocks,
      Map<ShuffleServerInfo, List<Long>> serverToBlockIds,
      boolean excludeDefectiveServers) {
    if (replicaNum <= 0) {
      return;
    }

    Stream<ShuffleServerInfo> servers;
    if (excludeDefectiveServers && CollectionUtils.isNotEmpty(defectiveServers)) {
      servers = Stream.concat(serverList.stream().filter(x -> !defectiveServers.contains(x)),
          serverList.stream().filter(defectiveServers::contains));
    } else {
      servers = serverList.stream();
    }
    if (excludeServers != null) {
      servers = servers.filter(x -> !excludeServers.contains(x));
    }

    Stream<ShuffleServerInfo> selected = servers.limit(replicaNum);
    if (excludeServers != null) {
      selected = selected.peek(excludeServers::add);
    }
    selected.forEach(ssi -> {
      serverToBlockIds.computeIfAbsent(ssi, id -> Lists.newArrayList())
          .add(sbi.getBlockId());
      serverToBlocks.computeIfAbsent(ssi, id -> Maps.newHashMap())
          .computeIfAbsent(sbi.getShuffleId(), id -> Maps.newHashMap())
          .computeIfAbsent(sbi.getPartitionId(), id -> Lists.newArrayList())
          .add(sbi);
    });
  }

  /**
   * The batch of sending belongs to the same task
   */
  @Override
  public SendShuffleDataResult sendShuffleData(String appId, List<ShuffleBlockInfo> shuffleBlockInfoList,
      Supplier<Boolean> needCancelRequest) {

    // shuffleServer -> shuffleId -> partitionId -> blocks
    Map<ShuffleServerInfo, Map<Integer,
        Map<Integer, List<ShuffleBlockInfo>>>> primaryServerToBlocks = Maps.newHashMap();
    Map<ShuffleServerInfo, Map<Integer,
        Map<Integer, List<ShuffleBlockInfo>>>> secondaryServerToBlocks = Maps.newHashMap();
    Map<ShuffleServerInfo, List<Long>> primaryServerToBlockIds = Maps.newHashMap();
    Map<ShuffleServerInfo, List<Long>> secondaryServerToBlockIds = Maps.newHashMap();

    // send shuffle block to shuffle server
    // for all ShuffleBlockInfo, create the data structure as shuffleServer -> shuffleId -> partitionId -> blocks
    // it will be helpful to send rpc request to shuffleServer

    // In order to reduce the data to send in quorum protocol,
    // we split these blocks into two rounds: primary and secondary.
    // The primary round contains [0, replicaWrite) replicas,
    // which is minimum number when there is no sending server failures.
    // The secondary round contains [replicaWrite, replica) replicas,
    // which is minimum number when there is at most *replicaWrite - replica* sending server failures.
    for (ShuffleBlockInfo sbi : shuffleBlockInfoList) {
      List<ShuffleServerInfo> allServers = sbi.getShuffleServerInfos();
      if (replicaSkipEnabled) {
        Set<ShuffleServerInfo> excludeServers = Sets.newHashSet();
        genServerToBlocks(sbi, allServers, replicaWrite, excludeServers,
            primaryServerToBlocks, primaryServerToBlockIds, true);
        genServerToBlocks(sbi, allServers,replica - replicaWrite,
            excludeServers, secondaryServerToBlocks, secondaryServerToBlockIds, false);
      } else {
        // When replicaSkip is disabled, we send data to all replicas within one round.
        genServerToBlocks(sbi, allServers, allServers.size(),
            null, primaryServerToBlocks, primaryServerToBlockIds, false);
      }
    }

    // maintain the count of blocks that have been sent to the server
    // unnecessary to use concurrent hashmap here unless you need to insert or delete entries in other threads
    // AtomicInteger is enough to reflect value changes in other threads
    Map<Long, AtomicInteger> blockIdsTracker = Maps.newHashMap();
    primaryServerToBlockIds.values().forEach(
        blockList -> blockList.forEach(block -> blockIdsTracker.put(block, new AtomicInteger(0)))
    );
    secondaryServerToBlockIds.values().forEach(
        blockList -> blockList.forEach(block -> blockIdsTracker.put(block, new AtomicInteger(0)))
    );

    Set<Long> failedBlockIds = Sets.newConcurrentHashSet();
    Set<Long> successBlockIds = Sets.newConcurrentHashSet();
    // if send block failed, the task will fail
    // todo: better to have fallback solution when send to multiple servers

    // sent the primary round of blocks.
    boolean isAllSuccess = sendShuffleDataAsync(
        appId,
        primaryServerToBlocks,
        primaryServerToBlockIds,
        blockIdsTracker,
        secondaryServerToBlocks.isEmpty(),
        needCancelRequest
    );

    // The secondary round of blocks is sent only when the primary group issues failed sending.
    // This should be infrequent.
    // Even though the secondary round may send blocks more than replicaWrite replicas,
    // we do not apply complicated skipping logic, because server crash is rare in production environment.
    if (!isAllSuccess && !secondaryServerToBlocks.isEmpty() && !needCancelRequest.get()) {
      LOG.info("The sending of primary round is failed partially, so start the secondary round");
      sendShuffleDataAsync(
          appId,
          secondaryServerToBlocks,
          secondaryServerToBlockIds,
          blockIdsTracker,
          true,
          needCancelRequest
      );
    }

    // check success and failed blocks according to the replicaWrite
    blockIdsTracker.entrySet().forEach(blockCt -> {
      long blockId = blockCt.getKey();
      int count = blockCt.getValue().get();
      if (count >= replicaWrite) {
        successBlockIds.add(blockId);
      } else {
        failedBlockIds.add(blockId);
      }
    });
    return new SendShuffleDataResult(successBlockIds, failedBlockIds);
  }

  /**
   * This method will wait until all shuffle data have been flushed
   * to durable storage in assigned shuffle servers.
   * @param shuffleServerInfoSet
   * @param appId
   * @param shuffleId
   * @param numMaps
   * @return
   */
  @Override
  public boolean sendCommit(Set<ShuffleServerInfo> shuffleServerInfoSet, String appId, int shuffleId, int numMaps) {
    ForkJoinPool forkJoinPool = new ForkJoinPool(
        dataCommitPoolSize == -1 ? shuffleServerInfoSet.size() : dataCommitPoolSize
    );
    AtomicInteger successfulCommit = new AtomicInteger(0);
    try {
      forkJoinPool.submit(() -> {
        shuffleServerInfoSet.parallelStream().forEach(ssi -> {
          RssSendCommitRequest request = new RssSendCommitRequest(appId, shuffleId);
          String errorMsg = "Failed to commit shuffle data to " + ssi + " for shuffleId[" + shuffleId + "]";
          long startTime = System.currentTimeMillis();
          try {
            RssSendCommitResponse response = getShuffleServerClient(ssi).sendCommit(request);
            if (response.getStatusCode() == StatusCode.SUCCESS) {
              int commitCount = response.getCommitCount();
              LOG.info("Successfully sendCommit for appId[" + appId + "], shuffleId[" + shuffleId
                  + "] to ShuffleServer[" + ssi.getId() + "], cost "
                  + (System.currentTimeMillis() - startTime) + " ms, got committed maps["
                  + commitCount + "], map number of stage is " + numMaps);
              if (commitCount >= numMaps) {
                RssFinishShuffleResponse rfsResponse =
                    getShuffleServerClient(ssi).finishShuffle(new RssFinishShuffleRequest(appId, shuffleId));
                if (rfsResponse.getStatusCode() != StatusCode.SUCCESS) {
                  String msg = "Failed to finish shuffle to " + ssi + " for shuffleId[" + shuffleId
                      + "] with statusCode " + rfsResponse.getStatusCode();
                  LOG.error(msg);
                  throw new Exception(msg);
                } else {
                  LOG.info("Successfully finish shuffle to " + ssi + " for shuffleId[" + shuffleId + "]");
                }
              }
            } else {
              String msg = errorMsg + " with statusCode " + response.getStatusCode();
              LOG.error(msg);
              throw new Exception(msg);
            }
            successfulCommit.incrementAndGet();
          } catch (Exception e) {
            LOG.error(errorMsg, e);
          }
        });
      }).join();
    } finally {
      forkJoinPool.shutdownNow();
    }

    // check if every commit/finish call is successful
    return successfulCommit.get() == shuffleServerInfoSet.size();
  }

  @Override
  public void registerShuffle(
      ShuffleServerInfo shuffleServerInfo,
      String appId,
      int shuffleId,
      List<PartitionRange> partitionRanges,
      RemoteStorageInfo remoteStorage,
      ShuffleDataDistributionType dataDistributionType) {
    String user = null;
    try {
      user = UserGroupInformation.getCurrentUser().getShortUserName();
    } catch (Exception e) {
      LOG.error("Error on getting user from ugi.", e);
    }
    LOG.info("User: {}", user);

    RssRegisterShuffleRequest request =
        new RssRegisterShuffleRequest(appId, shuffleId, partitionRanges, remoteStorage, user, dataDistributionType);
    RssRegisterShuffleResponse response = getShuffleServerClient(shuffleServerInfo).registerShuffle(request);

    String msg = "Error happened when registerShuffle with appId[" + appId + "], shuffleId[" + shuffleId
        + "], " + shuffleServerInfo;
    throwExceptionIfNecessary(response, msg);
    addShuffleServer(appId, shuffleId, shuffleServerInfo);
  }

  @Override
  public void registerCoordinators(String coordinators) {
    List<CoordinatorClient> clients = coordinatorClientFactory.createCoordinatorClient(coordinators);
    coordinatorClients.addAll(clients);
  }

  @Override
  public Map<String, String> fetchClientConf(int timeoutMs) {
    RssFetchClientConfResponse response =
        new RssFetchClientConfResponse(StatusCode.INTERNAL_ERROR, "Empty coordinator clients");
    for (CoordinatorClient coordinatorClient : coordinatorClients) {
      response = coordinatorClient.fetchClientConf(new RssFetchClientConfRequest(timeoutMs));
      if (response.getStatusCode() == StatusCode.SUCCESS) {
        LOG.info("Success to get conf from {}", coordinatorClient.getDesc());
        break;
      } else {
        LOG.warn("Fail to get conf from {}", coordinatorClient.getDesc());
      }
    }
    return response.getClientConf();
  }

  @Override
  public RemoteStorageInfo fetchRemoteStorage(String appId) {
    RemoteStorageInfo remoteStorage = new RemoteStorageInfo("");
    for (CoordinatorClient coordinatorClient : coordinatorClients) {
      RssFetchRemoteStorageResponse response =
          coordinatorClient.fetchRemoteStorage(new RssFetchRemoteStorageRequest(appId));
      if (response.getStatusCode() == StatusCode.SUCCESS) {
        remoteStorage = response.getRemoteStorageInfo();
        LOG.info("Success to get storage {} from {}", remoteStorage, coordinatorClient.getDesc());
        break;
      } else {
        LOG.warn("Fail to get conf from {}", coordinatorClient.getDesc());
      }
    }
    return remoteStorage;
  }

  @Override
  public ShuffleAssignmentsInfo getShuffleAssignments(String appId, int shuffleId, int partitionNum,
      int partitionNumPerRange, Set<String> requiredTags, int assignmentShuffleServerNumber,
      int estimateTaskConcurrency) {
    RssGetShuffleAssignmentsRequest request = new RssGetShuffleAssignmentsRequest(
        appId, shuffleId, partitionNum, partitionNumPerRange, replica, requiredTags,
        assignmentShuffleServerNumber, estimateTaskConcurrency);

    RssGetShuffleAssignmentsResponse response = new RssGetShuffleAssignmentsResponse(StatusCode.INTERNAL_ERROR);
    for (CoordinatorClient coordinatorClient : coordinatorClients) {
      try {
        response = coordinatorClient.getShuffleAssignments(request);
      } catch (Exception e) {
        LOG.error(e.getMessage());
      }

      if (response.getStatusCode() == StatusCode.SUCCESS) {
        LOG.info("Success to get shuffle server assignment from {}", coordinatorClient.getDesc());
        break;
      }
    }
    String msg = "Error happened when getShuffleAssignments with appId[" + appId + "], shuffleId[" + shuffleId
        + "], numMaps[" + partitionNum + "], partitionNumPerRange[" + partitionNumPerRange + "] to coordinator. "
        + "Error message: " + response.getMessage();
    throwExceptionIfNecessary(response, msg);

    return new ShuffleAssignmentsInfo(response.getPartitionToServers(), response.getServerToPartitionRanges());
  }

  @Override
  public void reportShuffleResult(
      Map<Integer, List<ShuffleServerInfo>> partitionToServers,
      String appId,
      int shuffleId,
      long taskAttemptId,
      Map<Integer, List<Long>> partitionToBlockIds,
      int bitmapNum) {
    Map<ShuffleServerInfo, List<Integer>> groupedPartitions = Maps.newHashMap();
    Map<Integer, Integer> partitionReportTracker = Maps.newHashMap();
    for (Map.Entry<Integer, List<ShuffleServerInfo>> entry : partitionToServers.entrySet()) {
      int partitionIdx = entry.getKey();
      for (ShuffleServerInfo ssi : entry.getValue()) {
        if (!groupedPartitions.containsKey(ssi)) {
          groupedPartitions.put(ssi, Lists.newArrayList());
        }
        groupedPartitions.get(ssi).add(partitionIdx);
      }
      if (CollectionUtils.isNotEmpty(partitionToBlockIds.get(partitionIdx))) {
        partitionReportTracker.putIfAbsent(partitionIdx, 0);
      }
    }

    for (Map.Entry<ShuffleServerInfo, List<Integer>> entry : groupedPartitions.entrySet()) {
      Map<Integer, List<Long>> requestBlockIds = Maps.newHashMap();
      for (Integer partitionId : entry.getValue()) {
        List<Long> blockIds = partitionToBlockIds.get(partitionId);
        if (CollectionUtils.isNotEmpty(blockIds)) {
          requestBlockIds.put(partitionId, blockIds);
        }
      }
      if (requestBlockIds.isEmpty()) {
        continue;
      }
      RssReportShuffleResultRequest request = new RssReportShuffleResultRequest(
          appId, shuffleId, taskAttemptId, requestBlockIds, bitmapNum);
      ShuffleServerInfo ssi = entry.getKey();
      try {
        RssReportShuffleResultResponse response = getShuffleServerClient(ssi).reportShuffleResult(request);
        if (response.getStatusCode() == StatusCode.SUCCESS) {
          LOG.info("Report shuffle result to " + ssi + " for appId[" + appId
              + "], shuffleId[" + shuffleId + "] successfully");
          for (Integer partitionId : requestBlockIds.keySet()) {
            partitionReportTracker.put(partitionId, partitionReportTracker.get(partitionId) + 1);
          }
        } else {
          LOG.warn("Report shuffle result to " + ssi + " for appId[" + appId
              + "], shuffleId[" + shuffleId + "] failed with " + response.getStatusCode());
        }
      } catch (Exception e) {
        LOG.warn("Report shuffle result is failed to " + ssi
            + " for appId[" + appId + "], shuffleId[" + shuffleId + "]");
      }
    }
    // quorum check
    for (Map.Entry<Integer, Integer> entry: partitionReportTracker.entrySet()) {
      if (entry.getValue() < replicaWrite) {
        throw new RssException("Quorum check of report shuffle result is failed for appId["
            + appId + "], shuffleId[" + shuffleId + "]");
      }
    }
  }

  @Override
  public Roaring64NavigableMap getShuffleResult(String clientType, Set<ShuffleServerInfo> shuffleServerInfoSet,
      String appId, int shuffleId, int partitionId) {
    RssGetShuffleResultRequest request = new RssGetShuffleResultRequest(
        appId, shuffleId, partitionId);
    boolean isSuccessful = false;
    Roaring64NavigableMap blockIdBitmap = Roaring64NavigableMap.bitmapOf();
    int successCnt = 0;
    for (ShuffleServerInfo ssi : shuffleServerInfoSet) {
      try {
        RssGetShuffleResultResponse response = getShuffleServerClient(ssi).getShuffleResult(request);
        if (response.getStatusCode() == StatusCode.SUCCESS) {
          // merge into blockIds from multiple servers.
          Roaring64NavigableMap blockIdBitmapOfServer = response.getBlockIdBitmap();
          blockIdBitmap.or(blockIdBitmapOfServer);
          successCnt++;
          if (successCnt >= replicaRead) {
            isSuccessful = true;
            break;
          }
        }
      } catch (Exception e) {
        LOG.warn("Get shuffle result is failed from " + ssi
            + " for appId[" + appId + "], shuffleId[" + shuffleId + "]");
      }
    }
    if (!isSuccessful) {
      throw new RssException("Get shuffle result is failed for appId["
          + appId + "], shuffleId[" + shuffleId + "]");
    }
    return blockIdBitmap;
  }

  @Override
  public Roaring64NavigableMap getShuffleResultForMultiPart(String clientType,
      Map<ShuffleServerInfo, Set<Integer>> serverToPartitions, String appId, int shuffleId) {
    Map<Integer, Integer> partitionReadSuccess = Maps.newHashMap();
    Roaring64NavigableMap blockIdBitmap = Roaring64NavigableMap.bitmapOf();
    for (Map.Entry<ShuffleServerInfo, Set<Integer>> entry : serverToPartitions.entrySet()) {
      ShuffleServerInfo shuffleServerInfo = entry.getKey();
      Set<Integer> requestPartitions = Sets.newHashSet();
      for (Integer partitionId : entry.getValue()) {
        partitionReadSuccess.putIfAbsent(partitionId, 0);
        if (partitionReadSuccess.get(partitionId) < replicaRead) {
          requestPartitions.add(partitionId);
        }
      }
      RssGetShuffleResultForMultiPartRequest request = new RssGetShuffleResultForMultiPartRequest(
          appId, shuffleId, requestPartitions);
      try {
        RssGetShuffleResultResponse response =
            getShuffleServerClient(shuffleServerInfo).getShuffleResultForMultiPart(request);
        if (response.getStatusCode() == StatusCode.SUCCESS) {
          // merge into blockIds from multiple servers.
          Roaring64NavigableMap blockIdBitmapOfServer = response.getBlockIdBitmap();
          blockIdBitmap.or(blockIdBitmapOfServer);
          for (Integer partitionId : requestPartitions) {
            Integer oldVal = partitionReadSuccess.get(partitionId);
            partitionReadSuccess.put(partitionId, oldVal + 1);
          }
        }
      } catch (Exception e) {
        LOG.warn("Get shuffle result is failed from " + shuffleServerInfo + " for appId[" + appId
            + "], shuffleId[" + shuffleId + "], requestPartitions" + requestPartitions);
      }
    }
    boolean isSuccessful = partitionReadSuccess.entrySet().stream().allMatch(x -> x.getValue() >= replicaRead);
    if (!isSuccessful) {
      throw new RssException("Get shuffle result is failed for appId[" + appId + "], shuffleId[" + shuffleId + "]");
    }
    return blockIdBitmap;
  }

  @Override
  public void registerApplicationInfo(String appId, long timeoutMs, String user) {
    RssApplicationInfoRequest request = new RssApplicationInfoRequest(appId, timeoutMs, user);
    List<Callable<Void>> callableList = Lists.newArrayList();
    coordinatorClients.forEach(coordinatorClient -> {
      callableList.add(() -> {
        try {
          RssApplicationInfoResponse response = coordinatorClient.registerApplicationInfo(request);
          if (response.getStatusCode() != StatusCode.SUCCESS) {
            LOG.error("Failed to send applicationInfo to " + coordinatorClient.getDesc());
          } else {
            LOG.info("Successfully send applicationInfo to " + coordinatorClient.getDesc());
          }
        } catch (Exception e) {
          LOG.warn("Error happened when send applicationInfo to " + coordinatorClient.getDesc(), e);
        }
        return null;
      });
    });
    try {
      List<Future<Void>> futures = heartBeatExecutorService.invokeAll(callableList, timeoutMs, TimeUnit.MILLISECONDS);
      for (Future<Void> future : futures) {
        if (!future.isDone()) {
          future.cancel(true);
        }
      }
    } catch (InterruptedException ie) {
      LOG.warn("register application is interrupted", ie);
    }
  }

  @Override
  public void sendAppHeartbeat(String appId, long timeoutMs) {
    RssAppHeartBeatRequest request = new RssAppHeartBeatRequest(appId, timeoutMs);
    List<Callable<Void>> callableList = Lists.newArrayList();
    Set<ShuffleServerInfo> allShuffleServers = getAllShuffleServers(appId);
    allShuffleServers.forEach(shuffleServerInfo -> {
          callableList.add(() -> {
            try {
              ShuffleServerClient client =
                  ShuffleServerClientFactory.getInstance().getShuffleServerClient(clientType, shuffleServerInfo);
              RssAppHeartBeatResponse response = client.sendHeartBeat(request);
              if (response.getStatusCode() != StatusCode.SUCCESS) {
                LOG.warn("Failed to send heartbeat to " + shuffleServerInfo);
              }
            } catch (Exception e) {
              LOG.warn("Error happened when send heartbeat to " + shuffleServerInfo, e);
            }
            return null;
          });
        }
    );

    coordinatorClients.forEach(coordinatorClient -> {
      callableList.add(() -> {
        try {
          RssAppHeartBeatResponse response = coordinatorClient.sendAppHeartBeat(request);
          if (response.getStatusCode() != StatusCode.SUCCESS) {
            LOG.warn("Failed to send heartbeat to " + coordinatorClient.getDesc());
          } else {
            LOG.info("Successfully send heartbeat to " + coordinatorClient.getDesc());
          }
        } catch (Exception e) {
          LOG.warn("Error happened when send heartbeat to " + coordinatorClient.getDesc(), e);
        }
        return null;
      });
    });
    try {
      List<Future<Void>> futures = heartBeatExecutorService.invokeAll(callableList, timeoutMs, TimeUnit.MILLISECONDS);
      for (Future<Void> future : futures) {
        if (!future.isDone()) {
          future.cancel(true);
        }
      }
    } catch (InterruptedException ie) {
      LOG.warn("heartbeat is interrupted", ie);
    }
  }

  @Override
  public void close() {
    heartBeatExecutorService.shutdownNow();
    coordinatorClients.forEach(CoordinatorClient::close);
    dataTransferPool.shutdownNow();
  }

  @Override
  public void unregisterShuffle(String appId, int shuffleId) {
    RssUnregisterShuffleRequest request = new RssUnregisterShuffleRequest(appId, shuffleId);
    List<Callable<Void>> callableList = Lists.newArrayList();

    Map<Integer, Set<ShuffleServerInfo>> appServerMap = shuffleServerInfoMap.get(appId);
    if (appServerMap == null) {
      return;
    }
    Set<ShuffleServerInfo> shuffleServerInfos = appServerMap.get(shuffleId);
    if (shuffleServerInfos == null) {
      return;
    }

    shuffleServerInfos.forEach(shuffleServerInfo -> {
          callableList.add(() -> {
            try {
              ShuffleServerClient client =
                  ShuffleServerClientFactory.getInstance().getShuffleServerClient(clientType, shuffleServerInfo);
              RssUnregisterShuffleResponse response = client.unregisterShuffle(request);
              if (response.getStatusCode() != StatusCode.SUCCESS) {
                LOG.warn("Failed to unregister shuffle to " + shuffleServerInfo);
              }
            } catch (Exception e) {
              LOG.warn("Error happened when unregistering to " + shuffleServerInfo, e);
            }
            return null;
          });
        }
    );

    ExecutorService executorService = null;
    try {
      executorService =
          Executors.newFixedThreadPool(
              Math.min(unregisterThreadPoolSize, shuffleServerInfos.size()),
              ThreadUtils.getThreadFactory("unregister-shuffle-%d")
          );
      List<Future<Void>> futures = executorService.invokeAll(callableList, unregisterRequestTimeSec, TimeUnit.SECONDS);
      for (Future<Void> future : futures) {
        if (!future.isDone()) {
          future.cancel(true);
        }
      }
    } catch (InterruptedException ie) {
      LOG.warn("Unregister shuffle is interrupted", ie);
    } finally {
      if (executorService != null) {
        executorService.shutdownNow();
      }
      removeShuffleServer(appId, shuffleId);
    }
  }

  private void throwExceptionIfNecessary(ClientResponse response, String errorMsg) {
    if (response != null && response.getStatusCode() != StatusCode.SUCCESS) {
      LOG.error(errorMsg);
      throw new RssException(errorMsg);
    }
  }

  Set<ShuffleServerInfo> getAllShuffleServers(String appId) {
    Map<Integer, Set<ShuffleServerInfo>> appServerMap = shuffleServerInfoMap.get(appId);
    if (appServerMap == null) {
      return Collections.EMPTY_SET;
    }
    Set<ShuffleServerInfo> serverInfos = Sets.newHashSet();
    appServerMap.values().forEach(serverInfos::addAll);
    return serverInfos;
  }

  @VisibleForTesting
  public ShuffleServerClient getShuffleServerClient(ShuffleServerInfo shuffleServerInfo) {
    return ShuffleServerClientFactory.getInstance().getShuffleServerClient(clientType, shuffleServerInfo);
  }

  @VisibleForTesting
  Set<ShuffleServerInfo> getDefectiveServers() {
    return defectiveServers;
  }

  void addShuffleServer(String appId, int shuffleId, ShuffleServerInfo serverInfo) {
    Map<Integer, Set<ShuffleServerInfo>> appServerMap = shuffleServerInfoMap.get(appId);
    if (appServerMap == null) {
      appServerMap = Maps.newConcurrentMap();
      shuffleServerInfoMap.put(appId, appServerMap);
    }
    Set<ShuffleServerInfo> shuffleServerInfos = appServerMap.get(shuffleId);
    if (shuffleServerInfos == null) {
      shuffleServerInfos = Sets.newConcurrentHashSet();
      appServerMap.put(shuffleId, shuffleServerInfos);
    }
    shuffleServerInfos.add(serverInfo);
  }

  @VisibleForTesting
  void removeShuffleServer(String appId, int shuffleId) {
    Map<Integer, Set<ShuffleServerInfo>> appServerMap = shuffleServerInfoMap.get(appId);
    if (appServerMap != null) {
      appServerMap.remove(shuffleId);
    }
  }
}
