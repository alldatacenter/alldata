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

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.physical.base.IndexGroupScan;
import org.apache.drill.exec.planner.index.IndexCallContext;
import org.apache.drill.exec.planner.index.IndexDescriptor;
import org.apache.drill.exec.planner.index.FunctionalIndexInfo;
import org.apache.drill.exec.planner.index.IndexPlanUtils;
import org.apache.drill.exec.planner.logical.DrillParseContext;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.planner.physical.Prule;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CoveringPlanNoFilterGenerator extends AbstractIndexPlanGenerator {

  private static final Logger logger = LoggerFactory.getLogger(CoveringPlanNoFilterGenerator.class);

  final protected IndexGroupScan indexGroupScan;
  final protected IndexDescriptor indexDesc;
  final boolean isSingletonSortedStream;
  // Ideally This functionInfo should be cached along with indexDesc.
  final protected FunctionalIndexInfo functionInfo;

  public CoveringPlanNoFilterGenerator(IndexCallContext indexContext,
                                       FunctionalIndexInfo functionInfo,
                                       boolean isSingleton,
                                       PlannerSettings settings) {
    super(indexContext, null, null, null, settings);
    this.functionInfo = functionInfo;
    this.indexDesc = functionInfo == null ? null : functionInfo.getIndexDesc();
    this.indexGroupScan = functionInfo == null ? null : functionInfo.getIndexDesc().getIndexGroupScan();
    this.isSingletonSortedStream = isSingleton;
  }

  public RelNode convertChild(final RelNode filter, final RelNode input) throws InvalidRelException {
      return this.convertChild();
  }

  public RelNode convertChild() throws InvalidRelException {
    Preconditions.checkNotNull(indexContext.getSort());
    if (indexGroupScan == null) {
      logger.error("Null indexgroupScan in CoveringIndexPlanGenerator.convertChild");
      return null;
    }
    //update sort expressions in context
    IndexPlanUtils.updateSortExpression(indexContext, indexContext.getSort() != null ?
            indexContext.getCollation().getFieldCollations() : null);

    ScanPrel indexScanPrel =
            IndexPlanUtils.buildCoveringIndexScan(origScan, indexGroupScan, indexContext, indexDesc);
    ((IndexGroupScan)indexScanPrel.getGroupScan()).setStatistics(((DbGroupScan)IndexPlanUtils.getGroupScan(origScan)).getStatistics());
    RelTraitSet indexScanTraitSet = indexScanPrel.getTraitSet();

    RelNode finalRel = indexScanPrel;
    if (indexContext.getLowerProject() != null) {

      RelCollation collation = IndexPlanUtils.buildCollationProject(indexContext.getLowerProject().getProjects(), null,
              indexContext.getScan(), functionInfo, indexContext);
      finalRel = new ProjectPrel(indexContext.getScan().getCluster(), indexScanTraitSet.plus(collation),
              indexScanPrel, indexContext.getLowerProject().getProjects(), indexContext.getLowerProject().getRowType());

      if (functionInfo.hasFunctional()) {
        //if there is functional index field, then a rewrite may be needed in upperProject/indexProject
        //merge upperProject with indexProjectPrel(from origProject) if both exist,
        ProjectPrel newProject = (ProjectPrel)finalRel;

        // then rewrite functional expressions in new project.
        List<RexNode> newProjects = Lists.newArrayList();
        DrillParseContext parseContxt = new DrillParseContext(PrelUtil.getPlannerSettings(newProject.getCluster()));
        for(RexNode projectRex: newProject.getProjects()) {
          RexNode newRex = IndexPlanUtils.rewriteFunctionalRex(indexContext, parseContxt, null, origScan, projectRex,
                  indexScanPrel.getRowType(), functionInfo);
          newProjects.add(newRex);
        }

        ProjectPrel rewrittenProject = new ProjectPrel(newProject.getCluster(),
                collation==null? newProject.getTraitSet() : newProject.getTraitSet().plus(collation),
                indexScanPrel, newProjects, newProject.getRowType());

        finalRel = rewrittenProject;
      }
    }

    finalRel = getSortNode(indexContext, finalRel, true, isSingletonSortedStream, indexContext.getExchange() != null);
    if (finalRel == null) {
      return null;
    }

    finalRel = Prule.convert(finalRel, finalRel.getTraitSet().plus(Prel.DRILL_PHYSICAL));

    logger.debug("CoveringPlanNoFilterGenerator got finalRel {} from origScan {}, original digest {}, new digest {}.",
            finalRel.toString(), indexContext.getScan().toString(),
            indexContext.getLowerProject()!=null?indexContext.getLowerProject().getDigest(): indexContext.getScan().getDigest(),
            finalRel.getDigest());
    return finalRel;
  }
}

