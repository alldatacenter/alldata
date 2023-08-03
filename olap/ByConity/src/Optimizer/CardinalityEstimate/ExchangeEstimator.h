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
#include <Optimizer/CardinalityEstimate/PlanNodeStatistics.h>
#include <QueryPlan/ExchangeStep.h>

namespace DB
{
class ExchangeEstimator
{
public:
    static PlanNodeStatisticsPtr estimate(std::vector<PlanNodeStatisticsPtr> & child_stats, const ExchangeStep & step);

private:
    static PlanNodeStatisticsPtr
    mapToOutput(PlanNodeStatisticsPtr & child_stats, const std::unordered_map<String, std::vector<String>> & out_to_input, size_t index);
};
};
