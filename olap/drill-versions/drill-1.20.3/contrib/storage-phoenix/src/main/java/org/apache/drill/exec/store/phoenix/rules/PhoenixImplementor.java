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
package org.apache.drill.exec.store.phoenix.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.adapter.jdbc.JdbcImplementor;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.core.CorrelationId;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;

public class PhoenixImplementor extends JdbcImplementor {

  public PhoenixImplementor(SqlDialect dialect, JavaTypeFactory typeFactory) {
    super(dialect, typeFactory);
  }

  @Override
  public Result result(SqlNode node, Collection<Clause> clauses, RelNode rel, Map<String, RelDataType> aliases) {
    if (node instanceof SqlIdentifier) {
      SqlIdentifier identifier = (SqlIdentifier) node;
      String name = identifier.names.get(identifier.names.size() -1);
      if (!aliasSet.contains(name)) {
        /*
         * phoenix does not support the 'SELECT `table_name`.`field_name`',
         * need to force the alias name and start from `table_name0`,
         * the result is that 'SELECT `table_name0`.`field_name`'.
         */
        aliasSet.add(name);
      }
    }
    return super.result(node, clauses, rel, aliases);
  }

  @Override
  public Result visit(Project e) {
    return super.visit(e);
  }

  @Override
  public Result visit(Filter e) {
    final RelNode input = e.getInput();
    Result x = visitChild(0, input);
    parseCorrelTable(e, x);
    if (input instanceof Aggregate) {
      return super.visit(e);
    } else {
      final Builder builder = x.builder(e, Clause.WHERE);
      builder.setWhere(builder.context.toSql(null, e.getCondition()));
      final List<SqlNode> selectList = new ArrayList<>();
      e.getRowType().getFieldNames().forEach(fieldName -> {
        /*
         * phoenix does not support the wildcard in the subqueries,
         * expand the wildcard at this time.
         */
        addSelect(selectList, new SqlIdentifier(fieldName, POS), e.getRowType());
      });
      builder.setSelect(new SqlNodeList(selectList, POS));
      return builder.result();
    }
  }

  @Override
  public Result visit(Join e) {
    return super.visit(e);
  }

  private void parseCorrelTable(RelNode relNode, Result x) {
    for (CorrelationId id : relNode.getVariablesSet()) {
      correlTableMap.put(id, x.qualifiedContext());
    }
  }
}
