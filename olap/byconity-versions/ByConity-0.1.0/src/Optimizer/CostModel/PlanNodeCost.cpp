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

#include <Optimizer/CostModel/CostModel.h>
#include <Optimizer/CostModel/PlanNodeCost.h>

namespace DB
{
PlanNodeCost PlanNodeCost::ZERO(0.0, 0.0, 0.0);

double PlanNodeCost::getCost() const
{
    return cpu_value * CostModel::CPU_COST_RATIO + mem_value * CostModel::MEM_COST_RATIO + net_value * CostModel::NET_COST_RATIO;
}

}
