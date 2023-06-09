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

import java.util.Collections;
import java.util.List;

import org.apache.drill.shaded.guava.com.google.common.base.Function;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class DrillCompoundIdentifier extends SqlIdentifier {

  private static final Function<String, String> STAR_TO_EMPTY = new Function<String, String>() {
    public String apply(String s) {
      return s.equals("*") ? "" : s;
    }
  };

  private final List<IdentifierHolder> ids;

  private static List<String> getNames(List<IdentifierHolder> identifiers) {
    List<String> names = Lists.newArrayListWithCapacity(identifiers.size());
    for (IdentifierHolder h : identifiers) {
      names.add(h.value);
    }
    return names;
  }

  public DrillCompoundIdentifier(List<IdentifierHolder> identifiers) {
    super(getNames(identifiers), identifiers.get(0).parserPos);
    this.ids = identifiers;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private List<IdentifierHolder> identifiers = Lists.newArrayList();

    public DrillCompoundIdentifier build() {
      return new DrillCompoundIdentifier(identifiers);
    }

    public void addString(String name, SqlParserPos pos) {
      identifiers.add(new IdentifierHolder(name, pos, false));
    }

    public void addIndex(int index, SqlParserPos pos) {
      identifiers.add(new IdentifierHolder(Integer.toString(index), pos, true));
    }
  }

  public SqlNode getAsSqlNode(boolean allowNoTableRefCompoundIdentifier) {
    if (ids.size() == 1) {
      return new SqlIdentifier(Collections.singletonList(ids.get(0).value), ids.get(0).parserPos);
    }

    int startIndex;
    SqlNode node;

    if (ids.get(1).isArray()) {
      // handle everything post zero index as item operator.
      startIndex = 1;
      node = new SqlIdentifier(
          ImmutableList.of(ids.get(0).value),
          null,
          ids.get(0).parserPos,
          ImmutableList.of(ids.get(0).parserPos));
    } else {
      if (allowNoTableRefCompoundIdentifier) {
        // For certain statements e.g. ANALYZE which only reference one table, compound column names may be referenced
        // without the table reference. For such cases, handle everything post one index as item operator.
        startIndex = 1;
        node = new SqlIdentifier(
            // Replaces star by empty string. See SqlIdentifier#isStar()
            ImmutableList.of(STAR_TO_EMPTY.apply(ids.get(0).value)), null,
            ids.get(0).parserPos,
            ImmutableList.of(ids.get(0).parserPos));
      } else {
        // handle everything post two index as item operator.
        startIndex = 2;
        node = new SqlIdentifier(
            // Replaces star by empty string. See SqlIdentifier#isStar()
            ImmutableList.of(ids.get(0).value, STAR_TO_EMPTY.apply(ids.get(1).value)), null,
            ids.get(0).parserPos,
            ImmutableList.of(ids.get(0).parserPos, ids.get(1).parserPos));
      }
    }
    for (int i = startIndex; i < ids.size(); i++) {
      node = ids.get(i).getNode(node);
    }

    return node;
  }

  public SqlNode getAsCompoundIdentifier() {
    List<String> names = Lists.newArrayListWithCapacity(ids.size());
    List<SqlParserPos> pos = Lists.newArrayListWithCapacity(ids.size());
    for (IdentifierHolder holder : ids) {
      names.add(holder.value);
      pos.add(holder.parserPos);
    }
    return new SqlIdentifier(names, null, pos.get(0), pos);
  }

  private static class IdentifierHolder {
    String value;
    SqlParserPos parserPos;
    boolean isArray;

    public IdentifierHolder(String value, SqlParserPos parserPos, boolean isArray) {
      super();
      this.isArray = isArray;
      this.value = value;
      this.parserPos = parserPos;
    }

    public boolean isArray() {
      return isArray;
    }

    public SqlNode getNode(SqlNode node) {
      SqlLiteral literal;
      if (isArray) {
        literal = SqlLiteral.createExactNumeric(value, parserPos);
      } else {
        literal = SqlLiteral.createCharString(value, parserPos);
      }
      return new SqlBasicCall(SqlStdOperatorTable.ITEM, new SqlNode[]{node, literal}, parserPos);
    }

  }
}
