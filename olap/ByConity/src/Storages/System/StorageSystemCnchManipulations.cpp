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

#include <Storages/System/StorageSystemCnchManipulations.h>

#include <Access/ContextAccess.h>
#include <CloudServices/CnchWorkerClient.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeUUID.h>
#include <DataTypes/DataTypesNumber.h>
#include <Interpreters/Context.h>
#include <Interpreters/WorkerGroupHandle.h>
#include <Storages/System/StorageSystemManipulations.h>
#include <WorkerTasks/ManipulationList.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
}

NamesAndTypesList StorageSystemCnchManipulations::getNamesAndTypes()
{
    return StorageSystemManipulations::getNamesAndTypes();
}

void StorageSystemCnchManipulations::fillData(MutableColumns & res_columns, ContextPtr context, const SelectQueryInfo &) const
{
    if (context->getServerType() != ServerType::cnch_server)
        throw Exception(ErrorCodes::NOT_IMPLEMENTED, "Table system.cnch_manipulations only supported in cnch-server");

    std::vector<ManipulationInfo> manipulations;
    auto worker_group = context->getCurrentWorkerGroup();

    for (const auto & worker: worker_group->getWorkerClients())
    {
        try
        {
            auto status = worker->getManipulationTasksStatus();
            manipulations.insert(manipulations.end(), status.begin(), status.end());
        }
        catch (...)
        {
            tryLogCurrentException(__PRETTY_FUNCTION__);
        }
    }

    const auto access = context->getAccess();
    bool check_access_for_tables = !access->isGranted(AccessType::SHOW_TABLES);

    for (const auto & elem : manipulations)
    {
        if (check_access_for_tables &&
            !access->isGranted(AccessType::SHOW_TABLES, elem.storage_id.database_name, elem.storage_id.table_name))
            continue;

        size_t i = 0;
        res_columns[i++]->insert(typeToString(elem.type));
        res_columns[i++]->insert(elem.task_id);
        res_columns[i++]->insert(elem.related_node);
        res_columns[i++]->insert(elem.storage_id.database_name);
        res_columns[i++]->insert(elem.storage_id.table_name);
        res_columns[i++]->insert(elem.storage_id.uuid);
        res_columns[i++]->insert(elem.elapsed);
        res_columns[i++]->insert(elem.progress);
        res_columns[i++]->insert(elem.num_parts);
        res_columns[i++]->insert(elem.source_part_names);
        res_columns[i++]->insert(elem.result_part_names);
        res_columns[i++]->insert(elem.partition_id);
        res_columns[i++]->insert(elem.total_size_bytes_compressed);
        res_columns[i++]->insert(elem.total_size_marks);
        res_columns[i++]->insert(elem.total_rows_count);
        res_columns[i++]->insert(elem.bytes_read_uncompressed);
        res_columns[i++]->insert(elem.bytes_written_uncompressed);
        res_columns[i++]->insert(elem.rows_read);
        res_columns[i++]->insert(elem.rows_written);
        res_columns[i++]->insert(elem.columns_written);
        res_columns[i++]->insert(elem.memory_usage);
        res_columns[i++]->insert(elem.thread_id);
    }
}

}
