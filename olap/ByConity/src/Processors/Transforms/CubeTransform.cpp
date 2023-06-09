/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include <Processors/Transforms/CubeTransform.h>
#include <Processors/Transforms/TotalsHavingTransform.h>
#include <QueryPlan/AggregatingStep.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

CubeTransform::CubeTransform(Block header, AggregatingTransformParamsPtr params_)
    : IAccumulatingTransform(std::move(header), appendGroupingSetColumn(params_->getHeader()))
    , params(std::move(params_))
    , keys(params->params.keys)
{
    if (keys.size() >= 8 * sizeof(mask))
        throw Exception("Too many keys are used for CubeTransform.", ErrorCodes::LOGICAL_ERROR);
}

Chunk CubeTransform::merge(Chunks && chunks, bool final)
{
    BlocksList rollup_blocks;
    for (auto & chunk : chunks)
        rollup_blocks.emplace_back(getInputPort().getHeader().cloneWithColumns(chunk.detachColumns()));

    auto rollup_block = params->aggregator.mergeBlocks(rollup_blocks, final);
    auto num_rows = rollup_block.rows();
    return Chunk(rollup_block.getColumns(), num_rows);
}

void CubeTransform::consume(Chunk chunk)
{
    consumed_chunks.emplace_back(std::move(chunk));
}

Chunk CubeTransform::generate()
{
    if (!consumed_chunks.empty())
    {
        if (consumed_chunks.size() > 1)
            cube_chunk = merge(std::move(consumed_chunks), false);
        else
            cube_chunk = std::move(consumed_chunks.front());

        consumed_chunks.clear();

        auto num_rows = cube_chunk.getNumRows();
        mask = (UInt64(1) << keys.size()) - 1;

        current_columns = cube_chunk.getColumns();
        current_zero_columns.clear();
        current_zero_columns.reserve(keys.size());

        for (auto key : keys)
            current_zero_columns.emplace_back(current_columns[key]->cloneEmpty()->cloneResized(num_rows));
    }

    auto gen_chunk = std::move(cube_chunk);

    if (mask)
    {
        --mask;

        auto columns = current_columns;
        auto size = keys.size();
        for (size_t i = 0; i < size; ++i)
            /// Reverse bit order to support previous behaviour.
            if ((mask & (UInt64(1) << (size - i - 1))) == 0)
                columns[keys[i]] = current_zero_columns[i];

        Chunks chunks;
        chunks.emplace_back(std::move(columns), current_columns.front()->size());
        cube_chunk = merge(std::move(chunks), false);
    }

    finalizeChunk(gen_chunk);
    if (!gen_chunk.empty())
        gen_chunk.addColumn(0, ColumnUInt64::create(gen_chunk.getNumRows(), grouping_set++));
    return gen_chunk;
}

}
