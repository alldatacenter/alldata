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
package org.apache.drill.exec.planner.sql.handlers;

import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.alias.AliasRegistry;
import org.apache.drill.exec.alias.AliasTarget;
import org.apache.drill.exec.server.options.QueryOptionManager;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class BaseAliasHandler extends DefaultSqlHandler {

  private static final Logger logger = LoggerFactory.getLogger(BaseAliasHandler.class);

  public BaseAliasHandler(SqlHandlerConfig config) {
    super(config);
  }

  /**
   * Checks whether aliases support is enabled.
   */
  protected void checkAliasesEnabled() {
    if (!context.getOptions().getBoolean(ExecConstants.ENABLE_ALIASES)) {
      throw UserException.validationError()
        .message("Aliases support is disabled.")
        .build(logger);
    }
  }

  /**
   * Admin privileges checker.
   *
   * @param options Options object
   */
  protected void checkAdminPrivileges(QueryOptionManager options) {
    if (context.isUserAuthenticationEnabled() && !hasAdminPrivileges(options)) {
      throw UserException
        .permissionError()
        .message("Not authorized to perform operations on public aliases.")
        .build(logger);
    }
  }

  /**
   * Returns {@code true} if query user has admin privileges.
   *
   * @param options Options object
   * @return {@code true} if query user has admin privileges
   */
  protected boolean hasAdminPrivileges(QueryOptionManager options) {
    return ImpersonationUtil.hasAdminPrivileges(
      context.getQueryUserName(),
      ExecConstants.ADMIN_USERS_VALIDATOR.getAdminUsers(options),
      ExecConstants.ADMIN_USER_GROUPS_VALIDATOR.getAdminUserGroups(options));
  }

  /**
   * Obtains userName from specified node and ensures that specified user has admin privileges
   * for the case when it is different from the query user.
   * If no user specified, query user will be returned.
   *
   * @param user source for userName
   * @return userName
   */
  protected String resolveUserName(SqlNode user) {
    String queryUserName = context.getQueryUserName();
    return Optional.ofNullable(user)
      .map(SqlLiteral.class::cast)
      .map(SqlLiteral::toValue)
      .filter(provided -> !queryUserName.equals(provided))
      .map(provided -> {
        // check whether current user is admin before performing alias operation for another user
        if (!hasAdminPrivileges(context.getOptions())) {
          throw UserException.permissionError()
            .message("Not authorized to perform operations on aliases for other users.")
            .build(logger);
        }
        return provided;
      })
      .orElse(queryUserName);
  }

  protected AliasRegistry getAliasRegistry(String aliasTarget) {
    switch (AliasTarget.valueOf(aliasTarget)) {
      case TABLE: {
        return config.getContext().getAliasRegistryProvider().getTableAliasesRegistry();
      }
      case STORAGE: {
        return config.getContext().getAliasRegistryProvider().getStorageAliasesRegistry();
      }
      default:
        throw UserException.validationError()
          .message("Unsupported alias target: [%s]", aliasTarget)
          .build(logger);
    }
  }
}
