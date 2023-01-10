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
#include <Common/thread_local_rng.h>
#include <Common/ConsistentHashUtils/ConsistentHashRing.h>

#include <boost/noncopyable.hpp>
#include <common/logger_useful.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int BRPC_EXCEPTION;
    extern const int NO_SUCH_SERVICE;
}

template <class T>
struct RpcClientPoolDefaultCreator
{
    std::shared_ptr<T> operator()(HostWithPorts host_port) const { return std::make_shared<T>(std::move(host_port)); }
};

template <class T>
class RpcClientPool : private boost::noncopyable
{
public:
    using Ptr = std::shared_ptr<T>;
    using ClientsMap = std::unordered_map<HostWithPorts, Ptr, std::hash<HostWithPorts>, HostWithPorts::IsExactlySame>;

    static String getName() { return T::getName() + "Pool"; }

    RpcClientPool(
        String service_name_,
        std::function<HostWithPortsVec()> lookup_,
        std::function<Ptr(HostWithPorts)> creator_ = RpcClientPoolDefaultCreator<T>{})
        : service_name(std::move(service_name_))
        , lookup(std::move(lookup_))
        , creator(std::move(creator_))
        , log(&Poco::Logger::get(getName() + ':' + getServiceName()))
    {
    }

    const auto & getServiceName() const { return service_name; }
    auto getClientsMapSize() const { return clients_map.size(); }

    void update()
    {
        std::unique_lock lock(state_mutex);
        updateUnlocked(lock);
    }

    Ptr get(const String & host, UInt16 port) { return get(addBracketsIfIpv6(host) + ':' + std::to_string(port)); }

    Ptr get(const HostWithPorts & host_ports)
    {
        std::unique_lock lock(state_mutex);
        if (auto iter = clients_map.find(host_ports); iter != clients_map.end())
        {
            return iter->second;
        }

        return clients_map.try_emplace(host_ports, creator(host_ports)).first->second;
    }

    Ptr get(const String & host_port)
    {
        auto host_ports = HostWithPorts::fromRPCAddress(host_port);
        return get(host_ports);
    }

    Ptr tryGetByRPCAddress(const String & host_with_rpc_port)
    {
        if (host_with_rpc_port.empty())
            return {};

        std::unique_lock lock(state_mutex);
        for (const auto & ptr : clients_vec)
            if (ptr->getRPCAddress() == host_with_rpc_port)
                return ptr;

        return {};
    }

    Ptr get()
    {
        std::unique_lock lock(state_mutex);
        updateIfNeedUnlocked(lock);

        std::uniform_int_distribution dist;
        while (!clients_vec.empty())
        {
            size_t i = dist(thread_local_rng) % clients_vec.size();
            if (clients_vec[i]->ok())
                return clients_vec[i];

            /// Remove unavailable client
            clients_vec[i] = clients_vec.back();
            clients_vec.pop_back();
        }

        throw Exception(ErrorCodes::NO_SUCH_SERVICE, "No available service for {}", service_name);
    }

    Ptr getByHashRing(const String & key)
    {
        std::unique_lock lock(state_mutex);
        updateIfNeedUnlocked(lock);

        /// The `clients_ring_map` is not necessary for all now, initialize here
        if (rpc_ring_map.empty())
            initOrUpdateRpcRingMap();

        while (!rpc_ring_map.empty())
        {
            auto rpc_address = rpc_ring_map.find(key);
            if (rpc_address.empty())
                break;

            if (auto it = rpc_clients_map.find(rpc_address); it != rpc_clients_map.end() && it->second->ok())
                return it->second;

            rpc_ring_map.erase(rpc_address);
        }

        throw Exception("No available service for " + service_name, ErrorCodes::LOGICAL_ERROR);
    }

    void initOrUpdateRpcRingMap()
    {
        rpc_ring_map.clear();
        rpc_clients_map.clear();
        for (auto & p : clients_map)
        {
            rpc_ring_map.insert(p.first.getRPCAddress());
            rpc_clients_map.emplace(p.first.getRPCAddress(), p.second);
        }
    }

    bool empty()
    {
        std::unique_lock lock(state_mutex);
        updateIfNeedUnlocked(lock);
        return clients_map.empty();
    }

    std::vector<Ptr> getAll()
    {
        std::unique_lock lock(state_mutex);
        updateIfNeedUnlocked(lock);

        return clients_vec;
    }

    HostWithPortsVec getAllHostWithPorts()
    {
        std::unique_lock lock(state_mutex);
        updateIfNeedUnlocked(lock);

        HostWithPortsVec clients;
        for (const auto & client : clients_map)
            clients.push_back(client.first);
        return clients;
    }

private:
    void updateIfNeedUnlocked(std::unique_lock<std::mutex> & lock)
    {
        time_t now = time(nullptr);
        if (clients_vec.empty() && now - last_update_time > error_update_interval)
            updateUnlocked(lock);
        else if (now - last_update_time > normal_update_interval)
            updateUnlocked(lock);
    }

    void updateUnlocked(std::unique_lock<std::mutex> &)
    {
        auto endpoints = lookup();
        last_update_time = time(nullptr);

        if (endpoints.empty())
        {
            LOG_INFO(log, "Empty service found in {}, will use old services", service_name);
            return;
        }

        ClientsMap new_clients_map;
        for (auto & ep : endpoints)
        {
            if (auto iter = clients_map.find(ep); iter != clients_map.end())
            {
                iter->second->reset();
                new_clients_map.insert(clients_map.extract(iter));
            }
            else
            {
                try
                {
                    new_clients_map.try_emplace(ep, creator(ep));
                }
                catch (...)
                {
                    LOG_ERROR(log, "Failed to create RPC client to {}: {}", ep.toDebugString(), getCurrentExceptionMessage(false));
                }
            }
        }

        clients_map = std::move(new_clients_map);
        clients_vec.clear();
        for (auto & p : clients_map)
            clients_vec.push_back(p.second);
        // if (!rpc_ring_map.empty())
        //     initOrUpdateRpcRingMap();
    }

private:
    const String service_name;
    std::function<HostWithPortsVec()> lookup;
    std::function<Ptr(HostWithPorts)> creator;
    Poco::Logger * log;

    mutable std::mutex state_mutex;

    time_t normal_update_interval{30};
    time_t error_update_interval{1};
    time_t last_update_time{0};

    std::vector<Ptr> clients_vec;

    /// keep the clients sorted by host_port.
    ClientsMap clients_map;

    /// for select node by consistent hash
    DB::ConsistentHashRing rpc_ring_map;
    std::unordered_map<String, Ptr> rpc_clients_map;
};

} /// end of namespace DB
