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

#include <Coordination/KeeperConnectionStats.h>

namespace DB
{

uint64_t KeeperConnectionStats::getMinLatency() const
{
    return min_latency;
}

uint64_t KeeperConnectionStats::getMaxLatency() const
{
    return max_latency;
}

uint64_t KeeperConnectionStats::getAvgLatency() const
{
    if (count != 0)
        return total_latency / count;
    return 0;
}

uint64_t KeeperConnectionStats::getLastLatency() const
{
    return last_latency;
}

uint64_t KeeperConnectionStats::getPacketsReceived() const
{
    return packets_received;
}

uint64_t KeeperConnectionStats::getPacketsSent() const
{
    return packets_sent;
}

void KeeperConnectionStats::incrementPacketsReceived()
{
    packets_received++;
}

void KeeperConnectionStats::incrementPacketsSent()
{
    packets_sent++;
}

void KeeperConnectionStats::updateLatency(uint64_t latency_ms)
{
    last_latency = latency_ms;
    total_latency += (latency_ms);
    count++;

    if (latency_ms < min_latency)
    {
        min_latency = latency_ms;
    }

    if (latency_ms > max_latency)
    {
        max_latency = latency_ms;
    }
}

void KeeperConnectionStats::reset()
{
    resetLatency();
    resetRequestCounters();
}

void KeeperConnectionStats::resetLatency()
{
    total_latency = 0;
    count = 0;
    max_latency = 0;
    min_latency = 0;
}

void KeeperConnectionStats::resetRequestCounters()
{
    packets_received = 0;
    packets_sent = 0;
}

}
