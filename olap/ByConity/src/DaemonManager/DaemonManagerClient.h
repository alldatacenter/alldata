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

#include <DaemonManager/DaemonHelper.h>
#include <DaemonManager/BackgroundJob.h>
#include <Interpreters/StorageID.h>
#include <CloudServices/RpcClientBase.h>
#include <Core/Types.h>
#include <Core/UUID.h>
#include <DaemonManager/DaemonManagerClient_fwd.h>

namespace DB
{
namespace Protos
{
    class DaemonManagerService_Stub;
}

namespace DaemonManager
{
class DaemonManagerClient : public RpcClientBase
{
public:
    static String getName() { return "DaemonManagerClient"; }

    DaemonManagerClient(String host_port);
    DaemonManagerClient(HostWithPorts host_ports);

    ~DaemonManagerClient() override;

    BGJobInfos getAllBGThreadServers(CnchBGThreadType type);
    std::optional<BGJobInfo> getDMBGJobInfo(const UUID & storage_uuid, CnchBGThreadType type);
    void controlDaemonJob(const StorageID & storage_id, CnchBGThreadType job_type, CnchBGThreadAction action);
    void forwardOptimizeQuery(const StorageID & storage_id, const String & partition_id, bool enable_try, bool mutations_sync, UInt64 timeout_ms);

private:
    std::unique_ptr<Protos::DaemonManagerService_Stub> stub_ptr;
};

}
}
