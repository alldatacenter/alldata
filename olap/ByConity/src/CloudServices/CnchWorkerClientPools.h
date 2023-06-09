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
#include <CloudServices/CnchWorkerClient.h>
#include <ServiceDiscovery/IServiceDiscovery.h>
#include <Common/RpcClientPool.h>
#include <ResourceManagement/VirtualWarehouseType.h>

namespace DB
{

using CnchWorkerClientPool = RpcClientPool<CnchWorkerClient>;
using CnchWorkerClientPoolPtr = std::shared_ptr<CnchWorkerClientPool>;
using ResourceManagement::VirtualWarehouseType;
using ResourceManagement::VirtualWarehouseTypes;

/// The cache for worker clients.
/// To get a specific VW (or workers of the VW), please use VirtualWarehousePool and VirtualWarehouseHandle.
class CnchWorkerClientPools
{
public:
    CnchWorkerClientPools(ServiceDiscoveryClientPtr sd_) : sd(std::move(sd_)) { }
    ~CnchWorkerClientPools() { }

    void setDefaultPSM(String psm) { default_psm = std::move(psm); }

    void addVirtualWarehouse(const String & name, const String & psm, VirtualWarehouseTypes vw_types);
    void removeVirtualWarehouse(const String & name);

    CnchWorkerClientPtr getWorker(const HostWithPorts & host_ports);

private:
    void addVirtualWarehouseImpl(const String & name, const String & psm, VirtualWarehouseTypes vw_types, std::lock_guard<std::mutex> &);

    ServiceDiscoveryClientPtr sd;
    String default_psm;

    std::mutex pools_mutex;
    std::unordered_map<String, CnchWorkerClientPoolPtr> pools;
    std::array<CnchWorkerClientPoolPtr, size_t(VirtualWarehouseType::Num)> default_pools;
};

};
