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

#include <Processors/Merges/IMergingTransform.h>
#include <Processors/Merges/Algorithms/MergingSortedAlgorithm.h>


namespace DB
{

/// Implementation of IMergingTransform via MergingSortedAlgorithm.
class MergingSortedTransform final : public IMergingTransform<MergingSortedAlgorithm>
{
public:
    MergingSortedTransform(
        const Block & header,
        size_t num_inputs,
        SortDescription description,
        size_t max_block_size,
        UInt64 limit_ = 0,
        WriteBuffer * out_row_sources_buf_ = nullptr,
        bool quiet_ = false,
        bool use_average_block_sizes = false,
        bool have_all_inputs_ = true,
        MergingSortedAlgorithm::PartIdMappingCallback part_id_mapping_cb_ = nullptr);

    String getName() const override { return "MergingSortedTransform"; }

protected:
    void onNewInput() override;
    void onFinish() override;

private:
    bool quiet = false;
};

}
