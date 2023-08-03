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

#include <DaemonManager/BackgroudJobExecutor.h>
#include <DaemonManager/BackgroundJob.h>
#include <CloudServices/CnchServerClient.h>

namespace DB::DaemonManager
{

namespace
{

void executeServerBGThreadAction(const StorageID & storage_id, const String & host_port, CnchBGThreadAction action, const Context & context, CnchBGThreadType type)
{
    CnchServerClientPtr server_client = context.getCnchServerClient(host_port);
    server_client->controlCnchBGThread(storage_id, type, action);
    LOG_DEBUG(&Poco::Logger::get(__func__), "Succeed to {} thread for {} on {}",
        toString(action), storage_id.getNameForLogs(), host_port);
}

} /// end anonymous namespace

bool IBackgroundJobExecutor::start(const BGJobInfo & info)
{
    return start(info.storage_id, info.host_port);
}

bool IBackgroundJobExecutor::stop(const BGJobInfo & info)
{
    return stop(info.storage_id, info.host_port);
}

bool IBackgroundJobExecutor::remove(const BGJobInfo & info)
{
    return remove(info.storage_id, info.host_port);
}

bool IBackgroundJobExecutor::drop(const BGJobInfo & info)
{
    return drop(info.storage_id, info.host_port);
}

bool IBackgroundJobExecutor::wakeup(const BGJobInfo & info)
{
    return wakeup(info.storage_id, info.host_port);
}

BackgroundJobExecutor::BackgroundJobExecutor(const Context & context_, CnchBGThreadType type_)
    : context{context_}, type{type_}
{}

bool BackgroundJobExecutor::start(const StorageID & storage_id, const String & host_port)
{
    executeServerBGThreadAction(storage_id, host_port, CnchBGThreadAction::Start, context, type);
    return true;
}

bool BackgroundJobExecutor::stop(const StorageID & storage_id, const String & host_port)
{
    executeServerBGThreadAction(storage_id, host_port, CnchBGThreadAction::Stop, context, type);
    return true;
}

bool BackgroundJobExecutor::remove(const StorageID & storage_id, const String & host_port)
{
    executeServerBGThreadAction(storage_id, host_port, CnchBGThreadAction::Remove, context, type);
    return true;
}

bool BackgroundJobExecutor::drop(const StorageID & storage_id, const String & host_port)
{
    executeServerBGThreadAction(storage_id, host_port, CnchBGThreadAction::Drop, context, type);
    return true;
}

bool BackgroundJobExecutor::wakeup(const StorageID & storage_id, const String & host_port)
{
    executeServerBGThreadAction(storage_id, host_port, CnchBGThreadAction::Wakeup, context, type);
    return true;
}

} // end namespace DB::DaemonManager
