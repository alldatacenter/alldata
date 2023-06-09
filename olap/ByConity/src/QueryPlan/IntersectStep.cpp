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

#include <QueryPlan/IntersectStep.h>

#include <Core/Block.h>
#include <Interpreters/Context.h>

namespace DB
{
IntersectStep::IntersectStep(
    DataStreams input_streams_,
    DataStream output_stream_,
    std::unordered_map<String, std::vector<String>> output_to_inputs_,
    bool distinct_)
    : SetOperationStep(input_streams_, output_stream_, output_to_inputs_), distinct(distinct_)
{
}

QueryPipelinePtr IntersectStep::updatePipeline(QueryPipelines, const BuildQueryPipelineSettings &)
{
    throw Exception("IntersectStep should be rewritten into UnionStep", ErrorCodes::NOT_IMPLEMENTED);
}

void IntersectStep::serialize(WriteBuffer &) const
{
    throw Exception("IntersectStep should be rewritten into UnionStep", ErrorCodes::NOT_IMPLEMENTED);
}

QueryPlanStepPtr IntersectStep::deserialize(ReadBuffer &, ContextPtr)
{
    throw Exception("IntersectStep should be rewritten into UnionStep", ErrorCodes::NOT_IMPLEMENTED);
}

bool IntersectStep::isDistinct() const
{
    return distinct;
}

std::shared_ptr<IQueryPlanStep> IntersectStep::copy(ContextPtr) const
{
    return std::make_unique<IntersectStep>(input_streams, output_stream.value(), distinct);
}

}
