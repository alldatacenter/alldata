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

#include <Optimizer/Rule/Pattern.h>
#include <QueryPlan/IQueryPlanStep.h>

#include <sstream>

namespace DB::Patterns
{

// typeOf patterns
inline PatternPtr typeOf(IQueryPlanStep::Type type) { return std::make_shared<TypeOfPattern>(type); }
inline PatternPtr any() { return typeOf(IQueryPlanStep::Type::Any); }
inline PatternPtr tree() { return typeOf(IQueryPlanStep::Type::Tree); }

inline PatternPtr project() { return typeOf(IQueryPlanStep::Type::Projection); }
inline PatternPtr filter() { return typeOf(IQueryPlanStep::Type::Filter); }
inline PatternPtr join() { return typeOf(IQueryPlanStep::Type::Join); }
inline PatternPtr aggregating() { return typeOf(IQueryPlanStep::Type::Aggregating); }
inline PatternPtr window() { return typeOf(IQueryPlanStep::Type::Window); }
inline PatternPtr mergingAggregated() { return typeOf(IQueryPlanStep::Type::MergingAggregated); }
inline PatternPtr unionn() { return typeOf(IQueryPlanStep::Type::Union); }
inline PatternPtr intersect() { return typeOf(IQueryPlanStep::Type::Intersect); }
inline PatternPtr except() { return typeOf(IQueryPlanStep::Type::Except); }
inline PatternPtr exchange() { return typeOf(IQueryPlanStep::Type::Exchange); }
inline PatternPtr remoteSource() { return typeOf(IQueryPlanStep::Type::RemoteExchangeSource); }
inline PatternPtr tableScan() { return typeOf(IQueryPlanStep::Type::TableScan); }
inline PatternPtr readNothing() { return typeOf(IQueryPlanStep::Type::ReadNothing); }
inline PatternPtr limit() { return typeOf(IQueryPlanStep::Type::Limit); }
inline PatternPtr limitBy() { return typeOf(IQueryPlanStep::Type::LimitBy); }
inline PatternPtr sorting() { return typeOf(IQueryPlanStep::Type::Sorting); }
inline PatternPtr mergeSorting() { return typeOf(IQueryPlanStep::Type::MergeSorting); }
inline PatternPtr partialSorting() { return typeOf(IQueryPlanStep::Type::PartialSorting); }
inline PatternPtr mergingSorted() { return typeOf(IQueryPlanStep::Type::MergingSorted); }
//inline PatternPtr materializing() { return typeOf(IQueryPlanStep::Type::Materializing); }
inline PatternPtr distinct() { return typeOf(IQueryPlanStep::Type::Distinct); }
inline PatternPtr extremes() { return typeOf(IQueryPlanStep::Type::Extremes); }
inline PatternPtr apply() { return typeOf(IQueryPlanStep::Type::Apply); }
inline PatternPtr enforceSingleRow() { return typeOf(IQueryPlanStep::Type::EnforceSingleRow); }
inline PatternPtr assignUniqueId() { return typeOf(IQueryPlanStep::Type::AssignUniqueId); }
inline PatternPtr cte() { return typeOf(IQueryPlanStep::Type::CTERef); }

// miscellaneous
inline PatternPredicate predicateNot(const PatternPredicate & predicate)
{
    return [=](const PlanNodePtr & node, const Captures & captures) -> bool {return !predicate(node, captures);};
}

}
