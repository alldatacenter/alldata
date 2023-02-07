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

import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserProtos.CreatePreparedStatementReq;
import org.apache.drill.exec.proto.UserProtos.GetCatalogsReq;
import org.apache.drill.exec.proto.UserProtos.GetColumnsReq;
import org.apache.drill.exec.proto.UserProtos.GetQueryPlanFragments;
import org.apache.drill.exec.proto.UserProtos.GetSchemasReq;
import org.apache.drill.exec.proto.UserProtos.GetServerMetaReq;
import org.apache.drill.exec.proto.UserProtos.GetTablesReq;
import org.apache.drill.exec.proto.UserProtos.RpcType;
import org.apache.drill.exec.proto.UserProtos.RunQuery;
import org.apache.drill.exec.rpc.RequestHandler;
import org.apache.drill.exec.rpc.Response;
import org.apache.drill.exec.rpc.ResponseSender;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.user.UserServer.BitToUserConnection;
import org.apache.drill.exec.work.user.UserWorker;

import com.google.protobuf.InvalidProtocolBufferException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

/**
 * Should create only one instance of this class per Drillbit service.
 */
// package private
class UserServerRequestHandler implements RequestHandler<BitToUserConnection> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserServerRequestHandler.class);

  private final UserWorker worker;

  public UserServerRequestHandler(final UserWorker worker) {
    this.worker = worker;
  }

  @Override
  public void handle(BitToUserConnection connection, int rpcType, ByteBuf pBody, ByteBuf dBody,
                     ResponseSender responseSender)
  throws RpcException {
    switch (rpcType) {

    case RpcType.RUN_QUERY_VALUE:
      logger.debug("Received query to run.  Returning query handle.");
      try {
        final RunQuery query = RunQuery.PARSER.parseFrom(new ByteBufInputStream(pBody));
        final QueryId queryId = worker.submitWork(connection, query);
        responseSender.send(new Response(RpcType.QUERY_HANDLE, queryId));
        break;
      } catch (InvalidProtocolBufferException e) {
        throw new RpcException("Failure while decoding RunQuery body.", e);
      }

    case RpcType.CANCEL_QUERY_VALUE:
      try {
        final QueryId queryId = QueryId.PARSER.parseFrom(new ByteBufInputStream(pBody));
        final Ack ack = worker.cancelQuery(queryId);
        responseSender.send(new Response(RpcType.ACK, ack));
        break;
      } catch (InvalidProtocolBufferException e) {
        throw new RpcException("Failure while decoding QueryId body.", e);
      }

    case RpcType.RESUME_PAUSED_QUERY_VALUE:
      try {
        final QueryId queryId = QueryId.PARSER.parseFrom(new ByteBufInputStream(pBody));
        final Ack ack = worker.resumeQuery(queryId);
        responseSender.send(new Response(RpcType.ACK, ack));
        break;
      } catch (final InvalidProtocolBufferException e) {
        throw new RpcException("Failure while decoding QueryId body.", e);
      }
    case RpcType.GET_QUERY_PLAN_FRAGMENTS_VALUE:
      try {
        final GetQueryPlanFragments req = GetQueryPlanFragments.PARSER.parseFrom(new ByteBufInputStream(pBody));
        responseSender.send(new Response(RpcType.QUERY_PLAN_FRAGMENTS, worker.getQueryPlan(connection, req)));
        break;
      } catch(final InvalidProtocolBufferException e) {
        throw new RpcException("Failure while decoding GetQueryPlanFragments body.", e);
      }
    case RpcType.GET_CATALOGS_VALUE:
      try {
        final GetCatalogsReq req = GetCatalogsReq.PARSER.parseFrom(new ByteBufInputStream(pBody));
        worker.submitCatalogMetadataWork(connection.getSession(), req, responseSender);
        break;
      } catch (final InvalidProtocolBufferException e) {
        throw new RpcException("Failure while decoding GetCatalogsReq body.", e);
      }
    case RpcType.GET_SCHEMAS_VALUE:
      try {
        final GetSchemasReq req = GetSchemasReq.PARSER.parseFrom(new ByteBufInputStream(pBody));
        worker.submitSchemasMetadataWork(connection.getSession(), req, responseSender);
        break;
      } catch (final InvalidProtocolBufferException e) {
        throw new RpcException("Failure while decoding GetSchemasReq body.", e);
      }
    case RpcType.GET_TABLES_VALUE:
      try {
        final GetTablesReq req = GetTablesReq.PARSER.parseFrom(new ByteBufInputStream(pBody));
        worker.submitTablesMetadataWork(connection.getSession(), req, responseSender);
        break;
      } catch (final InvalidProtocolBufferException e) {
        throw new RpcException("Failure while decoding GetTablesReq body.", e);
      }
    case RpcType.GET_COLUMNS_VALUE:
      try {
        final GetColumnsReq req = GetColumnsReq.PARSER.parseFrom(new ByteBufInputStream(pBody));
        worker.submitColumnsMetadataWork(connection.getSession(), req, responseSender);
        break;
      } catch (final InvalidProtocolBufferException e) {
        throw new RpcException("Failure while decoding GetColumnsReq body.", e);
      }
    case RpcType.CREATE_PREPARED_STATEMENT_VALUE:
      try {
        final CreatePreparedStatementReq req =
            CreatePreparedStatementReq.PARSER.parseFrom(new ByteBufInputStream(pBody));
        worker.submitPreparedStatementWork(connection, req, responseSender);
        break;
      } catch (final InvalidProtocolBufferException e) {
        throw new RpcException("Failure while decoding CreatePreparedStatementReq body.", e);
      }
    case RpcType.GET_SERVER_META_VALUE:
      try {
        final GetServerMetaReq req =
            GetServerMetaReq.PARSER.parseFrom(new ByteBufInputStream(pBody));
        worker.submitServerMetadataWork(connection.getSession(), req, responseSender);
        break;
      } catch (final InvalidProtocolBufferException e) {
        throw new RpcException("Failure while decoding CreatePreparedStatementReq body.", e);
      }
    default:
      throw new UnsupportedOperationException(
          String.format("UserServerRequestHandler received rpc of unknown type. Type was %d.", rpcType));
    }
  }
}
