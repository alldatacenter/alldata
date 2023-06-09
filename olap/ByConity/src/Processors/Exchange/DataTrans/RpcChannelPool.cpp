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

#include <memory>
#include <mutex>
#include <string>
#include <Processors/Exchange/DataTrans/RpcChannelPool.h>
#include <brpc/options.pb.h>
#include <fmt/format.h>

namespace DB
{
std::shared_ptr<RpcClient> RpcChannelPool::getClient(const String & host_port, const std::string & client_type, bool connection_reuse)
{
    auto mutex_itr = this->mutexes.find(client_type);
    auto & mutex = mutex_itr == mutexes.end() ? mutexes.at(BrpcChannelPoolOptions::DEFAULT_CONFIG_KEY) : mutex_itr->second;
    std::unique_lock lock(*mutex);

    auto itr = pool_options_map.find(client_type);

    // Get the options of the given client_type, otherwise get the default config.
    if (itr == pool_options_map.end())
    {
        std::cout << Poco::DateTimeFormatter::format(Poco::DateTime(), "%Y.%m.%d %H:%M:%S.%i") << " <Warning> "
                  << "RpcChannelPool::getClient "
                  << "The given client_type is not in config! <rpc_default> config will be taken.";
        itr = pool_options_map.find(BrpcChannelPoolOptions::DEFAULT_CONFIG_KEY);
        if (itr == pool_options_map.end())
        {
            std::cout << Poco::DateTimeFormatter::format(Poco::DateTime(), "%Y.%m.%d %H:%M:%S.%i") << " <Error> "
                      << "RpcChannelPool::getClient "
                      << "<rpc_default> is not in config. The default config should be taken but it's not. Check "
                         "BrpcChannelPoolConfigHolder::createTypedConfig. Return nullptr.";
            return nullptr;
        }
    }
    const BrpcChannelPoolOptions::PoolOptions & pool_options = itr->second;
    const auto & max_connections = pool_options.max_connections;
    auto & pool = channel_pool[client_type][host_port];
    if (connection_reuse && pool.clients.empty())
    {
        pool.clients.resize(max_connections, nullptr);
    }
    if (likely(pool_options.load_balancer == "rr")) // round robin
    {
        auto & index = pool.counter;
        auto & pool_clients = pool.clients;
        index = (++index) % pool_options.max_connections;
        if (connection_reuse && pool_clients.at(index) != nullptr)
        {
            return pool_clients.at(index);
        }
        else
        {
            auto connection_pool_options = pool_options.channel_options;
            if (static_cast<brpc::ConnectionType>(connection_pool_options.connection_type) == brpc::ConnectionType::CONNECTION_TYPE_SINGLE)
            {
                connection_pool_options.connection_group = pool_options.pool_name + "_" + std::to_string(index);
            }
            if (connection_reuse)
            {
                pool_clients[index] = std::make_shared<RpcClient>(host_port, &connection_pool_options);
                return pool_clients[index];
            }
            else
            {
                return std::make_shared<RpcClient>(host_port, &connection_pool_options);
            }
        }
    }
    else
    {
        // TODO: other load balancer
        std::cout << Poco::DateTimeFormatter::format(Poco::DateTime(), "%Y.%m.%d %H:%M:%S.%i") << " <Warning> "
                  << "RpcChannelPool::getClient "
                  << "The given load balancer is not defined. Return nullptr.";
    }
    return nullptr;
}

} // namespace DB
