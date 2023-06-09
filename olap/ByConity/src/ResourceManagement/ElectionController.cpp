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

#include <ResourceManagement/ElectionController.h>
#include <Catalog/Catalog.h>
#include <Common/Configurations.h>
#include <Coordination/Defines.h>
#include <Coordination/KeeperDispatcher.h>
#include <common/getFQDNOrHostName.h>
#include <ResourceManagement/ResourceTracker.h>
#include <ResourceManagement/VirtualWarehouseManager.h>
#include <ResourceManagement/WorkerGroupManager.h>
#include <ResourceManagement/WorkerGroupResourceCoordinator.h>

#include <ServiceDiscovery/IServiceDiscovery.h>
#include <Storages/PartCacheManager.h>

namespace DB::ErrorCodes
{
extern const int RESOURCE_MANAGER_INIT_ERROR;
}

namespace DB::ResourceManagement
{

ElectionController::ElectionController(ResourceManagerController & rm_controller_)
    : WithContext(rm_controller_.getContext())
    , LeaderElectionBase(getContext()->getRootConfig().resource_manager.check_leader_info_interval_ms)
    , rm_controller(rm_controller_)
{
    enable_leader_election = getContext()->hasZooKeeper();
    if (!enable_leader_election)
    {
        LOG_INFO(log, "RM is running in Single instance mode as ZooKeeper is not enabled. The current RM will become leader and should be the only instance.");
        onLeader();
        return;
    }

    startLeaderElection(getContext()->getSchedulePool());
}

ElectionController::~ElectionController()
{
    shutDown();
}

void ElectionController::onLeader()
{
    auto port = getContext()->getRootConfig().resource_manager.port.value;
    const std::string & rm_host = getHostIPFromEnv();
    auto current_address = createHostPortString(rm_host, port);
    LOG_INFO(log, "Starting leader callback for " + current_address);

    /// Sleep to prevent multiple leaders from appearing at the same time
    std::this_thread::sleep_for(std::chrono::milliseconds(wait_ms));

    /// Make sure get all needed metadata from KV store before becoming leader.
    if (!pullState())
    {
        if (!enable_leader_election)
        {
            /// We have no way to trigger a new onLeader action in Single instance mode.
            /// So we need to restart the process if we encounter any error.
            throw Exception("Failed to pull metadata from KV store in Single instance mode, please check the metadata service and restart RM.", ErrorCodes::RESOURCE_MANAGER_INIT_ERROR);
        }
        is_leader = false;
        exitLeaderElection();
    }
    else
    {
        LOG_INFO(log, "Current RM node " + current_address + " has become leader.");
        if (getContext()->getRootConfig().resource_manager.enable_auto_resource_sharing)
        {
            rm_controller.getWorkerGroupResourceCoordinator().start();
        }
        is_leader = true;
    }
}

void ElectionController::enterLeaderElection()
{
    try
    {
        auto port = getContext()->getRootConfig().resource_manager.port.value;
        const std::string & rm_host = getHostIPFromEnv();
        auto current_address = createHostPortString(rm_host, port);
        auto election_path = getContext()->getRootConfig().resource_manager.election_path.value;

        current_zookeeper = getContext()->getZooKeeper();
        current_zookeeper->createAncestors(election_path + "/");

        leader_election = std::make_unique<zkutil::LeaderElection>(
            getContext()->getSchedulePool(),
            election_path,
            *current_zookeeper,
            [&]() { onLeader(); },
            current_address,
            false
        );
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);
    }
}

bool ElectionController::pullState()
{
    auto retry_count = 3;
    auto success = false;
    do
    {
        try
        {
            // Clear outdated data
            auto & vw_manager = rm_controller.getVirtualWarehouseManager();
            vw_manager.clearVirtualWarehouses();
            auto & group_manager = rm_controller.getWorkerGroupManager();
            group_manager.clearWorkerGroups();
            auto & resource_tracker = rm_controller.getResourceTracker();
            resource_tracker.clearWorkers();
            rm_controller.initialize();
            success = true;
        }
        catch (...)
        {
            tryLogCurrentException(log);
            --retry_count;
        }
    } while (!success && retry_count > 0);

    return success;
}

void ElectionController::exitLeaderElection()
{
    shutDown();
}

void ElectionController::shutDown()
{
    is_leader = false;
    rm_controller.getWorkerGroupResourceCoordinator().stop();

    leader_election.reset();
    current_zookeeper.reset();
    LOG_DEBUG(log, "Exit leader election");
}

}
