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


import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.logical.partition.RewriteCombineBinaryOperators;
import org.apache.calcite.rel.RelNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IndexConditionInfo {
  public final RexNode indexCondition;
  public final RexNode remainderCondition;
  public final boolean hasIndexCol;

  public IndexConditionInfo(RexNode indexCondition, RexNode remainderCondition, boolean hasIndexCol) {
    this.indexCondition = indexCondition;
    this.remainderCondition = remainderCondition;
    this.hasIndexCol = hasIndexCol;
  }

  public static Builder newBuilder(RexNode condition,
                                   Iterable<IndexDescriptor> indexes,
                                   RexBuilder builder,
                                   RelNode scan) {
    return new Builder(condition, indexes, builder, scan);
  }

  public static class Builder {
    final RexBuilder builder;
    final RelNode scan;
    final Iterable<IndexDescriptor> indexes;
    private RexNode condition;

    public Builder(RexNode condition,
                   Iterable<IndexDescriptor> indexes,
                   RexBuilder builder,
                   RelNode scan) {
      this.condition = condition;
      this.builder = builder;
      this.scan = scan;
      this.indexes = indexes;
    }

    public Builder(RexNode condition,
                   IndexDescriptor index,
                   RexBuilder builder,
                   DrillScanRel scan) {
      this.condition = condition;
      this.builder = builder;
      this.scan = scan;
      this.indexes = Lists.newArrayList(index);
    }

    /**
     * Get a single IndexConditionInfo in which indexCondition has field  on all indexes in this.indexes
     * @return
     */
    public IndexConditionInfo getCollectiveInfo(IndexLogicalPlanCallContext indexContext) {
      Set<LogicalExpression> paths = Sets.newLinkedHashSet();
      for (IndexDescriptor index : indexes ) {
        paths.addAll(index.getIndexColumns());
        //paths.addAll(index.getNonIndexColumns());
      }
      return indexConditionRelatedToFields(Lists.newArrayList(paths), condition);
    }

    /*
     * A utility function to check whether the given index hint is valid.
     */
    public boolean isValidIndexHint(IndexLogicalPlanCallContext indexContext) {
      if (indexContext.indexHint.equals("")) {
        return false;
      }

      for (IndexDescriptor index: indexes ) {
        if (indexContext.indexHint.equals(index.getIndexName())) {
          return true;
        }
      }
      return false;
    }

    /**
     * Get a map of Index=>IndexConditionInfo, each IndexConditionInfo has the separated condition and remainder condition.
     * The map is ordered, so the last IndexDescriptor will have the final remainderCondition after separating conditions
     * that are relevant to this.indexes. The conditions are separated on LEADING index columns.
     * @return Map containing index{@link IndexDescriptor} and condition {@link IndexConditionInfo} pairs
     */
    public Map<IndexDescriptor, IndexConditionInfo> getFirstKeyIndexConditionMap() {

      Map<IndexDescriptor, IndexConditionInfo> indexInfoMap = Maps.newLinkedHashMap();

      RexNode initCondition = condition;
      for (IndexDescriptor index : indexes) {
        List<LogicalExpression> leadingColumns = new ArrayList<>();
        if (initCondition.isAlwaysTrue()) {
          break;
        }
        // TODO: Ensure we dont get NULL pointer exceptions
        leadingColumns.add(index.getIndexColumns().get(0));
        IndexConditionInfo info = indexConditionRelatedToFields(leadingColumns, initCondition);
        if (info == null || info.hasIndexCol == false) {
          // No info found, based on remaining condition. Check if the leading columns are same as another index
          IndexConditionInfo origInfo = indexConditionRelatedToFields(leadingColumns, condition);
          if (origInfo == null || origInfo.hasIndexCol == false) {
            // do nothing
          } else {
            indexInfoMap.put(index, origInfo);
            // Leave the initCondition as-is, since this is a duplicate condition
          }
          continue;
        }
        indexInfoMap.put(index, info);
        initCondition = info.remainderCondition;
      }
      return indexInfoMap;
    }

    /**
     * Given a RexNode corresponding to the condition expression tree and the index descriptor,
     * check if one or more columns involved in the condition tree form a prefix of the columns in the
     * index keys.
     * @param indexDesc
     * @param initCondition
     * @return True if prefix, False if not
     */
    public boolean isConditionPrefix(IndexDescriptor indexDesc, RexNode initCondition) {
      List<LogicalExpression> indexCols = indexDesc.getIndexColumns();
      boolean prefix = true;
      int numPrefix = 0;
      if (indexCols.size() > 0 && initCondition != null) {
        int i = 0;
        while (prefix && i < indexCols.size()) {
          LogicalExpression p = indexCols.get(i++);
          List<LogicalExpression> prefixCol = ImmutableList.of(p);
          IndexConditionInfo info = indexConditionRelatedToFields(prefixCol, initCondition);
          if (info != null && info.hasIndexCol) {
            numPrefix++;
            initCondition = info.remainderCondition;
            if (initCondition.isAlwaysTrue()) {
              // all filter conditions are accounted for
              break;
            }
          } else {
            prefix = false;
          }
        }
      }
      return numPrefix > 0;
    }

    /**
     * Get a map of Index=>IndexConditionInfo, each IndexConditionInfo has the separated condition and remainder condition.
     * The map is ordered, so the last IndexDescriptor will have the final remainderCondition after separating conditions
     * that are relevant to the indexList. The conditions are separated based on index columns.
     * @return Map containing index{@link IndexDescriptor} and condition {@link IndexConditionInfo} pairs
     */
    public Map<IndexDescriptor, IndexConditionInfo> getIndexConditionMap(List<IndexDescriptor> indexList) {
      return getIndexConditionMapInternal(indexList);
    }

    /**
     * Get a map of Index=>IndexConditionInfo, each IndexConditionInfo has the separated condition and remainder condition.
     * The map is ordered, so the last IndexDescriptor will have the final remainderCondition after separating conditions
     * that are relevant to this.indexes. The conditions are separated based on index columns.
     * @return Map containing index{@link IndexDescriptor} and condition {@link IndexConditionInfo} pairs
     */
    public Map<IndexDescriptor, IndexConditionInfo> getIndexConditionMap() {
      return getIndexConditionMapInternal(Lists.newArrayList(indexes));
    }

    private Map<IndexDescriptor, IndexConditionInfo> getIndexConditionMapInternal(List<IndexDescriptor> indexes) {

      Map<IndexDescriptor, IndexConditionInfo> indexInfoMap = Maps.newLinkedHashMap();
      RexNode initCondition = condition;
      for (IndexDescriptor index : indexes) {
        if (initCondition.isAlwaysTrue()) {
          break;
        }
        if (!isConditionPrefix(index, initCondition)) {
          continue;
        }
        IndexConditionInfo info = indexConditionRelatedToFields(index.getIndexColumns(), initCondition);
        if (info == null || info.hasIndexCol == false) {
          continue;
        }
        initCondition = info.remainderCondition;
        indexInfoMap.put(index, info);
      }
      return indexInfoMap;
    }

    /**
     * Given a list of Index Expressions(usually indexed fields/functions from one or a set of indexes),
     * separate a filter condition into
     *     1), relevant subset of conditions (by relevant, it means at least one given index Expression was found) and,
     *     2), the rest in remainderCondition
     * @param relevantPaths
     * @param condition
     * @return
     */
    public IndexConditionInfo indexConditionRelatedToFields(List<LogicalExpression> relevantPaths, RexNode condition) {
      // Use the same filter analyzer that is used for partitioning columns
      RewriteCombineBinaryOperators reverseVisitor =
          new RewriteCombineBinaryOperators(true, builder);

      condition = condition.accept(reverseVisitor);

      RexSeparator separator = new RexSeparator(relevantPaths, scan, builder);
      RexNode indexCondition = separator.getSeparatedCondition(condition);

      if (indexCondition == null) {
        return new IndexConditionInfo(null, null, false);
      }

      List<RexNode> conjuncts = RelOptUtil.conjunctions(condition);
      List<RexNode> indexConjuncts = RelOptUtil.conjunctions(indexCondition);
      for (RexNode indexConjunction: indexConjuncts) {
        RexUtil.removeAll(conjuncts, indexConjunction);
      }

      RexNode remainderCondition = RexUtil.composeConjunction(builder, conjuncts, false);

      indexCondition = indexCondition.accept(reverseVisitor);

      return new IndexConditionInfo(indexCondition, remainderCondition, true);
    }

  }
}
