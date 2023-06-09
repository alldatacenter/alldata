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

#include <ServiceDiscovery/ServiceDiscoveryLocal.h>

#include <IO/ReadHelpers.h>
#include <Common/Exception.h>
#include <Common/ProfileEvents.h>
#include <Common/StringUtils/StringUtils.h>
#include <Poco/Util/AbstractConfiguration.h>
#include <ServiceDiscovery/ServiceDiscoveryFactory.h>
#include <ServiceDiscovery/ServiceDiscoveryHelper.h>

#include <sstream>

namespace ProfileEvents
{
extern const Event SDRequest;
extern const Event SDRequestFailed;
extern const Event SDRequestUpstream;
extern const Event SDRequestUpstreamFailed;
}

namespace DB
{
namespace ErrorCodes
{
    extern const int SD_EMPTY_ENDPOINTS;
    extern const int SD_INVALID_TAG;
    extern const int SD_PSM_NOT_EXISTS;
    extern const int SD_UNKOWN_LB_STRATEGY;
}


ServiceDiscoveryLocal::ServiceDiscoveryLocal(const Poco::Util::AbstractConfiguration & config)
{
    loadConfig(config);
}

HostWithPortsVec ServiceDiscoveryLocal::lookup(const String & psm_name, ComponentType type, const String & vw_name)
{
    if (!exists(psm_name))
        throw Exception("psm:" + psm_name + " not exists in service registry.", ErrorCodes::SD_PSM_NOT_EXISTS);

    HostWithPortsVec res;
    Endpoints endpoints = table[psm_name];

    if (type == ComponentType::SERVER || type == ComponentType::WORKER)
    {
        for (auto & ep : endpoints)
        {
            if (type == ComponentType::WORKER && !vw_name.empty() && ep.virtual_warehouse != vw_name)
                continue;

            HostWithPorts host_with_ports{ep.host};
            host_with_ports.id = ep.hostname;
            if (ep.ports.count("PORT0"))
                host_with_ports.tcp_port = parse<UInt16>(ep.ports["PORT0"]);
            if (ep.ports.count("PORT1"))
                host_with_ports.rpc_port = parse<UInt16>(ep.ports["PORT1"]);
            if (ep.ports.count("PORT2"))
                host_with_ports.http_port = parse<UInt16>(ep.ports["PORT2"]);
            if (ep.ports.count("PORT5"))
                host_with_ports.exchange_port = parse<UInt16>(ep.ports["PORT5"]);
            if (ep.ports.count("PORT6"))
                host_with_ports.exchange_status_port = parse<UInt16>(ep.ports["PORT6"]);
            res.push_back(std::move(host_with_ports));
        }
    }
    else if (type == ComponentType::TSO || type == ComponentType::DAEMON_MANAGER || type == ComponentType::RESOURCE_MANAGER)
    {
        for (auto & ep : endpoints)
        {
            HostWithPorts host_with_ports{ep.host};
            host_with_ports.id = ep.hostname;
            if (ep.ports.count("PORT0"))
                host_with_ports.rpc_port = parse<UInt16>(ep.ports["PORT0"]);
            res.push_back(std::move(host_with_ports));
        }
    }

    return res;
}

ServiceEndpoints ServiceDiscoveryLocal::lookupEndpoints(const String & psm_name)
{
    if (!exists(psm_name))
        throw Exception("psm: " + psm_name + " not exists in service registry.", ErrorCodes::SD_PSM_NOT_EXISTS);

    auto endpoints = table.at(psm_name);
    ServiceEndpoints res;
    for (const auto & endpoint: endpoints)
    {
        res.emplace_back(ServiceEndpoint{endpoint.host, 0, endpoint.tags});
        if (endpoint.ports.count("PORT0"))
            res.back().port = std::stoi(endpoint.ports.at("PORT0"));
        for (const auto & [name, tag]: endpoint.tags)
            res.back().tags.emplace(name, tag);
        for (const auto & [name, port]: endpoint.ports) /// add ports to tags.
            res.back().tags.emplace(name, port);
        res.back().tags.emplace("hostname", endpoint.hostname);
    }
    return res;
}

IServiceDiscovery::WorkerGroupMap ServiceDiscoveryLocal::lookupWorkerGroupsInVW(const String & psm_name, const String & vw_name)
{
    if (!exists(psm_name))
        throw Exception("psm:" + psm_name + " not exists in service registry.", ErrorCodes::SD_PSM_NOT_EXISTS);

    IServiceDiscovery::WorkerGroupMap group_map;

    Endpoints endpoints = table[psm_name];
    for (auto & ep : endpoints)
    {
        if (ep.virtual_warehouse != vw_name)
            continue;

        auto & group = group_map[ep.worker_group];
        HostWithPorts host_with_ports{ep.host};
        host_with_ports.id = ep.hostname;
        if (ep.ports.count("PORT0"))
            host_with_ports.tcp_port = parse<UInt16>(ep.ports["PORT0"]);
        if (ep.ports.count("PORT1"))
            host_with_ports.rpc_port = parse<UInt16>(ep.ports["PORT1"]);
        if (ep.ports.count("PORT2"))
            host_with_ports.http_port = parse<UInt16>(ep.ports["PORT2"]);
        if (ep.ports.count("PORT5"))
            host_with_ports.exchange_port = parse<UInt16>(ep.ports["PORT5"]);
        if (ep.ports.count("PORT6"))
            host_with_ports.exchange_status_port = parse<UInt16>(ep.ports["PORT6"]);

        group.push_back(host_with_ports);
    }

    return group_map;
}

bool ServiceDiscoveryLocal::exists(const String & name)
{
    return table.find(name) != table.end();
}

void ServiceDiscoveryLocal::loadConfig(const Poco::Util::AbstractConfiguration & config)
{
    table.clear(); // reset the lookuptable;

    if (!config.has("service_discovery"))
        return;

    Poco::Util::AbstractConfiguration::Keys keys;
    config.keys("service_discovery", keys);

    for (auto & key : keys)
    {
        if (key == "mode" || key == "cluster" || key == "disable_cache" || key == "cache_timeout")
            continue;
        initService(config, "service_discovery." + key);
    }
}

void ServiceDiscoveryLocal::initService(const Poco::Util::AbstractConfiguration & config, const String & name)
{
    if (!config.has(name))
        return;

    Poco::Util::AbstractConfiguration::Keys keys;
    String psm;
    Endpoints endpoints;

    config.keys(name, keys);
    for (auto & key : keys)
    {
        if (startsWith(key, "node"))
        {
            Endpoint endpoint = {
                config.getString(name + "." + key + ".host"),
                config.getString(name + "." + key + ".hostname"),
                initPortsMap(config, name + "." + key + ".ports"),
                config.getString(name + "." + key + ".vw_name", "vw_default"),
                config.getString(name + "." + key + ".wg_name", "wg_default"),
                getTagsMap(config, name + "." + key)
            };
            endpoints.push_back(endpoint);
        }
        else if (key == "psm")
        {
            psm = config.getString(name + "." + key);
        }
    }
    table[psm] = endpoints;
}

std::map<String, String> ServiceDiscoveryLocal::initPortsMap(const Poco::Util::AbstractConfiguration & config, const String & name)
{
    std::map<String, String> ports_map;

    if (config.has(name))
    {
        Poco::Util::AbstractConfiguration::Keys keys;
        config.keys(name, keys);

        for (auto & key : keys)
        {
            if (startsWith(key, "port"))
            {
                String tag = config.getString(name + "." + key + ".name");
                String port = config.getString(name + "." + key + ".value");
                ports_map[tag] = port;
            }
        }
    }

    return ports_map;
}

std::map<String, String> ServiceDiscoveryLocal::getTagsMap(const Poco::Util::AbstractConfiguration & config, const String & name)
{
    std::map<String, String> tags_map;
    if (config.has(name))
    {
        Poco::Util::AbstractConfiguration::Keys keys;
        config.keys(name, keys);
        for (auto & key: keys)
        {
            tags_map.emplace(key, config.getString(name + "." + key));
        }
    }
    return tags_map;
}

void registerServiceDiscoveryLocal(ServiceDiscoveryFactory & factory)
{
    factory.registerServiceDiscoveryType(
        "local", [](const Poco::Util::AbstractConfiguration & config) { return std::make_shared<ServiceDiscoveryLocal>(config); });
}

}
