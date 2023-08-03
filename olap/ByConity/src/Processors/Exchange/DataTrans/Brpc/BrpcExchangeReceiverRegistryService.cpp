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

#include "BrpcExchangeReceiverRegistryService.h"

#include <Processors/Exchange/DataTrans/BroadcastSenderProxy.h>
#include <Processors/Exchange/DataTrans/BroadcastSenderProxyRegistry.h>
#include <Processors/Exchange/DataTrans/Brpc/BrpcRemoteBroadcastSender.h>
#include <Processors/Exchange/DataTrans/DataTransKey.h>
#include <Processors/Exchange/ExchangeUtils.h>
#include <brpc/stream.h>
#include <Common/Exception.h>
#include <common/scope_guard.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int BRPC_EXCEPTION;
}

void BrpcExchangeReceiverRegistryService::registry(
    ::google::protobuf::RpcController * controller,
    const ::DB::Protos::RegistryRequest * request,
    ::DB::Protos::RegistryResponse * /*response*/,
    ::google::protobuf::Closure * done)
{
    brpc::StreamId sender_stream_id = brpc::INVALID_STREAM_ID;
    BroadcastSenderProxyPtr sender_proxy;
    brpc::Controller * cntl = static_cast<brpc::Controller *>(controller);
    auto accpet_timeout_ms = request->wait_timeout_ms();
    /// SCOPE_EXIT wrap logic which run after done->Run(),
    /// since host socket of the accpeted stream is set in done->Run()
    SCOPE_EXIT({
        if (sender_proxy && sender_stream_id != brpc::INVALID_STREAM_ID)
        {
            try
            {
                sender_proxy->waitAccept(accpet_timeout_ms);
            }
            catch (...)
            {
                brpc::StreamClose(sender_stream_id);
                String error_msg
                    = "Create stream for " + sender_proxy->getDataKey()->getKey() + " failed by exception: " + getCurrentExceptionMessage(false);
                LOG_ERROR(log, error_msg);
                return;
            }
            try
            {
                auto real_sender = std::dynamic_pointer_cast<IBroadcastSender>(std::make_shared<BrpcRemoteBroadcastSender>(
                    sender_proxy->getDataKey(), sender_stream_id, sender_proxy->getContext(), sender_proxy->getHeader()));
                sender_proxy->becomeRealSender(std::move(real_sender));
            }
            catch (...)
            {
                brpc::StreamClose(sender_stream_id);
                LOG_ERROR(log, "Create stream failed for {} by exception: {}", sender_proxy->getDataKey()->getKey(), getCurrentExceptionMessage(false));
            }
        }
    });

    /// this done_guard guarantee to call done->Run() in any situation
    brpc::ClosureGuard done_guard(done);
    brpc::StreamOptions stream_options;
    stream_options.max_buf_size = max_buf_size;

    auto data_key = std::make_shared<ExchangeDataKey>(
        request->query_id(),
        request->write_segment_id(),
        request->read_segment_id(),
        request->parallel_id(),
        request->coordinator_address());

    if (brpc::StreamAccept(&sender_stream_id, *cntl, &stream_options) != 0)
    {
        sender_stream_id = brpc::INVALID_STREAM_ID;
        String error_msg = "Fail to accept stream for data_key-" + data_key->getKey();
        LOG_ERROR(log, error_msg);
        cntl->SetFailed(error_msg);
        return;
    }
    sender_proxy = BroadcastSenderProxyRegistry::instance().getOrCreate(data_key);

}

}
