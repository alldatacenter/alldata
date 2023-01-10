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

#include "DDLCreateAction.h"

#include <Catalog/Catalog.h>
#include <Transaction/TransactionCoordinatorRcCnch.h>

namespace DB
{

void DDLCreateAction::executeV1(TxnTimestamp commit_time)
{
    Catalog::CatalogPtr cnch_catalog = global_context.getCnchCatalog();

    if (!params.database.empty() && params.table.empty())
    {
        /// create database
        assert(!params.attach);
        cnch_catalog->createDatabase(params.database, params.uuid, txn_id, commit_time);
    }
    else if (!params.is_dictionary)
    {
        /// create table
        updateTsCache(params.uuid, commit_time);
        if (params.attach)
            cnch_catalog->attachTable(params.database, params.table, commit_time);
        else
            cnch_catalog->createTable(StorageID{params.database, params.table, params.uuid}, params.statement, "", txn_id, commit_time);
    }
    else
    {
        /// for dictionary
        if (params.attach)
            cnch_catalog->attachDictionary(params.database, params.table);
        else
            cnch_catalog->createDictionary(StorageID{params.database, params.table, params.uuid}, params.statement);
    }
}

void DDLCreateAction::updateTsCache(const UUID & uuid, const TxnTimestamp & commit_time)
{
    auto & ts_cache_manager = global_context.getCnchTransactionCoordinator().getTsCacheManager();
    auto table_guard = ts_cache_manager.getTimestampCacheTableGuard(uuid);
    auto & ts_cache = ts_cache_manager.getTimestampCacheUnlocked(uuid);
    ts_cache->insertOrAssign(UUIDHelpers::UUIDToString(uuid), commit_time);
}

void DDLCreateAction::abort() {}

}
