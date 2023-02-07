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
package org.apache.drill.exec.planner.common;

import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.calcite.rex.RexFieldAccess;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.metastore.metadata.TableMetadata;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.rules.ProjectRemoveRule;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexVisitor;
import org.apache.calcite.rex.RexVisitorImpl;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.apache.calcite.util.Pair;
import org.apache.calcite.util.Util;
import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.planner.logical.DrillRelFactories;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.logical.FieldsReWriterUtil;
import org.apache.drill.exec.planner.logical.DrillTranslatableTable;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.resolver.TypeCastRules;
import org.apache.drill.exec.util.Utilities;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that is a subset of the RelOptUtil class and is a placeholder
 * for Drill specific static methods that are needed during either logical or
 * physical planning.
 */
public abstract class DrillRelOptUtil {

  private static final Logger logger = LoggerFactory.getLogger(DrillRelOptUtil.class);

  final public static String IMPLICIT_COLUMN = "$drill_implicit_field$";

  // Similar to RelOptUtil.areRowTypesEqual() with the additional check for allowSubstring
  public static boolean areRowTypesCompatible(
      RelDataType rowType1,
      RelDataType rowType2,
      boolean compareNames,
      boolean allowSubstring) {
    if (rowType1 == rowType2) {
      return true;
    }
    if (compareNames) {
      // if types are not identity-equal, then either the names or
      // the types must be different
      return false;
    }
    if (rowType2.getFieldCount() != rowType1.getFieldCount()) {
      return false;
    }
    final List<RelDataTypeField> f1 = rowType1.getFieldList();
    final List<RelDataTypeField> f2 = rowType2.getFieldList();
    for (Pair<RelDataTypeField, RelDataTypeField> pair : Pair.zip(f1, f2)) {
      final RelDataType type1 = pair.left.getType();
      final RelDataType type2 = pair.right.getType();
      // If one of the types is ANY comparison should succeed
      if (type1.getSqlTypeName() == SqlTypeName.ANY
        || type2.getSqlTypeName() == SqlTypeName.ANY) {
        continue;
      }
      if (type1.getSqlTypeName() != type2.getSqlTypeName()) {
        if (allowSubstring
            && (type1.getSqlTypeName() == SqlTypeName.CHAR && type2.getSqlTypeName() == SqlTypeName.CHAR)
            && (type1.getPrecision() <= type2.getPrecision())) {
          return true;
        }

        // Check if Drill implicit casting can resolve the incompatibility
        List<TypeProtos.MinorType> types = Lists.newArrayListWithCapacity(2);
        types.add(Types.getMinorTypeFromName(type1.getSqlTypeName().getName()));
        types.add(Types.getMinorTypeFromName(type2.getSqlTypeName().getName()));
        return TypeCastRules.getLeastRestrictiveType(types) != null;
      }
    }
    return true;
  }

  /**
   * Returns a relational expression which has the same fields as the
   * underlying expression, but the fields have different names.
   *
   *
   * @param rel Relational expression
   * @param fieldNames Field names
   * @return Renamed relational expression
   */
  public static RelNode createRename(
      RelNode rel,
      final List<String> fieldNames) {
    final List<RelDataTypeField> fields = rel.getRowType().getFieldList();
    assert fieldNames.size() == fields.size();
    final List<RexNode> refs =
        new AbstractList<RexNode>() {
          @Override
          public int size() {
            return fields.size();
          }

          @Override
          public RexNode get(int index) {
            return RexInputRef.of(index, fields);
          }
        };

    return DrillRelFactories.LOGICAL_BUILDER
        .create(rel.getCluster(), null)
        .push(rel)
        .projectNamed(refs, fieldNames, true)
        .build();
  }

  public static boolean isTrivialProject(Project project, boolean useNamesInIdentityProjCalc) {
    if (!useNamesInIdentityProjCalc) {
      return ProjectRemoveRule.isTrivial(project);
    }  else {
      return containIdentity(project.getProjects(), project.getRowType(), project.getInput().getRowType());
    }
  }

  /** Returns a rowType having all unique field name.
   *
   * @param rowType input rowType
   * @param typeFactory type factory used to create a new row type.
   * @return a rowType having all unique field name.
   */
  public static RelDataType uniqifyFieldName(final RelDataType rowType, final RelDataTypeFactory typeFactory) {
    return typeFactory.createStructType(RelOptUtil.getFieldTypeList(rowType),
        SqlValidatorUtil.uniquify(rowType.getFieldNames(), SqlValidatorUtil.EXPR_SUGGESTER, true));
  }

  /**
   * Returns whether the leading edge of a given array of expressions is
   * wholly {@link RexInputRef} objects with types and names corresponding
   * to the underlying row type. */
  private static boolean containIdentity(List<? extends RexNode> exps,
                                        RelDataType rowType, RelDataType childRowType) {
    List<RelDataTypeField> fields = rowType.getFieldList();
    List<RelDataTypeField> childFields = childRowType.getFieldList();
    int fieldCount = childFields.size();
    if (exps.size() != fieldCount) {
      return false;
    }
    for (int i = 0; i < exps.size(); i++) {
      RexNode exp = exps.get(i);
      if (!(exp instanceof RexInputRef)) {
        return false;
      }
      RexInputRef var = (RexInputRef) exp;
      if (var.getIndex() != i) {
        return false;
      }
      if (!fields.get(i).getName().equals(childFields.get(i).getName())) {
        return false;
      }
      if (!fields.get(i).getType().equals(childFields.get(i).getType())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Travesal RexNode to find at least one operator in the given collection. Continue search if RexNode has a
   * RexInputRef which refers to a RexNode in project expressions.
   *
   * @param node RexNode to search
   * @param projExprs the list of project expressions. Empty list means there is No project operator underneath.
   * @param operators collection of operators to find
   * @return Return null if there is NONE; return the first appearance of item/flatten RexCall.
   */
  public static RexCall findOperators(final RexNode node, final List<RexNode> projExprs, final Collection<String> operators) {
    try {
      RexVisitor<Void> visitor =
          new RexVisitorImpl<Void>(true) {
            @Override
            public Void visitCall(RexCall call) {
              if (operators.contains(call.getOperator().getName().toLowerCase())) {
                throw new Util.FoundOne(call); /* throw exception to interrupt tree walk (this is similar to
                                              other utility methods in RexUtil.java */
              }
              return super.visitCall(call);
            }

            @Override
            public Void visitInputRef(RexInputRef inputRef) {
              if (projExprs.size() == 0 ) {
                return super.visitInputRef(inputRef);
              } else {
                final int index = inputRef.getIndex();
                RexNode n = projExprs.get(index);
                if (n instanceof RexCall) {
                  RexCall r = (RexCall) n;
                  if (operators.contains(r.getOperator().getName().toLowerCase())) {
                    throw new Util.FoundOne(r);
                  }
                }

                return super.visitInputRef(inputRef);
              }
            }
          };
      node.accept(visitor);
      return null;
    } catch (Util.FoundOne e) {
      Util.swallow(e, null);
      return (RexCall) e.getNode();
    }
  }

  public static boolean isLimit0(RexNode fetch) {
    if (fetch != null && fetch.isA(SqlKind.LITERAL)) {
      RexLiteral l = (RexLiteral) fetch;
      switch (l.getTypeName()) {
        case BIGINT:
        case INTEGER:
        case DECIMAL:
          if (((long) l.getValue2()) == 0) {
            return true;
          }
        default:
      }
    }
    return false;
  }

  /**
   * Find whether the given project rel can produce non-scalar output (hence unknown rowcount). This
   * would happen if the project has a flatten
   * @param project The project rel
   * @return Return true if the rowcount is unknown. Otherwise, false.
   */
  public static boolean isProjectOutputRowcountUnknown(Project project) {
    for (RexNode rex : project.getProjects()) {
      if (rex instanceof RexCall) {
        if ("flatten".equalsIgnoreCase(((RexCall) rex).getOperator().getName())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Find whether the given project rel has unknown output schema. This would happen if the
   * project has CONVERT_FROMJSON which can only derive the schema after evaluation is performed
   * @param project The project rel
   * @return Return true if the project output schema is unknown. Otherwise, false.
   */
  public static boolean isProjectOutputSchemaUnknown(Project project) {
    try {
      RexVisitor<Void> visitor =
          new RexVisitorImpl<Void>(true) {
            @Override
            public Void visitCall(RexCall call) {
              if ("convert_fromjson".equalsIgnoreCase(call.getOperator().getName())) {
                throw new Util.FoundOne(call); /* throw exception to interrupt tree walk (this is similar to
                                              other utility methods in RexUtil.java */
              }
              return super.visitCall(call);
            }
          };
      for (RexNode rex : project.getProjects()) {
        rex.accept(visitor);
      }
    } catch (Util.FoundOne e) {
      Util.swallow(e, null);
      return true;
    }
    return false;
  }

  public static TableScan findScan(RelNode... rels) {
    for (RelNode rel : rels) {
      if (rel instanceof TableScan) {
        return (TableScan) rel;
      } else if (rel instanceof RelSubset) {
        RelSubset relSubset = (RelSubset) rel;
        return findScan(Util.first(relSubset.getBest(), relSubset.getOriginal()));
      } else {
        return findScan(rel.getInputs().toArray(new RelNode[0]));
      }
    }
    return null;
  }

  /**
   * InputRefVisitor is a utility class used to collect all the RexInputRef nodes in a
   * RexNode.
   *
   */
  public static class InputRefVisitor extends RexVisitorImpl<Void> {
    private final List<RexInputRef> inputRefList;

    public InputRefVisitor() {
      super(true);
      inputRefList = new ArrayList<>();
    }

    @Override
    public Void visitInputRef(RexInputRef ref) {
      inputRefList.add(ref);
      return null;
    }

    @Override
    public Void visitCall(RexCall call) {
      for (RexNode operand : call.operands) {
        operand.accept(this);
      }
      return null;
    }

    public List<RexInputRef> getInputRefs() {
      return inputRefList;
    }
  }

  /**
   * For a given row type return a map between old field indices and one index right shifted fields.
   * @param rowType row type to be right shifted.
   * @return map hash map between old and new indices
   */
  public static Map<Integer, Integer> rightShiftColsInRowType(RelDataType rowType) {
    Map<Integer, Integer> map = new HashMap<>();
    int fieldCount = rowType.getFieldCount();
    for (int i = 0; i< fieldCount; i++) {
      map.put(i, i+1);
    }
    return map;
  }

  /**
   * Given a list of rexnodes it transforms the rexnodes by changing the expr to use new index mapped to the old index.
   * @param builder RexBuilder from the planner.
   * @param exprs RexNodes to be transformed.
   * @param corrMap Mapping between old index to new index.
   * @return list of transformed expressions
   */
  public static List<RexNode> transformExprs(RexBuilder builder, List<RexNode> exprs, Map<Integer, Integer> corrMap) {
    List<RexNode> outputExprs = new ArrayList<>();
    DrillRelOptUtil.RexFieldsTransformer transformer = new DrillRelOptUtil.RexFieldsTransformer(builder, corrMap);
    for (RexNode expr : exprs) {
      outputExprs.add(transformer.go(expr));
    }
    return outputExprs;
  }

  /**
   * Given a of rexnode it transforms the rexnode by changing the expr to use new index mapped to the old index.
   * @param builder RexBuilder from the planner.
   * @param expr RexNode to be transformed.
   * @param corrMap Mapping between old index to new index.
   * @return transformed expression
   */
  public static RexNode transformExpr(RexBuilder builder, RexNode expr, Map<Integer, Integer> corrMap) {
    DrillRelOptUtil.RexFieldsTransformer transformer = new DrillRelOptUtil.RexFieldsTransformer(builder, corrMap);
    return transformer.go(expr);
  }

  /**
   * RexFieldsTransformer is a utility class used to convert column refs in a RexNode
   * based on inputRefMap (input to output ref map).
   *
   * This transformer can be used to find and replace the existing inputRef in a RexNode with a new inputRef.
   */
  public static class RexFieldsTransformer {
    private final RexBuilder rexBuilder;
    private final Map<Integer, Integer> inputRefMap;

    public RexFieldsTransformer(
      RexBuilder rexBuilder,
      Map<Integer, Integer> inputRefMap) {
      this.rexBuilder = rexBuilder;
      this.inputRefMap = inputRefMap;
    }

    public RexNode go(RexNode rex) {
      if (rex instanceof RexCall) {
        ImmutableList.Builder<RexNode> builder = ImmutableList.builder();
        final RexCall call = (RexCall) rex;
        for (RexNode operand : call.operands) {
          builder.add(go(operand));
        }
        return call.clone(call.getType(), builder.build());
      } else if (rex instanceof RexInputRef) {
        RexInputRef var = (RexInputRef) rex;
        int index = var.getIndex();
        return rexBuilder.makeInputRef(var.getType(), inputRefMap.get(index));
      } else {
        return rex;
      }
    }
  }

  @SuppressWarnings("deprecation")
  public static boolean isProjectFlatten(RelNode project) {

    assert project instanceof Project : "Rel is NOT an instance of project!";

    for (RexNode rex : project.getChildExps()) {
      if (rex instanceof RexCall) {
        RexCall function = (RexCall) rex;
        String functionName = function.getOperator().getName();
        if (functionName.equalsIgnoreCase("flatten") ) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Stores information about fields, their names and types.
   * Is responsible for creating mapper which used in field re-writer visitor.
   */
  public static class ProjectPushInfo {
    private final List<SchemaPath> fields;
    private final FieldsReWriterUtil.FieldsReWriter reWriter;
    private final List<String> fieldNames;
    private final List<RelDataType> types;

    public ProjectPushInfo(List<SchemaPath> fields, Map<String, FieldsReWriterUtil.DesiredField> desiredFields) {
      this.fields = fields;
      this.fieldNames = new ArrayList<>();
      this.types = new ArrayList<>();

      Map<RexNode, Integer> mapper = new HashMap<>();

      int index = 0;
      for (Map.Entry<String, FieldsReWriterUtil.DesiredField> entry : desiredFields.entrySet()) {
        fieldNames.add(entry.getKey());
        FieldsReWriterUtil.DesiredField desiredField = entry.getValue();
        types.add(desiredField.getType());
        for (RexNode node : desiredField.getNodes()) {
          mapper.put(node, index);
        }
        index++;
      }
      this.reWriter = new FieldsReWriterUtil.FieldsReWriter(mapper);
    }

    public List<SchemaPath> getFields() {
      return fields;
    }

    public FieldsReWriterUtil.FieldsReWriter getInputReWriter() {
      return reWriter;
    }

    /**
     * Creates new row type based on stores types and field names.
     *
     * @param factory factory for data type descriptors.
     * @return new row type
     */
    public RelDataType createNewRowType(RelDataTypeFactory factory) {
      return factory.createStructType(types, fieldNames);
    }
  }

  public static ProjectPushInfo getFieldsInformation(RelDataType rowType, List<RexNode> projects) {
    ProjectFieldsVisitor fieldsVisitor = new ProjectFieldsVisitor(rowType);
    for (RexNode exp : projects) {
      PathSegment segment = exp.accept(fieldsVisitor);
      fieldsVisitor.addField(segment);
    }

    return fieldsVisitor.getInfo();
  }

  /**
   * Visitor that finds the set of inputs that are used.
   */
  private static class ProjectFieldsVisitor extends RexVisitorImpl<PathSegment> {
    private final List<String> fieldNames;
    private final List<RelDataTypeField> fields;

    private final Set<SchemaPath> newFields = Sets.newLinkedHashSet();
    private final Map<String, FieldsReWriterUtil.DesiredField> desiredFields = new LinkedHashMap<>();

    ProjectFieldsVisitor(RelDataType rowType) {
      super(true);
      this.fieldNames = rowType.getFieldNames();
      this.fields = rowType.getFieldList();
    }

    void addField(PathSegment segment) {
      if (segment instanceof PathSegment.NameSegment) {
        newFields.add(new SchemaPath((PathSegment.NameSegment) segment));
      }
    }

    ProjectPushInfo getInfo() {
      return new ProjectPushInfo(ImmutableList.copyOf(newFields), ImmutableMap.copyOf(desiredFields));
    }

    @Override
    public PathSegment visitInputRef(RexInputRef inputRef) {
      int index = inputRef.getIndex();
      String name = fieldNames.get(index);
      RelDataTypeField field = fields.get(index);
      addDesiredField(name, field.getType(), inputRef);
      return new PathSegment.NameSegment(name);
    }

    @Override
    public PathSegment visitCall(RexCall call) {
      String itemStarFieldName = FieldsReWriterUtil.getFieldNameFromItemStarField(call, fieldNames);
      if (itemStarFieldName != null) {
        addDesiredField(itemStarFieldName, call.getType(), call);
        return new PathSegment.NameSegment(itemStarFieldName);
      }

      if (SqlStdOperatorTable.ITEM.equals(call.getOperator())) {
        PathSegment mapOrArray = call.operands.get(0).accept(this);
        if (mapOrArray != null) {
          if (call.operands.get(1) instanceof RexLiteral) {
            return mapOrArray.cloneWithNewChild(Utilities.convertLiteral((RexLiteral) call.operands.get(1)));
          }
          return mapOrArray;
        }
      } else {
        for (RexNode operand : call.operands) {
          addField(operand.accept(this));
        }
      }
      return null;
    }

    @Override
    public PathSegment visitFieldAccess(RexFieldAccess fieldAccess) {
      PathSegment refPath = fieldAccess.getReferenceExpr().accept(this);
      PathSegment.NameSegment fieldPath = new PathSegment.NameSegment(fieldAccess.getField().getName());
      return refPath.cloneWithNewChild(fieldPath);
    }

    private void addDesiredField(String name, RelDataType type, RexNode originalNode) {
      FieldsReWriterUtil.DesiredField desiredField = desiredFields.get(name);
      if (desiredField == null) {
        desiredFields.put(name, new FieldsReWriterUtil.DesiredField(name, type, originalNode));
      } else {
        desiredField.addNode(originalNode);
      }
    }
  }

  /**
   * Returns whether statistics-based estimates or guesses are used by the optimizer
   * for the {@link RelNode} rel.
   * @param rel input
   * @return TRUE if the estimate is a guess, FALSE otherwise
   * */
  public static boolean guessRows(RelNode rel) {
    final PlannerSettings settings =
        rel.getCluster().getPlanner().getContext().unwrap(PlannerSettings.class);
    if (!settings.useStatistics()) {
      return true;
    }
    /* We encounter RelSubset/HepRelVertex which are CALCITE constructs, hence we
     * cannot add guessRows() to the DrillRelNode interface.
     */
    if (rel instanceof RelSubset) {
      if (((RelSubset) rel).getBest() != null) {
        return guessRows(((RelSubset) rel).getBest());
      } else if (((RelSubset) rel).getOriginal() != null) {
        return guessRows(((RelSubset) rel).getOriginal());
      }
    } else if (rel instanceof HepRelVertex) {
      if (((HepRelVertex) rel).getCurrentRel() != null) {
        return guessRows(((HepRelVertex) rel).getCurrentRel());
      }
    } else if (rel instanceof TableScan) {
      DrillTable table = Utilities.getDrillTable(rel.getTable());
      try {
        TableMetadata tableMetadata;
        return table == null
            || (tableMetadata = table.getGroupScan().getTableMetadata()) == null
            || !TableStatisticsKind.HAS_DESCRIPTIVE_STATISTICS.getValue(tableMetadata);
      } catch (IOException e) {
        logger.debug("Unable to obtain table metadata due to exception: {}", e.getMessage(), e);
        return true;
      }
    } else {
      for (RelNode child : rel.getInputs()) {
        if (guessRows(child)) { // at least one child is a guess
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns whether the join condition is a simple equi-join or not. A simple equi-join is
   * defined as an two-table equality join (no self-join)
   * @param join input join
   * @param joinFieldOrdinals join field ordinal w.r.t. the underlying inputs to the join
   * @return TRUE if the join is a simple equi-join (not a self-join), FALSE otherwise
   * */
  public static boolean analyzeSimpleEquiJoin(Join join, int[] joinFieldOrdinals) {
    RexNode joinExp = join.getCondition();
    if (joinExp.getKind() != SqlKind.EQUALS) {
      return false;
    } else {
      RexCall binaryExpression = (RexCall) joinExp;
      RexNode leftComparand = binaryExpression.operands.get(0);
      RexNode rightComparand = binaryExpression.operands.get(1);
      if (!(leftComparand instanceof RexInputRef)) {
        return false;
      } else if (!(rightComparand instanceof RexInputRef)) {
        return false;
      } else {
        int leftFieldCount = join.getLeft().getRowType().getFieldCount();
        int rightFieldCount = join.getRight().getRowType().getFieldCount();
        RexInputRef leftFieldAccess = (RexInputRef) leftComparand;
        RexInputRef rightFieldAccess = (RexInputRef) rightComparand;
        if (leftFieldAccess.getIndex() >= leftFieldCount + rightFieldCount ||
            rightFieldAccess.getIndex() >= leftFieldCount + rightFieldCount) {
          return false;
        }
        /* Both columns reference same table */
        if ((leftFieldAccess.getIndex() >= leftFieldCount &&
            rightFieldAccess.getIndex() >= leftFieldCount) ||
           (leftFieldAccess.getIndex() < leftFieldCount &&
            rightFieldAccess.getIndex() < leftFieldCount)) {
          return false;
        } else {
          if (leftFieldAccess.getIndex() < leftFieldCount) {
            joinFieldOrdinals[0] = leftFieldAccess.getIndex();
            joinFieldOrdinals[1] = rightFieldAccess.getIndex() - leftFieldCount;
          } else {
            joinFieldOrdinals[0] = rightFieldAccess.getIndex();
            joinFieldOrdinals[1] = leftFieldAccess.getIndex() - leftFieldCount;
          }
          return true;
        }
      }
    }
  }

  public static DrillTable getDrillTable(final TableScan scan) {
    DrillTable drillTable = scan.getTable().unwrap(DrillTable.class);
    if (drillTable == null) {
      DrillTranslatableTable transTable = scan.getTable().unwrap(DrillTranslatableTable.class);
      if (transTable != null) {
        drillTable = transTable.getDrillTable();
      }
    }
    return drillTable;
  }

  public static List<Pair<Integer, Integer>> analyzeSimpleEquiJoin(Join join) {
    List<Pair<Integer, Integer>> joinConditions = new ArrayList<>();
    try {
      RexVisitor<Void> visitor =
          new RexVisitorImpl<Void>(true) {
            @Override
            public Void visitCall(RexCall call) {
              if (call.getKind() == SqlKind.AND || call.getKind() == SqlKind.OR) {
                super.visitCall(call);
              } else {
                if (call.getKind() == SqlKind.EQUALS) {
                  RexNode leftComparand = call.operands.get(0);
                  RexNode rightComparand = call.operands.get(1);
                  // If a join condition predicate has something more complicated than a RexInputRef
                  // we bail out!
                  if (!(leftComparand instanceof RexInputRef && rightComparand instanceof RexInputRef)) {
                    joinConditions.clear();
                    throw new Util.FoundOne(call);
                  }
                  int leftFieldCount = join.getLeft().getRowType().getFieldCount();
                  int rightFieldCount = join.getRight().getRowType().getFieldCount();
                  RexInputRef leftFieldAccess = (RexInputRef) leftComparand;
                  RexInputRef rightFieldAccess = (RexInputRef) rightComparand;
                  if (leftFieldAccess.getIndex() >= leftFieldCount + rightFieldCount ||
                      rightFieldAccess.getIndex() >= leftFieldCount + rightFieldCount) {
                    joinConditions.clear();
                    throw new Util.FoundOne(call);
                  }
                  /* Both columns reference same table */
                  if ((leftFieldAccess.getIndex() >= leftFieldCount &&
                      rightFieldAccess.getIndex() >= leftFieldCount) ||
                          (leftFieldAccess.getIndex() < leftFieldCount &&
                              rightFieldAccess.getIndex() < leftFieldCount)) {
                    joinConditions.clear();
                    throw new Util.FoundOne(call);
                  } else {
                    if (leftFieldAccess.getIndex() < leftFieldCount) {
                      joinConditions.add(Pair.of(leftFieldAccess.getIndex(),
                          rightFieldAccess.getIndex() - leftFieldCount));
                    } else {
                      joinConditions.add(Pair.of(rightFieldAccess.getIndex(),
                          leftFieldAccess.getIndex() - leftFieldCount));
                    }
                  }
                }
              }
              return null;
            }
          };
      join.getCondition().accept(visitor);
    } catch (Util.FoundOne ex) {
      Util.swallow(ex, null);
    }
    return joinConditions;
  }

  public static List<RexInputRef> findAllRexInputRefs(final RexNode node) {
    List<RexInputRef> rexRefs = new ArrayList<>();
    RexVisitor<Void> visitor =
            new RexVisitorImpl<Void>(true) {
              @Override
              public Void visitInputRef(RexInputRef inputRef) {
                rexRefs.add(inputRef);
                return super.visitInputRef(inputRef);
              }
            };
    node.accept(visitor);
    return rexRefs;
  }
}
