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

#include <Storages/System/StorageSystemGlobalGCManager.h>
#include <Core/NamesAndTypes.h>
#include <Columns/IColumn.h>
#include <Interpreters/Context.h>
#include <Storages/SelectQueryInfo.h>
#include <DataTypes/DataTypeUUID.h>
#include <CloudServices/CnchServerClient.h>
#include <Common/HostWithPorts.h>


namespace DB
{
    NamesAndTypesList StorageSystemGlobalGCManager::getNamesAndTypes()
    {
        return
        {
            {"deleting_uuid", std::make_shared<DataTypeUUID>()}
        };
    }

    void StorageSystemGlobalGCManager::fillData(MutableColumns & res_columns, ContextPtr context, const SelectQueryInfo &) const
    {
        if (context->getServerType() != ServerType::cnch_server)
            return;
        UInt16 port = context->getRPCPort();
        CnchServerClient client{createHostPortString(getLoopbackIPFromEnv(), port)};
        std::set<UUID> deleting_uuid = client.getDeletingTablesInGlobalGC();
        std::for_each(deleting_uuid.begin(), deleting_uuid.end(),
            [& res_columns] (const UUID & uuid) {
                res_columns[0]->insert(uuid);
            });
    }
} // end namespace DB
