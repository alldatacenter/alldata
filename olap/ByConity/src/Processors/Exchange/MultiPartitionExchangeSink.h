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
#include <Core/ColumnNumbers.h>
#include <Functions/IFunction.h>
#include <Processors/Chunk.h>
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>
#include <Processors/Exchange/ExchangeBufferedSender.h>
#include <Processors/Exchange/ExchangeOptions.h>
#include <Processors/Exchange/IExchangeSink.h>
#include <Processors/IProcessor.h>

namespace DB
{
/// Send data to all partititons. Usually used with ResizeProcessor
///                   ||-> MultiPartitionExchangeSink
/// ResizeProcessor-->||-> MultiPartitionExchangeSink
///                   ||-> MultiPartitionExchangeSink
/// This pipeline will not keep data order and maximize the performance.
class MultiPartitionExchangeSink : public IExchangeSink
{
public:
    explicit MultiPartitionExchangeSink(
        Block header_,
        BroadcastSenderPtrs partition_senders_,
        ExecutableFunctionPtr repartition_func_,
        ColumnNumbers repartition_keys,
        ExchangeOptions options_);
    virtual String getName() const override { return "MultiPartitionExchangeSink"; }
    virtual void onCancel() override;
    virtual ~MultiPartitionExchangeSink() override = default;


protected:
    virtual void consume(Chunk) override;
    virtual void onFinish() override;

private:
    const Block & header;
    BroadcastSenderPtrs partition_senders;
    size_t partition_num;
    size_t column_num;
    ExecutableFunctionPtr repartition_func;
    const ColumnNumbers repartition_keys;
    ExchangeOptions options;
    ExchangeBufferedSenders buffered_senders;
    Poco::Logger * logger;
};

}
