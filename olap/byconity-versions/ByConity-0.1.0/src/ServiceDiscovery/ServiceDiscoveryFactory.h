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

#include <Core/Types.h>
#include <ServiceDiscovery/IServiceDiscovery.h>

#include <functional>
#include <unordered_map>
#include <common/singleton.h>
#include <Poco/Util/AbstractConfiguration.h>


namespace DB
{
class Context;

/**
 * ServiceDiscovery factory. Responsible for creating new service discovery objects.
 */
class ServiceDiscoveryFactory : public ext::singleton<ServiceDiscoveryFactory>
{
public:
    using Creator = std::function<ServiceDiscoveryClientPtr(const Poco::Util::AbstractConfiguration & config)>;

    void registerServiceDiscoveryType(const String & sd_type, Creator creator);

    ServiceDiscoveryClientPtr create(const Poco::Util::AbstractConfiguration & config);
    ServiceDiscoveryClientPtr get(const ServiceDiscoveryMode & mode) const;
    ServiceDiscoveryClientPtr tryGet(const ServiceDiscoveryMode & mode) const;
    ServiceDiscoveryClientPtr get(const Poco::Util::AbstractConfiguration & config) const;

private:
    using ServiceDiscoveryRegistry = std::unordered_map<String, Creator>;
    ServiceDiscoveryRegistry registry;

    using ServiceDiscoveryClients = std::unordered_map<ServiceDiscoveryMode, ServiceDiscoveryClientPtr>;
    ServiceDiscoveryClients sd_clients;
};

}
