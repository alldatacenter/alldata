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

#include <Core/Types.h>
#include <Interpreters/Context_fwd.h>

namespace DB
{

class Context;
namespace ResourceManagement
{
    struct WorkerNodeResourceData;
}
using WorkerNodeResourceData = ResourceManagement::WorkerNodeResourceData;

class CPUMonitor
{
    static constexpr auto filename = "/proc/stat";

public:
    struct Data
    {
        UInt64 total_ticks;
        UInt64 active_ticks;
        double cpu_usage;
    };

    CPUMonitor();
    ~CPUMonitor();

    Data get();

private:
    int fd;
    Data data{};
};

class MemoryMonitor
{
    static constexpr auto filename = "/proc/meminfo";

public:
    struct Data
    {
        UInt64 memory_total;
        UInt64 memory_available;
        double memory_usage;
    };

    MemoryMonitor();
    ~MemoryMonitor();

    Data get() const;
private:
    int fd;
};

class ResourceMonitor : protected WithContext
{
public:
    explicit ResourceMonitor(const ContextPtr global_context_): WithContext(global_context_), mem_monitor(), cpu_monitor() {}

    WorkerNodeResourceData createResourceData(bool init = false);

private:
    UInt64 getCPULimit();
    UInt64 getMemoryLimit();

    UInt64 getDiskSpace();
    UInt64 getQueryCount();
    UInt64 getBackgroundTaskCount();

private:
    MemoryMonitor mem_monitor;
    CPUMonitor cpu_monitor;
};

}
