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

#include <common/types.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/IAST.h>
#include <Storages/IStorage_fwd.h>
#include <Databases/IDatabase.h>
#include <mutex>


/// General functionality for several different database engines.

namespace DB
{

class Context;

String getTableDefinitionFromCreateQuery(const ASTPtr & query, bool attach);

/// A base class for databases that manage their own list of tables.
class DatabaseWithOwnTablesBase : public IDatabase, protected WithContext
{
public:
    bool isTableExist(const String & table_name, ContextPtr context) const override;

    StoragePtr tryGetTable(const String & table_name, ContextPtr context) const override;

    bool empty() const override;

    void attachTable(const String & table_name, const StoragePtr & table, const String & relative_table_path) override;

    StoragePtr detachTable(const String & table_name) override;

    DatabaseTablesIteratorPtr getTablesIterator(ContextPtr context, const FilterByNameFunction & filter_by_table_name) override;

    void shutdown() override;

    void clearBrokenTables() override;

    std::map<String, String> getBrokenTables() override;

    ~DatabaseWithOwnTablesBase() override;

protected:
    Tables tables;
    Poco::Logger * log;

    /// Information to log broken parts which fails to be loaded
    std::map<String, String> brokenTables;

    DatabaseWithOwnTablesBase(const String & name_, const String & logger, ContextPtr context);

    void attachTableUnlocked(const String & table_name, const StoragePtr & table, std::unique_lock<std::mutex> & lock);
    StoragePtr detachTableUnlocked(const String & table_name, std::unique_lock<std::mutex> & lock);
    StoragePtr getTableUnlocked(const String & table_name, std::unique_lock<std::mutex> & lock) const;
};

std::vector<StoragePtr> getViews(const StorageID & storage_id, const ContextPtr & context);

}
