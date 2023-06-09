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

#include <memory>
#include <string>
#include <Interpreters/Context_fwd.h>
#include <Interpreters/DistributedStages/PlanSegmentManagerRpcService.h>
#include <Interpreters/DistributedStages/executePlanSegment.h>
#include <Interpreters/NamedSession.h>
#include <Processors/Exchange/DataTrans/Brpc/ReadBufferFromBrpcBuf.h>
#include <brpc/controller.h>
#include <butil/iobuf.h>
#include <Common/Exception.h>

namespace DB
{
void PlanSegmentManagerRpcService::executeQuery(
    ::google::protobuf::RpcController * controller,
    const ::DB::Protos::ExecutePlanSegmentRequest * request,
    ::DB::Protos::ExecutePlanSegmentResponse * /*response*/,
    ::google::protobuf::Closure * done)
{
    brpc::ClosureGuard done_guard(done);
    brpc::Controller * cntl = static_cast<brpc::Controller *>(controller);
    try
    {
        ContextMutablePtr query_context;
        /// Create session context for worker
        if (context->getServerType() == ServerType::cnch_worker)
        {
            UInt64 txn_id = request->txn_id();
            auto named_session = context->acquireNamedCnchSession(txn_id, {}, true);
            query_context = Context::createCopy(named_session->context);
            query_context->setSessionContext(named_session->context);
        }
        else
        {
            query_context = Context::createCopy(context);
        }

        /// TODO: Authentication supports inter-server cluster secret, see https://github.com/ClickHouse/ClickHouse/commit/0159c74f217ec764060c480819e3ccc9d5a99a63
        Poco::Net::SocketAddress initial_socket_address(request->coordinator_host(), request->coordinator_port());
        query_context->setUser(request->user(), request->password(), initial_socket_address);

        /// Set client info.
        ClientInfo & client_info = query_context->getClientInfo();
        client_info.brpc_protocol_version = request->brpc_protocol_revision();
        client_info.query_kind = ClientInfo::QueryKind::SECONDARY_QUERY;
        client_info.interface = ClientInfo::Interface::BRPC;
        Decimal64 initial_query_start_time_microseconds {request->initial_query_start_time()};
        client_info.initial_query_start_time = initial_query_start_time_microseconds / 1000000;
        client_info.initial_query_start_time_microseconds = initial_query_start_time_microseconds;
        client_info.initial_user = request->user();
        client_info.initial_query_id = request->query_id();

        client_info.initial_address = initial_socket_address;

        client_info.current_query_id = request->query_id() + "_" + std::to_string(request->plan_segment_id());
        client_info.current_address = Poco::Net::SocketAddress(request->current_host(), request->current_port());

        /// Prepare settings.
        SettingsChanges settings_changes;
        settings_changes.reserve(request->settings_size());
        for (const auto & [key, value] : request->settings())
        {
            settings_changes.push_back({key, value});
        }

        /// Sets an extra row policy based on `client_info.initial_user`
        query_context->setInitialRowPolicy();

        /// Quietly clamp to the constraints since it's a secondary query.
        query_context->clampToSettingsConstraints(settings_changes);
        query_context->applySettingsChanges(settings_changes);

        /// Disable function name normalization when it's a secondary query, because queries are either
        /// already normalized on initiator node, or not normalized and should remain unnormalized for
        /// compatibility.
        query_context->setSetting("normalize_function_names", Field(0));

        /// Set quota
        if (!request->has_quota())
            query_context->setQuotaKey(request->quota());

        if (!query_context->hasQueryContext())
            query_context->makeQueryContext();

        ThreadFromGlobalPool async_thread([query_context = std::move(query_context),
                                           plan_segment_buf = std::make_shared<butil::IOBuf>(cntl->request_attachment().movable())]() {
            try
            {
                /// Plan segment Deserialization can't run in bthread since checkStackSize method is not compatible with all user-space lightweight threads that manually allocated stacks.
                ReadBufferFromBrpcBuf plan_segment_read_buf(*plan_segment_buf);
                auto plan_segment = PlanSegment::deserializePlanSegment(plan_segment_read_buf, query_context);
                executePlanSegmentInternal(std::move(plan_segment), std::move(query_context), false);
            }
            catch (...)
            {
                tryLogCurrentException(__PRETTY_FUNCTION__);
            }
        });
        async_thread.detach();
    }
    catch (...)
    {
        auto error_msg = getCurrentExceptionMessage(false);
        cntl->SetFailed(error_msg);
        LOG_ERROR(log, "executeQuery failed: {}", error_msg);
    }
}

}
