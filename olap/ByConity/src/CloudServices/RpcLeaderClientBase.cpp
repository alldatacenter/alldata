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

#include <CloudServices/RpcLeaderClientBase.h>
#include <Common/Exception.h>
#include <brpc/channel.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int BRPC_CANNOT_INIT_CHANNEL;
}

brpc::Channel & RpcLeaderClientBase::updateChannel(const String & host_port, brpc::ChannelOptions * options_)
{
    std::lock_guard lock(host_port_mutex);
    leader_host_port = host_port;
    if (0 != channel->Init(leader_host_port.c_str(), options_ ? options_ : default_options.get()))
        throw Exception("Failed to update RPC channel to " + leader_host_port, ErrorCodes::BRPC_CANNOT_INIT_CHANNEL);

    return getChannel();
}

}
