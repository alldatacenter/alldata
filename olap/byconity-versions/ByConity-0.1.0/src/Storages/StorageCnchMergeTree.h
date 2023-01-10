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

#include <optional>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <MergeTreeCommon/CnchStorageCommon.h>
#include <common/shared_ptr_helper.h>
#include "Catalog/DataModelPartWrapper_fwd.h"
#include <Storages/MergeTree/PartitionPruner.h>
#include <Storages/MergeTree/MergeTreeDataPartType.h>

namespace DB
{

struct PrepareContextResult;
class StorageCnchMergeTree final : public shared_ptr_helper<StorageCnchMergeTree>, public MergeTreeMetaBase, public CnchStorageCommonHelper
{
    friend struct shared_ptr_helper<StorageCnchMergeTree>;
public:
    ~StorageCnchMergeTree() override;

    std::string getName() const override { return "Cnch" + merging_params.getModeName() + "MergeTree";}

    bool supportsSampling() const override { return true; }
    bool supportsFinal() const override { return true; }
    bool supportsPrewhere() const override { return true; }
    bool supportsIndexForIn() const override { return true; }
    bool supportsMapImplicitColumn() const override { return true; }

    StoragePolicyPtr getStoragePolicy(StorageLocation location) const override;
    const String& getRelativeDataPath(StorageLocation location) const override;

    bool isRemote() const override { return true; }

    QueryProcessingStage::Enum
    getQueryProcessingStage(ContextPtr, QueryProcessingStage::Enum, const StorageMetadataPtr &, SelectQueryInfo &) const override;

    void startup() override;
    void shutdown() override;

    Pipe read(
        const Names & /*column_names*/,
        const StorageMetadataPtr & /*metadata_snapshot*/,
        SelectQueryInfo & /*query_info*/,
        ContextPtr /*local_context*/,
        QueryProcessingStage::Enum /*processed_stage*/,
        size_t /*max_block_size*/,
        unsigned /*num_streams*/) override;

    void read(
        QueryPlan & query_plan,
        const Names & /*column_names*/,
        const StorageMetadataPtr & /*metadata_snapshot*/,
        SelectQueryInfo & /*query_info*/,
        ContextPtr /*local_context*/,
        QueryProcessingStage::Enum /*processed_stage*/,
        size_t /*max_block_size*/,
        unsigned /*num_streams*/) override;

    PrepareContextResult prepareReadContext(
        const Names & column_names, const StorageMetadataPtr & metadata_snapshot, SelectQueryInfo & query_info, ContextPtr & local_context);

    BlockOutputStreamPtr write(const ASTPtr & query, const StorageMetadataPtr & metadata_snapshot, ContextPtr local_context) override;

    HostWithPortsVec getWriteWorkers(const ASTPtr & query, ContextPtr local_context) override;

    bool optimize(
        const ASTPtr & query,
        const StorageMetadataPtr & /*metadata_snapshot*/,
        const ASTPtr & partition,
        bool final,
        bool /*deduplicate*/,
        const Names & /* deduplicate_by_columns */,
        ContextPtr query_context) override;

    CheckResults checkData(const ASTPtr & query, ContextPtr local_context) override;

    time_t getTTLForPartition(const MergeTreePartition & partition) const;

    ServerDataPartsVector selectPartsToRead(
        const Names & column_names_to_return,
        ContextPtr local_context,
        const SelectQueryInfo & query_info);

    /// Return all base parts and delete bitmap metas in the given partitions.
    /// If `partitions` is empty, return meta for all partitions.
    MergeTreeDataPartsCNCHVector getUniqueTableMeta(TxnTimestamp ts, const Strings & partitions = {});

    /// return table's committed staged parts (excluding deleted ones).
    /// if partitions != null, ignore staged parts not belong to `partitions`.
    MergeTreeDataPartsCNCHVector getStagedParts(const TxnTimestamp & ts, const NameSet * partitions = nullptr, bool skip_delete_bitmap = false);

    /// Pre-condition: "parts" should have been sorted in part info order
    void getDeleteBitmapMetaForParts(const MergeTreeDataPartsCNCHVector & parts, ContextPtr context, TxnTimestamp start_time);
    /// For staged parts, delete bitmap represents delete_flag info which is optional, it's valid if it doesn't have delete_bitmap metadata.
    void getDeleteBitmapMetaForStagedParts(const MergeTreeDataPartsCNCHVector & parts, ContextPtr context, TxnTimestamp start_time);
    void getDeleteBitmapMetaForParts(const ServerDataPartsVector & parts, ContextPtr context, TxnTimestamp start_time);

    /// Used by the "SYSTEM DEDUP" command to repair unique table by removing duplicate keys in visible parts.
    void executeDedupForRepair(const ASTPtr & partition, ContextPtr context);

    /// Used by the "SYSTEM SYNC DEDUP WORKER" command to wait for all staged parts to publish
    void waitForStagedPartsToPublish(ContextPtr context);

    // Allocate parts to workers before we want to do some calculation on the parts, support non-select query.
    void allocateParts(ContextPtr local_context, ServerDataPartsVector & parts, WorkerGroupHandle & worker_group);

    UInt64 getTimeTravelRetention();

    void addCheckpoint(const Protos::Checkpoint & checkpoint);
    void removeCheckpoint(const Protos::Checkpoint & checkpoint);

    ColumnSizeByName getColumnSizes() const override
    {
        return {};
    }


    void checkAlterIsPossible(const AlterCommands & commands, ContextPtr local_context) const override;
    void alter(const AlterCommands & commands, ContextPtr local_context, TableLockHolder & table_lock_holder) override;
    void checkAlterSettings(const AlterCommands & commands) const;

    void checkAlterPartitionIsPossible(
        const PartitionCommands & commands, const StorageMetadataPtr & metadata_snapshot, const Settings & settings) const override;
    Pipe alterPartition(
        const StorageMetadataPtr & metadata_snapshot,
        const PartitionCommands & commands,
        ContextPtr query_context) override;

    void truncate(
        const ASTPtr & /*query*/,
        const StorageMetadataPtr & /* metadata_snapshot */,
        ContextPtr /* local_context */,
        TableExclusiveLockHolder &) override;

    ServerDataPartsVector selectPartsByPartitionCommand(ContextPtr local_context, const PartitionCommand & command);
    void dropPartitionOrPart(const PartitionCommand & command, ContextPtr local_context,
        IMergeTreeDataPartsVector* dropped_parts = nullptr);
    Block getBlockWithVirtualPartitionColumns(const std::vector<std::shared_ptr<MergeTreePartition>> & partition_list) const;

    struct PartitionDropInfo
    {
        Int64 max_block{0};
        size_t rows_count{0}; // rows count in drop range.
        size_t size{0}; // bytes size in drop range.
        size_t parts_count{0}; // covered parts in drop range.
        MergeTreePartition value;
    };
    using PartitionDropInfos = std::unordered_map<String, PartitionDropInfo>;
    MutableDataPartsVector createDropRangesFromPartitions(const PartitionDropInfos & partition_infos, const TransactionCnchPtr & txn);
    MutableDataPartsVector createDropRangesFromParts(const ServerDataPartsVector & parts_to_drop, const TransactionCnchPtr & txn);

    StorageCnchMergeTree * checkStructureAndGetCnchMergeTree(const StoragePtr & source_table) const;

    const String & getLocalStorePath() const;

    String genCreateTableQueryForWorker(const String & suffix);

protected:
    StorageCnchMergeTree(
        const StorageID & table_id_,
        const String & relative_data_path_,
        const StorageInMemoryMetadata & metadata_,
        bool attach_,
        ContextMutablePtr context_,
        const String & date_column_name_,
        const MergeTreeMetaBase::MergingParams & merging_params_,
        std::unique_ptr<MergeTreeSettings> settings_);

private:
    // Relative path to auxility storage disk root
    String relative_auxility_storage_path;

    CheckResults checkDataCommon(const ASTPtr & query, ContextPtr local_context, ServerDataPartsVector & parts);

    ServerDataPartsVector getAllParts(ContextPtr local_context);

    Strings selectPartitionsByPredicate(
        const SelectQueryInfo & query_info, std::vector<std::shared_ptr<MergeTreePartition>> & partition_list, const Names & column_names_to_return, ContextPtr local_context);

    void filterPartsByPartition(
        ServerDataPartsVector & parts,
        ContextPtr local_context,
        const SelectQueryInfo & query_info,
        const Names & column_names_to_return) const;

    void dropPartsImpl(ServerDataPartsVector& svr_parts_to_drop,
        IMergeTreeDataPartsVector& parts_to_drop, bool detach, ContextPtr local_context);

    void collectResource(ContextPtr local_context, ServerDataPartsVector & parts, const String & local_table_name, const std::set<Int64> & required_bucket_numbers = {});

    MutationCommands getFirstAlterMutationCommandsForPart(const DataPartPtr &) const override { return {}; }

    /// For select in interactive transaction session
    void filterPartsInExplicitTransaction(ServerDataPartsVector & data_parts, ContextPtr local_context);

    /// Generate view dependency create queries for materialized view writing
    Names genViewDependencyCreateQueries(const StorageID & storage_id, ContextPtr local_context, const String & table_suffix);
    String extractTableSuffix(const String & gen_table_name);
    std::set<Int64> getRequiredBucketNumbers(const SelectQueryInfo & query_info, ContextPtr context) const;

    void ingestPartition(const struct PartitionCommand & command, const ContextPtr local_context);

    /// Check if the ALTER can be performed:
    /// - all needed columns are present.
    /// - all type conversions can be done.
    /// - columns corresponding to primary key, indices, sign, sampling expression and date are not affected.
    /// If something is wrong, throws an exception.
    void checkAlterInCnchServer(const AlterCommands & commands, ContextPtr local_context) const;
};

struct PrepareContextResult
{
    String local_table_name;
    ServerDataPartsVector parts;
};

}
