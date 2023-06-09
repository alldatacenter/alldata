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

#include <CloudServices/commitCnchParts.h>

#include <CloudServices/CnchMergeMutateThread.h>
#include <CloudServices/CnchServerClient.h>
#include <CloudServices/CnchServerClientPool.h>
#include <Interpreters/Context.h>
#include <Interpreters/PartLog.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <Storages/MergeTree/MergeTreeCNCHDataDumper.h>
#include <Transaction/Actions/DDLAlterAction.h>
#include <Transaction/Actions/DropRangeAction.h>
#include <Transaction/Actions/InsertAction.h>
#include <Transaction/Actions/MergeMutateAction.h>
#include <Transaction/TransactionCommon.h>
#include <Transaction/TransactionCoordinatorRcCnch.h>
#include <Transaction/CnchWorkerTransaction.h>
#include <Transaction/TxnTimestamp.h>
#include <common/strong_typedef.h>
#include <Core/Types.h>
#include "Disks/IDisk.h"

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int BRPC_TIMEOUT;
    extern const int BAD_DATA_PART_NAME;
    extern const int NULL_POINTER_DEREFERENCE;
    extern const int BUCKET_TABLE_ENGINE_MISMATCH;
}

CnchDataWriter::CnchDataWriter(
    MergeTreeMetaBase & storage_,
    ContextPtr context_,
    ManipulationType type_,
    String task_id_,
    String consumer_group_,
    const cppkafka::TopicPartitionList & tpl_)
    : storage(storage_)
    , context(context_)
    , type(type_)
    , task_id(std::move(task_id_))
    , consumer_group(std::move(consumer_group_))
    , tpl(tpl_)
{
}

DumpedData CnchDataWriter::dumpAndCommitCnchParts(
    const IMutableMergeTreeDataPartsVector & temp_parts,
    const LocalDeleteBitmaps & temp_bitmaps,
    const IMutableMergeTreeDataPartsVector & temp_staged_parts)
{
    if (temp_parts.empty() && temp_bitmaps.empty() && temp_staged_parts.empty())
        // Nothing to dump and commit, returns
        return {};

    LOG_DEBUG(storage.getLogger(), "Start dump and commit {} parts, {} bitmaps, {} staged parts", temp_parts.size(), temp_bitmaps.size(), temp_staged_parts.size());
    // FIXME: find the root case.
    for (const auto & part: temp_parts)
    {
        if (part->info.min_block < 0 || part->info.max_block <= 0)
            throw Exception("Attempt to submit illegal part " + part->info.getPartName(), ErrorCodes::BAD_DATA_PART_NAME);
    }

    auto dumped_data = dumpCnchParts(temp_parts, temp_bitmaps, temp_staged_parts);
    commitDumpedParts(dumped_data);
    return dumped_data;
}

DumpedData CnchDataWriter::dumpCnchParts(
    const IMutableMergeTreeDataPartsVector & temp_parts,
    const LocalDeleteBitmaps & temp_bitmaps,
    const IMutableMergeTreeDataPartsVector & temp_staged_parts)
{
   if (temp_parts.empty() && temp_bitmaps.empty() && temp_staged_parts.empty())
        // Nothing to dump, returns
        return {};

    Stopwatch watch;

    const auto & settings = context->getSettingsRef();

    if (settings.debug_cnch_remain_temp_part)
    {
        for (const auto & part : temp_parts)
            part->is_temp = false;
        for (const auto & part : temp_staged_parts)
            part->is_temp = false;
    }

    auto curr_txn = context->getCurrentTransaction();

    // set main table uuid in server or worker side
    curr_txn->setMainTableUUID(storage.getStorageUUID());

    /// get offsets first and the parts shouldn't be dumped and committed if get offsets failed
    if (context->getServerType() == ServerType::cnch_worker)
    {
        auto kafka_table_id = curr_txn ? curr_txn->getKafkaTableID() : StorageID::createEmpty();
        if (!kafka_table_id.empty())
        {
            if (!curr_txn)
                throw Exception("No transaction set for committing Kafka transaction", ErrorCodes::LOGICAL_ERROR);

            curr_txn->getKafkaTpl(consumer_group, tpl);
            if (tpl.empty() || consumer_group.empty())
                throw Exception("No tpl got for kafka consume, and we won't dump and commit parts", ErrorCodes::LOGICAL_ERROR);
        }
    }

    auto txn_id = curr_txn->getTransactionID();

    /// Write undo buffer first before dump to vfs
    std::vector<UndoResource> undo_resources;
    undo_resources.reserve(temp_parts.size() + temp_bitmaps.size() + temp_staged_parts.size());
    /// For local parts and stage parts, the remote parts can be at different disk,
    /// so we record the disk name of each part in the undo buffer.
    /// For the delete bitmap, it's always be dumped to the default disk
    std::vector<DiskPtr> part_disks;
    part_disks.reserve(temp_parts.size() + temp_staged_parts.size());

    for (auto & part : temp_parts)
    {
        String part_name = part->info.getPartNameWithHintMutation();
        auto disk = storage.getStoragePolicy(IStorage::StorageLocation::MAIN)->getAnyDisk();
        
        // Assign part id here, since we need to record it into undo buffer before dump part to filesystem
        String relative_path = part_name;
        if (disk->getType() == DiskType::Type::ByteS3)
        {
            UUID part_id = newPartID(part->info, txn_id.toUInt64());
            part->uuid = part_id;
            relative_path = UUIDHelpers::UUIDToString(part_id);
        }

        undo_resources.emplace_back(txn_id, UndoResourceType::Part, part_name, relative_path + '/');
        undo_resources.back().setDiskName(disk->getName());
        part_disks.emplace_back(std::move(disk));
    }
    for (auto & bitmap : temp_bitmaps)
    {
        undo_resources.emplace_back(bitmap->getUndoResource(txn_id));
    }
    for (auto & staged_part : temp_staged_parts)
    {
        String part_name = staged_part->info.getPartNameWithHintMutation();
        auto disk = storage.getStoragePolicy(IStorage::StorageLocation::MAIN)->getAnyDisk();

        // Assign part id here, since we need to record it into undo buffer before dump part to filesystem
        String relative_path = part_name;
        if (disk->getType() == DiskType::Type::ByteS3)
        {
            UUID part_id = newPartID(staged_part->info, txn_id.toUInt64());
            staged_part->uuid = part_id;
            relative_path = UUIDHelpers::UUIDToString(part_id);
        }

        undo_resources.emplace_back(txn_id, UndoResourceType::StagedPart, part_name, relative_path + '/');
        undo_resources.back().setDiskName(disk->getName());
        part_disks.emplace_back(std::move(disk));
    }

    try
    {
        context->getCnchCatalog()->writeUndoBuffer(UUIDHelpers::UUIDToString(storage.getStorageUUID()), txn_id, undo_resources);
        LOG_DEBUG(storage.getLogger(), "Wrote undo buffer for {} resources in {} ms", undo_resources.size(), watch.elapsedMilliseconds());
    }
    catch (...)
    {
        LOG_ERROR(storage.getLogger(), "Fail to write undo buffer");
        throw;
    }

    /// Parallel dumping to shared storage
    DumpedData result;
    S3ObjectMetadata::PartGeneratorID part_generator_id(S3ObjectMetadata::PartGeneratorID::TRANSACTION,
        curr_txn->getTransactionID().toString());
    MergeTreeCNCHDataDumper dumper(storage, part_generator_id);

    watch.restart();
    ThreadPool dump_pool(std::min(
        static_cast<size_t>(storage.getSettings()->cnch_parallel_dumping_threads), std::max(temp_staged_parts.size(), temp_parts.size())));
    result.parts.resize(temp_parts.size());
    /// TODO: only use pool if > 1 parts
    for (size_t i = 0; i < temp_parts.size(); ++i)
    {
        dump_pool.scheduleOrThrowOnError([&, i]() {
            const auto & temp_part = temp_parts[i];
            auto dumped_part = dumper.dumpTempPart(temp_part, false, part_disks[i]);
            LOG_TRACE(storage.getLogger(), "Dumped part {}", temp_part->name);
            result.parts[i] = std::move(dumped_part);
        });
    }
    dump_pool.wait();
    // TODO: dump all bitmaps to one file to avoid creating too many small files on vfs
    result.bitmaps = dumpDeleteBitmaps(storage, temp_bitmaps);
    result.staged_parts.resize(temp_staged_parts.size());
    for (size_t i = 0; i < temp_staged_parts.size(); ++i)
    {
        dump_pool.scheduleOrThrowOnError([&, i]() {
            const auto & temp_staged_part = temp_staged_parts[i];
            auto staged_part
                = dumper.dumpTempPart(temp_staged_part, false, part_disks[i + temp_parts.size()]);
            LOG_TRACE(storage.getLogger(), "Dumped staged part {}", temp_staged_part->name);
            result.staged_parts[i] = std::move(staged_part);
        });
    }
    dump_pool.wait();

    LOG_DEBUG(
        storage.getLogger(),
        "Dumped {} parts, {} bitmaps, {} staged parts in {} ms",
        temp_parts.size(),
        temp_bitmaps.size(),
        temp_staged_parts.size(),
        watch.elapsedMilliseconds());
    return result;
}

void CnchDataWriter::commitDumpedParts(const DumpedData & dumped_data)
{
    Stopwatch watch;
    const auto & settings = context->getSettingsRef();
    const auto & dumped_parts = dumped_data.parts;
    const auto & delete_bitmaps = dumped_data.bitmaps;
    const auto & dumped_staged_parts = dumped_data.staged_parts;

    if (dumped_parts.empty() && delete_bitmaps.empty() && dumped_staged_parts.empty())
        // Nothing to commit, returns
        return;

    TxnTimestamp txn_id = context->getCurrentTransactionID();

    TxnTimestamp commit_time;

    try
    {
        // Check if current transaction can directly be executed on current server
        if (dynamic_pointer_cast<CnchServerTransaction>(context->getCurrentTransaction()))
        {
            if (settings.debug_cnch_force_commit_parts_rpc)
            {
                auto server_client = context->getCnchServerClient("0.0.0.0", context->getRPCPort());
                commit_time = server_client->commitParts(txn_id, type, storage, dumped_parts, delete_bitmaps, dumped_staged_parts, task_id, false, consumer_group, tpl);
            }
            else
            {
                commitPreparedCnchParts(dumped_data);
            }
        }
        else
        {
            auto is_server = context->getServerType() == ServerType::cnch_server;
            CnchServerClientPtr server_client;
            if (const auto & client_info = context->getClientInfo(); client_info.rpc_port)
            {
                /// case: "insert select/infile" forward to worker | manipulation task | cnch system log flush | ingestion from kafka | etc
                server_client = context->getCnchServerClient(client_info.current_address.host().toString(), client_info.rpc_port);
            }
            else if (auto worker_txn = dynamic_pointer_cast<CnchWorkerTransaction>(context->getCurrentTransaction()); worker_txn)
            {
                /// case: client submits INSERTs directly to worker
                server_client = worker_txn->getServerClient();
            }
            else
            {
                throw Exception("Server with transaction " + txn_id.toString() + " is unknown", ErrorCodes::LOGICAL_ERROR);
            }

            commit_time = server_client->precommitParts(
                context, txn_id, type, storage, dumped_parts, delete_bitmaps, dumped_staged_parts, task_id, is_server, consumer_group, tpl);
        }
    }
    catch (const Exception & e)
    {
        /// TODO: Update committing of parts to follow 2-phase transaction flow
        /// Current implementation results in garbage parts on HDFS if commitParts RPC times out
        /// and committing of parts to Catalog is unsuccessful on server side
        if (e.code() == ErrorCodes::BRPC_TIMEOUT && (type == ManipulationType::Merge))
            LOG_WARNING(storage.getLogger(), "Commit merged task '" + task_id + "' to server timeout");
        else
            throw;
    }

    if (type == ManipulationType::Merge)
    {
        for (const auto & part : dumped_parts)
        {
            MergeMutateAction::updatePartData(part, commit_time);
            part->relative_path = part->info.getPartNameWithHintMutation();
        }
    }

    if (auto part_log = context->getPartLog(storage.getDatabaseName()))
    {
        // for (auto & dumped_part : dumped_parts)
            // part_log->add(PartLog::createElement(PartLogElement::COMMIT_PART, dumped_part, watch.elapsed()));
    }

    LOG_DEBUG(
        storage.getLogger(),
        "Committed {} parts, {} bitmaps, {} staged parts in transaction {}, elapsed {} ms",
        dumped_parts.size(),
        delete_bitmaps.size(),
        dumped_staged_parts.size(),
        toString(UInt64(txn_id)),
        watch.elapsedMilliseconds());
}

TxnTimestamp CnchDataWriter::commitPreparedCnchParts(const DumpedData & dumped_data)
{
    Stopwatch watch;

    if (context->getServerType() != ServerType::cnch_server)
        throw Exception(ErrorCodes::LOGICAL_ERROR, "Must be called in Server mode: {}", context->getServerType());

    const auto & txn_coordinator = context->getCnchTransactionCoordinator();
    auto * log = storage.getLogger();
    auto txn = context->getCurrentTransaction();
    auto txn_id = txn->getTransactionID();
    TxnTimestamp commit_time;
    /// set main table uuid in server side
    txn->setMainTableUUID(storage.getStorageUUID());

    auto storage_ptr = storage.shared_from_this();
    if (!storage_ptr)
        throw Exception("storage_ptr is nullptr and invalid for use", ErrorCodes::NULL_POINTER_DEREFERENCE);

    do
    {
        if (type == ManipulationType::Insert)
        {
            if (dumped_data.parts.empty() && dumped_data.bitmaps.empty() && dumped_data.staged_parts.empty()
                && consumer_group.empty())
            {
                LOG_DEBUG(log, "Nothing to commit, we skip this call.");
                break;
            }

            if (!tpl.empty() && !consumer_group.empty())
                txn->setKafkaTpl(consumer_group, tpl);

            /// check the part is already correctly clustered for bucket table. All new inserted parts should be clustered.
            // if (storage->isBucketTable())
            // {
            //     auto table_definition_hash = storage->getTableHashForClusterBy();
            //     for (auto & part : prepared_parts)
            //     {
            //         if (context->getSettings().skip_table_definition_hash_check)
            //             part->table_definition_hash = table_definition_hash;

            //         if (!part->deleted &&
            //             (part->bucket_number < 0 || table_definition_hash != part->table_definition_hash))
            //         {
            //             throw Exception(
            //                 "Part " + part->name + " is not clustered or it has different table definition with storage. Part bucket number : "
            //                 + std::to_string(part->bucket_number) + ", part table_definition_hash : [" + std::to_string(part->table_definition_hash)
            //                 + "], table's table_definition_hash : [" + std::to_string(table_definition_hash) + "]",
            //                 ErrorCodes::BUCKET_TABLE_ENGINE_MISMATCH);
            //         }
            //     }
            // }

            // Precommit stage. Write intermediate parts to KV
            auto action
                = txn->createAction<InsertAction>(storage_ptr, dumped_data.parts, dumped_data.bitmaps, dumped_data.staged_parts);
            txn->appendAction(action);
            action->executeV2();
        }
        else if (type == ManipulationType::Drop)
        {
            if (dumped_data.parts.empty())
            {
                LOG_DEBUG(log, "No parts to commit, we skip this call.");
                break;
            }

            auto action = txn->createAction<DropRangeAction>(txn->getTransactionRecord(), storage_ptr);
            for (const auto & part : dumped_data.parts)
                action->as<DropRangeAction &>().appendPart(part);
            for (const auto & bitmap : dumped_data.bitmaps)
                action->as<DropRangeAction &>().appendDeleteBitmap(bitmap);

            txn->appendAction(std::move(action));
            commit_time = txn_coordinator.commitV2(txn);

            LOG_TRACE(
                log,
                "Committed {} parts in transaction {}, elapsed {} ms",
                dumped_data.parts.size(),
                txn_id.toUInt64(),
                watch.elapsedMilliseconds());
        }
        else if (type == ManipulationType::Merge || type == ManipulationType::Clustering || type == ManipulationType::Mutate)
        {
            auto bg_thread = context->getCnchBGThread(CnchBGThreadType::MergeMutate, storage.getStorageID());
            auto * merge_mutate_thread = dynamic_cast<CnchMergeMutateThread *>(bg_thread.get());
            if (dumped_data.parts.empty())
            {
                LOG_WARNING(log, "No parts to commit, worker may failed to merge parts, which task_id is {}", task_id);
                merge_mutate_thread->tryRemoveTask(task_id);
            }
            else
            {
                merge_mutate_thread->finishTask(task_id, dumped_data.parts.front(), [&] {
                    auto action = txn->createAction<MergeMutateAction>(txn->getTransactionRecord(), type, storage_ptr);

                    for (const auto & part : dumped_data.parts)
                        action->as<MergeMutateAction &>().appendPart(part);

                    action->as<MergeMutateAction &>().setDeleteBitmaps(dumped_data.bitmaps);
                    txn->appendAction(std::move(action));
                    commit_time = txn_coordinator.commitV2(txn);

                    LOG_TRACE(
                        log,
                        "Committed {} parts in transaction {}, elapsed {} ms",
                        dumped_data.parts.size(),
                        txn_id.toUInt64(),
                        watch.elapsedMilliseconds());
                });
            }
        }
        else
        {
            throw Exception(ErrorCodes::NOT_IMPLEMENTED, "Not support commit type {}", typeToString(type));
        }
    } while (false);

    return commit_time;
}

void CnchDataWriter::publishStagedParts(
    const MergeTreeDataPartsCNCHVector & staged_parts,
    const LocalDeleteBitmaps & bitmaps_to_dump)
{
    DumpedData items;
    TxnTimestamp txn_id = context->getCurrentTransactionID();

    for (auto & staged_part : staged_parts)
    {
        // new part that shares the data file with the staged part
        Protos::DataModelPart new_part_model;
        fillPartModel(storage, *staged_part, new_part_model);
        new_part_model.mutable_part_info()->set_mutation(txn_id);
        new_part_model.set_txnid(txn_id);
        new_part_model.clear_commit_time();
        new_part_model.set_staging_txn_id(staged_part->info.mutation);
        // storage may not have part columns info (CloudMergeTree), so set columns/columns_commit_time manually
        auto new_part = createPartFromModelCommon(storage, new_part_model);
        new_part->setColumnsPtr(std::make_shared<NamesAndTypesList>(staged_part->getColumns()));
        new_part->columns_commit_time = staged_part->columns_commit_time;

        /// staged drop part
        MergeTreePartInfo drop_part_info = staged_part->info.newDropVersion(txn_id, StorageType::HDFS);
        auto drop_part = std::make_shared<MergeTreeDataPartCNCH>(
            storage, drop_part_info.getPartName(), drop_part_info, staged_part->volume, std::nullopt);
        drop_part->partition = staged_part->partition;
        drop_part->bucket_number = staged_part->bucket_number;
        drop_part->deleted = true;

        items.parts.emplace_back(std::move(new_part));
        items.staged_parts.emplace_back(std::move(drop_part));
    }

    /// prepare undo resources
    /// setMetadata() return reference, so need to cast move
    std::vector<UndoResource> undo_resources;
    for (auto & part : items.parts)
        undo_resources.emplace_back(std::move(UndoResource(txn_id, UndoResourceType::Part, part->info.getPartNameWithHintMutation()).setMetadataOnly(true)));
    for (auto & staged_part : items.staged_parts)
        undo_resources.emplace_back(std::move(UndoResource(txn_id, UndoResourceType::StagedPart, staged_part->info.getPartNameWithHintMutation()).setMetadataOnly(true)));
    for (auto & bitmap : bitmaps_to_dump)
        undo_resources.emplace_back(bitmap->getUndoResource(txn_id));

    /// write undo buffer
    Stopwatch watch;
    try
    {
        context->getCnchCatalog()->writeUndoBuffer(UUIDHelpers::UUIDToString(storage.getStorageUUID()), txn_id, std::move(undo_resources));
        LOG_DEBUG(storage.getLogger(), "Wrote undo buffer for {} resources in {} ms", undo_resources.size(), watch.elapsedMilliseconds());
    }
    catch (...)
    {
        LOG_ERROR(storage.getLogger(), "Fail to write undo buffer");
        throw;
    }

    /// dump delete bitmaps
    items.bitmaps = dumpDeleteBitmaps(storage, bitmaps_to_dump);

    commitDumpedParts(items);
}

void CnchDataWriter::preload(const MutableMergeTreeDataPartsCNCHVector & dumped_parts)
{
    auto & settings = context->getSettingsRef();
    if (settings.enable_preload_parts || storage.getSettings()->enable_preload_parts)
    {
        bool sync_preload = !settings.enable_async_preload_parts;

        try
        {
            Stopwatch timer;
            auto server_client = context->getCnchServerClientPool().get();
            MutableMergeTreeDataPartsCNCHVector preload_parts;
            std::copy_if(dumped_parts.begin(), dumped_parts.end(), std::back_inserter(preload_parts), [](const auto & part) {
                return !part->deleted && !part->isPartial();
            });

            if (!preload_parts.empty())
            {
                auto max_timeout = std::max(30 * 1000L, settings.max_execution_time.totalMilliseconds());
                server_client->submitPreloadTask(storage, preload_parts, sync_preload, max_timeout);
                LOG_DEBUG(
                    storage.getLogger(),
                    "Finish submit preload task for {} parts to server {}, elapsed {} ms", preload_parts.size(), server_client->getRPCAddress(), timer.elapsedMilliseconds());
            }
            // TODO: invalidate deleted part's disk cache
        }
        catch (...)
        {
            tryLogCurrentException(__PRETTY_FUNCTION__, "Fail to preload");
            if (sync_preload)
                throw;
        }
    }
}

UUID CnchDataWriter::newPartID(const MergeTreePartInfo& part_info, UInt64 txn_timestamp)
{
    UUID random_id = UUIDHelpers::generateV4();
    PairInt64 random_id_pair = UUIDHelpers::UUIDToPairInt64(random_id);
    UInt64& random_id_low = random_id_pair.low;
    UInt64& random_id_high = random_id_pair.high;
    boost::hash_combine(random_id_low, part_info.min_block);
    boost::hash_combine(random_id_high, part_info.max_block);
    boost::hash_combine(random_id_low, part_info.mutation);
    boost::hash_combine(random_id_high, txn_timestamp);
    return random_id;
}

}
