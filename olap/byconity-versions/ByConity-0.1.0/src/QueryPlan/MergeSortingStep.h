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
#include <DataStreams/SizeLimits.h>
#include <Disks/IVolume.h>

namespace DB
{

/// Sorts stream of data. See MergeSortingTransform.
class MergeSortingStep : public ITransformingStep
{
public:
    explicit MergeSortingStep(
            const DataStream & input_stream,
            const SortDescription & description_,
            size_t max_merged_block_size_,
            UInt64 limit_,
            size_t max_bytes_before_remerge_,
            double remerge_lowered_memory_bytes_ratio_,
            size_t max_bytes_before_external_sort_,
            VolumePtr tmp_volume_,
            size_t min_free_disk_space_);

    explicit MergeSortingStep(const DataStream & input_stream, const SortDescription & description_, UInt64 limit_)
        : MergeSortingStep(input_stream, description_, 0, limit_, 0, 0, 0, nullptr, 0)
    {
    }

    String getName() const override { return "MergeSorting"; }

    Type getType() const override { return Type::MergeSorting; }
    const SortDescription & getSortDescription() const { return description; }
    UInt64 getLimit() const { return limit; }

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
    SortDescription description;
    size_t max_merged_block_size;
    UInt64 limit;

    size_t max_bytes_before_remerge;
    double remerge_lowered_memory_bytes_ratio;
    size_t max_bytes_before_external_sort;
    VolumePtr tmp_volume;
    size_t min_free_disk_space;
};

}

