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

namespace DB
{
class PlanNodeStatistics;
using PlanNodeStatisticsPtr = std::shared_ptr<PlanNodeStatistics>;

class PlanNodeStatisticsEstimate
{
public:
    PlanNodeStatisticsEstimate() : statistics(), derived(false) { }
    PlanNodeStatisticsEstimate(std::optional<PlanNodeStatisticsPtr> statistics_) : statistics(std::move(statistics_)), derived(true) { }

    const std::optional<PlanNodeStatisticsPtr> & getStatistics() const { return statistics; }
    bool has_value() const { return statistics.has_value(); }
    PlanNodeStatisticsPtr value_or(const PlanNodeStatisticsPtr & value) const { return statistics.value_or(value); }
    const PlanNodeStatisticsPtr & value() const { return statistics.value(); }
    bool isDerived() const { return derived; }

    explicit operator bool() const { return has_value(); }
    bool operator!() const { return !this->operator bool(); }

private:
    std::optional<PlanNodeStatisticsPtr> statistics;
    bool derived;
};

}
