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
#include <Processors/IAccumulatingTransform.h>
#include <Processors/Transforms/AggregatingTransform.h>

namespace DB
{

class RollupWithGroupingTransform : public IAccumulatingTransform
{
public:
    RollupWithGroupingTransform(Block header, AggregatingTransformParamsPtr params, ColumnNumbers groupings_, Names grouping_names_);
    String getName() const override { return "RollupWithGroupingTransform"; }
    static Block transformHeader(Block res, const Names & grouping_names);

protected:
    void consume(Chunk chunk) override;
    Chunk generate() override;

private:
    AggregatingTransformParamsPtr params;
    ColumnNumbers keys;
    ColumnNumbers groupings;
    Names grouping_names;
    Chunks consumed_chunks;
    Chunk rollup_chunk;
    size_t last_removed_key = 0;

    Chunk merge(Chunks && chunks, bool final);
};

}
