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
#include <QueryPlan/IQueryPlanStep.h>

namespace DB
{
using GroupId = UInt32;

class AnyStep : public IQueryPlanStep
{
public:
    AnyStep(DataStream output, GroupId group_id_) : group_id(group_id_) { output_stream = output; }

    String getName() const override { return "Leaf"; }
    Type getType() const override { return Type::Any; }

    QueryPipelinePtr updatePipeline(QueryPipelines pipelines, const BuildQueryPipelineSettings & context) override;
    void serialize(WriteBuffer & buf) const override;
    static QueryPlanStepPtr deserialize(ReadBuffer & buf, ContextPtr context);

    GroupId getGroupId() const { return group_id; }

    std::shared_ptr<IQueryPlanStep> copy(ContextPtr context) const override;
    void setInputStreams(const DataStreams & input_streams_) override;

private:
    GroupId group_id;
};
}
