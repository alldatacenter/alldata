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
import java.util.Collections;
import java.util.List;

import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.util.NlsString;
import org.apache.drill.exec.planner.sql.parser.DrillParserUtil;
import org.apache.drill.exec.planner.sql.parser.SqlShowSchemas;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.IS_SCHEMA_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SCHS_COL_SCHEMA_NAME;

import org.apache.drill.exec.store.ischema.InfoSchemaTableType;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

public class ShowSchemasHandler extends DefaultSqlHandler {

  public ShowSchemasHandler(SqlHandlerConfig config) { super(config); }

  /** Rewrite the parse tree as SELECT ... FROM INFORMATION_SCHEMA.SCHEMATA ... */
  @Override
  public SqlNode rewrite(SqlNode sqlNode) throws ForemanSetupException {
    SqlShowSchemas node = unwrap(sqlNode, SqlShowSchemas.class);
    List<SqlNode> selectList = Collections.singletonList(new SqlIdentifier(SCHS_COL_SCHEMA_NAME, SqlParserPos.ZERO));

    SqlNode fromClause = new SqlIdentifier(Arrays.asList(IS_SCHEMA_NAME, InfoSchemaTableType.SCHEMATA.name()), SqlParserPos.ZERO);

    SqlNode where = null;
    SqlNode likePattern = node.getLikePattern();
    if (likePattern != null) {
      SqlNode column = new SqlIdentifier(SCHS_COL_SCHEMA_NAME, SqlParserPos.ZERO);
      // schema names are case insensitive, wrap column in lower function, pattern to lower case
      if (likePattern instanceof SqlCharStringLiteral) {
        NlsString conditionString = ((SqlCharStringLiteral) likePattern).getNlsString();
        likePattern = SqlCharStringLiteral.createCharString(
            conditionString.getValue().toLowerCase(),
            conditionString.getCharsetName(),
            likePattern.getParserPosition());
        column = SqlStdOperatorTable.LOWER.createCall(SqlParserPos.ZERO, column);
      }
      where = DrillParserUtil.createCondition(column, SqlStdOperatorTable.LIKE, likePattern);
    } else if (node.getWhereClause() != null) {
      where = node.getWhereClause();
    }

    return new SqlSelect(SqlParserPos.ZERO, null, new SqlNodeList(selectList, SqlParserPos.ZERO),
        fromClause, where, null, null, null, null, null, null);
  }
}
