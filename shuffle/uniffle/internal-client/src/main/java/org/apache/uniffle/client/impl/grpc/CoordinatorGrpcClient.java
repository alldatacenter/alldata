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

package org.apache.uniffle.client.impl.grpc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.client.api.CoordinatorClient;
import org.apache.uniffle.client.request.RssAccessClusterRequest;
import org.apache.uniffle.client.request.RssAppHeartBeatRequest;
import org.apache.uniffle.client.request.RssApplicationInfoRequest;
import org.apache.uniffle.client.request.RssFetchClientConfRequest;
import org.apache.uniffle.client.request.RssFetchRemoteStorageRequest;
import org.apache.uniffle.client.request.RssGetShuffleAssignmentsRequest;
import org.apache.uniffle.client.request.RssSendHeartBeatRequest;
import org.apache.uniffle.client.response.RssAccessClusterResponse;
import org.apache.uniffle.client.response.RssAppHeartBeatResponse;
import org.apache.uniffle.client.response.RssApplicationInfoResponse;
import org.apache.uniffle.client.response.RssFetchClientConfResponse;
import org.apache.uniffle.client.response.RssFetchRemoteStorageResponse;
import org.apache.uniffle.client.response.RssGetShuffleAssignmentsResponse;
import org.apache.uniffle.client.response.RssSendHeartBeatResponse;
import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.common.rpc.StatusCode;
import org.apache.uniffle.common.storage.StorageInfo;
import org.apache.uniffle.common.storage.StorageInfoUtils;
import org.apache.uniffle.proto.CoordinatorServerGrpc;
import org.apache.uniffle.proto.CoordinatorServerGrpc.CoordinatorServerBlockingStub;
import org.apache.uniffle.proto.RssProtos;
import org.apache.uniffle.proto.RssProtos.AccessClusterRequest;
import org.apache.uniffle.proto.RssProtos.AccessClusterResponse;
import org.apache.uniffle.proto.RssProtos.ApplicationInfoRequest;
import org.apache.uniffle.proto.RssProtos.ApplicationInfoResponse;
import org.apache.uniffle.proto.RssProtos.ClientConfItem;
import org.apache.uniffle.proto.RssProtos.FetchClientConfResponse;
import org.apache.uniffle.proto.RssProtos.FetchRemoteStorageRequest;
import org.apache.uniffle.proto.RssProtos.FetchRemoteStorageResponse;
import org.apache.uniffle.proto.RssProtos.GetShuffleAssignmentsResponse;
import org.apache.uniffle.proto.RssProtos.GetShuffleServerListResponse;
import org.apache.uniffle.proto.RssProtos.PartitionRangeAssignment;
import org.apache.uniffle.proto.RssProtos.RemoteStorageConfItem;
import org.apache.uniffle.proto.RssProtos.ShuffleServerHeartBeatRequest;
import org.apache.uniffle.proto.RssProtos.ShuffleServerHeartBeatResponse;
import org.apache.uniffle.proto.RssProtos.ShuffleServerId;

public class CoordinatorGrpcClient extends GrpcClient implements CoordinatorClient {

  private static final Logger LOG = LoggerFactory.getLogger(CoordinatorGrpcClient.class);
  private CoordinatorServerBlockingStub blockingStub;

  public CoordinatorGrpcClient(String host, int port) {
    this(host, port, 3);
  }

  public CoordinatorGrpcClient(String host, int port, int maxRetryAttempts) {
    this(host, port, maxRetryAttempts, true);
  }

  public CoordinatorGrpcClient(String host, int port, int maxRetryAttempts, boolean usePlaintext) {
    super(host, port, maxRetryAttempts, usePlaintext);
    blockingStub = CoordinatorServerGrpc.newBlockingStub(channel);
  }

  public CoordinatorGrpcClient(ManagedChannel channel) {
    super(channel);
    blockingStub = CoordinatorServerGrpc.newBlockingStub(channel);
  }

  @Override
  public String getDesc() {
    return "Coordinator grpc client ref to " + host + ":" + port;
  }

  public GetShuffleServerListResponse getShuffleServerList() {
    return blockingStub.getShuffleServerList(Empty.newBuilder().build());
  }

  public ShuffleServerHeartBeatResponse doSendHeartBeat(
      String id,
      String ip,
      int port,
      long usedMemory,
      long preAllocatedMemory,
      long availableMemory,
      int eventNumInFlush,
      long timeout,
      Set<String> tags,
      boolean isHealthy,
      Map<String, StorageInfo> storageInfo) {
    ShuffleServerId serverId =
        ShuffleServerId.newBuilder().setId(id).setIp(ip).setPort(port).build();
    ShuffleServerHeartBeatRequest request =
        ShuffleServerHeartBeatRequest.newBuilder()
            .setServerId(serverId)
            .setUsedMemory(usedMemory)
            .setPreAllocatedMemory(preAllocatedMemory)
            .setAvailableMemory(availableMemory)
            .setEventNumInFlush(eventNumInFlush)
            .addAllTags(tags)
            .setIsHealthy(BoolValue.newBuilder().setValue(isHealthy).build())
            .putAllStorageInfo(StorageInfoUtils.toProto(storageInfo))
            .build();

    RssProtos.StatusCode status;
    ShuffleServerHeartBeatResponse response = null;

    try {
      response = blockingStub.withDeadlineAfter(timeout, TimeUnit.MILLISECONDS).heartbeat(request);
      status = response.getStatus();
    } catch (StatusRuntimeException e) {
      LOG.error(e.getMessage());
      status = RssProtos.StatusCode.TIMEOUT;
    } catch (Exception e) {
      LOG.error(e.getMessage());
      status = RssProtos.StatusCode.INTERNAL_ERROR;
    }

    if (response == null) {
      response = ShuffleServerHeartBeatResponse.newBuilder().setStatus(status).build();
    }

    if (status != RssProtos.StatusCode.SUCCESS) {
      LOG.error("Fail to send heartbeat to {}:{} {}", this.host, this.port, status);
    }

    return response;
  }

  public RssProtos.GetShuffleAssignmentsResponse doGetShuffleAssignments(
      String appId,
      int shuffleId,
      int numMaps,
      int partitionNumPerRange,
      int dataReplica,
      Set<String> requiredTags,
      int assignmentShuffleServerNumber,
      int estimateTaskConcurrency) {

    RssProtos.GetShuffleServerRequest getServerRequest = RssProtos.GetShuffleServerRequest.newBuilder()
        .setApplicationId(appId)
        .setShuffleId(shuffleId)
        .setPartitionNum(numMaps)
        .setPartitionNumPerRange(partitionNumPerRange)
        .setDataReplica(dataReplica)
        .addAllRequireTags(requiredTags)
        .setAssignmentShuffleServerNumber(assignmentShuffleServerNumber)
        .setEstimateTaskConcurrency(estimateTaskConcurrency)
        .build();

    return blockingStub.getShuffleAssignments(getServerRequest);
  }

  @Override
  public RssSendHeartBeatResponse sendHeartBeat(RssSendHeartBeatRequest request) {
    ShuffleServerHeartBeatResponse rpcResponse = doSendHeartBeat(
        request.getShuffleServerId(),
        request.getShuffleServerIp(),
        request.getShuffleServerPort(),
        request.getUsedMemory(),
        request.getPreAllocatedMemory(),
        request.getAvailableMemory(),
        request.getEventNumInFlush(),
        request.getTimeout(),
        request.getTags(),
        request.isHealthy(),
        request.getStorageInfo());

    RssSendHeartBeatResponse response;
    RssProtos.StatusCode statusCode = rpcResponse.getStatus();
    switch (statusCode) {
      case SUCCESS:
        response = new RssSendHeartBeatResponse(StatusCode.SUCCESS);
        break;
      case TIMEOUT:
        response = new RssSendHeartBeatResponse(StatusCode.TIMEOUT);
        break;
      default:
        response = new RssSendHeartBeatResponse(StatusCode.INTERNAL_ERROR);
    }
    return response;
  }

  @Override
  public RssAppHeartBeatResponse sendAppHeartBeat(RssAppHeartBeatRequest request) {
    RssProtos.AppHeartBeatRequest rpcRequest =
        RssProtos.AppHeartBeatRequest.newBuilder().setAppId(request.getAppId()).build();
    RssProtos.AppHeartBeatResponse rpcResponse = blockingStub
        .withDeadlineAfter(request.getTimeoutMs(), TimeUnit.MILLISECONDS).appHeartbeat(rpcRequest);
    RssAppHeartBeatResponse response;
    RssProtos.StatusCode statusCode = rpcResponse.getStatus();
    switch (statusCode) {
      case SUCCESS:
        response = new RssAppHeartBeatResponse(StatusCode.SUCCESS);
        break;
      default:
        response = new RssAppHeartBeatResponse(StatusCode.INTERNAL_ERROR);
    }
    return response;
  }

  @Override
  public RssApplicationInfoResponse registerApplicationInfo(RssApplicationInfoRequest request) {
    ApplicationInfoRequest rpcRequest =
        ApplicationInfoRequest.newBuilder().setAppId(request.getAppId()).setUser(request.getUser()).build();
    ApplicationInfoResponse rpcResponse = blockingStub
        .withDeadlineAfter(request.getTimeoutMs(), TimeUnit.MILLISECONDS).registerApplicationInfo(rpcRequest);
    RssApplicationInfoResponse response;
    RssProtos.StatusCode statusCode = rpcResponse.getStatus();
    switch (statusCode) {
      case SUCCESS:
        response = new RssApplicationInfoResponse(StatusCode.SUCCESS);
        break;
      default:
        response = new RssApplicationInfoResponse(StatusCode.INTERNAL_ERROR);
    }
    return response;
  }

  @Override
  public RssGetShuffleAssignmentsResponse getShuffleAssignments(RssGetShuffleAssignmentsRequest request) {
    RssProtos.GetShuffleAssignmentsResponse rpcResponse = doGetShuffleAssignments(
        request.getAppId(),
        request.getShuffleId(),
        request.getPartitionNum(),
        request.getPartitionNumPerRange(),
        request.getDataReplica(),
        request.getRequiredTags(),
        request.getAssignmentShuffleServerNumber(),
        request.getEstimateTaskConcurrency());

    RssGetShuffleAssignmentsResponse response;
    RssProtos.StatusCode statusCode = rpcResponse.getStatus();
    switch (statusCode) {
      case SUCCESS:
        response = new RssGetShuffleAssignmentsResponse(StatusCode.SUCCESS);
        // get all register info according to coordinator's response
        Map<ShuffleServerInfo, List<PartitionRange>> serverToPartitionRanges = getServerToPartitionRanges(rpcResponse);
        Map<Integer, List<ShuffleServerInfo>> partitionToServers = getPartitionToServers(rpcResponse);
        response.setServerToPartitionRanges(serverToPartitionRanges);
        response.setPartitionToServers(partitionToServers);
        break;
      case TIMEOUT:
        response = new RssGetShuffleAssignmentsResponse(StatusCode.TIMEOUT);
        break;
      default:
        response = new RssGetShuffleAssignmentsResponse(StatusCode.INTERNAL_ERROR, rpcResponse.getRetMsg());
    }

    return response;
  }

  @Override
  public RssAccessClusterResponse accessCluster(RssAccessClusterRequest request) {
    AccessClusterRequest rpcRequest = AccessClusterRequest
        .newBuilder()
        .setAccessId(request.getAccessId())
        .setUser(request.getUser())
        .addAllTags(request.getTags())
        .putAllExtraProperties(request.getExtraProperties())
        .build();
    AccessClusterResponse rpcResponse;
    try {
      rpcResponse = blockingStub
          .withDeadlineAfter(request.getTimeoutMs(), TimeUnit.MILLISECONDS).accessCluster(rpcRequest);
    } catch (Exception e) {
      return new RssAccessClusterResponse(StatusCode.INTERNAL_ERROR, e.getMessage());
    }

    RssAccessClusterResponse response;
    RssProtos.StatusCode statusCode = rpcResponse.getStatus();
    switch (statusCode) {
      case SUCCESS:
        response = new RssAccessClusterResponse(
            StatusCode.SUCCESS,
            rpcResponse.getRetMsg(),
            rpcResponse.getUuid()
        );
        break;
      default:
        response = new RssAccessClusterResponse(StatusCode.ACCESS_DENIED, rpcResponse.getRetMsg());
    }

    return response;
  }

  @Override
  public RssFetchClientConfResponse fetchClientConf(RssFetchClientConfRequest request) {
    FetchClientConfResponse rpcResponse;
    try {
      rpcResponse = blockingStub
          .withDeadlineAfter(request.getTimeoutMs(), TimeUnit.MILLISECONDS)
          .fetchClientConf(Empty.getDefaultInstance());
      Map<String, String> clientConf = rpcResponse
          .getClientConfList().stream().collect(Collectors.toMap(ClientConfItem::getKey, ClientConfItem::getValue));
      return new RssFetchClientConfResponse(
          StatusCode.SUCCESS,
          rpcResponse.getRetMsg(),
          clientConf);
    } catch (Exception e) {
      LOG.info(e.getMessage(), e);
      return new RssFetchClientConfResponse(StatusCode.INTERNAL_ERROR, e.getMessage());
    }
  }

  @Override
  public RssFetchRemoteStorageResponse fetchRemoteStorage(RssFetchRemoteStorageRequest request) {
    FetchRemoteStorageResponse rpcResponse;
    FetchRemoteStorageRequest rpcRequest =
        FetchRemoteStorageRequest.newBuilder().setAppId(request.getAppId()).build();
    try {
      rpcResponse = blockingStub.fetchRemoteStorage(rpcRequest);
      Map<String, String> remoteStorageConf = rpcResponse
          .getRemoteStorage()
          .getRemoteStorageConfList()
          .stream()
          .collect(Collectors.toMap(RemoteStorageConfItem::getKey, RemoteStorageConfItem::getValue));
      RssFetchRemoteStorageResponse tt = new RssFetchRemoteStorageResponse(
          StatusCode.SUCCESS,
          new RemoteStorageInfo(rpcResponse.getRemoteStorage().getPath(), remoteStorageConf));
      return tt;
    } catch (Exception e) {
      LOG.info("Failed to fetch remote storage from coordinator, " + e.getMessage(), e);
      return new RssFetchRemoteStorageResponse(StatusCode.INTERNAL_ERROR, null);
    }
  }

  // transform [startPartition, endPartition] -> [server1, server2] to
  // {partition1 -> [server1, server2], partition2 - > [server1, server2]}
  @VisibleForTesting
  public Map<Integer, List<ShuffleServerInfo>> getPartitionToServers(
      GetShuffleAssignmentsResponse response) {
    Map<Integer, List<ShuffleServerInfo>> partitionToServers = Maps.newHashMap();
    List<PartitionRangeAssignment> assigns = response.getAssignmentsList();
    for (PartitionRangeAssignment partitionRangeAssignment : assigns) {
      final int startPartition = partitionRangeAssignment.getStartPartition();
      final int endPartition = partitionRangeAssignment.getEndPartition();
      final List<ShuffleServerInfo> shuffleServerInfos = partitionRangeAssignment
          .getServerList()
          .stream()
          .map(ss -> new ShuffleServerInfo(ss.getId(), ss.getIp(), ss.getPort()))
          .collect(Collectors.toList());
      for (int i = startPartition; i <= endPartition; i++) {
        partitionToServers.put(i, shuffleServerInfos);
      }
    }
    if (partitionToServers.isEmpty()) {
      throw new RssException("Empty assignment to Shuffle Server");
    }
    return partitionToServers;
  }

  // get all ShuffleRegisterInfo with [shuffleServer, startPartitionId, endPartitionId]
  @VisibleForTesting
  public Map<ShuffleServerInfo, List<PartitionRange>> getServerToPartitionRanges(
      GetShuffleAssignmentsResponse response) {
    Map<ShuffleServerInfo, List<PartitionRange>> serverToPartitionRanges = Maps.newHashMap();
    List<PartitionRangeAssignment> assigns = response.getAssignmentsList();
    for (PartitionRangeAssignment assign : assigns) {
      List<ShuffleServerId> shuffleServerIds = assign.getServerList();
      if (shuffleServerIds != null) {
        PartitionRange partitionRange = new PartitionRange(assign.getStartPartition(), assign.getEndPartition());
        for (ShuffleServerId ssi : shuffleServerIds) {
          ShuffleServerInfo shuffleServerInfo =
              new ShuffleServerInfo(ssi.getId(), ssi.getIp(), ssi.getPort());
          if (!serverToPartitionRanges.containsKey(shuffleServerInfo)) {
            serverToPartitionRanges.put(shuffleServerInfo, Lists.newArrayList());
          }
          serverToPartitionRanges.get(shuffleServerInfo).add(partitionRange);
        }
      }
    }
    return serverToPartitionRanges;
  }
}
