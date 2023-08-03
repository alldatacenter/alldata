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
#include <cstddef>
#include <Interpreters/QueryExchangeLog.h>
#include <Processors/Chunk.h>
#include <Processors/Exchange/DataTrans/BoundedDataQueue.h>
#include <Processors/Exchange/DataTrans/Brpc/BrpcRemoteBroadcastReceiver.h>
#include <Processors/Exchange/DataTrans/Brpc/BrpcRemoteBroadcastSender.h>
#include <Processors/Exchange/DataTrans/DataTransKey.h>
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>
#include <Processors/Exchange/DataTrans/IBroadcastReceiver.h>
#include <Processors/Exchange/DataTrans/IBroadcastSender.h>
#include <Processors/Exchange/DataTrans/Local/LocalChannelOptions.h>
#include <boost/noncopyable.hpp>
#include <Poco/Logger.h>
#include <common/types.h>

namespace DB
{
class LocalBroadcastChannel final : public IBroadcastReceiver,
                                    public IBroadcastSender,
                                    public std::enable_shared_from_this<LocalBroadcastChannel>,
                                    boost::noncopyable
{
public:
    explicit LocalBroadcastChannel(
        DataTransKeyPtr data_key_, LocalChannelOptions options_, std::shared_ptr<QueryExchangeLog> query_exchange_log_ = nullptr);
    RecvDataPacket recv(UInt32 timeout_ms) override;
    void registerToSenders(UInt32 timeout_ms) override;
    void merge(IBroadcastSender &&) override;
    String getName() const override;
    BroadcastSenderType getType() override { return BroadcastSenderType::Local; }
    BroadcastStatus finish(BroadcastStatusCode status_code, String message) override;
    BroadcastStatus send(Chunk chunk) override;
    ~LocalBroadcastChannel() override;

private:
    BrpcRecvMetric recv_metric;
    BrpcSendMetric send_metric;
    DataTransKeyPtr data_key;
    LocalChannelOptions options;
    BoundedDataQueue<Chunk> receive_queue;
    BroadcastStatus init_status{BroadcastStatusCode::RUNNING, false, "init"};
    std::atomic<BroadcastStatus *> broadcast_status{&init_status};
    Poco::Logger * logger;
    std::shared_ptr<QueryExchangeLog> query_exchange_log;
};
}
