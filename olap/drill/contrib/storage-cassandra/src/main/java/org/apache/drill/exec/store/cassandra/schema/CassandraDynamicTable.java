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
package org.apache.drill.exec.store.cassandra.schema;

import org.apache.calcite.adapter.cassandra.CassandraRel;
import org.apache.calcite.adapter.cassandra.CassandraTable;
import org.apache.calcite.adapter.cassandra.CalciteUtils;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.linq4j.Queryable;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.QueryableTable;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.TranslatableTable;
import org.apache.calcite.schema.Wrapper;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.logical.DrillTableSelection;
import org.apache.drill.exec.store.StoragePlugin;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class CassandraDynamicTable extends DrillTable implements TranslatableTable, QueryableTable, Wrapper {

  private final CassandraTable table;

  public CassandraDynamicTable(StoragePlugin plugin, String storageEngineName, DrillTableSelection selection, CassandraTable table) {
    super(storageEngineName, plugin, selection);
    this.table = table;
  }

  @Override
  public RelNode toRel(RelOptTable.ToRelContext context, RelOptTable relOptTable) {
    RelOptCluster cluster = context.getCluster();
    return CalciteUtils.tableScanCreator(cluster, cluster.traitSetOf(CassandraRel.CONVENTION),
        relOptTable, table, relOptTable.getRowType());
  }

  @Override
  public <V> Queryable<V> asQueryable(QueryProvider queryProvider, SchemaPlus schema, String tableName) {
    CassandraTable cassandraTable = CassandraDynamicTable.this.table;
    return new CassandraTable.CassandraQueryable<V>(queryProvider, schema, cassandraTable, tableName) {
      public Enumerable<Object> query(List<Map.Entry<String, Class>> fields,
          List<Map.Entry<String, String>> selectFields, List<String> predicates,
          List<String> order, Integer offset, Integer fetch) {
        return cassandraTable.query(CalciteUtils.getSession(schema), fields, selectFields, predicates,
            order, offset, fetch);
      }
    };
  }

  @Override
  public Type getElementType() {
    return table.getElementType();
  }

  public <C> C unwrap(Class<C> aClass) {
    if (aClass.isInstance(this)) {
      return aClass.cast(this);
    } else if (aClass.isInstance(table)) {
      return aClass.cast(table);
    }
    return null;
  }

  @Override
  public Expression getExpression(SchemaPlus schema, String tableName, Class clazz) {
    return table.getExpression(schema, tableName, clazz);
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    return table.getRowType(typeFactory);
  }
}
