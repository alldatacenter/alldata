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

#include <Common/tests/gtest_global_context.h>
#include <DaemonManager/BackgroudJobExecutor.h>
#include <DaemonManager/BackgroundJob.h>
#include <DaemonManager/DaemonJobServerBGThread.h>
#include <Interpreters/Context.h>
#include <string>
#include <gtest/gtest.h>

using namespace DB::DaemonManager;
using namespace DB;

namespace GtestDaemonJob
{

constexpr auto SERVER1 = "169.128.0.1:1223";
constexpr auto SERVER2 = "169.128.0.2:1223";
constexpr auto SERVER3 = "169.128.0.3:1223";
struct Event
{
    UUID uuid;
    CnchBGThreadAction action;
    String host_port;
    bool operator == (const Event & other) const
    {
        return (other.uuid == uuid) && (other.action == action) && (other.host_port == host_port);
    }
};

std::ostream & operator << (std::ostream & os, const Event & e)
{
    os << "[uuid: " << toString(e.uuid) << ", action:" << toString(e.action)
        << ", host_port: " << e.host_port << ']';
    return os;
}

class StableExecutor : public IBackgroundJobExecutor
{
public:
    bool start(const StorageID & storage_id, const String & host_port) override
    {
        events.push_back({storage_id.uuid, CnchBGThreadAction::Start, host_port});
        return true;
    }

    bool stop(const StorageID & storage_id, const String & host_port) override
    {
        events.push_back({storage_id.uuid, CnchBGThreadAction::Stop, host_port});
        return true;
    }

    bool remove(const StorageID & storage_id, const String & host_port) override
    {
        events.push_back({storage_id.uuid, CnchBGThreadAction::Remove, host_port});
        return true;
    }

    bool drop(const StorageID & storage_id, const String & host_port) override
    {
        events.push_back({storage_id.uuid, CnchBGThreadAction::Drop, host_port});
        return true;
    }

    bool wakeup(const StorageID & storage_id, const String & host_port) override
    {
        events.push_back({storage_id.uuid, CnchBGThreadAction::Wakeup, host_port});
        return true;
    }

public:
    std::vector<Event> events;
};

class MockTargetServerCalculater : public ITargetServerCalculator
{
public:
    MockTargetServerCalculater() = default;
    CnchServerClientPtr getTargetServer(const StorageID &, UInt64) const override
    {
        HostWithPorts hp{"169.128.0.2", 1223, 1224, 1225}; /// SERVER2
        return std::make_shared<CnchServerClient>(hp);
    }
};

class StablePersistentStoreProxy : public IBGJobStatusPersistentStoreProxy
{
public:
    StablePersistentStoreProxy() = default;
    std::optional<CnchBGThreadStatus> createStatusIfNotExist(const StorageID & storage_id, CnchBGThreadStatus init_status) const override
    {
        auto it = stored_statuses.find(storage_id.uuid);
        if (it != stored_statuses.end())
            return it->second;
        stored_statuses.insert(std::make_pair(storage_id.uuid, init_status));
        return init_status;
    }

    void setStatus(const UUID & table_uuid, CnchBGThreadStatus status) const override
    {
        auto it = stored_statuses.find(table_uuid);
        if (it == stored_statuses.end())
            throw Exception("uuid not found", ErrorCodes::LOGICAL_ERROR);
        it->second = status;
    }

    CnchBGThreadStatus getStatus(const UUID & table_uuid, bool ) const override
    {
        return stored_statuses.at(table_uuid);
    }

    CacheClearer fetchStatusesIntoCache() override
    {
        return CacheClearer{this};
    }

    mutable std::map<UUID, CnchBGThreadStatus> stored_statuses;
};

UUID uuid1 = UUID{UInt128{0, 1}};
StorageID storage_id1 = {"db1", "tb1", uuid1};
UUID uuid2 = UUID{UInt128{0, 2}};
StorageID storage_id2 = {"db2", "tb2", uuid2};
UUID uuid3 = UUID{UInt128{0, 3}};
StorageID storage_id3 = {"db3", "tb3", uuid3};
UUID uuid4 = UUID{UInt128{0, 4}};
StorageID storage_id4 = {"db4", "tb4", uuid4};
UUID uuid5 = UUID{UInt128{0, 5}};
StorageID storage_id5 = {"db5", "tb5", uuid5};
UUID uuid6 = UUID{UInt128{0, 6}};
StorageID storage_id6 = {"db6", "tb6", uuid6};
UUID uuid7 = UUID{UInt128{0, 7}};
StorageID storage_id7 = {"db7", "tb7", uuid7};
UUID uuid8 = UUID{UInt128{0, 8}};
StorageID storage_id8 = {"db8", "tb8", uuid8};
UUID uuid9 = UUID{UInt128{0, 9}};
StorageID storage_id9 = {"db9", "tb9", uuid9};
UUID uuid10 = UUID{UInt128{0, 10}};
StorageID storage_id10 = {"db10", "tb10", uuid10};

TEST(daemon_job, getUUIDsFromBackgroundJobs)
{
    BackgroundJobs bg_jobs;
    auto uuids = getUUIDsFromBackgroundJobs(bg_jobs);
    EXPECT_EQ(uuids.empty(), true);

    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());

    bg_jobs.insert(std::make_pair(uuid1, std::make_shared<BackgroundJob>(storage_id1, CnchBGThreadStatus::Running, daemon_job, "")));

    bg_jobs.insert(std::make_pair(uuid2, std::make_shared<BackgroundJob>(storage_id2, CnchBGThreadStatus::Running, daemon_job, "")));
    uuids = getUUIDsFromBackgroundJobs(bg_jobs);

    EXPECT_EQ(uuids.size(), 2);
    std::set<UUID> expected_res {uuid1, uuid2};
    EXPECT_EQ(uuids, expected_res);
}


class UnstableExecutor : public StableExecutor
{
public:
    bool drop(const StorageID & storage_id, const String & host_port) override
    {
        if (storage_id.uuid == uuid8)
            return false;

        events.push_back({storage_id.uuid, CnchBGThreadAction::Drop, host_port});
        return true;
    }
};

TEST(daemon_job, getUpdateBGJobs)
{
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<UnstableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());

    BackgroundJobs bg_jobs;
    bg_jobs.insert(std::make_pair(uuid1, std::make_shared<BackgroundJob>(storage_id1, CnchBGThreadStatus::Running, daemon_job, SERVER2))); // remain uuid

    bg_jobs.insert(std::make_pair(uuid2, std::make_shared<BackgroundJob>(storage_id2, CnchBGThreadStatus::Stopped, daemon_job, SERVER2))); // remove uuid, server alive, stoped state

    bg_jobs.insert(std::make_pair(uuid3, std::make_shared<BackgroundJob>(storage_id3, CnchBGThreadStatus::Removed, daemon_job, SERVER2))); // remove uuid, server alive, removed state

    bg_jobs.insert(std::make_pair(uuid4, std::make_shared<BackgroundJob>(storage_id4, CnchBGThreadStatus::Running, daemon_job, SERVER1))); // remove uuid, server die

    bg_jobs.insert(std::make_pair(uuid5, std::make_shared<BackgroundJob>(storage_id5, CnchBGThreadStatus::Stopped, daemon_job, SERVER1))); // remove uuid, server die

    bg_jobs.insert(std::make_pair(uuid6, std::make_shared<BackgroundJob>(storage_id6, CnchBGThreadStatus::Removed, daemon_job, SERVER1))); // remove uuid, server die

    bg_jobs.insert(std::make_pair(uuid7, std::make_shared<BackgroundJob>(storage_id7, CnchBGThreadStatus::Running, daemon_job, SERVER2))); //// remove uuid, server alive, running state

    bg_jobs.insert(std::make_pair(uuid8, std::make_shared<BackgroundJob>(storage_id8, CnchBGThreadStatus::Running, daemon_job, SERVER2))); //// remove uuid, server alive , but remove() return failed

    auto bg_job10 = std::make_shared<BackgroundJob>(storage_id10, CnchBGThreadStatus::Removed, daemon_job, SERVER2); /// remove uuid, but expected status is Running
    bg_job10->setExpectedStatus(CnchBGThreadStatus::Running);
    bg_jobs.insert(std::make_pair(uuid10, bg_job10));

    std::unordered_map<UUID, StorageID> new_uuid_map {
        {uuid1, storage_id1},
        {uuid9, storage_id9},
        {uuid10, storage_id10},
    };

    std::vector<String> alive_servers = {SERVER2};
    UpdateResult update_res = getUpdateBGJobs(bg_jobs, new_uuid_map, alive_servers);

    EXPECT_EQ(bg_jobs.at(uuid2)->isRemoved(), true);
    EXPECT_EQ(bg_jobs.at(uuid3)->isRemoved(), true);
    EXPECT_EQ(bg_jobs.at(uuid4)->isRunning(), true);
    EXPECT_EQ(bg_jobs.at(uuid5)->isStopped(), true);
    EXPECT_EQ(bg_jobs.at(uuid6)->isRemoved(), true);
    EXPECT_EQ(bg_jobs.at(uuid7)->isRemoved(), true);
    EXPECT_EQ(bg_jobs.at(uuid8)->isRunning(), true);
    EXPECT_EQ(bg_jobs.at(uuid10)->isRemoved(), true);
    UUIDs expected_remove_uuids{uuid2, uuid3, uuid4, uuid5, uuid6, uuid7};
    EXPECT_EQ(update_res.remove_uuids, expected_remove_uuids);

    UUIDs expected_add_uuids{uuid9};
    EXPECT_EQ(update_res.add_uuids, expected_add_uuids);
}

TEST(daemon_job, test_daemon_job)
{
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<UnstableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const UnstableExecutor * executor = dynamic_cast<const UnstableExecutor * >(&daemon_job.getBgJobExecutor());
    const StablePersistentStoreProxy * proxy = dynamic_cast<const StablePersistentStoreProxy *>(&daemon_job.getStatusPersistentStore());

    Result execute_ret = daemon_job.executeJobAction(storage_id1, CnchBGThreadAction::Start);
    {
        EXPECT_EQ(execute_ret.res, true);
        EXPECT_EQ(executor->events.size(), 1);
        Event expected{uuid1, CnchBGThreadAction::Start, SERVER2};
        EXPECT_EQ(executor->events.at(0), expected);
        EXPECT_EQ(proxy->stored_statuses.at(uuid1), CnchBGThreadStatus::Running);
    }

    auto info = daemon_job.getBackgroundJob(uuid1);
    {
        EXPECT_EQ(info->getJobStatus(), CnchBGThreadStatus::Running);
        EXPECT_EQ(info->getHostPort(), SERVER2);
        EXPECT_EQ(info->getStorageID(), storage_id1);
    }

    execute_ret = daemon_job.executeJobAction(storage_id1, CnchBGThreadAction::Wakeup);
    {
        EXPECT_EQ(execute_ret.res, true);
        EXPECT_EQ(executor->events.size(), 2);
        Event expected{uuid1, CnchBGThreadAction::Wakeup, SERVER2};
        EXPECT_EQ(executor->events.at(1), expected);
        EXPECT_EQ(proxy->stored_statuses.at(uuid1), CnchBGThreadStatus::Running);
    }

    info = daemon_job.getBackgroundJob(uuid1);
    {
        EXPECT_EQ(info->getJobStatus(), CnchBGThreadStatus::Running);
        EXPECT_EQ(info->getHostPort(), SERVER2);
        EXPECT_EQ(info->getStorageID(), storage_id1);
    }

    execute_ret = daemon_job.executeJobAction(storage_id1, CnchBGThreadAction::Stop);
    {
        EXPECT_EQ(execute_ret.res, true);
        EXPECT_EQ(executor->events.size(), 3);
        Event expected{uuid1, CnchBGThreadAction::Stop, SERVER2};
        EXPECT_EQ(executor->events.at(2), expected);
        EXPECT_EQ(proxy->stored_statuses.at(uuid1), CnchBGThreadStatus::Stopped);
    }

    info = daemon_job.getBackgroundJob(uuid1);
    {
        EXPECT_EQ(info->getJobStatus(), CnchBGThreadStatus::Stopped);
        EXPECT_EQ(info->getHostPort(), SERVER2);
        EXPECT_EQ(info->getStorageID(), storage_id1);
    }
/// bring getServersInTopology into TargetServerCalculater to enable this test
#if 0
    execute_ret = daemon_job.executeJobAction(storage_id1, CnchBGThreadAction::Remove);
    {
        EXPECT_EQ(execute_ret.res, true);
        EXPECT_EQ(executor->events.size(), 4);
        Event expected{uuid1, CnchBGThreadAction::Remove, SERVER2};
        EXPECT_EQ(executor->events.at(3), expected);
        EXPECT_EQ(proxy->stored_statuses.at(uuid1), CnchBGThreadStatus::Removed);
    }

    info = daemon_job.getBackgroundJob(uuid1);
    {
        EXPECT_EQ(info->getJobStatus(), CnchBGThreadStatus::Removed);
        EXPECT_EQ(info->getHostPort(), SERVER2);
        EXPECT_EQ(info->getStorageID(), storage_id1);
    }

    execute_ret = daemon_job.executeJobAction(storage_id1, CnchBGThreadAction::Drop);
    {
        EXPECT_EQ(execute_ret.res, true);
        EXPECT_EQ(executor->events.size(), 5);
        Event expected{uuid1, CnchBGThreadAction::Drop, SERVER2};
        EXPECT_EQ(executor->events.at(4), expected);
        EXPECT_EQ(proxy->stored_statuses.at(uuid1), CnchBGThreadStatus::Removed);
    }

    info = daemon_job.getBackgroundJob(uuid1);
    {
        EXPECT_EQ(info->getJobStatus(), CnchBGThreadStatus::Removed);
        EXPECT_EQ(info->getHostPort(), SERVER2);
        EXPECT_EQ(info->getStorageID(), storage_id1);
    }
#endif
}

TEST(daemon_job, getAllTargetServerForBGJob)
{
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<UnstableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    BackgroundJobs bg_jobs;
    bg_jobs.insert(std::make_pair(uuid1, std::make_shared<BackgroundJob>(storage_id1, CnchBGThreadStatus::Running, daemon_job, SERVER1)));
    bg_jobs.insert(std::make_pair(uuid2, std::make_shared<BackgroundJob>(storage_id2, CnchBGThreadStatus::Running, daemon_job, SERVER1)));
    std::unordered_map<UUID, String> ret = getAllTargetServerForBGJob(bg_jobs, 10, daemon_job);
    std::unordered_map<UUID, String> expected_res = {
        {uuid1, SERVER2},
        {uuid2, SERVER2}
    };
    EXPECT_EQ(ret, expected_res);
}

TEST(daemon_job, findAliveServers)
{
    const std::map<String, UInt64> server_start_times = {
        {SERVER2, 123},
        {SERVER1, 124},
    };
    std::vector<String> res = findAliveServers(server_start_times);
    std::sort(res.begin(), res.end());
    std::vector<String> expected_res = {SERVER1, SERVER2};
    std::sort(expected_res.begin(), expected_res.end());
    EXPECT_EQ(res, expected_res);
}

TEST(daemon_job, updateServerStartTimeAndFindRestartServers)
{
    const std::map<String, UInt64> server_start_times_1 = {
        {SERVER3, 100},
        {SERVER2, 100},
        {SERVER1, 100}
    };
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<UnstableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());

    std::vector<String> res = daemon_job.updateServerStartTimeAndFindRestartServers(server_start_times_1);
    EXPECT_EQ(res.empty(), true);

    const std::map<String, UInt64> server_start_times_2 = {
        {SERVER2, 100},
        {SERVER1, 200}
    };

    res = daemon_job.updateServerStartTimeAndFindRestartServers(server_start_times_2);
    std::vector<String> expected_res = {SERVER1};
    EXPECT_EQ(res, expected_res);
}

TEST(daemon_job, checkIfServerDiedTrueCase)
{
    const std::vector<String> alive_host {SERVER1,SERVER2};
    bool ret = checkIfServerDied(alive_host, SERVER3);
    EXPECT_EQ(ret, true);
}

TEST(daemon_job, checkIfServerDiedFalseCase)
{
    const std::vector<String> alive_host {SERVER1,SERVER2};
    bool ret = checkIfServerDied(alive_host, SERVER1);
    EXPECT_EQ(ret, false);
}

/// Liveness test are below
TEST(daemon_job, checkLivenessIfNeed_counter_test_checking_at_right_time)
{
    auto fetch_bg_thread_from_server = []
        (Context &, CnchBGThreadType, Poco::Logger *, const std::vector<String> &)
        -> std::optional<std::unordered_multimap<UUID, BGJobInfoFromServer>>
    {
        return std::unordered_multimap<UUID, BGJobInfoFromServer>{};
    };

    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<UnstableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    BackgroundJobs empty_bgjs;
    std::vector<String> servers;
    size_t counter = LIVENESS_CHECK_INTERVAL;
    counter = checkLivenessIfNeed(
        counter,
        LIVENESS_CHECK_INTERVAL,
        *getContext().context,
        daemon_job,
        empty_bgjs,
        servers,
        fetch_bg_thread_from_server
    );
    EXPECT_EQ(counter, LIVENESS_CHECK_INTERVAL + 1);
}

TEST(daemon_job, checkLivenessIfNeed_counter_test_not_checking_when_not_in_time)
{
    auto fetch_bg_thread_from_server = []
        (Context &, CnchBGThreadType, Poco::Logger *, const std::vector<String> &)
        -> std::optional<std::unordered_multimap<UUID, BGJobInfoFromServer>>
    {
        return std::unordered_multimap<UUID, BGJobInfoFromServer>{};
    };

    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<UnstableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    BackgroundJobs empty_bgjs;
    std::vector<String> servers;
    size_t counter = LIVENESS_CHECK_INTERVAL - 1;
    counter = checkLivenessIfNeed(
        counter,
        LIVENESS_CHECK_INTERVAL,
        *getContext().context,
        daemon_job,
        empty_bgjs,
        servers,
        fetch_bg_thread_from_server
    );
    EXPECT_EQ(counter, LIVENESS_CHECK_INTERVAL);
}

TEST(daemon_job, checkLivenessIfNeed_counter_test_fetch_from_server_failed)
{
    auto fetch_bg_thread_from_server = []
        (Context &, CnchBGThreadType, Poco::Logger *, const std::vector<String> &)
        -> std::optional<std::unordered_multimap<UUID, BGJobInfoFromServer>>
    {
        return {};
    };

    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<UnstableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    BackgroundJobs empty_bgjs;
    std::vector<String> servers;
    size_t counter = LIVENESS_CHECK_INTERVAL;
    counter = checkLivenessIfNeed(
        counter,
        LIVENESS_CHECK_INTERVAL,
        *getContext().context,
        daemon_job,
        empty_bgjs,
        servers,
        fetch_bg_thread_from_server
    );
    EXPECT_EQ(counter, LIVENESS_CHECK_INTERVAL);
}

TEST(daemon_job, runMissingAndRemoveDuplicateJob_ussual_case_donothing1)
{
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    BackgroundJobPtr running_job1 = std::make_shared<BackgroundJob>(storage_id1, CnchBGThreadStatus::Running, daemon_job, SERVER1);
    BackgroundJobPtr running_job2 = std::make_shared<BackgroundJob>(storage_id2, CnchBGThreadStatus::Running, daemon_job, SERVER1);
    BackgroundJobPtr stopped_job = std::make_shared<BackgroundJob>(storage_id3, CnchBGThreadStatus::Stopped, daemon_job, SERVER1);

    BackgroundJobs bgs {
        {storage_id1.uuid, running_job1},
        {storage_id2.uuid, running_job2},
        {storage_id3.uuid, stopped_job}
    };

    std::unordered_multimap<UUID, BGJobInfoFromServer> bg_jobs_from_servers {
        {storage_id1.uuid, BGJobInfoFromServer{storage_id1, CnchBGThreadStatus::Running, SERVER1}},
        {storage_id2.uuid, BGJobInfoFromServer{storage_id1, CnchBGThreadStatus::Running, SERVER1}}
    };

    runMissingAndRemoveDuplicateJob(daemon_job, bgs, bg_jobs_from_servers);
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    EXPECT_EQ(executor->events.empty(), true);
}

TEST(daemon_job, runMissingAndRemoveDuplicateJob_ussual_case_donothing2)
{
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<UnstableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    BackgroundJobPtr running_job1 = std::make_shared<BackgroundJob>(storage_id1, CnchBGThreadStatus::Running, daemon_job, SERVER1);
    BackgroundJobPtr running_job2 = std::make_shared<BackgroundJob>(storage_id2, CnchBGThreadStatus::Running, daemon_job, SERVER1);
    BackgroundJobPtr stopped_job = std::make_shared<BackgroundJob>(storage_id3, CnchBGThreadStatus::Stopped, daemon_job, SERVER1);

    BackgroundJobs bgs {
        {storage_id1.uuid, running_job1},
        {storage_id2.uuid, running_job2},
        {storage_id3.uuid, stopped_job}
    };

    std::unordered_multimap<UUID, BGJobInfoFromServer> bg_jobs_from_servers {
        {storage_id1.uuid, BGJobInfoFromServer{storage_id1, CnchBGThreadStatus::Running, SERVER1}},
        {storage_id2.uuid, BGJobInfoFromServer{storage_id1, CnchBGThreadStatus::Running, SERVER1}},
        {storage_id3.uuid, BGJobInfoFromServer{storage_id1, CnchBGThreadStatus::Stopped, SERVER1}}
    };

    runMissingAndRemoveDuplicateJob(daemon_job, bgs, bg_jobs_from_servers);
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    EXPECT_EQ(executor->events.empty(), true);
}

TEST(daemon_job, runMissingAndRemoveDuplicateJob_runMissing)
{
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<UnstableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    BackgroundJobPtr running_job1 = std::make_shared<BackgroundJob>(storage_id1, CnchBGThreadStatus::Running, daemon_job, SERVER1);
    BackgroundJobPtr running_job2 = std::make_shared<BackgroundJob>(storage_id2, CnchBGThreadStatus::Running, daemon_job, SERVER1);
    BackgroundJobPtr stopped_job = std::make_shared<BackgroundJob>(storage_id3, CnchBGThreadStatus::Stopped, daemon_job, SERVER1);

    BackgroundJobs bgs {
        {storage_id1.uuid, running_job1},
        {storage_id2.uuid, running_job2},
        {storage_id3.uuid, stopped_job}
    };

    std::unordered_multimap<UUID, BGJobInfoFromServer> bg_jobs_from_servers {
        {storage_id1.uuid, BGJobInfoFromServer{storage_id1, CnchBGThreadStatus::Running, SERVER1}}
    };

    runMissingAndRemoveDuplicateJob(daemon_job, bgs, bg_jobs_from_servers);
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    EXPECT_EQ(executor->events.size(), 1);
    Event expected{uuid2, CnchBGThreadAction::Start, SERVER2};
    EXPECT_EQ(executor->events.at(0), expected);
}


TEST(daemon_job, runMissingAndRemoveDuplicateJob_remove_duplicate_case1)
{
    // two running, 1 have different host
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<UnstableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    BackgroundJobPtr running_job1 = std::make_shared<BackgroundJob>(storage_id1, CnchBGThreadStatus::Running, daemon_job, SERVER1);
    BackgroundJobPtr running_job2 = std::make_shared<BackgroundJob>(storage_id2, CnchBGThreadStatus::Running, daemon_job, SERVER1);
    BackgroundJobPtr stopped_job = std::make_shared<BackgroundJob>(storage_id3, CnchBGThreadStatus::Stopped, daemon_job, SERVER1);

    BackgroundJobs bgs {
        {storage_id1.uuid, running_job1},
        {storage_id2.uuid, running_job2},
        {storage_id3.uuid, stopped_job}
    };

    std::unordered_multimap<UUID, BGJobInfoFromServer> bg_jobs_from_servers {
        {storage_id1.uuid, BGJobInfoFromServer{storage_id1, CnchBGThreadStatus::Running, SERVER1}},
        {storage_id1.uuid, BGJobInfoFromServer{storage_id1, CnchBGThreadStatus::Running, SERVER2}},
        {storage_id2.uuid, BGJobInfoFromServer{storage_id2, CnchBGThreadStatus::Running, SERVER1}}
    };

    runMissingAndRemoveDuplicateJob(daemon_job, bgs, bg_jobs_from_servers);

    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    EXPECT_EQ(executor->events.size(), 1);
    Event expected{uuid1, CnchBGThreadAction::Remove, SERVER2};
    EXPECT_EQ(executor->events[0], expected);
}

TEST(daemon_job, runMissingAndRemoveDuplicateJob_remove_duplicate_case2)
{
    // 2 running, 2 have different host
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<UnstableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    BackgroundJobPtr running_job1 = std::make_shared<BackgroundJob>(storage_id1, CnchBGThreadStatus::Running, daemon_job, SERVER1);
    BackgroundJobPtr stopped_job = std::make_shared<BackgroundJob>(storage_id3, CnchBGThreadStatus::Stopped, daemon_job, SERVER1);

    BackgroundJobs bgs {
        {storage_id1.uuid, running_job1},
        {storage_id3.uuid, stopped_job}
    };

    std::unordered_multimap<UUID, BGJobInfoFromServer> bg_jobs_from_servers {
        {storage_id1.uuid, BGJobInfoFromServer{storage_id1, CnchBGThreadStatus::Running, SERVER3}},
        {storage_id1.uuid, BGJobInfoFromServer{storage_id1, CnchBGThreadStatus::Running, SERVER2}}
    };

    runMissingAndRemoveDuplicateJob(daemon_job, bgs, bg_jobs_from_servers);

    StableExecutor * executor = dynamic_cast<StableExecutor * >(&daemon_job.getBgJobExecutor());
    EXPECT_EQ(executor->events.size(), 3);
    std::sort(executor->events.begin(), executor->events.begin() + 2, [](const Event & lhs, const Event & rhs)
        {
            return (lhs.host_port.compare(rhs.host_port) < 0);
        });

    Event expected0{uuid1, CnchBGThreadAction::Remove, SERVER2};
    EXPECT_EQ(executor->events.at(0), expected0);
    Event expected1{uuid1, CnchBGThreadAction::Remove, SERVER3};
    EXPECT_EQ(executor->events.at(1), expected1);
    Event expected2{uuid1, CnchBGThreadAction::Start, SERVER2};
    EXPECT_EQ(executor->events.at(2), expected2);
}

TEST(daemon_job, runMissingAndRemoveDuplicateJob_correct_case1)
{
    // job in DM have different host
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<UnstableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    BackgroundJobPtr running_job1 = std::make_shared<BackgroundJob>(storage_id1, CnchBGThreadStatus::Running, daemon_job, SERVER1);
    BackgroundJobPtr running_job2 = std::make_shared<BackgroundJob>(storage_id2, CnchBGThreadStatus::Running, daemon_job, SERVER1);
    BackgroundJobPtr stopped_job = std::make_shared<BackgroundJob>(storage_id3, CnchBGThreadStatus::Stopped, daemon_job, SERVER1);

    BackgroundJobs bgs {
        {storage_id1.uuid, running_job1},
        {storage_id2.uuid, running_job2},
        {storage_id3.uuid, stopped_job}
    };

    std::unordered_multimap<UUID, BGJobInfoFromServer> bg_jobs_from_servers {
        {storage_id1.uuid, BGJobInfoFromServer{storage_id1, CnchBGThreadStatus::Running, SERVER1}},
        {storage_id2.uuid, BGJobInfoFromServer{storage_id2, CnchBGThreadStatus::Running, SERVER2}}
    };

    runMissingAndRemoveDuplicateJob(daemon_job, bgs, bg_jobs_from_servers);

    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    EXPECT_EQ(executor->events.size(), 2);
    Event expected0{uuid2, CnchBGThreadAction::Remove, SERVER2};
    EXPECT_EQ(executor->events[0], expected0);
    Event expected1{uuid2, CnchBGThreadAction::Start, SERVER2};
    EXPECT_EQ(executor->events[1], expected1);
}

TEST(daemon_job, runMissingAndRemoveDuplicateJob_correct_case2)
{
    /// job in dm is running, but job in server is stop
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<UnstableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    BackgroundJobPtr running_job2 = std::make_shared<BackgroundJob>(storage_id2, CnchBGThreadStatus::Running, daemon_job, SERVER1);
    BackgroundJobPtr stopped_job = std::make_shared<BackgroundJob>(storage_id3, CnchBGThreadStatus::Stopped, daemon_job, SERVER1);

    BackgroundJobs bgs {
        {storage_id2.uuid, running_job2},
        {storage_id3.uuid, stopped_job}
    };

    std::unordered_multimap<UUID, BGJobInfoFromServer> bg_jobs_from_servers {
        {storage_id2.uuid, BGJobInfoFromServer{storage_id2, CnchBGThreadStatus::Stopped, SERVER1}}
    };

    runMissingAndRemoveDuplicateJob(daemon_job, bgs, bg_jobs_from_servers);

    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    EXPECT_EQ(executor->events.size(), 1);
    Event expected{uuid2, CnchBGThreadAction::Start, SERVER2};
    EXPECT_EQ(executor->events.at(0), expected);
}

TEST(daemon_job, runMissingAndRemoveDuplicateJob_correct_case3)
{
    /// job in dm is stopped, but job in server is running
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<UnstableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    BackgroundJobPtr stopped_job = std::make_shared<BackgroundJob>(storage_id3, CnchBGThreadStatus::Stopped, daemon_job, SERVER1);

    BackgroundJobs bgs {
        {storage_id3.uuid, stopped_job}
    };

    std::unordered_multimap<UUID, BGJobInfoFromServer> bg_jobs_from_servers {
        {storage_id3.uuid, BGJobInfoFromServer{storage_id3, CnchBGThreadStatus::Running, SERVER1}}
    };

    runMissingAndRemoveDuplicateJob(daemon_job, bgs, bg_jobs_from_servers);
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    EXPECT_EQ(executor->events.size(), 1);
    Event expected{uuid3, CnchBGThreadAction::Stop, SERVER1};
    EXPECT_EQ(executor->events[0], expected);
}

TEST(daemon_job, checkLiveness_run_missing)
{
    auto fetch_bg_thread_from_server = []
        (Context &, CnchBGThreadType, Poco::Logger *, const std::vector<String> &)
        -> std::optional<std::unordered_multimap<UUID, BGJobInfoFromServer>>
    {
        return std::unordered_multimap<UUID, BGJobInfoFromServer>{
            {storage_id1.uuid, BGJobInfoFromServer{storage_id1, CnchBGThreadStatus::Running, SERVER1}}
        };
    };

    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<UnstableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    BackgroundJobPtr running_job1 = std::make_shared<BackgroundJob>(storage_id1, CnchBGThreadStatus::Running, daemon_job, SERVER1);
    BackgroundJobPtr running_job2 = std::make_shared<BackgroundJob>(storage_id2, CnchBGThreadStatus::Running, daemon_job, SERVER1);
    BackgroundJobPtr stopped_job = std::make_shared<BackgroundJob>(storage_id3, CnchBGThreadStatus::Stopped, daemon_job, SERVER1);

    BackgroundJobs bgs {
        {storage_id1.uuid, running_job1},
        {storage_id2.uuid, running_job2},
        {storage_id3.uuid, stopped_job}
    };
    std::vector<String> servers;
    size_t counter = LIVENESS_CHECK_INTERVAL;
    counter = checkLivenessIfNeed(
        counter,
        LIVENESS_CHECK_INTERVAL,
        *getContext().context,
        daemon_job,
        bgs,
        servers,
        fetch_bg_thread_from_server
    );
    EXPECT_EQ(counter, LIVENESS_CHECK_INTERVAL + 1);

    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    EXPECT_EQ(executor->events.size(), 1);
    Event expected{uuid2, CnchBGThreadAction::Start, SERVER2};
    EXPECT_EQ(executor->events.at(0), expected);
}

} // end namespace

