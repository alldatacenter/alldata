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
#include <Protos/resource_manager_rpc.pb.h>

#include <memory>

namespace DB::ResourceManagement
{

class ResourceManagerController;
class VirtualWarehouseManager;
class WorkerGroupManager;

class ResourceManagerServiceImpl : public Protos::ResourceManagerService
{

public:
    ResourceManagerServiceImpl(ResourceManagerController & rm_controller_);
    ~ResourceManagerServiceImpl() override = default;

    template <typename T>
    bool checkForLeader(T & response);

    void syncResourceUsage(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::SyncResourceInfoReq * request,
        ::DB::Protos::SyncResourceInfoResp * response,
        ::google::protobuf::Closure * done) override;

    void registerWorkerNode(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::RegisterWorkerNodeReq * request,
        ::DB::Protos::RegisterWorkerNodeResp * response,
        ::google::protobuf::Closure * done) override;

    void removeWorkerNode(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::RemoveWorkerNodeReq * request,
        ::DB::Protos::RemoveWorkerNodeResp * response,
        ::google::protobuf::Closure * done) override;

    void createVirtualWarehouse(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::CreateVirtualWarehouseReq * request,
        ::DB::Protos::CreateVirtualWarehouseResp * response,
        ::google::protobuf::Closure * done) override;

    void updateVirtualWarehouse(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::UpdateVirtualWarehouseReq * request,
        ::DB::Protos::UpdateVirtualWarehouseResp * response,
        ::google::protobuf::Closure * done) override;

    void getVirtualWarehouse(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::GetVirtualWarehouseReq * request,
        ::DB::Protos::GetVirtualWarehouseResp * response,
        ::google::protobuf::Closure * done) override;

    void dropVirtualWarehouse(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::DropVirtualWarehouseReq * request,
        ::DB::Protos::DropVirtualWarehouseResp * response,
        ::google::protobuf::Closure * done) override;

    void getAllVirtualWarehouses(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::GetAllVirtualWarehousesReq * request,
        ::DB::Protos::GetAllVirtualWarehousesResp * response,
        ::google::protobuf::Closure * done) override;

    void getAllWorkers(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::GetAllWorkersReq * request,
        ::DB::Protos::GetAllWorkersResp * response,
        ::google::protobuf::Closure * done) override;

    void createWorkerGroup(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::CreateWorkerGroupReq * request,
        ::DB::Protos::CreateWorkerGroupResp * response,
        ::google::protobuf::Closure * done) override;

    void dropWorkerGroup(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::DropWorkerGroupReq * request,
        ::DB::Protos::DropWorkerGroupResp * response,
        ::google::protobuf::Closure * done) override;

    void getWorkerGroups(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::GetWorkerGroupsReq * request,
        ::DB::Protos::GetWorkerGroupsResp * response,
        ::google::protobuf::Closure * done) override;

    void getAllWorkerGroups(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::GetAllWorkerGroupsReq * request,
        ::DB::Protos::GetAllWorkerGroupsResp * response,
        ::google::protobuf::Closure * done) override;

    void pickWorker(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::RMScheduleReq * request,
        ::DB::Protos::PickWorkerResp * response,
        ::google::protobuf::Closure * done) override;

    void pickWorkers(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::RMScheduleReq * request,
        ::DB::Protos::PickWorkersResp * response,
        ::google::protobuf::Closure * done) override;

    void pickWorkerGroup(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::RMScheduleReq * request,
        ::DB::Protos::PickWorkerGroupResp * response,
        ::google::protobuf::Closure * done) override;

    void syncQueueDetails(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::SyncQueueDetailsReq * request,
        ::DB::Protos::SyncQueueDetailsResp * response,
        ::google::protobuf::Closure * done) override;

private:
    ResourceManagerController & rm_controller;
    VirtualWarehouseManager & vw_manager;
    WorkerGroupManager & group_manager;
};

using ResourceManagerServicePtr = std::shared_ptr<ResourceManagerServiceImpl>;

}
