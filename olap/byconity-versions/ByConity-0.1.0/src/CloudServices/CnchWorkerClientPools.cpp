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

#include <CloudServices/CnchWorkerClientPools.h>

#include <Interpreters/StorageID.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int NO_SUCH_SERVICE;
}

void CnchWorkerClientPools::addVirtualWarehouse(const String & name, const String & psm, VirtualWarehouseTypes vw_types)
{
    std::lock_guard lock(pools_mutex);
    addVirtualWarehouseImpl(name, psm, vw_types, lock);
}

void CnchWorkerClientPools::addVirtualWarehouseImpl(const String & name, const String & psm, VirtualWarehouseTypes vw_types, std::lock_guard<std::mutex> &)
{
    //TBD: whether RPC in TLS model will use different port
    auto pool = std::make_shared<CnchWorkerClientPool>(psm, [sd = this->sd, name, psm] {
                                             return sd->lookup(psm, ComponentType::WORKER, name);
                                         });

    /// Will overwrite old default pool
    for (auto & u : vw_types)
        default_pools[size_t(u)] = pool;
    /// Will replace old pool
    pools[name] = std::move(pool);

    LOG_INFO(&Poco::Logger::get("CnchWorkerClientPools"), "Added new vw: {} ", name);
}

void CnchWorkerClientPools::removeVirtualWarehouse(const String & name)
{
    std::lock_guard lock(pools_mutex);
    auto iter = pools.find(name);
    if (iter != pools.end())
        pools.erase(iter);
    /// Won't remove from default pools
}

CnchWorkerClientPoolPtr CnchWorkerClientPools::getPool(const String & name)
{
    std::lock_guard lock(pools_mutex);

    if (!pools.count(name))
        addVirtualWarehouseImpl(name, default_psm, {}, lock);

    if (auto iter = pools.find(name); iter != pools.end())
        return iter->second;
    throw Exception(ErrorCodes::LOGICAL_ERROR, "No availiable virtual warehouse: ", name);
}


CnchWorkerClientPoolPtr CnchWorkerClientPools::getPool(VirtualWarehouseType vw_type)
{
    std::lock_guard lock(pools_mutex);
    auto & pool = default_pools[size_t(vw_type)];
    if (!pool)
        throw Exception(ErrorCodes::LOGICAL_ERROR, "No default pool for vw_type {} ", ResourceManagement::toString(vw_type));
    return pool;
}

CnchWorkerClientPoolPtr CnchWorkerClientPools::getPool(const Strings & names, VirtualWarehouseTypes vw_types)
{
    auto log = &Poco::Logger::get("CnchWorkerClientPools");

    std::lock_guard lock(pools_mutex);
    for (auto & name : names)
    {
        if (!pools.count(name))
            addVirtualWarehouseImpl(name, default_psm, {}, lock);

        auto iter = pools.find(name);
        if (iter == pools.end())
            continue;
        auto & pool = iter->second;

        if (pool->empty())
            LOG_DEBUG(log, "Worker pool {} is empty, just skip it", name);
        else
            return pool;
    }
    for (auto vw_type : vw_types)
    {
        auto & pool = default_pools[size_t(vw_type)];
        if (!pool)
            continue;

        if (pool->empty())
            LOG_DEBUG(log, "Default worker pool for {} is empty, just skip it", ResourceManagement::toString(vw_type));
        else
            return pool;
    }
    throw Exception(ErrorCodes::LOGICAL_ERROR, "No available virtual warehouse");
}

/// XXX: temporary solution.
CnchWorkerClientPtr CnchWorkerClientPools::getWorker(const HostWithPorts & host_ports)
{
    std::lock_guard lock(pools_mutex);
    for (auto & [name, pool]: pools)
    {
        if (auto client = pool->get(host_ports))
            return client;
    }
    throw Exception(ErrorCodes::NO_SUCH_SERVICE, "Can't get CnchWorker by host_ports: {} ", host_ports.toDebugString());
}

}
