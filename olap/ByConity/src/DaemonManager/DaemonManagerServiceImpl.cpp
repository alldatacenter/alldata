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

#include <DaemonManager/DaemonManagerServiceImpl.h>
#include <DaemonManager/DaemonHelper.h>
#include <CloudServices/CnchBGThreadCommon.h>
#include <Protos/RPCHelpers.h>
#include <brpc/closure_guard.h>
#include <brpc/controller.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int DAEMON_MANAGER_SCHEDULE_THREAD_FAILED;
    extern const int DAEMON_THREAD_HAS_STOPPED;
}

namespace DaemonManager
{

void fillDMBGJobInfo(const BGJobInfo & bg_job_data, Protos::DMBGJobInfo & pb)
{
    RPCHelpers::fillStorageID(bg_job_data.storage_id, *pb.mutable_storage_id());
    pb.set_host_port(bg_job_data.host_port);
    pb.set_status(bg_job_data.status);
    pb.set_expected_status(bg_job_data.expected_status);
    pb.set_last_start_time(bg_job_data.last_start_time);
}

void DaemonManagerServiceImpl:: GetAllBGThreadServers(
        ::google::protobuf::RpcController *,
        const ::DB::Protos::GetAllBGThreadServersReq * request,
        ::DB::Protos::GetAllBGThreadServersResp * response,
        ::google::protobuf::Closure * done)
{
    brpc::ClosureGuard done_guard(done);

    try
    {
        if (daemon_jobs.find(CnchBGThreadType(request->job_type())) == daemon_jobs.end())
            throw Exception(
                ErrorCodes::LOGICAL_ERROR,
                "No daemon job found for {}, this may always be caused by lack of config",
                toString(CnchBGThreadType(request->job_type())));
        auto daemon_job = daemon_jobs[CnchBGThreadType(request->job_type())];

        auto bg_job_datas = daemon_job->getBGJobInfos();
        std::for_each(bg_job_datas.begin(), bg_job_datas.end(),
            [&response] (const BGJobInfo & bg_job_data)
            {
                fillDMBGJobInfo(bg_job_data, *response->add_dm_bg_job_infos());
            }
        );
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        RPCHelpers::handleException(response->mutable_exception());
    }
}

void DaemonManagerServiceImpl::GetDMBGJobInfo(
    ::google::protobuf::RpcController *,
    const ::DB::Protos::GetDMBGJobInfoReq * request,
    ::DB::Protos::GetDMBGJobInfoResp * response,
    ::google::protobuf::Closure * done)
{
    brpc::ClosureGuard done_guard(done);
    try
    {
        UUID storage_uuid = RPCHelpers::createUUID(request->storage_uuid());
        auto it = daemon_jobs.find(CnchBGThreadType(request->job_type()));
        if (it == daemon_jobs.end())
            throw Exception(
                ErrorCodes::LOGICAL_ERROR,
                "No daemon job found for {}, this may always be caused by lack of config",
                toString(CnchBGThreadType(request->job_type())));
        auto daemon_job = it->second;
        BackgroundJobPtr bg_job_ptr = daemon_job->getBackgroundJob(storage_uuid);
        if (!bg_job_ptr)
        {
            LOG_INFO(log, "No background job found for uuid: {}", toString(storage_uuid));
            return;
        }
        BGJobInfo bg_job_data = bg_job_ptr->getBGJobInfo();
        fillDMBGJobInfo(bg_job_data, *response->mutable_dm_bg_job_info());
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        RPCHelpers::handleException(response->mutable_exception());
    }
}

void DaemonManagerServiceImpl::ControlDaemonJob(
    ::google::protobuf::RpcController *,
    const ::DB::Protos::ControlDaemonJobReq * request,
    ::DB::Protos::ControlDaemonJobResp * response,
    ::google::protobuf::Closure * done)
{
    brpc::ClosureGuard done_guard(done);

    try
    {
        StorageID storage_id = RPCHelpers::createStorageID(request->storage_id());
        CnchBGThreadAction action = static_cast<CnchBGThreadAction>(request->action());

        LOG_INFO(log, "Receive ControlDaemonJob RPC request for storage: {} job type: {} action: {}"
            , storage_id.getNameForLogs()
            , toString(CnchBGThreadType(request->job_type()))
            , toString(action));

        if (daemon_jobs.find(CnchBGThreadType(request->job_type())) == daemon_jobs.end())
            throw Exception(
                ErrorCodes::LOGICAL_ERROR,
                "No daemon job found for {}, this may always be caused by lack of config",
                toString(CnchBGThreadType(request->job_type())));
        auto daemon_job = daemon_jobs[CnchBGThreadType(request->job_type())];

        Result res = daemon_job->executeJobAction(storage_id, action);
        if (!res.res)
            throw Exception(res.error_str, ErrorCodes::LOGICAL_ERROR);
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        RPCHelpers::handleException(response->mutable_exception());
    }
}

void DaemonManagerServiceImpl::ForwardOptimizeQuery(
    ::google::protobuf::RpcController *,
    const ::DB::Protos::ForwardOptimizeQueryReq * request,
    ::DB::Protos::ForwardOptimizeQueryResp * response,
    ::google::protobuf::Closure * done)
{
    brpc::ClosureGuard done_guard(done);

    try
    {
        StorageID storage_id = RPCHelpers::createStorageID(request->storage_id());

        LOG_INFO(
            log,
            "Receive OptimizeTable RPC request for storage: {} partition_id: {} enable_try: {}",
            storage_id.getNameForLogs(), request->partition_id(), request->enable_try());

        if (daemon_jobs.find(CnchBGThreadType::MergeMutate) == daemon_jobs.end())
            throw Exception(
                ErrorCodes::LOGICAL_ERROR,
                "No daemon job found for MergeMutate, this may always be caused by lack of config");

        auto daemon_job = daemon_jobs.at(CnchBGThreadType::MergeMutate);
        const DaemonJobForMergeMutate * daemon_job_for_merge_mutate = dynamic_cast<DaemonJobForMergeMutate *>(daemon_job.get());

        daemon_job_for_merge_mutate->executeOptimize(storage_id, request->partition_id(), request->enable_try(), request->mutations_sync(), request->timeout_ms());
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        RPCHelpers::handleException(response->mutable_exception());
    }
}

}
}
