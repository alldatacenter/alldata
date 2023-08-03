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

#include <Interpreters/RuntimeFilter/RuntimeFilterService.h>

#include <Interpreters/RuntimeFilter/RuntimeFilterManager.h>
#include <Interpreters/SegmentScheduler.h>
#include <Processors/Exchange/DataTrans/RpcChannelPool.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

static void OnDispatchRuntimeFilter(Protos::DispatchRuntimeFilterResponse * response, brpc::Controller * cntl, std::shared_ptr<RpcClient> rpc_channel)
{
    std::unique_ptr<Protos::DispatchRuntimeFilterResponse> response_guard(response);
    std::unique_ptr<brpc::Controller> cntl_guard(cntl);

    rpc_channel->checkAliveWithController(*cntl);
    if (cntl->Failed())
    {
        LOG_DEBUG(&Poco::Logger::get("RuntimeFilterService"), "dispatch runtime filter to worker failed, message: " + cntl->ErrorText());
    }
    else
    {
        LOG_DEBUG(&Poco::Logger::get("RuntimeFilterService"), "dispatch runtime filter to worker success");
    }
}

static String to_string(const std::vector<size_t> & execute_segment_ids)
{
    if (execute_segment_ids.empty())
        return "{}";
    std::stringstream ss;
    auto it = execute_segment_ids.begin();
    ss << "{" << std::to_string(*it);
    for (it++; it != execute_segment_ids.end(); it++)
        ss << "," << std::to_string(*it);
    ss << "}";
    return ss.str();
}

void RuntimeFilterService::transferRuntimeFilter(
    ::google::protobuf::RpcController * /*controller*/,
    const ::DB::Protos::TransferRuntimeFilterRequest * request,
    ::DB::Protos::TransferRuntimeFilterResponse * /*response*/,
    ::google::protobuf::Closure * done)
{
    brpc::ClosureGuard done_guard(done);
    try
    {
        std::shared_ptr<RuntimeFilter> runtime_filter = std::make_shared<RuntimeFilter>();
        ReadBufferFromMemory read_buffer(request->filter_data().c_str(), request->filter_data().size());
        runtime_filter->deserialize(read_buffer, false);
        auto & manager = RuntimeFilterManager::getInstance();
        auto partial_runtime_filter = manager.getPartialRuntimeFilter(request->query_id(), request->filter_id());
        size_t received = partial_runtime_filter->merge(runtime_filter, request->remote_address());

        LOG_TRACE(log, "Coordinator receive query id: {}, filter id: {}, {}/{}",
                  request->query_id(), request->filter_id(), received, request->require_parallel_size());

        if (received > 0 && received == partial_runtime_filter->getRequireParallelSize() && context->getSegmentScheduler())
        {
            Stopwatch timer{CLOCK_MONOTONIC_COARSE};
            timer.start();

            WriteBufferFromOwnString write_buffer;
            partial_runtime_filter->getRuntimeFilter()->serialize(write_buffer, true);
            write_buffer.next();

            LOG_DEBUG(log, "Coordinator receive all partial runtime filter for query id: {}, filter id: {}, "
                           "try to dispatch to execute plan segments: {}",
                      request->query_id(), request->filter_id(), to_string(partial_runtime_filter->getExecuteSegmentIds()));

            for (auto segment_id : partial_runtime_filter->getExecuteSegmentIds())
            {
                Protos::DispatchRuntimeFilterRequest dispatch_request;
                dispatch_request.set_query_id(request->query_id());
                dispatch_request.set_segment_id(segment_id);
                dispatch_request.set_filter_id(request->filter_id());
                dispatch_request.set_filter_data(write_buffer.str());

//                /// Get worker addresses for execute plan segment id
//                size_t retry_count = 0;
//                while (context.getSegmentScheduler()->getWorkerAddress(request->query_id(), segment_id).size()
//                           != request->require_parallel_size()
//                       && retry_count <= context.getSettingsRef().runtime_filter_get_worker_times)
//                {
//                    LOG_WARNING(log,
//                        "RuntimeFilter wait for worker required count: {}, for query id: {}, filter id: {}, segment id: {}, retry count: "
//                        "{}",
//                        request->require_parallel_size(),
//                        request->query_id(),
//                        request->filter_id(),
//                        segment_id,
//                        retry_count);
//
//                    bthread_usleep(context.getSettingsRef().runtime_filter_get_worker_interval);
//                    retry_count++;
//                }

                AddressInfos worker_addresses = context->getSegmentScheduler()->getWorkerAddress(request->query_id(), segment_id);
//                if (retry_count >= context.getSettingsRef().runtime_filter_get_worker_times
//                    && worker_addresses.size() != request->require_parallel_size())
//                {
//                    LOG_WARNING(
//                        log,
//                        "RuntimeFilter can not get enough worker address size {}, not equal to required count {}, "
//                        "for query id: {}, filter id: {}, segment id: {}, due to segment scheduler not schedule this segment",
//                        worker_addresses.size(),
//                        request->require_parallel_size(),
//                        request->query_id(),
//                        request->filter_id(),
//                        segment_id);
//
//                    LOG_WARNING(
//                        log,
//                        "RuntimeFilter get segment scheduler current dispatch status for query id: {}, status: {}",
//                        request->query_id(),
//                        context.getSegmentScheduler()->getCurrentDispatchStatus(request->query_id()));
//
//                    return;
//                }

                for (const auto & address : worker_addresses)
                {
                    String host_port = extractExchangeStatusHostPort(address);
                    std::shared_ptr<RpcClient> rpc_client = RpcChannelPool::getInstance().getClient(host_port, BrpcChannelPoolOptions::DEFAULT_CONFIG_KEY, true);
                    std::shared_ptr<DB::Protos::RuntimeFilterService_Stub> command_service
                        = std::make_shared<Protos::RuntimeFilterService_Stub>(&rpc_client->getChannel());
                    auto * controller = new brpc::Controller;
                    auto * dispatch_response = new Protos::DispatchRuntimeFilterResponse;
                    command_service->dispatchRuntimeFilter(
                        controller,
                        &dispatch_request,
                        dispatch_response,
                        brpc::NewCallback(OnDispatchRuntimeFilter, dispatch_response, controller, rpc_client));
                    LOG_DEBUG(
                        log, "dispatch runtime filter query id: {}, segment id: {}, filter id: {}, host: {}", request->query_id(), segment_id, request->filter_id(), host_port);
                }
            }

            LOG_DEBUG(log, "RuntimeFilter dispatch all filter to worker with {} ms", timer.elapsedMilliseconds());
        }
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }
}

void RuntimeFilterService::dispatchRuntimeFilter(
    ::google::protobuf::RpcController * /*controller*/,
    const ::DB::Protos::DispatchRuntimeFilterRequest * request,
    ::DB::Protos::DispatchRuntimeFilterResponse * /*response*/,
    ::google::protobuf::Closure * done)
{
    brpc::ClosureGuard done_guard(done);
    try
    {
        Stopwatch timer{CLOCK_MONOTONIC_COARSE};
        timer.start();
        std::shared_ptr<RuntimeFilter> runtime_filter = std::make_shared<RuntimeFilter>();
        ReadBufferFromMemory read_buffer(request->filter_data().c_str(), request->filter_data().size());
        runtime_filter->deserialize(read_buffer, true);
        RuntimeFilterManager::getInstance().putRuntimeFilter(
            request->query_id(), request->segment_id(), request->filter_id(), runtime_filter);
        LOG_DEBUG(log, "Receive RuntimeFilter query id: {}, segment id: {}, filter id: {}, deserialize cost: {} ms",
                  request->query_id(), request->segment_id(), request->filter_id(), timer.elapsedMilliseconds());
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }
}
}
