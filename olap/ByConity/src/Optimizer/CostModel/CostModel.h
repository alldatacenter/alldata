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

#pragma once

#include <Interpreters/Context.h>

namespace DB
{
class CostModel
{
public:
    static constexpr double CPU_COST_RATIO = 0.74;
    static constexpr double NET_COST_RATIO = 0.16;
    static constexpr double MEM_COST_RATIO = 0.1;

    explicit CostModel(const Context & context) : context_settings(context.getSettingsRef()) { }

    double getAggregateCostWeight() const { return context_settings.cost_calculator_aggregating_weight; }

    double getJoinProbeSideCostWeight() const { return context_settings.cost_calculator_join_probe_weight; }
    double getJoinBuildSideCostWeight() const { return context_settings.cost_calculator_join_build_weight; }
    double getJoinOutputCostWeight() const { return context_settings.cost_calculator_join_output_weight; }

    double getTableScanCostWeight() const { return context_settings.cost_calculator_table_scan_weight; }

    double getCTECostWeight() const { return context_settings.cost_calculator_cte_weight; }

    double getProjectionCostWeight() const { return context_settings.cost_calculator_projection_weight; }

private:
    const Settings & context_settings;
};

}
