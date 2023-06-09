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

#include <atomic>

#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>
#include <Processors/Pipe.h>
#include <Processors/RowsBeforeLimitCounter.h>
#include <Processors/Sources/SourceWithProgress.h>
#include <Processors/Exchange/ExchangeOptions.h>


namespace DB
{
/// Read chunk from ExchangeSink.
class ExchangeSource : public SourceWithProgress
{
public:
    ExchangeSource(Block header_, BroadcastReceiverPtr receiver_, ExchangeOptions options_);
    ExchangeSource(Block header_, BroadcastReceiverPtr receiver_, ExchangeOptions options_, bool fetch_exception_from_scheduler_);
    ~ExchangeSource() override;

    IProcessor::Status prepare() override;
    String getName() const override;
    BroadcastReceiverPtr & getReceiver() { return receiver; }

protected:
    std::optional<Chunk> tryGenerate() override;
    void onCancel() override;

private:
    BroadcastReceiverPtr receiver;
    ExchangeOptions options;
    bool fetch_exception_from_scheduler;
    std::atomic<bool> was_query_canceled {false};
    std::atomic<bool> was_receiver_finished {false};
    Poco::Logger * logger;
};

}
