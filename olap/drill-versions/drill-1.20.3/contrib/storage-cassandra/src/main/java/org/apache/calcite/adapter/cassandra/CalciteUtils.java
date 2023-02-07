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
package org.apache.calcite.adapter.cassandra;

import com.datastax.driver.core.Session;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.exec.store.cassandra.CassandraStorageConfig;
import org.apache.drill.exec.store.cassandra.plan.CassandraEnumerablePrelContext;
import org.apache.drill.exec.store.cassandra.plan.DrillCassandraLimitRule;
import org.apache.drill.exec.store.cassandra.schema.CassandraDrillSchema;
import org.apache.drill.exec.store.enumerable.plan.EnumerableIntermediatePrelConverterRule;
import org.apache.drill.exec.store.enumerable.plan.VertexDrelConverterRule;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class CalciteUtils {
  private static final VertexDrelConverterRule VERTEX_DREL_CONVERTER_RULE =
      new VertexDrelConverterRule(CassandraRel.CONVENTION);

  private static final RelOptRule ENUMERABLE_INTERMEDIATE_PREL_CONVERTER_RULE =
      new EnumerableIntermediatePrelConverterRule(
          new CassandraEnumerablePrelContext(CassandraStorageConfig.NAME), CassandraRel.CONVENTION);

  public static CassandraTableScan tableScanCreator(RelOptCluster cluster, RelTraitSet traitSet,
      RelOptTable table, CassandraTable cassandraTable,
      RelDataType projectRowType) {
    return new CassandraTableScan(cluster, traitSet, table, cassandraTable, projectRowType);
  }

  public static Set<RelOptRule> cassandraRules() {
    Set<RelOptRule> rules = Arrays.stream(CassandraRules.RULES)
        .collect(Collectors.toSet());
    rules.add(DrillCassandraLimitRule.INSTANCE);
    rules.add(ENUMERABLE_INTERMEDIATE_PREL_CONVERTER_RULE);
    rules.add(VERTEX_DREL_CONVERTER_RULE);
    return rules;
  }

  public static Session getSession(SchemaPlus schema) {
    return schema.unwrap(CassandraDrillSchema.class).getDelegatingSchema().session;
  }
}
