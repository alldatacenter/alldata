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
#include <memory>
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>
#include <Processors/IProcessor.h>
#include <Processors/Exchange/IExchangeSink.h>
#include <Poco/Logger.h>
#include <common/types.h>

namespace DB
{
/// Sink which send data to ExchangeSource with LoadBalanceSelector.
class LoadBalancedExchangeSink : public IExchangeSink
{
public:
    class LoadBalanceSelector : private boost::noncopyable
    {
    public:
        explicit LoadBalanceSelector(size_t partition_num_) : partition_num(partition_num_) { }
        virtual size_t selectNext() = 0;
        virtual ~LoadBalanceSelector() = default;

    protected:
        size_t partition_num;
    };
    using LoadBalanceSelectorPtr = std::unique_ptr<LoadBalanceSelector>;

    explicit LoadBalancedExchangeSink(Block header_, BroadcastSenderPtrs senders_);
    virtual ~LoadBalancedExchangeSink() override;
    virtual String getName() const override { return "LoadBalancedExchangeSink"; }


protected:
    virtual void consume(Chunk) override;
    virtual void onFinish() override;
    virtual void onCancel() override;

private:
    Block header = getPort().getHeader();
    BroadcastSenderPtrs senders;
    LoadBalanceSelectorPtr partition_selector;
    Poco::Logger * logger;
};

}
