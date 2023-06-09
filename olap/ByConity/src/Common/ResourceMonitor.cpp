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

#include <Common/ResourceMonitor.h>

#include <Common/filesystemHelpers.h>
#include <common/getFQDNOrHostName.h>
#include <common/getMemoryAmount.h>
#include <Common/getNumberOfPhysicalCPUCores.h>
#include <Interpreters/Context.h>
#include <Interpreters/ProcessList.h>
#include <Parsers/ParserSelectWithUnionQuery.h>
#include <WorkerTasks/ManipulationList.h>
#include <IO/ReadBufferFromMemory.h>
#include <IO/ReadHelpers.h>
#include <ResourceManagement/CommonData.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int FILE_DOESNT_EXIST;
    extern const int CANNOT_OPEN_FILE;
    extern const int CANNOT_READ_FROM_FILE_DESCRIPTOR;
}

CPUMonitor::CPUMonitor()
{
    fd = ::open(filename, O_RDONLY | O_CLOEXEC);

    if (-1 == fd)
        throwFromErrno("Cannot open file " + std::string(filename), errno == ENOENT ? ErrorCodes::FILE_DOESNT_EXIST : ErrorCodes::CANNOT_OPEN_FILE);
}

CPUMonitor::~CPUMonitor()
{
    if (0 != ::close(fd))
        tryLogCurrentException(__PRETTY_FUNCTION__);
}

CPUMonitor::Data CPUMonitor::get()
{
    size_t buf_size = 1024;
    char buf[buf_size];

    ssize_t res = 0;
    int retry = 3;

    while (retry--)
    {
        res = ::pread(fd, buf, buf_size, 0);
        if (-1 == res)
        {
            if (errno == EINTR)
                continue;

            throwFromErrno("Cannot read from file " + std::string(filename), ErrorCodes::CANNOT_READ_FROM_FILE_DESCRIPTOR);
        }

        if (res >= 0)
            break;
    }

    if (res < 0)
        throw Exception("Can't open file " + std::string(filename), ErrorCodes::CANNOT_READ_FROM_FILE_DESCRIPTOR);

    ReadBufferFromMemory in(buf, res);

    uint64_t prev_active_ticks = data.active_ticks;
    uint64_t prev_total_ticks = data.total_ticks;

    // Remove cpu label
    skipNonNumericIfAny(in);
    data.total_ticks = 0;

    uint64_t idle_ticks = 0;
    for (auto i = 0; i < 8; i++)
    {
        /// Iterate through first 8 CPU time components (Guest is already included in user and nice)
        uint64_t tmp;
        readIntText(tmp, in);
        skipWhitespaceIfAny(in);
        data.total_ticks += tmp;
        if (i == 3)
            idle_ticks = tmp;
    }
    data.active_ticks = data.total_ticks - idle_ticks;

    // FIXME: Using sync interval as CPU Usage intervals
    uint64_t active_ticks_in_interval = data.active_ticks - prev_active_ticks;
    uint64_t total_ticks_in_interval = data.total_ticks - prev_total_ticks;
    data.cpu_usage = 100.00 * active_ticks_in_interval / total_ticks_in_interval;

    return data;
}


MemoryMonitor::MemoryMonitor()
{
    fd = ::open(filename, O_RDONLY | O_CLOEXEC);

    if (-1 == fd)
        throwFromErrno("Cannot open file " + std::string(filename), errno == ENOENT ? ErrorCodes::FILE_DOESNT_EXIST : ErrorCodes::CANNOT_OPEN_FILE);
}


MemoryMonitor::~MemoryMonitor()
{
    if (0 != ::close(fd))
        tryLogCurrentException(__PRETTY_FUNCTION__);
}

MemoryMonitor::Data MemoryMonitor::get() const
{
    Data data{};

    size_t buf_size = 1024;
    char buf[buf_size];

    ssize_t res = 0;
    int retry = 3;

    while (retry--)
    {
        res = ::pread(fd, buf, buf_size, 0);

        if (-1 == res)
        {
            if (errno == EINTR)
                continue;

            throwFromErrno("Cannot read from file " + std::string(filename), ErrorCodes::CANNOT_READ_FROM_FILE_DESCRIPTOR);
        }

        if (res >= 0)
            break;
    }

    if (res < 0)
        throw Exception("Can't open file " + std::string(filename), ErrorCodes::CANNOT_READ_FROM_FILE_DESCRIPTOR);

    ReadBufferFromMemory in(buf, res);

    String tmp;
    readWord(tmp, in);
    if (tmp != "MemTotal:")
        throw Exception("Unexpected element " + tmp + " while parsing meminfo", ErrorCodes::LOGICAL_ERROR);
    skipWhitespaceIfAny(in);
    readIntText(data.memory_total, in);
    // Skip MemFree line
    readString(tmp, in);
    skipWhitespaceIfAny(in);
    readString(tmp, in);
    skipWhitespaceIfAny(in);
    readWord(tmp, in);
    if (tmp != "MemAvailable:")
        throw Exception("Unexpected element " + tmp + " while parsing meminfo", ErrorCodes::LOGICAL_ERROR);
    skipWhitespaceIfAny(in);
    readIntText(data.memory_available, in);

    if (!data.memory_total)
        throw Exception("Total memory is 0", ErrorCodes::LOGICAL_ERROR);

    data.memory_usage = 100.00 * static_cast<double>(data.memory_total - data.memory_available) / data.memory_total;

    return data;
}

UInt64 ResourceMonitor::getCPULimit()
{
    if (getenv("POD_CPU_CORE_LIMIT"))
        return std::stoi(getenv("POD_CPU_CORE_LIMIT"));
    return getNumberOfPhysicalCPUCores();
}

UInt64 ResourceMonitor::getMemoryLimit()
{
    if (getenv("POD_MEMORY_BYTE_LIMIT"))
        return std::stoi(getenv("POD_MEMORY_BYTE_LIMIT"));
    return getMemoryAmount();
}

UInt64 ResourceMonitor::getDiskSpace()
{
    auto path = getContext()->getPath();
    auto stat = getStatVFS(path);
    auto available_bytes = stat.f_bavail * stat.f_blocks;
    return available_bytes;
}

UInt64 ResourceMonitor::getQueryCount()
{
    return getContext()->getProcessList().size(); /// TODO: remove system_query.
}

UInt64 ResourceMonitor::getBackgroundTaskCount()
{
    return getContext()->getManipulationList().size();
}

WorkerNodeResourceData ResourceMonitor::createResourceData(bool init)
{
    WorkerNodeResourceData data;

    data.host_ports = getContext()->getHostWithPorts();

    auto cpu_data = cpu_monitor.get();
    auto mem_data = mem_monitor.get();

    data.cpu_usage = cpu_data.cpu_usage;
    data.memory_usage = mem_data.memory_usage;
    data.memory_available = mem_data.memory_available;
    data.disk_space = getDiskSpace();
    data.query_num = getQueryCount();

    if (init)
    {
        data.cpu_limit = getCPULimit();
        data.memory_limit = getMemoryLimit();
    }

    return data;
}

}
