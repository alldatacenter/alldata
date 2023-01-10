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

#include <TSO/TSOProxy.h>
#include <TSO/Defines.h>
#include <Protos/tso.pb.h>
#include <Poco/Logger.h>
#include <atomic>

namespace DB
{

class KeeperDispatcher;

namespace TSO
{

struct TSOClock
{
    UInt64 physical;
    UInt32 logical;
};

class TSOServer;

class TSOImpl : public TSO
{

public:
    explicit TSOImpl();

    ~TSOImpl() override;

    void setPhysicalTime(UInt64 time);

    void setIsLeader(bool is_leader_) { is_leader.store(is_leader_, std::memory_order_release); }

    TSOClock getClock() const
    {
        UInt64 timestamp = ts.load(std::memory_order_acquire);
        TSOClock clock = {ts_to_physical(timestamp), UInt32 ts_to_logical(timestamp)};
        return clock;
    }

    void GetTimestamp(
        ::google::protobuf::RpcController* /*controller*/,
        const ::DB::TSO::GetTimestampReq* request,
        ::DB::TSO::GetTimestampResp* response,
        ::google::protobuf::Closure* done) override;

    void GetTimestamps(
        ::google::protobuf::RpcController* /*controller*/,
        const ::DB::TSO::GetTimestampsReq* request,
        ::DB::TSO::GetTimestampsResp* response,
        ::google::protobuf::Closure* done) override;

private:
    std::atomic<UInt64> ts = 0;
    std::atomic_bool is_leader{false};
    Poco::Logger * log = &Poco::Logger::get("TSOImpl");
    std::atomic<bool> logical_clock_checking {false};

    UInt64 fetchAddLogical(UInt32 to_add);
};

}

}
