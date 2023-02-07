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

import org.apache.calcite.sql.SqlSetOption;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.drill.exec.server.options.QueryOptionManager;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base handler for SQL_SET kind statements.
 */
abstract class AbstractSqlSetHandler extends AbstractSqlHandler {

  private static final Logger logger = LoggerFactory.getLogger(AbstractSqlSetHandler.class);

  final QueryContext context;

  AbstractSqlSetHandler(QueryContext context) {
    this.context = context;
  }

  /**
   * Extracts query {@link OptionValue.OptionScope} from the {@link SqlSetOption}.
   * @param statement Statement object
   * @param options Options object
   * @return parsed query scope
   */
  OptionValue.OptionScope getScope(SqlSetOption statement, QueryOptionManager options) {
    String scope = statement.getScope();

    if (scope == null) {
      return OptionValue.OptionScope.SESSION;
    }

    switch (scope.toLowerCase()) {
      case "session":
        if (options.getBoolean(ExecConstants.SKIP_ALTER_SESSION_QUERY_PROFILE)) {
          logger.debug("Will not write profile for ALTER SESSION SET ... ");
          context.skipWritingProfile(true);
        }
        return OptionValue.OptionScope.SESSION;
      case "system":
        return OptionValue.OptionScope.SYSTEM;
      default:
        throw UserException.validationError()
            .message("Invalid OPTION scope %s. Scope must be SESSION or SYSTEM.", scope)
            .build(logger);
    }
  }

  /**
   * Admin privileges checker.
   * @param options Options object
   */
  void checkAdminPrivileges(QueryOptionManager options) {
    if (context.isUserAuthenticationEnabled()
        && !ImpersonationUtil.hasAdminPrivileges(
            context.getQueryUserName(),
            ExecConstants.ADMIN_USERS_VALIDATOR.getAdminUsers(options),
            ExecConstants.ADMIN_USER_GROUPS_VALIDATOR.getAdminUserGroups(options))) {

      throw UserException
          .permissionError()
          .message("Not authorized to change SYSTEM options.")
          .build(logger);
    }
  }
}
