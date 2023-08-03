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

#include <QueryPlan/EnforceSingleRowStep.h>

#include <Processors/Transforms/EnforceSingleRowTransform.h>
#include <Processors/QueryPipeline.h>

namespace DB
{
EnforceSingleRowStep::EnforceSingleRowStep(const DB::DataStream & input_stream_)
    : ITransformingStep(input_stream_, input_stream_.header, {})
{
}

void EnforceSingleRowStep::transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &)
{
    pipeline.resize(1);
    pipeline.addSimpleTransform([&](const Block & header) { return std::make_shared<EnforceSingleRowTransform>(header); });
}

void EnforceSingleRowStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
}

void EnforceSingleRowStep::serialize(WriteBuffer & buf) const
{
    IQueryPlanStep::serializeImpl(buf);
}

QueryPlanStepPtr EnforceSingleRowStep::deserialize(ReadBuffer & buf, ContextPtr)
{
    String step_description;
    readBinary(step_description, buf);

    DataStream input_stream = deserializeDataStream(buf);
    return std::make_unique<EnforceSingleRowStep>(input_stream);
}

std::shared_ptr<IQueryPlanStep> EnforceSingleRowStep::copy(ContextPtr) const
{
    return std::make_unique<EnforceSingleRowStep>(input_streams[0]);
}

}
