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

#include <ResourceManagement/ResourceReporter.h>

#include <Interpreters/Context.h>
#include <ResourceManagement/CommonData.h>
#include <ResourceManagement/ResourceManagerClient.h>
#include <Common/HostWithPorts.h>



namespace DB::ErrorCodes
{
    extern const int NO_SUCH_SERVICE;
}

namespace DB::ResourceManagement
{

ResourceReporterTask::ResourceReporterTask(ContextPtr global_context_)
    : WithContext(global_context_)
    , log(&Poco::Logger::get("ResourceReporterTask"))
    , resource_monitor(std::make_unique<ResourceMonitor>(global_context_))
    , background_task(global_context_->getSchedulePool().createTask("ResourceReporterTask", [&](){ run(); }))
{
    LOG_TRACE(log, "Create ResourceReporterTask.");
    background_task->activateAndSchedule();
}

ResourceReporterTask::~ResourceReporterTask()
{
    try
    {
        LOG_TRACE(log, "Remove ResourceReporterTask.");
        background_task->deactivate();
        sendRemove();
    }
    catch (...)
    {
        tryLogCurrentException(log);
    }
}

void ResourceReporterTask::run()
{
    try
    {
        if (init_request)
        {
            sendRegister();
            init_request = false;
        }
        else
        {
            if (!sendHeartbeat())
            {
                init_request = true; // Ensure register eventually succeeds
                sendRegister();
            }
        }
        //TODO: Change to config setting
        background_task->scheduleAfter(1000);
    }
    catch (Exception & e)
    {
        // FIXME
        if (e.code() != ErrorCodes::NO_SUCH_SERVICE)
            background_task->scheduleAfter(3000);

        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }
}

bool ResourceReporterTask::sendHeartbeat()
{
    auto resource_manager = getContext()->getResourceManagerClient();
    auto data = resource_monitor->createResourceData();

    LOG_TRACE(log, "Send heartbeat to RM: {} self: {}", resource_manager->leader_host_port, data.host_ports.toDebugString());
    ContextPtr context = getContext();
    data.id = getWorkerID(context);
    data.vw_name = getVirtualWareHouseID(context);
    data.worker_group_id = getWorkerGroupID(context);
    return resource_manager->reportResourceUsage(data);
}

void ResourceReporterTask::sendRegister()
{
    auto resource_manager = getContext()->getResourceManagerClient();
    auto data = resource_monitor->createResourceData(true);

    LOG_TRACE(log, "Register Node in RM: {} self: {}", resource_manager->leader_host_port, data.host_ports.toDebugString());

    ContextPtr context = getContext();
    data.id = getWorkerID(context);
    data.vw_name = getVirtualWareHouseID(context);
    data.worker_group_id = getWorkerGroupID(context);
    resource_manager->registerWorker(data);
}

void ResourceReporterTask::sendRemove()
{
    auto resource_manager = getContext()->getResourceManagerClient();
    try
    {
        ContextPtr context = getContext();
        resource_manager->removeWorker(getWorkerID(context), getVirtualWareHouseID(context), getWorkerGroupID(context));
    }
    catch (...)
    {
        tryLogCurrentException("ResourceReporter::sendRemove", "Failed to unregister from RM " + resource_manager->leader_host_port);
    }
}

void ResourceReporterTask::start()
{
    LOG_TRACE(log, "Start ResourceReporterTask.");
    background_task->activateAndSchedule();
}

void ResourceReporterTask::stop()
{
    LOG_TRACE(log, "Stop ResourceReporterTask.");
    background_task->deactivate();
    sendRemove();
}

}
