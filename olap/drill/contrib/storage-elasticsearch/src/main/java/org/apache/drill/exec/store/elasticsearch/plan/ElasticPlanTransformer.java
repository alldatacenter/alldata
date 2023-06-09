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
import org.apache.calcite.adapter.elasticsearch.DrillElasticsearchTableScan;
import org.apache.calcite.adapter.elasticsearch.ElasticsearchAggregate;
import org.apache.calcite.adapter.elasticsearch.ElasticsearchFilter;
import org.apache.calcite.adapter.elasticsearch.ElasticsearchProject;
import org.apache.calcite.adapter.elasticsearch.ElasticsearchSort;
import org.apache.calcite.adapter.elasticsearch.ElasticsearchTable;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.prepare.RelOptTableImpl;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelShuttleImpl;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rel.type.RelRecordType;
import org.apache.calcite.rel.type.StructKind;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexShuttle;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.SqlTypeName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementation of RelShuttleImpl that transforms plan to fit Calcite ElasticSearch rel implementor.
 */
public class ElasticPlanTransformer extends RelShuttleImpl {

  private boolean hasProject = false;

  private RelDataTypeField mapField;

  /**
   * Replaces rowType of RelOptTable by rowType obtained from ElasticsearchTable.
   */
  @Override
  public RelNode visit(TableScan other) {
    RelOptTableImpl table = (RelOptTableImpl) other.getTable();
    ElasticsearchTable elasticsearchTable = Objects.requireNonNull(
        table.unwrap(ElasticsearchTable.class), "ElasticSearch table cannot be null");
    RelDataType rowType = elasticsearchTable.getRowType(other.getCluster().getTypeFactory());
    mapField = rowType.getFieldList().get(0);
    return new DrillElasticsearchTableScan(other.getCluster(), other.getTraitSet(), table.copy(rowType), elasticsearchTable, rowType);
  }

  @Override
  public RelNode visit(RelNode other) {
    // replaces project expressions with ITEM calls, since Calcite returns single map column `_MAP`
    // with actual table fields
    if (other instanceof ElasticsearchProject) {
      ElasticsearchProject project = (ElasticsearchProject) other;
      RelNode input = project.getInput().accept(this);
      List<RexNode> convertedExpressions = project.getProjects();
      // project closest to the scan should be rewritten only
      if (!this.hasProject) {
        ElasticExpressionMapper expressionMapper =
            new ElasticExpressionMapper(project.getCluster().getRexBuilder(),
                project.getInput().getRowType(), mapField);
        convertedExpressions = convertedExpressions.stream()
            .map(expression -> expression.accept(expressionMapper))
            .collect(Collectors.toList());

        RelRecordType relDataType = getRelRecordType(other.getRowType());
        this.hasProject = true;
        return CalciteUtils.createProject(project.getTraitSet(), input,
            convertedExpressions, relDataType);
      } else {
        return input;
      }
    } else if (other instanceof ElasticsearchFilter) {
      ElasticsearchFilter filter = (ElasticsearchFilter) other;
      RexNode convertedCondition = filter.getCondition().accept(
          new ElasticExpressionMapper(other.getCluster().getRexBuilder(), filter.getInput().getRowType(), mapField));
      return filter.copy(other.getTraitSet(), filter.getInput().accept(this), convertedCondition);
    } else if (other instanceof ElasticsearchSort) {
      ElasticsearchSort sort = (ElasticsearchSort) other;
      RelNode input = getMappedInput(sort.getInput());
      return sort.copy(other.getTraitSet(), input, sort.getCollation(), sort.offset, sort.fetch);
    } else if (other instanceof ElasticsearchAggregate) {
      ElasticsearchAggregate aggregate = (ElasticsearchAggregate) other;
      RelNode input = getMappedInput(aggregate.getInput());
      return aggregate.copy(other.getTraitSet(), input, aggregate.getGroupSet(),
          aggregate.getGroupSets(), aggregate.getAggCallList());
    }

    return super.visit(other);
  }

  /**
   * Generates project with mapped expressions above specified rel node
   * if there is no other project in the tree.
   */
  private RelNode getMappedInput(RelNode relNode) {
    boolean hasProject = this.hasProject;
    this.hasProject = false;
    RelNode input = relNode.accept(this);
    if (!this.hasProject) {
      this.hasProject = hasProject;
      RelOptCluster cluster = relNode.getCluster();
      List<RexNode> projections = IntStream.range(0, relNode.getRowType().getFieldCount())
          .mapToObj(i -> cluster.getRexBuilder().makeInputRef(relNode, i))
          .collect(Collectors.toList());

      return CalciteUtils.createProject(relNode.getTraitSet(), relNode,
          projections, relNode.getRowType()).accept(this);
    } else {
      return input;
    }
  }

  private RelRecordType getRelRecordType(RelDataType rowType) {
    List<RelDataTypeField> fields = new ArrayList<>();
    for (RelDataTypeField relDataTypeField : rowType.getFieldList()) {
      if (relDataTypeField.isDynamicStar()) {
        fields.add(mapField);
      } else {
        fields.add(relDataTypeField);
      }
    }

    return new RelRecordType(StructKind.FULLY_QUALIFIED, fields, false);
  }

  /**
   * Implementation of RexShuttle that replaces RexInputRef expressions with ITEM calls to _MAP field.
   */
  public static class ElasticExpressionMapper extends RexShuttle {
    private final RexBuilder rexBuilder;
    private final RelDataType relDataType;
    private final RelDataTypeField mapField;

    public ElasticExpressionMapper(RexBuilder rexBuilder, RelDataType relDataType, RelDataTypeField mapField) {
      this.rexBuilder = rexBuilder;
      this.relDataType = relDataType;
      this.mapField = mapField;
    }

    @Override
    public RexNode visitInputRef(RexInputRef inputRef) {
      if (inputRef.getType().getSqlTypeName() == SqlTypeName.DYNAMIC_STAR) {
        return rexBuilder.makeInputRef(mapField.getType(), 0);
      }
      return rexBuilder.makeCall(SqlStdOperatorTable.ITEM, rexBuilder.makeInputRef(relDataType, 0),
          rexBuilder.makeLiteral(relDataType.getFieldNames().get(inputRef.getIndex())));
    }
  }
}
