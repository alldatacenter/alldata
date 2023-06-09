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
package org.apache.drill.exec.store.elasticsearch.schema;

import org.apache.calcite.adapter.elasticsearch.DrillElasticsearchTableScan;
import org.apache.calcite.adapter.elasticsearch.ElasticsearchRel;
import org.apache.calcite.adapter.elasticsearch.ElasticsearchTable;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.linq4j.Queryable;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.schema.QueryableTable;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.TranslatableTable;
import org.apache.calcite.schema.Wrapper;
import org.apache.drill.exec.planner.logical.DrillTableSelection;
import org.apache.drill.exec.planner.logical.DynamicDrillTable;
import org.apache.drill.exec.store.StoragePlugin;

import java.lang.reflect.Type;

public class ElasticsearchDynamicTable extends DynamicDrillTable implements TranslatableTable, QueryableTable, Wrapper {

  private final ElasticsearchTable table;

  public ElasticsearchDynamicTable(StoragePlugin plugin, String storageEngineName, DrillTableSelection selection, Table table) {
    super(plugin, storageEngineName, selection);
    this.table = (ElasticsearchTable) table;
  }

  @Override
  public RelNode toRel(RelOptTable.ToRelContext context, RelOptTable relOptTable) {
    RelOptCluster cluster = context.getCluster();
    return new DrillElasticsearchTableScan(cluster,
        cluster.traitSetOf(ElasticsearchRel.CONVENTION), relOptTable, table, relOptTable.getRowType());
  }

  @Override
  public <T> Queryable<T> asQueryable(QueryProvider queryProvider, SchemaPlus schema, String tableName) {
    return table.asQueryable(queryProvider, schema, tableName);
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
}
