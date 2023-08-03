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

#include <Interpreters/RuntimeFilter/RuntimeFilterManager.h>

#include <Optimizer/DynamicFilters.h>
#include <Optimizer/PlanNodeSearcher.h>
#include <QueryPlan/FilterStep.h>
#include <QueryPlan/IQueryPlanStep.h>
#include <QueryPlan/ProjectionStep.h>
#include <QueryPlan/QueryPlan.h>

namespace DB
{
size_t PartialRuntimeFilter::merge(RuntimeFilterPtr runtime_filter_, const String & address)
{
    std::lock_guard guard(mutex);
    if (runtime_filter_)
    {
        if (!runtime_filter)
            runtime_filter = std::move(runtime_filter_);
        else
            runtime_filter->merge(*runtime_filter_);
        merged_address_set.emplace(address);
    }
    return merged_address_set.size();
}

RuntimeFilterManager & RuntimeFilterManager::getInstance()
{
    static RuntimeFilterManager ret;
    return ret;
}

static String toString(const PartialRuntimeFilters & filters)
{
    std::ostringstream ss;
    for (const auto & item : filters)
    {
        ss << "" << item.first << ": [";
        auto it = item.second->getExecuteSegmentIds().begin();
        auto end = item.second->getExecuteSegmentIds().end();
        if (it != end)
            ss << *it;
        for (it++; it != end; it++)
            ss << ", " << *it;
        ss << "]; ";
    }
    return ss.str();
}

void RuntimeFilterManager::registerQuery(const String & query_id, PlanSegmentTree & plan_segment_tree)
{
    std::unordered_map<DynamicFilterId, size_t> id_to_parallel_size_map;
    std::unordered_map<DynamicFilterId, std::unordered_set<size_t>> id_to_execute_plan_segments_map;
    for (auto & plan_segment_node : plan_segment_tree.getNodes())
    {
        auto & plan_segment = *plan_segment_node.getPlanSegment();
        auto segment_id = plan_segment.getPlanSegmentId();
        auto parallel_size = plan_segment.getParallelSize();

        for (const auto & node : plan_segment.getQueryPlan().getNodes())
        {
            if (node.step->getType() == IQueryPlanStep::Type::Projection)
            {
                const auto & projection_step = static_cast<const ProjectionStep &>(*node.step);
                for (const auto & item : projection_step.getDynamicFilters())
                {
                    auto it = id_to_parallel_size_map.find(item.second.id);
                    if (it == id_to_parallel_size_map.end())
                        id_to_parallel_size_map[item.second.id] = parallel_size;
                }
            }
            else if (node.step->getType() == IQueryPlanStep::Type::Filter)
            {
                const auto & filter_step = static_cast<const FilterStep &>(*node.step);
                if (filter_step.getFilter())
                {
                    auto filters = DynamicFilters::extractDynamicFilters(filter_step.getFilter());
                    for (const auto & dynamic_filter : filters.first)
                    {
                        auto dynamic_filter_id = DynamicFilters::extractId(dynamic_filter);
                        id_to_execute_plan_segments_map[dynamic_filter_id].emplace(segment_id);
                    }
                }
            }
        }
    }

    PartialRuntimeFilters runtime_filters;
    for (auto & item : id_to_parallel_size_map)
    {
        auto & plan_segments = id_to_execute_plan_segments_map[item.first];
        std::vector<size_t> plan_segment_ids{plan_segments.begin(), plan_segments.end()};
        runtime_filters.emplace(item.first, std::make_shared<PartialRuntimeFilter>(item.second, plan_segment_ids));
    }
    LOG_DEBUG(log, "Register query {}. {}", query_id, toString(runtime_filters));

    partial_runtime_filters.put(query_id, std::make_shared<PartialRuntimeFilters>(std::move(runtime_filters)));
}

void RuntimeFilterManager::removeQuery(const String & query_id)
{
    partial_runtime_filters.remove(query_id);
}

PartialRuntimeFilterPtr RuntimeFilterManager::getPartialRuntimeFilter(const String & query_id, RuntimeFilterId filter_id)
{
    auto partial_runtime_filter_map = partial_runtime_filters.get(query_id);
    auto partial_runtime_filter = partial_runtime_filter_map->find(filter_id);
    if (partial_runtime_filter == partial_runtime_filter_map->end())
        throw Exception();
    return partial_runtime_filter->second;
}

void RuntimeFilterManager::putRuntimeFilter(
    const String & query_id, size_t segment_id, RuntimeFilterId filter_id, RuntimeFilterPtr & runtime_filter)
{
    complete_runtime_filters
        .compute(
            makeKey(query_id, segment_id, filter_id),
            [](const auto &, CompleteRuntimeFilterPtr value) {
                if (!value)
                    return std::make_shared<CompleteRuntimeFilter>();
                return value;
            })
        ->set(runtime_filter);
}

RuntimeFilterPtr
RuntimeFilterManager::getRuntimeFilter(const String & query_id, size_t segment_id, RuntimeFilterId filter_id, size_t timeout_ms)
{
    return getRuntimeFilter(makeKey(query_id, segment_id, filter_id), timeout_ms);
}

RuntimeFilterPtr RuntimeFilterManager::getRuntimeFilter(const String & key, size_t timeout_ms)
{
    return complete_runtime_filters
        .compute(
            key,
            [](const auto &, CompleteRuntimeFilterPtr value) {
                if (!value)
                    return std::make_shared<CompleteRuntimeFilter>();
                return value;
            })
        ->get(timeout_ms);
}

void RuntimeFilterManager::removeRuntimeFilter(const String & query_id, size_t segment_id, RuntimeFilterId filter_id)
{
    complete_runtime_filters.remove(makeKey(query_id, segment_id, filter_id));
}

String RuntimeFilterManager::makeKey(const String & query_id, size_t segment_id, RuntimeFilterId filter_id)
{
    return query_id + "_" + std::to_string(segment_id) + "_" + std::to_string(filter_id);
}

}
