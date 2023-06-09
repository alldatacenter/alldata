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
package org.apache.drill.exec.planner.physical;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.common.DrillProjectRelBase;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.logical.DrillParseContext;
import org.apache.drill.exec.planner.physical.visitor.PrelVisitor;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;

/**
 * A physical Prel node for Project operator.
 */
public class ProjectPrel extends DrillProjectRelBase implements Prel{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProjectPrel.class);

  protected final boolean outputProj;

  public ProjectPrel(RelOptCluster cluster, RelTraitSet traits, RelNode child, List<RexNode> exps,
      RelDataType rowType) {
    this(cluster, traits, child, exps, rowType, false);
  }

  /**
   * Constructor for ProjectPrel.
   * @param cluster
   * @param traits traits of ProjectPrel node
   * @param child  input
   * @param exps   list of RexNode, representing expressions of projection.
   * @param rowType output rowType of projection expression.
   * @param outputProj true if ProjectPrel is inserted by {@link org.apache.drill.exec.planner.physical.visitor.TopProjectVisitor}
   *                   Such top Project operator does the following processing, before the result was presented to Screen/Writer
   *                   <ol>
   *                   <li>ensure final output field names are preserved</li>
   *                   <li>handle cases where input does not return any batch (a fast NONE) (see ProjectRecordBatch.handleNullInput() method)</li>
   *                   <li>handle cases where expressions in upstream operator were evaluated to NULL type </li>
   *                   (Null type will be converted into Nullable-INT)
   *                   </ol>
   *                   false otherwise.
   */
  public ProjectPrel(RelOptCluster cluster, RelTraitSet traits, RelNode child, List<RexNode> exps,
      RelDataType rowType, boolean outputProj) {
    super(DRILL_PHYSICAL, cluster, traits, child, exps, rowType);
    this.outputProj = outputProj;
  }

  @Override
  public Project copy(RelTraitSet traitSet, RelNode input, List<RexNode> exps, RelDataType rowType) {
    return new ProjectPrel(getCluster(), traitSet, input, exps, rowType, this.outputProj);
  }


  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {
    Prel child = (Prel) this.getInput();

    PhysicalOperator childPOP = child.getPhysicalOperator(creator);

    org.apache.drill.exec.physical.config.Project p = new org.apache.drill.exec.physical.config.Project(
        this.getProjectExpressions(new DrillParseContext(PrelUtil.getSettings(getCluster()))),  childPOP, outputProj);
    return creator.addMetadata(this, p);
  }

  @Override
  public Iterator<Prel> iterator() {
    return PrelUtil.iter(getInput());
  }

  @Override
  public <T, X, E extends Throwable> T accept(PrelVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitProject(this, value);
  }

  @Override
  public SelectionVectorMode[] getSupportedEncodings() {
    return SelectionVectorMode.DEFAULT;
  }

  @Override
  public SelectionVectorMode getEncoding() {
    return SelectionVectorMode.NONE;
  }

  /**
   * Whether this Project requires a final column re-ordering. Returns False for all cases except when
   * convert_fromjson function is present.  For convert_fromjson function, the Project operator at
   * run-time produces an output schema with convert_fromjson expr appended to the end of the schema.
   * We need a final column re-ordering to ensure the correct column order.
  */
  @Override
  public boolean needsFinalColumnReordering() {
    for (RexNode expr : this.exps) {
      // TODO: a convert_fromjson nested within other convert functions currently does not work.
      // When it is supported, we should enhance this check by using a visitor to find the nested function.
      if (expr.getKind() == SqlKind.OTHER_FUNCTION &&
          expr instanceof RexCall &&
          ((RexCall) expr).getOperator().getName().equalsIgnoreCase("CONVERT_FROMJSON")) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Prel prepareForLateralUnnestPipeline(List<RelNode> children) {
    RelDataTypeFactory typeFactory = this.getCluster().getTypeFactory();
    RexBuilder builder = this.getCluster().getRexBuilder();
    List<RexNode> projects = Lists.newArrayList();
    projects.add(builder.makeInputRef(typeFactory.createSqlType(SqlTypeName.INTEGER), 0));
    // right shift the previous field indices.
    projects.addAll(DrillRelOptUtil.transformExprs(builder, this.getProjects(),
                        DrillRelOptUtil.rightShiftColsInRowType(this.getInput().getRowType())));

    List<String> fieldNames = new ArrayList<>();
    List<RelDataType> fieldTypes = new ArrayList<>();

    fieldNames.add("$drill_implicit_field$");
    fieldTypes.add(typeFactory.createSqlType(SqlTypeName.INTEGER));

    for (RelDataTypeField field : this.rowType.getFieldList()) {
      fieldNames.add(field.getName());
      fieldTypes.add(field.getType());
    }

    RelDataType newRowType = typeFactory.createStructType(fieldTypes, fieldNames);

    return (Prel) this.copy(this.getTraitSet(), children.get(0), projects, newRowType);
  }
}
