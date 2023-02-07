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

import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.planner.sql.handlers.AbstractSqlHandler;
import org.apache.drill.exec.planner.sql.handlers.DropTableHandler;
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

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

public class SqlDropTable extends DrillSqlCall {
  public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator("DROP_TABLE", SqlKind.DROP_TABLE) {
    @Override
    public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
      return new SqlDropTable(pos, (SqlIdentifier) operands[0], (SqlLiteral) operands[1]);
    }
  };

  private final SqlIdentifier tableName;
  private final boolean tableExistenceCheck;

  public SqlDropTable(SqlParserPos pos, SqlIdentifier tableName, SqlLiteral tableExistenceCheck) {
    this(pos, tableName, tableExistenceCheck.booleanValue());
  }

  public SqlDropTable(SqlParserPos pos, SqlIdentifier tableName, boolean tableExistenceCheck) {
    super(pos);
    this.tableName = tableName;
    this.tableExistenceCheck = tableExistenceCheck;
  }

  @Override
  public SqlOperator getOperator() {
    return OPERATOR;
  }

  @Override
  public List<SqlNode> getOperandList() {
    final List<SqlNode> ops =
        ImmutableList.of(
            tableName,
            SqlLiteral.createBoolean(tableExistenceCheck, SqlParserPos.ZERO)
        );
    return ops;
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    writer.keyword("DROP");
    writer.keyword("TABLE");
    if (tableExistenceCheck) {
      writer.keyword("IF");
      writer.keyword("EXISTS");
    }
    tableName.unparse(writer, leftPrec, rightPrec);
  }

  @Override
  public AbstractSqlHandler getSqlHandler(SqlHandlerConfig config) {
    return new DropTableHandler(config);
  }

  public List<String> getSchema() {
    return SchemaUtilites.getSchemaPath(tableName);
  }

  public String getName() {
    if (tableName.isSimple()) {
      return tableName.getSimple();
    }

    return tableName.names.get(tableName.names.size() - 1);
  }

  public SqlIdentifier getTableIdentifier() {
    return tableName;
  }

  public boolean checkTableExistence() {
    return tableExistenceCheck;
  }

}
