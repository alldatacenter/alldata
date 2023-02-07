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

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.util.Util;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.planner.sql.handlers.AbstractSqlHandler;
import org.apache.drill.exec.planner.sql.handlers.MetastoreDropTableMetadataHandler;
import org.apache.drill.exec.planner.sql.handlers.SqlHandlerConfig;
import org.apache.drill.exec.util.Pointer;

import java.util.Arrays;
import java.util.List;

public class SqlDropTableMetadata extends DrillSqlCall {

  public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator("DROP_TABLE_METADATA", SqlKind.OTHER) {
    @Override
    public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
      return new SqlDropTableMetadata(pos, (SqlIdentifier) operands[0], (SqlLiteral) operands[1], (SqlLiteral) operands[2]);
    }
  };

  private final SqlIdentifier tableName;
  private final boolean checkMetadataExistence;
  private final DropMetadataType dropType;

  public SqlDropTableMetadata(SqlParserPos pos, SqlIdentifier tableName, SqlLiteral dropType, SqlLiteral checkMetadataExistence) {
    super(pos);
    this.tableName = tableName;
    this.dropType = DropMetadataType.valueOf(dropType.getStringValue().toUpperCase());
    this.checkMetadataExistence = checkMetadataExistence.booleanValue();
  }

  @Override
  public SqlOperator getOperator() {
    return OPERATOR;
  }

  @Override
  public List<SqlNode> getOperandList() {
    return Arrays.asList(
        tableName,
        SqlLiteral.createCharString(dropType.name(), SqlParserPos.ZERO),
        SqlLiteral.createBoolean(checkMetadataExistence, SqlParserPos.ZERO)
    );
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    writer.keyword("ANALYZE");
    writer.keyword("TABLE");
    tableName.unparse(writer, leftPrec, rightPrec);
    writer.keyword("DROP");
    writer.keyword(dropType.name());
    if (checkMetadataExistence) {
      writer.keyword("IF");
      writer.keyword("EXISTS");
    }
  }

  @Override
  public AbstractSqlHandler getSqlHandler(SqlHandlerConfig config, Pointer<String> textPlan) {
    return new MetastoreDropTableMetadataHandler(config, textPlan);
  }

  @Override
  public AbstractSqlHandler getSqlHandler(SqlHandlerConfig config) {
    return getSqlHandler(config, null);
  }

  public List<String> getSchemaPath() {
    return SchemaUtilites.getSchemaPath(tableName);
  }

  public String getName() {
    return Util.last(tableName.names);
  }

  public boolean checkMetadataExistence() {
    return checkMetadataExistence;
  }

  public DropMetadataType getDropType() {
    return dropType;
  }

  /**
   * Enum for metadata types to drop.
   */
  public enum DropMetadataType {
    METADATA,
    STATISTICS
  }
}
