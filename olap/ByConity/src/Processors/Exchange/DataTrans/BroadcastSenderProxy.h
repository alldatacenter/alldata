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
#include <memory>
#include <Core/Block.h>
#include <DataTypes/IDataType.h>
#include <Interpreters/Context_fwd.h>
#include <Processors/Exchange/DataTrans/DataTransKey.h>
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>
#include <Processors/Exchange/DataTrans/IBroadcastSender.h>
#include <Processors/Exchange/ExchangeDataKey.h>
#include <boost/core/noncopyable.hpp>
#include <bthread/mtx_cv_base.h>

namespace DB
{
class BroadcastSenderProxy final : public IBroadcastSender, boost::noncopyable
{
public:
    virtual ~BroadcastSenderProxy() override;
    BroadcastStatus send(Chunk chunk) override;
    BroadcastStatus finish(BroadcastStatusCode status_code, String message) override;
    void merge(IBroadcastSender && /*sender*/) override;
    String getName() const override;
    BroadcastSenderType getType() override;
    void accept(ContextPtr context_, Block header_);
    void waitAccept(UInt32 /*timeout_ms*/);

    void becomeRealSender(BroadcastSenderPtr sender);
    void waitBecomeRealSender(UInt32 /*timeout_ms*/);

    ContextPtr getContext() const;
    Block getHeader() const;
    DataTransKeyPtr getDataKey() const;

private:
    friend class BroadcastSenderProxyRegistry;
    explicit BroadcastSenderProxy(DataTransKeyPtr data_key_);

    mutable bthread::Mutex mutex;
    bthread::ConditionVariable wait_become_real;
    bthread::ConditionVariable wait_accept;
    std::atomic_bool has_real_sender {false};
    bool closed {false};
    DataTransKeyPtr data_key;

    ContextPtr context;
    Block header;
    BroadcastSenderPtr real_sender;

    UInt32 wait_timeout_ms;

    Poco::Logger * logger;
};

}
