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

#include <Interpreters/trySetVirtualWarehouse.h>

#include <Interpreters/DatabaseAndTableWithAlias.h>
#include <Interpreters/VirtualWarehousePool.h>
#include <Parsers/ASTInsertQuery.h>
#include <Parsers/ASTRefreshQuery.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <Parsers/queryToString.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
// TODO(zuochuang.zema) MERGE storage
// #include <Storages/StorageCnchMergeTree.h>
#include <Storages/StorageCnchHive.h>
#include <Storages/StorageMaterializedView.h>
#include <Storages/StorageView.h>
#include <unicode/tzfmt.h>
#include "Interpreters/DatabaseCatalog.h"
#include "Interpreters/StorageID.h"
#include "Storages/StorageCnchMergeTree.h"


namespace DB
{

/// Forward declaration
static bool trySetVirtualWarehouseFromAST(const ASTPtr & ast, ContextMutablePtr & context);

static void setVirtualWarehouseByName(const String & vw_name, ContextMutablePtr & context)
{
    auto vw_handle = context->getVirtualWarehousePool().get(vw_name);
    context->setCurrentVW(std::move(vw_handle));
}

[[maybe_unused]] static bool trySetVirtualWarehouseFromTable([[maybe_unused]] const String & database, [[maybe_unused]] const String & table, [[maybe_unused]] ContextMutablePtr & context)
{
    LOG_DEBUG(&Poco::Logger::get("trySetVirtualWarehouse"), "Try set virtual warehouse for table `{}.{}`", database, table);
    auto & database_catalog = DatabaseCatalog::instance();
    StorageID table_id(database, table);
    auto storage = database_catalog.tryGetTable(table_id, context);
    if (!storage)
        return false;

    if (auto cnch_table = dynamic_cast<StorageCnchMergeTree *>(storage.get()))
    {
        String vw_name = cnch_table->getSettings()->cnch_vw_default;

        setVirtualWarehouseByName(vw_name, context);
        LOG_DEBUG(
            &Poco::Logger::get("trySetVirtualWarehouse"),
            "Set virtual warehouse {} from {}", context->getCurrentVW()->getName(), storage->getStorageID().getNameForLogs());
        return true;
    }
    else if (auto cnchhive = dynamic_cast<StorageCnchHive *>(storage.get()))
    {
        String vw_name = cnchhive->settings.cnch_vw_default;

        setVirtualWarehouseByName(vw_name, context);
        LOG_DEBUG(
            &Poco::Logger::get("trySetVirtualWarehouse"),
            "CnchHive Set virtual warehouse {} from {}", context->getCurrentVW()->getName(), storage->getStorageID().getNameForLogs());
        return true;
    }
    else if (auto * view_table = dynamic_cast<StorageView *>(storage.get()))
    {
        if (trySetVirtualWarehouseFromAST(view_table->getInnerQuery(), context))
            return true;
    }
    // else if (auto mv_table = dynamic_cast<StorageMaterializedView *>(storage.get()))
    // {
    //     if (trySetVirtualWarehouseFromAST(mv_table->getInnerQuery(), context))
    //         return true;
    // }

    return false;
}

static bool trySetVirtualWarehouseFromAST([[maybe_unused]] const ASTPtr & ast, [[maybe_unused]] ContextMutablePtr & context)
{
    LOG_DEBUG(&Poco::Logger::get("trySetVirtualWarehouse"), "Trying to set virtual warehouse for expression `{}` ...", ast->formatForErrorMessage());
    do
    {
        auto & database_catalog = DatabaseCatalog::instance();
        if (auto * insert = ast->as<ASTInsertQuery>())
        {
            auto table_id = insert->table_id;
            if (table_id.database_name.empty())
                table_id.database_name = context->getCurrentDatabase();
            auto storage = database_catalog.tryGetTable(table_id, context);
            auto * cnch_table = dynamic_cast<StorageCnchMergeTree *>(storage.get());
            if (!cnch_table)
                break;

            String vw_name = cnch_table->getSettings()->cnch_vw_write;

            setVirtualWarehouseByName(vw_name, context);
            LOG_DEBUG(
                &Poco::Logger::get("trySetVirtualWarehouse"),
                "Set virtual warehouse {} from {}", context->getCurrentVW()->getName(), storage->getStorageID().getNameForLogs());
            return true;
        }
        else if (auto * table_expr = ast->as<ASTTableExpression>())
        {
            if (!table_expr->database_and_table_name)
                break;

            DatabaseAndTableWithAlias db_and_table(table_expr->database_and_table_name);
            if (db_and_table.database.empty()) db_and_table.database = context->getCurrentDatabase();
            if (trySetVirtualWarehouseFromTable(db_and_table.database, db_and_table.table, context))
                return true;
        }
        /// XXX: This is a hack solution for an uncommonn query `SELECT ... WHERE x IN table`
        /// which may cause some rare bugs if the identifiers confict with table names.
        /// But I don't want to make the problem complex ..
        else if (auto * func_expr = ast->as<ASTFunction>())
        {
            if (!func_expr->arguments || func_expr->arguments->children.size() != 2)
                break;
            if (func_expr->name != "in" && func_expr->name != "notIn")
                break;

            auto * id = func_expr->arguments->children.back()->as<ASTIdentifier>();
            if (!id || id->nameParts().size() > 2)
                break;

            String database = id->nameParts().empty() ? "" : id->nameParts().front();
            String table = id->nameParts().empty() ? id->name() : id->nameParts().back();
            if (trySetVirtualWarehouseFromTable(database, table, context))
                return true;
        }
        else if (auto * refresh_mv = ast->as<ASTRefreshQuery>())
        {
            auto storage = database_catalog.tryGetTable(StorageID(refresh_mv->database, refresh_mv->table), context);
            auto * view_table = dynamic_cast<StorageMaterializedView *>(storage.get());
            if (!view_table)
                break;

            if (trySetVirtualWarehouseFromTable(refresh_mv->database, refresh_mv->table, context))
               return true;
        }

    } while (false);

    for (auto & child : ast->children)
    {
        if (trySetVirtualWarehouseFromAST(child, context))
            return true;
    }

    return false;
}

bool trySetVirtualWarehouse(const ASTPtr & ast, ContextMutablePtr & context)
{
    if (context->tryGetCurrentVW())
        return true;

    LOG_DEBUG(&Poco::Logger::get("trySetVirtualWarehouse"), "Trying to set virtual warehouse for query `{}` ...", ast->formatForErrorMessage());

    if (const auto & vw_name = context->getSettingsRef().virtual_warehouse.value; !vw_name.empty())
    {
        setVirtualWarehouseByName(vw_name, context);
        LOG_DEBUG(
            &Poco::Logger::get("trySetVirtualWarehouse"),
            "Set virtual warehouse {} from query settings", context->getCurrentVW()->getName());
        return true;
    }
    else
    {
        return trySetVirtualWarehouseFromAST(ast, context);
    }
}

bool trySetVirtualWarehouseAndWorkerGroup(const ASTPtr & ast, ContextMutablePtr & context)
{
    if (context->tryGetCurrentWorkerGroup())
        return true;

    if (trySetVirtualWarehouse(ast, context))
    {
        auto value = context->getSettingsRef().vw_schedule_algo.value;
        auto algo = ResourceManagement::toVWScheduleAlgo(&value[0]);
        auto worker_group = context->getCurrentVW()->pickWorkerGroup(algo);
        LOG_DEBUG(&Poco::Logger::get("trySetVirtualWarehouse"), "Picked worker group {}", worker_group->getQualifiedName());

        context->setCurrentWorkerGroup(std::move(worker_group));
        return true;
    }
    else
    {
        return false;
    }
}

VirtualWarehouseHandle getVirtualWarehouseForTable([[maybe_unused]] const MergeTreeMetaBase & storage, [[maybe_unused]] const ContextPtr & context)
{
    String vw_name;
    String source;

    if (const auto & name = context->getSettingsRef().virtual_warehouse.value; !name.empty())
    {
        vw_name = name;
        source = "query settings";
    }
    else
    {
        vw_name = storage.getSettings()->cnch_vw_default;
        source = "table settings of " + storage.getStorageID().getNameForLogs();
    }

    auto vw = context->getVirtualWarehousePool().get(vw_name);
    LOG_DEBUG(&Poco::Logger::get("trySetVirtualWarehouse"), "Get virtual warehouse {} from {}", vw->getName(), source);

    return vw;
}

WorkerGroupHandle getWorkerGroupForTable(const MergeTreeMetaBase & storage, const ContextPtr & context)
{
    auto vw = getVirtualWarehouseForTable(storage, context);
    auto value = context->getSettingsRef().vw_schedule_algo.value;
    auto algo = ResourceManagement::toVWScheduleAlgo(&value[0]);
    auto worker_group = vw->pickWorkerGroup(algo);

    LOG_DEBUG(&Poco::Logger::get("trySetVirtualWarehouse"), "Picked worker group {}", worker_group->getQualifiedName());
    return worker_group;
}

}
