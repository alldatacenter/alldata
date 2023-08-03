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

#include "BrpcRemoteBroadcastReceiver.h"

#include <Core/Block.h>
#include <Processors/Exchange/DataTrans/BoundedDataQueue.h>
#include <Interpreters/Context.h>
#include <brpc/channel.h>
#include <common/logger_useful.h>

namespace DB
{
class StreamHandler : public brpc::StreamInputHandler
{
public:
    StreamHandler(const ContextPtr & context_, BrpcRemoteBroadcastReceiverShardPtr receiver_, Block header_, bool keep_order_)
        : context(context_), receiver(receiver_), header(std::move(header_)), keep_order(keep_order_)
    {
    }

    int on_received_messages(brpc::StreamId id, butil::IOBuf * const * messages, size_t size) noexcept override;

    void on_idle_timeout(brpc::StreamId id) override;

    void on_closed(brpc::StreamId id) override;

    void on_finished(brpc::StreamId id, int32_t finish_status_code) override;
private:
    ContextPtr context;
    Poco::Logger * log = &Poco::Logger::get("StreamHandler");
    BrpcRemoteBroadcastReceiverWeakPtr receiver;
    Block header;
    bool keep_order;
};

}
