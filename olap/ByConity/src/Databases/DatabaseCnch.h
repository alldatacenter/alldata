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

#include <mutex>
#include <Common/Exception.h>
#include <Common/escapeForFileName.h>
#include <Common/quoteString.h>
#include <Databases/DatabasesCommon.h>
#include <Parsers/ASTCreateQuery.h>
#include <Storages/IStorage.h>
#include <Transaction/TxnTimestamp.h>
#include <Common/ErrorCodes.h>
#include "Storages/IStorage_fwd.h"
#include <Interpreters/Context_fwd.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int SUPPORT_IS_DISABLED;
}

/* Class to provide operations for CNCH tables where metadata is stored in external kv storage
   And it doesn't manage its own tables
   Main differences from DatabaseOnDisk:
    - Attach tables can only be done permanently, i.e. only support `ATTACH <tbl> PERMARNENTLY`
    - DO NOT support ATTACH and DETACH database
   It doesn't make sense to detach / attach a database when metadata is stored in a centralized
   kv storage.
 */


class DatabaseCnch : public IDatabase, protected WithContext
{
public:
    DatabaseCnch(const String & name, UUID uuid, ContextPtr context);

    String getEngineName() const override { return "Cnch"; }
    UUID getUUID() const override { return db_uuid; }
    bool canContainMergeTreeTables() const override { return false; }
    void createTable(ContextPtr local_context, const String & table_name, const StoragePtr & table, const ASTPtr & query) override;
    void dropTable(ContextPtr local_context, const String & table_name, bool no_delay) override;
    [[noreturn]] StoragePtr detachTable(const String & name) override
    {
        throw Exception(ErrorCodes::SUPPORT_IS_DISABLED, fmt::format("Cnch database doesn't support DETACH, use DETACH {} PERMANENTLY", name));
    }
    void detachTablePermanently(ContextPtr local_context, const String & name) override;
    /// No need to be empty when drop cnch database. Catalog is responsible for deleting tables under current db.
    bool shouldBeEmptyOnDetach() const override { return false; }
    void renameDatabase(ContextPtr local_cotnext, const String & new_name) override;
    void renameTable(
        ContextPtr context,
        const String & table_name,
        IDatabase & to_database,
        const String & to_table_name,
        bool exchange,
        bool dictionary) override;
    ASTPtr getCreateDatabaseQuery() const override;
    void drop(ContextPtr context) override;
    bool isTableExist(const String & name, ContextPtr local_context) const override;
    StoragePtr tryGetTable(const String & name, ContextPtr local_context) const override;
    DatabaseTablesIteratorPtr getTablesIterator(ContextPtr local_context, const FilterByNameFunction & filter_by_table_name) override;
    bool empty() const override;
    void shutdown() override {}
    void createEntryInCnchCatalog(ContextPtr local_context) const;

    TxnTimestamp commit_time;
protected:
    ASTPtr getCreateTableQueryImpl(const String & name, ContextPtr local_context, bool throw_on_error) const override;
    StoragePtr tryGetTableImpl(const String & name, ContextPtr local_context) const;

private:
    const UUID db_uuid;
    /// local storage cache, mapping from name->storage, mainly for select query
    /// Work under an assumptions that database was re-created for each query
    mutable std::unordered_map<String, StoragePtr> cache;
    mutable std::shared_mutex cache_mutex;
    Poco::Logger * log;
};

using CnchDBPtr = std::shared_ptr<DatabaseCnch>;
using CnchDatabases = std::map<String, CnchDBPtr>;

}
