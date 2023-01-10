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

#include <Interpreters/InterpreterUndropQuery.h>
#include <Parsers/ASTUndropQuery.h>
#include <Catalog/Catalog.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
    extern const int SUPPORT_IS_DISABLED;
    extern const int UNKNOWN_TABLE;
    extern const int SYNTAX_ERROR;
}


InterpreterUndropQuery::InterpreterUndropQuery(const ASTPtr & query_ptr_, ContextPtr context_)
    :WithContext(context_), query_ptr(query_ptr_)
{
}

BlockIO InterpreterUndropQuery::execute()
{
    if (getContext()->getServerType() != ServerType::cnch_server)
        throw Exception("UNDROP query is only supported in CNCH server side", ErrorCodes::NOT_IMPLEMENTED);

    ASTUndropQuery & undrop_query = query_ptr->as<ASTUndropQuery &>();
    if (!undrop_query.table.empty())
    {
        String database = undrop_query.database;
        String table = undrop_query.table;

        if (database.empty())
            database = getContext()->getCurrentDatabase();

        if (getContext()->getCnchCatalog()->isTableExists(database, table, UINT64_MAX))
            throw Exception("Table with same name already exists, please rename current table before restore.", ErrorCodes::SUPPORT_IS_DISABLED);

        auto trash_table_versions = getContext()->getCnchCatalog()->getTrashTableVersions(database, table);
        UInt64 version_to_restore {0};

        if (trash_table_versions.size() == 0)
            throw Exception("No available table to restore.", ErrorCodes::UNKNOWN_TABLE);
        else
        {
            if (undrop_query.uuid == UUIDHelpers::Nil)
            {
                // if uuid is not specified, undrop the latest version the table.
                for (auto it = trash_table_versions.begin(); it != trash_table_versions.end(); it++)
                {
                    version_to_restore = version_to_restore >= it->second ? version_to_restore : it->second;
                }
            }
            else
            {
                // undrop the trashed table with specified uuid.
                String undrop_uuid = toString(undrop_query.uuid);
                for (auto it = trash_table_versions.begin(); it != trash_table_versions.end(); it++)
                {
                    if (it->first == undrop_uuid)
                    {
                        version_to_restore = it->second;
                        break;
                    }
                }

                if (!version_to_restore)
                    throw Exception("No matching table in trash to perform UNDROP operation.", ErrorCodes::UNKNOWN_TABLE);
            }
        }

        getContext()->getCnchCatalog()->undropTable(database, table, version_to_restore);
    }
    else if (!undrop_query.database.empty())
    {
        String database = undrop_query.database;

        if (getContext()->getCnchCatalog()->isDatabaseExists(database, UINT64_MAX))
            throw Exception("Database with same name already exists, please rename or remove current database before restore.", ErrorCodes::SUPPORT_IS_DISABLED);

        auto trash_db_versions = getContext()->getCnchCatalog()->getTrashDBVersions(database);

        UInt64 version_to_restore {0};

        for (auto & version : trash_db_versions)
        {
            version_to_restore = version_to_restore >= version ? version_to_restore : version;
        }

        getContext()->getCnchCatalog()->undropDatabase(database,version_to_restore);
    }
    else
    {
        throw Exception("Wrong UNDROP query.", ErrorCodes::SYNTAX_ERROR);
    }

    return {};
}

}
