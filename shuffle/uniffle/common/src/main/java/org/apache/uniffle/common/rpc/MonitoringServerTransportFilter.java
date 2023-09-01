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

import io.grpc.Attributes;
import io.grpc.ServerTransportFilter;

import org.apache.uniffle.common.metrics.GRPCMetrics;

import static org.apache.uniffle.common.metrics.GRPCMetrics.GRPC_SERVER_CONNECTION_NUMBER_KEY;

public class MonitoringServerTransportFilter extends ServerTransportFilter {
  private final GRPCMetrics grpcMetrics;

  public MonitoringServerTransportFilter(GRPCMetrics grpcMetrics) {
    this.grpcMetrics = grpcMetrics;
  }

  public Attributes transportReady(Attributes transportAttrs) {
    grpcMetrics.incGauge(GRPC_SERVER_CONNECTION_NUMBER_KEY);
    return super.transportReady(transportAttrs);
  }

  public void transportTerminated(Attributes transportAttrs) {
    if (transportAttrs != null) {
      grpcMetrics.decGauge(GRPC_SERVER_CONNECTION_NUMBER_KEY);
    }
    super.transportTerminated(transportAttrs);
  }
}
