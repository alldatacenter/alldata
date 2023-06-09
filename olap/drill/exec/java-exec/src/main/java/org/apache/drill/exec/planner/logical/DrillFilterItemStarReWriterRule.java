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
package org.apache.drill.exec.planner.logical;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.rel.rules.ProjectRemoveRule;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexVisitorImpl;
import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.planner.types.RelDataTypeDrillImpl;
import org.apache.drill.exec.planner.types.RelDataTypeHolder;
import org.apache.drill.exec.store.parquet.AbstractParquetGroupScan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.drill.exec.planner.logical.FieldsReWriterUtil.DesiredField;
import static org.apache.drill.exec.planner.logical.FieldsReWriterUtil.FieldsReWriter;

/**
 * Rule will transform item star fields in filter and replaced with actual field references.
 *
 * This will help partition pruning and push down rules to detect fields that can be pruned or push downed.
 * Item star operator appears when sub-select or cte with star are used as source.
 */
public class DrillFilterItemStarReWriterRule {

  public static final ProjectOnScan PROJECT_ON_SCAN = new ProjectOnScan(
          RelOptHelper.some(DrillProjectRel.class, RelOptHelper.any(DrillScanRel.class)),
          "DrillFilterItemStarReWriterRule.ProjectOnScan");

  public static final FilterOnScan FILTER_ON_SCAN = new FilterOnScan(
      RelOptHelper.some(DrillFilterRel.class, RelOptHelper.any(DrillScanRel.class)),
      "DrillFilterItemStarReWriterRule.FilterOnScan");

  public static final FilterProjectScan FILTER_PROJECT_SCAN = new FilterProjectScan(
      RelOptHelper.some(DrillFilterRel.class, RelOptHelper.some(DrillProjectRel.class, RelOptHelper.any(DrillScanRel.class))),
      "DrillFilterItemStarReWriterRule.FilterProjectScan");


  private static class ProjectOnScan extends RelOptRule {

    ProjectOnScan(RelOptRuleOperand operand, String id) {
      super(operand, id);
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
      DrillScanRel scan = call.rel(1);
      return scan.getGroupScan() instanceof AbstractParquetGroupScan && super.matches(call);
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
      DrillProjectRel projectRel = call.rel(0);
      DrillScanRel scanRel = call.rel(1);

      ItemStarFieldsVisitor itemStarFieldsVisitor = new ItemStarFieldsVisitor(scanRel.getRowType().getFieldNames());
      List<RexNode> projects = projectRel.getProjects();
      for (RexNode project : projects) {
        project.accept(itemStarFieldsVisitor);
      }

      // if there are no item fields, no need to proceed further
      if (itemStarFieldsVisitor.hasNoItemStarFields()) {
        return;
      }

      Map<String, DesiredField> itemStarFields = itemStarFieldsVisitor.getItemStarFields();

      DrillScanRel newScan = createNewScan(scanRel, itemStarFields);

      // re-write projects
      Map<RexNode, Integer> fieldMapper = createFieldMapper(itemStarFields.values(), scanRel.getRowType().getFieldCount());
      FieldsReWriter fieldsReWriter = new FieldsReWriter(fieldMapper);
      List<RexNode> newProjects = new ArrayList<>();
      for (RexNode node : projectRel.getChildExps()) {
        newProjects.add(node.accept(fieldsReWriter));
      }

      DrillProjectRel newProject = new DrillProjectRel(
          projectRel.getCluster(),
          projectRel.getTraitSet(),
          newScan,
          newProjects,
          projectRel.getRowType());

      if (ProjectRemoveRule.isTrivial(newProject)) {
        call.transformTo(newScan);
      } else {
        call.transformTo(newProject);
      }
    }

  }

  private static class FilterOnScan extends RelOptRule {

    FilterOnScan(RelOptRuleOperand operand, String id) {
      super(operand, id);
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
      DrillScanRel scan = call.rel(1);
      return scan.getGroupScan() instanceof AbstractParquetGroupScan && super.matches(call);
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
      DrillFilterRel filterRel = call.rel(0);
      DrillScanRel scanRel = call.rel(1);
      transformFilterCall(filterRel, null, scanRel, call);
    }
  }

  private static class FilterProjectScan extends RelOptRule {

    FilterProjectScan(RelOptRuleOperand operand, String id) {
      super(operand, id);
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
      DrillScanRel scan = call.rel(2);
      return scan.getGroupScan() instanceof AbstractParquetGroupScan && super.matches(call);
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
      DrillFilterRel filterRel = call.rel(0);
      DrillProjectRel projectRel = call.rel(1);
      DrillScanRel scanRel = call.rel(2);
      transformFilterCall(filterRel, projectRel, scanRel, call);
    }
  }


  /**
   * Removes item star call from filter expression and propagates changes into project (if present) and scan.
   *
   * @param filterRel original filter expression
   * @param projectRel original project expression
   * @param scanRel original scan expression
   * @param call original rule call
   */
  private static void transformFilterCall(DrillFilterRel filterRel, DrillProjectRel projectRel, DrillScanRel scanRel, RelOptRuleCall call) {
    List<String> fieldNames = projectRel == null ? scanRel.getRowType().getFieldNames() : projectRel.getRowType().getFieldNames();
    ItemStarFieldsVisitor itemStarFieldsVisitor = new ItemStarFieldsVisitor(fieldNames);
    filterRel.getCondition().accept(itemStarFieldsVisitor);

    // if there are no item fields, no need to proceed further
    if (itemStarFieldsVisitor.hasNoItemStarFields()) {
      return;
    }

    Map<String, DesiredField> itemStarFields = itemStarFieldsVisitor.getItemStarFields();

    DrillScanRel newScan = createNewScan(scanRel, itemStarFields);

    // create new project if was present in call
    DrillProjectRel newProject = null;
    if (projectRel != null) {

      // add new projects to the already existing in original project
      int projectIndex = scanRel.getRowType().getFieldCount();
      List<RexNode> newProjects = new ArrayList<>(projectRel.getProjects());
      for (DesiredField desiredField : itemStarFields.values()) {
        newProjects.add(new RexInputRef(projectIndex, desiredField.getType()));
        projectIndex++;
      }

      RelDataType newProjectRowType = createNewRowType(
          projectRel.getCluster().getTypeFactory(),
          projectRel.getRowType().getFieldList(),
          itemStarFields.keySet());

      newProject = new DrillProjectRel(
          projectRel.getCluster(),
          projectRel.getTraitSet(),
          newScan,
          newProjects,
          newProjectRowType);
    }

    // transform filter condition
    Map<RexNode, Integer> fieldMapper = createFieldMapper(itemStarFields.values(), scanRel.getRowType().getFieldCount());
    FieldsReWriter fieldsReWriter = new FieldsReWriter(fieldMapper);
    RexNode newCondition = filterRel.getCondition().accept(fieldsReWriter);

    // create new filter
    DrillFilterRel newFilter = DrillFilterRel.create(newProject != null ? newProject : newScan, newCondition);

    // wrap with project to have the same row type as before
    List<RexNode> newProjects = new ArrayList<>();
    RelDataType rowType = filterRel.getRowType();
    List<RelDataTypeField> fieldList = rowType.getFieldList();
    for (RelDataTypeField field : fieldList) {
      RexInputRef inputRef = new RexInputRef(field.getIndex(), field.getType());
      newProjects.add(inputRef);
    }

    DrillProjectRel wrapper = new DrillProjectRel(filterRel.getCluster(), filterRel.getTraitSet(), newFilter, newProjects, filterRel.getRowType());
    call.transformTo(wrapper);
  }

  /**
   * Creates new row type with merged original and new fields.
   *
   * @param typeFactory type factory
   * @param originalFields original fields
   * @param newFields new fields
   * @return new row type with original and new fields
   */
  private static RelDataType createNewRowType(RelDataTypeFactory typeFactory,
                                              List<RelDataTypeField> originalFields,
                                              Collection<String> newFields) {
    RelDataTypeHolder relDataTypeHolder = new RelDataTypeHolder();

    // add original fields
    for (RelDataTypeField field : originalFields) {
      relDataTypeHolder.getField(typeFactory, field.getName());
    }

    // add new fields
    for (String fieldName : newFields) {
      relDataTypeHolder.getField(typeFactory, fieldName);
    }

    return new RelDataTypeDrillImpl(relDataTypeHolder, typeFactory);
  }

  /**
   * Creates new scan with fields from original scan and fields used in item star operator.
   *
   * @param scanRel original scan expression
   * @param itemStarFields item star fields
   * @return new scan expression
   */
  private static DrillScanRel createNewScan(DrillScanRel scanRel, Map<String, DesiredField> itemStarFields) {
    RelDataType newScanRowType = createNewRowType(
            scanRel.getCluster().getTypeFactory(),
            scanRel.getRowType().getFieldList(),
            itemStarFields.keySet());

    List<SchemaPath> columns = new ArrayList<>(scanRel.getColumns());
    for (DesiredField desiredField : itemStarFields.values()) {
      String name = desiredField.getName();
      PathSegment.NameSegment nameSegment = new PathSegment.NameSegment(name);
      columns.add(new SchemaPath(nameSegment));
    }

    return new DrillScanRel(
            scanRel.getCluster(),
            scanRel.getTraitSet().plus(DrillRel.DRILL_LOGICAL),
            scanRel.getTable(),
            newScanRowType,
            columns);
  }

  /**
   * Creates node mapper to replace item star calls with new input field references.
   * Starting index should be calculated from the last used input expression (i.e. scan expression).
   * NB: field reference index starts from 0 thus original field count can be taken as starting index
   *
   * @param desiredFields list of desired fields
   * @param startingIndex starting index
   * @return field mapper
   */
  private static Map<RexNode, Integer> createFieldMapper(Collection<DesiredField> desiredFields, int startingIndex) {
    Map<RexNode, Integer> fieldMapper = new HashMap<>();

    int index = startingIndex;
    for (DesiredField desiredField : desiredFields) {
      for (RexNode node : desiredField.getNodes()) {
        // if field is referenced in more then one call, add each call to field mapper
        fieldMapper.put(node, index);
      }
      // increment index for the next node reference
      index++;
    }
    return fieldMapper;
  }

  /**
   * Traverses given node and stores all item star fields.
   * For the fields with the same name, stores original calls in a list, does not duplicate fields.
   * Holds state, should not be re-used.
   */
  private static class ItemStarFieldsVisitor extends RexVisitorImpl<RexNode> {

    private final Map<String, DesiredField> itemStarFields = new HashMap<>();
    private final List<String> fieldNames;

    ItemStarFieldsVisitor(List<String> fieldNames) {
      super(true);
      this.fieldNames = fieldNames;
    }

    boolean hasNoItemStarFields() {
      return itemStarFields.isEmpty();
    }

    Map<String, DesiredField> getItemStarFields() {
      return itemStarFields;
    }

    @Override
    public RexNode visitCall(RexCall call) {
      // need to figure out field name and index
      String fieldName = FieldsReWriterUtil.getFieldNameFromItemStarField(call, fieldNames);
      if (fieldName != null) {
        // if there is call to the already existing field, store call, do not duplicate field
        DesiredField desiredField = itemStarFields.get(fieldName);
        if (desiredField == null) {
          itemStarFields.put(fieldName, new DesiredField(fieldName, call.getType(), call));
        } else {
          desiredField.addNode(call);
        }
      }

      return super.visitCall(call);
    }

  }

}
