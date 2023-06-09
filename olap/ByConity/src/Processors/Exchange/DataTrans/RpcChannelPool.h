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

#include <Processors/Exchange/DataTrans/RpcClient.h>
#include <Common/Brpc/BrpcChannelPoolConfigHolder.h>
#include <Common/Brpc/BrpcApplication.h>
#include <Poco/DateTime.h>
#include <Poco/DateTimeFormatter.h>

namespace DB
{

///  Singleton class, use std::cout for log
class RpcChannelPool : public boost::noncopyable
{
public:
private:
    struct Pool
    {
        std::vector<std::shared_ptr<RpcClient>> clients;
        std::atomic<UInt32> counter{0};

        Pool() = default;
        explicit Pool(std::vector<std::shared_ptr<RpcClient>>::size_type n)
        {
            this->clients = std::vector<std::shared_ptr<RpcClient>>(n, nullptr);
        }
    };
    using PoolOptionsMap = BrpcChannelPoolConfigHolder::PoolOptionsMap;
    using ClientType = std::string;
    using HostPort = std::string;

public:
    static RpcChannelPool & getInstance()
    {
        static RpcChannelPool pool;
        return pool;
    }

    std::shared_ptr<RpcClient> getClient(const String & host_port, const std::string & client_type, bool connection_reuse = true);

    // carefully, calling this method will trigger the lock on @pool_options_map.
    PoolOptionsMap getDefaultChannelPoolOptions()
    {
        std::unique_lock<std::mutex> lock(pool_options_map_mutex);
        return this->pool_options_map;
    }

private:
    std::shared_ptr<BrpcChannelPoolConfigHolder> channel_pool_config_holder;

    PoolOptionsMap pool_options_map;
    std::mutex pool_options_map_mutex;
    std::unordered_map<ClientType, std::unordered_map<HostPort, Pool>> channel_pool;
    std::unordered_map<ClientType, std::unique_ptr<std::mutex>> mutexes;

    inline PoolOptionsMap getChannelPoolOptions() { return channel_pool_config_holder->queryConfig(); }

    RpcChannelPool()
    {
        channel_pool_config_holder = BrpcApplication::getInstance().getConfigHolderByType<BrpcChannelPoolConfigHolder>();

        pool_options_map = getChannelPoolOptions(); // init pool_options_map

        // can't init channel_pool here

        for (const auto & pair : pool_options_map) // init mutexes
        {
            // rpc_default key is supposed to be included.
            mutexes.emplace(pair.first, std::make_unique<std::mutex>());
        }

        // lock in the granularity of client_type if the options change after reload
        auto channel_pool_reload_callback = [this](const PoolOptionsMap * old_conf_to_del, const PoolOptionsMap * readonly_new_conf) {
            std::unique_lock pool_options_map_lock(pool_options_map_mutex);

            // normally, there should be NO insert or delete in pool options
            if (old_conf_to_del->size() > readonly_new_conf->size())
            {
                std::cout << Poco::DateTimeFormatter::format(Poco::DateTime(), "%Y.%m.%d %H:%M:%S.%i") << " <Error> "
                          << "RpcChannelPool::RpcChannelPool "
                          << "Existing pool options should NOT be deleted! Reload callback suspend.";
                return;
            }
            else if (old_conf_to_del->size() < readonly_new_conf->size())
            {
                std::cout << Poco::DateTimeFormatter::format(Poco::DateTime(), "%Y.%m.%d %H:%M:%S.%i") << " <Warning> "
                          << "RpcChannelPool::RpcChannelPool "
                          << "Existing pool options should NOT be added. Added ones will be omitted.";
            }

            for (const auto & pair : *old_conf_to_del)
            {
                const auto & key = pair.first;
                auto itr = readonly_new_conf->find(key);
                if (itr == readonly_new_conf->end())
                {
                    std::cout << Poco::DateTimeFormatter::format(Poco::DateTime(), "%Y.%m.%d %H:%M:%S.%i") << " <Error> "
                              << "RpcChannelPool::RpcChannelPool "
                              << "Existing pool options should NOT be deleted! Reload callback suspend, but some might have updated.";
                    return;
                }
                if (itr->second != pair.second) // update
                {
                    auto & mutex = mutexes.at(pair.first);
                    std::unique_lock<std::mutex> lock(*mutex);
                    pool_options_map[key] = readonly_new_conf->find(key)->second;
                    channel_pool[key].clear();
                    lock.unlock();
                }
            }
        };
        channel_pool_config_holder->initReloadCallback(channel_pool_reload_callback);
    }
};

}
