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

package org.apache.uniffle.common.rpc;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

import org.apache.uniffle.common.metrics.GRPCMetrics;

public class MonitoringServerInterceptor implements ServerInterceptor {

  private final GRPCMetrics grpcMetrics;

  public MonitoringServerInterceptor(GRPCMetrics grpcMetrics) {
    this.grpcMetrics = grpcMetrics;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> serverCall,
      Metadata metadata,
      ServerCallHandler<ReqT, RespT> serverCallHandler) {
    MethodDescriptor<ReqT, RespT> methodDescriptor = serverCall.getMethodDescriptor();
    String methodName = methodDescriptor.getBareMethodName();
    // a call is coming
    grpcMetrics.incCounter(methodName);
    MonitoringServerCall<ReqT, RespT> monitoringServerCall =
        new MonitoringServerCall<>(serverCall, methodName, grpcMetrics);
    return new MonitoringServerCallListener<>(
        serverCallHandler.startCall(monitoringServerCall, metadata));
  }
}
