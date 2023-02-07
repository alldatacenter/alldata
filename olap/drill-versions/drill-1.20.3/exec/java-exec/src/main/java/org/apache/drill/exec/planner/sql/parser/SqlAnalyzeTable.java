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
import org.apache.drill.exec.planner.sql.handlers.AbstractSqlHandler;
import org.apache.drill.exec.planner.sql.handlers.AnalyzeTableHandler;
import org.apache.drill.exec.planner.sql.handlers.SqlHandlerConfig;
import org.apache.drill.exec.util.Pointer;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * SQL tree for ANALYZE statement.
 */
public class SqlAnalyzeTable extends DrillSqlCall {
  public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator("ANALYZE_TABLE", SqlKind.OTHER_DDL) {
    public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
      Preconditions.checkArgument(operands.length == 4, "SqlAnalyzeTable.createCall() has to get 4 operands!");
      return new SqlAnalyzeTable(pos, operands[0], (SqlLiteral) operands[1],
          (SqlNodeList) operands[2], (SqlNumericLiteral) operands[3]
      );
    }
  };

  private final SqlNode tableRef;
  private final SqlLiteral estimate;
  private final SqlNodeList fieldList;
  private final SqlNumericLiteral samplePercent;

  public SqlAnalyzeTable(SqlParserPos pos, SqlNode tableRef, SqlLiteral estimate,
      SqlNodeList fieldList, SqlNumericLiteral samplePercent) {
    super(pos);
    this.tableRef = tableRef;
    this.estimate = estimate;
    this.fieldList = fieldList;
    this.samplePercent = samplePercent;
  }

  @Override
  public SqlOperator getOperator() {
    return OPERATOR;
  }

  @Override
  public List<SqlNode> getOperandList() {
    final List<SqlNode> operands = Lists.newArrayListWithCapacity(4);
    operands.add(tableRef);
    operands.add(estimate);
    operands.add(fieldList);
    operands.add(samplePercent);
    return operands;
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    writer.keyword("ANALYZE");
    writer.keyword("TABLE");
    tableRef.unparse(writer, leftPrec, rightPrec);
    writer.keyword(estimate.booleanValue() ? "ESTIMATE" : "COMPUTE");
    writer.keyword("STATISTICS");

    if (fieldList != null && fieldList.size() > 0) {
      writer.keyword("(");
      fieldList.get(0).unparse(writer, leftPrec, rightPrec);
      for (int i = 1; i < fieldList.size(); i++) {
        writer.keyword(",");
        fieldList.get(i).unparse(writer, leftPrec, rightPrec);
      }
      writer.keyword(")");
    }
    writer.keyword("SAMPLE");
    samplePercent.unparse(writer, leftPrec, rightPrec);
    writer.keyword("PERCENT");
  }

  @Override
  public AbstractSqlHandler getSqlHandler(SqlHandlerConfig config, Pointer<String> textPlan) {
    return new AnalyzeTableHandler(config, textPlan);
  }

  @Override
  public AbstractSqlHandler getSqlHandler(SqlHandlerConfig config) {
    return getSqlHandler(config, null);
  }

  public SqlNode getTableRef() {
    return tableRef;
  }

  public List<String> getFieldNames() {
    if (fieldList == null) {
      return ImmutableList.of();
    }

    List<String> columnNames = Lists.newArrayList();
    for (SqlNode node : fieldList.getList()) {
      columnNames.add(node.toString());
    }
    return columnNames;
  }

  public SqlNodeList getFieldList() {
    return fieldList;
  }

  public boolean getEstimate() {
    return estimate.booleanValue();
  }

  public int getSamplePercent() {
    return samplePercent.intValue(true);
  }
}
