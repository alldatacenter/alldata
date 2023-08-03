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

#include <CloudServices/RpcClientBase.h>

#include <errno.h>
#include <brpc/channel.h>
#include <brpc/controller.h>
#include <brpc/errno.pb.h>
#include <Common/Exception.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int BRPC_CANNOT_INIT_CHANNEL;
    extern const int BRPC_EXCEPTION;
    extern const int BRPC_TIMEOUT;
    extern const int BRPC_HOST_DOWN;
    extern const int BRPC_CONNECT_ERROR;
}

static auto getDefaultChannelOptions()
{
    brpc::ChannelOptions options;
    options.timeout_ms = 3000;
    return options;
}

RpcClientBase::RpcClientBase(const String & log_prefix, const String & host_port, brpc::ChannelOptions * options)
    : RpcClientBase(log_prefix, HostWithPorts::fromRPCAddress(host_port), options)
{
}

RpcClientBase::RpcClientBase(const String & log_prefix, HostWithPorts host_ports_, brpc::ChannelOptions * options)
    : log(&Poco::Logger::get(log_prefix + "[" + host_ports_.toDebugString() + "]"))
    , host_ports(std::move(host_ports_))
    , channel(std::make_unique<brpc::Channel>())
{
    initChannel(*channel, host_ports.getRPCAddress(), options);
}

RpcClientBase::~RpcClientBase()
{
}

void RpcClientBase::assertController(const brpc::Controller & cntl)
{
    if (cntl.Failed())
    {
        auto err = cntl.ErrorCode();

        if (err == ECONNREFUSED || err == ECONNRESET || err == ENETUNREACH)
        {
            ok_.store(false, std::memory_order_relaxed);
            throw Exception(std::to_string(err) + ":" + cntl.ErrorText(), ErrorCodes::BRPC_CONNECT_ERROR);
        }
        else if (err == EHOSTDOWN)
        {
            /// TODO: handle more error codes, temporarily remove EHOSTDOWN error https://github.com/apache/incubator-brpc/issues/936

            throw Exception(std::to_string(err) + ":" + cntl.ErrorText(), ErrorCodes::BRPC_HOST_DOWN);
        }
        else if (err == brpc::Errno::ERPCTIMEDOUT)
        {
            throw Exception(std::to_string(err) + ":" + cntl.ErrorText(), ErrorCodes::BRPC_TIMEOUT);
        }
        else /// Should we throw exception here to cover all other errors?
            throw Exception(std::to_string(err) + ":" + cntl.ErrorText(), ErrorCodes::BRPC_EXCEPTION);
    }
    else
    {
        ok_.store(true, std::memory_order_relaxed);
    }
}

void RpcClientBase::initChannel(brpc::Channel & channel_, const String & host_port_, brpc::ChannelOptions * options)
{
    /// Init only once
    if (!default_options)
        default_options = std::make_unique<brpc::ChannelOptions>(getDefaultChannelOptions());

    if (0 != channel_.Init(host_port_.c_str(), options ? options : default_options.get()))
        throw Exception(ErrorCodes::BRPC_CANNOT_INIT_CHANNEL, "Failed to initialize RPC channel to {}", host_port_);

    start_up_time = time(nullptr);

    LOG_DEBUG(log, "Initialized rpc channel to server with address: {}", host_port_);
}

}
