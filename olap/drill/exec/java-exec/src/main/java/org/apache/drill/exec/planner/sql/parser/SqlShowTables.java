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

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.exec.planner.sql.handlers.AbstractSqlHandler;
import org.apache.drill.exec.planner.sql.handlers.ShowTablesHandler;
import org.apache.drill.exec.planner.sql.handlers.SqlHandlerConfig;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.List;

/**
 * Sql parse tree node to represent statement:
 * SHOW TABLES [{FROM | IN} db_name] [LIKE 'pattern' | WHERE expr]
 */
public class SqlShowTables extends DrillSqlCall {

  private final SqlIdentifier db;
  private final SqlNode likePattern;
  private final SqlNode whereClause;

  public static final SqlSpecialOperator OPERATOR =
    new SqlSpecialOperator("SHOW_TABLES", SqlKind.OTHER) {
    @Override
    public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
      return new SqlShowTables(pos, (SqlIdentifier) operands[0], operands[1], operands[2]);
    }
  };

  public SqlShowTables(SqlParserPos pos, SqlIdentifier db, SqlNode likePattern, SqlNode whereClause) {
    super(pos);
    this.db = db;
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
    opList.add(db);
    opList.add(likePattern);
    opList.add(whereClause);
    return opList;
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    writer.keyword("SHOW");
    writer.keyword("TABLES");
    if (db != null) {
      db.unparse(writer, leftPrec, rightPrec);
    }
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
    return new ShowTablesHandler(config);
  }

  public SqlIdentifier getDb() { return db; }
  public SqlNode getLikePattern() { return likePattern; }
  public SqlNode getWhereClause() { return whereClause; }

}
