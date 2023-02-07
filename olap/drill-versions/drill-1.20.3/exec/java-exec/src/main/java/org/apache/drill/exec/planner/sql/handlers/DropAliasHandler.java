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
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.alias.AliasRegistry;
import org.apache.drill.exec.alias.Aliases;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.planner.sql.DirectPlan;
import org.apache.drill.exec.planner.sql.parser.SqlDropAlias;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;

/**
 * Handler for handling DROP ALIAS statements.
 */
public class DropAliasHandler extends BaseAliasHandler {

  private static final Logger logger = LoggerFactory.getLogger(DropAliasHandler.class);

  public DropAliasHandler(SqlHandlerConfig config) {
    super(config);
  }

  @Override
  public PhysicalPlan getPlan(SqlNode sqlNode) throws ForemanSetupException, IOException {
    checkAliasesEnabled();

    SqlDropAlias node = unwrap(sqlNode, SqlDropAlias.class);
    String alias = SchemaPath.getCompoundPath(node.getAlias().names.toArray(new String[0])).toExpr();

    String aliasTarget = ((SqlLiteral) node.getAliasKind()).toValue();
    AliasRegistry aliasRegistry = getAliasRegistry(aliasTarget);

    Aliases aliases = getAliases(node, aliasRegistry);
    boolean checkIfExists = ((SqlLiteral) node.getIfExists()).booleanValue();
    boolean removed = aliases.remove(alias);
    if (!removed && !checkIfExists) {
      throw UserException.validationError()
        .message("No alias found with given name [%s]", alias)
        .build(logger);
    }
    String message = removed
      ? String.format("%s alias '%s' dropped successfully", StringUtils.capitalize(aliasTarget.toLowerCase(Locale.ROOT)), alias)
      : String.format("No %s alias found with given name [%s]", aliasTarget.toLowerCase(Locale.ROOT), alias);
    return DirectPlan.createDirectPlan(context, removed, message);
  }

  private Aliases getAliases(SqlDropAlias dropAlias, AliasRegistry aliasRegistry) {
    boolean isPublicAlias = ((SqlLiteral) dropAlias.getIsPublic()).booleanValue();
    return isPublicAlias
      ? getPublicAliases(dropAlias, aliasRegistry)
      : getUserAliases(dropAlias, aliasRegistry);
  }

  private Aliases getUserAliases(SqlDropAlias dropAlias, AliasRegistry aliasRegistry) {
    if (!context.isImpersonationEnabled()) {
      throw UserException.validationError()
        .message("Cannot drop user alias when user impersonation is disabled")
        .build(logger);
    }
    String userName = resolveUserName(dropAlias.getUser());
    return aliasRegistry.getUserAliases(userName);
  }

  private Aliases getPublicAliases(SqlDropAlias dropAlias, AliasRegistry aliasRegistry) {
    if (dropAlias.getUser() != null) {
      throw UserException.validationError()
        .message("Cannot drop public alias for specific user")
        .build(logger);
    }
    checkAdminPrivileges(context.getOptions());
    return aliasRegistry.getPublicAliases();
  }
}
