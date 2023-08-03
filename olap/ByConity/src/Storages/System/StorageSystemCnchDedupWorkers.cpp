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

#include <Storages/System/StorageSystemCnchDedupWorkers.h>

#include <Catalog/Catalog.h>
#include <CloudServices/CloudMergeTreeDedupWorker.h>
#include <CloudServices/DedupWorkerManager.h>
#include <Columns/ColumnString.h>
#include <Columns/ColumnsNumber.h>
#include <Core/UUID.h>
#include <DataStreams/OneBlockInputStream.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeUUID.h>
#include <DataTypes/DataTypesNumber.h>
#include <Interpreters/Context.h>
#include <Storages/StorageCloudMergeTree.h>
#include <Storages/StorageCnchMergeTree.h>
#include <Storages/VirtualColumnUtils.h>

namespace DB
{
NamesAndTypesList StorageSystemCnchDedupWorkers::getNamesAndTypes()
{
    return {
        {"database", std::make_shared<DataTypeString>()},
        {"table", std::make_shared<DataTypeString>()},
        {"uuid", std::make_shared<DataTypeUUID>()},
        {"is_active", std::make_shared<DataTypeUInt64>()},

        {"create_time", std::make_shared<DataTypeDateTime>()},
        {"total_schedule_cnt", std::make_shared<DataTypeUInt64>()},
        {"total_dedup_cnt", std::make_shared<DataTypeUInt64>()},
        {"last_schedule_wait_ms", std::make_shared<DataTypeUInt64>()},
        {"last_task_total_cost_ms", std::make_shared<DataTypeUInt64>()},
        {"last_task_dedup_cost_ms", std::make_shared<DataTypeUInt64>()},
        {"last_task_publish_cost_ms", std::make_shared<DataTypeUInt64>()},
        {"last_task_staged_part_cnt", std::make_shared<DataTypeUInt64>()},
        {"last_task_visible_part_cnt", std::make_shared<DataTypeUInt64>()},
        {"last_task_staged_part_total_rows", std::make_shared<DataTypeUInt64>()},
        {"last_task_visible_part_total_rows", std::make_shared<DataTypeUInt64>()},

        {"server_rpc_address", std::make_shared<DataTypeString>()},
        {"server_tcp_address", std::make_shared<DataTypeString>()},
        {"worker_rpc_address", std::make_shared<DataTypeString>()},
        {"worker_tcp_address", std::make_shared<DataTypeString>()},
        {"last_exception_time", std::make_shared<DataTypeDateTime>()},
        {"last_exception", std::make_shared<DataTypeString>()},
    };
}

ColumnsDescription StorageSystemCnchDedupWorkers::getColumnsAndAlias()
{
    return ColumnsDescription(getNamesAndTypes());
}

void StorageSystemCnchDedupWorkers::fillDataOnServer(
    MutableColumns & res_columns,
    const ColumnPtr & needed_uuids,
    const std::map<UUID, StoragePtr> & tables,
    ContextPtr context,
    const UUIDToBGThreads & uuid_to_threads) const
{
    for (size_t i = 0, size = needed_uuids->size(); i < size; ++i)
    {
        auto uuid = (*needed_uuids)[i].safeGet<const UInt128 &>();
        auto thread = uuid_to_threads.at(UUID(uuid));
        DedupWorkerStatus status;
        if (auto dedup_worker_manager = dynamic_cast<DedupWorkerManager *>(thread.get()))
            status = dedup_worker_manager->getDedupWorkerStatus();

        auto storage = tables.at(UUID(uuid));
        auto storage_id = storage->getStorageID();

        size_t col_num = 0;
        res_columns[col_num++]->insert(storage_id.database_name);
        res_columns[col_num++]->insert(storage_id.table_name);
        res_columns[col_num++]->insert(storage_id.uuid);
        res_columns[col_num++]->insert(status.is_active);

        res_columns[col_num++]->insert(status.create_time);
        res_columns[col_num++]->insert(status.total_schedule_cnt);
        res_columns[col_num++]->insert(status.total_dedup_cnt);
        res_columns[col_num++]->insert(status.last_schedule_wait_ms);
        res_columns[col_num++]->insert(status.last_task_total_cost_ms);
        res_columns[col_num++]->insert(status.last_task_dedup_cost_ms);
        res_columns[col_num++]->insert(status.last_task_publish_cost_ms);
        res_columns[col_num++]->insert(status.last_task_staged_part_cnt);
        res_columns[col_num++]->insert(status.last_task_visible_part_cnt);
        res_columns[col_num++]->insert(status.last_task_staged_part_total_rows);
        res_columns[col_num++]->insert(status.last_task_visible_part_total_rows);

        auto server_host_ports = context->getHostWithPorts();
        res_columns[col_num++]->insert(server_host_ports.getRPCAddress());
        res_columns[col_num++]->insert(server_host_ports.getTCPAddress());
        res_columns[col_num++]->insert(status.worker_rpc_address);
        res_columns[col_num++]->insert(status.worker_tcp_address);
        res_columns[col_num++]->insert(status.last_exception_time);
        res_columns[col_num++]->insert(status.last_exception);
    }
}

void StorageSystemCnchDedupWorkers::fillDataOnWorker(
    MutableColumns & res_columns, const ColumnPtr & needed_uuids, const std::map<UUID, StoragePtr> & tables, ContextPtr context) const
{
    for (size_t i = 0, size = needed_uuids->size(); i < size; ++i)
    {
        auto uuid = (*needed_uuids)[i].safeGet<const UInt128 &>();
        auto storage = tables.at(UUID(uuid));
        auto table = dynamic_cast<StorageCloudMergeTree *>(storage.get());
        auto worker = table->tryGetDedupWorker();
        if (!worker)
            continue;

        auto storage_id = storage->getStorageID();

        size_t col_num = 0;
        res_columns[col_num++]->insert(storage_id.database_name);
        res_columns[col_num++]->insert(storage_id.table_name);
        res_columns[col_num++]->insert(storage_id.uuid);
        res_columns[col_num++]->insert(worker->isActive());

        auto status = worker->getDedupWorkerStatus();
        res_columns[col_num++]->insert(status.create_time);
        res_columns[col_num++]->insert(status.total_schedule_cnt);
        res_columns[col_num++]->insert(status.total_dedup_cnt);
        res_columns[col_num++]->insert(status.last_schedule_wait_ms);
        res_columns[col_num++]->insert(status.last_task_total_cost_ms);
        res_columns[col_num++]->insert(status.last_task_dedup_cost_ms);
        res_columns[col_num++]->insert(status.last_task_publish_cost_ms);
        res_columns[col_num++]->insert(status.last_task_staged_part_cnt);
        res_columns[col_num++]->insert(status.last_task_visible_part_cnt);
        res_columns[col_num++]->insert(status.last_task_staged_part_total_rows);
        res_columns[col_num++]->insert(status.last_task_visible_part_total_rows);

        auto server_host_ports = worker->getServerHostWithPorts();
        res_columns[col_num++]->insert(server_host_ports.getRPCAddress());
        res_columns[col_num++]->insert(server_host_ports.getTCPAddress());

        auto worker_host_ports = context->getHostWithPorts();
        res_columns[col_num++]->insert(worker_host_ports.getRPCAddress());
        res_columns[col_num++]->insert(worker_host_ports.getTCPAddress());

        res_columns[col_num++]->insert(status.last_exception);
        res_columns[col_num++]->insert(status.last_exception_time);
    }
}

void StorageSystemCnchDedupWorkers::fillData(MutableColumns & res_columns, ContextPtr context, const SelectQueryInfo & query_info) const
{
    std::map<UUID, StoragePtr> tables;
    UUIDToBGThreads uuid_to_threads;
    if (context->getServerType() == ServerType::cnch_server)
    {
        uuid_to_threads = context->getCnchBGThreadsMap(CnchBGThread::DedupWorker)->getAll();
        for (const auto & thread : uuid_to_threads)
        {
            auto storage
                = context->getCnchCatalog()->tryGetTableByUUID(*context, UUIDHelpers::UUIDToString(thread.first), TxnTimestamp::maxTS());
            if (!storage)
                continue;
            auto table = dynamic_cast<const StorageCnchMergeTree *>(storage.get());
            if (table && table->getInMemoryMetadataPtr()->hasUniqueKey())
                tables[table->getStorageUUID()] = storage;
        }
    }
    else if (context->getServerType() == ServerType::cnch_worker)
    {
        for (const auto & db : DatabaseCatalog::instance().getDatabases())
        {
            for (auto it = db.second->getTablesIterator(context); it->isValid(); it->next())
            {
                auto table = dynamic_cast<const StorageCloudMergeTree *>(it->table().get());
                if (table && table->getInMemoryMetadataPtr()->hasUniqueKey())
                    tables[table->getStorageID().uuid] = it->table();
            }
        }
    }
    else
        throw Exception("Cnch dedup workers info can only be read on server or worker", ErrorCodes::LOGICAL_ERROR);

    MutableColumnPtr col_database_mut = ColumnString::create();
    MutableColumnPtr col_table_mut = ColumnString::create();
    MutableColumnPtr col_uuid_mut = ColumnUInt128::create();
    for (auto & [uuid, storage] : tables)
    {
        auto storage_id = storage->getStorageID();
        col_database_mut->insert(storage_id.database_name);
        col_table_mut->insert(storage_id.table_name);
        col_uuid_mut->insert(storage_id.uuid);
    }


    ColumnPtr col_database = std::move(col_database_mut);
    ColumnPtr col_table = std::move(col_table_mut);
    ColumnPtr col_uuid = std::move(col_uuid_mut);

    /// Determine what tables are needed by the conditions in the query.
    {
        Block filtered_block{
            {col_database, std::make_shared<DataTypeString>(), "database"},
            {col_table, std::make_shared<DataTypeString>(), "table"},
            {col_uuid, std::make_shared<DataTypeUUID>(), "uuid"},
        };

        VirtualColumnUtils::filterBlockWithQuery(query_info.query, filtered_block, context);

        if (!filtered_block.rows())
            return;

        col_database = filtered_block.getByName("database").column;
        col_table = filtered_block.getByName("table").column;
        col_uuid = filtered_block.getByName("uuid").column;
    }

    if (context->getServerType() == ServerType::cnch_worker)
        fillDataOnWorker(res_columns, col_uuid, tables, context);
    else
        fillDataOnServer(res_columns, col_uuid, tables, context, uuid_to_threads);
}

}
