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

#include <Interpreters/Context.h>
#include <Interpreters/SegmentScheduler.h>
#include <Protos/plan_segment_manager.pb.h>
#include <brpc/server.h>
#include <common/logger_useful.h>

namespace DB
{
class PlanSegmentManagerRpcService : public Protos::PlanSegmentManagerService
{
public:
    explicit PlanSegmentManagerRpcService(ContextMutablePtr context_)
        : context(context_), log(&Poco::Logger::get("PlanSegmentManagerRpcService"))
    {
    }

    /// execute query described by plan segment
    void executeQuery(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::ExecutePlanSegmentRequest * request,
        ::DB::Protos::ExecutePlanSegmentResponse * response,
        ::google::protobuf::Closure * done) override;

    /// receive exception report send terminate query (coordinate host ---> segment executor host)
    void cancelQuery(
        ::google::protobuf::RpcController * /*controller*/,
        const ::DB::Protos::CancelQueryRequest * request,
        ::DB::Protos::CancelQueryResponse * response,
        ::google::protobuf::Closure * done) override
    {
        brpc::ClosureGuard done_guard(done);
        auto cancel_code
            = context->getPlanSegmentProcessList().tryCancelPlanSegmentGroup(request->query_id(), request->coordinator_address());
        response->set_ret_code(std::to_string(static_cast<int>(cancel_code)));
    }

    /// send plan segment status (segment executor host --> coordinator host)
    void sendPlanSegmentStatus(
        ::google::protobuf::RpcController * /*controller*/,
        const ::DB::Protos::SendPlanSegmentStatusRequest * request,
        ::DB::Protos::SendPlanSegmentStatusResponse * /*response*/,
        ::google::protobuf::Closure * done) override
    {
        brpc::ClosureGuard done_guard(done);
        RuntimeSegmentsStatus status(
            request->query_id(), request->segment_id(), request->is_succeed(), request->is_canceled(), request->message(), request->code());
        const SegmentSchedulerPtr & scheduler = context->getSegmentScheduler();
        scheduler->updateSegmentStatus(status);
        // this means exception happened during execution.
        if (!request->is_succeed() && !request->is_canceled())
        {
            scheduler->updateException(
                request->query_id(),
                "Segment:" + std::to_string(request->segment_id()) + ", exception:" + request->message(),
                request->code());
            try
            {
                scheduler->cancelPlanSegmentsFromCoordinator(request->query_id(), request->message(), context);
            }
            catch (...)
            {
                LOG_WARNING(log, "Call cancelPlanSegmentsFromCoordinator failed: " + getCurrentExceptionMessage(true));
            }
        }
        // todo  scheduler.cancelSchedule
    }

private:
    ContextMutablePtr context;
    Poco::Logger * log;
};
}
