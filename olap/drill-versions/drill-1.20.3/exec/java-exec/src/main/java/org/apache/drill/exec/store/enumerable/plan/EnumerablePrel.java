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
package org.apache.drill.exec.store.enumerable.plan;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.AbstractRelNode;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.physical.LeafPrel;
import org.apache.drill.exec.planner.physical.PhysicalPlanCreator;
import org.apache.drill.exec.planner.physical.visitor.PrelVisitor;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.schema.SchemaProvider;
import org.apache.drill.exec.store.enumerable.ColumnConverterFactoryProvider;
import org.apache.drill.exec.store.enumerable.EnumerableGroupScan;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * {@link LeafPrel} implementation that generates java code that may be executed to obtain results
 * for the provided plan part.
 */
public class EnumerablePrel extends AbstractRelNode implements LeafPrel {
  private final String code;
  private final String schemaPath;
  private final String plan;
  private final double rows;
  private final Map<String, Integer> fieldsMap;
  private final TupleMetadata schema;
  private final String planPrefix;
  private final ColumnConverterFactoryProvider factoryProvider;

  public EnumerablePrel(RelOptCluster cluster, RelTraitSet traitSet, RelNode input, EnumerablePrelContext context) {
    super(cluster, traitSet);
    this.rowType = input.getRowType();
    this.rows = input.estimateRowCount(cluster.getMetadataQuery());

    this.planPrefix = context.getPlanPrefix();
    RelNode transformedNode = context.transformNode(input);

    this.fieldsMap = context.getFieldsMap(transformedNode);

    this.plan = RelOptUtil.toString(transformedNode);
    this.code = context.generateCode(cluster, transformedNode);

    this.schemaPath = context.getTablePath(input);
    try {
      TableScan scan = Objects.requireNonNull(DrillRelOptUtil.findScan(input));
      SchemaProvider schemaProvider = DrillRelOptUtil.getDrillTable(scan)
          .getMetadataProviderManager().getSchemaProvider();
      this.schema = schemaProvider != null ? schemaProvider.read().getSchema() : null;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    factoryProvider = context.factoryProvider();
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) {
    List<SchemaPath> columns = rowType.getFieldNames().stream()
        .map(SchemaPath::getSimplePath)
        .collect(Collectors.toList());
    GroupScan groupScan =
      new EnumerableGroupScan(code, columns, fieldsMap, rows, schema, schemaPath, factoryProvider);
    return creator.addMetadata(this, groupScan);
  }

  @Override
  public RelWriter explainTerms(RelWriter pw) {
    pw.item(planPrefix, plan);
    return super.explainTerms(pw);
  }

  @Override
  public double estimateRowCount(RelMetadataQuery mq) {
    return rows;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PrelVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitLeaf(this, value);
  }

  @Override
  public BatchSchema.SelectionVectorMode[] getSupportedEncodings() {
    return BatchSchema.SelectionVectorMode.DEFAULT;
  }

  @Override
  public BatchSchema.SelectionVectorMode getEncoding() {
    return BatchSchema.SelectionVectorMode.NONE;
  }
}
