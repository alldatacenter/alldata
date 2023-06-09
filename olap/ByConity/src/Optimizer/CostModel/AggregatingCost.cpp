/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <Optimizer/CostModel/AggregatingCost.h>

#include <Optimizer/CostModel/CostCalculator.h>
#include <QueryPlan/AggregatingStep.h>

namespace DB
{
PlanNodeCost AggregatingCost::calculate(const AggregatingStep & step, CostContext & context)
{
    PlanNodeStatisticsPtr stats = context.stats;
    if (!step.isFinal())
        return PlanNodeCost::ZERO;

    PlanNodeStatisticsPtr children_stats = context.children_stats[0];

    if (!stats || !children_stats)
        return PlanNodeCost::ZERO;

    PlanNodeCost input_cost = PlanNodeCost::cpuCost(children_stats->getRowCount());
    PlanNodeCost build_cost = PlanNodeCost::cpuCost(stats->getRowCount()) * context.cost_model.getAggregateCostWeight();

    PlanNodeCost mem_cost = PlanNodeCost::memCost(stats->getRowCount() * step.getAggregates().size());
    return input_cost + build_cost + mem_cost;
}
}

