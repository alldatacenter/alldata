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

#include <atomic>
#include <Core/Types.h>
#include <boost/noncopyable.hpp>
#include <brpc/channel.h>
#include <brpc/controller.h>
#include <common/logger_useful.h>

namespace DB
{
class RpcClient : private boost::noncopyable
{
public:
    RpcClient(String host_port_, brpc::ChannelOptions * options = nullptr);
    ~RpcClient() = default;

    const auto & getAddress() const { return host_port; }
    bool ok() const { return ok_.load(std::memory_order_relaxed); }
    void reset() { ok_.store(true, std::memory_order_relaxed); }
    void checkAliveWithController(const brpc::Controller & cntl) noexcept;

    auto & getChannel() { return *brpc_channel; }

    void assertController(const brpc::Controller & cntl);

protected:
    void initChannel(brpc::Channel & channel_, const String host_port_, brpc::ChannelOptions * options = nullptr);

    Poco::Logger * log;
    String host_port;

    std::unique_ptr<brpc::Channel> brpc_channel;
    std::atomic_bool ok_{true};
};

}
