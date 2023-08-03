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
class AssignUniqueIdStep : public ITransformingStep
{
public:
    explicit AssignUniqueIdStep(const DataStream & input_stream_, String unique_id_);

    String getName() const override { return "AssignUniqueId"; }
    Type getType() const override { return IQueryPlanStep::Type::AssignUniqueId; }

    void setInputStreams(const DataStreams & input_streams_) override;
    void transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &) override;
    void serialize(WriteBuffer & buffer) const override;
    static QueryPlanStepPtr deserialize(ReadBuffer & buffer, ContextPtr context);

    String getUniqueId() const { return unique_id; }
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr) const override;

private:
    String unique_id;
};

}
