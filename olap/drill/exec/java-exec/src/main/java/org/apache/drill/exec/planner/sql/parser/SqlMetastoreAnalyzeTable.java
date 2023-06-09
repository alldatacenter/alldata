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
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlNumericLiteral;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.planner.sql.handlers.AbstractSqlHandler;
import org.apache.drill.exec.planner.sql.handlers.MetastoreAnalyzeTableHandler;
import org.apache.drill.exec.planner.sql.handlers.SqlHandlerConfig;
import org.apache.drill.exec.util.Pointer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SqlMetastoreAnalyzeTable extends DrillSqlCall {

  public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator("METASTORE_ANALYZE_TABLE", SqlKind.OTHER_DDL) {
    public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
      return new SqlMetastoreAnalyzeTable(pos, operands[0], (SqlNodeList) operands[1], operands[2],
          (SqlLiteral) operands[3], (SqlNumericLiteral) operands[4]);
    }
  };

  private final SqlNode tableRef;
  private final SqlNodeList fieldList;
  private final SqlLiteral level;
  private final SqlLiteral estimate;
  private final SqlNumericLiteral samplePercent;

  public SqlMetastoreAnalyzeTable(SqlParserPos pos, SqlNode tableRef, SqlNodeList fieldList,
      SqlNode level, SqlLiteral estimate, SqlNumericLiteral samplePercent) {
    super(pos);
    this.tableRef = tableRef;
    this.fieldList = fieldList;
    this.level = level != null ? SqlLiteral.unchain(level) : null;
    this.estimate = estimate;
    this.samplePercent = samplePercent;
  }

  @Override
  public SqlOperator getOperator() {
    return OPERATOR;
  }

  @Override
  public List<SqlNode> getOperandList() {
    return Arrays.asList(tableRef, fieldList, level, estimate, samplePercent);
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    writer.keyword("ANALYZE");
    writer.keyword("TABLE");
    tableRef.unparse(writer, leftPrec, rightPrec);
    if (fieldList != null) {
      writer.keyword("COLUMNS");
      if (fieldList.size() > 0) {
        writer.keyword("(");
        fieldList.get(0).unparse(writer, leftPrec, rightPrec);
        for (int i = 1; i < fieldList.size(); i++) {
          writer.keyword(",");
          fieldList.get(i).unparse(writer, leftPrec, rightPrec);
        }
        writer.keyword(")");
      } else {
        writer.keyword("NONE");
      }
    }
    writer.keyword("REFRESH");
    writer.keyword("METADATA");
    if (level != null) {
      level.unparse(writer, leftPrec, rightPrec);
    }
    if (estimate != null) {
      writer.keyword(estimate.booleanValue() ? "ESTIMATE" : "COMPUTE");
      writer.keyword("STATISTICS");
    }
    if (samplePercent != null) {
      writer.keyword("SAMPLE");
      samplePercent.unparse(writer, leftPrec, rightPrec);
      writer.keyword("PERCENT");
    }
  }

  @Override
  public AbstractSqlHandler getSqlHandler(SqlHandlerConfig config, Pointer<String> textPlan) {
    return new MetastoreAnalyzeTableHandler(config, textPlan);
  }

  @Override
  public AbstractSqlHandler getSqlHandler(SqlHandlerConfig config) {
    return getSqlHandler(config, null);
  }

  public SqlNode getTableRef() {
    return tableRef;
  }

  public List<SchemaPath> getFieldNames() {
    if (fieldList == null) {
      return null;
    }

    return fieldList.getList().stream()
        .map(sqlNode -> SchemaPath.parseFromString(sqlNode.toSqlString(null, true).getSql()))
        .collect(Collectors.toList());
  }

  public SqlNodeList getFieldList() {
    return fieldList;
  }

  public SqlLiteral getLevel() {
    return level;
  }

  public SqlLiteral getEstimate() {
    return estimate;
  }

  public SqlNumericLiteral getSamplePercent() {
    return samplePercent;
  }
}
