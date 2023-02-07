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
package org.apache.drill.exec.rpc.user;

import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared.QueryData;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryResult;
import org.apache.drill.exec.proto.UserBitShared.SaslMessage;
import org.apache.drill.exec.proto.UserProtos.BitToUserHandshake;
import org.apache.drill.exec.proto.UserProtos.CreatePreparedStatementReq;
import org.apache.drill.exec.proto.UserProtos.CreatePreparedStatementResp;
import org.apache.drill.exec.proto.UserProtos.GetCatalogsReq;
import org.apache.drill.exec.proto.UserProtos.GetCatalogsResp;
import org.apache.drill.exec.proto.UserProtos.GetColumnsReq;
import org.apache.drill.exec.proto.UserProtos.GetColumnsResp;
import org.apache.drill.exec.proto.UserProtos.GetQueryPlanFragments;
import org.apache.drill.exec.proto.UserProtos.GetSchemasReq;
import org.apache.drill.exec.proto.UserProtos.GetSchemasResp;
import org.apache.drill.exec.proto.UserProtos.GetServerMetaReq;
import org.apache.drill.exec.proto.UserProtos.GetServerMetaResp;
import org.apache.drill.exec.proto.UserProtos.GetTablesReq;
import org.apache.drill.exec.proto.UserProtos.GetTablesResp;
import org.apache.drill.exec.proto.UserProtos.QueryPlanFragments;
import org.apache.drill.exec.proto.UserProtos.RpcType;
import org.apache.drill.exec.proto.UserProtos.RunQuery;
import org.apache.drill.exec.proto.UserProtos.UserToBitHandshake;
import org.apache.drill.exec.rpc.RpcConfig;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

public class UserRpcConfig {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserRpcConfig.class);

  public static RpcConfig getMapping(DrillConfig config, Executor executor) {
    return RpcConfig.newBuilder()
        .name("USER")
        .timeout(config.getInt(ExecConstants.USER_RPC_TIMEOUT))
        .executor(executor)
        .add(RpcType.HANDSHAKE, UserToBitHandshake.class, RpcType.HANDSHAKE, BitToUserHandshake.class) // user to bit
        .add(RpcType.RUN_QUERY, RunQuery.class, RpcType.QUERY_HANDLE, QueryId.class) // user to bit
        .add(RpcType.CANCEL_QUERY, QueryId.class, RpcType.ACK, Ack.class) // user to bit
        .add(RpcType.QUERY_DATA, QueryData.class, RpcType.ACK, Ack.class) // bit to user
        .add(RpcType.QUERY_RESULT, QueryResult.class, RpcType.ACK, Ack.class) // bit to user
        .add(RpcType.RESUME_PAUSED_QUERY, QueryId.class, RpcType.ACK, Ack.class) // user to bit
        .add(RpcType.GET_QUERY_PLAN_FRAGMENTS, GetQueryPlanFragments.class,
          RpcType.QUERY_PLAN_FRAGMENTS, QueryPlanFragments.class) // user to bit
        .add(RpcType.GET_CATALOGS, GetCatalogsReq.class, RpcType.CATALOGS, GetCatalogsResp.class) // user to bit
        .add(RpcType.GET_SCHEMAS, GetSchemasReq.class, RpcType.SCHEMAS, GetSchemasResp.class) // user to bit
        .add(RpcType.GET_TABLES, GetTablesReq.class, RpcType.TABLES, GetTablesResp.class) // user to bit
        .add(RpcType.GET_COLUMNS, GetColumnsReq.class, RpcType.COLUMNS, GetColumnsResp.class) // user to bit
        .add(RpcType.CREATE_PREPARED_STATEMENT, CreatePreparedStatementReq.class,
            RpcType.PREPARED_STATEMENT, CreatePreparedStatementResp.class) // user to bit
        .add(RpcType.SASL_MESSAGE, SaslMessage.class, RpcType.SASL_MESSAGE, SaslMessage.class) // user <-> bit
        .add(RpcType.GET_SERVER_META, GetServerMetaReq.class, RpcType.SERVER_META, GetServerMetaResp.class) // user to bit
        .build();
  }

  public static final int RPC_VERSION = 5;

  // prevent instantiation
  private UserRpcConfig() {
  }

  /**
   * Contains the list of methods supported by the server (from user to bit)
   */
  public static final Set<RpcType> SUPPORTED_SERVER_METHODS = Sets.immutableEnumSet(
      ImmutableSet
        .<RpcType> builder()
        .add(RpcType.RUN_QUERY, RpcType.CANCEL_QUERY, RpcType.GET_QUERY_PLAN_FRAGMENTS, RpcType.RESUME_PAUSED_QUERY,
          RpcType.GET_CATALOGS, RpcType.GET_SCHEMAS, RpcType.GET_TABLES, RpcType.GET_COLUMNS,
          RpcType.CREATE_PREPARED_STATEMENT, RpcType.GET_SERVER_META)
        .build()
        );
}
