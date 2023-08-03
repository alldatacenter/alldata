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
#include <Poco/Util/AbstractConfiguration.h>
#include <common/logger_useful.h>
#include <memory>
#include <map>


namespace DB
{

struct Endpoint
{
    String host;
    String hostname;
    std::map<String, String> ports;
    String virtual_warehouse = "vw_default";
    String worker_group;
    std::map<String, String> tags;
};

using Endpoints = std::vector<Endpoint>;
class ServiceDiscoveryLocal : public IServiceDiscovery
{
public:
    using LookupTable = std::map<String, Endpoints>;

    ServiceDiscoveryLocal(const Poco::Util::AbstractConfiguration & config);

    void loadConfig(const Poco::Util::AbstractConfiguration & config);

    std::string getName() const override { return "local"; }

    ServiceDiscoveryMode getType() const override { return ServiceDiscoveryMode::LOCAL; }

    HostWithPortsVec lookup(const String & psm_name, ComponentType type, const String & vw_name = "") override;
    ServiceEndpoints lookupEndpoints(const String & psm_name) override;

    WorkerGroupMap lookupWorkerGroupsInVW(const String & psm, const String & vw_name) override;

private:
    LookupTable table;

    bool passCheckVwName(const Endpoint & e, const String & vw_name);

    bool exists(const String & name);

    void initService(const Poco::Util::AbstractConfiguration & config, const String & name);

    static std::map<String, String> initPortsMap(const Poco::Util::AbstractConfiguration & config, const String & name);
    static std::map<String, String> getTagsMap(const Poco::Util::AbstractConfiguration & config, const String & name);

    String toString(const Endpoint & e, const String & tag);
};

}
