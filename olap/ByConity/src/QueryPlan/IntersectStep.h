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

#pragma once
#include <QueryPlan/SetOperationStep.h>

namespace DB
{
class IntersectStep : public SetOperationStep
{
public:
    IntersectStep(
        DataStreams input_streams_,
        DataStream output_stream_,
        std::unordered_map<String, std::vector<String>> output_to_inputs_,
        bool distinct_);

    IntersectStep(DataStreams input_streams_, DataStream output_stream_, bool distinct_) : IntersectStep(input_streams_, output_stream_, {}, distinct_) { }

    String getName() const override { return "Intersect"; }
    Type getType() const override { return Type::Intersect; }

    QueryPipelinePtr updatePipeline(QueryPipelines pipelines, const BuildQueryPipelineSettings & context) override;
    void serialize(WriteBuffer & buffer) const override;
    static QueryPlanStepPtr deserialize(ReadBuffer & buffer, ContextPtr context);

    bool isDistinct() const;
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr) const override;

private:
    bool distinct;
};
}
