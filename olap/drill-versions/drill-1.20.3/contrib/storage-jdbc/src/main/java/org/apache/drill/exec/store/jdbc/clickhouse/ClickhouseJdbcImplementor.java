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
package org.apache.drill.exec.store.jdbc.clickhouse;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.adapter.jdbc.JdbcImplementor;
import org.apache.calcite.adapter.jdbc.JdbcTableScan;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIdentifier;

import java.util.Iterator;

public class ClickhouseJdbcImplementor extends JdbcImplementor {
  public ClickhouseJdbcImplementor(SqlDialect dialect,
                                   JavaTypeFactory typeFactory) {
    super(dialect, typeFactory);
  }

  @Override
  public Result visit(JdbcTableScan scan) {
    SqlIdentifier sqlIdentifier = scan.jdbcTable.tableName();
    Iterator<String> iter = sqlIdentifier.names.iterator();
    Preconditions.checkArgument(sqlIdentifier.names.size() == 3,
      "size of clickhouse table names:[%s] is not 3", sqlIdentifier.toString());
    iter.next();
    sqlIdentifier.setNames(ImmutableList.copyOf(iter), null);
    return result(sqlIdentifier, ImmutableList.of(Clause.FROM), scan, null);
  }
}
