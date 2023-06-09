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

#include <Core/Block.h>
#include <Processors/Chunk.h>
#include <Processors/Exchange/DataTrans/DataTransKey.h>
#include <brpc/stream.h>
#include <Poco/Logger.h>
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>
#include <Processors/Exchange/DataTrans/IBroadcastReceiver.h>
#include <Processors/Exchange/DataTrans/Brpc/AsyncRegisterResult.h>

#include <atomic>
#include <vector>

namespace DB
{
struct BrpcRecvMetric
{
    size_t recv_time_ms{0};
    size_t register_time_ms{0};
    size_t recv_bytes{0};
    size_t dser_time_ms{0};
    Int32 finish_code{};
    Int8 is_modifier{-1};
    String message;
};

class BrpcRemoteBroadcastReceiver : public std::enable_shared_from_this<BrpcRemoteBroadcastReceiver>, public IBroadcastReceiver
{
public:
    BrpcRemoteBroadcastReceiver(DataTransKeyPtr trans_key_, String registry_address_, ContextPtr context_, Block header_, bool keep_order_);
    ~BrpcRemoteBroadcastReceiver() override;

    void registerToSenders(UInt32 timeout_ms) override;
    RecvDataPacket recv(UInt32 timeout_ms) noexcept override;
    BroadcastStatus finish(BroadcastStatusCode status_code_, String message) override;
    String getName() const override;
    void pushReceiveQueue(Chunk chunk);
    void setSendDoneFlag() { send_done_flag.test_and_set(std::memory_order_release); }

    AsyncRegisterResult registerToSendersAsync(UInt32 timeout_ms);
    BrpcRecvMetric metric;
private:
    Poco::Logger * log = &Poco::Logger::get("BrpcRemoteBroadcastReceiver");
    DataTransKeyPtr trans_key;
    String registry_address;
    ContextPtr context;
    Block header;
    std::atomic<BroadcastStatusCode> finish_status_code{BroadcastStatusCode::RUNNING};
    std::atomic_flag send_done_flag = ATOMIC_FLAG_INIT;
    BoundedDataQueue<Chunk> queue;
    String data_key;
    brpc::StreamId stream_id{brpc::INVALID_STREAM_ID};

    bool keep_order;
};

using BrpcRemoteBroadcastReceiverShardPtr = std::shared_ptr<BrpcRemoteBroadcastReceiver>;
using BrpcRemoteBroadcastReceiverWeakPtr = std::weak_ptr<BrpcRemoteBroadcastReceiver>;
using BrpcReceiverPtrs = std::vector<BrpcRemoteBroadcastReceiverShardPtr>;
}
