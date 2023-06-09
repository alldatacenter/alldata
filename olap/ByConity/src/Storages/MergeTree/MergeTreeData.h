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

#include <MergeTreeCommon/MergeTreeMetaBase.h>

#include <Common/MultiVersion.h>
#include <Common/QueueForAsyncTask.h>
#include <Storages/IStorage.h>
#include <Storages/MergeTree/MergeTreeIndices.h>
#include <Storages/MergeTree/MergeTreePartInfo.h>
#include <Storages/MergeTree/MergeTreeSettings.h>
#include <Storages/MergeTree/MergeTreeMutationStatus.h>
#include <Storages/MergeTree/MergeList.h>
#include <Storages/DataDestinationType.h>
#include <IO/ReadBufferFromString.h>
#include <IO/WriteBufferFromFile.h>
#include <IO/ReadBufferFromFile.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Storages/MergeTree/MergeTreeDataPartInMemory.h>
#include <Storages/IndicesDescription.h>
#include <Storages/MergeTree/MergeTreePartsMover.h>
#include <Storages/MergeTree/MergeTreeWriteAheadLog.h>
#include <Interpreters/PartLog.h>
#include <Interpreters/Aggregator.h>
#include <Storages/PartitionCommands.h>

#include <boost/multi_index_container.hpp>
#include <boost/multi_index/ordered_index.hpp>
#include <boost/multi_index/global_fun.hpp>
#include <boost/range/iterator_range_core.hpp>

namespace DB
{

class AlterCommands;
class MergeTreePartsMover;
class Context;

struct JobAndPool;
class DiskUniqueKeyIndexCache;
class DiskUniqueRowStoreCache;

/// Auxiliary struct holding information about the future merged or mutated part.
struct EmergingPartInfo
{
    String disk_name;
    String partition_id;
    size_t estimate_bytes;
};

struct CurrentlySubmergingEmergingTagger;

struct SelectQueryOptions;
class ExpressionActions;
using ExpressionActionsPtr = std::shared_ptr<ExpressionActions>;
using ManyExpressionActions = std::vector<ExpressionActionsPtr>;
class MergeTreeDeduplicationLog;
class IBackgroundJobExecutor;

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int NOT_IMPLEMENTED;
}

/// Data structure for *MergeTree engines.
/// Merge tree is used for incremental sorting of data.
/// The table consists of several sorted parts.
/// During insertion new data is sorted according to the primary key and is written to the new part.
/// Parts are merged in the background according to a heuristic algorithm.
/// For each part the index file is created containing primary key values for every n-th row.
/// This allows efficient selection by primary key range predicate.
///
/// Additionally:
///
/// The date column is specified. For each part min and max dates are remembered.
/// Essentially it is an index too.
///
/// Data is partitioned by the value of the partitioning expression.
/// Parts belonging to different partitions are not merged - for the ease of administration (data sync and backup).
///
/// File structure of old-style month-partitioned tables (format_version = 0):
/// Part directory - / min-date _ max-date _ min-id _ max-id _ level /
/// Inside the part directory:
/// checksums.txt - contains the list of all files along with their sizes and checksums.
/// columns.txt - contains the list of all columns and their types.
/// primary.idx - contains the primary index.
/// [Column].bin - contains compressed column data.
/// [Column].mrk - marks, pointing to seek positions allowing to skip n * k rows.
///
/// File structure of tables with custom partitioning (format_version >= 1):
/// Part directory - / partition-id _ min-id _ max-id _ level /
/// Inside the part directory:
/// The same files as for month-partitioned tables, plus
/// count.txt - contains total number of rows in this part.
/// partition.dat - contains the value of the partitioning expression.
/// minmax_[Column].idx - MinMax indexes (see IMergeTreeDataPart::MinMaxIndex class) for the columns required by the partitioning expression.
///
/// Several modes are implemented. Modes determine additional actions during merge:
/// - Ordinary - don't do anything special
/// - Collapsing - collapse pairs of rows with the opposite values of sign_columns for the same values
///   of primary key (cf. CollapsingSortedBlockInputStream.h)
/// - Replacing - for all rows with the same primary key keep only the latest one. Or, if the version
///   column is set, keep the latest row with the maximal version.
/// - Summing - sum all numeric columns not contained in the primary key for all rows with the same primary key.
/// - Aggregating - merge columns containing aggregate function states for all rows with the same primary key.
/// - Graphite - performs coarsening of historical data for Graphite (a system for quantitative monitoring).

/// The MergeTreeData class contains a list of parts and the data structure parameters.
/// To read and modify the data use other classes:
/// - MergeTreeDataSelectExecutor
/// - MergeTreeDataWriter
/// - MergeTreeDataMergerMutator

class MergeTreeData : public MergeTreeMetaBase
{
public:
    using DataParts = std::set<DataPartPtr, LessDataPart>;
    using DataPartsVector = std::vector<DataPartPtr>;

    bool supportsMapImplicitColumn() const override { return true; }

    /// Auxiliary object to add a set of parts into the working set in two steps:
    /// * First, as PreCommitted parts (the parts are ready, but not yet in the active set).
    /// * Next, if commit() is called, the parts are added to the active set and the parts that are
    ///   covered by them are marked Outdated.
    /// If neither commit() nor rollback() was called, the destructor rollbacks the operation.
    class Transaction : private boost::noncopyable
    {
    public:
        Transaction(MergeTreeData & data_) : data(data_) {}

        DataPartsVector commit(MergeTreeData::DataPartsLock * acquired_parts_lock = nullptr);

        void rollback();

        /// Immediately remove parts from table's data_parts set and change part
        /// state to temporary. Useful for new parts which not present in table.
        void rollbackPartsToTemporaryState();

        size_t size() const { return precommitted_parts.size(); }
        bool isEmpty() const { return precommitted_parts.empty(); }

        ~Transaction()
        {
            try
            {
                rollback();
            }
            catch (...)
            {
                tryLogCurrentException("~MergeTreeData::Transaction");
            }
        }

    private:
        friend class MergeTreeData;

        MergeTreeData & data;
        DataParts precommitted_parts;

        void clear() { precommitted_parts.clear(); }
    };

    using PathWithDisk = std::pair<String, DiskPtr>;

    struct PartsTemporaryRename : private boost::noncopyable
    {
        PartsTemporaryRename(
            const MergeTreeData & storage_,
            const String & source_dir_)
            : storage(storage_)
            , source_dir(source_dir_)
        {
        }

        void addPart(const String & old_name, const String & new_name);

        /// Renames part from old_name to new_name
        void tryRenameAll();

        /// Renames all added parts from new_name to old_name if old name is not empty
        ~PartsTemporaryRename();

        const MergeTreeData & storage;
        const String source_dir;
        std::vector<std::pair<String, String>> old_and_new_names;
        std::unordered_map<String, PathWithDisk> old_part_name_to_path_and_disk;
        bool renamed = false;
    };

    /// Attach the table corresponding to the directory in full_path inside policy (must end with /), with the given columns.
    /// Correctness of names and paths is not checked.
    ///
    /// date_column_name - if not empty, the name of the Date column used for partitioning by month.
    ///     Otherwise, partition_by_ast is used for partitioning.
    ///
    /// order_by_ast - a single expression or a tuple. It is used as a sorting key
    ///     (an ASTExpressionList used for sorting data in parts);
    /// primary_key_ast - can be nullptr, an expression, or a tuple.
    ///     Used to determine an ASTExpressionList values of which are written in the primary.idx file
    ///     for one row in every `index_granularity` rows to speed up range queries.
    ///     Primary key must be a prefix of the sorting key;
    ///     If it is nullptr, then it will be determined from order_by_ast.
    ///
    /// require_part_metadata - should checksums.txt and columns.txt exist in the part directory.
    /// attach - whether the existing table is attached or the new table is created.
    MergeTreeData(const StorageID & table_id_,
                  const String & relative_data_path_,
                  const StorageInMemoryMetadata & metadata_,
                  ContextMutablePtr context_,
                  const String & date_column_name,
                  const MergingParams & merging_params_,
                  std::unique_ptr<MergeTreeSettings> settings_,
                  bool require_part_metadata_,
                  bool attach,
                  BrokenPartCallback broken_part_callback_ = [](const String &){});

    ~MergeTreeData() override;

    bool getQueryProcessingStageWithAggregateProjection(
        ContextPtr query_context, const StorageMetadataPtr & metadata_snapshot, SelectQueryInfo & query_info) const;

    QueryProcessingStage::Enum getQueryProcessingStage(
        ContextPtr query_context,
        QueryProcessingStage::Enum to_stage,
        const StorageMetadataPtr & metadata_snapshot,
        SelectQueryInfo & info) const override;

    static bool partsContainSameProjections(const DataPartPtr & left, const DataPartPtr & right);

    bool mayBenefitFromIndexForIn(const ASTPtr & left_in_operand, ContextPtr, const StorageMetadataPtr & metadata_snapshot) const override;

    /// Load the set of data parts from disk. Call once - immediately after the object is created.
    void loadDataParts(bool skip_sanity_checks);

    /** ----------------------- COMPATIBLE CODE BEGIN-------------------------- */
    /*  compatible with old metastore. remove this later  */

    /// load data parts from old version metastore and copy the metadata into new metastore.
    bool preLoadDataParts(bool skip_sanity_checks);
    /*  -----------------------  COMPATIBLE CODE END -------------------------- */

    /// load parts from file system;
    void loadPartsFromFileSystem(PartNamesWithDisks part_names_with_disks, PartNamesWithDisks wal_with_disks, bool skip_sanity_checks, DataPartsLock &);

    Int64 getMaxBlockNumber() const;

    /// Returns absolutely all parts (and snapshot of their states)
    DataPartsVector getAllDataPartsVector(DataPartStateVector * out_states = nullptr, bool require_projection_parts = false) const;

    /// Returns all detached parts
    DetachedPartsInfo getDetachedParts() const;

    void validateDetachedPartName(const String & name) const;

    void dropDetached(const ASTPtr & partition, bool part, ContextPtr context);

    MutableDataPartsVector tryLoadPartsToAttach(const ASTPtr & partition, bool attach_part,
            ContextPtr context, PartsTemporaryRename & renamed_parts, bool in_preattach = false);

    using PartNamesWithDisks = std::vector<std::pair<String, DiskPtr>>;

    MutableDataPartsVector tryLoadPartsInPathToAttach(const PartNamesWithDisks & parts_with_disk, const String & relative_path);

    virtual void attachPartsInDirectory(const PartNamesWithDisks & /*local_parts*/, const String & /*relative path*/, ContextPtr /*local_context*/)
    {
        throw Exception("Not implemented.", ErrorCodes::NOT_IMPLEMENTED);
    }

    /// Returns a committed part with the given name or a part containing it. If there is no such part, returns nullptr.
    DataPartPtr getActiveContainingPart(const String & part_name) const;
    DataPartPtr getActiveContainingPart(const MergeTreePartInfo & part_info) const;
    DataPartPtr getActiveContainingPart(const MergeTreePartInfo & part_info, DataPartState state, DataPartsLock & lock) const;

    /// Swap part with it's identical copy (possible with another path on another disk).
    /// If original part is not active or doesn't exist exception will be thrown.
    void swapActivePart(MergeTreeData::DataPartPtr part_copy);

    /// For a target part that will be fetched from another replica, find whether the local has an old version part.
    /// When mutating a part, its mutate version will be changed. For example, all_0_0_0 -> all_0_0_0_1, all_0_0_0_1 is the target part, all_0_0_0 is the old version part.
    /// Due to mutation commands may modify only few files in the old part, so a lot of files are not necessary to transfer.
    /// In this case, if the local has an old version part, transfer its checksum to the replica, then the replica will give the information.
    DataPartPtr getOldVersionPartIfExists(const String & part_name);

    DataPartsVector getPartsByPredicate(const ASTPtr & predicate);

    /// Split CLEAR COLUMN IN PARTITION WHERE command into multiple CLEAR COLUMN IN PARTITION commands.
    void handleClearColumnInPartitionWhere(MutationCommands & mutation_commands, const AlterCommands & alter_commands);

    /// Total size of active parts in bytes.
    size_t getTotalActiveSizeInBytes() const override;

    size_t getTotalActiveSizeInRows() const;

    size_t getPartsCount() const;
    size_t getMaxPartsCountForPartitionWithState(DataPartState state) const;
    size_t getMaxPartsCountForPartition() const;
    size_t getMaxInactivePartsCountForPartition() const;

    /// Get min value of part->info.getDataVersion() for all active parts.
    /// Makes sense only for ordinary MergeTree engines because for them block numbering doesn't depend on partition.
    std::optional<Int64> getMinPartDataVersion() const;

    /// If the table contains too many active parts, sleep for a while to give them time to merge.
    /// If until is non-null, wake up from the sleep earlier if the event happened.
    void delayInsertOrThrowIfNeeded(Poco::Event * until = nullptr) const;

    /// Renames temporary part to a permanent part and adds it to the parts set.
    /// It is assumed that the part does not intersect with existing parts.
    /// If increment != nullptr, part index is determining using increment. Otherwise part index remains unchanged.
    /// If out_transaction != nullptr, adds the part in the PreCommitted state (the part will be added to the
    /// active set later with out_transaction->commit()).
    /// Else, commits the part immediately.
    /// Returns true if part was added. Returns false if part is covered by bigger part.
    bool renameTempPartAndAdd(MutableDataPartPtr & part, SimpleIncrement * increment = nullptr, Transaction * out_transaction = nullptr, MergeTreeDeduplicationLog * deduplication_log = nullptr);

    /// The same as renameTempPartAndAdd but the block range of the part can contain existing parts.
    /// Returns all parts covered by the added part (in ascending order).
    /// If out_transaction == nullptr, marks covered parts as Outdated.
    DataPartsVector renameTempPartAndReplace(
        MutableDataPartPtr & part, SimpleIncrement * increment = nullptr, Transaction * out_transaction = nullptr, MergeTreeDeduplicationLog * deduplication_log = nullptr);

    /// Low-level version of previous one, doesn't lock mutex
    bool renameTempPartAndReplace(
            MutableDataPartPtr & part, SimpleIncrement * increment, Transaction * out_transaction, DataPartsLock & lock,
            DataPartsVector * out_covered_parts = nullptr, MergeTreeDeduplicationLog * deduplication_log = nullptr);

    bool renameTempPartInDetachDirecotry(MutableDataPartPtr & new_part, const DataPartPtr & old_part);

    /// Remove parts from working set immediately (without wait for background
    /// process). Transfer part state to temporary. Have very limited usage only
    /// for new parts which aren't already present in table.
    void removePartsFromWorkingSetImmediatelyAndSetTemporaryState(const DataPartsVector & remove);

    /// Removes parts from the working set parts.
    /// Parts in add must already be in data_parts with PreCommitted, Committed, or Outdated states.
    /// If clear_without_timeout is true, the parts will be deleted at once, or during the next call to
    /// clearOldParts (ignoring old_parts_lifetime).
    void removePartsFromWorkingSet(const DataPartsVector & remove, bool clear_without_timeout, DataPartsLock * acquired_lock = nullptr);
    void removePartsFromWorkingSet(const DataPartsVector & remove, bool clear_without_timeout, DataPartsLock & acquired_lock);

    /// Removes all parts from the working set parts
    ///  for which (partition_id = drop_range.partition_id && min_block >= drop_range.min_block && max_block <= drop_range.max_block).
    /// Used in REPLACE PARTITION command;
    DataPartsVector removePartsInRangeFromWorkingSet(const MergeTreePartInfo & drop_range, bool clear_without_timeout,
                                                     DataPartsLock & lock);

    /// Renames the part to detached/<prefix>_<part> and removes it from data_parts,
    //// so it will not be deleted in clearOldParts.
    /// If restore_covered is true, adds to the working set inactive parts, which were merged into the deleted part.
    void forgetPartAndMoveToDetached(const DataPartPtr & part, const String & prefix = "", bool restore_covered = false);

    /// If the part is Obsolete and not used by anybody else, immediately delete it from filesystem and remove from memory.
    void tryRemovePartImmediately(DataPartPtr && part);

    /// Returns old inactive parts that can be deleted. At the same time removes them from the list of parts but not from the disk.
    /// If 'force' - don't wait for old_parts_lifetime.
    DataPartsVector grabOldParts(bool force = false);

    /// Reverts the changes made by grabOldParts(), parts should be in Deleting state.
    void rollbackDeletingParts(const DataPartsVector & parts);

    /// Removes parts from data_parts, they should be in Deleting state
    void removePartsFinally(const DataPartsVector & parts);

    /// Remove parts from data_parts with checking its state, this routine is
    /// used in light weight detach.
    void removePartsFinallyUnsafe(const DataPartsVector & parts, DataPartsLock * acquired_lock = nullptr);

    /// Delete irrelevant parts from memory and disk.
    /// If 'force' - don't wait for old_parts_lifetime.
    void clearOldPartsFromFilesystem(bool force = false);
    void clearPartsFromFilesystem(const DataPartsVector & parts);

    /// Delete WAL files containing parts, that all already stored on disk.
    void clearOldWriteAheadLogs();

    /// Delete all directories which names begin with "tmp"
    /// Set non-negative parameter value to override MergeTreeSettings temporary_directories_lifetime
    /// Must be called with locked lockForShare() because use relative_data_path.
    void clearOldTemporaryDirectories(ssize_t custom_directories_lifetime_seconds = -1);

    void clearEmptyParts();

    /// After the call to dropAllData() no method can be called.
    /// Deletes the data directory and flushes the uncompressed blocks cache and the marks cache.
    void dropAllData();

    /// Drop data directories if they are empty. It is safe to call this method if table creation was unsuccessful.
    void dropIfEmpty();

    /// Moves the entire data directory. Flushes the uncompressed blocks cache
    /// and the marks cache. Must be called with locked lockExclusively()
    /// because changes relative_data_path.
    void rename(const String & new_table_path, const StorageID & new_table_id) override;

    /// Check if the ALTER can be performed:
    /// - all needed columns are present.
    /// - all type conversions can be done.
    /// - columns corresponding to primary key, indices, sign, sampling expression and date are not affected.
    /// If something is wrong, throws an exception.
    void checkAlterIsPossible(const AlterCommands & commands, ContextPtr context) const override;

    virtual bool supportsClearColumnInPartitionWhere() const { return false; }

    /// Checks if the Mutation can be performed.
    /// (currently no additional checks: always ok)
    void checkMutationIsPossible(const MutationCommands & commands, const Settings & settings) const override;

    /// Checks that partition name in all commands is valid
    void checkAlterPartitionIsPossible(const PartitionCommands & commands, const StorageMetadataPtr & metadata_snapshot, const Settings & settings) const override;

    /// Change MergeTreeSettings
    void changeSettings(
        const ASTPtr & new_settings,
        TableLockHolder & table_lock_holder);

    /** Create local backup (snapshot) for parts with specified prefix.
      * Backup is created in directory clickhouse_dir/shadow/i/, where i - incremental number,
      *  or if 'with_name' is specified - backup is created in directory with specified name.
      */
    PartitionCommandsResultInfo freezePartition(
        const ASTPtr & partition,
        const StorageMetadataPtr & metadata_snapshot,
        const String & with_name,
        ContextPtr context,
        TableLockHolder & table_lock_holder);

    /// Freezes all parts.
    PartitionCommandsResultInfo freezeAll(
        const String & with_name,
        const StorageMetadataPtr & metadata_snapshot,
        ContextPtr context,
        TableLockHolder & table_lock_holder);

    /// Unfreezes particular partition.
    PartitionCommandsResultInfo unfreezePartition(
        const ASTPtr & partition,
        const String & backup_name,
        ContextPtr context,
        TableLockHolder & table_lock_holder);

    /// Unfreezes all parts.
    PartitionCommandsResultInfo unfreezeAll(
        const String & backup_name,
        ContextPtr context,
        TableLockHolder & table_lock_holder);

    /// Moves partition to specified Disk
    void movePartitionToDisk(const ASTPtr & partition, const String & name, bool moving_part, ContextPtr context);

    /// Moves partition to specified Volume
    void movePartitionToVolume(const ASTPtr & partition, const String & name, bool moving_part, ContextPtr context);

    void checkPartitionCanBeDropped(const ASTPtr & partition) override;

    void checkPartCanBeDropped(const String & part_name);

    Pipe alterPartition(
        const StorageMetadataPtr & metadata_snapshot,
        const PartitionCommands & commands,
        ContextPtr query_context) override;

    /// Extracts MergeTreeData of other *MergeTree* storage
    ///  and checks that their structure suitable for ALTER TABLE ATTACH PARTITION FROM
    /// Tables structure should be locked.
    MergeTreeData & checkStructureAndGetMergeTreeData(const StoragePtr & source_table, const StorageMetadataPtr & src_snapshot, const StorageMetadataPtr & my_snapshot) const;
    MergeTreeData & checkStructureAndGetMergeTreeData(IStorage & source_table, const StorageMetadataPtr & src_snapshot, const StorageMetadataPtr & my_snapshot) const;

    virtual std::vector<MergeTreeMutationStatus> getMutationsStatus() const = 0;

    /// expose the metastore for metadata management. may return nullptr if enable_metastore is not set.
    MetaStorePtr getMetastore() const {return metastore; }

    /// get metastore path
    String getMetastorePath() const;

    /// Get disk where part is located.
    /// `additional_path` can be set if part is not located directly in table data path (e.g. 'detached/')
    DiskPtr getDiskForPart(const String & part_name, const String & additional_path = "") const;

    /// Get full path for part. Uses getDiskForPart and returns the full relative path.
    /// `additional_path` can be set if part is not located directly in table data path (e.g. 'detached/')
    std::optional<String> getFullRelativePathForPart(const String & part_name, const String & additional_path = "") const;

    bool storesDataOnDisk() const override { return true; }
    Strings getDataPaths() const override;

    using PathsWithDisks = std::vector<PathWithDisk>;
    PathsWithDisks getRelativeDataPathsWithDisks() const;

    /// Reserves space for the part based on the distribution of "big parts" in the same partition.
    /// Parts with estimated size larger than `min_bytes_to_rebalance_partition_over_jbod` are
    /// considered as big. The priority is lower than TTL. If reservation fails, return nullptr.
    ReservationPtr balancedReservation(
        const StorageMetadataPtr & metadata_snapshot,
        size_t part_size,
        size_t max_volume_index,
        const String & part_name,
        const MergeTreePartInfo & part_info,
        MergeTreeData::DataPartsVector covered_parts,
        std::optional<CurrentlySubmergingEmergingTagger> * tagger_ptr,
        const IMergeTreeDataPart::TTLInfos * ttl_infos,
        bool is_insert = false);

    /// Choose disk with max available free space
    /// Reserves 0 bytes
    ReservationPtr makeEmptyReservationOnLargestDisk() const { return getStoragePolicy(IStorage::StorageLocation::MAIN)->makeEmptyReservationOnLargestDisk(); }

    Disks getDisksByType(DiskType::Type type) const { return getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisksByType(type); }

    /// Get count of total merges with TTL in MergeList (system.merges) for all
    /// tables (not only current table).
    /// Method is cheap and doesn't require any locks.
    size_t getTotalMergesWithTTLInMergeList() const;

    using WriteAheadLogPtr = std::shared_ptr<MergeTreeWriteAheadLog>;
    WriteAheadLogPtr getWriteAheadLog();

    /// Limiting parallel sends per one table, used in DataPartsExchange
    std::atomic_uint current_table_sends {0};

    /// Parts that currently moving from disk/volume to another.
    /// This set have to be used with `currently_processing_in_background_mutex`.
    /// Moving may conflict with merges and mutations, but this is OK, because
    /// if we decide to move some part to another disk, than we
    /// assuredly will choose this disk for containing part, which will appear
    /// as result of merge or mutation.
    DataParts currently_moving_parts;

    /// Mutex for currently_moving_parts
    mutable std::mutex moving_parts_mutex;

    /// Schedules background job to like merge/mutate/fetch an executor
    virtual bool scheduleDataProcessingJob(IBackgroundJobExecutor & executor) = 0;
    /// Schedules job to move parts between disks/volumes and so on.
    bool scheduleDataMovingJob(IBackgroundJobExecutor & executor);
    bool areBackgroundMovesNeeded() const;

    /// used for sync metadata manually by `system sync meatadata db.table`
    void syncMetaData();

    void trySyncMetaData();

    /// add wal information to metastore when new wal file is created.
    void addWriteAheadLog(const String & file_name, const DiskPtr & disk) const;

    /// remove wal information from metastore if it is deleted from filesystem.
    void removeWriteAheadLog(const String & file_name) const;

    /// Fetch part only if some replica has it on shared storage like S3
    /// Overridden in StorageReplicatedMergeTree
    virtual bool tryToFetchIfShared(const IMergeTreeDataPart &, const DiskPtr &, const String &) { return false; }

    /// Parts that currently submerging (merging to bigger parts) or emerging
    /// (to be appeared after merging finished). These two variables have to be used
    /// with `currently_submerging_emerging_mutex`.
    DataParts currently_submerging_big_parts;
    std::map<String, EmergingPartInfo> currently_emerging_big_parts;
    /// Mutex for currently_submerging_parts and currently_emerging_parts
    mutable std::mutex currently_submerging_emerging_mutex;

    /// Get throttler for replicated fetches
    ThrottlerPtr getFetchesThrottler() const
    {
        return replicated_fetches_throttler;
    }

    /// Get throttler for replicated sends
    ThrottlerPtr getSendsThrottler() const
    {
        return replicated_sends_throttler;
    }

    /// Get required partition vector with query info
    DataPartsVector getRequiredPartitions(const SelectQueryInfo & query_info, ContextPtr context);

    void checkColumnsValidity(const ColumnsDescription & columns) const override;

protected:

    friend class IMergeTreeDataPart;
    friend class MergeTreeDataMergerMutator;
    friend struct ReplicatedMergeTreeTableMetadata;
    friend class StorageReplicatedMergeTree;
    friend class MergeTreeDataWriter;

    MergeTreePartsMover parts_mover;

    std::optional<UInt64> totalRowsByPartitionPredicateImpl(
        const SelectQueryInfo & query_info, ContextPtr context, const DataPartsVector & parts) const;

    /// Used to serialize calls to grabOldParts.
    std::mutex grab_old_parts_mutex;
    /// The same for clearOldTemporaryDirectories.
    std::mutex clear_old_temporary_directories_mutex;

    void checkStoragePolicy(const StoragePolicyPtr & new_storage_policy) const;

    /// Return parts in the Committed set that are covered by the new_part_info or the part that covers it.
    /// Will check that the new part doesn't already exist and that it doesn't intersect existing part.
    DataPartsVector getActivePartsToReplace(
        const MergeTreePartInfo & new_part_info,
        const String & new_part_name,
        DataPartPtr & out_covering_part,
        DataPartsLock & data_parts_lock) const;

    void renamePartAndDropMetadata(const String& name, const DataPartPtr& sourcePart);
    void renamePartAndInsertMetadata(const String& name, const DataPartPtr& sourcePart);

    /// Checks whether the column is in the primary key, possibly wrapped in a chain of functions with single argument.
    bool isPrimaryOrMinMaxKeyColumnPossiblyWrappedInFunctions(const ASTPtr & node, const StorageMetadataPtr & metadata_snapshot) const;

    /// Common part for |freezePartition()| and |freezeAll()|.
    using MatcherFn = std::function<bool(const String &)>;
    PartitionCommandsResultInfo freezePartitionsByMatcher(MatcherFn matcher, const StorageMetadataPtr & metadata_snapshot, const String & with_name, ContextPtr context);
    PartitionCommandsResultInfo unfreezePartitionsByMatcher(MatcherFn matcher, const String & backup_name, ContextPtr context);

    // Partition helpers
    bool canReplacePartition(const DataPartPtr & src_part) const;

    /// Tries to drop part in background without any waits or throwing exceptions in case of errors.
    virtual void dropPartNoWaitNoThrow(const String & part_name) = 0;

    virtual void dropPart(const String & part_name, bool detach, ContextPtr context) = 0;
    virtual void dropPartition(const ASTPtr & partition, bool detach, ContextPtr context) = 0;
    virtual void dropPartitionWhere(const ASTPtr & predicate, bool detach, ContextPtr context);
    virtual PartitionCommandsResultInfo attachPartition(const ASTPtr & partition, const StorageMetadataPtr & metadata_snapshot, bool part, ContextPtr context) = 0;
    virtual void replacePartitionFrom(const StoragePtr & source_table, const ASTPtr & partition, bool replace, ContextPtr context) = 0;
    virtual void movePartitionToTable(const StoragePtr & dest_table, const ASTPtr & partition, ContextPtr context) = 0;

    virtual void fetchPartition(
        const ASTPtr & partition,
        const StorageMetadataPtr & metadata_snapshot,
        const String & from,
        bool fetch_part,
        ContextPtr query_context);
    virtual void fetchPartitionWhere(
        const ASTPtr & predicate, const StorageMetadataPtr & metadata_snapshot, const String & from, ContextPtr query_context);

    virtual void repairPartition(const ASTPtr & partition, bool part, const String & from, ContextPtr);

    virtual void movePartitionToShard(const ASTPtr & partition, bool move_part, const String & to, ContextPtr query_context);

    virtual void movePartitionFrom(const StoragePtr & source_table, const ASTPtr & partition, ContextPtr context);

    virtual void ingestPartition(const PartitionCommand & /*command*/, ContextPtr /*context*/) { throw Exception("IngestPartition not implement", ErrorCodes::NOT_IMPLEMENTED); }

    virtual void samplePartitionWhere(
                            const String /*dst_database*/,
                            const String /*dst_table*/,
                            const ASTPtr & /*sharding_expression*/,
                            const ASTPtr & /*predicate*/,
                            const ContextPtr & /*query_context*/) { throw Exception("Sample Partition not implement", ErrorCodes::NOT_IMPLEMENTED); }

    void writePartLog(
        PartLogElement::Type type,
        const ExecutionStatus & execution_status,
        UInt64 elapsed_ns,
        const String & new_part_name,
        const DataPartPtr & result_part,
        const DataPartsVector & source_parts,
        const MergeListEntry * merge_entry);

    /// If part is assigned to merge or mutation (possibly replicated)
    /// Should be overridden by children, because they can have different
    /// mechanisms for parts locking
    virtual bool partIsAssignedToBackgroundOperation(const DataPartPtr & part) const = 0;

    /// Moves part to specified space, used in ALTER ... MOVE ... queries
    bool movePartsToSpace(const DataPartsVector & parts, SpacePtr space);

    /// Throttlers used in DataPartsExchange to lower maximum fetch/sends
    /// speed.
    ThrottlerPtr replicated_fetches_throttler;
    ThrottlerPtr replicated_sends_throttler;

    MetaStorePtr metastore;

private:
    /// RAII Wrapper for atomic work with currently moving parts
    /// Acquire them in constructor and remove them in destructor
    /// Uses data.currently_moving_parts_mutex
    struct CurrentlyMovingPartsTagger
    {
        MergeTreeMovingParts parts_to_move;
        MergeTreeData & data;
        CurrentlyMovingPartsTagger(MergeTreeMovingParts && moving_parts_, MergeTreeData & data_);

        ~CurrentlyMovingPartsTagger();
    };

    using CurrentlyMovingPartsTaggerPtr = std::shared_ptr<CurrentlyMovingPartsTagger>;

    /// Move selected parts to corresponding disks
    bool moveParts(const CurrentlyMovingPartsTaggerPtr & moving_tagger);

    void syncMetaImpl(DataPartsLock &);

    /// Select parts for move and disks for them. Used in background moving processes.
    CurrentlyMovingPartsTaggerPtr selectPartsForMove();

    /// Check selected parts for movements. Used by ALTER ... MOVE queries.
    CurrentlyMovingPartsTaggerPtr checkPartsForMove(const DataPartsVector & parts, SpacePtr space);

    /// collect all existing parts as well as wals from filesystem.
    void searchAllPartsOnFilesystem(std::map<String, DiskPtr> & parts_with_disks, std::map<String, DiskPtr> & wal_with_disks) const;

    std::mutex write_ahead_log_mutex;
    WriteAheadLogPtr write_ahead_log;

    virtual void startBackgroundMovesIfNeeded() = 0;

    bool enable_metastore{};

    void addPartContributionToDataVolume(const DataPartPtr & part);
    void removePartContributionToDataVolume(const DataPartPtr & part);

    void increaseDataVolume(size_t bytes, size_t rows, size_t parts);
    void decreaseDataVolume(size_t bytes, size_t rows, size_t parts);

    void setDataVolume(size_t bytes, size_t rows, size_t parts);

    std::atomic<size_t> total_active_size_bytes = 0;
    std::atomic<size_t> total_active_size_rows = 0;
    std::atomic<size_t> total_active_size_parts = 0;

    // Get partition matcher for FREEZE / UNFREEZE queries.
    MatcherFn getPartitionMatcher(const ASTPtr & partition, ContextPtr context) const;

    /// Returns default settings for storage with possible changes from global config.
    virtual std::unique_ptr<MergeTreeSettings> getDefaultSettings() const = 0;
};

/// RAII struct to record big parts that are submerging or emerging.
/// It's used to calculate the balanced statistics of JBOD array.
struct CurrentlySubmergingEmergingTagger
{
    MergeTreeData & storage;
    String emerging_part_name;
    MergeTreeData::DataPartsVector submerging_parts;
    Poco::Logger * log;

    CurrentlySubmergingEmergingTagger(
        MergeTreeData & storage_, const String & name_, MergeTreeData::DataPartsVector && parts_, Poco::Logger * log_)
        : storage(storage_), emerging_part_name(name_), submerging_parts(std::move(parts_)), log(log_)
    {
    }

    ~CurrentlySubmergingEmergingTagger();
};

}
