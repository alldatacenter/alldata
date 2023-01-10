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

#include <atomic>
#include <Interpreters/QueryExchangeLog.h>
#include <memory>
#include <optional>
#include <string>
#include <Processors/Chunk.h>
#include <Processors/Exchange/DataTrans/BroadcastSenderProxy.h>
#include <Processors/Exchange/DataTrans/BroadcastSenderProxyRegistry.h>
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>
#include <Processors/Exchange/DataTrans/Local/LocalBroadcastChannel.h>
#include <Processors/Exchange/DataTrans/Local/LocalChannelOptions.h>
#include <Poco/Logger.h>
#include <common/logger_useful.h>
#include <common/types.h>
#include <Processors/Exchange/ExchangeUtils.h>

namespace DB
{
LocalBroadcastChannel::LocalBroadcastChannel(DataTransKeyPtr data_key_, LocalChannelOptions options_, std::shared_ptr<QueryExchangeLog> query_exchange_log_)
    : data_key(std::move(data_key_))
    , options(std::move(options_))
    , receive_queue(options_.queue_size)
    , logger(&Poco::Logger::get("LocalBroadcastChannel"))
    , query_exchange_log(query_exchange_log_)
{
}

RecvDataPacket LocalBroadcastChannel::recv(UInt32 timeout_ms)
{
    Stopwatch s;
    Chunk recv_chunk;

    BroadcastStatus * current_status_ptr = broadcast_status.load(std::memory_order_acquire);
    /// Positive status code means that we should close immediately and negative code means we should conusme all in flight data before close
    if (current_status_ptr->code > 0)
        return *current_status_ptr;

    if (receive_queue.tryPop(recv_chunk, timeout_ms))
    {
        if (recv_chunk)
        {
            recv_metric.recv_bytes += recv_chunk.bytes();
            ExchangeUtils::transferGlobalMemoryToThread(recv_chunk.allocatedBytes());
            return RecvDataPacket(std::move(recv_chunk));
        }
        else
            return RecvDataPacket(*broadcast_status.load(std::memory_order_acquire));
    }

    BroadcastStatus current_status = finish(
        BroadcastStatusCode::RECV_TIMEOUT,
        "Receive from channel " + data_key->getKey() + " timeout after ms: " + std::to_string(timeout_ms));
    recv_metric.recv_time_ms += s.elapsedMilliseconds();
    return current_status;
}


BroadcastStatus LocalBroadcastChannel::send(Chunk chunk)
{
    Stopwatch s;
    BroadcastStatus * current_status_ptr = broadcast_status.load(std::memory_order_acquire);
    if (current_status_ptr->code != BroadcastStatusCode::RUNNING)
        return *current_status_ptr;

    auto bytes = chunk.allocatedBytes();
    send_metric.send_uncompressed_bytes += bytes;
    if (receive_queue.tryEmplace(options.max_timeout_ms / 2, std::move(chunk)))
    {
        ExchangeUtils::transferThreadMemoryToGlobal(bytes);
        return *broadcast_status.load(std::memory_order_acquire);
    }

    BroadcastStatus current_status = finish(
        BroadcastStatusCode::SEND_TIMEOUT,
        "Send to channel " + data_key->getKey() + " timeout after ms: " + std::to_string(options.max_timeout_ms));
    send_metric.send_time_ms += s.elapsedMilliseconds();
    return current_status;
}


BroadcastStatus LocalBroadcastChannel::finish(BroadcastStatusCode status_code, String message)
{
    BroadcastStatus * current_status_ptr = &init_status;

    BroadcastStatus * new_status_ptr = new BroadcastStatus(status_code, false, message);

    if (broadcast_status.compare_exchange_strong(current_status_ptr, new_status_ptr, std::memory_order_release, std::memory_order_acquire))
    {
        LOG_INFO(
            logger,
            "{} BroadcastStatus from {} to {} with message: {}",
            data_key->getKey(),
            current_status_ptr->code,
            new_status_ptr->code,
            new_status_ptr->message);
        if (new_status_ptr->code > 0)
            // close queue immediately
            receive_queue.close();
        else
            receive_queue.tryEmplace(options.max_timeout_ms, Chunk());
        auto res = *new_status_ptr;
        res.is_modifer = true;
        send_metric.finish_code = new_status_ptr->code;
        send_metric.is_modifier = 1;
        send_metric.message = new_status_ptr->message;
        return res;
    }
    else
    {
        LOG_TRACE(
            logger, "Fail to change broadcast status to {}, current status is: {} ", new_status_ptr->code, current_status_ptr->code);
        send_metric.finish_code = current_status_ptr->code;
        send_metric.is_modifier = 0;
        delete new_status_ptr;
        return *current_status_ptr;
    }
}

void LocalBroadcastChannel::registerToSenders(UInt32 timeout_ms)
{
    Stopwatch s;
    auto sender_proxy = BroadcastSenderProxyRegistry::instance().getOrCreate(data_key);
    sender_proxy->waitAccept(timeout_ms);
    sender_proxy->becomeRealSender(shared_from_this());
    recv_metric.register_time_ms += s.elapsedMilliseconds();
}

void LocalBroadcastChannel::merge(IBroadcastSender &&)
{
    throw Exception("merge is not implemented for LocalBroadcastChannel", ErrorCodes::NOT_IMPLEMENTED);
}

String LocalBroadcastChannel::getName() const
{
    return "Local: " + data_key->getKey();
};

LocalBroadcastChannel::~LocalBroadcastChannel()
{
    try
    {
        auto * status = broadcast_status.load(std::memory_order_acquire);
        if (status != &init_status)
            delete status;
        QueryExchangeLogElement element;
        if (auto key = std::dynamic_pointer_cast<const ExchangeDataKey>(data_key))
        {
            element.initial_query_id = key->getQueryId();
            element.write_segment_id = std::to_string(key->getWriteSegmentId());
            element.read_segment_id = std::to_string(key->getReadSegmentId());
            element.partition_id = std::to_string(key->getParallelIndex());
            element.coordinator_address = key->getCoordinatorAddress();
        }
        element.type = "local";
        element.event_time =
            std::chrono::duration_cast<std::chrono::seconds>(
                std::chrono::system_clock::now().time_since_epoch()).count();
        // sender
        element.send_time_ms = send_metric.send_time_ms;
        element.send_rows = send_metric.send_rows;
        element.send_uncompressed_bytes = send_metric.send_uncompressed_bytes;

        element.finish_code = send_metric.finish_code;
        element.is_modifier = send_metric.is_modifier;
        element.message = send_metric.message;

        // receiver
        element.recv_time_ms = recv_metric.recv_time_ms;
        element.register_time_ms = recv_metric.register_time_ms;
        element.recv_bytes = recv_metric.recv_bytes;

        if (query_exchange_log)
        {
            query_exchange_log->add(element);
        }

    }
    catch (...)
    {
        tryLogCurrentException(logger);
    }
}
}
