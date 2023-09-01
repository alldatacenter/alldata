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

import io.grpc.stub.StreamObserver;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.client.impl.grpc.CoordinatorGrpcClient;
import org.apache.uniffle.client.request.RssApplicationInfoRequest;
import org.apache.uniffle.common.config.RssBaseConf;
import org.apache.uniffle.common.metrics.GRPCMetrics;
import org.apache.uniffle.common.rpc.GrpcServer;
import org.apache.uniffle.coordinator.metric.CoordinatorGrpcMetrics;
import org.apache.uniffle.proto.CoordinatorServerGrpc;
import org.apache.uniffle.proto.RssProtos;

import static org.apache.uniffle.common.metrics.GRPCMetrics.GRPC_SERVER_CONNECTION_NUMBER_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class is to test the GRPC server's related metrics like {@code GRPC_SERVER_EXECUTOR_ACTIVE_THREADS_TAG}
 */
public class CoordinatorGrpcServerTest {

  static class MockedCoordinatorGrpcService extends CoordinatorServerGrpc.CoordinatorServerImplBase {
    @Override
    public void registerApplicationInfo(
        RssProtos.ApplicationInfoRequest request,
        StreamObserver<RssProtos.ApplicationInfoResponse> responseObserver) {
      RssProtos.ApplicationInfoResponse response = RssProtos.ApplicationInfoResponse
          .newBuilder()
          .setRetMsg("")
          .setStatus(RssProtos.StatusCode.SUCCESS)
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    }
  }

  @Test
  public void testGrpcConnectionSize() throws Exception {
    RssBaseConf baseConf = new RssBaseConf();
    baseConf.set(RssBaseConf.RPC_SERVER_PORT, 20001);
    baseConf.set(RssBaseConf.RPC_EXECUTOR_SIZE, 2);

    GRPCMetrics grpcMetrics = new CoordinatorGrpcMetrics();
    grpcMetrics.register(new CollectorRegistry(true));
    GrpcServer grpcServer = new GrpcServer(baseConf, new MockedCoordinatorGrpcService(), grpcMetrics);
    grpcServer.start();

    // case1: test the single one connection metric
    double connSize = grpcMetrics.getGaugeMap().get(GRPC_SERVER_CONNECTION_NUMBER_KEY).get();
    assertEquals(0, connSize);

    CoordinatorGrpcClient coordinatorGrpcClient = new CoordinatorGrpcClient("localhost", 20001);
    coordinatorGrpcClient.registerApplicationInfo(
        new RssApplicationInfoRequest("testGrpcConnectionSize", 10000, "user"));

    connSize = grpcMetrics.getGaugeMap().get(GRPC_SERVER_CONNECTION_NUMBER_KEY).get();
    assertEquals(1, connSize);

    // case2: test the multiple connections
    CoordinatorGrpcClient client1 = new CoordinatorGrpcClient("localhost", 20001);
    CoordinatorGrpcClient client2 = new CoordinatorGrpcClient("localhost", 20001);
    client1.registerApplicationInfo(new RssApplicationInfoRequest("testGrpcConnectionSize", 10000, "user"));
    client2.registerApplicationInfo(new RssApplicationInfoRequest("testGrpcConnectionSize", 10000, "user"));

    connSize = grpcMetrics.getGaugeMap().get(GRPC_SERVER_CONNECTION_NUMBER_KEY).get();
    assertEquals(3, connSize);

    grpcServer.stop();
  }
}
