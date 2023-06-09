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

#include <Processors/Transforms/RollupTransform.h>
#include <Processors/Transforms/TotalsHavingTransform.h>
#include <QueryPlan/AggregatingStep.h>

namespace DB
{

RollupTransform::RollupTransform(Block header, AggregatingTransformParamsPtr params_, bool add_grouping_set_column_)
    : IAccumulatingTransform(std::move(header),
                             add_grouping_set_column_ ? appendGroupingSetColumn(params_->getHeader()) : params_->getHeader())
    , params(std::move(params_))
    , keys(params->params.keys)
    , add_grouping_set_column(add_grouping_set_column_)
{
}

void RollupTransform::consume(Chunk chunk)
{
    consumed_chunks.emplace_back(std::move(chunk));
}

Chunk RollupTransform::merge(Chunks && chunks, bool final)
{
    BlocksList rollup_blocks;
    for (auto & chunk : chunks)
        rollup_blocks.emplace_back(getInputPort().getHeader().cloneWithColumns(chunk.detachColumns()));

    auto rollup_block = params->aggregator.mergeBlocks(rollup_blocks, final);
    auto num_rows = rollup_block.rows();
    return Chunk(rollup_block.getColumns(), num_rows);
}

Chunk RollupTransform::generate()
{
    if (!consumed_chunks.empty())
    {
        if (consumed_chunks.size() > 1)
            rollup_chunk = merge(std::move(consumed_chunks), false);
        else
            rollup_chunk = std::move(consumed_chunks.front());

        consumed_chunks.clear();
        last_removed_key = keys.size();
    }

    auto gen_chunk = std::move(rollup_chunk);

    if (last_removed_key)
    {
        --last_removed_key;
        auto key = keys[last_removed_key];

        auto num_rows = gen_chunk.getNumRows();
        auto columns = gen_chunk.getColumns();
        columns[key] = columns[key]->cloneEmpty()->cloneResized(num_rows);

        Chunks chunks;
        chunks.emplace_back(std::move(columns), num_rows);
        rollup_chunk = merge(std::move(chunks), false);
    }

    finalizeChunk(gen_chunk);
    if (add_grouping_set_column && !gen_chunk.empty())
        gen_chunk.addColumn(0, ColumnUInt64::create(gen_chunk.getNumRows(), set_counter++));
    return gen_chunk;
}

}
