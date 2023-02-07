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
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.planner.sql.handlers.AbstractSqlHandler;
import org.apache.drill.exec.planner.sql.handlers.RefreshMetadataHandler;
import org.apache.drill.exec.planner.sql.handlers.SqlHandlerConfig;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Sql parse tree node to represent statement:
 * REFRESH TABLE METADATA tblname
 */
public class SqlRefreshMetadata extends DrillSqlCall {
  public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator("REFRESH_TABLE_METADATA", SqlKind.OTHER_DDL) {
    @Override
    public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
      return new SqlRefreshMetadata(pos, (SqlIdentifier) operands[0], (SqlLiteral) operands[1], (SqlNodeList) operands[2]);
    }
  };

  private SqlIdentifier tblName;
  private final SqlLiteral allColumns;
  private final SqlNodeList fieldList;

  public SqlRefreshMetadata(SqlParserPos pos, SqlIdentifier tblName, SqlLiteral allColumns, SqlNodeList fieldList){
    super(pos);
    this.tblName = tblName;
    this.allColumns = allColumns;
    this.fieldList = fieldList;
  }

  @Override
  public SqlOperator getOperator() {
    return OPERATOR;
  }

  @Override
  public List<SqlNode> getOperandList() {
    List<SqlNode> ops = Lists.newArrayList();
    ops.add(tblName);
    ops.add(allColumns);
    ops.add(fieldList);
    return ops;
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    writer.keyword("REFRESH");
    writer.keyword("TABLE");
    writer.keyword("METADATA");
    if (!allColumns.booleanValue()) {
      writer.keyword("COLUMNS");
      if (fieldList == null) {
        writer.keyword("NONE");
      } else if (fieldList.size() > 0) {
        writer.keyword("(");
        fieldList.get(0).unparse(writer, leftPrec, rightPrec);
        for (int i = 1; i < fieldList.size(); i++) {
          writer.keyword(",");
          fieldList.get(i).unparse(writer, leftPrec, rightPrec);
        }
        writer.keyword(")");
      }
    }
    tblName.unparse(writer, leftPrec, rightPrec);
  }

  public String getName() {
    if (tblName.isSimple()) {
      return tblName.getSimple();
    }

    return tblName.names.get(tblName.names.size() - 1);
  }

  public List<String> getSchemaPath() {
    return SchemaUtilites.getSchemaPath(tblName);
  }

  @Override
  public AbstractSqlHandler getSqlHandler(SqlHandlerConfig config) {
    return new RefreshMetadataHandler(config);
  }

  public SqlNodeList getFieldList() {
    return fieldList;
  }

  public SqlLiteral getAllColumns() {
    return allColumns;
  }
}
