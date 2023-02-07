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
package org.apache.drill.exec.store.elasticsearch.plan;

import org.apache.calcite.adapter.elasticsearch.CalciteUtils;
import org.apache.calcite.adapter.elasticsearch.ElasticsearchProject;
import org.apache.calcite.adapter.elasticsearch.ElasticsearchRel;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule for converting Drill project to ElasticSearch project.
 * This rule contains a logic to split the project if it would have expressions
 * and convert only a simple project to ElasticSearch project.
 */
public class ElasticsearchProjectRule extends ConverterRule {
  private final Convention out;

  public static final ElasticsearchProjectRule INSTANCE = new ElasticsearchProjectRule();

  private ElasticsearchProjectRule() {
    super(Project.class, Convention.NONE, ElasticsearchRel.CONVENTION,
        "DrillElasticsearchProjectRule");
    this.out = ElasticsearchRel.CONVENTION;
  }

  @Override
  public RelNode convert(RelNode relNode) {
    Project project = (Project) relNode;
    NodeTypeFinder projectFinder = new NodeTypeFinder(ElasticsearchProject.class);
    project.getInput().accept(projectFinder);
    if (projectFinder.containsNode) {
      // Calcite adapter allows only a single Elasticsearch project per tree
      return null;
    }
    RelTraitSet traitSet = project.getTraitSet().replace(out);
    List<RexNode> innerProjections = new ArrayList<>();
    RelDataType rowType = project.getInput().getRowType();

    // check for literals only without input exprs
    DrillRelOptUtil.InputRefVisitor collectRefs = new DrillRelOptUtil.InputRefVisitor();
    project.getChildExps().forEach(exp -> exp.accept(collectRefs));

    if (!collectRefs.getInputRefs().isEmpty()) {
      for (RelDataTypeField relDataTypeField : rowType.getFieldList()) {
        innerProjections.add(project.getCluster().getRexBuilder().makeInputRef(project.getInput(), relDataTypeField.getIndex()));
      }
    }

    boolean allExprsInputRefs = project.getChildExps().stream().allMatch(rexNode -> rexNode instanceof RexInputRef);
    if (collectRefs.getInputRefs().isEmpty() || allExprsInputRefs) {
      return CalciteUtils.createProject(traitSet,
          convert(project.getInput(), out), project.getProjects(), project.getRowType());
    } else {
      Project elasticsearchProject = CalciteUtils.createProject(traitSet,
          convert(project.getInput(), out), innerProjections, project.getInput().getRowType());
      return project.copy(project.getTraitSet(), elasticsearchProject,
          project.getProjects(), project.getRowType());
    }
  }

}
