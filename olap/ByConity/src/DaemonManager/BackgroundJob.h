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

#include <CloudServices/CnchBGThreadCommon.h>
#include <Interpreters/StorageID.h>
#include <Poco/Logger.h>
#include <vector>
#include <string>
#include <unordered_map>
#include <mutex>

namespace DB::DaemonManager
{

class DaemonJobServerBGThread;

struct Result
{
    String error_str;
    bool res;
};

struct ServerInfo
{
    std::vector<String> restarted_servers;
    std::vector<String> alive_servers;
    std::unordered_map<UUID, String> target_host_map;
};

class BackgroundJob;
using BackgroundJobPtr = std::shared_ptr<BackgroundJob>;
using BackgroundJobs = std::unordered_map<UUID, BackgroundJobPtr>;

struct BGJobInfo
{
    const StorageID storage_id;
    const CnchBGThreadStatus status;
    const CnchBGThreadStatus expected_status;
    const String host_port;
    const std::time_t last_start_time;
};

inline bool operator == (const BGJobInfo & lhs, const BGJobInfo & rhs)
{
    return (lhs.storage_id == rhs.storage_id) &&
            (lhs.status == rhs.status) &&
            (lhs.expected_status == rhs.expected_status) &&
            (lhs.host_port == rhs.host_port) &&
            (lhs.last_start_time == rhs.last_start_time);
}

using BGJobInfos = std::vector<BGJobInfo>;

class BackgroundJob
{
/**
    Class invariants:
        - the status and host_port is always consistent with the last action. It means that if it is Started, the start RPC have been delivered to the `host_port` server. If it is Stopped, the stopped RPC have been delivered to the `host_port` server. One exception is that the host could be empty, that mean that job haven't been ever started, or the job is not running and located in server that have been restarted or died.
        - the expected_status use in sync method. If the status is different than the expected_status, an action will be executed. This is for support retry in case we want to reschedule the job to new host. We have to remove the job in old host first then start the job in new host. And we have to retry the action in case we remove the job succesfully but failed to start the new job.
        - if status is Removed. It will be destroyed later by DaemonJob.
        - never hold lock while call other function
        - use mutex whenever access: host_port, status, expected_status, last_start_time
        - The persistent job status only created at ctor and only change by user command, it will be cleaned by globalGC job
*/
public:
    BackgroundJob(StorageID storage_id_, DaemonJobServerBGThread & daemon_job_);
    /// TODO make it private
    BackgroundJob(StorageID storage_id_, CnchBGThreadStatus status_,
        DaemonJobServerBGThread & daemon_job_, String host_port);

    BackgroundJob(BackgroundJob && other) = delete;
    BackgroundJob & operator=(BackgroundJob && other) = delete;
    BackgroundJob(const BackgroundJob & other) = delete;
    BackgroundJob & operator=(const BackgroundJob & other) = delete;

    bool isRunning() const
    {
        return getJobStatus() == CnchBGThreadStatus::Running;
    }

    bool isStopped() const
    {
        return getJobStatus() == CnchBGThreadStatus::Stopped;
    }

    bool isRemoved() const
    {
        return getJobStatus() == CnchBGThreadStatus::Removed;
    }

    CnchBGThreadStatus getJobStatus() const
    {
        std::lock_guard<std::mutex> lock_guard(mutex);
        return status;
    }

    CnchBGThreadStatus getJobExpectedStatus() const
    {
        std::lock_guard<std::mutex> lock_guard(mutex);
        return expected_status;
    }

    String getHostPort() const
    {
        std::lock_guard<std::mutex> lock_guard(mutex);
        return host_port;
    }

    const StorageID & getStorageID() const
    {
        return storage_id;
    }

    const UUID & getUUID() const
    {
        return storage_id.uuid;
    }

    time_t getLastStartTime() const
    {
        std::lock_guard<std::mutex> lock_guard(mutex);
        return last_start_time;
    }

    void setExpectedStatus(CnchBGThreadStatus status);

    Result start(bool write_status_to_persisent_store);
    Result stop(bool force_send_rpc, bool write_status_to_persisent_store);
    Result remove(CnchBGThreadAction remove_type, bool write_status_to_persisent_store);
    Result wakeup();
    bool sync(const ServerInfo & server_info);

    struct SyncAction
    {
        bool need_start = false;
        bool need_remove = false;
        bool need_stop = false;
        bool clear_host_port = false;
    };
    std::optional<BackgroundJob::SyncAction> getSyncAction(const ServerInfo &) const;

    BGJobInfo getBGJobInfo() const
    {
        std::lock_guard<std::mutex> lock_guard(mutex);
        return BGJobInfo{storage_id, status, expected_status, host_port, last_start_time};
    }

private:
    bool executeSyncAction(const SyncAction &);

    const StorageID storage_id;
    DaemonJobServerBGThread & daemon_job;
    CnchBGThreadStatus status;
    CnchBGThreadStatus expected_status;
    String host_port;
    std::time_t last_start_time = 0;
    Poco::Logger * log;
    mutable std::mutex mutex;
};

}
