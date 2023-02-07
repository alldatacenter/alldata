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
package org.apache.drill.exec.planner.physical;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelCollations;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.common.DrillWriterRelBase;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.logical.DrillWriterRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait.DistributionField;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait.DistributionType;

import java.util.List;

public class WriterPrule extends Prule{
  public static final RelOptRule INSTANCE = new WriterPrule();

  public WriterPrule() {
    super(RelOptHelper.some(DrillWriterRel.class, DrillRel.DRILL_LOGICAL, RelOptHelper.any(RelNode.class)),
        "Prel.WriterPrule");
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    final DrillWriterRel writer = call.rel(0);
    final RelNode input = call.rel(1);

    final List<Integer> keys = writer.getPartitionKeys();
    final RelCollation collation = getCollation(keys);
    final boolean hashDistribute = PrelUtil.getPlannerSettings(call.getPlanner()).getOptions().getOption(ExecConstants.CTAS_PARTITIONING_HASH_DISTRIBUTE_VALIDATOR);
    final RelTraitSet traits = hashDistribute ?
        input.getTraitSet().plus(Prel.DRILL_PHYSICAL).plus(collation).plus(getDistribution(keys)) :
        input.getTraitSet().plus(Prel.DRILL_PHYSICAL).plus(collation);

    final RelNode convertedInput = convert(input, traits);

    if (!new WriteTraitPull(call).go(writer, convertedInput)) {
      DrillWriterRelBase newWriter = new WriterPrel(writer.getCluster(), convertedInput.getTraitSet(),
          convertedInput, writer.getCreateTableEntry());

      call.transformTo(newWriter);
    }
  }

  private RelCollation getCollation(List<Integer> keys){
    List<RelFieldCollation> fields = Lists.newArrayList();
    for (int key : keys) {
      fields.add(new RelFieldCollation(key));
    }
    return RelCollations.of(fields);
  }

  private DrillDistributionTrait getDistribution(List<Integer> keys) {
    List<DistributionField> fields = Lists.newArrayList();
    for (int key : keys) {
      fields.add(new DistributionField(key));
    }
    return new DrillDistributionTrait(DistributionType.HASH_DISTRIBUTED, ImmutableList.copyOf(fields));
  }

  private class WriteTraitPull extends SubsetTransformer<DrillWriterRelBase, RuntimeException> {

    public WriteTraitPull(RelOptRuleCall call) {
      super(call);
    }

    @Override
    public RelNode convertChild(DrillWriterRelBase writer, RelNode rel) throws RuntimeException {
      DrillDistributionTrait childDist = rel.getTraitSet().getTrait(DrillDistributionTraitDef.INSTANCE);

      // Create the Writer with the child's distribution because the degree of parallelism for the writer
      // should correspond to the number of child minor fragments. The Writer itself is not concerned with
      // the collation of the child.  Note that the Writer's output RowType consists of
      // {fragment_id varchar(255), number_of_records_written bigint} which are very different from the
      // child's output RowType.
      return new WriterPrel(writer.getCluster(),
          writer.getTraitSet().plus(childDist).plus(Prel.DRILL_PHYSICAL),
          rel, writer.getCreateTableEntry());
    }

  }
}
