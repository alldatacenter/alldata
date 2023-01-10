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

#include <Processors/Merges/Algorithms/IMergingAlgorithm.h>
#include <Processors/Merges/Algorithms/MergedData.h>
#include <Core/SortDescription.h>
#include <Core/SortCursor.h>


namespace DB
{

/// Merges several sorted inputs into one sorted output.
class MergingSortedAlgorithm final : public IMergingAlgorithm
{
public:

    /// Used for building part id mappings between input streams and output.
    /// Different from row sources in that there is no limit on the number of input parts.
    using PartIdMappingCallback = std::function<void(size_t part_index, size_t nrows)>;

    MergingSortedAlgorithm(
        const Block & header,
        size_t num_inputs,
        SortDescription description_,
        size_t max_block_size,
        UInt64 limit_ = 0,
        WriteBuffer * out_row_sources_buf_ = nullptr,
        bool use_average_block_sizes = false,
        PartIdMappingCallback part_id_mapping_cb_ = nullptr);

    void addInput();

    void initialize(Inputs inputs) override;
    void consume(Input & input, size_t source_num) override;
    Status merge() override;

    const MergedData & getMergedData() const { return merged_data; }

private:
    MergedData merged_data;

    /// Settings
    SortDescription description;
    UInt64 limit;
    bool has_collation = false;

    /// Used in Vertical merge algorithm to gather non-PK/non-index columns (on next step)
    /// If it is not nullptr then it should be populated during execution
    WriteBuffer * out_row_sources_buf = nullptr;

    /// Chunks currently being merged.
    Inputs current_inputs;

    SortCursorImpls cursors;

    SortingHeap<SortCursor> queue_without_collation;
    SortingHeap<SortCursorWithCollation> queue_with_collation;

    Status insertFromChunk(size_t source_num);

    template <typename TSortingHeap>
    Status mergeImpl(TSortingHeap & queue);

    PartIdMappingCallback part_id_mapping_cb;
};

}
