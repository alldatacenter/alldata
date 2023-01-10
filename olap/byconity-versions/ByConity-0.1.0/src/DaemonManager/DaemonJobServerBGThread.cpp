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

#include <DaemonManager/DaemonJobServerBGThread.h>
#include <Catalog/Catalog.h>
#include <Catalog/CatalogFactory.h>
#include <DaemonManager/DaemonFactory.h>
#include <DaemonManager/DaemonHelper.h>
#include <MergeTreeCommon/CnchTopologyMaster.h>
#include <Storages/StorageCnchMergeTree.h>
#include <Storages/Kafka/StorageCnchKafka.h>
#include <CloudServices/CnchServerClientPool.h>

namespace DB::DaemonManager
{

DaemonJobServerBGThread::DaemonJobServerBGThread(
    ContextMutablePtr global_context_,
    CnchBGThreadType type_,
    std::unique_ptr<IBackgroundJobExecutor> bg_job_executor_,
    std::unique_ptr<IBGJobStatusPersistentStoreProxy> status_persistent_store_proxy,
    std::unique_ptr<ITargetServerCalculator> target_server_calculator_)
    : DaemonJob(std::move(global_context_), type_),
      status_persistent_store{std::move(status_persistent_store_proxy)},
      bg_job_executor{std::move(bg_job_executor_)},
      target_server_calculator{std::move(target_server_calculator_)}
{}

void fixKafkaActiveStatuses(DaemonJobServerBGThread * daemon_job);
void DaemonJobServerBGThread::init()
{
    if (getType() == CnchBGThreadType::Consumer)
        fixKafkaActiveStatuses(this);
    background_jobs = fetchCnchBGThreadStatus();
    status_persistent_store =
        std::make_unique<BGJobStatusInCatalog::CatalogBGJobStatusPersistentStoreProxy>(getContext()->getCnchCatalog(), type);
    bg_job_executor = std::make_unique<BackgroundJobExecutor>(*getContext(), getType());
    target_server_calculator = std::make_unique<TargetServerCalculator>(*getContext(), getType(), getLog());
    DaemonJob::init();
}

std::unordered_map<UUID, StorageID> getUUIDsFromCatalog(DaemonJobServerBGThread & daemon_job)
{
    const Context & context = *daemon_job.getContext();
    auto data_models = context.getCnchCatalog()->getAllTables();
    std::unordered_map<UUID, StorageID> ret;
    Poco::Logger * log = daemon_job.getLog();
    for (const auto & data_model : data_models)
    {
        auto uuid = RPCHelpers::createUUID(data_model.uuid());

        if (Status::isDetached(data_model.status()) || Status::isDeleted(data_model.status()))
            continue;

        try
        {
            StoragePtr storage = nullptr;
            if (auto cache = daemon_job.getStorageCache(); cache)
            {
                auto res = cache->getOrSet(data_model.definition(), [&]()
                {
                    return Catalog::CatalogFactory::getTableByDefinition(
                        daemon_job.getContext(),
                        data_model.database(),
                        data_model.name(),
                        data_model.definition());
                });
                storage = std::move(res.first);
            }
            else
            {
                storage = Catalog::CatalogFactory::getTableByDefinition(
                        daemon_job.getContext(),
                        data_model.database(),
                        data_model.name(),
                        data_model.definition());
            }

            if (!storage)
            {
                LOG_WARNING(log, "Fail to get storagePtr for {}.{}", data_model.database(), data_model.name());
                continue;
            }
            if (daemon_job.isTargetTable(storage))
                ret.insert(std::make_pair(uuid, storage->getStorageID()));
        }
        catch (Exception & e)
        {
            LOG_WARNING(log, "Fail to schedule for {}.{}. Error: ", data_model.database(), data_model.name(), e.message());
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
        }
    }

    return ret;
}

std::set<UUID> getUUIDsFromBackgroundJobs(const BackgroundJobs & background_jobs)
{
    std::set<UUID> ret;
    std::transform(background_jobs.begin(), background_jobs.end()
        , std::inserter(ret, ret.end()),
        [] (const auto & p) { return p.first;}
    );
    return ret;
}

const std::vector<String> getServersInTopology(Context & context, Poco::Logger * log)
{
    std::vector<String> ret;
    std::shared_ptr<CnchTopologyMaster> topology_master = context.getCnchTopologyMaster();
    if (!topology_master)
    {
        LOG_ERROR(log, "Failed to get topology master");
        return ret;
    }

    std::list<CnchServerTopology> server_topologies = topology_master->getCurrentTopology();
    if (server_topologies.empty())
    {
        LOG_ERROR(log, "Server topology is empty, something wrong with topology");
        return ret;
    }

    HostWithPortsVec host_ports = server_topologies.back().getServerList();

    std::transform(host_ports.begin(), host_ports.end(), std::back_inserter(ret),
        [] (const HostWithPorts & host_port)
        {
            return host_port.getRPCAddress();
        }
    );

    return ret;
}

std::vector<String> findAliveServers(const std::map<String, UInt64> & new_server_start_time)
{
    std::vector<String> ret;
    std::transform(new_server_start_time.begin(), new_server_start_time.end()
        , std::back_inserter(ret),
        [] (const auto & p) { return p.first;}
    );
    return ret;
}

bool checkIfServerDied(const std::vector<String> & alive_host_port, const String & host_port)
{
    return (alive_host_port.end() ==
        std::find(alive_host_port.begin(), alive_host_port.end(), host_port));
}

std::map<String, UInt64> fetchServerStartTimes(Context & context, CnchTopologyMaster & topology_master, Poco::Logger * log)
{
    std::map<String, UInt64> ret;
    std::list<CnchServerTopology> server_topologies = topology_master.getCurrentTopology();
    if (server_topologies.empty())
    {
        LOG_ERROR(log, "Server topology is empty, something wrong with topology, this iteration will be skip!");
        return ret;
    }

    HostWithPortsVec host_ports = server_topologies.back().getServerList();

    for (const auto & host_port : host_ports)
    {
        String rpc_address = host_port.getRPCAddress();
        CnchServerClientPtr client_ptr = context.getCnchServerClientPool().get(host_port);
        if (!client_ptr)
            continue;

        try
        {
            UInt64 ts = client_ptr->getServerStartTime();
            ret.insert(std::make_pair(rpc_address, ts));
        }
        catch (...)
        {
            LOG_INFO(log, "Failed to reach server: {}", rpc_address);
        }
    }

    if (ret.size() != host_ports.size())
    {
        LOG_WARNING(log, "There is network partition, return empty result to skip this iteration");
        ret.clear();
    }

    return ret;
}

std::vector<String> DaemonJobServerBGThread::updateServerStartTimeAndFindRestartServers(const std::map<String, UInt64> & new_server_start_time)
{
    std::vector<String> ret;
    std::for_each(new_server_start_time.begin(), new_server_start_time.end(),
        [& ret, this] (const auto & p)
        {
            if (auto it = this->server_start_times.find(p.first); it != this->server_start_times.end())
            {
                if (p.second != it->second)
                    ret.push_back(p.first);
            }
        });

    server_start_times = new_server_start_time;
    return ret;
}

std::unordered_map<UUID, String> getAllTargetServerForBGJob(
    const BackgroundJobs & bg_jobs,
    UInt64 ts,
    DaemonJobServerBGThread & daemon_job)
{
    Poco::Logger * log = daemon_job.getLog();
    std::unordered_map<UUID, String> ret;
    for (const auto & p : bg_jobs)
    {
        StorageID storage_id({}, TABLE_WITH_UUID_NAME_PLACEHOLDER, p.first);
        CnchServerClientPtr client_ptr = nullptr;
        try
        {
            client_ptr = daemon_job.getTargetServer(storage_id, ts);
        }
        catch (const Exception & e)
        {
            LOG_WARNING(log, " Got exception {}. {} when getTargetServer for {}",
                e.code(), e.displayText(), storage_id.getNameForLogs());
        }
        catch (...)
        {
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
        }

        if (!client_ptr)
        {
            LOG_WARNING(log, "Failed to getTargetServer for {}", storage_id.getNameForLogs());
            continue;
        }

        ret.insert(std::make_pair(p.first, client_ptr->getRPCAddress()));
    }

    return ret;
}

/// TODO: pass const CnchTopologyMaster
ServerInfo DaemonJobServerBGThread::findServerInfo(
    const std::map<String, UInt64> & new_server_start_times,
    const BackgroundJobs & bg_jobs)
{
    ServerInfo ret;
    ret.alive_servers = findAliveServers(new_server_start_times);
    if (ret.alive_servers.empty())
        return ret;
    ret.target_host_map = getAllTargetServerForBGJob(bg_jobs, getContext()->getTimestamp(), *this);
    if (ret.target_host_map.empty())
        return ret;
    ret.restarted_servers = updateServerStartTimeAndFindRestartServers(new_server_start_times);
    return ret;
}

UpdateResult getUpdateBGJobs(
    const BackgroundJobs & background_jobs,
    const std::unordered_map<UUID, StorageID> & new_uuid_map,
    const std::vector<String> & alive_servers
)
{
    std::set<UUID> new_uuids;
    std::transform(new_uuid_map.begin(), new_uuid_map.end()
        , std::inserter(new_uuids, new_uuids.end()),
        [] (const auto & p) { return p.first;}
    );

    std::set<UUID> current_uuids = getUUIDsFromBackgroundJobs(background_jobs);

    UUIDs add_uuids;
    UUIDs remove_uuid_candidates;

    std::set_difference(
        new_uuids.begin(), new_uuids.end(),
        current_uuids.begin(), current_uuids.end(),
        std::inserter(add_uuids, add_uuids.begin())
    );

    std::set_difference(
        current_uuids.begin(), current_uuids.end(),
        new_uuids.begin(), new_uuids.end(),
        std::inserter(remove_uuid_candidates, remove_uuid_candidates.begin())
    );

    UUIDs remove_uuids;
    std::for_each(background_jobs.begin(), background_jobs.end(),
        [& remove_uuids] (auto & p)
        {
            if (p.second->isRemoved() && (p.second->getJobExpectedStatus() == CnchBGThreadStatus::Removed))
                remove_uuids.insert(p.first);
        }
    );

    std::for_each(
        remove_uuid_candidates.begin(),
        remove_uuid_candidates.end(),
        [& remove_uuids, & background_jobs, & alive_servers] (const UUID & uuid)
        {
            auto it = background_jobs.find(uuid);
            if (it != background_jobs.end())
            {
                bool server_died = checkIfServerDied(alive_servers, it->second->getHostPort());
                if (!server_died)
                {
                    Result ret = it->second->remove(CnchBGThreadAction::Drop, false);
                    if (ret.res)
                        remove_uuids.insert(uuid);
                }
                else
                    remove_uuids.insert(uuid);
            }
        }
    );

    return UpdateResult{std::move(remove_uuids), std::move(add_uuids)};
}

void syncServerBGJob(
    DaemonJobServerBGThread & daemon_job,
    BackgroundJobPtr & job_from_dm,
    std::vector<BGJobInfoFromServer> jobs_from_server)
{
    Poco::Logger * log = daemon_job.getLog();
    StorageID storage_id = job_from_dm->getStorageID();
    String job_from_dm_host_port = job_from_dm->getHostPort();
    CnchBGThreadStatus job_from_dm_status = job_from_dm->getJobStatus();
    std::ostringstream log_oss;
    log_oss << "syncServerBGJob: storage id: " << storage_id.getNameForLogs()
        << ", info in DM: [host_port " << job_from_dm_host_port << ", status "
        << toString(job_from_dm_status) << "], info in server:";

    for (size_t i = 0; i < jobs_from_server.size(); ++i)
    {
        log_oss << " [job " << i << " host_port " << jobs_from_server[i].host_port
            << " status " << toString(jobs_from_server[i].status) << "]";
    }
    LOG_INFO(log, log_oss.str());

    std::vector<BGJobInfoFromServer> same_host_port_jobs;
    for (BGJobInfoFromServer & job_from_server : jobs_from_server)
    {
        if (job_from_server.storage_id != storage_id)
        {
            LOG_ERROR(log, "syncServerBGJob: server storage_id is different, error in program logic {}",
                job_from_server.storage_id.getNameForLogs());
            continue;
        }

        if (job_from_server.host_port != job_from_dm_host_port)
        {
            LOG_INFO(log, "syncServerBGJob: remove {} from {}",
                storage_id.getNameForLogs(), job_from_server.host_port);
            daemon_job.getBgJobExecutor().remove(storage_id, job_from_server.host_port);
        }
        else
        {
            same_host_port_jobs.push_back(job_from_server);
        }
    }

    if (same_host_port_jobs.empty())
    {
        if (job_from_dm_status == CnchBGThreadStatus::Running)
        {
            LOG_INFO(log, "syncServerBGJob: same host port job size is empty while the job in DM is running, start job");
            job_from_dm->start(false);
        }
    }
    else if (same_host_port_jobs.size() > 1)
    {
        LOG_ERROR(log, "syncServerBGJob: same host port size > 1, error in program logic");
    }
    else
    {
        BGJobInfoFromServer same_host_port_job = same_host_port_jobs[0];
        if ((job_from_dm_status == CnchBGThreadStatus::Running)
                && (same_host_port_job.status != CnchBGThreadStatus::Running))
        {
            LOG_INFO(log, "syncServerBGJob: job from dm is running but job from server isn't, start job");
            job_from_dm->start(false);
        }
        else if ((job_from_dm_status == CnchBGThreadStatus::Stopped)
                && (same_host_port_job.status == CnchBGThreadStatus::Running))
        {
            LOG_INFO(log, "syncServerBGJob: job from dm isn't running but job from server is, stop job");
            job_from_dm->stop(true, false);
        }
    }
}

void runMissingAndRemoveDuplicateJob(
    DaemonJobServerBGThread & daemon_job,
    BackgroundJobs & check_jobs,
    const std::unordered_multimap<UUID, BGJobInfoFromServer> & jobs_from_server)
{
    Poco::Logger * log = daemon_job.getLog();
    std::for_each(check_jobs.begin(), check_jobs.end(),
        [& jobs_from_server, & log, & daemon_job] (auto & p)
        {
            BackgroundJobPtr & job = p.second;
            int count = jobs_from_server.count(p.first);
            if (count == 0)
            {
                if (job->isRunning())
                {
                    LOG_INFO(log, "There is no running job for missing_job {} in server, will run this missing job",
                        job->getStorageID().getNameForLogs());
                    job->start(false);
                }
            }
            else if (count == 1)
            {
                auto it = jobs_from_server.find(p.first);
                const BGJobInfoFromServer & job_from_server = it->second;
                if ((job_from_server.host_port != job->getHostPort())
                    || (job_from_server.status != job->getJobStatus()))
                {
                    LOG_INFO(log, "There are different between job info in server and DM {}"
                        , job->getStorageID().getNameForLogs());
                    syncServerBGJob(daemon_job, job, {job_from_server});
                }
            }
            else
            {
                LOG_INFO(log, "There are more than 1 jobs in server for {}"
                    , job->getStorageID().getNameForLogs());
                std::vector<BGJobInfoFromServer> duplicate_jobs_from_server;
                auto range = jobs_from_server.equal_range(p.first);
                for (auto it = range.first; it != range.second; ++it)
                    duplicate_jobs_from_server.push_back(it->second);

                syncServerBGJob(daemon_job, job, std::move(duplicate_jobs_from_server));
            }
        });
}

std::optional<std::unordered_multimap<UUID, BGJobInfoFromServer>> fetchBGThreadFromServer(
    Context & context,
    CnchBGThreadType type,
    Poco::Logger * log,
    const std::vector<String> & servers
)
{
    std::unordered_multimap<UUID, BGJobInfoFromServer> bg_jobs_data;
    try
    {
        for (const String & server : servers)
        {
            CnchServerClientPtr client_ptr = context.getCnchServerClientPool().get(server);
            if (!client_ptr)
                return {};
            auto tasks = client_ptr->getBackGroundStatus(type);
            for (const auto & task : tasks)
            {
                StorageID storage_id = RPCHelpers::createStorageID(task.storage_id());
                CnchBGThreadStatus task_status = CnchBGThreadStatus(task.status());
                bg_jobs_data.insert(std::make_pair(storage_id.uuid, BGJobInfoFromServer{storage_id, task_status, server}));
            }
        }
    }
    catch(...)
    {
        tryLogCurrentException(log, "Failed to get jobs in servers for liveness check");
        bg_jobs_data.clear();
    }

    if (bg_jobs_data.empty())
        return {};
    return bg_jobs_data;
}

size_t checkLivenessIfNeed(
    size_t counter,
    size_t liveness_check_interval,
    Context & context,
    DaemonJobServerBGThread & daemon_job,
    BackgroundJobs & check_bg_jobs,
    const std::vector<String> & servers,
    BGJobsFromServersFetcher fetch_bg_jobs_from_server /*fetchBGThreadFromServer*/
)
{
    const CnchBGThreadType type = daemon_job.getType();
    Poco::Logger * log = daemon_job.getLog();
    if ((counter % liveness_check_interval) != 0)
        return counter + 1;

    LOG_INFO(log, "Check liveness start");
    Stopwatch watch;
    auto bg_jobs_from_server =
        fetch_bg_jobs_from_server(context, type, log, servers);
    UInt64 milliseconds = watch.elapsedMilliseconds();
    if (milliseconds >= SLOW_EXECUTION_THRESHOLD_MS)
        LOG_DEBUG(log, "fetch bg jobs from server took {} ms.", milliseconds);

    if (!bg_jobs_from_server)
        return counter;
    runMissingAndRemoveDuplicateJob(daemon_job, check_bg_jobs, bg_jobs_from_server.value());
    return counter + 1;
}

/// every failed call on BackgroundJob in this function will be retried on next time
bool DaemonJobServerBGThread::executeImpl()
{
    Context & context = *getContext();
    std::shared_ptr<CnchTopologyMaster> topology_master = context.getCnchTopologyMaster();
    if (!topology_master)
    {
        LOG_ERROR(log, "Failed to get topology master");
        return false;
    }

    UInt64 milliseconds = 0;
    Stopwatch watch;
    BackgroundJobs background_jobs_clone;
    {
        std::shared_lock shared_lock(bg_jobs_mutex);
        background_jobs_clone = background_jobs;
    }

    milliseconds = watch.elapsedMilliseconds();
    if (milliseconds >= SLOW_EXECUTION_THRESHOLD_MS)
        LOG_DEBUG(log, "copy background jobs took {} ms.", milliseconds);

    watch.restart();
    std::unordered_map<UUID, StorageID> new_uuid_map = getUUIDsFromCatalog(*this);
    milliseconds = watch.elapsedMilliseconds();
    if (milliseconds >= SLOW_EXECUTION_THRESHOLD_MS)
        LOG_DEBUG(log, "getUUIDsFromCatalog took {} ms.", milliseconds);

    std::map<String, UInt64> new_server_start_times = fetchServerStartTimes(context, *topology_master, log);
    if (new_server_start_times.empty())
    {
        LOG_WARNING(log, "There are network partition, skip this iteration");
        return false;
    }

    const std::vector<String> alive_servers = findAliveServers(new_server_start_times);
    watch.restart();
    UpdateResult update_res = getUpdateBGJobs(background_jobs_clone, new_uuid_map, alive_servers);
    milliseconds = watch.elapsedMilliseconds();
    if (milliseconds >= SLOW_EXECUTION_THRESHOLD_MS)
        LOG_DEBUG(log, "getUpdateBGJobs took {} ms.", milliseconds);

    const UUIDs & remove_uuids = update_res.remove_uuids;
    for (auto uuid : remove_uuids)
        LOG_DEBUG(log, "UUID: {} will be removed from background jobs", UUIDHelpers::UUIDToString(uuid));

    const UUIDs & add_uuids = update_res.add_uuids;
    std::vector<BackgroundJobPtr> new_bg_jobs;

    watch.restart();
    if ((!remove_uuids.empty()) || (!add_uuids.empty()))
    {
        std::unique_lock lock(bg_jobs_mutex);
        std::for_each(remove_uuids.begin(), remove_uuids.end(), [this] (UUID uuid)
            {
                background_jobs.erase(uuid);
            });

        std::for_each(add_uuids.begin(), add_uuids.end(), [this, & new_uuid_map, &new_bg_jobs] (UUID uuid)
            {
                auto ret = background_jobs.insert(std::make_pair(uuid, std::make_shared<BackgroundJob>(new_uuid_map.at(uuid), *this)));
                if (ret.second)
                    new_bg_jobs.push_back(ret.first->second);
            });
    }

    milliseconds = watch.elapsedMilliseconds();
    if (milliseconds >= SLOW_EXECUTION_THRESHOLD_MS)
        LOG_DEBUG(log, "update bg jobs took {} ms.", milliseconds);

    std::for_each(remove_uuids.begin(), remove_uuids.end(), [& background_jobs_clone] (UUID uuid)
        {
            background_jobs_clone.erase(uuid);
        });

    if (background_jobs_clone.empty())
    {
        LOG_WARNING(log, "There is no jobs in background_jobs, skip sync");
        return true;
    }

    new_server_start_times = fetchServerStartTimes(context, *topology_master, log);
    if (new_server_start_times.empty())
    {
        LOG_WARNING(log, "There are network partition, skip sync");
        return false;
    }

    ServerInfo server_info = findServerInfo(new_server_start_times, background_jobs_clone);

    if (!server_info.restarted_servers.empty())
        LOG_INFO(log, "Found restart server!");

    if (server_info.alive_servers.empty())
    {
        LOG_WARNING(log, "Failed to found alive_servers, skip sync");
        return false;
    }

    if (server_info.target_host_map.empty())
    {
        LOG_WARNING(log, "no target host found, skip sync");
        return false;
    }

    {
        /// Scope for CacheClearer
        watch.restart();
        // fetch statuses in batch
        auto cache_clearer = status_persistent_store->fetchStatusesIntoCache();
        milliseconds = watch.elapsedMilliseconds();
        if (milliseconds >= SLOW_EXECUTION_THRESHOLD_MS)
            LOG_DEBUG(log, "fetch bg job statuses took {} ms.", milliseconds);

        watch.restart();
        std::for_each(
            background_jobs_clone.begin(),
            background_jobs_clone.end(),
            [&server_info] (const auto & p)
            {
                p.second->sync(server_info);
            }
        );
    }

    milliseconds = watch.elapsedMilliseconds();
    if (milliseconds >= SLOW_EXECUTION_THRESHOLD_MS)
        LOG_DEBUG(log, "sync bg jobs took {} ms.", milliseconds);

    watch.restart();
    counter_for_liveness_check = checkLivenessIfNeed(
        counter_for_liveness_check,
        liveness_check_interval,
        context,
        *this,
        background_jobs_clone,
        server_info.alive_servers,
        fetchBGThreadFromServer
    );

    milliseconds = watch.elapsedMilliseconds();
    if (milliseconds >= SLOW_EXECUTION_THRESHOLD_MS)
        LOG_DEBUG(log, "check liveness took {} ms.", milliseconds);
    return true;
}

CnchServerClientPtr DaemonJobServerBGThread::getTargetServer(const StorageID & storage_id, UInt64 ts) const
{
    return target_server_calculator->getTargetServer(storage_id, ts);
}

BGJobInfos DaemonJobServerBGThread::getBGJobInfos() const
{
    std::vector<BGJobInfo> res;
    BackgroundJobs background_jobs_clone;
    {
        std::shared_lock shared_lock(bg_jobs_mutex);
        background_jobs_clone = background_jobs;
    }

    std::transform(
        background_jobs_clone.begin(),
        background_jobs_clone.end(),
        std::back_inserter(res),
        [] (const std::pair<UUID, BackgroundJobPtr> & p)
            {
                return p.second->getBGJobInfo();
            }
        );
    return res;
}

BackgroundJobPtr DaemonJobServerBGThread::getBackgroundJob(const UUID & uuid) const
{
    std::shared_lock lock(bg_jobs_mutex);
    if (auto it = background_jobs.find(uuid); it != background_jobs.end())
    {
        return it->second;
    }

    return nullptr;
}

/// called from BRPC server, execute synchonously, no retry, persist job status to persistent storage
Result DaemonJobServerBGThread::executeJobAction(const StorageID & storage_id, CnchBGThreadAction action)
{
    Context & context = *getContext();
    LOG_DEBUG(log, "Executing a job action for uuid: {} {}", storage_id.getNameForLogs(), toString(action));
    UUID uuid = storage_id.uuid;

    switch (action)
    {
        case CnchBGThreadAction::Remove:
        case CnchBGThreadAction::Drop:
        {
            auto bg_ptr = getBackgroundJob(uuid);
            if (!bg_ptr)
            {
                String error_msg{"No job for uuid: " + storage_id.getNameForLogs()};
                LOG_WARNING(log, error_msg);
                return {error_msg, false};
            }
            else
            {
                const std::vector<String> servers = getServersInTopology(context, log);
                if (servers.empty())
                {
                    String error_msg{String("Failed to ") + toString(action) + ": " + storage_id.getNameForLogs()
                        + " because failed to get servers in topology"};
                    LOG_WARNING(log, error_msg);
                    return {error_msg, false};
                }

                bool server_died = checkIfServerDied(servers, bg_ptr->getHostPort());
                if (server_died)
                {
                    std::unique_lock lock(bg_jobs_mutex);
                    background_jobs.erase(uuid);
                    return {"", true};
                }
                else
                {
                    return bg_ptr->remove(action, true);
                }
            }
        }
        case CnchBGThreadAction::Stop:
        {
            auto bg_ptr = getBackgroundJob(uuid);
            if (!bg_ptr)
            {
                LOG_INFO(log, "bg job doesn't exist for uuid: {} hence, create a stop job", storage_id.getNameForLogs());
                auto new_job = std::make_pair(uuid, std::make_shared<BackgroundJob>(storage_id, CnchBGThreadStatus::Stopped, *this, ""));
                std::unique_lock lock(bg_jobs_mutex);
                auto res = background_jobs.insert(std::move(new_job));
                if (!res.second)
                    bg_ptr = res.first->second;
                else
                    return {"", true};
            }
            if (bg_ptr)
                return bg_ptr->stop(false, true);
            break;
        }
        case CnchBGThreadAction::Start:
        {
            auto bg_ptr = getBackgroundJob(uuid);
            if (!bg_ptr)
            {
                auto new_job = std::make_pair(uuid, std::make_shared<BackgroundJob>(storage_id, *this));
                std::unique_lock lock(bg_jobs_mutex);
                auto res = background_jobs.insert(new_job);
                if (res.second)
                    bg_ptr = res.first->second;
                else
                    return {"Failed to insert this uuid to background_jobs, the jobs probably has been started recently", false};
            }

            if (bg_ptr)
            {
                const String current_host_port = bg_ptr->getHostPort();
                if ((!bg_ptr->isRemoved()) &&
                    (!current_host_port.empty()))
                {
                    const std::vector<String> servers = getServersInTopology(context, log);
                    if (servers.empty())
                    {
                        String error_str = "failed to remove: " + storage_id.getNameForLogs()
                            + " because failed to get servers in topology";
                        LOG_WARNING(log, error_str);
                        return {error_str, false};
                    }

                    bool server_died = checkIfServerDied(servers, current_host_port);
                    if (!server_died)
                    {
                        LOG_INFO(log, "remove bg job: {} in {} before start new job", storage_id.getNameForLogs(), current_host_port);
                        Result res = bg_ptr->remove(CnchBGThreadAction::Remove, false);
                        if (!res.res)
                            return res;
                    }
                }
                return bg_ptr->start(true);
            }
            break;
        }
        case CnchBGThreadAction::Wakeup:
        {
            auto bg_ptr = getBackgroundJob(uuid);
            if (!bg_ptr)
            {
                auto error_msg = fmt::format("bg job doesn't exist for table: {} hence, stop wakeup", storage_id.getNameForLogs());
                return {error_msg, false};
            }
            else
                return bg_ptr->wakeup();
        }
    }
    return {"", false};
}

void DaemonJobForMergeMutate::executeOptimize(const StorageID & storage_id, const String & partition_id, bool enable_try, bool mutations_sync, UInt64 timeout_ms) const
{
    auto bg_ptr = getBackgroundJob(storage_id.uuid);
    if (!bg_ptr)
    {
        throw Exception(ErrorCodes::LOGICAL_ERROR, "bg job doesn't exist for table: {}, stop wakeup", storage_id.getNameForLogs());
    }

    auto info = bg_ptr->getBGJobInfo();

    if (info.status != CnchBGThreadStatus::Running)
    {
        throw Exception(
            ErrorCodes::LOGICAL_ERROR,
            "Can't execute optimize, because background job: {} at server {} in {} status",
            storage_id.getNameForLogs(), info.host_port, toString(info.status));
    }

    CnchServerClientPtr server_client = getContext()->getCnchServerClient(info.host_port);
    server_client->executeOptimize(storage_id, partition_id, enable_try, mutations_sync, timeout_ms);
}

BackgroundJobs DaemonJobServerBGThread::fetchCnchBGThreadStatus()
{
    BackgroundJobs ret;
    CnchServerClientPtrs cnch_servers = getContext()->getCnchServerClientPool().getAll();

    for (const auto & cnch_server : cnch_servers)
    {
        if (!cnch_server)
            continue;

        server_start_times[cnch_server->getRPCAddress()] = cnch_server->getServerStartTime();
        auto tasks = cnch_server->getBackGroundStatus(type);
        for (const auto & task : tasks)
        {
            StorageID storage_id = RPCHelpers::createStorageID(task.storage_id());

            auto task_status = CnchBGThreadStatus(task.status());
            LOG_TRACE(log, "{} is {} on {}", storage_id.getNameForLogs(), toString(task_status), cnch_server->getRPCAddress());

            if (auto it = ret.find(storage_id.uuid); it != ret.end())
            {
                // find duplicate threads
                auto & info = it->second;

                if (task_status == CnchBGThreadStatus::Running)
                {
                    if (CnchBGThreadStatus::Running == info->getJobStatus())
                    {
                        // remove a duplicate running task
                        try
                        {
                            getBgJobExecutor().remove(storage_id, cnch_server->getRPCAddress());
                        }
                        catch (...)
                        {
                            tryLogCurrentException(log, "Fail to remove duplicated task: " + storage_id.getNameForLogs());
                        }
                    }
                    else
                    {
                        // one stopped and one running case, keep the running one
                        ret.erase(it);
                        ret.insert(std::make_pair(storage_id.uuid, std::make_shared<BackgroundJob>(storage_id, CnchBGThreadStatus::Running, *this, cnch_server->getRPCAddress())));
                    }
                }
                else if (task_status == CnchBGThreadStatus::Stopped)
                {
                    // remove duplicate stop task
                    try
                    {
                        getBgJobExecutor().remove(storage_id, cnch_server->getRPCAddress());
                    }
                    catch (...)
                    {
                        tryLogCurrentException(log, "Fail to remove duplicated task: " + storage_id.getNameForLogs());
                    }
                }
            }
            else
            {
                ret.insert(std::make_pair(storage_id.uuid, std::make_shared<BackgroundJob>(storage_id, task_status, *this, cnch_server->getRPCAddress())));
            }
        }
    }
    return ret;
}

bool isCnchMergeTree(const StoragePtr & storage)
{
    return dynamic_cast<StorageCnchMergeTree *>(storage.get()) != nullptr;
}

bool isCnchKafka(const StoragePtr & storage)
{
    return dynamic_cast<StorageCnchKafka *>(storage.get()) != nullptr;
}

bool isCnchUniqueTableAndNeedDedup(const StoragePtr & storage)
{
    auto t = dynamic_cast<StorageCnchMergeTree *>(storage.get());
    return t && t->getInMemoryMetadataPtr()->hasUniqueKey();
}

void registerServerBGThreads(DaemonFactory & factory)
{
    factory.registerDaemonJobForBGThreadInServer<DaemonJobForCnch<CnchBGThreadType::PartGC, isCnchMergeTree>>("PART_GC");
    factory.registerDaemonJobForBGThreadInServer<DaemonJobForMergeMutate>("PART_MERGE");
    factory.registerDaemonJobForBGThreadInServer<DaemonJobForCnch<CnchBGThreadType::Clustering, isCnchMergeTree>>("PART_CLUSTERING");
    factory.registerDaemonJobForBGThreadInServer<DaemonJobForCnch<CnchBGThreadType::Consumer, isCnchKafka>>("CONSUMER");
    factory.registerDaemonJobForBGThreadInServer<DaemonJobForCnch<CnchBGThreadType::DedupWorker, isCnchUniqueTableAndNeedDedup>>("DEDUP_WORKER");
}

void fixKafkaActiveStatuses(DaemonJobServerBGThread * daemon_job)
{
    Poco::Logger * log = daemon_job->getLog();
    ContextMutablePtr context = daemon_job->getContext();
    std::shared_ptr<Catalog::Catalog> catalog = context->getCnchCatalog();
    auto data_models = catalog->getAllTables();
    for (const auto & data_model : data_models)
    {
        if (Status::isDetached(data_model.status()) || Status::isDeleted(data_model.status()))
            continue;

        try
        {
            StoragePtr storage = Catalog::CatalogFactory::getTableByDefinition(
                        daemon_job->getContext(),
                        data_model.database(),
                        data_model.name(),
                        data_model.definition());

            if (!storage)
            {
                LOG_WARNING(log, "Fail to get storagePtr for {}.{}", data_model.database(), data_model.name());
                continue;
            }

            if (daemon_job->isTargetTable(storage))
            {
                if (!catalog->getTableActiveness(storage, TxnTimestamp::maxTS()))
                {
                    LOG_INFO(log, "Found table {}.{} is inactive", data_model.database(), data_model.name());
                    catalog->setBGJobStatus(storage->getStorageID().uuid, CnchBGThreadType::Consumer, CnchBGThreadStatus::Stopped);
                    catalog->setTableActiveness(storage, true, TxnTimestamp::maxTS());
                }
            }
        }
        catch (Exception & e)
        {
            LOG_WARNING(log, "Fail to construct storage for {}.{}. Error: ", data_model.database(), data_model.name(), e.message());
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
        }
    }
}

}
