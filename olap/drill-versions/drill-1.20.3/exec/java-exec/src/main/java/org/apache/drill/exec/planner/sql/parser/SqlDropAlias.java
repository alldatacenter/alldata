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
import org.apache.drill.exec.planner.sql.handlers.AbstractSqlHandler;
import org.apache.drill.exec.planner.sql.handlers.DropAliasHandler;
import org.apache.drill.exec.planner.sql.handlers.SqlHandlerConfig;

import java.util.ArrayList;
import java.util.List;

public class SqlDropAlias extends DrillSqlCall {

  private final SqlNode isPublic;
  private final SqlNode ifExists;
  private final SqlIdentifier alias;
  private final SqlNode aliasKind;
  private final SqlNode user;

  public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator("DROP_ALIAS", SqlKind.OTHER_DDL) {
    @Override
    public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
      return SqlDropAlias.builder()
        .pos(pos)
        .alias((SqlIdentifier) operands[0])
        .aliasKind(operands[1])
        .ifExists(operands[2])
        .isPublic(operands[3])
        .user(operands[4])
        .build();
    }
  };

  private SqlDropAlias(SqlDropAliasBuilder builder) {
    super(builder.pos);
    this.alias = builder.alias;
    this.ifExists = builder.ifExists;
    this.isPublic = builder.isPublic;
    this.aliasKind = builder.aliasKind;
    this.user = builder.user;
  }

  public static SqlDropAliasBuilder builder() {
    return new SqlDropAliasBuilder();
  }

  @Override
  public SqlOperator getOperator() {
    return OPERATOR;
  }

  @Override
  public List<SqlNode> getOperandList() {
    List<SqlNode> opList = new ArrayList<>();
    opList.add(alias);
    opList.add(aliasKind);
    opList.add(ifExists);
    opList.add(isPublic);
    opList.add(user);
    return opList;
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    writer.keyword("DROP");
    if (((SqlLiteral) isPublic).booleanValue()) {
      writer.keyword("PUBLIC");
    }
    writer.keyword("ALIAS");
    if (((SqlLiteral) ifExists).booleanValue()) {
      writer.keyword("IF");
      writer.keyword("EXISTS");
    }
    alias.unparse(writer, leftPrec, rightPrec);

    writer.keyword("FOR");
    writer.keyword(((SqlLiteral) aliasKind).toValue());

    if (user != null) {
      writer.keyword("AS");
      writer.keyword("USER");
      user.unparse(writer, leftPrec, rightPrec);
    }
  }

  @Override
  public AbstractSqlHandler getSqlHandler(SqlHandlerConfig config) {
    return new DropAliasHandler(config);
  }

  public SqlNode getIsPublic() {
    return this.isPublic;
  }

  public SqlNode getIfExists() {
    return this.ifExists;
  }

  public SqlIdentifier getAlias() {
    return this.alias;
  }

  public SqlNode getAliasKind() {
    return this.aliasKind;
  }

  public SqlNode getUser() {
    return this.user;
  }

  public static class SqlDropAliasBuilder {
    private SqlParserPos pos;

    private SqlIdentifier alias;

    private SqlNode ifExists;

    private SqlNode isPublic;

    private SqlNode aliasKind;

    private SqlNode user;

    public SqlDropAliasBuilder pos(SqlParserPos pos) {
      this.pos = pos;
      return this;
    }

    public SqlDropAliasBuilder alias(SqlIdentifier alias) {
      this.alias = alias;
      return this;
    }

    public SqlDropAliasBuilder ifExists(SqlNode ifExists) {
      this.ifExists = ifExists;
      return this;
    }

    public SqlDropAliasBuilder isPublic(SqlNode isPublic) {
      this.isPublic = isPublic;
      return this;
    }

    public SqlDropAliasBuilder aliasKind(SqlNode aliasKind) {
      this.aliasKind = aliasKind;
      return this;
    }

    public SqlDropAliasBuilder user(SqlNode user) {
      this.user = user;
      return this;
    }

    public SqlDropAlias build() {
      return new SqlDropAlias(this);
    }
  }
}
