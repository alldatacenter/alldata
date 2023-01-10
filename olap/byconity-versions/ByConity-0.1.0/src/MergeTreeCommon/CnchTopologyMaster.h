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

#include <MergeTreeCommon/CnchServerTopology.h>
#include <Core/BackgroundSchedulePool.h>
#include <Interpreters/Context_fwd.h>

namespace DB
{
/***
 * This class help to supply a unified view of servers' topology in cnch cluster.
 * A background task will periodically fetch topology from metastore to ensure the cached topology
 * keeps up to date.
 */
class CnchTopologyMaster: WithContext
{
public:
    explicit CnchTopologyMaster(ContextPtr context_);

    ~CnchTopologyMaster();

    std::list<CnchServerTopology> getCurrentTopology();

    /// Get target server for table with current timestamp.
    HostWithPorts getTargetServer(const String & table_uuid, bool allow_empty_result, bool allow_tso_unavailable = false);
    /// Get target server with provided timestamp.
    HostWithPorts getTargetServer(const String & table_uuid, UInt64 ts,  bool allow_empty_result, bool allow_tso_unavailable = false);

    void shutDown();
private:

    void fetchTopologies();

    HostWithPorts getTargetServerImpl(
        const String & table_uuid,
        std::list<CnchServerTopology> & current_topology,
        UInt64 current_ts,
        bool allow_empty_result,
        bool allow_tso_unavailable);

    Poco::Logger * log = &Poco::Logger::get("CnchTopologyMaster");
    BackgroundSchedulePool::TaskHolder topology_fetcher;
    std::list<CnchServerTopology> topologies;
    mutable std::mutex mutex;
};

using CnchTopologyMasterPtr = std::shared_ptr<CnchTopologyMaster>;

}
