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

#include <Processors/Merges/MergingSortedTransform.h>
#include <DataStreams/ColumnGathererStream.h>
#include <IO/WriteBuffer.h>

#include <common/logger_useful.h>

namespace DB
{

MergingSortedTransform::MergingSortedTransform(
    const Block & header,
    size_t num_inputs,
    SortDescription  description_,
    size_t max_block_size,
    UInt64 limit_,
    WriteBuffer * out_row_sources_buf_,
    bool quiet_,
    bool use_average_block_sizes,
    bool have_all_inputs_,
    MergingSortedAlgorithm::PartIdMappingCallback part_id_mapping_cb_)
    : IMergingTransform(
        num_inputs, header, header, have_all_inputs_,
        header,
        num_inputs,
        std::move(description_),
        max_block_size,
        limit_,
        out_row_sources_buf_,
        use_average_block_sizes,
        part_id_mapping_cb_)
    , quiet(quiet_)
{
}

void MergingSortedTransform::onNewInput()
{
    algorithm.addInput();
}

void MergingSortedTransform::onFinish()
{
    if (quiet)
        return;

    const auto & merged_data = algorithm.getMergedData();

    auto * log = &Poco::Logger::get("MergingSortedTransform");

    double seconds = total_stopwatch.elapsedSeconds();

    if (!seconds)
        LOG_DEBUG(log, "Merge sorted {} blocks, {} rows in 0 sec.", merged_data.totalChunks(), merged_data.totalMergedRows());
    else
        LOG_DEBUG(log, "Merge sorted {} blocks, {} rows in {} sec., {} rows/sec., {}/sec",
            merged_data.totalChunks(), merged_data.totalMergedRows(), seconds,
            merged_data.totalMergedRows() / seconds,
            ReadableSize(merged_data.totalAllocatedBytes() / seconds));
}

}
