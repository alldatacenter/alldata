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

package org.apache.drill.exec.planner.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.calcite.rel.RelCollations;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Sort;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.sql.SqlKind;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.IndexGroupScan;
import org.apache.drill.exec.planner.common.DrillProjectRelBase;
import org.apache.drill.exec.planner.common.DrillScanRelBase;
import org.apache.drill.exec.planner.fragment.DistributionAffinity;
import org.apache.drill.exec.planner.logical.DrillOptiq;
import org.apache.drill.exec.planner.logical.DrillParseContext;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.common.OrderedRel;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;

public class IndexPlanUtils {

  public enum ConditionIndexed {
    NONE,
    PARTIAL,
    FULL}

  /**
   * Check if any of the fields of the index are present in a list of LogicalExpressions supplied
   * as part of IndexableExprMarker
   * @param exprMarker the marker that has analyzed original index condition on top of original scan
   * @param indexDesc the index definition plus functions to access materialized index
   * @return ConditionIndexed.FULL, PARTIAL or NONE depending on whether all, some or no columns
   * of the indexDesc are present in the list of LogicalExpressions supplied as part of exprMarker
   *
   */
  static public ConditionIndexed conditionIndexed(IndexableExprMarker exprMarker, IndexDescriptor indexDesc) {
    Map<RexNode, LogicalExpression> mapRexExpr = exprMarker.getIndexableExpression();
    List<LogicalExpression> infoCols = Lists.newArrayList();
    infoCols.addAll(mapRexExpr.values());
    if (indexDesc.allColumnsIndexed(infoCols)) {
      return ConditionIndexed.FULL;
    } else if (indexDesc.someColumnsIndexed(infoCols)) {
      return ConditionIndexed.PARTIAL;
    } else {
      return ConditionIndexed.NONE;
    }
  }

  /**
   * check if we want to apply index rules on this scan,
   * if group scan is not instance of DbGroupScan, or this DbGroupScan instance does not support secondary index, or
   *    this scan is already an index scan or Restricted Scan, do not apply index plan rules on it.
   * @param scanRel the current scan rel
   * @return true to indicate that we want to apply index rules on this scan, otherwise false
   */
  static public boolean checkScan(DrillScanRel scanRel) {
    GroupScan groupScan = scanRel.getGroupScan();
    if (groupScan instanceof DbGroupScan) {
      DbGroupScan dbscan = ((DbGroupScan) groupScan);
      // if we already applied index convert rule, and this scan is indexScan or restricted scan already,
      // no more trying index convert rule
      return dbscan.supportsSecondaryIndex() && (!dbscan.isIndexScan()) && (!dbscan.isRestrictedScan());
    }
    return false;
  }

  /**
   * For a particular table scan for table T1 and an index on that table, find out if it is a covering index
   * @return true if it is a covering index, otherwise false
   */
  static public boolean isCoveringIndex(IndexCallContext indexContext, FunctionalIndexInfo functionInfo) {
    if (functionInfo.hasFunctional()) {
      // need info from full query
      return queryCoveredByIndex(indexContext, functionInfo);
    }
    DbGroupScan groupScan = (DbGroupScan) getGroupScan(indexContext.getScan());
    List<LogicalExpression> tableCols = Lists.newArrayList();
    tableCols.addAll(groupScan.getColumns());
    return functionInfo.getIndexDesc().isCoveringIndex(tableCols);
  }


  /**
   * This method is called only when the index has at least one functional indexed field. If there is no function field,
   * we don't need to worry whether there could be paths not found in Scan.
   * In functional case, we have to check all available (if needed) operators to find out if the query is covered or not.
   * E.g. cast(a.b as INT) in project, a.b in Scan's rowType or columns, and cast(a.b as INT)
   * is an indexed field named '$0'. In this case, by looking at Scan, we see only 'a.b' which is not in index. We have to
   * look into Project, and if we see 'a.b' is only used in functional index expression cast(a.b as INT), then we know
   * this Project+Scan is covered.
   * @param indexContext the index call context
   * @param functionInfo functional index information that may impact rewrite
   * @return false if the query could not be covered by the index (should not create covering index plan)
   */
  static private boolean queryCoveredByIndex(IndexCallContext indexContext,
                              FunctionalIndexInfo functionInfo) {
    // for indexed functions, if relevant schemapaths are included in index(in indexed fields or non-indexed fields),
    // check covering based on the local information we have:
    // if references to schema paths in functional indexes disappear beyond capProject

    if (indexContext.getFilter() != null && indexContext.getUpperProject() == null) {
      if (!isFullQuery(indexContext)) {
        return false;
      }
    }

    DrillParseContext parserContext =
        new DrillParseContext(PrelUtil.getPlannerSettings(indexContext.getCall().rel(0).getCluster()));

    Set<LogicalExpression> exprs = Sets.newHashSet();
    if (indexContext.getUpperProject() != null) {
      if (indexContext.getLowerProject() == null) {
        for (RexNode rex : indexContext.getUpperProject().getProjects()) {
          LogicalExpression expr = RexToExpression.toDrill(parserContext, null, indexContext.getScan(), rex);
          exprs.add(expr);
        }
        // now collect paths in filter since upperProject may drop some paths in filter
        IndexableExprMarker filterMarker = new IndexableExprMarker(indexContext.getScan());
        indexContext.getFilterCondition().accept(filterMarker);
        for (RexNode rex : filterMarker.getIndexableExpression().keySet()) {
          LogicalExpression expr = RexToExpression.toDrill(parserContext, null, indexContext.getScan(), rex);
          exprs.add(expr);
        }
      } else {
        // we have underneath project, so we have to do more to convert expressions
        for (RexNode rex : indexContext.getUpperProject().getProjects()) {
          LogicalExpression expr = RexToExpression.toDrill(parserContext, indexContext.getLowerProject(), indexContext.getScan(), rex);
          exprs.add(expr);
        }

        // Now collect paths in filter since upperProject may drop some paths in filter.
        // Since this is (upper)Proj+Filter+(lower)Proj+Scan case, and IndexableExprMarker works
        // only with expressions that referencing directly to Scan, it has to use indexContext.origPushedCondition
        IndexableExprMarker filterMarker = new IndexableExprMarker(indexContext.getScan());
        indexContext.getOrigCondition().accept(filterMarker);

        for (RexNode rex : filterMarker.getIndexableExpression().keySet()) {
          // Since rex represents the filter expression directly referencing the scan row type,
          // (the condition has been pushed down of lowerProject), set the lowerProject as null.
          LogicalExpression expr = RexToExpression.toDrill(parserContext, null, indexContext.getScan(), rex);
          exprs.add(expr);
        }
      }
    }
    else if (indexContext.getLowerProject() != null) {
      for (RexNode rex : indexContext.getLowerProject().getProjects()) {
        LogicalExpression expr = DrillOptiq.toDrill(parserContext, indexContext.getScan(), rex);
        exprs.add(expr);
      }
    }
    else { // upperProject and lowerProject both are null, the only place to find columns being used in query is scan
      exprs.addAll(indexContext.getScanColumns());
    }

    Map<LogicalExpression, Set<SchemaPath>> exprPathMap = functionInfo.getPathsInFunctionExpr();
    PathInExpr exprSearch = new PathInExpr(exprPathMap);

    for (LogicalExpression expr: exprs) {
      if (expr.accept(exprSearch, null) == false) {
        return false;
      }
    }
    // if we come to here, paths in indexed function expressions are covered in capProject.
    // now we check other paths.

    // check the leftout paths (appear in capProject other than functional index expression) are covered by other index fields or not
    List<LogicalExpression> leftPaths = Lists.newArrayList(exprSearch.getRemainderPaths());

    indexContext.setLeftOutPathsInFunctions(exprSearch.getRemainderPathsInFunctions());
    return functionInfo.getIndexDesc().isCoveringIndex(leftPaths);
  }

  static private boolean isFullQuery(IndexCallContext indexContext) {
    RelNode rootInCall = indexContext.getCall().rel(0);
    // check if the tip of the operator stack we have is also the top of the whole query, if yes, return true
    if (indexContext.getCall().getPlanner().getRoot() instanceof RelSubset) {
      final RelSubset rootSet = (RelSubset) indexContext.getCall().getPlanner().getRoot();
      if (rootSet.getRelList().contains(rootInCall)) {
        return true;
      }
    } else {
      if (indexContext.getCall().getPlanner().getRoot().equals(rootInCall)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Build collation property for the 'lower' project, the one closer to the Scan
   * @param projectRexs list of row expressions
   * @param input input as a relational expression
   * @param indexInfo collects functional index information
   * @return the output RelCollation
   */
  public static RelCollation buildCollationLowerProject(List<RexNode> projectRexs, RelNode input, FunctionalIndexInfo indexInfo) {
    // if leading fields of index are here, add them to RelCollation
    List<RelFieldCollation> newFields = Lists.newArrayList();
    if (!indexInfo.hasFunctional()) {
      Map<LogicalExpression, Integer> projectExprs = Maps.newLinkedHashMap();
      DrillParseContext parserContext = new DrillParseContext(PrelUtil.getPlannerSettings(input.getCluster()));
      int idx=0;
      for (RexNode rex : projectRexs) {
        projectExprs.put(DrillOptiq.toDrill(parserContext, input, rex), idx);
        idx++;
      }
      int idxFieldCount = 0;
      for (LogicalExpression expr : indexInfo.getIndexDesc().getIndexColumns()) {
        if (!projectExprs.containsKey(expr)) {
          break;
        }
        RelFieldCollation.Direction dir = indexInfo.getIndexDesc().getCollation().getFieldCollations().get(idxFieldCount).direction;
        if ( dir == null) {
          break;
        }
        newFields.add(new RelFieldCollation(projectExprs.get(expr), dir,
            RelFieldCollation.NullDirection.UNSPECIFIED));
      }
      idxFieldCount++;
    } else {
      // TODO: handle functional index
    }

    return RelCollations.of(newFields);
  }

  /**
   * Build collation property for the 'upper' project, the one above the filter
   * @param projectRexs list of row expressions
   * @param inputCollation the input collation
   * @param indexInfo collects functional index information
   * @param collationFilterMap map for collation filter
   * @return the output RelCollation
   */
  public static RelCollation buildCollationUpperProject(List<RexNode> projectRexs,
                                                        RelCollation inputCollation, FunctionalIndexInfo indexInfo,
                                                        Map<Integer, List<RexNode>> collationFilterMap) {
    List<RelFieldCollation> outputFieldCollations = Lists.newArrayList();

    if (inputCollation != null) {
      List<RelFieldCollation> inputFieldCollations = inputCollation.getFieldCollations();
      if (!indexInfo.hasFunctional()) {
        for (int projectExprIdx = 0; projectExprIdx < projectRexs.size(); projectExprIdx++) {
          RexNode n = projectRexs.get(projectExprIdx);
          if (n instanceof RexInputRef) {
            RexInputRef ref = (RexInputRef)n;
            boolean eligibleForCollation = true;
            int maxIndex = getIndexFromCollation(ref.getIndex(), inputFieldCollations);
            if (maxIndex < 0) {
              eligibleForCollation = false;
              continue;
            }
            // check if the prefix has equality conditions
            for (int i = 0; i < maxIndex; i++) {
              int fieldIdx = inputFieldCollations.get(i).getFieldIndex();
              List<RexNode> conditions = collationFilterMap != null ? collationFilterMap.get(fieldIdx) : null;
              if ((conditions == null || conditions.size() == 0) &&
                  i < maxIndex-1) {
                // if an intermediate column has no filter condition, it would select all values
                // of that column, so a subsequent column cannot be eligible for collation
                eligibleForCollation = false;
                break;
              } else {
                for (RexNode r : conditions) {
                  if (!(r.getKind() == SqlKind.EQUALS)) {
                    eligibleForCollation = false;
                    break;
                  }
                }
              }
            }
            // for every projected expr, if it is eligible for collation, get the
            // corresponding field collation from the input
            if (eligibleForCollation) {
              for (RelFieldCollation c : inputFieldCollations) {
                if (ref.getIndex() == c.getFieldIndex()) {
                  RelFieldCollation outFieldCollation = new RelFieldCollation(projectExprIdx, c.getDirection(), c.nullDirection);
                  outputFieldCollations.add(outFieldCollation);
                }
              }
            }
          }
        }
      } else {
        // TODO: handle functional index
      }
    }
    return RelCollations.of(outputFieldCollations);
  }

  public static int getIndexFromCollation(int refIndex, List<RelFieldCollation> inputFieldCollations) {
    for (int i = 0; i < inputFieldCollations.size(); i++) {
      if (refIndex == inputFieldCollations.get(i).getFieldIndex()) {
        return i;
      }
    }
    return -1;
  }

  public static List<RexNode> getProjects(DrillProjectRelBase proj) {
    return proj.getProjects();
  }

  public static boolean generateLimit(OrderedRel sort) {
    RexNode fetchNode = sort.getFetch();
    int fetchValue = (fetchNode == null) ? -1 : RexLiteral.intValue(fetchNode);
    return fetchValue >=0;
  }

  public static RexNode getOffset(OrderedRel sort) {
    return sort.getOffset();
  }

  public static RexNode getFetch(OrderedRel sort) {
    return sort.getFetch();
  }


  /**
   * generate logical expressions for sort rexNodes in SortRel, the result is store to IndexPlanCallContext
   * @param indexContext the index call context
   * @param coll list of field collations
   */
  public static void updateSortExpression(IndexCallContext indexContext, List<RelFieldCollation> coll) {

    if (coll == null) {
      return;
    }

    DrillParseContext parserContext =
        new DrillParseContext(PrelUtil.getPlannerSettings(indexContext.getCall().rel(0).getCluster()));

    indexContext.createSortExprs();
    for (RelFieldCollation collation : coll) {
      int idx = collation.getFieldIndex();
      DrillProjectRelBase oneProject;
      if (indexContext.getUpperProject() != null && indexContext.getLowerProject() != null) {
        LogicalExpression expr = RexToExpression.toDrill(parserContext, indexContext.getLowerProject(), indexContext.getScan(),
            indexContext.getUpperProject().getProjects().get(idx));
        indexContext.getSortExprs().add(expr);
      }
      else { // one project is null now
        oneProject = (indexContext.getUpperProject() != null)? indexContext.getUpperProject() : indexContext.getLowerProject();
        if (oneProject != null) {
          LogicalExpression expr = RexToExpression.toDrill(parserContext, null, indexContext.getScan(),
              getProjects(oneProject).get(idx));
          indexContext.getSortExprs().add(expr);
        }
        else { // two projects are null
          SchemaPath path;
          RelDataTypeField f = indexContext.getScan().getRowType().getFieldList().get(idx);
          String pathSeg = f.getName().replaceAll("`", "");
          final String[] segs = pathSeg.split("\\.");
          path = SchemaPath.getCompoundPath(segs);
          indexContext.getSortExprs().add(path);
        }
      }
    }
  }

  /**
   * generate logical expressions for sort rexNodes in SortRel, the result is store to IndexPlanCallContext
   * @param indexContext the index call context
   * @param coll list of field collations
   */
  public static void updateSortExpression(IndexPhysicalPlanCallContext indexContext, List<RelFieldCollation> coll) {

    if (coll == null) {
      return;
    }

    DrillParseContext parserContext =
            new DrillParseContext(PrelUtil.getPlannerSettings(indexContext.call.rel(0).getCluster()));

    indexContext.sortExprs = Lists.newArrayList();
    for (RelFieldCollation collation : coll) {
      int idx = collation.getFieldIndex();
      ProjectPrel oneProject;
      if (indexContext.upperProject != null && indexContext.lowerProject != null) {
        LogicalExpression expr = RexToExpression.toDrill(parserContext, indexContext.lowerProject, indexContext.scan,
                indexContext.upperProject.getProjects().get(idx));
        indexContext.sortExprs.add(expr);
      }
      else { // one project is null now
        oneProject = (indexContext.upperProject != null)? indexContext.upperProject : indexContext.lowerProject;
        if (oneProject != null) {
          LogicalExpression expr = RexToExpression.toDrill(parserContext, null, indexContext.scan,
                  oneProject.getProjects().get(idx));
          indexContext.sortExprs.add(expr);
        }
        else { // two projects are null
          SchemaPath path;
          RelDataTypeField f = indexContext.scan.getRowType().getFieldList().get(idx);
          String pathSeg = f.getName().replaceAll("`", "");
          final String[] segs = pathSeg.split("\\.");
          path = SchemaPath.getCompoundPath(segs);
          indexContext.sortExprs.add(path);
        }
      }
    }
  }

  /**
   *
   * @param expr the input expression
   * @param context the index call context
   * @return if there is filter and expr is only in equality condition of the filter, return true
   */
  private static boolean exprOnlyInEquality(LogicalExpression expr, IndexCallContext context) {
    // if there is no filter, expr wont be in equality
    if (context.getFilter() == null) {
      return false;
    }
    final Set<LogicalExpression> onlyInEquality = context.getOrigMarker().getExpressionsOnlyInEquality();
    return onlyInEquality.contains(expr);

  }
  /**
   * Build collation property for project, the one closer to the Scan
   * @param projectRexs the expressions to project
   * @param project the project between projectRexs and input, it could be null if no such intermediate project(lower project)
   * @param input the input RelNode to the project, usually it is the scan operator.
   * @param indexInfo the index for which we are building index plan
   * @param context the context of this index planning process
   * @return the output RelCollation
   */
  public static RelCollation buildCollationProject(List<RexNode> projectRexs,
                                                   DrillProjectRelBase project,
                                                   RelNode input,
                                                   FunctionalIndexInfo indexInfo,
                                                   IndexCallContext context) {
    Map<LogicalExpression, Integer> projectExprs = getProjectExprs(projectRexs, project, input);
    return buildCollationForExpressions(projectExprs, indexInfo.getIndexDesc(), context);
  }

  /**
   * Build the collation property for index scan
   * @param indexDesc the index for which we are building index plan
   * @param context the context of this index planning process
   * @return the output RelCollation for the scan on index
   */
  public static RelCollation buildCollationCoveringIndexScan(IndexDescriptor indexDesc,
      IndexCallContext context) {
    Map<LogicalExpression, Integer> rowTypeExprs = getExprsFromRowType(context.getScan().getRowType());
    return buildCollationForExpressions(rowTypeExprs, indexDesc, context);
  }

  public static Map<LogicalExpression, Integer> getProjectExprs(List<RexNode> projectRexs,
                                                                DrillProjectRelBase project,
                                                                RelNode input) {
    Map<LogicalExpression, Integer> projectExprs = Maps.newLinkedHashMap();
    DrillParseContext parserContext = new DrillParseContext(PrelUtil.getPlannerSettings(input.getCluster()));
    int idx=0;
    for (RexNode rex : projectRexs) {
      LogicalExpression expr;
      expr = RexToExpression.toDrill(parserContext, project, input, rex);
      projectExprs.put(expr, idx);
      idx++;
    }
    return projectExprs;
  }

  public static Map<LogicalExpression, Integer> getExprsFromRowType( RelDataType indexScanRowType) {

    Map<LogicalExpression, Integer> rowTypeExprs = Maps.newLinkedHashMap();
    int idx = 0;
    for (RelDataTypeField field : indexScanRowType.getFieldList()) {
      rowTypeExprs.put(FieldReference.getWithQuotedRef(field.getName()), idx++);
    }
    return rowTypeExprs;
  }

  /**
   * Given index, compute the collations for a list of projected expressions(from Scan's rowType or Project's )
   * in the context
   * @param projectExprs the output expression list of a RelNode
   * @param indexDesc  the index for which we are building index plan
   * @param context  the context of this index planning process
   * @return the collation provided by index that will be exposed by the expression list
   */
  public static RelCollation buildCollationForExpressions(Map<LogicalExpression, Integer> projectExprs,
                                                        IndexDescriptor indexDesc,
                                                        IndexCallContext context) {

    assert projectExprs != null;

    final List<LogicalExpression> sortExpressions = context.getSortExprs();
    // if leading fields of index are here, add them to RelCollation
    List<RelFieldCollation> newFields = Lists.newArrayList();
    if (indexDesc.getCollation() == null) {
      return RelCollations.of(newFields);
    }

    // go through indexed fields to build collation
    // break out of the loop when found first indexed field [not projected && not _only_ in equality condition of filter]
    // or the leading field is not projected
    List<LogicalExpression> indexedCols = indexDesc.getIndexColumns();
    for (int idxFieldCount=0; idxFieldCount<indexedCols.size(); ++idxFieldCount) {
      LogicalExpression expr = indexedCols.get(idxFieldCount);

      if (!projectExprs.containsKey(expr)) {
        // leading indexed field is not projected
        // but it is only-in-equality field, -- we continue to next indexed field, but we don't generate collation for this field
        if (exprOnlyInEquality(expr, context)) {
          continue;
        }
        // else no more collation is needed to be generated, since we now have one leading field which is not in equality condition
        break;
      }

      // leading indexed field is projected,

      // if this field is not in sort expression && only-in-equality, we don't need to generate collation for this field
      // and we are okay to continue: generate collation for next indexed field.
      if (sortExpressions != null &&
          !sortExpressions.contains(expr) && exprOnlyInEquality(expr, context) ) {
        continue;
      }

      RelCollation idxCollation = indexDesc.getCollation();
      RelFieldCollation.NullDirection nullsDir = idxCollation == null ? RelFieldCollation.NullDirection.UNSPECIFIED :
              idxCollation.getFieldCollations().get(idxFieldCount).nullDirection;
      RelFieldCollation.Direction dir = (idxCollation == null)?
          null : idxCollation.getFieldCollations().get(idxFieldCount).direction;
      if (dir == null) {
        break;
      }
      newFields.add(new RelFieldCollation(projectExprs.get(expr), dir, nullsDir));
    }

    return RelCollations.of(newFields);
  }

  // TODO: proper implementation
  public static boolean pathOnlyInIndexedFunction(SchemaPath path) {
    return true;
  }

  public static RelCollation buildCollationNonCoveringIndexScan(IndexDescriptor indexDesc,
      RelDataType indexScanRowType,
      RelDataType restrictedScanRowType, IndexCallContext context) {

    if (context.getSortExprs() == null) {
      return RelCollations.of(RelCollations.EMPTY.getFieldCollations());
    }

    final List<RelDataTypeField> indexFields = indexScanRowType.getFieldList();
    final List<RelDataTypeField> rsFields = restrictedScanRowType.getFieldList();
    final Map<LogicalExpression, RelFieldCollation> collationMap = indexDesc.getCollationMap();

    assert collationMap != null : "Invalid collation map for index";

    List<RelFieldCollation> fieldCollations = Lists.newArrayList();

    Map<Integer, RelFieldCollation> rsScanCollationMap = Maps.newTreeMap();

    // for each index field that is projected from the indexScan, find the corresponding
    // field in the restricted scan's row type and keep track of the ordinal # in the
    // restricted scan's row type.
    for (int i = 0; i < indexScanRowType.getFieldCount(); i++) {
      RelDataTypeField f1 = indexFields.get(i);
      for (int j = 0; j < rsFields.size(); j++) {
        RelDataTypeField f2 = rsFields.get(j);
        if (f1.getName().equals(f2.getName())) {
          FieldReference ref = FieldReference.getWithQuotedRef(f1.getName());
          RelFieldCollation origCollation = collationMap.get(ref);
          if (origCollation != null) {
            RelFieldCollation fc = new RelFieldCollation(j,
                origCollation.direction, origCollation.nullDirection);
            rsScanCollationMap.put(origCollation.getFieldIndex(), fc);
          }
        }
      }
    }

    // should sort by the order of these fields in indexDesc
    for (Map.Entry<Integer, RelFieldCollation> entry : rsScanCollationMap.entrySet()) {
      RelFieldCollation fc = entry.getValue();
      if (fc != null) {
        fieldCollations.add(fc);
      }
    }

    final RelCollation collation = RelCollations.of(fieldCollations);
    return collation;
  }

  public static boolean scanIsPartition(GroupScan scan) {
    return (scan.isDistributed() || scan.getDistributionAffinity() == DistributionAffinity.HARD);
  }

  public static GroupScan getGroupScan(DrillScanRelBase relNode) {
    return relNode.getGroupScan();
  }

  public static RelCollation getCollation(Sort sort) {
    return sort.getCollation();
  }

  public static List<DrillDistributionTrait.DistributionField> getDistributionField(Sort rel) {
    List<DrillDistributionTrait.DistributionField> distFields = Lists.newArrayList();

    for (RelFieldCollation relField : getCollation(rel).getFieldCollations()) {
      DrillDistributionTrait.DistributionField field = new DrillDistributionTrait.DistributionField(relField.getFieldIndex());
      distFields.add(field);
    }
    return distFields;
  }

  public static ScanPrel buildCoveringIndexScan(DrillScanRelBase origScan,
      IndexGroupScan indexGroupScan,
      IndexCallContext indexContext,
      IndexDescriptor indexDesc) {

    FunctionalIndexInfo functionInfo = indexDesc.getFunctionalInfo();
    //to record the new (renamed)paths added
    List<SchemaPath> rewrittenPaths = Lists.newArrayList();
    DbGroupScan dbGroupScan = (DbGroupScan) getGroupScan(origScan);
    indexGroupScan.setColumns(
        rewriteFunctionColumn(dbGroupScan.getColumns(),
            functionInfo, rewrittenPaths));

    DrillDistributionTrait partition = scanIsPartition(getGroupScan(origScan))?
        DrillDistributionTrait.RANDOM_DISTRIBUTED : DrillDistributionTrait.SINGLETON;
    RelDataType newRowType = FunctionalIndexHelper.rewriteFunctionalRowType(origScan, indexContext, functionInfo, rewrittenPaths);

    // add a default collation trait otherwise Calcite runs into a ClassCastException, which at first glance
    // seems like a Calcite bug
    RelTraitSet indexScanTraitSet = origScan.getTraitSet().plus(Prel.DRILL_PHYSICAL).
        plus(RelCollationTraitDef.INSTANCE.getDefault()).plus(partition);

    // Create the collation traits for index scan based on the index columns under the
    // condition that the index actually has collation property (e.g hash indexes don't)
    if (indexDesc.getCollation() != null) {
      RelCollation collationTrait = buildCollationCoveringIndexScan(indexDesc, indexContext);
      indexScanTraitSet = indexScanTraitSet.plus(collationTrait);
    }

    ScanPrel indexScanPrel = new ScanPrel(origScan.getCluster(),
        indexScanTraitSet, indexGroupScan,
        newRowType, origScan.getTable());

    return indexScanPrel;
  }

  /**
   * For IndexGroupScan, if a column is only appeared in the should-be-renamed function,
   * this column is to-be-replaced column, we replace that column(schemaPath) from 'a.b'
   * to '$1' in the list of SchemaPath.
   * @param paths list of paths
   * @param functionInfo functional index information that may impact rewrite
   * @param addedPaths list of paths added
   * @return list of new paths
   */
  public static List<SchemaPath> rewriteFunctionColumn(List<SchemaPath> paths,
                                                       FunctionalIndexInfo functionInfo,
                                                       List<SchemaPath> addedPaths) {
    if (!functionInfo.hasFunctional()) {
      return paths;
    }

    List<SchemaPath> newPaths = Lists.newArrayList(paths);
    for (int i = 0; i < paths.size(); ++i) {
      SchemaPath newPath = functionInfo.getNewPath(paths.get(i));
      if (newPath == null) {
        continue;
      }

      addedPaths.add(newPath);
      // if this path only in indexed function, we are safe to replace it
      if (pathOnlyInIndexedFunction(paths.get(i))) {
        newPaths.set(i, newPath);
      }
      else {  // we should not replace this column, instead we add a new "$N" field.
        newPaths.add(newPath);
      }
    }
    return newPaths;
  }

  /**
   *A RexNode forest with three RexNodes for expressions "cast(a.q as int) * 2, b+c, concat(a.q, " world")"
   * on Scan RowType('a', 'b', 'c') will be like this:
   *
   *          (0)Call:"*"                                       Call:"concat"
   *           /         \                                    /           \
   *    (1)Call:CAST     2            Call:"+"        (5)Call:ITEM     ' world'
   *      /        \                   /     \          /   \
   * (2)Call:ITEM  TYPE:INT       (3)$1    (4)$2       $0    'q'
   *   /      \
   *  $0     'q'
   *
   * So for above expressions, when visiting the RexNode trees using PathInExpr, we could mark indexed expressions in the trees,
   * as shown in the diagram above are the node (1),
   * then collect the schema paths in the indexed expression but found out of the indexed expression -- node (5),
   * and other regular schema paths (3) (4)
   *
   * @param indexContext the index call context
   * @param parseContext the drill parse context
   * @param project the drill base class for logical and physical projects
   * @param scan a rel node scan
   * @param toRewriteRex the RexNode to be converted if it contains a functional index expression
   * @param newRowType data type for new row
   * @param functionInfo functional index information that may impact rewrite
   * @return rewritten functional row expression
   */
  public static RexNode rewriteFunctionalRex(IndexCallContext indexContext,
                                       DrillParseContext parseContext,
                                       DrillProjectRelBase project,
                                       RelNode scan,
                                       RexNode toRewriteRex,
                                       RelDataType newRowType,
                                       FunctionalIndexInfo functionInfo) {
    if (!functionInfo.hasFunctional()) {
      return toRewriteRex;
    }
    RexToExpression.RexToDrillExt rexToDrill = new RexToExpression.RexToDrillExt(parseContext, project, scan);
    LogicalExpression expr = toRewriteRex.accept(rexToDrill);

    final Map<LogicalExpression, Set<SchemaPath>> exprPathMap = functionInfo.getPathsInFunctionExpr();
    PathInExpr exprSearch = new PathInExpr(exprPathMap);
    expr.accept(exprSearch, null);
    Set<LogicalExpression> remainderPaths = exprSearch.getRemainderPaths();

    // now build the rex->logical expression map for SimpleRexRemap
    // left out schema paths
    Map<LogicalExpression, Set<RexNode>> exprToRex = rexToDrill.getMapExprToRex();
    final Map<RexNode, LogicalExpression> mapRexExpr = Maps.newHashMap();
    for (LogicalExpression leftExpr: remainderPaths) {
      if (exprToRex.containsKey(leftExpr)) {
        Set<RexNode> rexs = exprToRex.get(leftExpr);
        for (RexNode rex: rexs) {
          mapRexExpr.put(rex, leftExpr);
        }
      }
    }

    // functional expressions e.g. cast(a.b as int)
    for (LogicalExpression functionExpr: functionInfo.getExprMap().keySet()) {
      if (exprToRex.containsKey(functionExpr)) {
        Set<RexNode> rexs = exprToRex.get(functionExpr);
        for (RexNode rex: rexs) {
          mapRexExpr.put(rex, functionExpr);
        }
      }

    }

    SimpleRexRemap remap = new SimpleRexRemap(indexContext.getScan(), newRowType, indexContext.getScan().getCluster().getRexBuilder());
    remap.setExpressionMap(functionInfo.getExprMap());
    return remap.rewriteWithMap(toRewriteRex, mapRexExpr);
  }

  public static RexNode getLeadingPrefixMap(Map<LogicalExpression, RexNode> leadingPrefixMap,
                                  List<LogicalExpression> indexCols,
                                  IndexConditionInfo.Builder builder, RexNode condition) {
    boolean prefix = true;
    int i = 0;

    RexNode initCondition = condition.isAlwaysTrue() ? null : condition;
    while (prefix && i < indexCols.size()) {
      LogicalExpression p = indexCols.get(i++);
      List<LogicalExpression> prefixCol = ImmutableList.of(p);
      IndexConditionInfo info = builder.indexConditionRelatedToFields(prefixCol, initCondition);
      if (info != null && info.hasIndexCol) {
        // the col had a match with one of the conditions; save the information about
        // indexcol --> condition mapping
        leadingPrefixMap.put(p, info.indexCondition);
        initCondition = info.remainderCondition;
        if (initCondition.isAlwaysTrue()) {
          // all filter conditions are accounted for, so if the remainder is TRUE, set it to NULL because
          // we don't need to keep track of it for rest of the index selection
          initCondition = null;
          break;
        }
      } else {
        prefix = false;
      }
    }
    return initCondition;
  }

  public static List<RexNode> getLeadingFilters (Map<LogicalExpression, RexNode> leadingPrefixMap, List<LogicalExpression> indexCols) {
    List<RexNode> leadingFilters = Lists.newArrayList();
    if (leadingPrefixMap.size() > 0) {
      for (LogicalExpression p : indexCols) {
        RexNode n;
        if ((n = leadingPrefixMap.get(p)) != null) {
          leadingFilters.add(n);
        } else {
          break; // break since the prefix property will not be preserved
        }
      }
    }
    return leadingFilters;
  }

  public static RexNode getLeadingColumnsFilter(List<RexNode> leadingFilters, RexBuilder rexBuilder) {
    if (leadingFilters.size() > 0) {
      RexNode leadingColumnsFilter = RexUtil.composeConjunction(rexBuilder, leadingFilters, false);
      return leadingColumnsFilter;
    }
    return null;
  }

  public static RexNode getTotalRemainderFilter(RexNode indexColsRemFilter, RexNode incColsRemFilter, RexBuilder rexBuilder) {
    if (indexColsRemFilter != null && incColsRemFilter != null) {
      List<RexNode> operands = Lists.newArrayList();
      operands.add(indexColsRemFilter);
      operands.add(incColsRemFilter);
      RexNode totalRemainder = RexUtil.composeConjunction(rexBuilder, operands, false);
      return totalRemainder;
    } else if (indexColsRemFilter != null) {
      return indexColsRemFilter;
    } else {
      return incColsRemFilter;
    }
  }

  public static RexNode getTotalFilter(RexNode leadColsFilter, RexNode totRemColsFilter, RexBuilder rexBuilder) {
    RexNode condition = leadColsFilter;
    if (leadColsFilter != null && totRemColsFilter != null && !totRemColsFilter.isAlwaysTrue()) {
      List<RexNode> conditions = new ArrayList<RexNode>();
      conditions.add(leadColsFilter);
      conditions.add(totRemColsFilter);
      return RexUtil.composeConjunction(rexBuilder, conditions, true);
    }
    return condition;
  }
}
