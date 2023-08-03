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

#include <QueryPlan/ReadNothingStep.h>
#include <Processors/QueryPipeline.h>
#include <Processors/Sources/NullSource.h>

namespace DB
{

ReadNothingStep::ReadNothingStep(Block output_header)
    : ISourceStep(DataStream{.header = std::move(output_header), .has_single_port = true})
{
}

void ReadNothingStep::initializePipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &)
{
    pipeline.init(Pipe(std::make_shared<NullSource>(getOutputStream().header)));
}

void ReadNothingStep::serialize(WriteBuffer & buffer) const
{
    serializeBlock(output_stream->header, buffer);
}

QueryPlanStepPtr ReadNothingStep::deserialize(ReadBuffer & buffer, ContextPtr )
{
    Block output_header = deserializeBlock(buffer);
    return std::make_unique<ReadNothingStep>(output_header);
}

std::shared_ptr<IQueryPlanStep> ReadNothingStep::copy(ContextPtr) const
{
    return std::make_shared<ReadNothingStep>(output_stream->header);
}

}
