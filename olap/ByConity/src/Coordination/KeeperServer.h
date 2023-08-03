/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once

#include <Coordination/CoordinationSettings.h>
#include <Coordination/InMemoryLogStore.h>
#include <Coordination/KeeperStateMachine.h>
#include <Coordination/KeeperStateManager.h>
#include <Coordination/KeeperStorage.h>
#include <Coordination/Keeper4LWInfo.h>
#include <Coordination/KeeperContext.h>
#include <common/logger_useful.h>
#include <Poco/Util/AbstractConfiguration.h>

#include <unordered_map>
#include <libnuraft/raft_params.hxx>
#include <libnuraft/raft_server.hxx>

namespace DB
{

using RaftAppendResult = nuraft::ptr<nuraft::cmd_result<nuraft::ptr<nuraft::buffer>>>;

class KeeperServer
{
private:
    const int server_id;

    CoordinationSettingsPtr coordination_settings;

    nuraft::ptr<KeeperStateMachine> state_machine;

    nuraft::ptr<KeeperStateManager> state_manager;

    struct KeeperRaftServer;
    nuraft::ptr<KeeperRaftServer> raft_instance;
    nuraft::ptr<nuraft::asio_service> asio_service;
    nuraft::ptr<nuraft::rpc_listener> asio_listener;
    // because some actions can be applied
    // when we are sure that there are no requests currently being
    // processed (e.g. recovery) we do all write actions
    // on raft_server under this mutex.
    mutable std::mutex server_write_mutex;

    std::mutex append_entries_mutex;

    std::mutex initialized_mutex;
    std::atomic<bool> initialized_flag = false;
    std::condition_variable initialized_cv;
    std::atomic<bool> initial_batch_committed = false;

    nuraft::ptr<nuraft::cluster_config> last_local_config;

    Poco::Logger * log;

    /// Callback func which is called by NuRaft on all internal events.
    /// Used to determine the moment when raft is ready to server new requests
    nuraft::cb_func::ReturnCode callbackFunc(nuraft::cb_func::Type type, nuraft::cb_func::Param * param);

    /// Almost copy-paste from nuraft::launcher, but with separated server init and start
    /// Allows to avoid race conditions.
    void launchRaftServer(bool enable_ipv6);

    void shutdownRaftServer();

    void loadLatestConfig();

    void enterRecoveryMode(nuraft::raft_params & params);

    std::atomic_bool is_recovering = false;

    std::shared_ptr<KeeperContext> keeper_context;

public:
    KeeperServer(
        const KeeperConfigurationAndSettingsPtr & settings_,
        const Poco::Util::AbstractConfiguration & config_,
        ResponsesQueue & responses_queue_,
        SnapshotsQueue & snapshots_queue_);

    /// Load state machine from the latest snapshot and load log storage. Start NuRaft with required settings.
    void startup(const Poco::Util::AbstractConfiguration & config, bool enable_ipv6 = true);

    /// Put local read request and execute in state machine directly and response into
    /// responses queue
    void putLocalReadRequest(const KeeperStorage::RequestForSession & request);

    bool isRecovering() const { return is_recovering; }

    /// Put batch of requests into Raft and get result of put. Responses will be set separately into
    /// responses_queue.
    RaftAppendResult putRequestBatch(const KeeperStorage::RequestsForSessions & requests);

    /// Return set of the non-active sessions
    std::vector<int64_t> getDeadSessions();
    // std::unordered_set<int64_t> getDeadSessions();

    nuraft::ptr<KeeperStateMachine> getKeeperStateMachine() const { return state_machine; }

    void forceRecovery();

    bool isLeader() const;

    bool isFollower() const;

    bool isObserver() const;

    bool isLeaderAlive() const;

    /// @return follower count if node is not leader return 0
    uint64_t getFollowerCount() const;

    /// @return synced follower count if node is not leader return 0
    uint64_t getSyncedFollowerCount() const;

    /// Wait server initialization (see callbackFunc)
    void waitInit();

    /// Return true if KeeperServer initialized
    bool checkInit() const { return initialized_flag; }

    void shutdown();

    int getServerID() const { return server_id; }

    /// Get configuration diff between current configuration in RAFT and in XML file
    ConfigUpdateActions getConfigurationDiff(const Poco::Util::AbstractConfiguration & config);

    /// Apply action for configuration update. Actually call raft_instance->remove_srv or raft_instance->add_srv.
    /// Synchronously check for update results with retries.
    void applyConfigurationUpdate(const ConfigUpdateAction & task);


    /// Wait configuration update for action. Used by followers.
    /// Return true if update was successfully received.
    bool waitConfigurationUpdate(const ConfigUpdateAction & task);
};

}
