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
package org.apache.drill.exec.work.metadata;

import java.util.Arrays;

import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.sql.SqlJdbcFunctionCall;
import org.apache.calcite.sql.parser.SqlAbstractParserImpl.Metadata;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.sql.DrillParserConfig;
import org.apache.drill.exec.proto.UserProtos.ConvertSupport;
import org.apache.drill.exec.proto.UserProtos.CorrelationNamesSupport;
import org.apache.drill.exec.proto.UserProtos.DateTimeLiteralsSupport;
import org.apache.drill.exec.proto.UserProtos.GetServerMetaReq;
import org.apache.drill.exec.proto.UserProtos.GetServerMetaResp;
import org.apache.drill.exec.proto.UserProtos.GroupBySupport;
import org.apache.drill.exec.proto.UserProtos.IdentifierCasing;
import org.apache.drill.exec.proto.UserProtos.NullCollation;
import org.apache.drill.exec.proto.UserProtos.OrderBySupport;
import org.apache.drill.exec.proto.UserProtos.OuterJoinSupport;
import org.apache.drill.exec.proto.UserProtos.RequestStatus;
import org.apache.drill.exec.proto.UserProtos.RpcType;
import org.apache.drill.exec.proto.UserProtos.ServerMeta;
import org.apache.drill.exec.proto.UserProtos.SubQuerySupport;
import org.apache.drill.exec.proto.UserProtos.UnionSupport;
import org.apache.drill.exec.resolver.TypeCastRules;
import org.apache.drill.exec.rpc.Response;
import org.apache.drill.exec.rpc.ResponseSender;
import org.apache.drill.exec.rpc.user.UserSession;
import org.apache.drill.exec.server.DrillbitContext;

import org.apache.drill.shaded.guava.com.google.common.base.Splitter;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

/**
 * Contains worker {@link Runnable} for returning server meta information
 */
public class ServerMetaProvider {
  private static ServerMeta DEFAULT = ServerMeta.newBuilder()
      .addAllConvertSupport(getSupportedConvertOps())
      .addAllDateTimeFunctions(Splitter.on(",").split(SqlJdbcFunctionCall.getTimeDateFunctions()))
      .addAllDateTimeLiteralsSupport(Arrays.asList(DateTimeLiteralsSupport.values()))
      .addAllNumericFunctions(Splitter.on(",").split(SqlJdbcFunctionCall.getNumericFunctions()))
      .addAllOrderBySupport(Arrays.asList(OrderBySupport.OB_UNRELATED, OrderBySupport.OB_EXPRESSION))
      .addAllOuterJoinSupport(Arrays.asList(OuterJoinSupport.OJ_LEFT, OuterJoinSupport.OJ_RIGHT, OuterJoinSupport.OJ_FULL))
      .addAllStringFunctions(Splitter.on(",").split(SqlJdbcFunctionCall.getStringFunctions()))
      .addAllSystemFunctions(Splitter.on(",").split(SqlJdbcFunctionCall.getSystemFunctions()))
      .addAllSubquerySupport(Arrays.asList(SubQuerySupport.SQ_CORRELATED, SubQuerySupport.SQ_IN_COMPARISON, SubQuerySupport.SQ_IN_EXISTS, SubQuerySupport.SQ_IN_QUANTIFIED))
      .addAllUnionSupport(Arrays.asList(UnionSupport.U_UNION, UnionSupport.U_UNION_ALL))
      .setAllTablesSelectable(false)
      .setBlobIncludedInMaxRowSize(true)
      .setCatalogAtStart(true)
      .setCatalogSeparator(".")
      .setCatalogTerm("catalog")
      .setColumnAliasingSupported(true)
      .setNullPlusNonNullEqualsNull(true)
      .setCorrelationNamesSupport(CorrelationNamesSupport.CN_ANY)
      .setReadOnly(false)
      .setGroupBySupport(GroupBySupport.GB_UNRELATED)
      .setLikeEscapeClauseSupported(true)
      .setNullCollation(NullCollation.NC_HIGH)
      .setSchemaTerm("schema")
      .setSearchEscapeString("\\")
      .setTableTerm("table")
      .build();


  private static final Iterable<ConvertSupport> getSupportedConvertOps() {
    // A set would be more appropriate but it's not possible to produce
    // duplicates, and an iterable is all we need.
    ImmutableList.Builder<ConvertSupport> supportedConvertedOps = ImmutableList.builder();

    for (MinorType from: MinorType.values()) {
      for (MinorType to: MinorType.values()) {
        if (TypeCastRules.isCastable(from, to)) {
          supportedConvertedOps.add(ConvertSupport.newBuilder().setFrom(from).setTo(to).build());
        }
      }
    }

    return supportedConvertedOps.build();
  }
  /**
   * Runnable that creates server meta information for given {@code ServerMetaReq} and
   * sends the response at the end.
   */
  public static class ServerMetaWorker implements Runnable {
    private final UserSession session;
    private final DrillbitContext context;
    @SuppressWarnings("unused")
    private final GetServerMetaReq req;
    private final ResponseSender responseSender;

    public ServerMetaWorker(final UserSession session, final DrillbitContext context,
        final GetServerMetaReq req, final ResponseSender responseSender) {
      this.session = session;
      this.context = context;
      this.req = req;
      this.responseSender = responseSender;
    }

    @Override
    public void run() {
      final GetServerMetaResp.Builder respBuilder = GetServerMetaResp.newBuilder();
      try {
        final ServerMeta.Builder metaBuilder = ServerMeta.newBuilder(DEFAULT);
        PlannerSettings plannerSettings = new PlannerSettings(session.getOptions(), context.getFunctionImplementationRegistry());

        DrillParserConfig config = new DrillParserConfig(plannerSettings);

        int identifierMaxLength = config.identifierMaxLength();
        Metadata metadata = SqlParser.create("", config).getMetadata();
        metaBuilder
          .setMaxCatalogNameLength(identifierMaxLength)
          .setMaxColumnNameLength(identifierMaxLength)
          .setMaxCursorNameLength(identifierMaxLength)
          .setMaxSchemaNameLength(identifierMaxLength)
          .setMaxTableNameLength(identifierMaxLength)
          .setMaxUserNameLength(identifierMaxLength)
          .setIdentifierQuoteString(config.quoting().string)
          .setIdentifierCasing(getIdentifierCasing(config.unquotedCasing(), config.caseSensitive()))
          .setQuotedIdentifierCasing(getIdentifierCasing(config.quotedCasing(), config.caseSensitive()))
          .addAllSqlKeywords(Splitter.on(",").split(metadata.getJdbcKeywords()))
          .setCurrentSchema(session.getDefaultSchemaPath());
        respBuilder.setServerMeta(metaBuilder);
        respBuilder.setStatus(RequestStatus.OK);
      } catch(Throwable t) {
        respBuilder.setStatus(RequestStatus.FAILED);
        respBuilder.setError(MetadataProvider.createPBError("server meta", t));
      } finally {
        responseSender.send(new Response(RpcType.SERVER_META, respBuilder.build()));
      }
    }

    public static IdentifierCasing getIdentifierCasing(Casing casing, boolean caseSensitive) {
      switch(casing) {
      case TO_LOWER:
        return IdentifierCasing.IC_STORES_LOWER;

      case TO_UPPER:
        return IdentifierCasing.IC_STORES_UPPER;

      case UNCHANGED:
        return caseSensitive ? IdentifierCasing.IC_SUPPORTS_MIXED : IdentifierCasing.IC_STORES_MIXED;

      default:
        throw new AssertionError("Unknown casing:" + casing);
      }
    }
  }
}