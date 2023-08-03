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

#include <Interpreters/WorkerGroupHandle.h>

#include <ResourceManagement/CommonData.h>
#include <Common/parseAddress.h>
#include <Interpreters/Context.h>
#include <IO/ConnectionTimeouts.h>
#include <CloudServices/CnchWorkerClientPools.h>
#include <common/logger_useful.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int NO_SUCH_SERVICE;
    extern const int VIRTUAL_WAREHOUSE_NOT_FOUND;
    extern const int RESOURCE_MANAGER_NO_AVAILABLE_WORKER;
}

std::unique_ptr<DB::ConsistentHashRing> WorkerGroupHandleImpl::buildRing(const ShardsInfo & shards_info, const ContextPtr global_context)
{
    UInt32 num_replicas = global_context->getConfigRef().getInt("consistent_hash_ring.num_replicas", 16);
    UInt32 num_probes = global_context->getConfigRef().getInt("consistent_hash_ring.num_probes", 21);
    double load_factor = global_context->getConfigRef().getDouble("consistent_hash_ring.load_factor", 1.15);
    std::unique_ptr<DB::ConsistentHashRing> ret = std::make_unique<DB::ConsistentHashRing>(num_replicas, num_probes, load_factor);
    std::for_each(shards_info.begin(), shards_info.end(), [&](const ShardInfo & info)
    {
        ret->insert(info.worker_id);
    });
    return ret;
}

WorkerGroupHandleImpl::WorkerGroupHandleImpl(
    String id_,
    WorkerGroupHandleSource source_,
    String vw_name_,
    HostWithPortsVec hosts_,
    const ContextPtr & context_,
    const WorkerGroupMetrics & metrics_)
    : WithContext(context_->getGlobalContext())
    , id(std::move(id_))
    , source(std::move(source_))
    , vw_name(std::move(vw_name_))
    , hosts(std::move(hosts_))
    , metrics(metrics_)
{
    auto current_context = getContext();

    const auto & settings = current_context->getSettingsRef();
    const auto & config = current_context->getConfigRef();
    bool enable_ssl = current_context->isEnableSSL();

    UInt16 clickhouse_port = enable_ssl ? static_cast<UInt16>(config.getInt("tcp_port_secure", 0))
                                        : static_cast<UInt16>(config.getInt("tcp_port", 0));

    auto user_password = current_context->getCnchInterserverCredentials();
    auto default_database = config.getRawString("default_database", "default");

    for (size_t i = 0; i < hosts.size(); ++i)
    {
        auto & host = hosts[i];
        Address address(host.getTCPAddress(), user_password.first, user_password.second, clickhouse_port, enable_ssl);

        ShardInfo info;
        info.worker_id = host.id;
        info.shard_num = i + 1; /// shard_num begin from 1
        info.weight = 1;

        if (address.is_local)
            info.local_addresses.push_back(address);

        LOG_DEBUG(&Poco::Logger::get("WorkerGroupHandleImpl"), "Add address {}. is_local: {} id: {}", host.toDebugString(), address.is_local, host.id);

        ConnectionPoolPtr pool = std::make_shared<ConnectionPool>(
            settings.distributed_connections_pool_size,
            address.host_name, address.port,
            default_database, user_password.first, user_password.second,
            /*cluster_*/"",/*cluster_secret_*/"",
            "server", address.compression, address.secure, 1,
            host.exchange_port, host.exchange_status_port, host.rpc_port);

        info.pool = std::make_shared<ConnectionPoolWithFailover>(
            ConnectionPoolPtrs{pool}, settings.load_balancing, settings.connections_with_failover_max_tries);
        info.per_replica_pools = {std::move(pool)};

        shards_info.emplace_back(std::move(info));

        worker_clients.emplace_back(current_context->getCnchWorkerClientPools().getWorker(host));
    }

    /// if not jump consistent hash, build ring
    if (current_context->getPartAllocationAlgo() != Context::PartAllocator::JUMP_CONSISTENT_HASH)
    {
        ring = buildRing(this->shards_info, current_context);
        LOG_DEBUG(&Poco::Logger::get("WorkerGroupHandleImpl"), "Success built ring with {} nodes\n", ring->size());

    }
}

WorkerGroupHandleImpl::WorkerGroupHandleImpl(const WorkerGroupData & data, const ContextPtr & context_)
    : WorkerGroupHandleImpl(data.id, WorkerGroupHandleSource::RM, data.vw_name, data.host_ports_vec, context_, data.metrics)
{
    type = data.type;
}

WorkerGroupHandleImpl::WorkerGroupHandleImpl(const WorkerGroupHandleImpl & from, [[maybe_unused]] const std::vector<size_t> & indices)
    : WithContext(from.getContext()), id(from.getID()), vw_name(from.getVWName())
{
    auto current_context = context.lock();
    if (!current_context)
        throw Exception(ErrorCodes::LOGICAL_ERROR, "Context expired!");

    // TODO(zuochuang.zema) MERGE worker client pool
    for (size_t index : indices)
    {
        hosts.emplace_back(from.getHostWithPortsVec().at(index));
        shards_info.emplace_back(from.getShardsInfo().at(index));
        worker_clients.emplace_back(current_context->getCnchWorkerClientPools().getWorker(hosts.back()));
    }

    // TODO(zuochuang.zema) MERGE hash
    if (current_context->getPartAllocationAlgo() != Context::PartAllocator::JUMP_CONSISTENT_HASH)
    {
        ring = buildRing(this->shards_info, current_context);
    }
}

CnchWorkerClientPtr WorkerGroupHandleImpl::getWorkerClientByHash(const String & key) const
{
    if (worker_clients.empty())
        throw Exception("No available worker for " + id, ErrorCodes::RESOURCE_MANAGER_NO_AVAILABLE_WORKER);
    UInt64 index = std::hash<String>{}(key) % worker_clients.size();
    return worker_clients[index];
}

CnchWorkerClientPtr WorkerGroupHandleImpl::getWorkerClient() const
{
    if (worker_clients.empty())
        throw Exception("No available worker for " + id, ErrorCodes::RESOURCE_MANAGER_NO_AVAILABLE_WORKER);
    if (worker_clients.size() == 1)
        return worker_clients[0];

    std::uniform_int_distribution dist;
    auto index = dist(thread_local_rng) % worker_clients.size();
    return worker_clients[index];
}

CnchWorkerClientPtr WorkerGroupHandleImpl::getWorkerClient(const HostWithPorts & host_ports) const
{
    if (auto index = indexOf(host_ports))
    {
        return worker_clients.at(index.value());
    }

    throw Exception("Can't get WorkerClient for host_ports: " + host_ports.toDebugString(), ErrorCodes::NO_SUCH_SERVICE);
}

bool WorkerGroupHandleImpl::isSame(const WorkerGroupData & data) const
{
    if (id != data.id || vw_name != data.vw_name)
        return false;
    if (!HostWithPorts::isExactlySameVec(hosts, data.host_ports_vec))
        return false;
    return true;
}

WorkerGroupHandle WorkerGroupHandleImpl::cloneAndRemoveShards(const std::vector<uint64_t> & remove_marks) const
{
    std::vector<size_t> cloned_hosts;
    for (size_t i = 0; i < hosts.size(); ++i)
    {
        if (!remove_marks[i])
            cloned_hosts.emplace_back(i);
    }

    if (cloned_hosts.empty())
        throw Exception("No worker available for after removing not available shards.", ErrorCodes::VIRTUAL_WAREHOUSE_NOT_FOUND);

    return std::make_shared<WorkerGroupHandleImpl>(*this, cloned_hosts);
}

Strings WorkerGroupHandleImpl::getWorkerTCPAddresses(const Settings & settings) const
{
    Strings res;
    auto timeouts = ConnectionTimeouts::getTCPTimeoutsWithFailover(settings);
    for (const auto & shard_info : shards_info)
    {
        auto entry = shard_info.pool->get(timeouts, &settings, true);
        Connection * conn = &(*entry);
        res.emplace_back(conn->getHost() + ":" + std::to_string(conn->getPort()));
    }
    return res;
}

Strings WorkerGroupHandleImpl::getWorkerIDVec() const
{
    Strings res;
    for (const auto & host : hosts)
        res.emplace_back(host.id);
    return res;
}

std::vector<std::pair<String, UInt16>> WorkerGroupHandleImpl::getReadWorkers() const
{
    std::vector<std::pair<String, UInt16>> res;
    const auto & settings = getContext()->getSettingsRef();
    auto timeouts = ConnectionTimeouts::getTCPTimeoutsWithFailover(settings);
    for (const auto & shard_info : shards_info)
    {
        auto entry = shard_info.pool->get(timeouts, &settings, true);
        Connection * conn = &(*entry);
        res.emplace_back(conn->getHost(), conn->getPort());
    }
    return res;
}

}
