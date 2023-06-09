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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.adapter.jdbc.JdbcImplementor;
import org.apache.calcite.plan.ConventionTraitDef;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.AbstractRelNode;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.sql.SqlDialect;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.physical.PhysicalPlanCreator;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.visitor.PrelVisitor;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.store.SubsetRemover;
import org.apache.drill.exec.store.phoenix.PhoenixGroupScan;
import org.apache.drill.exec.store.phoenix.PhoenixScanSpec;

public class PhoenixPrel extends AbstractRelNode implements Prel {

  private final String sql;
  private final double rows;
  private final PhoenixConvention convention;

  public PhoenixPrel(RelOptCluster cluster, RelTraitSet traitSet, PhoenixIntermediatePrel prel) {
    super(cluster, traitSet);
    final RelNode input = prel.getInput();
    rows = input.estimateRowCount(cluster.getMetadataQuery());
    convention = (PhoenixConvention) input.getTraitSet().getTrait(ConventionTraitDef.INSTANCE);
    final SqlDialect dialect = convention.getPlugin().getDialect();
    final JdbcImplementor jdbcImplementor = new PhoenixImplementor(dialect, (JavaTypeFactory) getCluster().getTypeFactory());
    final JdbcImplementor.Result result = jdbcImplementor.visitChild(0, input.accept(SubsetRemover.INSTANCE));
    sql = result.asStatement().toSqlString(dialect).getSql();
    rowType = input.getRowType();
  }

  @Override
  public Iterator<Prel> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) {
    List<SchemaPath> schemaPaths = new ArrayList<>(rowType.getFieldCount());
    List<String> columns = new ArrayList<>(rowType.getFieldCount());
    for (String col : rowType.getFieldNames()) {
      schemaPaths.add(SchemaPath.getSimplePath(col));
      columns.add(SchemaPath.getSimplePath(col).rootName());
    }
    PhoenixGroupScan output = new PhoenixGroupScan(creator.getContext().getQueryUserName(), sql, schemaPaths,
      new PhoenixScanSpec(sql, columns, rows), convention.getPlugin());
    return creator.addMetadata(this, output);
  }

  @Override
  public RelWriter explainTerms(RelWriter pw) {
    return super.explainTerms(pw).item("sql", stripToOneLineSql(sql));
  }

  private String stripToOneLineSql(String sql) {
    StringBuilder sbt = new StringBuilder(sql.length());
    String[] sqlToken = sql.split("\\n");
    for (String sqlText : sqlToken) {
      sbt.append(sqlText).append(' ');
    }
    return sbt.toString();
  }

  @Override
  public double estimateRowCount(RelMetadataQuery mq) {
    return rows;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PrelVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitPrel(this, value);
  }

  @Override
  public SelectionVectorMode[] getSupportedEncodings() {
    return SelectionVectorMode.DEFAULT;
  }

  @Override
  public SelectionVectorMode getEncoding() {
    return SelectionVectorMode.NONE;
  }

  @Override
  public boolean needsFinalColumnReordering() {
    return false;
  }
}
