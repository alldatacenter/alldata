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
package org.apache.drill.exec.store.jdbc;

import org.apache.calcite.plan.ConventionTraitDef;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.AbstractRelNode;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.physical.PhysicalPlanCreator;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.visitor.PrelVisitor;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a JDBC Plan once the children nodes have been rewritten into SQL.
 */
public class JdbcPrel extends AbstractRelNode implements Prel {
  private final String sql;
  private final double rows;
  private final DrillJdbcConvention convention;

  public JdbcPrel(RelOptCluster cluster, RelTraitSet traitSet, JdbcIntermediatePrel prel) {
    super(cluster, traitSet);
    final RelNode input = prel.getInput();
    rows = input.estimateRowCount(cluster.getMetadataQuery());
    convention = (DrillJdbcConvention) input.getTraitSet().getTrait(ConventionTraitDef.INSTANCE);
    sql = convention.getPlugin().getJdbcDialect().generateSql(getCluster(), input);
    rowType = input.getRowType();
  }

  //Substitute newline. Also stripping away single line comments. Expecting hints to be nested in '/* <hint> */'
  private String stripToOneLineSql(String sql) {
    StringBuilder strippedSqlTextBldr = new StringBuilder(sql.length());
    String[] sqlToken = sql.split("\\n");
    for (String sqlTextLine : sqlToken) {
      if (!sqlTextLine.trim().startsWith("--")) { //Skip comments
        strippedSqlTextBldr.append(sqlTextLine).append(' ');
      }
    }
    return strippedSqlTextBldr.toString();
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) {
    List<SchemaPath> columns = new ArrayList<>();
    for (String col : rowType.getFieldNames()) {
      columns.add(SchemaPath.getSimplePath(col));
    }
    JdbcGroupScan output = new JdbcGroupScan(sql, columns, convention.getPlugin(), rows);
    return creator.addMetadata(this, output);
  }

  @Override
  public RelWriter explainTerms(RelWriter pw) {
    return super.explainTerms(pw).item("sql", stripToOneLineSql(sql));
  }

  @Override
  public double estimateRowCount(RelMetadataQuery mq) {
    return rows;
  }

  @Override
  public Iterator<Prel> iterator() {
    return Collections.emptyIterator();
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
