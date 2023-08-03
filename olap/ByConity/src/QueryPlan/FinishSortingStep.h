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
#include <Core/SortDescription.h>

namespace DB
{

/// Finish sorting of pre-sorted data. See FinishSortingTransform.
class FinishSortingStep : public ITransformingStep
{
public:
    FinishSortingStep(
        const DataStream & input_stream_,
        SortDescription prefix_description_,
        SortDescription result_description_,
        size_t max_block_size,
        UInt64 limit);

    String getName() const override { return "FinishSorting"; }

    Type getType() const override { return Type::FinishSorting; }

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
    SortDescription prefix_description;
    SortDescription result_description;
    size_t max_block_size;
    UInt64 limit;
};

}
