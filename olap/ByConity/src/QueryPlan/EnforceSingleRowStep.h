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
#include <QueryPlan/ITransformingStep.h>

namespace DB
{
class EnforceSingleRowStep : public ITransformingStep
{
public:
    explicit EnforceSingleRowStep(const DataStream & input_stream_);

    String getName() const override { return "EnforceSingleRow"; }
    Type getType() const override { return Type::EnforceSingleRow; }

    void transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings & settings) override;
    void serialize(WriteBuffer & buffer) const override;
    static QueryPlanStepPtr deserialize(ReadBuffer & buffer, ContextPtr context);
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr) const override;
    void setInputStreams(const DataStreams & input_streams_) override;
};

}
