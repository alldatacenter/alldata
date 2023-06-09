/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.client;

import java.util.Map;
import java.util.Set;

import org.apache.drill.common.Version;
import org.apache.drill.exec.proto.UserProtos.RpcEndpointInfos;
import org.apache.drill.exec.proto.UserProtos.RpcType;
import org.apache.drill.exec.rpc.user.UserRpcUtils;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

/**
 * A enumeration of server methods, and the version they were introduced
 *
 * it allows to introduce new methods without changing the protocol, with client
 * being able to gracefully handle cases were method is not handled by the server.
 */
public enum ServerMethod {
  /**
   * Submitting a query
   */
  RUN_QUERY(RpcType.RUN_QUERY, Constants.DRILL_0_0_0),

  /**
   * Plan a query without executing it
   */
  PLAN_QUERY(RpcType.QUERY_PLAN_FRAGMENTS, Constants.DRILL_0_0_0),

  /**
   * Cancel an existing query
   */
  CANCEL_QUERY(RpcType.CANCEL_QUERY, Constants.DRILL_0_0_0),

  /**
   * Resume a query
   */
  RESUME_PAUSED_QUERY(RpcType.RESUME_PAUSED_QUERY, Constants.DRILL_0_0_0),

  /**
   * Prepare a query for deferred execution
   */
  PREPARED_STATEMENT(RpcType.CREATE_PREPARED_STATEMENT, Constants.DRILL_1_8_0),

  /**
   * Get catalog metadata
   */
  GET_CATALOGS(RpcType.GET_CATALOGS, Constants.DRILL_1_8_0),

  /**
   * Get schemas metadata
   */
  GET_SCHEMAS(RpcType.GET_SCHEMAS, Constants.DRILL_1_8_0),

  /**
   * Get tables metadata
   */
  GET_TABLES(RpcType.GET_TABLES, Constants.DRILL_1_8_0),

  /**
   * Get columns metadata
   */
  GET_COLUMNS(RpcType.GET_COLUMNS, Constants.DRILL_1_8_0),

  /**
   * Get server metadata
   */
  GET_SERVER_META(RpcType.GET_SERVER_META, Constants.DRILL_1_10_0);

  private static class Constants {
    private static final Version DRILL_0_0_0 = new Version("0.0.0", 0, 0, 0, 0, "");
    private static final Version DRILL_1_8_0 = new Version("1.8.0", 1, 8, 0, 0, "");
    private static final Version DRILL_1_10_0 = new Version("1.10.0", 1, 10, 0, 0, "");
  }

  private static final Map<RpcType, ServerMethod> REVERSE_MAPPING;
  static {
    ImmutableMap.Builder<RpcType, ServerMethod> builder = ImmutableMap.builder();
    for(ServerMethod method: values()) {
      builder.put(method.rpcType, method);
    }
    REVERSE_MAPPING = Maps.immutableEnumMap(builder.build());
  }

  private final RpcType rpcType;
  private final Version minVersion;

  private ServerMethod(RpcType rpcType, Version minVersion) {
    this.rpcType = rpcType;
    this.minVersion = minVersion;
  }

  public Version getMinVersion() {
    return minVersion;
  }

  /**
   * Returns the list of methods supported by the server based on its advertised information.
   *
   * @param serverInfos the server information
   * @return a immutable set of capabilities
   */
  static final Set<ServerMethod> getSupportedMethods(Iterable<RpcType> supportedMethods, RpcEndpointInfos serverInfos) {
    ImmutableSet.Builder<ServerMethod> builder = ImmutableSet.builder();

    for(RpcType supportedMethod: supportedMethods) {
      ServerMethod method = REVERSE_MAPPING.get(supportedMethod);
      if (method == null) {
        // The server might have newer methods we don't know how to handle yet.
        continue;
      }
      builder.add(method);
    }

    // Fallback to version detection to cover the gap between Drill 1.8.0 and Drill 1.10.0
    if (serverInfos == null) {
      return Sets.immutableEnumSet(builder.build());
    }

    Version serverVersion = UserRpcUtils.getVersion(serverInfos);
    for(ServerMethod capability: ServerMethod.values()) {
      if (serverVersion.compareTo(capability.getMinVersion()) >= 0) {
        builder.add(capability);
      }
    }

    return Sets.immutableEnumSet(builder.build());
  }
}
