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

#include <Storages/MergeTree/CloudMergeTreeBlockOutputStream.h>

#include <CloudServices/CnchDedupHelper.h>
#include <CloudServices/CnchPartsHelper.h>
#include <CloudServices/commitCnchParts.h>
#include <Interpreters/PartLog.h>
#include <MergeTreeCommon/MergeTreeDataDeduper.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Storages/MergeTree/IMergeTreeDataPart_fwd.h>
#include <Storages/StorageCloudMergeTree.h>
#include <Storages/StorageCnchMergeTree.h>
#include <Transaction/CnchWorkerTransaction.h>
#include <WorkerTasks/ManipulationType.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int ABORTED;
    extern const int CNCH_LOCK_ACQUIRE_FAILED;
    extern const int INSERTION_LABEL_ALREADY_EXISTS;
    extern const int LOGICAL_ERROR;
    extern const int UNIQUE_KEY_STRING_SIZE_LIMIT_EXCEEDED;
}

CloudMergeTreeBlockOutputStream::CloudMergeTreeBlockOutputStream(
    MergeTreeMetaBase & storage_,
    StorageMetadataPtr metadata_snapshot_,
    ContextPtr context_,
    bool to_staging_area_)
    : storage(storage_)
    , log(storage.getLogger())
    , metadata_snapshot(std::move(metadata_snapshot_))
    , context(std::move(context_))
    , to_staging_area(to_staging_area_)
    , writer(storage, IStorage::StorageLocation::AUXILITY)
    , cnch_writer(storage, context, ManipulationType::Insert)
{
    if (!metadata_snapshot->hasUniqueKey() && to_staging_area)
        throw Exception("Table doesn't have UNIQUE KEY specified, can't write to staging area", ErrorCodes::LOGICAL_ERROR);
}

Block CloudMergeTreeBlockOutputStream::getHeader() const
{
    return metadata_snapshot->getSampleBlock();
}

void CloudMergeTreeBlockOutputStream::write(const Block & block)
{
    Stopwatch watch;
    LOG_DEBUG(storage.getLogger(), "Start to write new block");
    auto parts = convertBlockIntoDataParts(block);
    /// Generate delete bitmaps, delete bitmap is valid only when using delete_flag info for unique table
    LocalDeleteBitmaps bitmaps;
    const auto & txn = context->getCurrentTransaction();
    for (const auto & part : parts)
    {
        auto delete_bitmap = part->getDeleteBitmap(/*is_new_part*/ true);
        if (delete_bitmap && delete_bitmap->cardinality())
        {
            bitmaps.emplace_back(LocalDeleteBitmap::createBase(
                part->info, std::const_pointer_cast<Roaring>(delete_bitmap), txn->getPrimaryTransactionID().toUInt64()));
        }
    }
    LOG_DEBUG(storage.getLogger(), "Finish converting block into parts, elapsed {} ms", watch.elapsedMilliseconds());
    watch.restart();

    MutableMergeTreeDataPartsCNCHVector res;
    if (to_staging_area)
    {
        auto dumped = cnch_writer.dumpAndCommitCnchParts({}, bitmaps, /*staged_parts*/ parts);
        res = std::move(dumped.staged_parts);
    }
    else
    {
        auto dumped = cnch_writer.dumpAndCommitCnchParts(parts, bitmaps);
        res = std::move(dumped.parts);
    }
    LOG_DEBUG(
        storage.getLogger(),
        "Dump and commit {} parts, {} bitmaps, elapsed {} ms, pushing {} parts to preload vector.",
        res.size(),
        bitmaps.size(),
        watch.elapsedMilliseconds(),
        res.size());

    // batch all part to preload_parts for batch preloading in writeSuffix
    std::move(res.begin(), res.end(), std::back_inserter(preload_parts));
}

MergeTreeMutableDataPartsVector CloudMergeTreeBlockOutputStream::convertBlockIntoDataParts(const Block & block, bool use_inner_block_id)
{
    auto part_log = context->getGlobalContext()->getPartLog(storage.getDatabaseName());
    auto merge_tree_settings = storage.getSettings();
    auto settings = context->getSettingsRef();

    BlocksWithPartition part_blocks;

    /// For unique table, need to ensure that each part does not contain duplicate keys
    /// - when unique key is partition-level, split into sub-blocks first and then dedup the sub-block for each partition
    /// - when unique key is table-level
    /// -   if without version column, should dedup the input block first because split may change row order
    /// -   if use partition value as version, split first because `dedupWithUniqueKey` doesn't evaluate partition key expression
    /// -   if use explicit version, both approach work
    if (metadata_snapshot->hasUniqueKey() && !merge_tree_settings->partition_level_unique_keys
        && !storage.merging_params.partitionValueAsVersion())
    {
        FilterInfo filter_info = dedupWithUniqueKey(block);
        part_blocks = writer.splitBlockIntoParts(
            filter_info.num_filtered ? CnchDedupHelper::filterBlock(block, filter_info) : block,
            settings.max_partitions_per_insert_block,
            metadata_snapshot,
            context);
    }
    else
        part_blocks = writer.splitBlockIntoParts(block, settings.max_partitions_per_insert_block, metadata_snapshot, context);

    IMutableMergeTreeDataPartsVector parts;
    LOG_DEBUG(storage.getLogger(), "size of part_blocks {}", part_blocks.size());

    auto txn_id = context->getCurrentTransactionID();

    // Get all blocks of partition by expression
    for (auto & block_with_partition : part_blocks)
    {
        Row original_partition{block_with_partition.partition};
        auto bucketed_part_blocks = writer.splitBlockPartitionIntoPartsByClusterKey(block_with_partition, context->getSettingsRef().max_partitions_per_insert_block, metadata_snapshot, context);
        LOG_TRACE(storage.getLogger(), "size of bucketed_part_blocks {}", bucketed_part_blocks.size());
        const auto & txn = context->getCurrentTransaction();

        for (auto & bucketed_block_with_partition : bucketed_part_blocks)
        {
            Stopwatch watch;

            bucketed_block_with_partition.partition = Row(original_partition);
            if (metadata_snapshot->hasUniqueKey()
                && (merge_tree_settings->partition_level_unique_keys || storage.merging_params.partitionValueAsVersion()))
            {
                FilterInfo filter_info = dedupWithUniqueKey(bucketed_block_with_partition.block);
                if (filter_info.num_filtered)
                    bucketed_block_with_partition.block = CnchDedupHelper::filterBlock(bucketed_block_with_partition.block, filter_info);
            }

            DeleteBitmapPtr bitmap = std::make_shared<Roaring>();
            if (metadata_snapshot->hasUniqueKey() && bucketed_block_with_partition.block.has(StorageInMemoryMetadata::DELETE_FLAG_COLUMN_NAME))
            {
                /// Convert delete_flag info into delete bitmap
                const auto & delete_flag_column = bucketed_block_with_partition.block.getByName(StorageInMemoryMetadata::DELETE_FLAG_COLUMN_NAME);
                for (size_t rowid = 0; rowid < delete_flag_column.column->size(); ++rowid)
                {
                    if (delete_flag_column.column->getBool(rowid))
                        bitmap->add(rowid);
                }
            }

            /// Remove func columns
            for (auto & [name, _] : metadata_snapshot->getFuncColumns())
                if (bucketed_block_with_partition.block.has(name))
                    bucketed_block_with_partition.block.erase(name);

            auto block_id = use_inner_block_id ? increment.get() : context->getTimestamp();

            MergeTreeMutableDataPartPtr temp_part
                = writer.writeTempPart(bucketed_block_with_partition, metadata_snapshot, context, block_id, txn_id);

            /// Only add delete bitmap if it's not empty.
            if (bitmap->cardinality())
                temp_part->setDeleteBitmap(bitmap);

            if (txn->isSecondary())
                temp_part->secondary_txn_id = txn->getTransactionID();
            if (part_log)
                part_log->addNewPart(context, temp_part, watch.elapsed());
            LOG_DEBUG(
                storage.getLogger(),
                "Write part {}, {} rows, elapsed {} ms",
                temp_part->name,
                bucketed_block_with_partition.block.rows(),
                watch.elapsedMilliseconds());
            parts.push_back(std::move(temp_part));
        }
    }

    return parts;
}

void CloudMergeTreeBlockOutputStream::writeSuffix()
{
    try
    {
        writeSuffixImpl();
    }
    catch (const Exception & e)
    {
        if (e.code() == ErrorCodes::INSERTION_LABEL_ALREADY_EXISTS)
        {
            LOG_DEBUG(storage.getLogger(), e.displayText());
            return;
        }
        throw;
    }
}

void CloudMergeTreeBlockOutputStream::writeSuffixImpl()
{
    cnch_writer.preload(preload_parts);

    if (!metadata_snapshot->hasUniqueKey() || to_staging_area)
    {
        /// case1(normal table): commit all the temp parts as visible parts
        /// case2(unique table with async insert): commit all the temp parts as staged parts,
        ///     which will be converted to visible parts later by dedup worker
        /// insert is lock-free and faster than upsert due to its simplicity.
        writeSuffixForInsert();
    }
    else
    {
        /// case(unique table with sync insert): acquire the necessary locks to avoid write-write conflicts
        /// and then remove duplicate keys between visible parts and temp parts.
        writeSuffixForUpsert();
    }
}

void CloudMergeTreeBlockOutputStream::writeSuffixForInsert()
{
    // Commit for insert values in server side.
    auto txn = context->getCurrentTransaction();
    if (dynamic_pointer_cast<CnchServerTransaction>(txn) && !disable_transaction_commit)
    {
        txn->setMainTableUUID(storage.getStorageUUID());
        txn->commitV2();
        LOG_DEBUG(storage.getLogger(), "Finishing insert values commit in cnch server.");
    }
    else if (dynamic_pointer_cast<CnchWorkerTransaction>(txn))
    {
        auto kafka_table_id = txn->getKafkaTableID();
        if (!kafka_table_id.empty())
        {
            txn->setMainTableUUID(UUIDHelpers::toUUID(storage.getSettings()->cnch_table_uuid.value));
            Stopwatch watch;
            txn->commitV2();
            LOG_TRACE(
                storage.getLogger(), "Committed Kafka transaction {} elapsed {} ms", txn->getTransactionID(), watch.elapsedMilliseconds());
        }
        else
        {
            /// TODO: I thought the multiple branches should be unified.
            /// And a exception should be threw in the last `else` clause, otherwise there might be some potential bugs.
        }
    }
}

void CloudMergeTreeBlockOutputStream::writeSuffixForUpsert()
{
    auto txn = context->getCurrentTransaction();
    if (!txn)
        throw Exception("Transaction is not set", ErrorCodes::LOGICAL_ERROR);

    /// prefer to get cnch table uuid from settings as CloudMergeTree has no uuid for Kafka task
    String uuid_str = storage.getSettings()->cnch_table_uuid.value;
    if (uuid_str.empty())
        uuid_str = UUIDHelpers::UUIDToString(storage.getStorageUUID());

    txn->setMainTableUUID(UUIDHelpers::toUUID(uuid_str));
    if (auto worker_txn = dynamic_pointer_cast<CnchWorkerTransaction>(txn); worker_txn && !worker_txn->tryGetServerClient())
    {
        /// case: server initiated "insert select/infile" txn, need to set server client here in order to commit from worker
        if (const auto & client_info = context->getClientInfo(); client_info.rpc_port)
            worker_txn->setServerClient(context->getCnchServerClient(client_info.current_address.host().toString(), client_info.rpc_port));
        else
            throw Exception("Missing rpc_port, can't obtain server client to commit txn", ErrorCodes::LOGICAL_ERROR);
    }
    else
    {
        /// no need to set server client
        /// case: server initiated "insert values" txn, server client not required
        /// case: worker initiated "insert values|select|infile" txn, server client already set
    }

    auto catalog = context->getCnchCatalog();
    /// must use cnch table to construct staged parts.
    TxnTimestamp ts = context->getTimestamp();
    auto table = catalog->tryGetTableByUUID(*context, uuid_str, ts);
    if (!table)
        throw Exception("Table " + storage.getStorageID().getNameForLogs() + " has been dropped", ErrorCodes::ABORTED);
    auto cnch_table = dynamic_pointer_cast<StorageCnchMergeTree>(table);
    if (!cnch_table)
        throw Exception("Table " + storage.getStorageID().getNameForLogs() + " is not cnch merge tree", ErrorCodes::LOGICAL_ERROR);

    if (preload_parts.empty())
    {
        Stopwatch watch;
        txn->commitV2();
        LOG_INFO(
            log,
            "Committed transaction {} in {} ms, preload_parts is empty",
            txn->getTransactionID(),
            watch.elapsedMilliseconds(),
            preload_parts.size());
        return;
    }

    /// acquire locks for all the written partitions
    NameOrderedSet sorted_partitions;
    for (auto & part : preload_parts)
        sorted_partitions.insert(part->info.partition_id);

    CnchDedupHelper::DedupScope scope = storage.getSettings()->partition_level_unique_keys
        ? CnchDedupHelper::DedupScope::Partitions(sorted_partitions)
        : CnchDedupHelper::DedupScope::Table();

    std::vector<LockInfoPtr> locks_to_acquire = CnchDedupHelper::getLocksToAcquire(
        scope, txn->getTransactionID(), storage, storage.getSettings()->dedup_acquire_lock_timeout.value.totalMilliseconds());
    Stopwatch lock_watch;
    CnchLockHolder cnch_lock(*context, std::move(locks_to_acquire));
    if (!cnch_lock.tryLock())
    {
        throw Exception("Failed to acquire lock for txn " + txn->getTransactionID().toString(), ErrorCodes::CNCH_LOCK_ACQUIRE_FAILED);
    }
    ts = context->getTimestamp(); /// must get a new ts after locks are acquired
    MergeTreeDataPartsCNCHVector visible_parts = CnchDedupHelper::getVisiblePartsToDedup(scope, *cnch_table, ts);
    MergeTreeDataPartsCNCHVector staged_parts = CnchDedupHelper::getStagedPartsToDedup(scope, *cnch_table, ts);

    MergeTreeDataDeduper deduper(storage, context);
    LocalDeleteBitmaps bitmaps_to_dump = deduper.dedupParts(
        txn->getTransactionID(),
        CnchPartsHelper::toIMergeTreeDataPartsVector(visible_parts),
        CnchPartsHelper::toIMergeTreeDataPartsVector(staged_parts),
        {preload_parts.begin(), preload_parts.end()});

    Stopwatch watch;
    cnch_writer.publishStagedParts(staged_parts, bitmaps_to_dump);
    LOG_DEBUG(log, "Publishing staged parts take {} ms", watch.elapsedMilliseconds());

    watch.restart();
    txn->commitV2();
    LOG_INFO(
        log,
        "Committed transaction {} in {} ms (with {} ms holding lock)",
        txn->getTransactionID(),
        watch.elapsedMilliseconds(),
        lock_watch.elapsedMilliseconds());
}

namespace
{
    struct BlockUniqueKeyComparator
    {
        const ColumnsWithTypeAndName & keys;
        explicit BlockUniqueKeyComparator(const ColumnsWithTypeAndName & keys_) : keys(keys_) { }

        bool operator()(size_t lhs, size_t rhs) const
        {
            for (auto & key : keys)
            {
                int cmp = key.column->compareAt(lhs, rhs, *key.column, /*nan_direction_hint=*/1);
                if (cmp < 0)
                    return true;
                if (cmp > 0)
                    return false;
            }
            return false;
        }
    };
}

CloudMergeTreeBlockOutputStream::FilterInfo CloudMergeTreeBlockOutputStream::dedupWithUniqueKey(const Block & block)
{
    if (!metadata_snapshot->hasUniqueKey())
        return FilterInfo{};

    const ColumnWithTypeAndName * version_column = nullptr;
    if (metadata_snapshot->hasUniqueKey() && storage.merging_params.hasExplicitVersionColumn())
        version_column = &block.getByName(storage.merging_params.version_column);

    Block block_copy = block;
    metadata_snapshot->getUniqueKeyExpression()->execute(block_copy);

    ColumnsWithTypeAndName keys;
    ColumnsWithTypeAndName string_keys;
    for (auto & name : metadata_snapshot->getUniqueKeyColumns())
    {
        auto & col = block_copy.getByName(name);
        keys.push_back(col);
        if (col.type->getTypeId() == TypeIndex::String)
            string_keys.push_back(col);
    }

    BlockUniqueKeyComparator comparator(keys);
    /// first rowid of key -> rowid of the last occurrence of the same key
    std::map<size_t, size_t, decltype(comparator)> index(comparator);

    auto block_size = block_copy.rows();
    FilterInfo res;
    res.filter.assign(block_size, UInt8(1));

    ColumnWithTypeAndName delete_flag_column;
    if (version_column && block.has(StorageInMemoryMetadata::DELETE_FLAG_COLUMN_NAME))
        delete_flag_column = block.getByName(StorageInMemoryMetadata::DELETE_FLAG_COLUMN_NAME);

    auto is_delete_row = [&](int rowid) { return delete_flag_column.column && delete_flag_column.column->getBool(rowid); };

    /// In the case that engine has been set version column, if version is set by user(not zero), the delete row will obey the rule of version.
    /// Otherwise, the delete row will ignore comparing version, just doing the deletion directly.
    auto delete_ignore_version
        = [&](int rowid) { return is_delete_row(rowid) && version_column && !version_column->column->getUInt(rowid); };

    /// If there are duplicated keys, only keep the last one
    for (size_t rowid = 0; rowid < block_size; ++rowid)
    {
        if (auto it = index.find(rowid); it != index.end())
        {
            /// When there is no explict version column, use rowid as version number,
            /// Otherwise use value from version column
            size_t old_pos = it->second;
            size_t new_pos = rowid;
            if (version_column && !delete_ignore_version(rowid)
                && version_column->column->getUInt(old_pos) > version_column->column->getUInt(new_pos))
                std::swap(old_pos, new_pos);

            res.filter[old_pos] = 0;
            it->second = new_pos;
            res.num_filtered++;
        }
        else
            index[rowid] = rowid;

        /// Check the length limit for string type.
        size_t unique_string_keys_size = 0;
        for (auto & key : string_keys)
            unique_string_keys_size += static_cast<const ColumnString &>(*key.column).getDataAt(rowid).size;
        if (unique_string_keys_size > context->getSettingsRef().max_string_size_for_unique_key)
            throw Exception("The size of unique string keys out of limit", ErrorCodes::UNIQUE_KEY_STRING_SIZE_LIMIT_EXCEEDED);
    }
    return res;
}
}
