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

#include <QueryPlan/AnyStep.h>

namespace DB
{
QueryPipelinePtr AnyStep::updatePipeline(QueryPipelines, const BuildQueryPipelineSettings &)
{
    throw Exception("AnyStep is a fake step", ErrorCodes::NOT_IMPLEMENTED);
}

void AnyStep::serialize(WriteBuffer &) const
{
    throw Exception("AnyStep is a fake step", ErrorCodes::NOT_IMPLEMENTED);
}

QueryPlanStepPtr AnyStep::deserialize(ReadBuffer &, ContextPtr)
{
    throw Exception("AnyStep is a fake step", ErrorCodes::NOT_IMPLEMENTED);
}

std::shared_ptr<IQueryPlanStep> AnyStep::copy(ContextPtr) const
{
    return std::make_unique<AnyStep>(output_stream.value(), group_id);
}

void AnyStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
}

}
