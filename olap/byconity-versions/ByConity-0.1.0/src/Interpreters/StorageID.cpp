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

#include <Interpreters/StorageID.h>
#include <Interpreters/Context.h>
#include <Parsers/ASTQueryWithTableAndOutput.h>
#include <Parsers/ASTIdentifier.h>
#include <Common/quoteString.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>
#include <Interpreters/DatabaseAndTableWithAlias.h>
#include <Poco/Util/AbstractConfiguration.h>
#include <Storages/IStorage.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int UNKNOWN_DATABASE;
}

StorageID::StorageID(const ASTQueryWithTableAndOutput & query)
{
    database_name = query.database;
    table_name = query.table;
    uuid = query.uuid;
    assertNotEmpty();
}

StorageID::StorageID(const ASTTableIdentifier & table_identifier_node)
{
    DatabaseAndTableWithAlias database_table(table_identifier_node);
    database_name = database_table.database;
    table_name = database_table.table;
    uuid = database_table.uuid;
    assertNotEmpty();
}

StorageID::StorageID(const ASTPtr & node)
{
    if (const auto * identifier = node->as<ASTTableIdentifier>())
        *this = StorageID(*identifier);
    else if (const auto * simple_query = dynamic_cast<const ASTQueryWithTableAndOutput *>(node.get()))
        *this = StorageID(*simple_query);
    else
        throw Exception("Unexpected AST", ErrorCodes::LOGICAL_ERROR);
}

String StorageID::getTableName() const
{
    assertNotEmpty();
    return table_name;
}

String StorageID::getDatabaseName() const
{
    assertNotEmpty();
    if (database_name.empty())
        throw Exception("Database name is empty", ErrorCodes::UNKNOWN_DATABASE);
    return database_name;
}

String StorageID::getNameForLogs() const
{
    assertNotEmpty();
    return (database_name.empty() ? "" : backQuoteIfNeed(database_name) + ".") + backQuoteIfNeed(table_name)
           + (hasUUID() ? " (" + toString(uuid) + ")" : "");
}

bool StorageID::operator<(const StorageID & rhs) const
{
    assertNotEmpty();
    /// It's needed for ViewDependencies
    if (!hasUUID() && !rhs.hasUUID())
        /// If both IDs don't have UUID, compare them like pair of strings
        return std::tie(database_name, table_name) < std::tie(rhs.database_name, rhs.table_name);
    else if (hasUUID() && rhs.hasUUID())
        /// If both IDs have UUID, compare UUIDs and ignore database and table name
        return uuid < rhs.uuid;
    else
        /// All IDs without UUID are less, then all IDs with UUID
        return !hasUUID();
}

bool StorageID::operator==(const StorageID & rhs) const
{
    assertNotEmpty();
    if (hasUUID() && rhs.hasUUID())
        return uuid == rhs.uuid;
    else
        return std::tie(database_name, table_name) == std::tie(rhs.database_name, rhs.table_name);
}

String StorageID::getFullTableName() const
{
    return backQuoteIfNeed(getDatabaseName()) + "." + backQuoteIfNeed(table_name);
}

String StorageID::getFullNameNotQuoted() const
{
    return getDatabaseName() + "." + table_name;
}

StorageID StorageID::fromDictionaryConfig(const Poco::Util::AbstractConfiguration & config,
                                          const String & config_prefix)
{
    StorageID res = StorageID::createEmpty();
    res.database_name = config.getString(config_prefix + ".database", "");
    res.table_name = config.getString(config_prefix + ".name");
    const String uuid_str = config.getString(config_prefix + ".uuid", "");
    if (!uuid_str.empty())
        res.uuid = parseFromString<UUID>(uuid_str);
    return res;
}

String StorageID::getInternalDictionaryName() const
{
    assertNotEmpty();
    if (hasUUID())
        return toString(uuid);
    if (database_name.empty())
        return table_name;
    return database_name + "." + table_name;
}

void StorageID::serialize(WriteBuffer & buffer) const
{
    writeBinary(database_name, buffer);
    writeBinary(table_name, buffer);
    writeBinary(uuid, buffer);
}

StorageID StorageID::deserialize(ReadBuffer & buffer, ContextPtr context)
{
    String database_name;
    readBinary(database_name, buffer);

    String table_name;
    readBinary(table_name, buffer);

    UUID uuid;
    readBinary(uuid, buffer);

    if (table_name.empty() && uuid == UUIDHelpers::Nil)
    {
        return StorageID("_dummy", "_dummy", uuid);
    }
    auto storage_id_recv = StorageID(database_name, table_name, uuid);
    StoragePtr storage = DatabaseCatalog::instance().getTable({storage_id_recv.database_name, storage_id_recv.table_name}, context);
    if (storage)
        storage_id_recv = storage->getStorageID();
    return storage_id_recv;
}

}
