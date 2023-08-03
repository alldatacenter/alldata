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

package org.apache.uniffle.coordinator.metric;

import org.apache.uniffle.common.metrics.GRPCMetrics;

public class CoordinatorGrpcMetrics extends GRPCMetrics {

  public static final String HEARTBEAT_METHOD = "heartbeat";
  public static final String GET_SHUFFLE_ASSIGNMENTS_METHOD = "getShuffleAssignments";

  private static final String GRPC_GET_SHUFFLE_ASSIGNMENTS = "grpc_get_shuffle_assignments";
  private static final String GRPC_HEARTBEAT = "grpc_heartbeat";
  private static final String GRPC_GET_SHUFFLE_ASSIGNMENTS_TOTAL =
      "grpc_get_shuffle_assignments_total";
  private static final String GRPC_HEARTBEAT_TOTAL =
      "grpc_heartbeat_total";

  @Override
  public void registerMetrics() {
    gaugeMap.putIfAbsent(HEARTBEAT_METHOD,
        metricsManager.addGauge(GRPC_HEARTBEAT));
    gaugeMap.putIfAbsent(GET_SHUFFLE_ASSIGNMENTS_METHOD,
        metricsManager.addGauge(GRPC_GET_SHUFFLE_ASSIGNMENTS));
    counterMap.putIfAbsent(HEARTBEAT_METHOD,
        metricsManager.addCounter(GRPC_HEARTBEAT_TOTAL));
    counterMap.putIfAbsent(GET_SHUFFLE_ASSIGNMENTS_METHOD,
        metricsManager.addCounter(GRPC_GET_SHUFFLE_ASSIGNMENTS_TOTAL));
  }
}
