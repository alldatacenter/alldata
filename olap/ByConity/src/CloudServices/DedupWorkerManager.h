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

#pragma once

#include <CloudServices/ICnchBGThread.h>
#include <CloudServices/CnchWorkerClientPools.h>
#include <CloudServices/DedupWorkerStatus.h>
#include <Interpreters/VirtualWarehouseHandle.h>
#include <Interpreters/VirtualWarehousePool.h>
#include <Storages/IStorage_fwd.h>

namespace DB
{

class StorageCnchMergeTree;

/**
 * Manage dedup worker of one unique table that enables staging area for writing.
 */
class DedupWorkerManager: public ICnchBGThread
{
public:

    DedupWorkerManager(ContextPtr context, const StorageID & storage_id);

    ~DedupWorkerManager() override;

    void runImpl() override;

    void stop() override;

    /**
     * Check whether target dedup worker instance is valid.
     */
    DedupWorkerHeartbeatResult reportHeartbeat(const String & worker_table_name);

    DedupWorkerStatus getDedupWorkerStatus();

private:

    void iterate(StoragePtr & istorage);

    void assignDeduperToWorker(StoragePtr & cnch_table);

    /// caller must hold lock of worker_client_mutex.
    void selectDedupWorker(StorageCnchMergeTree & cnch_table);

    void stopDeduperWorker();

    bool checkDedupWorkerStatus(StoragePtr & storage);

    String getDedupWorkerDebugInfo();

    mutable std::mutex worker_client_mutex;
    CnchWorkerClientPtr worker_client;
    StorageID worker_storage_id;

};

}
