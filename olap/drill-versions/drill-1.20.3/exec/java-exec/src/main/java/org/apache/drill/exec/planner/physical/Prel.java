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

import java.io.IOException;
import java.util.List;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.common.DrillRelNode;
import org.apache.drill.exec.planner.physical.visitor.PrelVisitor;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;

public interface Prel extends DrillRelNode, Iterable<Prel> {

  Convention DRILL_PHYSICAL = new Convention.Impl("PHYSICAL", Prel.class) {
    @Override
    public boolean canConvertConvention(Convention toConvention) {
      return true;
    }

    @Override
    public boolean useAbstractConvertersForConversion(RelTraitSet fromTraits,
        RelTraitSet toTraits) {
      return true;
    }
  };

  PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException;

  <T, X, E extends Throwable> T accept(PrelVisitor<T, X, E> logicalVisitor, X value) throws E;

  /**
   * Supported 'encodings' of a Prel indicates what are the acceptable modes of SelectionVector
   * of its child Prel
   */
  SelectionVectorMode[] getSupportedEncodings();

  /**
   * A Prel's own SelectionVector mode - i.e whether it generates an SV2, SV4 or None
   */
  SelectionVectorMode getEncoding();
  boolean needsFinalColumnReordering();

  /**
   * If the operator is in Lateral/Unnest pipeline, then it generates a new operator which knows how to process
   * the rows accordingly during execution.
   * eg: TopNPrel -> SortPrel and LimitPrel
   * Other operators like FilterPrel, ProjectPrel etc will add an implicit row id to the output.
   */
  default Prel prepareForLateralUnnestPipeline(List<RelNode> children) {
    throw new UnsupportedOperationException("Adding Implicit RowID column is not supported for " +
            this.getClass().getSimpleName() + " operator ");
  }
}
