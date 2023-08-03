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

#include <ResourceManagement/ResourceTracker.h>

#include <Common/Configurations.h>
#include <Common/Exception.h>
#include <Interpreters/Context.h>
#include <ResourceManagement/CommonData.h>
#include <ResourceManagement/ResourceManagerController.h>

namespace DB::ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int NOT_FOUND_NODE;
    extern const int WORKER_NODE_ALREADY_EXISTS;
}

namespace DB::ResourceManagement
{

ResourceTracker::ResourceTracker(ResourceManagerController & rm_controller_)
    : rm_controller(rm_controller_)
    , log(&Poco::Logger::get("ResourceTracker"))
    , background_task(getContext()->getSchedulePool().createTask("ResourceTrackerTask", [&](){ clearLostWorkers(); }))
    , register_granularity_sec(getContext()->getRootConfig().resource_manager.worker_register_visible_granularity_sec.value)
{
    background_task->activateAndSchedule();
}

ResourceTracker::~ResourceTracker()
{
    try
    {
        LOG_TRACE(log, "Stopping ResourceTrackerTask");
        background_task->deactivate();
    }
    catch (...)
    {
        tryLogCurrentException(log);
    }
}

ContextPtr ResourceTracker::getContext() const
{
    return rm_controller.getContext();
}

void ResourceTracker::clearLostWorkers()
{
    time_t time_now = time(nullptr);
    size_t timeout = getContext()->getRootConfig().resource_manager.lost_worker_timeout_seconds.value;
    time_t timeout_threshold = time_now - timeout;

    std::lock_guard lock(node_mutex);
    auto it = worker_nodes.begin();
    while (it != worker_nodes.end())
    {
        if (it->second->last_update_time < timeout_threshold)
        {
            LOG_DEBUG(log, "Removing worker {}: {}, last updated about {} seconds ago.",
                            it->second->worker_group_id,
                            it->second->getID(),
                            time_now - it->second->last_update_time);
            try
            {
                rm_controller.removeWorkerNode(it->second->getID(), it->second->vw_name, it->second->worker_group_id);
            }
            catch (...)
            {
                LOG_ERROR(log, "Remove node from controller error! {}", it->second->toDebugString());
            }
            it = worker_nodes.erase(it);
        }
        else
            ++it;
    }

    background_task->scheduleAfter(timeout / 2 * 1000);
}

std::vector<WorkerNodePtr> ResourceTracker::loadWorkerNode(const String & vw_name, const std::vector<WorkerNodeCatalogData> & data)
{
    std::lock_guard lock(node_mutex);

    std::vector<WorkerNodePtr> res;
    for (auto & node: data)
    {
        auto id = node.id;
        if (auto it = worker_nodes.find(id); it != worker_nodes.end())
        {
            res.push_back(it->second);
        }
        else
        {
            auto work_node = std::make_shared<WorkerNode>(node.id, node.host_ports, vw_name, node.worker_group_id);
            worker_nodes.emplace(id, work_node);
            res.push_back(work_node);
        }
    }

    return res;
}

/// If the first value is True, that means the outdated work_node should be removed from work_group.
std::pair<bool, WorkerNodePtr> ResourceTracker::registerNodeImpl(const WorkerNodeResourceData & data, std::lock_guard<std::mutex> &)
{
    auto worker_id = data.id;

    if (auto it = worker_nodes.find(worker_id); it != worker_nodes.end())
    {
        if (!it->second->assigned)
            return {false, it->second};

        if (it->second->worker_group_id != data.worker_group_id)
        {
            LOG_WARNING(log, "The work_group_id {} of Node {} is different from the previous {}",
                            data.worker_group_id, data.id, it->second->worker_group_id);
            return {true, it->second};
        }

        if (it->second->vw_name != data.vw_name)
        {
            LOG_WARNING(log, "The vw_name {} of Node {} is different from the previous {}",
                            data.vw_name, data.id, it->second->vw_name);
            return {true, it->second};
        }

        if (!it->second->host.isSameEndpoint(data.host_ports))
        {
            LOG_WARNING(log, "Replace available node: {}, old_host: {}, new_host: {}",
                            it->first, it->second->host.toDebugString(), data.host_ports.toDebugString());
        }

        worker_nodes.erase(it);
    }

    size_t uptime_sec = rm_controller.getContext()->getUptimeSeconds();
    auto set_running = uptime_sec < (register_granularity_sec / 2);
    auto new_node = std::make_shared<WorkerNode>(data, set_running);
    worker_nodes.emplace(worker_id, new_node);
    return {false, new_node};
}

std::pair<bool, WorkerNodePtr> ResourceTracker::registerNode(const WorkerNodeResourceData & data)
{
    std::lock_guard lock(node_mutex);
    return registerNodeImpl(data, lock);
}

bool ResourceTracker::updateNode(const WorkerNodeResourceData & data)
{
    std::lock_guard lock(node_mutex);

    auto worker_id = data.id;

    if (auto it = worker_nodes.find(worker_id); it != worker_nodes.end())
    {
        it->second->update(data, register_granularity_sec);
        return true;
    }
    else
    {
        return false;
    }
}

void ResourceTracker::removeNode(const String & worker_id)
{
    std::lock_guard lock(node_mutex);
    worker_nodes.erase(worker_id);
}

void ResourceTracker::clearWorkers()
{
    std::lock_guard lock(node_mutex);
    worker_nodes.clear();
}

std::unordered_map<std::string, WorkerNodePtr> ResourceTracker::getAllWorkers()
{
    std::lock_guard lock(node_mutex);
    return worker_nodes;
}

}
