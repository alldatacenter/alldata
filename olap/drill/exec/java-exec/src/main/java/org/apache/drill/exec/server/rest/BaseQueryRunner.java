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
package org.apache.drill.exec.server.rest;

import java.util.Map;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.tools.ValidationException;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.proto.UserProtos.QueryResultsMode;
import org.apache.drill.exec.proto.UserProtos.RunQuery;
import org.apache.drill.exec.rpc.UserClientConnection;
import org.apache.drill.exec.rpc.user.InboundImpersonationManager;
import org.apache.drill.exec.server.options.OptionSet;
import org.apache.drill.exec.server.options.SessionOptionManager;
import org.apache.drill.exec.store.SchemaTreeProvider;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.exec.work.WorkManager;
import org.apache.parquet.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseQueryRunner {
  private static final Logger logger = LoggerFactory.getLogger(BaseQueryRunner.class);

  protected final WorkManager workManager;
  protected final WebUserConnection webUserConnection;
  private final OptionSet options;
  protected int maxRows;
  protected QueryId queryId;

  public BaseQueryRunner(final WorkManager workManager, final WebUserConnection webUserConnection) {
    this.workManager = workManager;
    this.webUserConnection = webUserConnection;
    this.options = webUserConnection.getSession().getOptions();
    this.maxRows = options.getInt(ExecConstants.QUERY_MAX_ROWS);
  }

  protected void applyUserName(String userName) {
    if (Strings.isNullOrEmpty(userName)) {
      return;
    }

    DrillConfig config = workManager.getContext().getConfig();
    if (!config.getBoolean(ExecConstants.IMPERSONATION_ENABLED)) {
      throw UserException.permissionError()
        .message("User impersonation is not enabled")
        .build(logger);
    }

    String proxyUserName = webUserConnection.getSession().getCredentials().getUserName();
    if (proxyUserName.equals(userName)) {
      // Either the proxy user is impersonating itself, which is a no-op, or
      // the userName on the UserSession has already been modified to be the
      // impersonated user by an earlier request belonging to the same session.
      return;
    }

    InboundImpersonationManager inboundImpersonationManager = new InboundImpersonationManager();
    boolean isAdmin = !config.getBoolean(ExecConstants.USER_AUTHENTICATION_ENABLED) ||
      ImpersonationUtil.hasAdminPrivileges(
          proxyUserName,
          ExecConstants.ADMIN_USERS_VALIDATOR.getAdminUsers(options),
          ExecConstants.ADMIN_USER_GROUPS_VALIDATOR.getAdminUserGroups(options));
    if (isAdmin) {
      // Admin user can impersonate any user they want to (when authentication is disabled, all users are admin)
      webUserConnection.getSession().replaceUserCredentials(
        inboundImpersonationManager,
        UserBitShared.UserCredentials.newBuilder().setUserName(userName).build());
    } else {
      // Check configured impersonation rules to see if this user is allowed to impersonate the given user
      inboundImpersonationManager.replaceUserOnSession(userName, webUserConnection.getSession());
    }
  }

  protected void applyOptions(Map<String, String> options) {
     if (options != null) {
      SessionOptionManager sessionOptionManager = webUserConnection.getSession().getOptions();
      for (Map.Entry<String, String> entry : options.entrySet()) {
        sessionOptionManager.setLocalOption(entry.getKey(), entry.getValue());
      }
    }
  }

  protected void applyDefaultSchema(String defaultSchema) throws ValidationException {
    if (!Strings.isNullOrEmpty(defaultSchema)) {
      SessionOptionManager options = webUserConnection.getSession().getOptions();
      @SuppressWarnings("resource")
      SchemaTreeProvider schemaTreeProvider = new SchemaTreeProvider(workManager.getContext());
      SchemaPlus rootSchema = schemaTreeProvider.createRootSchema(options);
      webUserConnection.getSession().setDefaultSchemaPath(defaultSchema, rootSchema);
    }
  }

  protected void applyRowLimit(int limit) {
    if (limit > 0 && maxRows > 0) {
      maxRows = Math.min(limit, maxRows);
    } else {
      maxRows = Math.max(limit, maxRows);
    }
  }

  protected void startQuery(QueryType queryType, String query, UserClientConnection clientConn) {
    final RunQuery runQuery = RunQuery.newBuilder()
        .setType(queryType)
        .setPlan(query)
        .setResultsMode(QueryResultsMode.STREAM_FULL)
        .setAutolimitRowcount(maxRows)
        .build();

    // Submit user query to Drillbit work queue.
    queryId = workManager.getUserWorker().submitWork(clientConn, runQuery);
  }
}
