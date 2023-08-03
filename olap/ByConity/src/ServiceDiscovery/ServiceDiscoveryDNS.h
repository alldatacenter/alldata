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
#include <ServiceDiscovery/DNSResolver.h>
#include <Poco/Util/AbstractConfiguration.h>
#include <memory>
#include <map>
#include <unordered_map>
#include <Poco/Logger.h>
#include <mutex>
#include <string>

namespace DB
{

/// DNS service discovery completely dependents on K8s Service.
/// We use K8s Headless Service name as the dns query.
/// IP list is retrieved by DNS A4 record (from K8s headless Service).
/// Ports are retrieved by DNS Srv record (from K8s non-headless Service).
/// Cluster name is included in K8s Headless Service name.

/// To be compatible with IServiceDiscovery, we make following assumption:
///     For workers, vw_name can be translated into K8s Headless Service name. see `makeWorkerA4Query`.
///     For other services, we assume PSM is equal to K8s Headless Service name.
///     Under same K8s Service, services expose same ports. (automatically guaranteed by k8s deployment).
///
/// sdk created vw has format: cnch-worker-${cluster}-${user_input}-${timestamp}${random}
/// the service name will follow the rule as ${vw_name}-headless

/// There are two exceptions: vw_default & vw_write
/// we cannot get the cluster info from this two vw_name
/// The K8s Headless Service name for vw_default is vw-default-${cluster}-headless
/// The K8s Headless Service name for vw_write is vw-write-${cluster}-headless

enum ServiceType
{
    server = 1,
    worker = 2,
    tso = 3,
    daemon_manager = 4,
    resource_manager = 5,
};

struct ServicePair
{
    ServiceType serviceType;
    String serviceName;
    String headlessServiceName;
};

class ServiceDiscoveryDNS : public IServiceDiscovery
{
public:
    using DNSClient = dns::DNSResolver;
    using DNSClientPtr = std::shared_ptr<dns::DNSResolver>;
    using Endpoint = cpputil::consul::ServiceEndpoint;
    using Endpoints = std::vector<Endpoint>;

    ServiceDiscoveryDNS(const Poco::Util::AbstractConfiguration & config);

    std::string getName() const override { return "dns"; }

    ServiceDiscoveryMode getType() const override { return ServiceDiscoveryMode::DNS; }

    HostWithPortsVec lookup(const String & psm_name, ComponentType type, const String & vw_name = "") override;
    Endpoints lookupEndpoints(const String & psm_name) override;

    WorkerGroupMap lookupWorkerGroupsInVW(const String & psm, const String & vw_name) override;

    String lookupPort(const String & psm_name, const String & port_name="PORT0");

private:
    std::map<String, ServicePair> serviceMap; // psm -> k8s Service pair
    Poco::Logger * log = &Poco::Logger::get("ServiceDiscoveryDNS");

    std::vector<String> generalLookup(ServicePair & service_pair, const String & port_name);

    std::vector<String> resolveHost(const DNSClientPtr & client, const String & headless_service);
    std::vector<String> resolveHostFromCache(const DNSClientPtr & client, const String & a4_query);
    std::vector<String> resolveHostFromUpstream(const DNSClientPtr & client, const String & a4_query);
    int resolvePort(const DNSClientPtr & client, const String & service_name, const String & port_name);
    int resolvePortFromCache(const DNSClientPtr & client, const String & srv_query);
    int resolvePortFromUpstream(const DNSClientPtr & client, const String & srv_query) const;
    String resolveHostname(const DNSClientPtr & client, const String & host);
    String resolveHostnameFromCache(const DNSClientPtr & client, const String & ptr_query);
    String resolveHostnameFromUpstream(const DNSClientPtr & client, const String & ptr_query) const;

    ServicePair makeWorkerServicePair(const String & vw_name) const;

    static String makeSrvQuery(const String & service_name, const String & port_name);
    static String makeString(const String & host, const String & port);

    std::mutex mutexHost;
    std::mutex mutexPort;
    std::mutex mutexHostname;

    struct cacheValueHost
    {
        std::vector<String> value;
        time_t last_update;
        cacheValueHost() : value(), last_update() {}
        cacheValueHost(std::vector<String> value_, time_t last_update_) : value(std::move(value_)), last_update(last_update_) {}
    };

    struct cacheValuePort
    {
        int value;
        time_t last_update;
    };

    struct cacheValueHostname
    {
        String value;
        time_t last_update;
    };

    std::unordered_map<String, cacheValueHost> cacheHost;
    std::unordered_map<String, cacheValuePort> cachePort;
    std::unordered_map<String, cacheValueHostname> cacheHostname;
public:
    // for TEST ONLY;
    void clearCache(){ cacheHost.clear(); cachePort.clear(); cacheHostname.clear();}
};

}
