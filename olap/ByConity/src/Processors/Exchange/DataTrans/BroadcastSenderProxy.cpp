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

#include <algorithm>
#include <atomic>
#include <memory>
#include <mutex>
#include <optional>
#include <string>
#include <Core/Block.h>
#include <DataTypes/IDataType.h>
#include <Interpreters/Context.h>
#include <Processors/Chunk.h>
#include <Processors/Exchange/DataTrans/BroadcastSenderProxy.h>
#include <Processors/Exchange/DataTrans/BroadcastSenderProxyRegistry.h>
#include <Processors/Exchange/DataTrans/DataTransKey.h>
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>
#include <Processors/Exchange/DataTrans/Local/LocalChannelOptions.h>
#include <Poco/Logger.h>
#include <Common/Exception.h>
#include <common/logger_useful.h>
#include <common/types.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int TIMEOUT_EXCEEDED;
    extern const int EXCHANGE_DATA_TRANS_EXCEPTION;
}

BroadcastSenderProxy::BroadcastSenderProxy(DataTransKeyPtr data_key_)
    : data_key(std::move(data_key_)), wait_timeout_ms(5000), logger(&Poco::Logger::get("BroadcastSenderProxy"))
{
}

BroadcastSenderProxy::~BroadcastSenderProxy()
{
    try
    {
        BroadcastSenderProxyRegistry::instance().remove(data_key);
    }
    catch (...)
    {
        tryLogCurrentException(logger);
    }
}


BroadcastStatus BroadcastSenderProxy::send(Chunk chunk)
{
    if (!has_real_sender.load(std::memory_order_acquire))
        waitBecomeRealSender(wait_timeout_ms);
    return real_sender->send(std::move(chunk));
}


BroadcastStatus BroadcastSenderProxy::finish(BroadcastStatusCode status_code, String message)
{
    if (!has_real_sender.load(std::memory_order_acquire))
    {
        // No need to waitBecomeRealSender since receiver can infer finish status as SEND_UNKNOWN_ERROR
        // if no finish code is received.
        if (status_code > BroadcastStatusCode::RUNNING)
        {
            std::lock_guard lock(mutex);
            // Wakeup all pending call for waitBecomeRealSender and waitAccept
            closed = true;
            wait_accept.notify_all();
            wait_become_real.notify_all();
            return BroadcastStatus(BroadcastStatusCode::SEND_NOT_READY, false, "Sender not ready");
        }

        waitBecomeRealSender(wait_timeout_ms);
    }
    return real_sender->finish(status_code, message);
}

void BroadcastSenderProxy::merge(IBroadcastSender && sender)
{
    if (!has_real_sender.load(std::memory_order_relaxed))
        waitBecomeRealSender(wait_timeout_ms);

    BroadcastSenderProxy * other = dynamic_cast<BroadcastSenderProxy *>(&sender);
    if (!other)
        real_sender->merge(std::move(sender));
    else
    {
        if (!other->has_real_sender)
            throw Exception("Can't merge proxy has no real sender " + data_key->dump(), ErrorCodes::LOGICAL_ERROR);

        real_sender->merge(std::move(*other->real_sender));
        other->has_real_sender.store(false, std::memory_order_release);

        std::unique_lock lock(other->mutex);
        other->context = ContextPtr();
        other->header = Block();
    }
}

String BroadcastSenderProxy::getName() const
{
    String prefix = "[Proxy]";
    return real_sender ? prefix + real_sender->getName() : prefix + data_key->dump();
}

void BroadcastSenderProxy::waitAccept(UInt32 timeout_ms)
{
    std::unique_lock lock(mutex);
    if (context)
        return;

    if (!wait_accept.wait_for(lock, std::chrono::milliseconds(timeout_ms), [this] {
            return this->header.operator bool() || closed;
        }))
        throw Exception("Wait accept timeout for " + data_key->dump(), ErrorCodes::TIMEOUT_EXCEEDED);
    else if (closed)
        throw Exception("Interrput accept for " + data_key->dump(), ErrorCodes::EXCHANGE_DATA_TRANS_EXCEPTION);
}

void BroadcastSenderProxy::accept(ContextPtr context_, Block header_)
{
    std::unique_lock lock(mutex);
    if (header || context)
        throw Exception("Can't call accept twice for {} " + data_key->dump(), ErrorCodes::LOGICAL_ERROR);
    context = std::move(context_);
    header = std::move(header_);
    wait_timeout_ms = context->getSettingsRef().exchange_timeout_ms / 2;
    wait_accept.notify_all();
}

void BroadcastSenderProxy::becomeRealSender(BroadcastSenderPtr sender)
{
    std::lock_guard lock(mutex);
    if (real_sender)
    {
        if (real_sender != sender)
            throw Exception("Can't set set real sender twice for " + data_key->dump(), ErrorCodes::LOGICAL_ERROR);
        return;
    }

    LOG_DEBUG(logger, "Proxy become real sender: {}", sender->getName());
    real_sender = std::move(sender);
    has_real_sender.store(true, std::memory_order_release);
    wait_become_real.notify_all();
}

void BroadcastSenderProxy::waitBecomeRealSender(UInt32 timeout_ms)
{
    std::unique_lock lock(mutex);
    if (real_sender)
        return;
    if (!wait_become_real.wait_for(
            lock, std::chrono::milliseconds(timeout_ms), [this] { return this->real_sender.operator bool() || closed; }))
        throw Exception("Wait become real sender timeout for " + data_key->dump(), ErrorCodes::TIMEOUT_EXCEEDED);
    else if (closed)
        throw Exception("Interrput accept for " + data_key->dump(), ErrorCodes::EXCHANGE_DATA_TRANS_EXCEPTION);
}

BroadcastSenderType BroadcastSenderProxy::getType()
{
    if (!has_real_sender.load(std::memory_order_relaxed))
        waitBecomeRealSender(wait_timeout_ms);
    return real_sender->getType();
}

ContextPtr BroadcastSenderProxy::getContext() const
{
    std::lock_guard lock(mutex);
    return context;
}

Block BroadcastSenderProxy::getHeader() const
{
    std::lock_guard lock(mutex);
    return header;
}

DataTransKeyPtr BroadcastSenderProxy::getDataKey() const
{
    return data_key;
}

}
