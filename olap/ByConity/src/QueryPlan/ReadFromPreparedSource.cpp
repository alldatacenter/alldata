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

#include <QueryPlan/ReadFromPreparedSource.h>
#include <QueryPlan/PlanSerDerHelper.h>
#include <Processors/QueryPipeline.h>
#include <Storages/IStorage.h>
#include <Interpreters/Context.h>
#include <Interpreters/InterpreterSelectQuery.h>
#include <IO/WriteHelpers.h>
#include <Parsers/queryToString.h>


namespace DB
{

ReadFromPreparedSource::ReadFromPreparedSource(Pipe pipe_, std::shared_ptr<const Context> context_)
    : ISourceStep(DataStream{.header = pipe_.getHeader()})
    , pipe(std::move(pipe_))
    , context(std::move(context_))
{
}

void ReadFromPreparedSource::initializePipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &)
{
    for (const auto & processor : pipe.getProcessors())
        processors.emplace_back(processor);

    pipeline.init(std::move(pipe));

    if (context)
        pipeline.addInterpreterContext(std::move(context));
}

std::shared_ptr<IQueryPlanStep> ReadFromPreparedSource::copy(ContextPtr) const
{
    throw Exception("ReadFromPreparedSource can not copy", ErrorCodes::NOT_IMPLEMENTED);
}

std::shared_ptr<IQueryPlanStep> ReadFromStorageStep::copy(ContextPtr) const
{
    throw Exception("ReadFromStorageStep can not copy", ErrorCodes::NOT_IMPLEMENTED);
}

}
