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

#include <Catalog/Catalog.h>
#include <Interpreters/Context.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeNullable.h>
#include <Storages/System/StorageSystemCnchDatabasesHistory.h>
#include <Common/Status.h>

namespace DB
{
NamesAndTypesList StorageSystemCnchDatabasesHistory::getNamesAndTypes()
{
    return {
        {"name", std::make_shared<DataTypeString>()},
        {"uuid", std::make_shared<DataTypeNullable>(std::make_shared<DataTypeUUID>())},
        {"delete_time", std::make_shared<DataTypeDateTime>()},
    };
}

void StorageSystemCnchDatabasesHistory::fillData(MutableColumns & res_columns, ContextPtr context, const SelectQueryInfo & ) const
{
    Catalog::CatalogPtr cnch_catalog = context->getCnchCatalog();

    if (context->getServerType() == ServerType::cnch_server && cnch_catalog)
    {
        //get CNCH databases
        Catalog::Catalog::DataModelDBs res = cnch_catalog->getDatabaseInTrash();

        for (size_t i = 0, size = res.size(); i != size; ++i)
        {
            size_t col_num = 0;
            res_columns[col_num++]->insert(res[i].name());
            /// fill database uuid if it exists, otherwise the field will be NULL
            if (res[i].has_uuid())
                res_columns[col_num++]->insert(RPCHelpers::createUUID(res[i].uuid()));
            else
                res_columns[col_num++]->insertDefault();
            auto commit_time = (res[i].commit_time() >> 18); // first 48 bits represent times
            res_columns[col_num++]->insert(commit_time / 1000); // convert to seconds
        }
    }
}

}
