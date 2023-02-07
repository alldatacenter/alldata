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

import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.exec.planner.sql.handlers.AbstractSqlHandler;
import org.apache.drill.exec.planner.sql.handlers.SqlHandlerConfig;
import org.apache.drill.exec.planner.sql.handlers.SqlHandlerUtil;
import org.apache.drill.exec.planner.sql.handlers.ViewHandler;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlWriter;

import java.util.List;

public class SqlCreateView extends DrillSqlCall {
  public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator("CREATE_VIEW", SqlKind.CREATE_VIEW) {
    @Override
    public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
      return new SqlCreateView(pos, (SqlIdentifier) operands[0], (SqlNodeList) operands[1], operands[2], (SqlLiteral) operands[3]);
    }
  };

  private SqlIdentifier viewName;
  private SqlNodeList fieldList;
  private SqlNode query;
  private SqlLiteral createType;

  public SqlCreateView(SqlParserPos pos, SqlIdentifier viewName, SqlNodeList fieldList,
                       SqlNode query, SqlLiteral createType) {
    super(pos);
    this.viewName = viewName;
    this.query = query;
    this.fieldList = fieldList;
    this.createType = createType;
  }

  @Override
  public SqlOperator getOperator() {
    return OPERATOR;
  }

  @Override
  public List<SqlNode> getOperandList() {
    List<SqlNode> ops = Lists.newArrayList();
    ops.add(viewName);
    ops.add(fieldList);
    ops.add(query);
    ops.add(createType);
    return ops;
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    writer.keyword("CREATE");
    switch (SqlCreateType.valueOf(createType.toValue())) {
      case SIMPLE:
        writer.keyword("VIEW");
        break;
      case OR_REPLACE:
        writer.keyword("OR");
        writer.keyword("REPLACE");
        writer.keyword("VIEW");
        break;
      case IF_NOT_EXISTS:
        writer.keyword("VIEW");
        writer.keyword("IF");
        writer.keyword("NOT");
        writer.keyword("EXISTS");
        break;
    }
    viewName.unparse(writer, leftPrec, rightPrec);
    if (fieldList.size() > 0) {
      SqlHandlerUtil.unparseSqlNodeList(writer, leftPrec, rightPrec, fieldList);
    }
    writer.keyword("AS");
    query.unparse(writer, leftPrec, rightPrec);
  }

  @Override
  public AbstractSqlHandler getSqlHandler(SqlHandlerConfig config) {
    return new ViewHandler.CreateView(config);
  }

  public List<String> getSchemaPath() {
    return SchemaUtilites.getSchemaPath(viewName);
  }

  public String getName() {
    if (viewName.isSimple()) {
      return viewName.getSimple();
    }

    return viewName.names.get(viewName.names.size() - 1);
  }

  public List<String> getFieldNames() {
    List<String> fieldNames = Lists.newArrayList();
    for (SqlNode node : fieldList.getList()) {
      fieldNames.add(node.toString());
    }
    return fieldNames;
  }

  public SqlNode getQuery() { return query; }

  public SqlCreateType getSqlCreateType() { return SqlCreateType.valueOf(createType.toValue()); }

}
