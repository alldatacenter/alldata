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

#include <Transaction/Actions/DDLAlterAction.h>

#include <Catalog/Catalog.h>
// #include <Storages/MergeTree/CnchMergeTreeMutationEntry.h>
// #include <Storages/StorageCnchMergeTree.h>
#include <Transaction/TransactionCoordinatorRcCnch.h>
#include <Storages/MergeTree/CnchMergeTreeMutationEntry.h>
#include <Storages/StorageCnchMergeTree.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

void DDLAlterAction::setNewSchema(String schema_)
{
    new_schema = schema_;
}

void DDLAlterAction::setMutationCommands(MutationCommands commands)
{
    /// Sanity check. Avoid mixing other commands with recluster command.
    if (commands.size() > 1)
    {
        for (auto & cmd : commands)
        {
            if (cmd.type == MutationCommand::Type::RECLUSTER)
                throw Exception("Cannot modify cluster by definition and other table schema together.", ErrorCodes::LOGICAL_ERROR);
        }
    }
    mutation_commands = std::move(commands);
}

void DDLAlterAction::executeV1(TxnTimestamp commit_time)
{
    /// In DDLAlter, we only update schema.
    LOG_DEBUG(log, "Wait for change schema in Catalog.");
    auto catalog = global_context.getCnchCatalog();
    try
    {
        if (!mutation_commands.empty())
        {
            CnchMergeTreeMutationEntry mutation_entry;
            mutation_entry.txn_id = txn_id;
            mutation_entry.commit_time = commit_time;
            mutation_entry.commands = mutation_commands;
            mutation_entry.columns_commit_time = mutation_commands.changeSchema() ? commit_time : table->commit_time;
            catalog->createMutation(table->getStorageID(), mutation_entry.txn_id.toString(), mutation_entry.toString());
            /// table default cluster status is true. Change to false if current mutation is resluster mutation.
            if (mutation_entry.isReclusterMutation())
                catalog->setTableClusterStatus(table->getStorageUUID(), false);
            LOG_DEBUG(log, "Successfully create mutation for alter query.");
        }

        // auto cache = global_context.getMaskingPolicyCache();
        // table->checkMaskingPolicy(*cache);

        updateTsCache(table->getStorageUUID(), commit_time);
        if (!new_schema.empty())
        {
            catalog->alterTable(table, new_schema, static_cast<StorageCnchMergeTree &>(*table).commit_time, txn_id, commit_time);
            LOG_DEBUG(log, "Successfully change schema in catalog.");
        }
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);
        catalog->removeMutation(table->getStorageID(), txn_id.toString());
        throw;
    }
}

void DDLAlterAction::updatePartData(MutableMergeTreeDataPartCNCHPtr part, TxnTimestamp commit_time)
{
    part->commit_time = commit_time;
}

void DDLAlterAction::updateTsCache(const UUID & uuid, const TxnTimestamp & commit_time)
{
    auto & ts_cache_manager = global_context.getCnchTransactionCoordinator().getTsCacheManager();
    auto table_guard = ts_cache_manager.getTimestampCacheTableGuard(uuid);
    auto & ts_cache = ts_cache_manager.getTimestampCacheUnlocked(uuid);
    ts_cache->insertOrAssign(UUIDHelpers::UUIDToString(uuid), commit_time);
}

}
