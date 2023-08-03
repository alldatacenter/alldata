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

#include <Storages/System/StorageSystemBrokenTables.h>
#include <Interpreters/Context.h>
#include <Access/ContextAccess.h>
#include <DataTypes/DataTypeString.h>

namespace DB
{


NamesAndTypesList StorageSystemBrokenTables::getNamesAndTypes()
{
    return {
        {"database", std::make_shared<DataTypeString>()},
        {"name", std::make_shared<DataTypeString>()},
        {"error", std::make_shared<DataTypeString>()},
    };
}

void StorageSystemBrokenTables::fillData(MutableColumns & res_columns, ContextPtr context, const SelectQueryInfo &) const
{
    const auto access = context->getAccess();
    const bool check_access_for_databases = !access->isGranted(AccessType::SHOW_DATABASES);

    const auto databases = DatabaseCatalog::instance().getDatabases();

    for (const auto & [database_name, database] : databases)
    {
        if (check_access_for_databases && !access->isGranted(AccessType::SHOW_DATABASES, database_name))
            continue;

        auto tables = database->getBrokenTables();
        for (const auto & [broken_table, error_msg] : tables)
        {
            res_columns[0]->insert(database_name);
            res_columns[1]->insert(broken_table);
            res_columns[2]->insert(error_msg);
        }
    }
}

}
