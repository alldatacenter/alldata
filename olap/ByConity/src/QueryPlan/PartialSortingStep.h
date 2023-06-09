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
#include <Core/SortDescription.h>
#include <DataStreams/SizeLimits.h>
#include <QueryPlan/ITransformingStep.h>

namespace DB
{

/// Sort separate chunks of data.
class PartialSortingStep : public ITransformingStep
{
public:
    explicit PartialSortingStep(const DataStream & input_stream, SortDescription sort_description_, UInt64 limit_, SizeLimits size_limits_ = {});

    String getName() const override { return "PartialSorting"; }

    Type getType() const override { return Type::PartialSorting; }
    const SortDescription & getSortDescription() const { return sort_description; }
    UInt64 getLimit() const { return limit; }
    const SizeLimits & getSizeLimits() const { return size_limits; }

    void transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &) override;

    void describeActions(JSONBuilder::JSONMap & map) const override;
    void describeActions(FormatSettings & settings) const override;

    /// Add limit or change it to lower value.
    void updateLimit(size_t limit_);

    void serialize(WriteBuffer &) const override;
    static QueryPlanStepPtr deserialize(ReadBuffer &, ContextPtr context_ = nullptr);
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr ptr) const override;
    void setInputStreams(const DataStreams & input_streams_) override;

private:
    SortDescription sort_description;
    UInt64 limit;
    SizeLimits size_limits;
};

}
