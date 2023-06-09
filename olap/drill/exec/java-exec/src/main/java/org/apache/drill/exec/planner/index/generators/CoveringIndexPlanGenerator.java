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
package org.apache.drill.exec.planner.index.generators;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.physical.base.IndexGroupScan;
import org.apache.drill.exec.planner.index.IndexLogicalPlanCallContext;
import org.apache.drill.exec.planner.index.IndexDescriptor;
import org.apache.drill.exec.planner.index.FunctionalIndexInfo;
import org.apache.drill.exec.planner.index.FunctionalIndexHelper;
import org.apache.drill.exec.planner.index.IndexPlanUtils;
import org.apache.drill.exec.planner.index.SimpleRexRemap;
import org.apache.drill.exec.planner.logical.DrillMergeProjectRule;
import org.apache.drill.exec.planner.logical.DrillParseContext;
import org.apache.drill.exec.planner.physical.FilterPrel;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.physical.Prule;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.calcite.rel.InvalidRelException;

import org.apache.calcite.rel.RelNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generate a covering index plan that is equivalent to the original plan.
 *
 * This plan will be further optimized by the filter pushdown rule of the Index plugin which should
 * push this filter into the index scan.
 */
public class CoveringIndexPlanGenerator extends AbstractIndexPlanGenerator {

  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CoveringIndexPlanGenerator.class);
  final protected IndexGroupScan indexGroupScan;
  final protected IndexDescriptor indexDesc;

  // Ideally This functionInfo should be cached along with indexDesc.
  final protected FunctionalIndexInfo functionInfo;

  public CoveringIndexPlanGenerator(IndexLogicalPlanCallContext indexContext,
                                    FunctionalIndexInfo functionInfo,
                                    IndexGroupScan indexGroupScan,
                                    RexNode indexCondition,
                                    RexNode remainderCondition,
                                    RexBuilder builder,
                                    PlannerSettings settings) {
    super(indexContext, indexCondition, remainderCondition, builder, settings);
    this.indexGroupScan = indexGroupScan;
    this.functionInfo = functionInfo;
    this.indexDesc = functionInfo.getIndexDesc();
  }

  /**
   *
   * @param inputIndex
   * @param functionInfo functional index information that may impact rewrite
   * @return
   */
  private RexNode rewriteFunctionalCondition(RexNode inputIndex, RelDataType newRowType,
                                             FunctionalIndexInfo functionInfo) {
    if (!functionInfo.hasFunctional()) {
      return inputIndex;
    }
    return FunctionalIndexHelper.convertConditionForIndexScan(inputIndex,
        origScan, newRowType, builder, functionInfo);
  }

  @Override
  public RelNode convertChild(final RelNode filter, final RelNode input) throws InvalidRelException {

    if (indexGroupScan == null) {
      logger.error("Null indexgroupScan in CoveringIndexPlanGenerator.convertChild");
      return null;
    }

    RexNode coveringCondition;
    ScanPrel indexScanPrel =
        IndexPlanUtils.buildCoveringIndexScan(origScan, indexGroupScan, indexContext, indexDesc);

    // If remainder condition, then combine the index and remainder conditions. This is a covering plan so we can
    // pushed the entire condition into the index.
    coveringCondition = IndexPlanUtils.getTotalFilter(indexCondition, remainderCondition, indexScanPrel.getCluster().getRexBuilder());
    RexNode newIndexCondition =
        rewriteFunctionalCondition(coveringCondition, indexScanPrel.getRowType(), functionInfo);

    // build collation for filter
    RelTraitSet indexFilterTraitSet = indexScanPrel.getTraitSet();

    FilterPrel indexFilterPrel = new FilterPrel(indexScanPrel.getCluster(), indexFilterTraitSet,
        indexScanPrel, newIndexCondition);

    ProjectPrel indexProjectPrel = null;
    if (origProject != null) {
      RelCollation collation = IndexPlanUtils.buildCollationProject(IndexPlanUtils.getProjects(origProject), null,
          origScan, functionInfo, indexContext);
      indexProjectPrel = new ProjectPrel(origScan.getCluster(), indexFilterTraitSet.plus(collation),
          indexFilterPrel, IndexPlanUtils.getProjects(origProject), origProject.getRowType());
    }

    RelNode finalRel;
    if (indexProjectPrel != null) {
      finalRel = indexProjectPrel;
    } else {
      finalRel = indexFilterPrel;
    }

    if (upperProject != null) {
      RelCollation newCollation =
          IndexPlanUtils.buildCollationProject(IndexPlanUtils.getProjects(upperProject), origProject,
              origScan, functionInfo, indexContext);

      ProjectPrel cap = new ProjectPrel(upperProject.getCluster(),
          newCollation==null?finalRel.getTraitSet() : finalRel.getTraitSet().plus(newCollation),
          finalRel, IndexPlanUtils.getProjects(upperProject), upperProject.getRowType());

      if (functionInfo.hasFunctional()) {
        //if there is functional index field, then a rewrite may be needed in upperProject/indexProject
        //merge upperProject with indexProjectPrel(from origProject) if both exist,
        ProjectPrel newProject = cap;
        if (indexProjectPrel != null) {
          newProject = (ProjectPrel) DrillMergeProjectRule.replace(newProject, indexProjectPrel);
        }
        // then rewrite functional expressions in new project.
        List<RexNode> newProjects = Lists.newArrayList();
        DrillParseContext parseContxt = new DrillParseContext(PrelUtil.getPlannerSettings(newProject.getCluster()));
        for(RexNode projectRex: newProject.getProjects()) {
          RexNode newRex = IndexPlanUtils.rewriteFunctionalRex(indexContext, parseContxt, null, origScan, projectRex, indexScanPrel.getRowType(), functionInfo);
          newProjects.add(newRex);
        }

        ProjectPrel rewrittenProject = new ProjectPrel(newProject.getCluster(),
            newCollation==null? newProject.getTraitSet() : newProject.getTraitSet().plus(newCollation),
            indexFilterPrel, newProjects, newProject.getRowType());

        cap = rewrittenProject;
      }

      finalRel = cap;
    }

    if (indexContext.getSort() != null) {
      finalRel = getSortNode(indexContext, finalRel, false,true, true);
      Preconditions.checkArgument(finalRel != null);
    }

    finalRel = Prule.convert(finalRel, finalRel.getTraitSet().plus(Prel.DRILL_PHYSICAL));

    logger.debug("CoveringIndexPlanGenerator got finalRel {} from origScan {}, original digest {}, new digest {}.",
        finalRel.toString(), origScan.toString(),
        upperProject==null?indexContext.getFilter().getDigest(): upperProject.getDigest(), finalRel.getDigest());
    return finalRel;
  }

  private RexNode rewriteConditionForProject(RexNode condition, List<RexNode> projects) {
    Map<RexNode, RexNode> mapping = new HashMap<>();
    rewriteConditionForProjectInternal(condition, projects, mapping);
    SimpleRexRemap.RexReplace replacer = new SimpleRexRemap.RexReplace(mapping);
    return condition.accept(replacer);
  }

  private void rewriteConditionForProjectInternal(RexNode condition, List<RexNode> projects, Map<RexNode, RexNode> mapping) {
    if (condition instanceof RexCall) {
      if ("ITEM".equals(((RexCall) condition).getOperator().getName().toUpperCase())) {
        int index = 0;
        for (RexNode project : projects) {
          if (project.toString().equals(condition.toString())) {
            // Map it to the corresponding RexInputRef for the project
            mapping.put(condition, new RexInputRef(index, project.getType()));
          }
          ++index;
        }
      } else {
        for (RexNode child : ((RexCall) condition).getOperands()) {
          rewriteConditionForProjectInternal(child, projects, mapping);
        }
      }
    }
  }
}
