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
package org.apache.drill.exec.store.cassandra.plan;

import org.apache.calcite.adapter.cassandra.CassandraToEnumerableConverterRule;
import org.apache.calcite.adapter.enumerable.EnumerableRel;
import org.apache.calcite.adapter.enumerable.EnumerableRelImplementor;
import org.apache.calcite.linq4j.tree.ClassDeclaration;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.store.SubsetRemover;
import org.apache.drill.exec.store.cassandra.CassandraColumnConverterFactoryProvider;
import org.apache.drill.exec.store.enumerable.ColumnConverterFactoryProvider;
import org.apache.drill.exec.store.enumerable.plan.EnumerablePrelContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CassandraEnumerablePrelContext implements EnumerablePrelContext {
  private final String planPrefix;

  public CassandraEnumerablePrelContext(String planPrefix) {
    this.planPrefix = planPrefix;
  }

  @Override
  public String generateCode(RelOptCluster cluster, RelNode relNode) {
    RelNode enumerableRel =
        CassandraToEnumerableConverterRule.INSTANCE.convert(relNode);

    ClassDeclaration classDeclaration = new EnumerableRelImplementor(cluster.getRexBuilder(), Collections.emptyMap())
        .implementRoot((EnumerableRel) enumerableRel, EnumerableRel.Prefer.ARRAY);
    return Expressions.toString(Collections.singletonList(classDeclaration), "\n", false);
  }

  @Override
  public RelNode transformNode(RelNode input) {
    return input.accept(SubsetRemover.INSTANCE);
  }

  @Override
  public Map<String, Integer> getFieldsMap(RelNode transformedNode) {
    return transformedNode.getRowType().getFieldList().stream()
        .collect(Collectors.toMap(
            RelDataTypeField::getName,
            RelDataTypeField::getIndex
        ));
  }

  @Override
  public String getPlanPrefix() {
    return planPrefix;
  }

  @Override
  public String getTablePath(RelNode input) {
    TableScan scan = Objects.requireNonNull(DrillRelOptUtil.findScan(input));
    List<String> qualifiedName = scan.getTable().getQualifiedName();
    return String.join(".", qualifiedName.subList(0, qualifiedName.size() - 1));
  }

  @Override
  public ColumnConverterFactoryProvider factoryProvider() {
    return CassandraColumnConverterFactoryProvider.INSTANCE;
  }
}
