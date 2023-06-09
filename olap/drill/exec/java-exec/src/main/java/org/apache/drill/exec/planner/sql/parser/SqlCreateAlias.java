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
import org.apache.drill.exec.planner.sql.handlers.CreateAliasHandler;
import org.apache.drill.exec.planner.sql.handlers.SqlHandlerConfig;

import java.util.ArrayList;
import java.util.List;

public class SqlCreateAlias extends DrillSqlCall {

  private final SqlIdentifier alias;
  private final SqlIdentifier source;
  private final SqlNode aliasKind;
  private final SqlNode replace;
  private final SqlNode isPublic;
  private final SqlNode user;

  public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator("CREATE_ALIAS", SqlKind.OTHER_DDL) {
    @Override
    public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
      return SqlCreateAlias.builder()
        .pos(pos)
        .alias((SqlIdentifier) operands[0])
        .source((SqlIdentifier) operands[1])
        .aliasKind(operands[2])
        .replace(operands[3])
        .isPublic(operands[4])
        .user(operands[5])
        .build();
    }
  };

  private SqlCreateAlias(SqlCreateAliasBuilder builder) {
    super(builder.pos);
    this.alias = builder.alias;
    this.source = builder.source;
    this.replace = builder.replace;
    this.isPublic = builder.isPublic;
    this.aliasKind = builder.aliasKind;
    this.user = builder.user;
  }

  public static SqlCreateAliasBuilder builder() {
    return new SqlCreateAliasBuilder();
  }

  @Override
  public SqlOperator getOperator() {
    return OPERATOR;
  }

  @Override
  public List<SqlNode> getOperandList() {
    List<SqlNode> opList = new ArrayList<>();
    opList.add(alias);
    opList.add(source);
    opList.add(aliasKind);
    opList.add(replace);
    opList.add(isPublic);
    opList.add(user);
    return opList;
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    writer.keyword("CREATE");
    if (((SqlLiteral) replace).booleanValue()) {
      writer.keyword("OR");
      writer.keyword("REPLACE");
    }
    if (((SqlLiteral) isPublic).booleanValue()) {
      writer.keyword("PUBLIC");
    }
    writer.keyword("ALIAS");
    alias.unparse(writer, leftPrec, rightPrec);

    writer.keyword("FOR");
    writer.keyword(((SqlLiteral) aliasKind).toValue());
    source.unparse(writer, leftPrec, rightPrec);

    if (user != null) {
      writer.keyword("AS");
      writer.keyword("USER");
      user.unparse(writer, leftPrec, rightPrec);
    }
  }

  @Override
  public AbstractSqlHandler getSqlHandler(SqlHandlerConfig config) {
    return new CreateAliasHandler(config);
  }

  public SqlIdentifier getAlias() {
    return this.alias;
  }

  public SqlIdentifier getSource() {
    return this.source;
  }

  public SqlNode getAliasKind() {
    return this.aliasKind;
  }

  public SqlNode getReplace() {
    return this.replace;
  }

  public SqlNode getIsPublic() {
    return this.isPublic;
  }

  public SqlNode getUser() {
    return this.user;
  }

  public static class SqlCreateAliasBuilder {
    private SqlParserPos pos;

    private SqlIdentifier alias;

    private SqlIdentifier source;

    private SqlNode replace;

    private SqlNode isPublic;

    private SqlNode aliasKind;

    private SqlNode user;

    public SqlCreateAliasBuilder pos(SqlParserPos pos) {
      this.pos = pos;
      return this;
    }

    public SqlCreateAliasBuilder alias(SqlIdentifier alias) {
      this.alias = alias;
      return this;
    }

    public SqlCreateAliasBuilder source(SqlIdentifier source) {
      this.source = source;
      return this;
    }

    public SqlCreateAliasBuilder replace(SqlNode replace) {
      this.replace = replace;
      return this;
    }

    public SqlCreateAliasBuilder isPublic(SqlNode isPublic) {
      this.isPublic = isPublic;
      return this;
    }

    public SqlCreateAliasBuilder aliasKind(SqlNode aliasKind) {
      this.aliasKind = aliasKind;
      return this;
    }

    public SqlCreateAliasBuilder user(SqlNode user) {
      this.user = user;
      return this;
    }

    public SqlCreateAlias build() {
      return new SqlCreateAlias(this);
    }
  }
}
