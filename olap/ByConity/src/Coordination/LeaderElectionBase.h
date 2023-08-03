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

#include <chrono>
#include <cstddef>
#include <thread>
#include <common/types.h>
#include <Common/Exception.h>
#include <Common/ZooKeeper/ZooKeeper.h>
#include <Core/BackgroundSchedulePool.h>
#include <Coordination/LeaderElection.h>

namespace DB
{

class LeaderElectionBase
{
public:
    explicit LeaderElectionBase(size_t wait_ms_): wait_ms(wait_ms_) {}

    virtual ~LeaderElectionBase()
    {
        if (restart_task)
            restart_task->deactivate();
    }

    virtual void onLeader() = 0;
    virtual void exitLeaderElection() = 0;
    virtual void enterLeaderElection() = 0;

    void startLeaderElection(BackgroundSchedulePool & pool)
    {
        enterLeaderElection();

        restart_task = pool.createTask("ElectionRestartTask", [&]() { run(); });
        restart_task->activateAndSchedule();
    }

    void run()
    {
        try
        {
            if (!current_zookeeper || current_zookeeper->expired())
            {
                exitLeaderElection();
                enterLeaderElection();
            }
        }
        catch (...)
        {
            tryLogCurrentException(__PRETTY_FUNCTION__);
        }

        restart_task->scheduleAfter(wait_ms);
    }

protected:
    size_t wait_ms;
    zkutil::ZooKeeperPtr current_zookeeper;
    zkutil::LeaderElectionPtr leader_election;
    BackgroundSchedulePool::TaskHolder restart_task;
};

}
