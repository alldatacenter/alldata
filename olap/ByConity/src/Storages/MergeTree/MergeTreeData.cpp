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

#include <Compression/CompressedReadBuffer.h>
#include <DataStreams/copyData.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeDate.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeEnum.h>
#include <DataTypes/DataTypeLowCardinality.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeUUID.h>
#include <DataTypes/DataTypeTuple.h>
#include <DataTypes/DataTypeByteMap.h>
#include <DataTypes/NestedUtils.h>
#include <DataTypes/MapHelpers.h>
#include <Formats/FormatFactory.h>
#include <Functions/FunctionFactory.h>
#include <Functions/IFunction.h>
#include <IO/ConcatReadBuffer.h>
#include <IO/Operators.h>
#include <IO/ReadBufferFromMemory.h>
#include <IO/WriteBufferFromString.h>
#include <Interpreters/ExpressionAnalyzer.h>
#include <Interpreters/PartLog.h>
#include <Interpreters/TreeRewriter.h>
#include <Interpreters/inplaceBlockConversions.h>
#include <Interpreters/Context.h>
#include <Interpreters/InterpreterSelectQuery.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTNameTypePair.h>
#include <Parsers/ASTPartition.h>
#include <Parsers/ASTSetQuery.h>
#include <Parsers/ExpressionListParsers.h>
#include <Parsers/parseQuery.h>
#include <Parsers/queryToString.h>
#include <Processors/Formats/InputStreamFromInputFormat.h>
#include <Storages/AlterCommands.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <Storages/MergeTree/MergeTreeDataPartCompact.h>
#include <Storages/MergeTree/MergeTreeDataPartInMemory.h>
#include <Storages/MergeTree/MergeTreeDataPartWide.h>
#include <Storages/MergeTree/MergeTreeSequentialSource.h>
#include <Storages/MergeTree/MergedBlockOutputStream.h>
#include <Storages/MergeTree/MergedColumnOnlyOutputStream.h>
#include <Storages/MergeTree/checkDataPart.h>
#include <Storages/MergeTree/localBackup.h>
#include <Storages/MergeTree/ChecksumsCache.h>
#include <Storages/StorageMergeTree.h>
#include <Storages/StorageReplicatedMergeTree.h>
#include <Storages/VirtualColumnUtils.h>
#include <Common/Increment.h>
#include <Common/SimpleIncrement.h>
#include <Common/Stopwatch.h>
#include <Common/StringUtils/StringUtils.h>
#include <Common/escapeForFileName.h>
#include <Common/quoteString.h>
#include <Common/typeid_cast.h>

#include <boost/range/adaptor/filtered.hpp>
#include <boost/algorithm/string/join.hpp>

#include <common/scope_guard_safe.h>

#include <algorithm>
#include <iomanip>
#include <optional>
#include <set>
#include <thread>
#include <typeinfo>
#include <typeindex>
#include <unordered_set>
#include <filesystem>


namespace fs = std::filesystem;

namespace ProfileEvents
{
    extern const Event RejectedInserts;
    extern const Event DelayedInserts;
    extern const Event DelayedInsertsMilliseconds;
    extern const Event DuplicatedInsertedBlocks;
}

namespace CurrentMetrics
{
    extern const Metric DelayedInserts;
    extern const Metric BackgroundMovePoolTask;
}

namespace DB
{

namespace ErrorCodes
{
    extern const int NO_SUCH_DATA_PART;
    extern const int NOT_IMPLEMENTED;
    extern const int DIRECTORY_ALREADY_EXISTS;
    extern const int TOO_MANY_UNEXPECTED_DATA_PARTS;
    extern const int DUPLICATE_DATA_PART;
    extern const int NO_SUCH_COLUMN_IN_TABLE;
    extern const int LOGICAL_ERROR;
    extern const int ILLEGAL_COLUMN;
    extern const int CORRUPTED_DATA;
    extern const int BAD_TYPE_OF_FIELD;
    extern const int BAD_ARGUMENTS;
    extern const int INVALID_PARTITION_VALUE;
    extern const int METADATA_MISMATCH;
    extern const int PART_IS_TEMPORARILY_LOCKED;
    extern const int TOO_MANY_PARTS;
    extern const int INCOMPATIBLE_COLUMNS;
    extern const int BAD_TTL_EXPRESSION;
    extern const int INCORRECT_FILE_NAME;
    extern const int BAD_DATA_PART_NAME;
    extern const int READONLY_SETTING;
    extern const int ABORTED;
    extern const int UNKNOWN_PART_TYPE;
    extern const int UNKNOWN_DISK;
    extern const int NOT_ENOUGH_SPACE;
    extern const int ALTER_OF_COLUMN_IS_FORBIDDEN;
    extern const int SUPPORT_IS_DISABLED;
    extern const int TOO_MANY_SIMULTANEOUS_QUERIES;
}

MergeTreeData::MergeTreeData(
    const StorageID & table_id_,
    const String & relative_data_path_,
    const StorageInMemoryMetadata & metadata_,
    ContextMutablePtr context_,
    const String & date_column_name,
    const MergingParams & merging_params_,
    std::unique_ptr<MergeTreeSettings> storage_settings_,
    bool require_part_metadata_,
    bool attach,
    BrokenPartCallback broken_part_callback_)
    : MergeTreeMetaBase(
        table_id_,
        relative_data_path_,
        metadata_,
        context_,
        date_column_name,
        merging_params_,
        std::move(storage_settings_),
        require_part_metadata_,
        attach,
        std::move(broken_part_callback_))
    , parts_mover(this)
    , replicated_fetches_throttler(std::make_shared<Throttler>(
          getSettings()->max_replicated_fetches_network_bandwidth, getContext()->getReplicatedFetchesThrottler()))
    , replicated_sends_throttler(
          std::make_shared<Throttler>(getSettings()->max_replicated_sends_network_bandwidth, getContext()->getReplicatedSendsThrottler()))
{
    const auto settings = getSettings();
    enable_metastore = settings->enable_metastore;

    if (getRelativeDataPath(IStorage::StorageLocation::MAIN).empty())
        throw Exception("MergeTree storages require data path", ErrorCodes::INCORRECT_FILE_NAME);

    /// Check sanity of MergeTreeSettings. Only when table is created.
    if (!attach)
        settings->sanityCheck(getContext()->getSettingsRef());

    MergeTreeDataFormatVersion min_format_version(0);
    if (!date_column_name.empty())
        min_format_version = MERGE_TREE_DATA_MIN_FORMAT_VERSION_WITH_CUSTOM_PARTITIONING;

    /// format_file always contained on any data path
    PathWithDisk version_file;
    /// Creating directories, if not exist.
    for (const auto & [path, disk] : getRelativeDataPathsWithDisks())
    {
        disk->createDirectories(path);
        disk->createDirectories(fs::path(path) / MergeTreeData::DETACHED_DIR_NAME);
        String current_version_file_path = fs::path(path) / MergeTreeData::FORMAT_VERSION_FILE_NAME;
        if (disk->exists(current_version_file_path))
        {
            if (!version_file.first.empty())
            {
                LOG_ERROR(log, "Duplication of version file {} and {}", fullPath(version_file.second, version_file.first), current_version_file_path);
                throw Exception("Multiple format_version.txt file", ErrorCodes::CORRUPTED_DATA);
            }
            version_file = {current_version_file_path, disk};
        }
    }

    if (enable_metastore && !metastore)
    {
        String table_metastore_path = getMetastorePath();
        bool force_meta_rebuild = (getContext()->getSettingsRef().TEST_KNOB & TEST_KNOB_FORCE_META_REBUILD);
        if (force_meta_rebuild && fs::exists(table_metastore_path))
        {
            LOG_INFO(log, "Got TEST_KNOB_FORCE_META_REBUILD, remove the existing metastore.");
            fs::remove_all(table_metastore_path);
        }
        if (!fs::exists(table_metastore_path))
        {
            LOG_DEBUG(log, "Create metastore directory {} for table {}", table_metastore_path, log_name);
            fs::create_directories(table_metastore_path);
        }
        metastore = std::make_shared<MergeTreeMeta>(table_metastore_path, log_name);
    }

    /// If not choose any
    if (version_file.first.empty())
        version_file = {fs::path(getRelativeDataPath(IStorage::StorageLocation::MAIN)) / MergeTreeData::FORMAT_VERSION_FILE_NAME, getStoragePolicy(IStorage::StorageLocation::MAIN)->getAnyDisk()};

    bool version_file_exists = version_file.second->exists(version_file.first);

    // When data path or file not exists, ignore the format_version check
    if (!attach || !version_file_exists)
    {
        format_version = min_format_version;
        auto buf = version_file.second->writeFile(version_file.first);
        writeIntText(format_version.toUnderType(), *buf);
        if (getContext()->getSettingsRef().fsync_metadata)
            buf->sync();
    }
    else
    {
        auto buf = version_file.second->readFile(version_file.first);
        UInt32 read_format_version;
        readIntText(read_format_version, *buf);
        format_version = read_format_version;
        if (!buf->eof())
            throw Exception("Bad version file: " + fullPath(version_file.second, version_file.first), ErrorCodes::CORRUPTED_DATA);
    }

    if (format_version < min_format_version)
    {
        if (min_format_version == MERGE_TREE_DATA_MIN_FORMAT_VERSION_WITH_CUSTOM_PARTITIONING.toUnderType())
            throw Exception(
                "MergeTree data format version on disk doesn't support custom partitioning",
                ErrorCodes::METADATA_MISMATCH);
    }
    String reason;
    if (!canUsePolymorphicParts(*settings, &reason) && !reason.empty())
        LOG_WARNING(log, "{} Settings 'min_rows_for_wide_part', 'min_bytes_for_wide_part', "
            "'min_rows_for_compact_part' and 'min_bytes_for_compact_part' will be ignored.", reason);
}

MergeTreeData::~MergeTreeData()
{
    /// clean checksums cache of current storage before destroy.
    if (auto cache = getContext()->getChecksumsCache())
        cache->dropChecksumCache(getStorageUniqueID());
}

void MergeTreeData::checkStoragePolicy(const StoragePolicyPtr & new_storage_policy) const
{
    const auto old_storage_policy = getStoragePolicy(IStorage::StorageLocation::MAIN);
    old_storage_policy->checkCompatibleWith(new_storage_policy);
}


std::optional<UInt64> MergeTreeData::totalRowsByPartitionPredicateImpl(
    const SelectQueryInfo & query_info, ContextPtr local_context, const DataPartsVector & parts) const
{
    if (parts.empty())
        return 0u;
    auto metadata_snapshot = getInMemoryMetadataPtr();
    ASTPtr expression_ast;
    Block virtual_columns_block = getBlockWithVirtualPartColumns(parts, true /* one_part */);

    // Generate valid expressions for filtering
    bool valid = VirtualColumnUtils::prepareFilterBlockWithQuery(query_info.query, local_context, virtual_columns_block, expression_ast);

    PartitionPruner partition_pruner(metadata_snapshot, query_info, local_context, true /* strict */);
    if (partition_pruner.isUseless() && !valid)
        return {};

    std::unordered_set<String> part_values;
    if (valid && expression_ast)
    {
        virtual_columns_block = getBlockWithVirtualPartColumns(parts, false /* one_part */);
        VirtualColumnUtils::filterBlockWithQuery(query_info.query, virtual_columns_block, local_context, expression_ast);
        part_values = VirtualColumnUtils::extractSingleValueFromBlock<String>(virtual_columns_block, "_part");
        if (part_values.empty())
            return 0;
    }
    // At this point, empty `part_values` means all parts.

    size_t res = 0;
    for (const auto & part : parts)
    {
        if ((part_values.empty() || part_values.find(part->name) != part_values.end()) && !partition_pruner.canBePruned(*part))
            res += part->rows_count;
    }
    return res;
}

MergeTreeData::DataPartsVector MergeTreeData::getRequiredPartitions(const SelectQueryInfo & query_info, ContextPtr local_context)
{
    DataPartsVector parts = getDataPartsVector();
    if (parts.empty())
        return {};
    auto metadata_snapshot = getInMemoryMetadataPtr();
    ASTPtr expression_ast;
    Block virtual_columns_block = getBlockWithVirtualPartColumns(parts, true /* one_part */);

    // Generate valid expressions for filtering
    bool valid = VirtualColumnUtils::prepareFilterBlockWithQuery(query_info.query, local_context, virtual_columns_block, expression_ast);

    PartitionPruner partition_pruner(metadata_snapshot, query_info, local_context, true /* strict */);
    if (partition_pruner.isUseless() && !valid)
        return {};

    std::unordered_set<String> part_values;
    if (valid && expression_ast)
    {
        virtual_columns_block = getBlockWithVirtualPartColumns(parts, false /* one_part */);
        VirtualColumnUtils::filterBlockWithQuery(query_info.query, virtual_columns_block, local_context, expression_ast);
        part_values = VirtualColumnUtils::extractSingleValueFromBlock<String>(virtual_columns_block, "_part");
        if (part_values.empty())
            return {};
    }

    DataPartsVector required_parts;
    for (const auto & part : parts)
    {
        if ((part_values.empty() || part_values.find(part->name) != part_values.end()) && !partition_pruner.canBePruned(*part))
            required_parts.emplace_back(part);
    }
    return required_parts;
}

void MergeTreeData::checkColumnsValidity(const ColumnsDescription & columns) const
{
    NamesAndTypesList func_columns = getInMemoryMetadataPtr()->getFuncColumns();

    for (auto & column: columns.getAll())
    {
        /// check func columns
        for (auto & [name, type]: func_columns)
        {
            if (name == column.name)
                throw Exception("Column " + backQuoteIfNeed(column.name) + " is reserved column", ErrorCodes::ILLEGAL_COLUMN);
        }

        /// block implicit key name for MergeTree family
        if (isMapImplicitKey(column.name))
            throw Exception("Column " + backQuoteIfNeed(column.name) + " contains reserved prefix word", ErrorCodes::ILLEGAL_COLUMN);

        if (column.type && column.type->isMap())
        {
            auto escape_name = escapeForFileName(column.name + getMapSeparator());
            auto pos = escape_name.find(getMapSeparator());
            /// The name of map column should not contain map separator, which is convenient for extracting map column name from a implicit column name.
            if (pos + getMapSeparator().size() != escape_name.size())
                throw Exception(
                    ErrorCodes::ILLEGAL_COLUMN,
                    "Map column name {} is invalid because its escaped name {} contains reserved word {}",
                    backQuoteIfNeed(column.name),
                    backQuoteIfNeed(escape_name),
                    getMapSeparator());

            if (storage_settings.get()->enable_compact_map_data)
            {
                const auto & type_map = typeid_cast<const DataTypeByteMap &>(*column.type);
                if (type_map.getValueType()->lowCardinality())
                {
                    throw Exception("Column " + backQuoteIfNeed(column.name) + " compact map type not compatible with LowCardinality type, you need remove LowCardinality or disable compact map", ErrorCodes::ILLEGAL_COLUMN);
                }
            }
        }
    }
}

Int64 MergeTreeData::getMaxBlockNumber() const
{
    auto lock = lockPartsRead();

    Int64 max_block_num = 0;
    for (const DataPartPtr & part : data_parts_by_info)
        max_block_num = std::max({max_block_num, part->info.max_block, part->info.mutation});

    return max_block_num;
}

/** ----------------------- COMPATIBLE CODE BEGIN-------------------------- */
/*  compatible with old metastore. remove this later  */
bool MergeTreeData::preLoadDataParts(bool skip_sanity_checks)
{
    bool res = false;
    // try to find the old version metastore;
    String old_version_meta_path;
    auto disks = getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks();
    for (auto disk_it = disks.begin(); disk_it != disks.end(); ++disk_it)
    {
        DiskPtr disk_ptr = *disk_it;
        for (auto it = disk_ptr->iterateDirectory(getRelativeDataPath(IStorage::StorageLocation::MAIN)); it->isValid(); it->next())
        {
            if (it->name() == "catalog.db")
            {
                old_version_meta_path = disk_ptr->getPath() + getRelativeDataPath(IStorage::StorageLocation::MAIN);
                break;
            }
        }
    }

    if (!old_version_meta_path.empty())
    {
        MetaStorePtr old_metastore = std::make_shared<MergeTreeMeta>(old_version_meta_path, log_name + "_OLD");
        if (old_metastore->checkMetaReady())
        {
            try
            {
                Stopwatch stopwatch;
                LOG_DEBUG(log, "[Compatible] Loading parts from old metastore in path {}", old_version_meta_path);
                /// try to load parts from old metastore.
                auto [loaded_parts, unloaded_parts] = old_metastore->loadPartFromMetastore(*this);
                /// add parts which loaded from metastore.
                auto part_lock = lockParts();
                data_parts_indexes.clear();
                for (auto & part : loaded_parts)
                {
                    part->setState(DataPartState::Committed);
                    if (!data_parts_indexes.insert(part).second)
                        throw Exception("Part " + part->name + " already exists", ErrorCodes::DUPLICATE_DATA_PART);

                    addPartContributionToDataVolume(part);
                }
                /// load from file system if there are unloaded parts or wals.
                if (!unloaded_parts.empty())
                {
                    LOG_DEBUG(log, "[Compatible] Loading {} unloaded parts from file system.", unloaded_parts.size());
                    loadPartsFromFileSystem(unloaded_parts, {}, skip_sanity_checks, part_lock);
                }

                /// parts have been loaded, sync the metadata to new metastore.
                for (auto it=data_parts_indexes.begin(); it!=data_parts_indexes.end(); it++)
                    metastore->addPart(*this, *it);

                LOG_DEBUG(log, "[Compatible] Takes {}ms to load parts from old metastore.", stopwatch.elapsedMilliseconds());

                res = true;
            }
            catch (...)
            {
                LOG_DEBUG(log, "Exception occurs during loading parts from old metastore.");
                tryLogCurrentException(__PRETTY_FUNCTION__);
            }
        }

        old_metastore->closeMetastore();

        String path_to_remove = old_version_meta_path + "catalog.db";
        LOG_DEBUG(log, "[Compatible] Removing old metastore path {}", path_to_remove);
        fs::remove_all(path_to_remove);
    }

    return res;
}
/*  -----------------------  COMPATIBLE CODE END -------------------------- */

/// if metastore is enabled and ready for use, it will try to load parts from metastore first. Other wise, load parts from file system.
void MergeTreeData::loadDataParts(bool skip_sanity_checks)
{
    bool force_meta_rebuild = (getContext()->getSettingsRef().TEST_KNOB & TEST_KNOB_FORCE_META_REBUILD);

    /** ----------------------- COMPATIBLE CODE BEGIN-------------------------- */
    bool loaded_from_old_metastore = false;
    if (!force_meta_rebuild && metastore)
    {
        /// try to load parts from metastore with old metastore format. will transform metadata from old version to new after loading.
        loaded_from_old_metastore = preLoadDataParts(skip_sanity_checks);
    }
    /*  -----------------------  COMPATIBLE CODE END -------------------------- */
    if (loaded_from_old_metastore)
    {
        /// has already sync metadata into new metastore. set metastore status.
        metastore->setMetastoreStatus(*this);
    }
    /// if metastore is enabled and has not been loaded as old metastore format, try load from metastore normally
    else if (metastore && metastore->checkMetastoreStatus(*this))
    {
        LOG_DEBUG(log, "Start loading data parts from metastore.");
        Stopwatch stopwatch;
        /// Load part from metastore;
        auto [loaded_parts, unloaded_parts] = metastore->loadFromMetastore(*this);
        /// get wal logs if any
        auto wals_with_disks = metastore->getWriteAheadLogs(*this);

        /// add parts which loaded from metastore.
        auto part_lock = lockParts();
        data_parts_indexes.clear();
        for (auto & part : loaded_parts)
        {
            part->setState(DataPartState::Committed);
            if (!data_parts_indexes.insert(part).second)
                throw Exception("Part " + part->name + " already exists", ErrorCodes::DUPLICATE_DATA_PART);

            addPartContributionToDataVolume(part);
        }

        /// load projections immediately after loading data parts.
        metastore->loadProjections(*this);

        /// load from file system if there are unloaded parts or wals.
        if (!unloaded_parts.empty() || !wals_with_disks.empty())
        {
            LOG_DEBUG(log, "Loading {} unloaded parts from file system.", unloaded_parts.size());
            loadPartsFromFileSystem(unloaded_parts, wals_with_disks, skip_sanity_checks, part_lock);
        }
        LOG_DEBUG(log, "Takes {}ms to load parts from metastore.", stopwatch.elapsedMilliseconds());
    }
    else
    {
        LOG_DEBUG(log, "Start loading data parts from filesystem.");
        Stopwatch stopwatch;

        const auto settings = getSettings();

        PartNamesWithDisks part_names_with_disks;
        PartNamesWithDisks wal_name_with_disks;

        auto disks = getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks();

        /// Only check if user did touch storage configuration for this table.
        if (!getStoragePolicy(IStorage::StorageLocation::MAIN)->isDefaultPolicy() && !skip_sanity_checks)
        {
            /// Check extra parts at different disks, in order to not allow to miss data parts at undefined disks.
            std::unordered_set<String> defined_disk_names;
            for (const auto & disk_ptr : disks)
                defined_disk_names.insert(disk_ptr->getName());

            for (const auto & [disk_name, disk] : getContext()->getDisksMap())
            {
                if (defined_disk_names.count(disk_name) == 0 && disk->exists(getRelativeDataPath(IStorage::StorageLocation::MAIN)))
                {
                    for (const auto it = disk->iterateDirectory(getRelativeDataPath(IStorage::StorageLocation::MAIN)); it->isValid(); it->next())
                    {
                        MergeTreePartInfo part_info;
                        if (MergeTreePartInfo::tryParsePartName(it->name(), &part_info, format_version))
                            throw Exception("Part " + backQuote(it->name()) + " was found on disk " + backQuote(disk_name) + " which is not defined in the storage policy", ErrorCodes::UNKNOWN_DISK);
                    }
                }
            }
        }

        /// Reversed order to load part from low priority disks firstly.
        /// Used for keep part on low priority disk if duplication found
        for (auto disk_it = disks.rbegin(); disk_it != disks.rend(); ++disk_it)
        {
            auto disk_ptr = *disk_it;
            for (auto it = disk_ptr->iterateDirectory(getRelativeDataPath(IStorage::StorageLocation::MAIN)); it->isValid(); it->next())
            {
                /// Skip temporary directories, file 'format_version.txt' and directory 'detached'.
                if (startsWith(it->name(), "tmp") || it->name() == MergeTreeData::FORMAT_VERSION_FILE_NAME || it->name() == MergeTreeData::DETACHED_DIR_NAME)
                    continue;

                if (!startsWith(it->name(), MergeTreeWriteAheadLog::WAL_FILE_NAME))
                    part_names_with_disks.emplace_back(it->name(), disk_ptr);
                else if (it->name() == MergeTreeWriteAheadLog::DEFAULT_WAL_FILE_NAME && settings->in_memory_parts_enable_wal)
                {
                    wal_name_with_disks.emplace_back(it->name(), disk_ptr);
                }
                else if (settings->in_memory_parts_enable_wal)
                {
                    wal_name_with_disks.emplace_back(it->name(), disk_ptr);
                }
            }
        }

        auto part_lock = lockParts();
        data_parts_indexes.clear();

        if (part_names_with_disks.empty() && wal_name_with_disks.empty())
        {
            LOG_DEBUG(log, "There are no data parts");
            return;
        }

        /// build up metastore
        if (enable_metastore && metastore)
            metastore->cleanMetastore();

        loadPartsFromFileSystem(part_names_with_disks, wal_name_with_disks, skip_sanity_checks, part_lock);

        if (metastore)
            metastore->setMetastoreStatus(*this);

        LOG_DEBUG(log, "Takes {}ms to load parts from file system.", stopwatch.elapsedMilliseconds());
    }

    /// Delete from the set of current parts those parts that are covered by another part (those parts that
    /// were merged), but that for some reason are still not deleted from the filesystem.
    /// Deletion of files will be performed later in the clearOldParts() method.

    if (data_parts_indexes.size() >= 2)
    {
        /// Now all parts are committed, so data_parts_by_state_and_info == committed_parts_range
        auto prev_jt = data_parts_by_state_and_info.begin();
        auto curr_jt = std::next(prev_jt);

        auto deactivate_part = [&] (DataPartIteratorByStateAndInfo it)
        {
            (*it)->remove_time.store((*it)->modification_time, std::memory_order_relaxed);
            modifyPartState(it, DataPartState::Outdated);
            removePartContributionToDataVolume(*it);
        };

        (*prev_jt)->assertState({DataPartState::Committed});

        while (curr_jt != data_parts_by_state_and_info.end() && (*curr_jt)->getState() == DataPartState::Committed)
        {
            /// Don't consider data parts belonging to different partitions.
            if ((*curr_jt)->info.partition_id != (*prev_jt)->info.partition_id)
            {
                ++prev_jt;
                ++curr_jt;
                continue;
            }

            if ((*curr_jt)->contains(**prev_jt))
            {
                deactivate_part(prev_jt);
                prev_jt = curr_jt;
                ++curr_jt;
            }
            else if ((*prev_jt)->contains(**curr_jt))
            {
                auto next = std::next(curr_jt);
                deactivate_part(curr_jt);
                curr_jt = next;
            }
            else
            {
                ++prev_jt;
                ++curr_jt;
            }
        }
    }

    calculateColumnSizesImpl();


    LOG_DEBUG(log, "Loaded data parts ({} items)", data_parts_indexes.size());
}

void MergeTreeData::loadPartsFromFileSystem(PartNamesWithDisks part_names_with_disks, PartNamesWithDisks wal_with_disks, bool skip_sanity_checks, DataPartsLock & part_lock)
{
    auto metadata_snapshot = getInMemoryMetadataPtr();
    const auto settings = getSettings();

    MutableDataPartsVector parts_from_wal;
    for (auto & [wal_name, disk_ptr] : wal_with_disks)
    {
        MergeTreeWriteAheadLog wal(*this, disk_ptr, wal_name);
        for (auto && part : wal.restore(metadata_snapshot, getContext()))
            parts_from_wal.push_back(std::move(part));

        /// add wal info into metastore if enabled.
        if (metastore)
            addWriteAheadLog(wal_name, disk_ptr);
    }

    /// Parallel loading of data parts.
    size_t num_threads = std::min(size_t(settings->max_part_loading_threads), part_names_with_disks.size());

    std::mutex mutex;

    DataPartsVector broken_parts_to_detach;
    size_t suspicious_broken_parts = 0;

    std::atomic<bool> has_adaptive_parts = false;
    std::atomic<bool> has_non_adaptive_parts = false;

    ThreadPool pool(num_threads);

    for (auto & part_names_with_disk : part_names_with_disks)
    {
        pool.scheduleOrThrowOnError([&]
        {
            const auto & part_name = part_names_with_disk.first;
            const auto part_disk_ptr = part_names_with_disk.second;

            MergeTreePartInfo part_info;
            if (!MergeTreePartInfo::tryParsePartName(part_name, &part_info, format_version))
                return;

            auto single_disk_volume = std::make_shared<SingleDiskVolume>("volume_" + part_name, part_disk_ptr, 0);
            auto part = createPart(part_name, part_info, single_disk_volume, part_name);
            bool broken = false;

            String part_path = fs::path(getRelativeDataPath(IStorage::StorageLocation::MAIN)) / part_name;
            String marker_path = fs::path(part_path) / IMergeTreeDataPart::DELETE_ON_DESTROY_MARKER_FILE_NAME;
            if (part_disk_ptr->exists(marker_path))
            {
                LOG_WARNING(log, "Detaching stale part {}{}, which should have been deleted after a move. That can only happen after unclean restart of ClickHouse after move of a part having an operation blocking that stale copy of part.", getFullPathOnDisk(IStorage::StorageLocation::MAIN, part_disk_ptr), part_name);
                std::lock_guard loading_lock(mutex);
                broken_parts_to_detach.push_back(part);
                ++suspicious_broken_parts;
                return;
            }

            try
            {
                part->loadColumnsChecksumsIndexes(require_part_metadata, true);
            }
            catch (const Exception & e)
            {
                /// Don't count the part as broken if there is not enough memory to load it.
                /// In fact, there can be many similar situations.
                /// But it is OK, because there is a safety guard against deleting too many parts.
                if (isNotEnoughMemoryErrorCode(e.code()))
                    throw;

                broken = true;
                tryLogCurrentException(__PRETTY_FUNCTION__);
            }
            catch (...)
            {
                broken = true;
                tryLogCurrentException(__PRETTY_FUNCTION__);
            }

            /// Ignore broken parts that can appear as a result of hard server restart.
            if (broken)
            {
                LOG_ERROR(log, "Detaching broken part {}{}. If it happened after update, it is likely because of backward incompability. You need to resolve this manually", getFullPathOnDisk(IStorage::StorageLocation::MAIN, part_disk_ptr), part_name);
                std::lock_guard loading_lock(mutex);
                broken_parts_to_detach.push_back(part);
                ++suspicious_broken_parts;

                return;
            }
            if (!part->index_granularity_info.is_adaptive)
                has_non_adaptive_parts.store(true, std::memory_order_relaxed);
            else
                has_adaptive_parts.store(true, std::memory_order_relaxed);

            part->modification_time = part_disk_ptr->getLastModified(fs::path(getRelativeDataPath(IStorage::StorageLocation::MAIN)) / part_name).epochTime();
            /// Assume that all parts are Committed, covered parts will be detected and marked as Outdated later
            part->setState(DataPartState::Committed);

            std::lock_guard loading_lock(mutex);
            if (!data_parts_indexes.insert(part).second)
                throw Exception("Part " + part->name + " already exists", ErrorCodes::DUPLICATE_DATA_PART);

            /// add part into metastore if enabled.
            if (metastore)
                metastore->addPart(*this, part);

            addPartContributionToDataVolume(part);
        });
    }

    pool.wait();

    for (auto & part : parts_from_wal)
    {
        if (getActiveContainingPart(part->info, DataPartState::Committed, part_lock))
            continue;

        part->modification_time = time(nullptr);
        /// Assume that all parts are Committed, covered parts will be detected and marked as Outdated later
        part->setState(DataPartState::Committed);

        if (!data_parts_indexes.insert(part).second)
            throw Exception("Part " + part->name + " already exists", ErrorCodes::DUPLICATE_DATA_PART);

        addPartContributionToDataVolume(part);
    }

    if (has_non_adaptive_parts && has_adaptive_parts && !settings->enable_mixed_granularity_parts)
        throw Exception("Table contains parts with adaptive and non adaptive marks, but `setting enable_mixed_granularity_parts` is disabled", ErrorCodes::LOGICAL_ERROR);

    has_non_adaptive_index_granularity_parts = has_non_adaptive_parts;

    if (suspicious_broken_parts > settings->max_suspicious_broken_parts && !skip_sanity_checks)
        throw Exception("Suspiciously many (" + toString(suspicious_broken_parts) + ") broken parts to remove.",
            ErrorCodes::TOO_MANY_UNEXPECTED_DATA_PARTS);

    for (auto & part : broken_parts_to_detach)
        part->renameToDetached("broken-on-start"); /// detached parts must not have '_' in prefixes

    /// Delete from the set of current parts those parts that are covered by another part (those parts that
    /// were merged), but that for some reason are still not deleted from the filesystem.
    /// Deletion of files will be performed later in the clearOldParts() method.

    if (data_parts_indexes.size() >= 2)
    {
        /// Now all parts are committed, so data_parts_by_state_and_info == committed_parts_range
        auto prev_jt = data_parts_by_state_and_info.begin();
        auto curr_jt = std::next(prev_jt);

        auto deactivate_part = [&] (DataPartIteratorByStateAndInfo it)
        {
            (*it)->remove_time.store((*it)->modification_time, std::memory_order_relaxed);
            modifyPartState(it, DataPartState::Outdated);
            removePartContributionToDataVolume(*it);
        };

        (*prev_jt)->assertState({DataPartState::Committed});

        while (curr_jt != data_parts_by_state_and_info.end() && (*curr_jt)->getState() == DataPartState::Committed)
        {
            /// Don't consider data parts belonging to different partitions.
            if ((*curr_jt)->info.partition_id != (*prev_jt)->info.partition_id)
            {
                ++prev_jt;
                ++curr_jt;
                continue;
            }

            if ((*curr_jt)->contains(**prev_jt))
            {
                deactivate_part(prev_jt);
                prev_jt = curr_jt;
                ++curr_jt;
            }
            else if ((*prev_jt)->contains(**curr_jt))
            {
                auto next = std::next(curr_jt);
                deactivate_part(curr_jt);
                curr_jt = next;
            }
            else
            {
                ++prev_jt;
                ++curr_jt;
            }
        }
    }

    calculateColumnSizesImpl();


    LOG_DEBUG(log, "Loaded data parts ({} items)", data_parts_indexes.size());
}


/// Is the part directory old.
/// True if its modification time and the modification time of all files inside it is less then threshold.
/// (Only files on the first level of nesting are considered).
static bool isOldPartDirectory(const DiskPtr & disk, const String & directory_path, time_t threshold)
{
    if (disk->getLastModified(directory_path).epochTime() >= threshold)
        return false;

    for (auto it = disk->iterateDirectory(directory_path); it->isValid(); it->next())
        if (disk->getLastModified(it->path()).epochTime() >= threshold)
            return false;

    return true;
}


void MergeTreeData::clearOldTemporaryDirectories(ssize_t custom_directories_lifetime_seconds)
{
    /// If the method is already called from another thread, then we don't need to do anything.
    std::unique_lock lock(clear_old_temporary_directories_mutex, std::defer_lock);
    if (!lock.try_lock())
        return;

    const auto settings = getSettings();
    time_t current_time = time(nullptr);
    ssize_t deadline = (custom_directories_lifetime_seconds >= 0)
        ? current_time - custom_directories_lifetime_seconds
        : current_time - settings->temporary_directories_lifetime.totalSeconds();

    /// Delete temporary directories older than a day.
    for (const auto & [path, disk] : getRelativeDataPathsWithDisks())
    {
        for (auto it = disk->iterateDirectory(path); it->isValid(); it->next())
        {
            if (startsWith(it->name(), "tmp_"))
            {
                try
                {
                    if (disk->isDirectory(it->path()) && isOldPartDirectory(disk, it->path(), deadline))
                    {
                        LOG_WARNING(log, "Removing temporary directory {}", fullPath(disk, it->path()));
                        disk->removeRecursive(it->path());
                    }
                }
                /// see getModificationTime()
                catch (const ErrnoException & e)
                {
                    if (e.getErrno() == ENOENT)
                    {
                        /// If the file is already deleted, do nothing.
                    }
                    else
                        throw;
                }
                catch (const fs::filesystem_error & e)
                {
                    if (e.code() == std::errc::no_such_file_or_directory)
                    {
                        /// If the file is already deleted, do nothing.
                    }
                    else
                        throw;
                }
            }
        }
    }
}


MergeTreeData::DataPartsVector MergeTreeData::grabOldParts(bool force)
{
    DataPartsVector res;

    /// If the method is already called from another thread, then we don't need to do anything.
    std::unique_lock lock(grab_old_parts_mutex, std::defer_lock);
    if (!lock.try_lock())
        return res;

    time_t now = time(nullptr);
    std::vector<DataPartIteratorByStateAndInfo> parts_to_delete;

    {
        auto parts_lock = lockParts();

        auto outdated_parts_range = getDataPartsStateRange(DataPartState::Outdated);
        for (auto it = outdated_parts_range.begin(); it != outdated_parts_range.end(); ++it)
        {
            const DataPartPtr & part = *it;

            auto part_remove_time = part->remove_time.load(std::memory_order_relaxed);

            if (part.unique() && /// Grab only parts that are not used by anyone (SELECTs for example).
                ((part_remove_time < now &&
                now - part_remove_time > getSettings()->old_parts_lifetime.totalSeconds()) || force
                || isInMemoryPart(part))) /// Remove in-memory parts immediately to not store excessive data in RAM
            {
                parts_to_delete.emplace_back(it);
            }
        }

        res.reserve(parts_to_delete.size());
        for (const auto & it_to_delete : parts_to_delete)
        {
            res.emplace_back(*it_to_delete);
            modifyPartState(it_to_delete, DataPartState::Deleting);
        }
    }

    if (!res.empty())
        LOG_TRACE(log, "Found {} old parts to remove.", res.size());

    return res;
}


void MergeTreeData::rollbackDeletingParts(const MergeTreeData::DataPartsVector & parts)
{
    auto lock = lockParts();
    for (const auto & part : parts)
    {
        /// We should modify it under data_parts_mutex
        part->assertState({DataPartState::Deleting});
        modifyPartState(part, DataPartState::Outdated);
    }
}

void MergeTreeData::removePartsFinally(const MergeTreeData::DataPartsVector & parts)
{
    {
        auto lock = lockParts();

        /// TODO: use data_parts iterators instead of pointers
        for (const auto & part : parts)
        {
            auto it = data_parts_by_info.find(part->info);
            if (it == data_parts_by_info.end())
                throw Exception("Deleting data part " + part->name + " doesn't exist", ErrorCodes::LOGICAL_ERROR);

            (*it)->assertState({DataPartState::Deleting});

            data_parts_indexes.erase(it);

            if (metastore)
                metastore->dropPart(*this, part);
        }
    }

    /// Data parts is still alive (since DataPartsVector holds shared_ptrs) and contain useful metainformation for logging
    /// NOTE: There is no need to log parts deletion somewhere else, all deleting parts pass through this function and pass away

    auto table_id = getStorageID();
    if (auto part_log = getContext()->getPartLog(table_id.database_name))
    {
        PartLogElement part_log_elem;

        part_log_elem.event_type = PartLogElement::REMOVE_PART;
        part_log_elem.event_time = time(nullptr);
        part_log_elem.duration_ms = 0; //-V1048

        part_log_elem.database_name = table_id.database_name;
        part_log_elem.table_name = table_id.table_name;

        for (const auto & part : parts)
        {
            part_log_elem.partition_id = part->info.partition_id;
            part_log_elem.part_name = part->name;
            part_log_elem.bytes_compressed_on_disk = part->getBytesOnDisk();
            part_log_elem.rows = part->rows_count;

            part_log->add(part_log_elem);
        }
    }
}

void MergeTreeData::removePartsFinallyUnsafe(const DataPartsVector & parts, DataPartsLock * acquired_lock)
{
    auto lock = (acquired_lock) ? DataPartsLock() : lockParts();

    for (auto & part : parts)
    {
        auto it = data_parts_by_info.find(part->info);
        if (it == data_parts_by_info.end())
            throw Exception("Deleting data part " + part->name + " doesn't exist", ErrorCodes::LOGICAL_ERROR);

        data_parts_indexes.erase(it);
    }
}

void MergeTreeData::clearOldPartsFromFilesystem(bool force)
{
    DataPartsVector parts_to_remove = grabOldParts(force);
    clearPartsFromFilesystem(parts_to_remove);
    removePartsFinally(parts_to_remove);

    /// This is needed to close files to avoid they reside on disk after being deleted.
    /// NOTE: we can drop files from cache more selectively but this is good enough.
    if (!parts_to_remove.empty())
        getContext()->dropMMappedFileCache();
}

void MergeTreeData::clearPartsFromFilesystem(const DataPartsVector & parts_to_remove)
{
    const auto settings = getSettings();
    if (parts_to_remove.size() > 1 && settings->max_part_removal_threads > 1 && parts_to_remove.size() > settings->concurrent_part_removal_threshold)
    {
        /// Parallel parts removal.

        size_t num_threads = std::min<size_t>(settings->max_part_removal_threads, parts_to_remove.size());
        ThreadPool pool(num_threads);

        /// NOTE: Under heavy system load you may get "Cannot schedule a task" from ThreadPool.
        for (const DataPartPtr & part : parts_to_remove)
        {
            pool.scheduleOrThrowOnError([&, thread_group = CurrentThread::getGroup()]
            {
                SCOPE_EXIT_SAFE(
                    if (thread_group)
                        CurrentThread::detachQueryIfNotDetached();
                );
                if (thread_group)
                    CurrentThread::attachTo(thread_group);

                LOG_DEBUG(log, "Removing part from filesystem {}", part->name);
                part->remove();
            });
        }

        pool.wait();
    }
    else
    {
        for (const DataPartPtr & part : parts_to_remove)
        {
            LOG_DEBUG(log, "Removing part from filesystem {}", part->name);
            part->remove();
        }
    }
}

void MergeTreeData::clearOldWriteAheadLogs()
{
    DataPartsVector parts = getDataPartsVector();
    std::vector<std::pair<Int64, Int64>> all_block_numbers_on_disk;
    std::vector<std::pair<Int64, Int64>> block_numbers_on_disk;

    for (const auto & part : parts)
        if (part->isStoredOnDisk())
            all_block_numbers_on_disk.emplace_back(part->info.min_block, part->info.max_block);

    if (all_block_numbers_on_disk.empty())
        return;

    std::sort(all_block_numbers_on_disk.begin(), all_block_numbers_on_disk.end());
    block_numbers_on_disk.push_back(all_block_numbers_on_disk[0]);
    for (size_t i = 1; i < all_block_numbers_on_disk.size(); ++i)
    {
        if (all_block_numbers_on_disk[i].first == all_block_numbers_on_disk[i - 1].second + 1)
            block_numbers_on_disk.back().second = all_block_numbers_on_disk[i].second;
        else
            block_numbers_on_disk.push_back(all_block_numbers_on_disk[i]);
    }

    auto is_range_on_disk = [&block_numbers_on_disk](Int64 min_block, Int64 max_block)
    {
        auto lower = std::lower_bound(block_numbers_on_disk.begin(), block_numbers_on_disk.end(), std::make_pair(min_block, Int64(-1L)));
        if (lower != block_numbers_on_disk.end() && min_block >= lower->first && max_block <= lower->second)
            return true;

        if (lower != block_numbers_on_disk.begin())
        {
            --lower;
            if (min_block >= lower->first && max_block <= lower->second)
                return true;
        }

        return false;
    };

    auto disks = getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks();
    for (auto disk_it = disks.rbegin(); disk_it != disks.rend(); ++disk_it)
    {
        auto disk_ptr = *disk_it;
        for (auto it = disk_ptr->iterateDirectory(getRelativeDataPath(IStorage::StorageLocation::MAIN)); it->isValid(); it->next())
        {
            auto min_max_block_number = MergeTreeWriteAheadLog::tryParseMinMaxBlockNumber(it->name());
            if (min_max_block_number && is_range_on_disk(min_max_block_number->first, min_max_block_number->second))
            {
                LOG_DEBUG(log, "Removing from filesystem the outdated WAL file " + it->name());
                disk_ptr->removeFile(getRelativeDataPath(IStorage::StorageLocation::MAIN) + it->name());
                removeWriteAheadLog(it->name());
            }
        }
    }
}

void MergeTreeData::clearEmptyParts()
{
    if (!getSettings()->remove_empty_parts)
        return;

    auto parts = getDataPartsVector();
    for (const auto & part : parts)
    {
        if (part->rows_count == 0)
            dropPartNoWaitNoThrow(part->name);
    }
}

void MergeTreeData::rename(const String & new_table_path, const StorageID & new_table_id)
{
    auto disks = getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks();

    for (const auto & disk : disks)
    {
        if (disk->exists(new_table_path))
            throw Exception{"Target path already exists: " + fullPath(disk, new_table_path), ErrorCodes::DIRECTORY_ALREADY_EXISTS};
    }

    for (const auto & disk : disks)
    {
        auto new_table_path_parent = parentPath(new_table_path);
        disk->createDirectories(new_table_path_parent);
        disk->moveDirectory(getRelativeDataPath(IStorage::StorageLocation::MAIN), new_table_path);
    }

    /// move metastore dir if exists. make sure metastore closed before moving
    if (metastore)
    {
        metastore->closeMetastore();
        String new_metastore_path = getContext()->getMetastorePath() + new_table_path;
        if (fs::exists(new_metastore_path))
            throw Exception("Metastore path already exists : " + new_metastore_path, ErrorCodes::DIRECTORY_ALREADY_EXISTS);
        fs::create_directories(new_metastore_path);
        fs::rename(fs::path(getMetastorePath()) / "catalog.db", fs::path(new_metastore_path) / "catalog.db");
    }

    if (!getStorageID().hasUUID())
        getContext()->dropCaches();

    setRelativeDataPath(StorageLocation::MAIN, new_table_path);
    renameInMemory(new_table_id);

    /// reopen metastore to make table be ready for use.
    if (enable_metastore)
        metastore = std::make_shared<MergeTreeMeta>(getMetastorePath(), log_name);
}

void MergeTreeData::dropAllData()
{
    LOG_TRACE(log, "dropAllData: waiting for locks.");

    auto lock = lockParts();

    LOG_TRACE(log, "dropAllData: removing data from memory.");

    DataPartsVector all_parts(data_parts_by_info.begin(), data_parts_by_info.end());

    data_parts_indexes.clear();

    column_sizes.clear();

    /// Tables in atomic databases have UUID and stored in persistent locations.
    /// No need to drop caches (that are keyed by filesystem path) because collision is not possible.
    if (!getStorageID().hasUUID())
        getContext()->dropCaches();

    LOG_TRACE(log, "dropAllData: removing data from filesystem.");

    /// Removing of each data part before recursive removal of directory is to speed-up removal, because there will be less number of syscalls.
    clearPartsFromFilesystem(all_parts);

    for (const auto & [path, disk] : getRelativeDataPathsWithDisks())
    {
        try
        {
            disk->removeRecursive(path);
        }
        catch (const fs::filesystem_error & e)
        {
            if (e.code() == std::errc::no_such_file_or_directory)
            {
                /// If the file is already deleted, log the error message and do nothing.
                tryLogCurrentException(__PRETTY_FUNCTION__);
            }
            else
                throw;
        }
    }

    /// remove metastore if exists. make sure the metastore is closed before remove metadata.
    String metastore_path = getContext()->getMetastorePath() + getRelativeDataPath(IStorage::StorageLocation::MAIN);
    if (fs::exists(metastore_path))
    {
        if (metastore)
            metastore->closeMetastore();
        fs::remove_all(getContext()->getMetastorePath() + getRelativeDataPath(IStorage::StorageLocation::MAIN));
    }

    setDataVolume(0, 0, 0);

    LOG_TRACE(log, "dropAllData: done.");
}

void MergeTreeData::dropIfEmpty()
{
    LOG_TRACE(log, "dropIfEmpty");

    auto lock = lockParts();

    if (!data_parts_by_info.empty())
        return;

    try
    {
        for (const auto & [path, disk] : getRelativeDataPathsWithDisks())
        {
            /// Non recursive, exception is thrown if there are more files.
            disk->removeFileIfExists(fs::path(path) / MergeTreeData::FORMAT_VERSION_FILE_NAME);
            disk->removeDirectory(fs::path(path) / MergeTreeData::DETACHED_DIR_NAME);
            disk->removeDirectory(path);
        }
    }
    catch (...)
    {
        // On unsuccessful creation of ReplicatedMergeTree table with multidisk configuration some files may not exist.
        tryLogCurrentException(__PRETTY_FUNCTION__);
    }
}

namespace
{

/// Conversion that is allowed for serializable key (primary key, sorting key).
/// Key should be serialized in the same way after conversion.
/// NOTE: The list is not complete.
bool isSafeForKeyConversion(const IDataType * from, const IDataType * to)
{
    if (from->getName() == to->getName())
        return true;

    /// Enums are serialized in partition key as numbers - so conversion from Enum to number is Ok.
    /// But only for types of identical width because they are serialized as binary in minmax index.
    /// But not from number to Enum because Enum does not necessarily represents all numbers.

    if (const auto * from_enum8 = typeid_cast<const DataTypeEnum8 *>(from))
    {
        if (const auto * to_enum8 = typeid_cast<const DataTypeEnum8 *>(to))
            return to_enum8->contains(*from_enum8);
        if (typeid_cast<const DataTypeInt8 *>(to))
            return true;    // NOLINT
        return false;
    }

    if (const auto * from_enum16 = typeid_cast<const DataTypeEnum16 *>(from))
    {
        if (const auto * to_enum16 = typeid_cast<const DataTypeEnum16 *>(to))
            return to_enum16->contains(*from_enum16);
        if (typeid_cast<const DataTypeInt16 *>(to))
            return true;    // NOLINT
        return false;
    }

    if (const auto * from_lc = typeid_cast<const DataTypeLowCardinality *>(from))
        return from_lc->getDictionaryType()->equals(*to);

    if (const auto * to_lc = typeid_cast<const DataTypeLowCardinality *>(to))
        return to_lc->getDictionaryType()->equals(*from);

    return false;
}

/// Special check for alters of VersionedCollapsingMergeTree version column
void checkVersionColumnTypesConversion(const IDataType * old_type, const IDataType * new_type, const String column_name)
{
    /// Check new type can be used as version
    if (!new_type->canBeUsedAsVersion())
        throw Exception("Cannot alter version column " + backQuoteIfNeed(column_name) +
            " to type " + new_type->getName() +
            " because version column must be of an integer type or of type Date or DateTime"
            , ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);

    auto which_new_type = WhichDataType(new_type);
    auto which_old_type = WhichDataType(old_type);

    /// Check alter to different sign or float -> int and so on
    if ((which_old_type.isInt() && !which_new_type.isInt())
        || (which_old_type.isUInt() && !which_new_type.isUInt())
        || (which_old_type.isDate() && !which_new_type.isDate())
        || (which_old_type.isDate32() && !which_new_type.isDate32())
        || (which_old_type.isDateTime() && !which_new_type.isDateTime())
        || (which_old_type.isFloat() && !which_new_type.isFloat()))
    {
        throw Exception("Cannot alter version column " + backQuoteIfNeed(column_name) +
            " from type " + old_type->getName() +
            " to type " + new_type->getName() + " because new type will change sort order of version column." +
            " The only possible conversion is expansion of the number of bytes of the current type."
            , ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
    }

    /// Check alter to smaller size: UInt64 -> UInt32 and so on
    if (new_type->getSizeOfValueInMemory() < old_type->getSizeOfValueInMemory())
    {
        throw Exception("Cannot alter version column " + backQuoteIfNeed(column_name) +
            " from type " + old_type->getName() +
            " to type " + new_type->getName() + " because new type is smaller than current in the number of bytes." +
            " The only possible conversion is expansion of the number of bytes of the current type."
            , ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
    }
}

}

void MergeTreeData::checkAlterIsPossible(const AlterCommands & commands, ContextPtr local_context) const
{
    /// Check that needed transformations can be applied to the list of columns without considering type conversions.
    StorageInMemoryMetadata new_metadata = getInMemoryMetadata();
    StorageInMemoryMetadata old_metadata = getInMemoryMetadata();

    const auto & settings = local_context->getSettingsRef();

    if (!settings.allow_non_metadata_alters)
    {

        auto mutation_commands = commands.getMutationCommands(new_metadata, settings.materialize_ttl_after_modify, getContext());

        if (!mutation_commands.empty())
            throw Exception(ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN, "The following alter commands: '{}' will modify data on disk, but setting `allow_non_metadata_alters` is disabled", queryToString(mutation_commands.ast()));
    }
    commands.apply(new_metadata, getContext());

    /// Set of columns that shouldn't be altered.
    NameSet columns_alter_type_forbidden;

    /// Primary key columns can be ALTERed only if they are used in the key as-is
    /// (and not as a part of some expression) and if the ALTER only affects column metadata.
    NameSet columns_alter_type_metadata_only;

    /// Columns to check that the type change is safe for partition key.
    NameSet columns_alter_type_check_safe_for_partition;

    if (old_metadata.hasPartitionKey())
    {
        /// Forbid altering columns inside partition key expressions because it can change partition ID format.
        auto partition_key_expr = old_metadata.getPartitionKey().expression;
        for (const auto & action : partition_key_expr->getActions())
        {
            for (const auto * child : action.node->children)
                columns_alter_type_forbidden.insert(child->result_name);
        }

        /// But allow to alter columns without expressions under certain condition.
        for (const String & col : partition_key_expr->getRequiredColumns())
            columns_alter_type_check_safe_for_partition.insert(col);
    }

    for (const auto & index : old_metadata.getSecondaryIndices())
    {
        for (const String & col : index.expression->getRequiredColumns())
            columns_alter_type_forbidden.insert(col);
    }

    if (old_metadata.hasSortingKey())
    {
        auto sorting_key_expr = old_metadata.getSortingKey().expression;
        for (const auto & action : sorting_key_expr->getActions())
        {
            for (const auto * child : action.node->children)
                columns_alter_type_forbidden.insert(child->result_name);
        }
        for (const String & col : sorting_key_expr->getRequiredColumns())
            columns_alter_type_metadata_only.insert(col);

        /// We don't process sample_by_ast separately because it must be among the primary key columns
        /// and we don't process primary_key_expr separately because it is a prefix of sorting_key_expr.
    }
    if (!merging_params.sign_column.empty())
        columns_alter_type_forbidden.insert(merging_params.sign_column);

    /// All of the above.
    NameSet columns_in_keys;
    columns_in_keys.insert(columns_alter_type_forbidden.begin(), columns_alter_type_forbidden.end());
    columns_in_keys.insert(columns_alter_type_metadata_only.begin(), columns_alter_type_metadata_only.end());
    columns_in_keys.insert(columns_alter_type_check_safe_for_partition.begin(), columns_alter_type_check_safe_for_partition.end());

    NameSet dropped_columns;

    std::map<String, const IDataType *> old_types;
    for (const auto & column : old_metadata.getColumns().getAllPhysical())
        old_types.emplace(column.name, column.type.get());

    NamesAndTypesList columns_to_check_conversion;
    auto name_deps = getDependentViewsByColumn(local_context);
    for (const AlterCommand & command : commands)
    {
        /// Just validate partition expression
        if (command.partition)
        {
            getPartitionIDFromQuery(command.partition, getContext());
        }

        if (command.column_name == merging_params.version_column)
        {
            /// Some type changes for version column is allowed despite it's a part of sorting key
            if (command.type == AlterCommand::MODIFY_COLUMN)
            {
                const IDataType * new_type = command.data_type.get();
                const IDataType * old_type = old_types[command.column_name];

                if (new_type)
                    checkVersionColumnTypesConversion(old_type, new_type, command.column_name);

                /// No other checks required
                continue;
            }
            else if (command.type == AlterCommand::DROP_COLUMN)
            {
                throw Exception(
                    "Trying to ALTER DROP version " + backQuoteIfNeed(command.column_name) + " column",
                    ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
            }
            else if (command.type == AlterCommand::RENAME_COLUMN)
            {
                throw Exception(
                    "Trying to ALTER RENAME version " + backQuoteIfNeed(command.column_name) + " column",
                    ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
            }
        }

        if (command.type == AlterCommand::MODIFY_ORDER_BY && !is_custom_partitioned)
        {
            throw Exception(
                "ALTER MODIFY ORDER BY is not supported for default-partitioned tables created with the old syntax",
                ErrorCodes::BAD_ARGUMENTS);
        }
        if (command.type == AlterCommand::MODIFY_TTL && !is_custom_partitioned)
        {
            throw Exception(
                "ALTER MODIFY TTL is not supported for default-partitioned tables created with the old syntax",
                ErrorCodes::BAD_ARGUMENTS);
        }
        if (command.type == AlterCommand::MODIFY_SAMPLE_BY)
        {
            if (!is_custom_partitioned)
                throw Exception(
                    "ALTER MODIFY SAMPLE BY is not supported for default-partitioned tables created with the old syntax",
                    ErrorCodes::BAD_ARGUMENTS);

            checkSampleExpression(new_metadata, getSettings()->compatibility_allow_sampling_expression_not_in_primary_key);
        }
        if (command.type == AlterCommand::ADD_INDEX && !is_custom_partitioned)
        {
            throw Exception(
                "ALTER ADD INDEX is not supported for tables with the old syntax",
                ErrorCodes::BAD_ARGUMENTS);
        }
        if (command.type == AlterCommand::ADD_PROJECTION && !is_custom_partitioned)
        {
            throw Exception(
                "ALTER ADD PROJECTION is not supported for tables with the old syntax",
                ErrorCodes::BAD_ARGUMENTS);
        }
        if (command.type == AlterCommand::RENAME_COLUMN)
        {
            if (columns_in_keys.count(command.column_name))
            {
                throw Exception(
                    "Trying to ALTER RENAME key " + backQuoteIfNeed(command.column_name) + " column which is a part of key expression",
                    ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
            }
        }
        else if (command.type == AlterCommand::DROP_COLUMN)
        {
            if (columns_in_keys.count(command.column_name))
            {
                throw Exception(
                    "Trying to ALTER DROP key " + backQuoteIfNeed(command.column_name) + " column which is a part of key expression",
                    ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
            }

            if (!command.clear)
            {
                const auto & deps_mv = name_deps[command.column_name];
                if (!deps_mv.empty())
                {
                    throw Exception(
                        "Trying to ALTER DROP column " + backQuoteIfNeed(command.column_name) + " which is referenced by materialized view "
                            + toString(deps_mv),
                        ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
                }
            }

            if (command.partition_predicate && !supportsClearColumnInPartitionWhere())
                throw Exception(ErrorCodes::NOT_IMPLEMENTED, "CLEAR COLUMN IN PARTITION WHERE is not supported by storage {}", getName());

            dropped_columns.emplace(command.column_name);
        }
        else if (command.isRequireMutationStage(getInMemoryMetadata()))
        {
            /// This alter will override data on disk. Let's check that it doesn't
            /// modify immutable column.
            if (columns_alter_type_forbidden.count(command.column_name))
                throw Exception("ALTER of key column " + backQuoteIfNeed(command.column_name) + " is forbidden",
                    ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);

            if (command.type == AlterCommand::MODIFY_COLUMN)
            {
                if (columns_alter_type_check_safe_for_partition.count(command.column_name))
                {
                    auto it = old_types.find(command.column_name);

                    assert(it != old_types.end());
                    if (!isSafeForKeyConversion(it->second, command.data_type.get()))
                        throw Exception("ALTER of partition key column " + backQuoteIfNeed(command.column_name) + " from type "
                                + it->second->getName() + " to type " + command.data_type->getName()
                                + " is not safe because it can change the representation of partition key",
                            ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
                }

                if (columns_alter_type_metadata_only.count(command.column_name))
                {
                    auto it = old_types.find(command.column_name);
                    assert(it != old_types.end());
                    if (!isSafeForKeyConversion(it->second, command.data_type.get()))
                        throw Exception("ALTER of key column " + backQuoteIfNeed(command.column_name) + " from type "
                                    + it->second->getName() + " to type " + command.data_type->getName()
                                    + " is not safe because it can change the representation of primary key",
                            ErrorCodes::ALTER_OF_COLUMN_IS_FORBIDDEN);
                }

                if (old_metadata.getColumns().has(command.column_name))
                {
                    columns_to_check_conversion.push_back(
                        new_metadata.getColumns().getPhysical(command.column_name));
                }
            }
        }
    }

    checkProperties(new_metadata, old_metadata);
    checkTTLExpressions(new_metadata, old_metadata);

    if (!columns_to_check_conversion.empty())
    {
        auto old_header = old_metadata.getSampleBlock();
        performRequiredConversions(old_header, columns_to_check_conversion, getContext());
    }

    if (old_metadata.hasSettingsChanges())
    {
        const auto current_changes = old_metadata.getSettingsChanges()->as<const ASTSetQuery &>().changes;
        const auto & new_changes = new_metadata.settings_changes->as<const ASTSetQuery &>().changes;
        for (const auto & changed_setting : new_changes)
        {
            const auto & setting_name = changed_setting.name;
            const auto & new_value = changed_setting.value;
            MergeTreeSettings::checkCanSet(setting_name, new_value);
            const Field * current_value = current_changes.tryGet(setting_name);

            if ((!current_value || *current_value != new_value)
                && MergeTreeSettings::isReadonlySetting(setting_name))
            {
                throw Exception{"Setting '" + setting_name + "' is readonly for storage '" + getName() + "'",
                                 ErrorCodes::READONLY_SETTING};
            }

            if (!current_value && MergeTreeSettings::isPartFormatSetting(setting_name))
            {
                MergeTreeSettings copy = *getSettings();
                copy.applyChange(changed_setting);
                String reason;
                if (!canUsePolymorphicParts(copy, &reason) && !reason.empty())
                    throw Exception("Can't change settings. Reason: " + reason, ErrorCodes::NOT_IMPLEMENTED);
            }

            if (setting_name == "storage_policy")
                checkStoragePolicy(getContext()->getStoragePolicy(new_value.safeGet<String>()));
        }

        /// Check if it is safe to reset the settings
        for (const auto & current_setting : current_changes)
        {
            const auto & setting_name = current_setting.name;
            const Field * new_value = new_changes.tryGet(setting_name);
            /// Prevent unsetting readonly setting
            if (MergeTreeSettings::isReadonlySetting(setting_name) && !new_value)
            {
                throw Exception{"Setting '" + setting_name + "' is readonly for storage '" + getName() + "'",
                                ErrorCodes::READONLY_SETTING};
            }

            if (MergeTreeSettings::isPartFormatSetting(setting_name) && !new_value)
            {
                /// Use default settings + new and check if doesn't affect part format settings
                auto copy = getDefaultSettings();
                copy->applyChanges(new_changes);
                String reason;
                if (!canUsePolymorphicParts(*copy, &reason) && !reason.empty())
                    throw Exception("Can't change settings. Reason: " + reason, ErrorCodes::NOT_IMPLEMENTED);
            }

        }
    }

    for (const auto & part : getDataPartsVector())
    {
        bool at_least_one_column_rest = false;
        for (const auto & column : part->getColumns())
        {
            if (!dropped_columns.count(column.name))
            {
                at_least_one_column_rest = true;
                break;
            }
        }
        if (!at_least_one_column_rest)
        {
            std::string postfix;
            if (dropped_columns.size() > 1)
                postfix = "s";
            throw Exception(ErrorCodes::BAD_ARGUMENTS,
                "Cannot drop or clear column{} '{}', because all columns in part '{}' will be removed from disk. Empty parts are not allowed", postfix, boost::algorithm::join(dropped_columns, ", "), part->name);
        }
    }
}


void MergeTreeData::checkMutationIsPossible(const MutationCommands & commands, const Settings & /*settings*/) const
{
    int num_fast_deletes = 0;
    for (auto & command : commands)
        num_fast_deletes += command.type == MutationCommand::Type::FAST_DELETE;
    if (num_fast_deletes > 1)
        throw Exception(ErrorCodes::NOT_IMPLEMENTED, "It's not allowed to execute multiple FASTDELETE commands");
    if (num_fast_deletes && commands.size() != 1)
        throw Exception(ErrorCodes::NOT_IMPLEMENTED, "It's not allowed to execute FASTDELETE with other commands");
}

void MergeTreeData::changeSettings(
        const ASTPtr & new_settings,
        TableLockHolder & /* table_lock_holder */)
{
    if (new_settings)
    {
        bool has_storage_policy_changed = false;

        const auto & new_changes = new_settings->as<const ASTSetQuery &>().changes;

        for (const auto & change : new_changes)
        {
            if (change.name == "storage_policy")
            {
                StoragePolicyPtr new_storage_policy = getContext()->getStoragePolicy(change.value.safeGet<String>());
                StoragePolicyPtr old_storage_policy = getStoragePolicy(IStorage::StorageLocation::MAIN);

                /// StoragePolicy of different version or name is guaranteed to have different pointer
                if (new_storage_policy != old_storage_policy)
                {
                    checkStoragePolicy(new_storage_policy);

                    std::unordered_set<String> all_diff_disk_names;
                    for (const auto & disk : new_storage_policy->getDisks())
                        all_diff_disk_names.insert(disk->getName());
                    for (const auto & disk : old_storage_policy->getDisks())
                        all_diff_disk_names.erase(disk->getName());

                    for (const String & disk_name : all_diff_disk_names)
                    {
                        auto disk = new_storage_policy->getDiskByName(disk_name);
                        if (disk->exists(getRelativeDataPath(IStorage::StorageLocation::MAIN)))
                            throw Exception("New storage policy contain disks which already contain data of a table with the same name", ErrorCodes::LOGICAL_ERROR);
                    }

                    for (const String & disk_name : all_diff_disk_names)
                    {
                        auto disk = new_storage_policy->getDiskByName(disk_name);
                        disk->createDirectories(getRelativeDataPath(IStorage::StorageLocation::MAIN));
                        disk->createDirectories(fs::path(getRelativeDataPath(IStorage::StorageLocation::MAIN)) / MergeTreeData::DETACHED_DIR_NAME);
                    }
                    /// FIXME how would that be done while reloading configuration???

                    has_storage_policy_changed = true;
                }
            }
        }

        /// Reset to default settings before applying existing.
        auto copy = getDefaultSettings();
        copy->applyChanges(new_changes);
        copy->sanityCheck(getContext()->getSettingsRef());

        storage_settings.set(std::move(copy));
        StorageInMemoryMetadata new_metadata = getInMemoryMetadata();
        new_metadata.setSettingsChanges(new_settings);
        setInMemoryMetadata(new_metadata);

        if (has_storage_policy_changed)
            startBackgroundMovesIfNeeded();
    }
}

void MergeTreeData::PartsTemporaryRename::addPart(const String & old_name, const String & new_name)
{
    old_and_new_names.push_back({old_name, new_name});
    for (const auto & [path, disk] : storage.getRelativeDataPathsWithDisks())
    {
        for (auto it = disk->iterateDirectory(fs::path(path) / source_dir); it->isValid(); it->next())
        {
            if (it->name() == old_name)
            {
                old_part_name_to_path_and_disk[old_name] = {path, disk};
                break;
            }
        }
    }
}

void MergeTreeData::PartsTemporaryRename::tryRenameAll()
{
    renamed = true;
    for (size_t i = 0; i < old_and_new_names.size(); ++i)
    {
        try
        {
            const auto & [old_name, new_name] = old_and_new_names[i];
            if (old_name.empty() || new_name.empty())
                throw DB::Exception("Empty part name. Most likely it's a bug.", ErrorCodes::INCORRECT_FILE_NAME);
            const auto & [path, disk] = old_part_name_to_path_and_disk[old_name];
            const auto full_path = fs::path(path) / source_dir; /// for old_name
            disk->moveFile(fs::path(full_path) / old_name, fs::path(full_path) / new_name);
        }
        catch (...)
        {
            old_and_new_names.resize(i);
            LOG_WARNING(storage.log, "Cannot rename parts to perform operation on them: {}", getCurrentExceptionMessage(false));
            throw;
        }
    }
}

MergeTreeData::PartsTemporaryRename::~PartsTemporaryRename()
{
    // TODO what if server had crashed before this destructor was called?
    if (!renamed)
        return;
    for (const auto & [old_name, new_name] : old_and_new_names)
    {
        if (old_name.empty())
            continue;

        try
        {
            const auto & [path, disk] = old_part_name_to_path_and_disk[old_name];
            const String full_path = fs::path(path) / source_dir; /// for old_name
            disk->moveFile(fs::path(full_path) / new_name, fs::path(full_path) / old_name);
        }
        catch (...)
        {
            tryLogCurrentException(__PRETTY_FUNCTION__);
        }
    }
}

MergeTreeData::DataPartsVector MergeTreeData::getActivePartsToReplace(
    const MergeTreePartInfo & new_part_info,
    const String & new_part_name,
    DataPartPtr & out_covering_part,
    DataPartsLock & /* data_parts_lock */) const
{
    /// Parts contained in the part are consecutive in data_parts, intersecting the insertion place for the part itself.
    auto it_middle = data_parts_by_state_and_info.lower_bound(DataPartStateAndInfo{DataPartState::Committed, new_part_info});
    auto committed_parts_range = getDataPartsStateRange(DataPartState::Committed);

    /// Go to the left.
    DataPartIteratorByStateAndInfo begin = it_middle;
    while (begin != committed_parts_range.begin())
    {
        auto prev = std::prev(begin);

        if (!new_part_info.contains((*prev)->info))
        {
            if ((*prev)->info.contains(new_part_info))
            {
                out_covering_part = *prev;
                return {};
            }

            if (!new_part_info.isDisjoint((*prev)->info))
                throw Exception(ErrorCodes::LOGICAL_ERROR, "Part {} intersects previous part {}. It is a bug.",
                                new_part_name, (*prev)->getNameWithState());

            break;
        }

        begin = prev;
    }

    /// Go to the right.
    DataPartIteratorByStateAndInfo end = it_middle;
    while (end != committed_parts_range.end())
    {
        if ((*end)->info == new_part_info)
            throw Exception(ErrorCodes::LOGICAL_ERROR, "Unexpected duplicate part {}. It is a bug.", (*end)->getNameWithState());

        if (!new_part_info.contains((*end)->info))
        {
            if ((*end)->info.contains(new_part_info))
            {
                out_covering_part = *end;
                return {};
            }

            if (!new_part_info.isDisjoint((*end)->info))
                throw Exception(ErrorCodes::LOGICAL_ERROR, "Part {} intersects next part {}. It is a bug.",
                                new_part_name, (*end)->getNameWithState());

            break;
        }

        ++end;
    }

    return DataPartsVector{begin, end};
}


bool MergeTreeData::renameTempPartAndAdd(MutableDataPartPtr & part, SimpleIncrement * increment, Transaction * out_transaction, MergeTreeDeduplicationLog * deduplication_log)
{
    if (out_transaction && &out_transaction->data != this)
        throw Exception("MergeTreeData::Transaction for one table cannot be used with another. It is a bug.",
            ErrorCodes::LOGICAL_ERROR);

    DataPartsVector covered_parts;
    {
        auto lock = lockParts();
        if (!renameTempPartAndReplace(part, increment, out_transaction, lock, &covered_parts, deduplication_log))
            return false;
    }
    if (!covered_parts.empty())
        throw Exception("Added part " + part->name + " covers " + toString(covered_parts.size())
            + " existing part(s) (including " + covered_parts[0]->name + ")", ErrorCodes::LOGICAL_ERROR);

    return true;
}


bool MergeTreeData::renameTempPartAndReplace(
    MutableDataPartPtr & part, SimpleIncrement * increment, Transaction * out_transaction,
    DataPartsLock & lock, DataPartsVector * out_covered_parts, MergeTreeDeduplicationLog * deduplication_log)
{
    if (out_transaction && &out_transaction->data != this)
        throw Exception("MergeTreeData::Transaction for one table cannot be used with another. It is a bug.",
            ErrorCodes::LOGICAL_ERROR);

    part->assertState({DataPartState::Temporary});

    MergeTreePartInfo part_info = part->info;
    String part_name;

    if (!part->info.isFakeDropRangePart())
    {
        if (DataPartPtr existing_part_in_partition = getAnyPartInPartition(part->info.partition_id, lock))
        {
            if (part->partition.value != existing_part_in_partition->partition.value)
                throw Exception(
                    "Partition value mismatch between two parts with the same partition ID. Existing part: "
                    + existing_part_in_partition->name + ", newly added part: " + part->name,
                    ErrorCodes::CORRUPTED_DATA);
        }
    }

    /** It is important that obtaining new block number and adding that block to parts set is done atomically.
      * Otherwise there is race condition - merge of blocks could happen in interval that doesn't yet contain new part.
      */
    if (increment)
    {
        part_info.min_block = part_info.max_block = increment->get();
        part_info.mutation = 0; /// it's equal to min_block by default
        part_name = part->getNewName(part_info);
    }
    else /// Parts from ReplicatedMergeTree already have names
        part_name = part->name;

    LOG_DEBUG(log, "Renaming temporary part {} to {}.", part->relative_path, part_name);

    if (auto it_duplicate = data_parts_by_info.find(part_info); it_duplicate != data_parts_by_info.end())
    {
        String message = "Part " + (*it_duplicate)->getNameWithState() + " already exists";

        if ((*it_duplicate)->checkState({DataPartState::Outdated, DataPartState::Deleting}))
            throw Exception(message + ", but it will be deleted soon", ErrorCodes::PART_IS_TEMPORARILY_LOCKED);

        throw Exception(message, ErrorCodes::DUPLICATE_DATA_PART);
    }

    DataPartPtr covering_part;
    DataPartsVector covered_parts = getActivePartsToReplace(part_info, part_name, covering_part, lock);
    DataPartsVector covered_parts_in_memory;

    if (covering_part)
    {
        LOG_WARNING(log, "Tried to add obsolete part {} covered by {}", part_name, covering_part->getNameWithState());
        return false;
    }

    /// Deduplication log used only from non-replicated MergeTree. Replicated
    /// tables have their own mechanism. We try to deduplicate at such deep
    /// level, because only here we know real part name which is required for
    /// deduplication.
    if (deduplication_log)
    {
        String block_id = part->getZeroLevelPartBlockID();
        auto res = deduplication_log->addPart(block_id, part_info);
        if (!res.second)
        {
            ProfileEvents::increment(ProfileEvents::DuplicatedInsertedBlocks);
            LOG_INFO(log, "Block with ID {} already exists as part {}; ignoring it", block_id, res.first.getPartName());
            return false;
        }
    }

    /// All checks are passed. Now we can rename the part on disk.
    /// So, we maintain invariant: if a non-temporary part in filesystem then it is in data_parts
    ///
    /// If out_transaction is null, we commit the part to the active set immediately, else add it to the transaction.
    part->name = part_name;
    part->info = part_info;
    part->is_temp = false;
    part->setState(DataPartState::PreCommitted);
    part->renameTo(part_name, true);

    auto part_it = data_parts_indexes.insert(part).first;
    if (metastore && !isInMemoryPart(part))
        metastore->addPart(*this, part);

    if (out_transaction)
    {
        out_transaction->precommitted_parts.insert(part);
    }
    else
    {
        size_t reduce_bytes = 0;
        size_t reduce_rows = 0;
        size_t reduce_parts = 0;
        auto current_time = time(nullptr);
        for (const DataPartPtr & covered_part : covered_parts)
        {
            covered_part->remove_time.store(current_time, std::memory_order_relaxed);
            modifyPartState(covered_part, DataPartState::Outdated);
            removePartContributionToColumnSizes(covered_part);
            reduce_bytes += covered_part->getBytesOnDisk();
            reduce_rows += covered_part->rows_count;
            ++reduce_parts;
        }

        decreaseDataVolume(reduce_bytes, reduce_rows, reduce_parts);

        modifyPartState(part_it, DataPartState::Committed);
        addPartContributionToColumnSizes(part);
        addPartContributionToDataVolume(part);
    }

    auto part_in_memory = asInMemoryPart(part);
    if (part_in_memory && getSettings()->in_memory_parts_enable_wal)
    {
        auto wal = getWriteAheadLog();
        wal->addPart(part_in_memory);
    }

    if (out_covered_parts)
    {
        for (DataPartPtr & covered_part : covered_parts)
            out_covered_parts->emplace_back(std::move(covered_part));
    }

    return true;
}

MergeTreeData::DataPartsVector MergeTreeData::renameTempPartAndReplace(
    MutableDataPartPtr & part, SimpleIncrement * increment, Transaction * out_transaction, MergeTreeDeduplicationLog * deduplication_log)
{
    if (out_transaction && &out_transaction->data != this)
        throw Exception("MergeTreeData::Transaction for one table cannot be used with another. It is a bug.",
            ErrorCodes::LOGICAL_ERROR);

    DataPartsVector covered_parts;
    {
        auto lock = lockParts();
        renameTempPartAndReplace(part, increment, out_transaction, lock, &covered_parts, deduplication_log);
    }
    return covered_parts;
}

bool MergeTreeData::renameTempPartInDetachDirecotry(MutableDataPartPtr & new_part, const DataPartPtr & old_part)
{
    new_part->assertState({IMergeTreeDataPart::State::Temporary});
    LOG_DEBUG(
        log,
        "Renaming temporary part {} to {}.",
        new_part->relative_path,
        String(MergeTreeData::DETACHED_DIR_NAME).append("/").append(new_part->name));

    auto new_part_tmp_path = new_part->getFullRelativePath();

    // rename temporary encoding part
    new_part->is_temp = false;
    new_part->renameTo(fs::path(MergeTreeData::DETACHED_DIR_NAME) / new_part->name, true);

    try
    {
        // remove old part in disk
        auto disk = old_part->volume->getDisk();
        auto old_path = old_part->getFullRelativePath();
        LOG_DEBUG(log, "Deleting the old part: {}", old_path);
        if (disk->exists(old_path))
            disk->removeRecursive(old_path);
    }
    catch(...)
    {
        new_part->is_temp = true;
        new_part->renameTo(new_part_tmp_path, true);

        throw Exception("Fail to remove the old detached part " + old_part->relative_path, ErrorCodes::LOGICAL_ERROR);
    }

    return true;
}

void MergeTreeData::removePartsFromWorkingSet(const MergeTreeData::DataPartsVector & remove, bool clear_without_timeout, DataPartsLock & /*acquired_lock*/)
{
    auto remove_time = clear_without_timeout ? 0 : time(nullptr);

    for (const DataPartPtr & part : remove)
    {
        if (part->getState() == IMergeTreeDataPart::State::Committed)
        {
            removePartContributionToColumnSizes(part);
            removePartContributionToDataVolume(part);
        }

        if (part->getState() == IMergeTreeDataPart::State::Committed || clear_without_timeout)
            part->remove_time.store(remove_time, std::memory_order_relaxed);

        if (part->getState() != IMergeTreeDataPart::State::Outdated)
            modifyPartState(part, IMergeTreeDataPart::State::Outdated);

        if (isInMemoryPart(part) && getSettings()->in_memory_parts_enable_wal)
            getWriteAheadLog()->dropPart(part->name);
    }
}

void MergeTreeData::removePartsFromWorkingSetImmediatelyAndSetTemporaryState(const DataPartsVector & remove)
{
    auto lock = lockParts();

    for (const auto & part : remove)
    {
        auto it_part = data_parts_by_info.find(part->info);
        if (it_part == data_parts_by_info.end())
            throw Exception("Part " + part->getNameWithState() + " not found in data_parts", ErrorCodes::LOGICAL_ERROR);

        modifyPartState(part, IMergeTreeDataPart::State::Temporary);
        /// Erase immediately
        data_parts_indexes.erase(it_part);

        if (metastore)
            metastore->dropPart(*this, part);
    }
}

void MergeTreeData::removePartsFromWorkingSet(const DataPartsVector & remove, bool clear_without_timeout, DataPartsLock * acquired_lock)
{
    auto lock = (acquired_lock) ? DataPartsLock() : lockParts();

    for (const auto & part : remove)
    {
        if (!data_parts_by_info.count(part->info))
            throw Exception("Part " + part->getNameWithState() + " not found in data_parts", ErrorCodes::LOGICAL_ERROR);

        part->assertState({DataPartState::PreCommitted, DataPartState::Committed, DataPartState::Outdated});
    }

    removePartsFromWorkingSet(remove, clear_without_timeout, lock);
}

MergeTreeData::DataPartsVector MergeTreeData::removePartsInRangeFromWorkingSet(const MergeTreePartInfo & drop_range, bool clear_without_timeout,
                                                                               DataPartsLock & lock)
{
    DataPartsVector parts_to_remove;

    if (drop_range.min_block > drop_range.max_block)
        throw Exception(ErrorCodes::LOGICAL_ERROR, "Invalid drop range: {}", drop_range.getPartName());

    auto partition_range = getDataPartsPartitionRange(drop_range.partition_id);

    for (const DataPartPtr & part : partition_range)
    {
        if (part->info.partition_id != drop_range.partition_id)
            throw Exception("Unexpected partition_id of part " + part->name + ". This is a bug.", ErrorCodes::LOGICAL_ERROR);

        /// It's a DROP PART and it's already executed by fetching some covering part
        bool is_drop_part = !drop_range.isFakeDropRangePart() && drop_range.min_block;

        if (is_drop_part && (part->info.min_block != drop_range.min_block || part->info.max_block != drop_range.max_block || part->info.getMutationVersion() != drop_range.getMutationVersion()))
        {
            /// Why we check only min and max blocks here without checking merge
            /// level? It's a tricky situation which can happen on a stale
            /// replica. For example, we have parts all_1_1_0, all_2_2_0 and
            /// all_3_3_0. Fast replica assign some merges (OPTIMIZE FINAL or
            /// TTL) all_2_2_0 -> all_2_2_1 -> all_2_2_2. So it has set of parts
            /// all_1_1_0, all_2_2_2 and all_3_3_0. After that it decides to
            /// drop part all_2_2_2. Now set of parts is all_1_1_0 and
            /// all_3_3_0. Now fast replica assign merge all_1_1_0 + all_3_3_0
            /// to all_1_3_1 and finishes it. Slow replica pulls the queue and
            /// have two contradictory tasks -- drop all_2_2_2 and merge/fetch
            /// all_1_3_1. If this replica will fetch all_1_3_1 first and then tries
            /// to drop all_2_2_2 after that it will receive the LOGICAL ERROR.
            /// So here we just check that all_1_3_1 covers blocks from drop
            /// all_2_2_2.
            ///
            bool is_covered_by_min_max_block = part->info.min_block <= drop_range.min_block && part->info.max_block >= drop_range.max_block && part->info.getMutationVersion() >= drop_range.getMutationVersion();

            if (is_covered_by_min_max_block)
            {
                LOG_INFO(log, "Skipping drop range for part {} because covering part {} already exists", drop_range.getPartName(), part->name);
                return {};
            }
        }

        if (part->info.min_block < drop_range.min_block)
        {
            if (drop_range.min_block <= part->info.max_block)
            {
                /// Intersect left border
                throw Exception(ErrorCodes::LOGICAL_ERROR, "Unexpected merged part {} intersecting drop range {}",
                                part->name, drop_range.getPartName());
            }

            continue;
        }

        /// Stop on new parts
        if (part->info.min_block > drop_range.max_block)
            break;

        if (part->info.min_block <= drop_range.max_block && drop_range.max_block < part->info.max_block)
        {
            /// Intersect right border
            throw Exception(ErrorCodes::LOGICAL_ERROR, "Unexpected merged part {} intersecting drop range {}",
                            part->name, drop_range.getPartName());
        }

        if (part->getState() != DataPartState::Deleting)
            parts_to_remove.emplace_back(part);
    }

    removePartsFromWorkingSet(parts_to_remove, clear_without_timeout, lock);

    return parts_to_remove;
}

void MergeTreeData::forgetPartAndMoveToDetached(const MergeTreeData::DataPartPtr & part_to_detach, const String & prefix, bool
restore_covered)
{
    LOG_INFO(log, "Renaming {} to {}{} and forgetting it.", part_to_detach->relative_path, prefix, part_to_detach->name);

    auto lock = lockParts();

    auto it_part = data_parts_by_info.find(part_to_detach->info);
    if (it_part == data_parts_by_info.end())
        throw Exception("No such data part " + part_to_detach->getNameWithState(), ErrorCodes::NO_SUCH_DATA_PART);

    /// What if part_to_detach is a reference to *it_part? Make a new owner just in case.
    DataPartPtr part = *it_part;

    if (part->getState() == DataPartState::Committed)
    {
        removePartContributionToDataVolume(part);
        removePartContributionToColumnSizes(part);
    }
    modifyPartState(it_part, DataPartState::Deleting);

    part->renameToDetached(prefix);

    data_parts_indexes.erase(it_part);
    if (metastore)
        metastore->dropPart(*this, part_to_detach);

    if (restore_covered && part->info.level == 0)
    {
        LOG_WARNING(log, "Will not recover parts covered by zero-level part {}", part->name);
        return;
    }

    if (restore_covered)
    {
        Strings restored;
        bool error = false;
        String error_parts;

        Int64 pos = part->info.min_block;

        auto is_appropriate_state = [] (DataPartState state)
        {
            return state == DataPartState::Committed || state == DataPartState::Outdated;
        };

        auto update_error = [&] (DataPartIteratorByInfo it)
        {
            error = true;
            error_parts += (*it)->getNameWithState() + " ";
        };

        auto it_middle = data_parts_by_info.lower_bound(part->info);

        /// Restore the leftmost part covered by the part
        if (it_middle != data_parts_by_info.begin())
        {
            auto it = std::prev(it_middle);

            if (part->contains(**it) && is_appropriate_state((*it)->getState()))
            {
                /// Maybe, we must consider part level somehow
                if ((*it)->info.min_block != part->info.min_block)
                    update_error(it);

                if ((*it)->getState() != DataPartState::Committed)
                {
                    addPartContributionToColumnSizes(*it);
                    addPartContributionToDataVolume(*it);
                    modifyPartState(it, DataPartState::Committed); // iterator is not invalidated here
                }

                pos = (*it)->info.max_block + 1;
                restored.push_back((*it)->name);
            }
            else
                update_error(it);
        }
        else
            error = true;

        /// Restore "right" parts
        for (auto it = it_middle; it != data_parts_by_info.end() && part->contains(**it); ++it)
        {
            if ((*it)->info.min_block < pos)
                continue;

            if (!is_appropriate_state((*it)->getState()))
            {
                update_error(it);
                continue;
            }

            if ((*it)->info.min_block > pos)
                update_error(it);

            if ((*it)->getState() != DataPartState::Committed)
            {
                addPartContributionToColumnSizes(*it);
                addPartContributionToDataVolume(*it);
                modifyPartState(it, DataPartState::Committed);
            }

            pos = (*it)->info.max_block + 1;
            restored.push_back((*it)->name);
        }

        if (pos != part->info.max_block + 1)
            error = true;

        for (const String & name : restored)
        {
            LOG_INFO(log, "Activated part {}", name);
        }

        if (error)
        {
            LOG_ERROR(log, "The set of parts restored in place of {} looks incomplete. There might or might not be a data loss.{}", part->name, (error_parts.empty() ? "" : " Suspicious parts: " + error_parts));
        }
    }
}


void MergeTreeData::tryRemovePartImmediately(DataPartPtr && part)
{
    DataPartPtr part_to_delete;
    {
        auto lock = lockParts();

        LOG_TRACE(log, "Trying to immediately remove part {}", part->getNameWithState());

        auto it = data_parts_by_info.find(part->info);
        if (it == data_parts_by_info.end() || (*it).get() != part.get())
            throw Exception("Part " + part->name + " doesn't exist", ErrorCodes::LOGICAL_ERROR);

        part.reset();

        if (!((*it)->getState() == DataPartState::Outdated && it->unique()))
            return;

        modifyPartState(it, DataPartState::Deleting);
        part_to_delete = *it;
    }

    try
    {
        part_to_delete->remove();
    }
    catch (...)
    {
        rollbackDeletingParts({part_to_delete});
        throw;
    }

    removePartsFinally({part_to_delete});
    LOG_TRACE(log, "Removed part {}", part_to_delete->name);
}


size_t MergeTreeData::getTotalActiveSizeInBytes() const
{
    return total_active_size_bytes.load(std::memory_order_acquire);
}


size_t MergeTreeData::getTotalActiveSizeInRows() const
{
    return total_active_size_rows.load(std::memory_order_acquire);
}


size_t MergeTreeData::getPartsCount() const
{
    return total_active_size_parts.load(std::memory_order_acquire);
}


size_t MergeTreeData::getMaxPartsCountForPartitionWithState(DataPartState state) const
{
    auto lock = lockPartsRead();

    size_t res = 0;
    size_t cur_count = 0;
    const String * cur_partition_id = nullptr;

    for (const auto & part : getDataPartsStateRange(state))
    {
        if (cur_partition_id && part->info.partition_id == *cur_partition_id)
        {
            ++cur_count;
        }
        else
        {
            cur_partition_id = &part->info.partition_id;
            cur_count = 1;
        }

        res = std::max(res, cur_count);
    }

    return res;
}


size_t MergeTreeData::getMaxPartsCountForPartition() const
{
    return getMaxPartsCountForPartitionWithState(DataPartState::Committed);
}


size_t MergeTreeData::getMaxInactivePartsCountForPartition() const
{
    return getMaxPartsCountForPartitionWithState(DataPartState::Outdated);
}


std::optional<Int64> MergeTreeData::getMinPartDataVersion() const
{
    auto lock = lockPartsRead();

    std::optional<Int64> result;
    for (const auto & part : getDataPartsStateRange(DataPartState::Committed))
    {
        if (!result || *result > part->info.getDataVersion())
            result = part->info.getDataVersion();
    }

    return result;
}


void MergeTreeData::delayInsertOrThrowIfNeeded(Poco::Event * until) const
{
    const auto settings = getSettings();
    const size_t parts_count_in_total = getPartsCount();
    if (parts_count_in_total >= settings->max_parts_in_total)
    {
        ProfileEvents::increment(ProfileEvents::RejectedInserts);
        throw Exception("Too many parts (" + toString(parts_count_in_total) + ") in all partitions in total. This indicates wrong choice of partition key. The threshold can be modified with 'max_parts_in_total' setting in <merge_tree> element in config.xml or with per-table setting.", ErrorCodes::TOO_MANY_PARTS);
    }

    size_t parts_count_in_partition = getMaxPartsCountForPartition();
    ssize_t k_inactive = -1;
    if (settings->inactive_parts_to_throw_insert > 0 || settings->inactive_parts_to_delay_insert > 0)
    {
        size_t inactive_parts_count_in_partition = getMaxInactivePartsCountForPartition();
        if (settings->inactive_parts_to_throw_insert > 0 && inactive_parts_count_in_partition >= settings->inactive_parts_to_throw_insert)
        {
            ProfileEvents::increment(ProfileEvents::RejectedInserts);
            throw Exception(
                ErrorCodes::TOO_MANY_PARTS,
                "Too many inactive parts ({}). Parts cleaning are processing significantly slower than inserts",
                inactive_parts_count_in_partition);
        }
        k_inactive = ssize_t(inactive_parts_count_in_partition) - ssize_t(settings->inactive_parts_to_delay_insert);
    }

    if (parts_count_in_partition >= settings->parts_to_throw_insert)
    {
        ProfileEvents::increment(ProfileEvents::RejectedInserts);
        throw Exception(
            ErrorCodes::TOO_MANY_PARTS,
            "Too many parts ({}). Merges are processing significantly slower than inserts",
            parts_count_in_partition);
    }

    if (k_inactive < 0 && parts_count_in_partition < settings->parts_to_delay_insert)
        return;

    const ssize_t k_active = ssize_t(parts_count_in_partition) - ssize_t(settings->parts_to_delay_insert);
    size_t max_k;
    size_t k;
    if (k_active > k_inactive)
    {
        max_k = settings->parts_to_throw_insert - settings->parts_to_delay_insert;
        k = k_active + 1;
    }
    else
    {
        max_k = settings->inactive_parts_to_throw_insert - settings->inactive_parts_to_delay_insert;
        k = k_inactive + 1;
    }
    const double delay_milliseconds = ::pow(settings->max_delay_to_insert * 1000, static_cast<double>(k) / max_k);

    ProfileEvents::increment(ProfileEvents::DelayedInserts);
    ProfileEvents::increment(ProfileEvents::DelayedInsertsMilliseconds, delay_milliseconds);

    CurrentMetrics::Increment metric_increment(CurrentMetrics::DelayedInserts);

    LOG_INFO(log, "Delaying inserting block by {} ms. because there are {} parts", delay_milliseconds, parts_count_in_partition);

    if (until)
        until->tryWait(delay_milliseconds);
    else
        std::this_thread::sleep_for(std::chrono::milliseconds(static_cast<size_t>(delay_milliseconds)));
}

MergeTreeData::DataPartPtr MergeTreeData::getActiveContainingPart(
    const MergeTreePartInfo & part_info, MergeTreeData::DataPartState state, DataPartsLock & /*lock*/) const
{
    auto current_state_parts_range = getDataPartsStateRange(state);

    /// The part can be covered only by the previous or the next one in data_parts.
    auto it = data_parts_by_state_and_info.lower_bound(DataPartStateAndInfo{state, part_info});

    if (it != current_state_parts_range.end())
    {
        if ((*it)->info == part_info)
            return *it;
        if ((*it)->info.contains(part_info))
            return *it;
    }

    if (it != current_state_parts_range.begin())
    {
        --it;
        if ((*it)->info.contains(part_info))
            return *it;
    }

    return nullptr;
}


void MergeTreeData::swapActivePart(MergeTreeData::DataPartPtr part_copy)
{
    auto lock = lockParts();
    for (auto original_active_part : getDataPartsStateRange(DataPartState::Committed)) // NOLINT (copy is intended)
    {
        if (part_copy->name == original_active_part->name)
        {
            auto active_part_it = data_parts_by_info.find(original_active_part->info);
            if (active_part_it == data_parts_by_info.end())
                throw Exception("Cannot swap part '" + part_copy->name + "', no such active part.", ErrorCodes::NO_SUCH_DATA_PART);

            /// We do not check allow_remote_fs_zero_copy_replication here because data may be shared
            /// when allow_remote_fs_zero_copy_replication turned on and off again

            original_active_part->force_keep_shared_data = false;

            if (original_active_part->volume->getDisk()->getType() == DiskType::Type::S3)
            {
                if (part_copy->volume->getDisk()->getType() == DiskType::Type::S3
                        && original_active_part->getUniqueId() == part_copy->getUniqueId())
                {
                    /// May be when several volumes use the same S3 storage
                    original_active_part->force_keep_shared_data = true;
                }
            }

            modifyPartState(original_active_part, DataPartState::DeleteOnDestroy);
            data_parts_indexes.erase(active_part_it);
            if (metastore)
                metastore->dropPart(*this, original_active_part);

            auto part_it = data_parts_indexes.insert(part_copy).first;
            modifyPartState(part_it, DataPartState::Committed);
            if (metastore)
                metastore->addPart(*this, part_copy);
            removePartContributionToDataVolume(original_active_part);
            addPartContributionToDataVolume(part_copy);

            auto disk = original_active_part->volume->getDisk();
            String marker_path = fs::path(original_active_part->getFullRelativePath()) / IMergeTreeDataPart::DELETE_ON_DESTROY_MARKER_FILE_NAME;
            try
            {
                disk->createFile(marker_path);
            }
            catch (Poco::Exception & e)
            {
                LOG_ERROR(log, "{} (while creating DeleteOnDestroy marker: {})", e.what(), backQuote(fullPath(disk, marker_path)));
            }
            return;
        }
    }
    throw Exception("Cannot swap part '" + part_copy->name + "', no such active part.", ErrorCodes::NO_SUCH_DATA_PART);
}


MergeTreeData::DataPartPtr MergeTreeData::getActiveContainingPart(const MergeTreePartInfo & part_info) const
{
    auto lock = lockParts();
    return getActiveContainingPart(part_info, DataPartState::Committed, lock);
}

MergeTreeData::DataPartPtr MergeTreeData::getActiveContainingPart(const String & part_name) const
{
    auto part_info = MergeTreePartInfo::fromPartName(part_name, format_version);
    return getActiveContainingPart(part_info);
}

MergeTreeData::DataPartPtr MergeTreeData::getOldVersionPartIfExists(const String &part_name)
{
    auto lock = lockParts();

    auto part_info = MergeTreePartInfo::fromPartName(part_name, format_version);
    DataPartPtr covering_part;

    DataPartsVector covered_parts = getActivePartsToReplace(part_info, part_name, covering_part, lock);
    return covered_parts.size() == 1 ? covered_parts[0] : nullptr;
}

MergeTreeData::DataPartsVector MergeTreeData::getPartsByPredicate(const ASTPtr & predicate_)
{
    DataPartsVector parts = getDataPartsVector();
    parts.erase(std::remove_if(parts.begin(), parts.end(), [](auto & part) { return part->info.isFakeDropRangePart(); }), parts.end());

    // Do nothing if this table is not partition by key.
    auto metadata_snapshot = getInMemoryMetadataPtr();
    if (!metadata_snapshot->hasPartitionKey())
        return parts;

    auto & partition_key = metadata_snapshot->getPartitionKey();

    MutableColumns mutable_columns = partition_key.sample_block.cloneEmptyColumns();

    for (auto & part : parts)
    {
        auto & partition_value = part->partition.value;
        for (size_t c = 0; c < mutable_columns.size(); ++c)
        {
            if (c < partition_value.size())
                mutable_columns[c]->insert(partition_value[c]);
            else
                /// When partitions have different partition key insert default value
                /// TODO: insert default value may cause delete partition with default value
                mutable_columns[c]->insertDefault();
        }
    }

    auto block = partition_key.sample_block.cloneWithColumns(std::move(mutable_columns));

    auto predicate = predicate_->clone();
    auto syntax_result = TreeRewriter(getContext()).analyze(predicate, block.getNamesAndTypesList());
    auto actions = ExpressionAnalyzer(predicate, syntax_result, getContext()).getActions(true);
    actions->execute(block);

    /// Check the result
    if (1 != block.columns())
        throw Exception("Wrong column number of WHERE clause's calculation result", ErrorCodes::LOGICAL_ERROR);
    else if (block.getNamesAndTypesList().front().type->getName() != "UInt8")
        throw Exception("Wrong column type of WHERE clause's calculation result", ErrorCodes::LOGICAL_ERROR);

    DataPartsVector res;
    const auto & flag_column = block.getColumnsWithTypeAndName().front().column;
    for (size_t i = 0; i < parts.size(); ++i)
    {
        if (flag_column->getInt(i))
            res.push_back(parts[i]);
    }

    return res;
}

void MergeTreeData::handleClearColumnInPartitionWhere(MutationCommands & mutation_commands, const AlterCommands & alter_commands)
{
    for (const auto & alter_cmd : alter_commands)
    {
        if (alter_cmd.type == AlterCommand::DROP_COLUMN && alter_cmd.partition_predicate)
        {
            /// Reconstruct command ast
            auto command_ast = alter_cmd.ast->clone();
            command_ast->as<ASTAlterCommand>()->predicate = nullptr; /// clear predicate

            std::set<String> partitions;
            DataPartsVector parts = getPartsByPredicate(alter_cmd.partition_predicate);
            for (auto & part : parts)
                partitions.emplace(part->info.partition_id);
            for(const auto & partition: partitions)
            {
                auto partition_ast = std::make_shared<ASTPartition>();
                partition_ast->id = partition;
                MutationCommand command;
                command.type = MutationCommand::Type::DROP_COLUMN;
                command.column_name = alter_cmd.column_name;
                command.clear = true;
                command.partition = partition_ast;
                command.predicate = nullptr;

                command_ast->as<ASTAlterCommand>()->partition = partition_ast; /// set partition
                command.ast = command_ast->clone();
                mutation_commands.emplace_back(command);
            }
        }
    }
}


static void loadPartAndFixMetadataImpl(MergeTreeData::MutableDataPartPtr part)
{
    auto disk = part->volume->getDisk();
    String full_part_path = part->getFullRelativePath();

    part->loadColumnsChecksumsIndexes(false, true);
    part->modification_time = disk->getLastModified(full_part_path).epochTime();
}

void MergeTreeData::checkAlterPartitionIsPossible(
    const PartitionCommands & commands, const StorageMetadataPtr & /*metadata_snapshot*/, const Settings & settings) const
{
    for (const auto & command : commands)
    {
        if (command.type == PartitionCommand::DROP_DETACHED_PARTITION
            && !settings.allow_drop_detached)
            throw DB::Exception("Cannot execute query: DROP DETACHED PART is disabled "
                                "(see allow_drop_detached setting)", ErrorCodes::SUPPORT_IS_DISABLED);

        if (command.partition && command.type != PartitionCommand::DROP_DETACHED_PARTITION
            && command.type != PartitionCommand::DROP_PARTITION_WHERE && command.type != PartitionCommand::FETCH_PARTITION_WHERE)
        {
            if (command.part)
            {
                auto part_name = command.partition->as<ASTLiteral &>().value.safeGet<String>();
                /// We are able to parse it
                MergeTreePartInfo::fromPartName(part_name, format_version);
            }
            else
            {
                /// We are able to parse it
                getPartitionIDFromQuery(command.partition, getContext());
            }
        }
    }
}

void MergeTreeData::checkPartitionCanBeDropped(const ASTPtr & partition)
{
    const String partition_id = getPartitionIDFromQuery(partition, getContext());
    auto parts_to_remove = getDataPartsVectorInPartition(MergeTreeDataPartState::Committed, partition_id);

    UInt64 partition_size = 0;

    for (const auto & part : parts_to_remove)
        partition_size += part->getBytesOnDisk();

    auto table_id = getStorageID();
    getContext()->checkPartitionCanBeDropped(table_id.database_name, table_id.table_name, partition_size);
}

void MergeTreeData::checkPartCanBeDropped(const String & part_name)
{
    auto part = getPartIfExists(part_name, {MergeTreeDataPartState::Committed});
    if (!part)
        throw Exception(ErrorCodes::NO_SUCH_DATA_PART, "No part {} in committed state", part_name);

    auto table_id = getStorageID();
    getContext()->checkPartitionCanBeDropped(table_id.database_name, table_id.table_name, part->getBytesOnDisk());
}

void MergeTreeData::movePartitionToDisk(const ASTPtr & partition, const String & name, bool moving_part, ContextPtr local_context)
{
    String partition_id;

    if (moving_part)
        partition_id = partition->as<ASTLiteral &>().value.safeGet<String>();
    else
        partition_id = getPartitionIDFromQuery(partition, local_context);

    DataPartsVector parts;
    if (moving_part)
    {
        auto part_info = MergeTreePartInfo::fromPartName(partition_id, format_version);
        parts.push_back(getActiveContainingPart(part_info));
        if (!parts.back() || parts.back()->name != part_info.getPartName())
            throw Exception("Part " + partition_id + " is not exists or not active", ErrorCodes::NO_SUCH_DATA_PART);
    }
    else
        parts = getDataPartsVectorInPartition(MergeTreeDataPartState::Committed, partition_id);

    auto disk = getStoragePolicy(IStorage::StorageLocation::MAIN)->getDiskByName(name);
    if (!disk)
        throw Exception("Disk " + name + " does not exists on policy " + getStoragePolicy(IStorage::StorageLocation::MAIN)->getName(), ErrorCodes::UNKNOWN_DISK);

    parts.erase(std::remove_if(parts.begin(), parts.end(), [&](auto part_ptr)
        {
            return part_ptr->volume->getDisk()->getName() == disk->getName();
        }), parts.end());

    if (parts.empty())
    {
        String no_parts_to_move_message;
        if (moving_part)
            no_parts_to_move_message = "Part '" + partition_id + "' is already on disk '" + disk->getName() + "'";
        else
            no_parts_to_move_message = "All parts of partition '" + partition_id + "' are already on disk '" + disk->getName() + "'";

        throw Exception(no_parts_to_move_message, ErrorCodes::UNKNOWN_DISK);
    }

    if (!movePartsToSpace(parts, std::static_pointer_cast<Space>(disk)))
        throw Exception("Cannot move parts because moves are manually disabled", ErrorCodes::ABORTED);
}


void MergeTreeData::movePartitionToVolume(const ASTPtr & partition, const String & name, bool moving_part, ContextPtr local_context)
{
    String partition_id;

    if (moving_part)
        partition_id = partition->as<ASTLiteral &>().value.safeGet<String>();
    else
        partition_id = getPartitionIDFromQuery(partition, local_context);

    DataPartsVector parts;
    if (moving_part)
    {
        auto part_info = MergeTreePartInfo::fromPartName(partition_id, format_version);
        parts.emplace_back(getActiveContainingPart(part_info));
        if (!parts.back() || parts.back()->name != part_info.getPartName())
            throw Exception("Part " + partition_id + " is not exists or not active", ErrorCodes::NO_SUCH_DATA_PART);
    }
    else
        parts = getDataPartsVectorInPartition(MergeTreeDataPartState::Committed, partition_id);

    auto volume = getStoragePolicy(IStorage::StorageLocation::MAIN)->getVolumeByName(name);
    if (!volume)
        throw Exception("Volume " + name + " does not exists on policy " + getStoragePolicy(IStorage::StorageLocation::MAIN)->getName(), ErrorCodes::UNKNOWN_DISK);

    if (parts.empty())
        throw Exception("Nothing to move (сheck that the partition exists).", ErrorCodes::NO_SUCH_DATA_PART);

    parts.erase(std::remove_if(parts.begin(), parts.end(), [&](auto part_ptr)
        {
            for (const auto & disk : volume->getDisks())
            {
                if (part_ptr->volume->getDisk()->getName() == disk->getName())
                {
                    return true;
                }
            }
            return false;
        }), parts.end());

    if (parts.empty())
    {
        String no_parts_to_move_message;
        if (moving_part)
            no_parts_to_move_message = "Part '" + partition_id + "' is already on volume '" + volume->getName() + "'";
        else
            no_parts_to_move_message = "All parts of partition '" + partition_id + "' are already on volume '" + volume->getName() + "'";

        throw Exception(no_parts_to_move_message, ErrorCodes::UNKNOWN_DISK);
    }

    if (!movePartsToSpace(parts, std::static_pointer_cast<Space>(volume)))
        throw Exception("Cannot move parts because moves are manually disabled", ErrorCodes::ABORTED);
}

void MergeTreeData::movePartitionToShard(const ASTPtr & /*partition*/, bool /*move_part*/, const String & /*to*/, ContextPtr /*query_context*/)
{
    throw Exception(ErrorCodes::NOT_IMPLEMENTED, "MOVE PARTITION TO SHARD is not supported by storage {}", getName());
}

void MergeTreeData::dropPartitionWhere(const ASTPtr &, bool, ContextPtr)
{
    throw Exception(ErrorCodes::NOT_IMPLEMENTED, "DROP PARTITION WHERE is not supported by storage {}", getName());
}

void MergeTreeData::fetchPartition(
    const ASTPtr & /*partition*/,
    const StorageMetadataPtr & /*metadata_snapshot*/,
    const String & /*from*/,
    bool /*fetch_part*/,
    ContextPtr /*query_context*/)
{
    throw Exception(ErrorCodes::NOT_IMPLEMENTED, "FETCH PARTITION is not supported by storage {}", getName());
}

void MergeTreeData::fetchPartitionWhere(const ASTPtr &, const StorageMetadataPtr &, const String &, ContextPtr)
{
    throw Exception(ErrorCodes::NOT_IMPLEMENTED, "FETCH PARTITION WHERE is not supported by storage {}", getName());
}

void MergeTreeData::repairPartition(const ASTPtr & , bool , const String & , ContextPtr)
{
    throw Exception(ErrorCodes::NOT_IMPLEMENTED, "REPAIR PARTITION is not supported by storage {}", getName());
}

Pipe MergeTreeData::alterPartition(
    const StorageMetadataPtr & metadata_snapshot,
    const PartitionCommands & commands,
    ContextPtr query_context)
{
    PartitionCommandsResultInfo result;
    for (const PartitionCommand & command : commands)
    {
        PartitionCommandsResultInfo current_command_results;
        switch (command.type)
        {
            case PartitionCommand::DROP_PARTITION:
            {
                if (command.part)
                {
                    auto part_name = command.partition->as<ASTLiteral &>().value.safeGet<String>();
                    checkPartCanBeDropped(part_name);
                    dropPart(part_name, command.detach, query_context);
                }
                else
                {
                    checkPartitionCanBeDropped(command.partition);
                    dropPartition(command.partition, command.detach, query_context);
                }
            }
            break;

            case PartitionCommand::DROP_PARTITION_WHERE:
                dropPartitionWhere(command.partition, command.detach, query_context);
                break;

            case PartitionCommand::DROP_DETACHED_PARTITION:
                dropDetached(command.partition, command.part, query_context);
                break;

            case PartitionCommand::ATTACH_PARTITION:
                current_command_results = attachPartition(command.partition, metadata_snapshot, command.part, query_context);
                break;
            case PartitionCommand::MOVE_PARTITION:
            {
                switch (*command.move_destination_type)
                {
                    case PartitionCommand::MoveDestinationType::DISK:
                        movePartitionToDisk(command.partition, command.move_destination_name, command.part, query_context);
                        break;

                    case PartitionCommand::MoveDestinationType::VOLUME:
                        movePartitionToVolume(command.partition, command.move_destination_name, command.part, query_context);
                        break;

                    case PartitionCommand::MoveDestinationType::TABLE:
                    {
                        checkPartitionCanBeDropped(command.partition);
                        String dest_database = query_context->resolveDatabase(command.to_database);
                        auto dest_storage = DatabaseCatalog::instance().getTable({dest_database, command.to_table}, query_context);
                        movePartitionToTable(dest_storage, command.partition, query_context);
                    }
                    break;

                    case PartitionCommand::MoveDestinationType::SHARD:
                    {
                        if (!getSettings()->part_moves_between_shards_enable)
                            throw Exception(ErrorCodes::SUPPORT_IS_DISABLED,
                                            "Moving parts between shards is experimental and work in progress"
                                            ", see part_moves_between_shards_enable setting");
                        movePartitionToShard(command.partition, command.part, command.move_destination_name, query_context);
                    }
                    break;
                }
            }
            break;

            case PartitionCommand::MOVE_PARTITION_FROM:
            {
                String from_database = query_context->resolveDatabase(command.from_database);
                auto from_storage = DatabaseCatalog::instance().getTable({from_database, command.from_table}, query_context);
                movePartitionFrom(from_storage, command.partition, query_context);
            }
            break;

            case PartitionCommand::REPLACE_PARTITION:
            {
                checkPartitionCanBeDropped(command.partition);
                String from_database = query_context->resolveDatabase(command.from_database);
                auto from_storage = DatabaseCatalog::instance().getTable({from_database, command.from_table}, query_context);
                replacePartitionFrom(from_storage, command.partition, command.replace, query_context);
            }
            break;

            case PartitionCommand::FETCH_PARTITION:
                fetchPartition(command.partition, metadata_snapshot, command.from_zookeeper_path, command.part, query_context);
                break;

            case PartitionCommand::FETCH_PARTITION_WHERE:
                fetchPartitionWhere(command.partition, metadata_snapshot, command.from_zookeeper_path, query_context);
                break;

            case PartitionCommand::REPAIR_PARTITION:
                repairPartition(command.partition, command.part, command.from_zookeeper_path, query_context);
                break;

            case PartitionCommand::FREEZE_PARTITION:
            {
                auto lock = lockForShare(query_context->getCurrentQueryId(), query_context->getSettingsRef().lock_acquire_timeout);
                current_command_results = freezePartition(command.partition, metadata_snapshot, command.with_name, query_context, lock);
            }
            break;

            case PartitionCommand::FREEZE_ALL_PARTITIONS:
            {
                auto lock = lockForShare(query_context->getCurrentQueryId(), query_context->getSettingsRef().lock_acquire_timeout);
                current_command_results = freezeAll(command.with_name, metadata_snapshot, query_context, lock);
            }
            break;

            case PartitionCommand::UNFREEZE_PARTITION:
            {
                auto lock = lockForShare(query_context->getCurrentQueryId(), query_context->getSettingsRef().lock_acquire_timeout);
                current_command_results = unfreezePartition(command.partition, command.with_name, query_context, lock);
            }
            break;

            case PartitionCommand::UNFREEZE_ALL_PARTITIONS:
            {
                auto lock = lockForShare(query_context->getCurrentQueryId(), query_context->getSettingsRef().lock_acquire_timeout);
                current_command_results = unfreezeAll(command.with_name, query_context, lock);
            }
            break;
            case PartitionCommand::INGEST_PARTITION:
            {
                ingestPartition(command, query_context);
            }
            break;
            case PartitionCommand::SAMPLE_PARTITION_WHERE:
            {
                String dst_database = command.from_database.empty() ? query_context->getCurrentDatabase() : command.from_database;
                samplePartitionWhere(dst_database, command.from_table, command.sharding_exp, command.partition, query_context);
            }
            break;

            default:
                throw Exception("Uninitialized partition command", ErrorCodes::LOGICAL_ERROR);
        }
        for (auto & command_result : current_command_results)
            command_result.command_type = command.typeToString();
        result.insert(result.end(), current_command_results.begin(), current_command_results.end());
    }

    if (query_context->getSettingsRef().alter_partition_verbose_result)
        return convertCommandsResultToSource(result);

    return {};
}

void MergeTreeData::movePartitionFrom(const StoragePtr &, const ASTPtr &, ContextPtr)
{
    throw Exception("movePartitionFrom is not supported by " + getName(), ErrorCodes::NOT_IMPLEMENTED);
}

MergeTreeData::DataPartsVector
MergeTreeData::getAllDataPartsVector(MergeTreeData::DataPartStateVector * out_states, bool require_projection_parts) const
{
    DataPartsVector res;
    if (require_projection_parts)
    {
        auto lock = lockPartsRead();
        for (const auto & part : data_parts_by_info)
        {
            for (const auto & [p_name, projection_part] : part->getProjectionParts())
                res.push_back(projection_part);
        }

        if (out_states != nullptr)
        {
            out_states->resize(res.size());
            for (size_t i = 0; i < res.size(); ++i)
                (*out_states)[i] = res[i]->getParentPart()->getState();
        }
    }
    else
    {
        auto lock = lockPartsRead();
        res.assign(data_parts_by_info.begin(), data_parts_by_info.end());

        if (out_states != nullptr)
        {
            out_states->resize(res.size());
            for (size_t i = 0; i < res.size(); ++i)
                (*out_states)[i] = res[i]->getState();
        }
    }

    return res;
}

std::vector<DetachedPartInfo>
MergeTreeData::getDetachedParts() const
{
    std::vector<DetachedPartInfo> res;

    for (const auto & [path, disk] : getRelativeDataPathsWithDisks())
    {
        for (auto it = disk->iterateDirectory(fs::path(path) / MergeTreeData::DETACHED_DIR_NAME); it->isValid(); it->next())
        {
            res.emplace_back();
            auto & part = res.back();

            DetachedPartInfo::tryParseDetachedPartName(it->name(), part, format_version);
            part.disk = disk->getName();
        }
    }
    return res;
}

void MergeTreeData::validateDetachedPartName(const String & name) const
{
    if (name.find('/') != std::string::npos || name == "." || name == "..")
        throw DB::Exception("Invalid part name '" + name + "'", ErrorCodes::INCORRECT_FILE_NAME);

    auto full_path = getFullRelativePathForPart(name, "detached/");

    if (!full_path)
        throw DB::Exception("Detached part \"" + name + "\" not found" , ErrorCodes::BAD_DATA_PART_NAME);

    if (startsWith(name, "attaching_") || startsWith(name, "deleting_"))
        throw DB::Exception("Cannot drop part " + name + ": "
                            "most likely it is used by another DROP or ATTACH query.",
                            ErrorCodes::BAD_DATA_PART_NAME);
}

void MergeTreeData::dropDetached(const ASTPtr & partition, bool part, ContextPtr local_context)
{
    PartsTemporaryRename renamed_parts(*this, "detached/");

    if (part)
    {
        String part_name = partition->as<ASTLiteral &>().value.safeGet<String>();
        validateDetachedPartName(part_name);
        renamed_parts.addPart(part_name, "deleting_" + part_name);
    }
    else
    {
        String partition_id = getPartitionIDFromQuery(partition, local_context);
        DetachedPartsInfo detached_parts = getDetachedParts();
        for (const auto & part_info : detached_parts)
            if (part_info.valid_name && part_info.partition_id == partition_id
                && part_info.prefix != "attaching" && part_info.prefix != "deleting")
                renamed_parts.addPart(part_info.dir_name, "deleting_" + part_info.dir_name);
    }

    LOG_DEBUG(log, "Will drop {} detached parts.", renamed_parts.old_and_new_names.size());

    renamed_parts.tryRenameAll();

    for (auto & [old_name, new_name] : renamed_parts.old_and_new_names)
    {
        const auto & [path, disk] = renamed_parts.old_part_name_to_path_and_disk[old_name];
        disk->removeRecursive(fs::path(path) / "detached" / new_name / "");
        LOG_DEBUG(log, "Dropped detached part {}", old_name);
        old_name.clear();
    }
}

MergeTreeData::MutableDataPartsVector MergeTreeData::tryLoadPartsToAttach(const ASTPtr & partition, bool attach_part,
        ContextPtr local_context, PartsTemporaryRename & renamed_parts, bool in_preattach)
{
    const String source_dir = "detached/";

    std::map<String, DiskPtr> name_to_disk;

    /// Let's compose a list of parts that should be added.
    if (attach_part)
    {
        const String part_id = partition->as<ASTLiteral &>().value.safeGet<String>();

        validateDetachedPartName(part_id);
        renamed_parts.addPart(part_id, "attaching_" + part_id);

        if (MergeTreePartInfo::tryParsePartName(part_id, nullptr, format_version))
            name_to_disk[part_id] = getDiskForPart(part_id, source_dir);
    }
    else
    {
        String partition_id = getPartitionIDFromQuery(partition, local_context);
        LOG_DEBUG(log, "Looking for parts for partition {} in {}", partition_id, source_dir);
        ActiveDataPartSet active_parts(format_version);

        const auto disks = getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks();

        for (const auto & disk : disks)
        {
            for (auto it = disk->iterateDirectory(getRelativeDataPath(IStorage::StorageLocation::MAIN) + source_dir); it->isValid(); it->next())
            {
                const String & name = it->name();
                MergeTreePartInfo part_info;

                // TODO what if name contains "_tryN" suffix?
                /// Parts with prefix in name (e.g. attaching_1_3_3_0, deleting_1_3_3_0) will be ignored
                if (!MergeTreePartInfo::tryParsePartName(name, &part_info, format_version)
                    || part_info.partition_id != partition_id)
                {
                    continue;
                }

                LOG_DEBUG(log, "Found part {}", name);
                active_parts.add(name);
                name_to_disk[name] = disk;
            }
        }
        LOG_DEBUG(log, "{} of them are active", active_parts.size());

        /// Inactive parts are renamed so they can not be attached in case of repeated ATTACH.
        for (const auto & [name, disk] : name_to_disk)
        {
            const String containing_part = active_parts.getContainingPart(name);

            if (!containing_part.empty() && containing_part != name)
                // TODO maybe use PartsTemporaryRename here?
                disk->moveDirectory(fs::path(getRelativeDataPath(IStorage::StorageLocation::MAIN)) / source_dir / name,
                    fs::path(getRelativeDataPath(IStorage::StorageLocation::MAIN)) / source_dir / ("inactive_" + name));
            else if (in_preattach)
                renamed_parts.addPart(name, name);
            else
                renamed_parts.addPart(name, "attaching_" + name);
        }
    }


    /// Try to rename all parts before attaching to prevent race with DROP DETACHED and another ATTACH.
    renamed_parts.tryRenameAll();

    /// Synchronously check that added parts exist and are not broken. We will write checksums.txt if it does not exist.
    LOG_DEBUG(log, "Checking parts");
    MutableDataPartsVector loaded_parts;
    loaded_parts.reserve(renamed_parts.old_and_new_names.size());

    for (const auto & [old_name, new_name] : renamed_parts.old_and_new_names)
    {
        LOG_DEBUG(log, "Checking part {}", new_name);

        auto single_disk_volume = std::make_shared<SingleDiskVolume>("volume_" + old_name, name_to_disk[old_name]);
        MutableDataPartPtr part = createPart(old_name, single_disk_volume, source_dir + new_name);

        loadPartAndFixMetadataImpl(part);
        loaded_parts.push_back(part);
    }

    return loaded_parts;
}

MergeTreeData::MutableDataPartsVector MergeTreeData::tryLoadPartsInPathToAttach(const PartNamesWithDisks & parts_with_disk, const String & relative_path)
{
    LOG_DEBUG(log, "Loading parts from relative path {}", relative_path);

    MutableDataPartsVector loaded_parts;
    loaded_parts.reserve(parts_with_disk.size());

    for (const auto & [part_name, disk_ptr] : parts_with_disk)
    {
        LOG_DEBUG(log, "Checking part {}", part_name);
        auto single_disk_volume = std::make_shared<SingleDiskVolume>("volume_" + part_name, disk_ptr);
        MutableDataPartPtr part = createPart(part_name, single_disk_volume, fs::path(relative_path) / part_name);

        loadPartAndFixMetadataImpl(part);
        loaded_parts.push_back(part);
    }

    LOG_DEBUG(log, "Loaded {} parts.", loaded_parts.size());

    return loaded_parts;
}

void MergeTreeData::Transaction::rollbackPartsToTemporaryState()
{
    if (!isEmpty())
    {
        WriteBufferFromOwnString buf;
        buf << " Rollbacking parts state to temporary and removing from working set:";
        for (const auto & part : precommitted_parts)
            buf << " " << part->relative_path;
        buf << ".";
        LOG_DEBUG(data.log, "Undoing transaction.{}", buf.str());

        data.removePartsFromWorkingSetImmediatelyAndSetTemporaryState(
            DataPartsVector(precommitted_parts.begin(), precommitted_parts.end()));
    }

    clear();
}

void MergeTreeData::Transaction::rollback()
{
    if (!isEmpty())
    {
        WriteBufferFromOwnString buf;
        buf << " Removing parts:";
        for (const auto & part : precommitted_parts)
            buf << " " << part->relative_path;
        buf << ".";
        LOG_DEBUG(data.log, "Undoing transaction.{}", buf.str());

        data.removePartsFromWorkingSet(
            DataPartsVector(precommitted_parts.begin(), precommitted_parts.end()),
            /* clear_without_timeout = */ true);
    }

    clear();
}

MergeTreeData::DataPartsVector MergeTreeData::Transaction::commit(MergeTreeData::DataPartsLock * acquired_parts_lock)
{
    DataPartsVector total_covered_parts;

    if (!isEmpty())
    {
        auto parts_lock = acquired_parts_lock ? MergeTreeData::DataPartsLock() : data.lockParts();
        auto * owing_parts_lock = acquired_parts_lock ? acquired_parts_lock : &parts_lock;

        auto current_time = time(nullptr);

        size_t add_bytes = 0;
        size_t add_rows = 0;
        size_t add_parts = 0;

        size_t reduce_bytes = 0;
        size_t reduce_rows = 0;
        size_t reduce_parts = 0;

        for (const DataPartPtr & part : precommitted_parts)
        {
            DataPartPtr covering_part;
            DataPartsVector covered_parts = data.getActivePartsToReplace(part->info, part->name, covering_part, *owing_parts_lock);
            if (covering_part)
            {
                LOG_WARNING(data.log, "Tried to commit obsolete part {} covered by {}", part->name, covering_part->getNameWithState());

                part->remove_time.store(0, std::memory_order_relaxed); /// The part will be removed without waiting for old_parts_lifetime seconds.
                data.modifyPartState(part, DataPartState::Outdated);
            }
            else
            {
                total_covered_parts.insert(total_covered_parts.end(), covered_parts.begin(), covered_parts.end());
                for (const DataPartPtr & covered_part : covered_parts)
                {
                    covered_part->remove_time.store(current_time, std::memory_order_relaxed);

                    reduce_bytes += covered_part->getBytesOnDisk();
                    reduce_rows += covered_part->rows_count;

                    data.modifyPartState(covered_part, DataPartState::Outdated);
                    data.removePartContributionToColumnSizes(covered_part);
                }
                reduce_parts += covered_parts.size();

                add_bytes += part->getBytesOnDisk();
                add_rows += part->rows_count;
                ++add_parts;

                data.modifyPartState(part, DataPartState::Committed);
                data.addPartContributionToColumnSizes(part);
            }
        }
        data.decreaseDataVolume(reduce_bytes, reduce_rows, reduce_parts);
        data.increaseDataVolume(add_bytes, add_rows, add_parts);
    }

    clear();

    return total_covered_parts;
}

bool MergeTreeData::isPrimaryOrMinMaxKeyColumnPossiblyWrappedInFunctions(
    const ASTPtr & node, const StorageMetadataPtr & metadata_snapshot) const
{
    const String column_name = node->getColumnName();

    for (const auto & name : metadata_snapshot->getPrimaryKeyColumns())
        if (column_name == name)
            return true;

    for (const auto & name : getMinMaxColumnsNames(metadata_snapshot->getPartitionKey()))
        if (column_name == name)
            return true;

    if (const auto * func = node->as<ASTFunction>())
        if (func->arguments->children.size() == 1)
            return isPrimaryOrMinMaxKeyColumnPossiblyWrappedInFunctions(func->arguments->children.front(), metadata_snapshot);

    return false;
}

bool MergeTreeData::mayBenefitFromIndexForIn(
    const ASTPtr & left_in_operand, ContextPtr, const StorageMetadataPtr & metadata_snapshot) const
{
    /// Make sure that the left side of the IN operator contain part of the key.
    /// If there is a tuple on the left side of the IN operator, at least one item of the tuple
    ///  must be part of the key (probably wrapped by a chain of some acceptable functions).
    const auto * left_in_operand_tuple = left_in_operand->as<ASTFunction>();
    const auto & index_wrapper_factory = MergeTreeIndexFactory::instance();
    if (left_in_operand_tuple && left_in_operand_tuple->name == "tuple")
    {
        for (const auto & item : left_in_operand_tuple->arguments->children)
        {
            if (isPrimaryOrMinMaxKeyColumnPossiblyWrappedInFunctions(item, metadata_snapshot))
                return true;
            for (const auto & index : metadata_snapshot->getSecondaryIndices())
                if (index_wrapper_factory.get(index)->mayBenefitFromIndexForIn(item))
                    return true;
            if (metadata_snapshot->selected_projection
                && metadata_snapshot->selected_projection->isPrimaryKeyColumnPossiblyWrappedInFunctions(item))
                return true;
        }
        /// The tuple itself may be part of the primary key, so check that as a last resort.
        if (isPrimaryOrMinMaxKeyColumnPossiblyWrappedInFunctions(left_in_operand, metadata_snapshot))
            return true;
        if (metadata_snapshot->selected_projection
            && metadata_snapshot->selected_projection->isPrimaryKeyColumnPossiblyWrappedInFunctions(left_in_operand))
            return true;
        return false;
    }
    else
    {
        for (const auto & index : metadata_snapshot->getSecondaryIndices())
            if (index_wrapper_factory.get(index)->mayBenefitFromIndexForIn(left_in_operand))
                return true;

        if (metadata_snapshot->selected_projection
            && metadata_snapshot->selected_projection->isPrimaryKeyColumnPossiblyWrappedInFunctions(left_in_operand))
            return true;

        return isPrimaryOrMinMaxKeyColumnPossiblyWrappedInFunctions(left_in_operand, metadata_snapshot);
    }
}

using PartitionIdToMaxBlock = std::unordered_map<String, Int64>;

static void selectBestProjection(
    const MergeTreeDataSelectExecutor & reader,
    const StorageMetadataPtr & metadata_snapshot,
    const SelectQueryInfo & query_info,
    const Names & required_columns,
    ProjectionCandidate & candidate,
    ContextPtr query_context,
    std::shared_ptr<PartitionIdToMaxBlock> max_added_blocks,
    const Settings & settings,
    const MergeTreeData::DataPartsVector & parts,
    ProjectionCandidate *& selected_candidate,
    size_t & min_sum_marks)
{
    MergeTreeData::DataPartsVector projection_parts;
    MergeTreeData::DataPartsVector normal_parts;
    for (const auto & part : parts)
    {
        const auto & projections = part->getProjectionParts();
        auto it = projections.find(candidate.desc->name);
        if (it != projections.end())
            projection_parts.push_back(it->second);
        else
            normal_parts.push_back(part);
    }

    if (projection_parts.empty())
        return;

    auto sum_marks = reader.estimateNumMarksToRead(
        projection_parts,
        candidate.required_columns,
        metadata_snapshot,
        candidate.desc->metadata,
        query_info, // TODO syntax_analysis_result set in index
        query_context,
        settings.max_threads,
        max_added_blocks);

    if (normal_parts.empty())
    {
        // All parts are projection parts which allows us to use in_order_optimization.
        // TODO It might be better to use a complete projection even with more marks to read.
        candidate.complete = true;
    }
    else
    {
        sum_marks += reader.estimateNumMarksToRead(
            normal_parts,
            required_columns,
            metadata_snapshot,
            metadata_snapshot,
            query_info, // TODO syntax_analysis_result set in index
            query_context,
            settings.max_threads,
            max_added_blocks);
    }

    // We choose the projection with least sum_marks to read.
    if (sum_marks < min_sum_marks)
    {
        selected_candidate = &candidate;
        min_sum_marks = sum_marks;
    }
}


bool MergeTreeData::getQueryProcessingStageWithAggregateProjection(
    ContextPtr query_context, const StorageMetadataPtr & metadata_snapshot, SelectQueryInfo & query_info) const
{
    const auto & settings = query_context->getSettingsRef();
    if (!settings.allow_experimental_projection_optimization || query_info.ignore_projections || query_info.is_projection_query)
        return false;

    const auto & query_ptr = query_info.query;

    // Currently projections don't support final yet.
    if (auto * select = query_ptr->as<ASTSelectQuery>(); select && select->final())
        return false;

    InterpreterSelectQuery select(
        query_ptr, query_context, SelectQueryOptions{QueryProcessingStage::WithMergeableState}.ignoreProjections().ignoreAlias());
    const auto & analysis_result = select.getAnalysisResult();

    bool can_use_aggregate_projection = true;
    /// If the first stage of the query pipeline is more complex than Aggregating - Expression - Filter - ReadFromStorage,
    /// we cannot use aggregate projection.
    if (analysis_result.join != nullptr || analysis_result.array_join != nullptr)
        can_use_aggregate_projection = false;

    /// Check if all needed columns can be provided by some aggregate projection. Here we also try
    /// to find expression matches. For example, suppose an aggregate projection contains a column
    /// named sum(x) and the given query also has an expression called sum(x), it's a match. This is
    /// why we need to ignore all aliases during projection creation and the above query planning.
    /// It's also worth noting that, sqrt(sum(x)) will also work because we can treat sum(x) as a
    /// required column.

    /// The ownership of ProjectionDescription is hold in metadata_snapshot which lives along with
    /// InterpreterSelect, thus we can store the raw pointer here.
    std::vector<ProjectionCandidate> candidates;
    NameSet keys;
    std::unordered_map<std::string_view, size_t> key_name_pos_map;
    size_t pos = 0;
    for (const auto & desc : select.getQueryAnalyzer()->aggregationKeys())
    {
        keys.insert(desc.name);
        key_name_pos_map.insert({desc.name, pos++});
    }
    auto actions_settings = ExpressionActionsSettings::fromSettings(settings);

    // All required columns should be provided by either current projection or previous actions
    // Let's traverse backward to finish the check.
    // TODO what if there is a column with name sum(x) and an aggregate sum(x)?
    auto rewrite_before_where =
        [&](ProjectionCandidate & candidate, const ProjectionDescription & projection,
            NameSet & required_columns, const Block & source_block, const Block & aggregates)
    {
        if (analysis_result.before_where)
        {
            candidate.where_column_name = analysis_result.where_column_name;
            candidate.remove_where_filter = analysis_result.remove_where_filter;
            candidate.before_where = analysis_result.before_where->clone();

            required_columns = candidate.before_where->foldActionsByProjection(
                required_columns,
                projection.sample_block_for_keys,
                candidate.where_column_name);

            if (required_columns.empty())
                return false;
            candidate.before_where->addAggregatesViaProjection(aggregates);
        }

        if (analysis_result.prewhere_info)
        {
            candidate.prewhere_info = analysis_result.prewhere_info;

            auto prewhere_actions = candidate.prewhere_info->prewhere_actions->clone();
            auto prewhere_required_columns = required_columns;
            // required_columns should not contain columns generated by prewhere
            for (const auto & column : prewhere_actions->getResultColumns())
                required_columns.erase(column.name);

            // Prewhere_action should not add missing keys.
            prewhere_required_columns = prewhere_actions->foldActionsByProjection(
                prewhere_required_columns, projection.sample_block_for_keys, candidate.prewhere_info->prewhere_column_name, false);

            if (prewhere_required_columns.empty())
                return false;
            candidate.prewhere_info->prewhere_actions = prewhere_actions;

            if (candidate.prewhere_info->row_level_filter)
            {
                auto row_level_filter_actions = candidate.prewhere_info->row_level_filter->clone();
                prewhere_required_columns = row_level_filter_actions->foldActionsByProjection(
                    prewhere_required_columns, projection.sample_block_for_keys, candidate.prewhere_info->row_level_column_name, false);

                if (prewhere_required_columns.empty())
                    return false;
                candidate.prewhere_info->row_level_filter = row_level_filter_actions;
            }

            if (candidate.prewhere_info->alias_actions)
            {
                auto alias_actions = candidate.prewhere_info->alias_actions->clone();
                prewhere_required_columns
                    = alias_actions->foldActionsByProjection(prewhere_required_columns, projection.sample_block_for_keys, {}, false);

                if (prewhere_required_columns.empty())
                    return false;
                candidate.prewhere_info->alias_actions = alias_actions;
            }
            required_columns.insert(prewhere_required_columns.begin(), prewhere_required_columns.end());
        }

        bool match = true;
        for (const auto & column : required_columns)
        {
            /// There are still missing columns, fail to match
            if (!source_block.has(column))
            {
                match = false;
                break;
            }
        }
        return match;
    };

    for (const auto & projection : metadata_snapshot->projections)
    {
        ProjectionCandidate candidate{};
        candidate.desc = &projection;

        if (projection.type == ProjectionDescription::Type::Aggregate && analysis_result.need_aggregate && can_use_aggregate_projection)
        {
            bool match = true;
            Block aggregates;
            // Let's first check if all aggregates are provided by current projection
            for (const auto & aggregate : select.getQueryAnalyzer()->aggregates())
            {
                const auto * column = projection.sample_block.findByName(aggregate.column_name);
                if (column)
                {
                    aggregates.insert(*column);
                }
                else
                {
                    match = false;
                    break;
                }
            }

            if (!match)
                continue;

            // Check if all aggregation keys can be either provided by some action, or by current
            // projection directly. Reshape the `before_aggregation` action DAG so that it only
            // needs to provide aggregation keys, and certain children DAG might be substituted by
            // some keys in projection.
            candidate.before_aggregation = analysis_result.before_aggregation->clone();
            auto required_columns = candidate.before_aggregation->foldActionsByProjection(keys, projection.sample_block_for_keys);

            if (required_columns.empty() && !keys.empty())
                continue;

            if (analysis_result.optimize_aggregation_in_order)
            {
                for (const auto & key : keys)
                {
                    auto actions_dag = analysis_result.before_aggregation->clone();
                    actions_dag->foldActionsByProjection({key}, projection.sample_block_for_keys);
                    candidate.group_by_elements_actions.emplace_back(std::make_shared<ExpressionActions>(actions_dag, actions_settings));
                }
            }

            // Reorder aggregation keys and attach aggregates
            candidate.before_aggregation->reorderAggregationKeysForProjection(key_name_pos_map);
            candidate.before_aggregation->addAggregatesViaProjection(aggregates);

            if (rewrite_before_where(candidate, projection, required_columns, projection.sample_block_for_keys, aggregates))
            {
                candidate.required_columns = {required_columns.begin(), required_columns.end()};
                for (const auto & aggregate : aggregates)
                    candidate.required_columns.push_back(aggregate.name);
                candidates.push_back(std::move(candidate));
            }
        }

        if (projection.type == ProjectionDescription::Type::Normal && (analysis_result.hasWhere() || analysis_result.hasPrewhere()))
        {
            const auto & actions
                = analysis_result.before_aggregation ? analysis_result.before_aggregation : analysis_result.before_order_by;
            NameSet required_columns;
            for (const auto & column : actions->getRequiredColumns())
                required_columns.insert(column.name);

            if (rewrite_before_where(candidate, projection, required_columns, projection.sample_block, {}))
            {
                candidate.required_columns = {required_columns.begin(), required_columns.end()};
                candidates.push_back(std::move(candidate));
            }
        }
    }

    // Let's select the best projection to execute the query.
    if (!candidates.empty())
    {
        std::shared_ptr<PartitionIdToMaxBlock> max_added_blocks;
        if (settings.select_sequential_consistency)
        {
            if (const StorageReplicatedMergeTree * replicated = dynamic_cast<const StorageReplicatedMergeTree *>(this))
                max_added_blocks = std::make_shared<PartitionIdToMaxBlock>(replicated->getMaxAddedBlocks());
        }

        auto parts = getDataPartsVector();
        MergeTreeDataSelectExecutor reader(*this);

        ProjectionCandidate * selected_candidate = nullptr;
        size_t min_sum_marks = std::numeric_limits<size_t>::max();
        bool has_ordinary_projection = false;
        /// Favor aggregate projections
        for (auto & candidate : candidates)
        {
            if (candidate.desc->type == ProjectionDescription::Type::Aggregate)
            {
                selectBestProjection(
                    reader,
                    metadata_snapshot,
                    query_info,
                    analysis_result.required_columns,
                    candidate,
                    query_context,
                    max_added_blocks,
                    settings,
                    parts,
                    selected_candidate,
                    min_sum_marks);
            }
            else
                has_ordinary_projection = true;
        }

        /// Select the best normal projection if no aggregate projection is available
        if (!selected_candidate && has_ordinary_projection)
        {
            min_sum_marks = reader.estimateNumMarksToRead(
                parts,
                analysis_result.required_columns,
                metadata_snapshot,
                metadata_snapshot,
                query_info, // TODO syntax_analysis_result set in index
                query_context,
                settings.max_threads,
                max_added_blocks);

            // Add 1 to base sum_marks so that we prefer projections even when they have equal number of marks to read.
            // NOTE: It is not clear if we need it. E.g. projections do not support skip index for now.
            min_sum_marks += 1;

            for (auto & candidate : candidates)
            {
                if (candidate.desc->type == ProjectionDescription::Type::Normal)
                {
                    selectBestProjection(
                        reader,
                        metadata_snapshot,
                        query_info,
                        analysis_result.required_columns,
                        candidate,
                        query_context,
                        max_added_blocks,
                        settings,
                        parts,
                        selected_candidate,
                        min_sum_marks);
                }
            }
        }

        if (!selected_candidate)
            return false;

        if (selected_candidate->desc->type == ProjectionDescription::Type::Aggregate)
        {
            selected_candidate->aggregation_keys = select.getQueryAnalyzer()->aggregationKeys();
            selected_candidate->aggregate_descriptions = select.getQueryAnalyzer()->aggregates();
        }

        query_info.projection = std::move(*selected_candidate);

        return true;
    }
    return false;
}


QueryProcessingStage::Enum MergeTreeData::getQueryProcessingStage(
    ContextPtr query_context,
    QueryProcessingStage::Enum to_stage,
    const StorageMetadataPtr & metadata_snapshot,
    SelectQueryInfo & query_info) const
{
    if (to_stage >= QueryProcessingStage::Enum::WithMergeableState)
    {
        if (getQueryProcessingStageWithAggregateProjection(query_context, metadata_snapshot, query_info))
        {
            if (query_info.projection->desc->type == ProjectionDescription::Type::Aggregate)
                return QueryProcessingStage::Enum::WithMergeableState;
        }
    }

    return QueryProcessingStage::Enum::FetchColumns;
}


MergeTreeData & MergeTreeData::checkStructureAndGetMergeTreeData(IStorage & source_table, const StorageMetadataPtr & src_snapshot, const StorageMetadataPtr & my_snapshot) const
{
    MergeTreeData * src_data = dynamic_cast<MergeTreeData *>(&source_table);
    if (!src_data)
        throw Exception("Table " + source_table.getStorageID().getNameForLogs() +
                        " supports attachPartitionFrom only for MergeTree family of table engines."
                        " Got " + source_table.getName(), ErrorCodes::NOT_IMPLEMENTED);

    if (my_snapshot->getColumns().getAllPhysical().sizeOfDifference(src_snapshot->getColumns().getAllPhysical()))
        throw Exception("Tables have different structure", ErrorCodes::INCOMPATIBLE_COLUMNS);

    auto query_to_string = [] (const ASTPtr & ast)
    {
        return ast ? queryToString(ast) : "";
    };

    if (query_to_string(my_snapshot->getSortingKeyAST()) != query_to_string(src_snapshot->getSortingKeyAST()))
        throw Exception("Tables have different ordering", ErrorCodes::BAD_ARGUMENTS);

    if (query_to_string(my_snapshot->getPartitionKeyAST()) != query_to_string(src_snapshot->getPartitionKeyAST()))
        throw Exception("Tables have different partition key", ErrorCodes::BAD_ARGUMENTS);

    if (format_version != src_data->format_version)
        throw Exception("Tables have different format_version", ErrorCodes::BAD_ARGUMENTS);

    return *src_data;
}

MergeTreeData & MergeTreeData::checkStructureAndGetMergeTreeData(
    const StoragePtr & source_table, const StorageMetadataPtr & src_snapshot, const StorageMetadataPtr & my_snapshot) const
{
    return checkStructureAndGetMergeTreeData(*source_table, src_snapshot, my_snapshot);
}

String MergeTreeData::getMetastorePath() const
{
    return getContext()->getMetastorePath() + getRelativeDataPath(IStorage::StorageLocation::MAIN);
}

DiskPtr MergeTreeData::getDiskForPart(const String & part_name, const String & additional_path) const
{
    const auto disks = getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks();

    for (const DiskPtr & disk : disks)
        for (auto it = disk->iterateDirectory(getRelativeDataPath(IStorage::StorageLocation::MAIN) + additional_path); it->isValid(); it->next())
            if (it->name() == part_name)
                return disk;

    return nullptr;
}


std::optional<String> MergeTreeData::getFullRelativePathForPart(const String & part_name, const String & additional_path) const
{
    auto disk = getDiskForPart(part_name, additional_path);
    if (disk)
        return getRelativeDataPath(IStorage::StorageLocation::MAIN) + additional_path;
    return {};
}

Strings MergeTreeData::getDataPaths() const
{
    Strings res;
    auto disks = getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks();
    for (const auto & disk : disks)
        res.push_back(getFullPathOnDisk(IStorage::StorageLocation::MAIN, disk));
    return res;
}

MergeTreeData::PathsWithDisks MergeTreeData::getRelativeDataPathsWithDisks() const
{
    PathsWithDisks res;
    auto disks = getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks();
    for (const auto & disk : disks)
        res.emplace_back(getRelativeDataPath(IStorage::StorageLocation::MAIN), disk);
    return res;
}

MergeTreeData::MatcherFn MergeTreeData::getPartitionMatcher(const ASTPtr & partition_ast, ContextPtr local_context) const
{
    bool prefixed = false;
    String id;

    if (format_version < MERGE_TREE_DATA_MIN_FORMAT_VERSION_WITH_CUSTOM_PARTITIONING)
    {
        /// Month-partitioning specific - partition value can represent a prefix of the partition to freeze.
        if (const auto * partition_lit = partition_ast->as<ASTPartition &>().value->as<ASTLiteral>())
        {
            id = partition_lit->value.getType() == Field::Types::UInt64
                 ? toString(partition_lit->value.get<UInt64>())
                 : partition_lit->value.safeGet<String>();
            prefixed = true;
        }
        else
            id = getPartitionIDFromQuery(partition_ast, local_context);
    }
    else
        id = getPartitionIDFromQuery(partition_ast, local_context);

    return [prefixed, id](const String & partition_id)
    {
        if (prefixed)
            return startsWith(partition_id, id);
        else
            return id == partition_id;
    };
}

PartitionCommandsResultInfo MergeTreeData::freezePartition(
    const ASTPtr & partition_ast,
    const StorageMetadataPtr & metadata_snapshot,
    const String & with_name,
    ContextPtr local_context,
    TableLockHolder &)
{
    return freezePartitionsByMatcher(getPartitionMatcher(partition_ast, local_context), metadata_snapshot, with_name, local_context);
}

PartitionCommandsResultInfo MergeTreeData::freezeAll(
    const String & with_name,
    const StorageMetadataPtr & metadata_snapshot,
    ContextPtr local_context,
    TableLockHolder &)
{
    return freezePartitionsByMatcher([] (const String &) { return true; }, metadata_snapshot, with_name, local_context);
}

PartitionCommandsResultInfo MergeTreeData::freezePartitionsByMatcher(
    MatcherFn matcher,
    const StorageMetadataPtr & metadata_snapshot,
    const String & with_name,
    ContextPtr local_context)
{
    String clickhouse_path = fs::canonical(local_context->getPath());
    String default_shadow_path = fs::path(clickhouse_path) / "shadow/";
    fs::create_directories(default_shadow_path);
    auto increment = Increment(fs::path(default_shadow_path) / "increment.txt").get(true);

    const String shadow_path = "shadow/";

    /// Acquire a snapshot of active data parts to prevent removing while doing backup.
    const auto data_parts = getDataParts();

    String backup_name = (!with_name.empty() ? escapeForFileName(with_name) : toString(increment));
    String backup_path = fs::path(shadow_path) / backup_name / "";

    for (const auto & disk : getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks())
        disk->onFreeze(backup_path);

    PartitionCommandsResultInfo result;

    size_t parts_processed = 0;
    for (const auto & part : data_parts)
    {
        if (!matcher(part->info.partition_id))
            continue;

        LOG_DEBUG(log, "Freezing part {} snapshot will be placed at {}", part->name, backup_path);

        part->volume->getDisk()->createDirectories(backup_path);

        String backup_part_path = fs::path(backup_path) / getRelativeDataPath(IStorage::StorageLocation::MAIN) / part->relative_path;
        if (auto part_in_memory = asInMemoryPart(part))
            part_in_memory->flushToDisk(fs::path(backup_path) / getRelativeDataPath(IStorage::StorageLocation::MAIN), part->relative_path, metadata_snapshot);
        else
            localBackup(part->volume->getDisk(), part->getFullRelativePath(), backup_part_path);

        part->volume->getDisk()->removeFileIfExists(fs::path(backup_part_path) / IMergeTreeDataPart::DELETE_ON_DESTROY_MARKER_FILE_NAME);

        part->is_frozen.store(true, std::memory_order_relaxed);
        result.push_back(PartitionCommandResultInfo{
            .partition_id = part->info.partition_id,
            .part_name = part->name,
            .backup_path = fs::path(part->volume->getDisk()->getPath()) / backup_path,
            .part_backup_path = fs::path(part->volume->getDisk()->getPath()) / backup_part_path,
            .backup_name = backup_name,
        });
        ++parts_processed;
    }

    LOG_DEBUG(log, "Freezed {} parts", parts_processed);
    return result;
}

PartitionCommandsResultInfo MergeTreeData::unfreezePartition(
    const ASTPtr & partition,
    const String & backup_name,
    ContextPtr local_context,
    TableLockHolder &)
{
    return unfreezePartitionsByMatcher(getPartitionMatcher(partition, local_context), backup_name, local_context);
}

PartitionCommandsResultInfo MergeTreeData::unfreezeAll(
    const String & backup_name,
    ContextPtr local_context,
    TableLockHolder &)
{
    return unfreezePartitionsByMatcher([] (const String &) { return true; }, backup_name, local_context);
}

PartitionCommandsResultInfo MergeTreeData::unfreezePartitionsByMatcher(MatcherFn matcher, const String & backup_name, ContextPtr)
{
    auto backup_path = fs::path("shadow") / escapeForFileName(backup_name) / getRelativeDataPath(IStorage::StorageLocation::MAIN);

    LOG_DEBUG(log, "Unfreezing parts by path {}", backup_path.generic_string());

    PartitionCommandsResultInfo result;

    for (const auto & disk : getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks())
    {
        if (!disk->exists(backup_path))
            continue;

        for (auto it = disk->iterateDirectory(backup_path); it->isValid(); it->next())
        {
            const auto & partition_directory = it->name();

            /// Partition ID is prefix of part directory name: <partition id>_<rest of part directory name>
            auto found = partition_directory.find('_');
            if (found == std::string::npos)
                continue;
            auto partition_id = partition_directory.substr(0, found);

            if (!matcher(partition_id))
                continue;

            const auto & path = it->path();

            disk->removeRecursive(path);

            result.push_back(PartitionCommandResultInfo{
                .partition_id = partition_id,
                .part_name = partition_directory,
                .backup_path = disk->getPath() + backup_path.generic_string(),
                .part_backup_path = disk->getPath() + path,
                .backup_name = backup_name,
            });

            LOG_DEBUG(log, "Unfreezed part by path {}", disk->getPath() + path);
        }
    }

    LOG_DEBUG(log, "Unfreezed {} parts", result.size());

    return result;
}

bool MergeTreeData::canReplacePartition(const DataPartPtr & src_part) const
{
    const auto settings = getSettings();

    if (!settings->enable_mixed_granularity_parts || settings->index_granularity_bytes == 0)
    {
        if (!canUseAdaptiveGranularity() && src_part->index_granularity_info.is_adaptive)
            return false;
        if (canUseAdaptiveGranularity() && !src_part->index_granularity_info.is_adaptive)
            return false;
    }
    return true;
}

inline UInt64 time_in_microseconds(std::chrono::time_point<std::chrono::system_clock> timepoint)
{
    return std::chrono::duration_cast<std::chrono::microseconds>(timepoint.time_since_epoch()).count();
}


inline UInt64 time_in_seconds(std::chrono::time_point<std::chrono::system_clock> timepoint)
{
    return std::chrono::duration_cast<std::chrono::seconds>(timepoint.time_since_epoch()).count();
}

void MergeTreeData::writePartLog(
    PartLogElement::Type type,
    const ExecutionStatus & execution_status,
    UInt64 elapsed_ns,
    const String & new_part_name,
    const DataPartPtr & result_part,
    const DataPartsVector & source_parts,
    const MergeListEntry * merge_entry)
try
{
    auto table_id = getStorageID();
    auto part_log = getContext()->getPartLog(table_id.database_name);
    if (!part_log)
        return;

    PartLogElement part_log_elem;

    part_log_elem.event_type = type;

    part_log_elem.error = static_cast<UInt16>(execution_status.code);
    part_log_elem.exception = execution_status.message;

    // construct event_time and event_time_microseconds using the same time point
    // so that the two times will always be equal up to a precision of a second.
    const auto time_now = std::chrono::system_clock::now();
    part_log_elem.event_time = time_in_seconds(time_now);
    part_log_elem.event_time_microseconds = time_in_microseconds(time_now);

    /// TODO: Stop stopwatch in outer code to exclude ZK timings and so on
    part_log_elem.duration_ms = elapsed_ns / 1000000;

    part_log_elem.database_name = table_id.database_name;
    part_log_elem.table_name = table_id.table_name;
    part_log_elem.partition_id = MergeTreePartInfo::fromPartName(new_part_name, format_version).partition_id;
    part_log_elem.part_name = new_part_name;

    if (result_part)
    {
        part_log_elem.path_on_disk = result_part->getFullPath();
        part_log_elem.bytes_compressed_on_disk = result_part->getBytesOnDisk();
        part_log_elem.rows = result_part->rows_count;
    }

    part_log_elem.source_part_names.reserve(source_parts.size());
    for (const auto & source_part : source_parts)
        part_log_elem.source_part_names.push_back(source_part->name);

    if (merge_entry)
    {
        part_log_elem.rows_read = (*merge_entry)->rows_read;
        part_log_elem.bytes_read_uncompressed = (*merge_entry)->bytes_read_uncompressed;

        part_log_elem.rows = (*merge_entry)->rows_written;
        part_log_elem.bytes_uncompressed = (*merge_entry)->bytes_written_uncompressed;
        part_log_elem.peak_memory_usage = (*merge_entry)->memory_tracker.getPeak();
    }

    part_log->add(part_log_elem);
}
catch (...)
{
    tryLogCurrentException(log, __PRETTY_FUNCTION__);
}

MergeTreeData::CurrentlyMovingPartsTagger::CurrentlyMovingPartsTagger(MergeTreeMovingParts && moving_parts_, MergeTreeData & data_)
    : parts_to_move(std::move(moving_parts_)), data(data_)
{
    for (const auto & moving_part : parts_to_move)
        if (!data.currently_moving_parts.emplace(moving_part.part).second)
            throw Exception("Cannot move part '" + moving_part.part->name + "'. It's already moving.", ErrorCodes::LOGICAL_ERROR);
}

MergeTreeData::CurrentlyMovingPartsTagger::~CurrentlyMovingPartsTagger()
{
    std::lock_guard lock(data.moving_parts_mutex);
    for (const auto & moving_part : parts_to_move)
    {
        /// Something went completely wrong
        if (!data.currently_moving_parts.count(moving_part.part))
            std::terminate();
        data.currently_moving_parts.erase(moving_part.part);
    }
}

bool MergeTreeData::scheduleDataMovingJob(IBackgroundJobExecutor & executor)
{
    if (parts_mover.moves_blocker.isCancelled())
        return false;

    auto moving_tagger = selectPartsForMove();
    if (moving_tagger->parts_to_move.empty())
        return false;

    executor.execute({[this, moving_tagger] () mutable
    {
        return moveParts(moving_tagger);
    }, PoolType::MOVE});
    return true;
}

bool MergeTreeData::areBackgroundMovesNeeded() const
{
    auto policy = getStoragePolicy(IStorage::StorageLocation::MAIN);

    if (policy->getVolumes().size() > 1)
        return true;

    return policy->getVolumes().size() == 1 && policy->getVolumes()[0]->getDisks().size() > 1;
}

bool MergeTreeData::movePartsToSpace(const DataPartsVector & parts, SpacePtr space)
{
    if (parts_mover.moves_blocker.isCancelled())
        return false;

    auto moving_tagger = checkPartsForMove(parts, space);
    if (moving_tagger->parts_to_move.empty())
        return false;

    return moveParts(moving_tagger);
}

MergeTreeData::CurrentlyMovingPartsTaggerPtr MergeTreeData::selectPartsForMove()
{
    MergeTreeMovingParts parts_to_move;

    auto can_move = [this](const DataPartPtr & part, String * reason) -> bool
    {
        if (partIsAssignedToBackgroundOperation(part))
        {
            *reason = "part already assigned to background operation.";
            return false;
        }
        if (currently_moving_parts.count(part))
        {
            *reason = "part is already moving.";
            return false;
        }

        return true;
    };

    std::lock_guard moving_lock(moving_parts_mutex);

    parts_mover.selectPartsForMove(parts_to_move, can_move, moving_lock);
    return std::make_shared<CurrentlyMovingPartsTagger>(std::move(parts_to_move), *this);
}

void MergeTreeData::renamePartAndDropMetadata(const String& name, const DataPartPtr& sourcePart)
{
    sourcePart->renameTo(name, true);
    const_cast<DataPart &>(*sourcePart).is_temp = true;

    modifyPartState(sourcePart, DataPartState::Outdated);
    auto it = data_parts_by_info.find(sourcePart->info);
    data_parts_indexes.erase(it);
}

void MergeTreeData::renamePartAndInsertMetadata(const String& name, const DataPartPtr& sourcePart)
{
    sourcePart->renameTo(name, true);
    const_cast<DataPart &>(*sourcePart).is_temp = false;

    auto s_part = std::const_pointer_cast<DataPart>(sourcePart);
    s_part->setState(DataPartState::Committed);
    data_parts_indexes.insert(s_part);
}

MergeTreeData::CurrentlyMovingPartsTaggerPtr MergeTreeData::checkPartsForMove(const DataPartsVector & parts, SpacePtr space)
{
    std::lock_guard moving_lock(moving_parts_mutex);

    MergeTreeMovingParts parts_to_move;
    for (const auto & part : parts)
    {
        auto reservation = space->reserve(part->getBytesOnDisk());
        if (!reservation)
            throw Exception("Move is not possible. Not enough space on '" + space->getName() + "'", ErrorCodes::NOT_ENOUGH_SPACE);

        auto reserved_disk = reservation->getDisk();

        if (reserved_disk->exists(getRelativeDataPath(IStorage::StorageLocation::MAIN) + part->name))
            throw Exception(
                "Move is not possible: " + fullPath(reserved_disk, getRelativeDataPath(IStorage::StorageLocation::MAIN) + part->name) + " already exists",
                ErrorCodes::DIRECTORY_ALREADY_EXISTS);

        if (currently_moving_parts.count(part) || partIsAssignedToBackgroundOperation(part))
            throw Exception(
                "Cannot move part '" + part->name + "' because it's participating in background process",
                ErrorCodes::PART_IS_TEMPORARILY_LOCKED);

        parts_to_move.emplace_back(part, std::move(reservation));
    }
    return std::make_shared<CurrentlyMovingPartsTagger>(std::move(parts_to_move), *this);
}

bool MergeTreeData::moveParts(const CurrentlyMovingPartsTaggerPtr & moving_tagger)
{
    LOG_INFO(log, "Got {} parts to move.", moving_tagger->parts_to_move.size());

    for (const auto & moving_part : moving_tagger->parts_to_move)
    {
        Stopwatch stopwatch;
        DataPartPtr cloned_part;

        auto write_part_log = [&](const ExecutionStatus & execution_status)
        {
            writePartLog(
                PartLogElement::Type::MOVE_PART,
                execution_status,
                stopwatch.elapsed(),
                moving_part.part->name,
                cloned_part,
                {moving_part.part},
                nullptr);
        };

        try
        {
            cloned_part = parts_mover.clonePart(moving_part);
            parts_mover.swapClonedPart(cloned_part);
            write_part_log({});
        }
        catch (...)
        {
            write_part_log(ExecutionStatus::fromCurrentException());
            if (cloned_part)
                cloned_part->remove();

            throw;
        }
    }
    return true;
}

bool MergeTreeData::partsContainSameProjections(const DataPartPtr & left, const DataPartPtr & right)
{
    if (left->getProjectionParts().size() != right->getProjectionParts().size())
        return false;
    for (const auto & [name, _] : left->getProjectionParts())
    {
        if (!right->hasProjection(name))
            return false;
    }
    return true;
}

MergeTreeData::WriteAheadLogPtr MergeTreeData::getWriteAheadLog()
{
    std::lock_guard lock(write_ahead_log_mutex);
    if (!write_ahead_log)
    {
        auto reservation = reserveSpace(getSettings()->write_ahead_log_max_bytes);
        write_ahead_log = std::make_shared<MergeTreeWriteAheadLog>(*this, reservation->getDisk());
    }

    return write_ahead_log;
}

size_t MergeTreeData::getTotalMergesWithTTLInMergeList() const
{
    return getContext()->getMergeList().getMergesWithTTLCount();
}

void MergeTreeData::addPartContributionToDataVolume(const DataPartPtr & part)
{
    increaseDataVolume(part->getBytesOnDisk(), part->rows_count, 1);
}

void MergeTreeData::removePartContributionToDataVolume(const DataPartPtr & part)
{
    decreaseDataVolume(part->getBytesOnDisk(), part->rows_count, 1);
}

void MergeTreeData::increaseDataVolume(size_t bytes, size_t rows, size_t parts)
{
    total_active_size_bytes.fetch_add(bytes, std::memory_order_acq_rel);
    total_active_size_rows.fetch_add(rows, std::memory_order_acq_rel);
    total_active_size_parts.fetch_add(parts, std::memory_order_acq_rel);
}

void MergeTreeData::decreaseDataVolume(size_t bytes, size_t rows, size_t parts)
{
    total_active_size_bytes.fetch_sub(bytes, std::memory_order_acq_rel);
    total_active_size_rows.fetch_sub(rows, std::memory_order_acq_rel);
    total_active_size_parts.fetch_sub(parts, std::memory_order_acq_rel);
}

void MergeTreeData::setDataVolume(size_t bytes, size_t rows, size_t parts)
{
    total_active_size_bytes.store(bytes, std::memory_order_release);
    total_active_size_rows.store(rows, std::memory_order_release);
    total_active_size_parts.store(parts, std::memory_order_release);
}

ReservationPtr MergeTreeData::balancedReservation(
    const StorageMetadataPtr & metadata_snapshot,
    size_t part_size,
    size_t max_volume_index,
    const String & part_name,
    const MergeTreePartInfo & part_info,
    MergeTreeData::DataPartsVector covered_parts,
    std::optional<CurrentlySubmergingEmergingTagger> * tagger_ptr,
    const IMergeTreeDataPart::TTLInfos * ttl_infos,
    bool is_insert)
{
    ReservationPtr reserved_space;
    auto min_bytes_to_rebalance_partition_over_jbod = getSettings()->min_bytes_to_rebalance_partition_over_jbod;
    if (tagger_ptr && min_bytes_to_rebalance_partition_over_jbod > 0 && part_size >= min_bytes_to_rebalance_partition_over_jbod)
    try
    {
        const auto & disks = getStoragePolicy(IStorage::StorageLocation::MAIN)->getVolume(max_volume_index)->getDisks();
        std::map<String, size_t> disk_occupation;
        std::map<String, std::vector<String>> disk_parts_for_logging;
        for (const auto & disk : disks)
            disk_occupation.emplace(disk->getName(), 0);

        std::set<String> committed_big_parts_from_partition;
        std::set<String> submerging_big_parts_from_partition;
        std::lock_guard lock(currently_submerging_emerging_mutex);

        for (const auto & part : currently_submerging_big_parts)
        {
            if (part_info.partition_id == part->info.partition_id)
                submerging_big_parts_from_partition.insert(part->name);
        }

        {
            auto lock_parts = lockParts();
            if (covered_parts.empty())
            {
                // It's a part fetch. Calculate `covered_parts` here.
                MergeTreeData::DataPartPtr covering_part;
                covered_parts = getActivePartsToReplace(part_info, part_name, covering_part, lock_parts);
            }

            // Remove irrelevant parts.
            covered_parts.erase(
                std::remove_if(
                    covered_parts.begin(),
                    covered_parts.end(),
                    [min_bytes_to_rebalance_partition_over_jbod](const auto & part)
                    {
                        return !(part->isStoredOnDisk() && part->getBytesOnDisk() >= min_bytes_to_rebalance_partition_over_jbod);
                    }),
                covered_parts.end());

            // Include current submerging big parts which are not yet in `currently_submerging_big_parts`
            for (const auto & part : covered_parts)
                submerging_big_parts_from_partition.insert(part->name);

            for (const auto & part : getDataPartsStateRange(MergeTreeData::DataPartState::Committed))
            {
                if (part->isStoredOnDisk() && part->getBytesOnDisk() >= min_bytes_to_rebalance_partition_over_jbod
                    && part_info.partition_id == part->info.partition_id)
                {
                    auto name = part->volume->getDisk()->getName();
                    auto it = disk_occupation.find(name);
                    if (it != disk_occupation.end())
                    {
                        if (submerging_big_parts_from_partition.find(part->name) == submerging_big_parts_from_partition.end())
                        {
                            it->second += part->getBytesOnDisk();
                            disk_parts_for_logging[name].push_back(formatReadableSizeWithBinarySuffix(part->getBytesOnDisk()));
                            committed_big_parts_from_partition.insert(part->name);
                        }
                        else
                        {
                            disk_parts_for_logging[name].push_back(formatReadableSizeWithBinarySuffix(part->getBytesOnDisk()) + " (submerging)");
                        }
                    }
                    else
                    {
                        // Part is on different volume. Ignore it.
                    }
                }
            }
        }

        for (const auto & [name, emerging_part] : currently_emerging_big_parts)
        {
            // It's possible that the emerging big parts are committed and get added twice. Thus a set is used to deduplicate.
            if (committed_big_parts_from_partition.find(name) == committed_big_parts_from_partition.end()
                && part_info.partition_id == emerging_part.partition_id)
            {
                auto it = disk_occupation.find(emerging_part.disk_name);
                if (it != disk_occupation.end())
                {
                    it->second += emerging_part.estimate_bytes;
                    disk_parts_for_logging[emerging_part.disk_name].push_back(
                        formatReadableSizeWithBinarySuffix(emerging_part.estimate_bytes) + " (emerging)");
                }
                else
                {
                    // Part is on different volume. Ignore it.
                }
            }
        }

        size_t min_occupation_size = std::numeric_limits<size_t>::max();
        std::vector<String> candidates;
        for (const auto & [disk_name, size] : disk_occupation)
        {
            if (size < min_occupation_size)
            {
                min_occupation_size = size;
                candidates = {disk_name};
            }
            else if (size == min_occupation_size)
            {
                candidates.push_back(disk_name);
            }
        }

        if (!candidates.empty())
        {
            // Random pick one disk from best candidates
            std::shuffle(candidates.begin(), candidates.end(), thread_local_rng);
            String selected_disk_name = candidates.front();
            WriteBufferFromOwnString log_str;
            writeCString("\nbalancer: \n", log_str);
            for (const auto & [disk_name, per_disk_parts] : disk_parts_for_logging)
                writeString(fmt::format("  {}: [{}]\n", disk_name, fmt::join(per_disk_parts, ", ")), log_str);
            LOG_DEBUG(log, log_str.str());

            if (ttl_infos)
                reserved_space = tryReserveSpacePreferringTTLRules(
                    metadata_snapshot,
                    part_size,
                    *ttl_infos,
                    time(nullptr),
                    max_volume_index,
                    is_insert,
                    getStoragePolicy(IStorage::StorageLocation::MAIN)->getDiskByName(selected_disk_name));
            else
                reserved_space = tryReserveSpace(part_size, getStoragePolicy(IStorage::StorageLocation::MAIN)->getDiskByName(selected_disk_name));

            if (reserved_space)
            {
                currently_emerging_big_parts.emplace(
                    part_name, EmergingPartInfo{reserved_space->getDisk(0)->getName(), part_info.partition_id, part_size});

                for (const auto & part : covered_parts)
                {
                    if (currently_submerging_big_parts.count(part))
                        LOG_WARNING(log, "currently_submerging_big_parts contains duplicates. JBOD might lose balance");
                    else
                        currently_submerging_big_parts.insert(part);
                }

                // Record submerging big parts in the tagger to clean them up.
                tagger_ptr->emplace(*this, part_name, std::move(covered_parts), log);
            }
        }
    }
    catch (...)
    {
        LOG_DEBUG(log, "JBOD balancer encounters an error. Fallback to random disk selection");
        tryLogCurrentException(log);
    }
    return reserved_space;
}

void MergeTreeData::searchAllPartsOnFilesystem(std::map<String, DiskPtr> & parts_with_disks, std::map<String, DiskPtr> & wal_with_disks) const
{
    auto disks = getStoragePolicy(IStorage::StorageLocation::MAIN)->getDisks();

    for (auto disk_it = disks.rbegin(); disk_it != disks.rend(); ++disk_it)
    {
        auto disk_ptr = *disk_it;
        for (auto it = disk_ptr->iterateDirectory(getRelativeDataPath(IStorage::StorageLocation::MAIN)); it->isValid(); it->next())
        {
            /// Skip temporary directories, file 'format_version.txt', WAL and directory 'detached'.
            if (startsWith(it->name(), "tmp") || it->name() == MergeTreeData::FORMAT_VERSION_FILE_NAME
                || it->name() == MergeTreeData::DETACHED_DIR_NAME)
                continue;

            if (it->name() == MergeTreeWriteAheadLog::DEFAULT_WAL_FILE_NAME || startsWith(it->name(), MergeTreeWriteAheadLog::WAL_FILE_NAME))
                wal_with_disks.emplace(it->name(), disk_ptr);
            else
                parts_with_disks.emplace(it->name(), disk_ptr);
        }
    }
}

void MergeTreeData::syncMetaData()
{
    auto lock = lockParts();
    syncMetaImpl(lock);
}

void MergeTreeData::trySyncMetaData()
{
    auto lock = DataPartsLock(data_parts_mutex, std::defer_lock);

    /// Since is uncommon that data parts on disk differ from those in metastore, we can skip the synchronization
    /// if fail to get parts lock and finish the task as soon as possible.
    if (lock.try_lock())
        syncMetaImpl(lock);
}

void MergeTreeData::syncMetaImpl(DataPartsLock & lock)
{
    /// skip sync meta if the metastore is not enabled or the metastore is not ready for use.
    if (!metastore || !metastore->checkMetastoreStatus(*this))
        return;

    LOG_DEBUG(log, "Starting sync meta");

    std::map<String, DiskPtr> existing_parts_with_disks;
    std::map<String, DiskPtr> existing_wals_with_disks;

    searchAllPartsOnFilesystem(existing_parts_with_disks, existing_wals_with_disks);
    /// sync wals with metastore
    PartNamesWithDisks wal_from_metastore = metastore->getWriteAheadLogs(*this);
    std::map<String, MutableDataPartPtr> parts_from_wal;
    auto metadata_snapshot = getInMemoryMetadataPtr();

    for (auto & [wal_file, disk] : wal_from_metastore)
    {
        auto found = existing_wals_with_disks.find(wal_file);
        if (found != existing_wals_with_disks.end() && found->second->getName() == disk->getName())
        {
            existing_wals_with_disks.erase(found);
            MergeTreeWriteAheadLog wal(*this, disk, wal_file);
            for (auto && part : wal.restore(metadata_snapshot, getContext()))
                parts_from_wal.emplace(part->name ,std::move(part));
        }
        else
        {
            metastore->removeWAL(*this, wal_file);
        }
    }
    /// sync existing parts on disks
    for (auto it = data_parts_by_info.begin(); it != data_parts_by_info.end(); )
    {
        const String & part_name = (*it)->name;
        auto found = existing_parts_with_disks.find(part_name);

        if (found != existing_parts_with_disks.end() && found->second->getName() == (*it)->volume->getDisk()->getName())
        {
            /// the part can be found both in part index and file system, remove from existing_parts_with_disks
            existing_parts_with_disks.erase(found);
            it++;
        }
        else if (parts_from_wal.count(part_name))
        {
            /// parts from wal has no corresponding part file;
            it++;
        }
        else
        {
            /// If the parts neither exists on disk nor in WAL, consider to remove it from part index.
            /// skip part whose state is deleting.
            if ((*it)->getState() == DataPartState::Deleting)
            {
                it++;
                continue;
            }
            /// part maybe already be deleted, remove it from part index and metastore.
            LOG_WARNING(log, "Removing part '{}' (Disk name: {}) from metastore because of metadata mismatch.", part_name, (*it)->volume->getDisk()->getName());
            metastore->dropPart(*this, *it);
            it = data_parts_by_info.erase(it);
        }
    }

    PartNamesWithDisks parts_with_disks;
    PartNamesWithDisks wals_with_disks;

    for (auto & [part_file, disk] : existing_parts_with_disks)
    {
        if (MergeTreePartInfo::tryParsePartName(part_file, nullptr, format_version))
            parts_with_disks.emplace_back(part_file, disk);
    }

    for (auto & [wal_file, disk] : existing_wals_with_disks)
    {
        /// add missing wal to metastore;
        addWriteAheadLog(wal_file, disk);
        wals_with_disks.emplace_back(wal_file, disk);
    }

    /// now, we should load those missing parts into metastore.
    if (!parts_with_disks.empty() || !wals_with_disks.empty())
    {
        LOG_DEBUG(log, "Reloading {} missing parts and {} missing wal from file system.", parts_with_disks.size(), wals_with_disks.size());
        loadPartsFromFileSystem(parts_with_disks, wals_with_disks, true, lock);

        if (data_parts_indexes.size() >= 2)
        {
            auto committed_range = getDataPartsStateRange(DataPartState::Committed);
            auto prev_jt = committed_range.begin();
            auto curr_jt = std::next(prev_jt);

            auto deactivate_part = [&] (DataPartIteratorByStateAndInfo it)
            {
                (*it)->remove_time.store((*it)->modification_time, std::memory_order_relaxed);
                modifyPartState(it, DataPartState::Outdated);
                removePartContributionToDataVolume(*it);
            };

            (*prev_jt)->assertState({DataPartState::Committed});

            while (curr_jt != committed_range.end() && (*curr_jt)->getState() == DataPartState::Committed)
            {
                /// Don't consider data parts belonging to different partitions.
                if ((*curr_jt)->info.partition_id != (*prev_jt)->info.partition_id)
                {
                    ++prev_jt;
                    ++curr_jt;
                    continue;
                }

                if ((*curr_jt)->contains(**prev_jt))
                {
                    deactivate_part(prev_jt);
                    prev_jt = curr_jt;
                    ++curr_jt;
                }
                else if ((*prev_jt)->contains(**curr_jt))
                {
                    auto next = std::next(curr_jt);
                    deactivate_part(curr_jt);
                    curr_jt = next;
                }
                else
                {
                    ++prev_jt;
                    ++curr_jt;
                }
            }
        }
    }

    calculateColumnSizesImpl();

    LOG_DEBUG(log, "Sync meta done");
}

void MergeTreeData::addWriteAheadLog(const String & file_name, const DiskPtr & disk) const
{
    if (metastore)
        metastore->addWAL(*this, file_name, disk);
}

void MergeTreeData::removeWriteAheadLog(const String & file_name) const
{
    if (metastore)
        metastore->removeWAL(*this, file_name);
}

CurrentlySubmergingEmergingTagger::~CurrentlySubmergingEmergingTagger()
{
    std::lock_guard lock(storage.currently_submerging_emerging_mutex);

    for (const auto & part : submerging_parts)
    {
        if (!storage.currently_submerging_big_parts.count(part))
        {
            LOG_ERROR(log, "currently_submerging_big_parts doesn't contain part {} to erase. This is a bug", part->name);
            assert(false);
        }
        else
            storage.currently_submerging_big_parts.erase(part);
    }
    storage.currently_emerging_big_parts.erase(emerging_part_name);
}

}
