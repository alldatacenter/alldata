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

#include <Catalog/StreamingHanlders.h>
#include <Common/Exception.h>
#include <common/logger_useful.h>
#include <butil/logging.h>

namespace DB::Catalog
{

void StreamingHandlerBase::on_idle_timeout(brpc::StreamId id)
{
    LOG_ERROR(log, "Time out while receiving data from stream " + std::to_string(id));
}

void StreamingHandlerBase::on_closed(brpc::StreamId)
{
    manager.removeHandler(shared_from_this());
}

int ServerPartsHandler::on_received_messages(brpc::StreamId id, butil::IOBuf *const *, size_t )
{
    /// send parts back;
    try
    {
        func(id, name_space, table_uuid, partitions, txnTimestamp);
        CHECK_EQ(0, brpc::StreamClose(id));
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        CHECK_EQ(0, brpc::StreamClose(id));
    }
    return 0;
}

int ClientPartsHandler::on_received_messages(brpc::StreamId id, butil::IOBuf *const messages[], size_t size)
{
    try
    {
        for (size_t i = 0; i < size; ++i)
            parts_loader(messages[i]->to_string());
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        CHECK_EQ(0, brpc::StreamClose(id));
        last_exception = getCurrentExceptionMessage(false);
    }
    return 0;
}

void ClientPartsHandler::on_closed(brpc::StreamId id)
{
    finishedGetParts();
    StreamingHandlerBase::on_closed(id);
}

void ClientPartsHandler::waitingForGetParts()
{
    std::unique_lock lock(sync_mutex);
    sync_cv.wait(lock);
}

void ClientPartsHandler::finishedGetParts()
{
    std::unique_lock lock(sync_mutex);
    sync_cv.notify_one();
}

}
