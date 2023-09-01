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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.proto.RssProtos;


public class MockedShuffleServerGrpcService extends ShuffleServerGrpcService {

  private static final Logger LOG = LoggerFactory.getLogger(MockedShuffleServerGrpcService.class);

  // appId -> shuffleId -> partitionRequestNum
  private Map<String, Map<Integer, AtomicInteger>> appToPartitionRequest = Maps.newConcurrentMap();

  private long mockedTimeout = -1L;

  private boolean recordGetShuffleResult = false;

  public void enableMockedTimeout(long timeout) {
    mockedTimeout = timeout;
  }

  public void enableRecordGetShuffleResult() {
    recordGetShuffleResult = true;
  }

  public void disableMockedTimeout() {
    mockedTimeout = -1;
  }

  public MockedShuffleServerGrpcService(ShuffleServer shuffleServer) {
    super(shuffleServer);
  }

  @Override
  public void sendShuffleData(RssProtos.SendShuffleDataRequest request,
      StreamObserver<RssProtos.SendShuffleDataResponse> responseObserver) {
    if (mockedTimeout > 0) {
      LOG.info("Add a mocked timeout on sendShuffleData");
      Uninterruptibles.sleepUninterruptibly(mockedTimeout, TimeUnit.MILLISECONDS);
    }
    super.sendShuffleData(request, responseObserver);
  }

  @Override
  public void reportShuffleResult(RssProtos.ReportShuffleResultRequest request,
                              StreamObserver<RssProtos.ReportShuffleResultResponse> responseObserver) {
    if (mockedTimeout > 0) {
      LOG.info("Add a mocked timeout on reportShuffleResult");
      Uninterruptibles.sleepUninterruptibly(mockedTimeout, TimeUnit.MILLISECONDS);
    }
    super.reportShuffleResult(request, responseObserver);
  }

  @Override
  public void getShuffleResult(RssProtos.GetShuffleResultRequest request,
                               StreamObserver<RssProtos.GetShuffleResultResponse> responseObserver) {
    if (mockedTimeout > 0) {
      LOG.info("Add a mocked timeout on getShuffleResult");
      Uninterruptibles.sleepUninterruptibly(mockedTimeout, TimeUnit.MILLISECONDS);
    }
    super.getShuffleResult(request, responseObserver);
  }

  @Override
  public void getShuffleResultForMultiPart(RssProtos.GetShuffleResultForMultiPartRequest request,
      StreamObserver<RssProtos.GetShuffleResultForMultiPartResponse> responseObserver) {
    if (mockedTimeout > 0) {
      LOG.info("Add a mocked timeout on getShuffleResult");
      Uninterruptibles.sleepUninterruptibly(mockedTimeout, TimeUnit.MILLISECONDS);
    }
    if (recordGetShuffleResult) {
      List<Integer> requestPartitions = request.getPartitionsList();
      Map<Integer, AtomicInteger> shuffleIdToPartitionRequestNum = appToPartitionRequest.computeIfAbsent(
          request.getAppId(), x -> Maps.newConcurrentMap());
      AtomicInteger partitionRequestNum = shuffleIdToPartitionRequestNum.computeIfAbsent(
          request.getShuffleId(), x -> new AtomicInteger(0));
      partitionRequestNum.addAndGet(requestPartitions.size());
    }
    super.getShuffleResultForMultiPart(request, responseObserver);
  }

  public Map<String, Map<Integer, AtomicInteger>> getShuffleIdToPartitionRequest() {
    return appToPartitionRequest;
  }
}
