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

#include <Core/Types.h>
#include <Common/HostWithPorts.h>

#include <atomic>
#include <boost/noncopyable.hpp>
#include <common/logger_useful.h>

namespace brpc
{
class Channel;
class ChannelOptions;
class Controller;
}

namespace DB
{
class RpcClientBase : private boost::noncopyable
{
public:
    RpcClientBase(const String & log_prefix, const String & host_port, brpc::ChannelOptions * options = nullptr);
    RpcClientBase(const String & log_prefix, HostWithPorts host_ports_, brpc::ChannelOptions * options = nullptr);
    virtual ~RpcClientBase();

    String getRPCAddress() const { return host_ports.getRPCAddress(); }
    String getTCPAddress() const { return host_ports.getTCPAddress(); }
    const String & getHostWithPortsID() const { return host_ports.id; }
    const auto & getHostWithPorts() const { return host_ports; }

    bool ok() const { return ok_.load(std::memory_order_relaxed); }
    void reset() { ok_.store(true, std::memory_order_relaxed); }

    auto & getChannel() { return *channel; }

    auto getActiveTime() const { return time(nullptr) - start_up_time; }

protected:
    void assertController(const brpc::Controller & cntl);
    void initChannel(brpc::Channel & channel_, const String & host_port_, brpc::ChannelOptions * options = nullptr);

    Poco::Logger * log;
    HostWithPorts host_ports;

    std::unique_ptr<brpc::Channel> channel;
    std::atomic_bool ok_{true};
    time_t start_up_time{0};
    std::unique_ptr<brpc::ChannelOptions> default_options;
};

}
