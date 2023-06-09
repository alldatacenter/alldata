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
package org.apache.drill.exec.planner.physical.visitor;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.physical.ScreenPrel;
import org.apache.drill.exec.planner.physical.WriterPrel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adds non-trivial top project to ensure the final output field names are preserved.
 * Such non-trivial project is needed due to Calcite's behavior of ProjectRemoveRule.
 * It will be added under Screen/Writer operator in the physical plan
 * if there is no other Projects under these operators,
 * in cases like * column expansion or partition by column processing.
 */
public class TopProjectVisitor extends BasePrelVisitor<Prel, Void, RuntimeException> {

  private final RelDataType validatedRowType;

  public TopProjectVisitor(RelDataType validatedRowType) {
    this.validatedRowType = validatedRowType;
  }

  /**
   * Traverses passed physical relational node and its children and checks if top project
   * should be added under screen or writer to preserve final output fields names.
   *
   * @param prel physical relational node
   * @param validatedRowType final output row type
   * @return physical relational node with added project if necessary
   */
  public static Prel insertTopProject(Prel prel, RelDataType validatedRowType){
    return prel.accept(new TopProjectVisitor(validatedRowType), null);
  }

  @Override
  public Prel visitPrel(Prel prel, Void value) throws RuntimeException {
    List<RelNode> children = new ArrayList<>();
    for (Prel child : prel){
      child = child.accept(this, null);
      children.add(child);
    }

    return (Prel) prel.copy(prel.getTraitSet(), children);
  }

  @Override
  public Prel visitScreen(ScreenPrel prel, Void value) {
    // insert project under screen only if we don't have writer underneath
    if (containsWriter(prel)) {
      return prel;
    }

    Prel newChild = ((Prel) prel.getInput()).accept(this, value);
    return prel.copy(prel.getTraitSet(), Collections.singletonList((RelNode)addTopProjectPrel(newChild, validatedRowType)));
  }

  @Override
  public Prel visitWriter(WriterPrel prel, Void value) {
    Prel newChild = ((Prel) prel.getInput()).accept(this, value);
    return prel.copy(prel.getTraitSet(), Collections.singletonList((RelNode)addTopProjectPrel(newChild, validatedRowType)));
  }

  /**
   * Checks if at least one of passed physical relational node children is writer.
   *
   * @param prel physical relational node
   * @return true of writer operator was found
   */
  private boolean containsWriter(Prel prel) {
    for (Prel child : prel){
      if (child instanceof WriterPrel || containsWriter(child)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Adds top project to ensure final output field names are preserved.
   * In case of duplicated column names, will rename duplicates.
   * Top project will be added only if top project is non-trivial and
   * child physical relational node is not project.
   *
   * @param prel physical relational node
   * @param validatedRowType final output row type
   * @return physical relational node with top project if necessary
   */
  private Prel addTopProjectPrel(Prel prel, RelDataType validatedRowType) {
    RelDataType rowType = prel.getRowType();
    if (rowType.getFieldCount() != validatedRowType.getFieldCount()) {
      return prel;
    }

    RexBuilder rexBuilder = prel.getCluster().getRexBuilder();
    List<RexNode> projections = new ArrayList<>();
    int projectCount = rowType.getFieldList().size();

    for (int i = 0; i < projectCount; i++) {
      projections.add(rexBuilder.makeInputRef(prel, i));
    }

    List<String> fieldNames = SqlValidatorUtil.uniquify(
        validatedRowType.getFieldNames(),
        SqlValidatorUtil.EXPR_SUGGESTER,
        prel.getCluster().getTypeFactory().getTypeSystem().isSchemaCaseSensitive());

    RelDataType newRowType = RexUtil.createStructType(prel.getCluster().getTypeFactory(), projections, fieldNames, null);
    ProjectPrel topProject = new ProjectPrel(prel.getCluster(),
        prel.getTraitSet(),
        prel,
        projections,
        newRowType,
        true);  //outputProj = true : NONE -> OK_NEW_SCHEMA, also handle expression with NULL type.

    if (prel instanceof Project && DrillRelOptUtil.isTrivialProject(topProject, true)) {
      return new ProjectPrel(prel.getCluster(),
          prel.getTraitSet(),
          ((Project) prel).getInput(),
          ((Project) prel).getProjects(),
          prel.getRowType(),
          true); //outputProj = true : NONE -> OK_NEW_SCHEMA, also handle expression with NULL type.
    } else {
      return topProject;
    }
  }


}
