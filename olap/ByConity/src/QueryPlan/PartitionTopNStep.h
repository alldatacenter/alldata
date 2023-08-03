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
#include <Disks/IVolume.h>
#include <Processors/Transforms/PartitionTopNTransform.h>
#include <QueryPlan/ITransformingStep.h>

namespace DB
{
/// Sorts stream of data. See MergeSortingTransform.
class PartitionTopNStep : public ITransformingStep
{
public:
    explicit PartitionTopNStep(const DataStream & input_stream_, const Names & partition_, const Names & order_by_, UInt64 limit_, PartitionTopNModel model_);

    String getName() const override { return "PartitionTopN"; }

    Type getType() const override { return Type::PartitionTopN; }
    const Names & getPartition() const { return partition; }
    const Names & getOrderBy() const { return order_by; }
    UInt64 getLimit() const { return limit; }
    PartitionTopNModel getModel() const { return model; }

    void transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &) override;

    void describeActions(JSONBuilder::JSONMap & map) const override;
    void describeActions(FormatSettings & settings) const override;

    void serialize(WriteBuffer &) const override;
    static QueryPlanStepPtr deserialize(ReadBuffer &, ContextPtr context_ = nullptr);
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr ptr) const override;
    void setInputStreams(const DataStreams & input_streams_) override;

private:
    Names partition;
    Names order_by;
    UInt64 limit;
    PartitionTopNModel model;
};

}
