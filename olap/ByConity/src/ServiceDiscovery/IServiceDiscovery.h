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
#include <memory>
#include <unordered_map>
#include <Core/Types.h>
#include <Common/Exception.h>
#include <Common/HostWithPorts.h>
#include <ServiceDiscovery/ServiceDiscoveryHelper.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int UNKNOWN_IDENTIFIER;
    extern const int NOT_IMPLEMENTED;
}

enum class ComponentType
{
    SERVER,
    WORKER,
    TSO,
    DAEMON_MANAGER,
    NNPROXY,  /// look up only by consul client
    KMS,  /// look up only by consul client
    RESOURCE_MANAGER,
    CLICKHOUSE_KEEPER,
};

inline String typeToString(ComponentType type)
{
    switch (type)
    {
        case ComponentType::SERVER:
            return "SERVER";
        case ComponentType::WORKER:
            return "WORKER";
        case ComponentType::TSO:
            return "TSO";
        case ComponentType::DAEMON_MANAGER:
            return "DAEMON_MANAGER";
        case ComponentType::NNPROXY:
            return "NNPROXY";
        case ComponentType::KMS:
            return "KMS";
        case ComponentType::RESOURCE_MANAGER:
            return "RESOURCE_MANAGER";
        case ComponentType::CLICKHOUSE_KEEPER:
            return "CLICKHOUSE_KEEPER";
        // default:
        //     return "UNKNOWN";
    }
}

enum class ServiceDiscoveryMode
{
    LOCAL,
    CONSUL,
    DNS,
};

inline String typeToString(ServiceDiscoveryMode mode)
{
    switch (mode)
    {
        case ServiceDiscoveryMode::LOCAL:
            return "LOCAL";
        case ServiceDiscoveryMode::CONSUL:
            return "CONSUL";
        case ServiceDiscoveryMode::DNS:
            return "DNS";
    }
}

inline ServiceDiscoveryMode toServiceDiscoveryMode(const String & mode)
{
    if (mode == "LOCAL")
        return ServiceDiscoveryMode::LOCAL;
    else if (mode == "CONSUL")
        return ServiceDiscoveryMode::CONSUL;
    else if (mode == "DNS")
        return ServiceDiscoveryMode::DNS;
    else
        throw Exception(ErrorCodes::UNKNOWN_IDENTIFIER, "Unknown ServiceDiscoveryMode {}", mode);
}

using ServiceEndpoint = cpputil::consul::ServiceEndpoint;
using ServiceEndpoints = std::vector<ServiceEndpoint>;

class IServiceDiscovery
{
public:
    using WorkerGroupMap = std::unordered_map<String, HostWithPortsVec>;

    /// Get the main function name.
    virtual std::string getName() const = 0;

    /// Get the mode of Service Discovery
    virtual ServiceDiscoveryMode getType() const = 0;

    /// Get the cluster name
    std::string getClusterName() const { return cluster; }

    virtual HostWithPortsVec lookup(const String & psm_name, ComponentType type, const String & vw_name = "") = 0;
    virtual ServiceEndpoints lookupEndpoints(const String &)
    {
        throw Exception(ErrorCodes::NOT_IMPLEMENTED, "Method {} doesn't lookupEndpoints now", getName());
    }

    virtual WorkerGroupMap lookupWorkerGroupsInVW([[maybe_unused]]const String & psm_name, [[maybe_unused]]const String & vw_name)
    {
        return {}; /// TODO: make it virtual
    }

    virtual ~IServiceDiscovery() = default;

protected:
    String cluster = "default";

    bool cache_disabled = false;
    UInt32 cache_timeout = 5;  /// seconds
};

using ServiceDiscoveryClientPtr = std::shared_ptr<IServiceDiscovery>;

}
