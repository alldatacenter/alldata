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

#include <iostream>
#include <Common/Brpc/BrpcChannelPoolOptions.h>

namespace DB
{
#define DEFINE(TYPE, NAME, DEFAULT, DESCRIPTION) const TYPE BrpcChannelPoolOptions::NAME{DEFAULT};

APPLY_FOR_RPC_PARAM(DEFINE)
APPLY_FOR_STREAM_PARAM(DEFINE)

#undef DEFINE
const std::string BrpcChannelPoolOptions::DEFAULT_CONFIG_KEY = "rpc_default";
const std::string BrpcChannelPoolOptions::STREAM_DEFAULT_CONFIG_KEY = "stream_default";

std::ostream & operator<<(std::ostream & os, const BrpcChannelPoolOptions::PoolOptions & pool_options)
{
    os << "PoolOptions@" << &pool_options << " {pool_name:" << pool_options.pool_name
       << ", max_connections:" << pool_options.max_connections << ", load_balancer:" << pool_options.load_balancer << ", ChannelOptions:{"
       << "connect_timout_ms:" << pool_options.channel_options.connect_timeout_ms
       << ", timeout_ms:" << pool_options.channel_options.timeout_ms
       << ", backup_request_ms:" << pool_options.channel_options.backup_request_ms
       << ", max_retry:" << pool_options.channel_options.max_retry
       << ", enable_circuit_breaker:" << (pool_options.channel_options.enable_circuit_breaker ? "true" : "false")
       << ", succeed_without_server:" << (pool_options.channel_options.succeed_without_server ? "true" : "false")
       << ", log_succeed_without_server:" << (pool_options.channel_options.log_succeed_without_server ? "true" : "false")
       << ", connection_group:" << pool_options.channel_options.connection_group << "}"
       << "}";
    return os;
}

const BrpcChannelPoolOptions::PoolOptions * BrpcChannelPoolOptions::getDefaultPoolOptions(std::string key)
{
    auto itr = BrpcChannelPoolOptions::options_map.find(key);
    if (itr == BrpcChannelPoolOptions::options_map.end())
    {
        return nullptr;
    }
    return &itr->second;
}

}
