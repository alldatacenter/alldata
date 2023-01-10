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

#include <Interpreters/RuntimeFilter/BuildRuntimeFilterTransform.h>

#include <Processors/Exchange/DataTrans/RpcClient.h>
#include <Processors/Exchange/DataTrans/RpcChannelPool.h>
#include <Protos/runtime_filter.pb.h>
#include <brpc/server.h>
#include <Common/Brpc/BrpcChannelPoolOptions.h>

namespace DB
{
BuildRuntimeFilterTransform::BuildRuntimeFilterTransform(
    const Block & header,
    ContextPtr context,
    const Block & join_keys,
    std::unordered_map<String, String> join_column_map,
    bool enable_bloom_filter,
    bool enable_range_filter,
    std::unordered_map<String, std::vector<String>> multiple_alias,
    std::optional<std::shared_ptr<RuntimeFilterConsumer>> consumer_)
    : ISimpleTransform(header, header, false), log(&Poco::Logger::get("BuildRuntimeFilterInputStream"))
{
    if (consumer_)
        consumer = consumer_.value();

    rf = std::make_shared<RuntimeFilter>();
    rf->init(context, join_keys, header, join_column_map, enable_bloom_filter, enable_range_filter, multiple_alias);
}

ISimpleTransform::Status BuildRuntimeFilterTransform::prepare()
{
    if (finished)
        return Status::Finished;
    auto stats = ISimpleTransform::prepare();
    if (stats == Status::Finished && input.isFinished())
    {
        input_finish = true;
        return Status::Ready;
    }
    return stats;
}

void BuildRuntimeFilterTransform::work()
{
    if (input_finish)
    {
        rf->finalize();
        if (consumer)
            consumer->addFinishRuntimeFilter(rf);
        finished = true;
        return;
    }
    else
        ISimpleTransform::work();
}

void BuildRuntimeFilterTransform::transform(Chunk & chunk)
{
    Block block = getInputPort().getHeader().cloneWithColumns(chunk.getColumns());
    rf->add(block);
}

RuntimeFilterConsumer::RuntimeFilterConsumer(
    std::string query_id_,
    UInt32 filter_id_,
    size_t local_stream_parallel_,
    size_t parallel_,
    AddressInfo coordinator_address_,
    AddressInfo current_address_)
    : query_id(std::move(query_id_))
    , filter_id(filter_id_)
    , local_stream_parallel(local_stream_parallel_)
    , parallel(parallel_)
    , coordinator_address(std::move(coordinator_address_))
    , current_address(std::move(current_address_))
    , timer{CLOCK_MONOTONIC_COARSE}
    , log(&Poco::Logger::get("RuntimeFilterBuild"))
{
    timer.start();
}

RuntimeFilterPtr RuntimeFilterConsumer::mergeRuntimeFilter()
{
    RuntimeFilterPtr runtime_filter = runtime_filters[0];
    if (runtime_filters.size() > 1)
    {
        std::vector<RuntimeFilterPtr> other_filters(runtime_filters.begin() + 1, runtime_filters.end());
        runtime_filter->mergeBatchFilers(other_filters);
    }
    return runtime_filter;
}

void RuntimeFilterConsumer::addFinishRuntimeFilter(RuntimeFilterPtr runtime_filter)
{
    std::lock_guard<std::mutex> guard(mutex);
    runtime_filters.emplace_back(std::move(runtime_filter));
    // no need to unlock since all other streams are finished.
    if (runtime_filters.size() == local_stream_parallel)
        return transferRuntimeFilter(mergeRuntimeFilter());
}

static void OnSendRuntimeFilterCallback(Protos::TransferRuntimeFilterResponse * response, brpc::Controller * cntl, std::shared_ptr<RpcClient> rpc_channel)
{
    std::unique_ptr<Protos::TransferRuntimeFilterResponse> response_guard(response);
    std::unique_ptr<brpc::Controller> cntl_guard(cntl);

    rpc_channel->checkAliveWithController(*cntl);
    if (cntl->Failed())
        LOG_DEBUG(&Poco::Logger::get("RuntimeFilterBuild"), "Send to coordinator failed, message: " + cntl->ErrorText());
    else
        LOG_DEBUG(&Poco::Logger::get("RuntimeFilterBuild"), "Send to coordinator success");
}

void RuntimeFilterConsumer::transferRuntimeFilter(const RuntimeFilterPtr & runtime_filter)
{
    WriteBufferFromOwnString write_buffer;
    runtime_filter->serialize(write_buffer, false);
    write_buffer.next();
    auto * response = new Protos::TransferRuntimeFilterResponse;
    auto * controller = new brpc::Controller;
    Protos::TransferRuntimeFilterRequest request;
    std::shared_ptr<RpcClient> rpc_client = RpcChannelPool::getInstance().getClient(extractExchangeStatusHostPort(coordinator_address), BrpcChannelPoolOptions::DEFAULT_CONFIG_KEY, true);
    Protos::RuntimeFilterService_Stub runtime_filter_service(&rpc_client->getChannel());
    request.set_query_id(query_id);
    request.set_filter_id(filter_id);
    request.set_remote_address(extractExchangeStatusHostPort(current_address));
    request.set_require_parallel_size(parallel);
    request.set_filter_data(write_buffer.str());
    runtime_filter_service.transferRuntimeFilter(
        controller, &request, response, brpc::NewCallback(OnSendRuntimeFilterCallback, response, controller, rpc_client));

    LOG_DEBUG(log, "Build success query id: {}, filter id: {}, stream parallel: {}, plan segment parallel: {}, cost: {} ms",
        query_id, filter_id, local_stream_parallel, parallel, timer.elapsedMilliseconds());
}

}
