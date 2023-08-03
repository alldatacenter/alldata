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

#include <Interpreters/CancellationCode.h>
#include <Interpreters/Context.h>
#include <Interpreters/DistributedStages/PlanSegmentProcessList.h>
#include <Interpreters/ProcessList.h>
#include <Interpreters/DistributedStages/AddressInfo.h>
#include <IO/WriteBufferFromString.h>
#include <Common/Exception.h>

#include <memory>
#include <mutex>
#include <string>
#include <utility>
#include <vector>
namespace DB
{
namespace ErrorCodes
{
    extern const int TOO_MANY_SIMULTANEOUS_QUERIES;
    extern const int QUERY_WITH_SAME_ID_IS_ALREADY_RUNNING;
    extern const int LOGICAL_ERROR;
    extern const int QUERY_WAS_CANCELLED;
}

PlanSegmentProcessList::EntryPtr
PlanSegmentProcessList::insert(const PlanSegment & plan_segment, ContextMutablePtr query_context, bool force)
{
    const String & initial_query_id = plan_segment.getQueryId();
    const String & segment_id_str = std::to_string(plan_segment.getPlanSegmentId());
    const String & coordinator_address = extractExchangeStatusHostPort(plan_segment.getCoordinatorAddress());
    std::vector<ProcessList::EntryPtr> need_to_cancel_entries;

    auto initial_query_start_time_ms = query_context->getClientInfo().initial_query_start_time_microseconds;
    {
        std::lock_guard lock(mutex);
        const auto segment_group_it = initail_query_to_groups.find(initial_query_id);
        if (segment_group_it != initail_query_to_groups.end())
        {
            if (segment_group_it->second.coordinator_address != coordinator_address
                && segment_group_it->second.initial_query_start_time_ms <= initial_query_start_time_ms)
            {
                if (!force && !query_context->getSettingsRef().replace_running_query)
                    throw Exception(
                        "Distributed query with id = " + initial_query_id + " is already running.",
                        ErrorCodes::QUERY_WITH_SAME_ID_IS_ALREADY_RUNNING);

                LOG_WARNING(
                    logger,
                    "Distributed query with id = {} will be replaced by other coordinator: {}",
                    initial_query_id,
                    plan_segment.getCoordinatorAddress().toString());
                for (auto & segment_query : segment_group_it->second.segment_queries)
                {
                    need_to_cancel_entries.push_back(segment_query.second);
                }
            }
        }
    }

    for (auto & query : need_to_cancel_entries)
        query->get().cancelQuery(true);

    bool need_wait_cancel = !need_to_cancel_entries.empty();
    need_to_cancel_entries.clear();

    WriteBufferFromOwnString pipeline_buffer;
    QueryPlan::ExplainPlanOptions options;
    plan_segment.getQueryPlan().explainPlan(pipeline_buffer, options);
    String pipeline_string = pipeline_buffer.str();

    ProcessList::EntryPtr entry;
    auto context_process_list_entry = query_context->getProcessListEntry().lock();
    if (context_process_list_entry)
        entry = std::move(context_process_list_entry);
    else
        entry = query_context->getProcessList().insert("\n" + pipeline_string, nullptr, query_context, force);

    auto res = std::make_unique<PlanSegmentProcessListEntry>(*this, &entry->get(), initial_query_id, plan_segment.getPlanSegmentId());

    std::unique_lock lock(mutex);
    if (need_wait_cancel)
    {
        const auto replace_running_query_max_wait_ms
            = query_context->getSettingsRef().replace_running_query_max_wait_ms.totalMilliseconds();
        if (!replace_running_query_max_wait_ms
            || !remove_group.wait_for(lock, std::chrono::milliseconds(replace_running_query_max_wait_ms), [&] {
                   auto it = initail_query_to_groups.find(initial_query_id);
                   return it == initail_query_to_groups.end() || it->second.coordinator_address == coordinator_address;
               }))
        {
            throw Exception(
                "Distributed query with id = " + initial_query_id + " is already running and can't be stopped",
                ErrorCodes::QUERY_WITH_SAME_ID_IS_ALREADY_RUNNING);
        }
    }

    const auto segment_group_it = initail_query_to_groups.find(initial_query_id);

    if (segment_group_it == initail_query_to_groups.end())
    {
        PlanSegmentGroup segment_group{
            .coordinator_address = extractExchangeStatusHostPort(plan_segment.getCoordinatorAddress()),
            .initial_query_start_time_ms = initial_query_start_time_ms,
            .segment_queries = {{plan_segment.getPlanSegmentId(), std::move(entry)}}};
        initail_query_to_groups.emplace(initial_query_id, std::move(segment_group));
        return res;
    }

    if (segment_group_it->second.coordinator_address != coordinator_address)
    {
        throw Exception(
            "Distributed query with id = " + initial_query_id + " is already running and can't be stopped",
            ErrorCodes::QUERY_WITH_SAME_ID_IS_ALREADY_RUNNING);
    }

    const auto emplace_res = segment_group_it->second.segment_queries.emplace(std::make_pair(plan_segment.getPlanSegmentId(), std::move(entry)));
    if (!emplace_res.second)
    {
        throw Exception("Exsited segment_id: " + segment_id_str + " for query: " + initial_query_id, ErrorCodes::LOGICAL_ERROR);
    }
    return res;
}


CancellationCode PlanSegmentProcessList::tryCancelPlanSegmentGroup(const String & initial_query_id, String coordinator_address)
{
    std::vector<ProcessList::EntryPtr> need_cancalled_queries;
    {
        std::lock_guard lock(mutex);
        auto segment_group_it = initail_query_to_groups.find(initial_query_id);
        if (segment_group_it != initail_query_to_groups.end())
        {
            if (!coordinator_address.empty() && segment_group_it->second.coordinator_address != coordinator_address)
            {
                LOG_WARNING(
                    logger,
                    "Fail to cancel distributed query[{}@{}], coordinator_address doesn't match, seg coordinator address is {}",
                    initial_query_id,
                    coordinator_address,
                    segment_group_it->second.coordinator_address
                    );

                return CancellationCode::CancelCannotBeSent;
            }

            for (auto & segment_query : segment_group_it->second.segment_queries)
            {
                need_cancalled_queries.push_back(segment_query.second);
            }
        }
    }

    if (need_cancalled_queries.empty())
    {
        return CancellationCode::NotFound;
    }

    auto res = CancellationCode::CancelSent;
    for (auto & query : need_cancalled_queries)
    {
        try
        {
            query->get().cancelQuery(true);
        }
        catch (...)
        {
            LOG_WARNING(
                logger,
                "Fail to cancel distributed query[{}@{}] by error: {}",
                initial_query_id,
                coordinator_address,
                getCurrentExceptionMessage(false));
            res = CancellationCode::CancelCannotBeSent;
        }
    }

    return res;
}

PlanSegmentProcessListEntry::PlanSegmentProcessListEntry(
    PlanSegmentProcessList & parent_, QueryStatus * status_, String initial_query_id_, size_t segment_id_)
    : parent(parent_), status(status_), initial_query_id(std::move(initial_query_id_)), segment_id(segment_id_)
{
}

PlanSegmentProcessListEntry::~PlanSegmentProcessListEntry()
{
    ProcessList::EntryPtr found_entry;
    {
        std::unique_lock lock(parent.mutex);

        const auto segment_group_it = parent.initail_query_to_groups.find(initial_query_id);

        if (segment_group_it == parent.initail_query_to_groups.end())
        {
            LOG_ERROR(parent.logger, "Logical error: Cannot found query: {} in PlanSegmentProcessList", initial_query_id);
            std::terminate();
        }

        PlanSegmentGroup & segment_group = segment_group_it->second;

        if (auto running_query = segment_group.segment_queries.find(segment_id); running_query != segment_group.segment_queries.end())
        {
            if (running_query->second && &running_query->second->get() == status)
            {
                found_entry = std::move(running_query->second);

                segment_group.segment_queries.erase(segment_id);
                LOG_TRACE(
                    parent.logger,
                    "Remove segment {} for distributed query {}@{} from PlanSegmentProcessList",
                    segment_id,
                    initial_query_id,
                    segment_group.coordinator_address);
            }
        }

        if (segment_group.segment_queries.empty())
        {
            LOG_TRACE(
                parent.logger, "Remove segment group for distributed query {}@{}", initial_query_id, segment_group.coordinator_address);
            parent.initail_query_to_groups.erase(segment_group_it);
            parent.remove_group.notify_all();
        }
    }

    if (found_entry)
        found_entry.reset();
}
}
