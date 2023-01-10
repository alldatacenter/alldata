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

#include <ServiceDiscovery/ServiceDiscoveryConsul.h>
#include <sstream>
#include <string>
#include <Common/Exception.h>
#include <common/logger_useful.h>
#include <ServiceDiscovery/ServiceDiscoveryFactory.h>
#include <ServiceDiscovery/ServiceDiscoveryHelper.h>
#include <IO/ReadHelpers.h>
#include <Common/ProfileEvents.h>
#include <Common/CurrentMetrics.h>

namespace ProfileEvents
{
    extern const Event SDRequestUpstream;
    extern const Event SDRequestUpstreamFailed;
}

namespace CurrentMetrics
{
    extern const Metric CnchSDRequestsUpstream;
}

namespace DB
{

namespace ErrorCodes
{
    extern const int SD_EMPTY_ENDPOINTS;
    extern const int SD_INVALID_TAG;
    extern const int SD_PSM_NOT_EXISTS;
    extern const int SD_UNKOWN_LB_STRATEGY;
    extern const int SD_FAILED_TO_INIT_CONSUL_CLIENT;
    extern const int NETWORK_ERROR;
    extern const int LOGICAL_ERROR;
}

ServiceDiscoveryConsul::ServiceDiscoveryConsul(const Poco::Util::AbstractConfiguration & config)
{
    /// initialize cluster variable
    if (config.hasProperty("service_discovery.cluster"))
        cluster = config.getString("service_discovery.cluster");
    /// initialize cache settings
    if (config.hasProperty("service_discovery.disable_cache"))
        cache_disabled = config.getBool("service_discovery.disable_cache");
    if (config.hasProperty("service_discovery.cache_timeout"))
        cache_timeout = config.getUInt("service_discovery.cache_timeout");

}

HostWithPortsVec ServiceDiscoveryConsul::lookup(const String & psm_name, ComponentType type, const String & vw_name)
{
    if (type != ComponentType::WORKER && !vw_name.empty())
        throw Exception("Should not specify vw name for non-worker components", ErrorCodes::LOGICAL_ERROR);

    Endpoints endpoints = fetchEndpoints(psm_name, vw_name);
    HostWithPortsVec result = formatResult(endpoints, type);

    if (result.size() == 0)
        LOG_DEBUG(log, "lookup " + typeToString(type) + " [" + psm_name + "][" + vw_name + "] returns empty result");
    return result;
}

ServiceDiscoveryConsul::Endpoints ServiceDiscoveryConsul::lookupEndpoints(const String & psm_name)
{
    return fetchEndpoints(psm_name, "");
}

IServiceDiscovery::WorkerGroupMap ServiceDiscoveryConsul::lookupWorkerGroupsInVW(const String & psm_name, const String & vw_name)
{
    Endpoints endpoints = fetchEndpoints(psm_name, vw_name);

    IServiceDiscovery::WorkerGroupMap group_map;

    for (auto & ep : endpoints)
    {
        String wg_name = ep.tags.count("wg_name") ? ep.tags["wg_name"] : "wg_default";

        auto & group = group_map[wg_name];

        HostWithPorts host_with_ports{ep.host};
        host_with_ports.tcp_port = static_cast<uint16_t>(ep.port);

        if (ep.tags.count("hostname"))
            host_with_ports.id = ep.tags.at("hostname");
        if (ep.tags.count("PORT1"))
            host_with_ports.rpc_port = parse<UInt16>(ep.tags.at("PORT1"));
        if (ep.tags.count("PORT2"))
            host_with_ports.http_port = parse<UInt16>(ep.tags.at("PORT2"));
        if (ep.tags.count("PORT5"))
            host_with_ports.exchange_port = parse<UInt16>(ep.tags.at("PORT5"));
        if (ep.tags.count("PORT6"))
            host_with_ports.exchange_status_port = parse<UInt16>(ep.tags.at("PORT6"));

        group.push_back(std::move(host_with_ports));
    }

    return group_map;
}

ServiceDiscoveryConsul::Endpoints ServiceDiscoveryConsul::fetchEndpoints(const String & psm_name, const String & vw_name)
{
    if (cache_disabled)
        return fetchEndpointsFromUpstream(psm_name, vw_name);
    else
        return fetchEndpointsFromCache(psm_name, vw_name);
}

ServiceDiscoveryConsul::Endpoints ServiceDiscoveryConsul::fetchEndpointsFromCache(const String & psm_name, const String & vw_name)
{
    SDCacheKey key {psm_name, vw_name};
    SDCacheValue<Endpoint> cache_res;

    // Cache hit. endpoints exists in cache and not outdated
    if (cache.get(key, cache_res) && time(nullptr) - cache_timeout < cache_res.last_update)
    {
        return cache_res.endpoints;
    }

    // Cache miss. We fetch entry and store in cache
    try
    {
        Endpoints res = fetchEndpointsFromUpstream(psm_name, vw_name);
        SDCacheValue<Endpoint> new_value {res, time(nullptr)};

        // update cache
        cache.put(key, new_value);
        return res;

    } catch (...)
    {
        LOG_WARNING(log, "failed to update sd cache, return old results");
        return cache_res.endpoints;
    }
}

ServiceDiscoveryConsul::Endpoints ServiceDiscoveryConsul::fetchEndpointsFromUpstream(const String &, const String &)
{
    return {};
}

HostWithPortsVec ServiceDiscoveryConsul::formatResult(const Endpoints & eps, ComponentType type)
{
    HostWithPortsVec result;
    if (type == ComponentType::SERVER || type == ComponentType::WORKER)
    {
        for (auto & e : eps)
        {
            HostWithPorts host_with_ports{e.host};
            host_with_ports.tcp_port = e.port;
            if (e.tags.count("hostname"))
                host_with_ports.id = e.tags.at("hostname");
            if (e.tags.count("PORT1"))
                host_with_ports.rpc_port = parse<UInt16>(e.tags.at("PORT1"));
            if (e.tags.count("PORT2"))
                host_with_ports.http_port = parse<UInt16>(e.tags.at("PORT2"));
            if (e.tags.count("PORT5"))
                host_with_ports.exchange_port = parse<UInt16>(e.tags.at("PORT5"));
            if (e.tags.count("PORT6"))
                host_with_ports.exchange_status_port = parse<UInt16>(e.tags.at("PORT6"));

            result.push_back(std::move(host_with_ports));
        }
    }
    else if (type == ComponentType::TSO || type == ComponentType::DAEMON_MANAGER || type == ComponentType::RESOURCE_MANAGER)
    {
        for (auto & e : eps)
        {
            HostWithPorts host_with_ports{e.host};
            host_with_ports.rpc_port = e.port;
            if (e.tags.count("hostname"))
                host_with_ports.id = e.tags.at("hostname");

            result.push_back(std::move(host_with_ports));
        }
    }
    else if (type == ComponentType::NNPROXY || type == ComponentType::KMS)
    {
        for (auto & e : eps)
        {
            HostWithPorts host_with_ports{e.host};
            host_with_ports.tcp_port = e.port;
            result.push_back(std::move(host_with_ports));
        }
    }

    /// sequence by virtual part feature.
    std::sort(result.begin(), result.end(), [](const HostWithPorts &x, const HostWithPorts &y) { return (x.id < y.id);} );

    return result;
}

bool ServiceDiscoveryConsul::passCheckCluster(const Endpoint & e)
{
    auto it = e.tags.find("cluster");
    return (it == e.tags.end() || it->second == cluster);  /// If endpoint doesn't have `cluster` tag, i.e. nnproxy, let it pass the checking
}

bool ServiceDiscoveryConsul::passCheckVwName(const Endpoint & e, const String & vw_name)
{
    auto it = e.tags.find("vw_name");
    return (it != e.tags.end() && it->second == vw_name);
}

void registerServiceDiscoveryConsul(ServiceDiscoveryFactory & factory) {
    factory.registerServiceDiscoveryType("consul", [](const Poco::Util::AbstractConfiguration & config){
        return std::make_shared<ServiceDiscoveryConsul>(config);
    });
}

// For testing. fake interface
HostWithPortsVec ServiceDiscoveryConsul::fakedLookup(const Endpoints & eps, const String & psm_name, ComponentType type, const String & vw_name)
{
    if (type != ComponentType::WORKER && !vw_name.empty())
        throw Exception("Should not specify vw name for non-worker components", ErrorCodes::LOGICAL_ERROR);

    Endpoints endpoints = fakedFetchEndpoints(eps, psm_name, vw_name);
    HostWithPortsVec result = formatResult(endpoints, type);

    if (result.size() == 0)
        LOG_DEBUG(log, "fakedLookup(" + psm_name + ") returns empty result");
    return result;
}

// For testing. fake interface.
ServiceDiscoveryConsul::Endpoints ServiceDiscoveryConsul::fakedFetchEndpoints(const Endpoints & eps, const String & psm_name, const String & vw_name)
{
    if (cache_disabled)
        return fakedFetchEndpointsFromUpstream(eps, vw_name);
    else
        return fakedFetchEndpointsFromCache(eps, psm_name, vw_name);
}

// For testing. fake interface.
ServiceDiscoveryConsul::Endpoints ServiceDiscoveryConsul::fakedFetchEndpointsFromCache(const Endpoints & eps, const String & psm_name, const String & vw_name)
{
    SDCacheKey key {psm_name, vw_name};
    SDCacheValue<Endpoint> cache_res;

    // Cache hit. endpoints exists in cache and not outdated
    if (cache.get(key, cache_res) && time(nullptr) - cache_timeout < cache_res.last_update)
    {
        return cache_res.endpoints;
    }

    // Cache miss. We fetch entry and store in cache
    try
    {
        Endpoints res = fakedFetchEndpointsFromUpstream(eps, vw_name);
        SDCacheValue<Endpoint> new_value {res, time(nullptr)};

        // update cache
        cache.put(key, new_value);
        return res;

    } catch (...)
    {
        LOG_WARNING(log, "failed to update sd cache, return old results");
        return cache_res.endpoints;
    }
}

// For testing. fake interface.
ServiceDiscoveryConsul::Endpoints ServiceDiscoveryConsul::fakedFetchEndpointsFromUpstream(const Endpoints & eps, const String & vw_name)
{
    auto retry_count = 2;
    while (retry_count >= 0)
    {
        try
        {
            ProfileEvents::increment(ProfileEvents::SDRequestUpstream);
            // by calling translate_one, we get endpoints from consul
            Endpoints res;
            for(auto & e : eps)
            {
                // kick out endpoint that doesn't belong to this cluster as earily as possible
                if(!passCheckCluster(e))
                    continue;
                // kick out endpoint that doesn't belong to this vw_name as earily as possible
                if(!vw_name.empty() && !passCheckVwName(e, vw_name))
                    continue;
                res.emplace_back(e);
            }
            return res;
        }
        catch (Exception & e)
        {
            ProfileEvents::increment(ProfileEvents::SDRequestUpstreamFailed);
            LOG_WARNING(log, "Try " + std::to_string(3 - retry_count)
                        + ": Error looking up from consul " + e.displayText());
            --retry_count;
            usleep(10000); // sleep for 10ms
        }
    }

    throw Exception("Unable to get endpoints from consul", ErrorCodes::NETWORK_ERROR);
}

}
