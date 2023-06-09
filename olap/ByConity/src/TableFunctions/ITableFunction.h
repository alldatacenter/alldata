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

#include <Parsers/IAST_fwd.h>
#include <Storages/IStorage_fwd.h>
#include <Storages/ColumnsDescription.h>

#include <memory>
#include <string>


namespace DB
{

class Context;

/** Interface for table functions.
  *
  * Table functions are not relevant to other functions.
  * The table function can be specified in the FROM section instead of the [db.]Table
  * The table function returns a temporary StoragePtr object that is used to execute the query.
  *
  * Example:
  * SELECT count() FROM remote('example01-01-1', merge, hits)
  * - go to `example01-01-1`, in `merge` database, `hits` table.
  *
  *
  * When creating table AS table_function(...) we probably don't know structure of the table
  * and have to request if from remote server, because structure is required to create a Storage.
  * To avoid failures on server startup, we write obtained structure to metadata file.
  * So, table function may have two different columns lists:
  *  - cached_columns written to metadata
  *  - the list returned from getActualTableStructure(...)
  * See StorageTableFunctionProxy.
  */

class ITableFunction : public std::enable_shared_from_this<ITableFunction>
{
public:
    static inline std::string getDatabaseName() { return "_table_function"; }

    /// Get the main function name.
    virtual std::string getName() const = 0;

    /// Returns true if we always know table structure when executing table function
    /// (e.g. structure is specified in table function arguments)
    virtual bool hasStaticStructure() const { return false; }
    /// Returns false if storage returned by table function supports type conversion (e.g. StorageDistributed)
    virtual bool needStructureConversion() const { return true; }

    virtual void parseArguments(const ASTPtr & /*ast_function*/, ContextPtr /*context*/) {}

    /// Returns actual table structure probably requested from remote server, may fail
    virtual ColumnsDescription getActualTableStructure(ContextPtr /*context*/) const = 0;

    /// Create storage according to the query.
    StoragePtr
    execute(const ASTPtr & ast_function, ContextPtr context, const std::string & table_name, ColumnsDescription cached_columns_ = {}) const;

    virtual ~ITableFunction() = default;

private:
    virtual StoragePtr executeImpl(
        const ASTPtr & ast_function, ContextPtr context, const std::string & table_name, ColumnsDescription cached_columns) const = 0;
    virtual const char * getStorageTypeName() const = 0;
};

using TableFunctionPtr = std::shared_ptr<ITableFunction>;

std::vector<String> parseDescription(const String & description, size_t l, size_t r, char separator, size_t max_addresses);

}
