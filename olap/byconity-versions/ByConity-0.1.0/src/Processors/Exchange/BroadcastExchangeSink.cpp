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

#include <Common/Exception.h>
#include <common/logger_useful.h>

#include <Processors/Exchange/BroadcastExchangeSink.h>
#include <Processors/Exchange/DataTrans/IBroadcastSender.h>
#include <Processors/Exchange/ExchangeUtils.h>
#include <Processors/Exchange/IExchangeSink.h>
#include <Processors/ISource.h>
#include <Processors/Transforms/AggregatingTransform.h>
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>

namespace DB
{
BroadcastExchangeSink::BroadcastExchangeSink(Block header_, BroadcastSenderPtrs senders_, ExchangeOptions options_)
    : IExchangeSink(std::move(header_))
    , senders(std::move(senders_))
    , options(std::move(options_))
    , buffer_chunk(getPort().getHeader(), options.send_threshold_in_bytes, options.send_threshold_in_row_num)
    , logger(&Poco::Logger::get("BroadcastExchangeSink"))
{
    if (options.force_use_buffer)
        buffer_chunk.resetBuffer();
}

BroadcastExchangeSink::~BroadcastExchangeSink() = default;


void BroadcastExchangeSink::consume(Chunk chunk)
{
    Chunk chunk_to_send;
    if (options.force_use_buffer)
    {
        chunk_to_send = buffer_chunk.add(std::move(chunk));
        if (!chunk_to_send)
            return;
    }
    else
    {
        chunk_to_send = std::move(chunk);
    }

    bool has_active_sender = false;
    for (size_t i = 0; i < senders.size() - 1; ++i)
    {
        auto status = ExchangeUtils::sendAndCheckReturnStatus(*senders[i], chunk_to_send.clone());
        if (status.code == BroadcastStatusCode::RUNNING)
            has_active_sender = true;
    }

    auto status = ExchangeUtils::sendAndCheckReturnStatus(*senders.back(), std::move(chunk_to_send));
    if (status.code == BroadcastStatusCode::RUNNING)
        has_active_sender = true;

    if (!has_active_sender)
        finish();
}


void BroadcastExchangeSink::onFinish()
{
    LOG_TRACE(logger, "BroadcastExchangeSink finish");
    if (!options.force_use_buffer)
        return;

    auto chunk = buffer_chunk.flush(true);
    if (!chunk)
        return;
    for (auto & sender : senders)
    {
        ExchangeUtils::sendAndCheckReturnStatus(*sender, chunk.clone());
    }
}

void BroadcastExchangeSink::onCancel()
{
    LOG_TRACE(logger, "BroadcastExchangeSink cancel");

    for (auto & sender : senders)
    {
        sender->finish(BroadcastStatusCode::SEND_CANCELLED, "Cancelled by pipeline");
    }
}

}
