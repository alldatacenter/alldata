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

#include <Optimizer/CostModel/ExchangeCost.h>

#include <Optimizer/CostModel/CostCalculator.h>
#include <QueryPlan/ExchangeStep.h>

namespace DB
{
PlanNodeCost ExchangeCost::calculate(const ExchangeStep & step, CostContext & context)
{
    // if shuffle cost is bigger then no shuffle.
    double base_cost = 1;

    // more shuffle keys is better than less shuffle keys.
    // todo data skew
    if (!step.getSchema().getPartitioningColumns().empty() && step.getSchema().getPartitioningHandle() == Partitioning::Handle::FIXED_HASH)
        base_cost += 1.0 / step.getSchema().getPartitioningColumns().size();

    if (!context.stats)
        return PlanNodeCost::netCost(base_cost);

    if (step.getSchema().getPartitioningHandle() == Partitioning::Handle::FIXED_ARBITRARY)
        return PlanNodeCost::ZERO;

    auto single_worker_cost = context.stats->getRowCount() + base_cost;
    return PlanNodeCost::netCost(
        step.getSchema().getPartitioningHandle() == Partitioning::Handle::FIXED_BROADCAST ? single_worker_cost * context.worker_size
                                                                                          : single_worker_cost);
}

}
