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

#include <Storages/System/StorageSystemBGThreads.h>

#include <CloudServices/CnchBGThreadsMap.h>
#include <CloudServices/ICnchBGThread.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeUUID.h>
#include <DataTypes/DataTypesNumber.h>
#include <Interpreters/Context.h>

namespace DB
{
NamesAndTypesList StorageSystemBGThreads::getNamesAndTypes()
{
    return {
        {"type", std::make_shared<DataTypeString>()},
        {"database", std::make_shared<DataTypeString>()},
        {"table", std::make_shared<DataTypeString>()},
        {"uuid", std::make_shared<DataTypeUUID>()},
        {"status", std::make_shared<DataTypeString>()},
        {"startup_time", std::make_shared<DataTypeDateTime>()},
        {"last_wakeup_interval", std::make_shared<DataTypeInt64>()},
        {"last_wakeup_time", std::make_shared<DataTypeDateTime>()},
        {"num_wakeup", std::make_shared<DataTypeUInt64>()},
    };
}

void StorageSystemBGThreads::fillData(MutableColumns & res_columns, ContextPtr context, const SelectQueryInfo &) const
{
    for (auto i = CnchBGThreadType::ServerMinType; i <= CnchBGThreadType::ServerMaxType; i = CnchBGThreadType(size_t(i) + 1))
    {
        for (auto && [_, t] : context->getCnchBGThreadsMap(i)->getAll())
        {
            size_t c = 0;
            res_columns[c++]->insert(toString(t->getType()));
            auto storage_id = t->getStorageID();
            res_columns[c++]->insert(storage_id.database_name);
            res_columns[c++]->insert(storage_id.table_name);
            res_columns[c++]->insert(storage_id.uuid);
            res_columns[c++]->insert(toString(t->getThreadStatus()));
            res_columns[c++]->insert(t->getStartupTime());
            res_columns[c++]->insert(t->getLastWakeupInterval());
            res_columns[c++]->insert(t->getLastWakeupTime());
            res_columns[c++]->insert(t->getNumWakeup());
        }
    }
}

}
