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

#include "DDLDropAction.h"

#include <Catalog/Catalog.h>
// #include <Interpreters/CnchSystemLog.h>
#include <Parsers/ASTDropQuery.h>
// #include <Storages/Kafka/StorageCnchKafka.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH.h>
#include <Storages/StorageCnchMergeTree.h>
// #include <DaemonManager/DaemonManagerClient.h>
#include <CloudServices/CnchServerClient.h>
#include <Transaction/TransactionCoordinatorRcCnch.h>
#include <Common/ErrorCodes.h>

namespace DB
{

namespace ErrorCodes
{
    extern const ErrorCode SYNTAX_ERROR;
}

void DDLDropAction::executeV1(TxnTimestamp commit_time)
{
    Catalog::CatalogPtr cnch_catalog = global_context.getCnchCatalog();

    if (!params.database.empty() && params.table.empty())
    {
        // drop database
        if (params.kind == ASTDropQuery::Kind::Drop)
        {
            for (auto & table : tables)
                updateTsCache(table->getStorageUUID(), commit_time);
            cnch_catalog->dropDatabase(params.database, params.prev_version, txn_id, commit_time); /*mark database deleted, we just remove database now.*/
        }
        else if (params.kind == ASTDropQuery::Kind::Truncate)
        {
            throw Exception("Unable to truncate database.", ErrorCodes::SYNTAX_ERROR);
        }
    }
    else if (!params.database.empty() && !params.table.empty())
    {
        if (tables.size()==1 && tables[0])
        {
            auto table = tables[0];

            // drop table
            if (params.kind == ASTDropQuery::Kind::Drop)
            {
                updateTsCache(table->getStorageUUID(), commit_time);
                cnch_catalog->dropTable(table, params.prev_version, txn_id, commit_time);
            }
            else if (params.kind == ASTDropQuery::Kind::Detach)
            {
                updateTsCache(table->getStorageUUID(), commit_time);
                cnch_catalog->detachTable(params.database, params.table, commit_time);
            }
            else if (params.kind == ASTDropQuery::Kind::Truncate)
            {
                throw Exception("Logical error: shouldn't be here", ErrorCodes::LOGICAL_ERROR);
            }
        }
        else if (params.is_dictionary)
        {
            if (params.kind == ASTDropQuery::Kind::Drop)
                cnch_catalog->dropDictionary(params.database, params.table);
            else if (params.kind == ASTDropQuery::Kind::Detach)
                cnch_catalog->detachDictionary(params.database, params.table);
        }
    }
}

void DDLDropAction::updateTsCache(const UUID & uuid, const TxnTimestamp & commit_time)
{
    auto & ts_cache_manager = global_context.getCnchTransactionCoordinator().getTsCacheManager();
    auto table_guard = ts_cache_manager.getTimestampCacheTableGuard(uuid);
    auto & ts_cache = ts_cache_manager.getTimestampCacheUnlocked(uuid);
    ts_cache->insertOrAssign(UUIDHelpers::UUIDToString(uuid), commit_time);
}

}
