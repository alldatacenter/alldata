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

#include <DataStreams/IBlockInputStream.h>

#include <Core/Block.h>
#include <common/types.h>
#include <Core/NamesAndTypes.h>
#include <Storages/IStorage.h>
#include <Storages/MergeTree/IMergeTreeDataPart_fwd.h>
#include <Storages/MergeTree/MergeTreeIndexGranularity.h>
#include <Storages/MergeTree/MergeTreeIndexGranularityInfo.h>
#include <Storages/MergeTree/MergeTreeIndices.h>
#include <Storages/MergeTree/MergeTreeProjections.h>
#include <Storages/MergeTree/MergeTreePartInfo.h>
#include <Storages/MergeTree/MergeTreePartition.h>
#include <Storages/MergeTree/MergeTreeDataPartChecksum.h>
#include <Storages/MergeTree/MergeTreeDataPartTTLInfo.h>
#include <Storages/MergeTree/MergeTreeIOSettings.h>
#include <Storages/MergeTree/MergeTreeDataPartVersions.h>
#include <Storages/MergeTree/KeyCondition.h>
#include <Storages/MergeTree/MergeTreeSuffix.h>
#include <Storages/UniqueKeyIndex.h>
#include <Storages/MergeTree/DeleteBitmapMeta.h>
#include <Transaction/TxnTimestamp.h>
#include <Poco/Path.h>
#include <Common/HashTable/HashMap.h>
#include <common/types.h>
#include <roaring.hh>
#include <forward_list>


namespace zkutil
{
    class ZooKeeper;
    using ZooKeeperPtr = std::shared_ptr<ZooKeeper>;
}

namespace DB
{

class IMergeTreeDataPart;
using IMergeTreeDataPartPtr = std::shared_ptr<const IMergeTreeDataPart>;
struct ColumnSize;
class MergeTreeMetaBase;
class MergeTreeData;
struct FutureMergedMutatedPart;
class IReservation;
using ReservationPtr = std::unique_ptr<IReservation>;

class IVolume;
using VolumePtr = std::shared_ptr<IVolume>;

class IMergeTreeReader;
class IMergeTreeDataPartWriter;
class MarkCache;
class UncompressedCache;

/// Description of the data part.
class IMergeTreeDataPart : public std::enable_shared_from_this<IMergeTreeDataPart>
{
public:
    //static constexpr auto DATA_FILE_EXTENSION = ".bin";
    static constexpr UInt64 NOT_INITIALIZED_COMMIT_TIME = 0;

    using Checksums = MergeTreeDataPartChecksums;
    using Checksum = MergeTreeDataPartChecksums::Checksum;
    using ChecksumsPtr = std::shared_ptr<Checksums>;
    using ValueSizeMap = std::map<std::string, double>;

    using MergeTreeReaderPtr = std::unique_ptr<IMergeTreeReader>;
    using MergeTreeWriterPtr = std::unique_ptr<IMergeTreeDataPartWriter>;

    using ColumnSizeByName = std::unordered_map<std::string, ColumnSize>;
    using NameToNumber = std::unordered_map<std::string, size_t>;

    using Index = Columns;
    using IndexPtr = std::shared_ptr<Index>;

    using Type = MergeTreeDataPartType;

    using Versions = std::shared_ptr<MergeTreeDataPartVersions>;

    IMergeTreeDataPart(
        const MergeTreeMetaBase & storage_,
        const String & name_,
        const MergeTreePartInfo & info_,
        const VolumePtr & volume,
        const std::optional<String> & relative_path,
        Type part_type_,
        const IMergeTreeDataPart * parent_part_,
        IStorage::StorageLocation location_,
        const UUID& part_id_ = UUIDHelpers::Nil);

    IMergeTreeDataPart(
        const MergeTreeMetaBase & storage_,
        const String & name_,
        const VolumePtr & volume,
        const std::optional<String> & relative_path,
        Type part_type_,
        const IMergeTreeDataPart * parent_part_,
        IStorage::StorageLocation location_,
        const UUID& part_id_ = UUIDHelpers::Nil);

    virtual MergeTreeReaderPtr getReader(
        const NamesAndTypesList & columns_,
        const StorageMetadataPtr & metadata_snapshot,
        const MarkRanges & mark_ranges,
        UncompressedCache * uncompressed_cache,
        MarkCache * mark_cache,
        const MergeTreeReaderSettings & reader_settings_,
        const ValueSizeMap & avg_value_size_hints_ = ValueSizeMap{},
        const ReadBufferFromFileBase::ProfileCallback & profile_callback_ = ReadBufferFromFileBase::ProfileCallback{}) const = 0;

    virtual MergeTreeWriterPtr getWriter(
        const NamesAndTypesList & columns_list,
        const StorageMetadataPtr & metadata_snapshot,
        const std::vector<MergeTreeIndexPtr> & indices_to_recalc,
        const CompressionCodecPtr & default_codec_,
        const MergeTreeWriterSettings & writer_settings,
        const MergeTreeIndexGranularity & computed_index_granularity = {}) const = 0;

    virtual bool isStoredOnDisk() const = 0;

    virtual bool supportsVerticalMerge() const { return false; }

    /// NOTE: Returns zeros if column files are not found in checksums.
    /// Otherwise return information about column size on disk.
    ColumnSize getColumnSize(const String & column_name, const IDataType & /* type */) const;

    /// Return information about column size on disk for all columns in part
    ColumnSize getTotalColumnsSize() const { return total_columns_size; }

    virtual String getFileNameForColumn(const NameAndTypePair & column) const = 0;

    virtual ~IMergeTreeDataPart();

    using ColumnToSize = std::map<std::string, UInt64>;
    /// Populates columns_to_size map (compressed size).
    void accumulateColumnSizes(ColumnToSize & /* column_to_size */) const;

    Type getType() const { return part_type; }

    String getTypeName() const { return getType().toString(); }

    void setColumns(const NamesAndTypesList & new_columns);
    virtual void setColumnsPtr(const NamesAndTypesListPtr & new_columns_ptr);

    const NamesAndTypesList & getColumns() const { return *columns_ptr; }
    NamesAndTypesListPtr getColumnsPtr() const { return columns_ptr; }
    NamesAndTypesList getNamesAndTypes() const { return *columns_ptr; }

    /// Throws an exception if part is not stored in on-disk format.
    void assertOnDisk() const;

    void remove() const;

    virtual void projectionRemove(const String & parent_to, bool keep_shared_data) const;


    /// Initialize columns (from columns.txt if exists, or create from column files if not).
    /// Load checksums from checksums.txt if exists. Load index if required.
    virtual void loadColumnsChecksumsIndexes(bool require_columns_checksums, bool check_consistency);

    String getMarksFileExtension() const { return index_granularity_info.marks_file_extension; }

    /// Generate the new name for this part according to `new_part_info` and min/max dates from the old name.
    /// This is useful when you want to change e.g. block numbers or the mutation version of the part.
    String getNewName(const MergeTreePartInfo & new_part_info) const;

    /// Returns column position in part structure or std::nullopt if it's missing in part.
    ///
    /// NOTE: Doesn't take column renames into account, if some column renames
    /// take place, you must take original name of column for this part from
    /// storage and pass it to this method.
    std::optional<size_t> getColumnPosition(const String & column_name) const;

    /// Returns the name of a column with minimum compressed size (as returned by getColumnSize()).
    /// If no checksums are present returns the name of the first physically existing column.
    String getColumnNameWithMinimumCompressedSize(const StorageMetadataPtr & metadata_snapshot) const;

    bool contains(const IMergeTreeDataPart & other) const { return info.contains(other.info); }

    /// If the partition key includes date column (a common case), this function will return min and max values for that column.
    std::pair<DayNum, DayNum> getMinMaxDate() const;

    /// otherwise, if the partition key includes dateTime column (also a common case), this function will return min and max values for that column.
    std::pair<time_t, time_t> getMinMaxTime() const;

    bool isEmpty() const { return rows_count == 0; }

    /// Compute part block id for zero level part. Otherwise throws an exception.
    String getZeroLevelPartBlockID() const;

    const auto & get_name() const { return name; }
    const auto & get_info() const { return info; }
    const auto & get_partition() const { return partition; }
    const auto & get_deleted() const { return deleted; }
    const auto & get_commit_time() const { return commit_time; }
    const auto & getUUID() const { return uuid; }

    const MergeTreeMetaBase & storage;

    String name;
    MergeTreePartInfo info;

    std::atomic<bool> has_bitmap {false};

    /// Part unique identifier.
    /// The intention is to use it for identifying cases where the same part is
    /// processed by multiple shards.
    UUID uuid = UUIDHelpers::Nil;

    VolumePtr volume;

    /// A directory path (relative to storage's path) where part data is actually stored
    /// Examples: 'detached/tmp_fetch_<name>', 'tmp_<name>', '<name>'
    mutable String relative_path;
    MergeTreeIndexGranularityInfo index_granularity_info;

    size_t rows_count = 0;


    time_t modification_time = 0;
    /// When the part is removed from the working set. Changes once.
    mutable std::atomic<time_t> remove_time { std::numeric_limits<time_t>::max() };

    /// If true, the destructor will delete the directory with the part.
    bool is_temp = false;

    /// If true it means that there are no ZooKeeper node for this part, so it should be deleted only from filesystem
    bool is_duplicate = false;

    /// If true it means that this part is under recoding so that we need take care to read this part when the query
    /// has been rewritten.
    mutable std::atomic<bool> is_encoding {false};

    /// Frozen by ALTER TABLE ... FREEZE ... It is used for information purposes in system.parts table.
    mutable std::atomic<bool> is_frozen {false};

    /// Flag for keep S3 data when zero-copy replication over S3 turned on.
    mutable bool force_keep_shared_data = false;

    /**
     * Part state is a stage of its lifetime. States are ordered and state of a part could be increased only.
     * Part state should be modified under data_parts mutex.
     *
     * Possible state transitions:
     * Temporary -> Precommitted:    we are trying to commit a fetched, inserted or merged part to active set
     * Precommitted -> Outdated:     we could not add a part to active set and are doing a rollback (for example it is duplicated part)
     * Precommitted -> Committed:    we successfully committed a part to active dataset
     * Precommitted -> Outdated:     a part was replaced by a covering part or DROP PARTITION
     * Outdated -> Deleting:         a cleaner selected this part for deletion
     * Deleting -> Outdated:         if an ZooKeeper error occurred during the deletion, we will retry deletion
     * Committed -> DeleteOnDestroy: if part was moved to another disk
     */
    enum class State
    {
        Temporary,       /// the part is generating now, it is not in data_parts list
        PreCommitted,    /// the part is in data_parts, but not used for SELECTs
        Committed,       /// active data part, used by current and upcoming SELECTs
        Outdated,        /// not active data part, but could be used by only current SELECTs, could be deleted after SELECTs finishes
        Deleting,        /// not active data part with identity refcounter, it is deleting right now by a cleaner
        DeleteOnDestroy, /// part was moved to another disk and should be deleted in own destructor
    };

    static constexpr auto all_part_states =
    {
        State::Temporary, State::PreCommitted, State::Committed, State::Outdated, State::Deleting,
        State::DeleteOnDestroy
    };

    using TTLInfo = MergeTreeDataPartTTLInfo;
    using TTLInfos = MergeTreeDataPartTTLInfos;

    TTLInfos ttl_infos;

    /// Current state of the part. If the part is in working set already, it should be accessed via data_parts mutex
    void setState(State new_state) const;
    State getState() const;

    /// Returns name of state
    static String stateToString(State state);
    String stateString() const;

    String getNameWithState() const
    {
        return name + " (state " + stateString() + ")";
    }

    /// Returns true if state of part is one of affordable_states
    bool checkState(const std::initializer_list<State> & affordable_states) const
    {
        for (auto affordable_state : affordable_states)
        {
            if (state == affordable_state)
                return true;
        }
        return false;
    }

    /// Throws an exception if state of the part is not in affordable_states
    void assertState(const std::initializer_list<State> & affordable_states) const;

    /// Primary key (correspond to primary.idx file).
    /// Always loaded in RAM. Contains each index_granularity-th value of primary key tuple.
    /// Note that marks (also correspond to primary key) is not always in RAM, but cached. See MarkCache.h.
    IndexPtr index = std::make_shared<Columns>();

    MergeTreePartition partition;

    /// Amount of rows between marks
    /// As index always loaded into memory
    MergeTreeIndexGranularity index_granularity;

    /// Index that for each part stores min and max values of a set of columns. This allows quickly excluding
    /// parts based on conditions on these columns imposed by a query.
    /// Currently this index is built using only columns required by partition expression, but in principle it
    /// can be built using any set of columns.
    struct MinMaxIndex
    {
        /// A direct product of ranges for each key column. See Storages/MergeTree/KeyCondition.cpp for details.
        std::vector<Range> hyperrectangle;
        bool initialized = false;

    public:
        MinMaxIndex() = default;

        /// For month-based partitioning.
        MinMaxIndex(DayNum min_date, DayNum max_date)
            : hyperrectangle(1, Range(min_date, true, max_date, true))
            , initialized(true)
        {
        }

        void load(const MergeTreeMetaBase & data, const DiskPtr & disk_, const String & part_path);
        void load(const MergeTreeMetaBase & data, ReadBuffer & buf);
        void store(const MergeTreeMetaBase & data, const DiskPtr & disk_, const String & part_path, Checksums & checksums) const;
        void store(const Names & column_names, const DataTypes & data_types, const DiskPtr & disk_, const String & part_path, Checksums & checksums) const;
        void store(const MergeTreeMetaBase & data, const String & part_path, WriteBuffer & buf) const;

        void update(const Block & block, const Names & column_names);
        void merge(const MinMaxIndex & other);
    };

    MinMaxIndex minmax_idx;

	Versions versions;

    /// only be used if the storage enables persistent checksums.
    ChecksumsPtr checksums_ptr {nullptr};

    /// Columns with values, that all have been zeroed by expired ttl
    NameSet expired_columns;

    CompressionCodecPtr default_codec;

    /// load checksum on demand. return ChecksumsPtr from global cache or its own checksums_ptr;
    ChecksumsPtr getChecksums() const;

    /// Get primary index, load if primary index is not initialized.
    IndexPtr getIndex() const;

    /// For data in RAM ('index')
    UInt64 getIndexSizeInBytes() const;
    UInt64 getIndexSizeInAllocatedBytes() const;
    UInt64 getMarksCount() const;

    UInt64 getBytesOnDisk() const { return bytes_on_disk; }
    void setBytesOnDisk(UInt64 bytes_on_disk_) { bytes_on_disk = bytes_on_disk_; }

    size_t getFileSizeOrZero(const String & file_name) const;
    off_t getFileOffsetOrZero(const String & file_name) const;

    /// Returns path to part dir relatively to disk mount point
    String getFullRelativePath() const;

    /// Returns full path to part dir
    String getFullPath() const;

    IMergeTreeDataPartPtr getMvccDataPart(const String & file_name) const;

    /// Moves a part to detached/ directory and adds prefix to its name
    void renameToDetached(const String & prefix) const;
    String getRelativePathForDetachedPart(const String & prefix) const;

    /// Makes checks and move part to new directory
    /// Changes only relative_dir_name, you need to update other metadata (name, is_temp) explicitly
    virtual void renameTo(const String & new_relative_path, bool remove_new_dir_if_exists) const;

    /// Makes clone of a part in detached/ directory via hard links
    virtual void makeCloneInDetached(const String & prefix, const StorageMetadataPtr & metadata_snapshot) const;

    /// Makes full clone of part in specified subdirectory (relative to storage data directory, e.g. "detached") on another disk
    void makeCloneOnDisk(const DiskPtr & disk, const String & directory_name) const;

    /// Check if there is only one map column not kv store and enable_compact_map_data.
    bool hasOnlyOneCompactedMapColumnNotKV() const;

    /// Checks that .bin and .mrk files exist.
    ///
    /// NOTE: Doesn't take column renames into account, if some column renames
    /// take place, you must take original name of column for this part from
    /// storage and pass it to this method.
    virtual bool hasColumnFiles(const NameAndTypePair & /* column */) const { return false; }

    /// Returns true if this part shall participate in merges according to
    /// settings of given storage policy.
    bool shallParticipateInMerges(const StoragePolicyPtr & storage_policy) const;

    /// Calculate the total size of the entire directory with all the files
    static UInt64 calculateTotalSizeOnDisk(const DiskPtr & disk_, const String & from);
    void calculateColumnsSizesOnDisk();

    String getRelativePathForPrefix(const String & prefix) const;

    bool isProjectionPart() const { return parent_part != nullptr; }

    const IMergeTreeDataPart * getParentPart() const { return parent_part; }

    const std::map<String, std::shared_ptr<IMergeTreeDataPart>> & getProjectionParts() const { return projection_parts; }

    void addProjectionPart(const String & projection_name, std::shared_ptr<IMergeTreeDataPart> && projection_part)
    {
        projection_parts.emplace(projection_name, std::move(projection_part));
    }

    bool hasProjection(const String & projection_name) const
    {
        return projection_parts.find(projection_name) != projection_parts.end();
    }

    void loadProjections(bool require_columns_checksums, bool check_consistency);

    /// Return set of metadat file names without checksums. For example,
    /// columns.txt or checksums.txt itself.
    NameSet getFileNamesWithoutChecksums() const;

    /// File with compression codec name which was used to compress part columns
    /// by default. Some columns may have their own compression codecs, but
    /// default will be stored in this file.
    static inline constexpr auto DEFAULT_COMPRESSION_CODEC_FILE_NAME = "default_compression_codec.txt";

    static inline constexpr auto DELETE_ON_DESTROY_MARKER_FILE_NAME = "delete-on-destroy.txt";

    static inline constexpr auto UUID_FILE_NAME = "uuid.txt";

    /// Checks that all TTLs (table min/max, column ttls, so on) for part
    /// calculated. Part without calculated TTL may exist if TTL was added after
    /// part creation (using alter query with materialize_ttl setting).
    bool checkAllTTLCalculated(const StorageMetadataPtr & metadata_snapshot) const;

    /// Returns serialization for column according to files in which column is written in part.
    SerializationPtr getSerializationForColumn(const NameAndTypePair & column) const;

    /// Return some uniq string for file
    /// Required for distinguish different copies of the same part on S3
    String getUniqueId() const;

    bool containsExactly(const IMergeTreeDataPart & other) const
    {
        return info.partition_id == other.info.partition_id && info.min_block == other.info.min_block
            && info.max_block == other.info.max_block && info.level >= other.info.level && commit_time >= other.commit_time;
    }

    void setPreviousPart(IMergeTreeDataPartPtr part) const { prev_part = std::move(part); }
    const IMergeTreeDataPartPtr & tryGetPreviousPart() const { return prev_part; }
    const IMergeTreeDataPartPtr & getPreviousPart() const;
    IMergeTreeDataPartPtr getBasePart() const;
    void enumeratePreviousParts(const std::function<void(const IMergeTreeDataPartPtr &)> &) const;

    bool isPartial() const { return info.hint_mutation; }

    /// FIXME: move to PartMetaEntry once metastore is added
    /// Used to prevent concurrent modification to a part.
    /// Can be removed once all data modification tasks (e.g, build bitmap index, recode) are
    /// implemented as mutation commands and parts become immutable.
    mutable std::mutex mutate_mutex;

    /// Total size on disk, not only columns. May not contain size of
    /// checksums.txt and columns.txt. 0 - if not counted;
    UInt64 bytes_on_disk{0};

    TxnTimestamp columns_commit_time;
    TxnTimestamp mutation_commit_time;

    UInt64 staging_txn_id = 0;

    /// Only for storage with UNIQUE KEY
    String min_unique_key;
    String max_unique_key;

    mutable UInt64 virtual_part_size = 0;

    /// secondary_txn_id > 0 mean this parts belong to an explicit transaction
    mutable TxnTimestamp secondary_txn_id {0};

    /// commit_time equals 0 means the data part is not committed.
    mutable TxnTimestamp commit_time {NOT_INITIALIZED_COMMIT_TIME};

    bool deleted = false;
    size_t covered_parts_count = 0; /// only for drop range. used to count how many parts the drop range covers.
    size_t covered_parts_size = 0; /// only for deleted part. used to record bytes_on_disk before the part is deleted.
    size_t covered_parts_rows = 0; /// only for deleted part. used to record rows_count before the part is deleted.

    Int64 bucket_number = -1;               /// bucket_number > 0 if the part is assigned to bucket
    UInt64 table_definition_hash = 0;       // cluster by definition hash for data file

    /************** Unique Table Delete Bitmap ***********/
    /// Should be not-null once set
    ImmutableDeleteBitmapPtr delete_bitmap;
    /// stored in descending order of commit time
    mutable std::forward_list<DataModelDeleteBitmapPtr> delete_bitmap_metas;

    void setDeleteBitmapMeta(DeleteBitmapMetaPtr bitmap_meta) const;

    void setDeleteBitmap(ImmutableDeleteBitmapPtr delete_bitmap_) { delete_bitmap = std::move(delete_bitmap_); }

    /// Return null if the part doesn't have delete bitmap.
    /// Otherwise load the bitmap on demand and return.
    virtual const ImmutableDeleteBitmapPtr & getDeleteBitmap([[maybe_unused]] bool is_unique_new_part = false) const
    {
        return delete_bitmap;
    }

    /// Return unique key index (corresponding to unique_key.idx) if the part has unique key.
    /// Data parts supporting unique key should override this method.
    virtual UniqueKeyIndexPtr getUniqueKeyIndex() const;

    /// Return version value from partition. Throws exception if the table didn't use partition as version
    UInt64 getVersionFromPartition() const;

    DiskCacheMode disk_cache_mode {DiskCacheMode::AUTO};
    bool enableDiskCache() const;

protected:
    friend class MergeTreeMetaBase;
    friend class MergeTreeData;
    friend class MergeTreeCloudData;
    friend class MergeScheduler;

    /// Total size of all columns, calculated once in calculateColumnSizesOnDisk
    ColumnSize total_columns_size;

    /// Size for each column, calculated once in calculateColumnSizesOnDisk
    ColumnSizeByName columns_sizes;

    /// Columns description. Cannot be changed, after part initialization.
    /// It could be shared between parts. This can help reduce memory usage during query execution.
    NamesAndTypesListPtr columns_ptr = std::make_shared<NamesAndTypesList>();
    const Type part_type;

    /// Not null when it's a projection part.
    const IMergeTreeDataPart * parent_part;

    std::map<String, std::shared_ptr<IMergeTreeDataPart>> projection_parts;

    /// Protect checksums_ptr. FIXME:  May need more protection in getChecksums()
    /// to prevent checksums_ptr from being modified and corvered by multiple threads.
    mutable std::mutex checksums_mutex;
    mutable std::mutex index_mutex;

    const IStorage::StorageLocation location;

    void removeIfNeeded();

    virtual void checkConsistency(bool require_part_metadata) const;
    void checkConsistencyBase() const;

    /// Fill each_columns_size and total_size with sizes from columns files on
    /// disk using columns and checksums.
    virtual void calculateEachColumnSizes(ColumnSizeByName & each_columns_size, ColumnSize & total_size) const = 0;

    std::optional<bool> keepSharedDataInDecoupledStorage() const;

    ColumnSize getMapColumnSizeNotKV(const IMergeTreeDataPart::ChecksumsPtr & checksums, const NameAndTypePair & column) const;

    IndexPtr loadIndexFromBuffer(ReadBuffer & index_file, const KeyDescription & primary_key) const;

    /// Loads unique key index if the part has unique key.
    /// Data parts supporting unique key should override this method.
    virtual UniqueKeyIndexPtr loadUniqueKeyIndex();

    virtual void removeImpl(bool keep_shared_data) const;

private:
    /// In compact parts order of columns is necessary
    NameToNumber column_name_to_position;

    /// Reads part unique identifier (if exists) from uuid.txt
    void loadUUID();

    /// Reads columns names and types from columns.txt
    void loadColumns(bool require);

    /// If versions.txt exists, reads versions from it
    void loadVersions();

    /// If checksums.txt exists, reads file's checksums (and sizes) from it
    virtual ChecksumsPtr loadChecksums(bool require);

    /// Loads marks index granularity into memory
    virtual void loadIndexGranularity();

    /// Loads index from cache
    void loadIndexFromCache();

    /// Loads index file.
    virtual IndexPtr loadIndex();

    /// Load rows count for this part from disk (for the newer storage format version).
    /// For the older format version calculates rows count from the size of a column with a fixed size.
    void loadRowsCount();

    /// Loads ttl infos in json format from file ttl.txt. If file doesn't exists assigns ttl infos with all zeros
    void loadTTLInfos();

    void loadPartitionAndMinMaxIndex();

    /// Load default compression codec from file default_compression_codec.txt
    /// if it not exists tries to deduce codec from compressed column without
    /// any specifial compression.
    void loadDefaultCompressionCodec();

    /// Found column without specific compression and return codec
    /// for this column with default parameters.
    CompressionCodecPtr detectDefaultCompressionCodec() const;

    mutable State state{State::Temporary};

    mutable IMergeTreeDataPartPtr prev_part;

public:

    /// APIs for data parts serialization/deserialization
    void storePartitionAndMinMaxIndex(WriteBuffer & buf) const;
    void loadColumns(ReadBuffer & buf);
    void loadPartitionAndMinMaxIndex(ReadBuffer & buf);
    /// FIXME: old_meta_format is used to make it compatible with old part metadata. Remove it later.
    void loadTTLInfos(ReadBuffer & buf, bool old_meta_format = false);
    void loadDefaultCompressionCodec(const String & codec_str);
    virtual void loadIndexGranularity(const size_t marks_count, const std::vector<size_t> & index_granularities);

    /** ----------------------- COMPATIBLE CODE BEGIN-------------------------- */
    /*  compatible with old metastore. remove this later  */

    /// deserialize metadata into MergeTreeDataPart.
    /// @IMPORTANT Do not load checksums
    void deserializeMetaInfo(const String & metadata);
    void deserializeColumns(ReadBuffer & buffer);
    void deserializePartitionAndMinMaxIndex(ReadBuffer & buffer);

    /// serialize part into binary format
    void serializePartitionAndMinMaxIndex(WriteBuffer & buf) const;

    /*  -----------------------  COMPATIBLE CODE END -------------------------- */
};

using MergeTreeDataPartState = IMergeTreeDataPart::State;
using MergeTreeDataPartPtr = std::shared_ptr<const IMergeTreeDataPart>;
using MergeTreeMutableDataPartPtr = std::shared_ptr<IMergeTreeDataPart>;

bool isCompactPart(const MergeTreeDataPartPtr & data_part);
bool isWidePart(const MergeTreeDataPartPtr & data_part);
bool isInMemoryPart(const MergeTreeDataPartPtr & data_part);
bool isCnchPart(const MergeTreeDataPartPtr & data_part);

void writePartBinary(const IMergeTreeDataPart & part, WriteBuffer & buf);
}
