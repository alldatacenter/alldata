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
package org.apache.drill.exec.store.cassandra.plan;

import org.apache.calcite.adapter.cassandra.CassandraLimit;
import org.apache.calcite.adapter.cassandra.CassandraRel;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.apache.drill.exec.planner.common.DrillLimitRelBase;
import org.apache.drill.exec.planner.logical.DrillRel;

public class DrillCassandraLimitRule extends ConverterRule {
  public static final DrillCassandraLimitRule INSTANCE = new DrillCassandraLimitRule();

  private DrillCassandraLimitRule() {
    super(DrillLimitRelBase.class, DrillRel.DRILL_LOGICAL, CassandraRel.CONVENTION,
        "DrillCassandraLimitRule");
  }

  public RelNode convert(RelNode relNode) {
    DrillLimitRelBase limit = (DrillLimitRelBase) relNode;
    final RelTraitSet traitSet =
        limit.getTraitSet().replace(CassandraRel.CONVENTION);
    return new CassandraLimit(limit.getCluster(), traitSet,
        convert(limit.getInput(), CassandraRel.CONVENTION), limit.getOffset(), limit.getFetch());
  }
}
