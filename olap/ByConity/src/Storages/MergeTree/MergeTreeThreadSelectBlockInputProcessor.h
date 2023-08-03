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
#include <Storages/MergeTree/MergeTreeBaseSelectProcessor.h>


namespace DB
{

class MergeTreeReadPool;


/** Used in conjunction with MergeTreeReadPool, asking it for more work to do and performing whatever reads it is asked
  * to perform.
  */
class MergeTreeThreadSelectBlockInputProcessor : public MergeTreeBaseSelectProcessor
{
public:
    MergeTreeThreadSelectBlockInputProcessor(
        const size_t thread_,
        const std::shared_ptr<MergeTreeReadPool> & pool_,
        const size_t min_marks_to_read_,
        const UInt64 max_block_size_,
        size_t preferred_block_size_bytes_,
        size_t preferred_max_column_in_block_size_bytes_,
        const MergeTreeMetaBase & storage_,
        const StorageMetadataPtr & metadata_snapshot_,
        const bool use_uncompressed_cache_,
        const PrewhereInfoPtr & prewhere_info_,
        ExpressionActionsSettings actions_settings,
        const MergeTreeReaderSettings & reader_settings_,

        const Names & virt_column_names_);

    String getName() const override { return "MergeTreeThread"; }

    ~MergeTreeThreadSelectBlockInputProcessor() override;

protected:
    /// Requests read task from MergeTreeReadPool and signals whether it got one
    bool getNewTask() override;

private:
    /// "thread" index (there are N threads and each thread is assigned index in interval [0..N-1])
    size_t thread;

    std::shared_ptr<MergeTreeReadPool> pool;
    size_t min_marks_to_read;

    /// Last part read in this thread
    std::string last_readed_part_name;
    /// Names from header. Used in order to order columns in read blocks.
    Names ordered_names;
};

}
