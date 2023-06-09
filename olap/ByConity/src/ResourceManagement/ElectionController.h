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

#include <Core/BackgroundSchedulePool.h>
#include <Interpreters/Context.h>
#include <Interpreters/Context_fwd.h>
#include <ResourceManagement/ResourceManagerController.h>
#include <Coordination/LeaderElectionBase.h>

namespace zkutil
{
    class LeaderElection;
    class ZooKeeper;
    using ZooKeeperPtr = std::shared_ptr<ZooKeeper>;
}

namespace DB::ResourceManagement
{

/** ElectionController is used for Resource Manager leader election (handled by ZooKeeper)
  * It contains a background thread to check for leader information, and ensures that a newly elected
  * leader retrieves its state from KV store.
  */
class ElectionController : public WithContext, public LeaderElectionBase
{

public:
    ElectionController(ResourceManagerController & rm_controller_);
    ~ElectionController() override;

private:
    void shutDown();

    void enterLeaderElection() override;
    void onLeader() override;
    void exitLeaderElection() override;

    // Pulls logical VW and worker group info from KV store.
    bool pullState();

    Poco::Logger * log = &Poco::Logger::get("ElectionController");
    ResourceManagerController & rm_controller;
    bool enable_leader_election{false};
    std::atomic_bool is_leader{false};

};

using ElectionControllerPtr = std::shared_ptr<ElectionController>;

}

