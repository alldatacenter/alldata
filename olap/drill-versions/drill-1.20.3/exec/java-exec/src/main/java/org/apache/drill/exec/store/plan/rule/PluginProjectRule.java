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
package org.apache.drill.exec.store.plan.rule;

import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.rules.ProjectRemoveRule;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.index.ExprToRex;
import org.apache.drill.exec.planner.logical.DrillProjectRel;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.store.plan.PluginImplementor;
import org.apache.drill.exec.store.plan.rel.PluginProjectRel;
import org.apache.drill.exec.util.Utilities;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The rule that converts provided project operator to plugin-specific implementation.
 */
public class PluginProjectRule extends PluginConverterRule {

  public PluginProjectRule(RelTrait in, Convention out, PluginImplementor pluginImplementor) {
    super(Project.class, in, out, "PluginProjectRule", pluginImplementor);
  }

  @Override
  public RelNode convert(RelNode rel) {
    Project project = (Project) rel;

    if (!getPluginImplementor().splitProject(project)) {
      return new PluginProjectRel(
        getOutConvention(),
        project.getCluster(),
        project.getTraitSet().replace(getOutConvention()),
        convert(project.getInput(), project.getTraitSet().replace(getOutConvention())),
        project.getProjects(),
        project.getRowType());
    }

    RelDataType inputRowType = project.getInput().getRowType();
    if (inputRowType.getFieldList().isEmpty()) {
      return null;
    }

    DrillRelOptUtil.ProjectPushInfo projectPushInfo =
      DrillRelOptUtil.getFieldsInformation(inputRowType, project.getProjects());
    Project pluginProject = createPluginProject(project, projectPushInfo);
    if (Utilities.isStarQuery(projectPushInfo.getFields()) ||
      pluginProject.getRowType().equals(inputRowType)) {
      return null;
    }

    List<RexNode> newProjects = project.getChildExps().stream()
      .map(n -> n.accept(projectPushInfo.getInputReWriter()))
      .collect(Collectors.toList());

    Project newProject =
      createProject(project, pluginProject, newProjects);

    if (ProjectRemoveRule.isTrivial(newProject)) {
      return pluginProject;
    } else {
      return newProject;
    }
  }

  protected Project createPluginProject(Project project, DrillRelOptUtil.ProjectPushInfo projectPushInfo) {
    ExprToRex exprToRex = new ExprToRex(project.getInput(), project.getInput().getRowType(), project.getCluster().getRexBuilder());
    List<RexNode> newProjects = projectPushInfo.getFields().stream()
      .map(f -> f.accept(exprToRex, null))
      .collect(Collectors.toList());
    return new PluginProjectRel(
      getOutConvention(),
      project.getCluster(),
      project.getTraitSet().replace(getOutConvention()),
      convert(project.getInput(), project.getTraitSet().replace(getOutConvention())),
      newProjects,
      projectPushInfo.createNewRowType(project.getCluster().getTypeFactory()));
  }

  protected Project createProject(Project project, Project input, List<RexNode> newProjects) {
    return new DrillProjectRel(project.getCluster(),
      project.getTraitSet().plus(DrillRel.DRILL_LOGICAL),
      input,
      newProjects,
      project.getRowType());
  }
}
