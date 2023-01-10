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

#include <memory>
#include <Processors/Exchange/DataTrans/IBroadcastSender.h>
#include <Processors/Exchange/LoadBalancedExchangeSink.h>
#include <Processors/Exchange/IExchangeSink.h>
#include <Processors/Transforms/AggregatingTransform.h>
#include <common/types.h>
#include <Common/Exception.h>
#include <common/logger_useful.h>
#include <Processors/Exchange/ExchangeUtils.h>
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int EXCHANGE_DATA_TRANS_EXCEPTION;
}

class RoundRobinSelector : public LoadBalancedExchangeSink::LoadBalanceSelector
{
public:
    explicit RoundRobinSelector(size_t partition_num_) : LoadBalanceSelector(partition_num_) { }
    virtual size_t selectNext() override { return count++ % partition_num; }

private:
    UInt32 count = rand(); // NOLINT
};

LoadBalancedExchangeSink::LoadBalancedExchangeSink(Block header_, BroadcastSenderPtrs senders_)
    : IExchangeSink(std::move(header_))
    , senders(std::move(senders_))
    , partition_selector(std::make_unique<RoundRobinSelector>(senders.size()))
    , logger(&Poco::Logger::get("LoadBalancedExchangeSink"))
{
}

LoadBalancedExchangeSink::~LoadBalancedExchangeSink() = default;


void LoadBalancedExchangeSink::consume(Chunk chunk)
{
    auto status = ExchangeUtils::sendAndCheckReturnStatus(*senders[partition_selector->selectNext()], std::move(chunk));
    if (status.code != BroadcastStatusCode::RUNNING)
        finish();
}

void LoadBalancedExchangeSink::onFinish()
{
    LOG_TRACE(logger, "LoadBalancedExchangeSink finish");
}

void LoadBalancedExchangeSink::onCancel()
{
    LOG_TRACE(logger, "LoadBalancedExchangeSink cancel");
    for (const BroadcastSenderPtr & sender : senders)
        sender->finish(BroadcastStatusCode::SEND_CANCELLED, "Cancelled by pipeline");
}

}
