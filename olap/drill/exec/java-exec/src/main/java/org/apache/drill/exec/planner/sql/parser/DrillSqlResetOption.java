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

import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSetOption;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.util.ImmutableNullableList;
import org.apache.drill.exec.planner.sql.handlers.ResetOptionHandler;

/**
 * Sql parse tree node to represent statement: {@code RESET { <NAME> | ALL } }.
 * Statement handled in: {@link ResetOptionHandler}
 */
public final class DrillSqlResetOption extends SqlSetOption {

  public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator("RESET_OPTION", SqlKind.SET_OPTION) {
    @Override
    public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
      SqlNode scopeNode = operands[0];
      return new DrillSqlResetOption(pos, scopeNode == null ? null : scopeNode.toString(), (SqlIdentifier) operands[1]);
    }
  };

  public DrillSqlResetOption(SqlParserPos pos, String scope, SqlIdentifier name) {
    super(pos, scope, name, null);
  }

  @Override
  public SqlKind getKind() {
    return SqlKind.SET_OPTION;
  }

  @Override
  public SqlOperator getOperator() {
    return OPERATOR;
  }

  @Override
  public List<SqlNode> getOperandList() {
    List<SqlNode> operandList = new ArrayList<>();

    SqlIdentifier scopeIdentifier = (this.getScope() == null) ? null : new SqlIdentifier(this.getScope(),
      SqlParserPos.ZERO);

    operandList.add(scopeIdentifier);
    operandList.add(this.getName());
    return ImmutableNullableList.copyOf(operandList);
  }

  @Override
  public void setOperand(int i, SqlNode operand) {
    switch (i) {
      case 0:
        if (operand != null) {
          this.setScope(((SqlIdentifier) operand).getSimple());
        } else {
          this.setScope(null);
        }
        break;
      case 1:
        this.setName((SqlIdentifier) operand);
        break;
      default:
        throw new AssertionError(i);
    }
  }

  @Override
  protected void unparseAlterOperation(SqlWriter writer, int leftPrec, int rightPrec) {
    writer.keyword("RESET");

    SqlWriter.Frame frame = writer.startList(SqlWriter.FrameTypeEnum.SIMPLE);

    this.getName().unparse(writer, leftPrec, rightPrec);
    writer.endList(frame);
  }
}
