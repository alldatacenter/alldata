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
package org.apache.drill.exec.planner.sql.parser;

import java.util.List;

import org.apache.drill.exec.planner.sql.handlers.AbstractSqlHandler;
import org.apache.drill.exec.planner.sql.handlers.ShowSchemasHandler;
import org.apache.drill.exec.planner.sql.handlers.SqlHandlerConfig;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Sql parse tree node to represent statement:
 * SHOW {DATABASES | SCHEMAS} [LIKE 'pattern' | WHERE expr]
 */
public class SqlShowSchemas extends DrillSqlCall {

  private final SqlNode likePattern;
  private final SqlNode whereClause;

  public static final SqlSpecialOperator OPERATOR =
    new SqlSpecialOperator("SHOW_SCHEMAS", SqlKind.OTHER) {
    @Override
    public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
      return new SqlShowSchemas(pos, operands[0], operands[1]);
    }
  };

  public SqlShowSchemas(SqlParserPos pos, SqlNode likePattern, SqlNode whereClause) {
    super(pos);
    this.likePattern = likePattern;
    this.whereClause = whereClause;
  }

  @Override
  public SqlOperator getOperator() {
    return OPERATOR;
  }

  @Override
  public List<SqlNode> getOperandList() {
    List<SqlNode> opList = Lists.newArrayList();
    opList.add(likePattern);
    opList.add(whereClause);
    return opList;
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    writer.keyword("SHOW");
    writer.keyword("SCHEMAS");
    if (likePattern != null) {
      writer.keyword("LIKE");
      likePattern.unparse(writer, leftPrec, rightPrec);
    }
    if (whereClause != null) {
      whereClause.unparse(writer, leftPrec, rightPrec);
    }
  }

  @Override
  public AbstractSqlHandler getSqlHandler(SqlHandlerConfig config) {
    return new ShowSchemasHandler(config);
  }

  public SqlNode getLikePattern() { return likePattern; }
  public SqlNode getWhereClause() { return whereClause; }

}
