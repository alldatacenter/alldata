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

#include <Storages/System/StorageSystemDMBGJobs.h>
#include <Core/NamesAndTypes.h>
#include <Columns/IColumn.h>
#include <Interpreters/Context.h>
#include <Storages/SelectQueryInfo.h>
#include <DaemonManager/DaemonManagerClient.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeUUID.h>
#include <CloudServices/CnchBGThreadCommon.h>


namespace DB
{
    NamesAndTypesList StorageSystemDMBGJobs::getNamesAndTypes()
    {
        return
        {
            {"type", std::make_shared<DataTypeString>()},
            {"database", std::make_shared<DataTypeString>()},
            {"table", std::make_shared<DataTypeString>()},
            {"uuid", std::make_shared<DataTypeUUID>()},
            {"host_port", std::make_shared<DataTypeString>()},
            {"status", std::make_shared<DataTypeString>()},
            {"expected_status", std::make_shared<DataTypeString>()},
            {"last_start_time", std::make_shared<DataTypeDateTime>()}
        };
    }

    void StorageSystemDMBGJobs::fillData(MutableColumns & res_columns, ContextPtr context, const SelectQueryInfo &) const
    {
        if (context->getServerType() != ServerType::cnch_server)
            return;
        /// TODO: can optimize further to reduce number of request by checking where condition
        auto daemon_manager_client = context->getDaemonManagerClient();
        if (!daemon_manager_client)
            throw Exception("daemon manager client is nullptr", ErrorCodes::LOGICAL_ERROR);

        const std::vector<CnchBGThreadType> types {
            CnchBGThreadType::PartGC,
            CnchBGThreadType::MergeMutate,
            CnchBGThreadType::Consumer,
            CnchBGThreadType::Clustering,
            CnchBGThreadType::DedupWorker
        };

        std::for_each(types.begin(), types.end(),
            [& daemon_manager_client, & res_columns] (CnchBGThreadType type)
            {
                DaemonManager::BGJobInfos bg_job_datas = daemon_manager_client->getAllBGThreadServers(type);
                std::for_each(bg_job_datas.begin(), bg_job_datas.end(),
                    [type, & res_columns] (const DaemonManager::BGJobInfo & bg_job_data)
                    {
                        size_t column_num = 0;
                        res_columns[column_num++]->insert(toString(type));
                        res_columns[column_num++]->insert(bg_job_data.storage_id.database_name);
                        res_columns[column_num++]->insert(bg_job_data.storage_id.table_name);
                        res_columns[column_num++]->insert(bg_job_data.storage_id.uuid);
                        res_columns[column_num++]->insert(bg_job_data.host_port);
                        res_columns[column_num++]->insert(toString(bg_job_data.status));
                        res_columns[column_num++]->insert(toString(bg_job_data.expected_status));
                        res_columns[column_num++]->insert(bg_job_data.last_start_time);
                    }
                );
            }
        );
    }
} // end namespace
