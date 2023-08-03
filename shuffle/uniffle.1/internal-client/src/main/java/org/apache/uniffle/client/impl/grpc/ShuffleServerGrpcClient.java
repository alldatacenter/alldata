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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnsafeByteOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.client.api.ShuffleServerClient;
import org.apache.uniffle.client.request.RssAppHeartBeatRequest;
import org.apache.uniffle.client.request.RssFinishShuffleRequest;
import org.apache.uniffle.client.request.RssGetInMemoryShuffleDataRequest;
import org.apache.uniffle.client.request.RssGetShuffleDataRequest;
import org.apache.uniffle.client.request.RssGetShuffleIndexRequest;
import org.apache.uniffle.client.request.RssGetShuffleResultForMultiPartRequest;
import org.apache.uniffle.client.request.RssGetShuffleResultRequest;
import org.apache.uniffle.client.request.RssRegisterShuffleRequest;
import org.apache.uniffle.client.request.RssReportShuffleResultRequest;
import org.apache.uniffle.client.request.RssSendCommitRequest;
import org.apache.uniffle.client.request.RssSendShuffleDataRequest;
import org.apache.uniffle.client.request.RssUnregisterShuffleRequest;
import org.apache.uniffle.client.response.RssAppHeartBeatResponse;
import org.apache.uniffle.client.response.RssFinishShuffleResponse;
import org.apache.uniffle.client.response.RssGetInMemoryShuffleDataResponse;
import org.apache.uniffle.client.response.RssGetShuffleDataResponse;
import org.apache.uniffle.client.response.RssGetShuffleIndexResponse;
import org.apache.uniffle.client.response.RssGetShuffleResultResponse;
import org.apache.uniffle.client.response.RssRegisterShuffleResponse;
import org.apache.uniffle.client.response.RssReportShuffleResultResponse;
import org.apache.uniffle.client.response.RssSendCommitResponse;
import org.apache.uniffle.client.response.RssSendShuffleDataResponse;
import org.apache.uniffle.client.response.RssUnregisterShuffleResponse;
import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.ShuffleBlockInfo;
import org.apache.uniffle.common.ShuffleDataDistributionType;
import org.apache.uniffle.common.exception.NotRetryException;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.common.rpc.StatusCode;
import org.apache.uniffle.common.util.RetryUtils;
import org.apache.uniffle.common.util.RssUtils;
import org.apache.uniffle.proto.RssProtos;
import org.apache.uniffle.proto.RssProtos.AppHeartBeatRequest;
import org.apache.uniffle.proto.RssProtos.AppHeartBeatResponse;
import org.apache.uniffle.proto.RssProtos.FinishShuffleRequest;
import org.apache.uniffle.proto.RssProtos.FinishShuffleResponse;
import org.apache.uniffle.proto.RssProtos.GetLocalShuffleDataRequest;
import org.apache.uniffle.proto.RssProtos.GetLocalShuffleDataResponse;
import org.apache.uniffle.proto.RssProtos.GetLocalShuffleIndexRequest;
import org.apache.uniffle.proto.RssProtos.GetLocalShuffleIndexResponse;
import org.apache.uniffle.proto.RssProtos.GetMemoryShuffleDataRequest;
import org.apache.uniffle.proto.RssProtos.GetMemoryShuffleDataResponse;
import org.apache.uniffle.proto.RssProtos.GetShuffleResultForMultiPartRequest;
import org.apache.uniffle.proto.RssProtos.GetShuffleResultForMultiPartResponse;
import org.apache.uniffle.proto.RssProtos.GetShuffleResultRequest;
import org.apache.uniffle.proto.RssProtos.GetShuffleResultResponse;
import org.apache.uniffle.proto.RssProtos.PartitionToBlockIds;
import org.apache.uniffle.proto.RssProtos.RemoteStorage;
import org.apache.uniffle.proto.RssProtos.RemoteStorageConfItem;
import org.apache.uniffle.proto.RssProtos.ReportShuffleResultRequest;
import org.apache.uniffle.proto.RssProtos.ReportShuffleResultResponse;
import org.apache.uniffle.proto.RssProtos.RequireBufferRequest;
import org.apache.uniffle.proto.RssProtos.RequireBufferResponse;
import org.apache.uniffle.proto.RssProtos.SendShuffleDataRequest;
import org.apache.uniffle.proto.RssProtos.SendShuffleDataResponse;
import org.apache.uniffle.proto.RssProtos.ShuffleBlock;
import org.apache.uniffle.proto.RssProtos.ShuffleCommitRequest;
import org.apache.uniffle.proto.RssProtos.ShuffleCommitResponse;
import org.apache.uniffle.proto.RssProtos.ShuffleData;
import org.apache.uniffle.proto.RssProtos.ShuffleDataBlockSegment;
import org.apache.uniffle.proto.RssProtos.ShufflePartitionRange;
import org.apache.uniffle.proto.RssProtos.ShuffleRegisterRequest;
import org.apache.uniffle.proto.RssProtos.ShuffleRegisterResponse;
import org.apache.uniffle.proto.ShuffleServerGrpc;
import org.apache.uniffle.proto.ShuffleServerGrpc.ShuffleServerBlockingStub;

public class ShuffleServerGrpcClient extends GrpcClient implements ShuffleServerClient {

  private static final Logger LOG = LoggerFactory.getLogger(ShuffleServerGrpcClient.class);
  private static final long FAILED_REQUIRE_ID = -1;
  private static final long RPC_TIMEOUT_DEFAULT_MS = 60000;
  private long rpcTimeout = RPC_TIMEOUT_DEFAULT_MS;
  private ShuffleServerBlockingStub blockingStub;

  public ShuffleServerGrpcClient(String host, int port) {
    this(host, port, 3);
  }

  public ShuffleServerGrpcClient(String host, int port, int maxRetryAttempts) {
    this(host, port, maxRetryAttempts, true);
  }

  public ShuffleServerGrpcClient(String host, int port, int maxRetryAttempts, boolean usePlaintext) {
    super(host, port, maxRetryAttempts, usePlaintext);
    blockingStub = ShuffleServerGrpc.newBlockingStub(channel);
  }

  public ShuffleServerBlockingStub getBlockingStub() {
    return blockingStub.withDeadlineAfter(rpcTimeout, TimeUnit.MILLISECONDS);
  }

  @Override
  public String getDesc() {
    return "Shuffle server grpc client ref " + host + ":" + port;
  }

  private ShuffleRegisterResponse doRegisterShuffle(
      String appId,
      int shuffleId,
      List<PartitionRange> partitionRanges,
      RemoteStorageInfo remoteStorageInfo,
      String user,
      ShuffleDataDistributionType dataDistributionType) {
    ShuffleRegisterRequest.Builder reqBuilder = ShuffleRegisterRequest.newBuilder();
    reqBuilder
        .setAppId(appId)
        .setShuffleId(shuffleId)
        .setUser(user)
        .setShuffleDataDistribution(RssProtos.DataDistribution.valueOf(dataDistributionType.name()))
        .addAllPartitionRanges(toShufflePartitionRanges(partitionRanges));
    RemoteStorage.Builder rsBuilder = RemoteStorage.newBuilder();
    rsBuilder.setPath(remoteStorageInfo.getPath());
    Map<String, String> remoteStorageConf = remoteStorageInfo.getConfItems();
    if (!remoteStorageConf.isEmpty()) {
      RemoteStorageConfItem.Builder rsConfBuilder = RemoteStorageConfItem.newBuilder();
      for (Map.Entry<String, String> entry : remoteStorageConf.entrySet()) {
        rsConfBuilder.setKey(entry.getKey()).setValue(entry.getValue());
        rsBuilder.addRemoteStorageConf(rsConfBuilder.build());
      }
    }
    reqBuilder.setRemoteStorage(rsBuilder.build());
    return getBlockingStub().registerShuffle(reqBuilder.build());
  }

  private ShuffleCommitResponse doSendCommit(String appId, int shuffleId) {
    ShuffleCommitRequest request = ShuffleCommitRequest.newBuilder()
        .setAppId(appId).setShuffleId(shuffleId).build();
    int retryNum = 0;
    while (retryNum <= maxRetryAttempts) {
      try {
        ShuffleCommitResponse response = getBlockingStub().commitShuffleTask(request);
        return response;
      } catch (Exception e) {
        retryNum++;
        LOG.warn("Send commit to host[" + host + "], port[" + port
            + "] failed, try again, retryNum[" + retryNum + "]", e);
      }
    }
    throw new RssException("Send commit to host[" + host + "], port[" + port + "] failed");
  }

  private AppHeartBeatResponse doSendHeartBeat(String appId, long timeout) {
    AppHeartBeatRequest request = AppHeartBeatRequest.newBuilder().setAppId(appId).build();
    return blockingStub.withDeadlineAfter(timeout, TimeUnit.MILLISECONDS).appHeartbeat(request);
  }

  // Only for tests
  @VisibleForTesting
  public long requirePreAllocation(int requireSize, int retryMax, long retryIntervalMax) throws Exception {
    return requirePreAllocation(
        "EMPTY",
        0,
        Collections.emptyList(),
        requireSize,
        retryMax,
        retryIntervalMax
    );
  }

  public long requirePreAllocation(
      String appId,
      int shuffleId,
      List<Integer> partitionIds,
      int requireSize,
      int retryMax,
      long retryIntervalMax) {
    RequireBufferRequest rpcRequest = RequireBufferRequest.newBuilder()
        .setShuffleId(shuffleId)
        .addAllPartitionIds(partitionIds)
        .setAppId(appId)
        .setRequireSize(requireSize)
        .build();

    long start = System.currentTimeMillis();
    RequireBufferResponse rpcResponse = getBlockingStub().requireBuffer(rpcRequest);
    int retry = 0;
    long result = FAILED_REQUIRE_ID;
    Random random = new Random();
    final int backOffBase = 2000;
    while (rpcResponse.getStatus() == RssProtos.StatusCode.NO_BUFFER) {
      LOG.info("Can't require " + requireSize + " bytes from " + host + ":" + port + ", sleep and try["
          + retry + "] again");
      if (retry >= retryMax) {
        LOG.warn("ShuffleServer " + host + ":" + port + " is full and can't send shuffle"
                + " data successfully after retry " + retryMax + " times, cost: {}(ms)",
            System.currentTimeMillis() - start);
        return result;
      }
      try {
        long backoffTime =
            Math.min(retryIntervalMax, backOffBase * (1L << Math.min(retry, 16)) + random.nextInt(backOffBase));
        Thread.sleep(backoffTime);
      } catch (Exception e) {
        LOG.warn("Exception happened when require pre allocation from " + host + ":" + port, e);
      }
      rpcResponse = getBlockingStub().requireBuffer(rpcRequest);
      retry++;
    }
    if (rpcResponse.getStatus() == RssProtos.StatusCode.SUCCESS) {
      LOG.info("Require preAllocated size of {} from {}:{}, cost: {}(ms)",
          requireSize, host, port, System.currentTimeMillis() - start);
      result = rpcResponse.getRequireBufferId();
    }
    return result;
  }

  private RssProtos.ShuffleUnregisterResponse doUnregisterShuffle(String appId, int shuffleId) {
    RssProtos.ShuffleUnregisterRequest request = RssProtos.ShuffleUnregisterRequest.newBuilder()
        .setAppId(appId)
        .setShuffleId(shuffleId)
        .build();
    return blockingStub.unregisterShuffle(request);
  }

  @Override
  public RssUnregisterShuffleResponse unregisterShuffle(RssUnregisterShuffleRequest request) {
    RssProtos.ShuffleUnregisterResponse rpcResponse = doUnregisterShuffle(request.getAppId(), request.getShuffleId());

    RssUnregisterShuffleResponse response;
    RssProtos.StatusCode statusCode = rpcResponse.getStatus();

    switch (statusCode) {
      case SUCCESS:
        response = new RssUnregisterShuffleResponse(StatusCode.SUCCESS);
        break;
      default:
        String msg = String.format("Errors on unregister shuffle to %s:%s for appId[%s].shuffleId[%], error: %s",
            host, port, request.getAppId(), request.getShuffleId(), rpcResponse.getRetMsg());
        LOG.error(msg);
        throw new RssException(msg);
    }

    return response;
  }

  @Override
  public RssRegisterShuffleResponse registerShuffle(RssRegisterShuffleRequest request) {
    ShuffleRegisterResponse rpcResponse = doRegisterShuffle(
        request.getAppId(),
        request.getShuffleId(),
        request.getPartitionRanges(),
        request.getRemoteStorageInfo(),
        request.getUser(),
        request.getDataDistributionType()
    );

    RssRegisterShuffleResponse response;
    RssProtos.StatusCode statusCode = rpcResponse.getStatus();
    switch (statusCode) {
      case SUCCESS:
        response = new RssRegisterShuffleResponse(StatusCode.SUCCESS);
        break;
      default:
        String msg = "Can't register shuffle to " + host + ":" + port
            + " for appId[" + request.getAppId() + "], shuffleId[" + request.getShuffleId()
            + "], errorMsg:" + rpcResponse.getRetMsg();
        LOG.error(msg);
        throw new RssException(msg);
    }
    return response;
  }

  @Override
  public RssSendShuffleDataResponse sendShuffleData(RssSendShuffleDataRequest request) {
    String appId = request.getAppId();
    Map<Integer, Map<Integer, List<ShuffleBlockInfo>>> shuffleIdToBlocks = request.getShuffleIdToBlocks();

    boolean isSuccessful = true;

    // prepare rpc request based on shuffleId -> partitionId -> blocks
    for (Map.Entry<Integer, Map<Integer, List<ShuffleBlockInfo>>> stb : shuffleIdToBlocks.entrySet()) {
      List<ShuffleData> shuffleData = Lists.newArrayList();
      int size = 0;
      int blockNum = 0;
      int shuffleId = stb.getKey();
      List<Integer> partitionIds = new ArrayList<>();

      for (Map.Entry<Integer, List<ShuffleBlockInfo>> ptb : stb.getValue().entrySet()) {
        List<ShuffleBlock> shuffleBlocks = Lists.newArrayList();
        for (ShuffleBlockInfo sbi : ptb.getValue()) {
          shuffleBlocks.add(ShuffleBlock.newBuilder().setBlockId(sbi.getBlockId())
              .setCrc(sbi.getCrc())
              .setLength(sbi.getLength())
              .setTaskAttemptId(sbi.getTaskAttemptId())
              .setUncompressLength(sbi.getUncompressLength())
              .setData(ByteString.copyFrom(sbi.getData()))
              .build());
          size += sbi.getSize();
          blockNum++;
        }
        shuffleData.add(ShuffleData.newBuilder().setPartitionId(ptb.getKey())
            .addAllBlock(shuffleBlocks)
            .build());
        partitionIds.add(ptb.getKey());
      }

      final int allocateSize = size;
      final int finalBlockNum = blockNum;
      try {
        RetryUtils.retry(() -> {
          long requireId = requirePreAllocation(
              appId,
              shuffleId,
              partitionIds,
              allocateSize,
              request.getRetryMax() / maxRetryAttempts,
              request.getRetryIntervalMax()
          );
          if (requireId == FAILED_REQUIRE_ID) {
            throw new RssException(String.format(
                "requirePreAllocation failed! size[%s], host[%s], port[%s]", allocateSize, host, port));
          }
          long start = System.currentTimeMillis();
          SendShuffleDataRequest rpcRequest = SendShuffleDataRequest.newBuilder()
              .setAppId(appId)
              .setShuffleId(stb.getKey())
              .setRequireBufferId(requireId)
              .addAllShuffleData(shuffleData)
              .setTimestamp(start)
              .build();
          SendShuffleDataResponse response = getBlockingStub().sendShuffleData(rpcRequest);
          LOG.info("Do sendShuffleData to {}:{} rpc cost:" + (System.currentTimeMillis() - start)
              + " ms for " + allocateSize + " bytes with " + finalBlockNum + " blocks", host, port);
          if (response.getStatus() != RssProtos.StatusCode.SUCCESS) {
            String msg = "Can't send shuffle data with " + finalBlockNum
                + " blocks to " + host + ":" + port
                + ", statusCode=" + response.getStatus()
                + ", errorMsg:" + response.getRetMsg();
            if (response.getStatus() == RssProtos.StatusCode.NO_REGISTER) {
              throw new NotRetryException(msg);
            } else {
              throw new RssException(msg);
            }
          }
          return response;
        }, request.getRetryIntervalMax(), maxRetryAttempts);
      } catch (Throwable throwable) {
        LOG.warn(throwable.getMessage());
        isSuccessful = false;
        break;
      }
    }

    RssSendShuffleDataResponse response;
    if (isSuccessful) {
      response = new RssSendShuffleDataResponse(StatusCode.SUCCESS);
    } else {
      response = new RssSendShuffleDataResponse(StatusCode.INTERNAL_ERROR);
    }
    return response;
  }

  @Override
  public RssSendCommitResponse sendCommit(RssSendCommitRequest request) {
    ShuffleCommitResponse rpcResponse = doSendCommit(request.getAppId(), request.getShuffleId());

    RssSendCommitResponse response;
    if (rpcResponse.getStatus() != RssProtos.StatusCode.SUCCESS) {
      String msg = "Can't commit shuffle data to " + host + ":" + port
          + " for [appId=" + request.getAppId() + ", shuffleId=" + request.getShuffleId() + "], "
          + "errorMsg:" + rpcResponse.getRetMsg();
      LOG.error(msg);
      throw new RssException(msg);
    } else {
      response = new RssSendCommitResponse(StatusCode.SUCCESS);
      response.setCommitCount(rpcResponse.getCommitCount());
    }
    return response;
  }

  @Override
  public RssAppHeartBeatResponse sendHeartBeat(RssAppHeartBeatRequest request) {
    AppHeartBeatResponse appHeartBeatResponse = doSendHeartBeat(request.getAppId(), request.getTimeoutMs());
    if (appHeartBeatResponse.getStatus() != RssProtos.StatusCode.SUCCESS) {
      String msg = "Can't send heartbeat to " + host + ":" + port
          + " for [appId=" + request.getAppId() + ", timeout=" + request.getTimeoutMs() + "ms], "
          + "errorMsg:" + appHeartBeatResponse.getRetMsg();
      LOG.error(msg);
      return new RssAppHeartBeatResponse(StatusCode.INTERNAL_ERROR);
    } else {
      return new RssAppHeartBeatResponse(StatusCode.SUCCESS);
    }
  }

  @Override
  public RssFinishShuffleResponse finishShuffle(RssFinishShuffleRequest request) {
    FinishShuffleRequest rpcRequest = FinishShuffleRequest.newBuilder()
        .setAppId(request.getAppId()).setShuffleId(request.getShuffleId()).build();
    long start = System.currentTimeMillis();
    FinishShuffleResponse rpcResponse = getBlockingStub().finishShuffle(rpcRequest);

    RssFinishShuffleResponse response;
    if (rpcResponse.getStatus() != RssProtos.StatusCode.SUCCESS) {
      String msg = "Can't finish shuffle process to " + host + ":" + port
          + " for [appId=" + request.getAppId() + ", shuffleId=" + request.getShuffleId() + "], "
          + "errorMsg:" + rpcResponse.getRetMsg();
      LOG.error(msg);
      throw new RssException(msg);
    } else {
      String requestInfo = "appId[" + request.getAppId() + "], shuffleId["
          + request.getShuffleId() + "]";
      LOG.info("FinishShuffle to {}:{} for {} cost {} ms", host, port, requestInfo,
          System.currentTimeMillis() - start);
      response = new RssFinishShuffleResponse(StatusCode.SUCCESS);
    }
    return response;
  }

  @Override
  public RssReportShuffleResultResponse reportShuffleResult(RssReportShuffleResultRequest request) {
    List<PartitionToBlockIds> partitionToBlockIds = Lists.newArrayList();
    for (Map.Entry<Integer, List<Long>> entry : request.getPartitionToBlockIds().entrySet()) {
      List<Long> blockIds = entry.getValue();
      if (blockIds != null && !blockIds.isEmpty()) {
        partitionToBlockIds.add(PartitionToBlockIds.newBuilder()
            .setPartitionId(entry.getKey())
            .addAllBlockIds(entry.getValue())
            .build());
      }
    }

    ReportShuffleResultRequest recRequest = ReportShuffleResultRequest.newBuilder()
        .setAppId(request.getAppId())
        .setShuffleId(request.getShuffleId())
        .setTaskAttemptId(request.getTaskAttemptId())
        .setBitmapNum(request.getBitmapNum())
        .addAllPartitionToBlockIds(partitionToBlockIds)
        .build();
    ReportShuffleResultResponse rpcResponse = doReportShuffleResult(recRequest);

    RssProtos.StatusCode statusCode = rpcResponse.getStatus();
    RssReportShuffleResultResponse response;
    switch (statusCode) {
      case SUCCESS:
        response = new RssReportShuffleResultResponse(StatusCode.SUCCESS);
        break;
      default:
        String msg = "Can't report shuffle result to " + host + ":" + port
            + " for [appId=" + request.getAppId() + ", shuffleId=" + request.getShuffleId()
            + ", errorMsg:" + rpcResponse.getRetMsg();
        LOG.error(msg);
        throw new RssException(msg);
    }

    return response;
  }

  private ReportShuffleResultResponse doReportShuffleResult(ReportShuffleResultRequest rpcRequest) {
    int retryNum = 0;
    while (retryNum < maxRetryAttempts) {
      try {
        ReportShuffleResultResponse response = getBlockingStub().reportShuffleResult(rpcRequest);
        return response;
      } catch (Exception e) {
        retryNum++;
        LOG.warn("Report shuffle result to host[" + host + "], port[" + port
            + "] failed, try again, retryNum[" + retryNum + "]", e);
      }
    }
    throw new RssException("Report shuffle result to host[" + host + "], port[" + port + "] failed");
  }

  @Override
  public RssGetShuffleResultResponse getShuffleResult(RssGetShuffleResultRequest request) {
    GetShuffleResultRequest rpcRequest = GetShuffleResultRequest
        .newBuilder()
        .setAppId(request.getAppId())
        .setShuffleId(request.getShuffleId())
        .setPartitionId(request.getPartitionId())
        .build();
    GetShuffleResultResponse rpcResponse = getBlockingStub().getShuffleResult(rpcRequest);
    RssProtos.StatusCode statusCode = rpcResponse.getStatus();

    RssGetShuffleResultResponse response;
    switch (statusCode) {
      case SUCCESS:
        try {
          response = new RssGetShuffleResultResponse(StatusCode.SUCCESS,
              rpcResponse.getSerializedBitmap().toByteArray());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        break;
      default:
        String msg = "Can't get shuffle result from " + host + ":" + port
            + " for [appId=" + request.getAppId() + ", shuffleId=" + request.getShuffleId()
            + ", errorMsg:" + rpcResponse.getRetMsg();
        LOG.error(msg);
        throw new RssException(msg);
    }

    return response;
  }

  @Override
  public RssGetShuffleResultResponse getShuffleResultForMultiPart(RssGetShuffleResultForMultiPartRequest request) {
    GetShuffleResultForMultiPartRequest rpcRequest = GetShuffleResultForMultiPartRequest
        .newBuilder()
        .setAppId(request.getAppId())
        .setShuffleId(request.getShuffleId())
        .addAllPartitions(request.getPartitions())
        .build();
    GetShuffleResultForMultiPartResponse rpcResponse = getBlockingStub().getShuffleResultForMultiPart(rpcRequest);
    RssProtos.StatusCode statusCode = rpcResponse.getStatus();

    RssGetShuffleResultResponse response;
    switch (statusCode) {
      case SUCCESS:
        try {
          response = new RssGetShuffleResultResponse(StatusCode.SUCCESS,
              rpcResponse.getSerializedBitmap().toByteArray());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        break;
      default:
        String msg = "Can't get shuffle result from " + host + ":" + port
            + " for [appId=" + request.getAppId() + ", shuffleId=" + request.getShuffleId()
            + ", errorMsg:" + rpcResponse.getRetMsg();
        LOG.error(msg);
        throw new RssException(msg);
    }

    return response;
  }

  @Override
  public RssGetShuffleDataResponse getShuffleData(RssGetShuffleDataRequest request) {
    long start = System.currentTimeMillis();
    GetLocalShuffleDataRequest rpcRequest = GetLocalShuffleDataRequest
        .newBuilder()
        .setAppId(request.getAppId())
        .setShuffleId(request.getShuffleId())
        .setPartitionId(request.getPartitionId())
        .setPartitionNumPerRange(request.getPartitionNumPerRange())
        .setPartitionNum(request.getPartitionNum())
        .setOffset(request.getOffset())
        .setLength(request.getLength())
        .setTimestamp(start)
        .build();
    GetLocalShuffleDataResponse rpcResponse = getBlockingStub().getLocalShuffleData(rpcRequest);
    String requestInfo = "appId[" + request.getAppId() + "], shuffleId["
        + request.getShuffleId() + "], partitionId[" + request.getPartitionId() + "]";
    LOG.info("GetShuffleData from {}:{} for {} cost {} ms", host, port, requestInfo,
        System.currentTimeMillis() - start);

    RssProtos.StatusCode statusCode = rpcResponse.getStatus();

    RssGetShuffleDataResponse response;
    switch (statusCode) {
      case SUCCESS:
        response = new RssGetShuffleDataResponse(
            StatusCode.SUCCESS, rpcResponse.getData().toByteArray());

        break;
      default:
        String msg = "Can't get shuffle data from " + host + ":" + port
            + " for " + requestInfo + ", errorMsg:" + rpcResponse.getRetMsg();
        LOG.error(msg);
        throw new RssException(msg);
    }
    return response;
  }

  @Override
  public RssGetShuffleIndexResponse getShuffleIndex(RssGetShuffleIndexRequest request) {
    GetLocalShuffleIndexRequest rpcRequest = GetLocalShuffleIndexRequest
        .newBuilder()
        .setAppId(request.getAppId())
        .setShuffleId(request.getShuffleId())
        .setPartitionId(request.getPartitionId())
        .setPartitionNumPerRange(request.getPartitionNumPerRange())
        .setPartitionNum(request.getPartitionNum())
        .build();
    long start = System.currentTimeMillis();
    GetLocalShuffleIndexResponse rpcResponse = getBlockingStub().getLocalShuffleIndex(rpcRequest);
    String requestInfo = "appId[" + request.getAppId() + "], shuffleId["
        + request.getShuffleId() + "], partitionId[" + request.getPartitionId() + "]";
    LOG.info("GetShuffleIndex from {}:{} for {} cost {} ms", host, port,
        requestInfo, System.currentTimeMillis() - start);

    RssProtos.StatusCode statusCode = rpcResponse.getStatus();

    RssGetShuffleIndexResponse response;
    switch (statusCode) {
      case SUCCESS:
        response = new RssGetShuffleIndexResponse(
            StatusCode.SUCCESS, rpcResponse.getIndexData().toByteArray(), rpcResponse.getDataFileLen());

        break;
      default:
        String msg = "Can't get shuffle index from " + host + ":" + port
            + " for " + requestInfo + ", errorMsg:" + rpcResponse.getRetMsg();
        LOG.error(msg);
        throw new RssException(msg);
    }
    return response;
  }

  @Override
  public RssGetInMemoryShuffleDataResponse getInMemoryShuffleData(
      RssGetInMemoryShuffleDataRequest request) {
    long start = System.currentTimeMillis();
    ByteString serializedTaskIdsBytes = ByteString.EMPTY;
    try {
      if (request.getExpectedTaskIds() != null) {
        serializedTaskIdsBytes =
            UnsafeByteOperations.unsafeWrap(RssUtils.serializeBitMap(request.getExpectedTaskIds()));
      }
    } catch (Exception e) {
      throw new RssException("Errors on serializing task ids bitmap.", e);
    }

    GetMemoryShuffleDataRequest rpcRequest = GetMemoryShuffleDataRequest
        .newBuilder()
        .setAppId(request.getAppId())
        .setShuffleId(request.getShuffleId())
        .setPartitionId(request.getPartitionId())
        .setLastBlockId(request.getLastBlockId())
        .setReadBufferSize(request.getReadBufferSize())
        .setSerializedExpectedTaskIdsBitmap(serializedTaskIdsBytes)
        .setTimestamp(start)
        .build();

    GetMemoryShuffleDataResponse rpcResponse = getBlockingStub().getMemoryShuffleData(rpcRequest);
    String requestInfo = "appId[" + request.getAppId() + "], shuffleId["
        + request.getShuffleId() + "], partitionId[" + request.getPartitionId() + "]";
    LOG.info("GetInMemoryShuffleData from {}:{} for " + requestInfo + " cost "
        + (System.currentTimeMillis() - start) + " ms", host, port);

    RssProtos.StatusCode statusCode = rpcResponse.getStatus();

    RssGetInMemoryShuffleDataResponse response;
    switch (statusCode) {
      case SUCCESS:
        response = new RssGetInMemoryShuffleDataResponse(
            StatusCode.SUCCESS, rpcResponse.getData().toByteArray(),
            toBufferSegments(rpcResponse.getShuffleDataBlockSegmentsList()));
        break;
      default:
        String msg = "Can't get shuffle in memory data from " + host + ":" + port
            + " for " + requestInfo + ", errorMsg:" + rpcResponse.getRetMsg();
        LOG.error(msg);
        throw new RssException(msg);
    }
    return response;
  }

  @Override
  public String getClientInfo() {
    return "ShuffleServerGrpcClient for host[" + host + "], port[" + port + "]";
  }

  private List<ShufflePartitionRange> toShufflePartitionRanges(List<PartitionRange> partitionRanges) {
    List<ShufflePartitionRange> ret = Lists.newArrayList();
    for (PartitionRange partitionRange : partitionRanges) {
      ret.add(ShufflePartitionRange
          .newBuilder()
          .setStart(partitionRange.getStart())
          .setEnd(partitionRange.getEnd()).build());
    }
    return ret;
  }

  private List<BufferSegment> toBufferSegments(List<ShuffleDataBlockSegment> blockSegments) {
    List<BufferSegment> ret = Lists.newArrayList();
    for (ShuffleDataBlockSegment sdbs : blockSegments) {
      ret.add(new BufferSegment(sdbs.getBlockId(), sdbs.getOffset(), sdbs.getLength(),
          sdbs.getUncompressLength(), sdbs.getCrc(), sdbs.getTaskAttemptId()));
    }
    return ret;
  }

  @VisibleForTesting
  public void adjustTimeout(long timeout) {
    rpcTimeout = timeout;
  }
}
