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

#include <Common/CGroup/CpuController.h>
#include <Common/Exception.h>
#include <Common/ThreadPool.h>
#include <Core/Types.h>
#include <Interpreters/Context.h>
#include <Interpreters/Context_fwd.h>

#include <atomic>
#include <condition_variable>
#include <deque>
#include <list>
#include <memory>
#include <mutex>
#include <unordered_map>

namespace DB
{
enum class ResourceGroupType
{
    Internal,
    VirtualWarehouse,
};

enum class QueryStatusType
{
    WAITING,
    RUNNING
};

class QueryStatus;
class Context;

struct ResourceGroupInfo
{
    String name;
    UInt8 can_run_more;
    UInt8 can_queue_more;
    Int64 soft_max_memory_usage;
    Int32 max_concurrent_queries;
    Int32 max_queued;
    Int32 max_queued_waiting_ms;
    Int32 priority;
    String parent_resource_group;
    Int64 cached_memory_usage_bytes;
    UInt32 running_queries;
    UInt32 queued_queries;
    UInt64 last_used;
    Int32 cpu_shares;
    bool in_use;
    UInt64 queued_time_total_ms;
    UInt64 running_time_total_ms;
};

/** Class responsible for resource management during query execution
  * Currently supports limiting running queries and queuing queries
  * This class also stores resource-group related metrics.
  * Resource groups can be viewed as a tree structure, where
  * each resource group has a certain filter(as specified by ResourceSelectCase)
  * and its children groups are used for further filters
  */
class IResourceGroup
{
public:
    // Struct that is used to manage and monitor a query's status (QueryStatusType)
    struct QueryEntity
    {
    public:
        QueryEntity(
            IResourceGroup * group_,
            const String & query_,
            const Context & query_context_,
            QueryStatusType status_type_ = QueryStatusType::WAITING);

        bool operator==(const QueryEntity & other) { return id == other.id; }

        IResourceGroup * group;
        String query;
        const ContextPtr query_context;
        QueryStatusType status_type = QueryStatusType::WAITING;
        Int32 id;
        UInt64 queue_timestamp;
        /// set after run
        QueryStatus * query_status;
    };
    using Element = std::shared_ptr<QueryEntity>;
    using Container = std::list<Element>;

    struct QueryEntityHandler
    {
    private:
        Container::iterator it;

    public:
        QueryEntityHandler(Container::iterator it_) : it(it_) { }
        ~QueryEntityHandler()
        {
            IResourceGroup * group = (*it)->group;
            group->queryFinished(it);
        }
    };
    using Handle = std::shared_ptr<QueryEntityHandler>;

    virtual ResourceGroupType getType() const = 0;
    virtual bool canRunMore() const = 0;
    virtual bool canQueueMore() const = 0;
    virtual ~IResourceGroup() {}

    std::lock_guard<std::mutex> getLock() const {return std::lock_guard<std::mutex>(root->mutex);}

    Container::iterator run(const String & query, const Context & query_context);
    Handle insert(Container::iterator it) { return std::make_shared<QueryEntityHandler>(it); }
    void processQueuedQueues();
    bool isLeaf() const { return children.empty(); }
    ResourceGroupInfo getInfo() const;

    IResourceGroup * getParent() const;
    void setParent(IResourceGroup * parent);

    IResourceGroup * getRoot() const;
    void setRoot();

    Int64 getSoftMaxMemoryUsage() const { return soft_max_memory_usage; }
    void setSoftMaxMemoryUsage(Int64 softMaxMemoryUsage) { soft_max_memory_usage = softMaxMemoryUsage; }

    Int64 getMinQueryMemoryUsage() const { return min_query_memory_usage; }
    void setMinQueryMemoryUsage(Int64 minQueryMemoryUsage) { min_query_memory_usage = minQueryMemoryUsage; }

    Int32 getMaxConcurrentQueries() const { return max_concurrent_queries; }
    void setMaxConcurrentQueries(Int32 maxConcurrentQueries) { max_concurrent_queries = maxConcurrentQueries; }

    Int32 getMaxQueued() const { return max_queued; }
    void setMaxQueued(Int32 maxQueued) { max_queued = maxQueued; }

    Int32 getMaxQueuedWaitingMs() const { return max_queued_waiting_ms; }
    void setMaxQueuedWaitingMs(Int32 maxQueuedWaitingMs) { max_queued_waiting_ms = maxQueuedWaitingMs; }

    Int32 getPriority() const { return priority; }
    void setPriority(Int32 priority_) { priority = priority_; }

    UInt64 getLastUsed() const { return last_used; }
    void setLastUsed(UInt64 last_used_) { last_used.store(last_used_, std::memory_order_release); }

    UInt64 getInUse() const { return in_use; }
    void setInUse(bool in_use_)
    {
        if (in_use.load(std::memory_order_consume) != in_use_)
            in_use.store(in_use_, std::memory_order_release);
    }

    const String & getName() const { return name; }
    void setName(const String & name_) { name = name_; }

    const String & getParentName() const { return parent_name; }
    void setParentName(const String & parent_name_) { parent_name = parent_name_; }

    UInt64 getQueuedTime() const { return queued_time_total_ms; }
    void setQueuedTime(UInt64 queued_time_total_ms_)  { queued_time_total_ms = queued_time_total_ms_; }

    UInt64 getRunningTime() const { return running_time_total_ms; }
    void setRunningTime(UInt64 running_time_total_ms_)  { running_time_total_ms = running_time_total_ms_; }

    FreeThreadPool * getThreadPool() const { return thread_pool ? thread_pool.get() : nullptr; }

protected:
    IResourceGroup() = default;

    // Updates total amount of time spent queueing and running queries of each resource group
    void internalUpdateQueryTime();
    // Updates memory usage of resource group
    void internalRefreshStats();
    // Executes queue management, i.e. running of queued queries
    bool internalProcessNext();
    // Updates resource group when a query has finished
    void queryFinished(Container::iterator entityIt);
    Container::iterator enqueueQuery(Element & element);
    Container::iterator runQuery(Element & element);


    String name;
    String parent_name;
    IResourceGroup * parent = nullptr;
    IResourceGroup * root = nullptr;
    std::unordered_map<String, IResourceGroup *> children;

    Int64 soft_max_memory_usage{0};
    Int64 min_query_memory_usage{0};
    Int32 max_concurrent_queries{0};
    Int32 max_queued{0};
    Int32 max_queued_waiting_ms{0};
    Int32 priority{0};
    std::atomic<Int64> last_used = 0;
    std::atomic<UInt64> queued_time_total_ms = 0;
    std::atomic<UInt64> running_time_total_ms = 0;
    std::atomic<bool> in_use = true;
    std::list<IResourceGroup *> eligible_groups;
    std::list<IResourceGroup *>::iterator eligible_iterator;
    Container running_queries;
    Int32 descendent_running_queries = 0;
    Container queued_queries;
    Int32 descendent_queued_queries = 0;
    Int64 cached_memory_usage_bytes = 0;
    std::atomic<Int32> id{0};
    std::shared_ptr<FreeThreadPool> thread_pool{nullptr};

    mutable std::mutex mutex;
    mutable std::condition_variable_any can_run;
};

}
