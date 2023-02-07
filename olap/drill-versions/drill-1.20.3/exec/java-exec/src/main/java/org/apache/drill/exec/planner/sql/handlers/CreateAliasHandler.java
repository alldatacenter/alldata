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
import org.apache.drill.exec.alias.AliasTarget;
import org.apache.drill.exec.alias.Aliases;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.planner.sql.DirectPlan;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.planner.sql.parser.SqlCreateAlias;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Handler for handling CREATE ALIAS statements.
 */
public class CreateAliasHandler extends BaseAliasHandler {

  private static final Logger logger = LoggerFactory.getLogger(CreateAliasHandler.class);

  public CreateAliasHandler(SqlHandlerConfig config) {
    super(config);
  }

  @Override
  public PhysicalPlan getPlan(SqlNode sqlNode) throws ForemanSetupException, IOException {
    checkAliasesEnabled();

    SqlCreateAlias node = unwrap(sqlNode, SqlCreateAlias.class);
    String alias = SchemaPath.getCompoundPath(node.getAlias().names.toArray(new String[0])).toExpr();

    String aliasTarget = ((SqlLiteral) node.getAliasKind()).toValue();
    AliasRegistry aliasRegistry = getAliasRegistry(aliasTarget);
    String value = getValue(node, aliasTarget);

    boolean replace = ((SqlLiteral) node.getReplace()).booleanValue();
    Aliases aliases = getAliases(node, aliasRegistry);
    if (!aliases.put(alias, value, replace)) {
      throw UserException.validationError()
        .message("Alias with given name [%s] already exists", alias)
        .build(logger);
    }
    return DirectPlan.createDirectPlan(context, true, String.format("%s alias '%s' for '%s' created successfully",
      StringUtils.capitalize(aliasTarget.toLowerCase(Locale.ROOT)), alias, value));
  }

  private String getValue(SqlCreateAlias node, String aliasTarget) {
    switch (AliasTarget.valueOf(aliasTarget)) {
      case TABLE: {
        return getTableQualifier(node.getSource());
      }
      case STORAGE: {
        return getStorageQualifier(node.getSource().names);
      }
      default:
        throw UserException.validationError()
          .message("Unsupported alias target: [%s]", aliasTarget)
          .build(logger);
    }
  }

  private String getStorageQualifier(List<String> path) {
    SchemaUtilites.resolveToDrillSchema(
      config.getConverter().getDefaultSchema(), path);
    if (path.size() > 1) {
      throw UserException.validationError()
        .message("Storage name expected, but provided [%s]",
          SchemaUtilites.getSchemaPath(path))
        .build(logger);
    }
    return SchemaPath.getCompoundPath(path.get(0)).toExpr();
  }

  private String getTableQualifier(SqlNode tableRef) {
    DrillTableInfo drillTableInfo = DrillTableInfo.getTableInfoHolder(tableRef, config);

    if (drillTableInfo.drillTable() == null) {
      throw UserException.validationError()
        .message("No table with given name [%s] exists in schema [%s]", drillTableInfo.tableName(),
          SchemaUtilites.getSchemaPath(drillTableInfo.schemaPath()))
        .build(logger);
    }
    String[] paths = new String[drillTableInfo.schemaPath().size() + 1];
    System.arraycopy(drillTableInfo.schemaPath().toArray(new String[0]), 0,
      paths, 0, drillTableInfo.schemaPath().size());
    paths[drillTableInfo.schemaPath().size()] = drillTableInfo.tableName();

    return SchemaPath.getCompoundPath(paths).toExpr();
  }

  private Aliases getAliases(SqlCreateAlias node, AliasRegistry aliasRegistry) {
    return ((SqlLiteral) node.getIsPublic()).booleanValue()
      ? getPublicAliases(node, aliasRegistry)
      : getUserAliases(node, aliasRegistry);
  }

  private Aliases getUserAliases(SqlCreateAlias node, AliasRegistry aliasRegistry) {
    if (!context.isImpersonationEnabled()) {
      throw UserException.validationError()
        .message("Cannot create user alias when user impersonation is disabled")
        .build(logger);
    }
    String userName = resolveUserName(node.getUser());
    aliasRegistry.createUserAliases(userName);

    return aliasRegistry.getUserAliases(userName);
  }

  private Aliases getPublicAliases(SqlCreateAlias node, AliasRegistry aliasRegistry) {
    if (node.getUser() != null) {
      throw UserException.validationError()
        .message("Cannot create public alias for specific user")
        .build(logger);
    }
    checkAdminPrivileges(context.getOptions());
    aliasRegistry.createPublicAliases();

    return aliasRegistry.getPublicAliases();
  }
}
