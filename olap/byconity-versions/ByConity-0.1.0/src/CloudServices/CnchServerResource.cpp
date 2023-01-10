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

#include <CloudServices/CnchServerResource.h>

#include <Catalog/DataModelPartWrapper.h>
#include <CloudServices/CnchPartsHelper.h>
#include <CloudServices/CnchWorkerClient.h>
#include <CloudServices/CnchWorkerResource.h>
#include <Interpreters/Context.h>
#include <MergeTreeCommon/assignCnchParts.h>
#include <brpc/controller.h>
#include <Common/Exception.h>
#include "Catalog/DataModelPartWrapper_fwd.h"

#include <Storages/Hive/HiveDataPart.h>
#include <Storages/StorageCnchHive.h>
#include <Storages/StorageCnchMergeTree.h>

namespace DB
{
AssignedResource::AssignedResource(const StoragePtr & storage_) : storage(storage_)
{
}

void AssignedResource::addDataParts(const ServerDataPartsVector & parts)
{
    for (const auto & part : parts)
    {
        if (!part_names.contains(part->name()))
        {
            part_names.emplace(part->name());
            server_parts.emplace_back(part);
        }
    }
}

void AssignedResource::addDataParts(const HiveDataPartsCNCHVector & parts)
{
    for (const auto & part : parts)
    {
        if (!part_names.count(part->name))
        {
            part_names.emplace(part->name);
            hive_parts.emplace_back(part);
        }
    }
}

CnchServerResource::~CnchServerResource()
{
    if (!worker_group)
        return;

    auto worker_clients = worker_group->getWorkerClients();

    for (auto & worker_client : worker_clients)
    {
        try
        {
            worker_client->removeWorkerResource(txn_id);
        }
        catch (...)
        {
            tryLogCurrentException(
                __PRETTY_FUNCTION__,
                "Error occurs when remove WorkerResource{" + txn_id.toString() + "} in worker " + worker_client->getRPCAddress());
        }
    }
}

void CnchServerResource::addCreateQuery(
    const ContextPtr & context, const StoragePtr & storage, const String & create_query, const String & worker_table_name)
{
    /// table should exists in SelectStreamFactory::createForShard
    /// so we create table in worker in advance
    if (context->getServerType() == ServerType::cnch_worker)
    {
        auto temp_context = Context::createCopy(context);
        auto worker_resource = context->getCnchWorkerResource();
        worker_resource->executeCreateQuery(temp_context, create_query, /* skip_if_exists */ true);
    }

    auto lock = getLock();

    auto it = assigned_table_resource.find(storage->getStorageUUID());
    if (it == assigned_table_resource.end())
        it = assigned_table_resource.emplace(storage->getStorageUUID(), AssignedResource{storage}).first;

    it->second.create_table_query = create_query;
    it->second.worker_table_name = worker_table_name;
}


void CnchServerResource::addBufferWorkers(const UUID & storage_id, const HostWithPortsVec & buffer_workers)
{
    auto lock = getLock();

    /// StorageID should exists.
    auto & assigned_resource = assigned_table_resource.at(storage_id);
    assigned_resource.buffer_workers = buffer_workers;
}

static std::vector<brpc::CallId> processSend(
    const ContextPtr & context,
    CnchWorkerClientPtr & client,
    const std::vector<AssignedResource> & resource_to_send,
    ExceptionHandler & handler)
{
    bool is_local = context->getServerType() == ServerType::cnch_server
        && client->getHostWithPorts().getRPCAddress() == context->getHostWithPorts().getRPCAddress();

    std::vector<brpc::CallId> call_ids;

    if (!is_local)
    {
        Strings create_queries;
        for (const auto & resource : resource_to_send)
        {
            if (!resource.sent_create_query)
                create_queries.emplace_back(resource.create_table_query);
        }
        client->sendCreateQueries(context, create_queries);
    }

    /// send data parts.
    for (const auto & resource : resource_to_send)
    {
        brpc::CallId call_id;

        if (!resource.server_parts.empty())
        {
            auto data_parts = std::move(resource.server_parts);
            CnchPartsHelper::flattenPartsVector(data_parts);
            call_id = client->sendQueryDataParts(
                context, resource.storage, resource.worker_table_name, data_parts, resource.bucket_numbers, handler);
        }
        else if (!resource.hive_parts.empty())
            call_id = client->sendCnchHiveDataParts(context, resource.storage, resource.worker_table_name, resource.hive_parts, handler);

        call_ids.emplace_back(std::move(call_id));
    }

    return call_ids;
}

void CnchServerResource::sendResource(const ContextPtr & context, const HostWithPorts & worker)
{
    /**
     * send_lock:
     * For union query, it may send resources to a worker multiple times,
     * If it is sent concurrently, the data may not be ready when one of the sub-queries is executed
     * So we need to avoid this situation by taking the send_lock.
     */
    auto send_lock = getLockForSend(worker.getRPCAddress());

    std::vector<AssignedResource> resource_to_send;
    {
        auto lock = getLock();
        allocateResource(context, lock);

        auto it = assigned_worker_resource.find(worker);
        if (it == assigned_worker_resource.end())
            return;

        resource_to_send = std::move(it->second);
        assigned_worker_resource.erase(it);
    }

    ExceptionHandler handler;
    auto worker_client = worker_group->getWorkerClient(worker);
    auto call_ids = processSend(context, worker_client, resource_to_send, handler);

    for (auto & call_id : call_ids)
        brpc::Join(call_id);

    handler.throwIfException();
    /// TODO: send offloading info.
}

void CnchServerResource::sendResource(const ContextPtr & context)
{
    ExceptionHandler handler;
    std::vector<brpc::CallId> call_ids;
    {
        auto lock = getLock();
        allocateResource(context, lock);

        if (!worker_group)
            return;

        for (const auto & [host_ports, resource] : assigned_worker_resource)
        {
            auto worker_client = worker_group->getWorkerClient(host_ports);
            auto curr_ids = processSend(context, worker_client, resource, handler);
            call_ids.insert(call_ids.end(), curr_ids.begin(), curr_ids.end());
        }
        assigned_worker_resource.clear();
    }

    for (auto & call_id : call_ids)
        brpc::Join(call_id);

    handler.throwIfException();
}

void CnchServerResource::allocateResource(const ContextPtr & context, std::lock_guard<std::mutex> &)
{
    std::vector<AssignedResource> resource_to_allocate;

    for (auto & [table_id, resource] : assigned_table_resource)
    {
        if (resource.empty())
            continue;

        resource_to_allocate.emplace_back(resource);
        resource.hive_parts.clear();
        resource.server_parts.clear();
        resource.sent_create_query = true;
    }

    if (resource_to_allocate.empty())
        return;

    if (!worker_group)
        worker_group = context->tryGetCurrentWorkerGroup();

    if (worker_group)
    {
        const auto & host_ports_vec = worker_group->getHostWithPortsVec();

        for (auto & resource : resource_to_allocate)
        {
            const auto & storage = resource.storage;
            const auto & server_parts = resource.server_parts;
            const auto & required_bucket_numbers = resource.bucket_numbers;
            ServerAssignmentMap assigned_map;
            HivePartsAssignMap assigned_hive_map;
            BucketNumbersAssignmentMap assigned_bucket_numbers_map;
            if (dynamic_cast<StorageCnchMergeTree *>(storage.get()))
            {
                if (isCnchBucketTable(context, *storage, server_parts))
                {
                    auto assignment = assignCnchPartsForBucketTable(server_parts, worker_group->getWorkerIDVec(), required_bucket_numbers);
                    assigned_map = assignment.parts_assignment_map;
                    assigned_bucket_numbers_map = assignment.bucket_number_assignment_map;
                }
                else
                    assigned_map = assignCnchParts(worker_group, server_parts);
            }
            else if (auto * cnchhive = dynamic_cast<StorageCnchHive *>(storage.get()))
            {
                bool use_simple_hash = cnchhive->settings.use_simple_hash;
                LOG_TRACE(log, "CnchSessionResource use_simple_hash is: {}", use_simple_hash);
                assigned_hive_map = assignCnchHiveParts(worker_group, resource.hive_parts);
            }

            for (const auto & host_ports : host_ports_vec)
            {
                ServerDataPartsVector assigned_parts;
                HiveDataPartsCNCHVector assigned_hive_parts;
                if (auto it = assigned_map.find(host_ports.id); it != assigned_map.end())
                {
                    assigned_parts = std::move(it->second);
                }

                if (auto it = assigned_hive_map.find(host_ports.id); it != assigned_hive_map.end())
                {
                    assigned_hive_parts = std::move(it->second);
                }

                std::set<Int64> assigned_bucket_numbers;
                if (auto it = assigned_bucket_numbers_map.find(host_ports.id); it != assigned_bucket_numbers_map.end())
                {
                    assigned_bucket_numbers = std::move(it->second);
                }

                auto it = assigned_worker_resource.find(host_ports);
                if (it == assigned_worker_resource.end())
                {
                    it = assigned_worker_resource.emplace(host_ports, std::vector<AssignedResource>{}).first;
                }

                it->second.emplace_back(storage);
                auto & worker_resource = it->second.back();

                worker_resource.addDataParts(assigned_parts);
                worker_resource.addDataParts(assigned_hive_parts);
                worker_resource.sent_create_query = resource.sent_create_query;
                worker_resource.create_table_query = resource.create_table_query;
                worker_resource.worker_table_name = resource.worker_table_name;
                worker_resource.bucket_numbers = assigned_bucket_numbers;
            }
        }
    }
}

}
