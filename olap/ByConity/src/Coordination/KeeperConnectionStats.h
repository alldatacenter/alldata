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

#include <common/types.h>
#include <memory>
#include <cstdint>

namespace DB
{

/// Request statistics for connection or dispatcher
class KeeperConnectionStats
{
public:
    KeeperConnectionStats() = default;

    uint64_t getMinLatency() const;
    uint64_t getMaxLatency() const;

    uint64_t getAvgLatency() const;
    uint64_t getLastLatency() const;

    uint64_t getPacketsReceived() const;
    uint64_t getPacketsSent() const;

    void incrementPacketsReceived();
    void incrementPacketsSent();

    void updateLatency(uint64_t latency_ms);
    void reset();

private:
    void resetLatency();
    void resetRequestCounters();

    /// all response with watch response included
    uint64_t packets_sent = 0;
    /// All user requests
    uint64_t packets_received = 0;

    /// For consistent with zookeeper measured by millisecond,
    /// otherwise maybe microsecond is better
    uint64_t total_latency = 0;
    uint64_t max_latency = 0;
    uint64_t min_latency = 0;

    /// last operation latency
    uint64_t last_latency = 0;

    uint64_t count = 0;
};

}
