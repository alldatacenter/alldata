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

#include <QueryPlan/ValuesStep.h>

#include <DataStreams/OneBlockInputStream.h>
#include <Processors/QueryPipeline.h>
#include <Processors/Sources/SourceFromSingleChunk.h>
#include <QueryPlan/PlanSerDerHelper.h>

namespace DB
{
ValuesStep::ValuesStep(Block header, Fields fields_, size_t rows_) : ISourceStep(DataStream{.header = header}), fields(fields_), rows(rows_)
{
}

void ValuesStep::initializePipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &)
{
    Block block;

    for (size_t index = 0; index < fields.size(); ++index)
    {
        auto col = output_stream->header.getByPosition(index).type->createColumn();
        for (size_t i = 0; i < rows; i++)
        {
            col->insert(fields[index]);
        }
        block.insert({std::move(col), output_stream->header.getByPosition(index).type, output_stream->header.getByPosition(index).name});
    }

    pipeline.init(Pipe(std::make_shared<SourceFromSingleChunk>(getOutputStream().header, Chunk(block.getColumns(), block.rows()))));
}

void ValuesStep::serialize(WriteBuffer & buffer) const
{
    serializeDataStream(output_stream.value(), buffer);
    writeVarUInt(fields.size(), buffer);
    for (auto & item : fields)
    {
        writeFieldBinary(item, buffer);
    }
    writeVarUInt(rows, buffer);
    writeVarInt(unique_id, buffer);
}

QueryPlanStepPtr ValuesStep::deserialize(ReadBuffer & buffer, ContextPtr)
{
    DataStream input_stream = deserializeDataStream(buffer);
    size_t size;
    readVarUInt(size, buffer);
    Fields fields;
    for (size_t i = 0; i < size; ++i)
    {
        Field field;
        readFieldBinary(field, buffer);
        fields.emplace_back(field);
    }

    size_t rows;
    readVarUInt(rows, buffer);
    Int32 unique_id;
    readVarInt(unique_id, buffer);
    return std::make_shared<ValuesStep>(input_stream.header, fields, rows);
}

std::shared_ptr<IQueryPlanStep> ValuesStep::copy(ContextPtr) const
{
    return std::make_shared<ValuesStep>(output_stream->header, fields);
}

}
