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
package org.apache.drill.exec.store.enumerable.plan;

import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.metadata.RelMetadataQuery;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface EnumMockRel extends RelNode {

  class MockEnumerablePrelContext implements EnumerablePrelContext {

    private final Convention convention;

    public MockEnumerablePrelContext(Convention convention) {
      this.convention = convention;
    }

    @Override
    public String generateCode(RelOptCluster cluster, RelNode relNode) {
      return null;
    }

    @Override
    public RelNode transformNode(RelNode input) {
      assert input.getTraitSet().getTrait(0).equals(convention) : "Conventions should match";
      return input;
    }

    @Override
    public Map<String, Integer> getFieldsMap(RelNode transformedNode) {
      return Collections.emptyMap();
    }

    @Override
    public String getPlanPrefix() {
      return "mock_prel";
    }

    @Override
    public String getTablePath(RelNode input) {
      return null;
    }
  }

  class EnumMockTableScan extends TableScan implements EnumMockRel {

    protected EnumMockTableScan(RelOptCluster cluster, RelTraitSet traitSet, RelOptTable table) {
      super(cluster, traitSet, table);
    }

    @Override
    public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
      assert inputs.isEmpty();
      return new EnumMockTableScan(getCluster(), traitSet, table);
    }

    @Override
    public double estimateRowCount(RelMetadataQuery mq) {
      return 5;
    }
  }
}
