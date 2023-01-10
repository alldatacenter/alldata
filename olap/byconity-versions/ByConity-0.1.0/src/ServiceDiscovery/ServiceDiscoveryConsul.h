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
#include <ServiceDiscovery/IServiceDiscovery.h>
#include <ServiceDiscovery/ServiceDiscoveryCache.h>
#include <Poco/Util/AbstractConfiguration.h>
#include <memory>
#include <Poco/Logger.h>
#include <ServiceDiscovery/ServiceDiscoveryHelper.h>

namespace DB
{

class ServiceDiscoveryConsul : public IServiceDiscovery
{
public:
    using Endpoint = cpputil::consul::ServiceEndpoint;
    using Endpoints = std::vector<Endpoint>;

    ServiceDiscoveryConsul(const Poco::Util::AbstractConfiguration & config);

    std::string getName() const override { return "consul"; }

    ServiceDiscoveryMode getType() const override { return ServiceDiscoveryMode::CONSUL; }

    HostWithPortsVec lookup(const String & psm_name, ComponentType type, const String & vw_name = "") override;
    Endpoints lookupEndpoints(const String & psm_name) override;

    WorkerGroupMap lookupWorkerGroupsInVW(const String & psm, const String & vw_name) override;

    // format results
    static HostWithPortsVec formatResult(const Endpoints & eps, ComponentType type);

private:
    Poco::Logger * log = &Poco::Logger::get("ServiceDiscoveryConsul");

    ServiceDiscoveryCache<Endpoint> cache;

    bool passCheckCluster(const Endpoint & e);
    bool passCheckVwName(const Endpoint & e, const String & vw_name);

    // real interface
    Endpoints fetchEndpoints(const String & psm_name, const String & vw_name);
    Endpoints fetchEndpointsFromCache(const String & psm_name, const String & vw_name);
    Endpoints fetchEndpointsFromUpstream(const String & psm_name, const String & vw_name);

public:
    // fake interface, processing logics are same, but endpoints are faked.
    // for TEST ONLY!!!
    Endpoints fakedFetchEndpointsFromUpstream(const Endpoints & eps, const String & vw_name);
    Endpoints fakedFetchEndpointsFromCache(const Endpoints & eps, const String & psm_name, const String & vw_name);
    Endpoints fakedFetchEndpoints(const Endpoints & eps, const String & psm_name, const String & vw_name);

    void clearCache() { cache.clear(); }

    HostWithPortsVec fakedLookup(const Endpoints & eps, const String & psm_name, ComponentType type, const String & vw_name = "");
};

}

