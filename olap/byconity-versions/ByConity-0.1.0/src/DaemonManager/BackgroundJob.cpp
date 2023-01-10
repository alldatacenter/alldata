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
#include <DaemonManager/BackgroundJob.h>
#include <Common/Exception.h>
#include <Protos/RPCHelpers.h>
#include <Common/RpcClientPool.h>
#include <common/logger_useful.h>

namespace DB::DaemonManager
{

BackgroundJob::BackgroundJob(StorageID storage_id_, DaemonJobServerBGThread & daemon_job_)
    : storage_id{std::move(storage_id_)}, daemon_job{daemon_job_}, status{CnchBGThreadStatus::Stopped}, expected_status{CnchBGThreadStatus::Running}, host_port{}, log{daemon_job_.getLog()}
{
    std::optional<CnchBGThreadStatus> stored_status = daemon_job.getStatusPersistentStore().createStatusIfNotExist(storage_id, CnchBGThreadStatus::Running);
    if (stored_status)
    {
        status = *stored_status;
        expected_status = *stored_status;
    }
}

BackgroundJob::BackgroundJob(StorageID storage_id_, CnchBGThreadStatus status_, DaemonJobServerBGThread & daemon_job_, String host_port_)
        : storage_id{std::move(storage_id_)}, daemon_job{daemon_job_}, status{status_}, expected_status{status_}, host_port{std::move(host_port_)}, log{daemon_job_.getLog()}
{
    std::optional<CnchBGThreadStatus> stored_status = daemon_job.getStatusPersistentStore().createStatusIfNotExist(storage_id, status);
    if (stored_status)
        expected_status = *stored_status;
}

Result BackgroundJob::start(bool write_status_to_persisent_store)
{
    String exception_str{"action failed"};
    // start() can be called even when the status is Running
    CnchServerClientPtr cnch_server = nullptr;
    try
    {
        if (write_status_to_persisent_store)
            daemon_job.getStatusPersistentStore().setStatus(storage_id.uuid, CnchBGThreadStatus::Running);
        cnch_server = daemon_job.getTargetServer(storage_id, 0);
    }
    catch (const Exception & e)
    {
        LOG_WARNING(log, " Got exception {}.{} when getTargetServer for {}",
            e.code(), e.displayText(), storage_id.getNameForLogs());
    }
    catch (...)
    {
        exception_str = getCurrentExceptionMessage(true);
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }

    if (!cnch_server)
    {
        String error_str = "Could not get a target server to start job for "
            + storage_id.getNameForLogs();
        if (!exception_str.empty())
            error_str += ", got exception: " + exception_str;

        LOG_WARNING(log, error_str);
        return {error_str, false};
    }

    const String new_host_port = cnch_server->getRPCAddress();
    LOG_DEBUG(
        log,
        "Will execute job: {} on server: {}",
        storage_id.getNameForLogs(), new_host_port);

    String old_host_port;
    CnchBGThreadStatus old_status;
    CnchBGThreadStatus old_expected_status;
    {
        std::lock_guard<std::mutex> lock_guard(mutex);
        old_host_port = host_port;
        old_status = status;
        old_expected_status = expected_status;
        status = CnchBGThreadStatus::Running;
        expected_status = CnchBGThreadStatus::Running;
        host_port = new_host_port;
    }

    bool ret = false;
    try
    {
        ret = daemon_job.getBgJobExecutor().start(getBGJobInfo());
    }
    catch (...)
    {
        exception_str = getCurrentExceptionMessage(true);
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }

    if (!ret)
    {
        LOG_WARNING(
            log,
            "Failed to launch backgroud job: {} at server {}",
            storage_id.getNameForLogs(), new_host_port);

        std::lock_guard<std::mutex> lock_guard(mutex);
        host_port = old_host_port;
        status = old_status;
        expected_status = old_expected_status;
    }
    else
    {
        std::lock_guard<std::mutex> lock_guard(mutex);
        last_start_time = std::time(nullptr);
    }
    return {exception_str, ret};
}

Result BackgroundJob::stop(bool force, bool write_status_to_persisent_store)
{
    String exception_str{"action failed"};
    if (write_status_to_persisent_store)
    {
        try
        {
            daemon_job.getStatusPersistentStore().setStatus(storage_id.uuid, CnchBGThreadStatus::Stopped);
        }
        catch (...)
        {
            exception_str = getCurrentExceptionMessage(true);
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
            return {exception_str, false};
        }
    }

    CnchBGThreadStatus old_status;
    CnchBGThreadStatus old_expected_status;
    String host_port_copy;
    {
        std::lock_guard<std::mutex> lock_guard(mutex);
        old_status = status;
        old_expected_status = expected_status;
        status = CnchBGThreadStatus::Stopped;
        expected_status = CnchBGThreadStatus::Stopped;
        host_port_copy = host_port;
    }

    if ((old_status == CnchBGThreadStatus::Stopped) && (force == false))
    {
        LOG_WARNING(
            log,
            "Do nothing because backgroud job: {} at server {} already stopped",
            storage_id.getNameForLogs(), host_port);

        return {"", true};
    }

    if (host_port_copy.empty())
    {
        LOG_DEBUG(
            log,
            "Successful Stop without rpc call for background job: {} because host_port is empty",
            storage_id.getNameForLogs());
        return {"", true};
    }

    bool ret = false;
    try
    {
        ret = daemon_job.getBgJobExecutor().stop(getBGJobInfo());
    }
    catch (...)
    {
        exception_str = getCurrentExceptionMessage(true);
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }

    if (!ret)
    {
        LOG_WARNING(
            log,
            "Failed to stop backgroud job: {} at server {}",
            storage_id.getNameForLogs(), host_port_copy
        );
        std::lock_guard<std::mutex> lock_guard(mutex);
        status = old_status;
        expected_status = old_expected_status;
    }
    return {exception_str, ret};
}

Result BackgroundJob::remove(CnchBGThreadAction remove_type, bool write_status_to_persisent_store)
{
    if ((remove_type != CnchBGThreadAction::Remove) &&
        (remove_type != CnchBGThreadAction::Drop))
        throw Exception("remove type is not remove or drop, this is a coding mistake", ErrorCodes::LOGICAL_ERROR);

    String exception_str{"action failed"};

    if (write_status_to_persisent_store)
    {
        try
        {
            daemon_job.getStatusPersistentStore().setStatus(storage_id.uuid, CnchBGThreadStatus::Removed);
        }
        catch (...)
        {
            exception_str = getCurrentExceptionMessage(true);
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
            return {exception_str, false};
        }
    }

    CnchBGThreadStatus old_status;
    CnchBGThreadStatus old_expected_status;
    String host_port_copy;
    {
        std::lock_guard<std::mutex> lock_guard(mutex);
        old_status = status;
        old_expected_status = expected_status;
        status = CnchBGThreadStatus::Removed;
        expected_status = CnchBGThreadStatus::Removed;
        host_port_copy = host_port;
    }

    /// TODO: handle the case restart server
    if (host_port_copy.empty())
    {
        LOG_DEBUG(
            log,
            "Successful {} without rpc call for: {} because host_port is empty",
            toString(remove_type), storage_id.getNameForLogs());
        return {"", true};
    }

    if (old_status == CnchBGThreadStatus::Removed)
    {
        LOG_WARNING(
            log,
            "Successful {} remove because backgroud job: {} at server {} already removed",
            toString(remove_type), storage_id.getNameForLogs(), host_port);
        return {"", true};
    }

    bool ret = false;
    try
    {
        if (remove_type == CnchBGThreadAction::Remove)
            ret = daemon_job.getBgJobExecutor().remove(getBGJobInfo());
        else
            ret = daemon_job.getBgJobExecutor().drop(getBGJobInfo());
    }
    catch (...)
    {
        exception_str = getCurrentExceptionMessage(true);
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }

    if (!ret)
    {
        LOG_WARNING(
            log,
            "Failed to {} backgroud job: {} at server {}",
            toString(remove_type), storage_id.getNameForLogs(), host_port_copy);

        std::lock_guard<std::mutex> lock_guard(mutex);
        status = old_status;
        expected_status = old_expected_status;
    }
    return {exception_str, ret};
}

Result BackgroundJob::wakeup()
{
    String exception_str{"action failed"};
    CnchBGThreadStatus curr_status;
    String host_port_copy;
    {
        std::lock_guard<std::mutex> lock_guard(mutex);
        curr_status = status;
        host_port_copy = host_port;
    }

    if (curr_status != CnchBGThreadStatus::Running)
    {
        LOG_WARNING(
            log,
            "Do nothing because backgroud job: {} at server {} not in {} status",
            storage_id.getNameForLogs(), host_port, toString(status));

        return {"BackgroundJob {} is not running" , false};
    }

    bool ret = false;
    try
    {
        ret = daemon_job.getBgJobExecutor().wakeup(getBGJobInfo());
    }
    catch (...)
    {
        exception_str = getCurrentExceptionMessage(true);
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }

    if (!ret)
    {
        LOG_WARNING(
            log,
            "Failed to wakeup backgroud job: {} at server {}",
            storage_id.getNameForLogs(), host_port_copy
        );
    }
    return {exception_str, ret};
}

std::optional<BackgroundJob::SyncAction> BackgroundJob::getSyncAction(const ServerInfo & server_info) const
{
    const std::vector<String> & restarted_servers = server_info.restarted_servers;
    const std::vector<String> & alive_servers = server_info.alive_servers;
    bool need_start = false;
    bool need_remove = false;
    bool need_stop = false;
    bool clear_host_port = false;

    {
        std::lock_guard<std::mutex> lock_guard(mutex);
        bool server_restarted = (
            (!host_port.empty()) &&
            (restarted_servers.end() !=
                std::find(restarted_servers.begin(), restarted_servers.end(), host_port))
        );
        bool server_died = (
            (!host_port.empty()) &&
            (alive_servers.end() == std::find(alive_servers.begin(), alive_servers.end(), host_port))
        );

        if (status != CnchBGThreadStatus::Running)
        {
            if (server_restarted || server_died)
                clear_host_port = true;

            if (status == expected_status)
                return BackgroundJob::SyncAction{false, false, false, clear_host_port};
            else
                return BackgroundJob::SyncAction{
                    (expected_status == CnchBGThreadStatus::Running),
                    (expected_status == CnchBGThreadStatus::Removed),
                    false,
                    clear_host_port
                };
        }

        need_stop = (expected_status == CnchBGThreadStatus::Stopped);
        if (server_restarted || server_died)
        {
            clear_host_port = true;
            if (!need_stop)
            {
                need_start = true;
                need_remove = false;
            }
            else
            {
                need_start = false;
                need_remove = false;
            }
        }

        if (!need_stop)
        {
            auto it = server_info.target_host_map.find(storage_id.uuid);
            if (
                (it != server_info.target_host_map.end()) &&
                (it->second != host_port)
               )
            {
                LOG_INFO(
                    log,
                    "Target host change for: {} , this bg jobs will be moved from {} to new target host"
                    , storage_id.getNameForLogs(), host_port);
                need_start = true;
                if (server_restarted || server_died)
                    need_remove = false;
                else
                    need_remove = true;
            }
        }
    }

    return BackgroundJob::SyncAction{need_start, need_remove, need_stop, clear_host_port};
}

bool BackgroundJob::executeSyncAction(const BackgroundJob::SyncAction & action)
{
    if (action.clear_host_port)
    {
        LOG_INFO(log, "clear host port for {}", storage_id.getNameForLogs());
        std::lock_guard<std::mutex> lock_guard(mutex);
        host_port.clear();
    }

    if (action.need_stop)
    {
        Result ret = stop(false, false);
        if (!ret.res)
            return false;
    }

    if (action.need_remove)
    {
        Result ret = remove(CnchBGThreadAction::Remove, false);
        if (!ret.res)
            return false;
    }

    if (action.need_start)
    {
        setExpectedStatus(CnchBGThreadStatus::Running);
        Result ret = start(false);
        if (!ret.res)
            return false;
    }

    return true;
}

bool BackgroundJob::sync(const ServerInfo & server_info)
{
    CnchBGThreadStatus store_status = daemon_job.getStatusPersistentStore().getStatus(storage_id.uuid, true);
    setExpectedStatus(store_status);

    std::optional<BackgroundJob::SyncAction> action = getSyncAction(server_info);
    if (!action)
        return false;
    return executeSyncAction(action.value());
}

void BackgroundJob::setExpectedStatus(CnchBGThreadStatus status_)
{
    std::lock_guard<std::mutex> lock_guard(mutex);
    expected_status = status_;
}

}
