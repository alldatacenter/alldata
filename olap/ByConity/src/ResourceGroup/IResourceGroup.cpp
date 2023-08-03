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

#include <Common/Exception.h>
#include <Core/Settings.h>
#include <Interpreters/Context.h>
#include <Interpreters/Context_fwd.h>
#include <ResourceGroup/IResourceGroup.h>
#include <ResourceGroup/VWResourceGroupManager.h>
#include <Interpreters/ProcessList.h>
#include <chrono>

namespace ProfileEvents
{
    extern const Event InsufficientConcurrencyQuery;
}

namespace DB
{

namespace ErrorCodes
{
    extern const int RESOURCE_NOT_ENOUGH;
    extern const int WAIT_FOR_RESOURCE_TIMEOUT;
    extern const int RESOURCE_GROUP_INTERNAL_ERROR;
}

IResourceGroup* IResourceGroup::getParent() const
{
    return parent;
}

void IResourceGroup::setParent(IResourceGroup * parent_)
{
    parent = parent_;
    root = parent_->root;
    parent_->children[name] = this;
}

IResourceGroup::QueryEntity::QueryEntity(
    IResourceGroup * group_,
    const String & query_,
    const Context & query_context_,
    QueryStatusType status_type_)
    : group(group_)
    , query(query_)
    , query_context(&query_context_)
    , status_type(status_type_)
    , id(group->root->id.fetch_add(1, std::memory_order_relaxed)) {}

void IResourceGroup::queryFinished(IResourceGroup::Container::iterator it)
{
    std::lock_guard lock(root->mutex);
    running_queries.erase(it);
    IResourceGroup * group = parent;
    while (group)
    {
        --group->descendent_running_queries;
        group->setInUse(true);
        group = group->parent;
    }
    setInUse(true);
}

IResourceGroup::Container::iterator IResourceGroup::run(const String & query, const Context & query_context)
{
    std::unique_lock lock(root->mutex);
    bool canRun = true;
    bool canQueue = true;
    IResourceGroup *group = this;
    while (group)
    {
        canRun &= group->canRunMore();
        canQueue &= group->canQueueMore();
        group = group->parent;
    }
    if (!canQueue && !canRun)
    {
        ProfileEvents::increment(ProfileEvents::InsufficientConcurrencyQuery);
        throw Exception("The resource is not enough for group " + name, ErrorCodes::RESOURCE_NOT_ENOUGH);
    }
    IResourceGroup::Element element = std::make_shared<IResourceGroup::QueryEntity>(this, query, query_context);
    if (canRun)
        return runQuery(element);
    auto it = enqueueQuery(element);

    if (!root->can_run.wait_for(lock,
                                std::chrono::milliseconds(max_queued_waiting_ms), [&]{ return element->status_type != QueryStatusType::WAITING;}))
    {
        queued_queries.erase(it);
        IResourceGroup * desc_group = parent;
        while (desc_group)
        {
            --desc_group->descendent_queued_queries;
            desc_group = desc_group->parent;
        }
        ProfileEvents::increment(ProfileEvents::InsufficientConcurrencyQuery);
        throw Exception("Waiting for resource timeout in " + name, ErrorCodes::WAIT_FOR_RESOURCE_TIMEOUT);
    }
    auto res = std::find(running_queries.begin(), running_queries.end(), element);
    if (res == running_queries.end())
        throw Exception("The running query can not be found in the resource group " + name, ErrorCodes::RESOURCE_GROUP_INTERNAL_ERROR);
    return res;
}

IResourceGroup::Container::iterator IResourceGroup::enqueueQuery(IResourceGroup::Element & element)
{
    element->queue_timestamp = element->query_context->getTimestamp();
    Container::iterator it = queued_queries.emplace(queued_queries.end(), element);
    IResourceGroup *group = parent;
    while (group)
    {
        ++group->descendent_queued_queries;
        group = group->parent;
        group->setInUse(true);
    }

    setInUse(true);
    return it;
}

IResourceGroup::Container::iterator IResourceGroup::runQuery(IResourceGroup::Element & element)
{
    element->query_status = nullptr;
    Container::iterator it = running_queries.emplace(running_queries.end(), element);
    element->status_type = QueryStatusType::RUNNING;
    cached_memory_usage_bytes += min_query_memory_usage;
    IResourceGroup *group = parent;
    while (group)
    {
        ++group->descendent_running_queries;
        group->cached_memory_usage_bytes += min_query_memory_usage;
        group = group->parent;

        group->setInUse(true);
    }
    setInUse(true);
    return it;
}

void IResourceGroup::internalUpdateQueryTime()
{
    // Only perform atomic operations if needed
    if (auto queued_num = queued_queries.size())
        queued_time_total_ms.fetch_add(queued_num);
    if (auto running_num = running_queries.size())
        running_time_total_ms.fetch_add(running_num);
}

void IResourceGroup::internalRefreshStats()
{
    Int64 newCacheMemoryUsage = 0, queryMemoryUsage;
    for (const auto & query : running_queries)
    {
        queryMemoryUsage = 0;
        if (query->query_status != nullptr)
            queryMemoryUsage = query->query_status->getUsedMemory();
        newCacheMemoryUsage += queryMemoryUsage < min_query_memory_usage ? min_query_memory_usage : queryMemoryUsage;
    }
    for (const auto & item : children)
    {
        item.second->internalRefreshStats();
        newCacheMemoryUsage += item.second->cached_memory_usage_bytes;
    }
    cached_memory_usage_bytes = newCacheMemoryUsage;
}

bool IResourceGroup::internalProcessNext()
{
    if (!canRunMore())
        return false;
    if (eligible_groups.size() != children.size() + 1)
    {
        eligible_groups.clear();
        eligible_groups.push_back(this);
        for (auto & item : children)
            eligible_groups.push_back(item.second);
        eligible_iterator = eligible_groups.begin();
    }
    size_t inEligibleGroupsNum = 0;
    while (inEligibleGroupsNum < eligible_groups.size())
    {
        if (eligible_iterator == eligible_groups.end())
            eligible_iterator = eligible_groups.begin();
        if (*eligible_iterator == this)
        {
            if (!queued_queries.empty())
            {
                runQuery(queued_queries.front());
                queued_queries.erase(queued_queries.begin());
                IResourceGroup *group = parent;
                while (group)
                {
                    --group->descendent_queued_queries;
                    group = group->parent;
                }
                ++eligible_iterator;
                return true;
            }
            else
            {
                ++eligible_iterator;
                ++inEligibleGroupsNum;
            }
        }
        else
        {
            if ((*eligible_iterator)->internalProcessNext())
            {
                ++eligible_iterator;
                return true;
            }
            else
            {
                ++eligible_iterator;
                ++inEligibleGroupsNum;
            }
        }
    }
    return false;
}

void IResourceGroup::setRoot()
{
    root = this;
}

IResourceGroup* IResourceGroup::getRoot() const
{
    return root;
}

void IResourceGroup::processQueuedQueues()
{
    std::lock_guard lock(root->mutex);
    internalUpdateQueryTime();
    internalRefreshStats();
    bool processed = false;
    while (internalProcessNext())
    {
        // process all
        processed = true;
    }
    if (processed) root->can_run.notify_all();
}

ResourceGroupInfo IResourceGroup::getInfo() const
{
    std::lock_guard lock(root->mutex);
    ResourceGroupInfo info;
    info.name = name;
    info.can_run_more = canRunMore();
    info.can_queue_more = canQueueMore();
    info.soft_max_memory_usage = soft_max_memory_usage;
    info.cached_memory_usage_bytes = cached_memory_usage_bytes;
    info.max_concurrent_queries = max_concurrent_queries;
    info.running_queries = running_queries.size() + descendent_running_queries;
    info.max_queued = max_queued;
    info.max_queued_waiting_ms = max_queued_waiting_ms;
    info.queued_queries = queued_queries.size() + descendent_queued_queries;
    info.priority = priority;
    info.parent_resource_group = parent_name;
    info.last_used = last_used.load(std::memory_order_consume);
    info.in_use = in_use.load(std::memory_order_consume);
    info.queued_time_total_ms = queued_time_total_ms.load(std::memory_order_consume);
    info.running_time_total_ms = running_time_total_ms.load(std::memory_order_consume);

    return info;
}

}
