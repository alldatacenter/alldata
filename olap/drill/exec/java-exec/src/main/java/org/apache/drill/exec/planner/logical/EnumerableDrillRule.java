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
package org.apache.drill.exec.planner.logical;

import org.apache.calcite.adapter.enumerable.EnumerableConvention;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;

/**
 * Rule that converts any Drill relational expression to enumerable format by adding a {@link DrillScreenRelBase}.
 */
public class EnumerableDrillRule extends ConverterRule {

  public static EnumerableDrillRule INSTANCE = new EnumerableDrillRule();


  private EnumerableDrillRule() {
    super(RelNode.class, DrillRel.DRILL_LOGICAL, EnumerableConvention.INSTANCE, "EnumerableDrillRule.");
  }

  @Override
  public boolean isGuaranteed() {
    return true;
  }

  @Override
  public RelNode convert(RelNode rel) {
    assert rel.getTraitSet().contains(DrillRel.DRILL_LOGICAL);
    return new DrillScreenRel(rel.getCluster(), rel.getTraitSet().replace(getOutConvention()), rel);
  }
}
