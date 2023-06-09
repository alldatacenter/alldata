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

#include <array>
#include <mutex>
#include <string>
#include <unordered_map>
#include <Core/Types.h>
#include <brpc/channel.h>

namespace DB
{
class BrpcChannelPoolOptions
{
public:
    static const std::string DEFAULT_CONFIG_KEY;
    static const std::string STREAM_DEFAULT_CONFIG_KEY;
    struct PoolOptions
    {
        std::string pool_name;

        uint16_t max_connections;

        std::string load_balancer;

        brpc::ChannelOptions channel_options;

        bool operator==(const PoolOptions & options) const
        {
            if (this->pool_name != options.pool_name || this->max_connections != options.max_connections
                || this->load_balancer != options.load_balancer)
            {
                return false;
            }
            if (this->channel_options.connect_timeout_ms != options.channel_options.connect_timeout_ms
                || this->channel_options.timeout_ms != options.channel_options.timeout_ms
                || this->channel_options.backup_request_ms != options.channel_options.backup_request_ms
                || this->channel_options.max_retry != options.channel_options.max_retry
                || this->channel_options.enable_circuit_breaker != options.channel_options.enable_circuit_breaker
                || this->channel_options.log_succeed_without_server != options.channel_options.log_succeed_without_server
                || this->channel_options.succeed_without_server != options.channel_options.succeed_without_server
                || this->channel_options.connection_group != options.channel_options.connection_group)
            {
                return false;
            }
            return true;
        }

        bool operator!=(const PoolOptions & options) const { return !(*this == options); }
    };
    using PoolOptionsMap = std::unordered_map<std::string, BrpcChannelPoolOptions::PoolOptions>;

private:

    PoolOptionsMap options_map;

public:
#define STR(M) #M
#define DEFAULT_STR(M) STR(M##_default)

// rpc default config params
#define APPLY_FOR_RPC_PARAM(M) \
    M(uint16_t, rpc_default_max_connections, 2, "max connections") \
    M(std::string, rpc_default_load_balancer, "rr", "") \
    M(int32_t, rpc_default_connect_timeout_ms, 1000, "") \
    M(int32_t, rpc_default_timeout_ms, 3000, "") \
    M(int32_t, rpc_default_backup_request_ms, -1, "") \
    M(int, rpc_default_max_retry, 3, "") \
    M(bool, rpc_default_enable_circuit_breaker, false, "") \
    M(bool, rpc_default_log_succeed_without_server, true, "") \
    M(bool, rpc_default_succeed_without_server, true, "")

// stream default config params
#define APPLY_FOR_STREAM_PARAM(M) \
    M(uint16_t, stream_default_max_connections, 4, "max connections") \
    M(std::string, stream_default_load_balancer, "rr", "") \
    M(int32_t, stream_default_connect_timeout_ms, 1000, "") \
    M(int32_t, stream_default_timeout_ms, 3000, "") \
    M(int32_t, stream_default_backup_request_ms, -1, "") \
    M(int, stream_default_max_retry, 3, "") \
    M(bool, stream_default_enable_circuit_breaker, false, "") \
    M(bool, stream_default_log_succeed_without_server, true, "") \
    M(bool, stream_default_succeed_without_server, true, "")


#define DECLARE(TYPE, NAME, DEFAULT, DESCRIPTION) static const TYPE NAME;

    APPLY_FOR_RPC_PARAM(DECLARE)
    APPLY_FOR_STREAM_PARAM(DECLARE)

#undef DECLARE

    static BrpcChannelPoolOptions & getInstance()
    {
        static BrpcChannelPoolOptions instance;
        static std::once_flag flag;
        std::call_once(flag, [](){
            #define FILL(P) \
                auto & P##_pool_options = instance.options_map[DEFAULT_STR(P)]; \
                P##_pool_options.pool_name = DEFAULT_STR(P); \
                P##_pool_options.max_connections = P##_default_max_connections; \
                P##_pool_options.load_balancer = P##_default_load_balancer; \
                P##_pool_options.channel_options.connect_timeout_ms = P##_default_connect_timeout_ms; \
                P##_pool_options.channel_options.timeout_ms = P##_default_timeout_ms; \
                P##_pool_options.channel_options.backup_request_ms = P##_default_backup_request_ms; \
                P##_pool_options.channel_options.max_retry = P##_default_max_retry; \
                P##_pool_options.channel_options.enable_circuit_breaker = P##_default_enable_circuit_breaker; \
                P##_pool_options.channel_options.log_succeed_without_server = P##_default_log_succeed_without_server; \
                P##_pool_options.channel_options.succeed_without_server = P##_default_succeed_without_server;

                FILL(rpc)
                FILL(stream)
            #undef FILL
        });
        return instance;
    }

    const PoolOptions * getDefaultPoolOptions(std::string key);


private:
    BrpcChannelPoolOptions() = default;
};

std::ostream & operator<<(std::ostream & os, const BrpcChannelPoolOptions::PoolOptions & pool_options);

}
