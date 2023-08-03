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

#include <Optimizer/CardinalityEstimate/AssignUniqueIdEstimator.h>

namespace DB
{
PlanNodeStatisticsPtr AssignUniqueIdEstimator::estimate(PlanNodeStatisticsPtr & child_stats, const AssignUniqueIdStep & step)
{
    if (!child_stats)
    {
        return nullptr;
    }

    String unique_symbol = step.getUniqueId();
    auto unique_stats = std::make_shared<SymbolStatistics>(child_stats->getRowCount(), 0, 0, 0, 8);

    std::unordered_map<String, SymbolStatisticsPtr> symbol_statistics;
    for (auto & stats : child_stats->getSymbolStatistics())
    {
        symbol_statistics[stats.first] = stats.second->copy();
    }
    symbol_statistics[unique_symbol] = unique_stats;

    auto stats = std::make_shared<PlanNodeStatistics>(child_stats->getRowCount(), std::move(symbol_statistics));
    return stats;
}

}
