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

#pragma once
#include <Processors/IAccumulatingTransform.h>
#include <Processors/Transforms/AggregatingTransform.h>

namespace DB
{

/// Takes blocks after grouping, with non-finalized aggregate functions.
/// Calculates subtotals and grand totals values for a set of columns.
class RollupTransform : public IAccumulatingTransform
{
public:
    RollupTransform(Block header, AggregatingTransformParamsPtr params, bool add_grouping_set_column = true);
    String getName() const override { return "RollupTransform"; }

protected:
    void consume(Chunk chunk) override;
    Chunk generate() override;

private:
    AggregatingTransformParamsPtr params;
    ColumnNumbers keys;
    Chunks consumed_chunks;
    Chunk rollup_chunk;
    size_t last_removed_key = 0;
    size_t set_counter = 0;
    // TODO @jingpeng @wangtao: this is a temporary fix to prevent RollupTransform generate `__grouping_set`,
    // can be removed if optimizer no longer use this transform
    bool add_grouping_set_column;

    Chunk merge(Chunks && chunks, bool final);
};

}
