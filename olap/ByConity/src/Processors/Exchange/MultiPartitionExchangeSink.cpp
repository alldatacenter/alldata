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

#include <tuple>
#include <Processors/Exchange/DataTrans/IBroadcastSender.h>
#include <Processors/Exchange/MultiPartitionExchangeSink.h>
#include <Processors/Exchange/IExchangeSink.h>
#include <Common/Exception.h>
#include <common/logger_useful.h>
#include <Columns/IColumn.h>
#include <Processors/Exchange/ExchangeBufferedSender.h>
#include <Processors/Exchange/ExchangeOptions.h>
#include <Processors/Exchange/RepartitionTransform.h>

namespace DB
{
MultiPartitionExchangeSink::MultiPartitionExchangeSink(
    Block header_,
    BroadcastSenderPtrs partition_senders_,
    ExecutableFunctionPtr repartition_func_,
    ColumnNumbers repartition_keys_,
    ExchangeOptions options_)
    : IExchangeSink(std::move(header_))
    , header(getPort().getHeader())
    , partition_senders(std::move(partition_senders_))
    , partition_num(partition_senders.size())
    , column_num(header.columns())
    , repartition_func(std::move(repartition_func_))
    , repartition_keys(std::move(repartition_keys_))
    , options(options_)
    , logger(&Poco::Logger::get("MultiPartitionExchangeSink"))

{
    for(size_t i = 0; i < partition_num; ++i)
    {
        ExchangeBufferedSender buffered_sender (header, partition_senders[i], options.send_threshold_in_bytes, options.send_threshold_in_row_num);
        buffered_senders.emplace_back(std::move(buffered_sender));
    }
}

void MultiPartitionExchangeSink::consume(Chunk chunk)
{
    if (partition_num == 1)
    {
        auto status = buffered_senders[0].sendThrough(std::move(chunk));
        if (status.code != BroadcastStatusCode::RUNNING)
            finish();
        return;
    }
    const auto & chunk_info = chunk.getChunkInfo();
    if (!buffered_senders[0].compareBufferChunkInfo(chunk_info))
    {
        for (size_t i = 0; i < partition_num; ++i)
        {
            buffered_senders[i].updateBufferChunkInfo(chunk_info);
        }
    }


    IColumn::Selector partition_selector;
    RepartitionTransform::PartitionStartPoints partition_start_points;
    std::tie(partition_selector, partition_start_points) = RepartitionTransform::doRepartition(
        partition_num, chunk, header, repartition_keys, repartition_func, RepartitionTransform::REPARTITION_FUNC_RESULT_TYPE);

    const auto &  columns = chunk.getColumns();
    for (size_t i = 0; i < column_num; i++)
    {
        auto materialized_column = columns[i]->convertToFullColumnIfConst();
        for (size_t j = 0; j < partition_num; ++j)
        {
            size_t from = partition_start_points[j];
            size_t length = partition_start_points[j + 1] - from;
            if (length == 0)
                continue; // no data for this partition continue;
            buffered_senders[j].appendSelective(i, *materialized_column, partition_selector, from, length);
        }
    }

    bool has_active_sender = false;
    for (size_t i = 0; i < partition_num; ++i)
    {
        auto status = buffered_senders[i].flush(false);
        if (status.code == BroadcastStatusCode::RUNNING)
            has_active_sender = true;
    }
    if (!has_active_sender)
        finish();
}

void MultiPartitionExchangeSink::onFinish()
{
    LOG_TRACE(logger, "MultiPartitionExchangeSink finish");
    for(size_t i = 0; i < partition_num ; ++i)
        buffered_senders[i].flush(true);
}

void MultiPartitionExchangeSink::onCancel()
{
    LOG_TRACE(logger, "MultiPartitionExchangeSink cancel");
    for (BroadcastSenderPtr & sender : partition_senders)
        sender->finish(BroadcastStatusCode::SEND_CANCELLED, "Cancelled by pipeline");
}

}
