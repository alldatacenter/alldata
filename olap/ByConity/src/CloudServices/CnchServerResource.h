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
#include <Catalog/DataModelPartWrapper_fwd.h>
#include <CloudServices/CnchWorkerClient.h>
#include <Common/HostWithPorts.h>
#include <Core/Types.h>
#include <Interpreters/StorageID.h>
#include <Interpreters/WorkerGroupHandle.h>
#include <Storages/IStorage_fwd.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>
#include <Storages/Hive/HiveDataPart_fwd.h>
#include <Transaction/TxnTimestamp.h>
#include <Poco/Logger.h>


namespace DB
{

class ServerResourceLockManager
{
public:
    void remove(const String & address)
    {
        std::lock_guard lock(mutex);
        hosts.erase(address);
        cv.notify_one();
    }

    void add(const String & address)
    {
        std::unique_lock lock(mutex);
        cv.wait(lock, [&]() { return !hosts.count(address); });
        hosts.emplace(address);
    }

private:
    std::mutex mutex;
    std::condition_variable cv;
    std::unordered_set<std::string> hosts;
};

struct SendLock
{
    SendLock(const std::string & address_, ServerResourceLockManager & manager_)
        : address(address_), manager(manager_)
    {
        manager.add(address);
    }

    ~SendLock()
    {
        manager.remove(address);
    }

    std::string address;
    ServerResourceLockManager & manager;
};

struct AssignedResource
{
    explicit AssignedResource(const StoragePtr & storage);

    StoragePtr storage;
    String worker_table_name;
    String create_table_query;
    bool sent_create_query{false};

    /// offloading info
    HostWithPortsVec buffer_workers;

    /// parts info
    ServerDataPartsVector server_parts;
    HiveDataPartsCNCHVector hive_parts;
    std::set<Int64> bucket_numbers;

    std::unordered_set<String> part_names;

    void addDataParts(const ServerDataPartsVector & parts);
    void addDataParts(const HiveDataPartsCNCHVector & parts);

    bool empty() const { return sent_create_query && server_parts.empty(); }
};

class CnchServerResource
{
public:
    explicit CnchServerResource(TxnTimestamp curr_txn_id)
        : txn_id(curr_txn_id)
        , log(&Poco::Logger::get("SessionResource(" + txn_id.toString() + ")"))
    {}

    ~CnchServerResource();

    void addCreateQuery(const ContextPtr & context, const StoragePtr & storage, const String & create_query, const String & worker_table_name);
    void setAggregateWorker(HostWithPorts aggregate_worker_)
    {
        aggregate_worker = std::move(aggregate_worker_);
    }

    void setWorkerGroup(WorkerGroupHandle worker_group_)
    {
        if (!worker_group)
            worker_group = std::move(worker_group_);
    }

    void skipCleanWorker() { skip_clean_worker = true; }

    template <typename T>
    void addDataParts(const UUID & storage_id, const std::vector<T> & data_parts, const std::set<Int64> & required_bucket_numbers = {})
    {
        std::lock_guard lock(mutex);
        auto & assigned_resource = assigned_table_resource.at(storage_id);

        assigned_resource.addDataParts(data_parts);
        if (assigned_resource.bucket_numbers.empty() && !required_bucket_numbers.empty())
            assigned_resource.bucket_numbers = required_bucket_numbers;
    }

    void addBufferWorkers(const UUID & storage_id, const HostWithPortsVec & buffer_workers);

    /// Send resource to worker
    void sendResource(const ContextPtr & context, const HostWithPorts & worker);
    /// allocate and send resource to worker_group
    void sendResource(const ContextPtr & context);

    /// WorkerAction should not throw
    using WorkerAction = std::function<std::vector<brpc::CallId>(CnchWorkerClientPtr, std::vector<AssignedResource> &, ExceptionHandler &)>;
    void sendResource(const ContextPtr & context, WorkerAction act);

    /// remove all resource in server
    void removeAll();

private:
    auto getLock() const { return std::lock_guard(mutex); }
    auto getLockForSend(const String & address) const { return SendLock{address, lock_manager}; }
    void cleanTaskInWorker(bool clean_resource = false) const;

    /// move resource from assigned_table_resource to assigned_worker_resource
    void allocateResource(const ContextPtr & context, std::lock_guard<std::mutex> &);

    void sendCreateQueries(const ContextPtr & context);
    void sendDataParts(const ContextPtr & context);
    void sendOffloadingInfo(const ContextPtr & context);

    TxnTimestamp txn_id;
    mutable std::mutex mutex; /// mutex for manager resource

    WorkerGroupHandle worker_group;
    HostWithPorts aggregate_worker;

    /// storage_uuid, assigned_resource
    std::unordered_map<UUID, AssignedResource> assigned_table_resource;
    std::unordered_map<HostWithPorts, std::vector<AssignedResource>> assigned_worker_resource;

    bool skip_clean_worker{false};
    Poco::Logger * log;
    mutable ServerResourceLockManager lock_manager;
};

}
