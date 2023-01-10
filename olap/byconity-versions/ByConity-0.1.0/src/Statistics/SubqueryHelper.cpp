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

#include <IO/ReadBufferFromString.h>
#include <Interpreters/executeQuery.h>
#include <Processors/Executors/PullingAsyncPipelineExecutor.h>
#include <Processors/Executors/PullingPipelineExecutor.h>
#include <Statistics/SubqueryHelper.h>
#include <Common/CurrentThread.h>

namespace DB::Statistics
{
static ContextMutablePtr createQueryContext(ContextPtr context)
{
    auto query_context = Context::createCopy(context);
    query_context->makeQueryContext();
    query_context->setCurrentQueryId(""); // generate random query_id
    query_context->getClientInfo().query_kind = ClientInfo::QueryKind::SECONDARY_QUERY;
    auto settings = context->getSettings();
    settings.enable_optimizer = false;
    settings.dialect_type = DialectType::CLICKHOUSE;
    settings.database_atomic_wait_for_drop_and_detach_synchronously = true;
    settings.enable_deterministic_sample_by_range = true;
    query_context->setSettings(settings);
    return query_context;
}

struct SubqueryHelper::DataImpl
{
    ContextPtr old_context;
    ContextMutablePtr subquery_context;
    // std::unique_ptr<CurrentThread::QueryScope> query_scope;
    std::unique_ptr<BlockIO> block_io;
    std::unique_ptr<PullingAsyncPipelineExecutor> executor;
};

SubqueryHelper::SubqueryHelper(std::unique_ptr<DataImpl> impl_) : impl(std::move(impl_))
{
}

SubqueryHelper SubqueryHelper::create(ContextPtr context, const String & sql)
{
    if (context->getSettingsRef().statistics_collect_debug_level >= 1)
    {
        LOG_INFO(&Poco::Logger::get("create stats subquery"), "collect stats with sql: " + sql);
    }
    auto impl = std::make_unique<SubqueryHelper::DataImpl>();
    impl->old_context = context;
    /// CurrentThread::detachQueryIfNotDetached();
    impl->subquery_context = createQueryContext(context);
    // impl->query_scope = std::make_unique<CurrentThread::QueryScope>(impl->subquery_context);
    impl->block_io = std::make_unique<BlockIO>(executeQuery(sql, impl->subquery_context, true, QueryProcessingStage::Complete, false));
    impl->executor = std::make_unique<PullingAsyncPipelineExecutor>(impl->block_io->pipeline);
    return SubqueryHelper(std::move(impl));
}

SubqueryHelper::~SubqueryHelper()
{
    if (impl->block_io)
    {
        impl->block_io->onFinish();
    }
    impl->executor.reset();
    impl->block_io.reset();
}

Block SubqueryHelper::getNextBlock()
{
    if (!impl->executor)
    {
        throw Exception("uninitialized SubqueryHelper", ErrorCodes::LOGICAL_ERROR);
    }

    Block block;
    auto ok = impl->executor->pull(block);
    impl->executor->rethrowExceptionIfHas();
    if (!ok)
    {
        return {};
    }
    else
    {
        return block;
    }
}

void executeSubQuery(ContextPtr old_context, const String & sql)
{
    ContextMutablePtr subquery_context = createQueryContext(old_context);
    // CurrentThread::QueryScope query_scope(subquery_context);

    String res;
    ReadBufferFromString is1(sql);
    WriteBufferFromString os1(res);

    executeQuery(is1, os1, false, subquery_context, {}, {}, true);
}
}
