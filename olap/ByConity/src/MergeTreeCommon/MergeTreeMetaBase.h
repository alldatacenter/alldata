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

#include <Interpreters/Context.h>
#include <Storages/IStorage.h>
#include <Storages/MergeTree/MergeTreeDataFormatVersion.h>
#include <Storages/extractKeyExpressionList.h>
#include <Storages/MergeTree/PinnedPartUUIDs.h>
#include <Storages/MergeTree/MergeTreeMeta.h>
#include <Storages/PrimaryIndexCache.h>
#include <MergeTreeCommon/IMergeTreePartMeta.h>
#include <Processors/Merges/Algorithms/Graphite.h>
#include <Common/SimpleIncrement.h>
#include <Disks/StoragePolicy.h>

namespace DB
{

class MutationCommands;

class MergeTreeMetaBase : public IStorage, public WithMutableContext, public MergeTreeDataPartTypeHelper
{
public:
    constexpr static auto FORMAT_VERSION_FILE_NAME = "format_version.txt";
    constexpr static auto DETACHED_DIR_NAME = "detached";

    /// Function to call if the part is suspected to contain corrupt data.
    using BrokenPartCallback = std::function<void (const String &)>;

    using PinnedPartUUIDsPtr = std::shared_ptr<const PinnedPartUUIDs>;

    using MetaStorePtr = std::shared_ptr<MergeTreeMeta>;

    /// Alter conversions which should be applied on-fly for part. Build from of
    /// the most recent mutation commands for part. Now we have only rename_map
    /// here (from ALTER_RENAME) command, because for all other type of alters
    /// we can deduce conversions for part from difference between
    /// part->getColumns() and storage->getColumns().
    struct AlterConversions
    {
        /// Rename map new_name -> old_name
        std::unordered_map<String, String> rename_map;

        bool isColumnRenamed(const String & new_name) const { return rename_map.count(new_name) > 0; }
        String getColumnOldName(const String & new_name) const { return rename_map.at(new_name); }
    };

    using DataPartsLock = std::unique_lock<std::shared_mutex>;
    using DataPartsReadLock = std::shared_lock<std::shared_mutex>;
    DataPartsLock lockParts() const { return DataPartsLock(data_parts_mutex); }
    DataPartsReadLock lockPartsRead() const { return DataPartsReadLock(data_parts_mutex); }

    using DeleteBitmapGetter = std::function<ImmutableDeleteBitmapPtr(const DataPartPtr &)>;
    using DataPartsDeleteSnapshot = std::map<DataPartPtr, ImmutableDeleteBitmapPtr, LessDataPart>;
    DataPartsDeleteSnapshot getLatestDeleteSnapshot(const DataPartsVector & parts) const
    {
        DataPartsDeleteSnapshot res;
        auto lock = lockPartsRead();
        for (auto & part : parts) {
            res.insert({part, part->getDeleteBitmap()});
        }
        return res;
    }

    std::shared_ptr<UniqueKeyIndexCache> unique_key_index_cache;

    virtual MergeTreeDataPartType choosePartType(size_t bytes_uncompressed, size_t rows_count) const;
    virtual MergeTreeDataPartType choosePartTypeOnDisk(size_t bytes_uncompressed, size_t rows_count) const;

    /// After this method setColumns must be called, part will store in main storage
    /// by default
    MutableDataPartPtr createPart(const String & name, MergeTreeDataPartType type,
        const MergeTreePartInfo & part_info, const VolumePtr & volume,
        const String & relative_path, const IMergeTreeDataPart * parent_part = nullptr,
        StorageLocation location = StorageLocation::MAIN) const;

    /// Create part, that already exists on filesystem.
    /// After this methods 'loadColumnsChecksumsIndexes' must be called.
    MutableDataPartPtr createPart(const String & name, const VolumePtr & volume,
        const String & relative_path, const IMergeTreeDataPart * parent_part = nullptr,
        StorageLocation location = StorageLocation::MAIN) const;

    MutableDataPartPtr createPart(const String & name, const MergeTreePartInfo & part_info,
        const VolumePtr & volume, const String & relative_path, const IMergeTreeDataPart * parent_part = nullptr,
        StorageLocation locaiton = StorageLocation::MAIN) const;

    /// Parameters for various modes.
    struct MergingParams
    {
        /// Merging mode. See above.
        enum Mode
        {
            Ordinary            = 0,    /// Enum values are saved. Do not change them.
            Collapsing          = 1,
            Summing             = 2,
            Aggregating         = 3,
            Replacing           = 5,
            Graphite            = 6,
            VersionedCollapsing = 7,
        };

        Mode mode;

        /// For Collapsing and VersionedCollapsing mode.
        String sign_column;

        /// For Summing mode. If empty - columns_to_sum is determined automatically.
        Names columns_to_sum;

        /// For Replacing/VersionedCollapsing/Unique mode. Can be empty for Replacing and Unique.
        String version_column;

        /// For Unique mode, users can also use value of partition expr as version.
        /// As a result, all rows inside a partition share the same version, removing
        /// the cost to writing and reading an extra version column.
        bool partition_value_as_version = false;

        /// For Graphite mode.
        Graphite::Params graphite_params;

        /// Check that needed columns are present and have correct types.
        void check(const StorageInMemoryMetadata & metadata, bool has_unique_key) const;

        String getModeName() const;

        bool partitionValueAsVersion() const { return partition_value_as_version; }
        bool hasExplicitVersionColumn() const { return !version_column.empty() && !partitionValueAsVersion(); }
        bool hasVersionColumn() const { return hasExplicitVersionColumn() || partitionValueAsVersion(); }
    };

    MergeTreeMetaBase(
        const StorageID & table_id_,
        const String & relative_data_path_,
        const StorageInMemoryMetadata & metadata_,
        ContextMutablePtr context_,
        const String & date_column_name,
        const MergingParams & merging_params_,
        std::unique_ptr<MergeTreeSettings> storage_settings_,
        bool require_part_metadata_,
        bool attach_,
        BrokenPartCallback broken_part_callback_ = [](const String &) {});

    /// Names
    std::string getName() const override { return "MergeTreeMetaBase"; }

    StoragePolicyPtr getStoragePolicy(StorageLocation location) const override;
    virtual const String& getRelativeDataPath(StorageLocation location) const;
    virtual void setRelativeDataPath(StorageLocation location, const String& rel_path);

    bool supportsFinal() const override
    {
        return merging_params.mode == MergingParams::Collapsing
            || merging_params.mode == MergingParams::Summing
            || merging_params.mode == MergingParams::Aggregating
            || merging_params.mode == MergingParams::Replacing
            || merging_params.mode == MergingParams::VersionedCollapsing;
    }

    bool supportsSubcolumns() const override { return true; }
    bool supportsPrewhere() const override { return true; }
    bool supportsSampling() const override { return true; }
    bool supportsIndexForIn() const override { return true; }
    bool supportsMapImplicitColumn() const override { return true; }

    NamesAndTypesList getVirtuals() const override;

    bool mayBenefitFromIndexForIn(const ASTPtr & left_in_operand , ContextPtr query_context, const StorageMetadataPtr & metadata_snapshot) const override;

    /// Logger
    const String & getLogName() const { return log_name; }
    Poco::Logger * getLogger() const override { return log; }

    /// A global unique id for the storage. If storage UUID is not empty, use the storage UUID. Otherwise, use the address of current object.
    String getStorageUniqueID() const;

    //// Data parts
    /// Returns a copy of the list so that the caller shouldn't worry about locks.
    DataParts getDataParts(const DataPartStates & affordable_states) const;
    /// Returns sorted list of the parts with specified states
    ///  out_states will contain snapshot of each part state
    DataPartsVector getDataPartsVector(
        const DataPartStates & affordable_states, DataPartStateVector * out_states = nullptr, bool require_projection_parts = false) const;
    /// Returns all parts in specified partition
    DataPartsVector getDataPartsVectorInPartition(DataPartState /*state*/, const String & /*partition_id*/) const;

    /// Returns Committed parts
    DataParts getDataParts() const;
    DataPartsVector getDataPartsVector() const;

    /// Returns the part with the given name and state or nullptr if no such part.
    DataPartPtr getPartIfExists(const String & part_name, const DataPartStates & valid_states);
    DataPartPtr getPartIfExistsWithoutLock(const String & part_name, const DataPartStates & valid_states);
    DataPartPtr getPartIfExists(const MergeTreePartInfo & part_info, const DataPartStates & valid_states);
    DataPartPtr getPartIfExistsWithoutLock(const MergeTreePartInfo & part_info, const DataPartStates & valid_states);
    bool hasPart(const String & part_name, const DataPartStates & valid_states);
    bool hasPart(const MergeTreePartInfo & part_info, const DataPartStates & valid_states);

    /// TODO:
    /// Total size of active parts in bytes.
    virtual size_t getTotalActiveSizeInBytes() const { return 1.0; }

    /// Should be called if part data is suspected to be corrupted.
    void reportBrokenPart(const String & name) const
    {
        broken_part_callback(name);
    }

    /// TODO (alesap) Duplicate method required for compatibility.
    /// Must be removed.
    static ASTPtr extractKeyExpressionList(const ASTPtr & node)
    {
        return DB::extractKeyExpressionList(node);
    }

    /// Column sizes
    size_t getColumnCompressedSize(const std::string & name) const
    {
        auto lock = lockPartsRead();
        const auto it = column_sizes.find(name);
        return it == std::end(column_sizes) ? 0 : it->second.data_compressed;
    }

    ColumnSizeByName getColumnSizes() const override
    {
        auto lock = lockPartsRead();
        return column_sizes;
    }

    /// Calculates column sizes in compressed form for the current state of data_parts. Call with data_parts mutex locked.
    void calculateColumnSizesImpl();

    /// Adds or subtracts the contribution of the part to compressed column sizes.
    void addPartContributionToColumnSizes(const DataPartPtr & part);
    void removePartContributionToColumnSizes(const DataPartPtr & part);

    /// For ATTACH/DETACH/DROP PARTITION.
    String getPartitionIDFromQuery(const ASTPtr & ast, ContextPtr context) const;

    MutableDataPartPtr cloneAndLoadDataPartOnSameDisk(const DataPartPtr & src_part, const String & tmp_part_prefix,
                    const MergeTreePartInfo & dst_part_info, const StorageMetadataPtr & metadata_snapshot);

    /// Returns true if table can create new parts with adaptive granularity
    /// Has additional constraint in replicated version
    virtual bool canUseAdaptiveGranularity() const
    {
        const auto settings = getSettings();
        return settings->index_granularity_bytes != 0 &&
            (settings->enable_mixed_granularity_parts || !has_non_adaptive_index_granularity_parts);
    }

    /// Get constant pointer to storage settings.
    /// Copy this pointer into your scope and you will
    /// get consistent settings.
    MergeTreeSettingsPtr getSettings() const
    {
        return storage_settings.get();
    }

    /// Get table path on disk
    String getFullPathOnDisk(StorageLocation location, const DiskPtr & disk) const;

    ReservationPtr reserveSpace(UInt64 expected_size, VolumePtr & volume) const;

    /// Reserves space at least 1MB.
    ReservationPtr reserveSpace(UInt64 expected_size, StorageLocation location = StorageLocation::MAIN) const;

    /// Reserves space at least 1MB on specific disk or volume.
    static ReservationPtr reserveSpace(UInt64 expected_size, SpacePtr space);
    static ReservationPtr tryReserveSpace(UInt64 expected_size, SpacePtr space);

    /// Reserves space at least 1MB preferring best destination according to `ttl_infos`.
    ReservationPtr reserveSpacePreferringTTLRules(
        const StorageMetadataPtr & metadata_snapshot,
        UInt64 expected_size,
        const IMergeTreeDataPart::TTLInfos & ttl_infos,
        time_t time_of_move,
        size_t min_volume_index = 0,
        bool is_insert = false,
        DiskPtr selected_disk = nullptr,
        StorageLocation location = StorageLocation::MAIN) const;

    ReservationPtr tryReserveSpacePreferringTTLRules(
        const StorageMetadataPtr & metadata_snapshot,
        UInt64 expected_size,
        const IMergeTreeDataPart::TTLInfos & ttl_infos,
        time_t time_of_move,
        size_t min_volume_index = 0,
        bool is_insert = false,
        DiskPtr selected_disk = nullptr,
        StorageLocation location = StorageLocation::MAIN) const;

    /// Returns destination disk or volume for the TTL rule according to current storage policy
    /// 'is_insert' - is TTL move performed on new data part insert.
    SpacePtr getDestinationForMoveTTL(const TTLDescription & move_ttl,
        bool is_insert = false, StorageLocation location = StorageLocation::MAIN) const;

    /// Checks if given part already belongs destination disk or volume for the
    /// TTL rule.
    bool isPartInTTLDestination(const TTLDescription & ttl, const IMergeTreeDataPart & part) const;

    /// Return alter conversions for part which must be applied on fly.
    AlterConversions getAlterConversionsForPart(MergeTreeDataPartPtr part) const;

    MergeTreeDataFormatVersion format_version;

    /// Merging params - what additional actions to perform during merge.
    const MergingParams merging_params;

    ///TODO: MOCK FOR MergeTreeDataDumper
    /// Names of columns for primary key.
    Names primary_key_columns;

    ExpressionActionsPtr sorting_key_expr;


    bool is_custom_partitioned = false;

    /// Used only for old syntax tables. Never changes after init.
    Int64 minmax_idx_date_column_pos = -1; /// In a common case minmax index includes a date column.
    Int64 minmax_idx_time_column_pos = -1; /// In other cases, minmax index often includes a dateTime column.

    /// Get partition key expression on required columns
    static ExpressionActionsPtr getMinMaxExpr(const KeyDescription & partition_key, const ExpressionActionsSettings & settings);
    /// Get column names required for partition key
    static Names getMinMaxColumnsNames(const KeyDescription & partition_key);
    /// Get column types required for partition key
    static DataTypes getMinMaxColumnsTypes(const KeyDescription & partition_key);

    ExpressionActionsPtr getPrimaryKeyAndSkipIndicesExpression(const StorageMetadataPtr & metadata_snapshot) const;
    ExpressionActionsPtr getSortingKeyAndSkipIndicesExpression(const StorageMetadataPtr & metadata_snapshot) const;

    /// Get compression codec for part according to TTL rules and <compression>
    /// section from config.xml.
    CompressionCodecPtr getCompressionCodecForPart(size_t part_size_compressed, const IMergeTreeDataPart::TTLInfos & ttl_infos, time_t current_time) const;

    /// Record current query id where querying the table. Throw if there are already `max_queries` queries accessing the same table.
    void insertQueryIdOrThrow(const String & query_id, size_t max_queries) const;
    /// Remove current query id after query finished.
    void removeQueryId(const String & query_id) const;

    /// Return the partition expression types as a Tuple type. Return DataTypeUInt8 if partition expression is empty.
    DataTypePtr getPartitionValueType() const;

    /// Construct a block consisting only of possible virtual columns for part pruning.
    /// If one_part is true, fill in at most one part.
    Block getBlockWithVirtualPartColumns(const DataPartsVector & parts, bool one_part) const;

    /// For generating names of temporary parts during insertion.
    SimpleIncrement insert_increment;

    bool has_non_adaptive_index_granularity_parts = false;

    PinnedPartUUIDsPtr getPinnedPartUUIDs() const;

    /// Lock part in zookeeper for use common S3 data in several nodes
    /// Overridden in StorageReplicatedMergeTree
    virtual void lockSharedData(const IMergeTreeDataPart &) const {}

    /// Unlock common S3 data part in zookeeper
    /// Overridden in StorageReplicatedMergeTree
    virtual bool unlockSharedData(const IMergeTreeDataPart &) const { return true; }

    bool isBucketTable() const override { return getInMemoryMetadata().isClusterByKeyDefined(); }
    UInt64 getTableHashForClusterBy() const override; // to compare table engines efficiently


protected:
    friend class IMergeTreeDataPart;
    friend class MergeTreeDataPartCNCH;
    friend class MergeTreeDataMergerMutator;
    friend struct ReplicatedMergeTreeTableMetadata;
    friend class StorageReplicatedMergeTree;
    friend class MergeTreeDataWriter;

    bool require_part_metadata;

    /// Current column sizes in compressed and uncompressed form.
    ColumnSizeByName column_sizes;

    /// Engine-specific methods
    BrokenPartCallback broken_part_callback;

    /// physical address of current object. used as identifier of the MergeTreeData if UUID doesn't exits.
    String storage_address;

    String log_name;
    Poco::Logger * log;

    /// Storage settings.
    /// Use get and set to receive readonly versions.
    MultiVersion<MergeTreeSettings> storage_settings;

    /// Used to determine which UUIDs to send to root query executor for deduplication.
    mutable std::shared_mutex pinned_part_uuids_mutex;
    PinnedPartUUIDsPtr pinned_part_uuids;

    /// Nullable key
    bool allow_nullable_key = false;

    /// Work with data parts

    struct TagByInfo{};
    struct TagByStateAndInfo{};

    static const MergeTreePartInfo & dataPartPtrToInfo(const DataPartPtr & part)
    {
        return part->info;
    }

    static DataPartStateAndInfo dataPartPtrToStateAndInfo(const DataPartPtr & part)
    {
        return {part->getState(), part->info};
    }

    using DataPartsIndexes = boost::multi_index_container<
        DataPartPtr,
        boost::multi_index::indexed_by<
            /// Index by Info
            boost::multi_index::ordered_unique<
                boost::multi_index::tag<TagByInfo>,
                boost::multi_index::global_fun<const DataPartPtr &, const MergeTreePartInfo &, dataPartPtrToInfo>>,
            /// Index by (State, Info), is used to obtain ordered slices of parts with the same state
            boost::multi_index::ordered_unique<
                boost::multi_index::tag<TagByStateAndInfo>,
                boost::multi_index::global_fun<const DataPartPtr &, DataPartStateAndInfo, dataPartPtrToStateAndInfo>,
                LessStateDataPart>>>;

    /// Current set of data parts.
    mutable std::shared_mutex data_parts_mutex;
    DataPartsIndexes data_parts_indexes;
    DataPartsIndexes::index<TagByInfo>::type & data_parts_by_info;
    DataPartsIndexes::index<TagByStateAndInfo>::type & data_parts_by_state_and_info;

    using DataPartIteratorByInfo = DataPartsIndexes::index<TagByInfo>::type::iterator;
    using DataPartIteratorByStateAndInfo = DataPartsIndexes::index<TagByStateAndInfo>::type::iterator;

    boost::iterator_range<DataPartIteratorByStateAndInfo> getDataPartsStateRange(DataPartState state) const
    {
        auto begin = data_parts_by_state_and_info.lower_bound(state, LessStateDataPart());
        auto end = data_parts_by_state_and_info.upper_bound(state, LessStateDataPart());
        return {begin, end};
    }

    boost::iterator_range<DataPartIteratorByInfo> getDataPartsPartitionRange(const String & partition_id) const
    {
        auto begin = data_parts_by_info.lower_bound(PartitionID(partition_id), LessDataPart());
        auto end = data_parts_by_info.upper_bound(PartitionID(partition_id), LessDataPart());
        return {begin, end};
    }

    static decltype(auto) getStateModifier(DataPartState state)
    {
        return [state] (const DataPartPtr & part) { part->setState(state); };
    }

    void modifyPartState(DataPartIteratorByStateAndInfo it, DataPartState state)
    {
        if (!data_parts_by_state_and_info.modify(it, getStateModifier(state)))
            throw Exception("Can't modify " + (*it)->getNameWithState(), ErrorCodes::LOGICAL_ERROR);
    }

    void modifyPartState(DataPartIteratorByInfo it, DataPartState state)
    {
        if (!data_parts_by_state_and_info.modify(data_parts_indexes.project<TagByStateAndInfo>(it), getStateModifier(state)))
            throw Exception("Can't modify " + (*it)->getNameWithState(), ErrorCodes::LOGICAL_ERROR);
    }

    void modifyPartState(const DataPartPtr & part, DataPartState state)
    {
        auto it = data_parts_by_info.find(part->info);
        if (it == data_parts_by_info.end() || (*it).get() != part.get())
            throw Exception("Part " + part->name + " doesn't exist", ErrorCodes::LOGICAL_ERROR);

        if (!data_parts_by_state_and_info.modify(data_parts_indexes.project<TagByStateAndInfo>(it), getStateModifier(state)))
            throw Exception("Can't modify " + (*it)->getNameWithState(), ErrorCodes::LOGICAL_ERROR);
    }

    PrimaryIndexCachePtr primary_index_cache;

    void checkProperties(const StorageInMemoryMetadata & new_metadata, const StorageInMemoryMetadata & old_metadata, bool attach = false) const;

    void setProperties(const StorageInMemoryMetadata & new_metadata, const StorageInMemoryMetadata & old_metadata, bool attach = false);

    void checkPartitionKeyAndInitMinMax(const KeyDescription & new_partition_key);

    void checkTTLExpressions(const StorageInMemoryMetadata & new_metadata, const StorageInMemoryMetadata & old_metadata) const;

    /// If there is no part in the partition with ID `partition_id`, returns empty ptr. Should be called under the lock.
    DataPartPtr getAnyPartInPartition(const String & partition_id, DataPartsLock & data_parts_lock) const;

    /// Return most recent mutations commands for part which weren't applied
    /// Used to receive AlterConversions for part and apply them on fly. This
    /// method has different implementations for replicated and non replicated
    /// MergeTree because they store mutations in different way.
    virtual MutationCommands getFirstAlterMutationCommandsForPart(const DataPartPtr & part) const = 0;

    bool canUsePolymorphicParts(const MergeTreeSettings & settings, String * out_reason = nullptr) const;

    static void checkSampleExpression(const StorageInMemoryMetadata & metadata, bool allow_sampling_expression_not_in_primary_key);

    /// Checks whether the column is in the primary key, possibly wrapped in a chain of functions with single argument.
    bool isPrimaryOrMinMaxKeyColumnPossiblyWrappedInFunctions(const ASTPtr & node, const StorageMetadataPtr & metadata_snapshot) const;

private:
    // Record all query ids which access the table. It's guarded by `query_id_set_mutex` and is always mutable.
    mutable std::set<String> query_id_set;
    mutable std::mutex query_id_set_mutex;

    /// Relative path data, changes during rename for ordinary databases use
    /// under lockForShare if rename is possible.
    String relative_data_path;
};

 /// EOF
}
