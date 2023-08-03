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
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>
#include <Processors/Exchange/ExchangeBufferedSender.h>
#include <Processors/Exchange/ExchangeOptions.h>
#include <Processors/Exchange/IExchangeSink.h>
#include <Processors/IProcessor.h>

namespace DB
{
/// Send data to single partititon. Usually used with RepartitionTransform and BufferedCopyTransform:
///                                                 ||-> SinglePartitionExchangeSink[partition 0]
/// RepartitionTransform--> BufferedCopyTransform-->||-> SinglePartitionExchangeSink[partition 1]
///                                                 ||-> SinglePartitionExchangeSink[partition 2]
/// This pipeline can keep data order and maximize the parallelism.
class SinglePartitionExchangeSink : public IExchangeSink
{
public:
    explicit SinglePartitionExchangeSink(Block header_,
    BroadcastSenderPtr sender_,
    size_t partition_id_,
    ExchangeOptions options_);
    String getName() const override { return "SinglePartitionExchangeSink"; }
    void onCancel() override;
    virtual ~SinglePartitionExchangeSink() override = default;

protected:
    void consume(Chunk) override;
    void onFinish() override;

private:
    const Block & header;
    BroadcastSenderPtr sender;
    size_t partition_id;
    size_t column_num;
    ExchangeOptions options;
    ExchangeBufferedSender buffered_sender;
    Poco::Logger * logger;
};

}
