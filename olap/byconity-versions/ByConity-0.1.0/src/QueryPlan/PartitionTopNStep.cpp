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

#include <IO/Operators.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteBuffer.h>
#include <Interpreters/Context.h>
#include <Processors/QueryPipeline.h>
#include <QueryPlan/PartitionTopNStep.h>
#include <Common/JSONBuilder.h>

namespace DB
{
PartitionTopNStep::PartitionTopNStep(
    const DataStream & input_stream_, const Names & partition_, const Names & order_by_, UInt64 limit_, PartitionTopNModel model_)
    : ITransformingStep(input_stream_, input_stream_.header, {}), partition(partition_), order_by(order_by_), limit(limit_), model(model_)
{
}

void PartitionTopNStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
    output_stream->header = input_streams_[0].header;
}


void PartitionTopNStep::transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings & context)
{
    auto input_header = pipeline.getHeader();
    pipeline.resize(context.context->getSettingsRef().max_threads);

    ColumnNumbers partition_by_columns;
    for (const auto & col : partition)
    {
        partition_by_columns.emplace_back(input_header.getPositionByName(col));
    }

    ColumnNumbers order_by_columns;
    for (const auto & col : order_by)
    {
        order_by_columns.emplace_back(input_header.getPositionByName(col));
    }

    pipeline.addSimpleTransform(
        [&](const Block & header) { return std::make_shared<PartitionTopNTransform>(header, limit, partition_by_columns, order_by_columns, model, true); });
}

void PartitionTopNStep::describeActions(FormatSettings &) const
{
}

void PartitionTopNStep::describeActions(JSONBuilder::JSONMap &) const
{
}

void PartitionTopNStep::serialize(WriteBuffer & buffer) const
{
    IQueryPlanStep::serializeImpl(buffer);
    writeBinary(partition, buffer);
    writeBinary(order_by, buffer);
    writeBinary(limit, buffer);
    serializeEnum(model, buffer);
}

QueryPlanStepPtr PartitionTopNStep::deserialize(ReadBuffer & buffer, ContextPtr)
{
    String step_description;
    readBinary(step_description, buffer);

    DataStream input_stream;
    input_stream = deserializeDataStream(buffer);

    Names partition;
    readBinary(partition, buffer);

    Names order_by;
    readBinary(order_by, buffer);

    UInt64 limit;
    readBinary(limit, buffer);

    PartitionTopNModel model;
    deserializeEnum(model, buffer);

    auto step = std::make_unique<PartitionTopNStep>(input_stream, partition, order_by, limit, model);

    step->setStepDescription(step_description);
    return step;
}

std::shared_ptr<IQueryPlanStep> PartitionTopNStep::copy(ContextPtr) const
{
    return std::make_shared<PartitionTopNStep>(input_streams[0], partition, order_by, limit, model);
}

}
