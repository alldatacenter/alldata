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

#include <Processors/Exchange/DataTrans/Brpc/BrpcRemoteBroadcastReceiver.h>
#include <Processors/Exchange/DataTrans/Brpc/StreamHandler.h>
#include <Processors/Exchange/DeserializeBufTransform.h>
#include "BrpcRemoteBroadcastReceiver.h"
#include "StreamHandler.h"
#include <Interpreters/QueryExchangeLog.h>
#include <Processors/Exchange/DataTrans/DataTransException.h>
#include <Processors/Exchange/DataTrans/DataTransKey.h>
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>
#include <Processors/Exchange/DataTrans/RpcClient.h>
#include <Processors/Exchange/DataTrans/RpcChannelPool.h>
#include <Processors/Exchange/ExchangeUtils.h>
#include <Protos/registry.pb.h>
#include <brpc/stream.h>

#include <atomic>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int BRPC_EXCEPTION;
    extern const int DISTRIBUTE_STAGE_QUERY_EXCEPTION;
}

BrpcRemoteBroadcastReceiver::BrpcRemoteBroadcastReceiver(
    DataTransKeyPtr trans_key_, String registry_address_, ContextPtr context_, Block header_, bool keep_order_)
    : trans_key(std::move(trans_key_))
    , registry_address(std::move(registry_address_))
    , context(std::move(context_))
    , header(std::move(header_))
    , queue(context->getSettingsRef().exchange_remote_receiver_queue_size)
    , data_key(trans_key->getKey())
    , keep_order(keep_order_)
{
}

BrpcRemoteBroadcastReceiver::~BrpcRemoteBroadcastReceiver()
{
    try
    {
        if (stream_id != brpc::INVALID_STREAM_ID)
        {
            brpc::StreamClose(stream_id);
            LOG_TRACE(log, "Stream {} for {} @ {} Close", stream_id, data_key, registry_address);
        }
        QueryExchangeLogElement element;
        if(auto key = std::dynamic_pointer_cast<const ExchangeDataKey>(trans_key))
        {
            element.initial_query_id = key->getQueryId();
            element.write_segment_id = std::to_string(key->getWriteSegmentId());
            element.read_segment_id = std::to_string(key->getReadSegmentId());
            element.partition_id = std::to_string(key->getParallelIndex());
            element.coordinator_address = key->getCoordinatorAddress();
        }
        element.event_time =
            std::chrono::duration_cast<std::chrono::seconds>(
                std::chrono::system_clock::now().time_since_epoch()).count();
        element.recv_time_ms = metric.recv_time_ms;
        element.register_time_ms = metric.register_time_ms;
        element.recv_bytes = metric.recv_bytes;
        element.dser_time_ms = metric.dser_time_ms;
        element.finish_code = metric.finish_code;
        element.is_modifier = metric.is_modifier;
        element.message = metric.message;
        element.type = "brpc_receiver@reg_addr_" + registry_address;
        if (context->getSettingsRef().log_query_exchange)
        {
            if (auto exchange_log = context->getQueryExchangeLog())
                exchange_log->add(element);
        }
    }
    catch (...)
    {
        tryLogCurrentException(log);
    }
}

void BrpcRemoteBroadcastReceiver::registerToSenders(UInt32 timeout_ms)
{
    Stopwatch s;
    std::shared_ptr<RpcClient> rpc_client = RpcChannelPool::getInstance().getClient(registry_address, BrpcChannelPoolOptions::STREAM_DEFAULT_CONFIG_KEY, true);
    Protos::RegistryService_Stub stub(Protos::RegistryService_Stub(&rpc_client->getChannel()));
    brpc::Controller cntl;
    brpc::StreamOptions stream_options;
    stream_options.handler = std::make_shared<StreamHandler>(context, shared_from_this(), header, keep_order);
    if (timeout_ms == 0)
        cntl.set_timeout_ms(rpc_client->getChannel().options().timeout_ms);
    else
        cntl.set_timeout_ms(timeout_ms);
    cntl.set_max_retry(3);
    if (brpc::StreamCreate(&stream_id, cntl, &stream_options) != 0)
        throw Exception("Fail to create stream for " + getName(), ErrorCodes::BRPC_EXCEPTION);

    if (stream_id == brpc::INVALID_STREAM_ID)
        throw Exception("Stream id is invalid for " + getName(), ErrorCodes::BRPC_EXCEPTION);

    Protos::RegistryRequest request;
    Protos::RegistryResponse response;
    auto exchange_key = std::dynamic_pointer_cast<ExchangeDataKey>(trans_key);
    request.set_query_id(exchange_key->getQueryId());
    request.set_write_segment_id(exchange_key->getWriteSegmentId());
    request.set_read_segment_id(exchange_key->getReadSegmentId());
    request.set_parallel_id(exchange_key->getParallelIndex());
    request.set_coordinator_address(exchange_key->getCoordinatorAddress());
    request.set_wait_timeout_ms(context->getSettingsRef().exchange_timeout_ms / 2);

    stub.registry(&cntl, &request, &response, nullptr);
    // if exchange_enable_force_remote_mode = 1, sender and receiver in same process and sender stream may close before rpc end
    if (cntl.ErrorCode() == brpc::EREQUEST && cntl.ErrorText().ends_with("was closed before responded"))
    {
        LOG_INFO(
            log,
            "Receiver register sender successfully but sender already finished, host-{} , data_key-{}, stream_id-{}",
            registry_address,
            data_key,
            stream_id);
        return;
    }
    rpc_client->assertController(cntl);
    metric.register_time_ms += s.elapsedMilliseconds();
    LOG_DEBUG(log, "Receiver register sender successfully, host-{} , data_key-{}, stream_id-{}", registry_address, data_key, stream_id);
}

void BrpcRemoteBroadcastReceiver::pushReceiveQueue(Chunk chunk)
{
    if (queue.closed())
        return;
    if (!queue.tryEmplace(context->getSettingsRef().exchange_timeout_ms, std::move(chunk)))
        throw Exception(
            "Push exchange data to receiver for " + getName() + " timeout for "
                + std::to_string(context->getSettingsRef().exchange_timeout_ms) + " ms.",
            ErrorCodes::DISTRIBUTE_STAGE_QUERY_EXCEPTION);
}

RecvDataPacket BrpcRemoteBroadcastReceiver::recv(UInt32 timeout_ms) noexcept
{
    Stopwatch s;
    Chunk received_chunk;
    if (!queue.tryPop(received_chunk, timeout_ms))
    {
        const auto error_msg = "Try pop receive queue for " + getName() + " timeout for " + std::to_string(timeout_ms) + " ms.";
        BroadcastStatus current_status = finish(BroadcastStatusCode::RECV_TIMEOUT, error_msg);
        return std::move(current_status);
    }

    // receive a empty chunk without any chunk info means the receive is done.
    if (!received_chunk && !received_chunk.getChunkInfo())
    {
        LOG_DEBUG(log, "{} finished ", getName());
        return RecvDataPacket(BroadcastStatus(BroadcastStatusCode::ALL_SENDERS_DONE, false, "receiver done"));
    }

    if (keep_order)
    {
        // Chunk in queue is created in StreamHanlder's on_received_messages callback, which is run in bthread.
        // Allocator (ref srcs/Common/Allocator.cpp) will add the momory of chunk to global memory tacker.
        // When this chunk is poped, we should add this memory to current query momory tacker, and subtract from global memory tacker.
        ExchangeUtils::transferGlobalMemoryToThread(received_chunk.allocatedBytes());
        metric.recv_bytes += received_chunk.bytes();
    }
    else
    {
        const ChunkInfoPtr & info = received_chunk.getChunkInfo();
        if (info)
        {
            if(auto iobuf_info = std::dynamic_pointer_cast<const DeserializeBufTransform::IOBufChunkInfo>(info))
            {
                metric.recv_bytes += iobuf_info->io_buf.length();
            }
        }
    }
    metric.recv_time_ms += s.elapsedMilliseconds();
    return RecvDataPacket(std::move(received_chunk));
}

BroadcastStatus BrpcRemoteBroadcastReceiver::finish(BroadcastStatusCode status_code_, String message)
{
    BroadcastStatusCode current_fin_code = finish_status_code.load(std::memory_order_relaxed);
    const auto *const msg = "BrpcRemoteBroadcastReceiver: already has been finished";
    if (current_fin_code != BroadcastStatusCode::RUNNING)
    {
        LOG_TRACE(
            log,
            "Broadcast {} finished and status can't be changed to {} any more. Current status: {}",
            data_key,
            status_code_,
            current_fin_code);
        metric.finish_code = current_fin_code;
        metric.is_modifier = 0;
        return BroadcastStatus(current_fin_code, false, msg);
    }

    int actual_status_code = BroadcastStatusCode::RUNNING;

    BroadcastStatusCode new_fin_code = status_code_;
    if (status_code_ < 0)
    {
        // if send_done_flag has never been set, sender should have some unkown errors.
        if (!send_done_flag.test(std::memory_order_acquire))
            new_fin_code = BroadcastStatusCode::SEND_UNKNOWN_ERROR;
    }

    if (finish_status_code.compare_exchange_strong(current_fin_code, new_fin_code, std::memory_order_relaxed, std::memory_order_relaxed))
    {
        if (new_fin_code > 0)
            queue.close();
        brpc::StreamFinish(stream_id, actual_status_code, new_fin_code, new_fin_code > 0);
        metric.finish_code = new_fin_code;
        metric.is_modifier = 1;
        metric.message = message;
        return BroadcastStatus(static_cast<BroadcastStatusCode>(new_fin_code), true, message);
    }
    else
    {
        LOG_TRACE(log, "Fail to change broadcast status to {}, current status is: {} ", new_fin_code, current_fin_code);
        metric.finish_code = current_fin_code;
        metric.is_modifier = 0;
        return BroadcastStatus(current_fin_code, false, msg);
    }
}

String BrpcRemoteBroadcastReceiver::getName() const
{
    return "BrpcReciver[" + trans_key->getKey() + "]@" + registry_address;
}

AsyncRegisterResult BrpcRemoteBroadcastReceiver::registerToSendersAsync(UInt32 timeout_ms)
{
    Stopwatch s;
    AsyncRegisterResult res;

    res.channel = RpcChannelPool::getInstance().getClient(registry_address, BrpcChannelPoolOptions::STREAM_DEFAULT_CONFIG_KEY, true);
    res.cntl = std::make_unique<brpc::Controller>();
    res.request = std::make_unique<Protos::RegistryRequest>();
    res.response = std::make_unique<Protos::RegistryResponse>();

    std::shared_ptr<RpcClient> & rpc_client = res.channel;
    Protos::RegistryService_Stub stub(Protos::RegistryService_Stub(&rpc_client->getChannel()));

    brpc::Controller & cntl = *res.cntl;
    brpc::StreamOptions stream_options;
    stream_options.handler = std::make_shared<StreamHandler>(context, shared_from_this(), header, keep_order);

    if (timeout_ms == 0)
        cntl.set_timeout_ms(rpc_client->getChannel().options().timeout_ms);
    else
        cntl.set_timeout_ms(timeout_ms);
    cntl.set_max_retry(3);
    if (brpc::StreamCreate(&stream_id, cntl, &stream_options) != 0)
        throw Exception("Fail to create stream for " + getName(), ErrorCodes::BRPC_EXCEPTION);

    if (stream_id == brpc::INVALID_STREAM_ID)
        throw Exception("Stream id is invalid for " + getName(), ErrorCodes::BRPC_EXCEPTION);

    auto exchange_key = std::dynamic_pointer_cast<ExchangeDataKey>(trans_key);

    res.request->set_query_id(exchange_key->getQueryId());
    res.request->set_write_segment_id(exchange_key->getWriteSegmentId());
    res.request->set_read_segment_id(exchange_key->getReadSegmentId());
    res.request->set_parallel_id(exchange_key->getParallelIndex());
    res.request->set_coordinator_address(exchange_key->getCoordinatorAddress());
    res.request->set_wait_timeout_ms(context->getSettingsRef().exchange_timeout_ms / 2);
    stub.registry(&cntl, res.request.get(), res.response.get(), brpc::DoNothing());
    metric.register_time_ms += s.elapsedMilliseconds();
    return res;
}

}
