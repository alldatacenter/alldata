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

import java.util.Arrays;
import java.util.List;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import org.apache.calcite.util.NlsString;
import org.apache.calcite.util.Util;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.planner.sql.parser.DrillParserUtil;
import org.apache.drill.exec.planner.sql.parser.SqlShowTables;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.ischema.InfoSchemaTableType;
import org.apache.drill.exec.work.foreman.ForemanSetupException;

import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.IS_SCHEMA_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SHRD_COL_TABLE_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SHRD_COL_TABLE_SCHEMA;

public class ShowTablesHandler extends DefaultSqlHandler {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ShowTablesHandler.class);

  public ShowTablesHandler(SqlHandlerConfig config) { super(config); }

  /** Rewrite the parse tree as SELECT ... FROM INFORMATION_SCHEMA.`TABLES` ... */
  @Override
  public SqlNode rewrite(SqlNode sqlNode) throws ForemanSetupException {
    SqlShowTables node = unwrap(sqlNode, SqlShowTables.class);
    List<SqlNode> selectList = Arrays.asList(
        new SqlIdentifier(SHRD_COL_TABLE_SCHEMA, SqlParserPos.ZERO),
        new SqlIdentifier(SHRD_COL_TABLE_NAME, SqlParserPos.ZERO));

    SqlNode fromClause = new SqlIdentifier(Arrays.asList(IS_SCHEMA_NAME, InfoSchemaTableType.TABLES.name()), SqlParserPos.ZERO);

    SchemaPlus schemaPlus;
    if (node.getDb() != null) {
      List<String> schemaNames = node.getDb().names;
      schemaPlus = SchemaUtilites.findSchema(config.getConverter().getDefaultSchema(), schemaNames);

      if (schemaPlus == null) {
        throw UserException.validationError()
            .message("Invalid schema name [%s]", SchemaUtilites.getSchemaPath(schemaNames))
            .build(logger);
      }

    } else {
      // If no schema is given in SHOW TABLES command, list tables from current schema
      schemaPlus = config.getConverter().getDefaultSchema();
    }

    if (SchemaUtilites.isRootSchema(schemaPlus)) {
      // If the default schema is a root schema, throw an error to select a default schema
      throw UserException.validationError()
          .message("No default schema selected. Select a schema using 'USE schema' command")
          .build(logger);
    }

    AbstractSchema drillSchema = SchemaUtilites.unwrapAsDrillSchemaInstance(schemaPlus);

    SqlNode where = DrillParserUtil.createCondition(
        new SqlIdentifier(SHRD_COL_TABLE_SCHEMA, SqlParserPos.ZERO),
        SqlStdOperatorTable.EQUALS,
        SqlLiteral.createCharString(drillSchema.getFullSchemaName(), Util.getDefaultCharset().name(), SqlParserPos.ZERO));

    SqlNode filter = null;
    if (node.getLikePattern() != null) {
      SqlNode likePattern = node.getLikePattern();
      SqlNode column = new SqlIdentifier(SHRD_COL_TABLE_NAME, SqlParserPos.ZERO);
      // wrap columns name values and condition in lower function if case insensitive
      if (!drillSchema.areTableNamesCaseSensitive() && likePattern instanceof SqlCharStringLiteral) {
        NlsString conditionString = ((SqlCharStringLiteral) likePattern).getNlsString();
        likePattern = SqlCharStringLiteral.createCharString(
            conditionString.getValue().toLowerCase(),
            conditionString.getCharsetName(),
            likePattern.getParserPosition());
        column = SqlStdOperatorTable.LOWER.createCall(SqlParserPos.ZERO, column);
      }
      filter = DrillParserUtil.createCondition(column, SqlStdOperatorTable.LIKE, likePattern);
    } else if (node.getWhereClause() != null) {
      filter = node.getWhereClause();
    }

    where = DrillParserUtil.createCondition(where, SqlStdOperatorTable.AND, filter);

    return new SqlSelect(SqlParserPos.ZERO, null, new SqlNodeList(selectList, SqlParserPos.ZERO),
        fromClause, where, null, null, null, null, null, null);
  }

  /**
   * Rewritten SHOW TABLES query should be executed against root schema. Otherwise if
   * query executed against, for example, jdbc plugin, returned table_schema column values
   * won't be consistent with Drill's values for same column. Also after jdbc filter push down
   * schema condition will be broken because it may contain name of Drill's storage plugin or
   * name of jdbc catalog which isn't present in table_schema column.
   *
   * @param sqlNode node produced by {@link #rewrite(SqlNode)}
   * @return converted rel node
   * @throws ForemanSetupException when fragment setup or ser/de failed
   * @throws RelConversionException when conversion failed
   * @throws ValidationException when sql node validation failed
   */
  @Override
  protected ConvertedRelNode validateAndConvert(SqlNode sqlNode) throws
      ForemanSetupException, RelConversionException, ValidationException {
    try {
      config.getConverter().useRootSchemaAsDefault(true);
      return super.validateAndConvert(sqlNode);
    } finally {
      config.getConverter().useRootSchemaAsDefault(false);
    }
  }
}