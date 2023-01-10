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
#include <DaemonManager/DaemonJob.h>
#include <Common/LRUCache.h>
#include <Core/UUID.h>
#include <DaemonManager/BackgroundJob.h>
#include <DaemonManager/DMDefines.h>
#include <DaemonManager/DaemonHelper.h>
#include <DaemonManager/BackgroudJobExecutor.h>
#include <DaemonManager/TargetServerCalculator.h>
#include <DaemonManager/BGJobStatusInCatalog.h>
#include <CloudServices/CnchBGThreadCommon.h>
#include <CloudServices/CnchServerClient.h>
#include <Protos/RPCHelpers.h>
#include <Common/Status.h>


namespace DB::DaemonManager
{

using CnchServerClientPtrs = std::vector<CnchServerClientPtr>;
using StorageCache = LRUCache<String, IStorage>;
using BGJobStatusInCatalog::IBGJobStatusPersistentStoreProxy;

struct BGJobInfoFromServer
{
    StorageID storage_id;
    CnchBGThreadStatus status;
    String host_port;
};

using BGJobsFromServersFetcher = std::function<std::optional<std::unordered_multimap<UUID, BGJobInfoFromServer>>(
    Context &,
    CnchBGThreadType,
    Poco::Logger *,
    const std::vector<String> &
)>;

class DaemonJobServerBGThread : public DaemonJob
{
public:
    using DaemonJob::DaemonJob;
    void init() override;
    CnchServerClientPtr getTargetServer(const StorageID &, UInt64) const;
    void setStorageCache(StorageCache * cache_) { cache = cache_; }
    StorageCache * getStorageCache() { return cache; }
    void setLivenessCheckInterval(size_t interval) { liveness_check_interval = interval; }
    BackgroundJobPtr getBackgroundJob(const UUID & uuid) const;
    BGJobInfos getBGJobInfos() const;
    Result executeJobAction(const StorageID & storage_id, CnchBGThreadAction action);
    virtual bool isTargetTable(const StoragePtr &) const { return false; }
    IBackgroundJobExecutor & getBgJobExecutor() const { return *bg_job_executor; }

    /// for unit test
    DaemonJobServerBGThread(ContextMutablePtr global_context_, CnchBGThreadType type_,
        std::unique_ptr<IBackgroundJobExecutor> bg_job_executor_,
        std::unique_ptr<IBGJobStatusPersistentStoreProxy> js_persistent_store_proxy,
        std::unique_ptr<ITargetServerCalculator> target_server_calculator);

    IBGJobStatusPersistentStoreProxy & getStatusPersistentStore() const { return *status_persistent_store; }
    std::vector<String> updateServerStartTimeAndFindRestartServers(const std::map<String, UInt64> &);

protected:
    bool executeImpl() override;
    ServerInfo findServerInfo(const std::map<String, UInt64> &, const BackgroundJobs &);
    BackgroundJobs fetchCnchBGThreadStatus();

    BackgroundJobs background_jobs;
    mutable std::shared_mutex bg_jobs_mutex;
    std::map<String, UInt64> server_start_times;
    size_t counter_for_liveness_check = 1;
    size_t liveness_check_interval = LIVENESS_CHECK_INTERVAL;
private:
    StorageCache * cache = nullptr;
    std::unique_ptr<IBGJobStatusPersistentStoreProxy> status_persistent_store{};
    std::unique_ptr<IBackgroundJobExecutor> bg_job_executor;
    std::unique_ptr<ITargetServerCalculator> target_server_calculator;
};

using DaemonJobServerBGThreadPtr = std::shared_ptr<DaemonJobServerBGThread>;

std::unordered_map<UUID, StorageID> getUUIDsFromCatalog(DaemonJobServerBGThread & daemon_job);

struct UpdateResult
{
    UUIDs remove_uuids;
    UUIDs add_uuids;
};

UpdateResult getUpdateBGJobs(
    const BackgroundJobs & background_jobs,
    const std::unordered_map<UUID, StorageID> & new_uuid_map,
    const std::vector<String> & alive_servers
);

bool checkIfServerDied(const std::vector<String> & alive_host_port, const String & host_port);
std::vector<String> findAliveServers(const std::map<String, UInt64> &);
std::set<UUID> getUUIDsFromBackgroundJobs(const BackgroundJobs & background_jobs);
std::unordered_map<UUID, String> getAllTargetServerForBGJob(
    const BackgroundJobs & bg_jobs,
    UInt64 ts,
    DaemonJobServerBGThread & daemon_job);

size_t checkLivenessIfNeed(
    size_t counter,
    size_t liveness_check_interval,
    Context & context,
    DaemonJobServerBGThread & daemon_job,
    BackgroundJobs & check_bg_jobs,
    const std::vector<String> & servers,
    BGJobsFromServersFetcher fetch_bg_jobs_from_server
);

void runMissingAndRemoveDuplicateJob(
    DaemonJobServerBGThread &,
    BackgroundJobs &,
    const std::unordered_multimap<UUID, BGJobInfoFromServer> &);

template <CnchBGThreadType T, bool (*isTargetTableF)(const StoragePtr &)>
struct DaemonJobForCnch : public DaemonJobServerBGThread
{
    DaemonJobForCnch(ContextMutablePtr global_context_) : DaemonJobServerBGThread(global_context_, T) { }
    bool isTargetTable(const StoragePtr & storage) const override { return isTargetTableF(storage); }
};

bool isCnchMergeTree(const StoragePtr & storage);

struct DaemonJobForMergeMutate : public DaemonJobForCnch<CnchBGThreadType::MergeMutate, isCnchMergeTree>
{
    using DaemonJobForCnch<CnchBGThreadType::MergeMutate, isCnchMergeTree>::DaemonJobForCnch;
    void executeOptimize(const StorageID & storage_id, const String & partition_id, bool enable_try, bool mutations_sync, UInt64 timeout_ms) const;
};

} /// end namespace DB::DaemonManager
