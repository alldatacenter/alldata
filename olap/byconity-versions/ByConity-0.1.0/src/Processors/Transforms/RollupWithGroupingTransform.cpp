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

#include <cstddef>
#include <DataTypes/DataTypesNumber.h>
#include <Processors/Transforms/RollupWithGroupingTransform.h>
#include <Processors/Transforms/TotalsHavingTransform.h>

namespace DB
{

RollupWithGroupingTransform::RollupWithGroupingTransform(
    Block header, AggregatingTransformParamsPtr params_, ColumnNumbers groupings_, Names grouping_names_)
    : IAccumulatingTransform(header, transformHeader(params_->getHeader(), grouping_names_))
    , params(std::move(params_))
    , keys(params->params.keys)
    , groupings(groupings_)
    , grouping_names(grouping_names_)
{
}


Block RollupWithGroupingTransform::transformHeader(Block res, const Names & grouping_names)
{
    for (const auto & name : grouping_names)
    {
        ColumnPtr col = ColumnUInt8::create();
        res.insert({col, std::make_shared<DataTypeUInt8>(), name});
    }
    return res;
}

void RollupWithGroupingTransform::consume(Chunk chunk)
{
    consumed_chunks.emplace_back(std::move(chunk));
}

Chunk RollupWithGroupingTransform::merge(Chunks && chunks, bool final)
{
    BlocksList rollup_blocks;
    for (auto & chunk : chunks)
        rollup_blocks.emplace_back(getOutputPort().getHeader().cloneWithColumns(chunk.detachColumns()));

    auto rollup_block = params->aggregator.mergeBlocks(rollup_blocks, final);
    auto num_rows = rollup_block.rows();
    return Chunk(rollup_block.getColumns(), num_rows);
}

Chunk RollupWithGroupingTransform::generate()
{
    if (!consumed_chunks.empty())
    {
        if (consumed_chunks.size() > 1)
            rollup_chunk = merge(std::move(consumed_chunks), false);
        else
            rollup_chunk = std::move(consumed_chunks.front());

        consumed_chunks.clear();
        last_removed_key = keys.size();

        for (size_t index = 0; index < grouping_names.size(); index++)
        {
            auto col = ColumnUInt8::create(rollup_chunk.getNumRows(), 0);
            rollup_chunk.addColumn(std::move(col));
        }
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

        for (auto grouping : groupings)
        {
            bool filled = false;
            for (size_t key_index = last_removed_key; key_index < keys.size(); ++key_index)
                if (keys[key_index] == grouping)
                {
                    filled = true;
                    break;
                }
            auto col = ColumnUInt8::create(rollup_chunk.getNumRows(), filled ? 1 : 0);
            rollup_chunk.addColumn(std::move(col));
        }
    }

    finalizeChunk(gen_chunk);
    return gen_chunk;
}

}
