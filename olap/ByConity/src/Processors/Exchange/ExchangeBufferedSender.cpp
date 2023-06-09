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

#include <cstddef>
#include <common/types.h>

#include <Columns/IColumn.h>
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>
#include <Processors/Exchange/DataTrans/IBroadcastSender.h>
#include <Processors/Exchange/ExchangeBufferedSender.h>
#include <Processors/Exchange/ExchangeUtils.h>
#include <Common/Exception.h>
#include <common/logger_useful.h>
#include <Processors/Chunk.h>

namespace DB
{
ExchangeBufferedSender::ExchangeBufferedSender(
    const Block & header_, BroadcastSenderPtr sender_, UInt64 threshold_in_bytes_, UInt64 threshold_in_row_num_)
    : header(header_)
    , column_num(header_.getColumns().size())
    , sender(sender_)
    , threshold_in_bytes(threshold_in_bytes_)
    , threshold_in_row_num(threshold_in_row_num_)
    , logger(&Poco::Logger::get("ExchangeBufferedSender"))
{
    resetBuffer();
}

BroadcastStatus ExchangeBufferedSender::flush(bool force)
{
    size_t rows = partition_buffer[0]->size();

    if (rows == 0)
        return BroadcastStatus(BroadcastStatusCode::RUNNING);

    if (!force)
    {
        if (bufferBytes() < threshold_in_bytes && rows < threshold_in_row_num)
            return BroadcastStatus(BroadcastStatusCode::RUNNING);
    }

    LOG_TRACE(logger, "flush buffer, force: {}, row: {}", force, rows);

    Chunk chunk(std::move(partition_buffer), rows, std::move(current_chunk_info));
    current_chunk_info = ChunkInfoPtr();

    auto res = ExchangeUtils::sendAndCheckReturnStatus(*sender, std::move(chunk));
    resetBuffer();
    return res;
}

bool ExchangeBufferedSender::compareBufferChunkInfo(const ChunkInfoPtr & chunk_info) const
{
    return ((current_chunk_info && chunk_info && *current_chunk_info == *chunk_info) || (!current_chunk_info && !chunk_info));
}


void ExchangeBufferedSender::updateBufferChunkInfo(ChunkInfoPtr chunk_info)
{
    flush(true);
    current_chunk_info = std::move(chunk_info);
}

BroadcastStatus ExchangeBufferedSender::sendThrough(Chunk chunk)
{
    return ExchangeUtils::sendAndCheckReturnStatus(*sender, std::move(chunk));
}

void ExchangeBufferedSender::resetBuffer()
{
    partition_buffer = header.cloneEmptyColumns();
}

void ExchangeBufferedSender::appendSelective(
    size_t column_idx, const IColumn & source, const IColumn::Selector & selector, size_t from, size_t length)
{
    partition_buffer[column_idx]->insertRangeSelective(source, selector, from, length);
}

size_t ExchangeBufferedSender::bufferBytes() const
{
    size_t total = 0;
    for (size_t i = 0; i < column_num; ++i)
    {
        total += partition_buffer[i]->byteSize();
    }
    return total;
}

}
