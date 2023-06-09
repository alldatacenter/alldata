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
import org.apache.commons.lang3.StringUtils;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.alias.AliasRegistry;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.planner.sql.DirectPlan;
import org.apache.drill.exec.planner.sql.parser.SqlDropAllAliases;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;

/**
 * Handler for handling DROP ALL ALIASES statements.
 */
public class DropAllAliasesHandler extends BaseAliasHandler {

  private static final Logger logger = LoggerFactory.getLogger(DropAllAliasesHandler.class);

  public DropAllAliasesHandler(SqlHandlerConfig config) {
    super(config);
  }

  @Override
  public PhysicalPlan getPlan(SqlNode sqlNode) throws ForemanSetupException, IOException {
    checkAliasesEnabled();

    SqlDropAllAliases node = unwrap(sqlNode, SqlDropAllAliases.class);

    String aliasTarget = ((SqlLiteral) node.getAliasKind()).toValue();
    AliasRegistry aliasRegistry = getAliasRegistry(aliasTarget);

    boolean isPublicAlias = ((SqlLiteral) node.getIsPublic()).booleanValue();
    if (isPublicAlias) {
      deletePublicAliases(node.getUser(), aliasRegistry);
    } else {
      deleteUserAliases(node.getUser(), aliasRegistry);
    }
    return DirectPlan.createDirectPlan(context, true, String.format("%s aliases dropped successfully",
      StringUtils.capitalize(aliasTarget.toLowerCase(Locale.ROOT))));
  }

  private void deleteUserAliases(SqlNode user, AliasRegistry aliasRegistry) {
    if (!context.isImpersonationEnabled()) {
      throw UserException.validationError()
        .message("Cannot drop user aliases when user impersonation is disabled")
        .build(logger);
    }
    String userName = resolveUserName(user);
    aliasRegistry.deleteUserAliases(userName);
  }

  private void deletePublicAliases(SqlNode user, AliasRegistry aliasRegistry) {
    if (user != null) {
      throw UserException.validationError()
        .message("Cannot drop public aliases for specific user")
        .build(logger);
    }
    checkAdminPrivileges(context.getOptions());
    aliasRegistry.deletePublicAliases();
  }
}
