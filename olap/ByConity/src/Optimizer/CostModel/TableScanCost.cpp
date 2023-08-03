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

#include <Optimizer/CostModel/CostCalculator.h>
#include <Optimizer/CostModel/TableScanCost.h>

namespace DB
{

PlanNodeCost TableScanCost::calculate(const TableScanStep &, CostContext & context)
{
    if (!context.stats)
        return PlanNodeCost::ZERO;
    return PlanNodeCost::cpuCost(context.stats->getRowCount()) * context.cost_model.getTableScanCostWeight();
}

}
