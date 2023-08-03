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
#include <DaemonManager/DaemonJobServerBGThread.h>
#include <DaemonManager/BackgroundJob.h>
#include <Interpreters/Context.h>
#include <gtest/gtest.h>
#include <string>
#include <thread>
#include <chrono>

using namespace DB::DaemonManager;
using namespace DB;

namespace GtestBackgroundJob
{

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

class UnstableExecutor : public IBackgroundJobExecutor
{
public:
    bool start(const StorageID &, const String &) override
    {
        return false;
    }

    bool stop(const StorageID &, const String &) override
    {
        return false;
    }

    bool remove(const StorageID &, const String &) override
    {
        return false;
    }

    bool drop(const StorageID &, const String &) override
    {
        return false;
    }

    bool wakeup(const StorageID &, const String &) override
    {
        return false;
    }
};

class MockTargetServerCalculater : public ITargetServerCalculator
{
public:
    MockTargetServerCalculater() = default;
    CnchServerClientPtr getTargetServer(const StorageID &, UInt64) const override
    {
        HostWithPorts hp{"169.128.0.2", 1223, 1224, 1225};
        return std::make_shared<CnchServerClient>(hp);
    }

    static constexpr auto TARGET_SERVER{"169.128.0.2:1223"};
};

const UUID g_uuid1 = UUID{UInt128{0, 1}};
const StorageID g_storage_id1{"db1", "tb1", g_uuid1};
const std::unordered_map<UUID, String> global_target_host_map {
        {g_uuid1, MockTargetServerCalculater::TARGET_SERVER}
};

constexpr auto SERVER1 = "169.128.0.1:1223";

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
        return {};
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

TEST(backgroundjob, test_persistent_status)
{
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const StablePersistentStoreProxy * proxy = dynamic_cast<const StablePersistentStoreProxy *>(&daemon_job.getStatusPersistentStore());

    {
        BackgroundJob bg_info{g_storage_id1, daemon_job};
        EXPECT_EQ(proxy->stored_statuses.at(g_uuid1), CnchBGThreadStatus::Running);
        bg_info.stop(false, true);
        EXPECT_EQ(proxy->stored_statuses.at(g_uuid1), CnchBGThreadStatus::Stopped);
    }

    {
        BackgroundJob bg_info{g_storage_id1, daemon_job};
        EXPECT_EQ(proxy->stored_statuses.at(g_uuid1), CnchBGThreadStatus::Stopped);
        EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Stopped);
        EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Stopped);
    }

    {
        BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Running, daemon_job, "127.0.0.1:9010"};
        EXPECT_EQ(proxy->stored_statuses.at(g_uuid1), CnchBGThreadStatus::Stopped);
        EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
        EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Stopped);
    }

    {
        const UUID uuid = UUID{UInt128{0, 2}};
        const StorageID storage_id{"db2", "tb2", uuid};
        BackgroundJob bg_info{storage_id, CnchBGThreadStatus::Stopped, daemon_job, "127.0.0.1:9010"};
        EXPECT_EQ(proxy->stored_statuses.at(uuid), CnchBGThreadStatus::Stopped);
        bg_info.start(false);
        EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
        EXPECT_EQ(proxy->stored_statuses.at(uuid), CnchBGThreadStatus::Stopped);
    }
}

TEST(backgroundjob, test_BackgroundJob_withStableDaemonJob)
{
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());

    const StablePersistentStoreProxy * proxy = dynamic_cast<const StablePersistentStoreProxy *>(&daemon_job.getStatusPersistentStore());
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());

    BackgroundJob bg_info{g_storage_id1, daemon_job};
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Stopped);
    EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(proxy->stored_statuses.at(g_uuid1), CnchBGThreadStatus::Running);
    EXPECT_TRUE(executor->events.empty());
    BGJobInfo expected_bg_job_data{g_storage_id1, CnchBGThreadStatus::Stopped, CnchBGThreadStatus::Running, "", 0};
    EXPECT_EQ(expected_bg_job_data, bg_info.getBGJobInfo());

    Result ret = bg_info.start(false);
    {
        EXPECT_EQ(ret.res, true);
        Event expected{g_uuid1, CnchBGThreadAction::Start, MockTargetServerCalculater::TARGET_SERVER};
        EXPECT_EQ(executor->events.at(0), expected);
        EXPECT_EQ(proxy->stored_statuses.at(g_uuid1), CnchBGThreadStatus::Running);
        EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
        EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Running);
        EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);
        EXPECT_EQ(bg_info.getStorageID(), g_storage_id1);
    }
    time_t last_start_time1 = bg_info.getBGJobInfo().last_start_time;
    EXPECT_NE(last_start_time1, 0);

    ret = bg_info.start(false);
    {
        EXPECT_EQ(ret.res, true);
        Event expected{g_uuid1, CnchBGThreadAction::Start, MockTargetServerCalculater::TARGET_SERVER};
        EXPECT_EQ(executor->events.at(1), expected);
        EXPECT_EQ(proxy->stored_statuses.at(g_uuid1), CnchBGThreadStatus::Running);
        EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
        EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Running);
        EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);
        EXPECT_EQ(bg_info.getStorageID(), g_storage_id1);
    }
    time_t last_start_time2 = bg_info.getBGJobInfo().last_start_time;

    ret = bg_info.wakeup();
    {
        EXPECT_EQ(ret.res, true);
        Event expected{g_uuid1, CnchBGThreadAction::Wakeup, MockTargetServerCalculater::TARGET_SERVER};
        EXPECT_EQ(executor->events.at(2), expected);
        EXPECT_EQ(proxy->stored_statuses.at(g_uuid1), CnchBGThreadStatus::Running);
        EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
        EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Running);
        EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);
        EXPECT_EQ(bg_info.getStorageID(), g_storage_id1);
        EXPECT_EQ(last_start_time2, bg_info.getBGJobInfo().last_start_time);
    }

    ret = bg_info.stop(false, false);
    {
        Event expected{g_uuid1, CnchBGThreadAction::Stop, MockTargetServerCalculater::TARGET_SERVER};
        EXPECT_EQ(executor->events.at(3), expected);
        EXPECT_EQ(ret.res, true);
        EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Stopped);
        EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Stopped);
        EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);
        EXPECT_EQ(last_start_time2, bg_info.getBGJobInfo().last_start_time);
        EXPECT_EQ(proxy->stored_statuses.at(g_uuid1), CnchBGThreadStatus::Running);
    }

    std::this_thread::sleep_for (std::chrono::seconds(2));

    ret = bg_info.start(false);
    {
        Event expected{g_uuid1, CnchBGThreadAction::Start, MockTargetServerCalculater::TARGET_SERVER};
        EXPECT_EQ(executor->events.at(4), expected);
        EXPECT_EQ(ret.res, true);
        EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
        EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Running);
        EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);
        EXPECT_EQ(proxy->stored_statuses.at(g_uuid1), CnchBGThreadStatus::Running);
    }
    time_t last_start_time3 = bg_info.getBGJobInfo().last_start_time;
    EXPECT_EQ(last_start_time3 > last_start_time2, true);

    ret = bg_info.remove(CnchBGThreadAction::Remove, false);
    {
        Event expected{g_uuid1, CnchBGThreadAction::Remove, MockTargetServerCalculater::TARGET_SERVER};
        EXPECT_EQ(executor->events.at(5), expected);
        EXPECT_EQ(ret.res, true);
        EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Removed);
        EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Removed);
        EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);
        EXPECT_EQ(last_start_time3, bg_info.getBGJobInfo().last_start_time);
        EXPECT_EQ(proxy->stored_statuses.at(g_uuid1), CnchBGThreadStatus::Running);
    }

    ret = bg_info.start(false);
    {
        EXPECT_EQ(ret.res, true);
        Event expected{g_uuid1, CnchBGThreadAction::Start, MockTargetServerCalculater::TARGET_SERVER};
        EXPECT_EQ(executor->events.at(6), expected);
    }

    ret = bg_info.remove(CnchBGThreadAction::Drop, false);
    {
        Event expected{g_uuid1, CnchBGThreadAction::Drop, MockTargetServerCalculater::TARGET_SERVER};
        EXPECT_EQ(executor->events.at(7), expected);
        EXPECT_EQ(ret.res, true);
        EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Removed);
        EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Removed);
        EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);
    }
}

TEST(backgroundjob, test_BackgroundJob_withUnstableDaemonJob)
{
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<UnstableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());

    BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Running, daemon_job, MockTargetServerCalculater::TARGET_SERVER};

    EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Running);
    Result ret = bg_info.stop(false, false);
    EXPECT_EQ(ret.res, false);
    EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);

    ret = bg_info.start(false);
    EXPECT_EQ(ret.res, false);
    EXPECT_EQ(ret.error_str.empty(), false);
    EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);

    ret = bg_info.remove(CnchBGThreadAction::Remove, false);
    EXPECT_EQ(ret.res, false);
    EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);

    ret = bg_info.remove(CnchBGThreadAction::Drop, false);
    EXPECT_EQ(ret.res, false);
    EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);
}

TEST(backgroundjob, test_BackgroundJob_sync_case0)
{
    // server restart; target host change; running job
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());

    std::vector<String> restarted_servers{SERVER1};
    std::vector<String> alive_servers{SERVER1, MockTargetServerCalculater::TARGET_SERVER};
    ServerInfo server_info {restarted_servers, alive_servers, global_target_host_map};
    BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Running, daemon_job, SERVER1};

    auto action = bg_info.getSyncAction(server_info);
    EXPECT_EQ(action.has_value(), true);
    EXPECT_EQ(action->need_remove, false);
    EXPECT_EQ(action->need_start, true);
    EXPECT_EQ(action->need_stop, false);
    EXPECT_EQ(action->clear_host_port, true);

    bool ret = bg_info.sync(server_info);
    EXPECT_EQ(ret, true);
    EXPECT_EQ(executor->events.size(), 1);

    Event expected_event{g_uuid1, CnchBGThreadAction::Start, MockTargetServerCalculater::TARGET_SERVER};
    EXPECT_EQ(executor->events.at(0), expected_event);
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);
}

TEST(backgroundjob, test_BackgroundJob_sync_case1)
{
    // target host change; running job
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());

    std::vector<String> restarted_servers{};
    std::vector<String> alive_servers{SERVER1, MockTargetServerCalculater::TARGET_SERVER};
    ServerInfo server_info {restarted_servers, alive_servers, global_target_host_map};
    BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Running, daemon_job, SERVER1};

    //test getSyncAction
    auto action = bg_info.getSyncAction(server_info);
    EXPECT_EQ(action.has_value(), true);
    EXPECT_EQ(action->need_remove, true);
    EXPECT_EQ(action->need_start, true);
    EXPECT_EQ(action->need_stop, false);
    EXPECT_EQ(action->clear_host_port, false);

    bool ret = bg_info.sync(server_info);
    EXPECT_EQ(ret, true);
    EXPECT_EQ(executor->events.size(), 2);
    Event expected_event0{g_uuid1, CnchBGThreadAction::Remove, SERVER1};
    EXPECT_EQ(executor->events.at(0), expected_event0);
    Event expected_event1{g_uuid1, CnchBGThreadAction::Start, MockTargetServerCalculater::TARGET_SERVER};
    EXPECT_EQ(executor->events.at(1), expected_event1);
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);
}

TEST(backgroundjob, test_BackgroundJob_sync_case2)
{
    // server die, target host change
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());

    std::vector<String> restarted_servers{};
    std::vector<String> alive_servers{MockTargetServerCalculater::TARGET_SERVER};
    ServerInfo server_info {restarted_servers, alive_servers, global_target_host_map};
    BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Running, daemon_job, SERVER1};

    //test getSyncAction
    auto action = bg_info.getSyncAction(server_info);
    EXPECT_EQ(action.has_value(), true);
    EXPECT_EQ(action->need_remove, false);
    EXPECT_EQ(action->need_start, true);
    EXPECT_EQ(action->need_stop, false);
    EXPECT_EQ(action->clear_host_port, true);

    bool ret = bg_info.sync(server_info);
    EXPECT_EQ(ret, true);
    EXPECT_EQ(executor->events.size(), 1);
    Event expected_event{g_uuid1, CnchBGThreadAction::Start, MockTargetServerCalculater::TARGET_SERVER};
    EXPECT_EQ(executor->events.at(0), expected_event);
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);
}

TEST(backgroundjob, test_BackgroundJob_sync_case3)
{
    // nothing change, running job
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());

    std::vector<String> restarted_servers{};
    std::vector<String> alive_servers{MockTargetServerCalculater::TARGET_SERVER};
    ServerInfo server_info {restarted_servers, alive_servers, global_target_host_map};
    BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Running, daemon_job, MockTargetServerCalculater::TARGET_SERVER};

    auto action = bg_info.getSyncAction(server_info);
    EXPECT_EQ(action.has_value(), true);
    EXPECT_EQ(action->need_remove, false);
    EXPECT_EQ(action->need_start, false);
    EXPECT_EQ(action->need_stop, false);
    EXPECT_EQ(action->clear_host_port, false);

    bool ret = bg_info.sync(server_info);
    EXPECT_EQ(ret, true);
    EXPECT_TRUE(executor->events.empty());
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);
}

TEST(backgroundjob, test_BackgroundJob_sync_case4)
{
    // stopped job, server die, target host change
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    std::vector<String> restarted_servers{};
    std::vector<String> alive_servers{MockTargetServerCalculater::TARGET_SERVER};
    ServerInfo server_info {restarted_servers, alive_servers, global_target_host_map};
    BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Stopped, daemon_job, SERVER1};

    auto action = bg_info.getSyncAction(server_info);
    EXPECT_EQ(action.has_value(), true);
    EXPECT_EQ(action->need_remove, false);
    EXPECT_EQ(action->need_start, false);
    EXPECT_EQ(action->need_stop, false);
    EXPECT_EQ(action->clear_host_port, true);

    bool ret = bg_info.sync(server_info);
    EXPECT_EQ(ret, true);
    EXPECT_TRUE(executor->events.empty());
    EXPECT_EQ(bg_info.getHostPort().empty(), true);
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Stopped);
}

TEST(backgroundjob, test_BackgroundJob_sync_case5)
{
    // removed bg job, server die, target host change
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    std::vector<String> restarted_servers{};
    std::vector<String> alive_servers{MockTargetServerCalculater::TARGET_SERVER};
    ServerInfo server_info {restarted_servers, alive_servers, global_target_host_map};
    BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Removed, daemon_job, SERVER1};

    auto action = bg_info.getSyncAction(server_info);
    EXPECT_EQ(action.has_value(), true);
    EXPECT_EQ(action->need_remove, false);
    EXPECT_EQ(action->need_start, false);
    EXPECT_EQ(action->need_stop, false);
    EXPECT_EQ(action->clear_host_port, true);

    bool ret = bg_info.sync(server_info);
    EXPECT_EQ(ret, true);
    EXPECT_TRUE(executor->events.empty());
    EXPECT_EQ(bg_info.getHostPort().empty(), true);
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Removed);
}

TEST(backgroundjob, test_BackgroundJob_sync_case6)
{
    // nothing change, remove job, set expected status to Running
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());

    std::vector<String> restarted_servers{};
    std::vector<String> alive_servers{MockTargetServerCalculater::TARGET_SERVER};
    ServerInfo server_info {restarted_servers, alive_servers, global_target_host_map};
    /// firstly set status to Running then call remove
    BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Running, daemon_job, MockTargetServerCalculater::TARGET_SERVER};
    bg_info.remove(CnchBGThreadAction::Remove, false);
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Removed);
    bg_info.setExpectedStatus(CnchBGThreadStatus::Running);
    //test getSyncAction
    auto action = bg_info.getSyncAction(server_info);
    EXPECT_EQ(action.has_value(), true);
    EXPECT_EQ(action->need_remove, false);
    EXPECT_EQ(action->need_start, true);
    EXPECT_EQ(action->need_stop, false);
    EXPECT_EQ(action->clear_host_port, false);

    bool ret = bg_info.sync(server_info);
    EXPECT_EQ(ret, true);
    EXPECT_EQ(executor->events.size(), 2);
    Event expected_event{g_uuid1, CnchBGThreadAction::Start, MockTargetServerCalculater::TARGET_SERVER};
    EXPECT_EQ(executor->events.at(1), expected_event);
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);
}

TEST(backgroundjob, test_BackgroundJob_sync_case13)
{
    /// target host not found
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    std::vector<String> restarted_servers;
    std::vector<String> alive_servers{SERVER1, MockTargetServerCalculater::TARGET_SERVER};
    ServerInfo server_info {restarted_servers, alive_servers, global_target_host_map};

    const StorageID no_target_host_storage_id{"db2", "tb2", UUID{UInt128{0, 2}}};
    BackgroundJob bg_info{no_target_host_storage_id, CnchBGThreadStatus::Running, daemon_job, SERVER1};

    auto action = bg_info.getSyncAction(server_info);
    EXPECT_EQ(action.has_value(), true);
    EXPECT_EQ(action->need_remove, false);
    EXPECT_EQ(action->need_start, false);
    EXPECT_EQ(action->need_stop, false);
    EXPECT_EQ(action->clear_host_port, false);

    bool ret = bg_info.sync(server_info);
    EXPECT_EQ(ret, true);
    EXPECT_TRUE(executor->events.empty());
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(bg_info.getHostPort(), SERVER1);
}

TEST(backgroundjob, test_BackgroundJob_sync_case14)
{
    /// target host not found, server restart
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    std::vector<String> restarted_servers{SERVER1};
    std::vector<String> alive_servers{SERVER1, MockTargetServerCalculater::TARGET_SERVER};
    ServerInfo server_info {restarted_servers, alive_servers, global_target_host_map};

    const UUID no_target_host_uuid{UInt128{0, 2}};
    const StorageID no_target_host_storage_id{"db2", "tb2", no_target_host_uuid};
    BackgroundJob bg_info{no_target_host_storage_id, CnchBGThreadStatus::Running, daemon_job, SERVER1};

    auto action = bg_info.getSyncAction(server_info);
    EXPECT_EQ(action.has_value(), true);
    EXPECT_EQ(action->need_remove, false);
    EXPECT_EQ(action->need_start, true);
    EXPECT_EQ(action->need_stop, false);
    EXPECT_EQ(action->clear_host_port, true);
    bool ret = bg_info.sync(server_info);
    EXPECT_EQ(ret, true);
    EXPECT_EQ(executor->events.size(), 1);
    Event expected_event{no_target_host_uuid, CnchBGThreadAction::Start, MockTargetServerCalculater::TARGET_SERVER};
    EXPECT_EQ(executor->events.at(0), expected_event);
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);
}

TEST(backgroundjob, test_BackgroundJob_sync_case15)
{
    /// target host not found, server die
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());

    std::vector<String> restarted_servers;
    std::vector<String> alive_servers{MockTargetServerCalculater::TARGET_SERVER};
    ServerInfo server_info {restarted_servers, alive_servers, global_target_host_map};

    const UUID no_target_host_uuid{UInt128{0, 2}};
    const StorageID no_target_host_storage_id{"db2", "tb2", no_target_host_uuid};
    BackgroundJob bg_info{no_target_host_storage_id, CnchBGThreadStatus::Running, daemon_job, SERVER1};
    auto action = bg_info.getSyncAction(server_info);
    EXPECT_EQ(action.has_value(), true);
    EXPECT_EQ(action->need_remove, false);
    EXPECT_EQ(action->need_start, true);
    EXPECT_EQ(action->need_stop, false);
    EXPECT_EQ(action->clear_host_port, true);
    bool ret = bg_info.sync(server_info);
    EXPECT_EQ(ret, true);
    EXPECT_EQ(executor->events.size(), 1);
    Event expected_event{no_target_host_uuid, CnchBGThreadAction::Start, MockTargetServerCalculater::TARGET_SERVER};
    EXPECT_EQ(executor->events.at(0), expected_event);
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Running);
    EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);
}

TEST(backgroundjob, test_BackgroundJob_sync_case16)
{
    // stopped job, server die, host_port_empty
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    std::vector<String> restarted_servers{};
    std::vector<String> alive_servers{MockTargetServerCalculater::TARGET_SERVER};
    ServerInfo server_info {restarted_servers, alive_servers, global_target_host_map};
    BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Stopped, daemon_job, ""};

    //test getSyncAction
    auto action = bg_info.getSyncAction(server_info);
    EXPECT_EQ(action.has_value(), true);
    EXPECT_EQ(action->need_remove, false);
    EXPECT_EQ(action->need_start, false);
    EXPECT_EQ(action->need_stop, false);
    EXPECT_EQ(action->clear_host_port, false);

    bool ret = bg_info.sync(server_info);
    EXPECT_EQ(ret, true);
    EXPECT_TRUE(executor->events.empty());
    EXPECT_EQ(bg_info.getHostPort().empty(), true);
    EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Stopped);
}

TEST(backgroundjob, test_BackgroundJob_sync_with_persistent_status_case0)
{
    // server restart; target host change; running job, persistent status is stop
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    StablePersistentStoreProxy * proxy = dynamic_cast<StablePersistentStoreProxy *>(&daemon_job.getStatusPersistentStore());
    std::map<UUID, CnchBGThreadStatus> & statuses_persistent_store =
        proxy->stored_statuses;

    std::vector<String> restarted_servers{SERVER1};
    std::vector<String> alive_servers{SERVER1, MockTargetServerCalculater::TARGET_SERVER};
    ServerInfo server_info {restarted_servers, alive_servers, global_target_host_map};
    EXPECT_TRUE(statuses_persistent_store.empty());

    {
        BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Running, daemon_job, SERVER1};
        EXPECT_EQ(statuses_persistent_store.at(g_uuid1), CnchBGThreadStatus::Running);
        statuses_persistent_store[g_uuid1] = CnchBGThreadStatus::Stopped;
        bool ret = bg_info.sync(server_info);
        EXPECT_EQ(ret, true);
        EXPECT_TRUE(executor->events.empty());
        EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Stopped);
        EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Stopped);
        EXPECT_TRUE(bg_info.getHostPort().empty());
    }

    statuses_persistent_store.clear();

    {
        //test getSyncAction
        BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Running, daemon_job, SERVER1};
        EXPECT_EQ(statuses_persistent_store.at(g_uuid1), CnchBGThreadStatus::Running);
        bg_info.setExpectedStatus(CnchBGThreadStatus::Stopped);
        auto action = bg_info.getSyncAction(server_info);
        EXPECT_EQ(action.has_value(), true);
        EXPECT_EQ(action->need_remove, false);
        EXPECT_EQ(action->need_start, false);
        EXPECT_EQ(action->need_stop, true);
        EXPECT_EQ(action->clear_host_port, true);
    }
}

TEST(backgroundjob, test_BackgroundJob_sync_with_persistent_status_case1)
{
    // target host change; running job, persistent status is stop
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    StablePersistentStoreProxy * proxy = dynamic_cast<StablePersistentStoreProxy *>(&daemon_job.getStatusPersistentStore());
    std::map<UUID, CnchBGThreadStatus> & statuses_persistent_store =
        proxy->stored_statuses;
    std::vector<String> restarted_servers{};
    std::vector<String> alive_servers{SERVER1, MockTargetServerCalculater::TARGET_SERVER};
    ServerInfo server_info {restarted_servers, alive_servers, global_target_host_map};
    EXPECT_TRUE(statuses_persistent_store.empty());

    {
        BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Running, daemon_job, SERVER1};
        EXPECT_EQ(statuses_persistent_store.at(g_uuid1), CnchBGThreadStatus::Running);
        statuses_persistent_store[g_uuid1] = CnchBGThreadStatus::Stopped;
        bool ret = bg_info.sync(server_info);
        EXPECT_EQ(ret, true);
        EXPECT_EQ(executor->events.size(), 1);
        Event expected{g_uuid1, CnchBGThreadAction::Stop, SERVER1};
        EXPECT_EQ(executor->events.at(0), expected);
        EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Stopped);
        EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Stopped);
        EXPECT_EQ(bg_info.getHostPort(), SERVER1);
    }

    statuses_persistent_store.clear();

    {
        //test getSyncAction
        BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Running, daemon_job, SERVER1};
        EXPECT_EQ(statuses_persistent_store.at(g_uuid1), CnchBGThreadStatus::Running);
        bg_info.setExpectedStatus(CnchBGThreadStatus::Stopped);
        auto action = bg_info.getSyncAction(server_info);
        EXPECT_EQ(action.has_value(), true);
        EXPECT_EQ(action->need_remove, false);
        EXPECT_EQ(action->need_start, false);
        EXPECT_EQ(action->need_stop, true);
        EXPECT_EQ(action->clear_host_port, false);
    }
}

TEST(backgroundjob, test_BackgroundJob_sync_with_persistent_status_case2)
{
    // server die; running job, persistent status is stop
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    StablePersistentStoreProxy * proxy = dynamic_cast<StablePersistentStoreProxy *>(&daemon_job.getStatusPersistentStore());
    std::map<UUID, CnchBGThreadStatus> & statuses_persistent_store =
        proxy->stored_statuses;
    std::vector<String> restarted_servers{};
    std::vector<String> alive_servers{SERVER1};
    ServerInfo server_info {restarted_servers, alive_servers, global_target_host_map};
    EXPECT_TRUE(statuses_persistent_store.empty());

    {
        BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Running, daemon_job, MockTargetServerCalculater::TARGET_SERVER};
        EXPECT_EQ(statuses_persistent_store.at(g_uuid1), CnchBGThreadStatus::Running);
        statuses_persistent_store[g_uuid1] = CnchBGThreadStatus::Stopped;
        bool ret = bg_info.sync(server_info);
        EXPECT_EQ(ret, true);
        EXPECT_TRUE(executor->events.empty());
        EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Stopped);
        EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Stopped);
        EXPECT_TRUE(bg_info.getHostPort().empty());
    }

    statuses_persistent_store.clear();

    {
        //test getSyncAction
        BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Running, daemon_job, MockTargetServerCalculater::TARGET_SERVER};
        EXPECT_EQ(statuses_persistent_store.at(g_uuid1), CnchBGThreadStatus::Running);
        bg_info.setExpectedStatus(CnchBGThreadStatus::Stopped);
        auto action = bg_info.getSyncAction(server_info);
        EXPECT_EQ(action.has_value(), true);
        EXPECT_EQ(action->need_remove, false);
        EXPECT_EQ(action->need_start, false);
        EXPECT_EQ(action->need_stop, true);
        EXPECT_EQ(action->clear_host_port, true);
    }
}

TEST(backgroundjob, test_BackgroundJob_sync_with_persistent_status_case3)
{
    // running job, persistent status is stop
    DaemonJobServerBGThread daemon_job(getContext().context, CnchBGThreadType::MergeMutate, std::make_unique<StableExecutor>(), std::make_unique<StablePersistentStoreProxy>(), std::make_unique<MockTargetServerCalculater>());
    const StableExecutor * executor = dynamic_cast<const StableExecutor * >(&daemon_job.getBgJobExecutor());
    StablePersistentStoreProxy * proxy = dynamic_cast<StablePersistentStoreProxy *>(&daemon_job.getStatusPersistentStore());
    std::map<UUID, CnchBGThreadStatus> & statuses_persistent_store =
        proxy->stored_statuses;
    std::vector<String> restarted_servers{};
    std::vector<String> alive_servers{SERVER1, MockTargetServerCalculater::TARGET_SERVER};
    ServerInfo server_info {restarted_servers, alive_servers, global_target_host_map};
    EXPECT_TRUE(statuses_persistent_store.empty());

    {
        BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Stopped, daemon_job, MockTargetServerCalculater::TARGET_SERVER};
    }

    {
        BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Running, daemon_job, MockTargetServerCalculater::TARGET_SERVER};
        bool ret = bg_info.sync(server_info);
        EXPECT_EQ(ret, true);
        EXPECT_EQ(executor->events.size(), 1);
        Event expected{g_uuid1, CnchBGThreadAction::Stop, MockTargetServerCalculater::TARGET_SERVER};
        EXPECT_EQ(executor->events.at(0), expected);
        EXPECT_EQ(bg_info.getJobStatus(), CnchBGThreadStatus::Stopped);
        EXPECT_EQ(bg_info.getJobExpectedStatus(), CnchBGThreadStatus::Stopped);
        EXPECT_EQ(bg_info.getHostPort(), MockTargetServerCalculater::TARGET_SERVER);
    }

    statuses_persistent_store.clear();

    {
        //test getSyncAction
        BackgroundJob bg_info{g_storage_id1, CnchBGThreadStatus::Running, daemon_job, MockTargetServerCalculater::TARGET_SERVER};
        EXPECT_EQ(statuses_persistent_store.at(g_uuid1), CnchBGThreadStatus::Running);
        bg_info.setExpectedStatus(CnchBGThreadStatus::Stopped);
        auto action = bg_info.getSyncAction(server_info);
        EXPECT_EQ(action.has_value(), true);
        EXPECT_EQ(action->need_remove, false);
        EXPECT_EQ(action->need_start, false);
        EXPECT_EQ(action->need_stop, true);
        EXPECT_EQ(action->clear_host_port, false);
    }
}

} // end namespace

