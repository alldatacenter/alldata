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

#include <mutex>
#include <Databases/DatabaseCnch.h>

#include <Catalog/Catalog.h>
#include <Interpreters/Context.h>
#include <Interpreters/ExternalDictionariesLoader.h>
#include <Parsers/ASTCreateQuery.h>
#include <Parsers/ParserCreateQuery.h>
#include <Parsers/formatAST.h>
#include <Parsers/parseQuery.h>
#include <Transaction/Actions/DDLCreateAction.h>
#include <Transaction/Actions/DDLDropAction.h>
#include <Transaction/ICnchTransaction.h>
#include <Transaction/TransactionCoordinatorRcCnch.h>
#include <Common/StringUtils/StringUtils.h>
#include <common/scope_guard.h>
#include <Common/Exception.h>
#include <common/logger_useful.h>
#include <Transaction/Actions/DDLRenameAction.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int CNCH_TRANSACTION_NOT_INITIALIZED;
    extern const int SYSTEM_ERROR;
    extern const int TABLE_ALREADY_EXISTS;
    extern const int NOT_IMPLEMENTED;
}

class CnchDatabaseTablesSnapshotIterator final : public DatabaseTablesSnapshotIterator
{
public:
    using DatabaseTablesSnapshotIterator::DatabaseTablesSnapshotIterator;
    UUID uuid() const override { return table()->getStorageID().uuid; }
};

DatabaseCnch::DatabaseCnch(const String & name_, UUID uuid, ContextPtr local_context)
    : IDatabase(name_)
    , WithContext(local_context->getGlobalContext())
    , db_uuid(uuid)
    , log(&Poco::Logger::get("DatabaseCnch (" + name_ + ")"))
{
    LOG_DEBUG(log, "Create database {} in query {}", database_name, local_context->getCurrentQueryId());
}

void DatabaseCnch::createTable(ContextPtr local_context, const String & table_name, const StoragePtr & table, const ASTPtr & query)
{
    LOG_DEBUG(log, "Create table {} in query {}", table_name, local_context->getCurrentQueryId());
    auto txn = local_context->getCurrentTransaction();
    if (!txn)
        throw Exception("Cnch transaction is not initialized", ErrorCodes::CNCH_TRANSACTION_NOT_INITIALIZED);
    if (!query->as<ASTCreateQuery>())
        throw Exception(ErrorCodes::LOGICAL_ERROR, "Query is not create query");

    // Disable create table as function for cnch database first.
    // Todo: add proper support for this new feature
    auto create_query = query->as<ASTCreateQuery &>();
    if (!create_query.storage && create_query.as_table_function)
        throw Exception(ErrorCodes::SUPPORT_IS_DISABLED, "create table as table function is not supported under cnch database");

    if ((!create_query.is_dictionary) && (!create_query.isView()) &&
        (!create_query.storage->engine || !startsWith(create_query.storage->engine->name, "Cnch")))
        throw Exception(ErrorCodes::SUPPORT_IS_DISABLED, "Cnch database only suport creating Cnch tables");

    String statement = serializeAST(*query);
    CreateActionParams params = {getDatabaseName(), table_name, table->getStorageUUID(), statement, create_query.attach, table->isDictionary()};
    auto create_table = txn->createAction<DDLCreateAction>(std::move(params));
    txn->appendAction(std::move(create_table));
    txn->commitV1();
    LOG_TRACE(log, "Successfully create table {} in query {}", table_name, local_context->getCurrentQueryId());
    if (table->isDictionary())
    {
        local_context->getExternalDictionariesLoader().reloadConfig("CnchCatalogRepository");
        LOG_TRACE(log, "Successfully add dictionary config for {}", table_name, local_context->getCurrentQueryId());
    }
}

void DatabaseCnch::dropTable(ContextPtr local_context, const String & table_name, bool no_delay)
{
    LOG_DEBUG(log, "Drop table {} in query {}, no delay {}", table_name, local_context->getCurrentQueryId(), no_delay);

    auto txn = local_context->getCurrentTransaction();

    if (!txn)
        throw Exception("Cnch transaction is not initialized", ErrorCodes::CNCH_TRANSACTION_NOT_INITIALIZED);

    StoragePtr storage = local_context->getCnchCatalog()->tryGetTable(*local_context, getDatabaseName(), table_name, TxnTimestamp::maxTS());
    bool is_dictionary = false;
    TxnTimestamp previous_version = 0;
    if (!storage)
    {
        if (!local_context->getCnchCatalog()->isDictionaryExists(getDatabaseName(), table_name))
            throw Exception("Can't get storage for table " + table_name, ErrorCodes::SYSTEM_ERROR);
        else
            is_dictionary = true;
    }
    else
    {
        previous_version = storage->commit_time;
    }

    DropActionParams params{getDatabaseName(), table_name, previous_version, ASTDropQuery::Kind::Drop, is_dictionary};
    auto drop_action = txn->createAction<DDLDropAction>(std::move(params), std::vector{std::move(storage)});
    txn->appendAction(std::move(drop_action));
    txn->commitV1();
    if (is_dictionary)
        local_context->getExternalDictionariesLoader().reloadConfig("CnchCatalogRepository");

    std::lock_guard wr{cache_mutex};
    if (cache.contains(table_name))
        cache.erase(table_name);
}

void DatabaseCnch::drop(ContextPtr local_context)
{
    LOG_DEBUG(log, "Drop database {} in query {}", database_name, local_context->getCurrentQueryId());

    auto txn = local_context->getCurrentTransaction();

    if (!txn)
        throw Exception("Cnch transaction is not initialized", ErrorCodes::CNCH_TRANSACTION_NOT_INITIALIZED);

    // get the lock of tables in current database
    std::vector<StoragePtr> tables_to_drop;
    std::vector<IntentLockPtr> locks;

    for (auto iterator = getTablesIterator(getContext(), [](const String &) { return true; }); iterator->isValid(); iterator->next())
    {
        StoragePtr table = iterator->table();
        const auto & storage_id = table->getStorageID();
        locks.emplace_back(txn->createIntentLock(IntentLock::TB_LOCK_PREFIX, storage_id.database_name, storage_id.table_name));
        tables_to_drop.emplace_back(table);
    }

    for (const auto & lock : locks)
        lock->lock();

    DropActionParams params{getDatabaseName(), "", commit_time, ASTDropQuery::Kind::Drop};
    auto drop_action = txn->createAction<DDLDropAction>(std::move(params));
    txn->appendAction(std::move(drop_action));
    txn->commitV1();
}

void DatabaseCnch::detachTablePermanently(ContextPtr local_context, const String & table_name)
{
    TransactionCnchPtr txn = local_context->getCurrentTransaction();

    if (!txn)
        throw Exception("Cnch transaction is not initialized", ErrorCodes::CNCH_TRANSACTION_NOT_INITIALIZED);

    StoragePtr storage = local_context->getCnchCatalog()->tryGetTable(*local_context, getDatabaseName(), table_name, TxnTimestamp::maxTS());
    bool is_dictionary = false;
    TxnTimestamp previous_version = 0;
    if (!storage)
    {
        if (!local_context->getCnchCatalog()->isDictionaryExists(getDatabaseName(), table_name))
            throw Exception("Can't get storage for table " + table_name, ErrorCodes::SYSTEM_ERROR);
        else
            is_dictionary = true;
    }
    else
        previous_version = storage->commit_time;

    /// detach table action
    DropActionParams params{getDatabaseName(), table_name, previous_version, ASTDropQuery::Kind::Detach, is_dictionary};
    auto detach_action = txn->createAction<DDLDropAction>(std::move(params), std::vector{storage});
    txn->appendAction(std::move(detach_action));
    txn->commitV1();

    if (is_dictionary)
        local_context->getExternalDictionariesLoader().reloadConfig("CnchCatalogRepository");

    std::lock_guard wr{cache_mutex};
    if (cache.contains(table_name))
        cache.erase(table_name);
}

ASTPtr DatabaseCnch::getCreateDatabaseQuery() const
{
    auto settings = getContext()->getSettingsRef();
    String query = "CREATE DATABASE " + backQuoteIfNeed(getDatabaseName()) + " ENGINE = Cnch";
    ParserCreateQuery parser(ParserSettings::valueOf(settings.dialect_type));
    ASTPtr ast = parseQuery(parser, query.data(), query.data() + query.size(), "", 0, settings.max_parser_depth);
    return ast;
}

bool DatabaseCnch::isTableExist(const String & name, ContextPtr local_context) const
{
    return (local_context->getCnchCatalog()->isTableExists(getDatabaseName(), name, local_context->getCurrentTransactionID().toUInt64())) ||
        (local_context->getCnchCatalog()->isDictionaryExists(getDatabaseName(), name));
}

StoragePtr DatabaseCnch::tryGetTable(const String & name, ContextPtr local_context) const
{
    try
    {
        {
            std::shared_lock rd{cache_mutex};
            auto it = cache.find(name);
            if (it != cache.end())
                return it->second;
        }
        auto res = tryGetTableImpl(name, local_context);
        if (res && !res->is_detached && !res->is_dropped)
        {
            std::lock_guard wr{cache_mutex};
            cache.emplace(name, res);
            return res;
        }
    }
    catch (Exception & e)
    {
        if (e.code() != ErrorCodes::UNKNOWN_TABLE)
            throw e;
    }
    return nullptr;
}

DatabaseTablesIteratorPtr DatabaseCnch::getTablesIterator(ContextPtr local_context, const FilterByNameFunction & filter_by_table_name)
{
    Tables tables;
    Strings names = local_context->getCnchCatalog()->getTablesInDB(getDatabaseName());
    std::for_each(names.begin(), names.end(), [this, &local_context, &tables](const String & name) {
        StoragePtr storage = tryGetTableImpl(name, local_context);
        if (!storage || storage->is_detached || storage->is_dropped)
            return;
        /// debug
        StorageID storage_id = storage->getStorageID();
        LOG_DEBUG(
            log,
            "UUID {} database {} table {}",
            UUIDHelpers::UUIDToString(storage_id.uuid),
            storage_id.getDatabaseName(),
            storage_id.getTableName());
        /// end debug
        tables.try_emplace(name, std::move(storage));
    });

    if (!filter_by_table_name)
        return std::make_unique<CnchDatabaseTablesSnapshotIterator>(std::move(tables), database_name);

    Tables filtered_tables;
    for (const auto & [table_name, storage] : tables)
        if (filter_by_table_name(table_name))
            filtered_tables.emplace(table_name, storage);

    return std::make_unique<CnchDatabaseTablesSnapshotIterator>(std::move(filtered_tables), database_name);
}

bool DatabaseCnch::empty() const
{
    Strings tables = getContext()->getCnchCatalog()->getTablesInDB(getDatabaseName());
    return tables.empty();
}

ASTPtr DatabaseCnch::getCreateTableQueryImpl(const String & name, ContextPtr local_context, bool throw_on_error) const
{
    StoragePtr storage = getContext()->getCnchCatalog()->tryGetTable(
            *local_context, getDatabaseName(), name, local_context->getCurrentTransactionID().toUInt64());

    if (!storage)
    {
        try
        {
            return getContext()->getCnchCatalog()->getCreateDictionary(getDatabaseName(), name);
        }
        catch (...)
        {
            if (throw_on_error)
                throw;
            else
                LOG_DEBUG(
                    log,
                    "Fail to get create query for dictionary {} in datase {} query id {}",
                    name,
                    getDatabaseName(),
                    local_context->getCurrentQueryId());
        }
    }

    if ((!storage) && throw_on_error)
        throw Exception("Table " + getDatabaseName() + "." + name + " doesn't exist.", ErrorCodes::UNKNOWN_TABLE);

    if (!storage)
        return {};

    String create_table_query = storage->getCreateTableSql();
    ParserCreateQuery p_create_query;
    ASTPtr ast{};
    try
    {
        ast = parseQuery(
            p_create_query,
            create_table_query,
            local_context->getSettingsRef().max_query_size,
            local_context->getSettingsRef().max_parser_depth);
    }
    catch (...)
    {
        if (throw_on_error)
            throw;
        else
            LOG_DEBUG(
                log,
                "Fail to parseQuery for table {} in datase {} query id {}, create query {}",
                name,
                getDatabaseName(),
                local_context->getCurrentQueryId(),
                create_table_query);
    }

    return ast;
}

void DatabaseCnch::createEntryInCnchCatalog(ContextPtr local_context) const
{
    auto txn = local_context->getCurrentTransaction();
    if (!txn)
        throw Exception("Cnch transaction is not initialized", ErrorCodes::CNCH_TRANSACTION_NOT_INITIALIZED);

    CreateActionParams params = {getDatabaseName(), "", getUUID(), ""};
    auto create_db = txn->createAction<DDLCreateAction>(std::move(params));
    txn->appendAction(std::move(create_db));
    txn->commitV1();
}

StoragePtr DatabaseCnch::tryGetTableImpl(const String & name, ContextPtr local_context) const
{
    TransactionCnchPtr cnch_txn = local_context->getCurrentTransaction();
    const TxnTimestamp & start_time = cnch_txn ? cnch_txn->getStartTime() : TxnTimestamp{local_context->getTimestamp()};

    StoragePtr storage_ptr = getContext()->getCnchCatalog()->tryGetTable(*local_context, getDatabaseName(), name, start_time);
    if (!storage_ptr)
        storage_ptr = getContext()->getCnchCatalog()->tryGetDictionary(database_name, name, local_context);

    return storage_ptr;
}

void DatabaseCnch::renameDatabase(ContextPtr local_context, const String & new_name)
{
    auto txn = local_context->getCurrentTransaction();
    RenameActionParams params;
    params.db_params = RenameActionParams::RenameDBParams{database_name, new_name, {}};
    params.type = RenameActionParams::Type::RENAME_DB;
    auto rename = txn->createAction<DDLRenameAction>(std::move(params));
    txn->appendAction(std::move(rename));
    txn->commitV1();
}

void DatabaseCnch::renameTable(
    ContextPtr local_context, const String & table_name, IDatabase & to_database, const String & to_table_name, bool /*exchange*/, bool /*dictionary*/)
{
    auto txn = local_context->getCurrentTransaction();
    if (to_database.isTableExist(to_table_name, local_context))
        throw Exception("Table " + to_database.getDatabaseName() + "." + to_table_name + " already exists.", ErrorCodes::TABLE_ALREADY_EXISTS);
    StoragePtr from_table = tryGetTableImpl(table_name, local_context);
    if (!from_table)
        throw Exception("Table " + database_name + "." + table_name + " doesn't exist.", ErrorCodes::UNKNOWN_TABLE);
    std::vector<IntentLockPtr> locks;

    if (std::make_pair(database_name, table_name)
        < std::make_pair(to_database.getDatabaseName(), to_table_name))
    {
        locks.push_back(txn->createIntentLock(IntentLock::TB_LOCK_PREFIX, database_name, from_table->getStorageID().table_name));
        locks.push_back(txn->createIntentLock(IntentLock::TB_LOCK_PREFIX, to_database.getDatabaseName(), to_table_name));
    }
    else
    {
        locks.push_back(txn->createIntentLock(IntentLock::TB_LOCK_PREFIX, to_database.getDatabaseName(), to_table_name));
        locks.push_back(txn->createIntentLock(IntentLock::TB_LOCK_PREFIX, database_name, from_table->getStorageID().table_name));
    }

    std::lock(*locks[0], *locks[1]);

    RenameActionParams params;
    params.table_params = RenameActionParams::RenameTableParams{database_name, table_name, from_table->getStorageUUID(), to_database.getDatabaseName(), to_table_name};
    auto rename_table = txn->createAction<DDLRenameAction>(std::move(params));
    txn->appendAction(std::move(rename_table));
    /// Commit in InterpreterRenameQuery because we can rename multiple tables in a same transaction
    std::lock_guard wr{cache_mutex};
    if (cache.contains(table_name))
        cache.erase(table_name);
}

}
