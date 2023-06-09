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

#include <ServiceDiscovery/ServiceDiscoveryDNS.h>
#include <Common/Exception.h>
#include <unordered_set>
#include <arpa/inet.h>
#include <ServiceDiscovery/ServiceDiscoveryFactory.h>
#include <Common/ProfileEvents.h>
#include <Common/CurrentMetrics.h>
#include <common/logger_useful.h>
#include <mutex>

namespace ProfileEvents
{
    // TODO: thinking to remove SDRequest and SDRequestFailed. The measurement is inaccurate and is not very meaningful.
    extern const Event SDRequest;
    extern const Event SDRequestFailed;
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
    extern const int SD_PSM_NOT_EXISTS;
    extern const int SD_INVALID_TAG;
    extern const int LOGICAL_ERROR;
    extern const int SD_UPSTREAM_ISSUE;
}

#define SRV_QUERY_PROTOCOL "._tcp."
#define DNS_HEADLESS_TAIL  "-headless"

ServiceDiscoveryDNS::ServiceDiscoveryDNS(const Poco::Util::AbstractConfiguration & config)
{
    /// initialize cluster variable
    if (config.hasProperty("service_discovery.cluster"))
        cluster = config.getString("service_discovery.cluster");
    /// initialize cache settings
    if (config.hasProperty("service_discovery.disable_cache"))
        cache_disabled = config.getBool("service_discovery.disable_cache");
    if (config.hasProperty("service_discovery.cache_timeout"))
        cache_timeout = config.getUInt("service_discovery.cache_timeout");
    /// initialize services. No need to initialize for vw, as vw ServicePair created dynamically.
    if (config.hasProperty("service_discovery.server"))
    {
        String psm = config.getString("service_discovery.server.psm");
        String serviceName = config.getString("service_discovery.server.service");
        String headlessServiceName = config.getString("service_discovery.server.headless_service");
        serviceMap[psm] = ServicePair{
            .serviceType = ServiceType::server,
            .serviceName = serviceName,
            .headlessServiceName = headlessServiceName
        };
    }
    if (config.hasProperty("service_discovery.tso"))
    {
        String psm = config.getString("service_discovery.tso.psm");
        String serviceName = config.getString("service_discovery.tso.service");
        String headlessServiceName = config.getString("service_discovery.tso.headless_service");
        serviceMap[psm] = ServicePair{
            .serviceType = ServiceType::tso,
            .serviceName = serviceName,
            .headlessServiceName = headlessServiceName
        };
    }
    if (config.hasProperty("service_discovery.daemon_manager"))
    {
        String psm = config.getString("service_discovery.daemon_manager.psm");
        String serviceName = config.getString("service_discovery.daemon_manager.service");
        String headlessServiceName = config.getString("service_discovery.daemon_manager.headless_service");
        serviceMap[psm] = ServicePair{
            .serviceType = ServiceType::daemon_manager,
            .serviceName = serviceName,
            .headlessServiceName = headlessServiceName
        };
    }
    if (config.hasProperty("service_discovery.resource_manager"))
    {
        String psm = config.getString("service_discovery.resource_manager.psm");
        String serviceName = config.getString("service_discovery.resource_manager.service");
        String headlessServiceName = config.getString("service_discovery.resource_manager.headless_service");
        serviceMap[psm] = ServicePair{
            .serviceType = ServiceType::resource_manager,
            .serviceName = serviceName,
            .headlessServiceName = headlessServiceName
        };
    }
}

// TODO: consider adding a bool parameter need_hostname to reduce unnecessary resolve hostname call.
HostWithPortsVec ServiceDiscoveryDNS::lookup(const String & psm_name, ComponentType type, const String & vw_name)
{
    HostWithPortsVec res;
    ServicePair service_pair;
    auto client = std::make_shared<DNSClient>();

    if (type == ComponentType::WORKER)
    {
        service_pair = makeWorkerServicePair(vw_name);
    }
    else
    {
        if (serviceMap.find(psm_name) == serviceMap.end())
            throw Exception("psm not exist", ErrorCodes::SD_PSM_NOT_EXISTS);

        service_pair = serviceMap[psm_name];
    }

    // conduct an A4 dns query.
    auto hosts = resolveHost(client, service_pair.headlessServiceName);
    // hosts size is 0 only when a4 domain does not exists.
    // we can return empty result to user earilier.
    if(hosts.size()==0)
    {
        LOG_DEBUG(log, "lookup " + typeToString(type) + " [" + psm_name + "][" + vw_name + "] returns empty result");
        return {};
    }

    int tcp_port = -1;
    int rpc_port = -1;
    int http_port = -1;
    int exchange_port = -1;
    int exchange_status_port = -1;

    // conduct SRV dns query.
    if (type == ComponentType::SERVER || type == ComponentType::WORKER)  // For server / worker pod. PORT0 is tcp port, PORT1 is rpc port, PORT2 is http port.
    {
        tcp_port = resolvePort(client, service_pair.serviceName, "PORT0");
        rpc_port = resolvePort(client, service_pair.serviceName, "PORT1");
        http_port = resolvePort(client, service_pair.serviceName, "PORT2");
        exchange_port = resolvePort(client, service_pair.serviceName, "PORT5");
        exchange_status_port = resolvePort(client, service_pair.serviceName, "PORT6");

        // Server and worker pods must contain tcp port and rpc port
        if (tcp_port <= 0 || rpc_port <= 0)
        {
            ProfileEvents::increment(ProfileEvents::SDRequestFailed);
            throw Exception("lookup " + typeToString(type) + " [" + psm_name + "][" + vw_name + "] returns invalid port value", ErrorCodes::SD_UPSTREAM_ISSUE);
        }
    }
    else if (type == ComponentType::TSO || type == ComponentType::DAEMON_MANAGER || type == ComponentType::RESOURCE_MANAGER) // For DM / TSO / RM pod. PORT0 is rpc port.
    {
        rpc_port = resolvePort(client, service_pair.serviceName, "PORT0");

        // DM and TSO pods must contain rpc port
        if (rpc_port <= 0)
        {
            ProfileEvents::increment(ProfileEvents::SDRequestFailed);
            throw Exception("lookup " + typeToString(type) + " [" + psm_name + "][" + vw_name + "] returns invalid port value", ErrorCodes::SD_UPSTREAM_ISSUE);
        }
    }

    for (auto & host : hosts)
    {
        HostWithPorts host_with_ports { host };
        host_with_ports.id = resolveHostname(client, host);
        host_with_ports.rpc_port = rpc_port;
        host_with_ports.tcp_port = tcp_port > 0 ? tcp_port : 0;
        host_with_ports.http_port = http_port > 0 ? http_port : 0;
        host_with_ports.exchange_port = exchange_port > 0 ? exchange_port : 0;
        host_with_ports.exchange_status_port = exchange_status_port > 0 ? exchange_status_port : 0;
        res.push_back(host_with_ports);
    }


    /// sequence by virtual part feature.
    std::sort(res.begin(), res.end(), [](const HostWithPorts &x, const HostWithPorts &y) { return (x.id < y.id);} );

    return res;
}

/// Lookup zookeeper(tso) endpoints
ServiceDiscoveryDNS::Endpoints ServiceDiscoveryDNS::lookupEndpoints(const String & psm_name)
{
    if (serviceMap.find(psm_name) == serviceMap.end())
        throw Exception("psm not exist: " + psm_name, ErrorCodes::SD_PSM_NOT_EXISTS);

    auto service_pair = serviceMap[psm_name];
    Endpoints res;
    auto client = std::make_shared<DNSClient>();

    // conduct an A4 dns query.
    auto hosts = resolveHost(client, service_pair.headlessServiceName);
    // hosts size is 0 only when a4 domain does not exists.
    // we can return empty result to user earlier.;
    if(hosts.empty())
    {
        LOG_DEBUG(log, "lookupEndpoints " + psm_name + " returns empty result");
        return {};
    }

    auto port1 = resolvePort(client, service_pair.serviceName, "PORT1");
    auto port2 = resolvePort(client, service_pair.serviceName, "PORT2");

    if (port1 <= 0 || port2 <= 0)
    {
        ProfileEvents::increment(ProfileEvents::SDRequestFailed);
        throw Exception("lookupEndpoints [" + psm_name + "] returns invalid port value", ErrorCodes::SD_UPSTREAM_ISSUE);
    }

    for (auto & host : hosts)
    {
        String hostname = resolveHostname(client, host);
        /// TODO: (zuochuang.zema) any other way to get customized tags based on DNS mode.
        auto keeper_id = hostname.substr(hostname.length() - 1, 1);
        res.emplace_back(ServiceEndpoint{host, 0, {}});

        res.back().tags.emplace("id", keeper_id);
        res.back().tags.emplace("PORT1", std::to_string(port1));
        res.back().tags.emplace("PORT2", std::to_string(port2));
        res.back().tags.emplace("hostname", hostname);
    }

    return res;
}

IServiceDiscovery::WorkerGroupMap ServiceDiscoveryDNS::lookupWorkerGroupsInVW(const String & psm_name, const String & vw_name)
{
    /// FIXME:
    auto hosts = lookup(psm_name, ComponentType::WORKER, vw_name);
    return IServiceDiscovery::WorkerGroupMap { {psm_name, hosts} };
}


String ServiceDiscoveryDNS::lookupPort(const String & psm_name, const String & port_name)
{
    if (serviceMap.find(psm_name) == serviceMap.end())
        throw Exception("psm not exist", ErrorCodes::SD_PSM_NOT_EXISTS);
    ServicePair service_pair = serviceMap[psm_name];

    auto client = std::make_shared<DNSClient>();
    return std::to_string(resolvePort(client, service_pair.serviceName, port_name));
}

std::vector<String> ServiceDiscoveryDNS::generalLookup(ServicePair & service_pair, const String & port_name)
{
    std::vector<String> result;
    auto client = std::make_shared<DNSClient>();
    // conduct an A4 dns query.
    auto hosts = resolveHost(client, service_pair.headlessServiceName);
    // hosts size is 0 only when a4 domain does not exists.
    // we can return empty result to user earilier.
    if (hosts.empty())
    {
        LOG_DEBUG(log, "generalLookup(" + service_pair.serviceName + ") returns empty result");
        return result;
    }

    // conduct SRV dns query.
    int port = resolvePort(client, service_pair.serviceName, port_name);
    if (port <= 0)
        return result;
    auto portStr = std::to_string(port);

    /// sequence by virtual part feature.
    std::sort(hosts.begin(), hosts.end(), [this, client](const String & x, const String & y) { return (resolveHostname(client, x) < resolveHostname(client, y)); } );

    for (auto & host : hosts)
        result.emplace_back(makeString(host, portStr));

    return result;
}

std::vector<String> ServiceDiscoveryDNS::resolveHost(const DNSClientPtr & client, const String & headless_service)
{
    const String & a4_query = headless_service;
    if(cache_disabled)
        return resolveHostFromUpstream(client, a4_query);
    else
        return resolveHostFromCache(client, a4_query);
}

std::vector<String> ServiceDiscoveryDNS::resolveHostFromCache(const DNSClientPtr & client, const String & a4_query)
{
    std::lock_guard<std::mutex> lock(mutexHost);
    auto it = cacheHost.find(a4_query);

    // record exists in cache and is not timeout
    if(it != cacheHost.end() && time(nullptr) - cache_timeout < it->second.last_update)
    {
        return it->second.value;
    }

    // fetch from upstream as cache miss or cache record timeout
    // 1. in normal case, upstream_res size > 0
    // 2. DNS_E_NXDOMAIN: upstream_res size = 0
    // 3. DNS_E_TEMPFAIL: fallback to tcp, if tcp still fails and is not DNS_E_NXDOMAIN, throw out exception
    // 4. other exception directly throw out
    std::vector<String> upstream_res = resolveHostFromUpstream(client, a4_query);
    // we will safely update the cache if upstream_res size > 0
    if(upstream_res.size() > 0)
        cacheHost[a4_query] = {upstream_res, time(nullptr)};
    return upstream_res;
}

std::vector<String> ServiceDiscoveryDNS::resolveHostFromUpstream(const DNSClientPtr & client, const String & a4_query)
{
    std::vector<String> result;
    try
    {
        ProfileEvents::increment(ProfileEvents::SDRequestUpstream);
        CurrentMetrics::Increment metric_increment{CurrentMetrics::CnchSDRequestsUpstream};
        auto a4_res = client->resolveA4(a4_query);
        if(a4_res && a4_res->dnsa4_nrr >0 && a4_res->dnsa4_addr)
        {
            for (int i = 0; i < a4_res->dnsa4_nrr; ++i)
            {
                const in_addr_t & ip = a4_res->dnsa4_addr[i].s_addr;
                result.emplace_back(DNSClient::inAddrToStr(ip));
            }
            return result;
        }
    }
    catch(const dns::DNSResolveException & e)
    {
        LOG_DEBUG(log, a4_query + " error message:" + e.what());

        // only fallback to use tcp on DNS_E_TEMPFAIL
        bool fallback = (e.err_code == DNS_E_TEMPFAIL) ? true : false;
        if(fallback)
        {
            try
            {
                ProfileEvents::increment(ProfileEvents::SDRequestUpstream);
                CurrentMetrics::Increment metric_increment{CurrentMetrics::CnchSDRequestsUpstream};
                return DNSClient::resolveA4ByTCP(a4_query);
            }
            catch(const dns::DNSResolveException & ep)
            {
                LOG_DEBUG(log, a4_query + "error message:" + ep.what());
                if(ep.err_code != DNS_E_NXDOMAIN)
                {
                    ProfileEvents::increment(ProfileEvents::SDRequestUpstreamFailed);
                    throw Exception("CNCH internal DNS error on resolve host: " + std::string(e.what()), ErrorCodes::SD_UPSTREAM_ISSUE);
                }
            }
        }

        // exception is not DNS_E_TEMPFAIL and is not DNS_E_NXDOMAIN
        if(e.err_code != DNS_E_NXDOMAIN)
        {
            ProfileEvents::increment(ProfileEvents::SDRequestUpstreamFailed);
            throw Exception("CNCH internal DNS error on resolve host: " + std::string(e.what()), ErrorCodes::SD_UPSTREAM_ISSUE);
        }
    }
    return result;
}

int ServiceDiscoveryDNS::resolvePort(const DNSClientPtr & client, const String & service_name, const String & port_name)
{
    const String srv_query = makeSrvQuery(service_name, port_name);
    if(cache_disabled)
        return resolvePortFromUpstream(client, srv_query);
    else
        return resolvePortFromCache(client, srv_query);
}

int ServiceDiscoveryDNS::resolvePortFromCache(const DNSClientPtr & client, const String & srv_query)
{
    std::lock_guard<std::mutex> lock(mutexPort);
    auto it = cachePort.find(srv_query);

    // record exists in cache and is not timeout
    if(it != cachePort.end() && time(nullptr) - cache_timeout < it->second.last_update)
    {
        return it->second.value;
    }

    // fetch from upstream as cache miss or cache record timeout
    // 1.in normal case, upstream_res is >0
    // 2.DNS_E_NXDOMAIN may have -1 returned.
    // 3.DNS_E_TEMPFAIL mat retry 3 times and throw out exception if 3 chances used up.
    // 4.other dns issues may throw out exception.
    int upstream_res = resolvePortFromUpstream(client, srv_query);
    // we will safely update the cache if upstream_res > 0
    if (upstream_res > 0)
        cachePort[srv_query] = {upstream_res, time(nullptr)};
    return upstream_res;
}

int ServiceDiscoveryDNS::resolvePortFromUpstream(const DNSClientPtr & client, const String & srv_query) const
{
    bool retry = true;
    int retry_count = 0;
    while(retry && retry_count++ < 3)
    {
        try
        {
            ProfileEvents::increment(ProfileEvents::SDRequestUpstream);
            CurrentMetrics::Increment metric_increment{CurrentMetrics::CnchSDRequestsUpstream};
            auto srv_res = client->resolveSrv(srv_query);
            if(srv_res && srv_res->dnssrv_nrr >0 && srv_res->dnssrv_srv)
            {
                return srv_res->dnssrv_srv[0].port;
            }
        }
        catch (const dns::DNSResolveException & e)
        {
            LOG_DEBUG(log, srv_query + " error message:" + e.what());
            // only retry on dns temporary failure
            retry = (e.err_code == DNS_E_TEMPFAIL) ? true : false;
            // throw exception when:
            // 1.exception other than DNS_E_TEMPFAIL and DNS_E_NXDOMAIN
            // 2.exception is DNS_E_TEMPFAIL and used up retry
            bool throw_exception = false;
            if((e.err_code != DNS_E_TEMPFAIL && e.err_code != DNS_E_NXDOMAIN)
            || (e.err_code == DNS_E_TEMPFAIL && retry_count == 2))
                throw_exception = true;

            if(throw_exception)
            {
                ProfileEvents::increment(ProfileEvents::SDRequestUpstreamFailed);
                throw Exception("CNCH internal DNS error on resolve port: " + std::string(e.what()), ErrorCodes::SD_UPSTREAM_ISSUE);
            }
        }
    }
    return -1;
}

String ServiceDiscoveryDNS::resolveHostname(const DNSClientPtr & client, const String & host)
{
    const String ptr_query = host;
    if(cache_disabled)
        return resolveHostnameFromUpstream(client, ptr_query);
    else
        return resolveHostnameFromCache(client, ptr_query);
}

String ServiceDiscoveryDNS::resolveHostnameFromCache(const DNSClientPtr & client, const String & ptr_query)
{
    std::lock_guard<std::mutex> lock(mutexHostname);
    auto it = cacheHostname.find(ptr_query);

    // record exists in cache and is not timeout
    if(it != cacheHostname.end() && time(nullptr) - cache_timeout < it->second.last_update)
    {
        return it->second.value;
    }

    // fetch from upstream as cache miss or cache record timeout
    // 1.in normal case, upstream_res len is >0
    // 2.DNS_E_NXDOMAIN may have "" returned. (This should not appear in dns mode, as each pod has hostname)
    // 3.DNS_E_TEMPFAIL mat retry 3 times and throw out exception if 3 chances used up.
    // 4.other dns issues may throw out exception.
    String upstream_res = resolveHostnameFromUpstream(client, ptr_query);
    // so we will safely update the cache if upstream_res != ""
    if (upstream_res != "")
        cacheHostname[ptr_query] = {upstream_res, time(nullptr)};
    return upstream_res;
}

String ServiceDiscoveryDNS::resolveHostnameFromUpstream(const DNSClientPtr & client, const String & ptr_query) const
{
    bool retry = true;
    int retry_count = 0;
    while(retry && retry_count++<3)
    {
        try
        {
            ProfileEvents::increment(ProfileEvents::SDRequestUpstream);
            CurrentMetrics::Increment metric_increment{CurrentMetrics::CnchSDRequestsUpstream};
            auto ptr_res = client->resolvePtr(ptr_query);
            if(ptr_res && ptr_res->dnsptr_nrr > 0 && ptr_res->dnsptr_ptr)
            {
                // only return result with headless keyword
                for (int i = 0 ; i < ptr_res->dnsptr_nrr ; i++)
                {
                    String name = ptr_res->dnsptr_ptr[i];
                    std::size_t found = name.find("headless");
                    if (found != std::string::npos)
                        return name;
                }
            }
        }
        catch (const dns::DNSResolveException & e)
        {
            LOG_DEBUG(log, ptr_query + " error message:" + e.what());
            // only retry on dns temporary failure
            retry = (e.err_code == DNS_E_TEMPFAIL) ? true : false;
            // TODO: we can throw exception even in DNS_E_NXDOMAIN, because we can gurantee each pod will have hostname.
            // throw exception when:
            // 1.exception other than DNS_E_TEMPFAIL and DNS_E_NXDOMAIN
            // 2.exception is DNS_E_TEMPFAIL and used up retry
            bool throw_exception = false;
            if((e.err_code != DNS_E_TEMPFAIL && e.err_code != DNS_E_NXDOMAIN)
            || (e.err_code == DNS_E_TEMPFAIL && retry_count == 2))
                throw_exception = true;

            if (throw_exception)
            {
                ProfileEvents::increment(ProfileEvents::SDRequestUpstreamFailed);
                throw Exception("CNCH internal DNS error on resolve hostname: " + std::string(e.what()), ErrorCodes::SD_UPSTREAM_ISSUE);
            }
        }
    }
    return "";
}

ServicePair ServiceDiscoveryDNS::makeWorkerServicePair(const String & vw_name) const
{
    String name;
    if (vw_name == "vw_default" )
    {
        name = "wg-default-";
        name.append(cluster);
    }
    else if (vw_name == "vw_write")
    {
        name = "wg-write-";
        name.append(cluster);
    }
    else
        name = vw_name;

    return ServicePair{
        .serviceType = ServiceType::worker,
        .serviceName =  name,
        .headlessServiceName =  name.append(DNS_HEADLESS_TAIL)
    };
}

String ServiceDiscoveryDNS::makeSrvQuery(const String & service_name, const String & port_name)
{
    String full_name("_");
    /// srv query has format: _${port_name}._tcp.${service_name}
    /// 26 is the length when leaving port_name & service_name as blank
    full_name.reserve(port_name.length() + service_name.length() + 7);
    return full_name.append(port_name).append(SRV_QUERY_PROTOCOL).append(service_name);
}

String ServiceDiscoveryDNS::makeString(const String & host, const String & port)
{
    String res;
    res.reserve(INET_ADDRSTRLEN + 6); // port has a most 5 digits
    return res.append(host).append(":").append(port);
}

void registerServiceDiscoveryDNS(ServiceDiscoveryFactory & factory) {
    factory.registerServiceDiscoveryType("dns", [](const Poco::Util::AbstractConfiguration & config){
        return std::make_shared<ServiceDiscoveryDNS>(config);
    });
}

}
