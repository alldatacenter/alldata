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

#include <CloudServices/CnchCreateQueryHelper.h>
#include <CloudServices/DedupWorkerManager.h>
#include <Databases/DatabasesCommon.h>
#include <Parsers/ASTCreateQuery.h>
#include <Storages/StorageCnchMergeTree.h>

namespace DB
{

DedupWorkerManager::DedupWorkerManager(ContextPtr context_, const StorageID & storage_id_)
    : ICnchBGThread(context_, CnchBGThreadType::DedupWorker, storage_id_), worker_storage_id(storage_id_)
{
}

DedupWorkerManager::~DedupWorkerManager()
{
    try
    {
        stop();
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }
}

void DedupWorkerManager::runImpl()
{
    try
    {
        auto storage = getStorageFromCatalog();
        iterate(storage);
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }

    scheduled_task->scheduleAfter(3 * 1000);
}

void DedupWorkerManager::stop()
{
    ICnchBGThread::stop();

    stopDeduperWorker();
}

void DedupWorkerManager::iterate(StoragePtr & storage)
{
    if (checkDedupWorkerStatus(storage))
        return;
    assignDeduperToWorker(storage);
}

void DedupWorkerManager::assignDeduperToWorker(StoragePtr & storage)
{
    std::lock_guard lock(worker_client_mutex);
    if (worker_client)
        return;
    try
    {
        worker_storage_id = storage->getStorageID();
        auto & cnch_table = checkAndGetCnchTable(storage);
        selectDedupWorker(cnch_table);

        /// create a unique table suffix
        String deduper_table_suffix = '_' + toString(std::chrono::system_clock::now().time_since_epoch().count());
        worker_storage_id.table_name += deduper_table_suffix;

        auto create_ast = getASTCreateQueryFromStorage(*storage, getContext());
        replaceCnchWithCloud(
            *create_ast, worker_storage_id.table_name, storage->getStorageID().getDatabaseName(), storage->getStorageID().getTableName());
        modifyOrAddSetting(*create_ast, "cloud_enable_dedup_worker", Field(UInt64(1)));
        String create_query = getTableDefinitionFromCreateQuery(static_pointer_cast<IAST>(create_ast), false);
        LOG_TRACE(log, "Create table query of dedup worker: {}", create_query);

        LOG_DEBUG(log, "Assigning a new dedup worker: {}", getDedupWorkerDebugInfo());
        worker_client->createDedupWorker(worker_storage_id, create_query, getContext()->getHostWithPorts());
        LOG_DEBUG(log, "Assigned a new dedup worker: {}", getDedupWorkerDebugInfo());
    }
    catch (...)
    {
        LOG_ERROR(log, "Fail to assign a new dedup worker due to {}", getCurrentExceptionMessage(false));
        worker_client = nullptr;
    }
}

void DedupWorkerManager::selectDedupWorker(StorageCnchMergeTree & cnch_table)
{
    auto vw_handle = getContext()->getVirtualWarehousePool().get(cnch_table.getSettings()->cnch_vw_write);
    worker_client = vw_handle->getWorker();
}

void DedupWorkerManager::stopDeduperWorker()
{
    std::lock_guard lock(worker_client_mutex);

    if (!worker_client)
        return;
    try
    {
        LOG_DEBUG(log, "Try to stop deduper: {}", getDedupWorkerDebugInfo());
        worker_client->dropDedupWorker(worker_storage_id);
        LOG_DEBUG(log, "Stop deduper sucessfully: {}", getDedupWorkerDebugInfo());
    }
    catch (...)
    {
        LOG_ERROR(log, "Fail to stop deduper due to {}", getCurrentExceptionMessage(false));
        // In this case, it's not necessary to repeat the stop action, just set worker_client to nullptr is enough
        // because dedup worker will check its validity via heartbeat and it'll stop itself soon.
    }
    worker_client = nullptr;
}

String DedupWorkerManager::getDedupWorkerDebugInfo()
{
    if (!worker_client)
        return "dedup worker is not assigned.";
    return "worker table is " + worker_storage_id.getNameForLogs() + ", host_with_port is "
        + worker_client->getHostWithPorts().toDebugString();
}

bool DedupWorkerManager::checkDedupWorkerStatus(StoragePtr & storage)
{
    std::lock_guard lock(worker_client_mutex);
    if (!worker_client)
        return false;
    try
    {
        if (storage_id.getFullTableName()
            != storage->getStorageID().getFullTableName()) /// check if storage id is changed, such as rename table
        {
            LOG_INFO(
                log,
                "cnch table storage id has changed, reassign dedup worker. Origin storage id: {}, current storage id: {}",
                storage_id.getNameForLogs(),
                storage->getStorageID().getNameForLogs());
            const_cast<StorageID &>(storage_id) = storage->getStorageID(); // correct storage id
            worker_client = nullptr;
        }
        else
        {
            DedupWorkerStatus status = worker_client->getDedupWorkerStatus(worker_storage_id);
            if (!status.is_active)
            {
                LOG_WARNING(log, "Deduper is inactive, try to assign a new one. Old deduper info: {}", getDedupWorkerDebugInfo());
                worker_client = nullptr;
            }
        }
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        /// just assign a new dedup worker as deduper will check validity.
        LOG_ERROR(log, "Failed to get deduper status of {}, try to assign a new one.", worker_storage_id.getNameForLogs());
        worker_client = nullptr;
    }
    return worker_client != nullptr;
}

DedupWorkerStatus DedupWorkerManager::getDedupWorkerStatus()
{
    std::lock_guard lock(worker_client_mutex);
    DedupWorkerStatus status;
    if (!worker_client)
        status.is_active = false;
    else
    {
        status = worker_client->getDedupWorkerStatus(worker_storage_id);
        auto worker_host_ports = worker_client->getHostWithPorts();
        status.worker_rpc_address = worker_host_ports.getRPCAddress();
        status.worker_tcp_address = worker_host_ports.getTCPAddress();
    }
    return status;
}

DedupWorkerHeartbeatResult DedupWorkerManager::reportHeartbeat(const String & worker_table_name)
{
    std::lock_guard lock(worker_client_mutex);
    LOG_TRACE(log, "Report heartbeat of deduper worker: worker table name is {}", worker_table_name);
    if (worker_client == nullptr || worker_table_name != worker_storage_id.table_name)
        return DedupWorkerHeartbeatResult::Kill;
    return DedupWorkerHeartbeatResult::Success;
}

}
