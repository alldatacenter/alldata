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
#include <mutex>
#include <vector>
#include <Interpreters/Context.h>
#include <Processors/Chunk.h>
#include <Processors/Exchange/DataTrans/DataTransKey.h>
#include <Processors/Exchange/DataTrans/IBroadcastSender.h>
#include <brpc/stream.h>
#include <bthread/mtx_cv_base.h>
#include <Processors/Exchange/DataTrans/Brpc/WriteBufferFromBrpcBuf.h>

namespace DB
{
struct BrpcSendMetric
{
    std::atomic<size_t> send_time_ms{0};
    std::atomic<size_t> send_rows{0};
    std::atomic<size_t> send_uncompressed_bytes{0};
    std::atomic<size_t> send_bytes{0};
    std::atomic<size_t> num_send_times{0};
    std::atomic<size_t> ser_time_ms{0};
    std::atomic<size_t> send_retry{0};
    std::atomic<size_t> send_retry_ms{0};
    std::atomic<size_t> overcrowded_retry{0};
    std::atomic<Int32> finish_code{};
    std::atomic<Int8> is_modifier{-1};
    String message;
};

class BrpcRemoteBroadcastSender : public IBroadcastSender
{
public:
    BrpcRemoteBroadcastSender(DataTransKeyPtr trans_key_, brpc::StreamId stream_id, ContextPtr context_, Block header_);
    ~BrpcRemoteBroadcastSender() override;

    BroadcastStatus send(Chunk chunk) noexcept override;
    BroadcastStatus finish(BroadcastStatusCode status_code_, String message) override;

    /// Merge another BrpcRemoteBroadcastSender to this sender, to simplify code, we assume that no member method is called concurrently
    void merge(IBroadcastSender && sender) override;
    String getName() const override;
    BroadcastSenderType getType() override { return BroadcastSenderType::Brpc; }

private:
    Poco::Logger * log = &Poco::Logger::get("BrpcRemoteBroadcastSender");
    DataTransKeyPtrs trans_keys;
    ContextPtr context;
    Block header;
    std::vector<brpc::StreamId> sender_stream_ids;
    BrpcSendMetric metric;

    BroadcastStatus sendIOBuffer(const butil::IOBuf & io_buffer, brpc::StreamId stream_id, const String & data_key);
    void serializeChunkToIoBuffer(Chunk chunk, WriteBufferFromBrpcBuf & out) const;
};
}
