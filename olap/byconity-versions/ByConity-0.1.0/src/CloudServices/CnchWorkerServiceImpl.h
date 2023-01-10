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

#include <Common/config.h>
#include <Interpreters/Context_fwd.h>
#include <Protos/cnch_worker_rpc.pb.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH.h>

#include <common/logger_useful.h>

namespace DB
{

class CnchWorkerServiceImpl : protected WithMutableContext, public DB::Protos::CnchWorkerService
{
public:
    explicit CnchWorkerServiceImpl(ContextPtr context_);
    ~CnchWorkerServiceImpl() override;

    void executeSimpleQuery(
        google::protobuf::RpcController * cntl,
        const Protos::ExecuteSimpleQueryReq * request,
        Protos::ExecuteSimpleQueryResp * response,
        google::protobuf::Closure * done) override;

    void submitManipulationTask(
        google::protobuf::RpcController * cntl,
        const Protos::SubmitManipulationTaskReq * request,
        Protos::SubmitManipulationTaskResp * response,
        google::protobuf::Closure * done) override;

    void shutdownManipulationTasks(
        google::protobuf::RpcController * cntl,
        const Protos::ShutdownManipulationTasksReq * request,
        Protos::ShutdownManipulationTasksResp * response,
        google::protobuf::Closure * done) override;

    void touchManipulationTasks(
        google::protobuf::RpcController * cntl,
        const Protos::TouchManipulationTasksReq * request,
        Protos::TouchManipulationTasksResp * response,
        google::protobuf::Closure * done) override;

    void getManipulationTasksStatus(
        google::protobuf::RpcController * cntl,
        const Protos::GetManipulationTasksStatusReq * request,
        Protos::GetManipulationTasksStatusResp * response,
        google::protobuf::Closure * done) override;

    void GetPreallocatedStatus(
        google::protobuf::RpcController *,
        const Protos::GetPreallocatedStatusReq * request,
        Protos::GetPreallocatedStatusResp * response,
        google::protobuf::Closure * done) override;

    void SetQueryIntent(
        google::protobuf::RpcController *,
        const Protos::SetQueryIntentReq * request,
        Protos::SetQueryIntentResp * response,
        google::protobuf::Closure * done) override;

    void SubmitSyncTask(
        google::protobuf::RpcController *,
        const Protos::SubmitSyncTaskReq * request,
        Protos::SubmitSyncTaskResp * response,
        google::protobuf::Closure * done) override;

    void ResetQueryIntent(
        google::protobuf::RpcController *,
        const Protos::ResetQueryIntentReq * request,
        Protos::ResetQueryIntentResp * response,
        google::protobuf::Closure * done) override;

    void SubmitScaleTask(
        google::protobuf::RpcController *,
        const Protos::SubmitScaleTaskReq * request,
        Protos::SubmitScaleTaskResp * response,
        google::protobuf::Closure * done) override;

    void ClearPreallocatedDataParts(
        google::protobuf::RpcController *,
        const Protos::ClearPreallocatedDataPartsReq * request,
        Protos::ClearPreallocatedDataPartsResp * response,
        google::protobuf::Closure * done) override;

    void createDedupWorker(
        google::protobuf::RpcController *,
        const Protos::CreateDedupWorkerReq * request,
        Protos::CreateDedupWorkerResp * response,
        google::protobuf::Closure * done) override;

    void dropDedupWorker(
        google::protobuf::RpcController *,
        const Protos::DropDedupWorkerReq * request,
        Protos::DropDedupWorkerResp * response,
        google::protobuf::Closure * done) override;

    void getDedupWorkerStatus(
        google::protobuf::RpcController *,
        const Protos::GetDedupWorkerStatusReq * request,
        Protos::GetDedupWorkerStatusResp * response,
        google::protobuf::Closure * done) override;

#if USE_RDKAFKA
    void submitKafkaConsumeTask(
        google::protobuf::RpcController * cntl,
        const Protos::SubmitKafkaConsumeTaskReq * request,
        Protos::SubmitKafkaConsumeTaskResp * response,
        google::protobuf::Closure * done) override;

    void getConsumerStatus(
        google::protobuf::RpcController * cntl,
        const Protos::GetConsumerStatusReq * request,
        Protos::GetConsumerStatusResp * response,
        google::protobuf::Closure * done) override;
#endif

    void preloadChecksumsAndPrimaryIndex(
        google::protobuf::RpcController * cntl,
        const Protos::PreloadChecksumsAndPrimaryIndexReq * request,
        Protos::PreloadChecksumsAndPrimaryIndexResp * response,
        google::protobuf::Closure * done) override;

    void getCloudMergeTreeStatus(
        google::protobuf::RpcController * cntl,
        const Protos::GetCloudMergeTreeStatusReq * request,
        Protos::GetCloudMergeTreeStatusResp * response,
        google::protobuf::Closure * done) override;

    void sendCreateQuery(
        google::protobuf::RpcController * cntl,
        const Protos::SendCreateQueryReq * request,
        Protos::SendCreateQueryResp * response,
        google::protobuf::Closure * done) override;

    void sendQueryDataParts(
        google::protobuf::RpcController * cntl,
        const Protos::SendDataPartsReq * request,
        Protos::SendDataPartsResp * response,
        google::protobuf::Closure * done) override;

    void removeWorkerResource(
        google::protobuf::RpcController * cntl,
        const Protos::RemoveWorkerResourceReq * request,
        Protos::RemoveWorkerResourceResp * response,
        google::protobuf::Closure * done) override;

    /*
    void sendQueryVirtualDataParts(
        google::protobuf::RpcController * cntl,
        const Protos::SendVirtualDataPartsReq * request,
        Protos::SendVirtualDataPartsResp * response,
        google::protobuf::Closure * done) override {}
        */

    void sendCnchHiveDataParts(
        google::protobuf::RpcController * cntl,
        const Protos::SendCnchHiveDataPartsReq * request,
        Protos::SendCnchHiveDataPartsResp * response,
        google::protobuf::Closure * done) override;

    void checkDataParts(
        google::protobuf::RpcController * cntl,
        const Protos::CheckDataPartsReq * request,
        Protos::CheckDataPartsResp * response,
        google::protobuf::Closure * done) override;

    void sendOffloading(
        google::protobuf::RpcController * cntl,
        const Protos::SendOffloadingReq * request,
        Protos::SendOffloadingResp * response,
        google::protobuf::Closure * done) override;

private:
    Poco::Logger * log;

    // class PreloadHandler;
    // std::shared_ptr<PreloadHandler> preload_handler;
};

}
