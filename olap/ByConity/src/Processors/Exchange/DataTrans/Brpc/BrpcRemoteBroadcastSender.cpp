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

#include "BrpcRemoteBroadcastSender.h"
#include "WriteBufferFromBrpcBuf.h"

#include <atomic>
#include <cerrno>
#include <memory>
#include <mutex>
#include <string>
#include <Compression/CompressedWriteBuffer.h>
#include <Compression/CompressionFactory.h>
#include <DataTypes/DataTypeFactory.h>
#include <Interpreters/QueryExchangeLog.h>
#include <Processors/Exchange/DataTrans/DataTransException.h>
#include <Processors/Exchange/DataTrans/IBroadcastSender.h>
#include <Processors/Exchange/DataTrans/NativeChunkOutputStream.h>
#include <Processors/Exchange/ExchangeDataKey.h>
#include <brpc/protocol.h>
#include <brpc/stream.h>
#include <bthread/bthread.h>
#include <Common/ClickHouseRevision.h>
#include <Common/Exception.h>
#include <Common/Stopwatch.h>
#include <common/logger_useful.h>

namespace DB
{
namespace
{
    const auto STREAM_WAIT_TIMEOUT_MS = 1000;
}

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int BRPC_EXCEPTION;
}

/**
 * 1-1 sender, to make 1-n sener, merge n 1-1 sender
 */
BrpcRemoteBroadcastSender::BrpcRemoteBroadcastSender(
    DataTransKeyPtr trans_key_, brpc::StreamId stream_id, ContextPtr context_, Block header_)
    : context(std::move(context_)), header(std::move(header_))
{
    trans_keys.emplace_back(std::move(trans_key_));
    sender_stream_ids.push_back(stream_id);
}

BrpcRemoteBroadcastSender::~BrpcRemoteBroadcastSender()
{
    try
    {
        for (brpc::StreamId sender_stream_id : sender_stream_ids)
        {
            if(sender_stream_id != brpc::INVALID_STREAM_ID)
                brpc::StreamClose(sender_stream_id);
        }
        if (trans_keys.empty())
            return;
        QueryExchangeLogElement element;
        if (auto key = std::dynamic_pointer_cast<const ExchangeDataKey>(trans_keys.front()))
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
        element.send_time_ms = metric.send_time_ms;
        element.send_rows = metric.send_rows;
        element.send_bytes = metric.send_bytes;
        element.send_uncompressed_bytes = metric.send_uncompressed_bytes;
        element.ser_time_ms = metric.ser_time_ms;
        element.send_retry = metric.send_retry;
        element.send_retry_ms = metric.send_retry_ms;
        element.overcrowded_retry = metric.overcrowded_retry;
        element.finish_code = metric.finish_code;
        element.is_modifier = metric.is_modifier;
        element.message = metric.message;
        element.type = "brpc_sender";
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

BroadcastStatus BrpcRemoteBroadcastSender::send(Chunk chunk) noexcept
{
    try
    {
        Stopwatch s;
        metric.num_send_times++;
        metric.send_rows += chunk.getNumRows();
        metric.send_uncompressed_bytes += chunk.bytes();
        WriteBufferFromBrpcBuf out;
        serializeChunkToIoBuffer(std::move(chunk), out);
        const auto & buf = out.getFinishedBuf();
        metric.send_bytes += buf.length();
        metric.ser_time_ms += s.elapsedMilliseconds();
        auto res = BroadcastStatus(BroadcastStatusCode::RUNNING);
        for (size_t i = 0; i < sender_stream_ids.size(); ++i)
        {
            BroadcastStatus ret_status = sendIOBuffer(buf, sender_stream_ids[i], trans_keys[i]->getKey());
            if (ret_status.is_modifer && ret_status.code != BroadcastStatusCode::RUNNING)
            {
                finish(
                    BroadcastStatusCode::SEND_CANCELLED,
                    "Cancelled by other, code: " + std::to_string(ret_status.code) + " msg: " + ret_status.message);
                return ret_status;
            }

            if (ret_status.code != BroadcastStatusCode::RUNNING)
                res = ret_status;
        }
        metric.send_time_ms += s.elapsedMilliseconds();
        return res;
    }
    catch (...)
    {
        String exception_str = getCurrentExceptionMessage(true);
        BroadcastStatus current_status = finish(BroadcastStatusCode::SEND_UNKNOWN_ERROR, exception_str);
        return current_status;
    }
}

void BrpcRemoteBroadcastSender::serializeChunkToIoBuffer(Chunk chunk, WriteBufferFromBrpcBuf & out) const
{
    const auto settings = context->getSettingsRef();
    if (settings.exchange_enable_block_compress)
    {
        std::string method = Poco::toUpper(settings.network_compression_method.toString());
        std::optional<int> level;
        if (method == "ZSTD")
            level = settings.network_zstd_compression_level;
        const CompressionCodecPtr & codec = CompressionCodecFactory::instance().get(method, level);
        CompressedWriteBuffer compressed_out(out, codec, DBMS_DEFAULT_BUFFER_SIZE * 2);
        NativeChunkOutputStream chunk_out(
            compressed_out, ClickHouseRevision::getVersionRevision(), header, !settings.low_cardinality_allow_in_native_format);
        chunk_out.write(chunk);
        compressed_out.next();
    }
    else
    {
        NativeChunkOutputStream chunk_out(
            out, ClickHouseRevision::getVersionRevision(), header, !settings.low_cardinality_allow_in_native_format);
        chunk_out.write(chunk);
    }
}

BroadcastStatus BrpcRemoteBroadcastSender::sendIOBuffer(const butil::IOBuf & io_buffer, brpc::StreamId stream_id, const String & data_key)
{
    if (io_buffer.size() > brpc::FLAGS_max_body_size)
        throw Exception(
            "Write stream-" + std::to_string(stream_id) + " buffer fail, io buffer size is bigger than "
                + std::to_string(brpc::FLAGS_max_body_size) + " current is " + std::to_string(io_buffer.size()),
            ErrorCodes::BRPC_EXCEPTION);

    size_t retry_count = 0;
    Stopwatch s;
    bool success = false;
    while (s.elapsedMilliseconds() < context->getSettingsRef().exchange_timeout_ms)
    {
        int rect_code = brpc::StreamWrite(stream_id, io_buffer);
        if (rect_code == 0)
        {
            success = true;
            break;
        }
        else if (rect_code == EAGAIN)
        {
            bthread_usleep(50 * 1000);
            timespec timeout = butil::milliseconds_from_now(STREAM_WAIT_TIMEOUT_MS);
            int wait_res_code = brpc::StreamWait(stream_id, &timeout);
            if (wait_res_code == EINVAL)
            {
                // TODO: retain stream object before finish code is read.
                // Ingore error when writing to the closed stream, because this stream is closed by remote peer before read any finish code.
                LOG_INFO(log, "Stream-{} with key {} is closed", stream_id, data_key);
                return BroadcastStatus(BroadcastStatusCode::RECV_UNKNOWN_ERROR, false, "Stream is closed by peer");
            }

            LOG_TRACE(
                log,
                "Stream write buffer full wait for {} ms,  retry count-{}, stream_id-{} ,with data_key-{} wait res code:{} size:{} ",
                STREAM_WAIT_TIMEOUT_MS,
                retry_count,
                stream_id,
                data_key,
                wait_res_code,
                io_buffer.size());
        }
        else if (rect_code == EINVAL)
        {
            // Ingore error when writing to the closed stream, because this stream is closed by remote peer before read any finish code.
            LOG_INFO(log, "Stream-{} with key {} is closed", stream_id, data_key);
            return BroadcastStatus(BroadcastStatusCode::RECV_UNKNOWN_ERROR, false, "Stream is closed by peer");
        }
        else if (rect_code == 1011) //EOVERCROWDED   | 1011 | The server is overcrowded
        {
            metric.overcrowded_retry += 1;
            bthread_usleep(1000 * 1000);
            LOG_WARNING(
                log, "Stream-{} write buffer error rect_code:{}, server is overcrowded, data_key-{}", stream_id, rect_code, data_key);
        }
        // stream finished
        else if (rect_code == -1)
        {
            int stream_finished_code = 0;
            auto rc = brpc::StreamFinishedCode(stream_id, stream_finished_code);
            // Stream is closed by remote peer and we can get finish code now
            if (rc == EINVAL)
                return BroadcastStatus(BroadcastStatusCode::RECV_UNKNOWN_ERROR, false, "Stream is closed by peer");
            LOG_INFO(log, "Stream-{} write receive finish request, finish code:{}, data_key-{}", stream_id, rect_code, data_key);
            return BroadcastStatus(static_cast<BroadcastStatusCode>(rect_code), false, "Stream Write receive finish request");
        }
        else
        {
            throw Exception(
                "Stream-" + std::to_string(stream_id) + " write buffer occurred error, the rect_code that we can not handle:"
                    + std::to_string(rect_code) + ", data_key-" + data_key,
                ErrorCodes::BRPC_EXCEPTION);
        }
        retry_count++;
    }
    metric.send_retry_ms += s.elapsedMilliseconds();
    metric.send_retry += retry_count;
    if (!success)
    {
        const auto msg = "Write stream-" + std::to_string(stream_id) + " fail, with data_key-" + data_key
            + ", size:" + std::to_string(io_buffer.size());
        LOG_ERROR(log, msg);
        auto current_status = BroadcastStatus(BroadcastStatusCode::SEND_TIMEOUT, true, msg);
        int actual_status_code = BroadcastStatusCode::RUNNING;
        int ret_code = brpc::StreamFinish(stream_id, actual_status_code, BroadcastStatusCode::SEND_TIMEOUT, true);
        if (ret_code != 0)
            return BroadcastStatus(static_cast<BroadcastStatusCode>(actual_status_code), false, "Stream Write receive finish request");
        return current_status;
    }
#ifndef NDEBUG
    LOG_TRACE(
        log,
        "Send exchange data size-{} KB with data_key-{}, stream-{} retry times:{} cost:{} ms",
        io_buffer.size() / 1024.0,
        data_key,
        stream_id,
        retry_count,
        s.elapsedMilliseconds());
#endif
    return BroadcastStatus(RUNNING);
}

BroadcastStatus BrpcRemoteBroadcastSender::finish(BroadcastStatusCode status_code_, String message)
{
    int code = 0;
    bool is_modifer = false;
    for (auto stream_id : sender_stream_ids)
    {
        int actual_status_code = status_code_;
        int ret_code = brpc::StreamFinish(stream_id, actual_status_code, status_code_, true);
        if (ret_code == 0)
        {
            is_modifer = true;
            // Close stream if all data are sent to make peer stream finished faster
            if (actual_status_code == BroadcastStatusCode::ALL_SENDERS_DONE)
                brpc::StreamClose(stream_id);
        }
        else
            // already has been changed
            code = actual_status_code;
    }
    if (is_modifer)
    {
        metric.finish_code = status_code_;
        metric.is_modifier = 1;
        metric.message = message;
        return BroadcastStatus(status_code_, true, message);
    }
    else
    {
        const auto *const msg = "BrpcRemoteBroadcastSender::finish, already has been finished";
        metric.finish_code = status_code_;
        metric.is_modifier = 0;
        return BroadcastStatus(
            static_cast<BroadcastStatusCode>(code), false, msg);
    }
}

void BrpcRemoteBroadcastSender::merge(IBroadcastSender && sender)
{
    BrpcRemoteBroadcastSender * other = dynamic_cast<BrpcRemoteBroadcastSender *>(&sender);
    if (!other)
        throw Exception("Sender to merge is not BrpcRemoteBroadcastSender", ErrorCodes::LOGICAL_ERROR);
    trans_keys.insert(
        trans_keys.end(), std::make_move_iterator(other->trans_keys.begin()), std::make_move_iterator(other->trans_keys.end()));
    other->trans_keys.clear();
    sender_stream_ids.insert(sender_stream_ids.end(), other->sender_stream_ids.begin(), other->sender_stream_ids.end());
    other->sender_stream_ids.clear();
}


String BrpcRemoteBroadcastSender::getName() const
{
    String name = "BrpcSender with keys:";
    for (const auto & trans_key : trans_keys)
    {
        name += trans_key->getKey() + "\n";
    }
    return name;
}

}
