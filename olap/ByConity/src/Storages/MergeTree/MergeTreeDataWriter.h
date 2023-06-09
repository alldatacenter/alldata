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

#include <Core/Block.h>

#include <IO/WriteBufferFromFile.h>
#include <Compression/CompressedWriteBuffer.h>

#include <Columns/ColumnsNumber.h>

#include <Interpreters/sortBlock.h>

#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include "common/types.h"


namespace DB
{

struct BucketInfo
{
    Int64 bucket_number {-1};
    UInt64 clustering_hash {0};
};

struct BlockWithPartition
{
    Block block;
    Row partition;
    BucketInfo bucket_info;

    BlockWithPartition(Block && block_, Row && partition_)
        : block(block_), partition(std::move(partition_))
    {
    }
};

using BlocksWithPartition = std::vector<BlockWithPartition>;

/** Writes new parts of data to the merge tree.
  */
class MergeTreeDataWriter
{
public:
    // use dest_policy_ to reserve space if specificed
    explicit MergeTreeDataWriter(MergeTreeMetaBase & data_, IStorage::StorageLocation location = IStorage::StorageLocation::MAIN):
        data(data_), write_location(location),
        log(&Poco::Logger::get(data.getLogName() + " (Writer)")) {}

    /** Split the block to blocks, each of them must be written as separate part.
      *  (split rows by partition)
      * Works deterministically: if same block was passed, function will return same result in same order.
      */
    static BlocksWithPartition splitBlockIntoParts(const Block & block, size_t max_parts, const StorageMetadataPtr & metadata_snapshot, ContextPtr context);
    static BlocksWithPartition splitBlockPartitionIntoPartsByClusterKey(const BlockWithPartition & block_with_partition, size_t max_parts, const StorageMetadataPtr & metadata_snapshot, ContextPtr context);
    static BlocksWithPartition populatePartitions(const Block & block, const Block & block_copy, const size_t max_parts, const Names expression_columns, bool is_bucket_scatter = false);

    /** All rows must correspond to same partition.
      * Returns part with unique name starting with 'tmp_', yet not added to MergeTreeData.
      */
    MergeTreeMetaBase::MutableDataPartPtr writeTempPart(BlockWithPartition & block, const StorageMetadataPtr & metadata_snapshot, bool optimize_on_insert);

    MergeTreeMetaBase::MutableDataPartPtr
    writeTempPart(BlockWithPartition & block, const StorageMetadataPtr & metadata_snapshot, ContextPtr context, UInt64 block_id = 0, Int64 mutation = 0, Int64 hint_mutation = 0);

    MergeTreeMetaBase::MutableDataPartPtr writeProjectionPart(
        Block block, const ProjectionDescription & projection, const IMergeTreeDataPart * parent_part);

    static MergeTreeMetaBase::MutableDataPartPtr writeTempProjectionPart(
        MergeTreeMetaBase & data,
        Poco::Logger * log,
        Block block,
        const ProjectionDescription & projection,
        const IMergeTreeDataPart * parent_part,
        size_t block_num,
        IStorage::StorageLocation location);

    Block mergeBlock(const Block & block, SortDescription sort_description, Names & partition_key_columns, IColumn::Permutation *& permutation);

private:
    static MergeTreeMetaBase::MutableDataPartPtr writeProjectionPartImpl(
        MergeTreeMetaBase & data,
        Poco::Logger * log,
        Block block,
        const StorageMetadataPtr & metadata_snapshot,
        MergeTreeMetaBase::MutableDataPartPtr && new_data_part);

    MergeTreeMetaBase & data;

    IStorage::StorageLocation write_location;

    Poco::Logger * log;
};

}
