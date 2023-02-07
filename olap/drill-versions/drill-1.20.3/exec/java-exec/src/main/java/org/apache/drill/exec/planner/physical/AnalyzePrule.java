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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.SingleRel;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.metastore.statistics.Statistic;
import org.apache.drill.exec.planner.logical.DrillAnalyzeRel;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

public class AnalyzePrule extends Prule {

  public static final RelOptRule INSTANCE = new AnalyzePrule();
  // List of output functions (from StatsAggBatch)
  private static final List<String> PHASE_1_FUNCTIONS = ImmutableList.of(
      Statistic.ROWCOUNT,    // total number of entries in table fragment
      Statistic.NNROWCOUNT,  // total number of non-null entries in table fragment
      Statistic.SUM_WIDTH,   // total column width across all entries in table fragment
      Statistic.CNT_DUPS,    // total count of non-singletons in table fragment
      Statistic.HLL,         // total distinct values in table fragment
      Statistic.TDIGEST      // quantile distribution of values in table fragment
    );

  // Mapping between output functions (from StatsMergeBatch) and
  // input functions (from StatsAggBatch)
  private static Map<String, String> PHASE_2_FUNCTIONS = new HashMap<>();
  static {
    PHASE_2_FUNCTIONS.put(Statistic.ROWCOUNT, Statistic.ROWCOUNT);
    PHASE_2_FUNCTIONS.put(Statistic.NNROWCOUNT, Statistic.NNROWCOUNT);
    PHASE_2_FUNCTIONS.put(Statistic.AVG_WIDTH, Statistic.SUM_WIDTH);
    PHASE_2_FUNCTIONS.put(Statistic.SUM_DUPS, Statistic.CNT_DUPS);
    PHASE_2_FUNCTIONS.put(Statistic.HLL_MERGE, Statistic.HLL);
    PHASE_2_FUNCTIONS.put(Statistic.NDV, Statistic.HLL);
    PHASE_2_FUNCTIONS.put(Statistic.TDIGEST_MERGE, Statistic.TDIGEST);
  }

  // List of input functions (from StatsMergeBatch) to UnpivotMapsBatch
  private static final List<String> UNPIVOT_FUNCTIONS = ImmutableList.of(
      Statistic.ROWCOUNT,    // total number of entries in the table
      Statistic.NNROWCOUNT,  // total number of non-null entries in the table
      Statistic.AVG_WIDTH,   // average column width across all entries in the table
      Statistic.HLL_MERGE,   // total distinct values(computed using hll) in the table
      Statistic.SUM_DUPS,    // total count of duplicate values across all entries in the table
      Statistic.NDV,         // total distinct values across all entries in the table
      Statistic.TDIGEST_MERGE // quantile distribution of all values in the table
  );

  public AnalyzePrule() {
    super(RelOptHelper.some(DrillAnalyzeRel.class, DrillRel.DRILL_LOGICAL,
        RelOptHelper.any(RelNode.class)), "Prel.AnalyzePrule");
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    final DrillAnalyzeRel analyze = call.rel(0);
    final RelNode input = call.rel(1);
    final SingleRel newAnalyze;
    final RelTraitSet singleDistTrait = call.getPlanner().emptyTraitSet().plus(Prel.DRILL_PHYSICAL)
        .plus(DrillDistributionTrait.SINGLETON);

    // Generate parallel ANALYZE plan:
    // Writer<-Unpivot<-StatsAgg(Phase2)<-Exchange<-StatsAgg(Phase1)<-Scan
    final RelTraitSet traits = input.getTraitSet().plus(Prel.DRILL_PHYSICAL).
        plus(DrillDistributionTrait.DEFAULT);
    RelNode convertedInput = convert(input, traits);

    final List<String> mapFields1 = Lists.newArrayList(PHASE_1_FUNCTIONS);
    final Map<String, String> mapFields2 = Maps.newHashMap(PHASE_2_FUNCTIONS);
    final List<String> mapFields3 = Lists.newArrayList(UNPIVOT_FUNCTIONS);
    mapFields1.add(0, Statistic.COLNAME);
    mapFields1.add(1, Statistic.COLTYPE);
    mapFields2.put(Statistic.COLNAME, Statistic.COLNAME);
    mapFields2.put(Statistic.COLTYPE, Statistic.COLTYPE);
    mapFields3.add(0, Statistic.COLNAME);
    mapFields3.add(1, Statistic.COLTYPE);
    // Now generate the two phase plan physical operators bottom-up:
    // STATSAGG->EXCHANGE->STATSMERGE->UNPIVOT
    if (analyze.getSamplePercent() < 100.0) {
      // If a sample samplePercent is specified add a filter for Bernoulli sampling
      RexBuilder builder = convertedInput.getCluster().getRexBuilder();
      RexNode sampleCondition;
      if (PrelUtil.getSettings(convertedInput.getCluster()).getOptions().getOption(ExecConstants.DETERMINISTIC_SAMPLING_VALIDATOR)) {
        sampleCondition = builder.makeCall(SqlStdOperatorTable.LESS_THAN_OR_EQUAL,
            builder.makeCall(SqlStdOperatorTable.RAND, builder.makeExactLiteral(BigDecimal.valueOf(1))),
            builder.makeExactLiteral(BigDecimal.valueOf(analyze.getSamplePercent()/100.0)));
      } else {
        sampleCondition = builder.makeCall(SqlStdOperatorTable.LESS_THAN_OR_EQUAL,
            builder.makeCall(SqlStdOperatorTable.RAND),
            builder.makeExactLiteral(BigDecimal.valueOf(analyze.getSamplePercent()/100.0)));
      }
      convertedInput = new FilterPrel(convertedInput.getCluster(), convertedInput.getTraitSet(),
              convertedInput, sampleCondition);
    }
    final StatsAggPrel statsAggPrel = new StatsAggPrel(analyze.getCluster(), traits,
        convertedInput, PHASE_1_FUNCTIONS);
    UnionExchangePrel exch = new UnionExchangePrel(statsAggPrel.getCluster(), singleDistTrait,
        statsAggPrel);
    final StatsMergePrel statsMergePrel = new StatsMergePrel(exch.getCluster(), singleDistTrait,
        exch, mapFields2, analyze.getSamplePercent());
    newAnalyze = new UnpivotMapsPrel(statsMergePrel.getCluster(), singleDistTrait, statsMergePrel,
        mapFields3);
    call.transformTo(newAnalyze);
  }
}
