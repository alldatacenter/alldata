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

#include <Core/NamesAndTypes.h>

#include <Interpreters/Context.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <Storages/MergeTree/MergeTreeDataMergerMutator.h>
#include <Common/ZooKeeper/ZooKeeper.h>
#include <common/logger_useful.h>


namespace DB
{

/**
 * future_part : parts to be ingested.
 * it contains new_part, source_part that requeired for ingestion.
 */
struct IngestPart
{
    FutureMergedMutatedPart future_part;
    ReservationPtr reserved_space;

    IngestPart(const FutureMergedMutatedPart & future_part_, ReservationPtr reserved_space_)
    : future_part(future_part_)
    , reserved_space(std::move(reserved_space_)) {}
};

using IngestPartPtr = std::shared_ptr<IngestPart>;
using IngestParts = std::vector<IngestPartPtr>;

class IngestPartition
{
public:

    struct IngestSource
    {
        IngestSource(const Block & block_) : block(block_) {}
        Block block;
        mutable std::mutex mutex;
    };

    using IngestSourcePtr = std::shared_ptr<IngestSource>;
    using IngestSources = std::vector<IngestSourcePtr>;

    IngestPartition(
        const StoragePtr & target_table_,
        const StoragePtr & source_table_,
        const ASTPtr & partition_,
        const Names & column_names_,
        const Names & key_names_,
        Int64 mutation_,
        const ContextPtr & context_);

    bool ingestPartition();

    static Names getOrderedKeys(const Names & key_names, const StorageInMemoryMetadata & data);
    static zkutil::EphemeralNodeHolderPtr getIngestTaskLock(const zkutil::ZooKeeperPtr & zk_ptr, const String & zookeeper_path, const String & replica_name, const String & partition_id);
    static String parseIngestPartition(const String & part_name, const MergeTreeDataFormatVersion & format_version);

private:

    ASTPtr getDefaultFilter(const String & column_name);
    String generateFilterString();
    String generateRemoteQuery(const String & source_database, const String & source_table, const String & partition_id, const Strings & column_lists);
    IngestParts generateIngestParts(MergeTreeData & data, const MergeTreeData::DataPartsVector & parts);
    IngestSources generateSourceBlocks(MergeTreeData & source_data,
                                                    const MergeTreeData::DataPartsVector & parts_to_read,
                                                    const Names & all_columns_with_partition_key);

    // ------ Utilities for Ingest partition -------
    std::optional<NameAndTypePair> tryGetMapColumn(const StorageInMemoryMetadata & data, const String & col_name);
    void writeNewPart(const StorageInMemoryMetadata & data, const IngestPartition::IngestSources & src_blocks, BlockOutputStreamPtr & output, Names & column_names);
    void checkIngestColumns(const StorageInMemoryMetadata & data, bool & has_map_implicite_key);
    void checkColumnStructure(const StorageInMemoryMetadata & target_data, const StorageInMemoryMetadata & src_data, const Names & column_names);
    int compare(Columns & target_key_cols, Columns & src_key_cols, size_t n, size_t m);
    void checkPartitionRows(MergeTreeData::DataPartsVector & parts, const Settings & settings, const String & table_type, bool check_compact_map);
    std::vector<Names> groupColumns(const Settings & settings);
    /**
     * Outer join the target_block with the src_blocks
     *
     * @param column_names The columns that going to be ingested in src_blocks
     * @param ordered_key_names The join keys
     */
    Block blockJoinBlocks(MergeTreeData & data,
                    Block & target_block,
                    const IngestPartition::IngestSources & src_blocks,
                    const Names & column_names,
                    const Names & ordered_key_names,
                    bool compact);

    /// Perform outer join, ingest the specified columns.
    MergeTreeData::MutableDataPartPtr ingestPart(MergeTreeData & data, const IngestPartPtr & ingest_part, const IngestPartition::IngestSources & src_blocks,
                const Names & ingest_column_names, const Names & ordered_key_names, const Names & all_columns,
                const Settings & settings);
    void ingestion(MergeTreeData & data, const IngestParts & parts_to_ingest, const IngestPartition::IngestSources & src_blocks,
                   const Names & ingest_column_names, const Names & ordered_key_names, const Names & all_columns, const Settings & settings);

    void ingestWidePart(MergeTreeData & data,
                const Block & header,
                MergeTreeData::MutableDataPartPtr & new_data_part,
                const MergeTreeData::DataPartPtr & target_part,
                const IngestPartition::IngestSources & src_blocks,
                const Names & ingest_column_names, const Names & ordered_key_names, const Names & all_columns,
                const Settings & settings,
                std::function<bool()> check_cached_cancel);

    void ingestCompactPart(
                MergeTreeData & data,
                const NamesAndTypesList & part_columns,
                MergeTreeData::MutableDataPartPtr & new_data_part,
                const MergeTreeData::DataPartPtr & target_part,
                const IngestPartition::IngestSources & src_blocks,
                const Names & ingest_column_names, const Names & ordered_key_names,
                const Settings & settings,
                std::function<bool()> check_cached_cancel
            );

    StoragePtr target_table;
    StoragePtr source_table;
    ASTPtr partition;
    Names column_names;
    Names key_names;
    Int64 mutation = 0;
    ContextPtr context;
    Poco::Logger * log;
};

}
